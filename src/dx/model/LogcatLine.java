package dx.model;

import com.sun.istack.internal.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogcatLine {
    private static final Pattern processRegexp = Pattern.compile("\\s([\\s\\d]+)\\s");

    private String source;
    private String processId;

    private DeviceProcess process;

    public LogcatLine(String source) {
        this.source = source;
        Matcher matcher = processRegexp.matcher(source);
        processId = matcher.find() ? matcher.group(1).trim() : null;
    }

    @Nullable
    public DeviceProcess getProcess() {
        return process;
    }

    public void setProcess(DeviceProcess process) {
        this.process = process;
    }

    public String getProcessUuid() {
        if (processId != null) {
            String[] split = processId.split("\\s+");
            return split.length > 1 ? split[0] : "?";
        }
        return "?";
    }

    @Override
    public String toString() {
        return (processId != null && process != null) ? source.replace(processId, processId + "/" + process.getSimpleName()) :
                processId != null ? source.replace(processId, processId + "/?") : source;
    }
}
