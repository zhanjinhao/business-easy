package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.IdentifierExistsVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.SelectItemIdentifierExistsVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.ViewToTableVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.Assert;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/3 20:55
 */
class SelectItemIdentifierExistsVisitorTest {

    private static String[] sqls = new String[]{
    };

    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        for (String sql : SqlReader.read("src/test/resources/selectitemidentifier_select.test", sqls)) {
            String source = sql;
            int i = source.lastIndexOf(";");
            sql = source.substring(0, i);
            boolean flag = Boolean.valueOf(source.substring(i + 1));
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            SQLStatement sqlStatement = sqlStatements.get(0);
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println();
            IdentifierExistsVisitor identifierExistsVisitor = new SelectItemIdentifierExistsVisitor("a");
            sqlStatement.accept(new ViewToTableVisitor());
            sqlStatement.accept(identifierExistsVisitor);
            boolean exists = identifierExistsVisitor.isExists();
            if (exists == flag) {
                System.out.println(source + " : " + exists + ":" + identifierExistsVisitor.getAmbiguousInfo());
            } else {
                System.err.println(source + " : " + exists + ":" + identifierExistsVisitor.getAmbiguousInfo());
                Assert.assertEquals(flag, exists);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
