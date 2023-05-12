package cn.addenda.businesseasy.jdbc;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import java.util.List;

/**
 * @author addenda
 * @since 2023/5/6 11:35
 */
public class DruidSelectJoinTest {

    public static void main(String[] args) {

        String sql =
            "select * from (select a from t1 left join t2 right join t3 where t1.a > c limit 1000"
                + " union all "
                + "select a from t11 left join t22 right join t33 where t11.a > c) t limit 1999" ;

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement);

    }

}
