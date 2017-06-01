
package wx.mq.common.message.status;

/**
 * Description 存放消息持久化存储结果状态
 */
public enum PutMessageStatus {
    // 存储成功
    PUT_OK,
    // 磁盘写入失败
    FLUSH_DISK_TIMEOUT,
    // 内存映射文件创建失败
    CREATE_MAPEDFILE_FAILED,
    // 消息异常
    MESSAGE_ILLEGAL,
    // 大小超出预定
    HEADERS_SIZE_EXCEEDED,
    PROPERTIES_SIZE_EXCEEDED,
    // 内存页繁忙
    OS_PAGECACHE_BUSY,
    // 服务已经停止运行
    SERVICE_NOT_AVAILABLE,
    // 其他错误
    UNKNOWN_ERROR,
}
