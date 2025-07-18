package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowHandeler implements Listener {

    private final WdpCustomItems plugin;

    public ThrowHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (event.getHand() != EquipmentSlot.HAND) {
            player.sendMessage("No Throwstone in offhand");
            event.setCancelled(true);
            return;
        }

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.throwStoneKey, PersistentDataType.BYTE)) return;

        event.setCancelled(true);

        // Consume item
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        // Launch snowball
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setVelocity(player.getLocation().getDirection().normalize().multiply(1.5));

        // Tag the snowball
        snowball.getPersistentDataContainer().set(plugin.throwStoneKey, PersistentDataType.BYTE, (byte) 1);

        // Spawn ItemDisplay and attach
        ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(
                player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5)),
                EntityType.ITEM_DISPLAY
        );
        display.setItemStack(new ItemStack(Material.COBBLESTONE));
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(0);

        // Attach cobble to the snowball
        snowball.addPassenger(display);
        snowball.setInvisible(true);
        // Sound
        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1, 1);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;

        Snowball snowball = (Snowball) event.getEntity();

        if (!snowball.getPersistentDataContainer().has(plugin.throwStoneKey, PersistentDataType.BYTE)) return;

        // Damage entity if hit
        if (event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();
            target.damage(4.0, (Entity) snowball.getShooter());
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
        }

        // Remove cobblestone display
        for (Entity passenger : snowball.getPassengers()) {
            passenger.remove();
        }

        // Add particles
        snowball.getWorld().spawnParticle(Particle.CRIT, snowball.getLocation(), 10);
    }
}
