package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.interceptor.InsertSelectAddItemMode;
import cn.addenda.businesseasy.jdbc.interceptor.UpdateItemMode;

/**
 * @author addenda
 * @since 2023/5/3 18:13
 */
public class BaseEntityContext {

    private BaseEntityContext() {
    }

    private static final ThreadLocal<String> MASTER_VIEW_TL = ThreadLocal.withInitial(() -> null);

    public static void setMasterView(String masterView) {
        MASTER_VIEW_TL.set(masterView);
    }

    public static String getMasterView() {
        return MASTER_VIEW_TL.get();
    }

    public static void clearMasterView() {
        MASTER_VIEW_TL.remove();
    }

    private static final ThreadLocal<Boolean> REPORT_ITEM_NAME_EXISTS_TL = ThreadLocal.withInitial(() -> null);

    public static void setReportItemNameExists(boolean flag) {
        REPORT_ITEM_NAME_EXISTS_TL.set(flag);
    }

    public static Boolean getReportItemNameExists() {
        return REPORT_ITEM_NAME_EXISTS_TL.get();
    }

    public static void clearReportItemNameExists() {
        REPORT_ITEM_NAME_EXISTS_TL.remove();
    }

    private static final ThreadLocal<Boolean> DUPLICATE_KEY_UPDATE_TL = ThreadLocal.withInitial(() -> null);

    public static void setDuplicateKeyUpdate(boolean flag) {
        DUPLICATE_KEY_UPDATE_TL.set(flag);
    }

    public static Boolean getDuplicateKeyUpdate() {
        return DUPLICATE_KEY_UPDATE_TL.get();
    }

    public static void clearDuplicateKeyUpdate() {
        DUPLICATE_KEY_UPDATE_TL.remove();
    }

    private static final ThreadLocal<InsertSelectAddItemMode> INSERT_SELECT_ADD_ITEM_MODE_TL = ThreadLocal.withInitial(() -> null);

    public static void setInsertSelectAddItemMode(InsertSelectAddItemMode flag) {
        INSERT_SELECT_ADD_ITEM_MODE_TL.set(flag);
    }

    public static InsertSelectAddItemMode getInsertSelectAddItemMode() {
        return INSERT_SELECT_ADD_ITEM_MODE_TL.get();
    }

    public static void clearInsertSelectAddItemMode() {
        INSERT_SELECT_ADD_ITEM_MODE_TL.remove();
    }

    private static final ThreadLocal<UpdateItemMode> UPDATE_ITEM_MODE_TL = ThreadLocal.withInitial(() -> null);

    public static void setUpdateItemMode(UpdateItemMode flag) {
        UPDATE_ITEM_MODE_TL.set(flag);
    }

    public static UpdateItemMode getUpdateItemMode() {
        return UPDATE_ITEM_MODE_TL.get();
    }

    public static void clearUpdateItemMode() {
        UPDATE_ITEM_MODE_TL.remove();
    }

    public static void clear() {
        clearMasterView();
        clearReportItemNameExists();
        clearDuplicateKeyUpdate();
        clearInsertSelectAddItemMode();
        clearUpdateItemMode();
    }

}
