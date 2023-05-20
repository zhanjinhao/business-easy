package cn.addenda.businesseasy.jdbc.baseentity;

import cn.addenda.businesseasy.jdbc.SqlReader;
import cn.addenda.businesseasy.jdbc.interceptor.baseentity.DefaultBaseEntitySource;
import cn.addenda.businesseasy.jdbc.interceptor.baseentity.DruidBaseEntityRewriter;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/5/7 14:29
 */
public class DruidBaseEntityRewriterTest {

    private static String[] sqls = new String[]{

    };

    @Test
    public void test1() {
        for (String sql : SqlReader.read("src/test/resources/select.test", sqls)) {
            System.out.println("------------------------------------------------------------------------------");
            DruidBaseEntityRewriter druidBaseEntityRewriter =
                    new DruidBaseEntityRewriter(null, null, new DefaultBaseEntitySource());
            String s = druidBaseEntityRewriter.rewriteSelectSql(sql, null);
            System.out.println(s);
        }
    }


}
