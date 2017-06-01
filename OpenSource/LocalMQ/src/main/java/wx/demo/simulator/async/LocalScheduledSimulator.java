package wx.demo.simulator.async;

import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultProducer;
import wx.demo.DefaultPullConsumer;
import wx.mq.local.LocalMessageQueue;
import wx.mq.util.sys.ThreadFactoryImpl;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description 本地异步测试
 */
public class LocalScheduledSimulator {

    final static Integer producerNums = 100;

    final static Integer consumerNums = 100;

    final static Random random = new Random();

    final static AtomicLong producerCounter = new AtomicLong();

    final static AtomicLong consumerCounter = new AtomicLong();


    public static void main(String args[]) throws Exception {

        // 内部的定期任务执行调度器
        final ScheduledExecutorService producerScheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ProducerScheduledExecutorService"));


        LocalMessageQueue localMessageStore = LocalMessageQueue.getInstance(System.getProperty("user.home") + File.separator + "lstore");

        KeyValue properties = new DefaultKeyValue();

        properties.put("TestProperty", "TestProperty");
        properties.put("STORE_PATH", "/tmp/lstore");

        KeyValue headers = new DefaultKeyValue();

        headers.put("TestHead", "TestHead");

        // 依次构造 Producer 与 Consumer

        for (int i = 0; i < producerNums; i++) {

            // 构建默认的 Producer
            DefaultProducer producer = new DefaultProducer(properties);


            // 执行随机发送
            producerScheduledExecutorService.scheduleAtFixedRate(() -> {

                BytesMessage message = producer.createBytesMessageToTopic("TestTopic" + random.nextInt(producerNums), ("TopicContent" + random.nextInt(producerNums)).getBytes());

                producer.send(message);

                // 计数器加一
                producerCounter.incrementAndGet();

            }, random.nextInt(60), 100, TimeUnit.MILLISECONDS);

        }

        Thread.sleep(6 * 1000);

        // 这里停止所有的生产者线程
        producerScheduledExecutorService.shutdown();

        for (int i = 0; i < producerNums; i++) {

            // 构建默认的 Consumer
            DefaultPullConsumer consumer = new DefaultPullConsumer(properties);

            consumer.attachQueue("TestTopic" + random.nextInt(producerNums), Arrays.asList(new String[]{"TestTopic" + random.nextInt(producerNums)}));

            // 执行随机发送
            producerScheduledExecutorService.scheduleAtFixedRate(() -> {

                while (true) {
                    // 拉取消息
                    DefaultBytesMessage message = (DefaultBytesMessage) consumer.poll();

                    if (message == null) {
                        break;
                    }

                    System.out.println(new String(message.getTopicOrQueueName()));

                    consumerCounter.incrementAndGet();

                }

                // 计数器加一

            }, random.nextInt(60), 100, TimeUnit.MILLISECONDS);

        }

        Thread.sleep(10 * 1000);

        System.out.println(producerCounter.get());
        System.out.println(consumerCounter.get());


    }

}
