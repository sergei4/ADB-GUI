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
            //Todo: remove late
            device.setSelected(false);
        }
    }

    private void startObserveDeviceService() {
        deviceMonitorService.observe()
                .doOnNext(list -> setDevicesDisconnected())
                .flatMap(devices -> Observable.from(devices)
                        .map(Device::fromAdbLine)
                        .doOnNext(device -> device.setConnected(true))
                        .map(this::setDeviceInfo)
                        .doOnNext(device -> deviceList.put(device.getId(), device))
                        .subscribeOn(Schedulers.io())
                        .toList())
                .map(list -> deviceList.values())
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
}
