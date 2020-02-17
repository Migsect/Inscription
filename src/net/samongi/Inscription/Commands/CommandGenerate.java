package net.samongi.Inscription.Commands;

import java.util.List;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Generator.GlyphGenerator;
import net.samongi.SamongiLib.CommandHandling.ArgumentType;
import net.samongi.SamongiLib.CommandHandling.BaseCommand;
import net.samongi.SamongiLib.CommandHandling.SenderType;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Runs a specified generator and thus gives the running player that glyph.
 *
 * @author Migsect
 */
public class CommandGenerate extends BaseCommand {

    public CommandGenerate(String command_path) {
        super(command_path);
        this.permission = "inscription.generate";
        this.allowed_senders.add(SenderType.PLAYER);

        this.allowed_arguments.add(new ArgumentType[0]);
        ArgumentType[] argumentTypes = {ArgumentType.STRING};
        this.allowed_arguments.add(argumentTypes);
    }

    @Override public boolean run(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            List<GlyphGenerator> generators = Inscription.getInstance().getGeneratorManager().getGenerators();
            player.sendMessage(ChatColor.GOLD + "Generator Types:");
            for (GlyphGenerator generator : generators) {
                player.sendMessage(ChatColor.WHITE + "- " + ChatColor.YELLOW + generator.getTypeName());
            }
        } else if (args.length == 1) {
            String generatorType = args[0];
            GlyphGenerator generator = Inscription.getInstance().getGeneratorManager().getGeneratorByType(generatorType);
            if (generator == null) {
                player.sendMessage(ChatColor.RED + "Could not find the specified generator type.");
                return true;
            }
            ItemStack item = generator.getGlyph().getItemStack();
            player.getInventory().addItem(item);
        }

        return true;
    }
}
