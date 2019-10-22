package dx;

import application.AdbInstallLocationProvider;
import application.AdbUtils;
import dx.model.Device;
import dx.model.DeviceRegistry;
import dx.service.DeviceMonitorService;
import org.junit.Before;
import org.junit.Test;

public class DeviceMonitorServiceTest {

    @Before
    public void before() {
        AdbUtils.setAdbInstallLocationProvider(new AdbInstallLocationProvider() {
            @Override
            public String getAdbInstallLocatoin() {
                return "/Users/eremkin/Library/Android/sdk/platform-tools/";
            }
        });
    }

    @Test
    public void observeDeviceTest() throws Exception {

        DeviceRegistry deviceRegistry = new DeviceRegistry(DeviceMonitorService.instance);
        deviceRegistry.observeDeviceList()
                .flatMapIterable(l -> l)
                .doOnNext(device -> System.out.println(device))
                .subscribe();

        DeviceMonitorService.instance.start();
        Thread.sleep(100000);
    }

    @Test
    public void observeDeviceProcessListTest() throws Exception {
        Device device = new Device("emulator-5554");
        device.observeProcessList()
                .subscribe();

        Thread.sleep(100000);
    }

    @Test
    public void observeLogcatTest() throws Exception {
        Device device = new Device("emulator-5554");
        device.setSelected(true);
        device.observeLog()
                .doOnNext(line -> System.out.println(line))
                .subscribe();

        Thread.sleep(10000);
    }
}
