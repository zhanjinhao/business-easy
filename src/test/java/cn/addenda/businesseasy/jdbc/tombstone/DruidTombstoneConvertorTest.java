package cn.addenda.businesseasy.jdbc.tombstone;

import cn.addenda.businesseasy.jdbc.interceptor.tombstone.DruidTombstoneSqlRewriter;

/**
 * @author addenda
 * @since 2023/5/2 15:54
 */
public class DruidTombstoneConvertorTest {

    public static void main(String[] args) {
        test1();
        System.out.println("-------------------------");
        test2();
        System.out.println("-------------------------");
        test3();
        System.out.println("-------------------------");
        test4();
    }

    private static void test1() {
        String sql = "insert into A(name, age) values (a, 1)";
//        DruidTombstoneConvertor druidTombstoneConvertor = new DruidTombstoneConvertor(Arrays.asList("B"));
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter(null);
        String s = druidTombstoneConvertor.rewriteInsertSql(sql);
        System.out.println(s);
    }

    private static void test2() {
        String sql = "delete from A where c > a";
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter(null);
        String s = druidTombstoneConvertor.rewriteDeleteSql(sql);
        System.out.println(s);
    }

    private static void test3() {
        String sql = "update A set a = 1 where c = 1";
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter(null);
        String s = druidTombstoneConvertor.rewriteUpdateSql(sql);
        System.out.println(s);
    }

    private static void test4() {
        String sql = "select * from (select a from A union select a from B) t";
//        DruidTombstoneConvertor druidTombstoneConvertor = new DruidTombstoneConvertor(Arrays.asList("a", "B"));
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter(null);
        String s = druidTombstoneConvertor.rewriteSelectSql(sql);
        System.out.println(s);
    }

}
