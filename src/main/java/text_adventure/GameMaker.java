package text_adventure;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;


public class GameMaker extends Application {

    public static void main(String[] args)
    {
        // Here you can work with args - command line parameters
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/game_maker.fxml"));
        Parent root = loader.load();

        Game game = Game.load(new File("marooned.json"));
        //Game game = Game.load(new File("tmp.json"));
        //Game game = Game.defaultGame();
        GameMakerController controller = loader.getController();
        controller.setGame(game);

        stage.setTitle("GameMaker");
        stage.setScene(new Scene(root));
        stage.show();
    }
}

