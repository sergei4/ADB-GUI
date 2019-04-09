package application.ui.simple;

import application.WindowController;
import application.log.Logger;
import application.startupcheck.StartupCheckController;
import application.ui.simple.devices.DevicesController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FXMLMainController implements WindowController, Initializable {
    private Stage stage;

    @FXML
    private Label appLogText;

    @FXML
    private DevicesController devicesController;

    @Override
    public void setStageAndSetupListeners(Stage stage) {
        this.stage = stage;
        stage.setWidth(1000);
        stage.setHeight(600);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);

        stage.setMaxWidth(1400);
        stage.setMaxHeight(700);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
}