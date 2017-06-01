package wx.mq.common.partition.fs;

import wx.mq.common.message.status.SelectMappedBufferResult;
import wx.mq.local.message.service.FlushMessageStoreService;
import wx.mq.util.fs.FSExtra;
import wx.mq.common.partition.fs.allocate.AllocateMappedPartitionService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static wx.mq.common.partition.fs.MappedPartitionUtil.copyMappedFiles;
import static wx.mq.common.partition.fs.MappedPartitionUtil.getLastMappedFile;

/**
 * 调度多个 MappedPartition
 */
public class MappedPartitionQueue {

    // 日志工具
    private final static Logger log = Logger.getLogger(MappedPartitionQueue.class.getName());

    // 全局存放路径
    private final String storePath;

    // 映射文件的体积
    private final int mappedFileSize;

    // 提前分配映射文件服务
    private final AllocateMappedPartitionService allocateMappedPartitionService;

    // 存放所有的映射文件
    private final CopyOnWriteArrayList<MappedPartition> mappedPartitions = new CopyOnWriteArrayList<MappedPartition>();

    // 数据 Flush 位置
    private long flushedWhere = 0;

    // 数据 Commit 位置
    private long committedWhere = 0;

    // MappedPartitionQueue 的执行时间
    private volatile long storeTimestamp = 0;

    // 每次删除的最大文件数
    private static final int DELETE_FILES_BATCH_MAX = 10;

    private FlushMessageStoreService flushMessageStoreService;


    /**
     * Description  默认构造函数
     *
     * @param storePath                      传入的存储文件目录，有可能传入 MessageStore 目录或者 ConsumeQueue 目录
     * @param mappedFileSize
     * @param allocateMappedPartitionService
     */
    public MappedPartitionQueue(final String storePath, int mappedFileSize,
                                AllocateMappedPartitionService allocateMappedPartitionService) {
        this.storePath = storePath;
        this.mappedFileSize = mappedFileSize;
        this.allocateMappedPartitionService = allocateMappedPartitionService;
    }

    public MappedPartitionQueue(final String storePath, int mappedFileSize,
                                AllocateMappedPartitionService allocateMappedPartitionService, FlushMessageStoreService flushMessageStoreService) {
        this.storePath = storePath;
        this.mappedFileSize = mappedFileSize;
        this.allocateMappedPartitionService = allocateMappedPartitionService;
        this.flushMessageStoreService = flushMessageStoreService;
    }

