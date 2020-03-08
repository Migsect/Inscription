package net.samongi.Inscription.Experience.ExperienceSource;

import net.samongi.Inscription.Experience.BlockTracker;
import net.samongi.Inscription.Experience.ExperienceReward;
import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Items.ItemUtil;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MaterialBreakExperienceSource implements ExperienceSource {

    // ---------------------------------------------------------------------------------------------------------------//
    public static final String CONFIG_KEY = "material-break";

    private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<MaskedBlockData, ExperienceReward> m_experiencePerBreak = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerBreak(BlockData blockData, ExperienceReward reward) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        m_experiencePerBreak.put(key, reward);
    }
    public ExperienceReward getExpPerBreak(BlockData blockData) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        return m_experiencePerBreak.get(key);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();

        BlockTracker tracker = Inscription.getInstance().getBlockTracker();
        if (tracker.isTracked(material) && tracker.isPlaced(location)) {
            // This block break cannot trigger experience.
            return;
        }

        BlockData blockData = event.getBlock().getBlockData();
        Player player = event.getPlayer();

        ExperienceReward reward = this.getExpPerBreak(blockData);
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

        Inscription.logger.fine("Parsing material break experience rewards");

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

            ConfigurationSection experienceRewardSection = section.getConfigurationSection(key);
            if (experienceRewardSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            ExperienceReward reward = ExperienceReward.parse(experienceRewardSection);
            setExpPerBreak(blockData, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + blockData.getAsString(true));
        }

        return true;
    }
    // ---------------------------------------------------------------------------------------------------------------//
}
