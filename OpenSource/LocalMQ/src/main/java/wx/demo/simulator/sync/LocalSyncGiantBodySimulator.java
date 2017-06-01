package wx.demo.simulator.sync;

import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultProducer;
import wx.demo.DefaultPullConsumer;
import wx.demo.simulator.Simulator;
import wx.mq.local.LocalMessageQueue;

import java.util.Arrays;

import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 测试在大消息体的情况下运行情况
 */
public class LocalSyncGiantBodySimulator {


    public static void main(String args[]) throws Exception {

        // 这里已经获取过了对象，等价于确定了 LocalMessageQueue 的存储地址
        LocalMessageQueue localMessageStore = LocalMessageQueue.getInstance("/tmp/lstore");

        long start = now();

        int flag = 0;

        KeyValue properties = new DefaultKeyValue();

        properties.put("TestProperty", "TestProperty");
        properties.put("STORE_PATH", "/tmp/lstore");

        // 构建默认的 Producer
        DefaultProducer producer = new DefaultProducer(properties);


        String body = Simulator.generatorContent(1024 * 256);

        for (int i = 0; i < 1024; i++) {

            BytesMessage message = producer.createBytesMessageToTopic("TestTopic", (body + i).getBytes());

            producer.send(message);

            message = producer.createBytesMessageToTopic("TestTopic1", (body + i).getBytes());

            producer.send(message);

            message = producer.createBytesMessageToQueue("TestQueue1", (body + i).getBytes());

            producer.send(message);
        }

        DefaultPullConsumer consumer = new DefaultPullConsumer(properties);

        consumer.attachQueue("TestQueue1", Arrays.asList("TestTopic", "TestTopic1"));

        while (true) {
            // 拉取消息
            DefaultBytesMessage message = (DefaultBytesMessage) consumer.poll();

            if (message == null) {
                break;
            }

//            System.out.println(new String(message.getBody()));

            flag++;
        }

        System.out.println(flag);

        System.out.println(now() - start);

    }
}
