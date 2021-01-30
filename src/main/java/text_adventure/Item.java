package text_adventure;

public class Item
{
    private String name, description, insitu, imageName;

    public Item(String name, String insitu, String description)
    {
        this(name, insitu, description, null);
    }

    public Item(String name, String insitu, String description, String imageName)
    {
        this.name = name;
        this.insitu = insitu;
        this.description = description;
        this.imageName = imageName;
    }

    public String getName() { return this.name; }

    public String getInsitu() { return this.insitu;}

    public void setInsitu(String insitu) { this.insitu = insitu; }

    public void setImageName(String imageName) { this.imageName=imageName; }

    public String getImageName() { return this.imageName; }

    public String describeInSitu()
    {
        return insitu.replaceAll("<>", "\033[0;1m"+name+"\033[0m").replaceAll("\\.$","");
    }

    public void describe()
    {
        Game.displayImage(imageName);
        Game.message(getDescription());
    }

    public String toString()
    {
        return this.name;
    }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }
}
