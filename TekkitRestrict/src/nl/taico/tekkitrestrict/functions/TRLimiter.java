package nl.taico.tekkitrestrict.functions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import nl.taico.tekkitrestrict.Log;
import nl.taico.tekkitrestrict.TRException;
import nl.taico.tekkitrestrict.TRItemProcessor;
import nl.taico.tekkitrestrict.TRListener;
import nl.taico.tekkitrestrict.TRPermHandler;
import nl.taico.tekkitrestrict.tekkitrestrict;
import nl.taico.tekkitrestrict.Log.Warning;
import nl.taico.tekkitrestrict.annotations.Safe;
import nl.taico.tekkitrestrict.objects.TRConfigLimit;
import nl.taico.tekkitrestrict.objects.TRItem;
import nl.taico.tekkitrestrict.objects.TRLimit;
import nl.taico.tekkitrestrict.objects.TRLocation;
import nl.taico.tekkitrestrict.objects.TRPermLimit;
import nl.taico.tekkitrestrict.objects.TREnums.ConfigFile;

public class TRLimiter {
	private int expire = -1;
	public String player = "";
	public boolean isModified = true; // default limiters will get saved.
	/** A list of different kinds of limited blocks and the locations where they are placed (For this player). */
	public List<TRLimit> itemlimits = Collections.synchronizedList(new LinkedList<TRLimit>());
	
	private static CopyOnWriteArrayList<TRLimiter> limiters = new CopyOnWriteArrayList<TRLimiter>();
	//private static List<TRLimiter> limiters = Collections.synchronizedList(new LinkedList<TRLimiter>());
	private static List<TRConfigLimit> configLimits = Collections.synchronizedList(new ArrayList<TRConfigLimit>());
	private static Map<String, String> allBlockOwners = Collections.synchronizedMap(new HashMap<String, String>());
	private static ConcurrentHashMap<String, List<TRPermLimit>> limiterPermCache = new ConcurrentHashMap<String, List<TRPermLimit>>();
	private static boolean changing = false;
	
	public static void reload() {
		//for (String str : limiterPermCache.keySet()){
		//	for (TRPermLimit t : limiterPermCache.get(str)){
		//		tekkitrestrict.log.info("[DEBUG] limiterPermCache.get("+str+"): " + t.id + ":" + t.data + " max="+t.max);
		//	}
		//}
		limiterPermCache.clear();
		
		List<String> limitedBlocks = tekkitrestrict.config.getStringList(ConfigFile.Advanced, "LimitBlocks");
		configLimits.clear();
		for (String limBlock : limitedBlocks) {
			String msg = null;
			if (limBlock.contains("{")){
				String temp[] = limBlock.split("\\{");
				limBlock = temp[0].trim();
				msg = Log.replaceColors(temp[1].replace("}", ""));
			}
			try {
				String[] temp = limBlock.split(" ");
				if (temp.length!=2){
					Warning.config("You have an error in your Advanced.config.yml in LimitBlocks:", false);
					Warning.config("\""+limBlock+"\" does not follow the syntaxis \"itemIndex limit\"!", false);
					continue;
				}
				int limit = 0;
				try {
					limit = Integer.parseInt(temp[1]);
				} catch (NumberFormatException ex){
					Warning.config("You have an error in your Advanced.config.yml in LimitBlocks:", false);
					Warning.config("\""+temp[1]+"\" is not a valid number!", false);
					continue;
				}
				List<TRItem> items;
				try {
					items = TRItemProcessor.processItemString(temp[0]);
				} catch (TRException ex) {
					Warning.config("You have an error in your Advanced.config.yml in LimitBlocks:", false);
					Warning.config(ex.getMessage(), false);
					continue;
				}
				
				for (TRItem ci : items){
					TRConfigLimit cLimit = new TRConfigLimit();
					cLimit.id = ci.id;
					cLimit.data = ci.data;
					cLimit.msg = (msg == null ? "" : msg);
					cLimit.configcount = limit;
					configLimits.add(cLimit);
				}
			} catch (Exception ex) {
				Warning.config("LimitBlocks: has an error!", false);
			}
		}
	}
	
	/** Remove all limits from this player. */
	public void clearLimitsAndClearInDB() {
		for (TRLimit ll : itemlimits) {
			ll.placedBlock.clear();
		}
		itemlimits.clear();
		saveLimiter(this);
	}
	
