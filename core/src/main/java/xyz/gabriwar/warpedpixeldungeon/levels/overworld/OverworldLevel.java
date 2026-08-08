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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BlueCat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Alter;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.alchemy.Cross;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.TownRemixedTiles;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Phase 1 of the overworld: an "active window" Level that is a sliding view
 * into an infinite deterministic world.
 *
 * The window is WIDTH x HEIGHT cells. (worldX, worldY) is the world coordinate of the
 * window's top-left cell. When the hero walks within MARGIN of an edge, the
 * window REBASES: it re-centres on the hero, the map is regenerated from
 * WorldModel plus the diff store, everything in the window is translated, and
 * the scene is rebuilt seamlessly.
 *
 * Player changes to terrain are captured as diffs (world-pos -> terrain) by
 * comparing the live window against the pristine generator output - no hooks
 * into Level.set needed. Diffs are captured before every rebase and save.
 */
public class OverworldLevel extends Level {

	//30,976 cells. Past 16,384 a tilemap needs more than one draw call, which
	//NoosaScript.drawQuadSetLarge already handles - the old 96x96 cap was for a
	//limit that is not there
	public static final int WIDTH  = 176;
	public static final int HEIGHT = 176;
	//rebase when the hero gets this close to a window edge
	private static final int MARGIN = 24;

	{
		color1 = 0x48763c;
		color2 = 0x59994a;
		//open sky: see as far as the engine's shadowcaster allows
		viewDistance = 20;
	}

	public long worldSeed = 0;
	//world coordinate of the window's (0,0)
	public int worldX = 0, worldY = 0;

	//a NETWORK MIRROR: built from a host's (seed, origin, seasonal shift) plus
	//the host's authoritative map. It derives terrain, the snow line, the art
	//layers, the fog rules and the place names - and nothing else. Every path
	//that would SIMULATE the world (streaming the window, spawning, the
	//waypoint march, the arrival handoff) checks this first
	private boolean network = false;

	//the seasonal shift the current window was derived for - the value the
	//host has to ship for a client to reproduce this window
	private float windowShift = 0f;

	//bumped every time the window is (re)derived, so a host can tell a client
	//"the window moved, here is the new origin" instead of the client having
	//to guess from the terrain. bundled, so it stays monotonic across a reload
	private int windowVersion = 0;

	public long worldSeed(){ return worldSeed; }
	public int worldX(){ return worldX; }
	public int worldY(){ return worldY; }

	/** The seasonal shift this window was derived for. */
	public float windowShift(){ return windowShift; }

	/** Changes whenever worldX/worldY/the seasonal shift do. */
	public int windowVersion(){ return windowVersion; }

	//player terrain changes, keyed by world position
	private HashMap<Long, Integer> diffs = new HashMap<>();

	//heaps that fell outside the active window, parked at their world position
	//until the window slides back over them
	private HashMap<Long, Heap> storedHeaps = new HashMap<>();

	//the untouched generator output for the current window - lets diff capture
	//compare against memory instead of re-running the whole generator
	private int[] pristine;

	//window shifts are QUANTIZED to a fixed step per axis, which makes the
	//next origin predictable - so it can be pre-generated on a worker thread
	//before the hero ever reaches the margin. rebase then just swaps buffers
	private static final int SHIFT_Q = 32;
	private final Object pregenLock = new Object();
	private Window pregenBuf;
	private int pregenOX, pregenOY;
	private boolean pregenReady = false;
	private boolean pregenRunning = false;

	//a generated window: the terrain plus, per cell, whether the ground is
	//frozen (the snow biome band) - the tile art needs it for what grows there
	//opaque to anything outside this package: a network mirror stages one off
	//the render thread and hands it straight back to adoptNetworkWindow
	public static class Window {
		int[] terrain;
		boolean[] frozen;
		byte[] waterDepth;   //steps to the nearest land (8-connected), capped
		float shift;         //the seasonal snapshot it was derived for
	}

	//one full generator pass for an arbitrary origin, PURE - safe off-thread.
	//the seasonal shift snapshot is read ONCE here and threaded through every
	//sample, so the window is derived for a single point of the year even if
	//the main thread moves the snapshot while a worker is still generating
	private Window generatePristine( int ox, int oy ){
		return generatePristine( worldSeed, ox, oy );
	}

