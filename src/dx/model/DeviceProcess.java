package dx.model;

public class DeviceProcess {
    private String user;
    private String pid;
    private String fullName;

    public DeviceProcess(String user, String pid, String fullName) {
        this.user = user;
        this.pid = pid;
        this.fullName = fullName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSimpleName() {
        return fullName.contains(":") ? fullName.substring(0, fullName.indexOf(":")) : fullName;
    }

    @Override
    public String toString() {
        return String.format("user: %s, pid: %s, name: %s", user, pid, fullName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceProcess that = (DeviceProcess) o;

        return pid.equals(that.pid);
    }

    @Override
    public int hashCode() {
        return pid.hashCode();
    }
}
