package wx.mq.util.ds;

import io.openmessaging.KeyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultKeyValue implements KeyValue {

    private final Map<String, Object> kvs = new HashMap<>();

    @Override
    public KeyValue put(String key, int value) {
        kvs.put(key, value);
        return this;
    }

    @Override
    public KeyValue put(String key, long value) {
        kvs.put(key, value);
        return this;
    }

    @Override
    public KeyValue put(String key, double value) {
        kvs.put(key, value);
        return this;
    }

    @Override
    public KeyValue put(String key, String value) {
        kvs.put(key, value);
        return this;
    }

    @Override
    public int getInt(String key) {
        return (Integer) kvs.getOrDefault(key, 0);
    }

    @Override
    public long getLong(String key) {
        return (Long) kvs.getOrDefault(key, 0L);
    }

    @Override
    public double getDouble(String key) {
        return (Double) kvs.getOrDefault(key, 0.0d);
    }

    @Override
    public String getString(String key) {

        Object value = kvs.getOrDefault(key, null);

        // 不为空时返回有效值
        if (value != null) {
            return String.valueOf(value);
        }

        return null;
    }

    @Override
    public Set<String> keySet() {
        return kvs.keySet();
    }

    @Override
    public boolean containsKey(String key) {
        return kvs.containsKey(key);
    }

    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{");
        kvs.forEach((String key, Object value) -> {
            stringBuilder.append(key + ":" + value + ", ");
        });

        stringBuilder.append("}");
        
        return "DefaultKeyValue{" +
                "kvs=" + kvs +
                '}';
    }
}
