package me.cnaude.plugin.PetCreeper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.ThaH3lper.EpicBoss.API;
import me.ThaH3lper.EpicBoss.EpicBoss;
import me.cnaude.plugin.PetCreeper.Commands.PetAgeCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetFreeCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetGiveCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetInfoCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetListCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetModeCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetNameCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetReloadCommand;
import me.cnaude.plugin.PetCreeper.Commands.PetSpawnCommand;
import me.cnaude.plugin.PetCreeper.Listeners.PetEntityListener;
import me.cnaude.plugin.PetCreeper.Listeners.PetPlayerListener;
import net.minecraft.server.Navigation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PetMain extends JavaPlugin {

    public static final String PLUGIN_NAME = "PetCreeper";
    public static final String LOG_HEADER = "[" + PLUGIN_NAME + "]";
    private final PetPlayerListener playerListener = new PetPlayerListener(this);
    private final PetEntityListener entityListener = new PetEntityListener(this);
    private static PetMain instance = null;
    public ConcurrentHashMap<String, ArrayList<Pet>> playersWithPets = new ConcurrentHashMap<String, ArrayList<Pet>>();
    public ConcurrentHashMap<Entity, String> petList = new ConcurrentHashMap<Entity, String>();
    public ConcurrentHashMap<Entity, String> petNameList = new ConcurrentHashMap<Entity, String>();
    public ConcurrentHashMap<Entity, Boolean> petFollowList = new ConcurrentHashMap<Entity, Boolean>();
    public ConcurrentHashMap<Integer, Entity> entityIds = new ConcurrentHashMap<Integer, Entity>();
    static final Logger log = Logger.getLogger("Minecraft");
    PetMainLoop mainLoop;
    public boolean configLoaded = false;
    private static PetConfig config;
    private PetFile petFile = new PetFile(this);
    private static API eBossAPI;

    @Override
    public void onEnable() {
        loadConfig();

        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(entityListener, this);

        if (petFile.loadPets()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                spawnPetsOf(p);
            }
        }

        mainLoop = new PetMainLoop(this);

        getCommand(PetConfig.commandPrefix).setExecutor(new PetCommand(this));
        getCommand(PetConfig.commandPrefix + "age").setExecutor(new PetAgeCommand(this));
        getCommand(PetConfig.commandPrefix + "free").setExecutor(new PetFreeCommand(this));
        getCommand(PetConfig.commandPrefix + "give").setExecutor(new PetGiveCommand(this));
        getCommand(PetConfig.commandPrefix + "info").setExecutor(new PetInfoCommand(this));
        getCommand(PetConfig.commandPrefix + "list").setExecutor(new PetListCommand(this));
        getCommand(PetConfig.commandPrefix + "mode").setExecutor(new PetModeCommand(this));
        getCommand(PetConfig.commandPrefix + "name").setExecutor(new PetNameCommand(this));
        getCommand(PetConfig.commandPrefix + "reload").setExecutor(new PetReloadCommand(this));
        getCommand(PetConfig.commandPrefix + "spawn").setExecutor(new PetSpawnCommand(this));

    }

    public void logInfo(String s) {
        log.log(Level.INFO, String.format("%s %s", LOG_HEADER, s));
    }

    public void message(Player p, String msg) {
        msg = msg.replaceAll("%PLAYER%", p.getName());
        msg = msg.replaceAll("%DPLAYER%", p.getDisplayName());
        p.sendMessage(msg);
    }

    public static PetConfig getPConfig() {
        return config;
    }

    public void loadConfig() {
        if (!this.configLoaded) {
            getConfig().options().copyDefaults(true);
            saveConfig();
            logInfo("Configuration loaded.");
            config = new PetConfig(this);
        } else {
            reloadConfig();
            getConfig().options().copyDefaults(false);
            config = new PetConfig(this);
            logInfo("Configuration reloaded.");
        }
        instance = this;
        configLoaded = true;
    }

    public boolean hasPerm(Player p, String s) {
        if ((p.hasPermission(s)) || (p.hasPermission("petcreeper.*"))
                || (p.isOp() && PetConfig.opsBypassPerms)
                || PetConfig.disablePermissions) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDisable() {
        this.mainLoop.end();

        for (String p : petList.values()) {
            despawnPetsOf(getServer().getPlayer(p));
        }
        petFile.savePets();
        petNameList.clear();
        entityIds.clear();
        petList.clear();
        petFollowList.clear();
        playersWithPets.clear();
    }

    public ArrayList<Pet> getPetsOf(Player p) {
        return playersWithPets.get(p.getName());
    }

    public String getNameOfPet(Entity e) {
        String s = e.getType().getName();
        if (petNameList.containsKey(e)) {
            s = petNameList.get(e);
        }
        return s;
    }

    public void spawnPetsOf(Player p) {
        if (isPetOwner(p)) {
            for (Iterator<Pet> iterator = getPetsOf(p).iterator(); iterator.hasNext();) {
                Pet pet = iterator.next();
                if (!spawnPet(pet, p, true)) {
                    if (playersWithPets.get(p.getName()).contains(pet)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void printPetInfo(Player p, Entity e) {
        message(p, ChatColor.GREEN + "=====" + ChatColor.YELLOW + "[Pet Details]" + ChatColor.GREEN + "=====");
        message(p, ChatColor.GREEN + "  Type: " + ChatColor.WHITE + e.getType().getName());
        message(p, ChatColor.GREEN + "  Health: " + ChatColor.WHITE + ((LivingEntity) e).getHealth());
        message(p, ChatColor.GREEN + "  Name: " + ChatColor.WHITE + getNameOfPet(e));
        message(p, ChatColor.GREEN + "  Following: " + ChatColor.WHITE + isFollowing(e));
        ChatColor mColor = ChatColor.BLUE;
        if (getModeOfPet(e, p) == Pet.modes.AGGRESSIVE) {
            mColor = ChatColor.RED;
        }
        if (getModeOfPet(e, p) == Pet.modes.DEFENSIVE) {
            mColor = ChatColor.GOLD;
        }
        message(p, ChatColor.GREEN + "  Mode: " + mColor + getModeOfPet(e, p));
        if (e.getType() == EntityType.SHEEP) {
            message(p, ChatColor.GREEN + "  Sheared: " + ChatColor.WHITE + ((Sheep) e).isSheared());
            message(p, ChatColor.GREEN + "  Color: " + ChatColor.WHITE + ((Sheep) e).getColor().name());
        }
        if (e.getType() == EntityType.PIG) {
            message(p, ChatColor.GREEN + "  Saddled: " + ChatColor.WHITE + ((Pig) e).hasSaddle());
        }
        if (e.getType() == EntityType.SLIME) {
            message(p, ChatColor.GREEN + "  Size: " + ChatColor.WHITE + ((Slime) e).getSize());
        }
        if (e.getType() == EntityType.MAGMA_CUBE) {
            message(p, ChatColor.GREEN + "  Size: " + ChatColor.WHITE + ((MagmaCube) e).getSize());
        }
        if (e.getType() == EntityType.ENDERMAN) {
            message(p, ChatColor.GREEN + "  Held Item: " + ChatColor.WHITE + ((Enderman) e).getCarriedMaterial().getItemType().name());
        }
        if (e.getType() == EntityType.VILLAGER) {
            message(p, ChatColor.GREEN + "  Profession: " + ChatColor.WHITE + ((Villager) e).getProfession());
        }
        if (e.getType() == EntityType.OCELOT) {
            message(p, ChatColor.GREEN + "  Cat Type: " + ChatColor.WHITE + ((Ocelot) e).getCatType().name());
        }
        if (e instanceof Ageable) {
            message(p, ChatColor.GREEN + "  Age: " + ChatColor.WHITE + ((Ageable) e).getAge());
        }
    }
    
    public boolean isFollowing(Entity e) {
        if (petFollowList.containsKey(e)) {            
            return petFollowList.get(e);            
        } else {
            return false;
        }
    }

    public boolean spawnPet(Pet pet, Player p, boolean msg) {
        boolean spawned = false;
        Location pos = p.getLocation().clone();
        pos.setY(pos.getY() + 1.0D);
        if (pos instanceof Location) {
            try {
                Entity e = p.getWorld().spawnEntity(pos, pet.type);
                if (e instanceof Entity) {
                    pet.entityId = e.getEntityId();
                    pet.initEntity(e, p);

                    petList.put(e, p.getName());
                    petNameList.put(e, pet.petName);
                    petFollowList.put(e, pet.followed);
                    entityIds.put(e.getEntityId(), e);

                    if (msg) {
                        p.sendMessage(ChatColor.GREEN + "Your pet " + ChatColor.YELLOW + pet.petName + ChatColor.GREEN + " greets you!");
                    }
                    spawned = true;
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                return false;
            }
        }
        return spawned;
    }

    public Pet.modes getModeOfPet(Entity e, Player p) {
        for (Pet pet : getPetsOf(p)) {
            if (pet.entityId == e.getEntityId()) {
                return pet.mode;
            }
        }
        return Pet.modes.AGGRESSIVE;
    }

    public void despawnPetsOf(Player p) {
        if (isPetOwner(p)) {
            for (Iterator<Pet> iterator = getPetsOf(p).iterator(); iterator.hasNext();) {
                despawnPet(iterator.next());
            }
        }
    }

    public void despawnPet(Pet pet) {
        if (entityIds.containsKey(pet.entityId)) {
            Entity e = entityIds.get(pet.entityId);
            pet.initPet(e);
            pet.petName = getNameOfPet(e);
            pet.entityId = -1;
            cleanUpLists(e);
            e.remove();
        }
    }

    public void printPetListOf(Player p) {
        if (isPetOwner(p)) {
            p.sendMessage(ChatColor.GREEN + "You are the proud owner of the following pets:");
            for (int i = 0; i < getPetsOf(p).size(); i++) {
                Pet pet = getPetsOf(p).get(i);
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.AQUA + (i + 1) + ChatColor.YELLOW + "] "
                        + ChatColor.YELLOW + pet.type.getName()
                        + ChatColor.GREEN + " named " + ChatColor.YELLOW
                        + pet.petName + ChatColor.GREEN + ".");
            }
        }
    }

    public void teleportPetsOf(Player p, boolean msg) {
        if (isPetOwner(p)) {
            for (Iterator i = getPetsOf(p).iterator(); i.hasNext();) {
                teleportPet((Pet) i.next(), msg);
            }
        }
    }

    public Entity getEntityOfPet(Pet pet) {
        Entity e = null;
        if (entityIds.containsKey(pet.entityId)) {
            e = entityIds.get(pet.entityId);
        }
        return e;
    }

    public void teleportPet(Pet pet, boolean msg) {
        if (entityIds.containsKey(pet.entityId)) {
            Entity e = entityIds.get(pet.entityId);
            Player p = getServer().getPlayer(petList.get(e));            
            this.despawnPet(pet);
            this.spawnPet(pet, p, false);
            if (msg) {
                p.sendMessage(ChatColor.GREEN + "Your pet " + ChatColor.YELLOW + getNameOfPet(e) + ChatColor.GREEN + " teleported to you.");
            }
        }
    }

    public void walkToPlayer(Entity e, Player p) {
        // Wolves and ocelots handle their own walking
        if (e instanceof Wolf || e instanceof Ocelot) {
            return;
        }
        if (e.getPassenger() instanceof Player) {
            return;
        }
        if (e.getType() == EntityType.CREEPER) {
            Navigation n = ((CraftLivingEntity) e).getHandle().getNavigation();
            n.a(p.getLocation().getX() + 2, p.getLocation().getY(), p.getLocation().getZ() + 2, 0.25f);
            return;
        }
        if (e.getType() == EntityType.SQUID) {
            Navigation n = ((CraftLivingEntity) e).getHandle().getNavigation();
            n.a(p.getLocation().getX() + 2, p.getLocation().getY(), p.getLocation().getZ() + 2, 0.25f);
            return;
        }
        if (e.getType() == EntityType.SKELETON) {
            Navigation n = ((CraftLivingEntity) e).getHandle().getNavigation();
            n.a(p.getLocation().getX() + 2, p.getLocation().getY(), p.getLocation().getZ() + 2, 0.25f);
            return;
        }
        if (e instanceof Monster) {
            ((Monster) e).setTarget(p);
        } else {
            Navigation n = ((CraftLivingEntity) e).getHandle().getNavigation();
            n.a(p.getLocation().getX() + 2, p.getLocation().getY(), p.getLocation().getZ() + 2, 0.25f);
        }
    }

    public void attackNearbyEntities(Entity e, Player p, Pet.modes mode) {
        // Passive pets don't attack
        if (mode == Pet.modes.PASSIVE) {
            ((Monster) e).setTarget(null);
            return;
        }
        try {
            if (p.getNearbyEntities(PetConfig.attackDistance, PetConfig.attackDistance, PetConfig.attackDistance).isEmpty()) {
                ((Monster) e).setTarget(null);
                return;
            }
        } catch (NoSuchElementException see) {
            ((Monster) e).setTarget(null);
            return;
        }
        if (e instanceof Monster) {
            for (Iterator<Entity> iterator = p.getNearbyEntities(PetConfig.attackDistance, PetConfig.attackDistance, PetConfig.attackDistance).iterator(); iterator.hasNext();) {
                Entity target = iterator.next();
                if (target == p) {
                    continue;
                }
                if (petList.containsKey(target)) {
                    // Don't attack owner
                    if (getServer().getPlayer(petList.get(target)) == p) {
                        continue;
                    }
                    // Globally we check if pets don't attack pets
                    if (!PetConfig.petsAttackPets) {
                        continue;
                    }
                }
                if (target instanceof Player) {
                    // Defensive pets don't attack players unless provoked
                    if (mode == Pet.modes.DEFENSIVE) {
                        continue;
                    }
                    // Globally we check if pets can attack players
                    if (!PetConfig.PetsAttackPlayers) {
                        continue;
                    }
                }
                if (target instanceof Monster
                        || (target instanceof LivingEntity && mode == Pet.modes.AGGRESSIVE)) {
                    /*
                     if (e instanceof Skeleton) {
                     ((Creature)e).
                     Location spawn = e.getLocation();
                     Location tl = target.getLocation().clone();
                     tl.setY(tl.getY() + 1);
                     spawn.setY(spawn.getY()+3);
                     Vector v = tl.toVector().subtract(spawn.toVector()).normalize();

                     int arrows = Math.round(1);
                     for (int i = 0; i < arrows; i++) {
                     Arrow ar = e.getWorld().spawnArrow(spawn, v, 2.0f, 12f);
                     //ar.setVelocity(ar.getVelocity());
                     ar.setShooter((LivingEntity)p);
                     }
                     break;
                     }
                     */
                    //p.sendMessage("targeting" + target.getType().getName());
                    ((Monster) e).setTarget((LivingEntity) target);
                    break;
                }
            }
        } else {
            //p.sendMessage("targeting null1");
            if (e instanceof Monster) {
                ((Monster) e).setTarget(null);
            }
        }
    }

    public void lockPetAge(Pet pet) {
        Entity e = getEntityOfPet(pet);
        if (e instanceof Ageable) {
            pet.ageLocked = true;
            ((Ageable) e).setAgeLock(true);
        }
    }

    public void unlockPetAge(Pet pet) {
        Entity e = getEntityOfPet(pet);
        if (e instanceof Ageable) {
            pet.ageLocked = false;
            ((Ageable) e).setAgeLock(false);
        }
    }

    public void setPetAsBaby(Pet pet) {
        Entity e = getEntityOfPet(pet);
        if (e instanceof Ageable) {
            ((Ageable) e).setBaby();
            pet.age = ((Ageable) e).getAge();
        }
    }

    public void setPetAsAdult(Pet pet) {
        Entity e = getEntityOfPet(pet);
        if (e instanceof Ageable) {
            ((Ageable) e).setAdult();
            pet.age = ((Ageable) e).getAge();
        }
    }

    public boolean tamePetOf(Player p, Entity e, boolean spawned) {
        if (eBossAPI == null) {
            setupEpicBossHandler();
        }
        if (eBossAPI != null) {
            if (eBossAPI.entityBoss((LivingEntity) e)) {
                return false;
            }
        }
        boolean tamed = false;
        if (e instanceof LivingEntity) {
            EntityType et = e.getType();
            ItemStack bait = p.getItemInHand();
            int amt = bait.getAmount();

            if (((bait.getType() == PetConfig.getBait(et)) && (amt > 0)) || spawned) {
                if (isPetOwner(p)) {
                    if (getPetsOf(p).size() >= PetConfig.maxPetsPerPlayer) {
                        p.sendMessage(ChatColor.RED + "You already have the maximum number of pets!");
                        return false;
                    }
                }
                if (!spawned) {
                    if (!hasPerm(p, "petcreeper.tame." + et.getName()) && !hasPerm(p, "petcreeper.tame.All")) {
                        p.sendMessage(ChatColor.RED + "You don't have permission to tame a " + et.getName() + ".");
                        return false;
                    }
                }
                if (!isPetOwner(p)) {
                    this.playersWithPets.put(p.getName(), new ArrayList<Pet>());
                }

                Entity pe;
                if (e.getType() == EntityType.CREEPER) {
                    World world = e.getWorld();
                    Location loc = e.getLocation();
                    Boolean powered = ((Creeper) e).isPowered();
                    e.remove();
                    pe = world.spawnEntity(loc, EntityType.CREEPER);
                    ((Creeper) pe).setPowered(powered);
                } else {
                    pe = e;
                }
                Pet pet = new Pet(pe);
                this.playersWithPets.get(p.getName()).add(pet);
                tamed = true;
                pe.setFireTicks(0);
                if (pe instanceof Tameable) {
                    ((Tameable) pe).setOwner(p);
                }

                if (spawned) {
                    //p.sendMessage(ChatColor.GREEN + "Your pet " + ChatColor.YELLOW + pet.type.getName() + ChatColor.GREEN + " greets you!");
                } else {
                    if (amt == 1) {
                        p.getInventory().removeItem(new ItemStack[]{bait});
                    } else {
                        bait.setAmount(amt - 1);
                    }
                    walkToPlayer(pe, p);
                    p.sendMessage(ChatColor.GREEN + "You tamed the " + ChatColor.YELLOW + pet.type.getName() + ChatColor.GREEN + "!");
                }
                entityIds.put(pet.entityId, pe);
                petNameList.put(pe, pet.petName);
                petFollowList.put(pe, pet.followed);
                petList.put(pe, p.getName());
                if (PetConfig.defaultPetMode.equals("D")) {
                    pet.mode = Pet.modes.DEFENSIVE;
                    if (pe instanceof Wolf) {
                        ((Wolf) pe).setOwner(p);
                    }
                }
                if (PetConfig.defaultPetMode.equals("P")) {
                    pet.mode = Pet.modes.PASSIVE;
                    if (pe instanceof Wolf) {
                        ((Wolf) pe).setOwner(p);
                    }
                }
                if (PetConfig.defaultPetMode.equals("A")) {
                    pet.mode = Pet.modes.AGGRESSIVE;
                    if (pe instanceof Wolf) {
                        ((Wolf) pe).setOwner(null);
                        ((Wolf) pe).setAngry(true);
                    }
                }
            }
        }
        return tamed;
    }

    public void untameAllPetsOf(Player p) {
        if (isPetOwner(p)) {
            for (Iterator<Pet> iterator = getPetsOf(p).iterator(); iterator.hasNext();) {
                Pet pet = iterator.next();
                Entity e = getEntityOfPet(pet);
                p.sendMessage(ChatColor.GREEN + "Your pet " + ChatColor.YELLOW + petNameList.get(e) + ChatColor.GREEN + " is now free!");
                cleanUpLists(e);
                if (e instanceof Tameable) {
                    ((Tameable) e).setOwner(null);
                }
                if (e instanceof Wolf) {
                    ((Wolf) e).setAngry(false);
                }
            }
        }
        playersWithPets.remove(p.getName());
    }

    public Pet getPet(Entity e) {
        Pet returnPet = new Pet();
        if (petList.containsKey(e)) {
            for (Pet pet : getPetsOf(getServer().getPlayer(petList.get(e)))) {
                if (pet.entityId == e.getEntityId()) {
                    returnPet = pet;
                }
            }
        }
        return returnPet;
    }

    public void cleanUpLists(Entity e) {
        int id = e.getEntityId();
        if (petList.containsKey(e)) {
            petList.remove(e);
        }
        if (petNameList.containsKey(e)) {
            petNameList.remove(e);
        }
        if (petFollowList.containsKey(e)) {
            petFollowList.remove(e);
        }
        if (entityIds.containsKey(id)) {
            entityIds.remove(id);
        }
    }

    public void untamePetOf(Player p, Entity e, boolean msg) {
        if (isPetOwner(p)) {
            for (Iterator<Pet> iterator = getPetsOf(p).iterator(); iterator.hasNext();) {
                Pet pet = iterator.next();
                if (pet.entityId == e.getEntityId()) {
                    cleanUpLists(e);
                    if (e instanceof Tameable) {
                        ((Tameable) e).setOwner(null);
                    }
                    if (e instanceof Wolf) {
                        ((Wolf) e).setAngry(false);
                    }
                    iterator.remove();
                    if (msg) {
                        p.sendMessage(ChatColor.GREEN + "Your pet " + ChatColor.YELLOW + pet.petName + ChatColor.GREEN + " is now free!");
                    }
                }
            }
            if (playersWithPets.get(p.getName()).isEmpty()) {
                playersWithPets.remove(p.getName());
            }
        }
    }

    public Player getMasterOf(Entity pet) {
        return getServer().getPlayer(petList.get(pet));
    }

    public boolean isPet(Entity pet) {
        if (pet != null) {
            return petList.containsKey(pet);
        }
        return false;
    }

    public boolean isPetOwner(Player p) {
        return playersWithPets.containsKey(p.getName());
    }

    public void setFollowed(Player p, Pet pet, boolean f) {
        pet.followed = f;
    }

    public static PetMain get() {
        return instance;
    }

    public void setupEpicBossHandler() {
        Plugin epicBossPlugin = getServer().getPluginManager().getPlugin("EpicBoss");

        if (epicBossPlugin == null) {
            return;
        }

        eBossAPI = new API((EpicBoss) epicBossPlugin);
        logInfo("EpicBoss detected. Players will not be able to tame bosses.");

    }
}
