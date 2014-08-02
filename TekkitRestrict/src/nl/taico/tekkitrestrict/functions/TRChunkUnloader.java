package nl.taico.tekkitrestrict.functions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;

import nl.taico.tekkitrestrict.Log;
import nl.taico.tekkitrestrict.Log.Warning;
import nl.taico.tekkitrestrict.TRConfigCache.ChunkUnloader;
import nl.taico.tekkitrestrict.objects.TREnums.ChunkUnloadMethod;

import forge.ForgeHooks;
import net.minecraft.server.EmptyChunk;

public class TRChunkUnloader {
	public static void unloadSChunks() {
		if (!ChunkUnloader.enabled) return;
		int tot = getTotalChunks();
		if (tot > ChunkUnloader.maxChunksTotal) {
			int nr = tot - ChunkUnloader.maxChunksTotal - 1;
			if (ChunkUnloader.unloadOrder == 0){
				nr = unloadEndChunks(nr, true);
				if (nr > 0) nr = unloadNetherChunks(nr, true);
				if (nr > 0) nr = unloadNormalChunks(nr, true);
			} else if (ChunkUnloader.unloadOrder == 1){
				nr = unloadNetherChunks(nr, true);
				if (nr > 0) nr = unloadEndChunks(nr, true);
				if (nr > 0) nr = unloadNormalChunks(nr, true);
			} else if (ChunkUnloader.unloadOrder == 2){
				nr = unloadNormalChunks(nr, true);
				if (nr > 0) nr = unloadEndChunks(nr, true);
				if (nr > 0) nr = unloadNetherChunks(nr, true);
			} else if (ChunkUnloader.unloadOrder == 3){
				nr = unloadEndChunks(nr, true);
				if (nr > 0) nr = unloadNormalChunks(nr, true);
				if (nr > 0) nr = unloadNetherChunks(nr, true);
			} else if (ChunkUnloader.unloadOrder == 4){
				nr = unloadNetherChunks(nr, true);
				if (nr > 0) nr = unloadNormalChunks(nr, true);
				if (nr > 0) nr = unloadEndChunks(nr, true);
			} else if (ChunkUnloader.unloadOrder == 5){
				nr = unloadNormalChunks(nr, true);
				if (nr > 0) nr = unloadNetherChunks(nr, true);
				if (nr > 0) nr = unloadEndChunks(nr, true);
			} else {
				Warning.config("Invalid value " + ChunkUnloader.unloadOrder + " for UnloadOrder in TPerformance.config.yml!", false);
				Warning.config("Valid: 0, 1, 2, 3, 4 or 5.", false);
				ChunkUnloader.unloadOrder = 0;
			}
			
			if (nr > 0) {
				Warning.other("Chunk Unloader cannot unload enough chunks!", false);
				Warning.other(nr + " chunks are loaded above the total maxChunks limit!", false);
				Warning.other("If the serverload is not too high, you can raise the total maxchunks count in the config.", false);
				Warning.other("If it is too high, please lower maxradii or kick some players.", false);
			}
			return;
		}
		
		unloadEndChunks(0, false);
		unloadNetherChunks(0, false);
		unloadNormalChunks(0, false);

	}
	
	private static int unloadEndChunks(int amount, boolean force) {
		if (!ChunkUnloader.enabled) return 0;
		try {
			List<World> worlds = Bukkit.getWorlds();
			
			ArrayList<Chunk> toRemove = new ArrayList<Chunk>(amount+1);
			
			for (World world : worlds) {
				if (world.getEnvironment() != Environment.THE_END) continue; //Only the end
				
				Chunk[] loadedChunks = world.getLoadedChunks();
				
				int tot = loadedChunks.length;
				if (!force && tot < ChunkUnloader.maxChunksEnd) return 0; //If the max chunk amount isn't reached, do nothing.

				for (Chunk chunk : loadedChunks) { //For every loaded chunk
					if (!force){
						if (tot < ChunkUnloader.maxChunksEnd) break; // Check for maximum chunks.
					} else {
						if (amount == 0) break;
					}
					
					if (!isChunkInUse(world, chunk.getX(), chunk.getZ(), ChunkUnloader.maxRadii)) {
						toRemove.add(chunk);
						if (!force) tot--;
						else amount--;
					}
				}
			}
			
			for (Chunk chunk : toRemove){
				int x = chunk.getX(), z = chunk.getZ();
				try {
					net.minecraft.server.WorldServer mcWorld = ((CraftWorld) chunk.getWorld()).getHandle();
					net.minecraft.server.Chunk mcChunk = mcWorld.chunkProviderServer.chunks.get(x, z);
					if (mcChunk == null){
						amount++;
						continue;
					}
					if (!(mcChunk instanceof EmptyChunk)) {
						if (!force) mcWorld.chunkProviderServer.queueUnload(x, z);
						else mcWorld.chunkProviderServer.unloadQueue.add(x, z);
						//mcChunk.removeEntities();
						//mcWorld.chunkProviderServer.saveChunk(mcChunk);
						//mcWorld.chunkProviderServer.saveChunkNOP(mcChunk);
					}

					//mcWorld.chunkProviderServer.unloadQueue.remove(x, z);
					//mcWorld.chunkProviderServer.chunks.remove(x, z);
					//mcWorld.chunkProviderServer.chunkList.remove(mcChunk);
				} catch (Exception ex){
					Log.debug("Unable to unload chunk at ["+x+","+z+"] in world " + chunk.getWorld().getName());
					amount++;
				}
			}
			
		} catch (Exception ex) {
			Warning.other("An error occurred in the Chunk Unloader [End]!", false);
			Log.Exception(ex, false);
		}
		return amount;
	}
	
