package net.samongi.Inscription.Commands;

import java.util.Map;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.CommandHandling.ArgumentType;
import net.samongi.SamongiLib.CommandHandling.BaseCommand;
import net.samongi.SamongiLib.CommandHandling.SenderType;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Displays the current experience of the player's glyphs.
 *
 * @author Alex
 */
public class CommandExperience extends BaseCommand {

    public CommandExperience(String command_path)
    {
        super(command_path);
        this.permission = "inscription.experience";
        this.allowed_senders.add(SenderType.PLAYER);
        this.allowed_arguments.add(new ArgumentType[0]);
    }
    @Override
    public boolean run(CommandSender sender, String[] args)
    {
        Player player = (Player) sender;
        PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
        Map<String, Integer> experience = data.getExperience_LEGACY();
        if (experience == null) {
            player.sendMessage(ChatColor.RED + "Experience data could not be found...");
            return true;
        }
        player.sendMessage(ChatColor.GOLD + "Current Experience");
        for (String k : experience.keySet()) {
            if (experience == null) {
                Inscription.logger.warning("Experience map returned null on key '" + k + "'");
                continue;
            }
            Integer exp = experience.get(k);
            if (exp == null) {
                Inscription.logger.warning("Experience get returned null on key '" + k + "'");
                continue;
            }
            player.sendMessage(ChatColor.WHITE + "- " + ChatColor.YELLOW + exp + " "
                + k + ChatColor.WHITE + " Exp");
        }
        return true;
    }
}
