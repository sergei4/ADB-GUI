package dx.model;

public abstract class MobileDeviceLogLine {
    protected final String source;

    public MobileDeviceLogLine(String source) {
        this.source = source;
    }

    public abstract MobileDeviceProcess getDeviceProcess();

    @Override
    public String toString() {
        return source;
    }
}
