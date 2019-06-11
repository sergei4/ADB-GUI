package dx.ui.progress;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ProgressDialogController implements Initializable {

    @FXML
    private Label text1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public static Stage createDialog(String progressString) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ProgressDialogController.class.getResource("/dx/ui/progress/ProgressDialogLayout.fxml"));

            Parent root1 = fxmlLoader.load();

            ProgressDialogController controller = fxmlLoader.getController();
            controller.setProgressText(progressString);

            Stage stage = new Stage();
            Scene scene = new Scene(root1);
            stage.setScene(scene);
            return stage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setProgressText(String text) {
        text1.setText(text);
    }
}
