package com.wdpserver.wdpcustomitems;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

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

        Location initialLocation = player.getLocation().clone();

        new BukkitRunnable() {
            double step = 0;
            boolean returning = false;
            Entity hookedEntity = null;

            // Keep track of hooks to remove later
            Set<FishHook> activeHooks = new HashSet<>();

            @Override
            public void run() {
                // Cancel if player moves
                if (player.getLocation().distanceSquared(initialLocation) > 0.01) {
                    cleanupHooks();
                    cancel();
                    return;
                }

                Location playerFeet = player.getLocation().clone().subtract(0, 0.5, 0);
                Location startLocation = playerFeet.clone();

                if (!returning) {
                    Location currentLocation = startLocation.clone().add(direction.clone().multiply(step));
                    step += 0.5;

                    spawnLine(startLocation, currentLocation);

                    // Spawn fishing hook entity at beam end
                    spawnHook(currentLocation);

                    for (Entity ent : currentLocation.getNearbyEntities(1, 1, 1)) {
                        if (ent instanceof LivingEntity && !ent.equals(player)) {
                            hookedEntity = ent;
                            returning = true;
                            break;
                        }
                    }

                    if (step > 20) { // max range
                        cleanupHooks();
                        cancel();
                    }
                } else {
                    if (hookedEntity == null || !hookedEntity.isValid()) {
                        cleanupHooks();
                        cancel();
                        return;
                    }

                    Vector pullVec = playerFeet.toVector().subtract(hookedEntity.getLocation().toVector()).normalize().multiply(0.7);
                    hookedEntity.setVelocity(pullVec);
                    hookedEntity.getWorld().spawnParticle(Particle.CRIT, hookedEntity.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

                    step -= 0.5;
                    Location currentLocation = startLocation.clone().add(direction.clone().multiply(step));

                    spawnLine(hookedEntity.getLocation(), playerFeet);

                    // Spawn fishing hook at hooked entity position for visual
                    spawnHook(hookedEntity.getLocation());

                    if (step <= 0) {
                        cleanupHooks();
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

            private void spawnHook(Location loc) {
                // Remove old hooks before spawning new one so only one visible
                cleanupHooks();

                FishHook hook = player.launchProjectile(FishHook.class);
                hook.teleport(loc);
                activeHooks.add(hook);
            }

            private void cleanupHooks() {
                for (FishHook hook : activeHooks) {
                    if (!hook.isDead()) hook.remove();
                }
                activeHooks.clear();
            }

        }.runTaskTimer(plugin, 0L, 2L);
    }
}
