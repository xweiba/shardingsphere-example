package example.shardingsphere.algorithms;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.HashSet;

public class ShardingDataBaseAlgorithmUserId implements StandardShardingAlgorithm<Integer> {

    @Override
    public String doSharding(final Collection<String> availableTargetNames, final PreciseShardingValue<Integer> shardingValue) {
        // 指定一个数据源
        Collection<String> dataBases = supportDataBase(availableTargetNames, shardingValue.getValue());
        if (!dataBases.isEmpty()) {
            return dataBases.stream().iterator().next();
        }
        return null;
    }

    @Override
    public Collection<String> doSharding(final Collection<String> availableTargetNames, final RangeShardingValue<Integer> shardingValue) {
        // 如果是范围的，需要取出对应范围的所有数据源
        Collection<String> result = new HashSet<>(2, 1F);
        for (int i = shardingValue.getValueRange().lowerEndpoint(); i <= shardingValue.getValueRange().upperEndpoint(); i++) {
            Collection<String> dataBases= supportDataBase(availableTargetNames, i);
            if (!dataBases.isEmpty()) {
                result.addAll(dataBases);
            }
        }
        return result;
    }

    @Override
    public String getType() {
        return "SHARDING_DATABASE_USER_ID";
    }

    public static Collection<String> supportDataBase(Collection<String> availableTargetNames, Integer userId) {
        Collection<String> result = new HashSet<>(2, 1F);
        for (String availableTargetName : availableTargetNames) {
            if (checkSupportDataBase(availableTargetName, userId)) {
                result.add(availableTargetName);
            }
        }
        return result;
    }

    public static boolean checkSupportDataBase(String each, Integer userId) {
        return each.contains("master") && !each.contains("slave") && each.endsWith(String.valueOf(userId % 2));
    }
}
