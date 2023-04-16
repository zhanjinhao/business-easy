package cn.addenda.businesseasy.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2023/2/17 9:51
 */
public class BEStackTraceUtils {

    private BEStackTraceUtils() {
    }

    private static final Set<String> DEFAULT_FILTER_PREFIX_SET;

    static {
        Set<String> set = new HashSet<>();
        set.add(Thread.class.getName());
        set.add(BEStackTraceUtils.class.getName());
        DEFAULT_FILTER_PREFIX_SET = Collections.unmodifiableSet(set);
    }

    /**
     * @param useSimpleClassName 是否按简写的类名输出
     * @param filterClassNames 全类名
     */
    public static String getCallerInfo(boolean useSimpleClassName, String... filterClassNames) {
        StackTraceElement stackTraceElement = determineStackTraceElement(filterClassNames);
        String className = stackTraceElement.getClassName();
        String methodName = stackTraceElement.getMethodName();
        if (useSimpleClassName) {
            className = extractSimpleClassName(className);
        }
        return className + "#" + methodName;
    }

    /**
     * @param useSimpleClassName 是否按简写的类名输出
     */
    public static String getCallerInfo(boolean useSimpleClassName) {
        return getCallerInfo(useSimpleClassName, null);
    }

    public static String getCallerInfo() {
        return getCallerInfo(true);
    }

    /**
     * @param useSimpleClassName 是否按简写的类名输出
     * @param filterClassNames 全类名
     */
    public static String getDetailedCallerInfo(boolean useSimpleClassName, String... filterClassNames) {
        StackTraceElement stackTraceElement = determineStackTraceElement(filterClassNames);
        String className = stackTraceElement.getClassName();
        String fileName = stackTraceElement.getFileName();
        int lineNumber = stackTraceElement.getLineNumber();
        String methodName = stackTraceElement.getMethodName();
        if (useSimpleClassName) {
            className = extractSimpleClassName(className);
        }
        return "location: [" + fileName + ": " + lineNumber + "], method: [" + className + "#" + methodName + "]";
    }

    /**
     * @param useSimpleClassName 是否按简写的类名输出
     */
    public static String getDetailedCallerInfo(boolean useSimpleClassName) {
        return getDetailedCallerInfo(useSimpleClassName, null);
    }

    public static String getDetailedCallerInfo() {
        return getDetailedCallerInfo(true);
    }

    private static StackTraceElement determineStackTraceElement(String... filterClassNames) {
        Set<String> filterClassNameSet = DEFAULT_FILTER_PREFIX_SET;
        if (filterClassNames != null) {
            filterClassNameSet = new HashSet<>(DEFAULT_FILTER_PREFIX_SET);
            filterClassNameSet.addAll(Arrays.stream(filterClassNames).collect(Collectors.toList()));
        }
        return determineStackTraceElement(Thread.currentThread().getStackTrace(), filterClassNameSet);
    }

    private static StackTraceElement determineStackTraceElement(StackTraceElement[] stackTrace, Set<String> filterClassNameSet) {
        for (StackTraceElement stackTraceElement : stackTrace) {
            String className = stackTraceElement.getClassName();
            boolean flag = false;
            for (String filterClassName : filterClassNameSet) {
                if (className.equals(filterClassName)) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                return stackTraceElement;
            }
        }
        throw new BEUtilsException("unexpected exception!  ");
    }

    private static String extractSimpleClassName(String className) {
        StringBuilder simpleClassName = new StringBuilder();
        for (int i = className.length() - 1; i > -1; i--) {
            char c = className.charAt(i);
            if (c == '.') {
                break;
            }
            simpleClassName.append(c);
        }
        return simpleClassName.reverse().toString();
    }

}
