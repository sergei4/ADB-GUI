package dx.model;

import application.AdbUtils;
import dx.service.DeviceMonitorService;
import dx.service.Service;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceRegistry {

    private DeviceMonitorService deviceMonitorService;

    private Map<String, Device> deviceList = new ConcurrentHashMap<>();

    private Subject<Collection<Device>, Collection<Device>> deviceListSbj = BehaviorSubject.create(Collections.emptyList());

    public DeviceRegistry(DeviceMonitorService deviceMonitorService) {
        this.deviceMonitorService = deviceMonitorService;
        deviceMonitorService.addListener(new Service.Listener() {
            @Override
            public void onStart() {
                startObserveDeviceService();
            }

            @Override
            public void onStop() {
                setDevicesDisconnected();
                deviceListSbj.onNext(deviceList.values());
            }
        });
        if (deviceMonitorService.isRunning()) {
            startObserveDeviceService();
        }
    }

    public Observable<Collection<Device>> observeDeviceList() {
        return deviceListSbj;
    }

    private void setDevicesDisconnected() {
        for (Device device : deviceList.values()) {
            device.setConnected(false);
        }
    }

    private void startObserveDeviceService() {
        deviceMonitorService.observe()
                .switchMap(devices -> Observable.from(devices)
                        .map(Device::fromAdbLine)
                        .map(this::setDeviceInfo)
                        .subscribeOn(Schedulers.io())
                        .toList())
                .map(this::updDevices)
                .subscribe(deviceListSbj::onNext);
    }

    private Device setDeviceInfo(Device device) {
        String id = device.getId();

        String response = AdbUtils.run(id, "shell getprop ro.build.version.release");
        String[] split = response.split("\n");

        if (split.length > 0) {
            device.setAndroidApiName(split[0]);
        }
        return device;
    }

    private Collection<Device> updDevices(List<Device> devices) {
        for (Device device : deviceList.values()) {
            if (!devices.contains(device)) {
                device.setConnected(false);
            }
        }
        for (Device device : devices) {
            if (deviceList.containsKey(device.getId())) {
                deviceList.get(device.getId());
            } else {
                deviceList.put(device.getId(), device);
            }
            deviceList.get(device.getId()).setConnected(true);
        }
        return deviceList.values();
    }
}
