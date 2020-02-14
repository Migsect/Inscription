package net.samongi.Inscription.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerManager {

    private Map<UUID, PlayerData> players = new HashMap<>();
    private final File data_location;

    public PlayerManager(File data_location)
    {
        this.data_location = data_location;
    }

    /**
     * Overload for player objects.
     *
     * @param player The player
     */
    public void loadPlayer(Player player)
    {
        this.loadPlayer(player.getUniqueId());
    }
    /**
     * Loads the player from the file location.
     * This will also cache all their current glyphs.
     *
     * @param player_UUID The player's UUID
     */
    public void loadPlayer(@Nonnull UUID player_UUID)
    {
        PlayerData data = PlayerData.load(data_location, player_UUID);
        if (data == null) return;
        this.players.put(player_UUID, data);
        data.getGlyphInventory().cacheGlyphs(data);
    }
    public void unloadPlayer(Player player)
    {
        this.unloadPlayer(player.getUniqueId());
    }
    public void unloadPlayer(UUID player_UUID)
    {
        PlayerData data = this.getData(player_UUID);
        if (data == null) return;
        if (!data_location.exists() || !data_location.isDirectory()) {
            data_location.mkdirs();
        }
        PlayerData.save(data, data_location);
        this.players.remove(player_UUID);
    }

    @Nonnull
    public PlayerData getData(@Nonnull Player player)
    {
        return this.getData(player.getUniqueId());
    }
    @Nullable
    public PlayerData getData(@Nonnull UUID player_UUID)
    {
        return this.players.get(player_UUID);
    }

    public void onPlayerJoin(@Nonnull PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        this.loadPlayer(player);
        ;
    }
    public void onPlayerQuit(@Nonnull PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        this.unloadPlayer(player);
    }
}
