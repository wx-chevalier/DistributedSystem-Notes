package wx.mq.util.ds;

import io.openmessaging.KeyValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Description 集合类型转化器
 */
public class CollectionTransformer {

    // 将 HashMap 转化为 String 时的间隔
    public static final char NAME_VALUE_SEPARATOR = 1;

    // 不同属性之间的分隔符
    public static final char PROPERTY_SEPARATOR = 2;

    /**
     * Description 将字符串转化为 Map
     *
     * @param properties
     * @return
     */
    public static Map<String, String> string2Map(final String properties) {
        Map<String, String> map = new HashMap<String, String>();
        if (properties != null) {
            String[] items = properties.split(String.valueOf(PROPERTY_SEPARATOR));
            for (String i : items) {
                String[] nv = i.split(String.valueOf(NAME_VALUE_SEPARATOR));
                if (2 == nv.length) {
                    map.put(nv[0], nv[1]);
                }
            }
        }

        return map;
    }

    /**
     * Description 将 Map 转化为字符串
     *
     * @param properties
     * @return
     */
    public static String map2String(Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();

                sb.append(name);
                sb.append(NAME_VALUE_SEPARATOR);
                sb.append(value);
                sb.append(PROPERTY_SEPARATOR);
            }
        }
        return sb.toString();
    }

    /**
     * Description 将字符串转化为 KeyValue 数据类型
     *
     * @param properties
     * @return
     */
    public static KeyValue string2KeyValue(final String properties) {

        // 初始化
        KeyValue kv = new DefaultKeyValue();

        // 判断是否传入有效值
        if (properties != null) {
            String[] items = properties.split(String.valueOf(PROPERTY_SEPARATOR));
            for (String i : items) {
                String[] nv = i.split(String.valueOf(NAME_VALUE_SEPARATOR));
                if (2 == nv.length) {
                    kv.put(nv[0], nv[1]);
                }
            }
        }

        return kv;
    }

    /**
     * Description 将 KeyValue 转化为字符串
     *
     * @param kv
     * @return
     */
    public static final String keyValue2String(KeyValue kv) {

        StringBuilder sb = new StringBuilder();

        if (kv != null) {

            for (String key : kv.keySet()) {
                sb.append(key);
                sb.append(NAME_VALUE_SEPARATOR);
                sb.append(kv.getString(key));
                sb.append(PROPERTY_SEPARATOR);
            }
        }
        return sb.toString();
    }


}
