package com.github.dreadslicer.tekkitrestrict;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.PluginManager;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Permission;
import com.griefcraft.model.Protection;

public class TRLWCProtect {
	/** @return False if the event was cancelled. */
	public static boolean checkLWCAllowed(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		// link up with LWC!
		if (Util.hasBypass(player, "lwc")) return true;
		
		Block block = event.getBlock();
		boolean istype = false;
		// tekkitrestrict.log.info(b.getTypeId()+":"+b.getData());
		for (int i = 0; i < TRConfigCache.LWC.blocked.size(); i++) {
			List<TRCacheItem> iss = TRCacheItem.processItemString("", TRConfigCache.LWC.blocked.get(i), -1);
			for (TRCacheItem ist : iss) {
				if (ist.compare(block.getTypeId(), block.getData())) {
					istype = true;
					i = TRConfigCache.LWC.blocked.size() + 1;
					break;
				}
			}
		}
		
		if (!istype) return true;
		
		if (TRConfigCache.LWC.lwcPlugin == null){
			PluginManager PM = tekkitrestrict.getInstance().getServer().getPluginManager();
			if (PM.isPluginEnabled("LWC")) TRConfigCache.LWC.lwcPlugin = (LWCPlugin) PM.getPlugin("LWC");
		}
		
		if (TRConfigCache.LWC.lwcPlugin == null) return true;
		
		LWC LWC = TRConfigCache.LWC.lwcPlugin.getLWC();
		String playername = player.getName().toLowerCase();
		for (BlockFace bf : BlockFace.values()) {
			Protection prot = LWC.getProtectionCache().getProtection(block.getRelative(bf));
			if (prot == null) continue;
			
			boolean hasAccess = false;

			for (Permission pe : prot.getPermissions()) {
				if (pe.getName().toLowerCase().equals(playername)){
					hasAccess = true;
					break;
				}
			}

			if (!prot.isOwner(player) && !hasAccess) {
				player.sendMessage("You are not allowed to place this here!");
				event.setCancelled(true);
				return false;
			}
		}
		return true;
	}
}