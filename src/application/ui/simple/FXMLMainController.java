package application.ui.simple;

import application.WindowController;
import application.startupcheck.StartupCheckController;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FXMLMainController implements WindowController, Initializable {
    private Stage stage;

    @Override
    public void setStageAndSetupListeners(Stage stage) {
        this.stage = stage;
        stage.setWidth(1000);
        stage.setHeight(550);
        stage.setMinWidth(1000);
        stage.setMinHeight(550);

        stage.setMaxWidth(1200);
        stage.setMaxHeight(700);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openADBValidator();
    }

    private void openADBValidator() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    StartupCheckController.showScreen(getClass());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
