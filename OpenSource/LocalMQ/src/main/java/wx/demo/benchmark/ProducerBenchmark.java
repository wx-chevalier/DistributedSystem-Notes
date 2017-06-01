package wx.demo.benchmark;

import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultProducer;
import wx.demo.simulator.Simulator;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 本地异步模拟器
 */
public class ProducerBenchmark {

    final static AtomicLong producerCounter = new AtomicLong();

    final static AtomicLong producerTime = new AtomicLong();

    /**
     * producer test
     *
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {

        KeyValue properties = new DefaultKeyValue();

        properties.put("TestProperty", "TestProperty");

        properties.put("STORE_PATH", "/tmp/lstore");

        KeyValue headers = new DefaultKeyValue();

        for (int i = 0; i < 10; i++) {
            headers.put("TestHead" + i, "TestHead" + i);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(20);

        final AtomicLong sendCount = new AtomicLong(0);
        final AtomicLong roundStartTime = new AtomicLong(0);
        final int PRODUCER_COUNT = 20;
        // 总共 4000 * 10000
        final int MESSAGE_COUNT_PER_PRODUCER = 200 * 10000 / 4;

        Random random = new Random();

        // 使用长消息体
        byte[] body = Simulator.generatorContent(random.nextInt(1024)).getBytes();

        final long startTime = now();

        for (int i = 0; i < PRODUCER_COUNT; i++) {

            int finalI = i;

            Runnable runnable = () -> {
                DefaultProducer producer = new DefaultProducer(properties);

                long start = now();

                for (int j = 0; j < MESSAGE_COUNT_PER_PRODUCER; j++) {
                    BytesMessage message;

                    message = producer.createBytesMessageToTopic("TestTopicType1_" + finalI, body);
                    producer.send(message);

                    message = producer.createBytesMessageToTopic("TestTopicType2_" + finalI, body);
                    producer.send(message);

                    message = producer.createBytesMessageToQueue("TestQueueType1_" + finalI, body);
                    producer.send(message);

                    message = producer.createBytesMessageToQueue("TestQueueType2_" + finalI, body);
                    producer.send(message);


                    if (j % 10000 == 0 && j != 0) {
                        long l = sendCount.addAndGet(40000);

                        System.out.println(String.format("共发送 %f K条, 本轮速度 %f, 总均速 %f K条/s,总耗时 %f 秒", ((float) l) / 1000, (float) l / (now() - roundStartTime.get()), ((float) l) / (now() - startTime), ((float) (now() - startTime) / 1000)));
                        roundStartTime.set(now());
                    }
                }

                // 全部执行完毕后进行 Flush 以及 Shutdown
                producer.flush();

                System.out.println("Over");

                producerTime.addAndGet(now() - start);

            };

            executorService.submit(runnable);
        }

    }
}
