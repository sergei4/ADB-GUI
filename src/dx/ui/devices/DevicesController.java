package dx.ui.devices;

import application.log.Logger;
import dx.helpers.AdbHelper;
import dx.helpers.IosHelper;
import dx.model.MobileDevice;
import dx.model.MobileDeviceRegistry;
import dx.model.MobileDeviceVisitor;
import dx.model.android.AndroidDevice;
import dx.model.ios.IphoneDevice;
import dx.service.AndroidDeviceMonitorService;
import dx.service.IphoneDeviceMonitorService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DevicesController implements Initializable {

    public interface Listener {
        void onDeviceChanged(MobileDevice device);
    }

    private List<Listener> listeners = new ArrayList<>();

    @FXML
    private ListView<MobileDevice> listDevices;

    @FXML
    private Button buttonADBToggle;

    @FXML
    private Pane devicePane;

    private ObservableList<MobileDevice> devicesListItems = FXCollections.observableArrayList();

    private MobileDeviceRegistry mobileDeviceRegistry;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listDevices.setItems(devicesListItems);
        listDevices.setCellFactory(listView -> new DeviceItemCell());

        listDevices.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<MobileDevice>() {
            @Override
            public void changed(ObservableValue<? extends MobileDevice> observable, MobileDevice oldValue, MobileDevice newValue) {
                for (Listener l : listeners) {
                    l.onDeviceChanged(newValue);
                }
            }
        });

        mobileDeviceRegistry = new MobileDeviceRegistry(
                AndroidDeviceMonitorService.instance,
                IphoneDeviceMonitorService.instance
        );

        mobileDeviceRegistry.observeDeviceList()
                .observeOn(JavaFxScheduler.platform())
                .flatMapIterable(d -> d)
                .doOnNext(this::updDevice)
                .subscribe();

        cfgDragAndDropEvent();

        AndroidDeviceMonitorService.instance.start();
        IphoneDeviceMonitorService.instance.start();
    }

    public MobileDevice getSelectedDevice() {
        return listDevices.getSelectionModel().getSelectedItem();
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void updDevice(MobileDevice device) {
        int index = devicesListItems.indexOf(device);
        if (index == -1) {
            devicesListItems.add(device);
        } else {
            listDevices.refresh();
        }
        if (listDevices.getSelectionModel().getSelectedItem() == null) {
            listDevices.getSelectionModel().selectFirst();
        }
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
                    MobileDevice currentDevice = listDevices.getSelectionModel().getSelectedItem();
                    if (currentDevice != null && currentDevice.isConnected()) {
                        currentDevice.visitBy(new MobileDeviceVisitor() {

                            @Override
                            public void visitBy(AndroidDevice device) {
                                installApkFile(device.getId(), file.getAbsoluteFile());
                            }

                            @Override
                            public void visitBy(IphoneDevice device) {
                                installIpaFile(device.getId(), file.getAbsoluteFile());
                            }
                        });
                    }
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
    }

    private void installApkFile(String deviceId, File apkFile) {
        new Thread(() -> {
            Logger.ds("Installing...");
            String result = AdbHelper.install(deviceId, apkFile.getAbsolutePath());
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
    }

    private void installIpaFile(String deviceId, File ipaFile) {
        new Thread(() -> {
            Logger.ds("Installing...");
            String result = IosHelper.install(deviceId, ipaFile.getAbsolutePath());
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
    }

    @FXML
    private void handleToggleADBClicked(ActionEvent event) {

    }
}