	public void clearLimits() {
		for (TRLimit ll : itemlimits) {
			ll.placedBlock.clear();
		}
		itemlimits.clear();
	}
	
	public int getMax(@NonNull Player player, int thisid, int thisdata){
		int max = -1;
		try {
			List<TRPermLimit> cached = limiterPermCache.get(player.getName());
			if (cached != null){
				boolean found = false;
				for (TRPermLimit lim : cached){
					if (lim.compare(thisid, thisdata)){
						if (lim.max == -2) return -1;
						if (lim.max != -1) return lim.max;
						
						found = true;
					}
				}
				if (found) return -1;
			}
			
			TRPermLimit pl = TRPermHandler.getPermLimitFromPerm(player, "tekkitrestrict.limiter", thisid, thisdata);
			if (pl != null) {
				if (cached == null) cached = new ArrayList<TRPermLimit>();
				cached.add(pl);
				limiterPermCache.put(player.getName(), cached);
				if (pl.max == -2) return -1;
				return pl.max;
			} else {
				for (int i = 0; i < configLimits.size(); i++) {
					TRConfigLimit cc = configLimits.get(i);
					if (cc.compare(thisid, thisdata)) {
						lastString = cc.msg;
						return cc.configcount;
					}
				}
			}
		} catch (Exception ex){
			Warning.other("An error occurred while trying to get the maxlimit of a player ('+TRLimiter.getMax(...):int')!", false);
			Log.Exception(ex, false);
		}
		return max;
	}
	
	private String lastString = "";

	/**
	 * If the player has not yet maxed out his limits, it will add the placed block to his limits.
	 * @return Whether a player has already maxed out their limits.
	 * @see TRListener#onBlockPlace(BlockPlaceEvent) Used by TRListener.onBlockPlace(BlockPlaceEvent)
	 */
	@Nullable public String checkLimit(BlockPlaceEvent event, boolean doBypassCheck) {
		String r = null;
		if (doBypassCheck && event.getPlayer().hasPermission("tekkitrestrict.bypass.limiter")) return null;//true
		Block block = event.getBlock();
		int thisid = block.getTypeId();
		int thisdata = block.getData();
		
		int TLimit = getMax(event.getPlayer(), thisid, thisdata);//Get the max for this player for id:data
		
		if (TLimit != -1) {
			TRLocation bloc2 = new TRLocation(block.getLocation());
			for (TRLimit limit : itemlimits) {
				if (limit.id != thisid || limit.data != thisdata) continue;
				
				int currentnum = limit.placedBlock.size();
				if (currentnum >= TLimit) {
					// this would be at max.
					String tbr = lastString;
					lastString = null;
					return tbr == null?"":tbr;//false
				} else {
					// loop through the placedblocks to make sure that we
					// aren't placing the same one down twice.
					boolean place2 = false;
					for (TRLocation j : limit.placedBlock) {
						if (j.equals(bloc2)) {
							place2 = true;
							break;
						}
					}
					
					if (!place2) {
						limit.placedBlock.add(bloc2);

						allBlockOwners.put(bloc2.world + ":" + bloc2.x + ":" + bloc2.y + ":" + bloc2.z, event.getPlayer().getName());
						isModified = true;
						return null;//true
					} else {
						//This block is already in the placed list, so allow placement but do not increment counts.
						return null;//true
					}
				}
			}

			// it hasn't quite gone through yet. We need to make a new limiter
			// and add this block to it.
			TRLimit g = new TRLimit();
			g.id = thisid;
			g.data = thisdata;
			g.placedBlock.add(bloc2);
			itemlimits.add(g);
			
			allBlockOwners.put(bloc2.world + ":" + bloc2.x + ":" + bloc2.y + ":" + bloc2.z, event.getPlayer().getName());
			isModified = true;
			// Making new
			return null;//true
		}

		return r;//true
	}

	public void checkBreakLimit(@NonNull BlockBreakEvent event) {
		// loop through player's limits.
		
		int id = event.getBlock().getTypeId();
		byte data = event.getBlock().getData();
		for (TRLimit limit : itemlimits) {
			if (limit.id != id || limit.data != data) continue;
			
			int currentnum = limit.placedBlock.size();
			if (currentnum <= 0) {
				// this would be at minimum
				// (LOG) maxed out
				return;
			} else {
				// add to it!
				TRLocation bloc = new TRLocation(event.getBlock().getLocation());
				limit.placedBlock.remove(bloc);
				
				allBlockOwners.remove(bloc.world + ":" + bloc.x + ":" + bloc.y + ":" + bloc.z);
				isModified = true;
				return;
			}
			
		}
	}
	
