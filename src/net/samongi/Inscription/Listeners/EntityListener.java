package net.samongi.Inscription.Listeners;

import net.samongi.Inscription.Inscription;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class EntityListener implements Listener {

    @EventHandler public void onEntityDeath(EntityDeathEvent event) {
        //Inscription.getInstance().getLootManager().onEntityDeath(event);
        //Inscription.getInstance().getExperienceManager().onEntityDeath(event);
    }

    //    @EventHandler public void onEntityShootBowEvent(EntityShootBowEvent event) {
    //        if (event.isCancelled()) {
    //            return;
    //        }
    //
    //        Entity shooter = event.getEntity();
    //        if (!(shooter instanceof Player)) {
    //            return;
    //        }
    //        Player playerShooter = (Player) shooter;
    //        ItemStack bowItem = event.getBow();
    //        Entity projectile = event.getProjectile();
    //        projectile.setMetadata("inscription_arrow_player", new FixedMetadataValue(Inscription.getInstance(), playerShooter));
    //        projectile.setMetadata("inscription_arrow_bow", new FixedMetadataValue(Inscription.getInstance(), bowItem.clone()));
    //    }

    @EventHandler public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled())
            return;
        //Inscription.getInstance().getExperienceManager().onEntityDamageEntity(event);
    }
}
