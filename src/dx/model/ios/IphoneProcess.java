package dx.model.ios;

import dx.model.MobileDeviceProcess;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IphoneProcess implements MobileDeviceProcess {

    private static final Pattern processRegexp = Pattern.compile("(.*)<.*>\\:");
    private static final Pattern simpleNameRegexp = Pattern.compile("([A-Za-z\\d]+)(\\(|\\[)");

    private String id = "";
    private String simpleName = "";

    public static IphoneProcess from(String logLine) {
        Matcher matcher;
        matcher = processRegexp.matcher(logLine);
        String processSubstr = matcher.find() ? matcher.group(1).trim() : null;
        String process;
        if (processSubstr != null) {
            String[] split = processSubstr.split("\\s+");
            process = split.length > 1 ? split[split.length - 1] : "?";
        } else {
            process = "?";
        }
        //System.out.println(process);
        IphoneProcess iphoneProcess = new IphoneProcess();
        iphoneProcess.id = process;
        matcher = simpleNameRegexp.matcher(process);
        iphoneProcess.simpleName = matcher.find() ? matcher.group(1).trim() : "";
        return iphoneProcess;
    }

    public String getProcessId() {
        return id;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }
}
