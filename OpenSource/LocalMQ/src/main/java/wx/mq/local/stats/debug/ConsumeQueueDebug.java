package wx.mq.local.stats.debug;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static wx.mq.local.LocalMessageQueueConfig.mapedFileSizeConsumeQueue;

public class ConsumeQueueDebug {


    public static void main(String args[]) throws IOException {

        String fileName = "/tmp/lstore/consumequeue/TestTopic1/0/00000000000000000000";

        FileChannel fileChannel = new RandomAccessFile(new File(fileName), "rw").getChannel();

        // 将文件映射到内存中
        ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, mapedFileSizeConsumeQueue);

        while (true) {
            long offset = byteBuffer.getLong();

            int size = byteBuffer.getInt();

            System.out.println(offset + ":" + size);
        }

    }
}
