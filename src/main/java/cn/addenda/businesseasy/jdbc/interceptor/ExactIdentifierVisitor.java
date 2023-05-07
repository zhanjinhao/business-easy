package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;

import java.util.List;
import java.util.Map;

/**
 * @author addenda
 * @since 2023/5/7 11:42
 */
public class ExactIdentifierVisitor extends AbstractIdentifierVisitor {

    private boolean exact;

    public ExactIdentifierVisitor() {
        super(null);
        this.exact = true;
    }

    @Override
    public void endVisit(SQLSelectQueryBlock x) {

        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getFrom());
        if (viewToTableMap.size() == 1) {
            // no-op
        } else {
            List<String> identifierList = identifierListStack.peek();
            for (String identifier : identifierList) {
                String owner = JdbcSQLUtils.extractColumnOwner(identifier);
                if (owner == null) {
                    exact = false;
                }
            }
        }

        super.endVisit(x);
    }

    public boolean isExact() {
        return exact;
    }

}
