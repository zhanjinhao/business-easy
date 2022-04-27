package cn.addenda.businesseasy.fieldfilling;

import cn.addenda.businesseasy.util.BESqlUtil;
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
public class SqlUtilInsertAddEntryTest {

    static String[] sqls = new String[]{

            "insert into score(SNO, CNO, DEGREE) values (109, '3-105', 76)",

            "insert into score(SNO, CNO, DEGREE) values (109, '3-105', 76) on duplicate key update SNO = 131, CNO = '4-111', DEGREE = 99",

            "insert into score(SNO, CNO, DEGREE) values (109, '3-105', 76), (109, '3-105', 76), (109, '3-105', 76)",

            "insert into score(SNO, CNO, DEGREE) values (?, '3-105', ?), (109, ?, 76), (?, '3-105', ?)",

            "insert into score set SNO = 109, CNO = '3-105', DEGREE = 76",

            "insert into score set SNO = 109, CNO = date_format(now(), 'yyyy-dd-mm'), DEGREE = DEGREE + 9 * 3",

            "insert ignore into score set SNO = 109, CNO = '3-105', DEGREE = 76",

            "insert ignore into score set SNO = ?, CNO = '3-105', DEGREE = ?",

            "insert ignore into score set SNO = '1387398', CNO = #{cno}, DEGREE = ?",

            "insert into table_listnames (name, address, tele) " +
                    "select * from (select 'rupert', 'somewhere', '022' from dual) tmp " +
                    "where not exists ( " +
                    "    select name from table_listnames where name = 'rupert' " +
                    ") limit 1"

    };

    public static void main(String[] args) {
        for (int i = 0; i < sqls.length; i++) {
            Map<String, Curd> entryMap = new LinkedHashMap<>();
            entryMap.put("create_user", new Literal(new Token(TokenType.STRING, "addenda")));
            entryMap.put("create_time", new Function(new Token(TokenType.IDENTIFIER, "now")));
            entryMap.put("del_fg", new Literal(new Token(TokenType.NUMBER, 0)));
            entryMap.put("remark", new Literal(new Token(TokenType.STRING, "zhanjinhao")));
            System.out.println(BESqlUtil.insertAddEntry(sqls[i], entryMap));
        }
    }

}
