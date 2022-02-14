package cn.addenda.businesseasy.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:37
 */
public class BEDateUtil {

    private BEDateUtil() {
        throw new BEUtilException("工具类不可实例化！");
    }

    private static final ZoneId defaultZoneId;

    static {
        String property = System.getProperty("business.easy.timezone");
        if (property == null || property.length() == 0) {
            defaultZoneId = ZoneId.systemDefault();
        } else {
            try {
                defaultZoneId = ZoneId.of(property);
            } catch (Exception e) {
                throw new BEUtilException("初始化BEDateUtil#defaultZoneId出错，business.easy.timezone value:" + property);
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
        Instant instant = Instant.ofEpochMilli(timestamp * 1000);
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

}
