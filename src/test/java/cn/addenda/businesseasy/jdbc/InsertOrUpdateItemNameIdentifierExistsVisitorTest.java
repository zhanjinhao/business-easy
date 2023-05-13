package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.IdentifierExistsVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.InsertOrUpdateItemNameIdentifierExistsVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/13 21:24
 */
public class InsertOrUpdateItemNameIdentifierExistsVisitorTest {

    private static String[] sqls = new String[]{
    };

    @Test
    public void test1() {
        for (String sql : SqlReader.read("src/test/resources/insertorupdateitemidentifierexistsvisitor.test", sqls)) {
            String source = sql;
            int i = source.lastIndexOf(";");
            sql = source.substring(0, i);
            boolean flag = Boolean.parseBoolean(source.substring(i + 1));
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            if (sqlStatements.size() == 0) {
                continue;
            }
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println();
            IdentifierExistsVisitor identifierExistsVisitor = new InsertOrUpdateItemNameIdentifierExistsVisitor(sql, "a");
            identifierExistsVisitor.visit();
            boolean exists = identifierExistsVisitor.isExists();
            if (exists == flag) {
                System.out.println(source + " : " + exists + ":" + identifierExistsVisitor.getAmbiguousInfo());
            } else {
                System.err.println(source + " : " + exists + ":" + identifierExistsVisitor.getAmbiguousInfo());
                Assert.assertEquals(flag, exists);
            }

        }
    }


}
