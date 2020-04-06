package net.samongi.Inscription.Ability.Abilities;

import net.samongi.Inscription.Ability.AbilityHandler;
import net.samongi.Inscription.Attributes.Types.VillagerScanAttributeType;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Menu.VillagerMenu;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class VillagerScanAbility extends AbilityHandler {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String ID = "MERCHANT_SCAN";
    private static final String DISPLAY = "Merchant Scan";
    private static final String DESCRIPTION = "Scans the area for various merchants. Radius is increased by glyphs.";

    //----------------------------------------------------------------------------------------------------------------//
    public VillagerScanAbility() {
        super(ID, DISPLAY, DESCRIPTION);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public void onAction(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
        if (playerData == null) {
            return;
        }

        int range = 0;

        CacheData data = playerData.getData(VillagerScanAttributeType.TYPE_IDENTIFIER);
        if (data instanceof VillagerScanAttributeType.Data) {
            VillagerScanAttributeType.Data villagerScanData = (VillagerScanAttributeType.Data) data;
            double aggregate = villagerScanData.calculateAggregate(player);
            range = (int) Math.floor(aggregate);
        }


        VillagerMenu menu = new VillagerMenu(player, range);
        menu.getMenu(player).openMenu();
    }

    //----------------------------------------------------------------------------------------------------------------//
}
