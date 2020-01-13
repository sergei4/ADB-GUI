package dx.ui.devices;

import dx.model.MobileDevice;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;

public class DeviceItemCell extends ListCell<MobileDevice> {

    private FXMLLoader loader;

    @Override
    protected void updateItem(MobileDevice device, boolean empty) {
        super.updateItem(device, empty);
        if (empty || device == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(getDeviceDescription(device));
            setGraphic(null);
        }
    }

    private String getDeviceDescription(MobileDevice device) {
        return (device.isConnected() ? "+" : "-") + device.getModel() + ": " + device.getOsVersion() + " (" + device.getId() + ")";
    }
}
