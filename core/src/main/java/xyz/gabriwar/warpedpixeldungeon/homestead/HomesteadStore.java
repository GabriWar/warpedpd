/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SectorMap;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * One world slice's homestead data (each OverworldLevel keeps its own, saved with it): what was
 * built where and how much punishment each piece has left, the roofs, and the sections the
 * homesteads' parts keep for themselves (StoreSection). Everything is keyed by WORLD cell
 * (OverworldLevel.worldKey) and indexed by sector (SectorMap), so the window moving under it
 * relabels nothing and the window's share is found sector by sector.
 *
 * The terrain itself is not here: a built wall is a terrain edit, kept by the slice's diff store
 * like any other (OverworldLevel.captureDiffs). This store adds what the terrain cannot say, and
 * the Homestead facade keeps the two in step.
 *
 * Written on the host's actor thread; the roof layers (render thread) and a co-op host's state
 * packets (network thread) walk it too, so every write and every walk is synchronized on it.
 */
public class HomesteadStore implements Bundlable {

	/** A built piece on a cell: which (BuildPiece.id) and its health. */
	public static final class Built {
		public final String piece;
		public int hp;

		Built( String piece, int hp ){
			this.piece = piece;
			this.hp = hp;
		}
	}

	/** A roof over a cell: its kind (RoofTiles.THATCH..GLASS), its health, and how many more
	 *  turns it burns (0: not alight). */
	public static final class Roof {
		public final int kind;
		public int hp;
		public int burning;

		Roof( int kind, int hp ){
			this.kind = kind;
			this.hp = hp;
		}
	}

	private static final String SECTION_PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/**
	 * The sections a slice keeps, by class name, one per part of the homesteads that needs one.
	 * A class that does not exist (yet) is skipped, so each part adds its own and edits nothing
	 * here (android/proguard-rules.pro keeps the homestead package whole).
	 */
	public static final String[] SECTIONS = {
			SECTION_PKG + "gathering.GatheringSlice",   //stumps growing back, quarried ground
			SECTION_PKG + "stations.StationSlice",      //work in progress at the stations
			SECTION_PKG + "farming.FarmSlice",          //crops, watering
			SECTION_PKG + "animals.AnimalSlice",        //pens, tied and roped animals
			SECTION_PKG + "threats.ThreatSlice",        //sieges, damage ledgers
			SECTION_PKG + "travel.TravelSlice",         //waystones of this slice
			//the villages' notice boards of this slice: after the waystones, which keep their cells by the well
			"xyz.gabriwar.warpedpixeldungeon.bounty.BountySlice"
	};

	final SectorMap<Built> built = new SectorMap<>();
	final SectorMap<Roof> roofs = new SectorMap<>();
	private final LinkedHashMap<String, StoreSection> sections = new LinkedHashMap<>();

	//bumped by every change a co-op guest or the roof layer has to know of
	private int version;

	//when each door or gate standing open was opened (the game's clock), so one the hero has just
	//opened by hand is not shut on him before he steps in. not saved: a load shuts them all
	final HashMap<Long, Float> openedAt = new HashMap<>();

	//the rooms found on this slice's window (Rooms), never saved: worked out again on demand
	final Rooms.Cache roomCache = new Rooms.Cache();

	public HomesteadStore(){
		for (String name : SECTIONS){
			Class<?> cls;
			try {
				cls = Class.forName( name );
			} catch (ClassNotFoundException e){
				continue;
			}
			try {
				sections.put( name, (StoreSection) cls.getConstructor().newInstance() );
			} catch (ReflectiveOperationException | ClassCastException e){
				throw new IllegalStateException( "homestead section " + name + " is not a public StoreSection", e );
			}
		}
	}

	/** This slice's section of a class, or null when that part of the homesteads is not built. */
	public <T extends StoreSection> T section( Class<T> cls ){
		return cls.cast( sections.get( cls.getName() ) );
	}

	public Collection<StoreSection> sections(){
		return sections.values();
	}

	/** Changes whenever a piece, its health or a roof does: what a mirror's copy is checked by. */
	public int version(){
		return version;
	}

	void changed(){
		version++;
	}

	// ----------------------------------------------------------------- pieces

	public Built builtAt( int wx, int wy ){
		return built.get( OverworldLevel.worldKey( wx, wy ) );
	}

	synchronized void setBuilt( int wx, int wy, String piece, int hp ){
		built.put( OverworldLevel.worldKey( wx, wy ), new Built( piece, hp ) );
		changed();
	}

	synchronized void clearBuilt( int wx, int wy ){
		if (built.remove( OverworldLevel.worldKey( wx, wy ) ) != null) changed();
	}

	/** Every built piece in the world rect [x0, x1) x [y0, y1). */
	public synchronized void forEachBuilt( int x0, int y0, int x1, int y1, SectorMap.Visitor<Built> visitor ){
		built.forEachIn( x0, y0, x1, y1, visitor );
	}

	public int builtCount(){
		return built.size();
	}

	// ----------------------------------------------------------------- roofs

	public Roof roofAt( int wx, int wy ){
		return roofs.get( OverworldLevel.worldKey( wx, wy ) );
	}

