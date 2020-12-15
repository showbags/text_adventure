package text_adventure;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
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
    private TextField imageField;

    @FXML
    private VBox directionBox;

    @FXML
    private VBox itemsBox;

    @FXML
    private VBox actionsBox;

    private Game game;
    private HashMap<Screen,ScreenRect> rects = new HashMap<>();
    private HashMap<Link, LinkLine> links = new HashMap<>();

    // Add a public no-args constructor
    public GameMakerController() { }

    public void setGame(Game game)
    {
        this.game=game;
        for (Screen screen : game.getScreens())
            addScreen(screen);
        for (Node node : pane.getChildren())
        {
            if (node instanceof LinkLine)
            {
                ((LinkLine)node).setTo();
            }
        }
    }

    public Game getGame() { return this.game; }

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources)
    {
        pane.requestFocus();
        pane.setOnKeyPressed(keyEvent->{
            if (keyEvent.getCode()==KeyCode.SPACE){
                try
                {
                    FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/screen_name.fxml"));
                    Node node = loader.load();
                    ScreenNameDialogController c = loader.getController();
                    Stage stage = new Stage(StageStyle.UTILITY);
                    stage.setTitle("New screen name");
                    Scene scene = new Scene((Parent) node);
                    stage.setScene(scene);
                    c.setStage(stage);
                    stage.showAndWait();
                    if (c.ok())
                    {
                        Screen screen = new Screen(c.getName());
                        screen.setLocation(cursorRect.getLayoutX(), cursorRect.getLayoutY());
                        game.addScreen(screen);
                        addScreen(screen);
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                keyEvent.consume();
            }
            else if (keyEvent.getCode()==KeyCode.DELETE)
            {
                ScreenRect selected = getSelected();
                pane.getChildren().remove(selected);
                for (Link link : selected.getScreen().getLinks())
                    pane.getChildren().remove(links.get(link));
                game.removeScreen(selected.getScreen());
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
                imageField.setText(rect.getScreen().getImageName());
                rect.imageProperty().bind(imageField.textProperty());
                directionBox.getChildren().clear();
                for (Link link : screen.getLinks() )
                    addDirectionForm(screen,link);

                itemsBox.getChildren().clear();
                for (Item item : screen.getItems() )
                    addItemForm(screen,item);

                actionsBox.getChildren().clear();
                for (Action action : screen.getActions())
                    addActionForm(screen, action);
            }
            else
            {
                rect.titleProperty().unbind();
                rect.descriptionProperty().unbind();
                rect.imageProperty().unbind();
                List<Node> list = directionBox.getChildren();
                if (!list.isEmpty())
                    list.subList(0, list.size()-1).clear();//leave item 0. It is the + button
            }
        });
        selectOnly(rect);
        pane.getChildren().add(rect);
        for (Link link : screen.getLinks())
            addLink(screen,link);
    }

    private void addDirectionForm(Screen screen, Link link)
    {
        directionBox.getChildren().add(new DirectionForm(this,screen,link));
    }

    private void removeDirectionForm(DirectionForm form)
    {
        directionBox.getChildren().remove(form);
    }

    private void removeLink(Screen screen, Link link)
    {
        screen.removeLink(link);
        pane.getChildren().remove(links.get(link));
    }

    public void addItemForm(Screen screen, Item item)
    {
        itemsBox.getChildren().add(new ItemForm(this, screen, item));
    }

    public void removeItem(Screen screen, Item item)
    {
        screen.removeItem(item.getName());
    }

    public void removeItemForm(ItemForm form)
    {
        itemsBox.getChildren().remove(form);
    }

    public void addActionForm(Screen screen, Action action)
    {
        actionsBox.getChildren().add(new ActionForm(this, screen, action));
    }

    public void removeAction(Screen screen, Action action)
    {
        screen.removeAction(action);
    }

    public void removeActionForm(ActionForm form)
    {
        actionsBox.getChildren().remove(form);
    }

    private void addLink(Screen screen, Link link)
    {
        ScreenRect rect = rects.get(screen);
        LinkLine linkLine = new LinkLine(this, rect, link);
        links.put(link, linkLine);
        pane.getChildren().add(linkLine);
        linkLine.toBack();
    }

    @FXML
    private void save()
    {
        try
        {
            game.write(game.getFile());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void saveAs()
    {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Flows","*.flo","*.fli"));
        chooser.setInitialFileName(game.getFile().toString());
        File file = chooser.showSaveDialog(cursorRect.getScene().getWindow());
        if (file != null)
        {
            game.setFile(file);
            save();
        }
    }

    @FXML
    private void propertiesPressed()
    {
        try
        {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/properties.fxml"));
            Node node=loader.load();
            PropertiesDialogController c=loader.getController();
            Stage stage=new Stage(StageStyle.UTILITY);
            stage.setTitle("Game properties");
            Scene scene=new Scene((Parent) node);
            stage.setScene(scene);
            c.setStage(stage);
            c.setGame(game);
            stage.showAndWait();
            if (c.ok())
            {
                game.setGameName(c.getGameName());
                game.setStartScreen(c.getStartScreen());
                game.setGameOverview(c.getGameDescription());
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
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
        ScreenRect selected = getSelected();
        if (selected==null) return;
        try
        {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/screen_select.fxml"));
            Node node = loader.load();
            ScreenSelectDialogController c = loader.getController();
            Stage stage = new Stage(StageStyle.UTILITY);
            stage.setTitle("Select screen");
            Scene scene = new Scene((Parent) node);
            stage.setScene(scene);
            c.setStage(stage);
            c.setGame(game);
            stage.showAndWait();
            if (c.ok())
            {
                Link link = game.link(selected.getScreen().getTitle(), c.getSelectedScreen(), c.getDirection(), "");
                addLink(selected.getScreen(), link);
                addDirectionForm(selected.getScreen(),link);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void newItem()
    {
        try
        {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/screen_name.fxml"));
            Node node=loader.load();
            ScreenNameDialogController c = loader.getController();
            Stage stage = new Stage(StageStyle.UTILITY);
            stage.setTitle("New item name");
            Scene scene = new Scene((Parent) node);
            stage.setScene(scene);
            c.setStage(stage);
            stage.showAndWait();
            if (c.ok())
            {
                Screen screen = getSelected().getScreen();
                Item item = screen.addItem(c.getName(),"","");
                addItemForm(screen,item);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void newAction()
    {
        Action action = getSelected().getScreen().addAction("","");
        addActionForm(getSelected().getScreen(), action);
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

    private ScreenRect getSelected()
    {
        for (Node node : pane.getChildren())
        {
            if (node instanceof ScreenRect)
                if ( ((ScreenRect)node).isSelected() )
                {
                    return (ScreenRect)node;
                }
        }
        return null;
    }

    class ScreenRect extends StackPane
    {
        private Screen screen;
        private BooleanProperty selected = new SimpleBooleanProperty();
        private StringProperty titleProperty, descriptionProperty, imageProperty;

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
            descriptionProperty.addListener( (obs,ov,nv) -> screen.setDescription(nv));
            imageProperty = new SimpleStringProperty(screen.getImageName());
            imageProperty.addListener( (obs,ov,nv) -> screen.setImageName(nv));

            Label label = new Label();
            label.textProperty().bind(titleProperty);
            label.setFont(Font.font("Arial", 8));
            label.setAlignment(Pos.CENTER);

            getChildren().add(label);
            setTranslateX(cursorRect.getTranslateX());
            setTranslateY(cursorRect.getTranslateY());
            setLayoutX(screen.getX());
            setLayoutY(screen.getY());
            layoutXProperty().addListener( (obs,ov,nv) -> screen.setX(nv.doubleValue()) );
            layoutYProperty().addListener( (obs,ov,nv) -> screen.setY(nv.doubleValue()) );

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

        public StringProperty imageProperty() { return imageProperty; }

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
        public DirectionForm(GameMakerController controller, Screen screen, Link link)
        {
            getChildren().add(makeTextField("To",link.getScreen()));
            getChildren().add(makeTextField("Direction",link.getDirection(),(obs,ov,nv) -> link.setDirection(nv)));
            getChildren().add(makeTextField("Description",link.getDescription(),(obs,ov,nv) -> link.setDescription(nv)));

            CheckBox cb = new CheckBox("Can pass");
            cb.setSelected(link.canPass());
            cb.selectedProperty().addListener( (obs,ov,nv) -> link.setCanPass(nv) );
            cb.setMinWidth(100);
            TextField tb = new TextField(link.getCantPassMessage());
            tb.disableProperty().bind(cb.selectedProperty());
            tb.textProperty().addListener( (obs,ov,nv) -> link.setCantPassMessage(nv) );
            HBox.setHgrow(tb, Priority.ALWAYS);
            getChildren().add(new HBox(cb, tb));

            Button takeButton = new Button("-");
            takeButton.setFont(new Font(8));
            takeButton.setOnAction( evt ->
            {
                controller.removeLink(screen, link);
                controller.removeDirectionForm(this);
            });
            getChildren().add(takeButton);

            setPadding(new Insets(5));
            setSpacing(3);
            setStyle("-fx-border-color: black;");
        }
    }

    static class ItemForm extends VBox
    {
        public ItemForm(GameMakerController controller, Screen screen, Item item)
        {
            getChildren().add(makeTextField("Name",item.getName()));
            getChildren().add(makeTextField("Description",item.getDescription(),(obs,ov,nv) -> item.setDescription(nv)));
            getChildren().add(makeTextField("In situ",item.getInsitu(),(obs,ov,nv) -> item.setInsitu(nv)));
            Button takeButton = new Button("-");
            takeButton.setFont(new Font(8));
            takeButton.setOnAction( evt ->
            {
                controller.removeItem(screen, item);
                controller.removeItemForm(this);
            });
            getChildren().add(takeButton);

            setPadding(new Insets(5));
            setSpacing(3);
            setStyle("-fx-border-color: black;");
        }
    }

    static class ActionForm extends VBox
    {
        public ActionForm(GameMakerController controller, Screen screen, Action action)
        {
            getChildren().add(makeTextField("Regex",action.getRegex(),(obs,ov,nv) -> action.setRegex(nv)));
            getChildren().add(makeTextArea("Script",action.getScript(),(obs,ov,nv) -> action.setScript(nv)));
            Button takeButton = new Button("-");
            takeButton.setFont(new Font(8));
            takeButton.setOnAction( evt ->
            {
                controller.removeAction(screen, action);
                controller.removeActionForm(this);
            });
            getChildren().add(takeButton);

            setPadding(new Insets(5));
            setSpacing(3);
            setStyle("-fx-border-color: black;");
        }
    }

    static HBox makeTextField(String name, String text)
    {
        return makeTextField(name,text,null);
    }

    static HBox makeTextField(String name, String text, ChangeListener<? super String> listener)
    {
        Label label = new Label(name);
        label.setMinWidth(100);
        TextField field = new TextField(text);
        if (listener!=null)
            field.textProperty().addListener(listener);
        else
            field.setEditable(false);
        HBox.setHgrow(field, Priority.ALWAYS);
        return new HBox(label, field);
    }

    static HBox makeTextArea(String name, String text, ChangeListener<? super String> listener)
    {
        Label label = new Label(name);
        label.setMinWidth(100);
        TextArea field = new TextArea(text);
        field.setWrapText(true);
        if (listener!=null)
            field.textProperty().addListener(listener);
        else
            field.setEditable(false);
        HBox.setHgrow(field, Priority.ALWAYS);
        return new HBox(label, field);
    }

    class LinkLine extends CubicCurve
    {
        private GameMakerController controller;
        private ScreenRect from;
        private Link link;

        public LinkLine(GameMakerController controller, ScreenRect from, Link link)
        {
            this.controller=controller;
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
            Screen to = controller.getGame().getScreen(link.getScreen());
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

