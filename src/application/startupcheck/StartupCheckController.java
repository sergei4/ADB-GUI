package application.startupcheck;

import application.ADBHelper;
import application.log.Logger;
import application.preferences.Preferences;
import dx.service.DeviceMonitorService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartupCheckController implements Initializable {

    public static ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public Pane pane;
    public TextField textFieldADBPath;
    public VBox vboxEditAdbPath;
    public VBox vboxLaoding;
    public Label labelStatus;
    private Stage stage;
    private Scene scene;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                boolean adbExists = ADBHelper.isADBFound();

                if (!adbExists) {
                    if (tryToFindADB()) {
                        adbExists = ADBHelper.isADBFound();
                        Logger.d("Is adb found after auto search: " + adbExists);
                    }
                } else {
                    Logger.d("Is adb found: " + Preferences.getInstance().getAdbPath());
                }

                boolean finalAdbExists = adbExists;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (finalAdbExists) {
                            stage.close();
                            DeviceMonitorService.instance.start();
                        } else {
                            vboxLaoding.setVisible(false);
                            vboxLaoding.setManaged(false);
                            vboxEditAdbPath.setVisible(true);
                            vboxEditAdbPath.setManaged(true);

                            textFieldADBPath.setText(Preferences.getInstance().getAdbPath());
                        }
                    }
                });
            }
        }).start();
    }

    private boolean tryToFindADB() {
// /Users/evgeni.shafran/Library/Android/sdk/platform-tools/
        File file = new File("/Users/");

        if (file.exists()) {
            for (File userFolder : file.listFiles()) {
                File adbPath = new File(userFolder, "/Library/Android/sdk/platform-tools/");
                if (adbPath.exists()) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Preferences.getInstance().setAdbPath(adbPath.getAbsolutePath() + "/");
                            try {
                                Preferences.getInstance().save();
                            } catch (IOException e) {
                            }
                        }
                    });

                    Logger.d("Found ADB at: " + adbPath.getAbsolutePath());

                    return true;
                }
            }
        }
        return false;
    }

    private void setStage(Stage stage, Scene scene) {
        this.stage = stage;
        this.scene = scene;
    }

    public static void showScreen(Class class1) throws IOException {
        showScreen(class1, null);
    }

    public static void showScreen(Class class1, Stage owner) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(class1.getResource("/application/startupcheck/StartupCheckLayout.fxml"));

        Parent root1 = (Parent) fxmlLoader.load();

        StartupCheckController controller = fxmlLoader.<StartupCheckController>getController();

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Startup Check");
        Scene scene = new Scene(root1);
        stage.setScene(scene);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.show();

        controller.setStage(stage, scene);
    }

    public void onCheckClicked(ActionEvent actionEvent) {
        Preferences.getInstance().setAdbPath(textFieldADBPath.getText());
        try {
            Preferences.getInstance().save();
        } catch (IOException ignored) {
        }

        labelStatus.setText("Checking path...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean adbExists = ADBHelper.isADBFound();
                Logger.d("Is adb found after user input: " + adbExists);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (adbExists) {
                            labelStatus.setText("ADB found! Go on and have some fun!");
                            labelStatus.setTextFill(Color.GREEN);
                            DeviceMonitorService.instance.start();
                        } else {
                            labelStatus.setText("No ADB found... Try different path");
                            labelStatus.setTextFill(Color.RED);
                        }
                    }
                });
            }
        }).start();
    }
}
