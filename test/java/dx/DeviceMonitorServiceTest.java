package dx;

import dx.helpers.AdbHelper;
import dx.helpers.IosHelper;
import dx.model.MobileDeviceRegistry;
import dx.service.AndroidDeviceMonitorService;
import dx.service.IphoneDeviceMonitorService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class DeviceMonitorServiceTest {

    @Before
    public void before() {
        AdbHelper.setAdbExecLocation(() -> "/Users/eremkin/Library/Android/sdk/platform-tools/");

        String path = new File(System.getProperty("user.dir"), "platform-tools" + File.separator + "_nix").getAbsolutePath();
        IosHelper.setLibExecLocation(() -> path);
    }

    @Test
    public void observeDeviceVisualTest() throws Exception {

        AndroidDeviceMonitorService.instance.start();
        IphoneDeviceMonitorService.instance.start();

        MobileDeviceRegistry mobileDeviceRegistry = new MobileDeviceRegistry(
                AndroidDeviceMonitorService.instance,
                IphoneDeviceMonitorService.instance
        );
        mobileDeviceRegistry.observeDeviceList()
                .flatMapIterable(l -> l)
                .doOnNext(device -> System.out.println(device))
                .take(50)
                .toBlocking()
                .subscribe();
    }
//
//    @Test
//    public void observeDeviceProcessListTest() throws Exception {
//        MobileDevice device = new MobileDevice("emulator-5554");
//        device.observeProcessList()
//                .subscribe();
//
//        Thread.sleep(100000);
//    }
//
//    @Test
//    public void observeLogcatTest() throws Exception {
//        MobileDevice device = new MobileDevice("emulator-5554");
//        device.setConnected(true);
//        device.observeLog()
//                .doOnNext(line -> System.out.println(line))
//                .subscribe();
//
//        Thread.sleep(10000);
//    }
}
