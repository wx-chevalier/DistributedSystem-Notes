package wx.mq.common.partition.fs;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Description 部分针对映射文件队列的工具函数
 */
public class MappedPartitionUtil {

    private final static Logger log = Logger.getLogger(MappedPartitionUtil.class.getName());

    /**
     * Description 从内存映射文件队列中返回首个映射文件
     *
     * @return
     */
    public static MappedPartition getFirstMappedFile(final CopyOnWriteArrayList<MappedPartition> mappedPartitions) {
        MappedPartition mappedPartitionFirst = null;

        if (!mappedPartitions.isEmpty()) {
            try {
                mappedPartitionFirst = mappedPartitions.get(0);
            } catch (IndexOutOfBoundsException e) {
                //ignore
            } catch (Exception e) {
                log.warning("getFirstMappedFile has exception.");
            }
        }

        return mappedPartitionFirst;
    }

    public static MappedPartition getFirstMappedFile(MappedPartitionQueue mappedPartitionQueue) {

        return getFirstMappedFile(mappedPartitionQueue.getMappedPartitions());
    }

    /**
     * Description 获取最后的映射文件
     *
     * @param mappedPartitions
     * @return
     */
    public static MappedPartition getLastMappedFile(final CopyOnWriteArrayList<MappedPartition> mappedPartitions) {
        MappedPartition mappedPartitionLast = null;

        while (!mappedPartitions.isEmpty()) {
            try {
                mappedPartitionLast = mappedPartitions.get(mappedPartitions.size() - 1);
                break;
            } catch (IndexOutOfBoundsException e) {
                //continue;
            } catch (Exception e) {
                log.warning("getLastMappedFile has exception.");
                break;
            }
        }

        return mappedPartitionLast;
    }

    public static MappedPartition getLastMappedFile(MappedPartitionQueue mappedPartitionQueue) {

        return getLastMappedFile(mappedPartitionQueue.getMappedPartitions());
    }

    /**
     * Description 根据某个时间戳从内存中查找映射文件
     *
     * @param mappedPartitions
     * @param timestamp
     * @return
     */
    public MappedPartition getMappedFileByTime(final CopyOnWriteArrayList<MappedPartition> mappedPartitions, final long timestamp) {
        Object[] mfs = MappedPartitionUtil.copyMappedFiles(mappedPartitions, 0);

        if (null == mfs)
            return null;

        for (int i = 0; i < mfs.length; i++) {
            MappedPartition mappedPartition = (MappedPartition) mfs[i];
            if (mappedPartition.getLastModifiedTimestamp() >= timestamp) {
                return mappedPartition;
            }
        }

        return (MappedPartition) mfs[mfs.length - 1];
    }

    /**
     * Description 复制某个映射文件序列
     *
     * @param mappedPartitions
     * @param reservedMappedFiles
     * @return
     */
    public static Object[] copyMappedFiles(final CopyOnWriteArrayList<MappedPartition> mappedPartitions, final int reservedMappedFiles) {
        Object[] mfs;

        if (mappedPartitions.size() <= reservedMappedFiles) {
            return null;
        }

        mfs = mappedPartitions.toArray();
        return mfs;
    }
}
