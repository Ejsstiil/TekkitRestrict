package nl.taico.tekkitrestrict.objects;

import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import ee.AlchemyBagData;

public class OpenAlcObj {
	private AlchemyBagData bag;
	private Player bagOwner;
	private Player viewer;
	private String viewerName;
	private String bagOwnerName;
	private int color;
	private static ArrayList<OpenAlcObj> allOpenAlcs = new ArrayList<OpenAlcObj>();
	
	public OpenAlcObj(@NonNull final AlchemyBagData bag, @NonNull final Player bagOwner, @NonNull final Player viewer, final int color){
		this.bag = bag;
		this.bagOwner = bagOwner;
		this.bagOwnerName = bagOwner.getName();
		this.viewer = viewer;
		this.viewerName = viewer.getName();
		this.color = color;
		allOpenAlcs.add(this);
	}
	
	@NonNull public String getBagOwnerName(){
		return bagOwnerName;
	}
	@NonNull public String getViewerName(){
		return viewerName;
	}
	@NonNull public Player getViewer(){
		return viewer;
	}
	@NonNull public Player getBagOwner(){
		return bagOwner;
	}
	@NonNull public AlchemyBagData getBag(){
		return bag;
	}
	public int getColor(){
		return this.color;
	}
	
	/*
	@Nullable public static OpenAlcObj getOpenAlcByOwner(@NonNull final String owner){
		for (final OpenAlcObj current : allOpenAlcs){
			if (current.bagOwnerName.equalsIgnoreCase(owner)) return current;
		}
		return null;
	}*/
	
	@Nullable public static OpenAlcObj getOpenAlcByViewer(@NonNull final String viewer){
		for (final OpenAlcObj current : allOpenAlcs){
			if (current.viewerName.equalsIgnoreCase(viewer)) return current;
		}
		return null;
	}
	
	public static boolean isViewing(@NonNull final String player){
		if (getOpenAlcByViewer(player) != null) return true;
		return false;
	}
	
	public static boolean isViewed(@NonNull final String player, final int color){
		for (final OpenAlcObj current : allOpenAlcs){
			if (current.bagOwnerName.equalsIgnoreCase(player) && current.color == color) return true;
		}
		return false;
	}

	
	public void delete() {
		allOpenAlcs.remove(this);
		this.viewer = null;
		this.bagOwner = null;
		this.bag = null;
	}
}
