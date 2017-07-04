package wx.mq.local.stats.debug;

import wx.mq.common.message.DefaultBytesMessage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static wx.mq.local.message.serialization.LocalMessageDecoder.readMessageFromByteBuffer;
import static wx.mq.local.LocalMessageQueueConfig.mapedFileSizeCommitLog;

public class MessageStoreDebugger {

    public static void main(String args[]) throws IOException {
        getMsgNumsInCommitLog();
    }

    public static void getMsgNumsInCommitLog() throws IOException {
        String fileName = "/tmp/lstore/bucketqueue/QUEUE_13/6/00000000000000000000";

        FileChannel fileChannel = new RandomAccessFile(new File(fileName), "rw").getChannel();

        // 将文件映射到内存中
        ByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, mapedFileSizeCommitLog);

        int flag = 0;

        while (true) {
            // 从当前位置读取消息
            DefaultBytesMessage bytesMessage = readMessageFromByteBuffer(mappedByteBuffer);

            if (bytesMessage == null) {
                break;
            }

            int size = bytesMessage.getMessageLength();

            // 正常的数据
            if (size > 0) {
                flag++;
            }
            // Intermediate file read error
            else if (size == -1) {
                break;
            }
        }

        System.out.println("当前文件消息数目：" + flag);
    }

}
