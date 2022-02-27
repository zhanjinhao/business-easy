package cn.addenda.businesseasy.fieldfilling;

import cn.addenda.businesseasy.fieldfilling.sql.SqlUtil;
import cn.addenda.ro.grammar.ast.statement.Curd;
import cn.addenda.ro.grammar.ast.statement.Function;
import cn.addenda.ro.grammar.ast.statement.Literal;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 11:52
 */
public class SqlUtilUpdateAddEntryTest {

    static String[] sqls = new String[]{

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python') where runoob_id = 3",

            "update runoob_tbl set runoob_title = replace(runoob_title, 'c++', 'python')"

    };

    public static void main(String[] args) {

        for (int i = 0; i < sqls.length; i++) {

            Map<String, Curd> entryMap = new LinkedHashMap<>();
            entryMap.put("modify_user", new Literal(new Token(TokenType.STRING, "addenda")));
            entryMap.put("modify_time", new Function(new Token(TokenType.IDENTIFIER, "now")));
            entryMap.put("remark", new Literal(new Token(TokenType.STRING, "zhanjinhao")));
            System.out.println(SqlUtil.updateAddEntry(sqls[i], entryMap));
        }

    }
}
