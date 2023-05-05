package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.IdentifierExistsVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.ViewToTableVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/3 20:55
 */
public class IdentifierExistsVisitorTest {
    private static String[] sqls = new String[]{
            " select  case a  when b  + 1  then '1' when b  + 2  then '2' else '3' end as A from  (  select 2 as a, 1 as b from dual   where a  = ?  )  A  where a  = ?"
    };

    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        for (String sql : SqlReader.read("src/test/resources/select.test", sqls)) {
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            SQLStatement sqlStatement = sqlStatements.get(0);
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println(sql);
            IdentifierExistsVisitor identifierExistsVisitor = new IdentifierExistsVisitor("a");
            sqlStatement.accept(new ViewToTableVisitor());
            sqlStatement.accept(identifierExistsVisitor);
            System.out.println(identifierExistsVisitor.isExists());
        }
    }

}
