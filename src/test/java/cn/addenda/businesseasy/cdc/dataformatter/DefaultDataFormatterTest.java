package cn.addenda.businesseasy.cdc.dataformatter;

import cn.addenda.businesseasy.cdc.format.DefaultDataFormatterRegistry;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author addenda
 * @datetime 2022/9/10 20:25
 */
public class DefaultDataFormatterTest {

    public static void main(String[] args) {
        DefaultDataFormatterRegistry registry = new DefaultDataFormatterRegistry();
        System.out.println(registry.typeAvailable(Long.class));


    }

}