	private Window generatePristine( long worldSeed, int ox, int oy ){
		Window w = new Window();
		w.terrain = new int[length()];
		w.frozen = new boolean[length()];
		WorldModel.Sample smp = new WorldModel.Sample();
		final float shift = WorldModel.seasonShift();
		w.shift = shift;
		for (int y = 0; y < HEIGHT; y++){
			for (int x = 0; x < WIDTH; x++){
				int cell = x + y * width();
				if (x == 0 || y == 0 || x == WIDTH-1 || y == HEIGHT-1){
					w.terrain[cell] = Terrain.WALL;
					continue;
				}
				int wwx = ox + x, wwy = oy + y;
				WorldModel.sample( worldSeed, wwx, wwy, shift, smp );
				int wild = WorldModel.wildTerrain( worldSeed, wwx, wwy, smp );
				int structure = WorldStructures.terrainAt( worldSeed, wwx, wwy, wild );
				w.terrain[cell] = structure != -1 ? structure : wild;
				w.frozen[cell] = smp.temperature < WorldModel.FREEZE;
			}
		}

		//open water gets a depth: steps from the nearest land, 8-connected
		//(two chamfer sweeps). the first tile off the shore stays walkable;
		//anything further is DEEP_WATER, and the shade darkens with depth.
		//only WATER cells carry depth: FROZEN_WATER (and a bridge) is land
		//for the shore test, so a lake the season froze over is walkable
		//across, and the open water beside its ice is shallow
		int W = width();
		w.waterDepth = new byte[length()];
		final byte CAP = 6;
		for (int i = 0; i < length(); i++){
			w.waterDepth[i] = w.terrain[i] == Terrain.WATER ? CAP : 0;
		}
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int c = x + y * W;
				if (w.waterDepth[c] == 0) continue;
				byte d = (byte)Math.min( CAP, 1 + Math.min(
						Math.min( w.waterDepth[c-1], w.waterDepth[c-W] ),
						Math.min( w.waterDepth[c-W-1], w.waterDepth[c-W+1] ) ) );
				if (d < w.waterDepth[c]) w.waterDepth[c] = d;
			}
		}
		for (int y = HEIGHT-2; y >= 1; y--){
			for (int x = WIDTH-2; x >= 1; x--){
				int c = x + y * W;
				if (w.waterDepth[c] == 0) continue;
				byte d = (byte)Math.min( CAP, 1 + Math.min(
						Math.min( w.waterDepth[c+1], w.waterDepth[c+W] ),
						Math.min( w.waterDepth[c+W-1], w.waterDepth[c+W+1] ) ) );
				if (d < w.waterDepth[c]) w.waterDepth[c] = d;
			}
		}
		for (int i = 0; i < length(); i++){
			if (w.waterDepth[i] >= 2) w.terrain[i] = Terrain.DEEP_WATER;
		}
		return w;
	}

	//per-cell frozen ground and water depth of the current window (see Window)
	private boolean[] frozen;
	private byte[] waterDepth;

	/** Is the ground of this window cell snowed under? */
	public boolean frozenAt( int cell ){
		return frozen != null && cell >= 0 && cell < frozen.length && frozen[cell];
	}

	// ------------------------------------------------------------ seasons

	//which slice of the year the window was derived for: the season, a coarse
	//eighth-of-season day bucket (the snow line creeps in ~8 steps a season)
	//and the weather band (cold snap / none / heat wave), packed as
	//(band+1)*1000 + season*100 + bucket. the world re-derives when it changes.
	//-1 = never stamped (a save from before the seasons turned)
	private int seasonStamp = -1;

	//the weather band the stamp carries
	private int climateBand(){
		return seasonStamp < 0 ? 0 : seasonStamp / 1000 - 1;
	}

	private int currentStamp(){
		xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season season
				= xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.season();
		int days = Math.max( 1, xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.daysInCurrentSeason() );
		int bucket = Math.min( 7, Math.max( 0,
				(xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.dayOfSeason() - 1) * 8 / days ) );
		//the weather band has hysteresis: a real cold snap or heat wave (the
		//surface temperature well off the season's norm - fronts, not the
		//day/night swing alone) enters it, and the weather has to settle back
		//near the norm to leave it - no flapping at the threshold
		int band = climateBand();
		float dev = xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.surfaceTemp()
				- WorldModel.seasonNormC();
		if (dev <= -8f) band = -1;
		else if (dev >= 9f) band = 1;
		else if (Math.abs( dev ) < 4f) band = 0;
		return (band + 1) * 1000 + season.ordinal() * 100 + bucket;
	}

	//the snapshot every generator call uses: the calendar's sinusoid plus the
	//stamped weather band. MAIN THREAD, before any (re)generation
	private void refreshSeasonShift(){
		//a locally generated window means the local calendar owns the
		//snapshot again: drop any host snapshot a previous network session
		//left held, or this window would be derived for someone else's year
		WorldModel.releaseSeasonShift();
		WorldModel.setSeasonShift( WorldModel.calendarShift()
				+ climateBand() * WorldModel.CLIMATE_SHIFT );
	}

	//the season turned under the window: re-derive the world for the new
	//slice of the year, in place, and get everything off the ground that
	//closed (ice that thawed into deep water, mostly)
	private void reseason(){
		//ORDER MATTERS. the diff store is captured against the pristine the
		//live map was BUILT FROM: captureDiffs records every cell where map
		//and pristine disagree as a player edit. were the pristine re-derived
		//first, every cell the season changed (ice thawed to water, grass
		//snowed under) would differ from the map's old-season value, read as
		//an edit, and FOSSILIZE the old season's terrain in the store for
		//good. captured first, the store holds only real edits; the rebuilt
		//map is new pristine + those edits, and an edit that now equals the
		//new pristine is dropped by the next capture
		captureDiffs();
		refreshSeasonShift();
		//the pre-generated next window was derived for the old season - and
		//so is whatever a worker still in flight is building: moving the
		//target origin makes it drop its result on arrival
		synchronized (pregenLock){
			pregenReady = false;
			pregenBuf = null;
			pregenOX = Integer.MIN_VALUE;
			pregenOY = Integer.MIN_VALUE;
		}
		regenWindow();
		placeTransitions();
		restoreHeapsInWindow();

		//nobody stands inside a cell the season closed. heaps stay where
		//they fell (a sunk heap is still a heap); mobs and the hero step to
		//the nearest open ground
		for (Mob m : mobs){
			if (passable[m.pos]) continue;
			int to = freeSpotWithin( m.pos, 6 );
			if (to == -1) to = clearSpotNear( m.pos );
			m.pos = to;
			if (m.sprite != null) m.sprite.place( to );
		}
		if (Dungeon.hero != null && Dungeon.level == this && !passable[Dungeon.hero.pos]){
			int to = freeSpotWithin( Dungeon.hero.pos, 8 );
			if (to == -1) to = clearSpotNear( Dungeon.hero.pos );
			Dungeon.hero.interrupt();
			Dungeon.hero.pos = to;
			if (Dungeon.hero.sprite != null) Dungeon.hero.sprite.place( to );
			GLog.w( Messages.get( this, "thaw" ) );
		}
		//net MP: remote heroes are not in mobs, so the loop above misses them.
		//left alone, a claimed hero stands inside the deep water their ice just
		//thawed into
		if (Dungeon.level == this){
			for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh
					: xyz.gabriwar.warpedpixeldungeon.net.NetManager.allKnownNetHeroes()){
				if (nh.pos < 0 || nh.pos >= length() || passable[nh.pos]) continue;
				int to = freeSpotWithin( nh.pos, 8 );
				if (to == -1) to = clearSpotNear( nh.pos );
				//NOT interrupt(): that resets the host's key-hold state, and this
				//hero is not the one at the keyboard. dropping the walk is enough
				nh.curAction = null;
				nh.pos = to;
				if (nh.sprite != null) nh.sprite.place( to );
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( nh,
						Messages.get( this, "thaw" ) );
			}
		}

		if (Dungeon.level == this
				&& com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene){
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateMap();
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog();
			Dungeon.observe();
		}
	}

	//kick a background pre-generation of the predicted next window when the
	//hero enters the warning band just outside the rebase margin
	private void maybePregen( int heroX, int heroY ){
		int pdx = heroX < MARGIN + 8 ? -SHIFT_Q : heroX >= WIDTH - MARGIN - 8 ? SHIFT_Q : 0;
		int pdy = heroY < MARGIN + 8 ? -SHIFT_Q : heroY >= HEIGHT - MARGIN - 8 ? SHIFT_Q : 0;
		if (pdx == 0 && pdy == 0) return;
		final int ox = worldX + pdx, oy = worldY + pdy;
		synchronized (pregenLock){
			if (pregenRunning) return;
			if (pregenReady && pregenOX == ox && pregenOY == oy) return;
			pregenRunning = true;
			pregenReady = false;
			pregenOX = ox;
			pregenOY = oy;
		}
		Thread worker = new Thread( () -> {
			Window buf = generatePristine( ox, oy );
			synchronized (pregenLock){
				if (pregenOX == ox && pregenOY == oy){
					pregenBuf = buf;
					pregenReady = true;
				}
				pregenRunning = false;
			}
		}, "ow-pregen" );
		worker.setPriority( Thread.MIN_PRIORITY );
		worker.setDaemon( true );
		worker.start();
	}

	//sector keys whose dragon hoard has been laid (never re-dropped) and whose
	//dragon has been slain (never respawned) - both persist in the bundle
	private java.util.HashSet<Long> hoardLaid = new java.util.HashSet<>();
	private java.util.HashSet<Long> sitesCleared = new java.util.HashSet<>();

	public void markSiteCleared( long sectorKey ){
		sitesCleared.add( sectorKey );
	}

	//the fixed WORLD position of the arrival waystone - a permanent landmark,
	//not something that follows the hero around
	public int arrivalX, arrivalY;

	//the magic map's waypoint, in world coordinates - the hero auto-walks
	//toward it across windows (rebases re-aim the walk)
	public boolean waypointActive = false;
	//false while the march is PAUSED (player tapped) - the waypoint is kept
	//and the hud compass tag resumes it
	public boolean waypointMarching = false;
	public int waypointX, waypointY;

	/** Puts the hero back on the road toward the kept waypoint. */
	public void resumeMarch(){
		if (!waypointActive) return;
		waypointMarching = true;
		marchStallPos = -1;
		marchStallCount = 0;
		aimAtWaypoint();
	}

	// ------------------------------------------------------------- the town

	//the town is part of the world (WorldStructures.townTerrain lays its 32x32
	//map at world origin). this level paints its art, wires its doorways and
	//stairs, keeps its folk and chests, and never fogs it

	private TownRemixedTiles.Layer[] townArt;

	//every mob that scrolled out of the window, parked at its world position
	//until the window slides back over it. the bundle keys still say 'folk':
	//the store began as the town's people and older saves carry that name
	private HashMap<Long, Mob> parkedMobs = new HashMap<>();
	//the game-clock time each of them was parked (same keys) - disposable
	//wildlife is forgotten once it has sat parked for too long
	private HashMap<Long, Float> parkedAt = new HashMap<>();
	//parked wildlife older than two world-days, or further than three windows
	//from the hero, is dropped on the next park; everyone else waits forever
	private static final float PARK_MAX_AGE = 2 * xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.FULL_CYCLE;
	private static final int PARK_MAX_DIST = 3 * WIDTH;
	//layout cells whose folk/chest have been created once (they persist from
	//then on: as mobs, parked, or as heaps/stored heaps)
	private java.util.HashSet<Integer> townSpawned = new java.util.HashSet<>();

	//where the next arrival on the surface lands, in world coordinates. set by
	//whoever sends the hero here (town doors, stairs, journal, beacon); the
	//window is re-centred on it if needed
	private static boolean pendingArrival = false;
	private static int pendingWX, pendingWY;

	/** The next trip to the surface lands on this world cell. */
	public static void arriveAt( int wx, int wy ){
		pendingArrival = true;
		pendingWX = wx;
		pendingWY = wy;
	}

	public static boolean hasPendingArrival(){ return pendingArrival; }

	/** ...on this town layout cell. */
	public static void arriveInTown( int layoutCell ){
		arriveAt( WorldStructures.townWorldX( layoutCell ), WorldStructures.townWorldY( layoutCell ) );
	}

	/** Sends the hero to the surface, landing on the pending arrival (or the last one). */
	public static void travelToSurface(){
		xyz.gabriwar.warpedpixeldungeon.levels.Level.beforeTransition();
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.mode
				= xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.Mode.RETURN;
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnDepth = DEPTH;
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnBranch = 0;
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnPos = -1;
		com.watabou.noosa.Game.switchScene( xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.class );
	}

	public static final int DEPTH = 97;

	//window cell of a world cell, or -1 when it is not inside the window's interior
	private int localCell( int wx, int wy ){
		int x = wx - worldX, y = wy - worldY;
		if (x <= 0 || y <= 0 || x >= width()-1 || y >= height()-1) return -1;
		return x + y * width();
	}

	private int localTownCell( int layoutCell ){
		return localCell( WorldStructures.townWorldX( layoutCell ), WorldStructures.townWorldY( layoutCell ) );
	}

	/** Is this window cell inside the town? */
	public boolean inTown( int cell ){
		return WorldStructures.townCell( worldX + cell % width(), worldY + cell / width() ) != -1;
	}

	//the hero is placed at getTransition(null) on arrival. the town doorways
	//are the level's real transitions; the arrival point is a synthetic one at
	//the pending/last arrival cell, re-centring the window on it if it is not
	//inside (a far-off jump: journal, beacon, coming up the dungeon stairs)
	@Override
	public LevelTransition getTransition( LevelTransition.Type type ){
		if (type != null) return super.getTransition( type );
		//a mirror never consumes the pending arrival and never slides its own
		//window: the host owns both, and stealing the arrival would move a
		//landmark the host is about to use
		if (network){
			int cell = Dungeon.hero != null && Dungeon.level == this
					? Dungeon.hero.pos : width()/2 + height()/2 * width();
			return new LevelTransition( this, cell, LevelTransition.Type.BRANCH_ENTRANCE );
		}
		if (pendingArrival){
			arrivalX = pendingWX;
			arrivalY = pendingWY;
			pendingArrival = false;
		}
		int cell = localCell( arrivalX, arrivalY );
		if (cell == -1 && Dungeon.level != this){
			jumpWindowTo( arrivalX, arrivalY );
			cell = localCell( arrivalX, arrivalY );
		}
		if (cell == -1) cell = Dungeon.hero != null ? Dungeon.hero.pos : width()/2 + height()/2 * width();
		return new LevelTransition( this, cell, LevelTransition.Type.BRANCH_ENTRANCE );
	}

	//re-centre the window on a far world cell while no scene shows the level:
	//everything on the ground and every mob are parked by world position,
	//blobs/plants/traps in the old window are dropped
	private void jumpWindowTo( int wx, int wy ){
		if (pristine != null) captureDiffs();
		for (Heap h : heaps.valueList()){
			long key = worldKey( worldX + h.pos % width(), worldY + h.pos / width() );
			Heap parked = storedHeaps.get( key );
			if (parked != null) parked.items.addAll( h.items ); else storedHeaps.put( key, h );
		}
		heaps.clear();
		for (Mob m : mobs.toArray( new Mob[0] )){
			park( m, worldX + m.pos % width(), worldY + m.pos / width() );
		}
		mobs.clear();
		pruneParked( wx, wy );
		blobs.clear();
		plants.clear();
		traps.clear();
		worldX = wx - WIDTH/2;
		worldY = wy - HEIGHT/2;
		visited = new boolean[length()];
		mapped = new boolean[length()];
		regenWindow();
		restoreHeapsInWindow();
		placeTransitions();
		populateTown();
	}

	//a mob leaving the window is parked at its world position. it is taken
	//off the scheduler (its buffs too - Actor.add puts them back on unpark)
	//with its clock rebased to zero, so re-adding it lands on the then-current
	//turn instead of stalling it by the time it sat parked; it loses its sprite
	//and the mob object itself waits in parkedMobs. the caller must still drop
	//it from mobs. a fleeing thief hands its loot to the heap store first so
	//nothing the player owns is ever duplicated or lost
	private void park( Mob m, int wx, int wy ){
		Actor.remove( m );
		for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff b : m.buffs()) Actor.remove( b );
		m.clearTime();
		if (m.sprite != null) m.sprite.killAndErase();
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief
				&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief) m).item != null){
			long key = worldKey( wx, wy );
			Heap parked = storedHeaps.get( key );
			if (parked == null){
				parked = new Heap();
				storedHeaps.put( key, parked );
			}
			parked.items.add( ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief) m).item );
			((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief) m).item = null;
		}
		//one mob per world cell: a cell already holding a parked mob (one the
		//window could not put back) hands the newcomer the nearest free key.
		//people are never dropped for want of a key - their search widens, and
		//as a last resort they take a nearby animal's place
		long key = worldKey( wx, wy );
		if (parkedMobs.containsKey( key )){
			int reach = keepsForever( m ) ? 4 : 1;
			for (int r = 1; r <= reach && parkedMobs.containsKey( key ); r++){
				for (int dy = -r; dy <= r && parkedMobs.containsKey( key ); dy++){
					for (int dx = -r; dx <= r && parkedMobs.containsKey( key ); dx++){
						if (!parkedMobs.containsKey( worldKey( wx + dx, wy + dy ) )) key = worldKey( wx + dx, wy + dy );
					}
				}
			}
			if (parkedMobs.containsKey( key )){
				if (!keepsForever( m )) return;   //wildlife: the newcomer is forgotten
				//evict the nearest parked animal rather than lose a person
				for (int dy = -reach; dy <= reach && parkedMobs.containsKey( key ); dy++){
					for (int dx = -reach; dx <= reach && parkedMobs.containsKey( key ); dx++){
						long k = worldKey( wx + dx, wy + dy );
						Mob held = parkedMobs.get( k );
						if (held != null && !keepsForever( held )){
							parkedMobs.remove( k );
							parkedAt.remove( k );
							key = k;
						}
					}
				}
				if (parkedMobs.containsKey( key )) return;   //nothing but people all around
			}
		}
		parkedMobs.put( key, m );
		parkedAt.put( key, xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.clock() );
	}

	//who is never dropped from the parked store: people (every NPC - villagers,
	//vendors, guards, town folk), site guardians (dragons, ruin skeletons) and
	//anything that has a home sector. the rest is wildlife and free game
	private static boolean keepsForever( Mob m ){
		return m instanceof NPC
				|| m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon
				|| m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.RuinSkeleton;
	}

	//forget parked wildlife that has gone stale: parked for over two
	//world-days, or further than three windows from the given world cell
	private void pruneParked( int hx, int hy ){
		float now = xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.clock();
		java.util.Iterator<HashMap.Entry<Long, Mob>> it = parkedMobs.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (keepsForever( e.getValue() )) continue;
			int wx = (int)(e.getKey() & 0xFFFFFFFFL), wy = (int)(e.getKey() >> 32);
			Float at = parkedAt.get( e.getKey() );
			if ((at != null && now - at > PARK_MAX_AGE)
					|| Math.abs( wx - hx ) > PARK_MAX_DIST || Math.abs( wy - hy ) > PARK_MAX_DIST){
				it.remove();
				parkedAt.remove( e.getKey() );
			}
		}
	}

	private void addFolk( Mob m, int cell ){
		m.pos = cell;
		if (Dungeon.level == this
				&& com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene){
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( m );
		} else {
			mobs.add( m );
		}
	}

	//put back every parked mob whose world cell is inside the window, on its
	//own cell when that is free and passable, else the nearest such cell
	//within 2; a mob with nowhere to stand stays parked for the next pass
	private void unparkMobs(){
		java.util.Iterator<HashMap.Entry<Long, Mob>> it = parkedMobs.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			int cell = localCell( (int)(e.getKey() & 0xFFFFFFFFL), (int)(e.getKey() >> 32) );
			if (cell == -1) continue;
			int at = freeSpotWithin( cell, 2 );
			if (at == -1) continue;
			Mob m = e.getValue();
			m.sprite = null;
			addFolk( m, at );
			it.remove();
			parkedAt.remove( e.getKey() );
		}
	}

	//is anyone standing here? Actor.findChar alone is NOT enough: the window is
	//re-populated (restoreFromBundle, jumpWindowTo) BEFORE Actor.init registers
	//the level's mobs, so the scheduler is empty there and every candidate cell
	//reads as free - which stacked unparked mobs on top of the ones already down
	private boolean occupied( int cell ){
		if (Actor.findChar( cell ) != null) return true;
		for (Mob m : mobs){
			if (m.pos == cell) return true;
		}
		return false;
	}

	//the cell itself, or the nearest passable unoccupied window cell within
	//the given radius, or -1
	private int freeSpotWithin( int cell, int radius ){
		if (passable[cell] && !occupied( cell )) return cell;
		int cx = cell % width(), cy = cell / width();
		for (int r = 1; r <= radius; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cx + dx, y = cy + dy;
					if (x <= 0 || y <= 0 || x >= width()-1 || y >= height()-1) continue;
					int c = x + y * width();
					if (passable[c] && !occupied( c )) return c;
				}
			}
		}
		return -1;
	}

	//the doorways the window currently spans: the town's buildings, its stairs
	//and mine gate, and every village house whose door is in view. all of them
	//are FIXED world landmarks - the transitions exist exactly while their
	//world cells sit inside the window
	private void placeTransitions(){
		transitions.clear();
		placeVillageDoors();
		for (int depth = 1; depth < WorldStructures.TOWN_DOORS.length; depth++){
			int cell = localTownCell( WorldStructures.TOWN_DOORS[depth] );
			if (cell == -1) continue;
			transitions.add( new LevelTransition( this, cell, LevelTransition.Type.BRANCH_EXIT,
					depth, xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel.BRANCH,
					LevelTransition.Type.BRANCH_ENTRANCE ) );
		}
		//swapped on request: the north gate is the dungeon's front door, and
		//the real staircase on the plaza descends into the kupua mines
		int gate = localTownCell( WorldStructures.TOWN_MINE_GATE );
		if (gate != -1){
			transitions.add( new LevelTransition( this, gate, LevelTransition.Type.REGULAR_EXIT,
					1, 0, LevelTransition.Type.SURFACE ) );
		}
		int stairs = localTownCell( WorldStructures.TOWN_STAIRS );
		if (stairs != -1 && !xyz.gabriwar.warpedpixeldungeon.Badges.checkOtilukeRescued()){
			transitions.add( new LevelTransition( this, stairs, LevelTransition.Type.BRANCH_EXIT,
					56, 0, LevelTransition.Type.REGULAR_ENTRANCE ) );
		}
	}

	//every village house door inside the window opens onto its own one-room
	//interior (branch 7), keyed by the depth id the door's world cell hashes to
	private void placeVillageDoors(){
		for (int sy = sector0Y(); sy <= sector1Y(); sy++){
			for (int sx = sector0X(); sx <= sector1X(); sx++){
				if (WorldStructures.siteType( worldSeed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int cx = WorldStructures.siteX( worldSeed, sx, sy );
				int cy = WorldStructures.siteY( worldSeed, sx, sy );
				int[] layout = WorldStructures.settlementLayout( worldSeed, sx, sy );
				for (int i = 1; i + 1 < layout.length; i += 2){
					int dwx = cx + WorldStructures.houseDoorDX( layout[i], layout[i+1] );
					int dwy = cy + WorldStructures.houseDoorDY( layout[i], layout[i+1] );
					int cell = localCell( dwx, dwy );
					//a door the world buried (a lake, another site's footprint)
					//is not a door any more
					if (cell == -1 || map[cell] != Terrain.DOOR) continue;
					transitions.add( new LevelTransition( this, cell, LevelTransition.Type.BRANCH_EXIT,
							WorldStructures.houseInteriorDepth( worldSeed, dwx, dwy ),
							xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel.BRANCH,
							LevelTransition.Type.BRANCH_ENTRANCE ) );
				}
			}
		}
	}

	//the sector band the window spans, with a ring of slack for the sites whose
	//footprints reach in from outside
	private int sector0X(){ return Math.floorDiv( worldX, WorldStructures.SECTOR ) - 1; }
	private int sector0Y(){ return Math.floorDiv( worldY, WorldStructures.SECTOR ) - 1; }
	private int sector1X(){ return Math.floorDiv( worldX + WIDTH, WorldStructures.SECTOR ) + 1; }
	private int sector1Y(){ return Math.floorDiv( worldY + HEIGHT, WorldStructures.SECTOR ) + 1; }

	//going in through a village door: the interior has to be told which door,
	//so it knows where to put the hero back down on the way out
	@Override
	public boolean activateTransition( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero,
			LevelTransition transition ){
		if (transition.destBranch == xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel.BRANCH){
			xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel.enterAt(
					worldX + transition.cell() % width(), worldY + transition.cell() / width() );
		}
		return super.activateTransition( hero, transition );
	}

	//(re)position the town's art over the footprint. off-window art is simply
	//not drawn - the layers stay put in customTiles/customWalls for the level's life
	private void placeTownArt(){
		if (townArt == null){
			townArt = new TownRemixedTiles.Layer[]{
					new TownRemixedTiles.Base(), new TownRemixedTiles.Deco(),
					new TownRemixedTiles.Deco2() };
			for (TownRemixedTiles.Layer layer : townArt) customTiles.add( layer );
		}
		int tx = WorldStructures.TOWN_X0 - worldX, ty = WorldStructures.TOWN_Y0 - worldY;
		for (TownRemixedTiles.Layer layer : townArt){
			layer.setRect( tx, ty, WorldStructures.TOWN_SIZE, WorldStructures.TOWN_SIZE );
			if (Dungeon.level == this
					&& com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene){
				//live: create() kills the old visual and the scene gets the new one
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( layer, false );
			}
		}
	}

	// ------------------------------------------------------- world dressing

	//two window-sized layers from ONE spritesheet (overworld_dress.png):
	//Ground below the hero (biome edge transitions, tree bodies), Canopy
	//above (walk-behind treetops). recomputed for every window
	private xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer dressGround, dressCanopy;
	private xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer.Village villageGround, villageCanopy;

	private long dressHash( int wx, int wy, long salt ){
		long h = worldSeed ^ salt;
		h ^= wx * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= wy * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		return h ^ (h >>> 33);
	}

	private static int pick( int[] set, long h ){
		return set[(int)Math.floorMod( h, set.length )];
	}

	//ground families for the edge-transition overlays, by visual strength:
	//stronger materials encroach on weaker ones. water is not here - the
	//water tiles stitch their own shores already
	private static int blendFamily( int t ){
		switch (t){
			case Terrain.SNOW: return 3;
			case Terrain.EMPTY_SP: return 2;                //sand
			case Terrain.GRASS: case Terrain.HIGH_GRASS:
			case Terrain.FURROWED_GRASS: case Terrain.SHRUB: return 1;
			case Terrain.EMPTY: case Terrain.EMPTY_DECO:
			case Terrain.DIRT_PATH: return 0;               //bare ground: receives all
			default: return -1;                             //no blending on/from this
		}
	}

	private static final int[] CORNER_ROW = { -1, xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.CORNER_GRASS,
			xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.CORNER_SAND,
			xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.CORNER_SNOW };

	private static final int[] BLEND_ROW = { -1, xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.BLEND_GRASS,
			xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.BLEND_SAND,
			xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.BLEND_SNOW };

	//a crown may hang over open ground, never over something the player has to
	//see and use: a doorway, a staircase, a signpost, a shrine
	private static boolean blocksSight( int terrain ){
		return terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR
				|| terrain == Terrain.LOCKED_DOOR || terrain == Terrain.CRYSTAL_DOOR
				|| terrain == Terrain.EXIT || terrain == Terrain.ENTRANCE
				|| terrain == Terrain.ENTRANCE_SP || terrain == Terrain.LOCKED_EXIT
				|| terrain == Terrain.SIGN || terrain == Terrain.PEDESTAL
				|| terrain == Terrain.ALCHEMY || terrain == Terrain.WELL;
	}

	private boolean forestAt( int cell ){
		if (cell < 0 || cell >= length()) return false;
		int t = map[cell];
		return (t == Terrain.TREE_PINE || t == Terrain.TREE_OAK) && !frozenAt( cell );
	}

	private java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap> bundledDress(
			java.util.Collection<xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap> list ){
		java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap> out = new java.util.ArrayList<>();
		for (xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap c : list){
			if (c instanceof xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer) out.add( c );
		}
		return out;
	}

	//the season is passed in, never read here: a network mirror is dressed for
	//the HOST's point of the year, which the client's own calendar may not
	//have caught up with when the window arrives
	private void placeDress( GameCalendar.Season season ){
		int[] ground = new int[length()];
		int[] canopy = new int[length()];
		java.util.Arrays.fill( ground, -1 );
		java.util.Arrays.fill( canopy, -1 );

		//the season picks the canopy: summer green, autumn orange-brown, and
		//in winter the forests standing on unfrozen ground carry snow (the
		//frozen ground has the winter pines). spring blossoms and autumn
		//leaves scatter on the ground by a world hash, so every window
		//agrees on where they lie
		int w = width();
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int cell = x + y * w;
				int wx = worldX + x, wy = worldY + y;
				int t = map[cell];

				//trees (the town's own pines included - its art no longer
				//paints open ground, the world dresses it)
				if (t == Terrain.TREE_PINE || t == Terrain.TREE_OAK){
					long h = dressHash( wx, wy, 0x7EEE5L );
					if (t == Terrain.TREE_OAK){
						int top, trunk;
						switch (season){
							case AUTUMN:
								top = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.OAK_AUTUMN_TOP;
								trunk = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.OAK_AUTUMN_TRUNK;
								break;
							case WINTER:
								top = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.OAK_WINTER_TOP;
								trunk = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.OAK_WINTER_TRUNK;
								break;
							default:
								top = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.OAK_SUMMER_TOP;
								trunk = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.OAK_SUMMER_TRUNK;
						}
						ground[cell] = trunk;
						//the crown hangs over the cell above the trunk - but never
						//over a way in or out: a doorway with a canopy on it reads
						//as a bush growing in the gate
						if (y > 1 && WorldStructures.townCell( wx, wy-1 ) == -1
								&& !blocksSight( map[cell-w] )) canopy[cell-w] = top;
					} else {
						ground[cell] = pick( frozenAt( cell )
								? xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.WINTER_PINES
								: xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.SUMMER_PINES, h );
					}
					continue;
				}

				if (t == Terrain.SIGN){
					ground[cell] = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.SIGNPOST;
					continue;
				}
				if (t == Terrain.DEEP_WATER){
					int depth = waterDepth != null ? waterDepth[cell] : 2;
					int[] shades = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.DEEP_SHADES;
					ground[cell] = shades[Math.max( 0, Math.min( shades.length-1, depth - 2 ) )];
					continue;
				}

				//biome edge transitions: the strongest neighbouring ground
				//material laps over this cell's edges (not inside the town -
				//its authored art owns those edges)
				if (WorldStructures.townCell( wx, wy ) != -1) continue;

				//the roads across frozen ground lie under a trodden snow cap,
				//and a bridge there is dusted over (the terrain tilemap keeps
				//BRIDGE as planks on frozen ground - the dusting is all here)
				if (frozenAt( cell )){
					if (t == Terrain.BRIDGE){
						ground[cell] = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.BRIDGE_SNOW;
						continue;
					}
					if (t == Terrain.DIRT_PATH){
						ground[cell] = pick( xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.ROAD_SNOW,
								dressHash( wx, wy, 0x5A0BL ) );
						continue;
					}
				}

				int f = blendFamily( t );
				if (f < 0) continue;
				int best = 0, mask = 0;
				for (int side = 0; side < 4; side++){
					int n = cell + (side == 0 ? -w : side == 1 ? 1 : side == 2 ? w : -1);
					int g = blendFamily( map[n] );
					if (g > f && g > 0){
						if (g > best){ best = g; mask = 0; }
						if (g == best) mask |= 1 << side;
					}
				}
				if (best > 0 && mask != 0){
					ground[cell] = BLEND_ROW[best] + mask;
				} else {
					//no side touches: a stronger material meeting only at a
					//diagonal still rounds the corner over
					int cBest = 0, cBits = 0;
					for (int d = 0; d < 4; d++){
						int n = cell + (d == 0 ? -w+1 : d == 1 ? w+1 : d == 2 ? w-1 : -w-1);
						int g = blendFamily( map[n] );
						if (g > f && g > 0){
							if (g > cBest){ cBest = g; cBits = 0; }
							if (g == cBest) cBits |= 1 << d;
						}
					}
					if (cBest > 0 && cBits != 0){
						ground[cell] = CORNER_ROW[cBest] + cBits;
					}
				}
				if (ground[cell] != -1 || frozenAt( cell )) continue;

				//spring: blossoms on the open grass of the meadows and plains
				//(about one cell in twelve); autumn: fallen leaves on the
				//ground beside the forests (about one in three)
				if (season == xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.SPRING){
					if (t != Terrain.GRASS) continue;
					long h = dressHash( wx, wy, 0xF10BEL );
					if (Math.floorMod( h, 12 ) != 0) continue;
					WorldModel.Biome b = WorldModel.biomeAt( worldSeed, wx, wy );
					if (b == WorldModel.Biome.MEADOW || b == WorldModel.Biome.PLAINS){
						ground[cell] = pick( xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.SPRING_FLOWERS, h >> 4 );
					}
				} else if (season == xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season.AUTUMN){
					if (t != Terrain.GRASS && t != Terrain.EMPTY && t != Terrain.EMPTY_DECO
							&& t != Terrain.DIRT_PATH) continue;
					long h = dressHash( wx, wy, 0x1EAFL );
					if (Math.floorMod( h, 3 ) != 0) continue;
					boolean byForest = forestAt( cell-w-1 ) || forestAt( cell-w ) || forestAt( cell-w+1 )
							|| forestAt( cell-1 ) || forestAt( cell+1 )
							|| forestAt( cell+w-1 ) || forestAt( cell+w ) || forestAt( cell+w+1 );
					if (!byForest) continue;
					WorldModel.Biome b = WorldModel.biomeAt( worldSeed, wx, wy );
					if (b == WorldModel.Biome.FOREST || b == WorldModel.Biome.MEADOW){
						ground[cell] = pick( xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.FALLEN_LEAVES, h >> 2 );
					}
				}
			}
		}

		//the villages' dressing: the stone well at the settlement's heart, in
		//every season - its masonry carries no snow, so one piece of art serves
		//the whole year. Houses use their terrain art directly, without the
		//oversized roof canopy.
		int[] vGround = new int[length()];
		int[] vCanopy = new int[length()];
		java.util.Arrays.fill( vGround, -1 );
		java.util.Arrays.fill( vCanopy, -1 );
		for (int sy = sector0Y(); sy <= sector1Y(); sy++){
			for (int sx = sector0X(); sx <= sector1X(); sx++){
				if (WorldStructures.siteType( worldSeed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int cx = WorldStructures.siteX( worldSeed, sx, sy );
				int cy = WorldStructures.siteY( worldSeed, sx, sy );
				int wellCell = localCell( cx, cy );
				if (wellCell == -1) continue;
				vGround[wellCell] = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.WELL_BASE;
				int above = localCell( cx, cy - 1 );
				if (above != -1) vCanopy[above] = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.WELL_TOP;
			}
		}

		if (dressGround == null){
			dressGround = new xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer();
			dressCanopy = new xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer();
			villageGround = new xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer.Village();
			villageCanopy = new xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer.Village();
			customTiles.add( dressGround );
			customTiles.add( villageGround );
			customWalls.add( dressCanopy );
			customWalls.add( villageCanopy );
		}
		xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer[] all =
				{ dressGround, villageGround, dressCanopy, villageCanopy };
		int[][] datas = { ground, vGround, canopy, vCanopy };
		boolean live = Dungeon.level == this
				&& com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
		for (int i = 0; i < all.length; i++){
			all[i].setRect( 0, 0, WIDTH, HEIGHT );
			all[i].setData( datas[i] );
			if (live) xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( all[i], i >= 2 );
		}
	}

	//the town's folk and chests, at the town's authored cells. each is created
	//once, when its cell first sits inside the window; from then on it persists
	//(the 'thief' blue cat that used to lurk at 372 was cut - it read as a
	//forced spawn in the middle of the plaza)
	private static final Object[][] TOWN_FOLK = {
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer4.class, 564 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer5.class, 523 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TownGuard.class, 176 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1.class, 324 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer2.class, 366 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2.class, 247 },
			//Remixed PD's own street folk, at its authored positions (Healer and
			//Plague Doctor deliberately not ported). The rest live in the buildings.
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 687 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 689 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 174 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 172 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownsfolkMovie.class, 525 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownsfolkSilent.class, 357 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Townsfolk.class, 214 },
			//the enchanting pedestal in the temple's alcove
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation.class, WorldStructures.TOWN_PEDESTAL },
	};
	private static final int[] TOWN_CHESTS = { 131, 197, 107, 289, 283, 732, 891, 740, 1022 };
	private static final int TOWN_OTILUKE = 756;

	//everything the town keeps alive, idempotent: run whenever the window
	//moves or the level is (re)built
	private void populateTown(){
		unparkMobs();

		//older saves: the plaza's 'thief' blue cat is gone
		for (Mob m : mobs.toArray( new Mob[0] )){
			if (m instanceof BlueCat
					&& WorldStructures.townCell( worldX + m.pos % width(), worldY + m.pos / width() ) != -1){
				mobs.remove( m );
				Actor.remove( m );
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
		parkedMobs.values().removeIf( m -> m instanceof BlueCat );
		parkedAt.keySet().retainAll( parkedMobs.keySet() );

		for (Object[] f : TOWN_FOLK){
			int layout = (Integer) f[1];
			int cell = localTownCell( layout );
			if (cell == -1 || townSpawned.contains( layout )) continue;
			townSpawned.add( layout );
			addFolk( (Mob) com.watabou.utils.Reflection.newInstance( (Class<?>) f[0] ), cell );
		}
		for (int layout : TOWN_CHESTS){
			int cell = localTownCell( layout );
			if (cell == -1 || townSpawned.contains( layout )) continue;
			townSpawned.add( layout );
			drop( townChestPrize(), cell ).type = Heap.Type.CHEST;
		}

		//the norn-stone altar: a blob, re-seeded whenever its cell is back in
		//the window (sliding out of the window empties it)
		int altar = localTownCell( WorldStructures.TOWN_ALTAR );
		if (altar != -1){
			Alter alter = (Alter) blobs.get( Alter.class );
			if (alter == null || alter.volume == 0){
				alter = new Alter();
				alter.seed( this, altar, 1 );
				blobs.put( Alter.class, alter );
			}
		}

		//Otiluke joins the town once rescued in the mines, and the rescue
		//seals the mine staircase for good
		if (xyz.gabriwar.warpedpixeldungeon.Badges.checkOtilukeRescued()){
			//a memorial stone stands over the sealed stairwell: solid, but not a
			//wall - the fog and wall-blocking passes leave STATUE alone, so the
			//seal reads as something that was DONE here instead of a blank gap
			int mineStairs = localTownCell( WorldStructures.TOWN_STAIRS );
			if (mineStairs != -1 && map[mineStairs] != Terrain.STATUE){
				if (Dungeon.level == this
						&& com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene){
					set( mineStairs, Terrain.STATUE );
					xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateMap( mineStairs );
				} else {
					map[mineStairs] = Terrain.STATUE;
				}
			}
			int cell = localTownCell( TOWN_OTILUKE );
			if (cell != -1 && !townSpawned.contains( TOWN_OTILUKE )){
				townSpawned.add( TOWN_OTILUKE );
				addFolk( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC(), cell );
			}
		}
	}

	private Item townChestPrize(){
		switch (Random.Int( 10 )){
			case 0: return new xyz.gabriwar.warpedpixeldungeon.items.Egg();
			case 1: return new xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift.Seed();
			case 2: return xyz.gabriwar.warpedpixeldungeon.items.Generator.randomUsingDefaults(
					xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.FOOD );
			case 3: return new xyz.gabriwar.warpedpixeldungeon.plants.Starflower.Seed();
			case 5: return new xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo();
			case 6: return new xyz.gabriwar.warpedpixeldungeon.items.SeekingClusterBombItem();
			default: return new xyz.gabriwar.warpedpixeldungeon.items.Gold( Random.IntRange( 1, 5 ) );
		}
	}

	//the town is never fogged: always lit, always explored
	@Override
	public boolean plainFogAt( int cell ){
		return inTown( cell );
	}

	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ){
		super.updateFieldOfView( c, fieldOfView );
		if (c != Dungeon.hero) return;
		int x0 = Math.max( 1, WorldStructures.TOWN_X0 - worldX );
		int y0 = Math.max( 1, WorldStructures.TOWN_Y0 - worldY );
		int x1 = Math.min( width()-2, WorldStructures.TOWN_X0 + WorldStructures.TOWN_SIZE - 1 - worldX );
		int y1 = Math.min( height()-2, WorldStructures.TOWN_Y0 + WorldStructures.TOWN_SIZE - 1 - worldY );
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				int cell = x + y * width();
				fieldOfView[cell] = true;
				visited[cell] = true;
			}
		}
	}

	/** Where a window cell is, for the HUD: the town, a named settlement, or the biome. */
	public String placeNameAt( int cell ){
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		if (WorldStructures.townCell( wx, wy ) != -1) return "Town";
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR );
		int sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (WorldStructures.siteType( worldSeed, sx+dx, sy+dy ) != WorldStructures.Site.VILLAGE) continue;
				int vx = WorldStructures.siteX( worldSeed, sx+dx, sy+dy );
				int vy = WorldStructures.siteY( worldSeed, sx+dx, sy+dy );
				int radius = WorldStructures.settlementLayout( worldSeed, sx+dx, sy+dy )[0];
				if (Math.abs( wx-vx ) <= radius + 3 && Math.abs( wy-vy ) <= radius + 3){
					return WorldStructures.villageName( worldSeed, sx+dx, sy+dy );
				}
			}
		}
		switch (WorldModel.biomeAt( worldSeed, wx, wy )){
			case OCEAN: return "Ocean";
			case RIVER: return "River";
			case BEACH: return "Beach";
			case PLAINS: return "Plains";
			case MEADOW: return "Meadow";
			case FOREST: return "Forest";
			case SWAMP: return "Swamp";
			case DESERT: return "Desert";
			case TUNDRA: return "Tundra";
			case SNOWFIELD: return "Snowfield";
			case FOOTHILLS: return "Foothills";
			default: return "Mountains";
		}
	}

	/** The signpost's text: the settlements it points at, with walking distances. */
	public void readSign( int cell ){
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		StringBuilder sb = new StringBuilder();
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR );
		int sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (int sy = sy0-2; sy <= sy0+2; sy++){
			for (int sx = sx0-2; sx <= sx0+2; sx++){
				if (WorldStructures.siteType( worldSeed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int vx = WorldStructures.siteX( worldSeed, sx, sy );
				int vy = WorldStructures.siteY( worldSeed, sx, sy );
				int dist = (int)Math.sqrt( (long)(vx-wx)*(vx-wx) + (long)(vy-wy)*(vy-wy) );
				if (dist > 2 * WorldStructures.SECTOR) continue;
				sb.append( Messages.get( OverworldLevel.class, "sign_entry",
						WorldStructures.villageName( worldSeed, sx, sy ), dist ) ).append( "\n" );
			}
		}
		int townDist = (int)Math.sqrt( (long)wx*wx + (long)wy*wy );
		sb.append( Messages.get( OverworldLevel.class, "sign_town", townDist ) );
		final String text = sb.toString();
		com.watabou.noosa.Game.runOnRenderThread( () ->
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.show(
						new xyz.gabriwar.warpedpixeldungeon.windows.WndMessage( text ) ) );
	}

	//the norn-stone altar forges relic weapons from three stones dropped on it
	@Override
	public void pressCell( int cell ){
		if (map[cell] == Terrain.PEDESTAL
				&& WorldStructures.townCell( worldX + cell % width(), worldY + cell / width() ) == WorldStructures.TOWN_ALTAR){
			Alter.transmute( cell );
		}
		super.pressCell( cell );
	}

	/** Clears the waypoint and stops the hero's walk toward it. */
	public void clearWaypoint(){
		waypointActive = false;
		waypointMarching = false;
		if (Dungeon.hero != null
				&& Dungeon.hero.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Move){
			Dungeon.hero.interrupt();
		}
	}

	//grace period: player-cancel is ignored briefly after a set, so the tap
	//that placed the waypoint can never leak through the closing window and
	//kill the march it just started
	public long waypointSetAt = 0;

	public void setWaypoint( int wx, int wy ){
		//the march walks the hero, and on a mirror the hero belongs to the host
		if (network) return;
		waypointActive = true;
		waypointMarching = true;
		waypointSetAt = System.currentTimeMillis();
		marchStallPos = -1;
		marchStallCount = 0;
		waypointX = wx;
		waypointY = wy;
		aimAtWaypoint();
	}

	//true while the level itself is issuing the waypoint walk - Hero.handle
	//must not treat that as a player tap (player taps CANCEL the waypoint)
	public boolean aimingWaypoint = false;

	//stall detection state - counted ONLY in the ready-hook re-aim path,
	//where a whole movement cycle has actually had the chance to fail.
	//counting inside aimAtWaypoint fired false 'blocked' on back-to-back
	//waypoint sets while standing still
	private int marchStallPos = -1;
	private int marchStallCount = 0;
	//adaptive leg length: shrinks while stuck (night vision, dense forest cut
	//the explored radius under the default), recovers on movement
	private int marchLegLen = 10;

	/**
	 * Act-hook entry: returns the next march destination (a passable cell
	 * toward the waypoint), or -1 when the march should not continue. Runs a
	 * genuine per-act stall check: each call means a whole act cycle had the
	 * chance to move the hero and didn't.
	 */
	public int marchDestination(){
		if (!waypointActive || !waypointMarching || Dungeon.hero == null) return -1;
		if (Dungeon.hero.pos == marchStallPos){
			//shrink the leg before striking out: shorter legs stay inside
			//whatever the CURRENT sight radius managed to reveal
			marchLegLen = Math.max( 1, marchLegLen / 2 );
			if (++marchStallCount >= 5){
				waypointMarching = false;
				marchStallCount = 0;
				marchStallPos = -1;
				marchLegLen = 10;
				GLog.w( Messages.get( this, "waypoint_blocked" ) );
				return -1;
			}
		} else {
			marchStallPos = Dungeon.hero.pos;
			marchStallCount = 0;
			marchLegLen = 10;
		}
		return navigableLeg();
	}

	//the 8 marching directions, octant-indexed (E, SE, S, SW, W, NW, N, NE)
	private static final int[][] OCTANTS = {
			{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}
	};

	/**
	 * A leg the hero can PROVABLY walk: candidates fan out from the direct
	 * bearing (then rotated 45 and 90 degrees to either side, then shorter),
	 * each tested with the same known-cells pathfinder the hero uses. the
	 * march hugs around lakes, tree lines and rock walls by itself instead
	 * of pausing at every local obstacle.
	 */
	private int navigableLeg(){
		int hx = Dungeon.hero.pos % width(), hy = Dungeon.hero.pos / width();
		int dxw = waypointX - (worldX + hx), dyw = waypointY - (worldY + hy);

		//known ground: passable AND explored - exactly what getCloser allows
		boolean[] known = new boolean[length()];
		for (int i = 0; i < known.length; i++){
			known[i] = passable[i] && (visited[i] || mapped[i]);
		}
		boolean[] fov = Dungeon.hero.fieldOfView != null
				&& Dungeon.hero.fieldOfView.length == length()
				? Dungeon.hero.fieldOfView : heroFOV;

		int octant = (int)Math.floorMod( Math.round(
				Math.atan2( dyw, dxw ) / (Math.PI / 4) ), 8 );
		int[] rotations = {0, 1, -1, 2, -2};
		int[] lens = { Math.max( 2, marchLegLen ), Math.max( 2, marchLegLen / 2 ) };

		for (int len : lens){
			for (int rot : rotations){
				int[] dir = OCTANTS[Math.floorMod( octant + rot, 8 )];
				int tx = Math.max( 2, Math.min( width()-3, hx + dir[0] * len ) );
				int ty = Math.max( 2, Math.min( height()-3, hy + dir[1] * len ) );
				int target = tx + ty * width();
				if (target == Dungeon.hero.pos) continue;
				if (!known[target] || Actor.findChar( target ) != null){
					int fixed = -1;
					for (int d : com.watabou.utils.PathFinder.NEIGHBOURS8){
						int c = target + d;
						if (c >= 0 && c < length() && known[c]
								&& Actor.findChar( c ) == null && c != Dungeon.hero.pos){
							fixed = c;
							break;
						}
					}
					if (fixed == -1) continue;
					target = fixed;
				}
				if (com.watabou.utils.PathFinder.getStep( Dungeon.hero.pos, target, known ) != -1
						&& Dungeon.findPath( Dungeon.hero, target, known, fov, true ) != null){
					return target;
				}
			}
		}
		//nothing navigable this turn - fall back to the direct leg and let the
		//stall machinery count its strike
		return marchLeg();
	}

	/**
	 * The next SHORT LEG toward the waypoint: ~10 cells from the hero in the
	 * waypoint's direction, never further. The hero's pathfinder only walks
	 * through KNOWN (visited/mapped) cells - a faraway target sits in fog and
	 * kills the path outright (the original 'march never starts' bug). short
	 * legs stay inside what the view distance has already revealed, and each
	 * act re-aims a fresh leg, so the march creeps and self-extends.
	 */
	public int marchLeg(){
		int hx = Dungeon.hero.pos % width(), hy = Dungeon.hero.pos / width();
		int hwx = worldX + hx, hwy = worldY + hy;
		int dxw = waypointX - hwx, dyw = waypointY - hwy;

		int LEG = marchLegLen;
		int stepX = Math.max( -LEG, Math.min( LEG, dxw ) );
		int stepY = Math.max( -LEG, Math.min( LEG, dyw ) );

		int tx = Math.max( 2, Math.min( width()-3, hx + stepX ) );
		int ty = Math.max( 2, Math.min( height()-3, hy + stepY ) );
		int target = clearSpotNear( tx + ty * width() );
		//a mob standing exactly on the leg's end also kills the path - side-step
		if (Actor.findChar( target ) != null && target != Dungeon.hero.pos){
			for (int d : com.watabou.utils.PathFinder.NEIGHBOURS8){
				int c = target + d;
				if (c >= 0 && c < length() && passable[c] && Actor.findChar( c ) == null){
					target = c;
					break;
				}
			}
		}
		return target;
	}

	public void aimAtWaypoint(){
		if (network || !waypointActive || Dungeon.hero == null || Dungeon.level != this) return;

		aimingWaypoint = true;
		int dst = marchLeg();
		try {
			Dungeon.hero.handle( dst );
		} finally {
			aimingWaypoint = false;
		}
		Dungeon.hero.next();
	}

	private static long worldKey( int wx, int wy ){
		return ((long)wy << 32) | (wx & 0xFFFFFFFFL);
	}

	@Override
	public com.watabou.noosa.Group addVisuals() {
		//scene creation just filled tileVariance with per-level randomness;
		//re-anchor it to world coordinates so art never shimmers on rebase
		worldAnchorVariance();
		com.watabou.noosa.Group v = super.addVisuals();
		//the wind, seen: leaves off the canopies and snow off the frozen
		//ground, one viewport-sized emitter (see WindDrift)
		v.add( new xyz.gabriwar.warpedpixeldungeon.effects.particles.WindDrift() );
		return v;
	}

	// ------------------------------------------------- surface weather hooks

	/** A tree with leaves to lose: a standing (unfrozen) forest cell. */
	public boolean canopyAt( int cell ){
		return forestAt( cell );
	}

	/** The aurora shows only over the far north: the hero on a snowfield or the tundra, at night. */
	public boolean auroraPossible(){
		if (Dungeon.hero == null || !xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.isNight()) return false;
		WorldModel.Biome b = biomeAtCell( Dungeon.hero.pos );
		return b == WorldModel.Biome.SNOWFIELD || b == WorldModel.Biome.TUNDRA;
	}

	/** A rainbow needs the sun: daytime, and the rain already over. */
	public boolean rainbowPossible(){
		xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase phase
				= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
		return (phase == xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAY
				|| phase == xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAWN)
				&& !xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.isRaining();
	}

	/** The swamps steam at dawn: fog of the place, whatever the fronts say (ClimateManager.setLocalFog). */
	public boolean localFog(){
		return Dungeon.hero != null
				&& xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase()
					== xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAWN
				&& biomeAtCell( Dungeon.hero.pos ) == WorldModel.Biome.SWAMP;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_OVERWORLD;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	@Override
	public void playLevelMusic() {
		//peaceful for now; biome-aware music comes later
		com.watabou.noosa.audio.Music.INSTANCE.playTracks(
				new String[]{Assets.Music.SEWERS_1, Assets.Music.SEWERS_2, Assets.Music.SEWERS_3},
				new float[]{1f, 1f, 0.5f},
				false);
	}

	@Override
	protected boolean build() {

		setSize( WIDTH, HEIGHT );

		if (worldSeed == 0){
			worldSeed = Dungeon.seed ^ 0x0E4A9B1DL;
			//first entry: centre the window on world origin
			worldX = -WIDTH/2;
			worldY = -HEIGHT/2;
		}

		//first arrival: the town plaza
		arrivalX = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA );
		arrivalY = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );

		seasonStamp = currentStamp();
		refreshSeasonShift();
		regenWindow();
		placeTransitions();
		populateTown();

		populate();

		return true;
	}

	//the pristine window for an origin: the pre-generated buffer when it is
	//the one that was asked for, else one full synchronous generator pass
	private Window takeWindow( int ox, int oy ){
		Window ready = null;
		synchronized (pregenLock){
			if (pregenReady && pregenOX == ox && pregenOY == oy){
				ready = pregenBuf;
				pregenBuf = null;
				pregenReady = false;
			}
		}
		return ready != null ? ready : generatePristine( ox, oy );
	}

	//installs a generated window: the pristine cache with its per-cell frozen
	//and water-depth arrays, then map[] - the host's authoritative array when
	//one is given (a network mirror), else the pristine plus the player's
	//recorded diffs - then the art layers and the flag maps. the outer ring is
	//always solid: the whole engine assumes level borders are impassable, and
	//edge water would make the water-stitcher read neighbours out of bounds.
	//the pristine output is kept so diff capture never re-runs the generator
	private void adoptWindow( Window w, int[] override, GameCalendar.Season season ){
		pristine = w.terrain;
		frozen = w.frozen;
		waterDepth = w.waterDepth;
		windowShift = w.shift;

		if (override != null){
			System.arraycopy( override, 0, map, 0, length() );
		} else if (diffs.isEmpty()){
			//overlay the player's recorded edits - just map lookups, no generator
			System.arraycopy( pristine, 0, map, 0, length() );
		} else {
			for (int y = 0; y < HEIGHT; y++){
				for (int x = 0; x < WIDTH; x++){
					int cell = x + y * width();
					Integer diff = diffs.get( worldKey( worldX + x, worldY + y ) );
					map[cell] = diff != null && x > 0 && y > 0 && x < WIDTH-1 && y < HEIGHT-1
							? diff : pristine[cell];
				}
			}
		}

		//ORDER MATTERS: the dressing reads map[] for its edge blends, its deep
		//water shades and its fallen leaves, so the authoritative terrain has
		//to be in place first
		placeTownArt();
		placeDress( season );

		buildFlagMaps();
		cleanWalls();

		windowVersion++;
	}

	private void regenWindow(){
		long t0 = System.currentTimeMillis();
		adoptWindow( takeWindow( worldX, worldY ), null, GameCalendar.season() );
		System.out.println( "[OW] window regen " + (System.currentTimeMillis()-t0) + "ms" );
	}

	// ----------------------------------------------------- network mirrors

	/**
	 * Allocates the per-level collections {@code Level.create()} normally sets
	 * up, for a level built from the network instead of generated. A mirror
	 * never runs create() - that would roll level feelings, mint the level's
	 * item drops and, on the surface, run the whole populator.
	 */
	public static void initNetworkCollections( Level l ){
		l.transitions = new ArrayList<>();
		l.mobs = new java.util.HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.butter = new SparseArray<>();
		l.naturalPlantOrder = new ArrayList<>();
	}

	/**
	 * A network mirror of a host's window: real terrain, the real snow line,
	 * the town art, the world dressing, the fog rules and the place names -
	 * all derived locally from (seed, origin, seasonal shift), with the host's
	 * authoritative map laid over the generator's output. No folk, no
	 * wildlife, no items, no transitions the host does not have, no diff
	 * store, no window streaming of its own: the host owns all of that.
	 *
	 * Safe off the render thread as long as the level is not yet
	 * {@code Dungeon.level} - the art layers only touch the scene once it is.
	 *
	 * @return null when the wire's window is not this build's window size, in
	 *         which case the caller must fall back to a generic level.
	 */
	public static OverworldLevel forNetwork( long worldSeed, int wx, int wy, float seasonShift,
			GameCalendar.Season season, int[] map, int w, int h ){
		if (w != WIDTH || h != HEIGHT) return null;
		if (map == null || map.length != WIDTH * HEIGHT) return null;

		OverworldLevel l = new OverworldLevel();
		initNetworkCollections( l );
		l.setSize( WIDTH, HEIGHT );
		l.network = true;
		return l.applyNetworkWindow( worldSeed, wx, wy, seasonShift, season, map ) ? l : null;
	}

	/**
	 * Re-derives this mirror for a new window origin and/or a new seasonal
	 * shift, and lays the host's map over it. Everything keyed by cell index
	 * (mobs, heaps, the hero, the camera) is the caller's to re-label - this
	 * only moves the ground under them.
	 *
	 * MUST run on the render thread whenever a GameScene is showing this
	 * level: the art layers hand themselves to the scene, and the tile
	 * variance table is world-anchored in the same pass.
	 *
	 * @return false when the wire's window is not the size of this one, having
	 *         changed nothing.
	 */
	public boolean applyNetworkWindow( long worldSeed, int wx, int wy, float seasonShift,
			GameCalendar.Season season, int[] map ){
		return adoptNetworkWindow( worldSeed, wx, wy, seasonShift, season,
				stageNetworkWindow( worldSeed, wx, wy, seasonShift ), map );
	}

	/**
	 * The expensive half of a mirror update - a full generator pass over the
	 * window - with nothing of this level touched. Safe (and meant) to run off
	 * the render thread: hand the result straight to adoptNetworkWindow.
	 */
	public Window stageNetworkWindow( long worldSeed, int wx, int wy, float seasonShift ){
		WorldModel.holdSeasonShift( seasonShift );
		return generatePristine( worldSeed, wx, wy );
	}

	/**
	 * The cheap half: installs an already-generated window (see
	 * stageNetworkWindow) and the host's map. Render thread only, for the
	 * reasons applyNetworkWindow documents.
	 */
	public boolean adoptNetworkWindow( long worldSeed, int wx, int wy, float seasonShift,
			GameCalendar.Season season, Window staged, int[] map ){
		if (map == null || map.length != length()) return false;
		if (staged == null || staged.terrain == null || staged.terrain.length != length()){
			//nothing usable was staged (a size skew, or a caller that could not
			//stage): generate here rather than leave the mirror on stale ground
			staged = stageNetworkWindow( worldSeed, wx, wy, seasonShift );
		}

		network = true;
		//a mirror never derives its own stamp: the host's shift is the only
		//point of the year this window is allowed to know
		seasonStamp = Integer.MIN_VALUE;
		//a PLAYER client builds its own exploration memory (it discards the
		//host's field of view and observes from its own cell), and that memory
		//is keyed by cell index - so it slides with the window exactly as the
		//host's does, or every remembered tile names a different place. nothing
		//to slide on the first adoption, when the window is still empty
		if (windowVersion > 0 && (wx != worldX || wy != worldY)){
			translateExploration( wx - worldX, wy - worldY );
		}
		this.worldSeed = worldSeed;
		this.worldX = wx;
		this.worldY = wy;

		//the host's snapshot goes in force before a single sample is taken:
		//the window, the snow line, the biome names and the dressing all hang
		//off it, and the client's own calendar must not move it afterwards
		WorldModel.holdSeasonShift( seasonShift );

		//the staged window was generated for exactly this shift; re-assert the
		//hold before adopting, because the dressing reads the snapshot again
		//for its biome lookups and a newer packet may have staged in between
		WorldModel.holdSeasonShift( seasonShift );
		adoptWindow( staged, map, season );
		placeTransitions();

		if (Dungeon.level == this) worldAnchorVariance();
		return true;
	}

	//record every window cell that no longer matches the pristine generator.
	//compares against the cached pristine window - zero generator calls.
	//the border ring is forced WALL by the window, not a player edit - skip it
	//or the world would sprout permanent phantom walls at old window edges
	private void captureDiffs(){
		if (pristine == null){
			//restored from an old save that predates the cache: rebuild once
			rebuildPristine();
		}
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int cell = x + y * width();
				int p = pristine[cell];
				if (map[cell] != p){
					long key = worldKey( worldX + x, worldY + y );
					//cosmetic degradation (burned or trampled vegetation) is NOT
					//recorded: nature reclaims it once the window moves away. this
					//also keeps the diff store from growing without bound - a big
					//wildfire would otherwise mint thousands of permanent diffs
					if (naturalDecay( p, map[cell] )){
						diffs.remove( key );
					} else {
						diffs.put( key, map[cell] );
					}
				} else if (!diffs.isEmpty()){
					//an edit that was later restored needs its stale diff dropped
					diffs.remove( worldKey( worldX + x, worldY + y ) );
				}
			}
		}
	}

	//one full generator pass to rebuild the pristine cache (load path only)
	private void rebuildPristine(){
		Window w = generatePristine( worldX, worldY );
		pristine = w.terrain;
		frozen = w.frozen;
		waterDepth = w.waterDepth;
	}

	/**
	 * Fills the tile-variance table from WORLD coordinates instead of the
	 * per-level random fill: every world cell keeps the same alt-art variant
	 * forever, no matter where the window sits. Without this each rebase
	 * re-rolled the whole window's art - a full-screen shimmer.
	 */
	public void worldAnchorVariance(){
		if (xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.tileVariance == null
				|| xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.tileVariance.length != length()){
			return;
		}
		for (int y = 0; y < HEIGHT; y++){
			for (int x = 0; x < WIDTH; x++){
				long h = worldSeed ^ 0x7A81A7CEL;
				h ^= (worldX + x) * 0x9E3779B97F4A7C15L;
				h = Long.rotateLeft( h, 31 );
				h ^= (worldY + y) * 0xC2B2AE3D27D4EB4FL;
				h *= 0xFF51AFD7ED558CCDL;
				h ^= h >>> 33;
				xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.tileVariance[x + y * width()]
						= (byte)Math.floorMod( h, 100 );
			}
		}
	}

	//is this change just vegetation damage the world should heal on its own?
	private static boolean naturalDecay( int pristine, int now ){
		boolean wasVegetation = pristine == Terrain.GRASS || pristine == Terrain.HIGH_GRASS
				|| pristine == Terrain.FURROWED_GRASS || pristine == Terrain.SHRUB;
		boolean isDamage = now == Terrain.EMBERS || now == Terrain.EMPTY
				|| now == Terrain.GRASS || now == Terrain.FURROWED_GRASS;
		return wasVegetation && isDamage;
	}

	//finds a passable cell at or spiralling out from the given cell
	private int clearSpotNear( int cell ){
		if (cell < 0 || cell >= length()) return WIDTH/2 + width()*(HEIGHT/2);
		if (passable[cell]) return cell;
		for (int r = 1; r < Math.max(WIDTH, HEIGHT); r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
					int x = cell % width() + dx, y = cell / width() + dy;
					if (x <= 0 || y <= 0 || x >= width()-1 || y >= height()-1) continue;
					int c = x + y * width();
					if (passable[c]) return c;
				}
			}
		}
		return cell;
	}

	//re-materialize any parked heap whose world position is inside the window
	private void restoreHeapsInWindow(){
		java.util.Iterator<HashMap.Entry<Long, Heap>> it = storedHeaps.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Heap> e = it.next();
			int wx = (int) (e.getKey() & 0xFFFFFFFFL);
			int wy = (int) (e.getKey() >> 32);
			int x = wx - worldX, y = wy - worldY;
			if (x > 0 && y > 0 && x < WIDTH-1 && y < HEIGHT-1){
				int cell = x + y * width();
				if (heaps.get( cell ) == null){
					Heap h = e.getValue();
					h.pos = cell;
					h.sprite = null;
					heaps.put( cell, h );
					xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( h );
					it.remove();
				}
			}
		}
	}

	@Override
	protected void createMobs() {
		//phase 5 populates the world; the phase-1 overworld is empty and peaceful
	}

	@Override
	protected void createItems() {
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		int cell;
		int tries = 100;
		do {
			cell = Random.Int( length() );
		} while ((!passable[cell] || inTown( cell )
				|| xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( cell ) != null)
				&& --tries > 0);
		return tries > 0 ? cell : -1;
	}

	//the phase the wildlife was last culled for (OverworldFauna.cull), not
	//bundled: a load re-culls once on the first step, harmlessly
	private xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase faunaCullPhase = null;

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell( ch );

		//a mirror never streams, reseasons, spawns or marches: the host does
		//all of it and ships the result
		if (!network && ch == Dungeon.hero && Dungeon.level == this){
			//the calendar (or a cold snap) moved the snow line: re-derive the
			//window first - it may move the hero off thawed ice
			int stamp = currentStamp();
			if (stamp != seasonStamp){
				seasonStamp = stamp;
				reseason();
			}

			int x = ch.pos % width(), y = ch.pos / width();
			if (x < MARGIN || y < MARGIN || x >= width() - MARGIN || y >= height() - MARGIN){
				rebase();
			} else {
				//approaching an edge: pre-generate the predicted next window
				maybePregen( x, y );
			}

			//the hour turned: cull the wildlife of the hour before (the
			//rebase pass does it too, but a hero who never reaches the
			//margin would keep the night's wolves all day)
			xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase phaseNow
					= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
			if (phaseNow != faunaCullPhase){
				faunaCullPhase = phaseNow;
				OverworldFauna.cull( this );
			}
			//the day turned: the caravans strike camp and pitch again further
			//along their roads (a hero who never reaches the margin would
			//otherwise keep the same stall standing all week)
			if (xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.weekday().ordinal() != caravanDay){
				placeCaravans();
			}

			//the swamp's fireflies
			OverworldFauna.fireflies( this, ch.pos );

			int wx = worldX + ch.pos % width(), wy = worldY + ch.pos / width();

			//waypoint reached?
			if (waypointActive
					&& Math.abs( wx - waypointX ) <= 1 && Math.abs( wy - waypointY ) <= 1){
				waypointActive = false;
				waypointMarching = false;
				xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( ch.pos ).burst(
						xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle.FACTORY, 6 );
				GLog.p( Messages.get( this, "waypoint_reached" ) );
			}

			//standing on a ruin's heart: offer the descent into the dungeon
			int sx = Math.floorDiv( wx, WorldStructures.SECTOR );
			int sy = Math.floorDiv( wy, WorldStructures.SECTOR );
			if (WorldStructures.siteType( worldSeed, sx, sy ) == WorldStructures.Site.RUIN
					&& WorldStructures.siteX( worldSeed, sx, sy ) == wx
					&& WorldStructures.siteY( worldSeed, sx, sy ) == wy){
				com.watabou.noosa.Game.runOnRenderThread( () -> {
	xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.show(
							new xyz.gabriwar.warpedpixeldungeon.windows.WndOptions(
									Messages.get( this, "ruin_title" ),
									Messages.get( this, "ruin_desc" ),
									Messages.get( this, "ruin_yes" ),
									Messages.get( this, "ruin_no" ) ){
						@Override
						protected void onSelect( int index ){
							if (index == 0){
								xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.mode
										= xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.Mode.RETURN;
								xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnDepth = 1;
								xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnBranch = 0;
								xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnPos = -1;
								com.watabou.noosa.Game.switchScene(
										xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.class );
							}
						}
					} );
			} );
			}

		}
	}

	/**
	 * Re-centres the window on the hero: captures diffs, shifts the world
	 * origin, regenerates the map, translates window content by the shift,
	 * and rebuilds the scene without a loading screen.
	 */
	private void rebase(){

		long tRebase = System.currentTimeMillis();

		int heroX = Dungeon.hero.pos % width(), heroY = Dungeon.hero.pos / width();
		//QUANTIZED shift: a fixed step per axis keeps the next origin
		//predictable, which is what lets pre-generation land a cache hit
		int dx = heroX < MARGIN ? -SHIFT_Q : heroX >= WIDTH - MARGIN ? SHIFT_Q : 0;
		int dy = heroY < MARGIN ? -SHIFT_Q : heroY >= HEIGHT - MARGIN ? SHIFT_Q : 0;
		if (dx == 0 && dy == 0) return;

		captureDiffs();

		worldX += dx;
		worldY += dy;

		//blobs SLIDE with the window BEFORE the flag maps are rebuilt, so
		//Web-style onBuildFlagMaps overrides stamp solid/flamable at the
		//post-translate cells (stamping first left phantom solids behind)
		for (xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob b
				: blobs.values().toArray( new xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob[0] )){
			b.translate( dx, dy, width(), height() );
		}

		regenWindow();

		//translate everything living in the window by (-dx, -dy)
		int shift = -dx - dy * width();

		Dungeon.hero.pos += shift;

		for (Mob m : mobs.toArray( new Mob[0] )){
			int nx = m.pos % width() - dx, ny = m.pos / width() - dy;
			if (nx <= 0 || ny <= 0 || nx >= width()-1 || ny >= height()-1){
				//walked out of the window: parked at its world position (the
				//pre-shift origin, since m.pos is still in pre-shift coords)
				//and put back when the window slides over it again. never
				//destroy()ed - that would count as a kill, and a shopkeeper's
				//destroy() sweeps EVERY FOR_SALE heap on the level
				mobs.remove( m );
				park( m, worldX - dx + m.pos % width(), worldY - dy + m.pos / width() );
			} else {
				m.pos += shift;
			}
		}
		pruneParked( worldX + Dungeon.hero.pos % width(), worldY + Dungeon.hero.pos / width() );

		//net MP: claimed and stashed remote heroes are NOT in mobs (they are
		//bundled separately and scheduled straight off the actor list), so the
		//loop above never touches them. they slide with the window like
		//everything else, or their cell index quietly starts naming a different
		//place - and the player on the other end watches themself teleport
		final java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero> netHeroes =
				new java.util.ArrayList<>(
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.allKnownNetHeroes() );
		for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh : netHeroes){
			int nx = nh.pos % width() - dx, ny = nh.pos / width() - dy;
			if (nx <= 0 || ny <= 0 || nx >= width()-1 || ny >= height()-1){
				//the window only ever follows the host, so a hero it slid past
				//is set down beside them rather than off the edge of the level
				int to = freeSpotWithin( Dungeon.hero.pos, 8 );
				nh.pos = to != -1 ? to : clearSpotNear( Dungeon.hero.pos );
			} else {
				nh.pos += shift;
			}
		}

		//the sprites that must slide with the window: exactly the mobs that
		//were translated above. anything unparked or spawned from here on gets
		//a fresh sprite already placed in the new frame
		final Mob[] toShift = mobs.toArray( new Mob[0] );

		SparseArray<Heap> oldHeaps = new SparseArray<>();
		for (Heap h : heaps.valueList()){
			oldHeaps.put( h.pos, h );
		}
		heaps.clear();
		for (Heap h : oldHeaps.valueList()){
			int ox = h.pos % width(), oy = h.pos / width();
			int nx = ox - dx, ny = oy - dy;
			if (nx > 0 && ny > 0 && nx < width()-1 && ny < height()-1){
				h.pos += shift;
				heaps.put( h.pos, h );
			} else {
				//left the window: park it at its world position - nothing on
				//the ground is ever lost, it waits for the window to come back
				//(pre-shift window origin, since ox/oy are pre-shift coords)
				long key = worldKey( worldX - dx + ox, worldY - dy + oy );
				Heap parked = storedHeaps.get( key );
				if (parked != null){
					parked.items.addAll( h.items );
				} else {
					storedHeaps.put( key, h );
				}
				if (h.sprite != null) h.sprite.kill();
			}
		}
		restoreHeapsInWindow();

		//a thrown Cross parks two raw cell indices in a buff for three turns.
		//slide them with the window; if either end scrolled out, hand the
		//weapon back at the hero's feet rather than resolve against garbage
		Cross.CircleBack circling = Dungeon.hero.buff( Cross.CircleBack.class );
		if (circling != null && !circling.translate( dx, dy, width(), height() )){
			drop( circling.cancel(), Dungeon.hero.pos ).sprite.drop();
		}

		//the town's doorways are FIXED world landmarks: transitions exist
		//exactly when their world cells sit inside the window
		placeTransitions();
		populateTown();

		//slide the exploration state along with the window
		translateExploration( dx, dy );

		plants.clear();
		traps.clear();

		//keep an autowalk going across the seam: its destination shifts with
		//the window, and getCloser rebuilds the (now stale) path against it on
		//the next step. a destination that slid past the window edge is
		//CLAMPED to the border interior - the hero keeps walking in the same
		//direction and the next rebase extends the road again
		if (Dungeon.hero.curAction != null){
			int nx = Dungeon.hero.curAction.dst % width() - dx;
			int ny = Dungeon.hero.curAction.dst / width() - dy;
			nx = Math.max( 2, Math.min( width()-3, nx ) );
			ny = Math.max( 2, Math.min( height()-3, ny ) );
			Dungeon.hero.curAction.dst = nx + ny * width();
		}
		//an active waypoint re-aims the walk against the shifted window - a
		//fresh short leg, always inside known/passable ground
		if (waypointActive && waypointMarching){
			if (Dungeon.hero.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Move){
				Dungeon.hero.curAction.dst = marchLeg();
			}
			marchStallPos = -1;
			marchStallCount = 0;
		}

		//THE INVISIBLE STREAM: a rebase is a pure coordinate re-labelling.
		//every visual - sprites WITH their in-flight motion tweens, live
		//particles, damage numbers, projectiles - and the camera all slide by
		//the same pixel delta, so the frame after the rebase renders
		//pixel-identically to the frame before. the whole visual application
		//runs as ONE atomic block on the render thread (the actor thread
		//waits up to a frame), so no half-shifted frame can ever be drawn -
		//that tear was the one-frame "teleport to the edge" flash
		populate();

		final float vsx = -dx * xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.SIZE;
		final float vsy = -dy * xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.SIZE;
		final java.util.concurrent.CountDownLatch applied = new java.util.concurrent.CountDownLatch( 1 );
		final long enqueueFrame = xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.frameId;
		com.watabou.noosa.Game.runOnRenderThread( () -> {
			long tApply = System.nanoTime();
			try {
				if (Dungeon.hero.sprite != null){
					Dungeon.hero.sprite.shiftWorld( vsx, vsy );
				}
				for (Mob m : toShift){
					if (m.sprite != null) m.sprite.shiftWorld( vsx, vsy );
				}
				//remote heroes are re-placed rather than pixel-shifted: one of
				//them may have been set down beside the host instead of sliding
				for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh : netHeroes){
					if (nh.sprite != null) nh.sprite.place( nh.pos );
				}
				for (Heap h : heaps.valueList()){
					if (h.sprite != null) h.sprite.place( h.pos );
				}
				//tile variance must be WORLD-anchored or every cell re-rolls its
				//alt art at each rebase - the whole terrain shimmered ("redraw")
				worldAnchorVariance();
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.shiftWorldVisuals( vsx, vsy );
				//fog marking joins the same atomic block, or the rebuilt fog
				//draws one frame ahead of the slid camera
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.shiftMapContent( dx, dy );
				Dungeon.observe();
				//the camera slides too - snapTo would recentre and eat the
				//deadzone offset, which read as a visible jerk
				com.watabou.noosa.Camera.main.scroll.offset( vsx, vsy );
			} finally {
				System.out.println( "[OW] visual apply " + ((System.nanoTime()-tApply)/1000000) + "ms"
						+ " (enq f" + enqueueFrame + " app f"
						+ xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.frameId + ")" );
				applied.countDown();
			}
		} );
		try {
			applied.await( 150, java.util.concurrent.TimeUnit.MILLISECONDS );
		} catch (InterruptedException ignored){}

		System.out.println( "[OW] rebase total " + (System.currentTimeMillis()-tRebase) + "ms" );
	}

	private void translateExploration( int dx, int dy ){
		boolean[] newVisited = new boolean[length()];
		boolean[] newMapped  = new boolean[length()];
		for (int y = 0; y < HEIGHT; y++){
			int oy = y + dy;
			if (oy < 0 || oy >= HEIGHT) continue;
			for (int x = 0; x < WIDTH; x++){
				int ox = x + dx;
				if (ox < 0 || ox >= WIDTH) continue;
				newVisited[x + y*width()] = visited[ox + oy*width()];
				newMapped[x + y*width()]  = mapped[ox + oy*width()];
			}
		}
		visited = newVisited;
		mapped = newMapped;
	}

	// ------------------------------------------------- life in the window

	private static long structHash( long a, long b ){
		long h = worldSeed( a, b );
		return h;
	}

	private static long worldSeed( long a, long b ){
		long h = a ^ 0xBEEF1L;
		h ^= b * 0x9E3779B97F4A7C15L;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		return h;
	}

	//is any mob of this class near the cell? parked mobs count: a site whose
	//guardians sit in the store (the window slid back before they were put
	//down, or their cells were taken) must not grow a second set
	private boolean mobNear( Class<?> cls, int cell, int radius ){
		for (Mob m : mobs){
			if (cls.isInstance( m )
					&& Math.abs( m.pos % width() - cell % width() ) <= radius
					&& Math.abs( m.pos / width() - cell / width() ) <= radius){
				return true;
			}
		}
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		for (HashMap.Entry<Long, Mob> e : parkedMobs.entrySet()){
			if (cls.isInstance( e.getValue() )
					&& Math.abs( (int)(e.getKey() & 0xFFFFFFFFL) - wx ) <= radius
					&& Math.abs( (int)(e.getKey() >> 32) - wy ) <= radius){
				return true;
			}
		}
		return false;
	}

	private boolean taggedHere( long sectorKey ){
		for (Mob m : mobs){
			if (homeSectorOf( m ) == sectorKey) return true;
		}
		for (Mob m : parkedMobs.values()){
			if (homeSectorOf( m ) == sectorKey) return true;
		}
		return false;
	}

	private static long homeSectorOf( Mob m ){
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager)
			return ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager) m).homeSector;
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager)
			return ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager) m).homeSector;
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper)
			return ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper) m).homeSector;
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard)
			return ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard) m).homeSector;
		return Long.MIN_VALUE;
	}

	private boolean addMob( Mob m, int cell ){
		//settlement layouts can reach past the window edge (a metropolis near
		//the border) - anything outside simply doesn't spawn this window
		int x = cell % width(), y = cell / width();
		if (cell < 0 || cell >= length() || x <= 1 || y <= 1 || x >= width()-2 || y >= height()-2){
			return false;
		}
		int at = clearSpotNear( cell );
		//occupied(), not Actor.findChar: on the level's first build the scheduler
		//is empty (Actor.init runs on the level switch), so everything spawned
		//here would otherwise pile onto the same clear spot
		if (occupied( at )) return false;
		m.pos = at;
		if (Dungeon.level == this){
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( m );
		} else {
			//first build: Dungeon.level is still null (GameScene.add would NPE);
			//Actor registration happens in Actor.init on the level switch
			mobs.add( m );
		}
		return true;
	}

	/**
	 * Breathes life into every site inside the window: villager families (one
	 * per house, sharing a look), guards and speciality shops in the big human
	 * towns, gnoll clans, bandit camps, skeleton-haunted ruins, dragon lairs
	 * with hoards, and free-roaming fauna by biome.
	 */
	private void populate(){

		for (int sy = sector0Y(); sy <= sector1Y(); sy++){
			for (int sx = sector0X(); sx <= sector1X(); sx++){
				WorldStructures.Site type = WorldStructures.siteType( worldSeed, sx, sy );
				if (type == WorldStructures.Site.NONE) continue;

				int cwx = WorldStructures.siteX( worldSeed, sx, sy ) - worldX;
				int cwy = WorldStructures.siteY( worldSeed, sx, sy ) - worldY;
				if (cwx < 2 || cwy < 2 || cwx >= WIDTH-2 || cwy >= HEIGHT-2) continue;
				int center = cwx + cwy * width();
				long key = (((long)sx) << 32) | (sy & 0xFFFFFFFFL);

				switch (type){
					case VILLAGE:
						populateSettlement( sx, sy, key, center );
						break;
					case RUIN:
						if (!mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.RuinSkeleton.class, center, 8 )){
							int n = 2 + (int)(structHash( sx, sy ) & 1L);
							for (int i = 0; i < n; i++){
								addMob( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.RuinSkeleton(),
										center + com.watabou.utils.PathFinder.NEIGHBOURS8[i % 8] * 2 );
							}
						}
						break;
					case DRAGON:
						if (!sitesCleared.contains( key )
								&& !mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon.class, center, 10 )){
							xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon drake
									= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon();
							drake.homeSector = key;
							addMob( drake, center );
						}
						//the hoard is laid exactly once per lair, ever
						if (!hoardLaid.contains( key )){
							hoardLaid.add( key );
							for (int i = 0; i < 3; i++){
								int c = center + com.watabou.utils.PathFinder.NEIGHBOURS8[(i*3) % 8];
								if (c >= 0 && c < length() && heaps.get( c ) == null && passable[c]){
									drop( new xyz.gabriwar.warpedpixeldungeon.items.Gold(
											com.watabou.utils.Random.IntRange( 200, 600 ) ), c );
								}
							}
						}
						break;
					case CAMP:
						//whatever the camp's owners left: dropped exactly once
						if (!hoardLaid.contains( key )){
							hoardLaid.add( key );
							int c = center + 1;
							if (heaps.get( c ) == null && passable[c]){
								Item left;
								switch ((int)(structHash( sx, sy ) & 3L)){
									case 0: left = xyz.gabriwar.warpedpixeldungeon.items.Generator.randomUsingDefaults(
											xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.FOOD ); break;
									case 1: left = xyz.gabriwar.warpedpixeldungeon.items.Generator.randomUsingDefaults(
											xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.POTION ); break;
									case 2: left = xyz.gabriwar.warpedpixeldungeon.items.Generator.randomUsingDefaults(
											xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.MISSILE ); break;
									default: left = new xyz.gabriwar.warpedpixeldungeon.items.Gold(
											com.watabou.utils.Random.IntRange( 40, 120 ) ); break;
								}
								drop( left, c ).type = Heap.Type.CHEST;
							}
						}
						break;
					case STONES:
						//the stones hold one written secret, once
						if (!hoardLaid.contains( key )){
							hoardLaid.add( key );
							if (heaps.get( center ) == null){
								drop( xyz.gabriwar.warpedpixeldungeon.items.Generator.randomUsingDefaults(
										xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.SCROLL ), center );
							}
						}
						break;
					case BIGTREE:
						//what the hollow at the great oak's foot has kept: a
						//handful of seeds and the dew caught in its bark, once
						if (!hoardLaid.contains( key )){
							hoardLaid.add( key );
							int hollow = center + width();
							if (hollow < length() && heaps.get( hollow ) == null && passable[hollow]){
								drop( xyz.gabriwar.warpedpixeldungeon.items.Generator.randomUsingDefaults(
										xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.SEED )
										.quantity( 3 ), hollow );
								drop( new xyz.gabriwar.warpedpixeldungeon.items.Dewdrop(), hollow );
							}
						}
						break;
				}
			}
		}

		placeFishermen();
		placeCaravans();

		//the hour's cull first: the night's wolves are gone by day, the
		//day's bunnies after dusk (OverworldFauna.cull)
		OverworldFauna.cull( this );

		//fauna by biome x hour x weather (OverworldFauna's table), topped up
		//away from the hero, out of the town and the settlements' streets;
		//a pack counts per member
		int fauna = 0;
		for (Mob m : mobs){
			if (OverworldFauna.isFauna( m )) fauna++;
		}
		xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase phase
				= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
		int tries = 60;
		while (fauna < 12 && tries-- > 0){
			int cell = com.watabou.utils.Random.Int( length() );
			if (!passable[cell] || water[cell] || inTown( cell ) || Actor.findChar( cell ) != null) continue;
			int heroDist = Integer.MAX_VALUE;
			if (Dungeon.hero != null && Dungeon.level == this){
				heroDist = Math.max( Math.abs( cell % width() - Dungeon.hero.pos % width() ),
						Math.abs( cell / width() - Dungeon.hero.pos / width() ) );
				if (heroDist < 16) continue;
			}
			if (OverworldFauna.nearSettlement( worldSeed,
					worldX + cell % width(), worldY + cell / width() )) continue;
			ArrayList<Mob> beasts = OverworldFauna.roll( biomeAtCell( cell ), phase );
			int placed = 0;
			for (int i = 0; i < beasts.size(); i++){
				if (addMob( beasts.get( i ), cell + (i == 0 ? 0 : com.watabou.utils.PathFinder.NEIGHBOURS8[i % 8]) )) placed++;
			}
			fauna += placed;
			if (placed >= 2 && heroDist <= 25 && OverworldFauna.isPack( beasts )) OverworldFauna.howl();
		}
	}

	private void populateSettlement( int sx, int sy, long key, int center ){
		if (taggedHere( key )) return;

		WorldStructures.Faction fac = WorldStructures.faction( worldSeed, sx, sy );

		if (fac == WorldStructures.Faction.BANDIT){
			if (!mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit.class, center, 14 )){
				int n = 3 + (int)Math.floorMod( structHash( sx, sy + 77 ), 3 );
				for (int i = 0; i < n; i++){
					addMob( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit(),
							center + com.watabou.utils.PathFinder.NEIGHBOURS8[i % 8] * 3 );
				}
			}
			return;
		}

		int[] layout = WorldStructures.settlementLayout( worldSeed, sx, sy );
		int houses = (layout.length - 1) / 2;

		//vendors scale with settlement size: village 1, town 2, city 3,
		//metropolis 4 - each with a distinct speciality
		int vendors = houses >= 45 ? 4 : houses >= 18 ? 3 : houses >= 9 ? 2
				: houses >= 5 ? 1 : 0;
		int guards  = houses >= 18 ? 4 : houses >= 9 ? 2 : 0;
		//families spawn for the most central houses only: big cities keep
		//most houses quiet, capping the mob count per settlement
		int populatedHouses = Math.min( houses, 10 );

		long sh = structHash( sx, sy );
		int vendorBase = (int)Math.floorMod( sh, xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper.SPECIALITIES );

		int vendorsPlaced = 0;
		for (int i = 1; i + 1 < layout.length && (i-1)/2 < populatedHouses; i += 2){
			int houseCell = center + layout[i] + layout[i+1] * width();
			long fh = structHash( sx * 131 + i, sy );

			//the first houses of a human settlement host its vendors
			if (fac == WorldStructures.Faction.HUMAN && vendorsPlaced < vendors){
				int spec = Math.floorMod( vendorBase + vendorsPlaced * 3,
						xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper.SPECIALITIES );
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper keeper
						= xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper.of( spec, key );
				if (addMob( keeper, houseCell )){
					keeper.openShop();
					vendorsPlaced++;
				}
				continue;
			}

			//a family per house: 1-3 members sharing a look and a colour
			int members = 1 + (int)Math.floorMod( fh, 3 );
			for (int mIdx = 0; mIdx < members; mIdx++){
				if (fac == WorldStructures.Faction.HUMAN){
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager v
							= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager();
					v.look = (int)Math.floorMod( fh >> 8, 3 );
					v.tint = (int)Math.floorMod( fh >> 16, 8 );
					v.homeSector = key;
					addMob( v, houseCell + (mIdx == 0 ? 0 : com.watabou.utils.PathFinder.NEIGHBOURS8[mIdx % 8] * 3) );
				} else {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager g
							= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager();
					g.tint = (int)Math.floorMod( fh >> 16, 8 );
					g.homeSector = key;
					addMob( g, houseCell + (mIdx == 0 ? 0 : com.watabou.utils.PathFinder.NEIGHBOURS8[mIdx % 8] * 3) );
				}
			}
		}

		//guards flank the well, more of them in the big places
		if (fac == WorldStructures.Faction.HUMAN){
			for (int g = 0; g < guards; g++){
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard guard
						= xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard.random( key );
				addMob( guard, center + com.watabou.utils.PathFinder.NEIGHBOURS8[(g * 2) % 8] * 2 );
			}
		}
	}

	// ------------------------------------------------- the road's own trade

	/**
	 * The bridge fishermen: one river crossing in six is worked for a living.
	 * A crossing speaks through its ANCHOR - the plank with no plank to its
	 * west or north - so a long bridge still gets exactly one man, and the
	 * roll is a pure function of that anchor's world cell. He is an NPC, so
	 * once placed he is parked and restored forever; a crossing that already
	 * has one (live or parked) is left alone.
	 */
	private void placeFishermen(){
		int w = width();
		for (int y = 2; y < HEIGHT-2; y++){
			for (int x = 2; x < WIDTH-2; x++){
				int cell = x + y * w;
				if (map[cell] != Terrain.BRIDGE) continue;
				if (map[cell-1] == Terrain.BRIDGE || map[cell-w] == Terrain.BRIDGE) continue;
				int wx = worldX + x, wy = worldY + y;
				if (Math.floorMod( dressHash( wx, wy, 0xF15E5L ), 6 ) != 0) continue;
				if (mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Fisherman.class, cell, 4 )) continue;
				//the bank he works from: dry, open ground beside the planks
				for (int d = 0; d < 9; d++){
					int dx = d % 3 - 1, dy = d / 3 - 1;
					if (dx == 0 && dy == 0) continue;
					int c = cell + dx + dy * w;
					if (!passable[c] || water[c] || map[c] == Terrain.BRIDGE) continue;
					if (Actor.findChar( c ) != null) continue;
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Fisherman f
							= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Fisherman();
					if (!addMob( f, c )) continue;
					f.openShop();
					break;   //one man to a bridge
				}
			}
		}
	}

	/**
	 * The caravans: one travelling stall per pair of neighbouring villages,
	 * standing a seventh of the way further along their road each weekday.
	 * Which pairs are on the road at all turns over with the day of the
	 * season, and the direction of travel is fixed per pair - so the position
	 * is a pure function of (seed, sector pair, weekday, day of season).
	 * Yesterday's stall is struck: the caravaneer and the goods laid out
	 * around HIM go with him, and nobody else's shelf is touched.
	 */
	private void placeCaravans(){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.weekday().ordinal();
		caravanDay = today;

		for (Mob m : mobs.toArray( new Mob[0] )){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m).day != today){
				strikeCaravan( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m );
			}
		}
		java.util.Iterator<HashMap.Entry<Long, Mob>> it = parkedMobs.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (!(e.getValue() instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer)) continue;
			if (((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) e.getValue()).day == today) continue;
			int px = (int)(e.getKey() & 0xFFFFFFFFL), py = (int)(e.getKey() >> 32);
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					Heap stall = storedHeaps.get( worldKey( px + dx, py + dy ) );
					if (stall != null && stall.type == Heap.Type.FOR_SALE){
						storedHeaps.remove( worldKey( px + dx, py + dy ) );
					}
				}
			}
			it.remove();
			parkedAt.remove( e.getKey() );
		}

		int dayOfSeason = xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.dayOfSeason();
		for (int sy = sector0Y(); sy <= sector1Y(); sy++){
			for (int sx = sector0X(); sx <= sector1X(); sx++){
				if (WorldStructures.siteType( worldSeed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				long nv = WorldStructures.roadNeighbour( worldSeed, sx, sy );
				if (nv == Long.MIN_VALUE) continue;
				//a pair of villages is one road: only its lower sector speaks
				if (WorldStructures.sectorOf( sx, sy ) > nv) continue;
				int nx = (int)(nv >> 32), ny = (int)nv;

				long ph = structHash( sx * 7919L + nx, sy * 7919L + ny );
				//about half the roads carry a stall on any given day, and which
				//half turns over from one day of the season to the next
				if ((((ph >>> 3) ^ dayOfSeason) & 1L) != 0) continue;

				float t = (today + 1) / 8f;
				if ((ph & 1L) != 0) t = 1f - t;    //this pair is walked the other way
				int ax = WorldStructures.siteX( worldSeed, sx, sy );
				int ay = WorldStructures.siteY( worldSeed, sx, sy );
				int bx = WorldStructures.siteX( worldSeed, nx, ny );
				int by = WorldStructures.siteY( worldSeed, nx, ny );
				int cell = localCell( Math.round( ax + (bx - ax) * t ),
						Math.round( ay + (by - ay) * t ) );
				if (cell == -1) continue;
				int road = roadSpotNear( cell );
				if (road == -1) continue;
				if (mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.class, road, 10 )) continue;

				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c
						= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer();
				c.day = today;
				if (addMob( c, road )) c.openShop();
			}
		}
	}

	//the weekday the caravans standing in the window were pitched for; not
	//bundled - the first step after a load re-checks it against the calendar
	private int caravanDay = -1;

	//an open stretch of road within four cells of the given one, or -1
	private int roadSpotNear( int cell ){
		int cx = cell % width(), cy = cell / width();
		for (int r = 0; r <= 4; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (r > 0 && Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cx + dx, y = cy + dy;
					if (x <= 1 || y <= 1 || x >= width()-2 || y >= height()-2) continue;
					int c = x + y * width();
					if (map[c] == Terrain.DIRT_PATH && passable[c] && Actor.findChar( c ) == null){
						return c;
					}
				}
			}
		}
		return -1;
	}

	//the stall comes down with the man: only the FOR_SALE heaps laid out around
	//HIM are swept, so a village shop three cells away keeps its shelf. never
	//destroy() - a Shopkeeper's destroy() clears every FOR_SALE heap on the level
	private void strikeCaravan( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c ){
		int w = width();
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int p = c.pos + dx + dy * w;
				if (p < 0 || p >= length()) continue;
				Heap stall = heaps.get( p );
				if (stall != null && stall.type == Heap.Type.FOR_SALE) stall.destroy();
			}
		}
		mobs.remove( c );
		Actor.remove( c );
		for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff b : c.buffs()) Actor.remove( b );
		if (c.sprite != null) c.sprite.killAndErase();
	}

	private static final String WORLD_SEED = "world_seed";
	private static final String WORLD_X    = "world_x";
	private static final String WORLD_Y    = "world_y";
	private static final String DIFF_KEYS  = "diff_keys";
	private static final String DIFF_VALS  = "diff_vals";
	private static final String HEAP_KEYS  = "stored_heap_keys";
	private static final String HEAP_VALS  = "stored_heaps";
	private static final String HOARDS     = "hoards_laid";
	private static final String CLEARED    = "sites_cleared";
	private static final String FOLK_KEYS  = "parked_folk_keys";
	private static final String FOLK_VALS  = "parked_folk";
	private static final String FOLK_AT    = "parked_at";
	private static final String TOWN_SPAWNED = "town_spawned";
	private static final String ARRIVAL_X  = "arrival_x";
	private static final String ARRIVAL_Y  = "arrival_y";
	private static final String WP_ACTIVE  = "wp_active";
	private static final String WP_MARCH   = "wp_marching";
	private static final String WP_X       = "wp_x";
	private static final String WP_Y       = "wp_y";
	private static final String SEASON_STAMP = "season_stamp";
	private static final String WINDOW_VER = "window_version";

	@Override
	public void storeInBundle( Bundle bundle ) {
		captureDiffs();
		super.storeInBundle( bundle );
		bundle.put( WORLD_SEED, worldSeed );
		bundle.put( WORLD_X, worldX );
		bundle.put( WORLD_Y, worldY );

		long[] keys = new long[diffs.size()];
		int[] vals = new int[diffs.size()];
		int i = 0;
		for (HashMap.Entry<Long, Integer> e : diffs.entrySet()){
			keys[i] = e.getKey();
			vals[i] = e.getValue();
			i++;
		}
		bundle.put( DIFF_KEYS, keys );
		bundle.put( DIFF_VALS, vals );

		long[] hkeys = new long[storedHeaps.size()];
		ArrayList<Heap> hvals = new ArrayList<>();
		i = 0;
		for (HashMap.Entry<Long, Heap> e : storedHeaps.entrySet()){
			hkeys[i] = e.getKey();
			hvals.add( e.getValue() );
			i++;
		}
		bundle.put( HEAP_KEYS, hkeys );
		bundle.put( HEAP_VALS, hvals );

		long[] hoard = new long[hoardLaid.size()];
		i = 0;
		for (Long k : hoardLaid) hoard[i++] = k;
		bundle.put( ARRIVAL_X, arrivalX );
		bundle.put( ARRIVAL_Y, arrivalY );

		long[] fkeys = new long[parkedMobs.size()];
		float[] fat = new float[parkedMobs.size()];
		java.util.ArrayList<Mob> fvals = new java.util.ArrayList<>();
		int fi = 0;
		for (HashMap.Entry<Long, Mob> e : parkedMobs.entrySet()){
			Float at = parkedAt.get( e.getKey() );
			fat[fi] = at != null ? at : xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.clock();
			fkeys[fi++] = e.getKey();
			fvals.add( e.getValue() );
		}
		bundle.put( FOLK_KEYS, fkeys );
		bundle.put( FOLK_VALS, fvals );
		bundle.put( FOLK_AT, fat );
		int[] spawned = new int[townSpawned.size()];
		int si = 0;
		for (int c : townSpawned) spawned[si++] = c;
		bundle.put( TOWN_SPAWNED, spawned );
		bundle.put( WP_ACTIVE, waypointActive );
		bundle.put( WP_MARCH, waypointMarching );
		bundle.put( WP_X, waypointX );
		bundle.put( WP_Y, waypointY );
		bundle.put( SEASON_STAMP, seasonStamp );
		//kept so the counter a client watches never runs backwards over a reload
		bundle.put( WINDOW_VER, windowVersion );
		bundle.put( HOARDS, hoard );
		long[] cleared = new long[sitesCleared.size()];
		i = 0;
		for (Long k : sitesCleared) cleared[i++] = k;
		bundle.put( CLEARED, cleared );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		worldSeed = bundle.getLong( WORLD_SEED );
		worldX = bundle.getInt( WORLD_X );
		worldY = bundle.getInt( WORLD_Y );

		diffs.clear();
		long[] keys = bundle.getLongArray( DIFF_KEYS );
		int[] vals = bundle.getIntArray( DIFF_VALS );
		if (keys != null && vals != null){
			for (int i = 0; i < keys.length; i++){
				diffs.put( keys[i], vals[i] );
			}
		}

		storedHeaps.clear();
		long[] hkeys = bundle.getLongArray( HEAP_KEYS );
		ArrayList<Heap> hvals = new ArrayList<>();
		for (com.watabou.utils.Bundlable b : bundle.getCollection( HEAP_VALS )){
			hvals.add( (Heap) b );
		}
		if (hkeys != null){
			for (int i = 0; i < Math.min( hkeys.length, hvals.size() ); i++){
				storedHeaps.put( hkeys[i], hvals.get( i ) );
			}
		}

		if (bundle.contains( ARRIVAL_X )){
			arrivalX = bundle.getInt( ARRIVAL_X );
			arrivalY = bundle.getInt( ARRIVAL_Y );
		} else {
			arrivalX = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA );
			arrivalY = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		}

		parkedMobs.clear();
		parkedAt.clear();
		long[] fkeys = bundle.getLongArray( FOLK_KEYS );
		//older saves carry no park times: everything parked counts as fresh
		float[] fat = bundle.contains( FOLK_AT ) ? bundle.getFloatArray( FOLK_AT ) : null;
		java.util.ArrayList<Mob> fvals = new java.util.ArrayList<>();
		for (com.watabou.utils.Bundlable b : bundle.getCollection( FOLK_VALS )){
			fvals.add( (Mob) b );
		}
		if (fkeys != null){
			for (int i = 0; i < Math.min( fkeys.length, fvals.size() ); i++){
				Mob parked = fvals.get( i );
				//a parked mob is OFF the scheduler, buffs included (see park).
				//restoring it puts the buffs straight back on - Char.restoreFromBundle
				//re-attaches every one of them, and Buff.attachTo is an Actor.add - so
				//take them off again here. left on, a parked mob's burning or poison
				//would keep ticking on a mob that is nowhere in the world, drop its
				//loot at a stale window cell, and could kill it inside the store.
				//Actor.add re-registers a char's buffs on unpark, times and all
				for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff b : parked.buffs()){
					Actor.remove( b );
				}
				parkedMobs.put( fkeys[i], parked );
				parkedAt.put( fkeys[i], fat != null && i < fat.length ? fat[i]
						: xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.clock() );
			}
		}
		townSpawned.clear();
		int[] spawned = bundle.getIntArray( TOWN_SPAWNED );
		if (spawned != null) for (int c : spawned) townSpawned.add( c );

		//Old saves may still contain the removed roof layers. Drop them while
		//recovering the three roofless town-art layers.
		java.util.ArrayList<TownRemixedTiles.Layer> found = new java.util.ArrayList<>();
		for (xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap c : customTiles){
			if (c instanceof TownRemixedTiles.Base || c instanceof TownRemixedTiles.Deco
					|| c instanceof TownRemixedTiles.Deco2) found.add( (TownRemixedTiles.Layer) c );
		}
		customWalls.removeIf( c -> c instanceof TownRemixedTiles.RoofBase
				|| c instanceof TownRemixedTiles.RoofDeco );
		if (found.size() == 3) townArt = found.toArray( new TownRemixedTiles.Layer[0] );
		customTiles.removeAll( bundledDress( customTiles ) );
		customWalls.removeAll( bundledDress( customWalls ) );
		waypointActive = bundle.getBoolean( WP_ACTIVE );
		waypointMarching = bundle.getBoolean( WP_MARCH );
		waypointX = bundle.getInt( WP_X );
		waypointY = bundle.getInt( WP_Y );

		hoardLaid.clear();
		long[] hoard = bundle.getLongArray( HOARDS );
		if (hoard != null) for (long k : hoard) hoardLaid.add( k );
		sitesCleared.clear();
		long[] cleared = bundle.getLongArray( CLEARED );
		if (cleared != null) for (long k : cleared) sitesCleared.add( k );

		//the stamp is kept as SAVED (not re-read): the window is re-derived
		//below for the calendar as it stands, and if the year moved on while
		//the hero was away (or the real clock did) the first step on the
		//surface sees the stale stamp and runs the full reseason - including
		//getting him off ice that is no longer there
		seasonStamp = bundle.contains( SEASON_STAMP ) ? bundle.getInt( SEASON_STAMP ) : -1;
		windowVersion = bundle.getInt( WINDOW_VER );
		refreshSeasonShift();

		//regenerate the window instead of trusting the bundled map: with the
		//diff store restored this reproduces the saved state byte-for-byte
		//when the generator is unchanged, and cleanly re-derives the world
		//when it HAS changed - the old path fossilized ~9k phantom "player
		//edits" after any generator update. also enforces the solid ring
		//and rebuilds the pristine cache in the same pass
		regenWindow();
		placeTransitions();
		populateTown();
	}

	/** The biome at a window cell, in world coordinates. */
	public WorldModel.Biome biomeAtCell( int cell ){
		return WorldModel.biomeAt( worldSeed,
				worldX + cell % width(), worldY + cell / width() );
	}

	@Override
	public String cellDescExtra( int cell ){
		return Messages.get( this, "biome",
				Messages.get( this, "biome_" + biomeAtCell( cell ).name().toLowerCase() ) );
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return "Water";
			case Terrain.DEEP_WATER:
				return "Deep water";
			case Terrain.BRIDGE:
				return "Plank bridge";
			case Terrain.SIGN:
				return "Signpost";
			case Terrain.TOWN_SOLID:
				return "Town building";
			case Terrain.TREE_PINE:
				return "Pine tree";
			case Terrain.TREE_OAK:
				return "Oak tree";
			case Terrain.BOULDER:
				return "Boulder";
			case Terrain.FLOWER_PATCH:
				return "Wildflowers";
			case Terrain.MUSHROOM_PATCH:
				return "Mushrooms";
			case Terrain.BARRICADE:
				return "Wooden fence";
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.WALL:
				return "A rocky mountainside.";
			case Terrain.DEEP_WATER:
				return "The bottom drops away here - too deep to wade. Only the shallows by the shore can be crossed on foot.";
			case Terrain.BRIDGE:
				return "Weathered planks carry the road over the water.";
			case Terrain.SIGN:
				return "A wooden signpost. Step up to it to read where the roads lead.";
			case Terrain.TOWN_SOLID:
				return "The timber and stone of the town.";
			case Terrain.SHRUB:
				return "A dense shrub.";
			case Terrain.TREE_PINE:
			case Terrain.TREE_OAK:
				return "A sturdy tree. It blocks the view.";
			case Terrain.BOULDER:
				return "A weathered boulder, mossy on the shady side.";
			case Terrain.FLOWER_PATCH:
				return "A patch of wildflowers swaying in the breeze.";
			case Terrain.MUSHROOM_PATCH:
				return "A cluster of marsh mushrooms.";
			case Terrain.BARRICADE:
				return "A paling fence of split logs, ringing the village. It would burn.";
			default:
				return super.tileDesc( tile );
		}
	}
}
