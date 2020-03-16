package net.samongi.Inscription.Attributes;

import java.util.HashMap;
import java.util.Map;

import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Player.PlayerData;

public abstract class Attribute {


    private Glyph glyph_container;
    private AttributeType type;

    public Attribute(AttributeType type) {
        this.type = type;
    }

    /**
     * Sets the glyph that this attribute is a part of.
     * This will determine it's hashcode.
     *
     * @param glyph
     */
    public void setGlyph(Glyph glyph) {
        this.glyph_container = glyph;
    }
    /**
     * Retrieves the glpyh this attribute is currently contained within.
     * By contract, a glyph should only be contained within one glyph.
     * This is due to the attribute referencing the glyph.
     *
     * @return The glyph this is contained within.
     */
    public Glyph getGlyph() {
        return this.glyph_container;
    }

    /**
     * Caches this glyph's effects with player data for quicker calculations
     * for events that are called rapidly and readily such as block breaks.
     * Implementation for this cahcing is required.
     *
     * @param data
     */
    public abstract void cache(PlayerData data);

    /**
     * Gets this attribute's type.
     *
     * @return
     */
    public AttributeType getType() {
        return this.type;
    }

    /**
     * Get the line of lore that can be parsed by the glyph's Parser object.
     * By contract when being parsed this should return a glyph that is identical when using
     * .equals given that .equals has been implemented.
     *
     * @return
     */
    public abstract String getLoreLine();

    public Map<String, Integer> getBaseExperience() {
        Map<String, Integer> experienceMap = new HashMap<>(this.getType().getBaseExperience_LEGACY());

        int rarityRank = this.getGlyph().getRarity().getRank();
        double rarityMultiplier = 1 + rarityRank * this.getType().getEffectRarityMultiplier();

        for (String experienceType : experienceMap.keySet()) {
            int nextValue = (int) (experienceMap.get(experienceType) * rarityMultiplier);
            experienceMap.put(experienceType, nextValue);
        }
        return experienceMap;
    }
    public Map<String, Integer> getLevelExperience() {

        Map<String, Integer> experienceMap = new HashMap<>(this.getType().getLevelExperience_LEGACY());

        int rarityRank = this.getGlyph().getRarity().getRank();
        double rarityMultiplier = 1 + rarityRank * this.getType().getEffectRarityMultiplier();

        for (String experienceType : experienceMap.keySet()) {
            int nextValue = (int) (experienceMap.get(experienceType) * rarityMultiplier);
            experienceMap.put(experienceType, nextValue);
        }
        return experienceMap;
    }

    /**
     * Returns the required experience that this attribute requires for the glyph to level up
     * This takes into account the level of the glyph it is currently set to.
     *
     * @return A Mapping of experience type to the amount of experience.
     */
    public Map<String, Integer> getExperience() {
        if (this.getType().getBaseExperience_LEGACY() == null) {
            return new HashMap<String, Integer>();
        }
        Map<String, Integer> experienceMap = new HashMap<>();

        int glyphLevel = this.getGlyph().getLevel();

        Map<String, Integer> baseExperienceMap = getBaseExperience();
        Map<String, Integer> levelExperienceMap = getLevelExperience();

        for (String experienceType : levelExperienceMap.keySet()) {

            int levelExperience = levelExperienceMap.getOrDefault(experienceType, 0);
            int baseExperience = baseExperienceMap.getOrDefault(experienceType, 0);
            int currentExperience = experienceMap.getOrDefault(experienceType, 0);

            experienceMap.put(experienceType, currentExperience + baseExperience + glyphLevel * levelExperience);
        }
        return experienceMap;
    }

}
