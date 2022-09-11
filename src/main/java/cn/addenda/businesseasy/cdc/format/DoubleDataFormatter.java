package cn.addenda.businesseasy.cdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.math.BigDecimal;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class DoubleDataFormatter implements DataFormatter<Double> {

    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public String format(Object obj) {
        return obj.toString();
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.DECIMAL, new BigDecimal(obj.toString()));
    }
}