	public void checkBreakLimit(int id, byte data, @NonNull Location bloc) {
		// loop through player's limits.
		for (TRLimit limit : itemlimits) {
			if (limit.id != id || limit.data != data) continue;
			
			int currentnum = limit.placedBlock.size();
			if (currentnum <= 0) {
				// this would be at minimum
				// (LOG) maxed out
				return;
			} else {
				// add to it!
				limit.placedBlock.remove(bloc);
				
				allBlockOwners.remove(bloc.getWorld().getName() + ":" + bloc.getBlockX() + ":" + bloc.getBlockY() + ":" + bloc.getBlockZ());
				isModified = true;
				return;
			}
			
		}
	}

	/**
	 * First checks all loaded limiters.<br>
	 * If it cannot find this player's limiter, it will look into the database.<br>
	 * If that also results in nothing, it will create a new limiter.
	 * @return The limiter the given player has. If it doesn't exist, it creates one.
	 */
	@NonNull public static TRLimiter getLimiter(@NonNull String playerName) {
		playerName = playerName.toLowerCase();
		// check if a previous itemlimiter exists...

		for (TRLimiter il : limiters){
			if (il.player.toLowerCase().equals(playerName)) {
				return il;
			}
		}
		//Player is not loaded or offline.
		
		TRLimiter r = new TRLimiter();
		r.player = playerName;
		
		changing = true;
		limiters.add(r);
		changing = false;
		
		//If player is online, check for bypass.
		Player p = Bukkit.getPlayer(playerName);
		if (p != null && p.hasPermission("tekkitrestrict.bypass.limiter")) return r; //return an empty limiter
		
		//If player is offline or isn't loaded, check the database.
		ResultSet dbin = null;
		try {
			dbin = tekkitrestrict.db.query("SELECT * FROM `tr_limiter` WHERE `player` = '" + playerName + "';");
			if (dbin == null){
				Warning.other("Unknown error occured when trying to get limits from database!", false);
				return r;
			}
			if (dbin.next()) {
				//This player exists in the database, so now we have to load him/her up.

				// add data
				String blockdata = dbin.getString("blockdata");
				dbin.close(); //We dont need dbin any more from here so we can close it.
				// scheme:
				// id:data/DATA|id:data/DATA
				if (blockdata.length() >= 3) {
					if (blockdata.contains("%")) {
						String[] prelimits = blockdata.split("%");
						for (int i = 0; i < prelimits.length; i++) {
							r.itemlimits.add(loadLimitFromString(prelimits[i]));
						}
					} else {
						r.itemlimits.add(loadLimitFromString(blockdata));
					}
				}

				for (TRLimit l : r.itemlimits) {
					for (TRLocation l1 : l.placedBlock) {
						allBlockOwners.put(l1.world + ":" + l1.x + ":" + l1.y + ":" + l1.z, r.player);
					}
				}

				return r;
			}
			
			dbin.close();
		} catch (Exception e) {
			try {
				if (dbin != null) dbin.close();
			} catch (SQLException e1) {}
		}

		return r; //Return an empty limiter
	}

