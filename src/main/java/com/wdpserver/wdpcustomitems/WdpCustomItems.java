package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WdpCustomItems extends JavaPlugin {

    public NamespacedKey swordKey;
    public NamespacedKey boltKey;
    public NamespacedKey damageKey;
    public NamespacedKey colorKey;
    public NamespacedKey knockbackKey;

    public final Map<UUID, Long> cooldowns = new HashMap<>();
    public final Map<UUID, BossBar> cooldownBars = new HashMap<>();
    public final Map<UUID, BossBar> readyBars = new HashMap<>();

    public long cooldownTimeMs;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Namespaced keys
        swordKey = new NamespacedKey(this, "plugin_sword");
        boltKey = new NamespacedKey(this, "boltkey");
        damageKey = new NamespacedKey(this, "beam_damage");
        colorKey = new NamespacedKey(this, "beam_color");
        knockbackKey = new NamespacedKey(this, "beam_knockback");

        cooldownTimeMs = (long) (getConfig().getDouble("cooldown", 5.0) * 1000);

        // Register the recipe here
        registerBoltRecipe();

        getServer().getPluginManager().registerEvents(new BeamHandler(this), this);

        Objects.requireNonNull(getCommand("givesword")).setExecutor(new GiveSwordCommand(this));
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor((sender, command, label, args) -> {
            reloadConfig();
            sender.sendMessage("§aConfig reloaded.");
            return true;
        });

        getLogger().info("WDP Custom Items started!");
    }


    @Override
    public void onDisable() {
        getLogger().info("WDP Custom Items disabled.");
    }

    private void registerBoltRecipe() {
        ItemStack speedStone = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = speedStone.getItemMeta();
        meta.setDisplayName("§bLightning Bolt");
        meta.getPersistentDataContainer().set(boltKey, PersistentDataType.BYTE, (byte) 1);
        meta.setLore(Collections.singletonList("With this in your inventory, the beam spawns lightning."));
        speedStone.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(boltKey, speedStone);
        recipe.shape(
                " E ",
                "ENE",
                " E "
        );
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('N', Material.DIAMOND);

        getServer().addRecipe(recipe);
    }

}
