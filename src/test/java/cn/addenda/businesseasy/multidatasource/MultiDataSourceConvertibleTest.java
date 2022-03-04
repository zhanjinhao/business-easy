package cn.addenda.businesseasy.multidatasource;

import cn.addenda.businesseasy.result.ServiceResultAdvisor;
import cn.addenda.businesseasy.result.TxTest;
import cn.addenda.businesseasy.result.TxTestService;
import cn.addenda.businesseasy.result.TxTestServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author ISJINHAO
 * @Date 2022/2/26 23:00
 */
public class MultiDataSourceConvertibleTest {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(MultiDataSourceDeclareConfiguration.class, TxTestServiceImpl.class);

        context.refresh();

        TxTestService txTestService = context.getBean(TxTestService.class);

        ServiceResultAdvisor bean = context.getBean(ServiceResultAdvisor.class);
        System.out.println(bean);

        System.out.println(txTestService.insert(new TxTest("springDeclare11111111111111111111111", "aha44")));

        context.close();
    }

}
