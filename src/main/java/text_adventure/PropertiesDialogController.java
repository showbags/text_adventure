package text_adventure;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PropertiesDialogController implements Initializable
{
    private boolean ok;
    private Stage stage;

    @FXML
    private TextField gameNameField;

    @FXML
    private TextField startScreenField;

    @FXML
    private TextArea gameDescriptionArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setStage(Stage stage) { this.stage = stage; }

    public String getGameName() { return gameNameField.getText(); }

    public String getStartScreen() { return startScreenField.getText(); }

    public String getGameDescription() { return gameDescriptionArea.getText(); }

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

    public void setGame(Game game)
    {
        gameNameField.setText(game.getGameName());
        startScreenField.setText(game.getStartScreen());
        gameDescriptionArea.setText(game.getGameOverview());
    }
}
