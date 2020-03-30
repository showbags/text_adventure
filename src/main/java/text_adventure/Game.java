package text_adventure;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Game
{
    private Map<String,Screen> screens = new HashMap<>();
    private Screen currentScreen;
    private Map<String, Item> inventory=new HashMap<>();

    private Game()
    {
    }

    public Game(File json) throws IOException, ParseException
    {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();

        FileReader reader = new FileReader(json);
        //Read JSON file
        Object obj = jsonParser.parse(reader);

        JSONArray jscreens = (JSONArray) obj;


        //Iterate over employee array
        jscreens.forEach( emp -> {
            addScreen( parseScreenObject( (JSONObject) emp ));
        } );

    }

    private Screen parseScreenObject(JSONObject jscreen)
    {
        //Get employee first name
        String title = (String)jscreen.get("title");
        String description = (String)jscreen.get("description");
        Screen screen = new Screen(this,title,description);
        JSONArray items = (JSONArray)jscreen.get("items");
        screen.display();
        //TODO: parse items
        //TODO: parse links
        return screen;
    }

    public void write() throws IOException
    {
        //First Employee
        JSONArray jscreens = new JSONArray();
        for ( Screen screen : screens.values() )
        {
            JSONObject screenDetails=new JSONObject();
            screenDetails.put("title", screen.getTitle());
            screenDetails.put("description", screen.getDescription());
            JSONArray jlinks = new JSONArray();
            for ( ScreenLink link : screen.getLinks().values() )
                jlinks.add(link.json());
            screenDetails.put("links",jlinks);
            JSONArray jitems = new JSONArray();
            for ( Item item : screen.getItems().values() )
                jlinks.add(item.json());
            screenDetails.put("items",jitems);
            screenDetails.toJSONString();
            jscreens.add(screenDetails);
        }

        //Write JSON file
        FileWriter file = new FileWriter("game.json");
        file.write(jscreens.toJSONString());
        file.flush();
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

    private void addScreen(Screen screen) { screens.put(screen.getTitle(),screen); }

    private void setCurrentScreen(Screen screen)
    {
        this.currentScreen=screen;
    }

    public Screen getScreen(String name) { return screens.get(name); }

    private void handleInput()
    {
        System.out.print("\n > ");
        Scanner in = new Scanner(System.in);
        String input = in. nextLine();
        //System.out.println("console: "+System.console());
        //String input=System.console().readLine().toLowerCase();
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
        String dir=goMatcher.group(2);
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

    private Pattern goPattern=Pattern.compile("(go )*(north|south|east|west|[nsew])");
    private Pattern getPattern=Pattern.compile("(get|pick up|take) (.+)");
    private Pattern lookPattern=Pattern.compile("(look at|look|examine) (.+)");

    private void handleGet(Matcher getMatcher)
    {
        String item=getMatcher.group(2);
        if (currentScreen.hasItem(item))
        {
            inventory.put(item, currentScreen.removeItem(item));
            error("You get the "+item);
        } else
            error("There is no "+item+" to get");
    }

    private void handleLook(Matcher lookMatcher)
    {
        String item=lookMatcher.group(2);
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

    public void link(String from, String to, String dir)
    {
        link(from,to,dir,"");
    }

    public void link(String from, String to, String dir, String desc)
    {
        link(from,to,dir,desc,true,"");
    }

    public void link(String from, String to, String dir, String desc, boolean can_pass, String cant_pass_message)
    {
        screens.get(from).link(to,dir,desc,can_pass,cant_pass_message);
    }


    public static void main(String[] args) throws Exception
    {
        Game game=new Game();

        Screen s1=new Screen(game, "Home", "You are at your home. There is a circular dining table in the center of the room.");
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
        Screen s2=new Screen(game, "Backyard", "You are out the back of your house. On the grass you can see some garden tools.");
        s2.addItem("spade", "Amongst the garden tools you can see a <>", "It is a rusty old spade perfect for picking up dog poo.");
        game.addScreen(s2);
        Screen s3=new Screen(game, "Living room", "You are in the living room of your house.");
        game.addScreen(s3);
        game.link("Home","Backyard","west","The back door leading to the <> is open to the outside");
        game.link("Backyard","Home","east","The back door of the house is open to the <>");
        game.link("Home","Living room","south", "There is a door to the <>", false, "The door is locked");
        game.link("Living room","Home","north", "There is a door to the <>");
        game.setCurrentScreen(s1);
        game.write();
        game.startGame();
    }
}

class Screen
{
    private Game game;
    private String title, description;
    private Map<String, ScreenLink> links=new HashMap<>();
    private Map<String, Item> items=new HashMap<>();
    private Map<String, String> actions=new HashMap<>();

    Screen(Game game, String title, String description)
    {
        this.game =game;
        this.title=title;
        this.description=description;
    }

    public void link(String screen, String dir, String desc, boolean can_pass, String cant_pass_message)
    {
        addLink(new ScreenLink(screen, dir, desc, can_pass, cant_pass_message));
    }

    void addLink(ScreenLink link)
    {
        links.put(link.getDirection(), link);
    }

    public String getTitle() { return this.title; }

    public String getDescription() { return this.description; }

    public Map<String,ScreenLink> getLinks() { return this.links; }

    public Map<String, Item> getItems() { return this.items; }

    public Map<String, String> getActions() { return this.actions; }

    void addItem(String name, String insitu, String description)
    {
        items.put(name, new Item(name, insitu, description));
    }

    boolean hasItem(String name)
    {
        return items.containsKey(name);
    }

    Item getItem(String name)
    {
        return items.get(name);
    }

    Item removeItem(String name)
    {
        return items.remove(name);
    }

    ScreenLink getLink(String dir)
    {
        return links.get(dir);
    }

    void addAction(String regex, String action)
    {
        actions.put(regex, action);
    }

    void display()
    {
        System.out.println("\033[0;1m"+title+"\033[0m\n");
        System.out.print(description+" ");
        for (Item item : items.values()) System.out.print(item.describeInSitu()+". ");
        for (ScreenLink link : links.values())
            System.out.print(link.describe()+". ");
        System.out.println();
    }

    void handleInput(String input)
    {
        for (Map.Entry<String, String> entry : actions.entrySet())
        {
            String regex=entry.getKey();
            if (input.matches(regex))
            {
                String groovy=entry.getValue();
                Binding binding=new Binding();
                binding.setVariable("ge", game);
                binding.setVariable("screen", this);
                GroovyShell shell=new GroovyShell(binding);
                shell.evaluate(groovy);
            }
        }
    }

    boolean isGameComplete()
    {
        return false;
    }
}

class ScreenLink
{
    private String screen, direction, description, cant_pass_message;
    private boolean can_pass;

    public ScreenLink(String screen, String direction, String description)
    {
        this(screen,direction,description,true,null);
    }

    public ScreenLink(String screen, String direction, String description, boolean can_pass, String cant_pass_message)
    {
        this.screen=screen;
        this.direction=direction;
        this.description=description;
        this.can_pass=can_pass;
        this.cant_pass_message=cant_pass_message;
    }

    public JSONObject json()
    {
        JSONObject json=new JSONObject();
        json.put("screen", screen);
        json.put("direction", direction);
        json.put("description", description);
        json.put("cant_pass_message", cant_pass_message);
        json.put("can_pass",String.valueOf(can_pass));
        return json;
    }

    public String getScreen() { return this.screen; }

    public boolean canPass() { return can_pass; }

    public void setCanPass(boolean can_pass) { this.can_pass=can_pass; }

    public String cantPassMessage() { return cant_pass_message; }

    public String getDirection() { return this.direction; }

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

    public JSONObject json()
    {
        JSONObject json=new JSONObject();
        json.put("name", name);
        json.put("description", description);
        json.put("insitu", insitu);
        return json;
    }

    public String describeInSitu(){
        return insitu.replaceAll("<>","\033[0;1m"+name+"\033[0m");
    }

    public String toString(){
        return this.name;
    }

    public String getDescription(){ return this.description; }
}