package cn.addenda.businesseasy.spring;

import cn.addenda.businesseasy.result.DeclareConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author ISJINHAO
 * @Date 2022/3/2 19:42
 */
public class ValueResolverUtilTest {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DeclareConfiguration.class, ValueResolverUtil.class);

        context.refresh();

        ValueResolverUtil valueResolverUtil = context.getBean(ValueResolverUtil.class);
        System.out.println(valueResolverUtil.resolveDollarPlaceholder("${db.url}"));
        System.out.println(valueResolverUtil.resolveFromContext("${db.url}"));
        System.out.println(valueResolverUtil.resolveHashPlaceholder("#{db.url}"));

        context.close();

    }

}
