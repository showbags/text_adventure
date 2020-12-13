package text_adventure;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class Game
{
    //TODO: help screen
    //TODO: spell check unknown commands
    //TODO: ascii art for images
    //TODO: add name of game
    //TODO: add home screen
    //TODO: add optional tag to identify screens (default to screen name)
    //TODO: game saving and save as

    //game details
    public Map<String,Screen> screens = new HashMap<>();

    private File file;
    private String startScreen, gameName, gameOverview;

    //game state
    private transient Screen currentScreen;
    private transient Map<String, Item> inventory=new HashMap<>();

    private Game() { }

    public static Game load(File jsonFile) throws IOException
    {
        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFile);
        Game game = gson.fromJson(reader, Game.class);
        game.setFile(jsonFile);
        for (Screen screen : game.screens.values())
        {
            if (screen.getDescription().contains("home"))
                game.setCurrentScreen(screen);
        }
        for (Screen screen : game.screens.values())
            screen.register(game);
        return game;
    }

    public void setFile(File file) { this.file=file; }

    public File getFile() { return this.file; }

    public void write(File jsonFile) throws IOException
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter(jsonFile);
        writer.write(gson.toJson(this));
        writer.close();
    }

    private void startGame()
    {
        do
        {
            clear();
            currentScreen.display();
            handleInput();
        } while (!currentScreen.isGameComplete());
    }

    public void addScreen(Screen screen) { screens.put(screen.getTitle(),screen); }

    private void setCurrentScreen(Screen screen)
    {
        this.currentScreen=screen;
    }

    public Map<String,Screen> getScreens() { return this.screens; }

    public Screen getScreen(String name) { return screens.get(name); }

    private void handleInput()
    {
        System.out.print("\n > ");
        Scanner in = new Scanner(System.in);
        String input = in. nextLine();
        System.out.println();
        if (input.isBlank()) return;
        if (currentScreen.handleInput(input))
            return;
        Matcher goMatcher=goPattern.matcher(input);
        if (goMatcher.matches())
        {
            setCurrentScreen(handleMove(goMatcher));
            return;
        }
        Matcher getMatcher=getPattern.matcher(input);
        if (getMatcher.matches())
        {
            handleGet(getMatcher);
            return;
        }
        Matcher lookMatcher=lookPattern.matcher(input);
        if (lookMatcher.matches())
        {
            handleLook(lookMatcher);
            return;
        }
        if (input.equals("i"))
        {
            inventory();
            return;
        }
        if (input.equals("q"))
        {
            endGame();
            return;
        }
        if (input.equals("help"))
        {
            gameOverview();
            generalHelp();
        }
        //fall through to here
        System.out.println("I don't understand \""+input+"\"");
        hold();

    }

    public void endGame()
    {
        System.out.println("Bye!");
        System.exit(0);
    }

    private Screen handleMove(Matcher goMatcher)
    {
        String dir=goMatcher.group(1);
        if (dir.equals("n"))
            dir="north";
        else if (dir.equals("s"))
            dir="south";
        else if (dir.matches("e"))
            dir="east";
        else if (dir.matches("w(est)*"))
            dir="west";

        ScreenLink link=currentScreen.getLink(dir);
        if (link==null)
        {
            error("You can't go that way");
            return currentScreen;
        }
        if (!link.canPass())
        {
            error(link.cantPassMessage());
            return currentScreen;
        }

        return getScreen(link.getScreen());
    }

    private static Pattern goPattern=Pattern.compile("(?:go )*(north|south|east|west|[nsew])");
    private static Pattern getPattern=Pattern.compile("(?:get|pick up|take) (.+)");
    private static Pattern lookPattern=Pattern.compile("(?:look at|look|examine|read) (.+)");

    private void handleGet(Matcher getMatcher)
    {
        String item=getMatcher.group(1);
        if (currentScreen.hasItem(item))
        {
            inventory.put(item, currentScreen.removeItem(item));
            error("You get the "+item);
        } else
            error("There is no "+item+" to get");
    }

    private void handleLook(Matcher lookMatcher)
    {
        String item=lookMatcher.group(1);
        if (currentScreen.hasItem(item))
            error("\n"+currentScreen.getItem(item).getDescription());
        else if (inventory.containsKey(item))
            error("\n"+inventory.get(item).getDescription());
        else
            error("There is no "+item+".");
    }

    private void inventory()
    {
        System.out.println("\nYou have the following items:\n");
        for (Item item : inventory.values())
            System.out.println(" * "+item);
        hold();
    }

    private void gameOverview()
    {
        System.out.println(gameOverview);
    }

    private void generalHelp()
    {
        System.out.printf(
                """
    %s  is a text adventure game. It consists of many different locations each of which contains a description of the scenery and objects 
    within it. You can move between locations by observing the directions that are available to you. You will find you way through the
    game by experimenting with different commands. Don't be disappointed if the game doesn't understand everything that you type.
    
    Some examples of commands that might do useful things:
    
     > go north         - move to the location to the north (if one exists)
     
     > get pen          - if there is a pen to pick up
     > pick up pen      - if there is a pen to pick up
     
     > look at pen      - take a closer look at the pen
     
     > tie rope to tree - if there's a rope and a tree ... it's worth a try
     
     > inventory/i        - show me the things that I'm currently carrying
     
     > quit/q             - quit the game
     
     > save               - save your current game
     
     > help               - show this screen
     
     Good luck!!
    """,gameName);
    }

    private void error(String error)
    {
        System.out.print(error);
        hold();
        /*try
        {
            Thread.sleep(1000);
        } catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }*/
    }

    public void hold()
    {
        System.out.print("\n\nPress ENTER");
        System.console().readLine();
    }

    private void clear()
    {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public ScreenLink link(String from, String to, String dir, String desc)
    {
        return link(from,to,dir,desc,true,"");
    }

    public ScreenLink link(String from, String to, String dir, String desc, boolean can_pass, String cant_pass_message)
    {
        return screens.get(from).link(to,dir,desc,can_pass,cant_pass_message);
    }

    public static Game defaultGame()
    {
        Game game = new Game();
        game.setFile(new File("game.json"));
        Screen s1 = new Screen(game, "Kitchen", "You are at kitchen of your home. There is a circular dining table in the center of the room.");
        s1.setLocation(200, 200);
        s1.addItem("key", "On the table is a <>", "It's just a key");
        s1.addAction("unlock door( with)*( the)*( key)*",
                """
                        if (!game.inventory.containsKey("key"))
                        game.error("You don't have a key");
                        else
                        {
                        screen.getLink("south").setCanPass(true);
                        game.error("The door unlocks");
                        }
                        """
        );
        s1.addAction("make( a)* sandwich",
                """
                        if (!game.inventory.containsKey("bread"))
                        game.error("You need bread to make a sandwich");
                        else if (!game.inventory.containsKey("vegemite"))
                        game.error("Some vegemite would be nice");
                        else if (!game.inventory.containsKey("knife"))
                        game.error("How are you going to spread the vegemite with no knife?");
                        else
                        {
                        System.out.println("You make a sandwich and win the game!!!!");
                        System.out.println("");
                        System.out.println(" ------------  CONGRATULATIONS!!!  ------------");
                        System.out.println("");
                        game.endGame();
                        }
                        """
        );
        game.addScreen(s1);
        Screen s2 = new Screen(game, "Backyard", "You are out the back of your house. On the grass you can see some garden tools.");
        s2.setLocation(100, 200);
        s2.addItem("knife", "Amongst the garden tools you can see a <>", "It looks like a pretty good sandwich making knife.");
        game.addScreen(s2);
        Screen s3 = new Screen(game, "Laundry", "You are in the laundry of your house.");
        s3.setLocation(200, 300);
        game.addScreen(s3);
        Screen s4 = new Screen(game, "Pool area", "You are in the pool area. There is a lovely clear pool that looks nice for swimming in.");
        s4.setLocation(200, 400);
        s4.addItem("note", "On the ground there is a <>", "The note says \"Go to the kitchen and make a sandwich to win the game\"");
        game.addScreen(s4);
        Screen s5 = new Screen(game, "Pantry", "You are in the pantry.");
        s5.setLocation(200, 100);
        s5.addItem("vegemite", "There is a <> on the shelves", "Mmmm. That would be nice on some bread");
        s5.addItem("bread", "There is some <> in the bread box", "It is perfect bread for making sandwiches");
        game.addScreen(s5);

        game.link("Kitchen", "Backyard", "west", "The back door leading to the <> is open to the outside");
        game.link("Kitchen", "Laundry", "south", "There is a door to the <>", false, "The door is locked");
        game.link("Kitchen", "Pantry","north", "There is an open pantry door to the <>");
        game.link("Pantry", "Kitchen","south", "You can leave the pantry to the <>");
        game.link("Backyard", "Kitchen", "east", "The back door of the house is open to the <>");
        game.link("Laundry", "Kitchen", "north", "There is a door to the <>");
        game.link("Laundry", "Pool area", "south", "There is a door leading outside to the <>");
        game.link("Pool area", "Laundry", "north", "To the <> is the laundry door");
        game.setCurrentScreen(s1);
        return game;
    }

    public static void main(String[] args) throws Exception
    {
        Game game;
        if (args.length==0)
        {
            game = defaultGame();
            //game.write(new File("game.json"));
        }
        else
        {
            game = Game.load(new File(args[0]));
        }
        game.startGame();
    }
}

