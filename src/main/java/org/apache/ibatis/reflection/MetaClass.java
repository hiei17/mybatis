/*
 *    Copyright 2009-2011 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @author Clinton Begin
 */
/**
 * 原类
 * 
 */
public class MetaClass {

    //有一个反射器
    //可以看到方法基本都是再次委派给这个Reflector
  private Reflector reflector;

  private MetaClass(Class<?> type) {
    //内部有缓存
    this.reflector = Reflector.forClass(type);
  }

  public static MetaClass forClass(Class<?> type) {
    return new MetaClass(type);
  }

  public static boolean isClassCacheEnabled() {
    return Reflector.isClassCacheEnabled();
  }

  public static void setClassCacheEnabled(boolean classCacheEnabled) {
    Reflector.setClassCacheEnabled(classCacheEnabled);
  }

  //找到这个属性的类型 返回对应MetaClass里面包装着对应reflector
  public MetaClass metaClassForProperty(String name) {
    Class<?> propType = reflector.getGetterType(name);
    return MetaClass.forClass(propType);
  }

  /**
   *
   * @param name 大写的属性表达式
   * @return
   */
  public String findProperty(String name) {
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  public String findProperty(String name, boolean useCamelCaseMapping) {
    if (useCamelCaseMapping) {
      name = name.replace("_", "");
    }
    return findProperty(name);
  }

  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop.getName());
      //递归
      return metaProp.getSetterType(prop.getChildren());
    }
    //递归出口
    return reflector.getSetterType(prop.getName());

  }

  /**
   * 通过reflector的最终返回值类型
   * @param name 属性表达式
   * @return class
   */
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop);
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    return getGetterType(prop);
  }

  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    Class<?> propType = getGetterType(prop);
    return MetaClass.forClass(propType);
  }

  /**
   *用reflector得到这个属性表达式get返回值类型
   * @param prop 属性表达式
   * @return 对应class
   */
  private Class<?> getGetterType(PropertyTokenizer prop) {

    //这个属性名 对应的返回类型(reflector构造时已经放好了map
    String propName = prop.getName();
    Class<?> type = reflector.getGetterType(propName);

    //有[] 并且是集合类 如    //orders[0]
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {

      //去对应invoke里面反射拿返回类型
      Type returnType = getGenericGetterType(propName);

      //是返回类型里面有类型参数
      if (returnType instanceof ParameterizedType) {
        //类型参数数组
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        //刚好一个类型参数
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            //orders=List<Order> 返回Order
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {//还是带类型参数的类型  List<List<Long>>
            //orders=List<List<Long>> 返回List
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }

      }
    }
    return type;
  }

  private Type getGenericGetterType(String propertyName) {
    try {
      Invoker invoker = reflector.getGetInvoker(propertyName);
      if (invoker instanceof MethodInvoker) {
        Field _method = MethodInvoker.class.getDeclaredField("method");
        _method.setAccessible(true);
        Method method = (Method) _method.get(invoker);
        return method.getGenericReturnType();
      } else if (invoker instanceof GetFieldInvoker) {
        Field _field = GetFieldInvoker.class.getDeclaredField("field");
        _field.setAccessible(true);
        Field field = (Field) _field.get(invoker);
        return field.getGenericType();
      }
    } catch (NoSuchFieldException e) {
    } catch (IllegalAccessException e) {
    }
    return null;
  }

  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (reflector.hasSetter(prop.getName())) {
        MetaClass metaProp = metaClassForProperty(prop.getName());
        return metaProp.hasSetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasSetter(prop.getName());
    }
  }

  //这个属性表达式说的属性 真的可以拿到吗 (有这个get或有这个field都可以
  public boolean hasGetter(String name) {

    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      //到这层 可以拿到
      if (reflector.hasGetter(prop.getName())) {

        //检查下一层
        MetaClass metaProp = metaClassForProperty(prop);
        //递归完都有才是真的有
        return metaProp.hasGetter(prop.getChildren());
      }
      return false;
    }

      return reflector.hasGetter(prop.getName());
  }

  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  /**
   *
   * @param name 大写的属性表达式
   * @param builder  往下接 最后得到正常的属性表达式
   * @return
   */
  private StringBuilder buildProperty(String name, StringBuilder builder) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      //正常的属性名(小写开头
      String propertyName = reflector.findPropertyName(prop.getName());
      if (propertyName != null) {
        builder.append(propertyName);
        builder.append(".");
        MetaClass metaProp = metaClassForProperty(propertyName);
        //递归
        metaProp.buildProperty(prop.getChildren(), builder);
      }
    }

    //递归出口 解析完毕
    String propertyName = reflector.findPropertyName(name);
    if (propertyName != null) {
      builder.append(propertyName);
    }

    return builder;
  }

  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
