package net.samongi.Inscription.Glyphs;

import java.util.*;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.samongi.Inscription.Experience.ExperienceMap;
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
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Glyph {

    // ---------------------------------------------------------------------------------------------------------------//
    static final int MIN_LEVEL = 1;

    // ---------------------------------------------------------------------------------------------------------------//
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
        String typeLine = ChatColor.stripColor(lore.get(0));
        if (!typeLine.contains("Glyph of ")) {
            return null;
        }
        String typeLineCleaned = typeLine.replace("Glyph of ", "").replace("Lv. ", "");
        String[] splitTypeLine = typeLineCleaned.split(" ");
        if (splitTypeLine.length != 3) {
            return null;
        }

        String rarityString = splitTypeLine[1];
        String elementString = splitTypeLine[2];

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
            AttributeManager attributeManager = Inscription.getInstance().getAttributeManager();
            Attribute attribute = attributeManager.parseLoreLine(lore.get(i));
            if (attribute != null) {
                attributes.add(attribute);
            }
        }

        ExperienceMap experienceMap = new ExperienceMap();
        /* Parsing the total experience the glyph has stored on it if it has any */
        boolean totalExpKey = false;
        for (int index = 1; index < lore.size(); index++) {
            String line = ChatColor.stripColor(lore.get(index)).trim();
            if (line.equals("Total Exp:")) {
                totalExpKey = true;
                continue;
            } else if (line.isEmpty()) {
                totalExpKey = false;
                continue;
            }
            if (!totalExpKey) {
                continue;
            }
            String[] splitLine = line.split(" ", 2);

            /* first token (the experience) */
            String amountString = splitLine[0].trim();
            /* Second to last token is the experience type */
            String typeString = splitLine[splitLine.length - 1].trim();
            int experience = 0;
            try {
                experience = Integer.parseInt(amountString);
            }
            catch (NumberFormatException e) {
                Inscription.logger.warning("Could not parse glyph lore line '" + line + "'");
            }
            if (experience == 0) {
                continue;
            }
            experienceMap.set(typeString, experience);
        }

        // Creating the glyph
        Glyph glyph = new Glyph();
        glyph.setRarity(rarity);
        glyph.setElement(element);
        for (Attribute a : attributes) {
            glyph.addAttribute(a);
        }
        glyph.addExperience(experienceMap, true);
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

        ConfigurationSection totalExperienceSection = section.getConfigurationSection("total-experience");
        glyph.m_totalExperience = new ExperienceMap(totalExperienceSection);

        List<String> attributeList = section.getStringList("attributes");
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

    // Count of the experience on the glyph.
    private ExperienceMap m_totalExperience = new ExperienceMap();

    // ---------------------------------------------------------------------------------------------------------------//
    public Glyph clone() {
        Glyph glyph = new Glyph();

        glyph.m_rarity = m_rarity;
        glyph.m_element = m_element;
        for (Attribute attribute : m_attributes) {
            glyph.addAttribute(attribute.getType().generate());
        }
        glyph.m_totalExperience = m_totalExperience.clone();

        return glyph;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public ExperienceMap getBaseExperienceRequirement() {
        ExperienceMap glyphExperience = new ExperienceMap();
        for (Attribute attribute : m_attributes) {
            ExperienceMap attributeExperience = new ExperienceMap(attribute.getBaseExperience());
            glyphExperience.addInplace(attributeExperience);
        }
        return glyphExperience;
    }

    public ExperienceMap getLevelExperienceRequirement() {
        ExperienceMap glyphExperience = new ExperienceMap();
        for (Attribute attribute : m_attributes) {
            ExperienceMap attributeExperience = new ExperienceMap(attribute.getLevelExperience());
            glyphExperience.addInplace(attributeExperience);
        }
        return glyphExperience;
    }

    public Set<String> getRelevantExperienceTypes() {
        Set<String> glyphExperience = new HashSet<>();
        glyphExperience.addAll(getBaseExperienceRequirement().experienceTypes());
        glyphExperience.addAll(getLevelExperienceRequirement().experienceTypes());
        return glyphExperience;
    }

    public ExperienceMap getTotalExperience() {
        return m_totalExperience.clone();
    }

    /**
     * Gets the total amount of experience needed for a glyph with this attributes
     * to get to the specified level.
     *
     * @param level The level to achieve.
     * @return The amount of experience needed.
     */
    public ExperienceMap getTotalExperienceForLevel(int level) {
        ExperienceMap baseXP = getBaseExperienceRequirement();
        ExperienceMap levelXP = getLevelExperienceRequirement();

        int adjustedLevel = level - MIN_LEVEL;
        ExperienceMap baseComponent = baseXP.multiply(adjustedLevel);
        double levelScalar = adjustedLevel * (adjustedLevel + 1) / (double)2;
        ExperienceMap levelComponent = levelXP.multiply(levelScalar);

        return baseComponent.add(levelComponent);
    }

    /**
     * Gets the amount of experience that one would need to from the prior level.
     *
     * @param level The level to calculate the experience for.
     * @return The experience needed for the level.
     */
    public ExperienceMap getExperienceToLevel(int level) {
        ExperienceMap baseXP = getBaseExperienceRequirement();
        ExperienceMap levelXP = getLevelExperienceRequirement();
        int adjustedLevel = level - MIN_LEVEL;
        return baseXP.add(levelXP.multiply(adjustedLevel));
    }

    /**
     * Calcates the level from the provided base experience, level experience, and total experience.
     *
     * @param B The base experience per level
     * @param M The increasing experience per level
     * @param E The current experience
     * @return The amount of levels that the experience can achieve.
     */
    private static int calculateLevel(int B, int M, int E) {
        // Quadratic equation variables

        double a = M / 2.0;
        double b = B + M / 2.0;
        double c = -E;

        // Calculating the quadratic equation.
        return (int) ((-b + Math.sqrt(-4 * a * c + b * b)) / (2 * a));
    }

    public int getLevel() {
        ExperienceMap baseXP = getBaseExperienceRequirement();
        ExperienceMap levelXP = getLevelExperienceRequirement();

        int possibleLevel = Inscription.getMaxLevel();
        for (String experienceKey : getRelevantExperienceTypes()) {
            int experience = m_totalExperience.get(experienceKey);
            int baseExperience = baseXP.get(experienceKey);
            int levelExperience = levelXP.get(experienceKey);

            int level = calculateLevel(baseExperience, levelExperience, experience) + MIN_LEVEL;
            if (level < possibleLevel) {
                possibleLevel = level;
            }
        }
        return possibleLevel;
    }

    public boolean isMaxLevel() {
        return getLevel() >= Inscription.getMaxLevel();
    }

    // ---------------------------------------------------------------------------------------------------------------//
    /**
     * Adds the experience to glyph. If the experience is more than the maximum amount of experience required to levelup this
     * glyph, then the overflow experience will be returned. Otherwise the returned experience will total 0.
     *
     * @param experience The experience to add to the glyph.
     * @return The overflow experience if there was any.
     */
    public ExperienceMap addExperience(ExperienceMap experience, boolean force) {
        ExperienceMap remainingExperienceToMax = getTotalExperienceForLevel(Inscription.getMaxLevel()).subtract(m_totalExperience);
        ExperienceMap overflowExperience = new ExperienceMap();
        if (experience.subtract(remainingExperienceToMax).getTotal() > 0 && !force) {
            overflowExperience = experience.subtract(remainingExperienceToMax);
        }

        m_totalExperience.addInplace(experience.subtract(overflowExperience));

        return overflowExperience;
    }

    public ExperienceMap addExperience(ExperienceMap experience)
    {
        return addExperience(experience, false);
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

    //--------------------------------------------------------------------------------------------------------------------//
    public TextComponent getTextComponent() {

        TextComponent glyphComponent = new TextComponent("[" + getDisplayName() + "]");
        glyphComponent.setColor(m_rarity.getColor().asBungee());

        BaseComponent[] components = new BaseComponent[]{new TextComponent(CraftItemStack.asNMSCopy(getItemStack()).save(new NBTTagCompound()).toString())};
        glyphComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, components));

        return glyphComponent;
    }

    public int getCustomModelData() {
        if (this.m_attributes.size() <= 0) {
            return 0;
        }
        return this.getElement().getModelIncrement() + this.m_attributes.get(0).getType().getModelIncrement() + this.getRarity().getModelIncrement();
    }

    public String getDisplayName() {
        String displayName = "" + m_rarity.getColor();
        for (Attribute a : m_attributes)
            displayName += a.getType().getDisplayName() + " ";
        displayName += "Glyph";
        return displayName;
    }

    public String getTypeLine() {
        return ChatColor.GRAY + "Lv. " + getLevel() + " " + m_rarity.getColor() + m_rarity.getDisplay() + ChatColor.GRAY + " Glyph of " + m_element.getColor()
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
        if(isMaxLevel())
        {
            lines.add(ChatColor.GREEN + "Level Maxed");
            return lines;
        }
        ExperienceMap experienceToLevel = getExperienceToLevel(getLevel() + 1);
        ExperienceMap experienceToLevelProgress = m_totalExperience.subtract(getTotalExperienceForLevel(getLevel()));

        Set<String> experienceTypes = new HashSet<>();
        experienceTypes.addAll(experienceToLevel.experienceTypes());
        experienceTypes.addAll(m_totalExperience.experienceTypes());

        for (String experienceType : experienceTypes) {
            int requiredAmount = experienceToLevel.get(experienceType);
            int progressAmount = experienceToLevelProgress.get(experienceType);

            String experienceLine = "";
            if (requiredAmount > progressAmount) {
                experienceLine += "" + ChatColor.GRAY + progressAmount;
            } else {
                experienceLine += "" + ChatColor.GREEN + progressAmount;
            }
            experienceLine += "" + ChatColor.DARK_GREEN + " / " + requiredAmount + " " + experienceType + " Exp";

            // Adding the line to the lore
            lines.add(experienceLine);
        }
        return lines;
    }

    public List<String> getExperienceTotalsLines() {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GRAY + "Total Exp:");
        for (String experienceType : getRelevantExperienceTypes()) {
            String line = "  " + ChatColor.GRAY + "" + m_totalExperience.get(experienceType) + ChatColor.DARK_GREEN + " " + experienceType;
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
        itemMeta.setDisplayName(getDisplayName());

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
            item_name += a.getType().getDisplayName() + " ";
        item_name += "Glyph";

        System.out.println(item_name);

        List<String> lore = new ArrayList<>();

        // Creating the info line
        String type_line = ChatColor.GREEN + "Lv. " + getLevel();
        type_line += " " + m_rarity.getColor() + m_rarity.getDisplay();
        type_line += ChatColor.WHITE + " Glyph of " + m_element.getColor() + m_element.getDisplay();
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


        /* Setting the attributes */
        List<String> attributes = new ArrayList<>();
        for (Attribute a : this.m_attributes) {
            attributes.add(a.getType().getTypeName());
        }
        section.set("attributes", attributes);
        section.set("total-experience", m_totalExperience.toConfigurationSection());

        return section;
    }

    //--------------------------------------------------------------------------------------------------------------------//
}
