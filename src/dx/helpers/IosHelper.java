package dx.helpers;

import dx.Executor;

import java.io.File;
import java.util.function.Supplier;

public class IosHelper {
    private static Supplier<String> libExecLocation;

    public static void setLibExecLocation(Supplier<String> libExecLocation) {
        IosHelper.libExecLocation = libExecLocation;
    }

    private static String composeCommand(String command) {
        return composeCommand(command, null);
    }

    private static String composeCommand(String command, String args) {
        return (new File(libExecLocation.get(), command).getAbsolutePath()) + (args == null ? "" : " " + args);
    }

    public static String deviceList() {
        return Executor.run(composeCommand("idevice_id", "-l"));
    }

    public static String getDeviceProperties(String deviceId) {
        return Executor.run(composeCommand("ideviceinfo", "-u " + deviceId));
    }

    public static String createScreenshot(String deviceId, String outFile) {
        String args = "-u " + deviceId + " " + outFile;
        String result = Executor.run(composeCommand("idevicescreenshot", args));
        return !result.contains("Could not start") ? "" : result;
    }

    public static String install(String deviceId, String ipaFile) {
        String args = "-u " + deviceId + " -i" + ipaFile;
        String result = Executor.run(composeCommand("ideviceinstaller", args));
        return result.contains("Complete") ? null : "Error"; //Todo:
    }

    public static rx.Observable<String> observeDeviceLog(String deviceId) {
        return Executor.observeProcess(composeCommand("idevicesyslog", "-u " + deviceId))
                .filter(s -> !s.isEmpty());
    }
}