class Screen
{
    private transient Game game;
    private String title, description;
    private Map<String, ScreenLink> links=new HashMap<>();
    private Map<String, Item> items=new HashMap<>();
    private List<Action> actions=new ArrayList<>();
    private double x,y;

    public Screen(Game game, String title)
    {
        this(game,title,"");
    }

    public Screen(Game game, String title, String description)
    {
        register(game);
        this.title=title;
        this.description=description;
    }

    public Game getGame() { return this.game; }

    public void register(Game game) { this.game=game; }

    public double getX() { return this.x; }
    public double getY() { return this.y; }

    public void setX(double x) { this.x=x; }
    public void setY(double y) { this.y=y; }

    public void setLocation(double x, double y)
    {
        setX(x);
        setY(y);
    }

    public ScreenLink link(String screen, String dir, String desc, boolean can_pass, String cant_pass_message)
    {
        return addLink(new ScreenLink(screen, dir, desc, can_pass, cant_pass_message));
    }

    public ScreenLink addLink(ScreenLink link)
    {
        links.put(link.getDirection(), link);
        return link;
    }
    
    public void removeLink(ScreenLink link)
    {
        links.remove(game.getScreen(link.getScreen()).getTitle());
    }

    public String getTitle() { return this.title; }

    public String getDescription() { return this.description; }

