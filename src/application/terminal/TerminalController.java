package application.terminal;

import dx.helpers.AdbHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class TerminalController implements Initializable {

    @FXML
    private Label label;
    @FXML
    private TextArea actiontarget;

    @FXML
    private TextField textField;

    @FXML
    private void handleSubmitButtonAction(ActionEvent event) {

        String result = dx.Executor.run(AdbHelper.composeAdbCommand(textField.getText()));

        actiontarget.setText(result);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

}
