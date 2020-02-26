package net.samongi.Inscription.Glyphs;

import java.util.*;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Glyphs.Types.GlyphElement;
import net.samongi.Inscription.Glyphs.Types.GlyphRarity;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeManager;
import net.samongi.Inscription.Attributes.AttributeType;

import net.samongi.SamongiLib.Text.TextUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Glyph {

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

        // If the item does not have a display name, it can't be a glyph
        if (itemMeta == null || !itemMeta.hasDisplayName() || !itemMeta.hasLore()) {
            return null;
        }

        // can't be a glyph
        List<String> lore = itemMeta.getLore(); // Getting the lore
        if (lore.size() < 2) {
            return null;
        }

        // Parsing the type line
        // Example string: Lv. 20 Rare /Glyph of/ Time
        String type_line = ChatColor.stripColor(lore.get(0)).replace("Glyph of ", "").replace("Lv. ", "");
        String[] splitTypeLine = type_line.split(" ");
        if (splitTypeLine.length != 3) {

            return null;
        }

        String levelString = splitTypeLine[0];
        String rarityString = splitTypeLine[1];
        String elementString = splitTypeLine[2];

        // Parsing the level
        int level = -1;
        try {
            level = Integer.parseInt(levelString);
        }
        catch (NumberFormatException e) {
            Inscription.logger.warning("[GLYPH PARSE] '" + level + "' is not a valid glyph level.");
            return null;
        }

        if (level < 0) {
            Inscription.logger.warning("[GLYPH PARSE] '" + level + "' is not a valid glyph level.");
            return null;
        }

        GlyphRarity rarity = Inscription.getInstance().getGlyphTypesManager().getRarityByDisplay(rarityString);
        if (rarity == null) {
            Inscription.logger.warning("[GLYPH PARSE] '" + rarityString + "' is not a valid glyph rarity.");
            return null;
        }

        GlyphElement element = Inscription.getInstance().getGlyphTypesManager().getElementByDisplay(elementString);
        if (element == null) {
            Inscription.logger.warning("[GLYPH PARSE] '" + elementString + "' is not a valid glyph element.");
            return null;
        }

        // Parsing all the Attributes (lore items 1 to n)
        List<Attribute> attributes = new ArrayList<>();
        for (int i = 1; i < lore.size(); i++) {
            AttributeManager a_manager = Inscription.getInstance().getAttributeManager();
            Attribute attribute = a_manager.parseLoreLine(lore.get(i));
            if (attribute != null) {
                attributes.add(attribute);
            }
        }

        Map<String, Integer> experienceMap = new HashMap<>();
        /* Parsing the experience the glyph has stored on it if it has any */
        for (int i = 1; i < lore.size(); i++) {
            String line = ChatColor.stripColor(lore.get(i));
            String[] s_line = line.split(" ");
            if (s_line.length < 3) {
                continue;
            }
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
            if (experience == 0) {
                continue;
            }
            experienceMap.put(token_last, experience);
        }

        // Creating the glyph
        Glyph glyph = new Glyph();
        glyph.setLevel_LEGACY(level);
        glyph.setRarity(rarity);
        glyph.setElement(element);
        for (Attribute a : attributes) {
            glyph.addAttribute(a);
        }
        glyph.setExperience_LEGACY(experienceMap);

        return glyph;
    }
    public static Glyph getGlyph(ConfigurationSection section) {
        Glyph glyph = new Glyph();

        String rarityString = section.getString("rarity");
        GlyphRarity rarity = Inscription.getInstance().getGlyphTypesManager().getRarity(rarityString);
        if (rarity == null) {
            return null;
        }
        glyph.setRarity(rarity);

        String elementString = section.getString("element");
        GlyphElement element = Inscription.getInstance().getGlyphTypesManager().getElement(elementString);
        if (element == null) {
            return null;
        }
        glyph.setElement(element);

        int level = section.getInt("level", -1);
        if (level < 0) {
            return null;
        }
        glyph.setLevel_LEGACY(level);

        ConfigurationSection experienceSection = section.getConfigurationSection("experience");
        Set<String> keys = experienceSection.getKeys(false);
        for (String k : keys) {
            int experience = experienceSection.getInt(k, -1);
            if (experience < 0) {
                continue;
            }
            glyph.setExperience_LEGACY(k, experience);
        }

        List<String> attributeList = section.getStringList("attributes");
        if (attributeList == null) {
            return null;
        }
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

    // ---------------------------------------------------------------------------------------------------------------//

    private GlyphRarity m_rarity = null;
    private GlyphElement m_element = null;

    // Attributes of the glyph
    private ArrayList<Attribute> m_attributes = new ArrayList<>();

    // Level of the glyph
    private int m_level = 0;

    // Stored experience in the glyph
    private Map<String, Integer> m_experienceToNextLevel = new HashMap<>();
    private Map<String, Integer> m_totalExperience = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//

    public Map<String, Integer> getBaseExperienceRequirement() {
        Map<String, Integer> glyphExperience = new HashMap<>();
        for (Attribute attribute : m_attributes) {
            Map<String, Integer> attributeExperience = attribute.getType().getBaseExperience();
            for (String experienceType : attributeExperience.keySet()) {
                glyphExperience.put(experienceType, glyphExperience.getOrDefault(experienceType, 0) + attributeExperience.get(experienceType));
            }
        }
        return glyphExperience;
    }

    public Map<String, Integer> getLevelExperienceRequirement() {
        Map<String, Integer> glyphExperience = new HashMap<>();
        for (Attribute attribute : m_attributes) {
            Map<String, Integer> attributeExperience = attribute.getType().getLevelExperience();
            for (String experienceType : attributeExperience.keySet()) {
                glyphExperience.put(experienceType, glyphExperience.getOrDefault(experienceType, 0) + attributeExperience.get(experienceType));
            }
        }
        return glyphExperience;
    }

    public Set<String> getRelevantExperienceTypes() {
        Set<String> glyphExperience = new HashSet<>();
        for (Attribute attribute : m_attributes) {
            glyphExperience.addAll(attribute.getType().getBaseExperience().keySet());
            glyphExperience.addAll(attribute.getType().getLevelExperience().keySet());
        }
        return glyphExperience;
    }

    private static int calculateLevel(int baseExperience, int levelExperience, int experience) {
        int B = baseExperience;
        int M = levelExperience;
        int E = experience;
        double a = M / 2;
        double b = B + M / 2;
        double c = -E;
        return (int) ((-b + Math.sqrt(-4 * a * c + b * b)) / (2 * a));
    }

    public int getLevel() {
        Map<String, Integer> baseExperienceRequirement = getBaseExperienceRequirement();
        Map<String, Integer> levelExperienceRequirement = getLevelExperienceRequirement();

        int possibleLevel = Inscription.getMaxLevel();
        for (String experienceKey : getRelevantExperienceTypes()) {
            int experience = m_totalExperience.getOrDefault(experienceKey, 0);
            int baseExperience = baseExperienceRequirement.getOrDefault(experienceKey, 0);
            int levelExperience = levelExperienceRequirement.getOrDefault(experienceKey, 0);

            int level = Math.max((calculateLevel(baseExperience, levelExperience, experience)), 1);
            if (level < possibleLevel) {
                possibleLevel = level;
            }
        }
        return possibleLevel;
    }

    public Map<String, Integer> getTotalExperience_LEGACY() {
        Map<String, Integer> totalExperience = new HashMap<>(m_experienceToNextLevel);
        for (String experienceType : getRelevantExperienceTypes()) {
            totalExperience.put(experienceType, 0);
        }

        Map<String, Integer> baseExperienceRequirement = getBaseExperienceRequirement();
        Map<String, Integer> levelExperienceRequirement = getLevelExperienceRequirement();
        for (String experienceType : totalExperience.keySet()) {
            int baseExperience = baseExperienceRequirement.getOrDefault(experienceType, 0);
            int levelExperience = levelExperienceRequirement.getOrDefault(experienceType, 0);

            int baseExperienceComponent = baseExperience * m_level;
            int levelExperienceComponent = (levelExperience * m_level * (m_level + 1)) / 2;
            int experience = baseExperienceComponent + levelExperience;

            totalExperience.put(experienceType, experience + totalExperience.getOrDefault(experienceType, 0));
        }
        return totalExperience;

    }
    /**
     * Gets the current level of this glyph
     *
     * @return The level of this glyph
     */
    public int getLevel_LEGACY() {
        return m_level;
    }
    /**
     * Sets the level of this glyph to the value specified
     *
     * @param level The value to set the level of the glyph to
     */
    public void setLevel_LEGACY(int level) {
        m_level = level;
    }
    /**
     * Adds the amount of levels to this glyphs levels.
     *
     * @param levels The amount of levels to add
     */
    public void addLevel_LEGACY(int levels) {
        m_level += levels;
    }
    /**
     * Increments the level on this glyph by one
     */
    public void addLevel_LEGACY() {
        m_level++;
    }

    /**
     * Returns the experience needed to levelup this glyph.
     * This returns a map because there can be different types of experience.
     *
     * @return A Map of experienc types mapped to amounts needed.
     */
    public Map<String, Integer> getExperienceToLevel_LEGACY() {
        // The mapping of experience to return
        Map<String, Integer> ret_exp = new HashMap<>();
        for (Attribute a : this.getAttributes()) {
            Map<String, Integer> a_exp = a.getExperience(); // experience for the
            // attribute
            for (String k : a_exp.keySet()) {
                if (ret_exp.containsKey(k)) {
                    ret_exp.put(k, ret_exp.get(k) + a_exp.get(k));
                } else {
                    ret_exp.put(k, a_exp.get(k));
                }
            }
        }
        return ret_exp;
    }

    public int getExperienceToLevel_LEGACY(String type) {
        Integer amount = this.getExperienceToLevel_LEGACY().get(type);
        if (amount == null) {
            return 0;
        }
        return amount;
    }

    public Map<String, Integer> getExperience_LEGACY() {
        return this.m_experienceToNextLevel;
    }
    /**
     * Returns the amount of the type experience that this glyph has
     * Will return 0 if the type is not being stored on the glyph at all
     *
     * @param type The type of experience
     * @return The amount of experience stored on the glyph
     */
    public int getExperience_LEGACY(String type) {
        if (!this.m_experienceToNextLevel.containsKey(type)) {
            return 0;
        }
        return this.m_experienceToNextLevel.get(type);
    }
    /**
     * Sets the experience stored on this glyph to the mapping of experience
     * The map passed in will be cloned to prevent object nonsense from occuring.
     *
     * @param m_experience The mapping of the experience
     */
    public void setExperience_LEGACY(Map<String, Integer> m_experience) {
        this.m_experienceToNextLevel = new HashMap<>(m_experience);
    }
    /**
     * Will set the experience of the specified type to the specified amount
     *
     * @param type       The type of experience to set
     * @param experience The amount of experience to set to
     */
    public void setExperience_LEGACY(String type, int experience) {
        this.m_experienceToNextLevel.put(type, experience);
    }
    /**
     * Adds the map of experience types and amounts to the current amount of
     * experience in this glyph.
     * If the glyph does not contain any experience, this is equivalent to
     * "setExperience";
     *
     * @param experience The map of types and amounts of experience
     */
    public void addExperience_LEGACY(Map<String, Integer> experience) {
        // Looping through all the experience values in the passed in map
        for (String s : experience.keySet()) {
            int current_experience = 0;
            if (this.m_experienceToNextLevel.containsKey(s)) {
                current_experience = this.m_experienceToNextLevel.get(s);
            }
            this.m_experienceToNextLevel.put(s, experience.get(s) + current_experience);
        }
    }
    /**
     * This will add the specified amount of experience to the type
     *
     * @param type       The string type of the experience
     * @param experience The amount of experience to add
     */
    public void addExperience_LEGACY(String type, int experience) {
        int current_experience = 0;
        if (this.m_experienceToNextLevel.containsKey(type)) {
            current_experience = this.m_experienceToNextLevel.get(type);
        }
        this.m_experienceToNextLevel.put(type, experience + current_experience);
    }
    /**
     * Will reset all experience that is currently stored in this glyph
     * This will result in an empty hashmap of exprience values and will not
     * remember what types
     * of experience this glyph had previously had.
     */
    public void resetExperience_LEGACY() {
        this.m_experienceToNextLevel = new HashMap<String, Integer>();
    }

    public boolean isMaxLevel() {
        return this.getLevel() >= Inscription.getMaxLevel();
    }
    public boolean isMaxLevel_LEGACY() {
        return this.m_level >= MAX_LEVEL;

    }
    public boolean canLevel_LEGACY() {
        if (this.isMaxLevel_LEGACY()) {
            return false;
        }
        Map<String, Integer> level_experience = this.getExperienceToLevel_LEGACY();
        for (String type : level_experience.keySet()) {
            if (!this.m_experienceToNextLevel.containsKey(type)) {
                return false;
            }
            if (this.m_experienceToNextLevel.get(type) < level_experience.get(type)) {
                return false;
            }
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
    public boolean attemptLevelup_LEGACY() {
        if (!this.canLevel_LEGACY()) {
            return false;
        }
        Map<String, Integer> level_experience = this.getExperienceToLevel_LEGACY();
        for (String type : level_experience.keySet()) {
            this.m_experienceToNextLevel.put(type, this.m_experienceToNextLevel.get(type) - level_experience.get(type));
        }
        this.addLevel_LEGACY();
        return true;
    }
    /**
     * Returns a map of all the required experience
     * Experience types will be discluded in this map if they have a value of 0 or
     * below
     *
     * @return A map of the remaining experience needed for each experience type
     */
    public Map<String, Integer> remainingExperience_LEGACY() {
        Map<String, Integer> remain_exp = new HashMap<>();
        Map<String, Integer> required_exp = this.getExperienceToLevel_LEGACY();
        for (String t : required_exp.keySet()) {
            if (this.m_experienceToNextLevel.containsKey(t)) {
                remain_exp.put(t, required_exp.get(t) - this.m_experienceToNextLevel.get(t));
            } else {
                remain_exp.put(t, required_exp.get(t));
            }
            if (remain_exp.get(t) <= 0) {
                remain_exp.remove(t);
            }
        }
        return remain_exp;
    }
    /**
     * Returns the remaining experience for the specific type of experience
     *
     * @param type The type of experience
     * @return The amount of experience this glyph needs of that type to levelup
     */
    public int remainingExperience_LEGACY(String type) {
        Map<String, Integer> required_exp_map = this.getExperienceToLevel_LEGACY();
        if (!required_exp_map.containsKey(type)) {
            return -1;
        }
        int required_exp = required_exp_map.get(type);
        if (!this.m_experienceToNextLevel.containsKey(type)) {
            return required_exp;
        }
        return required_exp - this.m_experienceToNextLevel.get(type);
    }

    //--------------------------------------------------------------------------------------------------------------------//
    /**
     * Sets the rarity of this glyph
     *
     * @param rarity The rarity of this glyph to set it to
     */
    public void setRarity(GlyphRarity rarity) {
        this.m_rarity = rarity;
    }
    /**
     * Gets the rarity that this glyph is set as
     *
     * @return The rarity of this glyph
     */
    public GlyphRarity getRarity() {
        return this.m_rarity;
    }

    /**
     * Sets this glyph to the specified element
     *
     * @param element The element to set
     */
    public void setElement(GlyphElement element) {
        this.m_element = element;
    }
    /**
     * Returns the element that this glpyh is set as.
     *
     * @return The element of this glyph
     */
    public GlyphElement getElement() {
        return this.m_element;
    }

    /**
     * Adds the attribute to this glyph
     * Will set the attribute's glyph to this glyph as a reference
     *
     * @param attribute The attribute to add to this glyph
     */
    public void addAttribute(Attribute attribute) {
        attribute.setGlyph(this);
        this.m_attributes.add(attribute);
    }
    /**
     * Gets a list of all the attributes on this glyph
     * Each of these attribute objects should be unique.
     *
     * @return A list of attributes
     */
    public List<Attribute> getAttributes() {
        return this.m_attributes;
    }

    public int getCustomModelData() {
        if (this.m_attributes.size() <= 0) {
            return 0;
        }
        return this.getElement().getModelIncrement() + this.m_attributes.get(0).getType().getModelIncrement() + this.getRarity().getModelIncrement();
    }

    public String getTypeLine() {
        return ChatColor.GRAY + "Lv. " + this.m_level + " " + m_rarity.getColor() + m_rarity.getDisplay() + ChatColor.GRAY + " Glyph of " + m_element.getColor()
            + m_element.getDisplay();
    }

    public List<String> getAttributeLines() {
        List<String> lines = new ArrayList<>();
        for (Attribute attribute : this.m_attributes) {
            lines.addAll(TextUtils.wrapText(attribute.getLoreLine(), 60, 2));
        }
        return lines;
    }

    public List<String> getExperienceLines() {
        List<String> lines = new ArrayList<>();
        Map<String, Integer> experienceToLevel = this.getExperienceToLevel_LEGACY();
        for (String type : experienceToLevel.keySet()) {
            int experienceAmount = 0;
            if (this.m_experienceToNextLevel.containsKey(type)) {
                experienceAmount = this.m_experienceToNextLevel.get(type);
            }
            int requiredAmount = experienceToLevel.get(type);
            String experienceLine = "";
            if (requiredAmount > experienceAmount) {
                experienceLine += "" + ChatColor.GRAY + experienceAmount;
            } else {
                experienceLine += "" + ChatColor.GREEN + experienceAmount;
            }
            experienceLine += "" + ChatColor.DARK_GREEN + " / " + requiredAmount + " " + type + " Exp";

            // Adding the line to the lore
            lines.add(experienceLine);
        }
        return lines;
    }

    public List<String> getExperienceTotalsLines() {

        List<String> lines = new ArrayList<>();
        Map<String, Integer> totalExperience = getTotalExperience_LEGACY();
        lines.add(ChatColor.GRAY + "Total Exp:");
        for (String experienceType : getRelevantExperienceTypes()) {
            String line = "  " + ChatColor.GRAY + "" + totalExperience.get(experienceType) + ChatColor.DARK_GREEN + " " + experienceType;
            lines.add(line);
        }
        return lines;
    }

    //--------------------------------------------------------------------------------------------------------------------//

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
        String item_name = "" + m_rarity.getColor();
        for (Attribute a : m_attributes)
            item_name += a.getType().getNameDescriptor() + " ";
        item_name += "Glyph";
        itemMeta.setDisplayName(item_name);

        // Creating the lore
        List<String> lore = new ArrayList<>();

        // Creating the info line
        String type_line = getTypeLine();
        lore.add(type_line);

        // Adding all the attribute lines
        lore.add("");
        lore.addAll(getAttributeLines());

        // Adding all the experience lines
        lore.add("");
        lore.addAll(getExperienceLines());
        lore.add("");
        lore.addAll(getExperienceTotalsLines());

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
        String item_name = "" + m_rarity.getColor();
        for (Attribute a : m_attributes)
            item_name += a.getType().getNameDescriptor() + " ";
        item_name += "Glyph";

        System.out.println(item_name);

        List<String> lore = new ArrayList<>();

        // Creating the info line
        String type_line =
            ChatColor.GREEN + "Lv. " + this.m_level + " " + m_rarity.getColor() + m_rarity.getDisplay() + ChatColor.WHITE + " Glyph of " + m_element.getColor()
                + m_element.getDisplay();
        lore.add(type_line);

        // Adding all the attribute lines
        for (Attribute a : this.m_attributes) {
            lore.add(a.getLoreLine());
        }

        for (String s : lore) {
            System.out.println(s);
        }

    }

    //--------------------------------------------------------------------------------------------------------------------//
    public ConfigurationSection getAsConfigurationSection() {
        ConfigurationSection section = new YamlConfiguration();

        /* Setting the rarity */
        section.set("rarity", this.m_rarity.getType());
        /* Setting the element */
        section.set("element", this.m_element.getType());
        /* Setting the level */
        section.set("level", this.m_level);

        /* Setting the experience */
        ConfigurationSection experienceSection = new YamlConfiguration();
        for (String key : this.m_experienceToNextLevel.keySet()) {
            int amount = this.m_experienceToNextLevel.get(key);
            experienceSection.set(key, amount);
        }
        section.set("experience", experienceSection);

        /* Setting the attributes */
        List<String> attributes = new ArrayList<>();
        for (Attribute a : this.m_attributes) {
            attributes.add(a.getType().getName());
        }
        section.set("attributes", attributes);

        ConfigurationSection totalExperienceSection = new YamlConfiguration();
        Map<String, Integer> totalExperience = getTotalExperience_LEGACY();
        for (String experienceType : getRelevantExperienceTypes()) {
            totalExperienceSection.set(experienceType, totalExperience.getOrDefault(experienceType, 0));
        }
        section.set("total-experience", totalExperienceSection);

        return section;
    }
}