    public Item addItem(String name, String insitu, String description)
    {
        Item item = new Item(name, insitu, description);
        items.put(name, item);
        return item;
    }

    public boolean hasItem(String name)
    {
        return items.containsKey(name);
    }

    public Item getItem(String name)
    {
        return items.get(name);
    }

    public Item removeItem(String name)
    {
        return items.remove(name);
    }

    public void removeAction(Action action)
    {
        actions.remove(action);
    }

    public Map<String, ScreenLink> getLinks() { return links; }

    public ScreenLink getLink(String dir)
    {
        return links.get(dir);
    }

    public Map<String, Item> getItems() { return this.items; }

    public List<Action> getActions() { return this.actions; }

    public Action addAction(String regex, String script)
    {
        Action action = new Action(regex, script);
        actions.add(action);
        return action;
    }

    public void display()
    {
        System.out.println("\033[0;1m"+title+"\033[0m\n");
        System.out.print(description+" ");
        for (Item item : items.values()) System.out.print(item.describeInSitu()+". ");
        for (ScreenLink link : links.values())
            System.out.print(link.describe()+". ");
        System.out.println();
    }

    public boolean handleInput(String input)
    {
        for (Action action : actions)
        {
            String regex=action.getRegex();
            if (input.matches(regex))
            {
                String groovy=action.getScript();
                Binding binding=new Binding();
                binding.setVariable("game", game);
                binding.setVariable("screen", this);
                GroovyShell shell=new GroovyShell(binding);
                shell.evaluate(groovy);
                game.hold();
                return true;
            }
        }
        return false;

    }

    public boolean isGameComplete()
    {
        return false;
    }
}

class ScreenLink
{
    private String screen, direction, description, cant_pass_message;
    private boolean can_pass;

    public ScreenLink(String screen, String direction, String description, boolean can_pass, String cant_pass_message)
    {
        this.screen=screen;
        this.direction=direction;
        this.description=description;
        this.can_pass=can_pass;
        this.cant_pass_message=cant_pass_message;
    }

    public String getScreen() { return this.screen; }

    public boolean canPass() { return can_pass; }

    @SuppressWarnings("unused")
    public void setCanPass(boolean can_pass) { this.can_pass=can_pass; }

    public String cantPassMessage() { return cant_pass_message; }

    public String getDirection() { return this.direction; }

    public void setDirection(String direction ) { this.direction=direction; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public String describe()
    {
        return description.replaceAll("<>",direction);
    }
}

class Item
{
    private String name, description, insitu;

    public Item(String name, String insitu, String description)
    {
        this.name=name;
        this.insitu=insitu;
        this.description=description;
    }

    public String getName() { return this.name; }

    public String getInsitu() { return this.insitu;}

    public void setInsitu(String insitu) { this.insitu=insitu; }

    public String describeInSitu(){
        return insitu.replaceAll("<>","\033[0;1m"+name+"\033[0m");
    }

    public String toString(){
        return this.name;
    }

    public String getDescription(){ return this.description; }

    public void setDescription(String description) { this.description=description; }
}

class Action
{
    private String regex, script;

    public Action(String regex, String script)
    {
        this.regex=regex;
        this.script=script;
    }

    public String getRegex() { return this.regex; }

    public String getScript() { return this.script; }

    public void setRegex(String regex) { this.regex=regex; }

    public void setScript(String script) { this.script=script; }

}