package cn.addenda.businesseasy.asynctask;

/**
 * @author 01395265
 * @date 2022/5/31
 */
public class ResultTest {

    public static void main(String[] args) {

        final TernaryResult<String, String, String> ternaryResult = new TernaryResult<>("r1", "r2", "r3");
        final QuaternionResult<String, String, String, String> quaternionResult = new QuaternionResult<>();
        final BinaryResult<String, String> binaryResult = new BinaryResult<>();

        QuaternionResult<String, String, String, String> merge1 = quaternionResult.merge(ternaryResult);
        System.out.println(merge1);

        final BinaryResult<String, String> merge2 = binaryResult.merge(ternaryResult);
        System.out.println(merge2);

    }

}
