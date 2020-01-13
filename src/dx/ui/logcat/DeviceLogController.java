package dx.ui.logcat;

import application.utils.FolderUtil;
import dx.FXMLMainController;
import dx.model.MobileDevice;
import dx.model.MobileDeviceLogLine;
import dx.ui.devices.DevicesController;
import dx.ui.progress.ProgressDialogController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import res.R;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DeviceLogController implements Initializable {

    @FXML
    public ListView<MobileDeviceLogLine> listViewLog;
    @FXML
    public Button buttonToggle;
    @FXML
    public Button buttonClearLogCat;
    @FXML
    private ComboBox<String> runnableProcessList;
    @FXML
    private TextField userFilter;

    private ObservableList<MobileDeviceLogLine> logItemList = FXCollections.observableArrayList();

    private FilteredList<MobileDeviceLogLine> outLogItemList = new FilteredList<>(logItemList);

    private Supplier<MobileDevice> deviceSupplier = () -> null;

    private ObservableList<String> processItemList = FXCollections.observableArrayList();
    private SortedList<String> sortedProcessItemList = new SortedList<>(processItemList);

    {
        sortedProcessItemList.setComparator(String::compareTo);
    }

    private rx.Subscription deviceLogSubs;
    private rx.Subscription processSubs;

    private static class ComplexFilter {
        private String processName = "";
        private String userFilterStr = "";

        Predicate<MobileDeviceLogLine> createPredicate() {
            Predicate<MobileDeviceLogLine> processFilter = line ->
                    processName.equals("") || line.getDeviceProcess() != null && line.getDeviceProcess().getSimpleName().equals(processName);
            Predicate<MobileDeviceLogLine> userFilter = line -> line.toString().toLowerCase().contains(userFilterStr.toLowerCase());
            return processFilter.and(userFilter);
        }
    }

    private ComplexFilter complexFilter = new ComplexFilter();

    private DevicesController.Listener deviceControllerListener = new DevicesController.Listener() {
        @Override
        public void onDeviceChanged(MobileDevice device) {
            logItemList.clear();
            if (isRunning()) {
                stop();
                start(device);
            } else {
                stop();
            }
        }
    };

    public void setDeviceSupplier(Supplier<MobileDevice> supplier) {
        deviceSupplier = supplier;
    }

    public DevicesController.Listener getDeviceControllerListener() {
        return deviceControllerListener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        runnableProcessList.setItems(sortedProcessItemList);
        runnableProcessList.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        complexFilter.processName = newValue == null ? "" : newValue;
                        outLogItemList.setPredicate(complexFilter.createPredicate());
                    }
                });

        listViewLog.setItems(outLogItemList);
        listViewLog.setCellFactory(cell -> new ListCell<MobileDeviceLogLine>() {
            @Override
            protected void updateItem(MobileDeviceLogLine item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item.toString());
                    setFont(R.Fonts.courierFont13);
                } else {
                    setText(null);
                }
            }
        });

        userFilter.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                complexFilter.userFilterStr = newValue == null ? "" : newValue;
                outLogItemList.setPredicate(complexFilter.createPredicate());
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

    private void observeProcessList(MobileDevice device) {
        processItemList.clear();
        processItemList.add("");
        if (processSubs != null && !processSubs.isUnsubscribed()) {
            processSubs.unsubscribe();
        }
        if (device != null && device.isConnected()) {
            processSubs = Observable.interval(3, TimeUnit.SECONDS)
                    .flatMap(t -> Observable.from(device.getProcessList()))
                    //.filter(process -> process.getUser().startsWith("u"))
                    .observeOn(JavaFxScheduler.platform())
                    .filter(process -> !processItemList.contains(process.getSimpleName()))
                    .doOnNext(process -> processItemList.add(process.getSimpleName()))
                    .subscribe();
        }
    }

    private void start(MobileDevice device) {
        if (device != null && device.isConnected()) {
            logItemList.clear();
            toggleButtons(true);
            this.deviceLogSubs = device.observeLog()
                    .onBackpressureBuffer()
                    .observeOn(JavaFxScheduler.platform())
                    .subscribe(this::addLine, this::onError, () -> toggleButtons(false));
            observeProcessList(device);
        }
    }

    private void onError(Throwable e) {
        toggleButtons(false);
        e.printStackTrace();
    }

    private void stop() {
        toggleButtons(false);
        if (deviceLogSubs != null && !deviceLogSubs.isUnsubscribed()) {
            deviceLogSubs.unsubscribe();
        }
    }

    private boolean isRunning() {
        return deviceLogSubs != null && !deviceLogSubs.isUnsubscribed();
    }

    private void addLine(MobileDeviceLogLine line) {
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
        MobileDevice device = deviceSupplier.get();
        if (device != null) {
            Stage dialog = ProgressDialogController.createDialog("Gathering information. Please wait...");
            dialog.initStyle(StageStyle.UNDECORATED);
            FXMLMainController.showDialog(dialog);
            MobileLogUtils.save(device, Observable.from(outLogItemList), () -> Platform.runLater(dialog::close));
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