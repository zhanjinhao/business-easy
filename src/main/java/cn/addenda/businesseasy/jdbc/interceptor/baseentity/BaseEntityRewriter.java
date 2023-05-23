package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.visitor.item.InsertSelectAddItemMode;
import cn.addenda.businesseasy.jdbc.visitor.item.UpdateItemMode;

/**
 * @author addenda
 * @since 2023/5/2 19:35
 */
public interface BaseEntityRewriter {

    String rewriteInsertSql(String sql, InsertSelectAddItemMode insertSelectAddItemMode,
                            boolean duplicateKeyUpdate, UpdateItemMode updateItemMode, boolean reportItemNameExists);

    String rewriteSelectSql(String sql, String masterView);

    String rewriteUpdateSql(String sql, UpdateItemMode updateItemMode, boolean reportItemNameExists);

}
