import java.net.URL;
import java.util.ResourceBundle;
 
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.fxml.Initializable;
import javafx.scene.paint.Color;

public class GameMakerController
implements Initializable
{
    @FXML
    private Rectangle cursorRect;

    @FXML
    private AnchorPane pane;

    // Add a public no-args constructor
    public GameMakerController()
    {
    }

    @Override 
    public void initialize(URL fxmlFileLocation, ResourceBundle resources)
    {
        pane.requestFocus();
	pane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
	        if (keyEvent.getCode()==KeyCode.SPACE){
		     System.out.println("Space pressed");
                     Rectangle rect = new SceneRect();
		     pane.getChildren().add(rect);
		     keyEvent.consume();
		}
	    }
        });
    }

    @FXML
    private void mouseEntered(MouseEvent event)
    {
	 pane.requestFocus();
    }

    @FXML
    private void mouseClicked(MouseEvent event)
    {
	    cursorRect.setLayoutX(event.getX());
	    cursorRect.setLayoutY(event.getY());
	    pane.requestFocus();
    }

    class SceneRect extends Rectangle
    {
	public SceneRect()
	{
        	super(cursorRect.getWidth(), cursorRect.getHeight());
		setTranslateX(-cursorRect.getWidth()/2);
		setTranslateY(-cursorRect.getHeight()/2);
		setLayoutX(cursorRect.getLayoutX());
		setLayoutY(cursorRect.getLayoutY());
		setFill(Color.TRANSPARENT);
		setStroke(Color.BLACK);
		setOnMouseEntered( e -> setStroke(Color.RED) );
		setOnMouseExited( e -> setStroke(Color.BLACK) );
	}

    }

}

