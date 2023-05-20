package cn.addenda.businesseasy.jdbc.dynamicsql;

import cn.addenda.businesseasy.jdbc.SqlReader;
import cn.addenda.businesseasy.jdbc.interceptor.*;
import cn.addenda.businesseasy.jdbc.interceptor.dynamicsql.DruidDynamicSQLAssembler;
import cn.addenda.businesseasy.jdbc.interceptor.dynamicsql.DynamicSQLAssembler;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/13 12:41
 */
public class DynamicSQLAssemblerInsertAdditemTest {

    private static String[] sqls = new String[]{
    };

    @Test
    public void test1() {
        String[] read = SqlReader.read("src/test/resources/cn/addenda/businesseasy/jdbc/interceptor/dynamicsql/sqlassemblerinsertadditem.test", sqls);
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
            DynamicSQLAssembler druidDynamicSQLAssembler = new DruidDynamicSQLAssembler();
            Item item = new Item("if_del", 0);
            String s = druidDynamicSQLAssembler.insertAddItem(DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)), null, item,
                    InsertSelectAddItemMode.ITEM, false, UpdateItemMode.NOT_NULL);
            sqlStatements = SQLUtils.parseStatements(s, DbType.mysql);
            List<SQLStatement> expectSqlStatements = SQLUtils.parseStatements(expect, DbType.mysql);
            Assert.assertEquals(DruidSQLUtils.toLowerCaseSQL(expectSqlStatements.get(0)).replaceAll("\\s+", ""),
                    DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)).replaceAll("\\s+", ""));

        }
    }

}
