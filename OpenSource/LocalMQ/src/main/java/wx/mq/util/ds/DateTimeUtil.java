package wx.mq.util.ds;

import java.util.Calendar;

/**
 * Description 时间与日期辅助
 */
public class DateTimeUtil {

    /**
     * Description 返回当前时间戳
     *
     * @return
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Description 将时间戳转化为时间字符串
     *
     * @param t
     * @return
     */
    public static String timeMillisToHumanString(final long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return String.format("%04d%02d%02d%02d%02d%02d%03d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND));
    }
}
