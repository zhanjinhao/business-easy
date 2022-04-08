package cn.addenda.businesseasy.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:38
 */
public class BEJsonUtil {

    private static final ObjectMapper BASIC = new ObjectMapper();
    private static final ObjectMapper TRIM_NULL = new ObjectMapper();

    static {
        BASIC.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        BASIC.registerModule(new JavaTimeModule());

        TRIM_NULL.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TRIM_NULL.setSerializationInclusion(Include.NON_NULL);
        TRIM_NULL.registerModule(new JavaTimeModule());
    }

    private static class JavaTimeModule extends SimpleModule {

        public JavaTimeModule() {
            addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
                    if (value == null) {
                        jgen.writeString("");
                        return;
                    }
                    jgen.writeNumber(BEDateUtil.localDateTimeToTimestamp(value));
                }
            });
            addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                    String text = jp.getText();
                    if ("".equals(text)) {
                        return null;
                    }
                    return BEDateUtil.timestampToLocalDateTime(Long.parseLong(text));
                }
            });
        }
    }

    private static String objectToString(ObjectMapper objectMapper, Object input) {
        if (null == input) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new BEUtilException("Json转换异常[Object to String].");
        }
    }

    /**
     * @param input 待转换对象
     * @return 如果转换失败返回<code>null</code>，否则返回转换后的json字符串
     */
    public static String objectToString(Object input) {
        if (null == input) {
            return null;
        }

        return objectToString(BASIC, input);
    }

    /**
     * @param input 待转换对象
     * @return 如果转换失败返回<code>null</code>，否则返回转换后的json字符串
     */
    public static String objectToString(Object input, String... ignoreProperties) {
        if (null == input) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializerFactory(objectMapper.getSerializerFactory()
            .withSerializerModifier(new IgnorePropertiesBeanSerializerModifier(Arrays.stream(ignoreProperties).collect(Collectors.toSet()))));
        return objectToString(objectMapper, input);
    }

    static class IgnorePropertiesBeanSerializerModifier extends BeanSerializerModifier {

        private final Set<String> ignoreProperties;

        public IgnorePropertiesBeanSerializerModifier(Set<String> ignoreProperties) {
            this.ignoreProperties = ignoreProperties;
        }

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
            beanProperties.removeIf(writer -> ignoreProperties.contains(writer.getName()));
            return beanProperties;
        }
    }

    /**
     * 转换Json String为指定类
     *
     * @param inputJson 待转换json字符串
     * @param targetType 目标类型
     * @param <T> 目标类
     * @return 目标类
     */
    public static <T> T stringToObject(String inputJson, TypeReference<T> targetType) {
        if (!StringUtils.hasText(inputJson)) {
            return null;
        }

        return stringToObject(BASIC, inputJson, targetType);
    }

    public static <T> T stringToObject(ObjectMapper objectMapper, String inputJson, TypeReference<T> targetType) {
        if (!StringUtils.hasText(inputJson)) {
            return null;
        }

        try {
            return objectMapper.readValue(inputJson, targetType);
        } catch (Exception e) {
            throw new BEUtilException("Json转换异常[String to Type].", e);
        }
    }

    public static String formatJson(String content) {
        try {
            Object obj = BASIC.readValue(content, Object.class);
            return BASIC.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BEUtilException("格式化Json出错，content: " + content, e);
        }
    }

    public static String trimNull(String content) {
        try {
            Object o = BASIC.readValue(content, Object.class);
            return objectToString(TRIM_NULL, o);
        } catch (JsonProcessingException e) {
            throw new BEUtilException("去除Json的空值时出错！", e);
        }
    }

    public static void main(String[] args) {
        String content = "{\"context\":\"\",\"layer\":\"Drivingdata\"}";
        System.out.println(formatJson(content));
    }

}
