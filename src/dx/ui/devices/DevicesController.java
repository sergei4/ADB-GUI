package dx.ui.devices;

import application.ADBHelper;
import application.log.Logger;
import dx.model.Device;
import dx.model.DeviceRegistry;
import dx.service.DeviceMonitorService;
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
        void onDeviceChanged(Device device);
    }

    private List<Listener> listeners = new ArrayList<>();

    @FXML
    private ListView<Device> listDevices;

    @FXML
    private Button buttonADBToggle;

    @FXML
    private Pane devicePane;

    private ObservableList<Device> devicesListItems = FXCollections.observableArrayList();

    private DeviceRegistry deviceRegistry;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listDevices.setItems(devicesListItems);
        listDevices.setCellFactory(listView -> new DeviceItemCell());

        listDevices.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Device>() {
            @Override
            public void changed(ObservableValue<? extends Device> observable, Device oldValue, Device newValue) {
                for (Listener l : listeners) {
                    l.onDeviceChanged(newValue);
                }
            }
        });

        deviceRegistry = new DeviceRegistry(DeviceMonitorService.instance);

        deviceRegistry.observeDeviceList()
                .observeOn(JavaFxScheduler.platform())
                .flatMapIterable(d -> d)
                .doOnNext(this::updDevice)
                .subscribe();

        cfgDragAndDropEvent();
    }

    public Device getSelectedDevice() {
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

    private void updDevice(Device device) {
        int index = devicesListItems.indexOf(device);
        if (index == -1) {
            devicesListItems.add(device);
        } else {
            //devicesListItems.set(index, device);
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
                    new Thread(() -> {
                        Device currentDevice = listDevices.getSelectionModel().getSelectedItem();
                        if (currentDevice != null && currentDevice.isConnected()) {
                            Logger.ds("Installing...");
                            String result = ADBHelper.install(currentDevice.getId(), file.getAbsolutePath());
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

    }
}
