package com.wdpserver.wdpcustomitems;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ThrowHandeler implements Listener {
    private final WdpCustomItems plugin;

    public ThrowHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

    }
}

