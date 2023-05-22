package cn.addenda.businesseasy.jdbc;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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

    @Test
    public void test2() {
//        SQLExpr sqlExpr = DruidSQLUtils.objectToSQLExpr(LocalDateTime.now());
//        System.out.println(DruidSQLUtils.toLowerCaseSQL(sqlExpr));
        List<SQLStatement> sqlStatements = DruidSQLUtils.parseStatements("select now() from dual", DbType.mysql);

        SQLExpr sqlExpr = DruidSQLUtils.objectToSQLExpr("now(3)");
        System.out.println(DruidSQLUtils.toLowerCaseSQL(sqlExpr));
    }

}
