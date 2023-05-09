package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

import cn.addenda.businesseasy.jdbc.interceptor.ExactIdentifierVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.SelectItemStarExistsVisitor;

/**
 * @author addenda
 * @since 2023/5/7 19:58
 */
public class DruidSQLChecker implements SQLChecker {

    @Override
    public boolean exactIdentifier(String sql) {
        ExactIdentifierVisitor exactIdentifierVisitor = new ExactIdentifierVisitor(sql);
        exactIdentifierVisitor.visit();
        return exactIdentifierVisitor.isExact();
    }

    @Override
    public boolean allColumnExists(String sql) {
        SelectItemStarExistsVisitor selectItemStarExistsVisitor = new SelectItemStarExistsVisitor(sql);
        selectItemStarExistsVisitor.visit();
        return selectItemStarExistsVisitor.isExists();
    }

}
