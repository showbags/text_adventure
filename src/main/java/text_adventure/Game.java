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
    //game details
    public Map<String,Screen> screens = new HashMap<>();

    private File file;

    //game state
    private transient Screen currentScreen;
    private transient Map<String, Item> inventory=new HashMap<>();

    private Game()
    {
    }

    public static Game load(File jsonFile) throws IOException
    {
        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFile);
        Game game = gson.fromJson(reader, Game.class);
        game.setFile(jsonFile);
        game.setCurrentScreen(game.getScreen("Home"));
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
        Matcher goMatcher=goPattern.matcher(input);
        if (goMatcher.matches())
        {
            Screen screen=handleMove(goMatcher);
            if (screen!=null)
            {
                setCurrentScreen(screen);
                return;
            }
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
        currentScreen.handleInput(input);
        if (input.equals("i"))
        {
            inventory();
            return;
        }
        if (input.equals("q"))
        {
            System.out.println("Bye!");
            System.exit(0);
        }
        System.out.println("Unknown command "+input);
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
            return null;
        }
        if (!link.canPass())
        {
            System.out.println(link.cantPassMessage());
            hold();
            return null;
        }

        return getScreen(link.getScreen());
    }

    private Pattern goPattern=Pattern.compile("(?:go )*(north|south|east|west|[nsew])");
    private Pattern getPattern=Pattern.compile("(?:get|pick up|take) (.+)");
    private Pattern lookPattern=Pattern.compile("(?:look at|look|examine) (.+)");

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
        {
            System.out.println("\n"+currentScreen.getItem(item).getDescription());
            hold();
            return;
        }
        if (inventory.containsKey(item))
        {
            System.out.println("\n"+inventory.get(item).getDescription());
            hold();
        } else
            error("There is no "+item+".");
    }

    private void inventory()
    {
        System.out.println("\nYou have the following items:\n");
        for (Item item : inventory.values())
            System.out.println(" * "+item);
        hold();
    }

    private void error(String error)
    {
        System.out.print(error);
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
    }

    private void hold()
    {
        System.out.print("\nPress ENTER");
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


    public static void main(String[] args) throws Exception
    {
        Game game=new Game();
        if (false)
        {
            Screen s1 = new Screen(game, "Home", "You are at your home. There is a circular dining table in the center of the room.");
            s1.setLocation(200, 200);
            s1.addItem("key", "On the table is a <>", "It's just a key");
            s1.addAction("unlock door( with)*( the)*( key)*",
                    "if (!game.inventory.containsKey(\"key\"))\n"+
                            "System.out.println(\"You don't have a key\");\n"+
                            "else\n"+
                            "{\n"+
                            "screen.getLink(\"south\").setCanPass(true);\n"+
                            "System.out.println(\"The door unlocks\");\n"+
                            "}\n"+
                            "game.hold();\n"
            );
            game.addScreen(s1);
            Screen s2 = new Screen(game, "Backyard", "You are out the back of your house. On the grass you can see some garden tools.");
            s2.setLocation(100, 200);
            s2.addItem("spade", "Amongst the garden tools you can see a <>", "It is a rusty old spade perfect for picking up dog poo.");
            game.addScreen(s2);
            Screen s3 = new Screen(game, "Living room", "You are in the living room of your house.");
            s3.setLocation(200, 300);
            game.addScreen(s3);
            game.link("Home", "Backyard", "west", "The back door leading to the <> is open to the outside");
            game.link("Backyard", "Home", "east", "The back door of the house is open to the <>");
            game.link("Home", "Living room", "south", "There is a door to the <>", false, "The door is locked");
            game.link("Living room", "Home", "north", "There is a door to the <>");
            game.setCurrentScreen(s1);
            game.write(new File("game.json"));
        }
        else
        {
            game = Game.load(new File("game3.json"));
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
    private Map<String, String> actions=new HashMap<>();
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

    public String getTitle() { return this.title; }

    public String getDescription() { return this.description; }

    public void addItem(String name, String insitu, String description)
    {
        items.put(name, new Item(name, insitu, description));
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

    public Map<String, ScreenLink> getLinks() { return links; }

    public ScreenLink getLink(String dir)
    {
        return links.get(dir);
    }

    public Map<String, Item> getItems() { return this.items; }

    public void addAction(String regex, String action)
    {
        actions.put(regex, action);
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

    public void handleInput(String input)
    {
        for (Map.Entry<String, String> entry : actions.entrySet())
        {
            String regex=entry.getKey();
            if (input.matches(regex))
            {
                String groovy=entry.getValue();
                Binding binding=new Binding();
                binding.setVariable("game", game);
                binding.setVariable("screen", this);
                GroovyShell shell=new GroovyShell(binding);
                shell.evaluate(groovy);
            }
        }
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

    public String describeInSitu(){
        return insitu.replaceAll("<>","\033[0;1m"+name+"\033[0m");
    }

    public String toString(){
        return this.name;
    }

    public String getDescription(){ return this.description; }
}