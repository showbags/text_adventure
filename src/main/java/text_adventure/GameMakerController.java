package text_adventure;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.ResourceBundle;

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
            Pane rect = new ScreenRect(screen);
            pane.getChildren().add(rect);
        }
    }

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources)
    {
        pane.requestFocus();
        pane.setOnKeyPressed(keyEvent->{
            if (keyEvent.getCode()==KeyCode.SPACE){
                System.out.println("Space pressed");
                Screen screen = new Screen(game);
                screen.setLocation(cursorRect.getLayoutX(), cursorRect.getLayoutY());
                Pane rect = new ScreenRect(screen);
                pane.getChildren().add(rect);
                keyEvent.consume();
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
        double d = cursorRect.getWidth();
        cursorRect.setLayoutX(d*(Math.round(event.getX()/d)));
        cursorRect.setLayoutY(d*(Math.round(event.getY()/d)));
        pane.requestFocus();
    }

    public void selectOnly(ScreenRect rect)
    {
        for (Node node : pane.getChildren())
        {
            if (node instanceof ScreenRect)
            {
                ((ScreenRect)node).setSelected(false);
            }
        }
        rect.setSelected(true);
    }

    class ScreenRect extends Pane
    {
        private Screen screen;
        private BooleanProperty selected = new SimpleBooleanProperty();

        public ScreenRect(Screen screen)
        {
            setMinWidth(cursorRect.getWidth());
            setMaxWidth(cursorRect.getWidth());
            setMinHeight(cursorRect.getHeight());
            setMaxHeight(cursorRect.getHeight());
            this.screen=screen;
            Label label = new Label(screen.getTitle());
            label.setFont(Font.font("Arial", 8));
            label.setAlignment(Pos.CENTER);

            HBox hbox = new HBox(label);
            hbox.setAlignment(Pos.CENTER);
            hbox.setPrefSize(cursorRect.getWidth(),cursorRect.getHeight());
            getChildren().add(hbox);
            setTranslateX(cursorRect.getTranslateX());
            setTranslateY(cursorRect.getTranslateY());
            setLayoutX(screen.getX());
            setLayoutY(screen.getY());
            focusedProperty().addListener( (obs,ov,nv)-> setStyle());
            setOnMouseEntered( e -> requestFocus() );
            setOnMouseExited( e -> getParent().requestFocus() );
            selected.addListener( (obs,ov,nv)->setStyle());

            setOnMouseClicked( e ->
            {
                selectOnly(this);
                e.consume();
            } );
            setStyle();
        }

        public String selectedBorder = "-fx-border-color: blue;";
        public String focusedBorder = "-fx-border-color: black;";
        public String nonFocusedBorder = "";

        public void setStyle() { setStyle(makeStyle()); }

        public String makeStyle()
        {
            return background()+border();
        }

        public String border()
        {
            return isSelected() ? selectedBorder : isFocused() ? focusedBorder : nonFocusedBorder;
        }

        public String background()
        {
            return "-fx-background-color: rgba(220,220,220,0.5);";
        }

        public boolean isSelected() { return selected.getValue(); }

        public void setSelected(boolean selected)  {  this.selected.setValue(selected); }

    }
}

