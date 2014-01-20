package nl.taico.tekkitrestrict.commands;

import java.io.File;

import net.minecraft.server.BaseMod;
import net.minecraft.server.Container;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemInWorldManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.mod_EE;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Player;

import nl.taico.tekkitrestrict.Log;
import nl.taico.tekkitrestrict.Send;
import nl.taico.tekkitrestrict.tekkitrestrict;
import nl.taico.tekkitrestrict.Log.Warning;
import nl.taico.tekkitrestrict.objects.OpenAlcObj;
import nl.taico.tekkitrestrict.objects.TREnums.ConfigFile;

import ee.AlchemyBagData;
import forge.IGuiHandler;
import forge.MinecraftForge;
import forge.NetworkMod;
import forge.packets.PacketOpenGUI;

public class TRCmdOpenAlc implements CommandExecutor {
	private final Send send;
	public TRCmdOpenAlc(){
		send = new Send();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		send.sender = sender;
		
		if (send.noConsole()) return true;
		

		if (!sender.hasPermission("tekkitrestrict.openalc")){
			sender.sendMessage(ChatColor.RED + "You are not allowed to use this command!");
			return true;
		}
		
		if (!tekkitrestrict.config.getBoolean(ConfigFile.General, "UseOpenAlc", true)){
			sender.sendMessage(ChatColor.RED + "Openalc is disabled!");
		}
		
		if (!tekkitrestrict.EEEnabled) {
			sender.sendMessage(ChatColor.DARK_RED + "EE is not enabled!");
			return true;
		}
		
		if (args.length == 0) {
			sender.sendMessage(ChatColor.GREEN + "/openalc <player> <color> - Open an alchemy bag.");
			sender.sendMessage(ChatColor.GREEN + "Color can be white, lime, etc. OR a number from 0-15.");
			return true;
		} else if (args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Not enough arguments!");
			sender.sendMessage(ChatColor.RED + "Usage: /openalc <player> <color>");
			return true;
		} else if (args.length != 2){
			sender.sendMessage(ChatColor.RED + "Too many arguments!");
			sender.sendMessage(ChatColor.RED + "Usage: /openalc <player> <color>");
			return true;
		}
		
		Player player = (Player) sender;
		
		if (OpenAlcObj.isViewing(player.getName())) restoreViewerInventory(player, false); //If player has an inventory open, close it.
		
		EntityPlayer looker = ((CraftPlayer) player).getHandle();
		Player OPlayer = Playerz(player, args[0]);
		
		if (OPlayer == null){
			sender.sendMessage(ChatColor.RED + "Player " + args[0] + " cannot be found!");
			return true;
		}
		
		String OName = OPlayer.getName();
		
		if (player.hasPermission("tekkitrestrict.openalc.deny."+OName) && !player.isOp() && !player.hasPermission("tekkitrestrict.openalc.deny.all")){
			sender.sendMessage(ChatColor.RED + "You are not allowed to open " + OName + "'s alchemy bags!");
			return true;
		}
		
		if (OpenAlcObj.isViewed(OName)){
			sender.sendMessage(ChatColor.RED + "Someone else is already viewing a bag of " + OName + "!");
			return true;
		}
		
		EntityPlayer holder = ((CraftPlayer) OPlayer).getHandle();

		int color = getColor(args[1]);
		if (color == -1){
			sender.sendMessage(ChatColor.RED + "Unknown color!");
			sender.sendMessage(ChatColor.RED + "Color can be white, lime, etc. OR a number from 0-15.");
			return true;
		}
		
		try {
			//World world = ((CraftWorld) player.getWorld()).getHandle();
			AlchemyBagData alcdata = openGui(looker, holder, color);
			if (alcdata == null){
				Warning.other("An error occurred. " + OName + "'s bag will not save properly if he is offline.", false);
				sender.sendMessage(ChatColor.RED + "An error occured: Unable to find the specified bag!");
				return true;
			}
			
			new OpenAlcObj(alcdata, OPlayer, player); //Add new OpenAlcObject
			
			String strcolor = getColor(color);
			sender.sendMessage(ChatColor.GREEN + "Opened " + OName + "'s " + strcolor + " Alchemy Bag!");
			sender.sendMessage(ChatColor.BLUE + "Close your inventory (twice) to restore your own inventory.");
			
			tekkitrestrict.log.info(player.getName() + " opened " + OName + "'s " + strcolor + " Alchemy Bag!");
			
		} catch (Exception ex) {
			sender.sendMessage(ChatColor.RED + "An error has occurred processing your command.");
			Warning.other("Exception in OpenAlc 'TRCommandAlc.onCommand(...)'! Error: " + ex.toString(), false);
		}

			return true;
	}

