package cn.addenda.businesseasy.propertyrefresh;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * @Author ISJINHAO
 * @Date 2022/4/5 15:06
 */
public class PropertyRefreshApplication {
    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(PropertyRefreshTestConfiguration.class, BusinessServiceImpl.class);

        context.refresh();

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        context.close();
    }
}
