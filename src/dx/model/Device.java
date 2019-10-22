package dx.model;

import application.ADBHelper;
import application.AdbUtils;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Device {

    private String id = "";

    private String model = "";

    private boolean isEmulator;

    private boolean isConnected;

    private String androidApi;
    private String androidApiName = "indefinite";

    private final Map<String, DeviceProcess> processList = new ConcurrentHashMap<>();

    private Subscription updProcessSubscription;

    public Device(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEmulator() {
        return isEmulator;
    }

    public void setEmulator(boolean emulator) {
        isEmulator = emulator;
    }

    public String getAndroidApiName() {
        return androidApiName;
    }

    public void setAndroidApiName(String androidApiName) {
        this.androidApiName = androidApiName;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public static Device fromAdbLine(String adbLine) {
        String[] deviceDescriptionSplit = adbLine.split("\\s+");
        String id = deviceDescriptionSplit[0];
        Device device = new Device(id);
        String model = "Device";
        for (String descriptionTemp : deviceDescriptionSplit) {
            if (descriptionTemp.contains("model:")) {
                model = descriptionTemp.split("model:")[1];
            }
        }
        device.model = model;
        if (id.startsWith("emulator")) {
            device.setEmulator(true);
        }
        return device;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            if (updProcessSubscription == null || updProcessSubscription.isUnsubscribed()) {
                updProcessSubscription =
                        observeProcessList()
                                .flatMapIterable(items -> items)
                                .doOnNext(process -> {
                                    processList.put(process.getPid(), process);
                                })
                                .subscribe(
                                        process -> processList.put(process.getPid(), process),
                                        e -> {
                                            //Todo: replace by logger
                                        }
                                );
            }
        } else {
            if (updProcessSubscription != null && !updProcessSubscription.isUnsubscribed()) {
                updProcessSubscription.unsubscribe();
            }
        }
    }

    public Observable<List<DeviceProcess>> observeProcessList() {
        return Observable.<DeviceProcess>unsafeCreate(s -> {
            String result = AdbUtils.run(id, "shell ps");
            String[] lines = result.split("\n");
            if (lines.length > 1) {
                for (int i = 1; i < lines.length; i++) {
                    String[] processLine = lines[i].split("\\s+");
                    String user = processLine[0];
                    String pid = processLine[1];
                    String name = processLine[processLine.length - 1];
                    s.onNext(new DeviceProcess(user, pid, name));
                }
            }
            s.onCompleted();
        }).toList().subscribeOn(Schedulers.io());
    }

    public Observable<LogcatLine> observeLog() {
        return ADBHelper.observeLogcat(id)
                .map(this::parseLogLine);
    }

    private LogcatLine parseLogLine(String logLine) {
        LogcatLine logcatLine = new LogcatLine(logLine);
        logcatLine.setProcess(processList.get(logcatLine.getProcessUuid()));
        return logcatLine;
    }

    @Override
    public String toString() {
        return String.format("Id: %s, Model: %s, api: %s, connected: %s", id, model, androidApiName, isConnected);
    }
}
