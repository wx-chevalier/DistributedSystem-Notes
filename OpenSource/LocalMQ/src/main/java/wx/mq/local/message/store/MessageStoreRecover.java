package wx.mq.local.message.store;

import wx.mq.local.LocalMessageQueue;
import wx.mq.local.message.service.PostPutMessageRequest;
import wx.mq.common.partition.fs.MappedPartition;
import wx.mq.common.partition.fs.MappedPartitionQueue;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import static wx.mq.local.message.serialization.LocalMessageDecoder.checkMessageAndReturnSize;
import static wx.mq.local.message.serialization.LocalMessageSerializer.MESSAGE_MAGIC_CODE;
import static wx.mq.local.message.serialization.LocalMessageSerializer.MESSAGE_MAGIC_CODE_POSTION;
import static wx.mq.local.message.serialization.LocalMessageSerializer.MESSAGE_STORE_TIMESTAMP_POSTION;
import static wx.mq.util.ds.DateTimeUtil.timeMillisToHumanString;

/**
 * Description MessageStore 恢复
 */
public class MessageStoreRecover {

    // 日志记录工具
    private final static Logger log = Logger.getLogger(LocalMessageQueue.class.getName());

    /**
     * Description 正常情况下恢复，此时所有的数据都被 Flush
     */
    public static final void recoverNormally(MessageStore messageStore) {

        final MappedPartitionQueue mappedPartitionQueue = messageStore.getMappedPartitionQueue();

        // 获取当前映射文件
        final List<MappedPartition> mappedPartitions = mappedPartitionQueue.getMappedPartitions();

        // 非空文件
        if (!mappedPartitions.isEmpty()) {

            // 从最后三个文件开始恢复
            int index = mappedPartitions.size() - 3;
            if (index < 0)
                index = 0;

            // 获取当前映射文件
            MappedPartition mappedPartition = mappedPartitions.get(index);
            ByteBuffer byteBuffer = mappedPartition.sliceByteBuffer();
            long processOffset = mappedPartition.getFileFromOffset();
            long mappedFileOffset = 0;

            // 不断获取到消息数据
            while (true) {
                PostPutMessageRequest postPutMessageRequest = checkMessageAndReturnSize(byteBuffer);
                int size = postPutMessageRequest.getMsgSize();
                // Normal data
                if (postPutMessageRequest.isSuccess() && size > 0) {
                    mappedFileOffset += size;
                }
                // Come the end of the file, switch to the next file Since the
                // return 0 representatives met last hole,
                // this can not be included in truncate offset
                else if (postPutMessageRequest.isSuccess() && size == 0) {
                    index++;
                    if (index >= mappedPartitions.size()) {
                        // Current branch can not happen
                        log.info("recover last 3 physics file over, last maped file " + mappedPartition.getFileName());
                        break;
                    } else {
                        mappedPartition = mappedPartitions.get(index);
                        byteBuffer = mappedPartition.sliceByteBuffer();
                        processOffset = mappedPartition.getFileFromOffset();
                        mappedFileOffset = 0;
                        log.info("recover next physics file, " + mappedPartition.getFileName());
                    }
                }
                // Intermediate file read error
                else if (!postPutMessageRequest.isSuccess()) {
                    log.info("recover physics file end, " + mappedPartition.getFileName());
                    break;
                }
            }

            processOffset += mappedFileOffset;
            mappedPartitionQueue.setFlushedWhere(processOffset);
            mappedPartitionQueue.setCommittedWhere(processOffset);
            mappedPartitionQueue.truncateDirtyFiles(processOffset);
        }
    }


