package dx.helpers;

import application.intentbroadcasts.IntentBroadcast;
import application.log.Logger;
import application.preferences.Preferences;
import dx.Executor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class AdbHelper {

    private static Supplier<String> adbExecLocation;

    public static void setAdbExecLocation(Supplier<String> adbExecLocation) {
        AdbHelper.adbExecLocation = adbExecLocation;
    }

    public static String composeAdbCommand(String command) {
        return composeAdbCommand(null, command);
    }

    public static String composeAdbCommand(String deviceId, String command) {
        if (command.startsWith("adb")) {
            command = command.replaceFirst("adb", "");
        }

        return adbExecLocation.get() + "adb "
                + (deviceId != null ? "-s " + deviceId + " " : "")
                + command;
    }

    public static String deviceList() {
        return Executor.run(composeAdbCommand("devices -l"));
    }

    @Deprecated
    public static boolean pull(String from, String to) {
        return pull(null, from, to);
    }

    public static boolean pull(String deviceId, String from, String to) {

        //to = to.replaceAll(" ", "_");
        //to = to.replaceAll(" ", "\\\\ ");
        /*byte ptext[] = new byte[0];
        try {
            ptext = to.getBytes("UTF-8");

        to = new String(ptext, "CP1252");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/

        String cmd = "pull " + from + " " + to + "";
        String result = Executor.run(composeAdbCommand(deviceId, cmd));

        if (!result.trim().contains("100%")) {
            Logger.e(result + "\nto: " + to);
            return false;
        }

        return true;
    }

    @Deprecated
    public static String rm(String fileToDelete) {
        return rm(null, fileToDelete);
    }

    public static String rm(String deviceId, String fileToDelete) {
        return Executor.run(composeAdbCommand(deviceId, "shell rm \"" + fileToDelete + "\""));
    }

    public static String killServer() {
        return Executor.run(composeAdbCommand("kill-server"));
    }

    public static String openApp(String packageString) {
        String result = Executor.run(composeAdbCommand("shell monkey -p " + packageString + " 1"));

        if (!result.contains("No activities found to run")) {
            result = null;
        }

        return result;
    }

    public static String runMonkey(String applicationName, int numberOfSteps, int throttle) {
        String result = Executor.run(composeAdbCommand("shell monkey -p " + applicationName + " -v --throttle " + throttle + " " + numberOfSteps));

        if (!result.contains("No activities found to run")) {
            result = null;
        }

        return result;
    }

    public static String kill(String packageName, String pid) {
        String result = Executor.run(composeAdbCommand("shell run-as " + packageName + " kill " + pid));

        if (!result.contains("not debuggable")) {
            result = null;
        }

        return result;
    }

    public static String clearData(String getSelectedAppPackage) {

        String result = Executor.run(composeAdbCommand("shell pm clear " + getSelectedAppPackage));

        if (result.contains("Success")) {
            result = null;
        }

        return result;
    }

    //Todo: should be removed after refactoring
    @Deprecated
    public static String install(String apkFile) {
        return install(null, apkFile);
    }

    //Todo: think why adb install -r -t "file.apk" doesn't work on macOS
    public static String install(String deviceId, String apkFile) {

        String installCmd;
        if (Preferences.OS.startsWith("windows")) {
            installCmd = "install -r -t \"" + apkFile + "\"";
        } else {
            installCmd = "install -r -t " + apkFile + "";
        }
        Logger.d(installCmd);

        String result = Executor.run(composeAdbCommand(deviceId, installCmd));
        String[] split = result.split("\n");

        Logger.d("install result: " + result);

        if (split.length > 0) {
            if (split[split.length - 1].contains("Failure") || split[split.length - 1].contains("Missing")) {
                return split[split.length - 1];
            } else {
                return null;
            }
        }

        return "Wired Error";
    }

    public static void sendInputText(String text) {
        String result = Executor.run(composeAdbCommand("shell input text \"" + text + "\""));
        Logger.d("shell input text " + text + " -> " + result);

    }

    public static String sendIntent(IntentBroadcast intentBroadcast) {
        String command = "shell am " + intentBroadcast.activityManagerCommand +
                (!intentBroadcast.action.equals("") ? " -a " + intentBroadcast.action : "") +
                (!intentBroadcast.data.equals("") ? " -d " + intentBroadcast.data : "") +
                (!intentBroadcast.mimeType.equals("") ? " -t " + intentBroadcast.mimeType : "") +
                (!intentBroadcast.category.equals("") ? " -c " + intentBroadcast.category : "") +
                (!intentBroadcast.component.equals("") ? " -n " + intentBroadcast.component : "") +
                "";

        Logger.d(command);

        String result = Executor.run(composeAdbCommand(command));

        if (!result.contains("Error:")) {
            result = null;
        }

        return result;
    }

    public static String connectDeviceToWifi() {
        String result = null;

        result = Executor.run(composeAdbCommand("shell ip -f inet addr show wlan0"));
        Logger.d("shell ip " + result);

        String[] split = result.split("\n");
        String ip = null;
        for (String line : split) {
            if (line.contains("inet")) {

                String[] splitSpaces = line.split("\\s+");
                int i = 0;
                for (String word : splitSpaces) {
                    if (word.contains("inet")) {
                        break;
                    }
                    i++;
                }

                if (splitSpaces.length > i + 1) {
                    ip = splitSpaces[i + 1].split("/")[0];
                }

                Logger.d("connectDeviceToWifi: ip: " + ip);

                break;
            }
        }

        if (ip != null) {
            result = Executor.run(composeAdbCommand("tcpip 5555"));
            Logger.d("tcpip " + result);
            if (result.contains("restarting")) {

                result = Executor.run(composeAdbCommand("connect " + ip + ":5555"));
                Logger.d("connect " + result);

                if (result.contains("connected")) {
                    result = null;
                }

            }
        }
        return result;
    }

    public static String setDateTime(Calendar calendar) {
        String result = null;
        result = Executor.run(composeAdbCommand("shell settings put global auto_time 0"));
        Logger.d(result);

        result = Executor.run(composeAdbCommand("shell settings put global auto_time_zone 0"));
        Logger.d(result);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMddhhmmyyyy.ss");

        String dateString = simpleDateFormat.format(calendar.getTime());

        result = Executor.run(composeAdbCommand("root"));

        result = Executor.run(composeAdbCommand("shell date " + dateString));
        Logger.d("shell date " + dateString + " ---> " + result);

        if (!result.contains("bad date") && !result.contains("not permitted")) {
            result = null;
        }

        return result;
    }

    public static Set<String> getPackages() {
        String result = Executor.run(composeAdbCommand("shell pm list packages"));

        String[] split = result.split("\n");

        Set<String> packages = new HashSet<>();

        for (int i = 1; i < split.length; i++) {
            String packageName = split[i].replace("package:", "").trim();
            if (packageName.equals("android")) {
                continue;
            }

            packages.add(packageName);
        }

        return packages;
    }

    public static boolean isADBFound() {
        String result = Executor.run(composeAdbCommand("version"));
        return result.startsWith("Android Debug Bridge");
    }

    public static String getLogcat(String deviceId, int lines) {
        return Executor.run(AdbHelper.composeAdbCommand(deviceId, "logcat -t " + lines));
    }

    public static rx.Observable<String> observeLogcat(String deviceId) {
        return Executor.observeProcess(composeAdbCommand(deviceId, "logcat"))
                .filter(s -> !s.isEmpty());
    }

    public static void openLangSettings(String deviceId) {
        Executor.run(composeAdbCommand(deviceId, "shell am start -n com.android.settings/.LanguageSettings"));
    }

    public static String getAndroidApi(String deviceId) {
        return Executor.run(composeAdbCommand(deviceId, "shell getprop ro.build.version.release"));
    }

    public static String createScreenshot(String deviceId, String tmpFile) {
        return Executor.run(composeAdbCommand(deviceId, "adb shell screencap -p " + tmpFile));
    }

    public static String getProcessList(String deviceId) {
        return Executor.run(composeAdbCommand(deviceId, "shell ps"));
    }
}
