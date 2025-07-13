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
    private final double grapplingRange;

    // Cooldown state
    private final Map<UUID, Long> cooldownEndTimes = new HashMap<>();
    private final Map<UUID, BossBar> cooldownBars = new HashMap<>();
    private final Map<UUID, BossBar> readyBars = new HashMap<>();

    public GraplingHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
        this.grapplingRange = plugin.getConfig().getDouble("grappling-hook.range", 30.0);
    }

    @EventHandler
    public void onRightClickTrident(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TRIDENT) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(plugin.grapplingKey, PersistentDataType.BYTE)) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Prevent duplicate grappling
        if (plugin.hasGrappling.getOrDefault(playerId, false)) {
            player.sendMessage(RED + "You are already using the grappling hook.");
            return;
        }
        plugin.hasGrappling.put(playerId, true);

        // Remove ready bar if shown
        BossBar readyBar = readyBars.remove(playerId);
        if (readyBar != null) readyBar.removeAll();

        long now = System.currentTimeMillis();

        // Check cooldown
        if (cooldownEndTimes.containsKey(playerId)) {
            long endTime = cooldownEndTimes.get(playerId);
            if (now < endTime) {
                long secondsLeft = (endTime - now) / 1000 + 1;

                BossBar cooldownBar = cooldownBars.computeIfAbsent(playerId, id ->
                        Bukkit.createBossBar(
                                RED + "GRAPPLING COOLDOWN",
                                BarColor.RED,
                                BarStyle.SEGMENTED_10
                        )
                );
                cooldownBar.addPlayer(player);

                player.sendMessage(RED + "Grappling Hook Cooldown: " + secondsLeft + "s remaining.");
                plugin.hasGrappling.remove(playerId);
                return;
            } else {
                cooldownEndTimes.remove(playerId);
            }
        }

        Location playerStart = player.getLocation().clone();
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Location hookStart = eyeLoc.clone().add(right.multiply(0.3));
        Location pullTarget = eyeLoc.clone().subtract(0, 0.3, 0);

        new BukkitRunnable() {
            double step = 0;
            boolean returning = false;
            int returnTicks = 0;
            Entity hookedEntity = null;

            @Override
            public void run() {
                // Cancel if player moved
                if (player.getLocation().distanceSquared(playerStart) > 0.01) {
                    player.sendMessage(RED + "You moved, grappling hook canceled.");
                    finish(false);
                    cancel();
                    return;
                }

                if (!returning) {
                    // Extending the hook
                    Location current = hookStart.clone().add(direction.clone().multiply(step));
                    step += 1.5;

                    spawnLine(hookStart, current);
                    current.getWorld().spawnParticle(Particle.CRIT, current, 20, 0.5, 0.5, 0.5, 0.1);

                    if (!current.getBlock().isPassable()) {
                        finish(false);
                        cancel();
                        return;
                    }
                    if (step > grapplingRange) {
                        finish(false);
                        cancel();
                        return;
                    }

                    for (Entity ent : current.getNearbyEntities(1, 1, 1)) {
                        if (ent instanceof LivingEntity && !ent.equals(player)) {
                            hookedEntity = ent;
                            returning = true;
                            break;
                        }
                    }

                } else {
                    // Pulling back
                    returnTicks++;
                    if (hookedEntity == null || !hookedEntity.isValid() || returnTicks > 100) {
                        finish(hookedEntity != null);
                        cancel();
                        return;
                    }
                    if (hookedEntity.getLocation().distance(pullTarget) <= 3.0) {
                        finish(true);
                        cancel();
                        return;
                    }

                    Vector pull = pullTarget.toVector().subtract(hookedEntity.getLocation().toVector())
                            .normalize().multiply(0.7);
                    hookedEntity.setVelocity(pull);

                    hookedEntity.getWorld().spawnParticle(Particle.CRIT, hookedEntity.getLocation(), 10, 0.2, 0.2, 0.2, 0);
                    spawnLine(hookedEntity.getLocation(), pullTarget);
                }
            }

            private void finish(boolean grabbed) {
                plugin.hasGrappling.remove(playerId);

                long cooldown = grabbed ? plugin.longCooldownTimeMsGrappling : plugin.shortCooldownTimeMs;
                long endTime = System.currentTimeMillis() + cooldown;
                cooldownEndTimes.put(playerId, endTime);

                BossBar cooldownBar = cooldownBars.computeIfAbsent(playerId, id ->
                        Bukkit.createBossBar(
                                RED + "GRAPPLING COOLDOWN",
                                BarColor.RED,
                                BarStyle.SEGMENTED_10
                        )
                );
                cooldownBar.addPlayer(player);

                final BossBar barRef = cooldownBar;
                long start = System.currentTimeMillis();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        long elapsed = System.currentTimeMillis() - start;
                        if (elapsed >= cooldown) {
                            barRef.removeAll();
                            cooldownBars.remove(playerId);

                            BossBar ready = Bukkit.createBossBar(
                                    GREEN + "GRAPPLING READY",
                                    BarColor.GREEN,
                                    BarStyle.SOLID
                            );
                            ready.addPlayer(player);
                            readyBars.put(playerId, ready);
                            cancel();
                            return;
                        }
                        double progress = 1.0 - ((double) elapsed / cooldown);
                        barRef.setProgress(progress);
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }

            private void spawnLine(Location from, Location to) {
                Vector vec = to.toVector().subtract(from.toVector());
                double length = vec.length();
                if (length == 0) return;

                Vector stepVec = vec.normalize().multiply(0.3);
                int steps = (int) (length / 0.3);

                for (int i = 0; i < steps; i++) {
                    Location loc = from.clone().add(stepVec.clone().multiply(i));
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        boolean holdingGrappling = false;

        if (item != null && item.getType() == Material.TRIDENT && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(plugin.grapplingKey, PersistentDataType.BYTE)) {
                holdingGrappling = true;
            }
        }

        BossBar cooldownBar = cooldownBars.get(playerId);
        BossBar readyBar = readyBars.get(playerId);
        long now = System.currentTimeMillis();

        if (holdingGrappling) {
            if (cooldownEndTimes.getOrDefault(playerId, 0L) > now) {
                if (cooldownBar != null) cooldownBar.addPlayer(player);
            } else {
                if (readyBar != null) readyBar.addPlayer(player);
            }
        } else {
            if (cooldownBar != null) cooldownBar.removePlayer(player);
            if (readyBar != null) readyBar.removePlayer(player);
        }
    }
}
