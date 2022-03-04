package cn.addenda.businesseasy.multidatasource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Random;

/**
 * @Author ISJINHAO
 * @Date 2022/3/3 18:59
 */
public class RandomSlaveDataSourceSelector implements SlaveDataSourceSelector {
    private final Random random = new Random(System.nanoTime());

    @Override
    public DataSource select(String key, List<DataSource> dataSourceList) {
        return dataSourceList.get((int) Math.abs((long) random.nextInt() % dataSourceList.size()));
    }

}
