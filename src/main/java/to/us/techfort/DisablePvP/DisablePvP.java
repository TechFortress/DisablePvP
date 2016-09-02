package to.us.techfort.DisablePvP;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.PreventPvPEvent;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
public class DisablePvP extends JavaPlugin implements Listener
{
    String disabledMessage = ChatColor.RED + "You turned /pvp off";
    String enabledMessage = ChatColor.GREEN + "You turned /pvp on";
    String attackerDisabled = ChatColor.RED + "You disabled /pvp";
    String victimDisabled = ChatColor.RED + "Your victim has /pvp off";
    String noClaim = ChatColor.RED + "There's no claim here.";
    String claimPvPEnabled = "PvP has been" + ChatColor.GREEN + " enabled" + ChatColor.RESET + " in this claim.";
    String claimPvPDisabled = "PvP has been" + ChatColor.RED + " disabled" + ChatColor.RESET + " in this claim.";

    FileConfiguration config = getConfig();
    DataStore ds;
    Set<PotionEffectType> positiveEffects;
    boolean isPvPDisabledPlayer(Player player)
    {
        return config.getStringList("playersDisabled").contains(player.getUniqueId().toString());
    }

    void addPvPDisabledPlayer(Player player)
    {
        if (!isPvPDisabledPlayer(player))
        {
            List<String> newList = config.getStringList("playersDisabled");
            newList.add(player.getUniqueId().toString());
            config.set("playersDisabled", newList);
        }
        saveConfig();
    }

    void removePvPDisabledPlayer(Player player)
    {
        List<String> newList = config.getStringList("playersDisabled");
        newList.remove(player.getUniqueId().toString());
        config.set("playersDisabled", newList);
        saveConfig();
    }

    boolean isPvPEnabledClaim(Claim claim)
    {
        return config.getStringList("claimsEnabled").contains(claim.getID().toString());
    }

    void addPvPEnabledClaim(Claim claim)
    {
        if (!isPvPEnabledClaim(claim))
        {
            List<String> newList = config.getStringList("claimsEnabled");
            newList.add(claim.getID().toString());
            config.set("claimsEnabled", newList);
        }
        saveConfig();
    }

    void removePvPEnabledClaim(Claim claim)
    {
        List<String> newList = config.getStringList("claimsEnabled");
        newList.remove(claim.getID().toString());
        config.set("claimsEnabled", newList);
        saveConfig();
    }

    public void onEnable()
    {
        config.addDefault("playersDisabled", new ArrayList<String>());
        config.addDefault("claimsEnabled", new ArrayList<String>());
        config.options().copyDefaults(true);
        saveConfig();
        GriefPrevention gp = (GriefPrevention)getServer().getPluginManager().getPlugin("GriefPrevention");
        ds = gp.dataStore;
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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            if (cmd.getName().equalsIgnoreCase("pvp"))
            {
                if (args.length < 2)
                {
                    sender.sendMessage("/pvp <on|off|check> <player>");
                    return true;
                }
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null)
                {
                    sender.sendMessage(args[1] + " ain't a player.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("on"))
                {
                    enablePvP(player);
                    sender.sendMessage("Enabled PvP for " + player.getName());
                }
                else if (args[0].equalsIgnoreCase("off"))
                {
                    disablePvP(player);
                    sender.sendMessage("Disabled PvP for " + player.getName());
                }
                else if (args[0].equalsIgnoreCase("check"))
                {
                    if (isPvPDisabledPlayer(player))
                        sender.sendMessage(player.getName() + " disabled PvP");
                    else
                        sender.sendMessage(player.getName() + " did not disable PvP");
                    return true;
                }
                else
                    sender.sendMessage("/pvp <on|off|check> <player>");
                return true;
            }
            return false;
        }

        Player player = (Player)sender;

        if (cmd.getName().equalsIgnoreCase("pvp"))
        {
            if (args.length > 0) //if option is explicitly stated
            {
                switch (args[0].toLowerCase())
                {
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

        if (cmd.getName().equalsIgnoreCase("claimpvp"))
        {
            Claim claim = ds.getClaimAt(player.getLocation(), true, null);
            if (claim == null)
            {
                player.sendMessage(noClaim);
                return true;
            }
            String notAllowed = claim.allowGrantPermission(player);
            if (notAllowed == null) //e.g. allowed to do this
            {
                if (isPvPEnabledClaim(claim))
                {
                    removePvPEnabledClaim(claim);
                    player.sendMessage(claimPvPDisabled);
                    return true;
                }
                else
                {
                    addPvPEnabledClaim(claim);
                    player.sendMessage(claimPvPEnabled);
                    return true;
                }
            }
            else
            {
                player.sendMessage(ChatColor.RED + notAllowed);
                return true;
            }
        }
        return false;
    }

    void disablePvP(Player player)
    {
        addPvPDisabledPlayer(player);
        player.sendMessage(disabledMessage);
    }

    void enablePvP(Player player)
    {
        removePvPDisabledPlayer(player);
        player.sendMessage(enabledMessage);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerDamage(EntityDamageByEntityEvent event)
    {
        //Check if victim is player and if attacker is a player or victim
        if (event.getEntityType() != EntityType.PLAYER || (event.getDamager().getType() != EntityType.PLAYER || event.getDamager().getType() != EntityType.ARROW))
            return;

        //Get the attacker
        Entity damager = event.getDamager();
        Player attacker = null;
        switch (damager.getType())
        {
            case ARROW:
                Projectile arrow = (Projectile)damager;
                if (!(arrow.getShooter() instanceof Player))
                    return; //Dispenser
                attacker = (Player)arrow.getShooter();
                break;
            case PLAYER:
                attacker = (Player)damager;
        }

        //Check if attacker disabled PvP
        if (isPvPDisabledPlayer(attacker))
        {
            event.setCancelled(true);
            attacker.sendMessage(attackerDisabled);
            return;
        }

        //Attacker has PvP enabled; check if victim disabled PvP
        Player victim = (Player)event.getEntity();

        if (isPvPDisabledPlayer(victim))
        {
            event.setCancelled(true);
            attacker.sendMessage(victimDisabled);
            return; //redundant, but in case I want to add to this method in the future...
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPotionSplash(PotionSplashEvent event)
    {
        ThrownPotion potion = event.getPotion();
        //Check if a player threw the potion
        if (!(potion.getShooter() instanceof Player))
            return;

        //Check if this potion has harmful effects
        boolean isHarmful = false;
        for (PotionEffect effect : potion.getEffects())
        {
            if (!positiveEffects.contains(effect))
            {
                isHarmful = true;
                break;
            }
        }
        if (!isHarmful)
            return;

        //Check if affected entities are players with PvP disabled (except the thrower)
        for (LivingEntity entity : event.getAffectedEntities())
        {
            if (entity instanceof Player)
            {
                if (isPvPDisabledPlayer((Player)entity) && entity != potion.getShooter())
                    event.setIntensity(entity, 0);
            }
        }
    }

    @EventHandler
    void onGPPreventingPvPInAClaim(PreventPvPEvent event)
    {
        Claim claim = event.getClaim();
        if (claim == null)
            return;

        if (isPvPEnabledClaim(claim))
            event.setCancelled(true);
    }
}
