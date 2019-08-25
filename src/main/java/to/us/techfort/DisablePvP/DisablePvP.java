package to.us.techfort.DisablePvP;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Robo on 9/1/2016.
 */
public class DisablePvP extends JavaPlugin implements Listener {
    String disabledMessage = ChatColor.RED + "You turned /pvp off";
    String enabledMessage = ChatColor.GREEN + "You turned /pvp on";
    String attackerDisabled = ChatColor.RED + "You disabled /pvp";
    String victimDisabled = ChatColor.RED + "Your victim has /pvp off";
    String noClaim = ChatColor.RED + "There's no claim here.";

    FileConfiguration config = getConfig();
    Set<PotionEffectType> positiveEffects;

    boolean isPvPDisabledPlayer(Player player) {
        return config.getStringList("playersDisabled").contains(player.getUniqueId().toString());
    }

    void addPvPDisabledPlayer(Player player) {
        if (!isPvPDisabledPlayer(player)) {
            List<String> newList = config.getStringList("playersDisabled");
            newList.add(player.getUniqueId().toString());
            config.set("playersDisabled", newList);
        }
        saveConfig();
    }

    void removePvPDisabledPlayer(Player player) {
        List<String> newList = config.getStringList("playersDisabled");
        newList.remove(player.getUniqueId().toString());
        config.set("playersDisabled", newList);
        saveConfig();
    }

