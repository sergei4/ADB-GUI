package application.ui.simple;

import application.WindowController;
import application.log.Logger;
import application.startupcheck.StartupCheckController;
import application.ui.simple.devices.DevicesController;
import application.ui.simple.devices.InstallApkListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        openADBValidator();

        devicesController.setInstallApkListener(new InstallApkListener() {
            @Override
            public void onStartInstall() {
                updApplicationLog(Color.BLUE, "Installing...");
            }

            @Override
            public void onSuccessInstall() {
                updApplicationLog(Color.GREEN, "Application has been installed successful");
            }

            @Override
            public void onFailedInstall(String result) {
                Logger.e(result);
                Pattern causeRegExpr = Pattern.compile("Failure (.*)");
                Matcher matcher = causeRegExpr.matcher(result);
                String cause = "";
                if(matcher.find()){
                    cause = matcher.group(1);
                }
                updApplicationLog(Color.RED, "Failed installation: " + cause);
            }
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

    private void updApplicationLog(Color color, String text) {
        Platform.runLater(() -> {
            appLogText.setTextFill(color);
            appLogText.setText(text);
        });
    }
}
