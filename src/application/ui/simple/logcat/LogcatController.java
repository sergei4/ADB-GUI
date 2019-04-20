package application.ui.simple.logcat;

import application.AdbUtils;
import application.DateUtil;
import application.Main;
import application.log.Logger;
import application.model.Model;
import application.model.ModelListener;
import application.model.PackageProcess;
import application.preferences.Preferences;
import application.ui.simple.FXMLMainController;
import application.ui.simple.progress.ProgressDialogController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by evgeni.shafran on 10/17/16.
 */
public class LogcatController implements Initializable {

    private static final long INTERVAL_DURATION = 5000;

    private boolean working;

    private String selectedProcess = "";

    private static ExecutorService mainExecutor = Executors.newSingleThreadExecutor();

    @FXML
    public ListView<String> listViewLog;
    @FXML
    public Button buttonToggle;
    @FXML
    public Button buttonClearLogCat;
    @FXML
    private ComboBox<String> runnableProcessList;

    private ObservableList<String> logListItems = FXCollections.observableArrayList();

    private Map<String, PackageProcess> processList = new HashMap<>();
    private ObservableList<String> runnablePackegeListItems = FXCollections.observableArrayList();

    private final Object modifyProcessMapMarker = new Object();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        runnableProcessList.setItems(runnablePackegeListItems);
        runnableProcessList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                selectedProcess = newValue;
                restart();
            }
        });

        listViewLog.setItems(logListItems);
        listViewLog.setCellFactory(cell -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item);
                    setFont(Main.courierFont13);
                }
            }
        });

        toggleButtonsStates(false);

        Model.instance.addSelectedDeviceListener(new ModelListener() {
            @Override
            public void onChangeModelListener() {
                new Thread(() -> {
                    synchronized (modifyProcessMapMarker) {
                        processList.clear();
                    }
                }).start();

                if (Objects.nonNull(Model.instance.getSelectedDevice())) {
                    if (working) {
                        start();
                    }
                } else {
                    stop();
                }
            }
        });
        new Thread(() -> updRunningProcesses()).start();
    }

    public void onToggleClicked(ActionEvent actionEvent) {
        if (working) {
            stop();
        } else {
            if (Objects.nonNull(Model.instance.getSelectedDevice())) {
                start();
            }
        }
    }

    private void start() {
        toggleButtonsStates(false);
        logListItems.clear();
        working = true;
        buttonToggle.setText("Stop");

        mainExecutor.execute(() -> {
            String logcatCommand = AdbUtils.getAdbCommand(Model.instance.getSelectedDeviceId(), "logcat");

            Process process;
            try {
                String[] envp = {};
                process = Runtime.getRuntime().exec(logcatCommand, envp);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = "";
                while (working && (line = reader.readLine()) != null) {
                    addLine(line);
                }

                process.destroy();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void stop() {
        working = false;
        buttonToggle.setText("Start");
    }

    private void restart() {
        if (working) {
            stop();
            start();
        }
    }

    private void addLine(String line) {
        Logger.d(line);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (!selectedProcess.equals("")) {
                    synchronized (modifyProcessMapMarker) {
                        Pair<String, String> process = findProcessWithName(line);
                        if (process != null && process.getValue().equals(selectedProcess)) {
                            logListItems.add(line);
                        }
                    }
                } else {
                    logListItems.add(line);
                }
            }
        });
    }

    public void onClearLocallyClicked(ActionEvent actionEvent) {
        logListItems.clear();
    }

    public void saveSelectedDeviceLog() {
        Stage dialog = ProgressDialogController.createDialog("Gathering information. Please wait...");
        dialog.initStyle(StageStyle.UNDECORATED);
        FXMLMainController.showDialog(dialog);
        saveSelectedDeviceLogImpl(s -> Platform.runLater(dialog::close));
    }

    private void saveSelectedDeviceLogImpl(Consumer<String> consumer) {
        new Thread(() -> {
            String deviceId = Model.instance.getSelectedDeviceId();
            if (deviceId != null) {
                Logger.ds("Gathering information...");
                String result = AdbUtils.run(deviceId, "logcat -t 12000");
                saveLogToFile(Arrays.asList(result.split("\\n")));
                consumer.accept("OK");
            }
        }).start();
    }

    @FXML
    public void onSaveToFileClicked(ActionEvent actionEvent) {
        new Thread(() -> {
            final List<String> listToSave = new ArrayList<>(logListItems.size());
            listToSave.addAll(logListItems);
            saveLogToFile(listToSave);
        }).start();
    }

    private final Pattern processRegexp = Pattern.compile("\\s([\\s\\d]+)\\s");

    private Pair<String, String> findProcessWithName(String logLine) {
        Matcher matcher = processRegexp.matcher(logLine);
        if (matcher.find()) {
            String processUid = matcher.group(1);
            String[] split = processUid.split("\\s");

            PackageProcess packageProcess = split.length > 1 ? processList.get(split[0]) : null;
            if (Objects.nonNull(packageProcess)) {
                return new Pair<>(processUid, packageProcess.process);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void saveLogToFile(List<String> listToSave) {
        File logcatFolder = Preferences.getInstance().getLogcatFolder();

        PrintWriter writer = null;
        Logger.ds("Saving log...");
        try {
            File logFile = new File(logcatFolder,
                    Model.instance.getSelectedDevice().getName() + " " +
                            Model.instance.getSelectedDevice().getAndroidVersion() + " " +
                            DateUtil.getCurrentTimeStamp() + ".txt");


            writer = new PrintWriter(logFile, "UTF-8");

            writer.println("Device name: " + Model.instance.getSelectedDevice().getName());
            writer.println("Model: " + Model.instance.getSelectedDevice().getModel());
            writer.println("Android version: " + Model.instance.getSelectedDevice().getAndroidVersion());
            writer.println();

            synchronized (modifyProcessMapMarker) {
                for (String line : listToSave) {
                    Pair<String, String> process = findProcessWithName(line);
                    if (Objects.nonNull(process)) {
                        line = line.replace(process.getKey(), process.getKey() + "/" + process.getValue());
                        writer.println(line);
                    } else {
                        writer.println(line);
                    }
                }
            }
            writer.close();
            Logger.fs("Log saved: " + logFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Logger.es("Error creating log: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Logger.es("Error creating log: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Logger.es("Error creating log: " + e.getMessage());
        }
    }

    public void onOpenLogFolderClicked(ActionEvent actionEvent) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(Preferences.getInstance().getLogcatFolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void toggleButtonsStates(boolean exceptionState) {
        buttonClearLogCat.setDisable(exceptionState);
    }

    private void updRunningProcesses() {
        while (true) {
            if (Objects.nonNull(Model.instance.getSelectedDevice())) {

                String result;
                String[] split;

                result = AdbUtils.run("shell ps");
                split = result.split("\n");

                List<PackageProcess> activeProcesses = new ArrayList<>();
                for (int i = 1; i < split.length; i++) {

                    PackageProcess packageProcess = new PackageProcess();
                    String[] process = split[i].split("\\s+");
                    packageProcess.process = process[process.length - 1];
                    packageProcess.PID = process[1];

                    activeProcesses.add(packageProcess);
                }

                result = AdbUtils.run("shell pm list packages");
                split = result.split("\n");

                List<String> runnablePackages = new ArrayList<>();
                for (int i = 0; i < split.length; i++) {
                    String packageName = split[i].replace("package:", "").trim();
                    if (packageName.equals("android")) {
                        continue;
                    }
                    for (PackageProcess process : activeProcesses) {
                        if (process.process.equals(packageName)) {
                            runnablePackages.add(packageName);
                        }
                    }
                }

                updRunnableAppsList(runnablePackages);

                synchronized (modifyProcessMapMarker) {
                    for (PackageProcess process : activeProcesses) {
                        processList.put(process.PID, process);
                    }
                }
            }
            try {
                Thread.sleep(INTERVAL_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updRunnableAppsList(List<String> packages) {
        Collections.sort(packages, String::compareTo);
        packages.add(0, "");
        Platform.runLater(() -> {
            for (String p : packages) {
                if (!runnablePackegeListItems.contains(p)) {
                    runnablePackegeListItems.add(p);
                }
            }
            runnablePackegeListItems.removeIf(current -> !packages.contains(current));
        });
    }
}