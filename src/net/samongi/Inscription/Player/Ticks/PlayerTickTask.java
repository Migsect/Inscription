package net.samongi.Inscription.Player.Ticks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class PlayerTickTask extends BukkitRunnable {

    private int m_ticks = 0;

    @Override public void run() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            PlayerTickEvent event = new PlayerTickEvent(player, m_ticks);
            Bukkit.getPluginManager().callEvent(event);
        }
        m_ticks++;
    }
}
