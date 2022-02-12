package cn.addenda.businesseasy.bo;

import java.lang.reflect.Method;

/**
 * @Author ISJINHAO
 * @Date 2022/2/12 13:14
 */
public class ConvertSRStatusTest {

    public static void main(String[] args) {
        test();
    }

    public static void test() {
        Method[] methods = TestAnnotated.class.getMethods();
        for (Method method : methods) {
            if (method.getName().equals("testMethod")) {
                ConvertSRStatus annotation = method.getAnnotation(ConvertSRStatus.class);
                String[] strings = annotation.errorToDispatch();
                for (String s : strings) {
                    System.out.println(s);
                }
            }
        }
    }

    private static class TestAnnotated {
        @ConvertSRStatus(errorToDispatch = {"1:2", "3:4"})
        public void testMethod() {

        }
    }

}
