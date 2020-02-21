package net.samongi.Inscription.Glyphs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeManager;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Glyph implements Serializable {

    // Serialization UID
    private static final long serialVersionUID = -8133713348644333985L;

    // (This max level should be configurable)
    public static final int MAX_LEVEL = 100;

    /**
     * Will parse an itemstack and return a glyph object
     * Will return null if the itemstack does not represent a glyph
     *
     * @param item Itemstack to parse
     * @return Glyph object, null if itemstack is not a glyph item
     */
    public static Glyph getGlyph(ItemStack item) {
        // If the passed object is null, it can't be a glyph
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();

        // If the item doesn't have meta data, it can't be a glyph
        if (itemMeta == null) {
            return null;
        }

        // If the item does not have a display name, it can't be a glyph
        if (!itemMeta.hasDisplayName())
            return null;
        if (!itemMeta.hasLore())
            return null; // If the item does not have lore, it
        // can't be a glyph
        List<String> lore = itemMeta.getLore(); // Getting the lore
        if (lore.size() < 2)
            return null; // Lore needs more than just one line

        // Parsing the type line
        String type_line = ChatColor.stripColor(lore.get(0)).toLowerCase().replace("glyph of ", "").replace("lv. ", "");
        /* Splitting the lore into tokens */
        String[] split_type_line = type_line.split(" ");
        // Example string: Lv. 20 Rare /Glyph of/ Time
        String level_string = split_type_line[0];
        String rarity_string = split_type_line[1];
        String element_string = split_type_line[2];

        // Parsing the level
        int level = -1;
        try {
            level = Integer.parseInt(level_string);
        }
        catch (NumberFormatException e) {
            return null;
        }
        if (level < 0)
            return null;

        // Parsing the rarity
        GlyphRarity rarity = GlyphRarity.valueOf(rarity_string.toUpperCase());
        if (rarity == null)
            return null; // No rarity no glyph

        // Parsing the element
        GlyphElement element = GlyphElement.valueOf(element_string.toUpperCase());
        if (element == null)
            return null; // No element no glyph

        // Parsing all the Attributes (lore items 1 to n)
        List<Attribute> attributes = new ArrayList<>();
        for (int i = 1; i < lore.size(); i++) {
            AttributeManager a_manager = Inscription.getInstance().getAttributeManager();
            Attribute attribute = a_manager.parseLoreLine(lore.get(i));
            if (attribute != null)
                attributes.add(attribute);
        }

        Map<String, Integer> experience_map = new HashMap<>();
        /* Parsing the experience the glyph has stored on it if it has any */
        for (int i = 1; i < lore.size(); i++) {
            String line = ChatColor.stripColor(lore.get(i));
            String[] s_line = line.split(" ");
            if (s_line.length < 3)
                continue;
            /* first token (the experience) */
            String token_first = s_line[0];
            /* Second to last token is the experience type */
            String token_last = s_line[s_line.length - 2];
            int experience = 0;
            try {
                experience = Integer.parseInt(token_first);
            }
            catch (NumberFormatException e) {
                ;
            }
            if (experience == 0)
                continue;
            experience_map.put(token_last, experience);
        }

        // Creating the glyph
        Glyph glyph = new Glyph();
        glyph.setLevel(level);
        glyph.setRarity(rarity);
        glyph.setElement(element);
        for (Attribute a : attributes) {
            glyph.addAttribute(a);
        }
        glyph.setExperience(experience_map);

        return glyph;
    }

    public static Glyph getGlyph(ConfigurationSection section) {
        Glyph glyph = new Glyph();

        String rarityString = section.getString("rarity");
        GlyphRarity rarity = GlyphRarity.valueOf(rarityString);
        if (rarity == null)
            return null;
        glyph.setRarity(rarity);

        String elementString = section.getString("element");
        GlyphElement element = GlyphElement.valueOf(elementString);
        if (element == null)
            return null;
        glyph.setElement(element);

        int level = section.getInt("level", -1);
        if (level < 0)
            return null;
        glyph.setLevel(level);

        ConfigurationSection experienceSection = section.getConfigurationSection("experience");
        Set<String> keys = experienceSection.getKeys(false);
        for (String k : keys) {
            int experience = experienceSection.getInt(k, -1);
            if (experience < 0)
                continue;
            glyph.setExperience(k, experience);
        }

        List<String> attributeList = section.getStringList("attributes");
        if (attributeList == null)
            return null;
        AttributeManager attributeManager = Inscription.getInstance().getAttributeManager();
        for (String attribute : attributeList) {
            AttributeType attributeType = attributeManager.getAttributeType(attribute);
            if (attributeType == null) {
                Inscription.logger.severe("AttributeType not found: ", attribute);
                continue;
            }
            glyph.addAttribute(attributeType.generate());
        }

        return glyph;
    }

    // <--- Start Class Members --->

    private GlyphRarity rarity = null;
    private GlyphElement element = null;

    // Attributes of the glyph
    private ArrayList<Attribute> attributes = new ArrayList<>();

    // Level of the glyph
    private int level = 0;

    // Stored experience in the glyph
    private Map<String, Integer> experience = new HashMap<>();

    /**
     * Gets the current level of this glyph
     *
     * @return The level of this glyph
     */
    public int getLevel() {
        return this.level;
    }
    /**
     * Sets the level of this glyph to the value specified
     *
     * @param level The value to set the level of the glyph to
     */
    public void setLevel(int level) {
        this.level = level;
    }
    /**
     * Adds the amount of levels to this glyphs levels.
     *
     * @param levels The amount of levels to add
     */
    public void addLevel(int levels) {
        this.level += levels;
    }
    /**
     * Increments the level on this glyph by one
     */
    public void addLevel() {
        this.level++;
    }

    /**
     * Returns the experience needed to levelup this glyph.
     * This returns a map because there can be different types of experience.
     *
     * @return A Map of experienc types mapped to amounts needed.
     */
    public Map<String, Integer> getExperienceToLevel() {
        // The mapping of experience to return
        Map<String, Integer> ret_exp = new HashMap<>();
        for (Attribute a : this.getAttributes()) {
            Map<String, Integer> a_exp = a.getExperience(); // experience for the
            // attribute
            for (String k : a_exp.keySet()) {
                if (ret_exp.containsKey(k))
                    ret_exp.put(k, ret_exp.get(k) + a_exp.get(k));
                else
                    ret_exp.put(k, a_exp.get(k));
            }
        }
        return ret_exp;
    }
    public int getExperienceToLevel(String type) {
        Integer i = this.getExperienceToLevel().get(type);
        if (i == null)
            return 0;
        return i;
    }
    public Map<String, Integer> getExperience() {
        return this.experience;
    }
    /**
     * Returns the amount of the type experience that this glyph has
     * Will return 0 if the type is not being stored on the glyph at all
     *
     * @param type The type of experience
     * @return The amount of experience stored on the glyph
     */
    public int getExperience(String type) {
        if (!this.experience.containsKey(type))
            return 0;
        return this.experience.get(type);
    }
    /**
     * Sets the experience stored on this glyph to the mapping of experience
     * The map passed in will be cloned to prevent object nonsense from occuring.
     *
     * @param experience The mapping of the experience
     */
    public void setExperience(Map<String, Integer> experience) {
        this.experience = new HashMap<>(experience);
    }
    /**
     * Will set the experience of the specified type to the specified amount
     *
     * @param type       The type of experience to set
     * @param experience The amount of experience to set to
     */
    public void setExperience(String type, int experience) {
        this.experience.put(type, experience);
    }
    /**
     * Adds the map of experience types and amounts to the current amount of
     * experience in this glyph.
     * If the glyph does not contain any experience, this is equivalent to
     * "setExperience";
     *
     * @param experience The map of types and amounts of experience
     */
    public void addExperience(Map<String, Integer> experience) {
        // Looping through all the experience values in the passed in map
        for (String s : experience.keySet()) {
            int current_experience = 0;
            if (this.experience.containsKey(s))
                current_experience = this.experience.get(s);
            this.experience.put(s, experience.get(s) + current_experience);
        }
    }
    /**
     * This will add the specified amount of experience to the type
     *
     * @param type       The string type of the experience
     * @param experience The amount of experience to add
     */
    public void addExperience(String type, int experience) {
        int current_experience = 0;
        if (this.experience.containsKey(type))
            current_experience = this.experience.get(type);
        this.experience.put(type, experience + current_experience);
    }
    /**
     * Will reset all experience that is currently stored in this glyph
     * This will result in an empty hashmap of exprience values and will not
     * remember what types
     * of experience this glyph had previously had.
     */
    public void resetExperience() {
        this.experience = new HashMap<String, Integer>();
    }

    public boolean isMaxLevel() {
        return this.level >= MAX_LEVEL;

    }
    public boolean canLevel() {
        if (this.isMaxLevel()) {
            return false;
        }
        Map<String, Integer> level_experience = this.getExperienceToLevel();
        for (String type : level_experience.keySet()) {
            if (!this.experience.containsKey(type))
                return false;
            if (this.experience.get(type) < level_experience.get(type))
                return false;
        }
        return true;
    }
    /**
     * Will attempt to levelup the glyph
     * If the glyph does not have enough experience to levelup then the attempt
     * will fail
     * and return false
     *
     * @return Returns true if the glyph was leveled.
     */
    public boolean attemptLevelup() {
        if (!this.canLevel()) {
            return false;
        }
        Map<String, Integer> level_experience = this.getExperienceToLevel();
        for (String type : level_experience.keySet()) {
            this.experience.put(type, this.experience.get(type) - level_experience.get(type));
        }
        this.addLevel();
        return true;
    }
    /**
     * Returns a map of all the required experience
     * Experience types will be discluded in this map if they have a value of 0 or
     * below
     *
     * @return A map of the remaining experience needed for each experience type
     */
    public Map<String, Integer> remainingExperience() {
        Map<String, Integer> remain_exp = new HashMap<>();
        Map<String, Integer> required_exp = this.getExperienceToLevel();
        for (String t : required_exp.keySet()) {
            if (this.experience.containsKey(t))
                remain_exp.put(t, required_exp.get(t) - this.experience.get(t));
            else
                remain_exp.put(t, required_exp.get(t));
            if (remain_exp.get(t) <= 0)
                remain_exp.remove(t);
        }
        return remain_exp;
    }
    /**
     * Returns the remaining experience for the specific type of experience
     *
     * @param type The type of experience
     * @return The amount of experience this glyph needs of that type to levelup
     */
    public int remainingExperience(String type) {
        Map<String, Integer> required_exp_map = this.getExperienceToLevel();
        if (!required_exp_map.containsKey(type))
            return -1;
        int required_exp = required_exp_map.get(type);
        if (!this.experience.containsKey(type))
            return required_exp;
        return required_exp - this.experience.get(type);
    }

    /**
     * Sets the rarity of this glyph
     *
     * @param rarity The rarity of this glyph to set it to
     */
    public void setRarity(GlyphRarity rarity) {
        this.rarity = rarity;
    }
    /**
     * Gets the rarity that this glyph is set as
     *
     * @return The rarity of this glyph
     */
    public GlyphRarity getRarity() {
        return this.rarity;
    }

    /**
     * Sets this glyph to the specified element
     *
     * @param element The element to set
     */
    public void setElement(GlyphElement element) {
        this.element = element;
    }
    /**
     * Returns the element that this glpyh is set as.
     *
     * @return The element of this glyph
     */
    public GlyphElement getElement() {
        return this.element;
    }

    /**
     * Adds the attribute to this glyph
     * Will set the attribute's glyph to this glyph as a reference
     *
     * @param attribute The attribute to add to this glyph
     */
    public void addAttribute(Attribute attribute) {
        attribute.setGlyph(this);
        this.attributes.add(attribute);
    }
    /**
     * Gets a list of all the attributes on this glyph
     * Each of these attribute objects should be unique.
     *
     * @return A list of attributes
     */
    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    public int getCustomModelData() {
        if (this.attributes.size() <= 0) {
            return 0;
        }
        return this.element.getModelIncrement() + this.attributes.get(0).getType().getModelIncrement() + this.getRarity().getModelIncrement();
    }

    /**
     * Generates an itemstack based on the abstract glyph object
     * By contract this itemstack should return an equal glyph object in value
     * when using
     * the Glyph.getGlyph(***) class method
     *
     * @return
     */
    public ItemStack getItemStack() {
        /* TODO Use configuration to specify the items for glyphs */
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta itemMeta = item.getItemMeta();

        // Creating the item name
        String item_name = "" + rarity.getColor();
        for (Attribute a : attributes)
            item_name += a.getType().getNameDescriptor() + " ";
        item_name += "Glyph";
        itemMeta.setDisplayName(item_name);

        // Creating the lore
        List<String> lore = new ArrayList<>();

        // Creating the info line
        String type_line = ChatColor.GRAY + "Lv. " + this.level + " " + rarity.getColor() + rarity.getDisplay() + ChatColor.GRAY + " Glyph of " + element.getColor() + element.getDisplay();
        lore.add(type_line);

        // Adding all the attribute lines
        for (Attribute a : this.attributes)
            lore.add(a.getLoreLine());

        // Adding all the experience lines
        Map<String, Integer> exp_to_level = this.getExperienceToLevel();
        for (String type : exp_to_level.keySet()) {
            int exp_amount = 0;
            if (this.experience.containsKey(type))
                exp_amount = this.experience.get(type);
            int req_amount = exp_to_level.get(type);
            String exp_line = "";
            if (req_amount > exp_amount)
                exp_line += "" + ChatColor.GRAY + exp_amount;
            else
                exp_line += "" + ChatColor.GREEN + exp_amount;
            exp_line += "" + ChatColor.DARK_GREEN + " / " + req_amount + " " + type + " Exp";

            // Adding the line to the lore
            lore.add(exp_line);
        }

        itemMeta.setLore(lore);
        itemMeta.setCustomModelData(getCustomModelData());
        item.setItemMeta(itemMeta);

        return item;
    }

    /**
     * Debug method used to print the itemstack info of the glyph
     * Was using with JUnit tests
     */
    public void printItemStack() {
        String item_name = "" + rarity.getColor();
        for (Attribute a : attributes)
            item_name += a.getType().getNameDescriptor() + " ";
        item_name += "Glyph";

        System.out.println(item_name);

        List<String> lore = new ArrayList<>();

        // Creating the info line
        String type_line = ChatColor.GREEN + "Lv. " + this.level + " " + rarity.getColor() + rarity.getDisplay() + ChatColor.WHITE + " Glyph of " + element.getColor() + element.getDisplay();
        lore.add(type_line);

        // Adding all the attribute lines
        for (Attribute a : this.attributes)
            lore.add(a.getLoreLine());

        for (String s : lore)
            System.out.println(s);

    }

    public ConfigurationSection getAsConfigurationSection() {
        ConfigurationSection section = new YamlConfiguration();

        /* Setting the rarity */
        section.set("rarity", this.rarity.toString());
        /* Setting the element */
        section.set("element", this.element.toString());
        /* Setting the level */
        section.set("level", this.level);

        /* Setting the experience */
        ConfigurationSection experienceSection = new YamlConfiguration();
        for (String key : this.experience.keySet()) {
            int amount = this.experience.get(key);
            experienceSection.set(key, amount);
        }
        section.set("experience", experienceSection);

        /* Setting the attributes */
        List<String> attributes = new ArrayList<>();
        for (Attribute a : this.attributes) {
            attributes.add(a.getType().getName());
        }
        section.set("attributes", attributes);

        return section;
    }
}
