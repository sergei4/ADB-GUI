package application.ui.simple;

import application.FolderUtil;
import application.WindowController;
import application.log.Logger;
import application.preferences.Preferences;
import application.startupcheck.StartupCheckController;
import application.ui.simple.devices.DevicesController;
import application.ui.simple.logcat.LogcatController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FXMLMainController implements WindowController, Initializable {
    private static Stage stage;

    @FXML
    private ToggleButton btnOpenScreenShotScreen;

    @FXML
    private ToggleButton btnOpenLogScreen;

    @FXML
    private Label appLogText;

    @FXML
    private DevicesController devicesController;

    @FXML
    private LogcatController logcatController;

    @FXML
    private Pane logcat;

    @FXML
    private Pane screenshot;

    @Override
    public void setStageAndSetupListeners(Stage stage) {
        this.stage = stage;
        stage.setWidth(1200);
        stage.setHeight(600);

        stage.setMinWidth(1200);
        stage.setMinHeight(550);

        stage.setMaxWidth(1400);
        stage.setMaxHeight(800);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        screenshot.visibleProperty().bind(btnOpenScreenShotScreen.selectedProperty());
        logcat.visibleProperty().bind(btnOpenLogScreen.selectedProperty());

        appLogText.setText("");

        Logger.setShowLogListener(new Logger.LoggerListener() {
            @Override
            public void onNewLogToShow(String message) {
                log(Color.BLACK, message);
            }

            @Override
            public void onNewErrorLogToShow(String message) {
                log(Color.RED, message);
            }

            @Override
            public void onFinishLogToShow(String message) {
                log(Color.GREEN, message);
            }
        });

        openADBValidator();
    }

    protected void log(Color color, String message) {
        appLogText.setTextFill(color);
        appLogText.setText(message);
    }

    private void openADBValidator() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    StartupCheckController.showScreen(getClass(), stage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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

    public void onOpenScreenshotFolderClicked(ActionEvent actionEvent) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(FolderUtil.getSnapshotFolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onSaveLogFolderClicked(ActionEvent actionEvent) {
        logcatController.saveSelectedDeviceLog();
    }

    public static void showDialog(Stage dialog) {
        Platform.runLater(() -> {
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            dialog.show();
        });
    }
}
