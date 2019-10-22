package dx.service;

import application.AdbUtils;
import application.log.Logger;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DeviceMonitorService implements Service {

    public static DeviceMonitorService instance = new DeviceMonitorService();

    private Observable<String> adbDeviceObserver;

    private ConnectableObservable<List<String>> deviceObserver;
    private rx.Subscription subscription;

    private Set<Service.Listener> listeners = new HashSet<>();

    private DeviceMonitorService() {
        adbDeviceObserver = Observable.<String>unsafeCreate(
                s -> {
                    String result = AdbUtils.run("devices -l");
                    String[] split = result.split("\n");
                    for (String line : split) {
                        s.onNext(line);
                    }
                    s.onCompleted();
                })
                .filter(s -> s.contains("transport_id"))
                .subscribeOn(Schedulers.io());
    }

    public void addListener(DeviceMonitorService.Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(DeviceMonitorService.Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void start() {
        if (!isRunning()) {
            Logger.d("Start service");
            deviceObserver = rx.Observable.interval(1, TimeUnit.SECONDS)
                    .flatMap(emitter -> adbDeviceObserver.toList())
                    .publish();
            subscription = deviceObserver.connect();
            for (Service.Listener l : listeners) {
                l.onStart();
            }
        } else {
            Logger.d("Service is already running");
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            subscription.unsubscribe();
            for (Service.Listener l : listeners) {
                l.onStop();
            }
            Logger.d("Someone stopped me!!!");
        }
    }

    @Override
    public boolean isRunning() {
        return subscription != null && !subscription.isUnsubscribed();
    }

    public Observable<List<String>> observe() {
        if (isRunning()) {
            return deviceObserver;
        } else {
            Logger.d("Service hasn't been started yet");
            return rx.Observable.empty();
        }
    }
}
