package wx.mq.common.message.append;

import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.status.AppendMessageResult;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Write messages callback interface
 */
public interface AppendMessageCallback {

    /**
     * After message serialization, write MapedByteBuffer
     *
     * @param fileOffset
     * @param byteBuffer
     * @param maxBlank
     * @param message
     * @return How many bytes to write
     */
    AppendMessageResult doAppend(final long fileOffset, final ByteBuffer byteBuffer,
                                 final int maxBlank, final DefaultBytesMessage message);

    AppendMessageResult doAppend(final long fileOffset, final ByteBuffer byteBuffer,
                                 final int maxBlank, final List<DefaultBytesMessage> messages);
}
