package application.ui.simple.screencapture;

import application.ADBHelper;
import application.AdbUtils;
import application.DateUtil;
import application.FolderUtil;
import application.log.Logger;
import application.model.Model;
import application.preferences.Preferences;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenCaptureController implements Initializable {

    @FXML
    public Pane paneImageContainer;

    private File snapshotsFolder = FolderUtil.getSnapshotFolder();

    @FXML
    public ImageView imageViewCapture;

    @FXML
    public Pane pane;

    private Image screenshotImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    private void updatePicture() {
        File file = getTempSnapshotFile();
        if (file.exists()) {
            screenshotImage = new Image(file.toURI().toString());

            updateScreenRatio(screenshotImage);

            imageViewCapture.setImage(screenshotImage);

            imageViewCapture.fitWidthProperty().bind(paneImageContainer.widthProperty());
            imageViewCapture.fitHeightProperty().bind(paneImageContainer.heightProperty());
        }
    }

    private void updateScreenRatio(Image image) {
        //stage.minWidthProperty().bind(scene.heightProperty().divide(1.83333333333333d));
        //stage.minHeightProperty().bind(scene.widthProperty().multiply(1.83333333333333d));
    }

    @FXML
    public void onCreateSnapshotClicked(ActionEvent actionEvent) {
        new Thread(() -> {
            String tempPicture = "/sdcard/temp.png";
            Logger.d("Taking snapshot " + tempPicture);

            String result = AdbUtils.run("shell screencap -p " + tempPicture);
            if (!result.equals("")) {
                Logger.e("Error taking snapshot: " + result);
                return;
            }

            File snapshotFile = getTempSnapshotFile();
            if (snapshotFile.exists()) {
                snapshotFile.delete();
            }

            if (ADBHelper.pull(tempPicture, snapshotFile.getAbsolutePath())) {
                Logger.d("Created snapshot: " + snapshotFile.getAbsolutePath());
            }

            ADBHelper.rm(tempPicture);

            Platform.runLater(() -> updatePicture());
        }).start();
    }

    @FXML
    public void onSaveClicked(ActionEvent actionEvent) {
        if (screenshotImage != null) {
            new Thread(() -> {
                Logger.ds("Saving snapshot...");
                String fileName = Model.instance.getSelectedDevice().getName() + " " +
                        Model.instance.getSelectedDevice().getAndroidVersion() + " " +
                        DateUtil.getCurrentTimeStamp() + ".png";

                fileName = fileName.replace(" ", "");

                File snapshotFile = new File(snapshotsFolder, fileName);

                BufferedImage bImage = SwingFXUtils.fromFXImage(screenshotImage, null);
                try {
                    ImageIO.write(bImage, "png", snapshotFile);
                    Logger.fs("Snapshot saved: " + snapshotFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.es("Error during creating snapshot");
                }
            }).start();
        }
    }

    public void onOpenFolderClicked(ActionEvent actionEvent) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(new File(Preferences.getInstance().getSnapshotFolder()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getTempSnapshotFile() {
        return new File(snapshotsFolder, "temp_snapshot.png");
    }
}
