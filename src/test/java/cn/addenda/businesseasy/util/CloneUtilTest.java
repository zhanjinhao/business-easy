package cn.addenda.businesseasy.util;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author ISJINHAO
 * @date 2022/2/15
 */
public class CloneUtilTest {

    public static void main(String[] args) {

        List<Son> sons1 = BEArrayUtils.asArrayList(new Son("a", "1"), new Son("b", "2"), new Son("c", "2"));
        System.out.println(BECloneUtils.cloneByJDKSerialization(sons1));

        List<Son> sons2 = BEArrayUtils.asLinkedList(new Son("a", "1"), new Son("b", "2"), new Son("c", "2"));
        System.out.println(BECloneUtils.cloneByJDKSerialization(sons2));

        Set<Son> sons3 = BEArrayUtils.asHashSet(new Son("a", "1"), new Son("b", "2"), new Son("c", "2"));
        System.out.println(BECloneUtils.cloneByJDKSerialization(sons3));
    }

    static class Son implements Serializable {

        private String name;
        private String age;

        public Son(String name, String age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "Son{" +
                "name='" + name + '\'' +
                ", age='" + age + '\'' +
                '}';
        }
    }

}
