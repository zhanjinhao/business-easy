package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.ViewToTableVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/3 20:55
 */
public class ViewToTableVisitorTest {
    private static String[] sqls = new String[]{

    };

    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        for (String sql : SqlReader.read("src/test/resources/select.test", sqls)) {
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            SQLStatement sqlStatement = sqlStatements.get(0);
            System.out.println(sql);
            sqlStatement.accept(new ViewToTableVisitor());
        }
    }

}
