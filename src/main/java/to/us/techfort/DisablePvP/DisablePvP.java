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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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
    List<String> getPvPDisabledPlayers()
    {
        return config.getStringList("playersDisabled");
    }

    List<String> getPvPEnabledClaims()
    {
        return config.getStringList("claimsEnabled");
    }

    public void onEnable()
    {
        config.addDefault("playersDisabled", null);
        config.addDefault("claimsEnabled", null);
        config.options().copyDefaults(true);
        saveConfig();
        GriefPrevention gp = (GriefPrevention)getServer().getPluginManager().getPlugin("GriefPrevention");
        ds = gp.dataStore;
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
                    sender.sendMessage("/pvp <on|off> <player>");
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
                else
                    sender.sendMessage("/pvp <on|off> <player>");
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
            if (!getPvPDisabledPlayers().contains(player.getUniqueId().toString()))
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
                String claimID = claim.getID().toString();
                if (getPvPEnabledClaims().contains(claimID))
                {
                    getPvPEnabledClaims().remove(claimID);
                    saveConfig();
                    player.sendMessage(claimPvPDisabled);
                    return true;
                }
                else
                {
                    getPvPEnabledClaims().add(claimID);
                    saveConfig();
                    player.sendMessage(claimPvPEnabled);
                    return true;
                }
            }
        }
        return false;
    }

    void disablePvP(Player player)
    {
        if (!getPvPDisabledPlayers().contains(player.getUniqueId().toString()))
            getPvPDisabledPlayers().add(player.getUniqueId().toString());
        saveConfig();
        player.sendMessage(disabledMessage);
    }

    void enablePvP(Player player)
    {
        getPvPDisabledPlayers().remove(player.getUniqueId().toString());
        saveConfig();
        player.sendMessage(enabledMessage);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerDamage(EntityDamageByEntityEvent event)
    {
        if ((event.getEntityType() != EntityType.PLAYER) || event.getDamager().getType() != EntityType.PLAYER)
            return; //If the attacker nor victim is a player, forgettuhboutit

        Player attacker = (Player)event.getDamager();

        if (getPvPDisabledPlayers().contains(attacker.getUniqueId().toString()))
        {
            event.setCancelled(true);
            attacker.sendMessage(attackerDisabled);
            return;
        }

        //If we're here, attacker has pvp enabled

        Player victim = (Player)event.getEntity();

        if (getPvPDisabledPlayers().contains(victim.getUniqueId().toString()))
        {
            event.setCancelled(true);
            attacker.sendMessage(victimDisabled);
            return; //redundant, but in case I want to add to this method in the future...
        }
    }

    @EventHandler
    void onGPPreventingPvPInAClaim(PreventPvPEvent event)
    {
        Claim claim = event.getClaim();
        if (claim == null)
            return;

        if (getPvPEnabledClaims().contains(claim.getID().toString()))
            event.setCancelled(true);
    }
}
