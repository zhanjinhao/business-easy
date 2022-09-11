package cn.addenda.businesseasy.cdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;

/**
 * @author addenda
 * @datetime 2022/9/10 16:52
 */
public interface DataFormatterRegistry {

    String format(Object obj);

    Token parse(Object obj);

    boolean typeAvailable(Class<?> clazz);

}
