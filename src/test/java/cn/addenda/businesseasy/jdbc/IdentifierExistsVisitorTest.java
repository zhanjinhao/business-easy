package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.IdentifierExistsVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/5/3 20:55
 */
public class IdentifierExistsVisitorTest {

    private static String[] sqls = new String[]{
    };

    @Test
    public void test1() {
        for (String sql : SqlReader.read("src/test/resources/identifier_select.test", sqls)) {
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
            IdentifierExistsVisitor identifierExistsVisitor = new IdentifierExistsVisitor(sql, "a");
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
