package com.wdpserver.wdpcustomitems;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.ChatColor.*;

public class GraplingHandeler implements Listener {

    private final WdpCustomItems plugin;
    private final long grapplingCooldownTimeMs;

    // Independent cooldown and bar tracking
    private final Map<UUID, Long> grapplingCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> grapplingCooldownBars = new HashMap<>();
    private final Map<UUID, BossBar> grapplingReadyBars = new HashMap<>();

    public GraplingHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
        this.grapplingCooldownTimeMs = plugin.longCooldownTimeMsGrappling; // Use separate config if needed
    }

    @EventHandler
    public void onRightClickTrident(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.TRIDENT) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (!event.getItem().getItemMeta().getPersistentDataContainer().has(plugin.grapplingKey, PersistentDataType.BYTE))
            return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Remove ready bar
        BossBar readyBar = grapplingReadyBars.remove(playerId);
        if (readyBar != null) readyBar.removeAll();

        long now = System.currentTimeMillis();

        if (grapplingCooldowns.containsKey(playerId)) {
            long last = grapplingCooldowns.get(playerId);
            long elapsed = now - last;

            if (elapsed < grapplingCooldownTimeMs) {
                long secondsLeft = ((grapplingCooldownTimeMs - elapsed) / 1000) + 1;

                BossBar cooldownBar = grapplingCooldownBars.get(playerId);
                if (cooldownBar != null && !cooldownBar.getPlayers().contains(player)) {
                    cooldownBar.addPlayer(player);
                }

                player.sendMessage(RED + "Grappling Hook Cooldown: " + secondsLeft + "s remaining.");
                return;
            }
        }

        grapplingCooldowns.put(playerId, now);

        BossBar cooldownBar = Bukkit.createBossBar(
                RED + "GRAPPLING COOLDOWN",
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );
        grapplingCooldownBars.put(playerId, cooldownBar);
        cooldownBar.addPlayer(player);

        Vector direction = player.getLocation().getDirection().normalize();
        Location initialLocation = player.getLocation().clone();

        new BukkitRunnable() {
            double step = 0;
            boolean returning = false;
            Entity hookedEntity = null;

            @Override
            public void run() {
                if (player.getLocation().distanceSquared(initialLocation) > 0.01) {
                    player.sendMessage(RED + "You moved, grappling hook canceled.");
                    cancel();
                    return;
                }

                Location playerFeet = player.getEyeLocation().clone().subtract(0, 0.3, 0);
                Location eyeLoc = player.getEyeLocation();
                Vector dir = eyeLoc.getDirection().normalize();
                Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                Location startLocation = eyeLoc.clone().add(right.multiply(0.3));

                if (!returning) {
                    Location currentLocation = startLocation.clone().add(dir.clone().multiply(step));
                    step += 1.5;

                    spawnLine(startLocation, currentLocation);
                    currentLocation.getWorld().spawnParticle(Particle.CRIT, currentLocation, 20, 0.5, 0.5, 0.5, 0.1);

                    if (!currentLocation.getBlock().isPassable()) {
                        cancel();
                        return;
                    }

                    for (Entity ent : currentLocation.getNearbyEntities(1, 1, 1)) {
                        if (ent instanceof LivingEntity && !ent.equals(player)) {
                            hookedEntity = ent;
                            returning = true;
                            break;
                        }
                    }

                    if (step > 20) cancel();

                } else {
                    if (hookedEntity == null || !hookedEntity.isValid()) {
                        cancel();
                        return;
                    }

                    Vector pullVec = playerFeet.toVector().subtract(hookedEntity.getLocation().toVector()).normalize().multiply(0.7);
                    hookedEntity.setVelocity(pullVec);
                    hookedEntity.getWorld().spawnParticle(Particle.CRIT, hookedEntity.getLocation(), 10, 0.2, 0.2, 0.2, 0);

                    step -= 0.5;
                    spawnLine(hookedEntity.getLocation(), playerFeet);

                    if (step <= 0) cancel();
                }
            }

            private void spawnLine(Location from, Location to) {
                Vector vec = to.toVector().subtract(from.toVector());
                double length = vec.length();
                Vector unit = vec.normalize();

                double gap = 0.3;
                int count = (int) (length / gap);

                for (int i = 0; i < count; i++) {
                    Location particleLoc = from.clone().add(unit.clone().multiply(i * gap));
                    particleLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        // Cooldown updater
        new BukkitRunnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - now;
                if (elapsed >= grapplingCooldownTimeMs) {
                    cooldownBar.removeAll();
                    grapplingCooldownBars.remove(playerId);

                    BossBar ready = Bukkit.createBossBar(
                            GREEN + "GRAPPLING READY",
                            BarColor.GREEN,
                            BarStyle.SOLID
                    );
                    ready.addPlayer(player);
                    grapplingReadyBars.put(playerId, ready);
                    cancel();
                    return;
                }

                double progress = 1.0 - ((double) elapsed / grapplingCooldownTimeMs);
                cooldownBar.setProgress(progress);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        boolean holdingGrappling = false;

        if (newItem != null && newItem.getType() == Material.TRIDENT && newItem.hasItemMeta()) {
            ItemMeta meta = newItem.getItemMeta();
            if (meta.getPersistentDataContainer().has(plugin.grapplingKey, PersistentDataType.BYTE)) {
                holdingGrappling = true;
            }
        }

        if (!holdingGrappling) {
            BossBar cooldownBar = grapplingCooldownBars.get(playerId);
            if (cooldownBar != null) cooldownBar.removePlayer(player);

            BossBar readyBar = grapplingReadyBars.get(playerId);
            if (readyBar != null) readyBar.removePlayer(player);
        } else {
            BossBar cooldownBar = grapplingCooldownBars.get(playerId);
            if (cooldownBar != null) cooldownBar.addPlayer(player);

            BossBar readyBar = grapplingReadyBars.get(playerId);
            if (readyBar != null) readyBar.addPlayer(player);
        }
    }

}
