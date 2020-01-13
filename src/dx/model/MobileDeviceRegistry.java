package dx.model;

import dx.model.android.AndroidDevice;
import dx.model.ios.IphoneDevice;
import dx.service.MobileDeviceMonitorService;
import rx.Observable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MobileDeviceRegistry {

    private Map<String, MobileDevice> deviceList = new ConcurrentHashMap<>();

    private Observable<Collection<MobileDevice>> allDevices;

    public MobileDeviceRegistry(
            MobileDeviceMonitorService<AndroidDevice> androidDeviceMonitorService,
            MobileDeviceMonitorService<IphoneDevice> iphoneDeviceMonitorService
    ) {
        Observable<List<AndroidDevice>> androidDevices = androidDeviceMonitorService.observe();
        Observable<List<IphoneDevice>> iphoneDevices = iphoneDeviceMonitorService.observe();

        allDevices = Observable.combineLatest(androidDevices, iphoneDevices, (android, iphone) -> {
            List<MobileDevice> result = new ArrayList<>();
            result.addAll(android);
            result.addAll(iphone);
            return result;
        }).map(this::updDeviceList).share();
    }

    public Observable<Collection<MobileDevice>> observeDeviceList() {
        return allDevices.onBackpressureBuffer();
    }

    private Collection<MobileDevice> updDeviceList(List<MobileDevice> devices) {
        for (MobileDevice device : deviceList.values()) {
            if (!devices.contains(device)) {
                device.setConnected(false);
            }
        }
        for (MobileDevice device : devices) {
            if (deviceList.containsKey(device.getId())) {
                deviceList.get(device.getId()).copyProperties(device);
            } else {
                deviceList.put(device.getId(), device);
            }
            deviceList.get(device.getId()).setConnected(true);
        }
        return deviceList.values();
    }
}
