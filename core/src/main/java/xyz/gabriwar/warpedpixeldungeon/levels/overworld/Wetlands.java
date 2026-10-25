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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DrownedAcolyte;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.PearlOyster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.WildCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SunkenIdol;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SwampWitch;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.WitchCauldron;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.Pearl;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.Seashell;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.DrownedAcolyteSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * The wetlands' places at work on a surface level (WetlandSites lays them, this keeps them): the
 * witch and her cauldron in her hut and the idol in its shrine, set down once a place is whole in
 * the window; each place found when it is first seen (a line in the log, its page in the guide);
 * the shrine's drowned acolytes rising by night when a hero comes near; and a beach's pools - the
 * shells and pearls the sea leaves in them at low water and takes back as it comes in, refilled
 * WetlandSites.POOL_REFILL a day up to POOL_CAP, the crabs about them by day and the pearl oyster
 * that settles in one now and then.
 *
 * Host only: a co-op guest's mirror is sent the mobs and the heaps this lays. Saved with the level
 * (the places found, the pools' stock) under keys an older save simply lacks.
 */
public final class Wetlands {

	private final OverworldLevel level;

	//the places a hero has seen, by key
	private final HashSet<Long> found = new HashSet<>();
	//a beach's pools by key: {what they hold, the world day that was counted on}
	private final LinkedHashMap<Long, int[]> pools = new LinkedHashMap<>();
	//not saved: the tide each beach was last seen at (true: out), for the line when it turns
	private final HashMap<Long, Boolean> tideSeen = new HashMap<>();

	/** How near a hero comes before a shrine's acolytes rise, and a beach's tide is told. */
	public static final int GUARD_NEAR = 10, TIDE_NEAR = 12;
	//found when a hero has seen any cell this near its centre
	private static final int SEEN_NEAR = 2;
	//the once-keys' salts (OverworldLevel.dueOnce)
	private static final long GUARDS = 0x6A2D5L, CRABS = 0xC2AB5L, OYSTER = 0x0957E2L;

	Wetlands( OverworldLevel level ){
		this.level = level;
	}

	//every place whose grounds and a cell round them lie inside the window's populated band
	private ArrayList<WetlandSites.Place> whole(){
		int x0 = level.worldX + 2, y0 = level.worldY + 2;
		int x1 = level.worldX + level.width() - 3, y1 = level.worldY + level.height() - 3;
		ArrayList<WetlandSites.Place> out = new ArrayList<>();
		for (WetlandSites.Place p : WetlandSites.placesIn( level.worldSeed, x0, y0, x1, y1 )){
			if (p.cx - WetlandSites.REACH - 1 < x0 || p.cy - WetlandSites.REACH - 1 < y0
					|| p.cx + WetlandSites.REACH + 1 > x1 || p.cy + WetlandSites.REACH + 1 > y1) continue;
			out.add( p );
		}
		return out;
	}

	private int cell( int[] wxy ){
		return level.localCell( wxy[0], wxy[1] );
	}

	// ------------------------------------------------------------ their folk

	/** The witch, her cauldron and the shrines' idols of the window, set down where none stands
	 *  yet (and a witch never again once she fled). Each a once-per-place piece of furniture, kept
	 *  forever in the parked store as every NPC is. */
	public void populate(){
		if (level.isMirror() || level.altitude() != 0) return;
		for (WetlandSites.Place p : whole()){
			long key = p.key();
			switch (p.kind){
				case WITCH_HUT:
					if (!level.clearedSites().contains( key ) && !standing( SwampWitch.class, key )){
						SwampWitch w = SwampWitch.of( key );
						w.openShop();
						level.addMob( w, cell( WetlandSites.witchCell( p ) ) );
					}
					if (!standing( WitchCauldron.class, key )){
						level.addMob( WitchCauldron.of( key ), cell( WetlandSites.cauldronCell( p ) ) );
					}
					break;
				case SUNKEN_SHRINE:
					if (!standing( SunkenIdol.class, key )){
						level.addMob( SunkenIdol.of( key ), cell( WetlandSites.idolCell( p ) ) );
					}
					break;
				default:
			}
		}
	}

	//is one of this place's kind of furniture in the window or parked?
	private boolean standing( Class<? extends Mob> kind, long key ){
		for (Mob m : level.mobs) if (kind.isInstance( m ) && keyOf( m ) == key) return true;
		for (Mob m : level.parked()) if (kind.isInstance( m ) && keyOf( m ) == key) return true;
		return false;
	}

	private static long keyOf( Mob m ){
		if (m instanceof SwampWitch) return ((SwampWitch) m).siteKey;
		if (m instanceof WitchCauldron) return ((WitchCauldron) m).siteKey;
		if (m instanceof SunkenIdol) return ((SunkenIdol) m).siteKey;
		if (m instanceof PearlOyster) return ((PearlOyster) m).poolsKey;
		if (m instanceof DrownedAcolyte) return ((DrownedAcolyte) m).shrineKey;
		return Long.MIN_VALUE;
	}

	// ------------------------------------------------------------ the clock

	/** The host's step or turn (OverworldLevel.tickWorld): places found, the swamp's page, the
	 *  acolytes rising, the pools' tide. */
	void tick( int heroPos ){
		if (level.isMirror() || level.altitude() != 0 || heroPos < 0) return;
		//the swamp's own page, the first time a hero stands in one (once pages write themselves: no
		//building the guide's graph every step of a walk through a swamp before then)
		if (level.swampAt( heroPos ) && GuideProgress.descended() && !GuideProgress.pageFound( SWAMP_PAGE )){
			GuideGraph.reveal( GuideGraph.TAG_SWAMP );
		}
		ArrayList<WetlandSites.Place> places = whole();
		if (places.isEmpty()) return;
		ArrayList<Hero> heroes = OverworldLevel.heroesOn( level );
		for (WetlandSites.Place p : places){
			discover( p, heroes );
			if (p.kind == WorldStructures.Site.SUNKEN_SHRINE) guards( p, heroes );
			else if (p.kind == WorldStructures.Site.TIDE_POOLS) tide( p, heroes );
		}
	}

	/** The guide's page on the swamps (GuideGraph). */
	public static final String SWAMP_PAGE = "wet_swamps";

	private void discover( WetlandSites.Place p, ArrayList<Hero> heroes ){
		long key = p.key();
		if (found.contains( key ) || level.visited == null) return;
		boolean seen = false;
		for (int dy = -SEEN_NEAR; dy <= SEEN_NEAR && !seen; dy++){
			for (int dx = -SEEN_NEAR; dx <= SEEN_NEAR && !seen; dx++){
				int c = level.localCell( p.cx + dx, p.cy + dy );
				seen = c != -1 && level.visited[c];
			}
		}
		if (!seen) return;
		found.add( key );
		GuideGraph.reveal( tagOf( p.kind ) );
		int c = level.localCell( p.cx, p.cy );
		String line = Messages.get( WetlandSites.class, "found_" + p.kind.name().toLowerCase() );
		for (Hero h : heroes){
			if (level.distance( h.pos, c ) <= 14) NetManager.heroLog( h, GLog.HIGHLIGHT + line );
		}
	}

	/** The guide's discovery for a place of this kind. */
	public static String tagOf( WorldStructures.Site kind ){
		switch (kind){
			case WITCH_HUT:     return GuideGraph.TAG_WITCH_HUT;
			case SUNKEN_SHRINE: return GuideGraph.TAG_SUNKEN_SHRINE;
			default:            return GuideGraph.TAG_TIDE_POOLS;
		}
	}

	/** Has a hero of this level found the place? */
	public boolean found( long key ){
		return found.contains( key );
	}

	//by night, a hero coming near a shrine: its drowned acolytes rise out of the water round it,
	//once a night
	private void guards( WetlandSites.Place p, ArrayList<Hero> heroes ){
		if (!DayNightCycle.isNight()) return;
		int home = level.localCell( p.cx, p.cy );
		Hero near = null;
		for (Hero h : heroes){
			if (level.distance( h.pos, home ) <= GUARD_NEAR){
				near = h;
				break;
			}
		}
		if (near == null) return;
		int night = WorldClock.night();
		//the night's own key, kept two days: a real-clock night runs past midnight into the next
		//day, and must not raise its guards a second time there
		if (!level.dueOnce( OverworldLevel.structHash( p.key() ^ GUARDS, night ), 2 )) return;
		ArrayList<Integer> water = new ArrayList<>();
		for (int dy = -3; dy <= 3; dy++){
			for (int dx = -3; dx <= 3; dx++){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) < 2) continue;
				int c = level.localCell( p.cx + dx, p.cy + dy );
				if (c == -1 || level.map[c] != Terrain.WATER || Actor.findChar( c ) != null || level.occupied( c )) continue;
				//never right beside a hero: they rise at the shrine, not at his elbow
				boolean clear = true;
				for (Hero h : heroes) clear &= level.distance( h.pos, c ) > 1;
				if (clear) water.add( c );
			}
		}
		Random.shuffle( water );
		int n = Math.min( water.size(), WetlandSites.guardCount( p.key(), night ) );
		boolean shown = false;
		for (int i = 0; i < n; i++){
			int c = water.get( i );
			DrownedAcolyte a = DrownedAcolyte.of( p.key(), p.cx, p.cy );
			if (!level.addMob( a, c )) continue;
			if (level.liveScene() && a.sprite != null){
				if (a.sprite instanceof DrownedAcolyteSprite) ((DrownedAcolyteSprite) a.sprite).rise();
				if (level.heroFOV[a.pos]){
					Splash.at( a.pos, 0x5C7A6A, 10 );
					shown = true;
				}
			}
		}
		if (shown){
			SpatialSound.play( Assets.Sounds.WATER, home, 0.9f, 0.7f );
			for (Hero h : heroes){
				if (level.distance( h.pos, home ) <= GUARD_NEAR + 4){
					NetManager.heroLog( h, GLog.WARNING + Messages.get( WetlandSites.class, "acolytes_rise" ) );
				}
			}
		}
	}

	// ------------------------------------------------------------ the pools

	/** What a beach's pools hold now ({stock, the day counted}), refilled for today. */
	int[] stock( long key ){
		int[] s = pools.get( key );
		int today = WorldClock.day();
		if (s == null){
			s = new int[]{ WetlandSites.refilled( 0, Integer.MIN_VALUE, today ), today };
			pools.put( key, s );
		} else if (s[1] != today){
			s[0] = WetlandSites.refilled( s[0], s[1], today );
			s[1] = today;
		}
		return s;
	}

	//the pools' cells in the window that still hold water (open or iced)
	private ArrayList<Integer> poolCells( WetlandSites.Place p ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int i = 0; i < p.pools.length; i += 2){
			int c = level.localCell( p.pools[i], p.pools[i + 1] );
			if (c != -1 && (level.map[c] == Terrain.WATER || level.map[c] == Terrain.FROZEN_WATER)) out.add( c );
		}
		return out;
	}

	private static boolean find( Item item ){
		return item instanceof Seashell || item instanceof Pearl;
	}

	//a heap of nothing but the pools' finds
	private static boolean finds( Heap h ){
		if (h == null || h.type != Heap.Type.HEAP || h.items.isEmpty()) return false;
		for (Item i : h.items) if (!find( i )) return false;
		return true;
	}

	//the sea at a beach's pools: out, it leaves what they hold in them (and a crab or two by day, a
	//pearl oyster now and then); in - or the pools iced over - it takes back what lies in them
	private void tide( WetlandSites.Place p, ArrayList<Hero> heroes ){
		long key = p.key();
		int[] s = stock( key );
		ArrayList<Integer> cells = poolCells( p );
		if (cells.isEmpty()) return;
		boolean iced = level.map[cells.get( 0 )] == Terrain.FROZEN_WATER;
		boolean out = WetlandSites.tideOut( WorldClock.turn() ) && !iced;
		if (out){
			for (int c : cells){
				if (s[0] <= 0) break;
				if (level.heaps.get( c ) != null || Actor.findChar( c ) != null) continue;
				Item item = Random.Int( WetlandSites.PEARL_ONE_IN ) == 0 ? new Pearl() : new Seashell();
				Heap h = level.drop( item, c );
				h.seen = level.heroFOV != null && level.heroFOV[c];
				s[0]--;
			}
			crabs( p, cells, heroes );
			oyster( p, cells );
		} else {
			for (int c : cells){
				Heap h = level.heaps.get( c );
				if (!finds( h )) continue;
				int n = 0;
				for (Item i : h.items) n += i.quantity();
				h.destroy();
				s[0] = Math.min( WetlandSites.POOL_CAP, s[0] + n );
			}
		}
		//the tide turning, told to whoever stands by the pools
		Boolean was = tideSeen.put( key, out );
		if (was != null && was != out && !iced){
			int home = level.localCell( p.cx, p.cy );
			for (Hero h : heroes){
				if (level.distance( h.pos, home ) <= TIDE_NEAR){
					NetManager.heroLog( h, Messages.get( WetlandSites.class, out ? "tide_out" : "tide_in" ) );
				}
			}
		}
	}

	//a crab or two among the pools by day at low water, once a day, out of every hero's sight
	private void crabs( WetlandSites.Place p, ArrayList<Integer> pools, ArrayList<Hero> heroes ){
		if (DayNightCycle.phase() != DayNightCycle.Phase.DAY) return;
		ArrayList<Integer> sand = new ArrayList<>();
		for (int c : pools){
			for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
				int m = c + n;
				if (m < 0 || m >= level.length() || !level.passable[m] || level.water[m]
						|| Actor.findChar( m ) != null || level.occupied( m ) || !unseen( m, heroes )) continue;
				if (!sand.contains( m )) sand.add( m );
			}
		}
		if (sand.isEmpty() || !level.dueOnce( OverworldLevel.structHash( p.key() ^ CRABS, 0 ), 1 )) return;
		Random.shuffle( sand );
		int n = Math.min( sand.size(), Random.IntRange( 1, 2 ) );
		for (int i = 0; i < n; i++){
			level.addMob( Random.Int( 2 ) == 0 ? new WildCrab() : new Crab(), sand.get( i ) );
		}
	}

	//a pearl oyster settled in a pool at low water, once in OYSTER_DAYS, while none is there
	private void oyster( WetlandSites.Place p, ArrayList<Integer> pools ){
		if (standing( PearlOyster.class, p.key() )) return;
		ArrayList<Integer> free = new ArrayList<>();
		for (int c : pools){
			if (level.heaps.get( c ) == null && Actor.findChar( c ) == null && !level.occupied( c )
					&& (level.heroFOV == null || !level.heroFOV[c])) free.add( c );
		}
		if (free.isEmpty() || !level.dueOnce( OverworldLevel.structHash( p.key() ^ OYSTER, 0 ), WetlandSites.OYSTER_DAYS )) return;
		level.addMob( PearlOyster.of( p.key() ), free.get( Random.Int( free.size() ) ) );
	}

	//out of every hero's sight (the host's field of view; a guest's hero, when he is more than his
	//view's reach away)
	private boolean unseen( int cell, ArrayList<Hero> heroes ){
		if (level.heroFOV != null && level.heroFOV[cell]) return false;
		for (Hero h : heroes){
			if (h != Dungeon.hero && level.distance( h.pos, cell ) <= 8) return false;
		}
		return true;
	}

	// ------------------------------------------------------------ the grounds

	/** Is this window cell within a place's grounds (no wildlife is set down in a hut or a shrine)? */
	boolean inBerth( int cell ){
		if (cell < 0 || cell >= level.length()) return false;
		int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
		for (WetlandSites.Place p : WetlandSites.placesIn( level.worldSeed, wx, wy, wx, wy )){
			if (p.kind != WorldStructures.Site.TIDE_POOLS && p.holds( wx, wy )) return true;
		}
		return false;
	}

	/** What a cell of a place is called when examined, or null: a tide pool, a shrine's statue. */
	String tileName( int cell ){
		String k = tileKind( cell );
		return k == null ? null : Messages.get( WetlandSites.class, k );
	}

	/** ...and what the examining says of it: a pool tells the tide. */
	String tileDesc( int cell ){
		String k = tileKind( cell );
		if (k == null) return null;
		if (k.equals( "pool" )){
			long turn = WorldClock.turn();
			int hours = Math.max( 1, Math.round( 24f * WetlandSites.turnsToTurn( turn ) / DayNightCycle.FULL_CYCLE ) );
			if (level.map[cell] == Terrain.FROZEN_WATER) return Messages.get( WetlandSites.class, "pool_desc_iced" );
			return Messages.get( WetlandSites.class, WetlandSites.tideOut( turn ) ? "pool_desc_out" : "pool_desc_in", hours );
		}
		return Messages.get( WetlandSites.class, k + "_desc" );
	}

	private String tileKind( int cell ){
		if (level.altitude() != 0 || cell < 0 || cell >= level.length()) return null;
		int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
		for (WetlandSites.Place p : WetlandSites.placesIn( level.worldSeed, wx, wy, wx, wy )){
			int t = WetlandSites.terrain( p, wx, wy );
			if (t == -1 || level.map[cell] != t && !(t == Terrain.WATER && level.map[cell] == Terrain.FROZEN_WATER)) continue;
			if (p.kind == WorldStructures.Site.TIDE_POOLS && t == Terrain.WATER) return "pool";
			if (p.kind == WorldStructures.Site.SUNKEN_SHRINE && t == Terrain.STATUE) return "statue";
			if (p.kind == WorldStructures.Site.SUNKEN_SHRINE && t == Terrain.WALL) return "shrine_wall";
			if (p.kind == WorldStructures.Site.WITCH_HUT && t == Terrain.BRIDGE) return "boardwalk";
		}
		return null;
	}

	// ------------------------------------------------------------ saved

	private static final String FOUND = "wet_found";
	private static final String POOL_KEYS = "wet_pool_keys";
	private static final String POOL_STOCK = "wet_pool_stock";
	private static final String POOL_DAY = "wet_pool_day";

	void store( Bundle bundle ){
		long[] f = new long[found.size()];
		int i = 0;
		for (long k : found) f[i++] = k;
		bundle.put( FOUND, f );
		long[] keys = new long[pools.size()];
		int[] stock = new int[pools.size()], day = new int[pools.size()];
		i = 0;
		for (java.util.Map.Entry<Long, int[]> e : pools.entrySet()){
			keys[i] = e.getKey();
			stock[i] = e.getValue()[0];
			day[i++] = e.getValue()[1];
		}
		bundle.put( POOL_KEYS, keys );
		bundle.put( POOL_STOCK, stock );
		bundle.put( POOL_DAY, day );
	}

	//an older save has none of it: nothing found, every beach's pools full on the first visit
	void restore( Bundle bundle ){
		found.clear();
		pools.clear();
		tideSeen.clear();
		long[] f = bundle.contains( FOUND ) ? bundle.getLongArray( FOUND ) : null;
		if (f != null) for (long k : f) found.add( k );
		long[] keys = bundle.contains( POOL_KEYS ) ? bundle.getLongArray( POOL_KEYS ) : null;
		int[] stock = bundle.contains( POOL_STOCK ) ? bundle.getIntArray( POOL_STOCK ) : null;
		int[] day = bundle.contains( POOL_DAY ) ? bundle.getIntArray( POOL_DAY ) : null;
		if (keys != null && stock != null && day != null){
			for (int i = 0; i < Math.min( keys.length, Math.min( stock.length, day.length ) ); i++){
				pools.put( keys[i], new int[]{ stock[i], day[i] } );
			}
		}
	}
}
