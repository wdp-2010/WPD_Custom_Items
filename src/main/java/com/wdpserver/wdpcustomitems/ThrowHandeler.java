package com.wdpserver.wdpcustomitems;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class ThrowHandeler implements Listener {
    private final WdpCustomItems plugin;

    public ThrowHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            ItemMeta meta = item.getItemMeta();

            if (item == null) return;

            if (!item.hasItemMeta()) return ;

            if (meta.getPersistentDataContainer().has(plugin.throwStoneKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);

                item.setAmount(item.getAmount() - 1);

                ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5)),EntityType.ITEM_DISPLAY);

                display.setItemStack(new ItemStack(Material.COBBLESTONE));
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(0);

                display.getPersistentDataContainer().set(plugin.throwStoneKey, PersistentDataType.BYTE, (byte)1);

                Vector velocity = player.getLocation().getDirection().normalize().multiply(1.5);
                display.setVelocity(velocity);

                player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1, 1);

                BukkitScheduler scheduler = Bukkit.getScheduler();

                // After spawning the display:
                scheduler.runTaskTimer(plugin, new Runnable() {
                    int ticksLived = 0;
                    ItemDisplay displayRef = display;
                    Vector velocityRef = velocity;

                    @Override
                    public void run() {
                        if (displayRef.isDead() || !displayRef.isValid()) {
                            cancel();
                            return;
                        }

                        // Move it manually
                        Location currentLoc = displayRef.getLocation();
                        currentLoc.add(velocityRef);
                        displayRef.teleport(currentLoc);

                        // Detect nearby entities to "hit"
                        for (Entity nearby : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
                            if (nearby instanceof LivingEntity && !(nearby instanceof Player && ((Player)nearby).getUniqueId().equals(player.getUniqueId()))) {
                                LivingEntity target = (LivingEntity) nearby;
                                target.damage(4.0, player);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);

                                // Remove display
                                displayRef.remove();
                                cancel();
                                return;
                            }
                        }

                        // Remove after 3 seconds
                        ticksLived++;
                        if (ticksLived > 60) {
                            displayRef.remove();
                            cancel();
                        }
                    }

                    private void cancel() {
                        // This method is a placeholder to exit the task
                        // In actual code, you call BukkitRunnable.cancel()
                    }
                }, 0L, 1L);

            }
        }
    }
}

