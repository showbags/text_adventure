package text_adventure;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Game
{



    private static void writeJson() throws IOException
    {
        //First Employee
        JSONObject employeeDetails = new JSONObject();
        employeeDetails.put("firstName", "Lokesh");
        employeeDetails.put("lastName", "Gupta");
        employeeDetails.put("website", "howtodoinjava.com");

        JSONObject employeeObject = new JSONObject();
        employeeObject.put("employee", employeeDetails);

        //Second Employee
        JSONObject employeeDetails2 = new JSONObject();
        employeeDetails2.put("firstName", "Brian");
        employeeDetails2.put("lastName", "Schultz");
        employeeDetails2.put("website", "example.com");

        JSONObject employeeObject2 = new JSONObject();
        employeeObject2.put("employee", employeeDetails2);

        //Add employees to list
        JSONArray employeeList = new JSONArray();
        employeeList.add(employeeObject);
        employeeList.add(employeeObject2);

        //Write JSON file
        try (FileWriter file = new FileWriter("employees.json")) {

            file.write(employeeList.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String,Screen> screens = new HashMap<>();
    private Screen currentScreen;
    private Map<String, Item> inventory=new HashMap<>();

    private Game()
    {
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
        writeJson();
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

    Item(String name, String insitu, String description)
    {
        this.name=name;
        this.insitu=insitu;
        this.description=description;
    }

    String describeInSitu(){
        return insitu.replaceAll("<>","\033[0;1m"+name+"\033[0m");
    }

    public String toString(){
        return this.name;
    }

    String getDescription(){ return this.description; }
}