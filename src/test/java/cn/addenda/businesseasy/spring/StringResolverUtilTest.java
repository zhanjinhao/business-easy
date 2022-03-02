package cn.addenda.businesseasy.spring;

import cn.addenda.businesseasy.result.DeclareConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author ISJINHAO
 * @Date 2022/3/2 19:42
 */
public class StringResolverUtilTest {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DeclareConfiguration.class, StringResolverUtil.class);

        context.refresh();

        StringResolverUtil stringResolverUtil = context.getBean(StringResolverUtil.class);
        System.out.println(stringResolverUtil.resolverPlaceholderByDollarHelper("${db.url}"));
        System.out.println(stringResolverUtil.resolveFromContext("${db.url}"));
        System.out.println(stringResolverUtil.replacePlaceholdersByHashHelper("#{db.url}"));

        context.close();

    }

}
