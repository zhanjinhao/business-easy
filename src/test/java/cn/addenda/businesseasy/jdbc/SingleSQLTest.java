 package cn.addenda.businesseasy.jdbc;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import java.util.List;

/**
 * @author addenda
 * @since 2023/5/6 12:18
 */
public class SingleSQLTest {

    public static void main(String[] args) {
//        test1();
//        test2();
//        test3();
//        test4();
//        test5();
        test6();
    }

    private static void test1() {
        String sql = "insert into a(name, age) values ('addenda', 12) if not exists";

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement);
    }

    private static void test2() {
        String sql = "insert into a(name, age) select bname, bage from b";

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement);
    }

    private static void test3() {
        String sql = "update a set name = 'a' where c = '10'";

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement);
    }

    private static void test4() {
        String sql = "delete from a where c = '10'";

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement);
    }

    private static void test5() {
        String sql = "insert  into table_listnames ( name, address, tele )   select *  from  (  select 'rupert', 'somewhere', '022' from dual   )  tmp  where  not exists  (  select name  from table_listnames   where name  = 'rupert'  )  limit 1";

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement);
    }

    private static void test6() {
        String sql = "insert into table_listnames (name, address, tele, if_del)\n"
            + "select *, 0 as if_del\n"
            + "from (select 'rupert', 'somewhere', '022' from (select * from c where if_del = 0) c) tmp\n"
            + "where not exists(select name\n"
            + "                 from (select * from table_listnames where if_del = 0) table_listnames\n"
            + "                 where name = 'rupert')\n"
            + "limit 1";

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement);
    }

}
