package text_adventure;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private VBox directionBox;

    private Game game;

    // Add a public no-args constructor
    public GameMakerController()
    {
    }

    public void setGame(Game game)
    {
        this.game=game;
        for (Screen screen : game.getScreens().values() )
            addScreen(screen);
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
                addScreen(screen);
                keyEvent.consume();
            }
        });
    }

    private void addScreen(Screen screen)
    {
        ScreenRect rect = new ScreenRect(screen);
        rect.selectedProperty().addListener( (obs,ov,nv) ->{
            if (nv)
            {
                titleField.setText(rect.titleProperty().getValue());
                rect.titleProperty().bind(titleField.textProperty());
                descriptionArea.setText(screen.getDescription());
                for (ScreenLink link : rect.getScreen().getLinks().values() )
                    directionBox.getChildren().add(new DirectionForm(link));
            }
            else
            {
                rect.titleProperty().unbind();
                directionBox.getChildren().clear();
            }
        });
        pane.getChildren().add(rect);
    }

    @FXML
    private void mouseEntered()
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

    @FXML
    private void newDirection()
    {
        System.out.println("add direction");
    }

    @FXML
    private void newItem()
    {
        System.out.println("add item");
    }

    @FXML
    private void newAction()
    {
        System.out.println("add action");
    }

    public void selectOnly(ScreenRect rect)
    {
        for (Node node : pane.getChildren())
        {
            if (node instanceof ScreenRect)
                ((ScreenRect)node).setSelected(false);
        }
        rect.setSelected(true);
    }

    class ScreenRect extends Pane
    {
        private Screen screen;
        private BooleanProperty selected = new SimpleBooleanProperty();
        private StringProperty titleProperty;

        public ScreenRect(Screen screen)
        {
            this.screen=screen;
            setMinWidth(cursorRect.getWidth());
            setMaxWidth(cursorRect.getWidth());
            setMinHeight(cursorRect.getHeight());
            setMaxHeight(cursorRect.getHeight());
            this.screen=screen;
            titleProperty = new SimpleStringProperty(screen.getTitle());
            Label label = new Label();
            label.textProperty().bind(titleProperty);
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

        public Screen getScreen() { return this.screen; }

        public StringProperty titleProperty() { return titleProperty; }

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

        public BooleanProperty selectedProperty() { return this.selected; }

    }

    class DirectionForm extends VBox
    {
        public DirectionForm(ScreenLink link)
        {
            getChildren().add(new Label("screen"));
            getChildren().add(new TextField(link.getScreen()));
            getChildren().add(new Label("direction"));
            getChildren().add(new TextField(link.getDirection()));
            getChildren().add(new Label("description"));
            getChildren().add(new TextField(link.getDescription()));
            CheckBox cb = new CheckBox("can_pass");
            cb.setSelected(link.canPass());
            getChildren().add(cb);
            getChildren().add(new Label("cant_pass_message"));
            getChildren().add(new TextField(link.cantPassMessage()));
        }
    }
}