	synchronized void setRoof( int wx, int wy, int kind, int hp ){
		roofs.put( OverworldLevel.worldKey( wx, wy ), new Roof( kind, hp ) );
		changed();
	}

	synchronized void clearRoof( int wx, int wy ){
		if (roofs.remove( OverworldLevel.worldKey( wx, wy ) ) != null) changed();
	}

	/** Every roof in the world rect [x0, x1) x [y0, y1). */
	public synchronized void forEachRoof( int x0, int y0, int x1, int y1, SectorMap.Visitor<Roof> visitor ){
		roofs.forEachIn( x0, y0, x1, y1, visitor );
	}

	// ----------------------------------------------------------------- the bundle

	private static final String B_KEYS   = "hs_built_keys";
	private static final String B_PIECES = "hs_built_pieces";
	private static final String B_HP     = "hs_built_hp";
	private static final String R_KEYS   = "hs_roof_keys";
	private static final String R_KINDS  = "hs_roof_kinds";
	private static final String R_HP     = "hs_roof_hp";
	private static final String R_BURN   = "hs_roof_burning";
	private static final String VERSION  = "hs_version";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		storePieces( bundle, built, roofs );
		bundle.put( VERSION, version );
		for (StoreSection s : sections.values()){
			Bundle b = new Bundle();
			s.storeInBundle( b );
			bundle.put( s.getClass().getName(), b );
		}
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		built.clear();
		roofs.clear();
		restorePieces( bundle, built, roofs );
		version = bundle.contains( VERSION ) ? bundle.getInt( VERSION ) : 0;
		for (StoreSection s : sections.values()){
			String name = s.getClass().getName();
			if (bundle.contains( name )) s.restoreFromBundle( bundle.getBundle( name ) );
		}
	}

	private static void storePieces( Bundle bundle, java.util.Map<Long, Built> built, java.util.Map<Long, Roof> roofs ){
		long[] keys = new long[built.size()];
		String[] pieces = new String[built.size()];
		int[] hp = new int[built.size()];
		int i = 0;
		for (java.util.Map.Entry<Long, Built> e : built.entrySet()){
			keys[i] = e.getKey();
			pieces[i] = e.getValue().piece;
			hp[i++] = e.getValue().hp;
		}
		bundle.put( B_KEYS, keys );
		bundle.put( B_PIECES, pieces );
		bundle.put( B_HP, hp );

		long[] rkeys = new long[roofs.size()];
		int[] kinds = new int[roofs.size()], rhp = new int[roofs.size()], burn = new int[roofs.size()];
		i = 0;
		for (java.util.Map.Entry<Long, Roof> e : roofs.entrySet()){
			rkeys[i] = e.getKey();
			kinds[i] = e.getValue().kind;
			rhp[i] = e.getValue().hp;
			burn[i++] = e.getValue().burning;
		}
		bundle.put( R_KEYS, rkeys );
		bundle.put( R_KINDS, kinds );
		bundle.put( R_HP, rhp );
		bundle.put( R_BURN, burn );
	}

	private static void restorePieces( Bundle bundle, java.util.Map<Long, Built> built, java.util.Map<Long, Roof> roofs ){
		long[] keys = bundle.contains( B_KEYS ) ? bundle.getLongArray( B_KEYS ) : null;
		String[] pieces = bundle.contains( B_PIECES ) ? bundle.getStringArray( B_PIECES ) : null;
		int[] hp = bundle.contains( B_HP ) ? bundle.getIntArray( B_HP ) : null;
		if (keys != null && pieces != null && hp != null){
			for (int i = 0; i < Math.min( keys.length, Math.min( pieces.length, hp.length ) ); i++){
				built.put( keys[i], new Built( pieces[i], hp[i] ) );
			}
		}
		long[] rkeys = bundle.contains( R_KEYS ) ? bundle.getLongArray( R_KEYS ) : null;
		int[] kinds = bundle.contains( R_KINDS ) ? bundle.getIntArray( R_KINDS ) : null;
		int[] rhp = bundle.contains( R_HP ) ? bundle.getIntArray( R_HP ) : null;
		int[] burn = bundle.contains( R_BURN ) ? bundle.getIntArray( R_BURN ) : null;
		if (rkeys != null && kinds != null && rhp != null){
			for (int i = 0; i < Math.min( rkeys.length, Math.min( kinds.length, rhp.length ) ); i++){
				Roof r = new Roof( kinds[i], rhp[i] );
				if (burn != null && i < burn.length) r.burning = burn[i];
				roofs.put( rkeys[i], r );
			}
		}
	}

	// ----------------------------------------------------------------- co-op

	/** Co-op host: the pieces and roofs in the world rect a guest's mirror shows, as a bundle's
	 *  text (OverworldLevel.sharedHomestead). Sections stay on the host. */
	public synchronized String shared( int x0, int y0, int x1, int y1 ){
		Bundle b = new Bundle();
		storePieces( b, built.snapshotIn( x0, y0, x1, y1 ), roofs.snapshotIn( x0, y0, x1, y1 ) );
		return b.toString();
	}

	/** A guest's mirror takes over the host's pieces and roofs (shared): what it draws and examines. */
	public synchronized void adoptShared( Bundle b ){
		built.clear();
		roofs.clear();
		restorePieces( b, built, roofs );
		changed();
	}
}
