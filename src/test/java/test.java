import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: panda
 * @Date: 2018/4/24 上午 10:14
 */
public class test {
    @Test
    public void test() throws IOException {

        //遍历5个类加载器 总之要拿到输入流
        InputStream inputStream = Resources.getResourceAsStream("resources/mybatis-config.xml");

        //解析xml放入Configuration  从Configuration得到DefaultSqlSessionFactory
        SqlSessionFactory build = new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession sqlSession = build.openSession();
        Object result = sqlSession.selectOne("select * from user");
    }
}
