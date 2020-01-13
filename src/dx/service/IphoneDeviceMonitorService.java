package dx.service;

import application.log.Logger;
import dx.helpers.IosHelper;
import dx.model.ios.IphoneDevice;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class IphoneDeviceMonitorService extends MobileDeviceMonitorService<IphoneDevice> {

    public static IphoneDeviceMonitorService instance = new IphoneDeviceMonitorService("iphone");

    private ConnectableObservable<List<IphoneDevice>> deviceObserver;

    private IphoneDeviceMonitorService(String serviceName) {
        super(serviceName);
        Observable<String> iphoneUuidObserver = Observable.<String>unsafeCreate(
                s -> {
                    String result = IosHelper.deviceList();
                    String[] split = result.split("\n");
                    for (String line : split) {
                        s.onNext(line);
                    }
                    s.onCompleted();
                })
                .filter(uuid -> !uuid.equals(""))
                .filter(uuid -> !uuid.contains("ERROR"))
                .subscribeOn(Schedulers.io());

        deviceObserver = Observable.interval(1, TimeUnit.SECONDS)
                .flatMap(emitter -> isRunning() ? iphoneUuidObserver.toList() : Observable.<String>empty().toList())
                .switchMap(uuids -> Observable.from(uuids)
                        .map(IphoneDevice::new)
                        .doOnNext(this::fillProperties)
                        //.mergeWith(Observable.just(new IphoneDevice.Test()))
                        .toList())
                .publish();
    }

    private void fillProperties(IphoneDevice device) {
        String result = IosHelper.getDeviceProperties(device.getId());
        Properties props = new Properties();
        try {
            props.load(new StringReader(result));
            device.setModel(props.getProperty("ProductType"));
            device.setOsVersion(props.getProperty("ProductVersion"));
        } catch (IOException e) {
            Logger.e("Couldn't read iphone device properties");
        }
    }

    @Override
    protected ConnectableObservable<List<IphoneDevice>> getDeviceObservable() {
        return deviceObserver;
    }
}
