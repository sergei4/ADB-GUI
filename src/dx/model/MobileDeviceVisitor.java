package dx.model;

import dx.model.android.AndroidDevice;
import dx.model.ios.IphoneDevice;

public interface MobileDeviceVisitor {

    void visitBy(AndroidDevice device);

    void visitBy(IphoneDevice device);
}
