package cn.addenda.businesseasy.jdbc.tombstone;

import cn.addenda.businesseasy.jdbc.interceptor.tombstone.DruidTombstoneSqlRewriter;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/5/2 15:54
 */
public class DruidTombstoneConvertorTest {

    @Test
    public void test1() {
        String sql = "insert into A(name, age) values (a, 1)";
//        DruidTombstoneConvertor druidTombstoneConvertor = new DruidTombstoneConvertor(Arrays.asList("B"));
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter();
        String s = druidTombstoneConvertor.rewriteInsertSql(sql, false);
        System.out.println(s);
    }

    @Test
    public void test2() {
        String sql = "delete from A where c > a";
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter();
        String s = druidTombstoneConvertor.rewriteDeleteSql(sql);
        System.out.println(s);
    }

    @Test
    public void test3() {
        String sql = "update A set a = 1 where c = 1";
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter();
        String s = druidTombstoneConvertor.rewriteUpdateSql(sql);
        System.out.println(s);
    }

    @Test
    public void test4() {
        String sql = "select * from (select a from A union select a from B) t";
//        DruidTombstoneConvertor druidTombstoneConvertor = new DruidTombstoneConvertor(Arrays.asList("a", "B"));
        DruidTombstoneSqlRewriter druidTombstoneConvertor = new DruidTombstoneSqlRewriter();
        String s = druidTombstoneConvertor.rewriteSelectSql(sql, false);
        System.out.println(s);
    }

}
