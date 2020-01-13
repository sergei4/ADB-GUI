package dx;

import application.WindowController;
import application.log.Logger;
import application.preferences.Preferences;
import application.utils.DialogUtil;
import application.utils.FolderUtil;
import dx.helpers.AdbHelper;
import dx.helpers.IosHelper;
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

        if (Preferences.getInstance().getPlatformToolsPath().equals("")) {
            findPlatformToolsPath();
        }

        AdbHelper.setAdbExecLocation(() -> Preferences.getInstance().getPlatformToolsPath());
        IosHelper.setLibExecLocation(() -> Preferences.getInstance().getPlatformToolsPath());

        hostService = getHostServices();

        FXMLLoader loader = new FXMLLoader(FXMLMainController.class.getResource("FXMLMain.fxml"));

        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 620);

        Image iconImage = new Image("/res/devexperts_logo.png");
        primaryStage.getIcons().add(iconImage);

        primaryStage.setTitle("MobiTool");
        primaryStage.setScene(scene);

        WindowController controller = loader.getController();
        controller.setStageAndSetupListeners(primaryStage); // or what you want to do

        //primaryStage.setResizable(false);
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                AdbHelper.killServer();
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

    private void findPlatformToolsPath() {
        Logger.d("Find : " + Preferences.OS);
        System.out.println(System.getProperty("user.dir"));

        if (Preferences.OS.startsWith("windows")) {
            File platformToolsPath = new File(System.getProperty("user.dir"), "platform-tools" + File.separator + "windows");
            if (platformToolsPath.exists()) {
                Preferences.getInstance().setPlatformToolsPath(platformToolsPath.getAbsolutePath() + File.separator);
            }
        } else {
            File platformToolsPath = new File(System.getProperty("user.dir"), "platform-tools" + File.separator + "_nix");
            if (platformToolsPath.exists()) {
                Preferences.getInstance().setPlatformToolsPath(platformToolsPath.getAbsolutePath() + File.separator);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
