package text_adventure;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ScreenSelectDialogController implements Initializable
{
    private boolean ok;
    private Stage stage;

    @FXML
    private ListView<String> listView;

    @FXML
    private TextField dirField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setStage(Stage stage) { this.stage = stage; }

    public void setGame(Game game)
    {
        ObservableList<String> screens = FXCollections.observableArrayList( game.screens.keySet() );
        listView.setItems( screens );
    }

    public String getSelectedScreen()
    {
        return listView.getSelectionModel().getSelectedItem();
    }

    public String getDirection() { return dirField.getText(); }

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
