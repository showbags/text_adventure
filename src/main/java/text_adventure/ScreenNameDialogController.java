package text_adventure;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ScreenNameDialogController implements Initializable
{
    private boolean ok;
    private Stage stage;

    @FXML
    private TextField nameField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setStage(Stage stage) { this.stage = stage; }

    public String getName() { return nameField.getText(); }

    public boolean ok() { return ok; }

    @FXML
    public void okPressed()
    {
        ok = true;
        stage.close();
    }

    @FXML
    public void cancelPressed()
    {
        stage.close();
    }
}

