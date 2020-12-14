package text_adventure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game
{
    //TODO: ascii art for images
    //TODO: add optional tag to identify screens (default to screen name)
    //TODO: game saving and loading

    //game details
    private File file;
    private String startScreen, gameName, gameOverview;
    private List<Screen> screens = new ArrayList<>();

    //game state
    private List<Item> inventory=new ArrayList<>();

    private transient Screen currentScreen;
    private transient Map<String,Screen> screenMap;
    private transient Map<String,Item> inventoryMap;

    private static Set<String> words = new HashSet<>();

    private Game()
    {
        try
        {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream("words.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ( (line=reader.readLine())!=null ) {
                words.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Game load(File jsonFile) throws IOException
    {
        Gson gson = new Gson();
        FileReader reader = new FileReader(jsonFile);
        Game game = gson.fromJson(reader, Game.class);
        game.setFile(jsonFile);
        game.setCurrentScreenToStartScreen();
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
        overviewAndHelp();
        do
        {
            clear();
            currentScreen.display();
            handleInput();
        } while (!currentScreen.isGameComplete());
    }

    public void addScreen(Screen screen)
    {
        screens.add(screen);
        invalidateScreens();
    }

    public Map<String,Screen> getScreenMap()
    {
        if (screenMap==null)
        {
            screenMap=new HashMap<>();
            for (Screen screen : screens)
                screenMap.put(screen.getTitle(), screen);
        }
        return screenMap;
    }

    public Screen getScreen(String title)
    {
        return getScreenMap().get(title);
    }

    private Map<String,Item> getInventoryMap()
    {
        if (inventoryMap==null)
        {
            inventoryMap=new HashMap<>();
            for (Item item : inventory)
                inventoryMap.put(item.getName(), item);
        }
        return inventoryMap;
    }

    public Item getInventoryItem(String item)
    {
        return getInventoryMap().get(item);
    }

    public void addInventoryItem(Item item)
    {
        invalidateInventory();
        inventory.add(item);
    }

    public boolean hasInventoryItem(String item)
    {
        return getInventoryMap().containsKey(item);
    }

    private void invalidateScreens() { screenMap=null; }
    private void invalidateInventory() { inventoryMap=null; }

    public void removeScreen(Screen screen)
    {
        screens.remove(screen);
        invalidateScreens();
    }

    private void setCurrentScreenToStartScreen()
    {
        setCurrentScreen(getScreen(startScreen));
    }

    private void setCurrentScreen(Screen screen)
    {
        this.currentScreen=screen;
    }

    public void goTo(String screen) { setCurrentScreen(getScreen(screen)); }

    public List<Screen> getScreens() { return this.screens; }

    private void handleInput()
    {
        System.out.print("\n > ");
        Scanner in = new Scanner(System.in);
        String input = in. nextLine();
        System.out.println();
        if (input.isBlank()) return;
        if (currentScreen.handleInput(this,input))
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
        if (input.equals("save"))
        {
            try
            {
                startScreen=currentScreen.getTitle();
                File file = new File("save.json");
                write(file);
                error("Game saved as: "+file);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            return;
        }
        if (input.equals("help"))
        {
            overviewAndHelp();
            return;
        }
        if (spellCheck(input))
        {
            return;
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
        else if (dir.matches("d(own)*"))
            dir="down";
        else if (dir.matches("u(p)*"))
            dir="up";

        Link link=currentScreen.getLink(dir);
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

    private static Pattern goPattern=Pattern.compile("(?:go )*(north|south|east|west|up|down|[nsewud])");
    private static Pattern getPattern=Pattern.compile("(?:get|pick up|take) (.+)");
    private static Pattern lookPattern=Pattern.compile("(?:look at|look|examine|read) (.+)");

    private void handleGet(Matcher getMatcher)
    {
        String item=getMatcher.group(1);
        if (currentScreen.hasItem(item))
        {
            addInventoryItem(currentScreen.removeItem(item));
            error("You get the "+item);
        } else
            error("There is no "+item+" to get");
    }

    private void handleLook(Matcher lookMatcher)
    {
        String name=lookMatcher.group(1);
        Item item = getInventoryItem(name);
        if (item==null) item = currentScreen.getItem(name);
        if (item!=null)
            error("\n"+currentScreen.getItem(name).getDescription());
        else
            error("There is no "+name+".");
    }

    private void inventory()
    {
        System.out.println("\nYou have the following items:\n");
        for (Item item : inventory)
            System.out.println(" * "+item);
        hold();
    }

    private void overviewAndHelp()
    {
        clear();
        gameOverview();
        System.out.println();
        generalHelp();
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

    private boolean spellCheck(String input)
    {
        for (String word : input.split("\\s+"))
        {
            if (!words.contains(word))
            {
                error("What's a \""+word+"\"?");
                return true;
            }
        }
        return false;
    }

    private void error(String error)
    {
        System.out.print(error);
        hold();
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

    public Link link(String from, String to, String dir, String desc)
    {
        return link(from,to,dir,desc,true,"");
    }

    public Link link(String from, String to, String dir, String desc, boolean can_pass, String cant_pass_message)
    {
        return getScreen(from).link(to,dir,desc,can_pass,cant_pass_message);
    }

    public static Game defaultGame()
    {
        Game game = new Game();
        game.setFile(new File("game.json"));
        Screen s1 = new Screen("Kitchen", "You are at kitchen of your home. There is a circular dining table in the center of the room.");
        s1.setLocation(200, 200);
        s1.addItem("key", "On the table is a <>", "It's just a key");
        s1.addAction("unlock door( with)*( the)*( key)*",
                """
                        if (!game.hasInventoryItem("key"))
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
                        if (!game.hasInventoryItem("bread"))
                        game.error("You need bread to make a sandwich");
                        else if (!game.hasInventoryItem("vegemite"))
                        game.error("Some vegemite would be nice");
                        else if (!game.hasInventoryItem("knife"))
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
        Screen s2 = new Screen("Backyard", "You are out the back of your house. On the grass you can see some garden tools.");
        s2.setLocation(100, 200);
        s2.addItem("knife", "Amongst the garden tools you can see a <>", "It looks like a pretty good sandwich making knife.");
        game.addScreen(s2);
        Screen s3 = new Screen("Laundry", "You are in the laundry of your house.");
        s3.setLocation(200, 300);
        game.addScreen(s3);
        Screen s4 = new Screen("Pool area", "You are in the pool area. There is a lovely clear pool that looks nice for swimming in.");
        s4.setLocation(200, 400);
        s4.addItem("note", "On the ground there is a <>", "The note says \"Go to the kitchen and make a sandwich to win the game\"");
        game.addScreen(s4);
        Screen s5 = new Screen("Pantry", "You are in the pantry.");
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

    public void setGameOverview(String gameOverview) { this.gameOverview=gameOverview; }

    public String getGameOverview() { return this.gameOverview; }

    public void setGameName(String gameName) { this.gameName=gameName; }

    public String getGameName() { return this.gameName; }

    public String getStartScreen() { return startScreen; }

    public void setStartScreen(String startScreen) { this.startScreen=startScreen; }
}

class Screen
{
    private String title, description;
    private List<Link> links=new ArrayList<>();
    private List<Item> items=new ArrayList<>();
    private List<Action> actions=new ArrayList<>();
    private double x,y;

    private transient Map<String,Item> itemMap;
    private transient Map<String, Link> linkMap;

    public Screen(String title)
    {
        this(title,"");
    }

    public Screen(String title, String description)
    {
        this.title=title;
        this.description=description;
    }

    public double getX() { return this.x; }
    public double getY() { return this.y; }

    public void setX(double x) { this.x=x; }
    public void setY(double y) { this.y=y; }

    public void setLocation(double x, double y)
    {
        setX(x);
        setY(y);
    }

    public Link link(String screen, String dir, String desc, boolean can_pass, String cant_pass_message)
    {
        return addLink(new Link(screen, dir, desc, can_pass, cant_pass_message));
    }

    public Link addLink(Link link)
    {
        invalidateLinks();
        links.add(link);
        return link;
    }
    
    public void removeLink(Link link)
    {
        invalidateLinks();
        links.remove(link);
    }

    public String getTitle() { return this.title; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description=description; }

    public Map<String,Item> getItemMap()
    {
        if (itemMap==null)
        {
            itemMap = new HashMap<>();
            for (Item item : items)
                itemMap.put(item.getName(),item);
        }
        return itemMap;
    }

    private void invalidateItems() { itemMap=null; }

    public Map<String, Link> getLinkMap()
    {
        if (linkMap==null)
        {
            linkMap= new HashMap<>();
            for (Link link : links)
                linkMap.put(link.getDirection(),link);
        }
        return linkMap;
    }

    private void invalidateLinks() { linkMap=null; }

    public Item addItem(String name, String insitu, String description)
    {
        invalidateItems();
        Item item = new Item(name, insitu, description);
        items.add(item);
        return item;
    }

    public boolean hasItem(String name)
    {
        return getItemMap().containsKey(name);
    }

    public Item getItem(String name)
    {
        return getItemMap().get(name);
    }

    public Item removeItem(String name)
    {
        invalidateItems();
        Item item = getItem(name);
        items.remove(item);
        return item;
    }

    public void removeAction(Action action)
    {
        actions.remove(action);
    }

    public List<Link> getLinks() { return links; }

    public Link getLink(String dir)
    {
        return getLinkMap().get(dir);
    }

    public List<Item> getItems() { return this.items; }

    public List<Action> getActions() { return this.actions; }

    public Action addAction(String regex, String script)
    {
        Action action = new Action(regex, script);
        actions.add(action);
        return action;
    }

    public void display()
    {
        System.out.println("\n \033[0;1m"+title+"\033[0m\n");
        System.out.print("  "+description+" ");
        for (Item item : items) System.out.print(item.describeInSitu()+". ");
        for (Link link : links)
            System.out.print(link.describe()+". ");
        System.out.println();
    }

    public boolean handleInput(Game game, String input)
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
                return true;
            }
        }
        return false;
    }

    public boolean isGameComplete() { return false; }
}

class Link
{
    private String screen, direction, description, cant_pass_message;
    private boolean can_pass;

    public Link(String screen, String direction, String description, boolean can_pass, String cant_pass_message)
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
        if (description.isBlank())
            return "You can go "+direction;
        else
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