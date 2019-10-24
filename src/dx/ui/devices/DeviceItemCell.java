package dx.ui.devices;

import dx.model.Device;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;

public class DeviceItemCell extends ListCell<Device> {

    private FXMLLoader loader;

    @Override
    protected void updateItem(Device device, boolean empty) {
        super.updateItem(device, empty);
        if (empty || device == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(getDeviceDescription(device));
            setGraphic(null);
        }
    }

    private String getDeviceDescription(Device device) {
        return (device.isConnected() ? "+" : "-") + device.getModel() + ": " + device.getAndroidApiName() + " (" + device.getId() + ")";
    }
}
