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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shark;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.LighthouseKeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.VillageFisher;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sea.Seafaring;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The sea of the surface, as one OverworldLevel holds it (docs/sea.md): the islands and the places
 * of the sea its window reaches (SeaSites, adopted with every window), the ones its heroes have
 * found, the village fishers' stalls set up, the sharks of its open water, every hero's moored boat
 * for the art, and the share of all that a co-op guest's mirror shows.
 *
 * Threads: the window's data is swapped whole on the actor thread (adopt) and only read elsewhere;
 * found places and moorings reach the render thread as immutable snapshots. A mirror finds, spawns
 * and lays nothing: its host's share (adoptShared) is all it knows.
 */
public final class SeaWorld {

	/** The kinds of the sea's places a hero finds, as the found store keeps them. */
	public static final int ISLAND = 0, WRECK = 1, LIGHTHOUSE = 2;

	/** A hero within this of a place hears its found line. */
	static final int FOUND_HEARD = 12;
	/** The sharks of a window: at most this many by day, one more at night, set down out of sight
	 *  SHARK_NEAR to SHARK_FAR cells from a hero, gone once they stray LOST cells from every hero. */
	static final int SHARKS = 2, SHARK_NEAR = 14, SHARK_FAR = 30, LOST = 40;
	//world turns between two looks at the sharks
	static final int SHARK_EVERY = 4;
	/** A fisher sets up on a bank with deep water within this of it. */
	static final int FISHER_DEEP = 3;

	private final OverworldLevel level;

	//the window's islands' cells (WindowGenerator.Window.island) and every place of the sea it reaches
	private volatile byte[] island;
	private volatile List<SeaSites.Sector> sectors = Collections.emptyList();

	//found: a place's world key -> its kind, in the order found; and the snapshot the map, the HUD
	//and the art read on the render thread
	private final LinkedHashMap<Long, Integer> found = new LinkedHashMap<>();
	private volatile Found[] foundSnapshot = new Found[0];

	//the villages whose fisher has set up her stall (sector keys)
	private final HashSet<Long> fishers = new HashSet<>();

	//the boats moored on the slice, {wx, wy, kind ordinal} each: worked out on the host, shipped to a mirror
	private volatile int[][] moorings = new int[0][];

	//the islands whose buried chest is dug up (the world keys of their marked cells): their marks are
	//gone. the snapshot is what the art, the examine window and a guest's mirror read
	private final HashSet<Long> dug = new HashSet<>();
	private volatile long[] dugSnapshot = new long[0];

	private int sharkIn = 0;

	SeaWorld( OverworldLevel level ){
		this.level = level;
	}

	// ------------------------------------------------------------------ the window

	/** A new window: its islands' cells and the sea's places it reaches (OverworldLevel.adoptSites). */
	void adopt( WindowGenerator.Window w ){
		island = w.island;
		sectors = w.sea != null ? w.sea : Collections.<SeaSites.Sector>emptyList();
		if (!level.isMirror()) refreshMoorings();
	}

	/** The sea's places the window reaches: islands, wrecks and lighthouses. Any thread. */
	public List<SeaSites.Sector> sectors(){
		return sectors;
	}

	/** Is this window cell an island's ground? Any thread. */
	public boolean islandAt( int cell ){
		byte[] i = island;
		return i != null && cell >= 0 && cell < i.length && i[cell] != 0;
	}

	/** Does a palm of the warm isles stand on this window cell (the sea's art draws it)? */
	public boolean palmAt( int cell ){
		byte[] i = island;
		return i != null && cell >= 0 && cell < i.length && i[cell] == WindowGenerator.ISLAND_PALM
				&& level.map[cell] == Terrain.TREE_OAK;
	}

	// ------------------------------------------------------------------ found places

	/** A place of the sea a hero has found, as the render thread reads it: its key and cell, its kind,
	 *  and the name it goes by. */
	public static final class Found {
		public final long key;
		public final int kind, x, y;
		public final String name;

		Found( long seed, long key, int kind ){
			this.key = key;
			this.kind = kind;
			x = (int)(key & 0xFFFFFFFFL);
			y = (int)(key >> 32);
			name = nameOf( seed, kind, x, y );
		}
	}

