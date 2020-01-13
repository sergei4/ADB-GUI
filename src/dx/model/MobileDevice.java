package dx.model;

import rx.Observable;

import java.util.Collection;

public abstract class MobileDevice {

    private String id = "";

    private String model = "";

    private boolean isEmulator;

    private String osVersion = "indefinite";

    private boolean connected;

    public MobileDevice() {

    }

    public MobileDevice(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEmulator() {
        return isEmulator;
    }

    public void setEmulator(boolean emulator) {
        isEmulator = emulator;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        if (connected) {
            onConnect();
        } else {
            onDisconnect();
        }
    }

    protected abstract void onConnect();

    protected abstract void onDisconnect();

    public abstract Collection<? extends MobileDeviceProcess> getProcessList();

    public abstract Observable<? extends MobileDeviceLogLine> observeLog();

    public abstract Observable<? extends MobileDeviceLogLine> observeFullDeviceLog();

    public void copyProperties(MobileDevice device){
        model = device.model;
        isEmulator = device.isEmulator;
        osVersion = device.osVersion;
    }

    public abstract void visitBy(MobileDeviceVisitor visitor);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MobileDevice device = (MobileDevice) o;

        return id.equals(device.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
