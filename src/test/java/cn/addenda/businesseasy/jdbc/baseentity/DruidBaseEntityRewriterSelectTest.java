package cn.addenda.businesseasy.jdbc.baseentity;

import cn.addenda.businesseasy.jdbc.SqlReader;
import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
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
public class DruidBaseEntityRewriterSelectTest {

    private static String[] sqls = new String[]{
    };

    @Test
    public void test1() {
        String[] read = SqlReader.read(
                "src/test/resources/cn/addenda/businesseasy/jdbc/interceptor/baseentity/baseentityselect.test", sqls);
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
            String s = baseEntityRewriter.rewriteSelectSql(DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)), null);
            sqlStatements = SQLUtils.parseStatements(s, DbType.mysql);
            List<SQLStatement> expectSqlStatements = SQLUtils.parseStatements(expect, DbType.mysql);
            Assert.assertEquals(
                    DruidSQLUtils.toLowerCaseSQL(expectSqlStatements.get(0)).replaceAll("\\s+", " "),
                    DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)).replaceAll("\\s+", " "));
        }
    }

    @Test
    public void test2() {
        String sql = " select 1 from  (  select a  from dual  d1 join dual  d2 on d1.id  = d2.outer_id    )  t1  where  (  select 2 from dual   )  > t1.a; select 1, t1.d1_creator as creator, t1.d2_creator as t1_d2_creator, t1.d1_creator_name as creator_name, t1.d2_creator_name as t1_d2_creator_name , t1.d1_create_time as create_time, t1.d2_create_time as t1_d2_create_time, t1.d1_modifier as modifier, t1.d2_modifier as t1_d2_modifier, t1.d1_modifier_name as modifier_name , t1.d2_modifier_name as t1_d2_modifier_name, t1.d1_modify_time as modify_time, t1.d2_modify_time as t1_d2_modify_time, t1.d1_remark as remark, t1.d2_remark as t1_d2_remark from ( select a, d1.creator as d1_creator, d2.creator as d2_creator, d1.creator_name as d1_creator_name, d2.creator_name as d2_creator_name , d1.create_time as d1_create_time, d2.create_time as d2_create_time, d1.modifier as d1_modifier, d2.modifier as d2_modifier, d1.modifier_name as d1_modifier_name , d2.modifier_name as d2_modifier_name, d1.modify_time as d1_modify_time, d2.modify_time as d2_modify_time, d1.remark as d1_remark, d2.remark as d2_remark from dual d1 join dual d2 on d1.id = d2.outer_id ) t1 where ( select 2, dual.creator as dual_creator, dual.creator_name as dual_creator_name, dual.create_time as dual_create_time, dual.modifier as dual_modifier , dual.modifier_name as dual_modifier_name, dual.modify_time as dual_modify_time, dual.remark as dual_remark from dual ) > t1.a";
        String source = sql;
        int i = source.lastIndexOf(";");
        sql = source.substring(0, i);
        String expect = source.substring(i + 1);
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        if (sqlStatements.size() == 0) {
            return;
        }
        BaseEntityRewriter baseEntityRewriter = new DruidBaseEntityRewriter();
        String s = baseEntityRewriter.rewriteSelectSql(DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)), "t1_d1");
        sqlStatements = SQLUtils.parseStatements(s, DbType.mysql);
        List<SQLStatement> expectSqlStatements = SQLUtils.parseStatements(expect, DbType.mysql);
        Assert.assertEquals(
                DruidSQLUtils.toLowerCaseSQL(expectSqlStatements.get(0)).replaceAll("\\s+", " "),
                DruidSQLUtils.toLowerCaseSQL(sqlStatements.get(0)).replaceAll("\\s+", " "));

    }
}
