package net.samongi.Inscription.Conditions.Helpers;

import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Types.ForBlockCondition;
import net.samongi.Inscription.Conditions.Types.TargetEntityCondition;
import net.samongi.Inscription.Conditions.Types.TargetInBiomeCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.Inscription.TypeClass.TypeClasses.BlockClass;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.Set;

public class BlockConditionHelper {

    private static final MaskedBlockData.Mask[] s_defaultDataMasks = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};

    public enum Option {
    }

    public static Set<Condition> getConditionsForTargetBlock(Block block) {
        return getConditionsForTargetBlock(block, new HashSet<>());
    }

    public static Set<Condition> getConditionsForTargetBlock(Block block, Set<TargetEntityConditionHelper.Option> options) {
        Set<Condition> conditionGroups = new HashSet<>();

        BlockData blockData = block.getBlockData();
        MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, s_defaultDataMasks);
        conditionGroups.addAll( BlockClass.handler.getInvolvedAsCondition(maskedBlockData, (tc) -> new ForBlockCondition((BlockClass) tc)));

        Biome blockBiome = block.getBiome();
        conditionGroups.addAll( BiomeClass.handler.getInvolvedAsCondition(blockBiome, (tc) -> new TargetInBiomeCondition((BiomeClass) tc)));

        return conditionGroups;
    }
}
