/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.cnaude.plugin.PetCreeper.Commands;

import me.cnaude.plugin.PetCreeper.PetConfig;
import me.cnaude.plugin.PetCreeper.PetMain;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
//import org.bukkit.craftbukkit.entity.CraftSkeleton;
//import org.bukkit.craftbukkit.entity.CraftZombie;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftSkeleton;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftZombie;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;

/**
 *
 * @author cnaude
 */
public class PetSpawnCommand implements CommandExecutor {

    private final PetMain plugin;

    public PetSpawnCommand(PetMain instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            int spawnCount = 1;
            if (args.length == 2) {
                if (args[1].matches("\\d+")) {
                    spawnCount = Integer.parseInt(args[1]);
                }
            }
            if (spawnCount > PetConfig.maxSpawnCount) {
                spawnCount = PetConfig.maxSpawnCount;
            }
            if (args.length >= 1) {
                String petType = args[0];
                String subType = "";
                if (petType.equalsIgnoreCase("ocelot")) {
                    petType = "Ozelot";
                } else if (petType.equalsIgnoreCase("wither")) {
                    petType = "WitherBoss";
                } else if (petType.equalsIgnoreCase("zombievillager")) {
                    petType = "Zombie";
                    subType = "Villager";
                } else if (petType.equalsIgnoreCase("witherskeleton")) {
                    petType = "Skeleton";
                    subType = "Wither";
                } else if (petType.equalsIgnoreCase("redcat")) {
                    petType = "Ozelot";
                    subType = "red_cat";
                } else if (petType.equalsIgnoreCase("blackcat")) {
                    petType = "Ozelot";
                    subType = "black_cat";
                } else if (petType.equalsIgnoreCase("siamesecat")) {
                    petType = "Ozelot";
                    subType = "siamese_cat";
                } else if (petType.equalsIgnoreCase("wildcat")) {
                    petType = "Ozelot";
                    subType = "wild_ocelot";
                } 
                EntityType et = EntityType.fromName(petType);
                if (et != null) {
                    if (!et.isAlive()) {
                        plugin.message(p, ChatColor.RED + "Invalid pet type.");
                        return true;
                    }
                    if (!plugin.hasPerm(p, "petcreeper.spawn." + et.getName()) && !plugin.hasPerm(p, "petcreeper.spawn.All")) {
                        p.sendMessage(ChatColor.RED + "You don't have permission to spawn a " + et.getName() + ".");
                        return true;
                    }
                    for (int x = 1; x <= spawnCount; x++) {
                        if (plugin.isPetOwner(p)) {
                            if (plugin.getPetsOf(p).size() >= PetConfig.maxPetsPerPlayer) {
                                p.sendMessage(ChatColor.RED + "You have too many pets!");
                                return true;
                            }
                        }
                        Entity e = p.getWorld().spawnEntity(p.getLocation(), et);
                        if (e instanceof Ageable) {
                            if (PetConfig.defaultPetAge.equalsIgnoreCase("baby")) {
                                ((Ageable)e).setBaby();
                                ((Ageable)e).setAgeLock(PetConfig.lockSpawnedBabies);
                            } else if (PetConfig.defaultPetAge.equalsIgnoreCase("adult")) {
                                ((Ageable)e).setAdult();
                            } 
                        }
                        if (e instanceof Skeleton) {
                            if (subType.equalsIgnoreCase("wither")) {
                                ((CraftSkeleton) e).getHandle().setSkeletonType(1);
                            }
                        }
                        if (e instanceof Zombie) {
                            if (subType.equalsIgnoreCase("villager")) {
                                ((CraftZombie)e).getHandle().setVillager(true);
                            }
                        }
                        if (e instanceof Ocelot) {
                            if (!subType.isEmpty()) {
                                ((Ocelot)e).setCatType(Ocelot.Type.valueOf(subType.toUpperCase()));
                            }                            
                        }
                        if (plugin.tamePetOf(p, e, true)) {
                            p.sendMessage(ChatColor.GREEN + "You spawned a pet " + ChatColor.YELLOW 
                                    + et.getName() + ChatColor.GREEN + " named " + ChatColor.YELLOW 
                                    + plugin.getNameOfPet(e) + ChatColor.GREEN + "!");
                        }
                    }
                } else {
                    plugin.message(p, ChatColor.RED + "Invalid pet type.");
                }
            } else {
                plugin.message(p, ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/" + PetConfig.commandPrefix + "spawn [pet type] ([count])");
            }
        }
        return true;
    }
}
