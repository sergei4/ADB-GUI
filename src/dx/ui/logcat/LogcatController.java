package dx.ui.logcat;

import application.FolderUtil;
import application.Main;
import dx.FXMLMainController;
import dx.model.Device;
import dx.model.LogcatLine;
import dx.ui.devices.DevicesController;
import dx.ui.progress.ProgressDialogController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class LogcatController implements Initializable {

    private String selectedProcess = "";

    @FXML
    public ListView<LogcatLine> listViewLog;
    @FXML
    public Button buttonToggle;
    @FXML
    public Button buttonClearLogCat;
    @FXML
    private ComboBox<String> runnableProcessList;
    @FXML
    private TextField userFilter;

    private ObservableList<LogcatLine> logItemList = FXCollections.observableArrayList();

    private FilteredList<LogcatLine> outLogItemList = new FilteredList<>(logItemList);

    private Supplier<Device> deviceSupplier = () -> null;

    private DevicesController.Listener deviceControllerListener = new DevicesController.Listener() {
        @Override
        public void onDeviceChanged(Device device) {
            logItemList.clear();
            if (isRunning()) {
                stop();
                start(device);
            } else {
                stop();
            }
        }
    };

    public void setDeviceSupplier(Supplier<Device> supplier) {
        deviceSupplier = supplier;
    }

    public DevicesController.Listener getDeviceControllerListener() {
        return deviceControllerListener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        runnableProcessList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                selectedProcess = newValue;
            }
        });

        listViewLog.setItems(outLogItemList);
        listViewLog.setCellFactory(cell -> new ListCell<LogcatLine>() {
            @Override
            protected void updateItem(LogcatLine item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item.toString());
                    setFont(Main.courierFont13);
                }
            }
        });

        userFilter.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                outLogItemList.setPredicate(line -> line.getSource().contains(newValue));
            }
        });

        toggleButtons(false);
    }

    public void onToggleClicked(ActionEvent actionEvent) {
        if (isRunning()) {
            stop();
        } else {
            start(deviceSupplier.get());
        }
    }

    private void start(Device device) {
        if (device != null && device.isConnected()) {
            logItemList.clear();
            toggleButtons(true);
            startObserve(device.observeLog()
                    .onBackpressureBuffer()
                    .observeOn(JavaFxScheduler.platform())
                    .subscribe(this::addLine, this::onError)
            );
        }
    }

    private void onError(Throwable e) {
        toggleButtons(false);
        e.printStackTrace();
    }

    private void stop() {
        toggleButtons(false);
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    private rx.Subscription subscription;

    private void startObserve(rx.Subscription subscription) {
        this.subscription = subscription;
    }

    private boolean isRunning() {
        return subscription != null && !subscription.isUnsubscribed();
    }

    private void addLine(LogcatLine line) {
        logItemList.add(line);
    }

    private void toggleButtons(boolean started) {
        Platform.runLater(() -> {
            if (started) {
                buttonToggle.setText("Stop");
            } else {
                buttonToggle.setText("Start");
            }
        });
    }

    public void onClearLocallyClicked(ActionEvent actionEvent) {
        logItemList.clear();
    }

    @FXML
    public void onSaveToFileClicked(ActionEvent actionEvent) throws Exception {
        Device device = deviceSupplier.get();
        if (device != null) {
            Stage dialog = ProgressDialogController.createDialog("Gathering information. Please wait...");
            dialog.initStyle(StageStyle.UNDECORATED);
            FXMLMainController.showDialog(dialog);
            LogcatUtils.save(device, Observable.from(outLogItemList), () -> Platform.runLater(dialog::close));
        }
    }

    public void onOpenLogFolderClicked(ActionEvent actionEvent) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(FolderUtil.getLogsFolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}