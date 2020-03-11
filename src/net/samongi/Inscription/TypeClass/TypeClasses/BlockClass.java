package net.samongi.Inscription.TypeClass.TypeClasses;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.TypeClass.TypeClass;
import net.samongi.Inscription.TypeClass.TypeClassHandler;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockClass extends TypeClass {

    //----------------------------------------------------------------------------------------------------------------//
    public static final TypeClassHandler<BlockClass> handler = new TypeClassHandler<>("block-classes", BlockClass::new, new BlockClass("GLOBAL", true));

    //----------------------------------------------------------------------------------------------------------------//
    private final Set<MaskedBlockData> m_blockData = new HashSet<>();
    private final MaskedBlockData.Mask[] m_defaultDataMasks = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};

    //----------------------------------------------------------------------------------------------------------------//
    private BlockClass(String name, boolean isGlobal) {
        super(name, isGlobal);
    }
    private BlockClass(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
        parse(section);
    }
    //----------------------------------------------------------------------------------------------------------------//
    @Nonnull public Set<MaskedBlockData> getBlockDatas() {
        return getClassMembers().stream().map((Object obj) -> (MaskedBlockData) obj).collect(Collectors.toSet());
    }

    public boolean containsBlockData(@Nonnull BlockData type) {
        MaskedBlockData key = new MaskedBlockData(type, m_defaultDataMasks);
        return this.m_blockData.contains(key);
    }

    public boolean matchesBlock(@Nonnull Block block) {
        return isGlobal() || this.containsBlockData(block.getBlockData());
    }

    public void addBlockData(@Nonnull BlockData blockData) {
        MaskedBlockData key = new MaskedBlockData(blockData, m_defaultDataMasks);
        this.m_blockData.add(key);
    }

    public boolean addBlockData(@Nonnull String blockDataString) {
        try {
            BlockData blockData = Bukkit.createBlockData(blockDataString);
            this.addBlockData(blockData);
        }
        catch (IllegalArgumentException error) {
            return false;
        }

        return true;
    }
    //----------------------------------------------------------------------------------------------------------------//
    @Override protected void parse(ConfigurationSection section) {
        List<String> blockDataStrings = section.getStringList("block-data");
        if (blockDataStrings != null) {
            Inscription.logger.fine("Found Block Data:");
            for (String blockDataString : blockDataStrings) {
                boolean valid = addBlockData(blockDataString);
                if (!valid) {
                    Inscription.logger.warning("'" + blockDataString + "' is not a valid type for BlockClass '" + getName() + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + blockDataString + "'");
            }
        }

        // This adds a root based material class that takes the material from the block data.
        boolean isMaterialClass = section.getBoolean("is-material-class", false);
        if(isMaterialClass)
        {
            MaterialClass materialClass = new MaterialClass(getName());
            for(MaskedBlockData blockData : m_blockData)
            {
                materialClass.addMaterial(blockData.getBlockData().getMaterial());
            }
            MaterialClass.handler.register(materialClass);
        }

    }

    @Override protected TypeClassHandler<?> getTypeClassManager() {
        return handler;
    }

    @Override public void addGlobalClassMembers() {
        for (Material type : Material.values()) {
            if (!type.isBlock()) {
                continue;
            }
            addBlockData(type.createBlockData());
        }
    }

    @Override public Set<Object> getDirectClassMembers() {
        return new HashSet<>(m_blockData);
    }

    //----------------------------------------------------------------------------------------------------------------//

}
