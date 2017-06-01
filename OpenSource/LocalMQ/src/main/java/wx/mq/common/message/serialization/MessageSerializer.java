package wx.mq.common.message.serialization;

import java.nio.charset.Charset;

public class MessageSerializer {

    // 消息体最大长度 /Byte
    public final static int MAX_MESSAGE_SIZE = 257 * 1024;

    // File at the end of the minimum fixed length empty
    public final static int END_FILE_MIN_BLANK_LENGTH = 4 + 4;

    // 消息序号长度
    public final static int MSG_ID_LENGTH = 8 + 8;

    // 编码
    public final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    // 消息的标识
    public final static int MESSAGE_MAGIC_CODE = 0xAABBCCDD ^ 1880681586 + 8;

    // 用于标识文件的最后一个消息，即在空间不够的情况下标识最后一个消息
    public final static int BLANK_MAGIC_CODE = 0xBBCCDDEE ^ 1880681586 + 8;
    
}
