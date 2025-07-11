package com.wdpserver.wdpcustomitems;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class RecipeCommand implements CommandExecutor, Listener {

    private final RecipeManager recipeManager;

    public RecipeCommand(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "Available Recipes:");
            for (Map.Entry<NamespacedKey, RecipeManager.RecipeInfo> entry : recipeManager.getRecipes().entrySet()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + entry.getValue().displayName());
            }
            sender.sendMessage(ChatColor.GRAY + "Use /recipes <name> to see details.");
            return true;
        }

        String input = String.join(" ", args).toLowerCase();

        RecipeManager.RecipeInfo selectedRecipe = null;
        for (RecipeManager.RecipeInfo recipeInfo : recipeManager.getRecipes().values()) {
            if (recipeInfo.displayName().toLowerCase().equals(input)) {
                selectedRecipe = recipeInfo;
                break;
            }
        }

        if (selectedRecipe == null) {
            sender.sendMessage(ChatColor.RED + "Recipe not found: " + input);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        showRecipeInGUI(player, selectedRecipe);
        return true;
    }

    private void showRecipeInGUI(Player player, RecipeManager.RecipeInfo recipeInfo) {
        ShapedRecipe shaped = recipeInfo.recipe();
        String[] shape = shaped.getShape();
        Map<Character, RecipeChoice> ingredientMap = shaped.getChoiceMap();

        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.YELLOW + "Recipe: " + recipeInfo.displayName());

        for (int row = 0; row < shape.length; row++) {
            String line = shape[row];
            for (int col = 0; col < line.length(); col++) {
                char key = line.charAt(col);
                if (key == ' ') continue;  // skip empty spots

                int slot = row * 9 + (col + 1);  // row * 9 + col to place in correct GUI slot

                RecipeChoice choice = ingredientMap.get(key);
                if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
                    if (!materialChoice.getChoices().isEmpty()) {
                        Material mat = materialChoice.getChoices().get(0);
                        gui.setItem(slot, new ItemStack(mat));
                    }
                } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                    if (!exactChoice.getChoices().isEmpty()) {
                        ItemStack item = exactChoice.getChoices().get(0).clone();
                        gui.setItem(slot, item);
                    }
                }
            }
        }

        ItemStack arrowItem = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = arrowItem.getItemMeta();
        meta.setItemModel(new NamespacedKey("wdpserver","arrow"));
        meta.setDisplayName(ChatColor.GREEN + "→");  // Right arrow symbol
        arrowItem.setItemMeta(meta);

        // Put the result item in slot 22 (middle-right)
        ItemStack result = shaped.getResult().clone();
        gui.setItem(16, result);
        gui.setItem(14, arrowItem);


        player.openInventory(gui);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null || event.getView().getTitle() == null) return;

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.startsWith("Recipe:")) {
            event.setCancelled(true);
        }
    }
}
