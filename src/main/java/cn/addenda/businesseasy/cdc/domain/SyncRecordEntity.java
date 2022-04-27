package cn.addenda.businesseasy.cdc.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @Author ISJINHAO
 * @Date 2022/4/8 19:13
 */
public class SyncRecordEntity {

    private long id;

    private String tableName;

    private String syncName;

    private long next;

    private LocalDateTime createTime;

    private LocalDateTime modifyTime;

    public SyncRecordEntity() {
    }

    public SyncRecordEntity(String tableName, String syncName) {
        this.tableName = tableName;
        this.syncName = syncName;
    }

    public SyncRecordEntity(String tableName, String syncName, long next) {
        this.tableName = tableName;
        this.syncName = syncName;
        this.next = next;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public long getNext() {
        return next;
    }

    public void setNext(long next) {
        this.next = next;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(LocalDateTime modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getSyncName() {
        return syncName;
    }

    public void setSyncName(String syncName) {
        this.syncName = syncName;
    }

    @Override
    public String toString() {
        return "SyncRecordEntity{" +
                "id=" + id +
                ", tableName='" + tableName + '\'' +
                ", syncName='" + syncName + '\'' +
                ", next=" + next +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncRecordEntity that = (SyncRecordEntity) o;
        return Objects.equals(tableName, that.tableName) && Objects.equals(syncName, that.syncName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, syncName);
    }

}