	///** Called when a player logs off while having an inventory open. */
	/*
	@SuppressWarnings("deprecation")
	public static void setPlayerInv(Player player) {
		if (player == null) return;
		
		String name = player.getName();
		HashMap<CraftPlayer, AlchemyBagData> data = openAlc.remove(name);
		
		if (data != null) {
			for (CraftPlayer current : data.keySet()){
				if (Bukkit.getPlayerExact(current.getName()) == null) current.saveData(); //Save the player if he is offline.
			}
			for (AlchemyBagData current : data.values()) current.a(); //Save the alchemy bag.
		} else return;
		
		player.updateInventory();
	}*/
	
	public static void setPlayerInv(Player player, boolean inform) {
		if (player == null) return;

		String name = player.getName();
		OpenAlcObj openAlc = OpenAlcObj.getOpenAlcByOwner(name);
		
		if (openAlc == null) openAlc = OpenAlcObj.getOpenAlcByViewer(name);
		else { //A bagOwner has been found and he matches this player.
			boolean bagOwnerOnline = (Bukkit.getPlayerExact(openAlc.getBagOwnerName()) != null);
			if (bagOwnerOnline && openAlc.getBagOwner() != openAlc.getViewer()) return; //If the player that owns the bag is online, return to prevent unwanted closing.
			
			openAlc.getBagOwner().saveData();
			openAlc.getBag().a();
			
			player = openAlc.getViewer();
			openAlc.delete();
			
			player.openInventory(player.getInventory());
			player.closeInventory();
			
			if (inform) player.sendMessage(ChatColor.BLUE + "Your own inventory was restored.");
			return;
		}
		
		if (openAlc == null) return;
		else { //A viewer has been found and he matches this player.
			boolean bagOwnerOnline = (Bukkit.getPlayerExact(openAlc.getBagOwnerName()) != null);
			if (!bagOwnerOnline) openAlc.getBagOwner().saveData(); //If online, saving is not required.
			
			openAlc.getBag().a();
			
			openAlc.delete();
			player.openInventory(player.getInventory());
			player.closeInventory();
			
			if (inform) player.sendMessage(ChatColor.BLUE + "Your own inventory was restored.");
		}
	}
	
	/**
	 * Restore the inventory of the viewer.
	 * @param player The viewer of the bag.
	 */
	public static void restoreViewerInventory(Player player, boolean inform) {
		if (player == null) return;

		String name = player.getName();
		OpenAlcObj openAlc = OpenAlcObj.getOpenAlcByViewer(name);
		
		if (openAlc == null) return;
		else { //A viewer has been found and he matches this player.
			boolean bagOwnerOnline = (Bukkit.getPlayerExact(openAlc.getBagOwnerName()) != null);
			if (!bagOwnerOnline) openAlc.getBagOwner().saveData(); //If online, saving is not required.
			
			openAlc.getBag().a();
			
			openAlc.delete();
			player.openInventory(player.getInventory());
			player.closeInventory();
			
			if (inform) player.sendMessage(ChatColor.BLUE + "Your own inventory was restored.");
		}
	}
	
	///** Called when a player closes his inventory. */
	/*
	public static void setPlayerInv2(Player player) {
		if (player == null) return;
		//First inv close is bagowner, second one is viewer
		String name = player.getName();//First invclose: bagowner
		String viewer = openAlc2.remove(name); //Remove bagowner (who is also online)
		String tbr = "";
		
		if (viewer != null){
			tbr = viewer;
		} else {
			if (onlineBagOwners.remove(name)) tbr = name;
			else return;
		}
		
		HashMap<CraftPlayer, AlchemyBagData> data = openAlc.remove(tbr);
		
		if (data != null){
			for (CraftPlayer current : data.keySet()){
				if (Bukkit.getPlayerExact(current.getName()) == null) current.saveData();
			}
			for (AlchemyBagData current : data.values()) current.a();
			
			player = Bukkit.getPlayer(tbr);
			player.openInventory(player.getInventory());
			player.closeInventory();

			player.sendMessage(ChatColor.BLUE + "Your own inventory was restored.");
		}
	}*/
	
