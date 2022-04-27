package cn.addenda.businesseasy.cdc.domain;

import java.time.LocalDateTime;

/**
 * @Author ISJINHAO
 * @Date 2022/4/8 19:08
 */
public class ChangeEntity {

    private long id;

    private String tableName;

    private String tableChange;

    private LocalDateTime createTime;

    public ChangeEntity() {
    }

    public ChangeEntity(String tableName, String tableChange, LocalDateTime createTime) {
        this.tableName = tableName;
        this.tableChange = tableChange;
        this.createTime = createTime;
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

    public String getTableChange() {
        return tableChange;
    }

    public void setTableChange(String tableChange) {
        this.tableChange = tableChange;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "ChangeEntity{" +
                "id=" + id +
                ", tableName='" + tableName + '\'' +
                ", tableChange='" + tableChange + '\'' +
                ", createTime=" + createTime +
                '}';
    }

}
