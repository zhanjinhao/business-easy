package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.visitor.identifier.ExactIdentifierVisitor;
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
public class ExactIdentifierVisitorTest {

    private static String[] sqls = new String[]{
    };

    @Test
    public void test1() {
        for (String sql : SqlReader.read("src/test/resources/exactidentifier.test", sqls)) {
            String source = sql;
            int i = source.lastIndexOf(";");
            sql = source.substring(0, i);
            boolean flag = Boolean.parseBoolean(source.substring(i + 1).trim());
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            if (sqlStatements.isEmpty()) {
                continue;
            }
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println();
            ExactIdentifierVisitor identifierExistsVisitor = new ExactIdentifierVisitor(sql);
            identifierExistsVisitor.visit();
            boolean exists = identifierExistsVisitor.isExact();
            if (exists == flag) {
                System.out.println(source + " : " + exists);
            } else {
                System.err.println(source + " : " + exists);
                Assert.assertEquals(flag, exists);
            }
        }
    }

}