    /**
     * Description 加载内存映射文件序列
     *
     * @return
     */
    public boolean load() {

        // 读取存储路径
        File dir = new File(this.storePath);

        // 列举目录下所有文件
        File[] files = dir.listFiles();

        // 如果文件不为空，则表示有必要加载
        if (files != null) {

            // 重排序
            Arrays.sort(files);

            // 遍历所有的文件
            for (File file : files) {

                // 如果碰到某个文件尚未填满，则返回加载完毕
                if (file.length() != this.mappedFileSize) {
                    log.warning(file + "\t" + file.length()
                            + " length not matched message store config value, ignore it");
                    return true;
                }

                // 否则加载文件
                try {

                    // 实际读取文件
                    MappedPartition mappedPartition = new MappedPartition(file.getPath(), mappedFileSize);

                    // 设置当前文件指针到文件尾
                    mappedPartition.setWrotePosition(this.mappedFileSize);
                    mappedPartition.setFlushedPosition(this.mappedFileSize);

                    // 将文件放置到 MappedFiles 数组中
                    this.mappedPartitions.add(mappedPartition);
//                    log.info("load " + file.getPath() + " OK");

                } catch (IOException e) {
                    log.warning("load file " + file + " error");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Description 将内存内的数据写入到磁盘
     *
     * @param flushLeastPages
     * @return 返回值表示是否已经 Flush 完毕
     */
    public boolean flush(final int flushLeastPages) {

        boolean result = true;

        MappedPartition mappedPartition = this.findMappedFileByOffset(this.flushedWhere, false);
        if (mappedPartition != null) {
            long tmpTimeStamp = mappedPartition.getStoreTimestamp();
            int offset = mappedPartition.flush(flushLeastPages);
            long where = mappedPartition.getFileFromOffset() + offset;
            result = where == this.flushedWhere;
            this.flushedWhere = where;
            if (0 == flushLeastPages) {
                this.storeTimestamp = tmpTimeStamp;
            }
        }

        return result;
    }

    /**
     * Description 依据偏移量获取到内存映射文件
     *
     * @param offset                Offset.
     * @param returnFirstOnNotFound If the mapped file is not found, then return the first one.
     * @return Mapped file or null (when not found and returnFirstOnNotFound is <code>false</code>).
     */
    public MappedPartition findMappedFileByOffset(final long offset, final boolean returnFirstOnNotFound) {
        try {

            // 首先获取首个文件
            MappedPartition mappedPartition = MappedPartitionUtil.getFirstMappedFile(this.mappedPartitions);

            // 如果存在首个文件
            if (mappedPartition != null) {

                // 计算当前偏移对应的文件下标
                int index = (int) ((offset / this.mappedFileSize) - (mappedPartition.getFileFromOffset() / this.mappedFileSize));

                if ((index < 0 || index >= this.mappedPartitions.size()) && returnFirstOnNotFound) {
                    log.warning(String.format("Offset for %s not matched. Request offset: %s , index: %s , " +
                                    "mappedFileSize: %s , mappedPartitions count: %s ",
                            mappedPartition.getFileName(),
                            offset,
                            index,
                            this.mappedFileSize,
                            this.mappedPartitions.size()));
                }

                try {
                    return this.mappedPartitions.get(index);
                } catch (Exception e) {
                    if (returnFirstOnNotFound) {
                        log.warning("【Error】findMappedFileByOffset failure. ");
                        return mappedPartition;
                    }
                }
            }
        } catch (Exception e) {
            log.warning("findMappedFileByOffset Exception");
        }

        // 否则默认返回为空
        return null;
    }


    public MappedPartition findMappedFileByOffset(final long offset) {
        return findMappedFileByOffset(offset, false);
    }


    /**
     * Description 根据起始偏移量查找最后一个文件
     *
     * @param startOffset
     * @return
     */
    public MappedPartition getLastMappedFileOrCreate(final long startOffset) {

        long createOffset = -1;

        // 获取最后一个内存映射文件
        MappedPartition mappedPartitionLast = getLastMappedFile(this.mappedPartitions);

        // 如果当前映射文件队列中不存在任何的映射文件，则初始创建偏移为映射文件大小的整数倍
        if (mappedPartitionLast == null) {
            createOffset = startOffset - (startOffset % this.mappedFileSize);
        }

        // 如果存在映射文件，并且最后一个文件还写满了，则创建偏移量为上一个文件的最后偏移量加上映射文件的体积
        if (mappedPartitionLast != null && mappedPartitionLast.isFull()) {
            createOffset = mappedPartitionLast.getFileFromOffset() + this.mappedFileSize;
        }

        // 如果有必要创建文件
        if (createOffset != -1) {

            // 获取到下一个文件的路径与文件名
            String nextFilePath = this.storePath + File.separator + FSExtra.offset2FileName(createOffset);

            // 以及下下个文件的路径与文件名
            String nextNextFilePath = this.storePath + File.separator
                    + FSExtra.offset2FileName(createOffset + this.mappedFileSize);

            // 指向待创建的映射文件句柄
            MappedPartition mappedPartition = null;

            // 判断是否存在创建映射文件的服务
            if (this.allocateMappedPartitionService != null) {

                // 使用服务创建
                mappedPartition = this.allocateMappedPartitionService.putRequestAndReturnMappedFile(nextFilePath,
                        nextNextFilePath, this.mappedFileSize);
                // 进行预热处理
            } else {

                // 否则直接创建
                try {
                    mappedPartition = new MappedPartition(nextFilePath, this.mappedFileSize);
                } catch (IOException e) {
                    log.warning("create mappedPartition exception");
                }
            }

            // 如果创建成功
            if (mappedPartition != null) {

                // 将该文件标志位队列中创建的第一个文件
                if (this.mappedPartitions.isEmpty()) {
                    mappedPartition.setFirstCreateInQueue(true);
                }

                // 则添加到当前队列
                this.mappedPartitions.add(mappedPartition);
            }

            return mappedPartition;
        }

        return mappedPartitionLast;
    }

    public CopyOnWriteArrayList<MappedPartition> getMappedPartitions() {
        return mappedPartitions;
    }

    public void setFlushedWhere(long flushedWhere) {
        this.flushedWhere = flushedWhere;
    }

    public void setCommittedWhere(long committedWhere) {
        this.committedWhere = committedWhere;
    }

    /**
     * Description 截取脏文件
     *
     * @param offset
     */
    public void truncateDirtyFiles(long offset) {
        List<MappedPartition> willRemoveFiles = new ArrayList<MappedPartition>();

        for (MappedPartition file : this.mappedPartitions) {
            long fileTailOffset = file.getFileFromOffset() + this.mappedFileSize;

            // 如果文件末尾偏移已经大于了当前处理到的偏移
            if (fileTailOffset > offset) {

                // 如果当前处理到的偏移量已经大于文件名代表的偏移
                if (offset >= file.getFileFromOffset()) {
                    file.setWrotePosition((int) (offset % this.mappedFileSize));
                    file.setFlushedPosition((int) (offset % this.mappedFileSize));
                } else {
                    // 如果处理到的偏移量小于文件名代表的偏移
                    file.destroy(1000);
                    willRemoveFiles.add(file);
                }
            }
        }

        this.deleteExpiredFile(willRemoveFiles);
    }

    /**
     * Description 关闭所有的映射文件
     *
     * @param intervalForcibly
     */
    public void shutdown(final long intervalForcibly) {
        for (MappedPartition mf : this.mappedPartitions) {
            mf.shutdown(intervalForcibly);
        }
    }

    /**
     * Description 删除所有过期的文件
     *
     * @param files
     */
    private void deleteExpiredFile(List<MappedPartition> files) {

        if (!files.isEmpty()) {

            Iterator<MappedPartition> iterator = files.iterator();
            while (iterator.hasNext()) {
                MappedPartition cur = iterator.next();
                if (!this.mappedPartitions.contains(cur)) {
                    iterator.remove();
                    log.info("This mappedFile " + cur.getFileName() + " is not contained by mappedPartitions, so skip it.");
                }
            }

            try {
                if (!this.mappedPartitions.removeAll(files)) {
                    log.warning("deleteExpiredFile remove failed.");
                }
            } catch (Exception e) {
                log.warning("deleteExpiredFile has exception.");
            }
        }
    }

    public long getMaxOffset() {
        MappedPartition mappedPartition = getLastMappedFile(this.mappedPartitions);
        if (mappedPartition != null) {
            return mappedPartition.getFileFromOffset() + mappedPartition.getReadPosition();
        }
        return 0;
    }

    public int getMappedFileSize() {
        return mappedFileSize;
    }

    /**
     * Description 删除最后一个内存映射文件
     */
    public void deleteLastMappedFile() {
        MappedPartition lastMappedPartition = MappedPartitionUtil.getLastMappedFile(this);
        if (lastMappedPartition != null) {
            lastMappedPartition.destroy(1000);
            this.mappedPartitions.remove(lastMappedPartition);
            log.info("on recover, destroy a logic mapped file " + lastMappedPartition.getFileName());

        }
    }

    /**
     * Description 删除当前 MappedPartitionQueue 对应的所有文件
     */
    public void destroy() {

        for (MappedPartition mf : this.mappedPartitions) {
            mf.destroy(1000 * 3);
        }
        this.mappedPartitions.clear();
        this.flushedWhere = 0;

        // delete parent directory
        File file = new File(storePath);
        if (file.isDirectory()) {
            file.delete();
        }
    }


    public int deleteExpiredFileByTime(final long expiredTime,
                                       final int deleteFilesInterval,
                                       final long intervalForcibly,
                                       final boolean cleanImmediately) {
        Object[] mfs = copyMappedFiles(this.mappedPartitions, 0);

        if (null == mfs)
            return 0;

        int mfsLength = mfs.length - 1;
        int deleteCount = 0;
        List<MappedPartition> files = new ArrayList<MappedPartition>();
        if (null != mfs) {
            for (int i = 0; i < mfsLength; i++) {
                MappedPartition mappedPartition = (MappedPartition) mfs[i];
                long liveMaxTimestamp = mappedPartition.getLastModifiedTimestamp() + expiredTime;
                if (System.currentTimeMillis() >= liveMaxTimestamp || cleanImmediately) {
                    if (mappedPartition.destroy(intervalForcibly)) {
                        files.add(mappedPartition);
                        deleteCount++;

                        if (files.size() >= DELETE_FILES_BATCH_MAX) {
                            break;
                        }

                        if (deleteFilesInterval > 0 && (i + 1) < mfsLength) {
                            try {
                                Thread.sleep(deleteFilesInterval);
                            } catch (InterruptedException e) {
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        deleteExpiredFile(files);

        return deleteCount;
    }


    public int deleteExpiredFileByOffset(long offset, int unitSize) {
        Object[] mfs = copyMappedFiles(this.mappedPartitions, 0);

        List<MappedPartition> files = new ArrayList<MappedPartition>();
        int deleteCount = 0;
        if (null != mfs) {

            int mfsLength = mfs.length - 1;

            for (int i = 0; i < mfsLength; i++) {
                boolean destroy;
                MappedPartition mappedPartition = (MappedPartition) mfs[i];
                SelectMappedBufferResult result = mappedPartition.selectMappedBuffer(this.mappedFileSize - unitSize);
                if (result != null) {
                    long maxOffsetInLogicQueue = result.getByteBuffer().getLong();
                    result.release();
                    destroy = maxOffsetInLogicQueue < offset;
                    if (destroy) {
                        log.info("physic min offset " + offset + ", logics in current mappedPartition max offset "
                                + maxOffsetInLogicQueue + ", delete it");
                    }
                } else if (!mappedPartition.isAvailable()) { // Handle hanged file.
                    log.warning("Found a hanged consume queue file, attempting to delete it.");
                    destroy = true;
                } else {
                    log.warning("this being not executed forever.");
                    break;
                }

                // TODO: Externalize this hardcoded value
                if (destroy && mappedPartition.destroy(1000 * 60)) {
                    files.add(mappedPartition);
                    deleteCount++;
                } else {
                    break;
                }
            }
        }

        deleteExpiredFile(files);

        return deleteCount;
    }
}