	/** @return The limiter the given player has. If it doesn't exist, it creates one. */
	@NonNull public static TRLimiter getOnlineLimiter(@NonNull Player player) {
		String playerName = player.getName().toLowerCase();
		// check if a previous itemlimiter exists...

		for (TRLimiter il : limiters){
			if (il.player.toLowerCase().equals(playerName)) {
				return il;
			}
		}
		
		TRLimiter r = new TRLimiter();
		r.player = playerName;
		changing = true;
		limiters.add(r);
		changing = false;
		
		if (player.hasPermission("tekkitrestrict.bypass.limiter")) return r; //return an empty limiter
		
		// check to see if this player exists in the database...
		ResultSet dbin = null;
		try {
			dbin = tekkitrestrict.db.query("SELECT * FROM `tr_limiter` WHERE `player` = '" + playerName + "';");
			if (dbin == null){
				Warning.other("Unknown error occured when trying to get limits from database!", false);
				return r;
			}
			if (dbin.next()) {
				// This player exists in the database!!!
				// load them up!

				// add data
				String blockdata = dbin.getString("blockdata");
				dbin.close(); //We dont need dbin any more from here so we can close it.
				// scheme:
				// id:data/DATA|id:data/DATA
				if (blockdata.length() >= 3) {
					if (blockdata.contains("%")) {
						String[] prelimits = blockdata.split("%");
						for (int i = 0; i < prelimits.length; i++) {
							r.itemlimits.add(loadLimitFromString(prelimits[i]));
						}
					} else {
						r.itemlimits.add(loadLimitFromString(blockdata));
					}
				}

				for (TRLimit l : r.itemlimits) {
					for (TRLocation l1 : l.placedBlock) {
						allBlockOwners.put(l1.world + ":" + l1.x + ":" + l1.y + ":" + l1.z, r.player);
					}
				}

				return r;
			}
			
			dbin.close();
		} catch (Exception e) {
			try {
				if (dbin != null) dbin.close();
			} catch (SQLException e1) {}
		}

		return r; //Return an empty limiter
	}
	
	/** Note: You should add the locations in this limit to allBlockOwners after calling this method. */
	@NonNull private static TRLimit loadLimitFromString(@NonNull String ins) {
		// ins = ins.replace("/", "!");
		String[] limit = ins.split("&");

		TRLimit l = new TRLimit();
		if (limit.length == 2) {
			String item = limit[0];
			String locStr = limit[1];
			// block id parse
			if (item.length() > 0) {
				if (item.contains(":")) {
					// c = org.bukkit.block.
					String[] mat = item.split(":");
					try {
						l.id = Integer.parseInt(mat[0]);
						l.data = Byte.parseByte(mat[1]);
					} catch (NumberFormatException ex){
						Warning.config("Invalid limiter value in database: \""+item+"\"!", false);
						l.id = 0;
						l.data = 0;
					}
				} else {
					try {
						l.id = Integer.parseInt(item);
						l.data = 0;
					} catch (NumberFormatException ex){
						Warning.config("Invalid limiter value in database: \""+item+"\"!", false);
						l.id = 0;
						l.data = 0;
					}
				}

				// DATA parse:
				// world,x,y,z=world,x,y,z=...=...
				if (locStr.length() > 0) {
					if (locStr.contains("_")) {
						String[] datas = locStr.split("_");
						for (int j = 0; j < datas.length; j++) {
							TRLocation loc = locParse(datas[j]);
							if (loc != null) l.placedBlock.add(loc);
						}
					} else {
						TRLocation loc = locParse(locStr);
						if (loc != null) l.placedBlock.add(loc);
					}
				}
			}
		}
		return l;
	}

	/** Save the limiters to the database. */
	public static void saveLimiters() {
		// looping through each player's limiters
		while (changing){
			try {
				Thread.sleep(10);
			} catch (Exception ex){}
		}
		changing = true;
		for (TRLimiter lb : limiters){
			saveLimiter(lb);
		}
		changing = false;
	}

	/**
	 * Called by QuitListener.quit(Player) to make a players limits expire (after 6x32 = 192 ticks) when he logs off.
	 * @see nl.taico.tekkitrestrict.listeners.QuitListener#quit(Player) QuitListener.quit(Player)
	 * @see tekkitrestrict#initHeartBeat()
	 */
	public static void setExpire(@NonNull String player) {
		player = player.toLowerCase();
		// gets the player from the list of limiters (does nothing if player doesn't exist)

		for (TRLimiter il : limiters) {
			if (!il.player.equals(player)) continue;
			
			il.expire = 6; // Every 32 ticks 32*6 = Every 192 ticks
			return;
		}
	}

	/**
	 * IMPORTANT: set Changing to true before using this and to false after using this.
	 * Dynamically unload a player (after they logout).<br>
	 * Saves the limiter first, then uses <code>limitBlock.clearLimits()</code> and
	 * then uses <code>limiters.remove(limitBlock)</code>
	 * @see #saveLimiter(TRLimiter)
	 */
	private static void deLoadLimiter(@NonNull TRLimiter limitBlock) {
		saveLimiter(limitBlock);
		// clear limits
		limitBlock.clearLimits();
		// remove this limiter from the list.
		limiters.remove(limitBlock);
	}

