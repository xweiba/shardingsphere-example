package example.shardingsphere.algorithms;

import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingValue;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;

public class ShardingDataBaseHintAlgorithmUserId implements HintShardingAlgorithm<BigInteger> {

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, HintShardingValue<BigInteger> shardingValue) {
        Collection<BigInteger> values = shardingValue.getValues();
        if (values == null || values.isEmpty()) return null;
        BigInteger shardingDataBaseValue = ((LinkedList<BigInteger>)shardingValue.getValues()).get(0);
        return ShardingDataBaseAlgorithmUserId.supportDataBase(availableTargetNames, shardingDataBaseValue.intValue());
    }
}
