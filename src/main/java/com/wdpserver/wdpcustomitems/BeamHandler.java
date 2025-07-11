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

    private final Map<UUID, Long> beamCooldowns = new HashMap<>();
    private final Map<UUID, BossBar> beamCooldownBars = new HashMap<>();
    private final Map<UUID, BossBar> beamReadyBars = new HashMap<>();

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
        if (!meta.getPersistentDataContainer().has(plugin.beamSwordKey, PersistentDataType.BYTE)) return;

        if (plugin.hasBeam.containsKey(playerId)) {
            player.sendMessage(RED + "You already used the beam. Please wait.");
            return;
        }

        BossBar readyBar = beamReadyBars.remove(playerId);
        if (readyBar != null) readyBar.removeAll();

        long now = System.currentTimeMillis();
        long defaultCooldown = plugin.longCooldownTimeMs;

        // Check cooldown
        if (beamCooldowns.containsKey(playerId)) {
            long last = beamCooldowns.get(playerId);
            long elapsed = now - last;

            if (elapsed < defaultCooldown) {
                long secondsLeft = ((defaultCooldown - elapsed) / 1000) + 1;

                BossBar cooldownBar = beamCooldownBars.get(playerId);
                if (cooldownBar != null && !cooldownBar.getPlayers().contains(player)) {
                    cooldownBar.addPlayer(player);
                }

                player.sendMessage(RED + "Beam Cooldown: " + secondsLeft + "s remaining.");
                return;
            }
        }

        plugin.hasBeam.put(playerId, true);

        BeamSword beamSword = new BeamSword(item, plugin);

        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        int particleSpeed = plugin.getConfig().getInt("beam-speed", 1);
        double maxDistance = plugin.getConfig().getDouble("range", 50.0);

        boolean hasBolt = Arrays.stream(player.getInventory().getContents()).anyMatch(this::isBolt);

        int particles = 50;
        double step = maxDistance / particles;

        new BukkitRunnable() {
            int i = 0;
            boolean hitBlock = false;
            boolean hasHitEntity = false;
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                if (i >= particles || hitBlock) {
                    finishCooldown(hasHitEntity, player);
                    cancel();
                    return;
                }

                double distance = i * step;
                Location point = start.clone().add(direction.clone().multiply(distance));

                for (Entity e : point.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                    if (e instanceof LivingEntity && e != player && hitEntities.add(e.getUniqueId())) {
                        LivingEntity target = (LivingEntity) e;

                        if (hasBolt) {
                            target.getWorld().strikeLightning(target.getLocation());
                            target.setFireTicks(80);
                        } else {
                            target.setVelocity(direction.clone().multiply(beamSword.knockback));
                        }

                        target.damage(beamSword.damage, player);
                        hasHitEntity = true;
                    }
                }

                if (point.getBlock().getType().isSolid()) {
                    hitBlock = true;
                    if (!hasHitEntity) finishCooldown(false, player);
                }

                player.getWorld().spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        new Particle.DustOptions(beamSword.color, 1)
                );
                player.getWorld().spawnParticle(
                        Particle.CLOUD,
                        point,
                        3,
                        0.2, 0.2, 0.2,
                        0.01
                );

                i++;
            }
        }.runTaskTimer(plugin, 0L, particleSpeed);
    }

    private void finishCooldown(boolean hitEntity, Player player) {
        UUID playerId = player.getUniqueId();
        plugin.hasBeam.remove(playerId);

        long cooldown = hitEntity ? plugin.longCooldownTimeMs : plugin.shortCooldownTimeMs;
        beamCooldowns.put(playerId, System.currentTimeMillis());

        BossBar cooldownBar = Bukkit.createBossBar(
                RED + "BEAM COOLDOWN",
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );
        beamCooldownBars.put(playerId, cooldownBar);
        cooldownBar.addPlayer(player);

        long start = System.currentTimeMillis();

        new BukkitRunnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed >= cooldown) {
                    cooldownBar.removeAll();
                    beamCooldownBars.remove(playerId);

                    BossBar readyBar = Bukkit.createBossBar(
                            GREEN + "BEAM READY",
                            BarColor.GREEN,
                            BarStyle.SOLID
                    );
                    readyBar.addPlayer(player);
                    beamReadyBars.put(playerId, readyBar);
                    cancel();
                    return;
                }
                double progress = 1.0 - ((double) elapsed / cooldown);
                cooldownBar.setProgress(progress);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        boolean holdingBeamSword = false;

        if (newItem != null && newItem.getType() == Material.DIAMOND_SWORD && newItem.hasItemMeta()) {
            ItemMeta meta = newItem.getItemMeta();
            if (meta.getPersistentDataContainer().has(plugin.beamSwordKey, PersistentDataType.BYTE)) {
                holdingBeamSword = true;
            }
        }

        if (!holdingBeamSword) {
            BossBar cooldownBar = beamCooldownBars.get(playerId);
            if (cooldownBar != null) cooldownBar.removePlayer(player);

            BossBar readyBar = beamReadyBars.get(playerId);
            if (readyBar != null) readyBar.removePlayer(player);
        } else {
            BossBar cooldownBar = beamCooldownBars.get(playerId);
            if (cooldownBar != null) cooldownBar.addPlayer(player);

            BossBar readyBar = beamReadyBars.get(playerId);
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
