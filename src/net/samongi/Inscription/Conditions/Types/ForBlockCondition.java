package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.TypeClassCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.BlockClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class ForBlockCondition extends TypeClassCondition<BlockClass> {

    // ---------------------------------------------------------------------------------------------------------------//
    public ForBlockCondition(BlockClass block) {
        super(block);
    }
    public ForBlockCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super();

        String blockClassString = section.getString("block-class");
        if (blockClassString == null) {
            throw new InvalidConfigurationException("'block-class' is not defined");
        }

        BlockClass blockClass = BlockClass.handler.getTypeClass(blockClassString);
        if (blockClass == null) {
            throw new InvalidConfigurationException("'" + blockClassString + "' is not a valid block class");
        }

        m_typeClass = blockClass;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        return ChatColor.YELLOW + " on " + ChatColor.BLUE + getTypeClass().getName();
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean equals(ForBlockCondition other) {
        return getTypeClass().equals(other.getTypeClass());
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof ForBlockCondition) {
            return equals((ForBlockCondition) obj);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
