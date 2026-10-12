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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SectorMap;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A slice's felled trees growing back (HomesteadStore's gathering section): every stump the hero
 * left, by world cell, with the day it was felled (WorldClock.day()). A stump grows only through
 * spring and summer: SAPLING_DAYS such days after the felling it is a sapling, TREE_DAYS after it
 * the tree it was again. Nothing grows while nobody looks: the stage is worked out from the felling
 * day and the calendar when the window lays the cell (the first turn on it) and once a day after,
 * so a stump left for a summer is a tree when the hero comes back. A stump dug out, or built over,
 * is forgotten.
 *
 * The stump and the sapling are terrain (Terrain.STUMP_*, SAPLING_*), kept with the player's other
 * edits by the diff store; the tree grown back is the generator's own again, so its edit is let go.
 * Seeds the hero plants (an acorn, a pine cone, a bush seed: items/treeseeds) are followed the same
 * way from the day they went in: a sapling grows into its tree, a young bush into a bush.
 */
public class GatheringSlice extends StoreSection {

	/** Growing days (spring or summer) after the felling when the stump is a sapling, and the tree. */
	public static final int SAPLING_DAYS = 4, TREE_DAYS = 14;
	/** Growing days after a bush seed is planted when the young bush has filled out. */
	public static final int BUSH_DAYS = 6;

	//felled trees still growing back: world cell -> the day each was felled
	private final SectorMap<Integer> stumps = new SectorMap<>();

	//when the stumps in the window were last brought up to date: the day, and the window's version
	//(a new window lays cells nobody has looked at yet). never saved: a load looks again
	private int passDay = Integer.MIN_VALUE, passWindow = Integer.MIN_VALUE;
	private boolean due = true;

	public GatheringSlice(){
	}

	/** A tree was felled on a window cell today (or on the day given): its stump starts growing back. */
	public synchronized void felled( OverworldLevel ow, int cell, int day ){
		stumps.put( key( ow, cell ), day );
	}

	/** A seed was planted on a window cell on the day given (the cell already its sapling or young
	 *  bush): it grows from then as a felled tree's sapling does, or into a bush. */
	public void planted( OverworldLevel ow, int cell, int day ){
		felled( ow, cell, day );
	}

	/** A stump or sapling dug out: it grows no more. */
	public synchronized void forget( OverworldLevel ow, int cell ){
		stumps.remove( key( ow, cell ) );
	}

	/** The day the tree on a world cell was felled, null when nothing grows back there. */
	public synchronized Integer felledOn( int wx, int wy ){
		return stumps.get( OverworldLevel.worldKey( wx, wy ) );
	}

	public synchronized int growing(){
		return stumps.size();
	}

	private static long key( OverworldLevel ow, int cell ){
		return OverworldLevel.worldKey( ow.worldX + cell % ow.width(), ow.worldY + cell / ow.width() );
	}

	// ----------------------------------------------------------------- the hooks

	@Override
	public void materialize( OverworldLevel level ){
		due = true;
	}

	@Override
	public synchronized void cellChanged( OverworldLevel level, int cell ){
		long k = key( level, cell );
		if (stumps.containsKey( k ) && !growingThing( level.map[cell] )) stumps.remove( k );
	}

	@Override
	public void worldTurn( OverworldLevel level ){
		int today = WorldClock.day();
		if (!due && today == passDay && level.windowVersion() == passWindow) return;
		due = false;
		passDay = today;
		passWindow = level.windowVersion();
		regrow( level, today );
	}

	// ----------------------------------------------------------------- growing back

