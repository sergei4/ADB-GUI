package dx.model.ios;

import dx.model.MobileDeviceLogLine;

public class IphoneLogLine extends MobileDeviceLogLine {

    private IphoneProcess process;

    public IphoneLogLine(String source) {
        super(source);
        this.process = IphoneProcess.from(source);
    }

    @Override
    public IphoneProcess getDeviceProcess() {
        return process;
    }

    public void setIphoneProcess(IphoneProcess process) {
        this.process = process;
    }
}
