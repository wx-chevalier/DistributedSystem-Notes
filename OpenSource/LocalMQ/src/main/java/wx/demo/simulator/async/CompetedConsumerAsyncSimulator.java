package wx.demo.simulator.async;

import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultProducer;
import wx.demo.DefaultPullConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deference 模拟多 Consumer 竞争性争抢
 */
public class CompetedConsumerAsyncSimulator {

    public static AtomicLong flag = new AtomicLong(0);

    public static ConcurrentSkipListSet<String> messageCountByTopicAndBody = new ConcurrentSkipListSet<>();

    public static void main(String args[]) throws Exception {

        // 这里已经获取过了对象，等价于确定了 LocalMessageQueue 的存储地址

        KeyValue properties = new DefaultKeyValue();

        properties.put("TestProperty", "TestProperty");
        properties.put("STORE_PATH", "/tmp/lstore");


        final int producerNums = 20;

        ArrayList<DefaultProducer> producers = new ArrayList<>();

        // 构建足够多的 Producer
        for (int i = 0; i < producerNums; i++) {
            DefaultProducer producer = new DefaultProducer(properties);
            producers.add(producer);
        }


        for (int i = 0; i < 1024; i++) {

            DefaultProducer producer = producers.get(i % producerNums);

            BytesMessage message = producer.createBytesMessageToTopic("TestTopic", ("TestTopic:TopicContent" + i).getBytes());

            messageCountByTopicAndBody.add("TestTopic:TopicContent" + i);

            producer.send(message);

            message = producer.createBytesMessageToTopic("TestTopic1", ("TestTopic1:TopicContent" + i).getBytes());

            messageCountByTopicAndBody.add("TestTopic1:TopicContent" + i);

            producer.send(message);

            message = producer.createBytesMessageToQueue("TestQueue1", ("TestQueue1:TopicContent" + i).getBytes());

            messageCountByTopicAndBody.add("TestQueue1:TopicContent" + i);

            producer.send(message);
        }

        // 将所有消息进行持久化
        for (int i = 0; i < producerNums; i++) {
            producers.get(i).flush(false);
        }

        ArrayList<Runnable> runnables = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {

            int finalI = i;

            runnables.add(() -> {

                // 设置两个 Consumer 同时争抢 refId 为 0 的 Producer 生产的消息
                DefaultPullConsumer consumer = new DefaultPullConsumer(properties, finalI);

                if (finalI == 1) {
                    consumer.setRefOffset(-1);
                }

                consumer.attachQueue("TestQueue1", Arrays.asList("TestTopic", "TestTopic1"));

                while (true) {

                    // 拉取消息
                    DefaultBytesMessage message = (DefaultBytesMessage) consumer.poll();

                    if (message == null) {
                        break;
                    }

                    if (messageCountByTopicAndBody.contains(new String(message.getBody()))) {
                        messageCountByTopicAndBody.remove(new String(message.getBody()));
                    } else {
                        throw new Error("存在重复消息");
                    }

                    System.out.println("剩余未被消费消息数：" + messageCountByTopicAndBody.size() +
                            " 已消费消息数：" + flag.incrementAndGet() +
                            " Consumer" + finalI + ":" + new String(message.getBody()));

                }

                System.out.println(" Consumer" + finalI + "已退出！");


            });

        }

        runnables.forEach(runnable -> {
            executorService.submit(runnable);
        });

    }
}
