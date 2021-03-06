package nl.taico.tekkitrestrict.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.taico.tekkitrestrict.Log;
import nl.taico.tekkitrestrict.config.SettingsStorage;

public class TREMCSet {
	/**
	 * Get a list of Integers from a String range.
	 * @return An ArrayList of Integers with the values specified by the given range.
	 */
	private static ArrayList<Integer> handleData(final String data, final String key){
		final ArrayList<Integer> allData = new ArrayList<Integer>();
		if (data == null){
			allData.add(0);
		}
		else if (data.equals("*")) {
			Log.Config.Notice("Usage of the * datavalue for setting EMC is highly discouraged! It will increase the loadtime (and possibly also lagg) tremendeously. Please use ranges instead.");
			for (int i = 0;i<=65535;i++){
				allData.add(i);
			}
		}
		else if (data.contains(",")){
			final String temp[] = data.split(",");
			for (String current : temp){
				if (current.contains("-")){
					final String temp2[] = current.split("-");
					if (!temp2[0].matches("\\d+") || !temp2[1].matches("\\d+")){
						Log.Warning.config("The data value of '"+key+"' in SetEMC (ModModifications.config) is invalid!", false);
						allData.clear();
						allData.add(0);
					} else {
						final int end = Integer.parseInt(temp2[1]);
						for (int i = Integer.parseInt(temp2[0]); i<=end; i++){
							allData.add(i);
						}
					}
				} else {
					if (!current.matches("\\d+")){
						Log.Warning.config("The data value of '"+key+"' in SetEMC (ModModifications.config) is invalid!", false);
						allData.clear();
						allData.add(0);
					} else {
						allData.add(Integer.parseInt(current));
					}
				}
			}
		} else if (data.contains("-")){//No comma's, only a - so just 1 split needed.
			final String temp[] = data.split("-");
			if (!temp[0].matches("\\d+") || !temp[1].matches("\\d+")){
				Log.Warning.config("The data value of '"+key+"' in SetEMC (ModModifications.config) is invalid!", false);
				allData.clear();
				allData.add(0);
			} else {
				final int end = Integer.parseInt(temp[1]);
				for (int i = Integer.parseInt(temp[0]); i<=end; i++){
					allData.add(i);
				}
			}
		} else { //Only 1 value. 
			if (!data.matches("\\d+")){
				Log.Warning.config("The data value of '"+key+"' in SetEMC (ModModifications.config) is invalid!", false);
				allData.clear();
				allData.add(0);
			} else {
				allData.add(Integer.parseInt(data));
			}
		}
		return allData;
	}

	private static void loadConfigEMC() {
		final List<String> configEMC = SettingsStorage.modModificationsConfig.getStringList("SetEMC");

		for (final String current : configEMC){
			if (!current.contains(" ")){
				Log.Warning.config("You have an invalid value in SetEMC in ModModifications.config: \""+current+"\"", false);
				continue;
			}

			final String values[] = current.split(" ");
			int EMC = 0, id = 0;
			final ArrayList<Integer> data;

			try {
				EMC = Integer.parseInt(values[1]);
			} catch (NumberFormatException ex){
				Log.Warning.config("You have an invalid value in SetEMC in ModModifications.config: \""+current+"\"", false);
				continue;
			}

			if (values[0].contains(":")){
				final String temp[] = values[0].split(":");

				data = handleData(temp[1], current);

				try {
					id = Integer.parseInt(temp[0]);
				} catch (NumberFormatException ex){
					Log.Warning.config("You have an invalid value in SetEMC in ModModifications.config: \""+current+"\"", false);
					continue;
				}
			} else {
				data = new ArrayList<Integer>();
				data.add(0);

				try {
					id = Integer.parseInt(values[0]);
				} catch (NumberFormatException ex){
					Log.Warning.config("You have an invalid value in SetEMC in ModModifications.config: \""+current+"\"", false);
					continue;
				}
			}

			for (final int currentdata : data){
				setEMC(id, currentdata, EMC);
			}
		}
	}

	public static void reload() {
		loadConfigEMC();
	}

	/**
	 * Remove the EMC value of an item.
	 * WARNING: If that item is also used as fuel, unexpected behavior may occur.
	 * */
	public static void removeEMC(final int id, final int data){
		final HashMap<Integer, Integer> old = ee.EEMaps.alchemicalValues.get(id);
		if (old == null) return;
		old.remove(data);
		ee.EEMaps.alchemicalValues.put(id, old);
	}

	/**
	 * Add or set the EMC value of a single item.
	 * If EMC = 0, it will remove the EMC value of that item.
	 * @see #removeEMC(int, int)
	 */
	public static void setEMC(final int id, final int data, final int EMC){
		Log.trace("Setting EMC of "+id+":"+data+" to "+EMC);
		if (EMC == 0)
			removeEMC(id, data);
		else
			ee.EEMaps.addEMC(id, data, EMC);
	}
}
