package nl.taico.tekkitrestrict.listeners;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import nl.taico.tekkitrestrict.tekkitrestrict;
import nl.taico.tekkitrestrict.TRConfigCache.Hacks;
import nl.taico.tekkitrestrict.functions.TRNoHack;
import nl.taico.tekkitrestrict.objects.TREnums.HackType;

public class NoHackSpeed implements Listener{
	private static ConcurrentHashMap<String, Double[]> tickLastLoc = new ConcurrentHashMap<String, Double[]>();
	private static ConcurrentHashMap<String, Integer> tickTolerance = new ConcurrentHashMap<String, Integer>();
	private static int maxmove = 15 + 1; // above this value, players will be kicked anyways.
	
	@EventHandler(priority = EventPriority.MONITOR)
	void handleMove(PlayerMoveEvent e) {
		final Player player = e.getPlayer();
		if (player.hasPermission("tekkitrestrict.bypass.hack.speed")) return;
		
		// determine position of player to get velocity.
		final Location loc = player.getLocation();
		final String name = player.getName();
		Bukkit.getScheduler().scheduleAsyncDelayedTask(tekkitrestrict.getInstance(), new Runnable(){
			public void run(){
				double xNew = loc.getX(), zNew = loc.getZ();
				
				// determine XZ velocity
				
				Double[] XZ = tickLastLoc.get(name);
				if (XZ != null) {
					final double xOld = XZ[0], zOld = XZ[1];
					
					double xe = xOld - xNew;
					double ze = zOld - zNew;
					double velo = Math.sqrt(xe*xe+ze*ze);

					if (velo >= Hacks.speed.value && velo <= maxmove) {
						Integer oldValue = tickTolerance.get(name);
						if (oldValue == null) tickTolerance.put(name, 1);
						else {
							if ((oldValue + 1) > Hacks.speed.tolerance) {
								Bukkit.getScheduler().scheduleSyncDelayedTask(tekkitrestrict.getInstance(), new Runnable(){
									public void run(){
										TRNoHack.handleHack(player, HackType.speed);
										player.teleport(player.getWorld().getHighestBlockAt(new Double(xOld).intValue(), new Double(zOld).intValue()).getLocation());
									}
								});
								
								tickTolerance.remove(name);
								
							} else
								tickTolerance.put(name, oldValue + 1);
						}
					} else {
						tickTolerance.remove(name);
					}
				}
				tickLastLoc.put(name, new Double[] { xNew, zNew });
			}
		});
		
	}

	public static void clearMaps() {
		// flushes the fly locator map.
		tickLastLoc.clear();
	}

	public static void playerLogout(String playerName) {
		tickLastLoc.remove(playerName);
	}
}
