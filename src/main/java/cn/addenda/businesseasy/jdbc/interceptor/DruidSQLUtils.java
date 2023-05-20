package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.util.BEDateUtils;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Function;

import static com.alibaba.druid.sql.visitor.VisitorFeature.OutputUCase;

/**
 * @author addenda
 * @datetime 2023/4/28 21:11
 */
public class DruidSQLUtils extends SQLUtils {

    public static String toLowerCaseSQL(SQLObject sqlObject) {
        return toLowerCaseSQL(sqlObject, false);
    }

    public static String toLowerCaseSQL(SQLObject sqlObject, boolean printEnter) {
        if (sqlObject == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(out, false);

        visitor.setUppCase(false);
        // 兼容druid某些版本setUppCase不生效的问题
        visitor.config(OutputUCase, false);

        visitor.setParameterized(false);
        visitor.setPrettyFormat(printEnter);
        sqlObject.accept(visitor);
        return out.toString();
    }

    public static SQLExpr objectToSQLExpr(Object o) {
        if (o == null) {
            return new SQLNullExpr();
        } else if (o instanceof LocalDate) {
            return new SQLDateExpr(BEDateUtils.localDateToDate((LocalDate) o));
        } else if (o instanceof Time) {
            return new SQLTimeExpr((Date) o, TimeZone.getDefault());
        } else if (o instanceof LocalTime) {
            return new SQLTimeExpr(BEDateUtils.localTimeToDate((LocalTime) o), TimeZone.getDefault());
        } else if (o instanceof Date) {
            return new SQLDateTimeExpr((Date) o, TimeZone.getDefault());
        } else if (o instanceof LocalDateTime) {
            return new SQLDateTimeExpr(BEDateUtils.localDateTimeToDate((LocalDateTime) o), TimeZone.getDefault());
        } else if (o instanceof Boolean) {
            return new SQLBooleanExpr((Boolean) o);
        } else if (o instanceof Float) {
            return new SQLFloatExpr((Float) o);
        } else if (o instanceof Byte) {
            return new SQLTinyIntExpr((Byte) o);
        } else if (o instanceof Double) {
            return new SQLDoubleExpr((Double) o);
        } else if (o instanceof Long) {
            return new SQLBigIntExpr((Long) o);
        } else if (o instanceof Short) {
            return new SQLSmallIntExpr((Short) o);
        } else if (o instanceof Integer) {
            return new SQLIntegerExpr((Integer) o);
        } else if (o instanceof Number) {
            return new SQLNumberExpr((Number) o);
        } else if (o instanceof CharSequence) {
            // todo 支持函数的集合
            String text = String.valueOf(o);
            if (text.length() >= 3 && JdbcSQLUtils.isCurd(text, "now")) {
                SQLExpr param = objectToSQLExpr(Integer.valueOf(text.substring(4, text.length() - 1)));
                return new SQLMethodInvokeExpr("now", null, param);
            }
            return new SQLCharExpr(text);
        } else if (o instanceof Character) {
            return new SQLCharExpr(String.valueOf(o));
        }
        throw new UnsupportedOperationException("不支持的数据类型，class：" + o.getClass() + "， object：" + o + "。");
    }

    public static String removeEnter(String sql) {
        StringBuilder stringBuilder = new StringBuilder();
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement statement : sqlStatements) {
            stringBuilder.append(DruidSQLUtils.toLowerCaseSQL(statement)).append("\n");
        }
        return stringBuilder.toString().trim();
    }

    public static String statementMerge(String sql, Function<SQLStatement, String> function) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
        StringBuilder stringBuilder = new StringBuilder();
        for (SQLStatement sqlStatement : stmtList) {
            stringBuilder.append(function.apply(sqlStatement)).append("\n");
        }
        return stringBuilder.toString().trim();
    }

}
