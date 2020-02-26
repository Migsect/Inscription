package net.samongi.Inscription.Attributes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Player.PlayerData;

public abstract class Attribute implements Serializable {

    // Generated Serialization UID
    private static final long serialVersionUID = -5994047304332535496L;

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

    /**
     * Returns the required experience that this attribute requires for the glyph to level up
     * This takes into account the level of the glyph it is currently set to.
     *
     * @return A Mapping of experience type to the amount of experience.
     */
    public Map<String, Integer> getExperience() {
        if (this.getType().getBaseExperience() == null) {
            return new HashMap<String, Integer>();
        }
        Map<String, Integer> experience_map = new HashMap<>(this.getType().getBaseExperience());
        int glyph_level = this.getGlyph().getLevel_LEGACY();
        int ratity_level = this.getGlyph().getRarity().getRank();
        double rarity_multiplier = this.getType().getRarityMultiplier();

        for (String s : this.getType().getLevelExperience().keySet()) {
            if (!experience_map.containsKey(s)) {
                experience_map.put(s, (int) (this.getType().getLevelExperience().get(s) * glyph_level * (1 + rarity_multiplier * ratity_level)));
            } else {
                experience_map
                    .put(s, (int) (experience_map.get(s) + this.getType().getLevelExperience().get(s) * glyph_level * (1 + rarity_multiplier * ratity_level)));
            }
        }
        return experience_map;
    }

}
