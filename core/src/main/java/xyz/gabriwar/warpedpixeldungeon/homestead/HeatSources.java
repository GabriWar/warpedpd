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

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CoalBed;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ForgeCoals;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.NestCoals;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * What warms a room: the heat each cell of a slice gives the room it is in, in degrees (C) added
 * to the room's air (Rooms.Room.air), summed over the room's floor. A room with any heat is HEATED,
 * and the hero is Comfy in it (no hypothermia, no heatstroke).
 *
 * Built in: a bed of coals burning on the cell (a camp fire's, a forge's or a nest's CoalBed - the
 * hearth is a camp fire's coal bed set against a wall) gives COALS.
 * The rest comes from PROVIDERS, looked up by class name (a missing one is skipped), each a
 * public class with a public no-argument constructor implementing Provider; one made in code (a
 * test, a debug scene) is added with register. Each is asked for every floor cell of a room
 * whenever the room's heat is read (the climate reads it every turn), so it must be cheap: look at
 * what stands on that one cell. The heat lives only in the room: a cell's own warmth beside a fire
 * is the fire's tile heat (TileTemperature), as it always was.
 *
 * <pre>
 * public class StationHeat implements HeatSources.Provider {
 *     public float heatAt( OverworldLevel level, int cell ){
 *         Mob m = ...the station standing on the cell...;
 *         return m instanceof Kiln && ((Kiln) m).firing() ? 4f : 0f;
 *     }
 * }
 * </pre>
 */
public final class HeatSources {

	private HeatSources(){}

	/** A part of the homesteads whose things warm a room. */
	public interface Provider {
		/** The heat (C) what stands on this cell gives the room it is in; 0 for none. */
		float heatAt( OverworldLevel level, int cell );
	}

	/** A bed of coals burning, any kind: what it gives its room, C. */
	public static final float COALS = 6f;

	private static final String PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/** The providers, by class name. */
	public static final String[] PROVIDERS = {
			PKG + "stations.StationHeat"    //stations: the kiln, the forge, the hearth beyond its coals
	};

	private static final Providers<Provider> providers = new Providers<>( Provider.class, PROVIDERS );

	/** Adds a provider made in code (a test, a debug scene). */
	public static void register( Provider p ){
		providers.add( p );
	}

	public static void unregister( Provider p ){
		providers.remove( p );
	}

	/** The heat one cell of a slice gives the room it is in: its burning coals and every provider's. */
	public static float heatAt( OverworldLevel level, int cell ){
		return heatAt( level, cell, coalBeds( level ), providers.all() );
	}

	/** The heat of a room: the sum over its floor. */
	public static float roomHeat( OverworldLevel level, Rooms.Room room ){
		ArrayList<CoalBed> coals = coalBeds( level );
		List<Provider> all = providers.all();
		float heat = 0f;
		for (int cell : room.cells) heat += heatAt( level, cell, coals, all );
		return heat;
	}

	private static float heatAt( OverworldLevel level, int cell, ArrayList<CoalBed> coals, List<Provider> all ){
		float heat = 0f;
		for (CoalBed b : coals) if (b.burning( cell )) heat += COALS;
		for (Provider p : all) heat += p.heatAt( level, cell );
		return heat;
	}

	//every kind of coal bed (as BellowsDraft.COALS): looked up by kind, not by walking the level's
	//blobs, since the render thread reads the climate too
	@SuppressWarnings("unchecked")
	private static final Class<? extends CoalBed>[] COAL_KINDS = new Class[]{ CampFire.class, ForgeCoals.class, NestCoals.class };

	//the level's beds of coals with any coal in them
	private static ArrayList<CoalBed> coalBeds( OverworldLevel level ){
		ArrayList<CoalBed> out = new ArrayList<>();
		for (Class<? extends CoalBed> kind : COAL_KINDS){
			Blob b = level.blobs.get( kind );
			if (b != null && b.volume > 0) out.add( (CoalBed) b );
		}
		return out;
	}
}
