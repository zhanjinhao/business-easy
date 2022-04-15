package cn.addenda.businesseasy.cdc;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author ISJINHAO
 * @Date 2022/4/13 20:41
 */
public class CdcSpringSimpleBootstrap {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(CdcConfiguration.class, CdcTestServiceImpl.class);

        context.refresh();

        CdcTestService cdcTestService = context.getBean(CdcTestService.class);

        cdcTestService.insert();

        context.close();

    }

}
