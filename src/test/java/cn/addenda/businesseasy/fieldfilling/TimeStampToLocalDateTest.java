package cn.addenda.businesseasy.fieldfilling;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @Author ISJINHAO
 * @Date 2022/2/4 20:21
 */
public class TimeStampToLocalDateTest {

    public static void main(String[] args) {
        // 时间戳与时区无关，永远表示的是GMT+0的时间
        System.out.println(Instant.now().toEpochMilli());

        // Instant是对时间戳的抽象
        Instant instant = Instant.ofEpochMilli(1644032880484L);

        // 时间戳转为LocalDateTime，需要赋予时区
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC+0"));

        System.out.println(localDateTime);
    }

}
