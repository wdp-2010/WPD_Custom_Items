package com.wdpserver.wdpcustomitems;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.ChatColor.*;

public class BeamHandler implements Listener {

    private final WdpCustomItems plugin;

    public BeamHandler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.DIAMOND_SWORD || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.swordKey, PersistentDataType.BYTE)) return;

        BossBar readyBar = plugin.readyBars.remove(playerId);
        if (readyBar != null) readyBar.removeAll();

        long now = System.currentTimeMillis();
        if (plugin.cooldowns.containsKey(playerId)) {
            long last = plugin.cooldowns.get(playerId);
            long elapsed = now - last;
            if (elapsed < plugin.cooldownTimeMs) {
                long secondsLeft = (plugin.cooldownTimeMs - elapsed) / 1000;

                BossBar cooldownBar = plugin.cooldownBars.get(playerId);
                if (cooldownBar != null && !cooldownBar.getPlayers().contains(player)) {
                    cooldownBar.addPlayer(player);
                }

                player.sendMessage(RED + "Cooldown: " + secondsLeft + "s remaining.");
                return;
            }
        }

        plugin.cooldowns.put(playerId, now);

        BossBar cooldownBar = Bukkit.createBossBar(
                RED + "BEAM COOLDOWN",
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );
        plugin.cooldownBars.put(playerId, cooldownBar);
        cooldownBar.addPlayer(player);

        BeamSword beamSword = new BeamSword(item, plugin);

        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        int particleSpeed = plugin.getConfig().getInt("beam-speed", 1);
        double maxDistance = plugin.getConfig().getDouble("range", 50.0);

        boolean hasBolt = Arrays.stream(player.getInventory().getContents()).anyMatch(this::isBolt);

        int particles = 100;
        double step = maxDistance / particles;

        new BukkitRunnable() {
            int i = 0;
            boolean hitBlock = false;
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                if (i >= particles || hitBlock) {
                    cancel();
                    return;
                }

                double distance = i * step;
                Location point = start.clone().add(direction.clone().multiply(distance));

                for (Entity e : point.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                    if (e instanceof LivingEntity && e != player && hitEntities.add(e.getUniqueId())) {
                        LivingEntity target = (LivingEntity) e;
                        target.setVelocity(direction.clone().multiply(beamSword.knockback));
                        target.damage(beamSword.damage, player);
                        target.setNoDamageTicks(5);
                        if (hasBolt) {
                            target.getWorld().strikeLightning(target.getLocation());
                        }
                    }
                }

                if (point.getBlock().getType().isSolid()) {
                    hitBlock = true;
                    cancel();
                    return;
                }

                player.getWorld().spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        new Particle.DustOptions(beamSword.color, 1)
                );
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        new Particle.DustOptions(beamSword.color, 1)
                );

                i++;
            }
        }.runTaskTimer(plugin, 0L, particleSpeed);

        new BukkitRunnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - now;
                if (elapsed >= plugin.cooldownTimeMs) {
                    cooldownBar.removeAll();
                    plugin.cooldownBars.remove(playerId);

                    BossBar ready = Bukkit.createBossBar(
                            GREEN + "BEAM READY",
                            BarColor.GREEN,
                            BarStyle.SOLID
                    );
                    ready.addPlayer(player);
                    plugin.readyBars.put(playerId, ready);
                    cancel();
                    return;
                }
                double progress = 1.0 - ((double) elapsed / plugin.cooldownTimeMs);
                cooldownBar.setProgress(progress);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        boolean holdingSword = false;

        if (newItem != null && newItem.getType() == Material.DIAMOND_SWORD && newItem.hasItemMeta()) {
            ItemMeta meta = newItem.getItemMeta();
            if (meta.getPersistentDataContainer().has(plugin.swordKey, PersistentDataType.BYTE)) {
                holdingSword = true;
            }
        }

        if (!holdingSword) {
            BossBar cooldownBar = plugin.cooldownBars.get(playerId);
            if (cooldownBar != null) cooldownBar.removePlayer(player);

            BossBar readyBar = plugin.readyBars.get(playerId);
            if (readyBar != null) readyBar.removePlayer(player);
        } else {
            BossBar cooldownBar = plugin.cooldownBars.get(playerId);
            if (cooldownBar != null) cooldownBar.addPlayer(player);

            BossBar readyBar = plugin.readyBars.get(playerId);
            if (readyBar != null) readyBar.addPlayer(player);
        }
    }

    private boolean isBolt(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        Byte tag = meta.getPersistentDataContainer().get(plugin.boltKey, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }
}
