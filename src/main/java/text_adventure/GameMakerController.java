package text_adventure;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
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

    private Game game;

    // Add a public no-args constructor
    public GameMakerController()
    {
    }

    public void setGame(Game game)
    {
        this.game=game;
        for (Screen screen : game.getScreens().values() )
        {

        }
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
                    Screen screen = new Screen(game);
                    screen.setX(cursorRect.getX());
                    screen.setY(cursorRect.getY());
                    Rectangle rect = new ScreenRect(screen);
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

    class ScreenRect extends Rectangle
    {
        private Screen screen;

        public ScreenRect(Screen screen)
        {
            super(cursorRect.getWidth(), cursorRect.getHeight());
            this.screen=screen;
            setLayoutX(screen.getX());
            setLayoutY(screen.getY());
            setFill(Color.TRANSPARENT);
            setStroke(Color.BLACK);
            setOnMouseEntered( e -> setStroke(Color.RED) );
            setOnMouseExited( e -> setStroke(Color.BLACK) );
        }

    }

}

