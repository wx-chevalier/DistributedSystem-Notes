package wx.demo.benchmark;

import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultPullConsumer;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static wx.mq.util.ds.DateTimeUtil.now;


public class ConsumerBenchmark {

    public static void main(String[] args) {

        final AtomicInteger flag = new AtomicInteger(0);

        KeyValue properties = new DefaultKeyValue();

        properties.put("TestProperty", "TestProperty");

        properties.put("STORE_PATH", "/tmp/lstore");

        final int CONSUMER_COUNT = 20;

        final long startTime = now();

        ExecutorService executorService = Executors.newFixedThreadPool(CONSUMER_COUNT);

        for (int i = 0; i < CONSUMER_COUNT; i++) {

            int finalI = i;

            Runnable runnable = () -> {

                // 设置两个 Consumer 同时争抢 refId 为 0 的 Producer 生产的消息
                DefaultPullConsumer consumer = new DefaultPullConsumer(properties, finalI);

                if (finalI == 1) {
                    consumer.setRefOffset(-1);
                }

                consumer.attachQueue("TestQueueType1_" + finalI, Arrays.asList("TestTopicType1_" + finalI, "TestTopicType2_" + finalI, "TestQueueType2_" + finalI));

                while (true) {

                    // 拉取消息
                    DefaultBytesMessage message = (DefaultBytesMessage) consumer.poll();

                    if (message == null) {
                        break;
                    }

                    System.out.println(" 已消费消息数：" + (float) flag.incrementAndGet() / 1000 +
                            " Consumer" + finalI + ":" + new String(message.getBody())
                            + "总耗时：" + (now() - startTime)
                    );

                }


            };

            executorService.submit(runnable);
        }
    }
}
