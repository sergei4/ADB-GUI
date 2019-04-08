package application.ui.simple.logcat;

import application.AdbUtils;
import application.DateUtil;
import application.log.Logger;
import application.logexceptions.ExceptionLog;
import application.model.Model;
import application.model.ModelListener;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by evgeni.shafran on 10/17/16.
 */
public class LogcatController implements Initializable {

    public static ExecutorService executor = Executors.newSingleThreadExecutor();

    public ListView<String> listViewLog;
    public Button buttonToggle;
    public Button buttonClearLogCat;

    private ObservableList<String> logListItems = FXCollections.observableArrayList();
    private boolean working;
    private ArrayList<ExceptionLog> exceptions;
    private volatile boolean exceptionState;
    private int exceptionIndex;

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
                if (working && Model.instance.getSelectedDevice() != null) {
                    start();
                }
            }
        });
    }

    public void onToggleClicked(ActionEvent actionEvent) {
        if (working) {
            stop();
        } else {
            start();
        }
    }

    private void start() {
        if (Model.instance.getSelectedDevice() == null) {
            return;
        }

        toggleButtonsStates(false);
        logListItems.clear();
        working = true;
        buttonToggle.setText("Stop");

        executor.execute(new Runnable() {
            @Override
            public void run() {
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

                        if (!exceptionState) {
                            addLine(line);
                        }
                    }

                    process.destroy();

                } catch (Exception e) {
                    e.printStackTrace();
                }

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
        for (String line : logListItems) {
            listToSave.add(line);
        }

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

                    for (String line : listToSave) {
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
        this.exceptionState = exceptionState;
        buttonClearLogCat.setDisable(exceptionState);
    }
}
