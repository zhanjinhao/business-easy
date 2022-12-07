package cn.addenda.businesseasy.util;

import org.springframework.util.Assert;

/**
 * @author addenda
 * @datetime 2022/11/28 22:49
 */
public class BEAssertUtils {

    private BEAssertUtils() {

    }

    public static void notNull(Object condition) {
        Assert.notNull(condition, "parameter cannot be null. ");
    }

    public static void notNull(Object condition, String filedName) {
        Assert.notNull(condition, filedName + " cannot be null. ");
    }

    public static void notModified(Object condition, String filedName) {
        Assert.isNull(condition, filedName + " cannot be modified. ");
    }

    public static void notApplied(Object condition, String filedName) {
        Assert.isNull(condition, filedName + " cannot be applied. ");
    }

}
