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

    public static Set<Condition> getConditionsForPlayer(Player player) {
        return getConditionsForPlayer(player, new HashSet<>());
    }

    public static Set<Condition> getConditionsForPlayer(Player player, Option option) {
        HashSet<Option> options = new HashSet<>();
        options.add(option);
        return getConditionsForPlayer(player, options);
    }


    public static Set<Condition> getConditionsForPlayer(Player player, Set<Option> options) {
        Set<Condition> conditionGroups = new HashSet<>();

        conditionGroups.add(new PlayerWhileLevelCondition((double) player.getLevel(), ComparativeCondition.Mode.NULL));
        conditionGroups.add(new PlayerWhileLifeCondition((double) player.getHealth(), ComparativeCondition.Mode.NULL));

        if (!options.contains(Option.NO_LOCATION)) {
            Biome playerBiome = player.getLocation().getBlock().getBiome();
            conditionGroups.addAll(BiomeClass.handler.getInvolvedAsCondition(playerBiome, (tc) -> new PlayerInBiomeCondition((BiomeClass) tc)));
        }

        Material mainHandMaterial = Material.AIR;
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        mainHandMaterial =  player.getInventory().getItemInMainHand().getType();

        conditionGroups.addAll(MaterialClass.handler.getInvolvedAsCondition(mainHandMaterial, (tc) -> new PlayerUsingMaterialCondition((MaterialClass) tc)));

        boolean wearingAny =
            !options.contains(Option.ONLY_WEARING_HELMET) && !options.contains(Option.ONLY_WEARING_HELMET) && !options.contains(Option.ONLY_WEARING_HELMET)
                && !options.contains(Option.ONLY_WEARING_HELMET);
        Set<Condition> wearingConditions = new HashSet<>();
        PlayerInventory playerInventory = player.getInventory();
        if (playerInventory.getHelmet() != null && (wearingAny || options.contains(Option.ONLY_WEARING_HELMET))) {
            Material armorMaterial = playerInventory.getHelmet().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        if (playerInventory.getChestplate() != null && (wearingAny || options.contains(Option.ONLY_WEARING_CHEST))) {
            Material armorMaterial = playerInventory.getChestplate().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        if (playerInventory.getLeggings() != null && (wearingAny || options.contains(Option.ONLY_WEARING_LEGS))) {
            Material armorMaterial = playerInventory.getLeggings().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        if (playerInventory.getBoots() != null && (wearingAny || options.contains(Option.ONLY_WEARING_BOOTS))) {
            Material armorMaterial = playerInventory.getBoots().getType();
            wearingConditions
                .addAll(MaterialClass.handler.getInvolvedAsCondition(armorMaterial, (tc) -> new PlayerWhileWearingMaterialCondition((MaterialClass) tc)));
        }
        conditionGroups.addAll(wearingConditions);

        return conditionGroups;
    }
}
