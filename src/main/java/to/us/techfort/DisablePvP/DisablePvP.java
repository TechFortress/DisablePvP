package to.us.techfort.DisablePvP;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Robo on 9/1/2016.
 */
public class DisablePvP extends JavaPlugin implements Listener {
    private String DISABLED_MESSAGE = ChatColor.RED + "You turned /pvp off";
    private String ENABLED_MESSAGE = ChatColor.GREEN + "You turned /pvp on";
    private String ATTACKER_DISABLED_MESSAGE = ChatColor.RED + "You disabled /pvp";
    private String VICTIM_DISABLED_MESSAGE = ChatColor.RED + "Your victim has /pvp off";

    private Set<PotionEffectType> negativeEffects;

    private boolean hasPvP(Player player)
    {
        return !getConfig().contains(player.getUniqueId().toString());
    }

    private void disablePvP(Player player, boolean sendMessage)
    {
        getConfig().set(player.getUniqueId().toString(), null);
        saveConfig();
        if (sendMessage)
            player.sendMessage(DISABLED_MESSAGE);
    }

    private void enablePvP(Player player, boolean sendMessage)
    {
        getConfig().set(player.getUniqueId().toString(), true);
        saveConfig();
        if (sendMessage)
            player.sendMessage(ENABLED_MESSAGE);
    }

    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        negativeEffects = new HashSet<>(Arrays.asList
                (
                        PotionEffectType.SLOW,
                        PotionEffectType.SLOW_DIGGING,
                        PotionEffectType.HARM,
                        PotionEffectType.CONFUSION,
                        PotionEffectType.BLINDNESS,
                        PotionEffectType.HUNGER,
                        PotionEffectType.WEAKNESS,
                        PotionEffectType.POISON,
                        PotionEffectType.LEVITATION,
                        PotionEffectType.UNLUCK,
                        PotionEffectType.BAD_OMEN
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
                    enablePvP(player, false);
                    sender.sendMessage("Enabled PvP for " + player.getName());
                } else if (args[0].equalsIgnoreCase("off")) {
                    disablePvP(player, false);
                    sender.sendMessage("Disabled PvP for " + player.getName());
                } else if (args[0].equalsIgnoreCase("check")) {
                    if (hasPvP(player))
                        sender.sendMessage(player.getName() + " has PvP enabled");
                    else
                        sender.sendMessage(player.getName() + " has PvP disabled");
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
                        disablePvP(player, true);
                        return true;
                    case "enable":
                    case "on":
                    case "true":
                        enablePvP(player, true);
                        return true;
                }
            }

            //Otherwise, toggle
            if (hasPvP(player))
                disablePvP(player, true);
            else
                enablePvP(player, true);
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        handleEntityDamageEventCuzThxSpigot(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        //Check if a player threw the potion
        if (!(potion.getShooter() instanceof Player))
            return;

        //Check if this potion has harmful effects
        boolean isHarmful = false;
        for (PotionEffect effect : potion.getEffects())
        {
            if (negativeEffects.contains(effect.getType()))
            {
                isHarmful = true;
                break;
            }
        }
        if (!isHarmful)
            return;

        Player shooter = (Player)potion.getShooter();

        //Check if affected entities are players with PvP disabled (except the thrower)
        for (LivingEntity victim : event.getAffectedEntities())
        {
            if (victim == shooter)
                continue;
            if (victim instanceof Player)
            {
                if (isPvPAllowed(shooter, (Player)victim))
                    event.setIntensity(victim, 0);
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
        if (event.getEntityType() != EntityType.PLAYER)
            return;

        Player attacker = getAttacker(event.getDamager());
        if (attacker == null)
            return;

        event.setCancelled(!isPvPAllowed((Player)event.getEntity(), attacker));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onProjectileCollide(ProjectileCollideEvent event)
    {
        //Check if victim is a player
        if (event.getCollidedWith().getType() != EntityType.PLAYER)
            return;

        //Check if attacker is a player
        if (!(event.getEntity().getShooter() instanceof Player))
            return;

        Player attacker = getAttacker(event.getEntity());
        if (attacker == null)
            return;

        event.setCancelled(!isPvPAllowed((Player)event.getCollidedWith(), attacker));
    }

    private boolean isPvPAllowed(Player victim, Player attacker)
    {
        if (!hasPvP(attacker))
        {
            attacker.sendActionBar(ATTACKER_DISABLED_MESSAGE);
            return false;
        }

        if (!hasPvP(victim))
        {
            attacker.sendActionBar(VICTIM_DISABLED_MESSAGE);
            return false;
        }

        return true;
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
