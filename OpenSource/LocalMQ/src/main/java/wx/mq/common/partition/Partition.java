package wx.mq.common.partition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Partition {

    protected volatile boolean cleanupOver = false;

    protected final AtomicLong refCount = new AtomicLong(1);
    protected volatile boolean available = true;

    private volatile long firstShutdownTimestamp = 0;

    /**
     * Description 判断当前内存映射文件是否被占用
     *
     * @return
     */
    public synchronized boolean hold() {

        // 如果当前尚未被关闭
        if (this.available) {

            // 并且引用值大于零，注意，该引用值初始为一
            if (this.refCount.getAndIncrement() > 0) {

                // 返回真
                return true;
            } else {

                // 否则进行减一操作，即减去刚才判断时候加的一
                this.refCount.getAndDecrement();
            }
        }

        return false;
    }

    /**
     * Description 停止某个内存映射文件
     *
     * @param intervalForcibly
     */
    public void shutdown(final long intervalForcibly) {
        if (this.available) {
            this.available = false;
            this.firstShutdownTimestamp = System.currentTimeMillis();
            this.release();
        } else if (this.getRefCount() > 0) {
            if ((System.currentTimeMillis() - this.firstShutdownTimestamp) >= intervalForcibly) {
                this.refCount.set(-1000 - this.getRefCount());
                this.release();
            }
        }
    }

    /**
     * Description 释放当前文件的写入锁
     */
    public void release() {
        long value = this.refCount.decrementAndGet();
        if (value > 0)
            return;

        synchronized (this) {

            this.cleanupOver = this.cleanup();
        }
    }

    public abstract boolean cleanup();

    public long getRefCount() {
        return this.refCount.get();
    }

    public boolean isCleanupOver() {
        return this.refCount.get() <= 0;
    }
}
