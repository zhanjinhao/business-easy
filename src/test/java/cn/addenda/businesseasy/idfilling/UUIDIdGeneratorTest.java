package cn.addenda.businesseasy.idfilling;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:34
 */
public class UUIDIdGeneratorTest {


    public static void main(String[] args) {
        UUIDIdGenerator idGenerator = new UUIDIdGenerator();
        System.out.println(idGenerator.nextSqc("tCourse"));
    }

}
