package text_adventure;

import javafx.beans.property.*;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
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
    private HashMap<Screen,ScreenRect> rects = new HashMap<>();

    // Add a public no-args constructor
    public GameMakerController() { }

    public void setGame(Game game)
    {
        this.game=game;
        for (Screen screen : game.getScreens().values() )
            addScreen(screen);
        for (Node node : pane.getChildren())
        {
            if (node instanceof ScreenLinkLine)
            {
                ((ScreenLinkLine)node).setTo();
            }
        }
    }

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources)
    {
        pane.requestFocus();
        pane.setOnKeyPressed(keyEvent->{
            if (keyEvent.getCode()==KeyCode.SPACE){
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
        rects.put(screen,rect);

        rect.selectedProperty().addListener( (obs,ov,nv) ->{
            if (nv)
            {
                titleField.setText(rect.titleProperty().getValue());
                rect.titleProperty().bind(titleField.textProperty());
                descriptionArea.setText(rect.descriptionProperty().getValue());
                rect.descriptionProperty().bind(descriptionArea.textProperty());
                int iter=0;
                for (ScreenLink link : rect.getScreen().getLinks().values() )
                    directionBox.getChildren().add(iter++,new DirectionForm(link));
            }
            else
            {
                rect.titleProperty().unbind();
                rect.descriptionProperty().unbind();
                List<Node> list = directionBox.getChildren();
                list.subList(0, list.size()-1).clear();//leave item 0. It is the + button
            }
        });
        selectOnly(rect);
        pane.getChildren().add(rect);
        for (ScreenLink link : screen.getLinks().values())
            pane.getChildren().add(new ScreenLinkLine(rect, link));
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

    class ScreenRect extends StackPane
    {
        private Screen screen;
        private BooleanProperty selected = new SimpleBooleanProperty();
        private StringProperty titleProperty, descriptionProperty;

        public ScreenRect(Screen screen)
        {
            this.screen=screen;
            setMinWidth(cursorRect.getWidth());
            setMaxWidth(cursorRect.getWidth());
            setMinHeight(cursorRect.getHeight());
            setMaxHeight(cursorRect.getHeight());
            this.screen=screen;
            titleProperty = new SimpleStringProperty(screen.getTitle());
            descriptionProperty = new SimpleStringProperty(screen.getDescription());
            Label label = new Label();
            label.textProperty().bind(titleProperty);
            label.setFont(Font.font("Arial", 8));
            label.setAlignment(Pos.CENTER);

            getChildren().add(label);
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

            enableDrag(this);
            setStyle();
        }

        public Screen getScreen() { return this.screen; }

        public StringProperty titleProperty() { return titleProperty; }

        public StringProperty descriptionProperty() { return descriptionProperty; }

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
            return "-fx-background-color: rgba(200,200,200,0.5);";
        }

        public boolean isSelected() { return selected.getValue(); }

        public void setSelected(boolean selected)  {  this.selected.setValue(selected); }

        public BooleanProperty selectedProperty() { return this.selected; }
    }

    static class Orig { double x, y; }

    // make a node movable by dragging it around with the mouse.
    private void enableDrag(final ScreenRect rect) {
        final Orig orig= new Orig();
        rect.setOnMousePressed(e->{
            orig.x = e.getX();
            orig.y = e.getY();
            e.consume();
        });
        rect.setOnMouseDragged(e->{
            double delx = orig.x-e.getX();
            double dely = orig.y-e.getY();
            rect.relocate( rect.getLayoutX()-delx, rect.getLayoutY()-dely );
            e.consume();
        });
    }
    
    static class DirectionForm extends VBox
    {
        public DirectionForm(ScreenLink link)
        {
            Label toLabel = new Label("To");
            toLabel.setMinWidth(100);
            TextField toField = new TextField(link.getScreen());
            HBox.setHgrow(toField, Priority.ALWAYS);
            getChildren().add(new HBox(toLabel,toField));

            Label dirLabel = new Label("Direction");
            dirLabel.setMinWidth(100);
            TextField dirField = new TextField(link.getDirection());
            HBox.setHgrow(dirField, Priority.ALWAYS);
            getChildren().add(new HBox(dirLabel, dirField));

            Label descLabel = new Label("Description");
            descLabel.setMinWidth(100);
            TextField descField = new TextField(link.getDescription());
            HBox.setHgrow(descField, Priority.ALWAYS);
            getChildren().add(new HBox(descLabel, descField));

            CheckBox cb = new CheckBox("Can pass");
            cb.setSelected(link.canPass());
            cb.setMinWidth(100);
            TextField tb = new TextField(link.cantPassMessage());
            tb.disableProperty().bind(cb.selectedProperty());
            HBox.setHgrow(tb, Priority.ALWAYS);
            getChildren().add(new HBox(cb, tb));
        }
    }

    class ScreenLinkLine extends CubicCurve
    {
        private ScreenRect from;
        private ScreenLink link;

        public ScreenLinkLine(ScreenRect from, ScreenLink link)
        {
            this.from=from;
            this.link=link;
            setStrokeWidth(2d);
            double r = Math.random();
            double g = Math.random();
            double b = Math.random();
            String dir = link.getDirection();
            double pmx = dir.equals("west") ? -40 : dir.equals("east") ? 40 : 0;
            double pmy = dir.equals("north") ? -40 : dir.equals("south") ? 40 : 0;
            setStroke(Color.color(r, g, b));
            setFill(null);
            startXProperty().bind(from.layoutXProperty().add(pmx));
            startYProperty().bind(from.layoutYProperty().add(pmy));
            setTo();
        }

        public void setTo()
        {
            Screen to = from.getScreen().getScreen(link);
            ScreenRect toRect=GameMakerController.this.rects.get(to);

            //TODO: listen to additions of screens to update this line where necessary
            if (toRect!=null)
            {
                endXProperty().bind(from.layoutXProperty().add(toRect.layoutXProperty()).multiply(0.5));
                endYProperty().bind(from.layoutYProperty().add(toRect.layoutYProperty()).multiply(0.5));
                controlX1Property().bind(startXProperty());
                controlY1Property().bind(startYProperty());
                controlX2Property().bind(endXProperty());
                controlY2Property().bind(endYProperty());
            }
        }

    }
}

