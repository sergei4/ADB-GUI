package application.ui.simple.devices;

import application.ADBHelper;
import application.AdbUtils;
import application.intentbroadcasts.IntentBroadcast;
import application.log.Logger;
import application.model.Device;
import application.model.Model;
import application.model.ModelListener;
import application.screencapture.ScreenCaptureController;
import application.services.DeviceMonitorService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DevicesController implements Initializable {

    private boolean killed = false;

    @FXML
    private ListView<String> listDevices;

    @FXML
    private Button buttonADBToggle;

    private ObservableList<String> devicesListItems = FXCollections.observableArrayList();

    private List<Device> availableDevices;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listDevices.setItems(devicesListItems);

        listDevices.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                if (listDevices.getSelectionModel().getSelectedIndex() >= 0) {
                    Model.instance.setSelectedDevice(
                            availableDevices.get(listDevices.getSelectionModel().getSelectedIndex()));
                }
            }
        });


        Model.instance.addModelListener(new ModelListener() {

            @Override
            public void onChangeModelListener() {
                refreshDevices();
            }
        });

        refreshDevices();
    }

    @FXML
    private void handleToggleADBClicked(ActionEvent event) {

        devicesListItems.clear();

        if (killed) {
            DeviceMonitorService.instance.startMonitoringDevices();
            buttonADBToggle.setText("Kill");
            Logger.fs("ADB server started");
        } else {
            buttonADBToggle.setText("Start monitoring");
            DeviceMonitorService.instance.stopMonitoringDevices();
            AdbUtils.executor.execute(new Runnable() {

                @Override
                public void run() {
                    Logger.d(ADBHelper.killServer());
                    Logger.fs("ADB server killed");
                }
            });

//            DialogUtil.showInfoDialog("Restarting ADB service from this tool can cause device to be 'unauthorized'\n" +
//                    "In that case please open you favourite command line (terminal) and enter:\n" +
//                    "adb devices\n" +
//                    "Then press start monitoring");
        }

        killed = !killed;
    }

    @FXML
    private void handleTakeSnapshotClicked(ActionEvent event) {
        try {
            ScreenCaptureController.showScreen(getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void refreshDevices() {

        Device selectedDevice = Model.instance.getSelectedDevice();

        int i = 0;

        devicesListItems.clear();
        availableDevices = Model.instance.getAvailableDevices();
        boolean setSelected = false;
        for (Device device : availableDevices) {
            devicesListItems.add(getDeviceDescription(device));

            if (selectedDevice != null && device.getId().equals(selectedDevice.getId())){
                listDevices.getSelectionModel().select(i);
                setSelected = true;
            }

            i++;
        }

        if (!setSelected && devicesListItems.size() > 0) {
            listDevices.getSelectionModel().select(0);
        }
    }

    private String getDeviceDescription(Device device) {
        return device.getName() + " " + device.getAndroidVersion() + " " + device.getId();
    }


    public void onOpenDevSettings(ActionEvent actionEvent) {
        AdbUtils.executor.execute(new Runnable() {
            @Override
            public void run() {
                IntentBroadcast intent = new IntentBroadcast();
                intent.activityManagerCommand = IntentBroadcast.ACTIVITY_MANAGER_COMMAND_START;
                intent.action = "android.settings.APPLICATION_DEVELOPMENT_SETTINGS";
                ADBHelper.sendIntent(intent);
            }
        });
    }
}