	private static boolean logged = false, logged2 = false;
	
	/** Saves 1 limiter to the database. */
	@Safe(allownull = false)
	private static void saveLimiter(@NonNull TRLimiter lb) {
		if (lb.player == null){
			Warning.other("An error occurred while saving the limits! Error: Null player name!", false);
			return;
		}
		String player = lb.player.toLowerCase();
		String blockdata = null;
		
		try {
			int size = lb.itemlimits.size();
			int i = 0;
			Iterator<TRLimit> limitsIt = lb.itemlimits.iterator();
			while (limitsIt.hasNext()){
				TRLimit limit1 = limitsIt.next();
				if (limit1.id == -1) continue;
				
				String suf;
				if (i == size - 1) suf = "";
				else suf = "%";
				
				String block = "" + limit1.id; //id or id:data
				
				// set data if it exists
				if (limit1.data == 0)
					block = "" + limit1.id;
				else
					block = "" + limit1.id + ":" + limit1.data;
				
				
				// Get DATA.
				String DATA = "";
				// loop through each block data
				int size2 = limit1.placedBlock.size();
				int j = 0;
				
				Iterator<TRLocation> pblockIt = limit1.placedBlock.iterator();
				while (pblockIt.hasNext()){
					
					String suf1;
					if (j == size2 - 1) suf1 = "";
					else suf1 = "_";
					TRLocation l = pblockIt.next();
					DATA += l.world + "," + l.x + "," + l.y + "," + l.z + suf1;
					j++;
				}
				
				String datarr = "";
				if (size2 > 0) {
					datarr = block + "&" + DATA + suf;
				}
				if (blockdata == null) blockdata = "";
				blockdata += datarr;
				
				i++;
			}
			if (blockdata == null) blockdata = "";
		} catch (Exception ex) {
			if (!logged){
				Warning.other("An error occurred while saving the limits! Error: Cannot create string to save to database!", false);
				logged = true;
			}
		}

		if (blockdata == null) return;
		try {
				tekkitrestrict.db.query("INSERT OR REPLACE INTO `tr_limiter` (`player`,`blockdata`) VALUES ('"
						+ player
						+ "','"
						+ blockdata
						+ "');");
			
		} catch (SQLException ex) {
			if (!logged2){
				Warning.other("An error occurred while saving the limits! Error: Cannot insert into database!", false);
				logged2 = true;
			}
		}
	}
	
	/** Parses a String formatted like <code>"world,x,y,z"</code> to a location. */
	@Nullable private static TRLocation locParse(String ins) {
		try {
			if (ins.contains(",")) {
				// determine if the world for this exists...
				String[] lac = ins.split(",");
				World cw = Bukkit.getWorld(lac[0]);
				if (cw != null) {
					return new TRLocation(cw.getName(), Integer.parseInt(lac[1]), Integer.parseInt(lac[2]), Integer.parseInt(lac[3]));
				}
			}
		} catch (Exception ex){
			Warning.other("Error while loading a limiter: malformed limiter location in the database!", false);
		}

		return null;
	}