	/**
	 * @param player The player that needs to see the inventory.
	 * @param targetPlayer The player that owns the inventory.
	 * @return 
	 */
	public static AlchemyBagData openGui(EntityPlayer player, EntityPlayer targetPlayer, int color) {
		BaseMod mod = mod_EE.getInstance();
		if (!(mod instanceof NetworkMod)) return null;
		
		IGuiHandler handler = MinecraftForge.getGuiHandler(mod);
		if (handler == null) return null;
		
		Container container = (Container) handler.getGuiElement(56, targetPlayer, player.world, color, 0, 0);
		if (container == null) return null;
		
		container = CraftEventFactory.callInventoryOpenEvent(player, container);
		if (container == null) return null;
		player.realGetNextWidowId();
		player.H();

		PacketOpenGUI pkt = new PacketOpenGUI(player.getCurrentWindowIdField(),
				MinecraftForge.getModID((NetworkMod) mod), 56, color, 0, 0);
		player.netServerHandler.sendPacket(pkt.getPacket());
		player.activeContainer = container;
		player.activeContainer.windowId = player.getCurrentWindowIdField();
		player.activeContainer.addSlotListener(player);
		AlchemyBagData test = null;
		try {
			test = (AlchemyBagData) container.getInventory();
		} catch (Exception ex){
			Warning.other("An error occured in OpenAlc ('+TRCommandAlc.openGui(...):AlchemyBagData')!", false);
			Log.Exception(ex, false);
			test = null;
		}
		return test;
	}

	public static int getColor(String color) {
		color = color.toLowerCase();
		switch (color){
		case "white": return 0;
		case "orange": return 1;
		case "lightpurple":
		case "magenta": return 2;
		case "lightblue": return 3;
		case "yellow": return 4;
		case "lightgreen":
		case "lime": return 5;
		case "pink": return 6;
		case "gray":
		case "grey": return 7;
		case "lightgray":
		case "lightgrey": return 8;
		case "cyan": return 9;
		case "purple": return 10;
		case "blue": return 11;
		case "brown": return 12;
		case "green": return 13;
		case "red": return 14;
		case "black": return 15;
		default:
			try {
				int tbr = Integer.parseInt(color);
				if (tbr < 16 && tbr > -1) return tbr;
			} catch (NumberFormatException ex){}
			return -1;
		}
	}
	public static String getColor(int color) {
		switch (color){
			case 0: return "white";
			case 1: return "orange";
			case 2: return "magenta";
			case 3: return "lightblue";
			case 4: return "yellow";
			case 5: return "lime";
			case 6: return "pink";
			case 7: return "gray";
			case 8: return "lightgray";
			case 9: return "cyan";
			case 10: return "purple";
			case 11: return "blue";
			case 12: return "brown";
			case 13: return "green";
			case 14: return "red";
			case 15: return "black";
			//case -2: return "all";
			default: return "unknown";
		}
	}

	static Player Playerz(Player sender, String name) {
		//Check if the targetplayer is online
		Player target = Bukkit.getPlayer(name);
		if (target != null) return target;
		
		//Otherwise search in the players folder fore the player.
		File playerfolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "players");

		String playername = matchUser(playerfolder.listFiles(), name);
		if (playername == null) return null;
		
		try {
			MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
			EntityPlayer entity = new EntityPlayer(server, server.getWorldServer(0), playername, new ItemInWorldManager(server.getWorldServer(0)));
			target = entity.getBukkitEntity();
			if (target != null) {
				//Do as if the player logs on.
				target.loadData();
				return target;
			}
			sender.sendMessage(ChatColor.RED + "Player " + name + " can not be found!");
		} catch (Exception ex) {
			sender.sendMessage("Error while retrieving offline player data!");
			Warning.other("An error occured in OpenAlc ('-TRCommandAlc.Playerz(...):Player')!", false);
			Log.Exception(ex, false);
			return null;
		}
		return target;
	}
	
	static Player Playerz(CommandSender sender, String name) {
		//Check if the targetplayer is online
		Player target = Bukkit.getPlayer(name);
		if (target != null) return target;
		
		//Otherwise search in the players folder fore the player.
		final File playerfolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "players");

		final String playername = matchUser(playerfolder.listFiles(), name);
		if (playername == null) return null;
		
		try {
			final MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
			final EntityPlayer entity = new EntityPlayer(server, server.getWorldServer(0), playername, new ItemInWorldManager(server.getWorldServer(0)));
			target = entity.getBukkitEntity();
			if (target != null) {
				//Do as if the player logs on.
				target.loadData();
				return target;
			}
			sender.sendMessage(ChatColor.RED + "Player " + name + " can not be found!");
		} catch (final Exception ex) {
			sender.sendMessage("Error while retrieving offline player data!");
			Warning.other("An error occured in OpenAlc ('-TRCommandAlc.Playerz(...):Player')!", false);
			Log.Exception(ex, false);
			return null;
		}
		return target;
	}
	
	public static String matchUser(File container[], String search) {
		if (search == null) return null;
		
		String found = null;
		String lowerSearch = search.toLowerCase();
		int delta = 0x7fffffff;
		for (File file : container){
			String filename = file.getName();
			String str = filename.substring(0, filename.length() - 4);
			if (!str.toLowerCase().startsWith(lowerSearch)) continue;
			
			int curDelta = str.length() - lowerSearch.length();
			if (curDelta < delta) {
				found = str;
				delta = curDelta;
			}
			if (curDelta == 0) break;
		}
		return found;
	}

}