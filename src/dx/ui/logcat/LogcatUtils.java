package dx.ui.logcat;

import application.FileUtils;
import application.log.Logger;
import dx.model.Device;
import dx.model.LogcatLine;
import rx.Observable;
import rx.Subscriber;

import java.io.File;
import java.io.PrintWriter;


public class LogcatUtils {
    public static void save(Device device, Observable<LogcatLine> lines, Runnable onFinish) throws Exception {
        File logFile = FileUtils.createLogFile(device);
        PrintWriter writer = new PrintWriter(logFile, "UTF-8");
        writer.println("Model: " + device.getModel());
        writer.println("Android version: " + device.getAndroidApiName());
        writer.println();

        lines.onBackpressureBuffer()
                .map(LogcatLine::toString)
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        Logger.fs("Log saved: " + logFile.getAbsolutePath());
                        writer.close();
                        onFinish.run();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Logger.es("Log hasn't been created");
                        writer.close();
                        onFinish.run();
                    }

                    @Override
                    public void onNext(String s) {
                        writer.println(s);
                    }
                });
    }
}
