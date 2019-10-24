package dx.ui.screencapture;

import application.ADBHelper;
import application.AdbUtils;
import application.FileUtils;
import application.FolderUtil;
import application.log.Logger;
import application.preferences.Preferences;
import dx.model.Device;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScreenCaptureController implements Initializable {

    @FXML
    public Pane paneImageContainer;

    private File snapshotsFolder = FolderUtil.getSnapshotFolder();

    @FXML
    public ImageView imageViewCapture;

    @FXML
    public Pane pane;

    @FXML
    private Button btnOpenInEditor;

    private DeviceSnapshot deviceSnapshot;

    private Supplier<Device> deviceSupplier = () -> null;

    private static class DeviceSnapshot {
        Device device;
        Image image;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (Preferences.OS.startsWith("windows")) {
            btnOpenInEditor.setVisible(true);
            btnOpenInEditor.setManaged(true);
        }
    }

    public void setDeviceSupplier(Supplier<Device> supplier) {
        deviceSupplier = supplier;
    }

    private void updatePicture(Device device) {
        File file = getTempSnapshotFile();
        if (file.exists()) {
            deviceSnapshot = new DeviceSnapshot();

            deviceSnapshot.device = device;
            deviceSnapshot.image = new Image(file.toURI().toString());

            updateScreenRatio(deviceSnapshot.image);

            imageViewCapture.setImage(deviceSnapshot.image);

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
            Device currentDevice = deviceSupplier.get();
            if (currentDevice != null && currentDevice.isConnected()) {
                Logger.d("Taking snapshot " + tempPicture);

                String result = AdbUtils.run(currentDevice.getId(), "adb shell screencap -p " + tempPicture);
                if (!result.equals("")) {
                    Logger.e("Error taking snapshot: " + result);
                    return;
                }

                File snapshotFile = getTempSnapshotFile();
                if (snapshotFile.exists()) {
                    snapshotFile.delete();
                }

                if (ADBHelper.pull(currentDevice.getId(), tempPicture, snapshotFile.getAbsolutePath())) {
                    Logger.d("Created snapshot: " + snapshotFile.getAbsolutePath());
                }

                ADBHelper.rm(currentDevice.getId(), tempPicture);

                Platform.runLater(() -> updatePicture(currentDevice));
            }
        }).start();
    }

    @FXML
    private void onSaveClicked(ActionEvent actionEvent) {
        saveFileImpl(s -> {
        });
    }

    private void saveFileImpl(Consumer<File> createdFile) {
        if (deviceSnapshot != null) {
            new Thread(() -> {

                File snapshotFile = FileUtils.createSnapshotFile(deviceSnapshot.device);

                BufferedImage bImage = SwingFXUtils.fromFXImage(deviceSnapshot.image, null);
                float ratio = 400f / bImage.getWidth();

                try {
                    bImage = Thumbnails.of(bImage).size((int) (bImage.getWidth() * ratio), (int) (bImage.getHeight() * ratio)).asBufferedImage();
                    ImageIO.write(bImage, "png", snapshotFile);
                    createdFile.accept(snapshotFile);
                    Logger.fs("Snapshot saved: " + snapshotFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.es("Error during creating snapshot");
                }
            }).start();
        }
    }

    @FXML
    private void onEditClicked(ActionEvent actionEvent) {
        saveFileImpl(file -> {
            try {
                Desktop.getDesktop().edit(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