    /**
     * Description 根据异常情况从上一次的保存点中恢复
     *
     * @param messageStore
     */
    public static void recoverAbnormally(MessageStore messageStore) {

        // 获取所有的内存映射文件
        final List<MappedPartition> mappedPartitions = messageStore.getMappedPartitionQueue().getMappedPartitions();

        // 如果文件非空
        if (!mappedPartitions.isEmpty()) {

            // 开始寻找从何文件开始恢复
            int index = mappedPartitions.size() - 1;

            MappedPartition mappedPartition = null;

            for (; index >= 0; index--) {

                // 获取当前映射文件
                mappedPartition = mappedPartitions.get(index);

                // 判断并且恢复文件内容到内存中
                if (isMappedFileMatchedRecover(mappedPartition)) {
                    log.info("recover from this mapped file " + mappedPartition.getFileName());
                    break;
                }
            }

            // 如果没有找到合适的，
            if (index < 0) {
                index = 0;
                mappedPartition = mappedPartitions.get(index);
            }

            // 获取当前文件的读取句柄
            ByteBuffer byteBuffer = mappedPartition.sliceByteBuffer();

            // 从文件名中获取到当前的总体偏移
            long processOffset = mappedPartition.getFileFromOffset();
            long mappedFileOffset = 0;

            while (true) {

                // 从当前位置读取消息
                PostPutMessageRequest postPutMessageRequest = checkMessageAndReturnSize(byteBuffer);
                int size = postPutMessageRequest.getMsgSize();

                // 正常的数据
                if (size > 0) {
                    mappedFileOffset += size;

                    // 执行消息的写入到 ConsumeQueue 的操作
                    messageStore.getMessageStore().putMessagePositionInfo(postPutMessageRequest);

                }
                // Intermediate file read error
                else if (size == -1) {
                    log.info("recover physics file end, " + mappedPartition.getFileName());
                    break;
                }
                // Come the end of the file, switch to the next file
                // Since the return 0 representatives met last hole, this can
                // not be included in truncate offset
                else if (size == 0) {
                    index++;
                    if (index >= mappedPartitions.size()) {
                        // The current branch under normal circumstances should
                        // not happen
                        log.info("recover physics file over, last maped file " + mappedPartition.getFileName());
                        break;
                    } else {
                        mappedPartition = mappedPartitions.get(index);
                        byteBuffer = mappedPartition.sliceByteBuffer();
                        processOffset = mappedPartition.getFileFromOffset();
                        mappedFileOffset = 0;

                        // 开始恢复下一个物理文件中的数据
                        log.info("recover next physics file, " + mappedPartition.getFileName());
                    }
                }
            }

            processOffset += mappedFileOffset;
            messageStore.getMappedPartitionQueue().setFlushedWhere(processOffset);
            messageStore.getMappedPartitionQueue().setCommittedWhere(processOffset);
            messageStore.getMappedPartitionQueue().truncateDirtyFiles(processOffset);

            // 清除 ConsumeQueue 中冗余的数据Clear ConsumeQueue redundant data
            messageStore.getMessageStore().truncateDirtyLogicFiles(processOffset);
        }

        // 如果文件已经全部被清空，则回置为 0
        else {
            messageStore.getMappedPartitionQueue().setFlushedWhere(0);
            messageStore.getMappedPartitionQueue().setCommittedWhere(0);

            // 删除目前的 ConsumeQueue 目录下的代码
            messageStore.getMessageStore().destroyLogics();
        }
    }

    /**
     * Description 判断当前消息是否合法
     *
     * @param mappedPartition
     * @return
     */
    private static boolean isMappedFileMatchedRecover(final MappedPartition mappedPartition) {
        ByteBuffer byteBuffer = mappedPartition.sliceByteBuffer();

        int magicCode = byteBuffer.getInt(MESSAGE_MAGIC_CODE_POSTION);
        if (magicCode != MESSAGE_MAGIC_CODE) {
            return false;
        }

        long storeTimestamp = byteBuffer.getLong(MESSAGE_STORE_TIMESTAMP_POSTION);

        if (0 == storeTimestamp) {
            return false;
        }

        log.info("find check timestamp, " +  //
                storeTimestamp + " " + //
                timeMillisToHumanString(storeTimestamp));

        return true;
    }
}
