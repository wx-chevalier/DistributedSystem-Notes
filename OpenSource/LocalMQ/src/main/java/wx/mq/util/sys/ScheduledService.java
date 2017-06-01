package wx.mq.util.sys;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Description 自带 ExecutorService 的
 */
public abstract class ScheduledService {

    // 内置的定期执行器
    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(this.getServiceName()));

    // 获取当前 Service 名称
    public abstract String getServiceName();

    // 定期执行的操作
    public abstract void run();

    /**
     * Description 启动定期执行的服务
     *
     * @param initialDelay
     * @param period
     * @param timeUnit
     */
    public void start(long initialDelay, long period, TimeUnit timeUnit) {

        this.scheduledExecutorService.scheduleAtFixedRate(this::run, initialDelay, period, timeUnit);

    }


}
