package net.samongi.Inscription.Test;

import static org.junit.Assert.*;

import net.samongi.Inscription.Glyphs.Glyph;

import org.junit.Test;

public class GlyphTests {

    @Test public void constructorTest_0() {
        Glyph glyph = new Glyph();
        if (glyph.getRarity() != null)
            fail("Glyph had a rarity set.");
        if (glyph.getElement() != null)
            fail("Glyph had a element set.");
        if (glyph.getAttributes().size() != 0)
            fail("Glyph did not have an emepty attribute array.");
        if (glyph.getLevel() != 0)
            fail("Glyph had a non-zero levle");
        if (glyph.getExperienceToLevel().size() != 0)
            fail("Glyph had a experience values");
    }

    //    @Test public void rarityTest_0() {
    //        Glyph glyph = new Glyph();
    //
    //        GlyphElement_OLD element_to_test = GlyphElement_OLD.AIR;
    //
    //        glyph.setElement(element_to_test);
    //        if (glyph.getElement() != element_to_test)
    //            fail("Element received from glyph was not correct element.");
    //    }
    //
    //    @Test public void elementTest_0() {
    //        Glyph glyph = new Glyph();
    //
    //        int level_to_test = 10;
    //
    //        glyph.setLevel(level_to_test);
    //        if (glyph.getLevel() != level_to_test)
    //            fail("Element received from glyph was not correct element.");
    //    }
    //
    //    @Test public void levelTest_0() {
    //        Glyph glyph = new Glyph();
    //
    //        GlyphRarity_OLD rarity_to_test = GlyphRarity_OLD.COMMON;
    //
    //        glyph.setRarity(rarity_to_test);
    //        if (glyph.getRarity() != rarity_to_test)
    //            fail("Element received from glyph was not correct element.");
    //    }
    //
    //    @Test public void itemstackTest_0() {
    //        Glyph glyph = new Glyph();
    //
    //        GlyphRarity_OLD rarity_to_test = GlyphRarity_OLD.COMMON;
    //        GlyphElement_OLD element_to_test = GlyphElement_OLD.AIR;
    //        int level_to_test = 10;
    //
    //        glyph.setRarity(rarity_to_test);
    //        glyph.setElement(element_to_test);
    //        glyph.setLevel(level_to_test);
    //
    //        glyph.printItemStack();
    //    }

}
