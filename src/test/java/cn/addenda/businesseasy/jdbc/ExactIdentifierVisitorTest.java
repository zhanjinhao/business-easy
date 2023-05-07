package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.ExactIdentifierVisitor;
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
public class ExactIdentifierVisitorTest {

    private static String[] sqls = new String[]{
            "select #{flightId1}, 1 , orderdate , lead ( orderdate  , 1  ) over ( partition by customernumber  order by orderdate   )   as nextorderdate from orders join customers;false"
    };

    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        for (String sql : SqlReader.read("src/test/resources/exactidentifier_select.test", sqls)) {
            String source = sql;
            int i = source.lastIndexOf(";");
            sql = source.substring(0, i);
            boolean flag = Boolean.valueOf(source.substring(i + 1));
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            SQLStatement sqlStatement = sqlStatements.get(0);
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println();
            ExactIdentifierVisitor identifierExistsVisitor = new ExactIdentifierVisitor();
            sqlStatement.accept(new ViewToTableVisitor());
            sqlStatement.accept(identifierExistsVisitor);
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