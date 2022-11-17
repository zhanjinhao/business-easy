package cn.addenda.businesseasy.json;

import cn.addenda.businesseasy.util.BEDateUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;

public class LocalDateStrDeSerializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode jsonNode = jp.getCodec().readTree(jp);
        final String s = jsonNode.asText();
        return BEDateUtils.parseLd(s, BEDateUtils.YMD_FORMATTER);
    }
}
