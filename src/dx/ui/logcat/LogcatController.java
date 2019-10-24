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
import javafx.collections.transformation.SortedList;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LogcatController implements Initializable {

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

    private ObservableList<String> processItemList = FXCollections.observableArrayList();
    private SortedList<String> sortedProcessItemList = new SortedList<>(processItemList);

    {
        sortedProcessItemList.setComparator(String::compareTo);
    }

    private rx.Subscription logCatSubs;
    private rx.Subscription processSubs;

    private static class ComplexFilter {
        private String selectedProcessName = "";
        private String userFilterStr = "";

        Predicate<LogcatLine> createPredicate() {
            Predicate<LogcatLine> processFilter = line ->
                    selectedProcessName.equals("") || line.getProcess() != null && line.getProcess().getFullName().equals(selectedProcessName);
            Predicate<LogcatLine> userFilter = line -> line.getSource().toLowerCase().contains(userFilterStr.toLowerCase());
            return processFilter.and(userFilter);
        }
    }

    private ComplexFilter complexFilter = new ComplexFilter();

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
        runnableProcessList.setItems(sortedProcessItemList);
        runnableProcessList.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        complexFilter.selectedProcessName = newValue == null ? "" : newValue;
                        outLogItemList.setPredicate(complexFilter.createPredicate());
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

    private void observeProcessList(Device device) {
        processItemList.clear();
        processItemList.add("");
        if (processSubs != null && !processSubs.isUnsubscribed()) {
            processSubs.unsubscribe();
        }
        if (device != null && device.isConnected()) {
            processSubs = Observable.interval(3, TimeUnit.SECONDS)
                    .flatMap(t -> Observable.from(device.getProcessList()))
                    .filter(process -> process.getUser().startsWith("u"))
                    .observeOn(JavaFxScheduler.platform())
                    .filter(process -> !processItemList.contains(process.getFullName()))
                    .doOnNext(process -> processItemList.add(process.getFullName()))
                    .subscribe();
        }
    }

    private void start(Device device) {
        if (device != null && device.isConnected()) {
            logItemList.clear();
            toggleButtons(true);
            this.logCatSubs = device.observeLog()
                    .onBackpressureBuffer()
                    .observeOn(JavaFxScheduler.platform())
                    .subscribe(this::addLine, this::onError);
            observeProcessList(device);
        }
    }

    private void onError(Throwable e) {
        toggleButtons(false);
        e.printStackTrace();
    }

    private void stop() {
        toggleButtons(false);
        if (logCatSubs != null && !logCatSubs.isUnsubscribed()) {
            logCatSubs.unsubscribe();
        }
    }

    private boolean isRunning() {
        return logCatSubs != null && !logCatSubs.isUnsubscribed();
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