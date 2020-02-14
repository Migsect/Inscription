package net.samongi.Inscription.Listeners;

import net.samongi.Inscription.Inscription;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (event.isCancelled()) return;
        Inscription.getInstance().getLootHandler().onBlockBreak(event);
        Inscription.getInstance().getExperienceManager().onBlockBreak(event);
        Inscription.getInstance().getExperienceManager().getTracker().onBlockBreak(event);
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (event.isCancelled()) return;
        Inscription.getInstance().getExperienceManager().onBlockPlace(event);
        Inscription.getInstance().getExperienceManager().getTracker()
            .onBlockPlace(event);
    }
    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event)
    {
        if (event.isCancelled()) return;
        Inscription.getInstance().getExperienceManager().getTracker()
            .onBlockPistonExtend(event);
    }
    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event)
    {
        if (event.isCancelled()) return;
        Inscription.getInstance().getExperienceManager().getTracker()
            .onBlockPistonRetract(event);
    }
}
