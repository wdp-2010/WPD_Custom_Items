package com.wdpserver.wdpcustomitems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WdpCustomItems extends JavaPlugin {
    public NamespacedKey diaBeamSwordKey;
    public NamespacedKey boltKey;
    public NamespacedKey beamDamageKey;
    public NamespacedKey beamColorKey;
    public NamespacedKey beamKnockbackKey;
    public NamespacedKey beamStoneKey;
    public NamespacedKey throwStoneKey;
    public NamespacedKey jumpBootsKey;
    public NamespacedKey grapplingKey;
    public NamespacedKey beamRangeKey;
    public NamespacedKey beamCooldownKey;

    public final Map<UUID, Boolean> hasBeam = new HashMap<>();
    public final Map<UUID, Long> cooldowns = new HashMap<>();
    public final Map<UUID, BossBar> cooldownBars = new HashMap<>();
    public final Map<UUID, BossBar> readyBars = new HashMap<>();

    public Map<UUID, Long> beamCooldownDurations = new HashMap<>();
    public Map<UUID, Boolean> hasGrappling = new HashMap<>();

    public long longCooldownTimeMs;
    public long shortCooldownTimeMs;
    public long longCooldownTimeMsGrappling;

    private RecipeManager recipeManager;


    @Override
    public void onEnable() {
        ShapelessRecipe recolorRecipe = new ShapelessRecipe(
                new NamespacedKey(this, "recolor_red"),
                new ItemStack(Material.DIAMOND_SWORD)
        );

        this.recipeManager = new RecipeManager();

        RecipeCommand recipeCommand = new RecipeCommand(recipeManager);

        recolorRecipe.addIngredient(Material.DIAMOND_SWORD);
        recolorRecipe.addIngredient(Material.RED_DYE);
        getServer().addRecipe(recolorRecipe);

        saveDefaultConfig();

        // Namespaced keys
        diaBeamSwordKey = new NamespacedKey(this, "plugin_sword");
        boltKey = new NamespacedKey(this, "boltkey");
        beamDamageKey = new NamespacedKey(this, "beam_damage");
        beamColorKey = new NamespacedKey(this, "beam_color");
        beamKnockbackKey = new NamespacedKey(this, "beam_knockback");
        beamCooldownKey = new NamespacedKey(this, "beamdefcooldown");
        beamRangeKey= new NamespacedKey(this, "beamrange");
        beamStoneKey = new NamespacedKey(this, "beamstone");
        throwStoneKey = new NamespacedKey(this, "throwstone");
        jumpBootsKey = new NamespacedKey(this, "doublejumpboots");
        grapplingKey = new NamespacedKey(this, "grapplinghook");

        longCooldownTimeMs = (long) (getConfig().getDouble("beam-sword.cooldown", 5.0) * 1000);
        shortCooldownTimeMs = (long) (getConfig().getDouble("beam-sword.short-cooldown", 2.0) * 1000);
        longCooldownTimeMsGrappling = (long) (getConfig().getDouble("grappling-hook.cooldown", 5.0) * 1000);

        // Register the recipe here
        registerBoltRecipe();
        registerDiaBeamSwordRecipe();
        registerBeamStoneRecipe();
        registerThrowStoneRecipe();
        registerJumpBootsRecipe();
        registerGrapplingHookRecipe();
        registerNetheriteBeamSwordUpgradeRecipe();

        getServer().getPluginManager().registerEvents(new BeamHandler(this), this);
        getServer().getPluginManager().registerEvents(new RecolorCraftHandler(this), this);
        getServer().getPluginManager().registerEvents(new ThrowHandeler(this), this);
        getServer().getPluginManager().registerEvents(new DoubleJumpHandler(this), this);
        getServer().getPluginManager().registerEvents(new GraplingHandeler(this), this);
        getServer().getPluginManager().registerEvents(recipeCommand, this);


        Objects.requireNonNull(getCommand("givesword")).setExecutor(new GiveSwordCommand(this));
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor((sender, command, label, args) -> {
            reloadConfig();
            sender.sendMessage("§aConfig reloaded.");
            return true;
        });
        getCommand("recipes").setExecutor(recipeCommand);

        getLogger().info("WDP Custom Items started!");
    }


    @Override
    public void onDisable() {
        getLogger().info("WDP Custom Items disabled.");
    }

    private boolean isBeamSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(diaBeamSwordKey, PersistentDataType.BYTE);
    }

    public ItemStack createCustomBeamSword(int damage, String color, double knockback, long cooldown, double range, Material material) {
        ItemStack sword = new ItemStack(material);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName("§bBeam Sword");
        meta.getPersistentDataContainer().set(diaBeamSwordKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(beamDamageKey, PersistentDataType.INTEGER, damage);
        meta.getPersistentDataContainer().set(beamColorKey, PersistentDataType.STRING, color);
        meta.getPersistentDataContainer().set(beamKnockbackKey, PersistentDataType.DOUBLE, knockback);
        meta.getPersistentDataContainer().set(beamRangeKey, PersistentDataType.DOUBLE, range);
        meta.getPersistentDataContainer().set(beamCooldownKey, PersistentDataType.LONG, cooldown);

        meta.setItemModel(new NamespacedKey("wdpserver","beam_sword"));

        meta.setLore(Arrays.asList(
                "§fBeam:",
                "§7Damage: §f" + damage,
                "§7Color: §f" + color,
                "§7Knockback: §f" + knockback,
                "§7Cooldown: §f" + cooldown + " s",
                "§7Range: §f" + range + " Blocks"
        ));

        sword.setItemMeta(meta);
        return sword;
    }
    public ItemStack createThrowRock() {
        ItemStack ThrowRock = new ItemStack(Material.COBBLESTONE);
        ItemMeta meta = ThrowRock.getItemMeta();

        meta.setMaxStackSize(32);
        meta.setDisplayName("§bThrowable Stone");
        meta.setLore(Collections.singletonList("§fRight click to trow"));
        meta.getPersistentDataContainer().set(throwStoneKey, PersistentDataType.BYTE, (byte) 1);

        ThrowRock.setItemMeta(meta);
        return ThrowRock;

    }
    public ItemStack createBeamStone() {
        ItemStack beamStone = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = beamStone.getItemMeta();

        meta.setDisplayName("§bBeam Stone");
        meta.getPersistentDataContainer().set(beamStoneKey, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(new NamespacedKey("wdpserver","beam_stone"));

        beamStone.setItemMeta(meta);
        return beamStone;

    }
    public ItemStack createJumpBoots() {
        ItemStack jumpBoots = new ItemStack(Material.IRON_BOOTS);
        ItemMeta meta = jumpBoots.getItemMeta();

        meta.getPersistentDataContainer().set(jumpBootsKey, PersistentDataType.BYTE, (byte) 1);
        meta.setDisplayName("§bDouble Jump Boots");
        meta.setLore(Arrays.asList("§fJump once on air","§fpress space twice to duoule jump"));

        jumpBoots.setItemMeta(meta);
        return jumpBoots;
    }
    public ItemStack createGrapplingHook() {
        ItemStack jumpBoots = new ItemStack(Material.TRIDENT);
        ItemMeta meta = jumpBoots.getItemMeta();

        meta.getPersistentDataContainer().set(grapplingKey, PersistentDataType.BYTE, (byte) 1);
        meta.setDisplayName("§bGrappling Hook");
        meta.setLore(Collections.singletonList("§fGrab an entity"));

        jumpBoots.setItemMeta(meta);
        return jumpBoots;
    }


    private void registerBoltRecipe() {
        ItemStack bolt = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = bolt.getItemMeta();

        NamespacedKey key = this.boltKey;

        meta.setDisplayName("§bLightning Bolt");
        meta.getPersistentDataContainer().set(boltKey, PersistentDataType.BYTE, (byte) 1);
        meta.setLore(Collections.singletonList("§fWith this in your inventory, the Epic Sword spawn lightning."));
        meta.setItemModel(new NamespacedKey("wdpserver","bolt"));

        bolt.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(key, bolt);
        recipe.shape(
                "  D" ,
                " N ",
                "D  "
        );
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHER_STAR);

        recipeManager.registerRecipe("Bolt Stone", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerDiaBeamSwordRecipe() {
        int damage = getConfig().getInt("diamond-beam-sword.damage", 12);
        String color = getConfig().getString("diamond-beam-sword.color", "RED");
        int knockback = getConfig().getInt("diamond-beam-sword.knockback", 3);
        long cooldown = getConfig().getLong("diamond-beam-sword.cooldown", 5);
        double range = getConfig().getDouble("diamond-beam-sword.range", 50);

        NamespacedKey key = this.diaBeamSwordKey;

        ItemStack result = createCustomBeamSword(damage, color, knockback, cooldown, range, Material.DIAMOND_SWORD);  //  config
        ItemStack beamStone = createBeamStone();
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Define shape: example shape, you can adjust (DONE)
        recipe.shape(
                " N ",
                "DSD",
                " D "
        );

        recipe.setIngredient('N', new RecipeChoice.ExactChoice(beamStone));
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        recipeManager.registerRecipe("Beam Sword", key, recipe);

        getServer().addRecipe(recipe);
    }

    public void registerNetheriteBeamSwordUpgradeRecipe() {
        // Config values for the upgraded sword
        int damage = getConfig().getInt("netherite-beam-sword.damage", 20);
        String color = getConfig().getString("netherite-beam-sword.color", "DARK_PURPLE");
        double knockback = getConfig().getDouble("netherite-beam-sword.knockback", 5.0);
        long cooldown = getConfig().getLong("netherite-beam-sword.cooldown", 4);
        double range = getConfig().getDouble("netherite-beam-sword.range", 60.0);

        // Unique key for the recipe
        NamespacedKey key = new NamespacedKey(this, "netherite_beam_sword_upgrade");

        // 1. Create the diamond beam sword for matching (must match exactly)
        ItemStack baseItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta baseMeta = baseItem.getItemMeta();
        if (baseMeta != null) {
            baseMeta.getPersistentDataContainer().set(diaBeamSwordKey, PersistentDataType.BYTE, (byte) 1);
            baseItem.setItemMeta(baseMeta);
        }

        // 2. Create the resulting netherite beam sword
        ItemStack result = createCustomBeamSword(damage, color, knockback, cooldown, range, Material.NETHERITE_SWORD);

        // 3. Build smithing transform recipe
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                key,
                result,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.ExactChoice(baseItem), // Requires exact PDC
                new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT)
        );

        // 4. Register the recipe with the server
        Bukkit.addRecipe(recipe);

        getLogger().info("✅ Registered Netherite Beam Sword upgrade recipe");
    }


    public void registerBeamStoneRecipe() {
        ItemStack result = createBeamStone();

        NamespacedKey key = this.beamStoneKey;

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Define shape: example shape, you can adjust (DONE)
        recipe.shape(
                " N ",
                "DFD",
                " D "
        );

        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('F', Material.SUNFLOWER);

        recipeManager.registerRecipe("Beam Stone", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerThrowStoneRecipe() {
        ItemStack result = createThrowRock();
        result.setAmount(9);

        NamespacedKey key = this.throwStoneKey;

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                "CCC",
                "CCC",
                "CCC"
        );

        recipe.setIngredient('C', Material.COBBLESTONE);

        recipeManager.registerRecipe("Throw Stone", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerJumpBootsRecipe() {
        ItemStack result = createJumpBoots();

        NamespacedKey key = this.jumpBootsKey;

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                "   ",
                "I I",
                "W W"
        );

        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('W', Material.WIND_CHARGE);

        recipeManager.registerRecipe("Double Jump Boots", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerGrapplingHookRecipe() {
        ItemStack result = createGrapplingHook();
        NamespacedKey key = this.grapplingKey;
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                " T ",
                " F ",
                "   "
        );

        recipe.setIngredient('T', Material.TRIDENT);
        recipe.setIngredient('F', Material.FISHING_ROD);

        recipeManager.registerRecipe("Grappling Hook", key, recipe);

        getServer().addRecipe(recipe);
    }



}
