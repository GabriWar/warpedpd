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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.AwaySettlers;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The threats on a home while the hero was away, SETTLED WHEN HE COMES BACK: every night gone by
 * since the last one done with (ThreatWorld.settledTo) - rolled neither live with him there nor
 * settled before - is rolled as it would have been live (HomeThreats.siegeNight, raidNight), with
 * the home's notice as it stands (nothing moves while he is away, so it is what it was when he
 * left; its light by night counted). A night is gone by once its dark is over (past).
 *
 * - The FIRST raid of an absence empties the richest chest on the home's ground - its things are
 *   gone, not dropped anywhere - and forces the door or gate nearest that chest, taking half its
 *   health. A later raid in the same absence finds nothing more to take: an absence never costs
 *   more than one chest.
 * - A siege night gnaws at the fences and gates: the SIEGE_PIECES outermost of them take
 *   SIEGE_DAMAGE each.
 *
 * Every night that brought something makes a line for the home's ledger. The nights are settled
 * as soon as the home's whole ground is in the window again (ThreatSlice), before tonight is rolled
 * live, so none is ever skipped, also the ones the hero slept through at home; the lines wait
 * (ThreatWorld.keepUntold) until he is back at the home, told by AwaySettlers after a day or more
 * away (settle) or by ThreatSlice after less. An absence ends when he is told. Pure: the same nights,
 * the same home and the same seed settle the same way.
 */
public class ThreatSettler implements AwaySettlers.Settler {

	/** A siege night away: this much to each of the SIEGE_PIECES outermost fences and gates. */
	public static final int SIEGE_DAMAGE = 6, SIEGE_PIECES = 4;
	/** How many of a robbed chest's things the ledger names. */
	static final int NAMED = 3;

	@Override
	public List<String> settle( HomesteadWorld.Home home, int daysAway ){
		ThreatWorld world = ThreatWorld.get();
		if (world == null || !(Dungeon.level instanceof OverworldLevel)) return null;
		OverworldLevel level = (OverworldLevel) Dungeon.level;
		if (level.isMirror() || HomeThreats.hearth( level, home ) == -1) return null;
		settleNights( level, home );
		return world.tell( home );
	}

	/**
	 * Settles every night at a home gone by and not yet done with, its lines kept to be told
	 * (ThreatWorld.keepUntold): how many nights. The home's hearthstone must be in the window.
	 */
	public static int settleNights( OverworldLevel level, HomesteadWorld.Home home ){
		ThreatWorld world = ThreatWorld.get();
		int first = world.settledTo( home ) + 1;
		if (!past( first )) return 0;

		Notice.Score score = Notice.of( level, home, true );
		world.keepNotice( home, score );
		int notice = score.total();
		long seed = level.worldSeed;

		ArrayList<String> lines = new ArrayList<>();
		int night = first;
		for (; past( night ); night++){
			if (HomeThreats.raidNight( seed, home, night, notice )){
				lines.add( world.robbed( home )
						? Messages.get( ThreatSettler.class, "raid_again", night + 1 )
						: rob( level, home, night ) );
			}
			if (HomeThreats.siegeNight( seed, home, night, notice )) lines.add( gnaw( level, home, night ) );
		}
		world.roll( home, night - 1 );
		world.keepUntold( home, lines );
		return night - first;
	}

	/** The first night (WorldClock.night's index) whose dark had not ended at a world turn: the
	 *  first a hero there at that turn may have missed. */
	public static int firstNight( int turn ){
		return Math.floorDiv( turn, DayNightCycle.FULL_CYCLE );
	}

	/** Is a night gone by: an earlier one than tonight's, or tonight's own once its dark is over
	 *  (its dawn come: a real-clock night may run past the day's turn count into the small hours). */
	public static boolean past( int night ){
		int tonight = WorldClock.night();
		if (night != tonight) return night < tonight;
		DayNightCycle.Phase p = DayNightCycle.phase();
		return (long)(night + 1) * DayNightCycle.FULL_CYCLE <= WorldClock.turn()
				&& (p == DayNightCycle.Phase.DAWN || p == DayNightCycle.Phase.DAY);
	}

