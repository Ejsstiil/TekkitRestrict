package nl.taico.tekkitrestrict.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import nl.taico.tekkitrestrict.Log;

public class ModModificationsConfig extends TRConfig {
	public static ArrayList<String> defaultContents(boolean extra){
		ArrayList<String> tbr = new ArrayList<String>(120);
		
		tbr.add("##########################################################################################");
		tbr.add("## Configuration file for TekkitRestrict                                                ##");
		tbr.add("## Authors: Taeir, DreadEnd (aka DreadSlicer)                                           ##");
		tbr.add("## BukkitDev: http://dev.bukkit.org/server-mods/tekkit-restrict/                        ##");
		tbr.add("## Please ask questions/report issues on the BukkitDev page.                            ##");
		tbr.add("##########################################################################################");
		tbr.add("");
		tbr.add("##########################################################################################");
		tbr.add("################################ Mod Modifications Config ################################");
		tbr.add("##########################################################################################");
		tbr.add("");
		tbr.add("# Limit fly time using rings and jet packs in minutes");
		tbr.add("# NOTE: This feature is currently not working.");
		tbr.add("# Default: false");
		tbr.add("LimitFlightTime: false");
		if (extra) tbr.add("#:-;-:# LimitFlightTime");
		tbr.add("FlyLimitDailyMinutes: 9999999");
		tbr.add("");
		tbr.add("# Should offensive powers of gemarmor be allowed? (explosion, lightning)");
		tbr.add("# Default: false");
		tbr.add("AllowGemArmorOffensive: false");
		if (extra) tbr.add("#:-;-:# AllowGemArmorOffensive");
		tbr.add("");
		tbr.add("# Should the defensive powers of gemarmor be allowed? (flying, automatic running)");
		tbr.add("# Default: true");
		tbr.add("AllowGemArmorDefensive: true");
		if (extra) tbr.add("#:-;-:# AllowGemArmorDefensive");
		tbr.add("");
		tbr.add("# Set RedPower timers minimum time in seconds");
		tbr.add("# Default: 1.0");
		tbr.add("RPTimerMin: 1.0");
		if (extra) tbr.add("#:-;-:# RPTimerMin");
		tbr.add("");
		tbr.add("##########################################################################################");
		tbr.add("######################################### Set EMC ########################################");
		tbr.add("##########################################################################################");
		tbr.add("");
		tbr.add("# Set or change EMC Values for ANY item, block or tool, even the ones that usually don't");
		tbr.add("# have EMC values.");
		tbr.add("#");
		tbr.add("# Example: HARD Mode -> DarkMatter, DM Blocks, RedMatter, RMBlocks: EMC*2.");
		tbr.add("#- \"27541 300000\"");
		tbr.add("#- \"126:8 800000\"");
		tbr.add("#- \"27563 900000\"");
		tbr.add("#- \"126:9 1300000\"");
		tbr.add("#");
		tbr.add("# DEFAULT: EMC farm removal [Milk, Blaze Rod, Bonemeal]");
		tbr.add("SetEMC: ");
		if (extra) tbr.add("#:-;-:# SetEMC 3");
		tbr.add("- \"335 768\"");
		tbr.add("- \"377 308\"");
		tbr.add("- \"351:15 29\"");
		tbr.add("");
		tbr.add("##########################################################################################");
		tbr.add("####################################### Item Max EU ######################################");
		tbr.add("##########################################################################################");
		tbr.add("");
		tbr.add("# Set the Max EU storable in an IC2 Item (Check out IC2 Wiki for MaxEU details)");
		tbr.add("#");
		tbr.add("# Some default EU values:");
		tbr.add("#- \"30148 40000\" #Nano saber");
		tbr.add("#- \"30234 10000\" #Diamond Drill");
		tbr.add("#- \"30177 100000\" #ANY piece Nano Armor");
		tbr.add("#- \"30173 1000000\" #ANY piece Quantum Armor");
		tbr.add("#");
		tbr.add("# Examples:");
		tbr.add("# Charge Quantum armor set 2x slower");
		tbr.add("#- \"30171-30174 1000000 500\"");
		tbr.add("# Charge Quantum armor set 4x slower");
		tbr.add("#- \"30171-30174 1000000 250\"");
		tbr.add("MaxEU: []");
		if (extra) tbr.add("#:-;-:# MaxEU");
		tbr.add("");
		tbr.add("##########################################################################################");
		tbr.add("################################### Max EE Tool Charge ###################################");
		tbr.add("##########################################################################################");
		tbr.add("# MaxCharge limits the maximum EE charge of an item. (0-100%)");
		tbr.add("#- \"27573 30\" (This will set the max charge of a morning star to 30%)");
		tbr.add("#- \"27564-27573 50\" (This will Gimp ALL Red Matter Tools down to only 50% charge capacity)");
		tbr.add("MaxCharge: []");
		if (extra) tbr.add("#:-;-:# MaxCharge");
		tbr.add("");
		tbr.add("# This will automatically decharge set EE items when a player enters a safezone.");
		tbr.add("# NOTE: Requires DechargeEE in SafeZones.config to be true");
		tbr.add("DechargeInSS: ");
		if (extra) tbr.add("#:-;-:# DechargeInSS");
		tbr.add("- 27526");
		tbr.add("- 27527");
		tbr.add("- 27530");
		tbr.add("- 27531");
		tbr.add("- 27533");
		tbr.add("- 27534");
		tbr.add("- 27535");
		tbr.add("- 27583");
		tbr.add("- 27593");
		tbr.add("- 27543");
		tbr.add("- 27544");
		tbr.add("- 27545");
		tbr.add("- 27546");
		tbr.add("- 27547");
		tbr.add("- 27548");
		tbr.add("- 27564");
		tbr.add("- 27565");
		tbr.add("- 27566");
		tbr.add("- 27567");
		tbr.add("- 27568");
		tbr.add("- 27569");
		tbr.add("- 27570");
		tbr.add("- 27572");
		tbr.add("- 27573");
		tbr.add("- 27574");
		tbr.add("");
		tbr.add("##########################################################################################");
		
		return tbr;
	}
	
	public static void upgradeFile(){
		ArrayList<String> def = convertDefaults(defaultContents(true));
		boolean fixed = false;
		int j = def.size();
		for (int i = 0; i < j; i++){
			String s = def.get(i);
			if (s.startsWith("-")){
				switch (s){
					case "- \"335 100\"":
					case "- 335 100":
						def.set(i, "- \"335 768\"");
						fixed = true;
						break;
					case "- \"369 780\"":
					case "- 369 780":
						def.set(i, "- \"377 308\"");
						fixed = true;
						break;
					case "- \"351:15 30\"":
					case "- 351:15 30":
						def.set(i, "- \"351:15 29\"");
						fixed = true;
						break;
					case "- \"139 4\"":
					case "- 139 4":
					case "- \"37 4\"":
					case "- 37 4":
					case "- \"38 4\"":
					case "- 38 4":
					case "- \"295 4\"":
					case "- 295 4":
						def.remove(i);
						i--;j--;
						break;
				}
			}
		}
		if (fixed) Log.Warning.load("Fixed old incorrect default values for EMC farm fixes. All EMC farm fixes now should work properly.", false);
		
		//Duplicate remover
		Iterator<String> it = def.iterator();
		HashSet<String> tbr = new HashSet<String>();
		while (it.hasNext()){
			String s = it.next();
			if (s.startsWith("-")){
				if (!tbr.add(s)) it.remove();
			}
		}
		upgradeFile("ModModifications", def);
	}
}
