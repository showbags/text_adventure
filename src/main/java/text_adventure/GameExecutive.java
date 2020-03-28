package text_adventure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.*;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class GameExecutive
{
    public static void main(String[] args) throws Exception
    {
        writeJson();
        GameExecutive ge=new GameExecutive();
        Screen s1=new Screen(ge, "Home", "You are at your home. There is a circular dining table in the center of the room.");
        s1.addItem("key", "On the table is a <>", "It's just a key");
        s1.addAction("unlock door( with)*( the)*( key)*",
                "if (!ge.inventory.containsKey(\"key\"))\n"+
                        "System.out.println(\"You don't have a key\");\n"+
                        "else\n"+
                        "{\n"+
                        "screen.getLink(\"south\").setCanPass(true);\n"+
                        "System.out.println(\"The door unlocks\");\n"+
                        "}\n"+
                        "ge.hold();\n"
        );
        Screen s2=new Screen(ge, "Backyard", "You are out the back of your house. On the grass you can see some garden tools.");
        s2.addItem("spade", "Amongst the garden tools you can see a <>", "It is a rusty old spade perfect for picking up dog poo.");
        Screen s3=new Screen(ge, "Living room", "You are in the living room of your house.");
        new ScreenLink(s1, s2, "west", "east", "The back door leading to the <> is open to the outside", "The back door of the house is open to the <>");
        new ScreenLink(s1, s3, "south", "north", "There is a door to the <>", "There is a door to the <>", false, "The door is locked");
        ge.setCurrentScreen(s1);
        ge.startGame();
    }

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

    public static void parse(File file)
    {

    }

    private Screen currentScreen;
    private Map<String, Item> inventory=new HashMap<>();

    private GameExecutive()
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

    private void setCurrentScreen(Screen screen)
    {
        this.currentScreen=screen;
    }

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

        return link.getScreen(currentScreen);
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
}

class Screen
{
    private GameExecutive ge;
    private String title, description;
    private Map<String, ScreenLink> links=new HashMap<>();
    private Map<String, Item> items=new HashMap<>();
    private Map<String, String> actions=new HashMap<>();

    Screen(GameExecutive ge, String title, String description)
    {
        this.ge=ge;
        this.title=title;
        this.description=description;
    }

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

    void addLink(ScreenLink link)
    {
        links.put(link.getDirection(this), link);
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
            System.out.print(link.describe(this)+". ");
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
                binding.setVariable("ge", ge);
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
    private Screen screen1, screen2;
    private String direction1, direction2;
    private String description1, description2;
    private boolean can_pass;
    private String cant_pass_message;

    ScreenLink(Screen screen1, Screen screen2, String direction1, String direction2, String description1, String description2)
    {
        this(screen1,screen2,direction1,direction2,description1,description2,true,null);
    }

    ScreenLink(Screen screen1, Screen screen2, String direction1, String direction2, String description1, String description2, boolean can_pass, String cant_pass_message)
    {
        this.screen1=screen1;
        this.screen2=screen2;
        this.direction1=direction1;
        this.direction2=direction2;
        this.description1=description1;
        this.description2=description2;
        this.can_pass=can_pass;
        this.cant_pass_message=cant_pass_message;
        screen1.addLink(this);
        screen2.addLink(this);
    }

    boolean canPass() { return can_pass; }

    void setCanPass(boolean can_pass) { this.can_pass=can_pass; }

    String cantPassMessage() { return cant_pass_message; }

    String getDirection(Screen currentScreen) { return currentScreen==screen1 ? direction1 : direction2; }

    Screen getScreen(Screen currentScreen) { return currentScreen==screen1 ? screen2 : screen1; }

    String describe(Screen currentScreen)
    { return currentScreen==screen1 ? description1.replaceAll("<>",direction1) : description2.replaceAll("<>",direction2); }
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