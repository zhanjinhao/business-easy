package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.visitor.ViewToTableVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.Test;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/3 20:55
 */
public class ViewToTableVisitorTest {
    private static String[] sqls = new String[]{

    };

    @Test
    public void test1() {
        for (String sql : SqlReader.read("src/test/resources/select.test", sqls)) {
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            SQLStatement sqlStatement = sqlStatements.get(0);
            System.out.println(sql);
            sqlStatement.accept(ViewToTableVisitor.getInstance());
        }
    }

}