	/**
	 * Brings every stump and sapling in the window to the stage its growing days since the felling
	 * have earned: stump, sapling, then the tree again (no longer followed). A sapling that would
	 * be a tree waits while someone stands on it or something lies on it. Only ever forward. Every
	 * cell that grew is redrawn at once, as one (Gathering.changed): a cleared wood of a hundred
	 * stumps is one pass of the dressing, not a hundred.
	 */
	public void regrow( OverworldLevel ow, int today ){
		final ArrayList<int[]> found = new ArrayList<>();
		synchronized (this){
			stumps.forEachIn( ow.worldX + 1, ow.worldY + 1, ow.worldX + ow.width() - 1, ow.worldY + ow.height() - 1,
					(x, y, day) -> found.add( new int[]{ x, y, day } ) );
		}
		HashMap<Integer, Boolean> season = new HashMap<>();
		int[] grew = new int[found.size()];
		int n = 0;
		for (int[] f : found){
			int c = ow.localCell( f[0], f[1] );
			if (c == -1) continue;
			int t = ow.map[c];
			if (!growingThing( t )){
				synchronized (this){
					stumps.remove( OverworldLevel.worldKey( f[0], f[1] ) );
				}
				continue;
			}
			if (t == Terrain.SAPLING_SHRUB){
				//a young bush fills out once it has had its growing days; a bush is solid, so it
				//waits while someone stands on it or something lies on it
				if (growingDays( f[2], today, BUSH_DAYS, season ) < BUSH_DAYS
						|| Actor.findChar( c ) != null || ow.heaps.get( c ) != null) continue;
				Level.set( c, Terrain.SHRUB, ow );
				grew[n++] = c;
				continue;
			}
			boolean oak = t == Terrain.STUMP_OAK || t == Terrain.SAPLING_OAK;
			int stage = stage( f[2], today, season );
			if (stage == 2 && (Actor.findChar( c ) != null || ow.heaps.get( c ) != null)) stage = 1;
			int want = stage == 0 ? (oak ? Terrain.STUMP_OAK : Terrain.STUMP_PINE)
					: stage == 1 ? (oak ? Terrain.SAPLING_OAK : Terrain.SAPLING_PINE)
					: (oak ? Terrain.TREE_OAK : Terrain.TREE_PINE);
			//never back: a sapling stays one whatever a clock set back says
			if (want == t || (BuiltTerrain.sapling( t ) && stage == 0)) continue;
			Level.set( c, want, ow );
			grew[n++] = c;
		}
		//the section's own hook forgets the trees grown back with the rest
		if (n > 0) Gathering.changed( ow, Arrays.copyOf( grew, n ) );
	}

	//a stump or a sapling: what grows back
	private static boolean growingThing( int t ){
		return BuiltTerrain.stump( t ) || BuiltTerrain.sapling( t );
	}

	/** The stage a tree felled on one day has reached on another: 0 the stump, 1 a sapling, 2 the
	 *  tree grown back. */
	public static int stage( int felled, int today ){
		return stage( felled, today, new HashMap<>() );
	}

	private static int stage( int felled, int today, HashMap<Integer, Boolean> season ){
		int days = growingDays( felled, today, TREE_DAYS, season );
		return days >= TREE_DAYS ? 2 : days >= SAPLING_DAYS ? 1 : 0;
	}

	/** The days after the felling, up to today, that were spring or summer (counted up to `cap`). */
	public static int growingDays( int felled, int today, int cap ){
		return growingDays( felled, today, cap, new HashMap<>() );
	}

	private static int growingDays( int felled, int today, int cap, HashMap<Integer, Boolean> season ){
		int n = 0;
		for (int d = felled + 1; d <= today && n < cap; d++){
			Boolean grows = season.get( d );
			if (grows == null){
				GameCalendar.Season s = GameCalendar.seasonOfDay( d );
				grows = s == GameCalendar.Season.SPRING || s == GameCalendar.Season.SUMMER;
				season.put( d, grows );
			}
			if (grows) n++;
		}
		return n;
	}

	// ----------------------------------------------------------------- the bundle

	private static final String KEYS = "stumps_keys";
	private static final String DAYS = "stumps_days";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		long[] keys = new long[stumps.size()];
		int[] days = new int[stumps.size()];
		int i = 0;
		for (java.util.Map.Entry<Long, Integer> e : stumps.entrySet()){
			keys[i] = e.getKey();
			days[i++] = e.getValue();
		}
		bundle.put( KEYS, keys );
		bundle.put( DAYS, days );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		stumps.clear();
		due = true;
		if (!bundle.contains( KEYS ) || !bundle.contains( DAYS )) return;
		long[] keys = bundle.getLongArray( KEYS );
		int[] days = bundle.getIntArray( DAYS );
		for (int i = 0; i < Math.min( keys.length, days.length ); i++) stumps.put( keys[i], days[i] );
	}
}
