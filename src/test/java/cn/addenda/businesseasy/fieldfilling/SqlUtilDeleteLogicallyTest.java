package cn.addenda.businesseasy.fieldfilling;

import cn.addenda.businesseasy.fieldfilling.sql.SqlUtil;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 11:52
 */
public class SqlUtilDeleteLogicallyTest {

    static String[] sqls = new String[]{
            "delete from score where DEGREE < 50",
            "delete from score where CREATE_TM < now()",
            "delete from score where DEGREE + 1 < 60 - 1",
            "delete from score where CREATE_TM < now() and DEGREE + 1 < 60 - 1",
            "delete from score"
    };

    public static void main(String[] args) {

        for (int i = 0; i < sqls.length; i++) {
            String s = SqlUtil.deleteLogically(sqls[i], null, null);
            System.out.println(s);
        }
    }

}
