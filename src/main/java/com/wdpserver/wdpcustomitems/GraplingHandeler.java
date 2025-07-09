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
        Vector direction = player.getLocation().getDirection().normalize();

        // Save initial player location to detect movement
        Location initialLocation = player.getLocation().clone();

        new BukkitRunnable() {
            double step = 0;
            boolean returning = false;
            Entity hookedEntity = null;

            @Override
            public void run() {
                // Cancel if player moved more than 0.1 blocks away from initial location
                if (player.getLocation().distanceSquared(initialLocation) > 0.01) {
                    cancel();
                    return;
                }

                Location playerFeet = player.getEyeLocation().clone().subtract(0, 0.3, 0);
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection().normalize();

                // Calculate right vector perpendicular to look direction and up vector
                Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

                // Offset the eye location a bit to the right (e.g. 0.3 blocks)
                Location startLocation = eyeLoc.clone().add(right.multiply(0.3));


                if (!returning) {
                    Location currentLocation = startLocation.clone().add(direction.clone().multiply(step));
                    step += 1.5;

                    spawnLine(startLocation, currentLocation);
                    // currentLocation = the moving beam tip location
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

                    if (step > 20) {
                        cancel();
                    }
                } else {
                    if (hookedEntity == null || !hookedEntity.isValid()) {
                        cancel();
                        return;
                    }

                    Vector pullVec = playerFeet.toVector().subtract(hookedEntity.getLocation().toVector()).normalize().multiply(0.7);
                    hookedEntity.setVelocity(pullVec);
                    hookedEntity.getWorld().spawnParticle(Particle.CRIT, hookedEntity.getLocation(), 10, 0.2, 0.2, 0.2, 0);

                    step -= 0.5;
                    Location currentLocation = startLocation.clone().add(direction.clone().multiply(step));

                    spawnLine(hookedEntity.getLocation(), playerFeet);

                    if (step <= 0) {
                        cancel();
                    }
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
    }
}