	private static int unloadNetherChunks(int amount, boolean force) {
		if (!ChunkUnloader.enabled) return 0;
		try {
			List<World> worlds = Bukkit.getWorlds();
			
			ArrayList<Chunk> toRemove = new ArrayList<Chunk>(amount+1);
			
			for (World world : worlds) {
				if (world.getEnvironment() != Environment.NETHER) continue; //Only the nether
				
				Chunk[] loadedChunks = world.getLoadedChunks();
				
				int tot = loadedChunks.length;
				if (!force && tot < ChunkUnloader.maxChunksNether) return 0; //If the max chunk amount isn't reached, do nothing.

				for (Chunk chunk : loadedChunks) { //For every loaded chunk
					if (!force){
						if (tot < ChunkUnloader.maxChunksNether) break; // Check for maximum chunks.
					} else {
						if (amount == 0) break;
					}
					
					if (!isChunkInUse(world, chunk.getX(), chunk.getZ(), ChunkUnloader.maxRadii)) {
						toRemove.add(chunk);
						if (!force) tot--;
						else amount--;
					}
				}
			}
			
			for (Chunk chunk : toRemove){
				if (chunk == null) continue;
				int x = chunk.getX(), z = chunk.getZ();
				try {
					net.minecraft.server.WorldServer mcWorld = ((CraftWorld) chunk.getWorld()).getHandle();
					net.minecraft.server.Chunk mcChunk = mcWorld.chunkProviderServer.chunks.get(x, z);
					if (mcChunk == null){
						amount++;
						continue;
					}

					if (!(mcChunk instanceof EmptyChunk)) {
						if (!force) mcWorld.chunkProviderServer.queueUnload(x, z);
						else mcWorld.chunkProviderServer.unloadQueue.add(x, z);
						//mcChunk.removeEntities();
						//mcWorld.chunkProviderServer.saveChunk(mcChunk);
						//mcWorld.chunkProviderServer.saveChunkNOP(mcChunk);
					}

					//mcWorld.chunkProviderServer.unloadQueue.remove(x, z);
					//mcWorld.chunkProviderServer.chunks.remove(x, z);
					//mcWorld.chunkProviderServer.chunkList.remove(mcChunk);
				} catch (Exception ex){
					Log.debug("Unable to unload chunk at ["+x+","+z+"] in world " + chunk.getWorld().getName());
					amount++;
				}
			}
			
		} catch (Exception ex) {
			Warning.other("An error occurred in the Chunk Unloader [Nether]!", false);
			Log.Exception(ex, false);
		}
		
		return amount;
	}
	
