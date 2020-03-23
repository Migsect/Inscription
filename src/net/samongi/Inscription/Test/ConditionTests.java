package net.samongi.Inscription.Test;

import static org.junit.Assert.*;

import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Types.ToBiomeCondition;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;

import net.samongi.SamongiLib.Logger.BetterLogger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

public class ConditionTests {
    @BeforeClass
    public static void oneTimeSetUp() {
        Inscription.logger = new BetterLogger(Logger.getLogger("UnitTest"));
    }

    @Test public void ToBiomeEqualityTrueSelf()
    {
        BiomeClass classTest1 = new BiomeClass("test1", false);

        Condition toBiomeCondition_1 = new ToBiomeCondition(classTest1);

        assertEquals(toBiomeCondition_1, toBiomeCondition_1);
    }


    @Test public void ToBiomeEqualityTrueSameBase()
    {
        BiomeClass classTest1 = new BiomeClass("test1", false);

        Condition toBiomeCondition_1 = new ToBiomeCondition(classTest1);
        Condition toBiomeCondition_2 = new ToBiomeCondition(classTest1);

        assertEquals(toBiomeCondition_1, toBiomeCondition_2);
    }

    @Test public void ToBiomeEqualityTrueDifferentBase()
    {
        BiomeClass classTest1 = new BiomeClass("test1", false);
        BiomeClass classTest2 = new BiomeClass("test1", false);

        Condition toBiomeCondition_1 = new ToBiomeCondition(classTest1);
        Condition toBiomeCondition_2 = new ToBiomeCondition(classTest2);

        assertEquals(toBiomeCondition_1, toBiomeCondition_2);
    }

    @Test public void ToBiomeEqualityFalse()
    {
        BiomeClass classTest1 = new BiomeClass("test1", false);
        BiomeClass classTest2 = new BiomeClass("test2", false);

        Condition toBiomeCondition_1 = new ToBiomeCondition(classTest1);
        Condition toBiomeCondition_2 = new ToBiomeCondition(classTest2);

        assertNotEquals(toBiomeCondition_1, toBiomeCondition_2);
    }
}
