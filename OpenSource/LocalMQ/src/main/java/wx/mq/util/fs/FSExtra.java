package wx.mq.util.fs;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.logging.Logger;

import static wx.mq.local.LocalMessageQueueConfig.*;

/**
 * @function 包含常用的文件系统操作方法
 */
public class FSExtra {

    // 日志记录
    private final static Logger log = Logger.getLogger(FSExtra.class.getName());

    /**
     * Description 确定目录是否存在，不存在则创建
     *
     * @param dirName 目录名
     * @return void
     */
    public static void ensureDirOK(final String dirName) {
        if (dirName != null) {
            File f = new File(dirName);
            if (!f.exists()) {
                boolean result = f.mkdirs();
//                log.info(dirName + " mkdir " + (result ? "OK" : "Failed"));
            }
        }
    }

    /**
     * Description 将偏移量写入到文件名中
     *
     * @param offset
     * @return
     */
    public static String offset2FileName(final long offset) {
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(20);
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(false);
        return nf.format(offset);
    }

    /**
     * Description 检测磁盘中的 Commit Offset
     *
     * @param offsetPy
     * @param maxOffsetPy
     * @return
     */
    public static boolean checkInDiskByCommitOffset(long offsetPy, long maxOffsetPy) {
        long memory = (long) (getTotalPhysicalMemorySize() * (accessMessageInMemoryMaxRatio / 100.0));
        return (maxOffsetPy - offsetPy) > memory;
    }

    /**
     * Description 获取总的物理内存
     *
     * @return
     */
    public static long getTotalPhysicalMemorySize() {

        // 最大物理内存为 4G
        long physicalTotal = 1024 * 1024 * 1024 * 4L;
        OperatingSystemMXBean osmxb = ManagementFactory.getOperatingSystemMXBean();
        if (osmxb instanceof com.sun.management.OperatingSystemMXBean) {
            physicalTotal = ((com.sun.management.OperatingSystemMXBean) osmxb).getTotalPhysicalMemorySize();
        }

        return physicalTotal;
    }

    public static boolean isTheBatchFull(int sizePy, int maxMsgNums, int bufferTotal, int messageTotal, boolean isInDisk) {

        if (0 == bufferTotal || 0 == messageTotal) {
            return false;
        }

        if ((messageTotal + 1) >= maxMsgNums) {
            return true;
        }

        if (isInDisk) {
            if ((bufferTotal + sizePy) > maxTransferBytesOnMessageInDisk) {
                return true;
            }

            if ((messageTotal + 1) > maxTransferCountOnMessageInDisk) {
                return true;
            }
        } else {
            if ((bufferTotal + sizePy) > maxTransferBytesOnMessageInMemory) {
                return true;
            }

            if ((messageTotal + 1) > maxTransferCountOnMessageInMemory) {
                return true;
            }
        }

        return false;
    }


    /**
     * Description 读取文件内容并且转化为字符串
     *
     * @param fileName
     * @return
     */
    public static String file2String(final String fileName) {
        File file = new File(fileName);
        return file2String(file);
    }

    /**
     * Description 读取文件内容并且转化为字符串
     *
     * @param file
     * @return
     */
    public static String file2String(final File file) {
        if (file.exists()) {
            char[] data = new char[(int) file.length()];
            boolean result = false;

            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
                int len = fileReader.read(data);
                result = len == data.length;
            } catch (IOException e) {
                // e.printStackTrace();
            } finally {
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (result) {
                return new String(data);
            }
        }
        return null;
    }

    /**
     * Description 读取文件内容并且转化为字符串
     *
     * @param url
     * @return
     */
    public static String file2String(final URL url) {
        InputStream in = null;
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setUseCaches(false);
            in = urlConnection.getInputStream();
            int len = in.available();
            byte[] data = new byte[len];
            in.read(data, 0, len);
            return new String(data, "UTF-8");
        } catch (Exception ignored) {
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

        return null;
    }


    /**
     * Description 将字符串写入到文件中
     *
     * @param str
     * @param fileName
     * @throws IOException
     */
    public static void string2File(final String str, final String fileName) throws IOException {

        String tmpFile = fileName + ".tmp";
        string2FileNotSafe(str, tmpFile);

        String bakFile = fileName + ".bak";
        String prevContent = file2String(fileName);
        if (prevContent != null) {
            string2FileNotSafe(prevContent, bakFile);
        }

        File file = new File(fileName);
        file.delete();

        file = new File(tmpFile);
        file.renameTo(new File(fileName));
    }


    /**
     * Description 不安全地写入方式
     *
     * @param str
     * @param fileName
     * @throws IOException
     */
    public static void string2FileNotSafe(final String str, final String fileName) throws IOException {
        File file = new File(fileName);
        File fileParent = file.getParentFile();
        if (fileParent != null) {
            fileParent.mkdirs();
        }
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(str);
        } catch (IOException e) {
            throw e;
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

}


