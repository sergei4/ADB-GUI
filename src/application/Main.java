package application;

import application.log.Logger;
import application.preferences.Preferences;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.PrintStream;

public class Main extends Application {

    public static HostServices hostService;

    public static Font courierFont13;

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (Preferences.getInstance().isDebug()) {
            System.setOut(new PrintStream(Preferences.getInstance().getLogFile()));
            System.setErr(new PrintStream(Preferences.getInstance().getLogFileErr()));
        }

        if (Preferences.getInstance().getAdbPath().equals("")) {
            findADBPath();
        }

        AdbUtils.setAdbInstallLocationProvider(Preferences.getInstance());

        hostService = getHostServices();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("../dx/FXMLMain.fxml"));

        courierFont13 = Font.loadFont(getClass().getResource("cour.ttf").toString(), 13);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 620);

        Image iconImage = new Image("/res/devexperts_logo.png");
        primaryStage.getIcons().add(iconImage);

        primaryStage.setTitle("ADB GUI Tool");
        primaryStage.setScene(scene);

        WindowController controller = loader.getController();
        controller.setStageAndSetupListeners(primaryStage); // or what you want to do

        //primaryStage.setResizable(false);
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                AdbUtils.run("adb kill-server");
                Platform.exit();
                System.exit(0);
            }
        });

        //Todo: think how to resolve this
        if (FolderUtil.getSnapshotFolder().getAbsolutePath().contains(" ")) {
            DialogUtil.showErrorDialog("This app do not support operating from a path with spaces,\n" +
                    "please move the app and start again");
            System.exit(0);
        }
    }

    private void findADBPath() {
        Logger.d("Find adb on: " + Preferences.OS);

        if (Preferences.OS.startsWith("windows")) {
            File adbPath = new File(System.getProperty("user.dir"), "platform-tools" + File.separator + "windows");
            File adbFile = new File(adbPath, "adb.exe");
            if (adbFile.exists()) {
                Preferences.getInstance().setAdbPath(adbPath.getAbsolutePath() + File.separator);
            }
        } else {
            File adbPath = new File(System.getProperty("user.dir"), "platform-tools" + File.separator + "_nix");
            File adbFile = new File(adbPath, "adb");
            if (adbFile.exists()) {
                Preferences.getInstance().setAdbPath(adbPath.getAbsolutePath() + File.separator);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
