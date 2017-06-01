package wx.mq.common.partition.fs;


import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.append.AppendMessageCallback;
import wx.mq.common.message.status.AppendMessageResult;
import wx.mq.common.message.status.AppendMessageStatus;
import wx.mq.common.message.status.SelectMappedBufferResult;
import wx.mq.common.partition.Partition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static wx.mq.util.ds.DateTimeUtil.now;
import static wx.mq.util.fs.FSExtra.ensureDirOK;
import static wx.mq.util.fs.nio.BufferUtil.unmap;

/**
 * Description 内存映射文件
 */
public class MappedPartition extends Partition {

    // 日志工具
    private final static Logger log = Logger.getLogger(MappedPartition.class.getName());

    // 系统页存大小
    public static final int OS_PAGE_SIZE = 1024 * 4;

    // 全部打开的 MappedPartition
    protected static final AtomicInteger TOTAL_MAPPED_FILES = new AtomicInteger(0);

    // 总的虚拟内存数
    protected static final AtomicLong TOTAL_MAPPED_VIRTUAL_MEMORY = new AtomicLong(0);

    // 存放文件名
    private String fileName;

    // 当前文件尺寸
    protected int fileSize;

    // 存放文件偏移量
    private long fileFromOffset;

    // 存放文件句柄
    private File file;

    // 内存文件映射句柄
    private MappedByteBuffer mappedByteBuffer;

    // 写入位置
    protected final AtomicInteger wrotePosition = new AtomicInteger(0);

    // NIO 的文件通道
    protected FileChannel fileChannel;

    // 记录文件的 Flush 位置
    private final AtomicInteger flushedPosition = new AtomicInteger(0);

    // 存放最后的修改信息
    private volatile long storeTimestamp = 0;

    // 是否为首次创建
    private boolean firstCreateInQueue = false;

    /**
     * Description 默认构造函数
     *
     * @param fileName
     * @param fileSize
     * @throws IOException
     */
    public MappedPartition(final String fileName, final int fileSize) throws IOException {
        init(fileName, fileSize);
    }

