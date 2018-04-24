package org.apache.ibatis.plugin;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.Properties;
/*

MyBatis 允许使用插件来拦截的方法调用包括：

        Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
        ParameterHandler (getParameterObject, setParameters)
        ResultSetHandler (handleResultSets, handleOutputParameters)
        StatementHandler (prepare, parameterize, batch, update, query)

*/

/**
 * @Author: panda
 * @Date: 2018/4/24 上午 11:40
 */
@Intercepts({@Signature(//指定想要拦截的方法签名
        type = Executor.class,//Executor 是负责执行低层映射语句的内部对象
        method = "update",
        args = {MappedStatement.class, Object.class})})
public class ExamplePlugin implements Interceptor {
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    public void setProperties(Properties properties) {
    }

}

/*
这样配置生效
会拦截在 Executor 实例中所有的 “update” 方法调用
<!-- mybatis-config.xml -->

    <plugins>
        <plugin interceptor="org.mybatis.example.ExamplePlugin">
             <property name="someProperty" value="100"/>
        </plugin>
    </plugins>

*/
