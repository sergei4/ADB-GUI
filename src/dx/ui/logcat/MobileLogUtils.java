package dx.ui.logcat;

import application.log.Logger;
import application.utils.FileUtils;
import dx.model.MobileDevice;
import dx.model.MobileDeviceLogLine;
import rx.Observable;
import rx.Subscriber;

import java.io.File;
import java.io.PrintWriter;


public class MobileLogUtils {
    public static void save(MobileDevice device, Observable<? extends MobileDeviceLogLine> lines, Runnable onFinish) throws Exception {
        File logFile = FileUtils.createLogFile(device);
        PrintWriter writer = new PrintWriter(logFile, "UTF-8");
        writer.println("Model: " + device.getModel());
        writer.println("OS version: " + device.getOsVersion());
        writer.println();

        lines.onBackpressureBuffer()
                .map(MobileDeviceLogLine::toString)
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
