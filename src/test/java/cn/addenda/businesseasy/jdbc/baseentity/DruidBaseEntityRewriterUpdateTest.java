package cn.addenda.businesseasy.jdbc.baseentity;

import cn.addenda.businesseasy.jdbc.SqlReader;
import cn.addenda.businesseasy.jdbc.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.visitor.item.UpdateItemMode;
import cn.addenda.businesseasy.jdbc.interceptor.baseentity.BaseEntityRewriter;
import cn.addenda.businesseasy.jdbc.interceptor.baseentity.DruidBaseEntityRewriter;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/14 17:07
 */
public class DruidBaseEntityRewriterUpdateTest {

    private static String[] sqls = new String[]{
    };

    @Test
    public void test1() {
        String[] read = SqlReader.read(
                "src/test/resources/cn/addenda/businesseasy/jdbc/interceptor/baseentity/baseentityupdate.test", sqls);
        for (int line = 0; line < read.length; line++) {
            String sql = read[line];
            String source = sql;
            int i = source.lastIndexOf(";");
            sql = source.substring(0, i);
            String expect = source.substring(i + 1);
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            if (sqlStatements.size() == 0) {
                continue;
            }
            System.out.println(line + " : ------------------------------------------------------------------------------------");
            BaseEntityRewriter baseEntityRewriter = new DruidBaseEntityRewriter();
            String s = baseEntityRewriter.rewriteUpdateSql(DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)),
                    UpdateItemMode.NOT_NULL, false);
            sqlStatements = SQLUtils.parseStatements(s, DbType.mysql);
            List<SQLStatement> expectSqlStatements = SQLUtils.parseStatements(expect, DbType.mysql);
            Assert.assertEquals(
                    DruidSQLUtils.toLowerCaseSQL(expectSqlStatements.get(0)).replaceAll("\\s+", " "),
                    DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)).replaceAll("\\s+", " "));

        }
    }
}
