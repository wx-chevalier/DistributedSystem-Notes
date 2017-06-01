package wx.mq.util.fs.serialization;

import org.json.simple.parser.ParseException;
import wx.mq.local.LocalMessageQueue;

import java.io.IOException;
import java.util.logging.Logger;

import static wx.mq.util.fs.FSExtra.file2String;
import static wx.mq.util.fs.FSExtra.string2File;

/**
 * Description
 */
public abstract class ConfigManager {

    // 日志记录器
    private final static Logger log = Logger.getLogger(LocalMessageQueue.class.getName());

    public abstract String encode();

    public boolean load() {
        String fileName = null;
        try {
            fileName = this.configFilePath();
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

    public abstract String configFilePath();

    private boolean loadBak() {
        String fileName = null;
        try {
            fileName = this.configFilePath();
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

    public abstract void decode(final String jsonString) throws ParseException;

    public synchronized void persist() {
        String jsonString = this.encode(true);
        if (jsonString != null) {
            String fileName = this.configFilePath();
            try {
                string2File(jsonString, fileName);
            } catch (IOException e) {
                log.warning("【Error】persist file Exception, " + fileName);
            }
        }
    }

    public abstract String encode(final boolean prettyFormat);
}
