/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.cnaude.plugin.PetCreeper.Commands;

import me.cnaude.plugin.PetCreeper.Pet;
import me.cnaude.plugin.PetCreeper.PetConfig;
import me.cnaude.plugin.PetCreeper.PetMain;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 *
 * @author cnaude
 */
public class PetFreeCommand implements CommandExecutor {

    private final PetMain plugin;

    public PetFreeCommand(PetMain instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (plugin.isPetOwner(p)) {
                if (args.length == 1) {
                    if (args[0].matches("\\d+")) {
                        int idx = Integer.parseInt(args[0]) - 1;
                        if (idx >= 0 && idx < plugin.getPetsOf(p).size()) {
                            Pet pet = plugin.getPetsOf(p).get(idx);
                            Entity e = plugin.getEntityOfPet(pet);
                            plugin.untamePetOf(p, e, true);
                        } else {
                            plugin.message(p, ChatColor.RED + "Invalid pet ID.");
                        }
                    } else if (args[0].toString().equalsIgnoreCase("all")) {
                        plugin.untameAllPetsOf(p);
                    } else {
                        plugin.message(p, ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/" + PetConfig.commandPrefix + "free [id|all]");
                    }
                } else {
                    plugin.message(p, ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/" + PetConfig.commandPrefix + "free [id|all]");
                }
            } else {
                plugin.message(p, ChatColor.RED + "You have no pets. :(");
            }
        }
        return true;
    }
}
