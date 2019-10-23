package dx.ui.devices;

import application.ADBHelper;
import application.log.Logger;
import dx.model.Device;
import dx.model.DeviceRegistry;
import dx.service.DeviceMonitorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import rx.schedulers.JavaFxScheduler;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DevicesController implements Initializable {

    private boolean killed = false;

    @FXML
    private ListView<String> listDevices;

    @FXML
    private Button buttonADBToggle;

    @FXML
    private Pane devicePane;

    private ObservableList<String> devicesListItems = FXCollections.observableArrayList();

    private DeviceRegistry deviceRegistry;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listDevices.setItems(devicesListItems);

//        listDevices.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
//            public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
//                if (listDevices.getSelectionModel().getSelectedIndex() >= 0) {
//                    Model.instance.setSelectedDevice(availableDevices.get(listDevices.getSelectionModel().getSelectedIndex()));
//                }
//            }
//        });

        deviceRegistry = new DeviceRegistry(DeviceMonitorService.instance);

        deviceRegistry.observeDeviceList()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(devices -> {
                    devicesListItems.clear();
                    for (Device device : devices) {
                        devicesListItems.add(getDeviceDescription(device));
                    }
                });

        cfgDragAndDropEvent();
    }

    private void cfgDragAndDropEvent() {
        devicePane.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                if (event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });

        devicePane.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    File file = db.getFiles().get(0);
                    Logger.ds("Installing...");
                    new Thread(() -> {
                        String result = ADBHelper.install(file.getAbsolutePath());
                        if (result == null) {
                            Logger.fs("Application has been installed successful");
                        } else {
                            Pattern causeRegExpr = Pattern.compile("Failure (.*)");
                            Matcher matcher = causeRegExpr.matcher(result);
                            String cause = "";
                            if (matcher.find()) {
                                cause = matcher.group(1);
                            }
                            Logger.es("Failed installation: " + cause);
                        }
                    }).start();
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
    }

    @FXML
    private void handleToggleADBClicked(ActionEvent event) {
//
//        devicesListItems.clear();
//
//        if (killed) {
//            DeviceMonitorService.instance.startMonitoringDevices();
//            buttonADBToggle.setText("Kill");
//            Logger.fs("ADB server started");
//        } else {
//            buttonADBToggle.setText("Start monitoring");
//            DeviceMonitorService.instance.stopMonitoringDevices();
//            AdbUtils.executor.execute(new Runnable() {
//
//                @Override
//                public void run() {
//                    Logger.d(ADBHelper.killServer());
//                    Logger.fs("ADB server killed");
//                }
//            });
//        }
//
//        killed = !killed;
    }

//    private void refreshDevices() {
//
//        Device selectedDevice = Model.instance.getSelectedDevice();
//
//        int i = 0;
//
//        devicesListItems.clear();
//        availableDevices = Model.instance.getAvailableDevices();
//        boolean setSelected = false;
//        for (Device device : availableDevices) {
//            devicesListItems.add(getDeviceDescription(device));
//
//            if (selectedDevice != null && device.getId().equals(selectedDevice.getId())) {
//                listDevices.getSelectionModel().select(i);
//                setSelected = true;
//            }
//
//            i++;
//        }
//
//        if (!setSelected && devicesListItems.size() > 0) {
//            listDevices.getSelectionModel().select(0);
//        }
//    }

    private String getDeviceDescription(Device device) {
        return device.getModel() + ": " + device.getAndroidApiName() + " (" + device.getId() + ")" + (device.isConnected() ? "+" : "-");
    }
}
