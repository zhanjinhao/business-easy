package cn.addenda.businesseasy.spring;

import cn.addenda.businesseasy.result.DeclareConfiguration;
import cn.addenda.businesseasy.result.TxTest;
import cn.addenda.businesseasy.result.TxTestService;
import cn.addenda.businesseasy.result.TxTestServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author ISJINHAO
 * @Date 2022/3/2 18:18
 */
public class ApplicationContextUtilTest {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DeclareConfiguration.class, ApplicationContextUtil.class);

        context.refresh();

        ApplicationContextUtil applicationContextUtil = context.getBean(ApplicationContextUtil.class);
        TxTestService txTestService = new TxTestServiceImpl();
        applicationContextUtil.autowiredInstanceByType(txTestService);

        applicationContextUtil.registerSingletonBean(txTestService);

        txTestService = applicationContextUtil.getBean(TxTestService.class);


        System.out.println(txTestService.insert(new TxTest("springDeclare11111111111111111111111", "aha44")));

        context.close();

    }

}