	/** The name of the sea's place of a kind standing on a world cell. */
	public static String nameOf( long seed, int kind, int x, int y ){
		int sx = Math.floorDiv( x, WorldStructures.SECTOR ), sy = Math.floorDiv( y, WorldStructures.SECTOR );
		switch (kind){
			case WRECK:      return Messages.get( SeaWorld.class, "wreck_name", SeaSites.shipName( seed, sx, sy ) );
			case LIGHTHOUSE: return Messages.get( SeaWorld.class, "light_name", SeaSites.lightName( seed, sx, sy ) );
			default:         return SeaSites.islandName( seed, sx, sy );
		}
	}

	/** The places found, in the order found. Any thread. */
	public Found[] found(){
		return foundSnapshot;
	}

	public boolean isFound( long key ){
		for (Found f : foundSnapshot) if (f.key == key) return true;
		return false;
	}

	private void rebuildFound(){
		Found[] s = new Found[found.size()];
		int i = 0;
		for (Map.Entry<Long, Integer> e : found.entrySet()) s[i++] = new Found( level.worldSeed(), e.getKey(), e.getValue() );
		foundSnapshot = s;
	}

	//records a place found; true when it was new
	private boolean mark( long key, int kind ){
		if (found.containsKey( key )) return false;
		found.put( key, kind );
		return true;
	}

	/** The host's world tick (OverworldLevel.tickWorld): places found when first seen, the sharks. */
	void tick( int heroPos ){
		if (level.isMirror() || level.altitude() != 0) return;
		boolean grown = false;
		for (SeaSites.Sector s : sectors){
			if (s.island != null){
				SeaSites.Island i = s.island;
				int r = Math.max( 1, i.radius - 1 );
				int[][] look = { { i.cx, i.cy }, { i.cx + r, i.cy }, { i.cx - r, i.cy }, { i.cx, i.cy + r }, { i.cx, i.cy - r } };
				grown |= find( i.key(), ISLAND, look, GuideGraph.TAG_ISLAND );
			}
			if (s.wreck != null){
				SeaSites.Wreck w = s.wreck;
				grown |= find( w.key(), WRECK, new int[][]{ { w.holdX, w.holdY }, { w.x0, w.y0 }, { w.x1(), w.y1() } },
						GuideGraph.TAG_WRECK );
			}
			if (s.light != null){
				SeaSites.Light l = s.light;
				grown |= find( l.key(), LIGHTHOUSE, new int[][]{ { l.x, l.y }, { l.x, l.y - 1 } }, GuideGraph.TAG_LIGHTHOUSE );
			}
		}
		if (grown) rebuildFound();
		sharks();
	}

	//a place found the first time any of its cells has been seen, by the host or a guest
	private boolean find( long key, int kind, int[][] cells, String tag ){
		if (found.containsKey( key )) return false;
		int seen = -1;
		for (int[] p : cells){
			int c = level.localCell( p[0], p[1] );
			if (c == -1) continue;
			if (level.visited != null && level.visited[c]){
				seen = c;
				break;
			}
			for (Hero nh : OverworldLevel.heroesOn( level )){
				if (nh.fieldOfView != null && nh.fieldOfView.length == level.length() && nh.fieldOfView[c]){
					seen = c;
					break;
				}
			}
			if (seen != -1) break;
		}
		if (seen == -1) return false;
		mark( key, kind );
		GuideGraph.reveal( tag );
		String line = Messages.get( SeaWorld.class, kind == ISLAND ? "found_island" : kind == WRECK ? "found_wreck" : "found_light",
				nameOf( level.worldSeed(), kind, (int)(key & 0xFFFFFFFFL), (int)(key >> 32) ) );
		for (Hero h : OverworldLevel.heroesOn( level )){
			if (level.distance( h.pos, seen ) <= FOUND_HEARD + h.viewDistance){
				NetManager.heroLog( h, GLog.HIGHLIGHT + line );
			}
		}
		return true;
	}

