package net.samongi.Inscription.Experience.ExperienceSource;

import net.samongi.Inscription.Experience.ExperienceReward;
import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Items.ItemUtil;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MaterialPlaceExperienceSource implements ExperienceSource {

    // ---------------------------------------------------------------------------------------------------------------//
    public static final String CONFIG_KEY = "material-place";

    private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<MaskedBlockData, ExperienceReward> m_experiencePerPlace = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerPlace(BlockData blockData, ExperienceReward reward) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        m_experiencePerPlace.put(key, reward);
    }
    public ExperienceReward getExpPerPlace(BlockData blockData) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        return m_experiencePerPlace.get(key);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler public void onBlockPlace(BlockPlaceEvent event) {
        BlockData blockData = event.getBlock().getBlockData();
        Player player = event.getPlayer();

        ExperienceReward reward = this.getExpPerPlace(blockData);
        if (reward == null) {
            return;
        }

        reward.reward(player);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseRewards(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing material place experience rewards");

        Set<String> blockDataKeys = section.getKeys(false);
        for (String key : blockDataKeys) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning(String.format("'%s is not a configuration section'", key));
                continue;
            }
            BlockData blockData = ItemUtil.parseBlockData(key);
            if (blockData == null) {
                Inscription.logger.warning("  " + key + " is not valid material data.");
                continue;
            }

            ConfigurationSection experienceTypeSection = section.getConfigurationSection(key);
            if (experienceTypeSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            ExperienceReward reward = ExperienceReward.parse(experienceTypeSection);
            setExpPerPlace(blockData, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + blockData.getAsString(true));
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//

}