	/** load all of the block:player pairs from db. */
	public static void init() {
		ResultSet dbin = null;
		try {
			dbin = tekkitrestrict.db.query("SELECT * FROM `tr_limiter`;");
			if (dbin == null){
				Warning.other("Unable to load the limits from the database!", true);
				return;
			}
			while (dbin.next()) {
				// This player exists in the database!!!
				// load them up!

				// This player does not have a bypass =(
				// add data
				String player = dbin.getString("player").toLowerCase();
				String blockdata = dbin.getString("blockdata");
				// scheme:
				// id:data/DATA|id:data/DATA
				if (blockdata.length() >= 3) {
					if (blockdata.contains("%")) {
						String[] prelimits = blockdata.split("%");
						for (int i = 0; i < prelimits.length; i++) {
							String g = prelimits[i];
							TRLimit l = loadLimitFromString(g);
							List<TRLocation> blks = l.placedBlock;
							for (TRLocation loc : blks) {
								allBlockOwners.put(
										loc.world + ":"
												+ loc.x + ":"
												+ loc.y + ":"
												+ loc.z, player);
							}
						}
					} else {
						String g = blockdata;
						TRLimit l = loadLimitFromString(g);
						List<TRLocation> blks = l.placedBlock;
						for (TRLocation loc : blks) {
							allBlockOwners.put(loc.world + ":"
									+ loc.x + ":" + loc.y
									+ ":" + loc.z, player);
						}
					}
				}
			}
			dbin.close();
		} catch (Exception ex) {
			try {
				if (dbin != null) dbin.close();
			} catch (SQLException ex2) {}
			Warning.otherWarnings.add("[SEVERE] An error occurred while loading the limiter!");
			tekkitrestrict.log.severe("An error occurred while loading the limiter!");
			Log.Exception(ex, true);
		}
	}

//	/** Used by {@link #manageData()} for the Future call. */
//	private static Location tempLoc;
	/**
	 * Manages and removes bad data.
	 * Determines if the limit exists at a location. If not, remove it.
	 * Called by savethread.
	 */
	public static void manageData() {
		for (TRLimiter lb : limiters) {
			boolean changed = false;
			for (TRLimit l : lb.itemlimits) {
				try {
					Iterator<TRLocation> it = l.placedBlock.iterator();
					while (it.hasNext()){
						TRLocation loc = it.next();
						//tempLoc = loc;
						Chunk chunk = loc.getChunk();//tempLoc.getChunk();
						
						if (!chunk.isLoaded()){
							chunk.load(false);
							
							/*
							try {
								Future<Chunk> returnFuture = Bukkit.getScheduler().callSyncMethod(tekkitrestrict.getInstance(), new Callable<Chunk>() {
								   public Chunk call() {
									   Chunk c = tempLoc.getChunk();
									   c.load();
								       return c;
								   }
								});
								chunk = returnFuture.get();
							} catch (Exception ex){
								continue;
							}*/
						}
						
						if(chunk.isLoaded()){
							Block b = loc.getBlock();
							if (b.getTypeId() != l.id){
								it.remove();
								changed = true;
							} else if (l.data != b.getData() && !(b.getData() == 0 && l.data == -10) && l.data != -1) {//TODO IMPORTANT FIXME change THIS!
								it.remove();
								changed = true;
							}
						} else {
							tekkitrestrict.log.info("[DEBUG] Chunk is not loaded at: "+chunk.getX() + ", " + chunk.getZ());
						}
					}
				} catch (ConcurrentModificationException ex){
					continue;
				}
			}
			
			if (changed) saveLimiter(lb);
		}
	}

	/**
	 * Called by the heartbeat to unload limits of players that are offline.
	 * @see #deLoadLimiter(TRLimiter) Uses deLoadLimiter(TRLimitBlock)
	 * @see tekkitrestrict#initHeartBeat() Called by tekkitrestrict.initHeartBeat()
	 */
	public static void expireLimiters() {
		// loop through each limiter.
		ArrayList<TRLimiter> tbr = new ArrayList<TRLimiter>();

		for (TRLimiter il : limiters){
			if (il.expire == -1) continue;
			if (il.expire == 0) { // do expire
				tbr.add(il);
				//Expired limiter
			} else {
				// Age limiter
				il.expire--;
			}
		}
		if (changing) return;
		changing = true;
		for (TRLimiter lb : tbr){
			deLoadLimiter(lb);
		}
		changing = false;
	}

	/** Called when a player logs in to make his limits not expire any more. */
	public static void removeExpire(@NonNull String playerName) {
		playerName = playerName.toLowerCase();
		for (TRLimiter il : limiters){
			if (il.player.equalsIgnoreCase(playerName))
				il.expire = -1;
		}
	}

	/** @return The owner of this block, or null if none is found. */
	@Nullable public static String getPlayerAt(@NonNull Block block) {
		// Cache block:owners, so this goes really fast.
		Location bloc = block.getLocation();
		String pl = allBlockOwners.get(bloc.getWorld().getName() + ":" + bloc.getBlockX() + ":" + bloc.getBlockY() + ":" + bloc.getBlockZ());
		return pl;
	}

	@NonNull public static ArrayList<String> getDebugInfo(){
		ArrayList<String> tbr = new ArrayList<String>();
		for (TRConfigLimit limit : configLimits){
			tbr.add("L:" + limit.id+":"+limit.data+"_"+limit.configcount);
		}
		
		return tbr;
	}
}
