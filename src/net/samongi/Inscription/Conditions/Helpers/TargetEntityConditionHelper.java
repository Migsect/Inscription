package net.samongi.Inscription.Conditions.Helpers;

import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Types.TargetEntityCondition;
import net.samongi.Inscription.Conditions.Types.TargetInBiomeCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TargetEntityConditionHelper {

    public enum Option {
        NO_LOCATION,
        ONLY_WEARING_HELMET,
        ONLY_WEARING_CHEST,
        ONLY_WEARING_LEGS,
        ONLY_WEARING_BOOTS
    }

    public static List<Set<Condition>> getConditionsForTargetEntity(Entity entity) {
        return getConditionsForTargetEntity(entity, new HashSet<>());
    }

    public static List<Set<Condition>> getConditionsForTargetEntity(Entity entity, Set<Option> options) {
        List<Set<Condition>> conditionGroups = new ArrayList<>();

        EntityType entityType = entity.getType();
        conditionGroups.add( EntityClass.handler.getInvolvedAsCondition(entityType, (tc) -> new TargetEntityCondition((EntityClass) tc)));

        Biome entityBiome = entity.getLocation().getBlock().getBiome();
        conditionGroups.add( BiomeClass.handler.getInvolvedAsCondition(entityBiome, (tc) -> new TargetInBiomeCondition((BiomeClass) tc)));

        return conditionGroups;
    }
}
