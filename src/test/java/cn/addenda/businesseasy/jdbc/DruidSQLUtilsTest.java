package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/5/14 15:53
 */
public class DruidSQLUtilsTest {

    @Test
    public void test1() {
        String sql1 = "insert a (name, age) values('be', 18)";
        String sql2 = "insert a (name, age) VALUES('be', 18)";

        System.out.println(DruidSQLUtils.removeEnter(sql1));
        System.out.println(DruidSQLUtils.removeEnter(sql2));
        Assert.assertEquals(DruidSQLUtils.removeEnter(sql1), DruidSQLUtils.removeEnter(sql2));
    }

}
