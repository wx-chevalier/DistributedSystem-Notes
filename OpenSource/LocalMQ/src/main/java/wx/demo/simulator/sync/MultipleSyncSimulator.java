package wx.demo.simulator.sync;

import io.openmessaging.KeyValue;
import io.openmessaging.Message;
import io.openmessaging.MessageHeader;
import io.openmessaging.Producer;
import io.openmessaging.PullConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wx.mq.util.ds.DefaultKeyValue;
import wx.demo.DefaultProducer;
import wx.demo.DefaultPullConsumer;
import org.junit.Assert;

/**
 * Description 模拟使用内存存储的消息队列
 */
public class MultipleSyncSimulator {


    public static void main(String[] args) {
        KeyValue properties = new DefaultKeyValue();
        /*
        //实际测试时利用 STORE_PATH 传入存储路径
        //所有producer和consumer的STORE_PATH都是一样的，选手可以自由在该路径下创建文件
         */
        properties.put("STORE_PATH", "/tmp/lstore");

        //这个测试程序的测试逻辑与实际评测相似，但注意这里是单线程的，实际测试时会是多线程的，并且发送完之后会Kill进程，再起消费逻辑

        Producer producer = new DefaultProducer(properties);

        //构造测试数据
        String topic1 = "TOPIC1"; //实际测试时大概会有100个Topic左右
        String topic2 = "TOPIC2"; //实际测试时大概会有100个Topic左右
        String queue1 = "QUEUE1"; //实际测试时，queue数目与消费线程数目相同
        String queue2 = "QUEUE2"; //实际测试时，queue数目与消费线程数目相同
        List<Message> messagesForTopic1 = new ArrayList<>(1024);
        List<Message> messagesForTopic2 = new ArrayList<>(1024);
        List<Message> messagesForQueue1 = new ArrayList<>(1024);
        List<Message> messagesForQueue2 = new ArrayList<>(1024);
        for (int i = 0; i < 10240; i++) {
            //注意实际比赛可能还会向消息的headers或者properties里面填充其它内容
            messagesForTopic1.add(producer.createBytesMessageToTopic(topic1, (topic1 + i).getBytes()));
            messagesForTopic2.add(producer.createBytesMessageToTopic(topic2, (topic2 + i).getBytes()));
            messagesForQueue1.add(producer.createBytesMessageToQueue(queue1, (queue1 + i).getBytes()));
            messagesForQueue2.add(producer.createBytesMessageToQueue(queue2, (queue2 + i).getBytes()));
        }

        long start = System.currentTimeMillis();
        //发送, 实际测试时，会用多线程来发送, 每个线程发送自己的Topic和Queue
        for (int i = 0; i < 10240; i++) {
            producer.send(messagesForTopic1.get(i));
            producer.send(messagesForTopic2.get(i));
            producer.send(messagesForQueue1.get(i));
            producer.send(messagesForQueue2.get(i));
        }
        long end = System.currentTimeMillis();

        long T1 = end - start;

        //请保证数据写入磁盘中

        //消费样例1，实际测试时会Kill掉发送进程，另取进程进行消费
        {
            PullConsumer consumer1 = new DefaultPullConsumer(properties);
            consumer1.attachQueue(queue1, Collections.singletonList(topic1));

            int queue1Offset = 0, topic1Offset = 0;

            long startConsumer = System.currentTimeMillis();
            while (true) {
                Message message = consumer1.poll();
                if (message == null) {
                    //拉取为null则认为消息已经拉取完毕
                    break;
                }
                String topic = message.headers().getString(MessageHeader.TOPIC);
                String queue = message.headers().getString(MessageHeader.QUEUE);
                //实际测试时，会一一比较各个字段
                if (topic != null) {
                    Message message1 = messagesForTopic1.get(topic1Offset++);
                    Assert.assertEquals(topic1, topic);
//                    Assert.assertEquals(message1, message);
                } else {
                    Message message1 = messagesForQueue1.get(queue1Offset++);
                    Assert.assertEquals(queue1, queue);
//                    Assert.assertEquals(message1, message);
                }
            }
            long endConsumer = System.currentTimeMillis();
            long T2 = endConsumer - startConsumer;
            System.out.println(String.format("Team1 cost:%d ms tps:%d q/ms", T2 + T1, (queue1Offset + topic1Offset) / (T1 + T2)));

        }

        //消费样例2，实际测试时会Kill掉发送进程，另取进程进行消费
        {
            PullConsumer consumer2 = new DefaultPullConsumer(properties);
            List<String> topics = new ArrayList<>();
            topics.add(topic1);
            topics.add(topic2);
            consumer2.attachQueue(queue2, topics);

            int queue2Offset = 0, topic1Offset = 0, topic2Offset = 0;

            long startConsumer = System.currentTimeMillis();
            while (true) {
                Message message = consumer2.poll();
                if (message == null) {
                    //拉取为null则认为消息已经拉取完毕
                    break;
                }

                String topic = message.headers().getString(MessageHeader.TOPIC);
                String queue = message.headers().getString(MessageHeader.QUEUE);
                //实际测试时，会一一比较各个字段
                if (topic != null) {
                    if (topic.equals(topic1)) {
                        Message message1 = messagesForTopic1.get(topic1Offset++);

//                        Assert.assertEquals(message1, message);
                    } else {
                        Message message1 = messagesForTopic2.get(topic2Offset++);
                        Assert.assertEquals(topic2, topic);
//                        Assert.assertEquals(message1, message);
                    }
                } else {
                    Message message1 = messagesForQueue2.get(queue2Offset++);
                    Assert.assertEquals(queue2, queue);
//                    Assert.assertEquals(message1, message);
                }
            }
            long endConsumer = System.currentTimeMillis();
            long T2 = endConsumer - startConsumer;
            System.out.println(String.format("Team2 cost:%d ms tps:%d q/ms", T2 + T1, (queue2Offset + topic1Offset) / (T1 + T2)));
        }


    }
}
