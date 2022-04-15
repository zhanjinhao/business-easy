package cn.addenda.businesseasy.cdc;

import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.statement.Curd;
import cn.addenda.ro.grammar.ast.update.Update;

/**
 * @Author ISJINHAO
 * @Date 2022/4/9 15:25
 */
public class CdcSqlUtil {

    private CdcSqlUtil() {
    }

    public static String extractDmlTableName(String sql) {

        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);

        Curd parse = curdParser.parse();
        if (parse instanceof Update) {
            return (String) ((Update) parse).getTableName().getLiteral();
        } else if (parse instanceof Insert) {
            return (String) ((Insert) parse).getTableName().getLiteral();
        } else if (parse instanceof Delete) {
            return (String) ((Delete) parse).getTableName().getLiteral();
        }

        return null;
    }

}
