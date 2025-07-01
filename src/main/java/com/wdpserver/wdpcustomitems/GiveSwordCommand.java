package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collections;

import static org.bukkit.ChatColor.*;

public class GiveSwordCommand implements CommandExecutor {

    private final com.wdpserver.wdpcustomitems.WdpCustomItems plugin;

    public GiveSwordCommand(com.wdpserver.wdpcustomitems.WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage(RED + "Usage: /givesword <damage> <color> <knockback>");
            return true;
        }

        try {
            int damage = Integer.parseInt(args[0]);
            String color = args[1].toUpperCase();
            double knockback = Double.parseDouble(args[2]);

            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.setDisplayName("§bCustom Beam Sword");
            meta.getPersistentDataContainer().set(plugin.swordKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(plugin.damageKey, PersistentDataType.INTEGER, damage);
            meta.getPersistentDataContainer().set(plugin.colorKey, PersistentDataType.STRING, color);
            meta.getPersistentDataContainer().set(plugin.knockbackKey, PersistentDataType.DOUBLE, knockback);
            meta.setLore(Arrays.asList(
                    "Damage: " + damage,
                    "Color: " + color,
                    "Knockback: " + knockback
            ));

            sword.setItemMeta(meta);

            player.getInventory().addItem(sword);
            player.sendMessage(GREEN + "Given a custom beam sword!");

        } catch (Exception e) {
            player.sendMessage(RED + "Invalid arguments.");
        }

        return true;
    }
}
