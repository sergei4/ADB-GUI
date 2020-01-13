package dx.service;

import application.log.Logger;
import dx.model.MobileDevice;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class MobileDeviceMonitorService<D extends MobileDevice> implements Service {

    private String serviceName = "";

    private boolean running;

    private rx.Subscription subscription;

    private Set<Service.Listener> listeners = new HashSet<>();

    public MobileDeviceMonitorService(String serviceName) {
        this.serviceName = serviceName;
    }

    abstract protected ConnectableObservable<List<D>> getDeviceObservable();

    public void addListener(Service.Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Service.Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void start() {
        if (!running) {
            log("start");
            if (subscription == null || subscription.isUnsubscribed()) {
                subscription = getDeviceObservable().connect();
            }
            running = true;
            for (Service.Listener l : listeners) {
                l.onStart();
            }
        } else {
            log("is already running");
        }
    }

    @Override
    public void stop() {
        if (running) {
            running = false;
            for (Service.Listener l : listeners) {
                l.onStop();
            }
            log("someone stopped me!!!");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public Observable<List<D>> observe() {
        return getDeviceObservable();
    }

    private void log(String message) {
        Logger.d(String.format("MobileDevice monitor service %s: %s", serviceName, message));
    }
}
