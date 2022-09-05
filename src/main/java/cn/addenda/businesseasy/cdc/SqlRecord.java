package cn.addenda.businesseasy.cdc;

/**
 * @author addenda
 * @datetime 2022/9/3 16:28
 */
public class SqlRecord {

    public static final String TYPE_INSERT = "I";
    public static final String TYPE_UPDATE = "U";
    public static final String TYPE_DELETE = "D";

    private final String executableSql;

    private final String type;

    private final String serialNum;

    private final String records;

    public SqlRecord(String executableSql, String type, String serialNum, String records) {
        this.executableSql = executableSql;
        this.type = type;
        this.serialNum = serialNum;
        this.records = records;
    }

    public String getExecutableSql() {
        return executableSql;
    }

    public String getType() {
        return type;
    }

    public String getRecords() {
        return records;
    }

    public String getSerialNum() {
        return serialNum;
    }
}
