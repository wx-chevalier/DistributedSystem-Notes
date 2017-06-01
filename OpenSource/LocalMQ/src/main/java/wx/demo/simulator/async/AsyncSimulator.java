package wx.demo.simulator.async;

import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultProducer;
import wx.demo.DefaultPullConsumer;
import org.junit.Assert;
import wx.mq.local.LocalMessageQueue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 本地异步模拟器
 */
public class AsyncSimulator {

    final static AtomicLong producerCounter = new AtomicLong();

    final static AtomicLong producerTime = new AtomicLong();

    final static AtomicLong consumerCounter = new AtomicLong();

    final static AtomicLong consumerTime = new AtomicLong();


    public static void main(String[] args) {

        LocalMessageQueue localMessageStore = LocalMessageQueue.getInstance(System.getProperty("user.home") + File.separator + "lstore");

        KeyValue properties = new DefaultKeyValue();

        properties.put("TestProperty", "TestProperty");
        properties.put("STORE_PATH", "/tmp/lstore");

        KeyValue headers = new DefaultKeyValue();

        headers.put("TestHead", "TestHead");

        ArrayList<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 2; i++) {

            int finalI = i;

            Runnable runnable = () -> {
                DefaultProducer producer = new DefaultProducer(properties);

                long start = now();

                for (int j = 0; j < 1024; j++) {
                    BytesMessage message;

                    message = producer.createBytesMessageToTopic("TestQueueType1_" + finalI, (("TopicContent" + j)).getBytes());

                    producer.send(message);

                    message = producer.createBytesMessageToTopic("TestTopicType1_" + finalI, (("TopicContent" + j)).getBytes());

                    producer.send(message);

                    message = producer.createBytesMessageToTopic("TestTopicType2_" + finalI, (("TopicContent" + j)).getBytes());

                    producer.send(message);


                    producerCounter.incrementAndGet();

                }

                producerTime.addAndGet(now() - start);

            };

            new Thread(runnable).run();
        }

        for (int i = 0; i < 2; i++) {

            int finalI = i;

            Runnable runnable = () -> {
                DefaultPullConsumer consumer = new DefaultPullConsumer(properties);

                consumer.attachQueue("TestQueueType1_" + finalI, Arrays.asList("TestTopicType1_" + finalI, "TestTopicType2_" + finalI));

                long start = now();

                while (true) {
                    // 拉取消息
                    DefaultBytesMessage message = (DefaultBytesMessage) consumer.poll();

                    if (message == null) {
                        break;
                    }

                    Assert.assertTrue(Arrays.asList("TestQueueType1_" + finalI, "TestTopicType1_" + finalI, "TestTopicType2_" + finalI).contains(message.getTopicOrQueueName()));

                    consumerCounter.incrementAndGet();
                }

                consumerTime.addAndGet(now() - start);


                System.out.println("生产者时间：" + producerTime.get() + "，生产者消息数：" + producerCounter.get());
                System.out.println("消费者时间：" + consumerTime.get() + "，消费者消息数" + consumerCounter.get());


            };

            new Thread(runnable).run();
        }


    }
}
