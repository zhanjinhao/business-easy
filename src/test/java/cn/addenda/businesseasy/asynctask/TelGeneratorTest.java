package cn.addenda.businesseasy.asynctask;

/**
 * @author 01395265
 * @date 2022/5/25
 */
public class TelGeneratorTest {

    public static void main(String[] args) throws InterruptedException {

        final TelGenerator telGenerator = new TelGenerator(1, 2, 3, 4, 4, 2, 1);

        final long start = System.currentTimeMillis();
        final String generate = telGenerator.generate();
        System.out.println("tel content: " + generate);
        System.out.println("cost : " + (System.currentTimeMillis() - start) + "ms");

    }

}
