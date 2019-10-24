package dx;

import application.AdbUtils;
import application.FolderUtil;
import application.WindowController;
import application.log.Logger;
import application.preferences.Preferences;
import application.startupcheck.StartupCheckController;
import dx.model.Device;
import dx.ui.devices.DevicesController;
import dx.ui.logcat.LogcatController;
import dx.ui.logcat.LogcatUtils;
import dx.ui.progress.ProgressDialogController;
import dx.ui.screencapture.ScreenCaptureController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
    private ScreenCaptureController screenshotController;

    @FXML
    private Pane menuPane;

    @FXML
    private Pane logcat;

    @FXML
    private Pane screenshot;

    @Override
    public void setStageAndSetupListeners(Stage stage) {
        FXMLMainController.stage = stage;

        stage.setWidth(1400);
        stage.setHeight(800);

        stage.setMinWidth(1200);
        stage.setMinHeight(700);

        stage.setMaxWidth(1400);
        stage.setMaxHeight(800);

        stage.setAlwaysOnTop(Preferences.getInstance().isWindowIsAlwaysOn());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //init controllers
        screenshotController.setDeviceSupplier(() -> devicesController.getSelectedDevice());

        logcatController.setDeviceSupplier(() -> devicesController.getSelectedDevice());
        devicesController.addListener(logcatController.getDeviceControllerListener());

        //init menu
        MenuBar menuBar = new MenuBar();

        Menu settingsMenu = new Menu("Settings");
        menuBar.getMenus().add(settingsMenu);

        CheckMenuItem alwaysOnTop = new CheckMenuItem("Always on top");
        settingsMenu.getItems().add(alwaysOnTop);

        alwaysOnTop.setSelected(Preferences.getInstance().isWindowIsAlwaysOn());
        alwaysOnTop.selectedProperty().addListener((observable, oldValue, newValue) -> {
            Preferences.getInstance().setWindowIsAlwaysOn(newValue);
            stage.setAlwaysOnTop(newValue);
        });

        //menuPane.getChildren().add(menuBar);

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

    private void setWindowOnTop(boolean b) {
        stage.setAlwaysOnTop(b);
    }

    protected void log(Color color, String message) {
        Platform.runLater(() -> {
            appLogText.setTextFill(color);
            appLogText.setText(message);
        });
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

    @FXML
    public void onOpenLogFolderClicked(ActionEvent actionEvent) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(FolderUtil.getLogsFolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onOpenScreenshotFolderClicked(ActionEvent actionEvent) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(FolderUtil.getSnapshotFolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onSaveLogFolderClicked(ActionEvent actionEvent) throws Exception {
        Device currentDevice = devicesController.getSelectedDevice();
        if (currentDevice != null && currentDevice.isConnected()) {
            Stage dialog = ProgressDialogController.createDialog("Gathering information. Please wait...");
            dialog.initStyle(StageStyle.UNDECORATED);
            FXMLMainController.showDialog(dialog);
            LogcatUtils.save(currentDevice, currentDevice.observeFullDeviceLog(), () -> Platform.runLater(dialog::close));
        }
    }

    @FXML
    public void onOpenLangSettingsClicked(ActionEvent actionEvent) {
        Device currentDevice = devicesController.getSelectedDevice();
        if (currentDevice != null && currentDevice.isConnected()) {
            AdbUtils.run(currentDevice.getId(), "adb shell am start -n com.android.settings/.LanguageSettings");
        }
    }

    public static void showDialog(Stage dialog) {
        Platform.runLater(() -> {
            dialog.widthProperty().addListener((observable, oldValue, newValue) -> {
                dialog.setX(stage.getX() + stage.getWidth() / 2 - dialog.getWidth() / 2);
            });
            dialog.heightProperty().addListener((observable, oldValue, newValue) -> {
                dialog.setY(stage.getY() + stage.getHeight() / 2 - dialog.getHeight() / 2);
            });

            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            dialog.showAndWait();
        });
    }
}
