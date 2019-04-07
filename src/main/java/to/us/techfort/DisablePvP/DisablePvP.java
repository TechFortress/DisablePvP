package to.us.techfort.DisablePvP;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
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
import org.bukkit.plugin.java.JavaPlugin;
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
    String noClaim = ChatColor.RED + "There's no claim here.";
    String claimPvPEnabled = "PvP has been" + ChatColor.GREEN + " enabled" + ChatColor.RESET + " in this claim.";
    String claimPvPDisabled = "PvP has been" + ChatColor.RED + " disabled" + ChatColor.RESET + " in this claim.";

    FileConfiguration config = getConfig();
    DataStore ds;
    Set<PotionEffectType> positiveEffects;

    boolean isPvPEnabledClaim(Claim claim) {
        return !config.getStringList("claimsDisabled").contains(claim.getID().toString());
    }

    void removePvPEnabledClaim(Claim claim) {
        if (!isPvPEnabledClaim(claim)) {
            List<String> newList = config.getStringList("claimsDisabled");
            newList.add(claim.getID().toString());
            config.set("claimsEnabled", newList);
        }
        saveConfig();
    }

    void addPvPEnabledClaim(Claim claim) {
        List<String> newList = config.getStringList("claimsDisabled");
        newList.remove(claim.getID().toString());
        config.set("claimsEnabled", newList);
        saveConfig();
    }

    public void onEnable() {
        config.addDefault("claimsEnabled", new ArrayList<String>());
        config.options().copyDefaults(true);
        saveConfig();
        GriefPrevention gp = (GriefPrevention) getServer().getPluginManager().getPlugin("GriefPrevention");
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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("claimpvp")) {
            Claim claim = ds.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                player.sendMessage(noClaim);
                return true;
            }
            String notAllowed = claim.allowGrantPermission(player);
            if (notAllowed == null) //e.g. allowed to do this
            {
                if (!isPvPEnabledClaim(claim)) {
                    removePvPEnabledClaim(claim);
                    player.sendMessage(claimPvPDisabled);
                    return true;
                } else {
                    addPvPEnabledClaim(claim);
                    player.sendMessage(claimPvPEnabled);
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + notAllowed);
                return true;
            }
        }
        return false;
    }

    @EventHandler
    void onGPPreventingPvPInAClaim(PreventPvPEvent event) {
        Claim claim = event.getClaim();
        if (claim == null)
            return;

        if (isPvPEnabledClaim(claim))
            event.setCancelled(true);
    }
    
/**Used to allow arrows to pass through players on PvP-protected claims*/
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    void onProjectileCollideInClaim(ProjectileCollideEvent event)
    {
        if (event.getCollidedWith().getType() != EntityType.PLAYER)
            return;

        if (!(event.getEntity().getShooter() instanceof Player))
            return;

        //determine which player is attacking, if any
        Player attacker = (Player)event.getEntity().getShooter();

        PlayerData attackerData = ds.getPlayerData(attacker.getUniqueId());

        Claim attackerClaim = this.ds.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
        if(!attackerData.ignoreClaims)
        {
            if( attackerClaim != null && //ignore claims mode allows for pvp inside land claims
                    !attackerData.inPvpCombat() &&
                    GriefPrevention.instance.claimIsPvPSafeZone(attackerClaim))
            {
                attackerData.lastClaim = attackerClaim;
                PreventPvPEvent pvpEvent = new PreventPvPEvent(attackerClaim);
                Bukkit.getPluginManager().callEvent(pvpEvent);
                if(!pvpEvent.isCancelled())
                {
                    event.setCancelled(true);
                    return;
                }
            }

            Player defender = (Player)event.getCollidedWith();
            PlayerData defenderData = ds.getPlayerData(defender.getUniqueId());

            Claim defenderClaim = this.ds.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
            if( defenderClaim != null &&
                    !defenderData.inPvpCombat() &&
                    GriefPrevention.instance.claimIsPvPSafeZone(defenderClaim))
            {
                defenderData.lastClaim = defenderClaim;
                PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                Bukkit.getPluginManager().callEvent(pvpEvent);
                if(!pvpEvent.isCancelled())
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
