package application;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Deprecated
public class AdbUtils {

    public static ExecutorService executor = Executors.newScheduledThreadPool(2);
    public static ExecutorService executorTimeout = Executors.newScheduledThreadPool(2);

    public interface TimeCallListener {
        void timeout();

        void error(Exception e);
    }

    public static <T> T timedCall(Callable<T> c, long timeout, TimeUnit timeUnit, final TimeCallListener timeCallListener) {
        FutureTask<T> task = new FutureTask<T>(c);
        executor.execute(task);
        executorTimeout.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    task.get(timeout, timeUnit);
                } catch (InterruptedException e) {
                    timeCallListener.error(e);
                } catch (ExecutionException e) {
                    timeCallListener.error(e);
                } catch (TimeoutException e) {
                    timeCallListener.timeout();
                }
            }
        });
        return null;
    }
}
