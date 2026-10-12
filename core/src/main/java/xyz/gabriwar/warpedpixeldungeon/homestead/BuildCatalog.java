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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Every piece the hero can build, in menu order.
 *
 * The pieces are registered by PROVIDER classes, one per part of the homesteads, looked up BY
 * NAME from the fixed list {@link #PROVIDERS} the first time the catalog is read. A provider that
 * does not exist (yet) is skipped without a word, so each part adds its own provider class and
 * never edits this one or another part's. A provider is a public class with a public no-argument
 * constructor that implements {@link Provider} and calls {@link #register} for each of its pieces.
 * android/proguard-rules.pro keeps every Provider: the shrinker cannot see a class named in a
 * string. The core walls, windows, doors, fences, floors, path, bridge and roofs are CorePieces'.
 */
public final class BuildCatalog {

	/** A part of the homesteads that has pieces to build. */
	public interface Provider {
		void register();
	}

	private static final String PIECES = "xyz.gabriwar.warpedpixeldungeon.homestead.pieces.";

	/** The providers, in the order their pieces are listed. */
	public static final String[] PROVIDERS = {
			PIECES + "CorePieces",        //walls, windows, doors, fences, floors, path, bridge, roofs
			PIECES + "RoomPieces",        //rooms: beds, furniture
			PIECES + "StationPieces",     //stations and crafting
			PIECES + "FarmPieces",        //farming and the animals' pens
			PIECES + "DefencePieces",     //threats: defences
			PIECES + "TravelPieces"       //travel: hearthstone, waystones
	};

	//the catalog once every provider has registered; the one being filled while they do
	private static LinkedHashMap<String, BuildPiece> pieces, loading;

	private BuildCatalog(){}

	private static synchronized LinkedHashMap<String, BuildPiece> pieces(){
		if (pieces != null) return pieces;
		//a provider registering its pieces adds them to the list being made
		if (loading != null) return loading;
		loading = new LinkedHashMap<>();
		try {
			for (String name : PROVIDERS){
				Class<?> cls;
				try {
					cls = Class.forName( name );
				} catch (ClassNotFoundException e){
					continue;
				}
				try {
					((Provider) cls.getConstructor().newInstance()).register();
				} catch (ReflectiveOperationException | ClassCastException e){
					throw new IllegalStateException( "build piece provider " + name + " is not a public BuildCatalog.Provider", e );
				}
			}
			//only a whole catalog is kept: a provider that failed fails every read alike, never
			//leaving the pieces after it missing
			pieces = loading;
		} finally {
			loading = null;
		}
		return pieces;
	}

	/** Adds a piece at the end of the list; a provider calls it. Ids are unique. */
	public static synchronized void register( BuildPiece piece ){
		//the providers' own pieces first: a registration from anywhere else comes after them
		LinkedHashMap<String, BuildPiece> all = pieces();
		if (all.containsKey( piece.id )) throw new IllegalArgumentException( "a build piece is already called " + piece.id );
		all.put( piece.id, piece );
	}

	/** Every piece, in menu order. */
	public static List<BuildPiece> all(){
		return Collections.unmodifiableList( new ArrayList<>( pieces().values() ) );
	}

	/** The pieces of one group of the menu, in order. */
	public static List<BuildPiece> in( BuildCategory category ){
		ArrayList<BuildPiece> out = new ArrayList<>();
		for (BuildPiece p : pieces().values()) if (p.category == category) out.add( p );
		return out;
	}

	/** The piece with that id, or null. */
	public static BuildPiece byId( String id ){
		return id == null ? null : pieces().get( id );
	}

	/** The TERRAIN piece that lays this terrain, or null (an open door finds its door's piece:
	 *  BuiltTerrain.closed). */
	public static BuildPiece forTerrain( int terrain ){
		int t = BuiltTerrain.closed( terrain );
		for (BuildPiece p : pieces().values()) if (p.kind == BuildPiece.Kind.TERRAIN && p.terrain == t) return p;
		return null;
	}

	/** The ROOF piece of a roof kind, or null. */
	public static BuildPiece forRoof( int kind ){
		for (BuildPiece p : pieces().values()) if (p.kind == BuildPiece.Kind.ROOF && p.roofKind == kind) return p;
		return null;
	}
}
