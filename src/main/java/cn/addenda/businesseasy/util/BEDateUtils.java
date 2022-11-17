package cn.addenda.businesseasy.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:37
 */
public class BEDateUtils {

    public static final String FULL_FORMATTER = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String YMD_FORMATTER = "yyyy-MM-dd";
    public static final String HMSS_FORMATTER = "HH:mm:ss.SSS";
    private static final Map<String, DateTimeFormatter> formatterMap = new ConcurrentHashMap<>();

    private BEDateUtils() {
        throw new BEUtilsException("工具类不可实例化！");
    }

    private static final ZoneId defaultZoneId;

    static {
        String property = System.getProperty("businesseasy.timezone");
        if (property == null || property.length() == 0) {
            defaultZoneId = ZoneId.systemDefault();
        } else {
            try {
                defaultZoneId = ZoneId.of(property);
            } catch (Exception e) {
                throw new BEUtilsException("初始化BEDateUtil#defaultZoneId出错，businesseasy.timezone value:" + property);
            }
        }
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        return dateToLocalDateTime(date, defaultZoneId);
    }

    public static LocalDateTime dateToLocalDateTime(Date date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        if (zoneId == null) {
            zoneId = defaultZoneId;
        }
        Instant instant = date.toInstant();
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public static Long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        return localDateTimeToTimestamp(localDateTime, defaultZoneId);
    }

    public static Long localDateTimeToTimestamp(LocalDateTime localDateTime, ZoneId zoneId) {
        if (localDateTime == null) {
            return null;
        }
        if (zoneId == null) {
            zoneId = defaultZoneId;
        }
        return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    public static LocalDateTime timestampToLocalDateTime(Long timestamp) {
        return timestampToLocalDateTime(timestamp, defaultZoneId);
    }

    public static LocalDateTime timestampToLocalDateTime(Long timestamp, ZoneId zoneId) {
        if (timestamp == null) {
            return null;
        }
        if (zoneId == null) {
            zoneId = defaultZoneId;
        }
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return localDateTimeToDate(localDateTime, defaultZoneId);
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime, ZoneId zoneId) {
        if (localDateTime == null) {
            return null;
        }
        if (zoneId == null) {
            zoneId = defaultZoneId;
        }
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        Instant instant = zonedDateTime.toInstant();
        return Date.from(instant);
    }

    public static String format(LocalDateTime localDateTime, String formatter) {
        DateTimeFormatter dateTimeFormatter =
                formatterMap.computeIfAbsent(formatter, s -> DateTimeFormatter.ofPattern(formatter));
        return dateTimeFormatter.format(localDateTime);
    }

    public static String format(LocalDate localDate, String formatter) {
        DateTimeFormatter dateTimeFormatter =
                formatterMap.computeIfAbsent(formatter, s -> DateTimeFormatter.ofPattern(formatter));
        return dateTimeFormatter.format(localDate);
    }

    public static String format(LocalTime localTime, String formatter) {
        DateTimeFormatter dateTimeFormatter =
                formatterMap.computeIfAbsent(formatter, s -> DateTimeFormatter.ofPattern(formatter));
        return dateTimeFormatter.format(localTime);
    }

    public static LocalDateTime parseLdt(String localDateTime, String formatter) {
        DateTimeFormatter dateTimeFormatter =
                formatterMap.computeIfAbsent(formatter, s -> DateTimeFormatter.ofPattern(formatter));
        return LocalDateTime.parse(localDateTime, dateTimeFormatter);
    }

    public static LocalDate parseLd(String localDate, String formatter) {
        DateTimeFormatter dateTimeFormatter =
                formatterMap.computeIfAbsent(formatter, s -> DateTimeFormatter.ofPattern(formatter));
        return LocalDate.parse(localDate, dateTimeFormatter);
    }

    public static LocalTime parseLt(String localTime, String formatter) {
        DateTimeFormatter dateTimeFormatter =
                formatterMap.computeIfAbsent(formatter, s -> DateTimeFormatter.ofPattern(formatter));
        return LocalTime.parse(localTime, dateTimeFormatter);
    }

}
