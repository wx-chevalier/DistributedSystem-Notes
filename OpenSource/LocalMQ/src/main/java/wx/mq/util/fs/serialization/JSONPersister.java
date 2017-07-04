package wx.mq.util.fs.serialization;

import org.json.simple.parser.ParseException;
import wx.mq.local.LocalMessageQueue;

import java.io.IOException;
import java.util.logging.Logger;

import static wx.mq.util.fs.FSExtra.file2String;
import static wx.mq.util.fs.FSExtra.string2File;

/**
 * Description JSON 持久化工具
 */
public abstract class JSONPersister {

    // 日志记录器
    private final static Logger log = Logger.getLogger(LocalMessageQueue.class.getName());

    /**
     * Description 获取持久化之后的文件路径
     *
     * @return
     */
    public abstract String getPersistFilePath();

    /**
     * Description 对于类属性进行编码
     *
     * @return
     */
    public abstract String encode();

    public abstract String encode(final boolean prettyFormat);

    /**
     * Description 对于获取到的 JSON 字符串进行解码
     *
     * @param jsonString
     * @throws ParseException
     */
    public abstract void decode(final String jsonString) throws ParseException;

    /**
     * Description 加载 JSON 字符串，并且将其解码为内存对象
     *
     * @return
     */
    public boolean load() {
        String fileName = null;
        try {
            fileName = this.getPersistFilePath();
            String jsonString = file2String(fileName);

            if (null == jsonString || jsonString.length() == 0) {
                return this.loadBak();
            } else {
                this.decode(jsonString);
                log.info(String.format("load %s OK", fileName));
                return true;
            }
        } catch (Exception e) {
            log.warning("load " + fileName + " Failed, and try to load backup file");
            return this.loadBak();
        }
    }

    /**
     * Description 在正常文件被损坏后加载备份文件
     *
     * @return
     */
    private boolean loadBak() {
        String fileName = null;
        try {
            fileName = this.getPersistFilePath();
            String jsonString = file2String(fileName + ".bak");
            if (jsonString != null && jsonString.length() > 0) {
                this.decode(jsonString);
                log.info("load " + fileName + " OK");
                return true;
            }
        } catch (Exception e) {
            log.warning("【Error】load " + fileName + " Failed");
            return false;
        }

        return true;
    }

    /**
     * Description 进行持久化存储工作
     */
    public synchronized void persist() {
        String jsonString = this.encode(true);
        if (jsonString != null) {
            String fileName = this.getPersistFilePath();
            try {
                string2File(jsonString, fileName);
            } catch (IOException e) {
                log.warning("【Error】persist file Exception, " + fileName);
            }
        }
    }

}
