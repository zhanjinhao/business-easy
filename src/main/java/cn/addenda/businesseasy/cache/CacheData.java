package cn.addenda.businesseasy.cache;

import cn.addenda.businesseasy.json.LocalDateTimeStrDeSerializer;
import cn.addenda.businesseasy.json.LocalDateTimeStrSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;

public class CacheData<T> {

    @JsonSerialize(using = LocalDateTimeStrSerializer.class)
    @JsonDeserialize(using = LocalDateTimeStrDeSerializer.class)
    private LocalDateTime expireTime;
    private T data;

    public CacheData() {
    }

    public CacheData(LocalDateTime expireTime, T data) {
        this.expireTime = expireTime;
        this.data = data;
    }

    public CacheData(T data) {
        this.data = data;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CacheData{" +
            "expireTime=" + expireTime +
            ", data=" + data +
            '}';
    }
}
