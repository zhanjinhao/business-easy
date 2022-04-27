package cn.addenda.businesseasy.cdc.domain;

import java.time.LocalDateTime;

/**
 * @Author ISJINHAO
 * @Date 2022/4/10 20:14
 */
public class SyncNameEntity {

    private long id;
    private String syncName;
    private LocalDateTime createTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSyncName() {
        return syncName;
    }

    public void setSyncName(String syncName) {
        this.syncName = syncName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "SyncNameEntity{" +
                "id=" + id +
                ", syncName='" + syncName + '\'' +
                ", createTime=" + createTime +
                '}';
    }

}