	//the raid's take: the richest chest emptied (the absence's one), the door or gate nearest it forced
	private static String rob( OverworldLevel level, HomesteadWorld.Home home, int night ){
		StorageChest chest = null;
		long best = -1;
		for (Mob m : level.mobs){
			if (!(m instanceof StorageChest) || ((StorageChest) m).storage.items.isEmpty()
					|| !HomeThreats.inArea( level, home, m.pos )) continue;
			long v = Notice.valueOf( (StorageChest) m );
			if (v > best){
				best = v;
				chest = (StorageChest) m;
			}
		}
		if (chest == null) return Messages.get( ThreatSettler.class, "raid_nothing", home.title(), night + 1 );

		ArrayList<Item> things = new ArrayList<>( chest.storage.items );
		things.sort( Comparator.comparingInt( (Item i) -> Notice.valueOf( i ) ).reversed() );
		StringBuilder named = new StringBuilder();
		for (int i = 0; i < Math.min( NAMED, things.size() ); i++){
			if (named.length() > 0) named.append( ", " );
			named.append( things.get( i ).title() );
		}
		if (things.size() > NAMED) named.append( Messages.get( ThreatSettler.class, "and_more" ) );
		chest.storage.clear();
		ThreatWorld.get().setRobbed( home );

		String door = forceDoor( level, home, chest.pos );
		return door == null
				? Messages.get( ThreatSettler.class, "raid_robbed", home.title(), night + 1, things.size(), named.toString() )
				: Messages.get( ThreatSettler.class, "raid_robbed_door", home.title(), night + 1, things.size(), named.toString(), door );
	}

	//the door or gate on the home's ground nearest a cell takes half its health: its name, or null
	private static String forceDoor( OverworldLevel level, HomesteadWorld.Home home, int near ){
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int c : builtIn( level, home )){
			int t = level.map[c];
			if (!BuiltTerrain.door( t ) && !BuiltTerrain.gate( t )) continue;
			int d = level.distance( c, near );
			if (d < bestD){
				bestD = d;
				best = c;
			}
		}
		if (best == -1) return null;
		BuildPiece p = Homestead.pieceAt( level, best );
		if (p == null) return null;
		Homestead.damage( level, best, Math.max( 1, p.maxHp / 2 ), null );
		return p.name();
	}

	//a siege night: the outermost fences and gates gnawed
	private static String gnaw( OverworldLevel level, HomesteadWorld.Home home, int night ){
		ArrayList<Integer> fences = new ArrayList<>();
		for (int c : builtIn( level, home )){
			BuildPiece p = Homestead.pieceAt( level, c );
			if (p != null && (p.terrain == Terrain.FENCE || p.terrain == Terrain.FENCE_GATE)) fences.add( c );
		}
		//outermost first, the window's order among equals: the same every time
		fences.sort( Comparator.comparingInt( (Integer c) -> -HomeThreats.distance( level, home, c ) ).thenComparingInt( c -> c ) );
		int hurt = 0;
		for (int i = 0; i < Math.min( SIEGE_PIECES, fences.size() ); i++){
			if (Homestead.damage( level, fences.get( i ), SIEGE_DAMAGE, null )) hurt++;
		}
		return hurt > 0
				? Messages.get( ThreatSettler.class, "siege_gnawed", home.title(), night + 1, hurt )
				: Messages.get( ThreatSettler.class, "siege_circled", home.title(), night + 1 );
	}

	//the window cells of every built piece on a home's ground, in the window's order
	private static ArrayList<Integer> builtIn( OverworldLevel level, HomesteadWorld.Home home ){
		ArrayList<Integer> out = new ArrayList<>();
		int r = HomeThreats.AREA;
		level.homestead().forEachBuilt( home.wx - r, home.wy - r, home.wx + r + 1, home.wy + r + 1, ( x, y, b ) -> {
			int c = level.localCell( x, y );
			if (c != -1) out.add( c );
		} );
		out.sort( Integer::compare );
		return out;
	}
}
