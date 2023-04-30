package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.sql.visitor.VisitorFeature;

import static com.alibaba.druid.sql.visitor.VisitorFeature.OutputUCase;

/**
 * @author addenda
 * @datetime 2023/4/28 21:11
 */
public class DruidSQLUtils extends SQLUtils {

    public static String toLowerCaseSQL(SQLObject sqlObject) {
        StringBuilder out = new StringBuilder();
        SQLASTOutputVisitor visitor = new MySqlOutputVisitor(out, false);
        FormatOption formatOption = new FormatOption(false, true);

        visitor.setUppCase(formatOption.isUppCase());
        // 兼容druid某些版本setUppCase不生效的问题
        visitor.config(OutputUCase, false);

        visitor.setParameterized(formatOption.isParameterized());

        int featuresValue = VisitorFeature.of(
                VisitorFeature.OutputPrettyFormat
        );

        visitor.setFeatures(featuresValue);
        sqlObject.accept(visitor);
        return out.toString();
    }

}
