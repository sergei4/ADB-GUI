package dx.model.android;

import dx.model.MobileDeviceLogLine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogcatLine extends MobileDeviceLogLine {
    private final static String PROCESS_ID_UNKNOWN = "?";

    private static final Pattern processRegexp = Pattern.compile("\\s([\\s\\d]+)\\s");

    private final String logProcessId;
    private final String processSubstr;

    private AndroidProcess process;

    public LogcatLine(String source) {
        super(source);
        Matcher matcher = processRegexp.matcher(source);

        processSubstr = matcher.find() ? matcher.group(1).trim() : null;
        if (processSubstr != null) {
            String[] split = processSubstr.split("\\s+");
            logProcessId = split.length > 1 ? split[0] : PROCESS_ID_UNKNOWN;
        } else {
            logProcessId = PROCESS_ID_UNKNOWN;
        }
    }

    public String getSource() {
        return source;
    }

    @Override
    public AndroidProcess getDeviceProcess() {
        return process;
    }

    public void setProcess(AndroidProcess process) {
        this.process = process;
    }

    public String getProcessUuid() {
        return logProcessId;
    }

    @Override
    public String toString() {
        if (processSubstr != null && process != null) {
            return source.replaceAll(processSubstr, processSubstr + "/" + process.getFullName());
        } else {
            return source;
        }
    }
}
