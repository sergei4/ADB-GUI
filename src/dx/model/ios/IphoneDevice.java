package dx.model.ios;

import dx.helpers.IosHelper;
import dx.model.MobileDevice;
import dx.model.MobileDeviceLogLine;
import dx.model.MobileDeviceVisitor;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IphoneDevice extends MobileDevice {
    private Map<String, IphoneProcess> processList = new ConcurrentHashMap<>();

    public IphoneDevice(String id) {
        super(id);
    }

    @Override
    protected void onConnect() {

    }

    @Override
    protected void onDisconnect() {

    }

    @Override
    public Collection<IphoneProcess> getProcessList() {
        return processList.values();
    }

    @Override
    public Observable<? extends MobileDeviceLogLine> observeLog() {
        return getLogObservable()
                .map(IphoneLogLine::new)
                .scan((logLine, logLine2) -> {
                    if (logLine2.getDeviceProcess().getProcessId().equals("?")) {
                        logLine2.setIphoneProcess(logLine.getDeviceProcess());
                    }
                    return logLine2;
                })
                .doOnNext(this::updProcessList)
                .subscribeOn(Schedulers.io());
    }

    private void updProcessList(IphoneLogLine logLine) {
        IphoneProcess deviceProcess = logLine.getDeviceProcess();
        processList.put(deviceProcess.getProcessId(), deviceProcess);
    }

    protected Observable<String> getLogObservable() {
        return IosHelper.observeDeviceLog(getId());
    }

    @Override
    public Observable<MobileDeviceLogLine> observeFullDeviceLog() {
        return Observable.empty();
    }

    @Override
    public void visitBy(MobileDeviceVisitor visitor) {
        visitor.visitBy(this);
    }

    @Override
    public String toString() {
        return String.format("Iphone id: %s, Model: %s, connected: %s", getId(), getModel(), isConnected());
    }

    /**
     * Debuggable class
     */
    public static class Test extends IphoneDevice {
        public Test() {
            super("iphone-test-uuid");
            setModel("IPHONE TEST DEVICE");
            setOsVersion("0.0.0");
        }

        @Override
        public Observable<String> getLogObservable() {
            File logFile = new File(System.getProperty("user.dir") + File.separator + "test" + File.separator + "files", "iphone_test_log.txt");
            if (logFile.exists()) {
                System.out.println("log file: " + logFile.getAbsolutePath());
                return Observable.unsafeCreate(
                        s -> {
                            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    s.onNext(line);
                                    //Thread.sleep(10);
                                }
                            } catch (Exception e) {
                                s.onError(e);
                            }
                            s.onCompleted();
                        });
            } else {
                return Observable.empty();
            }
        }
    }
}
