package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.baseentity.DruidBaseEntityRewriterInsertTest;
import cn.addenda.businesseasy.jdbc.baseentity.DruidBaseEntityRewriterSelectTest;
import cn.addenda.businesseasy.jdbc.baseentity.DruidBaseEntityRewriterUpdateTest;
import cn.addenda.businesseasy.jdbc.dynamicsql.DynamicSQLAssemblerInsertAdditemTest;
import cn.addenda.businesseasy.jdbc.dynamicsql.DynamicSQLAssemblerTableAddWhereConditionTest;
import cn.addenda.businesseasy.jdbc.dynamicsql.DynamicSQLAssemblerUpdateAdditemTest;
import cn.addenda.businesseasy.jdbc.tombstone.*;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/5/13 15:43
 */
public class WholeTest {

    @Test
    public void main() {
        new ViewToTableVisitorTest().test1();
        new SelectItemStarExistsVisitorTest().test1();
        new SelectItemIdentifierExistsVisitorTest().test1();
        new IdentifierExistsVisitorTest().test1();
        new ExactIdentifierVisitorTest().test1();
        new InsertOrUpdateItemNameIdentifierExistsVisitorTest().test1();
        new DruidTombstoneConvertorUpdateTest().test1();
        new DruidTombstoneConvertorSelectTest().test1();
        new DruidTombstoneConvertorSelect2Test().test1();
        new DruidTombstoneConvertorDeleteTest().test1();
        new DruidTombstoneConvertorInsertTest().test1();
        new DynamicSQLAssemblerInsertAdditemTest().test1();
        new DynamicSQLAssemblerTableAddWhereConditionTest().test1();
        new DynamicSQLAssemblerUpdateAdditemTest().test1();
        new DruidBaseEntityRewriterInsertTest().test1();
        new DruidBaseEntityRewriterUpdateTest().test1();
        new DruidBaseEntityRewriterSelectTest().test1();
    }

}
