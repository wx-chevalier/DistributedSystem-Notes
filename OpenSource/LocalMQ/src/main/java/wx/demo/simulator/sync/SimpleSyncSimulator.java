package wx.demo.simulator.sync;

import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultProducer;
import wx.demo.DefaultPullConsumer;
import wx.demo.simulator.Simulator;

import java.util.Arrays;

import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 本地单线程测试
 */
public class SimpleSyncSimulator extends Simulator {

    static final public boolean enableProducer = true;

    static final public boolean enableConsumer = true;

    public static void main(String args[]) throws Exception {

        long start = now();

        int flag = 0;

        KeyValue properties = new DefaultKeyValue();

        properties.put("TestProperty", "TestProperty");
        properties.put("STORE_PATH", "/tmp/lstore");

        if (enableProducer) {

            // 构建默认的 Producer
            DefaultProducer producer = new DefaultProducer(properties);

            for (int i = 0; i < 2; i++) {

                BytesMessage message = producer.createBytesMessageToTopic("TestTopic", ("TestTopic:TopicContent" + i).getBytes());

                message.putHeaders("MSG_ID", i);
                message.putProperties("Properties", i);


                producer.send(message);

                message = producer.createBytesMessageToTopic("TestTopic1", ("TestTopic1:TopicContent" + i).getBytes());

                producer.send(message);

                message = producer.createBytesMessageToQueue("TestQueue1", ("TestQueue1:TopicContent" + i).getBytes());

                producer.send(message);
            }

            producer.flush(false);

            System.out.println("发送完毕！");

        }

        if (enableConsumer) {
            DefaultPullConsumer consumer = new DefaultPullConsumer(properties);

            consumer.attachQueue("TestQueue1", Arrays.asList("TestTopic", "TestTopic1"));

            while (true) {
                // 拉取消息
                DefaultBytesMessage message = (DefaultBytesMessage) consumer.poll();

                if (message == null) {
                    break;
                }

                System.out.println(
                        new String(message.getBody()) +
                                " : " + message.headers() +
                                " : " + message.properties()
                );

                flag++;
            }

            System.out.println(flag);

            System.out.println(now() - start);
        }


    }
}