    public void onEnable() {
        config.addDefault("playersDisabled", new ArrayList<String>());
        config.addDefault("claimsEnabled", new ArrayList<String>());
        config.options().copyDefaults(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(this, this);
        positiveEffects = new HashSet<>(Arrays.asList
                (
                        PotionEffectType.ABSORPTION,
                        PotionEffectType.DAMAGE_RESISTANCE,
                        PotionEffectType.FAST_DIGGING,
                        PotionEffectType.FIRE_RESISTANCE,
                        PotionEffectType.HEAL,
                        PotionEffectType.HEALTH_BOOST,
                        PotionEffectType.INCREASE_DAMAGE,
                        PotionEffectType.INVISIBILITY,
                        PotionEffectType.JUMP,
                        PotionEffectType.NIGHT_VISION,
                        PotionEffectType.REGENERATION,
                        PotionEffectType.SATURATION,
                        PotionEffectType.SPEED,
                        PotionEffectType.WATER_BREATHING
                )
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (cmd.getName().equalsIgnoreCase("pvp")) {
                if (args.length < 2) {
                    sender.sendMessage("/pvp <on|off|check> <player>");
                    return true;
                }
                Player player = Bukkit.getPlayerExact(args[1]);
                if (player == null) {
                    sender.sendMessage(args[1] + " ain't a player.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("on")) {
                    enablePvP(player);
                    sender.sendMessage("Enabled PvP for " + player.getName());
                } else if (args[0].equalsIgnoreCase("off")) {
                    disablePvP(player);
                    sender.sendMessage("Disabled PvP for " + player.getName());
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (isPvPDisabledPlayer(player))
                        sender.sendMessage(player.getName() + " disabled PvP");
                    else
                        sender.sendMessage(player.getName() + " did not disable PvP");
                    return true;
                } else
                    sender.sendMessage("/pvp <on|off|check> <player>");
                return true;
            }
            return false;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("pvp")) {
            if (args.length > 0) //if option is explicitly stated
            {
                switch (args[0].toLowerCase()) {
                    case "disable":
                    case "off":
                    case "false":
                        disablePvP(player);
                        return true;
                    case "enable":
                    case "on":
                    case "true":
                        enablePvP(player);
                        return true;
                }
            }

            //Otherwise, toggle
            if (isPvPDisabledPlayer(player))
                enablePvP(player);
            else
                disablePvP(player);
            return true;
        }
        return false;
    }

    void disablePvP(Player player) {
        addPvPDisabledPlayer(player);
        player.sendMessage(disabledMessage);
    }

    void enablePvP(Player player) {
        removePvPDisabledPlayer(player);
        player.sendMessage(enabledMessage);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerDamage(EntityDamageByEntityEvent event) {
        handleEntityDamageEventCuzThxSpigot(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        //Check if a player threw the potion
        if (!(potion.getShooter() instanceof Player))
            return;

        //Check if this potion has harmful effects
        boolean isHarmful = false;
        for (PotionEffect effect : potion.getEffects()) {
            if (!positiveEffects.contains(effect)) {
                isHarmful = true;
                break;
            }
        }
        if (!isHarmful)
            return;

        //Mark whether the shooter disabled PvP or not
        boolean shooterDisabledPvP = isPvPDisabledPlayer((Player) potion.getShooter());

        //Check if affected entities are players with PvP disabled (except the thrower)
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player) {
                if ((shooterDisabledPvP || isPvPDisabledPlayer((Player) entity)) && entity != potion.getShooter())
                    event.setIntensity(entity, 0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerIgniteWithArrow(EntityCombustByEntityEvent event)
    {
        EntityDamageByEntityEvent eventWrapper = new EntityDamageByEntityEvent(event.getCombuster(), event.getEntity(), EntityDamageEvent.DamageCause.FIRE_TICK, (double) event.getDuration());
        handleEntityDamageEventCuzThxSpigot(eventWrapper);
        event.setCancelled(eventWrapper.isCancelled());
    }

    //Credit to BigScary for some of this thanks to the spigot-madness of changing this event >_>
    void handleEntityDamageEventCuzThxSpigot(EntityDamageByEntityEvent event)
    {
        Entity damager = event.getDamager();

        //Check if victim is player
        if (event.getEntityType() != EntityType.PLAYER)
            return;

        //Get the attacker


        //Check if attacker is a player
        if (attacker == null)
            return;

        //Check if attacker disabled PvP
        if (isPvPDisabledPlayer(attacker))
        {
            event.setCancelled(true);
            attacker.sendActionBar(attackerDisabled);
            return;
        }

        //Attacker has PvP enabled; check if victim disabled PvP
        Player victim = (Player) event.getEntity();

        if (isPvPDisabledPlayer(victim))
        {
            event.setCancelled(true);
            attacker.sendActionBar(victimDisabled);
            return; //redundant, but in case I want to add to this method in the future...
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onProjectileCollide(ProjectileCollideEvent event)
    {
        //Is this copy-pasted code? Is this bad practice?
        //You sure betcha it is!!!!!1111!!

        //Check if victim is a player
        if (event.getCollidedWith().getType() != EntityType.PLAYER)
            return;

        //Check if attacker is a player
        if (!(event.getEntity().getShooter() instanceof Player))
            return;

        Entity damager = event.getEntity();

        //Get the attacker
        Player attacker;
        switch (damager.getType())
        {
            case ARROW:
            case SPECTRAL_ARROW:
                attacker = (Player)event.getEntity().getShooter();
                break;
            default: //Please
                return;
        }

        //Check if attacker disabled PvP
        if (isPvPDisabledPlayer(attacker)) {
            event.setCancelled(true);
            return;
        }

        //Attacker has PvP enabled; check if victim disabled PvP
        Player victim = (Player) event.getEntity();

        if (isPvPDisabledPlayer(victim)) {
            event.setCancelled(true);
            return; //redundant, but in case I want to add to this method in the future...
        }
    }

    private boolean isPvPAllowed(Player victim, Player attacker)
    {

    }

    private Player getAttacker(Entity damager)
    {
        Player attacker = null;
        switch (damager.getType())
        {
            case ARROW:
            case SPECTRAL_ARROW:
                Projectile arrow = (Projectile) damager;
                if (!(arrow.getShooter() instanceof Player))
                    return null; //Dispenser, etc.
                attacker = (Player) arrow.getShooter();
                break;
            case PLAYER:
                attacker = (Player) damager;
        }
        return attacker;
    }
}
