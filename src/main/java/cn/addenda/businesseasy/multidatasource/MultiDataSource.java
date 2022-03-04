package cn.addenda.businesseasy.multidatasource;

import cn.addenda.businesseasy.util.BEUtilException;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author ISJINHAO
 * @Date 2022/3/1 18:43
 */
public class MultiDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> dataSourceThreadLocal = new ThreadLocal<>();

    private Map<String, MultiDataSourceEntry> datasourceHolderMap = new HashMap();

    private SlaveDataSourceSelector slaveDataSourceSelector = new RandomSlaveDataSourceSelector();

    @Override
    protected Object determineCurrentLookupKey() {
        return dataSourceThreadLocal.get();
    }

    public static void setCurDataSource(String dataSourceName, String mode) {
        dataSourceThreadLocal.set(dataSourceName + "." + mode);
    }

    public static void clearCurDataSource() {
        dataSourceThreadLocal.remove();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        String key = null;
        try {
            key = (String) determineCurrentLookupKey();
            // 但key为空的时候，即没有注解的时候，走默认的MASTER库
            if (key == null || key.length() == 0) {
                return datasourceHolderMap.get(MultiDataSourceConstant.DEFAULT).getMaster();
            }
            String[] split = key.split("\\.");
            MultiDataSourceEntry multiDataSourceEntry = datasourceHolderMap.get(split[0]);
            if (MultiDataSourceConstant.MASTER.equals(split[1])) {
                return multiDataSourceEntry.getMaster();
            } else if (MultiDataSourceConstant.SLAVE.equals(split[1])) {
                List<DataSource> slaves = multiDataSourceEntry.getSlaves();
                return slaveDataSourceSelector.select(key, slaves);
            } else {
                throw new BEUtilException("无法识别的多数据源模式，只能选择 MASTER 或者 SLAVE，当前：" + key);
            }
        } catch (Exception e) {
            if (BEUtilException.class.equals(e.getClass())) {
                throw e;
            } else {
                throw new BEUtilException("从配置的多数据源中获取数据源失败！当前key：" + key, e);
            }
        } finally {
            clearCurDataSource();
        }
    }

    public void setSlaveDataSourceSelector(SlaveDataSourceSelector slaveDataSourceSelector) {
        this.slaveDataSourceSelector = slaveDataSourceSelector;
    }

    public void setDatasourceHolderMap(Map<String, MultiDataSourceEntry> datasourceHolderMap) {
        this.datasourceHolderMap = datasourceHolderMap;
    }

    public void addMultiDataSourceEntry(String key, MultiDataSourceEntry multiDataSourceEntry) {
        datasourceHolderMap.put(key, multiDataSourceEntry);
    }

    @Override
    public void afterPropertiesSet() {
        datasourceHolderMap.forEach((key, multiDataSourceEntry) -> {
            List<DataSource> slaves = multiDataSourceEntry.getSlaves();
            if (slaves == null || slaves.isEmpty()) {
                List<DataSource> slavesThatCopyFromMaster = new ArrayList<>();
                slavesThatCopyFromMaster.add(multiDataSourceEntry.getMaster());
                multiDataSourceEntry.setSlaves(slavesThatCopyFromMaster);
            }
        });
    }

}
