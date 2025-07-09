package com.wdpserver.wdpcustomitems;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GraplingHandeler implements Listener {

    private final Plugin plugin;

    public GraplingHandeler(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClickTrident(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != org.bukkit.Material.TRIDENT) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();
        Location startLocation = player.getEyeLocation();
        Vector direction = startLocation.getDirection().normalize();

        new BukkitRunnable() {
            Location currentLocation = startLocation.clone();
            boolean returning = false;
            Entity hookedEntity = null;
            double step = 0;

            @Override
            public void run() {
                Location playerEye = player.getEyeLocation();

                if (!returning) {
                    // Move forward
                    currentLocation = playerEye.clone().add(direction.clone().multiply(step));
                    step += 0.5;

                    // Spawn particles along line from player eye to currentLocation
                    spawnLine(playerEye, currentLocation);

                    // Check for entities near current location
                    for (Entity ent : currentLocation.getNearbyEntities(1, 1, 1)) {
                        if (ent instanceof LivingEntity && !ent.equals(player)) {
                            hookedEntity = ent;
                            returning = true;
                            break;
                        }
                    }

                    if (step > 20) { // Max range (10 blocks)
                        cancel();
                    }
                } else {
                    if (hookedEntity != null && hookedEntity.isValid()) {
                        // Pull entity toward player
                        Vector pullVec = playerEye.toVector().subtract(hookedEntity.getLocation().toVector()).normalize().multiply(0.7);
                        hookedEntity.setVelocity(pullVec);

                        // Spawn particles on hooked entity
                        hookedEntity.getWorld().spawnParticle(Particle.CRIT, hookedEntity.getLocation(), 10, 0.2, 0.2, 0.2, 0);
                    } else {
                        cancel();
                        return;
                    }

                    // Move beam back towards player
                    step -= 0.5;
                    currentLocation = playerEye.clone().add(direction.clone().multiply(step));

                    // Spawn particles along line from hooked entity to player eye
                    spawnLine(hookedEntity.getLocation(), playerEye);

                    if (step <= 0) {
                        cancel();
                    }
                }
            }

            // Spawn particles evenly along the line between two locations
            private void spawnLine(Location from, Location to) {
                Vector vec = to.toVector().subtract(from.toVector());
                double length = vec.length();
                Vector unit = vec.normalize();

                double gap = 0.3; // distance between particles
                int count = (int) (length / gap);

                for (int i = 0; i < count; i++) {
                    Location particleLoc = from.clone().add(unit.clone().multiply(i * gap));
                    particleLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    particleLoc.getWorld().spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0, 0, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
