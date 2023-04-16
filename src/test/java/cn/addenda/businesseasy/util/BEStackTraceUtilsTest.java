package cn.addenda.businesseasy.util;

/**
 * @author addenda
 * @datetime 2023/3/2 19:29
 */
public class BEStackTraceUtilsTest {


    public static void main(String[] args) {

        System.out.println(BEStackTraceUtils.getCallerInfo());
        System.out.println(BEStackTraceUtils.getDetailedCallerInfo());
    }

}
