package dx.model.android;

import application.log.Logger;
import dx.helpers.AdbHelper;
import dx.model.MobileDevice;
import dx.model.MobileDeviceVisitor;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AndroidDevice extends MobileDevice {

    private final Map<String, AndroidProcess> processList = new ConcurrentHashMap<>();

    private Subscription updProcessSubscription;

    public AndroidDevice(String adbLine) {
        String[] deviceDescriptionSplit = adbLine.split("\\s+");
        String id = deviceDescriptionSplit[0];
        String model = "UNKNOWN";
        for (String descriptionTemp : deviceDescriptionSplit) {
            if (descriptionTemp.contains("model:")) {
                model = descriptionTemp.split("model:")[1];
            }
        }
        setId(id);
        setModel(model);
    }

    protected void onConnect() {
        if (updProcessSubscription == null || updProcessSubscription.isUnsubscribed()) {
            updProcessSubscription =
                    Observable.interval(1, TimeUnit.SECONDS)
                            .flatMap(c -> observeProcessListInternal())
                            .flatMapIterable(items -> items)
                            .subscribe(
                                    process -> processList.put(process.getPid(), process),
                                    e -> Logger.e(e.getMessage() != null ? e.getMessage() : "Error during collect device active process list")
                            );
        }
    }

    @Override
    protected void onDisconnect() {
        if (updProcessSubscription != null && !updProcessSubscription.isUnsubscribed()) {
            updProcessSubscription.unsubscribe();
        }
    }

    public Collection<AndroidProcess> getProcessList() {
        List<AndroidProcess> result = new ArrayList<>();
        processList.forEach((key, androidProcess) -> {
            if (androidProcess.getUser().startsWith("u")) {
                result.add(androidProcess);
            }
        });
        return result;
    }

    private Observable<List<AndroidProcess>> observeProcessListInternal() {
        return Observable.<AndroidProcess>unsafeCreate(
                s -> {
                    String result = AdbHelper.getProcessList(getId());
                    String[] lines = result.split("\n");
                    if (lines.length > 1) {
                        for (int i = 1; i < lines.length; i++) {
                            String[] processLine = lines[i].split("\\s+");
                            String user = processLine[0];
                            String pid = processLine[1];
                            String name = processLine[processLine.length - 1];
                            s.onNext(new AndroidProcess(user, pid, name));
                        }
                    }
                    s.onCompleted();
                })
                .toList().subscribeOn(Schedulers.io());
    }

    public Observable<LogcatLine> observeLog() {
        return AdbHelper.observeLogcat(getId())
                .map(this::parseLogLine)
                .subscribeOn(Schedulers.io());
    }

    public Observable<LogcatLine> observeFullDeviceLog() {
        return Observable.<String>unsafeCreate(s -> {
            String result = AdbHelper.getLogcat(getId(), 12000);
            String[] lines = result.split("\n");
            for (String line : lines) {
                s.onNext(line);
            }
            s.onCompleted();
        }).filter(s -> !s.isEmpty())
                .map(this::parseLogLine)
                .subscribeOn(Schedulers.io());
    }

    private LogcatLine parseLogLine(String logLine) {
        LogcatLine logcatLine = new LogcatLine(logLine);
        logcatLine.setProcess(processList.get(logcatLine.getProcessUuid()));
        return logcatLine;
    }

    @Override
    public String toString() {
        return String.format("Android id: %s, Model: %s, api: %s, connected: %s", getId(), getModel(), getOsVersion(), isConnected());
    }

    @Override
    public void visitBy(MobileDeviceVisitor visitor) {
        visitor.visitBy(this);
    }
}
