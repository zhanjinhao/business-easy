package cn.addenda.businesseasy.json;

import cn.addenda.businesseasy.util.BEDateUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;

/**
 * @author ISJINHAO
 * @date 2021/9/13
 */
public class LocalTimeStrSerializer extends JsonSerializer<LocalTime> {

    @Override
    public void serialize(LocalTime localTime, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString(BEDateUtils.format(localTime, BEDateUtils.HMSS_FORMATTER));
    }

}
