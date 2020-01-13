package dx.service;

import dx.helpers.AdbHelper;
import dx.model.android.AndroidDevice;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AndroidDeviceMonitorService extends MobileDeviceMonitorService<AndroidDevice> {

    public static AndroidDeviceMonitorService instance = new AndroidDeviceMonitorService("android");

    private ConnectableObservable<List<AndroidDevice>> deviceObserver;

    private AndroidDeviceMonitorService(String serviceName) {
        super(serviceName);
        Observable<String> adbDeviceObserver = Observable.<String>unsafeCreate(
                s -> {
                    String result = AdbHelper.deviceList();
                    String[] split = result.split("\n");
                    for (String line : split) {
                        s.onNext(line);
                    }
                    s.onCompleted();
                })
                .filter(s -> s.contains("transport_id"))
                .subscribeOn(Schedulers.io());

        deviceObserver = rx.Observable.interval(1, TimeUnit.SECONDS)
                .flatMap(emitter -> isRunning() ? adbDeviceObserver.toList() : Observable.<String>empty().toList())
                .switchMap(adbLine -> Observable.from(adbLine)
                        .map(AndroidDevice::new)
                        .doOnNext(this::setDeviceInfo)
                        .toList())
                .publish();
    }

    private void setDeviceInfo(AndroidDevice device) {
        String response = AdbHelper.getAndroidApi(device.getId());
        String[] split = response.split("\n");

        if (split.length > 0) {
            device.setOsVersion(split[0]);
        }
    }

    @Override
    protected ConnectableObservable<List<AndroidDevice>> getDeviceObservable() {
        return deviceObserver;
    }
}
