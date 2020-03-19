package net.samongi.Inscription.Conditions.Helpers;

import net.samongi.Inscription.Conditions.ComparativeCondition;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Types.*;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerConditionHelper {

    public enum Option {
        NO_LOCATION,
        ONLY_WEARING_HELMET,
        ONLY_WEARING_CHEST,
        ONLY_WEARING_LEGS,
        ONLY_WEARING_BOOTS
    }

    public static List<Set<Condition>> getConditionsForPlayer(Player player) {
        return getConditionsForPlayer(player, new HashSet<>());
    }

    public static List<Set<Condition>> getConditionsForPlayer(Player player, Set<Option> options) {
        List<Set<Condition>> conditionGroups = new ArrayList<>();

        Set<Condition> whileLevelConditions = new HashSet<>();
        whileLevelConditions.add(new PlayerWhileLevelCondition((double) player.getLevel(), ComparativeCondition.Mode.NULL));
        conditionGroups.add(whileLevelConditions);

        Set<Condition> whileLifeConditions = new HashSet<>();
        whileLifeConditions.add(new PlayerWhileLifeCondition((double) player.getHealth(), ComparativeCondition.Mode.NULL));
        conditionGroups.add(whileLifeConditions);

        if (!options.contains(Option.NO_LOCATION)) {
            Biome playerBiome = player.getLocation().getBlock().getBiome();
            conditionGroups.add(BiomeClass.handler.getInvolvedAsCondition(playerBiome, (tc) -> new PlayerInBiomeCondition((BiomeClass) tc)));
        }

        Material mainHandMaterial = Material.AIR;
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (mainHandItem != null) {
            mainHandMaterial =  player.getInventory().getItemInMainHand().getType();
        }
        conditionGroups.add(MaterialClass.handler.getInvolvedAsCondition(mainHandMaterial, (tc) -> new PlayerUsingMaterialCondition((MaterialClass) tc)));

        boolean wearingAny =
            !options.contains(Option.ONLY_WEARING_HELMET) && !options.contains(Option.ONLY_WEARING_HELMET) && !options.contains(Option.ONLY_WEARING_HELMET)
                && !options.contains(Option.ONLY_WEARING_HELMET);
        Set<Condition> wearingConditions = new HashSet<>();
        PlayerInventory playerInventory = player.getInventory();
        if (wearingAny || options.contains(Option.ONLY_WEARING_HELMET)) {
            Material armorMaterial = playerInventory.getHelmet().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        if (wearingAny || options.contains(Option.ONLY_WEARING_CHEST)) {
            Material armorMaterial = playerInventory.getChestplate().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        if (wearingAny || options.contains(Option.ONLY_WEARING_LEGS)) {
            Material armorMaterial = playerInventory.getChestplate().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        if (wearingAny || options.contains(Option.ONLY_WEARING_BOOTS)) {
            Material armorMaterial = playerInventory.getChestplate().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        conditionGroups.add(wearingConditions);

        return conditionGroups;
    }
}