    /**
     * Description 初始化某个内存映射文件
     *
     * @param fileName 文件名
     * @param fileSize 文件尺寸
     * @throws IOException 打开文件出现异常
     */
    private void init(final String fileName, final int fileSize) throws IOException {

        // 文件名
        this.fileName = fileName;

        // 文件尺寸
        this.fileSize = fileSize;

        // 文件句柄
        this.file = new File(fileName);

        // 从文件名中获取到当前文件的全局偏移量
        this.fileFromOffset = Long.parseLong(this.file.getName());

        boolean ok = false;

        // 确保文件目录真实存在
        ensureDirOK(this.file.getParent());

        try {
            // 尝试打开文件
            this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();

            // 将文件映射到内存中
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);

            // 添加总的映射大小
            TOTAL_MAPPED_VIRTUAL_MEMORY.addAndGet(fileSize);

            // 总的映射数目加一
            TOTAL_MAPPED_FILES.incrementAndGet();

            // 设置成功标志位
            ok = true;

        } catch (FileNotFoundException e) {
            log.info("create file channel " + this.fileName + " Failed. ");
            throw e;
        } catch (IOException e) {
            log.info("map file " + this.fileName + " Failed. ");
            throw e;
        } finally {
            if (!ok && this.fileChannel != null) {
                this.fileChannel.close();
            }
        }
    }


    /**
     * Description 清空当前缓存中数据
     *
     * @param flushLeastPages
     * @return The current flushed position
     */
    public int flush(final int flushLeastPages) {

        // 如果不允许进行 Flush，则直接返回
        if (!this.isAbleToFlush(flushLeastPages)) {
            return this.getFlushedPosition();
        }

//        log.info(String.format("触发 Flush, 文件名： %s, 当前写入位置： %s, 当前已缓冲位置： %s, 将会 Flush 全部数据", this.fileName, this.wrotePosition.get(), this.flushedPosition.get()));

        // 判断是否正在由其他进程进行 Flush
        if (this.hold()) {

            // 获取到当前最大可读位置
            int value = getReadPosition();

            try {

                this.mappedByteBuffer.force();

            } catch (Throwable e) {
                log.warning("Error occurred when force data to disk.");
            }

            // 设置当前新的 Flush 位置
            this.flushedPosition.set(value);

            // 释放当前
            this.release();
        } else {
            log.warning("in flush, hold failed, flush offset = " + this.flushedPosition.get());
            this.flushedPosition.set(getReadPosition());
        }
        return this.getFlushedPosition();
    }

    /**
     * Description 实际向文件中写入数据
     * 此方法用于直接写入字节流的情况
     *
     * @param data
     * @return
     */
    public boolean appendMessage(final byte[] data) {
        int currentPos = this.wrotePosition.get();

        if ((currentPos + data.length) <= this.fileSize) {
            try {
                this.fileChannel.position(currentPos);
                this.fileChannel.write(ByteBuffer.wrap(data));
            } catch (Throwable e) {
                log.warning("Error occurred when append message to mappedFile.");
            }
            this.wrotePosition.addAndGet(data.length);
            return true;
        }

        return false;
    }

    public AppendMessageResult appendMessage(final DefaultBytesMessage message, final AppendMessageCallback cb) {

        // 首先判断传入参数有效
        assert message != null;
        assert cb != null;

        // 获取当前的写入位置
        int currentPos = this.wrotePosition.get();

        // 如果当前还是可写的
        if (currentPos < this.fileSize) {

            // 获取到实际的写入句柄
            ByteBuffer byteBuffer = this.mappedByteBuffer.slice();

            // 调整当前写入位置
            byteBuffer.position(currentPos);

            // 记录信息
            AppendMessageResult result = null;

            // 调用回调函数中的实际写入操作
            result = cb.doAppend(this.getFileFromOffset(), byteBuffer, this.fileSize - currentPos, message);

            this.wrotePosition.addAndGet(result.getWroteBytes());
            this.storeTimestamp = result.getStoreTimestamp();
            return result;
        }

        // 如果出现异常则报错
        log.warning("MappedPartition.appendMessage return null, wrotePosition: " + currentPos + " fileSize: " + fileSize + "");

        // 返回未知异常
        return new AppendMessageResult(AppendMessageStatus.UNKNOWN_ERROR);
    }

    public AppendMessageResult appendMessages(final List<DefaultBytesMessage> messages, final AppendMessageCallback cb) {

        // 首先判断传入参数有效
        assert messages != null;
        assert messages.size() > 0;
        assert cb != null;

        // 获取当前的写入位置
        int currentPos = this.wrotePosition.get();

        // 如果当前还是可写的
        if (currentPos < this.fileSize) {

            // 获取到实际的写入句柄
            ByteBuffer byteBuffer = this.mappedByteBuffer.slice();

            // 调整当前写入位置
            byteBuffer.position(currentPos);

            // 记录信息
            AppendMessageResult result = null;

            // 调用回调函数中的实际写入操作
            result = cb.doAppend(this.getFileFromOffset(), byteBuffer, this.fileSize - currentPos, messages);

            this.wrotePosition.addAndGet(result.getWroteBytes());

            this.storeTimestamp = result.getStoreTimestamp();

            return result;
        }

        // 如果出现异常则报错
        log.warning("MappedPartition.appendMessage return null, wrotePosition: " + currentPos + " fileSize: " + fileSize + "");

        // 返回未知异常
        return new AppendMessageResult(AppendMessageStatus.UNKNOWN_ERROR);
    }

    /**
     * Description 删除某个文件
     *
     * @param intervalForcibly
     * @return
     */
    public boolean destroy(final long intervalForcibly) {
        this.shutdown(intervalForcibly);

        if (this.isCleanupOver()) {
            try {
                this.fileChannel.close();
                log.info("close file channel " + this.fileName + " OK");

                long beginTime = System.currentTimeMillis();
                boolean result = this.file.delete();
                log.info("delete file[REF:" + this.getRefCount() + "] " + this.fileName
                        + (result ? " OK, " : " Failed, ") + "W:" + this.getWrotePosition() + " M:"
                        + this.getFlushedPosition() + ", "
                        + (now() - beginTime));
            } catch (Exception e) {
                log.warning("close file channel " + this.fileName + " Failed. ");
            }

            return true;
        } else {
            log.warning("destroy mapped file[REF:" + this.getRefCount() + "] " + this.fileName
                    + " Failed. cleanupOver: ");
        }

        return false;
    }

    /**
     * Description 判断是否允许 Flush
     *
     * @param flushLeastPages
     * @return
     */
    private boolean isAbleToFlush(final int flushLeastPages) {

        // 获取上次 Flush 的位置
        int flush = this.flushedPosition.get();

        // 获取当前缓存中的最大位置
        int write = getReadPosition();

        // 判断当前文件是否已经满了，如果满了则肯定可以 Flush
        if (this.isFull()) {
            return true;
        }

        // 判断系统参数设置，如果设置了有效地最小 Flush 页，则判断是否已经包含了足够页数
        if (flushLeastPages > 0) {
            return ((write / OS_PAGE_SIZE) - (flush / OS_PAGE_SIZE)) >= flushLeastPages;
        } else {
            // 否则只要有数据未 Flush 就进行操作
            return write > flush;
        }
    }

    /**
     * Description 当前可读的最大位置，也就是当前 MappedPartition 中存有数据的最大位置
     *
     * @return The max position which have valid data
     */
    public int getReadPosition() {
        return this.wrotePosition.get();
    }

    public long getStoreTimestamp() {
        return storeTimestamp;
    }

    public void setWrotePosition(int wrotePosition) {
        this.wrotePosition.set(wrotePosition);
    }

    public void setFlushedPosition(int flushedPosition) {

        this.flushedPosition.set(flushedPosition);
    }

    /**
     * Description 将内存映射文件标识为 True 或 False
     *
     * @param firstCreateInQueue
     */
    public void setFirstCreateInQueue(boolean firstCreateInQueue) {
        this.firstCreateInQueue = firstCreateInQueue;
    }

    public long getFileFromOffset() {
        return fileFromOffset;
    }

    public int getFlushedPosition() {
        return flushedPosition.get();
    }


    /**
     * Description 获取当前文件的最后修改时间
     *
     * @return
     */
    public long getLastModifiedTimestamp() {
        return this.file.lastModified();
    }

    /**
     * Description 判断当前文件是否已经写满
     *
     * @return
     */
    public boolean isFull() {
        return this.fileSize == this.wrotePosition.get();
    }

    public int getFileSize() {
        return fileSize;
    }

    /**
     * Description 创建共享序列
     *
     * @return
     */
    public ByteBuffer sliceByteBuffer() {
        return this.mappedByteBuffer.slice();
    }

    public String getFileName() {
        return fileName;
    }

    public int getWrotePosition() {
        return wrotePosition.get();
    }

    public boolean isAvailable() {
        return available;
    }

    public SelectMappedBufferResult selectMappedBuffer(int pos, int size) {
        int readPosition = getReadPosition();
        if ((pos + size) <= readPosition) {

            if (this.hold()) {
                ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
                byteBuffer.position(pos);
                ByteBuffer byteBufferNew = byteBuffer.slice();
                byteBufferNew.limit(size);
                return new SelectMappedBufferResult(this.fileFromOffset + pos, byteBufferNew, size, this);
            } else {
                log.warning("matched, but hold failed, request pos: " + pos + ", fileFromOffset: "
                        + this.fileFromOffset);
            }
        } else {
            log.warning("selectMappedBuffer request pos invalid, request pos: " + pos + ", size: " + size
                    + ", fileFromOffset: " + this.fileFromOffset);
        }

        return null;
    }

    /**
     * Description 根据设定的文件内起始偏移量，寻找合适的 ByteBuffer
     */
    public SelectMappedBufferResult selectMappedBuffer(int pos) {

        // 当前可读的最大范围
        int readPosition = getReadPosition();

        // 如果起始位置小于最大可读位置
        if (pos < readPosition && pos >= 0) {
            if (this.hold()) {

                ByteBuffer byteBuffer = this.mappedByteBuffer.slice();

                // 重新定位
                byteBuffer.position(pos);
                int size = readPosition - pos;

                // 获取当前 ByteBuffer 的拷贝句柄
                ByteBuffer byteBufferNew = byteBuffer.slice();
                byteBufferNew.limit(size);

                return new SelectMappedBufferResult(this.fileFromOffset + pos, byteBufferNew, size, this);
            }
        }

        return null;
    }

    public boolean isFirstCreateInQueue() {
        return firstCreateInQueue;
    }

    @Override
    public boolean cleanup() {

        try {
            
            long currentRef = this.refCount.get();

            if (this.isAvailable()) {
                log.warning("【Error】this file[REF:" + refCount + "] " + this.fileName
                        + " have not shutdown, stop unmapping.");
                return false;
            }

            if (this.isCleanupOver()) {
                log.warning("【Error】this file[REF:" + currentRef + "] " + this.fileName
                        + " have cleanup, do not do it again.");
                return true;
            }

            unmap(this.mappedByteBuffer);


            this.fileChannel.close();
            TOTAL_MAPPED_VIRTUAL_MEMORY.addAndGet(this.fileSize * (-1));
            TOTAL_MAPPED_FILES.decrementAndGet();
            log.info("unmap file[REF:" + currentRef + "] " + this.fileName + " OK");
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Description 初始化文件时进行热加载
     */
    public void warmMappedFile() {
        long beginTime = now();

        ByteBuffer byteBuffer = this.mappedByteBuffer.slice();

        for (int i = 0, j = 0; i < this.fileSize; i += MappedPartition.OS_PAGE_SIZE, j++) {

            byteBuffer.put(i, (byte) 0);

            // prevent gc
            if (j % 1000 == 0) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        log.info(String.format("mapped file warm-up done. mappedFile=%s, costTime=%s", this.getFileName(),
                System.currentTimeMillis() - beginTime));

    }


}
