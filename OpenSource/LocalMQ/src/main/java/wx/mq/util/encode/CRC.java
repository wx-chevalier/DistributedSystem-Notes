package wx.mq.util.encode;

import java.util.zip.CRC32;

/**
 * Description 存放编码相关工具
 */
public class CRC {


    /**
     * Description 对某个字节数组计算 CRC
     * @param array
     * @return
     */
    public static int crc32(byte[] array) {
        if (array != null) {
            return crc32(array, 0, array.length);
        }

        return 0;
    }


    /**
     * Description 根据指定的起始与长度计算 CRC
     * @param array
     * @param offset
     * @param length
     * @return
     */
    public static int crc32(byte[] array, int offset, int length) {
        CRC32 crc32 = new CRC32();
        crc32.update(array, offset, length);
        return (int) (crc32.getValue() & 0x7FFFFFFF);
    }
}