	/**
	 * A sea chart read (items/sea/SeaChart): every island within reach of where it was drawn goes on
	 * the map, and the wreck or the lighthouse on it with it. How many islands it marked that were
	 * not known.
	 */
	public int chart( int wx, int wy, int reach ){
		long seed = level.worldSeed();
		int sx0 = Math.floorDiv( wx - reach, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy - reach, WorldStructures.SECTOR );
		int sx1 = Math.floorDiv( wx + reach, WorldStructures.SECTOR ), sy1 = Math.floorDiv( wy + reach, WorldStructures.SECTOR );
		int marked = 0;
		for (SeaSites.Sector s : SeaSites.sectors( seed, sx0, sy0, sx1, sy1 )){
			SeaSites.Island i = s.island;
			if (i == null) continue;
			long dx = i.cx - wx, dy = i.cy - wy;
			if (dx * dx + dy * dy > (long) reach * reach) continue;
			if (mark( i.key(), ISLAND )) marked++;
			if (i.wreck != null) mark( i.wreck.key(), WRECK );
			if (i.light != null) mark( i.light.key(), LIGHTHOUSE );
		}
		rebuildFound();
		if (marked > 0) GuideGraph.reveal( GuideGraph.TAG_ISLAND );
		return marked;
	}

	// ------------------------------------------------------------------ the places' folk and finds

	/** The window's places of the sea come to life (OverworldLevel.populateSites): a wreck's salvage,
	 *  once ever; a lighthouse's keeper, whenever none is about and he was never driven off. Each
	 *  waits until it lies whole inside the window. Host only. */
	void populate(){
		if (level.isMirror() || level.altitude() != 0) return;
		for (SeaSites.Sector s : sectors){
			if (s.wreck != null && whole( s.wreck.x0, s.wreck.y0 - 1, s.wreck.x1(), s.wreck.y1() + 1 )){
				salvage( s.wreck );
			}
			if (s.light != null && whole( s.light.x - 1, s.light.y - 2, s.light.x + 1, s.light.y + 1 )){
				keeper( s.light );
			}
		}
	}

	//the box lies inside the window's populated band, two cells clear of its edge
	private boolean whole( int x0, int y0, int x1, int y1 ){
		return x0 - level.worldX >= 2 && y0 - level.worldY >= 2
				&& x1 - level.worldX < level.width() - 2 && y1 - level.worldY < level.height() - 2;
	}

	//what the sea left in a wreck's hold: a sea chest of its timbers and rope, its purse, and one
	//time in three something rare from its cargo. Once ever
	private void salvage( SeaSites.Wreck w ){
		int hold = level.localCell( w.holdX, w.holdY );
		if (hold == -1 || !level.claimHoard( w.key() )) return;
		long h = OverworldLevel.structHash( w.key(), 0x5A17A6EL );
		ArrayList<Item> loot = new ArrayList<>();
		loot.add( new Plank().quantity( 4 + (int) Math.floorMod( h, 5L ) ) );
		loot.add( new Rope().quantity( 1 + (int) Math.floorMod( h >>> 8, 3L ) ) );
		loot.add( new Gold( Random.IntRange( 60, 200 ) ) );
		if (Math.floorMod( h >>> 16, 3L ) == 0){
			Generator.Category[] rare = { Generator.Category.RING, Generator.Category.WAND, Generator.Category.ARTIFACT };
			Item it = Generator.randomUsingDefaults( rare[(int) Math.floorMod( h >>> 24, 3L )] );
			if (it != null) loot.add( it );
		}
		Heap heap = null;
		for (Item it : loot) heap = level.drop( it, hold );
		if (heap != null) heap.type = Heap.Type.CHEST;
	}

	//a lighthouse's keeper on its apron, unless he is about (in the window or parked) or was driven off
	private void keeper( SeaSites.Light l ){
		long key = l.key();
		if (level.clearedSites().contains( key )) return;
		for (Mob m : level.mobs){
			if (m instanceof LighthouseKeeper && ((LighthouseKeeper) m).lightKey == key) return;
		}
		for (Mob m : level.parked()){
			if (m instanceof LighthouseKeeper && ((LighthouseKeeper) m).lightKey == key) return;
		}
		int at = level.localCell( l.x, l.y + 1 );
		if (at == -1) return;
		LighthouseKeeper k = LighthouseKeeper.of( key, l.x, l.y );
		if (level.addMob( k, at )) k.openShop();
	}

