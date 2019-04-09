package application.ui.simple.logcat;

import application.AdbUtils;
import application.DateUtil;
import application.log.Logger;
import application.model.Model;
import application.model.ModelListener;
import application.model.PackageProcess;
import application.preferences.Preferences;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Font;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by evgeni.shafran on 10/17/16.
 */
public class LogcatController implements Initializable {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    public ListView<String> listViewLog;
    public Button buttonToggle;
    public Button buttonClearLogCat;

    private ObservableList<String> logListItems = FXCollections.observableArrayList();
    private boolean working;

    private Map<String, PackageProcess> processList = new HashMap<>();

    private final Object modifyProcessMapMarker = new Object();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listViewLog.setItems(logListItems);
        listViewLog.setCellFactory(cell -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item);
                    setFont(Font.font("courier"));
                }
            }
        });

        toggleButtonsStates(false);

        Model.instance.addSelectedDeviceListener(new ModelListener() {
            @Override
            public void onChangeModelListener() {
                if (Objects.nonNull(Model.instance.getSelectedDevice())) {
                    if (working) {
                        start();
                    }
                } else {
                    stop();
                }
            }
        });
        scheduledExecutorService.schedule(() -> updRunningProcesses(), 100, TimeUnit.MILLISECONDS);
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

        executor.execute(() -> {
            String logcatCommand = AdbUtils.getAdbCommand("logcat");

            Process process;
            try {

                String[] envp = {};
                process = Runtime.getRuntime().exec(logcatCommand, envp);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = "";
                while ((line = reader.readLine()) != null) {
                    if (!working) {
                        break;
                    }
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

    private void addLine(String line) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                logListItems.add(line);
            }
        });
    }

    public void onClearLocallyClicked(ActionEvent actionEvent) {
        logListItems.clear();
    }

    public void onClearADBClicked(ActionEvent actionEvent) {
        //stop();
        logListItems.clear();
        /*AdbUtils.runAsync("logcat -c", new AdbUtils.ADBRunListener() {
            @Override
            public void onFinish(String resporse) {

            }
        });*/
    }

    public void onSaveToFileClicked(ActionEvent actionEvent) {
        final List<String> listToSave = new ArrayList<>(logListItems.size());
        listToSave.addAll(logListItems);

        new Thread(new Runnable() {
            @Override
            public void run() {
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

                    Pattern processReg = Pattern.compile("\\s\\s([\\s\\d]+)\\s");

                    for (String line : listToSave) {

                        Matcher matcher = processReg.matcher(line);
                        if (matcher.find()) {
                            String processUid = matcher.group(1);
                            String[] split = processUid.split("\\s");
                            synchronized (modifyProcessMapMarker) {
                                PackageProcess packageProcess = split.length > 1 ? processList.get(split[0]) : null;
                                if (Objects.nonNull(packageProcess)) {
                                    line = line.replace(processUid, processUid + "/" + packageProcess.process);
                                    Logger.d(line);
                                    writer.println(line);
                                } else {
                                    writer.println(line);
                                }
                            }
                        }
                        writer.println(line);
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
        }).start();
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
        synchronized (modifyProcessMapMarker) {
            String result = AdbUtils.run("shell ps");
            String[] split = result.split("\n");
            for (int i = 1; i < split.length; i++) {

                PackageProcess packageProcess = new PackageProcess();
                String[] process = split[i].split("\\s+");
                packageProcess.process = process[process.length - 1];
                packageProcess.PID = process[1];

                processList.put(packageProcess.PID, packageProcess);
            }
        }
    }
}