	private static int unloadNormalChunks(int amount, boolean force) {
		if (!ChunkUnloader.enabled) return 0;
		try {
			List<World> worlds = Bukkit.getWorlds();
			
			ArrayList<Chunk> toRemove = new ArrayList<Chunk>(amount+1);
			
			for (World world : worlds) {
				if (world.getEnvironment() != Environment.NORMAL) continue; //Only normal
				
				Chunk[] loadedChunks = world.getLoadedChunks();
				
				int tot = loadedChunks.length;
				if (!force && tot < ChunkUnloader.maxChunksNormal) return 0; //If the max chunk amount isn't reached, do nothing.

				for (Chunk chunk : loadedChunks) { //For every loaded chunk
					if (!force){
						if (tot < ChunkUnloader.maxChunksNormal) break; // Check for maximum chunks.
					} else {
						if (amount == 0) break;
					}
					
					if (!isChunkInUse(world, chunk.getX(), chunk.getZ(), ChunkUnloader.maxRadii)) {
						toRemove.add(chunk);
						if (!force) tot--;
						else amount--;
					}
				}
			}
			
			for (Chunk chunk : toRemove){
				int x = chunk.getX(), z = chunk.getZ();
				try {
					net.minecraft.server.WorldServer mcWorld = ((CraftWorld) chunk.getWorld()).getHandle();
					net.minecraft.server.Chunk mcChunk = mcWorld.chunkProviderServer.chunks.get(x, z);
					if (mcChunk == null){
						amount++;
						continue;
					}
					if (!(mcChunk instanceof EmptyChunk)) {
						if (!force) mcWorld.chunkProviderServer.queueUnload(x, z);
						else mcWorld.chunkProviderServer.unloadQueue.add(x, z);
						//mcChunk.removeEntities();
						//mcWorld.chunkProviderServer.saveChunk(mcChunk);
						//mcWorld.chunkProviderServer.saveChunkNOP(mcChunk);
					}

					//mcWorld.chunkProviderServer.unloadQueue.remove(x, z);
					//mcWorld.chunkProviderServer.chunks.remove(x, z);
					//mcWorld.chunkProviderServer.chunkList.remove(mcChunk);
				} catch (Exception ex){
					Log.debug("Unable to unload chunk at ["+x+","+z+"] in world " + chunk.getWorld().getName());
					amount++;
				}
			}
			
		} catch (Exception ex) {
			Warning.other("An error occurred in the Chunk Unloader [Normal]!", false);
			Log.Exception(ex, false);
		}
		
		return amount;
	}

	/**
	 * Gets total chunks from each world's chunkProviderServer
	 * @return The number of chunks loaded
	 */
	private static int getTotalChunks() {
		List<World> worlds = Bukkit.getWorlds();
		int r = 0;
		for (World world : worlds){
			r += world.getLoadedChunks().length;
		}
		return r;
	}

	/** @return If there are currently players near that chunk. */
	private static boolean isChunkInUse(World world, int x, int z, int dist) {
		Player[] players = Bukkit.getOnlinePlayers();

		try {
			for(Player player : players){
				try {
					Location loc = player.getLocation();
					
					if (loc.getWorld() != world) continue;
					
					if (Math.abs(loc.getBlockX() - (x << 4)) <= dist && Math.abs(loc.getBlockZ() - (z << 4)) <= dist) {
						return true;
					}
				} catch (Exception ex){
					return true;
				}
			}
		} catch (Exception ex){
			return true;
		}

		return false;
	}
	

	
	private static boolean unloadChunk(Chunk chunk, boolean force){
		net.minecraft.server.WorldServer mcWorld = ((CraftWorld) chunk.getWorld()).getHandle();
		int x = chunk.getX();
		int z = chunk.getZ();
		net.minecraft.server.Chunk mcChunk = ((CraftChunk) chunk).getHandle();
		
		if (mcChunk == null) return false;

		if (!(mcChunk instanceof EmptyChunk)) {
			if (!force) mcWorld.chunkProviderServer.queueUnload(x, z);
			else mcWorld.chunkProviderServer.unloadQueue.add(x, z);
			//mcChunk.removeEntities();
			//mcWorld.chunkProviderServer.saveChunk(mcChunk);
			//mcWorld.chunkProviderServer.saveChunkNOP(mcChunk);
			return true;
		}
		return true;
	}
	
	static void unloadChunks(ChunkUnloadMethod method, World world, int amount){
		if (method == ChunkUnloadMethod.UnloadAllChunksUnforced){
			Chunk[] loaded = world.getLoadedChunks().clone();
			ArrayList<Chunk> tbr = new ArrayList<Chunk>();
			for (Chunk c : loaded){
				if (!isChunkInUse(world, c.getX(), c.getZ(), ChunkUnloader.maxRadii) && !hasChunkLoader(c)){
					tbr.add(c);
				}
			}
			
			for (Chunk c : tbr){
				unloadChunk(c, false);
			}
		}
	}
	
	private static boolean hasChunkLoader(Chunk chunk){
		return hasChunkLoader(((CraftChunk) chunk).getHandle());
	}
	
	private static boolean hasChunkLoader(net.minecraft.server.Chunk chunk){
		return !ForgeHooks.canUnloadChunk(chunk);
	}
	
}