	/**
	 * A human village by the water gets its fisher (OverworldLevel.populateSettlement): her stall on
	 * the bank nearest the well with deep water beside it, where the boats she sells can go straight
	 * in. Set up once per village, kept in the slice's save; a village whose bank lies outside the
	 * window waits for one that holds it. Host only.
	 */
	void settleFisher( long key ){
		if (level.isMirror() || level.altitude() != 0 || fishers.contains( key )) return;
		VillageRoutine.Settlement s = VillageRoutine.settlement( level.worldSeed(), key );
		if (s == null || s.gnoll) return;
		for (long k : VillageRoutine.shores( level, s )){
			int c = level.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) );
			if (c == -1 || level.occupied( c ) || !level.passable[c] || level.heaps.get( c ) != null) continue;
			if (!deepNear( c )) continue;
			VillageFisher f = VillageFisher.of( key );
			if (level.addMob( f, c )){
				f.openShop();
				fishers.add( key );
			}
			return;
		}
	}

	//deep water within FISHER_DEEP of a cell
	private boolean deepNear( int cell ){
		int w = level.width(), x = cell % w, y = cell / w;
		for (int dy = -FISHER_DEEP; dy <= FISHER_DEEP; dy++){
			for (int dx = -FISHER_DEEP; dx <= FISHER_DEEP; dx++){
				int nx = x + dx, ny = y + dy;
				if (nx < 1 || ny < 1 || nx >= w - 1 || ny >= level.height() - 1) continue;
				if (level.map[nx + ny * w] == Terrain.DEEP_WATER) return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ buried treasure

	//the once-key of an island's treasure (claimHoard): its own, never its island's
	private static long treasureKey( SeaSites.Island i ){
		return OverworldLevel.worldKey( i.digX, i.digY ) ^ 0x7EA5_0BE5L;
	}

	//the island whose marked sand lies on this window cell, still undug, or null
	private SeaSites.Island treasureIsland( int cell ){
		if (cell < 0 || cell >= level.length() || level.map[cell] != Terrain.EMPTY_SP) return null;
		int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
		for (SeaSites.Sector s : sectors){
			if (s.island != null && s.island.digX == wx && s.island.digY == wy && !dug( s.island )) return s.island;
		}
		return null;
	}

	/** Has this island's buried chest been dug up (its mark is gone)? Any thread (a snapshot). */
	public boolean dug( SeaSites.Island i ){
		long key = OverworldLevel.worldKey( i.digX, i.digY );
		for (long d : dugSnapshot) if (d == key) return true;
		return false;
	}

	private void markDug( long key ){
		if (!dug.add( key )) return;
		long[] s = new long[dug.size()];
		int i = 0;
		for (Long k : dug) s[i++] = k;
		dugSnapshot = s;
	}

	/** Is a treasure buried under this window cell, still undug (homestead/gathering/Gathering.dig)? */
	public boolean treasureAt( int cell ){
		return !level.isMirror() && treasureIsland( cell ) != null;
	}

	/** The shovel has dug the marked sand (Gathering.dug): a sea chest comes up - gold and something
	 *  rare of a lost ship's cargo - once ever, and the mark is gone from the sand. Host only. */
	public void unearth( int cell ){
		SeaSites.Island i = treasureIsland( cell );
		if (i == null || level.isMirror()) return;
		markDug( OverworldLevel.worldKey( i.digX, i.digY ) );
		if (level.liveScene()) com.watabou.noosa.Game.runOnRenderThread( level::presentSeaArt );
		if (!level.claimHoard( treasureKey( i ) )) return;
		long h = OverworldLevel.structHash( treasureKey( i ), 0x60_1DL );
		level.drop( new Gold( Random.IntRange( 150, 400 ) ), cell );
		Generator.Category[] rare = { Generator.Category.RING, Generator.Category.WAND, Generator.Category.ARTIFACT,
				Generator.Category.WAND };
		Item it = Generator.randomUsingDefaults( rare[(int) Math.floorMod( h, (long) rare.length )] );
		Heap heap = it != null ? level.drop( it, cell ) : level.heaps.get( cell );
		if (heap != null){
			heap.type = Heap.Type.CHEST;
			if (heap.sprite != null) heap.sprite.view( heap );
		}
		for (Hero hero : OverworldLevel.heroesOn( level )){
			if (level.distance( hero.pos, cell ) <= 1) NetManager.heroLog( hero, GLog.POSITIVE + Messages.get( SeaWorld.class, "treasure" ) );
		}
		GuideGraph.reveal( GuideGraph.TAG_ISLAND );
	}

	// ------------------------------------------------------------------ sharks

	//the window's sharks topped up out of sight of every hero near the open sea, and the ones strayed
	//far from them all let go
	private void sharks(){
		if (--sharkIn > 0) return;
		sharkIn = SHARK_EVERY;
		ArrayList<Hero> heroes = OverworldLevel.heroesOn( level );
		if (heroes.isEmpty()) return;
		int live = 0;
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof Shark)) continue;
			int nearest = Integer.MAX_VALUE;
			for (Hero h : heroes) nearest = Math.min( nearest, level.distance( h.pos, m.pos ) );
			if (nearest > LOST && !level.heroFOV[m.pos]){
				OverworldLevel.vanish( level, m );
			} else {
				live++;
			}
		}
		int max = SHARKS + (DayNightCycle.isNight() ? 1 : 0);
		if (live >= max) return;
		Hero h = heroes.get( Random.Int( heroes.size() ) );
		int w = level.width(), hx = h.pos % w, hy = h.pos / w;
		for (int tries = 0; tries < 12; tries++){
			int d = Random.IntRange( SHARK_NEAR, SHARK_FAR );
			double a = Random.Float( (float)(2 * Math.PI) );
			int x = hx + (int) Math.round( Math.cos( a ) * d ), y = hy + (int) Math.round( Math.sin( a ) * d );
			if (x < 2 || y < 2 || x >= w - 2 || y >= level.height() - 2) continue;
			int c = x + y * w;
			if (!Shark.openSea( level, c ) || level.heroFOV[c] || level.occupied( c )) continue;
			if (OverworldLevel.remoteHeroDistance( level, c ) < SHARK_NEAR) continue;
			Shark s = new Shark();
			s.pos = c;
			if (Dungeon.level == level && Dungeon.hero != null){
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( s );
			} else {
				level.mobs.add( s );
			}
			return;
		}
	}

	// ------------------------------------------------------------------ the moored boats

	/** The boats moored on the slice, {wx, wy, kind ordinal} each. Any thread (a snapshot). */
	public int[][] moorings(){
		return moorings;
	}

	private void refreshMoorings(){
		ArrayList<int[]> m = Seafaring.moorings( level );
		moorings = m.toArray( new int[0][] );
	}

	/** A boat launched, moored, boarded, hauled out or sunk: the art shows the boats as they float now. */
	public void boatsChanged(){
		if (!level.isMirror()) refreshMoorings();
		if (level.liveScene()) com.watabou.noosa.Game.runOnRenderThread( level::presentSeaArt );
	}

	// ------------------------------------------------------------------ the land of the islands

	/** The biome an island's cell is (its beach and its woods alike), or null off the islands. */
	public WorldModel.Biome biomeAt( int cell ){
		return islandAt( cell ) ? WorldModel.Biome.BEACH : null;
	}

	/** What the HUD names a window cell by when it lies in a found place of the sea, or null. */
	public String placeNameAt( int cell ){
		int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
		for (SeaSites.Sector s : sectors){
			if (s.island != null && s.island.landAt( wx, wy ) && isFound( s.island.key() )){
				return SeaSites.islandName( level.worldSeed(), s.island.sx, s.island.sy );
			}
			if (s.island == null && s.wreck != null && near( s.wreck.x0 - 2, s.wreck.y0 - 2, s.wreck.x1() + 2, s.wreck.y1() + 2, wx, wy )
					&& isFound( s.wreck.key() )){
				return nameOf( level.worldSeed(), WRECK, s.wreck.holdX, s.wreck.holdY );
			}
			if (s.island == null && s.light != null && near( s.light.x - 3, s.light.y - 3, s.light.x + 3, s.light.y + 3, wx, wy )
					&& isFound( s.light.key() )){
				return nameOf( level.worldSeed(), LIGHTHOUSE, s.light.x, s.light.y );
			}
		}
		return null;
	}

	private static boolean near( int x0, int y0, int x1, int y1, int x, int y ){
		return x >= x0 && y >= y0 && x <= x1 && y <= y1;
	}

	/** Does the sea's art draw anything that hangs on this window cell (an island's, a wreck's or a
	 *  lighthouse's): a change to it lays the art again. */
	public boolean artAt( int cell ){
		if (islandAt( cell )) return true;
		int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
		for (SeaSites.Sector s : sectors){
			if (s.wreck != null && near( s.wreck.x0 - 1, s.wreck.y0 - 1, s.wreck.x1() + 1, s.wreck.y1() + 1, wx, wy )) return true;
			if (s.light != null && near( s.light.x - 1, s.light.y - 2, s.light.x + 1, s.light.y + 1, wx, wy )) return true;
		}
		return false;
	}

	/** What a cell of the sea's places is called in the examine window (a key of this class's, and
	 *  "<key>_desc"), or null: a wreck's hull or hold, a lighthouse's tower or apron, a treasure's mark,
	 *  a moored boat. */
	public String cellKey( int cell ){
		if (cell < 0 || cell >= level.length()) return null;
		int wx = level.worldX + cell % level.width(), wy = level.worldY + cell / level.width();
		int t = level.map[cell];
		for (int[] m : moorings){
			if (m[0] == wx && m[1] == wy) return m[2] == xyz.gabriwar.warpedpixeldungeon.items.sea.Boat.Kind.SAILBOAT.ordinal()
					? "moored_sail" : "moored_raft";
		}
		for (SeaSites.Sector s : sectors){
			if (s.wreck != null && s.wreck.contains( wx, wy ) && (t == Terrain.CUSTOM_DECO || t == Terrain.CUSTOM_DECO_EMPTY)){
				return wx == s.wreck.holdX && wy == s.wreck.holdY ? "wreck_hold" : "wreck";
			}
			if (s.wreck != null && t == Terrain.CUSTOM_DECO_EMPTY){
				for (int[] mouth : s.wreck.mouth()) if (mouth[0] == wx && mouth[1] == wy) return "wreck_shore";
			}
			if (s.light != null){
				if (s.light.contains( wx, wy ) && t == Terrain.CUSTOM_DECO) return "lighthouse";
				if (wy == s.light.y + 1 && Math.abs( wx - s.light.x ) <= 1 && t == Terrain.CUSTOM_DECO_EMPTY) return "apron";
			}
			if (s.island != null && s.island.digX == wx && s.island.digY == wy && t == Terrain.EMPTY_SP
					&& !dug( s.island )) return "dig_spot";
		}
		return null;
	}

	// ------------------------------------------------------------------ co-op guests

	/** Co-op host: a fingerprint of what a guest's mirror shows of the sea (found places, moorings,
	 *  dug marks). Any thread: it reads the snapshots only. */
	public long sharedSig(){
		Found[] f = foundSnapshot;
		long h = f.length * 0x9E3779B97F4A7C15L;
		for (Found x : f) h = h * 31 + x.key;
		for (int[] m : moorings) h = h * 31 + ((long) m[0] * 73856093L ^ (long) m[1] * 19349663L ^ m[2]);
		for (long d : dugSnapshot) h = h * 31 + d;
		return h;
	}

	private static final String SH_FOUND = "found";
	private static final String SH_KINDS = "kinds";
	private static final String SH_MOOR = "moor";
	private static final String SH_DUG = "dug";

	/** Co-op host: found places, moorings and dug marks as a bundle's text. Any thread (the snapshots). */
	public String shared(){
		Bundle b = new Bundle();
		Found[] f = foundSnapshot;
		long[] keys = new long[f.length];
		int[] kinds = new int[f.length];
		for (int i = 0; i < f.length; i++){
			keys[i] = f[i].key;
			kinds[i] = f[i].kind;
		}
		b.put( SH_FOUND, keys );
		b.put( SH_KINDS, kinds );
		int[][] m = moorings;
		int[] flat = new int[m.length * 3];
		for (int j = 0; j < m.length; j++){
			flat[j * 3] = m[j][0];
			flat[j * 3 + 1] = m[j][1];
			flat[j * 3 + 2] = m[j][2];
		}
		b.put( SH_MOOR, flat );
		b.put( SH_DUG, dugSnapshot );
		return b.toString();
	}

	/** A co-op guest's mirror takes over its host's (shared): its map pins the same places, its art
	 *  draws the same boats. */
	void adoptShared( Bundle b ){
		found.clear();
		long[] keys = b.contains( SH_FOUND ) ? b.getLongArray( SH_FOUND ) : null;
		int[] kinds = b.contains( SH_KINDS ) ? b.getIntArray( SH_KINDS ) : null;
		if (keys != null && kinds != null){
			for (int i = 0; i < Math.min( keys.length, kinds.length ); i++) found.put( keys[i], kinds[i] );
		}
		rebuildFound();
		int[] flat = b.contains( SH_MOOR ) ? b.getIntArray( SH_MOOR ) : new int[0];
		int[][] m = new int[flat.length / 3][];
		for (int j = 0; j < m.length; j++) m[j] = new int[]{ flat[j * 3], flat[j * 3 + 1], flat[j * 3 + 2] };
		moorings = m;
		dug.clear();
		dugSnapshot = new long[0];
		if (b.contains( SH_DUG )) for (long d : b.getLongArray( SH_DUG )) markDug( d );
		if (level.liveScene()) com.watabou.noosa.Game.runOnRenderThread( level::presentSeaArt );
	}

	// ------------------------------------------------------------------ the save

	private static final String FOUND_KEYS  = "sea_found_keys";
	private static final String FOUND_KINDS = "sea_found_kinds";
	private static final String FISHERS     = "sea_fishers";
	private static final String DUG         = "sea_dug";

	void storeInBundle( Bundle bundle ){
		long[] keys = new long[found.size()];
		int[] kinds = new int[found.size()];
		int i = 0;
		for (Map.Entry<Long, Integer> e : found.entrySet()){
			keys[i] = e.getKey();
			kinds[i++] = e.getValue();
		}
		bundle.put( FOUND_KEYS, keys );
		bundle.put( FOUND_KINDS, kinds );
		long[] f = new long[fishers.size()];
		i = 0;
		for (Long k : fishers) f[i++] = k;
		bundle.put( FISHERS, f );
		bundle.put( DUG, dugSnapshot );
	}

	//older saves found nothing of the sea, set up no fisher and dug up no chest
	void restoreFromBundle( Bundle bundle ){
		found.clear();
		long[] keys = bundle.contains( FOUND_KEYS ) ? bundle.getLongArray( FOUND_KEYS ) : null;
		int[] kinds = bundle.contains( FOUND_KINDS ) ? bundle.getIntArray( FOUND_KINDS ) : null;
		if (keys != null && kinds != null){
			for (int i = 0; i < Math.min( keys.length, kinds.length ); i++){
				//a kind this build does not know is skipped, never guessed at
				if (kinds[i] < ISLAND || kinds[i] > LIGHTHOUSE) continue;
				found.put( keys[i], kinds[i] );
			}
		}
		rebuildFound();
		fishers.clear();
		long[] f = bundle.contains( FISHERS ) ? bundle.getLongArray( FISHERS ) : null;
		if (f != null) for (long k : f) fishers.add( k );
		dug.clear();
		dugSnapshot = new long[0];
		if (bundle.contains( DUG )) for (long d : bundle.getLongArray( DUG )) markDug( d );
	}
}
