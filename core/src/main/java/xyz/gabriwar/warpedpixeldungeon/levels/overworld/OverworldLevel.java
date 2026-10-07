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
	//the window's edges must never be on screen, or every slide shows a band of world
	//appearing on one side: at zoom 1 a 1080p window sees 60 cells either way, so the hero
	//is kept at least 64 from every edge. the walking zone (64..112) is a shift plus 16 of
	//hysteresis, so a hero pacing across the trigger line does not slide the window back
	//and forth on every step
	private static final int MARGIN = 64;

	//which slice of the world this level is (WorldLayers): 0 the surface,
	//positive the mountains, negative the caves. fixed at construction (or by
	//the bundle) - everything derived from the window depends on it
	private int altitude = 0;

	public OverworldLevel(){
		this( 0 );
	}

	public OverworldLevel( int altitude ){
		setAltitude( altitude );
	}

	private void setAltitude( int altitude ){
		this.altitude = altitude;
		if (WorldLayers.openSky( altitude )){
			color1 = 0x48763c;
			color2 = 0x59994a;
			//open sky: see as far as the engine's shadowcaster allows
			viewDistance = 20;
		} else {
			color1 = 0x534f3e;
			color2 = 0xb9d661;
			viewDistance = 8;
		}
	}

	/** The world seed a run's dungeon seed generates the surface from. */
	public static long worldSeedOf( long dungeonSeed ){
		return dungeonSeed ^ 0x0E4A9B1DL;
	}

	/** The slice this level is (see WorldLayers). */
	public int altitude(){ return altitude; }

	/** Sky overhead - weather, day and night, the seasons - or cave rock. */
	public boolean openSky(){ return WorldLayers.openSky( altitude ); }

	/** The depth the climate simulates this slice at: the surface under the sky, a mid dungeon floor in the caves. */
	@Override
	public int climateDepth(){ return openSky() ? 0 : 16; }

	/** Debug: sends the hero to another slice, landing on the world cell under him. */
	public void travelToSlice( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero, int dest ){
		if (!WorldLayers.exists( dest ) || dest == altitude) return;
		int cell = hero.pos;
		LevelTransition t = new LevelTransition( this, cell,
				dest > altitude ? LevelTransition.Type.BRANCH_ENTRANCE : LevelTransition.Type.BRANCH_EXIT,
				WorldLayers.depthOf( dest ), 0, LevelTransition.Type.BRANCH_ENTRANCE );
		activateTransition( hero, t );
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

	/** A network mirror, which simulates nothing (HazardWatch asks before any change). */
	boolean isMirror(){
		return network;
	}

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

	//...and what the settlements' fire rings are placed by (SettlementLights.hearthCell)
	int[] pristine(){ return pristine; }

	//the ore veins of the current window (WindowGenerator.Window.veins), null on the surface
	private byte[] veins;

	//window shifts are QUANTIZED to a fixed step per axis, which makes the
	//next origin predictable - so the next window can be PREPARED on a worker
	//thread before the hero ever reaches the margin: terrain, the player's edits,
	//the dressing and the tile variance, all pure functions of (seed, origin,
	//season, edits). the rebase then commits array copies
	private static final int SHIFT_Q = 32;
	//the preparation starts this many cells before the rebase margin (kept under
	//WIDTH/2 - MARGIN, or both sides' bands would overlap)
	private static final int PREP_BAND = 16;
	//where the hero stood on his last step, for the direction the preparation bets on
	private int lastHeroCell = -1;
	private final Object pregenLock = new Object();
	//the windows being prepared or ready, by origin, oldest first: two, so a hero whose next
	//origin flips between two guesses step after step (a path zig-zagging round rocks, a walk
	//along a margin's line) gets both prepared once, where one slot restarted a whole
	//preparation at every step and never finished one
	private static final int PREP_SLOTS = 2;
	private final ArrayList<Prep> preps = new ArrayList<>( PREP_SLOTS );
	//how many preparations were ever started (the tests count them)
	private int prepsStarted = 0;

	private static final class Prep {
		final int ox, oy;
		//null while the worker runs (or when it failed: running is false then)
		WindowGenerator.Prepared result;
		boolean running = true;
		Prep( int ox, int oy ){ this.ox = ox; this.oy = oy; }
	}

	//the slot for an origin; under pregenLock
	private Prep prepFor( int ox, int oy ){
		for (Prep p : preps) if (p.ox == ox && p.oy == oy) return p;
		return null;
	}

	//bumped whenever the diff store changes; a preparation records the version it saw
	private int diffsVersion = 0;
	//the world-anchored tile variance of the current window, until the scene has taken it
	private byte[] pendingVariance;

	//per-cell frozen ground, water tier and way-between-slices of the current
	//window (see WindowGenerator.Window)
	private boolean[] frozen;
	private byte[] waterDepth;
	private byte[] link;
	//...the human villages' fields (WorldStructures.fieldPlots), and the season the window
	//was dressed for: what the fields are drawn as (DungeonTileSheet.fieldTile)
	private boolean[] field;
	private GameCalendar.Season dressSeason;
	/** Is this cell a way between the slices marked by a plain arrow (WindowGenerator.linkTile)?
	 *  Its stairs terrain is drawn as the ground around it: the arrow is all there is. The cave
	 *  mouth has art of its own, and the town keeps its staircase. */
	public boolean arrowLink( int cell ){
		if (cell < 0 || cell >= length()) return false;
		if (map[cell] != Terrain.ENTRANCE && map[cell] != Terrain.EXIT) return false;
		byte l = link != null && cell < link.length ? link[cell] : WindowGenerator.LINK_NONE;
		return l != WindowGenerator.LINK_MOUTH && l != WindowGenerator.LINK_CAVE_EXIT
				&& WorldStructures.townCell( worldX + cell % width(), worldY + cell / width() ) == -1;
	}

	/** Is the ground of this window cell snowed under? */
	public boolean frozenAt( int cell ){
		return frozen != null && cell >= 0 && cell < frozen.length && frozen[cell];
	}

	//the dangers of this slice: firedamp, cave-ins, thin ice, the thin air and the gusts of the
	//heights (HazardWatch, where they lie: LayerHazards)
	private final HazardWatch hazards = new HazardWatch( this );

	public HazardWatch hazards(){
		return hazards;
	}

	//the way between slices standing on a window cell (WindowGenerator.LINK_*), LINK_NONE off the window
	byte linkAt( int cell ){
		byte[] l = link;
		return l == null || cell < 0 || cell >= l.length ? WindowGenerator.LINK_NONE : l[cell];
	}

	/** Is this window cell a village's field, ploughed and sown (and not trampled flat)? Any
	 *  thread: the window's own mask, swapped whole when the window moves. */
	public boolean fieldAt( int cell ){
		boolean[] f = field;
		return f != null && cell >= 0 && cell < f.length && f[cell] && map[cell] == Terrain.FURROWED_GRASS;
	}

	/** The season the window's fields are drawn for: the one it was dressed for (a network
	 *  mirror's, its host's). */
	public GameCalendar.Season fieldSeason(){
		return dressSeason != null ? dressSeason : GameCalendar.season();
	}

	//the places on the mountains the window holds (layerSites' MountainSites.Site) and the heat
	//of their hot springs by window cell: both swapped whole when the window moves (adoptSites),
	//so any thread may read them
	private volatile java.util.List<MountainSites.Site> peakSites = java.util.Collections.emptyList();
	private volatile byte[] springHeat;

	/** The places on the mountains in the window (empty off the peaks). Any thread: never resolves. */
	public java.util.List<MountainSites.Site> mountainSites(){
		return peakSites;
	}

	/** The warmth a hot spring of the mountains gives this window cell (MountainSites.POOL_C on its
	 *  water, STEAM_C within two cells), or NEGATIVE_INFINITY: TileTemperature takes the higher. */
	public float springWarmth( int cell ){
		byte[] h = springHeat;
		if (h == null || cell < 0 || cell >= h.length || h[cell] == 0) return Float.NEGATIVE_INFINITY;
		return h[cell] == 2 ? MountainSites.POOL_C : MountainSites.STEAM_C;
	}

	/** Is this window cell a hot spring's water? */
	public boolean hotSpring( int cell ){
		byte[] h = springHeat;
		return h != null && cell >= 0 && cell < h.length && h[cell] == 2;
	}

	/** Under a roof on the peaks: inside the hermit's hut or a waystation's shelter, or beside the
	 *  waystation's hearth. */
	public boolean shelterAt( int cell ){
		if (cell < 0 || cell >= length()) return false;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		for (MountainSites.Site s : peakSites){
			if (s.kind != MountainSites.Kind.HERMIT && s.kind != MountainSites.Kind.PASS) continue;
			if (Math.abs( wx - s.wx ) <= 1 && Math.abs( wy - s.wy ) <= 1) return true;
			if (s.kind == MountainSites.Kind.PASS){
				int[] h = MountainSites.hearthCell( worldSeed, s );
				if (Math.abs( wx - h[0] ) <= 1 && Math.abs( wy - h[1] ) <= 1) return true;
			}
		}
		return false;
	}

	/** The ground a boulder here lies on: what its neighbours show (see WindowGenerator.rockGround). */
	public int groundUnderRock( int cell ){
		return WindowGenerator.rockGround( map, frozen, cell );
	}

	/** Is the boulder on this window cell a tall standing rock (drawn by the dressing)? */
	public boolean tallRockAt( int cell ){
		return map[cell] == Terrain.BOULDER
				&& WindowGenerator.tallRock( worldSeed, altitude, worldX + cell % width(), worldY + cell / width() );
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
		//the caves know no seasons: nothing there is derived from the calendar
		if (!openSky()) return 0;
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
		discardPreparation();
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

	//the quantized origin the window takes for a hero standing here, or null while he
	//is further than `band` cells from every edge
	private int[] nextOrigin( int heroX, int heroY, int band ){
		int pdx = heroX < band ? -SHIFT_Q : heroX >= WIDTH - band ? SHIFT_Q : 0;
		int pdy = heroY < band ? -SHIFT_Q : heroY >= HEIGHT - band ? SHIFT_Q : 0;
		if (pdx == 0 && pdy == 0) return null;
		return new int[]{ worldX + pdx, worldY + pdy };
	}

	//the origin a hero heading (hx, hy) will need next, counting an axis only when he moves
	//toward that edge and is within `band` of it; null when neither edge is ahead
	private int[] headingOrigin( int x, int y, int hx, int hy, int band ){
		int pdx = hx > 0 && x >= WIDTH - band ? SHIFT_Q : hx < 0 && x < band ? -SHIFT_Q : 0;
		int pdy = hy > 0 && y >= HEIGHT - band ? SHIFT_Q : hy < 0 && y < band ? -SHIFT_Q : 0;
		if (pdx == 0 && pdy == 0) return null;
		return new int[]{ worldX + pdx, worldY + pdy };
	}

	private boolean preparedFor( int ox, int oy ){
		synchronized (pregenLock){
			Prep p = prepFor( ox, oy );
			return p != null && p.result != null && p.result.season == GameCalendar.season();
		}
	}

	//a worker still in flight finds its slot gone and drops its result
	private void discardPreparation(){
		synchronized (pregenLock){
			preps.clear();
		}
	}

	//starts preparing the window for an origin on a worker thread, once. the worker reads
	//only the seed, the origin, the season and a snapshot of the edits taken here, and
	//writes only into its own PreparedWindow - it never touches the level
	private void schedulePrep( final int ox, final int oy ){
		final Prep slot;
		synchronized (pregenLock){
			Prep had = prepFor( ox, oy );
			if (had != null && (had.running || had.result != null)) return;
			if (had != null) preps.remove( had );
			//the oldest guess makes room
			if (preps.size() >= PREP_SLOTS) preps.remove( 0 );
			slot = new Prep( ox, oy );
			preps.add( slot );
			prepsStarted++;
		}
		final GameCalendar.Season season = GameCalendar.season();
		final HashMap<Long, Integer> edits = diffs.isEmpty() ? null : new HashMap<>( diffs );
		final int version = diffsVersion;
		//the fallen stars' scorch on that window as it stands now; a star coming down or cooling
		//before the rebase changes the signature, and the window is derived again then
		final HashMap<Long, Integer> scorch = scorchFor( ox, oy );
		final long scorchSig = scorchSig( ox, oy );
		final long seed = worldSeed;
		Thread worker = new Thread( () -> {
			WindowGenerator.Prepared p = null;
			long t0 = System.currentTimeMillis();
			try {
				p = WindowGenerator.prepare( seed, altitude, ox, oy, season, edits, version, scorch, scorchSig );
			} catch (Throwable t){
				System.out.println( "[OW] window prep failed: " + t );
			}
			synchronized (pregenLock){
				slot.result = p;
				slot.running = false;
			}
			System.out.println( "[OW] window prep " + (System.currentTimeMillis() - t0) + "ms" );
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

	/** The sectors whose dragon has been slain: a traveller's rumour never sends the hero to an empty lair. */
	public java.util.Set<Long> clearedSites(){
		return java.util.Collections.unmodifiableSet( sitesCleared );
	}

	// ------------------------------------------------- the places of a slice

	//the places of the slice laid into the current window (CaveSites.Site below the surface,
	//MountainSites.Site above it), resolved by the generator and swapped whole when a window is
	//adopted: any thread may read them
	private volatile java.util.List<Object> layerSites = java.util.Collections.emptyList();
	private volatile java.util.List<CaveSites.Site> caveSiteList = java.util.Collections.emptyList();
	//the tombs of the slice below, under this window (x0, y0, x1, y1 each): nobody digs down onto them
	private volatile int[] sealedUnder = new int[0];

	/** The places laid into the current window, an immutable list (CaveSites.Site in the caves,
	 *  MountainSites.Site on the peaks). */
	public java.util.List<Object> layerSites(){ return layerSites; }

	/** ...the caves' alone. */
	public java.util.List<CaveSites.Site> caveSites(){ return caveSiteList; }

	private void adoptSites( WindowGenerator.Window w ){
		java.util.List<Object> sites = w.sites != null ? w.sites : java.util.Collections.emptyList();
		ArrayList<CaveSites.Site> caves = new ArrayList<>();
		ArrayList<MountainSites.Site> peaks = new ArrayList<>();
		for (Object o : sites){
			if (o instanceof CaveSites.Site) caves.add( (CaveSites.Site) o );
			else if (o instanceof MountainSites.Site) peaks.add( (MountainSites.Site) o );
		}
		layerSites = sites;
		caveSiteList = java.util.Collections.unmodifiableList( caves );
		springHeat = peaks.isEmpty() ? null : MountainSites.springHeat( peaks, worldX, worldY );
		peakSites = java.util.Collections.unmodifiableList( peaks );
		sealedUnder = w.sealedBelow != null ? w.sealedBelow : new int[0];
		//the reach of every found place the window holds is known now: the HUD names it there
		//(a peak's place is kept by its anchor's cell: MountainSites.Site.key is a hash)
		boolean grown = false;
		for (CaveSites.Site s : caves) grown |= knowReach( s.key, s.x0, s.y0, s.x1, s.y1 );
		for (MountainSites.Site s : peaks) grown |= knowReach( worldKey( s.wx, s.wy ), s.x0, s.y0, s.x1, s.y1 );
		if (grown) rebuildFoundSnapshot();
	}

	//a found place's reach, once a window holding it is adopted: whether it was new
	private boolean knowReach( long key, int x0, int y0, int x1, int y1 ){
		if (!foundSites.containsKey( key ) || foundBoxes.containsKey( key )) return false;
		foundBoxes.put( key, new int[]{ x0, y0, x1, y1 } );
		return true;
	}

	/** The hoard laid once ever under this key (the hoards_laid set: a hoard laid, a stone added, a
	 *  journal read): true, and recorded, the first time. */
	public boolean claimHoard( long key ){
		return hoardLaid.add( key );
	}

	//the world day each periodic key was last due on (dueOnce), dropped a month on
	private final java.util.LinkedHashMap<Long, Integer> dueDays = new java.util.LinkedHashMap<>();

	/** True, and recorded, when this key's last record is at least periodDays world days old
	 *  (WorldClock.day), or it has none: a place's guards come back once a day. */
	boolean dueOnce( long key, int periodDays ){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		Integer last = dueDays.get( key );
		if (last != null && today - last < periodDays) return false;
		dueDays.put( key, today );
		return true;
	}

	private void sweepDue(){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		dueDays.values().removeIf( d -> today - d > 30 );
	}

	/** How many mobs of exactly this class stand, or wait parked, in a world rect (inclusive). */
	int census( Class<?> cls, int x0, int y0, int x1, int y1 ){
		int n = 0;
		for (Mob m : mobs){
			if (m.getClass() != cls) continue;
			int wx = worldX + m.pos % width(), wy = worldY + m.pos / width();
			if (wx >= x0 && wy >= y0 && wx <= x1 && wy <= y1) n++;
		}
		for (HashMap.Entry<Long, Mob> e : parkedMobs.entrySet()){
			if (e.getValue().getClass() != cls) continue;
			int wx = (int)(e.getKey() & 0xFFFFFFFFL), wy = (int)(e.getKey() >> 32);
			if (wx >= x0 && wy >= y0 && wx <= x1 && wy <= y1) n++;
		}
		return n;
	}

	/** A place a hero has seen, as the render thread reads it: its anchor's key, its kind
	 *  (CaveSites.Type ordinal in the caves), the anchor and its reach (the anchor alone until a
	 *  window holding it has been adopted since the place was restored from a save). */
	public static final class FoundSite {
		public final long key;
		public final int kind, x, y, x0, y0, x1, y1;
		FoundSite( long key, int kind, int[] box ){
			this.key = key;
			this.kind = kind;
			x = (int)(key & 0xFFFFFFFFL);
			y = (int)(key >> 32);
			x0 = box != null ? box[0] : x;
			y0 = box != null ? box[1] : y;
			x1 = box != null ? box[2] : x;
			y1 = box != null ? box[3] : y;
		}
	}

	//the places of this slice a hero has seen (anchor key -> kind), in the order found, with the
	//reach of each once known; and the copy the render thread reads, rebuilt after every change
	private final java.util.LinkedHashMap<Long, Integer> foundSites = new java.util.LinkedHashMap<>();
	private final HashMap<Long, int[]> foundBoxes = new HashMap<>();
	private volatile FoundSite[] foundSnapshot = new FoundSite[0];

	/** The places of this slice a hero has seen (the world map pins them, the HUD names them). */
	public FoundSite[] foundSites(){ return foundSnapshot; }

	private void rebuildFoundSnapshot(){
		FoundSite[] s = new FoundSite[foundSites.size()];
		int i = 0;
		for (java.util.Map.Entry<Long, Integer> e : foundSites.entrySet()){
			s[i++] = new FoundSite( e.getKey(), e.getValue(), foundBoxes.get( e.getKey() ) );
		}
		foundSnapshot = s;
	}

	/** A place is found the first time the cell it is seen from has been seen: every hero within
	 *  twelve of it hears its line. Host only (tickWorld); a guest's map shows no places. */
	void discover( long key, int kind, int seeX, int seeY, int[] box, String line ){
		if (foundSites.containsKey( key )) return;
		int c = localCell( seeX, seeY );
		if (c == -1 || visited == null || !visited[c]) return;
		foundSites.put( key, kind );
		foundBoxes.put( key, box );
		rebuildFoundSnapshot();
		for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h : heroesOn( this )){
			if (distance( h.pos, c ) <= 12) xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( h, GLog.HIGHLIGHT + line );
		}
	}

	//the burning rifts' flare (CaveSites.tick): the crack cell it bursts on (a world key, or
	//MIN_VALUE for none) and when, and when the next may come. not bundled: a load forgets a flare
	long riftFlareKey = Long.MIN_VALUE;
	float riftFlareAt, riftNextFlare;

	//what the camp's miner last said, for the window and the cell he said it at (CaveSites.rumours)
	private String rumourCache;
	private int rumourVersion = -1, rumourCell = -1;

	/** What a cave miner standing on this cell has heard (CaveSites.rumours), worked out on the
	 *  actor thread once per window. */
	public String caveRumours( int cell ){
		if (rumourCache == null || rumourVersion != windowVersion || rumourCell != cell){
			rumourCache = CaveSites.rumours( this, cell );
			rumourVersion = windowVersion;
			rumourCell = cell;
		}
		return rumourCache;
	}

	/** Can nothing break this cell - the pick, a bomb? A tomb's walls and door (CaveSites), which
	 *  stand over any older edit of their cells (resealTombs). */
	public boolean unbreakable( int cell ){
		if (altitude >= 0 || cell < 0 || cell >= length()) return false;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		for (CaveSites.Site s : caveSiteList){
			if (s.type == CaveSites.Type.TOMB && Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ) == 3) return true;
		}
		return false;
	}

	/** Is this window cell inside a tomb of the window that is still shut, its door locked? */
	boolean inSealedTomb( int cell ){
		if (altitude >= 0 || cell < 0 || cell >= length()) return false;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		for (CaveSites.Site s : caveSiteList){
			if (s.type != CaveSites.Type.TOMB || Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ) > 2) continue;
			int door = localCell( s.cx, s.cy + 3 );
			if (door != -1 && map[door] == Terrain.LOCKED_DOOR) return true;
		}
		return false;
	}

	//a tomb's walls stand over whatever an older save's edits made of their cells (a mine boulder
	//broken there before the tomb was, a shaft dug down onto it): the edit is dropped and the wall
	//laid again, so nothing but its key opens it. its door keeps an edit only when it is a door
	//(opened with the key). host only; true when the map changed
	private boolean resealTombs(){
		if (network || altitude >= 0 || diffs.isEmpty() || pristine == null) return false;
		boolean changed = false;
		for (CaveSites.Site s : caveSiteList){
			if (s.type != CaveSites.Type.TOMB) continue;
			for (int dy = -3; dy <= 3; dy++){
				for (int dx = -3; dx <= 3; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != 3) continue;
					long key = worldKey( s.cx + dx, s.cy + dy );
					Integer was = diffs.get( key );
					if (was == null) continue;
					if (dx == 0 && dy == 3 && (was == Terrain.DOOR || was == Terrain.OPEN_DOOR)) continue;
					diffs.remove( key );
					int c = localCell( s.cx + dx, s.cy + dy );
					if (c != -1) map[c] = pristine[c];
					changed = true;
				}
			}
		}
		if (changed) diffsVersion++;
		return changed;
	}

	//the nearest open ground to a cell, outside every shut tomb (and bare of heaps, when asked)
	private int openGroundNear( int cell, boolean bare ){
		int w = width();
		for (int r = 0; r < Math.max( WIDTH, HEIGHT ); r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cell % w + dx, y = cell / w + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= height() - 1) continue;
					int c = x + y * w;
					if (passable[c] && !inSealedTomb( c ) && (!bare || heaps.get( c ) == null)) return c;
				}
			}
		}
		return cell;
	}

	/** Is a sealed tomb of the slice below under this cell (the pick will not dig down onto it)? */
	public boolean sealedBelow( int cell ){
		if (cell < 0 || cell >= length()) return false;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		int[] b = sealedUnder;
		for (int i = 0; i < b.length; i += 4){
			if (wx >= b[i] && wy >= b[i + 1] && wx <= b[i + 2] && wy <= b[i + 3]) return true;
		}
		return false;
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

	//the depth the next FALL lands on, set by fallingFrom; -1 when the fall is
	//not off a slice (InterlevelScene then takes the floor below as always)
	private static int pendingFallDepth = -1;
	//the next arrival is the bottom of a shaft the hero dug: it becomes a way back up
	private static boolean pendingShaft = false;

	/** InterlevelScene.fall: the depth the pending fall lands on (consumed), or -1. */
	public static int takeFallDepth(){
		int d = pendingFallDepth;
		pendingFallDepth = -1;
		return d;
	}

	//the slice a fall from this cell lands on: off a mountain, the band of the
	//ground under the open air; everywhere else the slice below
	private int fallTarget( int cell ){
		if (altitude > 0){
			int wx = worldX + cell % width(), wy = worldY + cell / width();
			return Math.min( altitude - 1, WorldLayers.band( WorldModel.elevation( worldSeed, wx, wy ) ) );
		}
		return altitude - 1;
	}

	/** The depth a fall from this cell lands on, or -1 when there is no slice under it. */
	public int fallDepth( int cell ){
		int target = fallTarget( cell );
		return WorldLayers.exists( target ) ? WorldLayers.depthOf( target ) : -1;
	}

	/** A monster that fell alive from the slice above lands on this world cell, or waits there parked. */
	public void receiveFallen( Mob m, int wx, int wy ){
		int cell = localCell( wx, wy );
		int at = cell == -1 ? -1 : freeSpotWithin( cell, 3 );
		if (at == -1){
			park( m, wx, wy );
		} else {
			m.pos = at;
			mobs.add( m );
		}
	}

	/** Is there a slice under this cell to fall (or dig) into? */
	public boolean fallsThrough( int cell ){
		return WorldLayers.exists( fallTarget( cell ) );
	}

	/** Chasm.heroFall: the hero drops off this cell; the slice below catches him on the same world cell. */
	public void fallingFrom( int cell ){
		arriveAt( worldX + cell % width(), worldY + cell / width() );
		pendingFallDepth = WorldLayers.depthOf( fallTarget( cell ) );
	}

	/**
	 * Pickaxe: the hero digs straight down from where he stands. The cell
	 * becomes the shaft's top (an EXIT that lives on as an edit), the landing
	 * on the slice below its bottom (see landingCell), and he climbs down.
	 */
	public void digDown( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		int cell = hero.pos;
		set( cell, Terrain.EXIT );
		xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateMap( cell );
		linkTransition( cell );
		LevelTransition t = getTransition( cell );
		if (t == null) return;
		pendingShaft = true;
		activateTransition( hero, t );
	}
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

	/** Sends the hero to any slice of the world (debug scenes), landing on the pending arrival:
	 *  travelToSurface for the slices above and below. */
	public static void travelToAltitude( int altitude ){
		xyz.gabriwar.warpedpixeldungeon.levels.Level.beforeTransition();
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.mode
				= xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.Mode.RETURN;
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnDepth = WorldLayers.depthOf( altitude );
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnBranch = 0;
		xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.returnPos = -1;
		com.watabou.noosa.Game.switchScene( xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene.class );
	}

	public static final int DEPTH = 97;

	//window cell of a world cell, or -1 when it is not inside the window's interior
	public int localCell( int wx, int wy ){
		int x = wx - worldX, y = wy - worldY;
		if (x <= 0 || y <= 0 || x >= width()-1 || y >= height()-1) return -1;
		return x + y * width();
	}

	private int localTownCell( int layoutCell ){
		//the town stands on the surface only: no slice above or below it has its cells
		if (altitude != 0) return -1;
		return localCell( WorldStructures.townWorldX( layoutCell ), WorldStructures.townWorldY( layoutCell ) );
	}

	/** Is this window cell inside the town? */
	public boolean inTown( int cell ){
		return altitude == 0
				&& WorldStructures.townCell( worldX + cell % width(), worldY + cell / width() ) != -1;
	}
	//the hero is placed at getTransition(null) on arrival. the town doorways
	//are the level's real transitions; the arrival point is a synthetic one at
	//the pending/last arrival cell, re-centring the window on it if it is not
	//inside (a far-off jump: journal, beacon, coming up the dungeon stairs)
	@Override
	public LevelTransition getTransition( LevelTransition.Type type ){
		//a pending arrival is THE place the next trip lands, whatever kind of
		//transition the scene asked for (a slice's stairs ask by type)
		if (type != null && !pendingArrival) return super.getTransition( type );
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
		return new LevelTransition( this, landingCell( cell ), LevelTransition.Type.BRANCH_ENTRANCE );
	}

	//where an arrival actually stands. a fall or a dug shaft may land inside
	//rock (broken through: the cell is carved) or on something solid (the
	//nearest open cell instead); the bottom of a shaft becomes the way back up
	private int landingCell( int cell ){
		if (cell < 0 || cell >= length() || !insideMap( cell )) return clearSpotNear( cell );
		int t = map[cell];
		boolean rock = t == Terrain.WALL || t == Terrain.WALL_DECO
				|| t == Terrain.MINE_CRYSTAL || t == Terrain.MINE_BOULDER;
		//the walls of a hut or a tower on the peaks are no rock to break through: beside them instead
		if (rock && altitude > 0 && builtWall( cell ) != null) rock = false;
		if (pendingShaft){
			pendingShaft = false;
			if (t != Terrain.ENTRANCE && t != Terrain.EXIT){
				set( cell, Terrain.ENTRANCE, this );
				linkTransition( cell );
			}
			return cell;
		}
		if (rock){
			set( cell, Terrain.EMPTY_DECO, this );
			return cell;
		}
		return passable[cell] ? cell : clearSpotNear( cell );
	}

	//a fall from the slice above lands on the same world cell (fallingFrom set
	//it as the arrival); rock there is broken through
	@Override
	public int fallCell( boolean fallIntoPit ){
		return getTransition( null ).cell();
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
		//nothing lit is remembered in a window just arrived in
		litNow = litDirty = null;
		regenWindow();
		//a market's village crowds its well from the first (Settler)
		if (altitude == 0) refreshMarkets( eventTurn() );
		restoreHeapsInWindow();
		placeTransitions();
		populateTown();
		//the settlements of the new window are peopled at once, each at its day's business, and
		//so are a slice's places (a camp's trader, a tomb's guard, a hermit, an eyrie's pair): the
		//next rebase is a long walk away. the wildlife and the surface sites' guardians wait for it
		if (altitude == 0) populateSettlements();
		if (altitude != 0) populateLayerSites();
		//the first step settles the new window's events
		liveEvents = null;
	}

	//a mob leaving the window is parked at its world position. it is taken
	//off the scheduler (its buffs too - Actor.add puts them back on unpark)
	//with its clock rebased to zero, so re-adding it lands on the then-current
	//turn instead of stalling it by the time it sat parked; it loses its sprite
	//and the mob object itself waits in parkedMobs. the caller must still drop
	//it from mobs. a fleeing thief hands its loot to the heap store first so
	//nothing the player owns is ever duplicated or lost
	private void park( Mob m, int wx, int wy ){
		//never stored: the road's walkers (RoadTraffic puts whoever should be out back on the
		//road), an outlaw making off with a caravan's goods, and a hunt's beasts (the hunt ends
		//with the first to leave, HuntEvent). read BEFORE Actor.remove, which detaches buffs
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack hunting = m.buff( xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack.class );
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadWalker
				|| (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit
						&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit) m).retreating)
				|| hunting != null){
			vanish( this, m );
			if (hunting != null) HuntEvent.lost( this, hunting );
			return;
		}
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
		//an eyrie's eagle waits at its nest: where it was circling may be open air over the drop,
		//where unparkMobs finds no ground to put it back on
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle
				&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle) m).nestX != Integer.MIN_VALUE){
			wx = ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle) m).nestX;
			wy = ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle) m).nestY;
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
				|| m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.RuinSkeleton
				//an eyrie's pair is placed once (MountainSites): pruned, it would never come back
				|| m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle
				//a shrine's fish is set in its pool once (CaveSites): pruned, it would never come back
				|| m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShrinePiranha
				//an ambush party waits out its stall's day (CaravanAmbush.disbandOrphans)
				|| (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit
						&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit) m).eventHeld());
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
			int wx = (int)(e.getKey() & 0xFFFFFFFFL), wy = (int)(e.getKey() >> 32);
			int cell = localCell( wx, wy );
			if (cell == -1) continue;
			Mob m = e.getValue();
			//a settlement's people come back to wherever the day has got to (VillageRoutine)
			int at = VillageRoutine.arrivalCell( this, m, wx, wy );
			if (at == -1) at = freeSpotWithin( cell, 2 );
			if (at == -1) continue;
			//a fish whose pool cell is taken waits for it rather than coming back onto dry land
			if (!OverworldFauna.standsAt( this, m, at )) continue;
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
	boolean occupied( int cell ){
		if (Actor.findChar( cell ) != null) return true;
		for (Mob m : mobs){
			if (m.pos == cell) return true;
		}
		return false;
	}

	//the cell itself, or the nearest passable unoccupied window cell within
	//the given radius, or -1
	int freeSpotWithin( int cell, int radius ){
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
	//and mine gate. all of them are FIXED world landmarks - the transitions
	//exist exactly while their world cells sit inside the window
	private void placeTransitions(){
		transitions.clear();
		placeLayerLinks();
		if (altitude != 0) return;
		for (int depth = 1; depth < WorldStructures.TOWN_DOORS.length; depth++){
			int cell = localTownCell( WorldStructures.TOWN_DOORS[depth] );
			if (cell == -1) continue;
			transitions.add( new LevelTransition( this, cell, LevelTransition.Type.BRANCH_EXIT,
					depth, xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel.BRANCH,
					LevelTransition.Type.BRANCH_ENTRANCE ) );
		}
		//swapped on request: the north gate is the dungeon's front door, and
		//the real staircase on the plaza descends into the kupua mines
		int gate = localTownCell( WorldStructures.TOWN_DUNGEON_GATE );
		if (gate != -1){
			transitions.add( new LevelTransition( this, gate, LevelTransition.Type.REGULAR_EXIT,
					1, 0, LevelTransition.Type.SURFACE ) );
		}
		int stairs = localTownCell( WorldStructures.TOWN_MINE_GATE );
		if (stairs != -1 && !xyz.gabriwar.warpedpixeldungeon.Badges.checkOtilukeRescued()){
			transitions.add( new LevelTransition( this, stairs, LevelTransition.Type.BRANCH_EXIT,
					56, 0, LevelTransition.Type.REGULAR_ENTRANCE ) );
		}
	}

	//the ways between the slices are the window's ENTRANCE (up) and EXIT (down)
	//cells: the ones the generator made (cave mouths, cliff stairs, cave
	//ladders) and the shafts the hero dug, which live on as edits. the town's
	//own staircase is the mine's, not a slice's
	private void placeLayerLinks(){
		for (int cell = 0; cell < length(); cell++){
			int t = map[cell];
			if (t != Terrain.ENTRANCE && t != Terrain.EXIT) continue;
			if (!insideMap( cell ) || inTown( cell )) continue;
			linkTransition( cell );
		}
	}

	//the transition of one way between slices: up from an ENTRANCE, down from an EXIT
	private void linkTransition( int cell ){
		boolean up = map[cell] == Terrain.ENTRANCE;
		int dest = altitude + (up ? 1 : -1);
		if (!WorldLayers.exists( dest )) return;
		transitions.add( new LevelTransition( this, cell,
				up ? LevelTransition.Type.BRANCH_ENTRANCE : LevelTransition.Type.BRANCH_EXIT,
				WorldLayers.depthOf( dest ), 0,
				up ? LevelTransition.Type.BRANCH_EXIT : LevelTransition.Type.BRANCH_ENTRANCE ) );
	}

	//the sector band the window spans, with a ring of slack for the sites whose
	//footprints reach in from outside
	private int sector0X(){ return Math.floorDiv( worldX, WorldStructures.SECTOR ) - 1; }
	private int sector0Y(){ return Math.floorDiv( worldY, WorldStructures.SECTOR ) - 1; }
	private int sector1X(){ return Math.floorDiv( worldX + WIDTH, WorldStructures.SECTOR ) + 1; }
	private int sector1Y(){ return Math.floorDiv( worldY + HEIGHT, WorldStructures.SECTOR ) + 1; }

	//a way to another slice lands the hero on the same world cell there
	@Override
	public boolean activateTransition( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero,
			LevelTransition transition ){
		if (transition.destBranch == 0 && WorldLayers.isLayerDepth( transition.destDepth )){
			arriveAt( worldX + transition.cell() % width(), worldY + transition.cell() / width() );
		}
		return super.activateTransition( hero, transition );
	}

	//(re)position the town's art over the footprint. off-window art is simply
	//not drawn - the layers stay put in customTiles/customWalls for the level's life
	private void layoutTownArt(){
		if (altitude != 0) return;
		if (townArt == null){
			townArt = new TownRemixedTiles.Layer[]{
					new TownRemixedTiles.Base(), new TownRemixedTiles.Deco(),
					new TownRemixedTiles.Deco2(),
					new TownRemixedTiles.RoofBase(), new TownRemixedTiles.RoofDeco() };
			for (int i = 0; i < townArt.length; i++){
				(i < 3 ? customTiles : customWalls).add( townArt[i] );
			}
		}
		int tx = WorldStructures.TOWN_X0 - worldX, ty = WorldStructures.TOWN_Y0 - worldY;
		for (TownRemixedTiles.Layer layer : townArt){
			layer.setRect( tx, ty, WorldStructures.TOWN_SIZE, WorldStructures.TOWN_SIZE );
		}
	}

	boolean liveScene(){
		return Dungeon.level == this
				&& com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
	}

	/** puts the window's art on screen: the town's five layers at their window rect and the
	 *  four dress layers with the window's data. A no-op without a live scene */
	private void presentWindowArt(){
		presentWindowArt( 0, 0 );
	}

	/**
	 * The same after the window moved by (dcx, dcy): the dress tilemaps slide their content
	 * along first, so the refill finds the overlap unchanged and rebuilds only the exposed
	 * strips. Runs on the render thread during a rebase, inside the same atomic block as the
	 * sprites and the camera, so nothing moves a frame early.
	 */
	private void presentWindowArt( int dcx, int dcy ){
		if (!liveScene()) return;
		//the hero has walked on: have the world map's slab painted around them before they
		//open it (a no-op while they are still well inside the painted one)
		if ((dcx != 0 || dcy != 0) && Dungeon.hero != null){
			xyz.gabriwar.warpedpixeldungeon.ui.WorldChart.prepare( worldSeed,
					worldX + Dungeon.hero.pos % width(), worldY + Dungeon.hero.pos / width() );
		}
		if (townArt != null){
			for (int i = 0; i < townArt.length; i++){
				//the roofs draw over the hero, the rest under: the same split as their creation
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( townArt[i], i >= 3 );
			}
		}
		if (dressGround != null){
			xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer[] all = { dressGround, dressEdges, dressCanopy };
			for (int i = 0; i < all.length; i++){
				if (dcx != 0 || dcy != 0) all[i].shiftVisual( dcx, dcy );
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.add( all[i], i >= 2 );
			}
		}
		if (below != null){
			for (xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer b : below){
				if (dcx != 0 || dcy != 0) b.shiftVisual( dcx, dcy );
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.addBelow( b );
			}
		}
		if (tops != null){
			for (xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer t : tops){
				if (dcx != 0 || dcy != 0) t.shiftVisual( dcx, dcy );
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.addRockTops( t );
			}
		}
	}

	/** GameScene.create: the views of the other bands are not level tilemaps the scene builds by itself */
	public void presentSliceViews(){
		if (below != null){
			for (xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer b : below) xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.addBelow( b );
		}
		if (tops != null){
			for (xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer t : tops) xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.addRockTops( t );
		}
	}

	// ------------------------------------------------------------ the rock's tops

	//the open-sky slices' rock seen from below as the land on top of it (WindowGenerator.Window.top):
	//per window cell, the tile of its top, the dressing's blends and corners over it, and
	//whether its scarp is earth; null in the caves
	private int[] top;
	private int[][] topParts;
	private boolean[] earth;
	//the tops on screen, a view's three parts: under the walls' rims, on every rock cell with
	//rock in front of it (the rest of the rock shows its face)
	private xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer[] tops;

	private void setTopData( int[] top, int[] blends, int[] corners, boolean[] earth ){
		this.top = top;
		this.earth = earth;
		if (top == null){
			tops = null;
			topParts = null;
			return;
		}
		topParts = new int[][]{ top, blends, corners };
		if (tops == null){
			tops = new xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer[topParts.length];
			for (int i = 0; i < tops.length; i++) tops[i] = new xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer( xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer.TOPS, i );
		}
		for (int i = 0; i < tops.length; i++){
			int[] data = new int[length()];
			for (int c = 0; c < data.length; c++) data[c] = rockTopAt( c ) ? topParts[i][c] : -1;
			tops[i].setRect( 0, 0, WIDTH, HEIGHT );
			tops[i].setData( data );
		}
	}

	@Override
	public boolean rockTopAt( int cell ){
		int[] t = top;
		return t != null && cell >= 0 && cell + width() < length() && cell < t.length && t[cell] != -1
				&& map[cell] == Terrain.WALL
				&& xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.wallStitcheable( map[cell + width()] );
	}

	/** The ground on top of the rock here (WindowGenerator.viewTile), -1 where it is no natural
	 *  rock: what its lip shows over the cell north of it. */
	public int topGroundAt( int cell ){
		int[] t = top;
		return t != null && cell >= 0 && cell < t.length && map[cell] == Terrain.WALL ? t[cell] : -1;
	}

	/** Is the rock here earth rather than stone (WorldModel.earthenScarp)? Its face and rims are
	 *  drawn in earth. */
	public boolean earthAt( int cell ){
		boolean[] e = earth;
		return e != null && cell >= 0 && cell < e.length && e[cell] && map[cell] == Terrain.WALL;
	}

	/** GameScene.updateMap: a cell changed, so it and the rock above it (whose front it is) may have
	 *  gained or lost a top: mined rock shows its face, and so does the rock behind it now. */
	public void retop( int cell ){
		if (tops == null) return;
		for (int c : new int[]{ cell, cell - width() }){
			if (c < 0 || c >= length()) continue;
			boolean shown = rockTopAt( c );
			for (int i = 0; i < tops.length; i++) tops[i].setCell( c, shown ? topParts[i][c] : -1 );
			if (discoverable != null && shown) discoverable[c] = true;
		}
	}

	@Override
	public void cleanWalls(){
		super.cleanWalls();
		if (top == null) return;
		for (int c = 0; c < length(); c++) if (rockTopAt( c )) discoverable[c] = true;
	}

	//how many cells of rock a look carries into the land on top of it before it gives out
	static final int TOP_SIGHT = 3;

	//the natural rock in sight range is a half-transparent obstacle: a look runs on into the land
	//on top of a cliff, but only TOP_SIGHT cells of rock deep, and anything else that blocks sight
	//on the way (a tree, a built wall without a top) stops it as usual. So a cliff is seen from
	//its foot, and its brow a few steps in, not the whole plateau behind it
	private void seeRockTops( Char c, boolean[] fieldOfView ){
		if (top == null) return;
		seeIntoRock( width(), height(), c.pos, c.viewDistance, TOP_SIGHT,
				cell -> topGroundAt( cell ) != -1, losBlocking, fieldOfView );
	}

	/**
	 * Marks seen every rock cell within r of from whose straight line from there crosses at most
	 * depth rock cells (itself included) and no other sight-blocking cell. Pure, for the tests.
	 */
	static void seeIntoRock( int w, int h, int from, int r, int depth,
	                         java.util.function.IntPredicate rock, boolean[] blocking, boolean[] fieldOfView ){
		int cx = from % w, cy = from / w;
		for (int y = Math.max( 0, cy - r ); y <= Math.min( h - 1, cy + r ); y++){
			for (int x = Math.max( 0, cx - r ); x <= Math.min( w - 1, cx + r ); x++){
				int dx = x - cx, dy = y - cy;
				if (dx * dx + dy * dy > r * r) continue;
				int cell = x + y * w;
				if (fieldOfView[cell] || !rock.test( cell )) continue;
				if (reaches( w, cx, cy, x, y, depth, rock, blocking )) fieldOfView[cell] = true;
			}
		}
	}

	//walks the line from (x0, y0) to (x1, y1), the start left out: through at most depth cells of
	//rock, and nothing else that blocks sight
	private static boolean reaches( int w, int x0, int y0, int x1, int y1, int depth,
	                                java.util.function.IntPredicate rock, boolean[] blocking ){
		int dx = Math.abs( x1 - x0 ), dy = Math.abs( y1 - y0 );
		int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
		int err = dx - dy, x = x0, y = y0, through = 0;
		while (x != x1 || y != y1){
			int e2 = 2 * err;
			if (e2 > -dy){ err -= dy; x += sx; }
			if (e2 < dx){ err += dx; y += sy; }
			int cell = x + y * w;
			if (rock.test( cell )){
				if (++through > depth) return false;
			} else if (blocking[cell] && (x != x1 || y != y1)){
				return false;
			}
		}
		return true;
	}

	//the view down from a mountain slice: the ground of the bands below, seen through the
	//drops, a view's three parts per depth and the rims' shade (WindowGenerator.belowLayers).
	//null everywhere else
	private xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer[] below;

	/** whether the ground below shows at this cell: a drop there is drawn as that ground */
	public boolean seesBelow( int cell ){
		if (below == null) return false;
		for (xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer b : below){
			if (b.part == xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer.TILES && b.depth < WindowGenerator.BELOW_LAYERS && b.at( cell ) != -1) return true;
		}
		return false;
	}

	private void setBelowData( WindowGenerator.Views v ){
		if (v == null){
			below = null;
			return;
		}
		int depths = v.depth.length;
		if (below == null){
			below = new xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer[depths * 3 + 1];
			for (int d = 0; d < depths; d++){
				for (int part = 0; part < 3; part++) below[d * 3 + part] = new xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer( d, part );
			}
			below[depths * 3] = new xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer( depths, xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer.TILES );
		}
		for (int d = 0; d < depths; d++){
			int[][] parts = { v.depth[d].tiles, v.depth[d].blends, v.depth[d].corners };
			for (int part = 0; part < 3; part++) below[d * 3 + part].setData( parts[part] );
		}
		below[depths * 3].setData( v.shade );
		for (xyz.gabriwar.warpedpixeldungeon.tiles.SliceGroundLayer b : below) b.setRect( 0, 0, WIDTH, HEIGHT );
	}

	// ------------------------------------------------------- world dressing

	//three window-sized layers from ONE spritesheet (overworld_dress.png):
	//Ground below the hero (biome edge transitions, tree and rock bodies),
	//Edges over it (the corner roundings of those transitions), Canopy above
	//(walk-behind treetops and rock peaks). recomputed for every window
	private xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer dressGround, dressEdges, dressCanopy;

	private boolean forestAt( int cell ){
		return WindowGenerator.forestAt( map, frozen, cell );
	}

	private long dressHash( int wx, int wy, long salt ){
		return WindowGenerator.dressHash( worldSeed, wx, wy, salt );
	}

	private java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap> bundledDress(
			java.util.Collection<xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap> list ){
		java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap> out = new java.util.ArrayList<>();
		for (xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap c : list){
			if (c instanceof xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer) out.add( c );
		}
		return out;
	}

	//hands the window's dressing to its three layers (created on first use); the layers are
	//put on screen by presentWindowArt
	private void setDressData( int[][] dress ){
		if (dressGround == null){
			dressGround = new xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer();
			dressEdges = new xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer();
			dressCanopy = new xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer();
			customTiles.add( dressGround );
			customTiles.add( dressEdges );
			customWalls.add( dressCanopy );
		}
		xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer[] all = { dressGround, dressEdges, dressCanopy };
		for (int i = 0; i < all.length; i++){
			all[i].setRect( 0, 0, WIDTH, HEIGHT );
			all[i].setData( dress[i] );
		}
	}

	/** GameScene.updateMap: a cell changed, so the ore glinting on it and on the rock face above it
	 *  (whose south side it is) is worked out again - a mined vein loses its glint, and the rock
	 *  behind it shows its own if the vein runs on (Ores). Visual only: mirrors do it too. */
	public void redressRock( int cell ){
		if (altitude == 0 || dressGround == null) return;
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		redressOre( cell );
		redressOre( cell - width() );
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW redress ore", t );
	}

	//only an ore glint (or nothing) is ever replaced: the ground layer's other overlays are not this one's
	private void redressOre( int c ){
		if (c < 0 || c >= length()) return;
		int want = dressCanopy.get( c ) != -1 ? -1
				: Ores.faceTileAt( worldSeed, worldX, worldY, map, veins, null, c );
		int cur = dressGround.get( c );
		if (cur == want || (cur != -1 && Ores.faceKind( cur ) == null)) return;
		dressGround.setCell( c, want );
	}

	/** The ore the pick prises out of this cell: a slice's standing rock on a vein (Ores), or null. */
	public xyz.gabriwar.warpedpixeldungeon.items.ore.Ore oreFrom( int cell ){
		if (veins == null || cell < 0 || cell >= length() || map[cell] != Terrain.WALL || veins[cell] == 0) return null;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		Ores.Kind k = Ores.Kind.values()[veins[cell] - 1];
		xyz.gabriwar.warpedpixeldungeon.items.ore.Ore ore = com.watabou.utils.Reflection.newInstance( k.item );
		if (ore != null) ore.quantity( Ores.yield( worldSeed, altitude, wx, wy ) );
		return ore;
	}

	/** The gem a crystal of a cave seam holds when it is mined itself (Ores.gemAt), or null. */
	public xyz.gabriwar.warpedpixeldungeon.items.ore.Gem gemFrom( int cell ){
		if (altitude >= 0 || cell < 0 || cell >= length() || map[cell] != Terrain.MINE_CRYSTAL) return null;
		Ores.GemKind g = Ores.gemAt( worldSeed, altitude, worldX + cell % width(), worldY + cell / width() );
		return g == null ? null : com.watabou.utils.Reflection.newInstance( g.item );
	}

	//the ore and gems the caves' creatures have dropped on this slice today (Golem's lumps,
	//CrystalWisp's gems): each drop halves the odds of the next until the world's day turns, so the
	//beasts that come back with every window are never a mine of their own - the rock is where the ore is
	private int oreDropDay = -1, oreDrops = 0;

	/** The chance a cave creature's ore or gem drop of this base chance lands now: halved for
	 *  every one that already fell on this slice today (WorldClock.day). */
	public float oreDropChance( float base ){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		if (oreDropDay != today){
			oreDropDay = today;
			oreDrops = 0;
		}
		return base * (float) Math.pow( 0.5, oreDrops );
	}

	/** A cave creature's ore or gem fell: the next one today is half as likely. */
	public void oreDropped(){
		oreDrops++;
	}

	/** The metal glinting on this cell's rock face, as the dressing draws it (so the name always matches the art), or null. */
	public Ores.Kind shownOre( int cell ){
		return altitude == 0 || dressGround == null ? null : Ores.faceKind( dressGround.get( cell ) );
	}

	/** The colour a rich vein on this cell's rock face catches the light in (CaveLife's glints):
	 *  gold, deepsilver and skyiron, read off the dressing so it shines where the art shows it;
	 *  0 for any other rock. Render thread. */
	public int oreGlint( int cell ){
		Ores.Kind k = shownOre( cell );
		return k == Ores.Kind.GOLD || k == Ores.Kind.DEEPSILVER || k == Ores.Kind.SKYIRON ? k.colour : 0;
	}

	//the town's folk and chests, at the town's authored cells. each is created
	//once, when its cell first sits inside the window; from then on it persists
	//(the 'thief' blue cat that used to lurk at 372 was cut - it read as a
	//forced spawn in the middle of the plaza)
	private static final Object[][] TOWN_FOLK = {
			//nobody fixed at present: the enchanting pedestal moved into the church
			//(TownChurchLevel). The people are in TOWN_SLEEPERS: they come and go
			//with the sun
	};
	private static final int[] TOWN_CHESTS = { 131, 197, 107, 289, 283, 732, 891, 740, 1022 };

	//everyone who lives in the town: the quest folk and Remixed PD's own street folk,
	//at their authored positions (Healer and Plague Doctor deliberately not ported).
	//Not in TOWN_FOLK: they sleep at the inn, so TownCommute brings them out each
	//morning and they walk themselves in at dusk
	private static final Object[][] TOWN_SLEEPERS = {
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer4.class, 564 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer5.class, 523 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TownGuard.class, 176 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1.class, 324 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer2.class, 366 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2.class, 247 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 687 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 689 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 174 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk.class, 172 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownsfolkMovie.class, 525 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownsfolkSilent.class, 357 },
			{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Townsfolk.class, 214 },
	};

	//Otiluke only lives here once rescued from the mines
	public Object[][] sleeperHomes(){
		if (!xyz.gabriwar.warpedpixeldungeon.Badges.checkOtilukeRescued()) return TOWN_SLEEPERS;
		Object[][] all = java.util.Arrays.copyOf( TOWN_SLEEPERS, TOWN_SLEEPERS.length + 1 );
		all[TOWN_SLEEPERS.length] = new Object[]{ xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC.class, TOWN_OTILUKE };
		return all;
	}

	//a town layout cell as a window cell, -1 when the window does not hold it
	public int townLocal( int layoutCell ){ return localTownCell( layoutCell ); }

	//the inn's doorway, where the street folk go in at dusk and come out at dawn
	public int innDoorCell(){ return localTownCell( WorldStructures.TOWN_DOORS[6] ); }

	//a sleeper the window left behind still holds their post: nobody is sent to
	//fill it while they wait to be unparked
	public boolean parkedFolk( Class<?> cls, int home ){
		for (Mob m : parkedMobs.values()){
			if (m.getClass() == cls
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC) m).home == home) return true;
		}
		return false;
	}
	private static final int TOWN_OTILUKE = 756;

	//everything the town keeps alive, idempotent: run whenever the window
	//moves or the level is (re)built
	private void populateTown(){
		unparkMobs();
		if (altitude != 0){
			//townsfolk who wandered onto a slice before the town was the surface's alone
			for (Mob m : mobs.toArray( new Mob[0] )){
				if (m instanceof NPC && ((NPC) m).sleepsAtInn()){
					mobs.remove( m );
					Actor.remove( m );
					if (m.sprite != null) m.sprite.killAndErase();
				}
			}
			parkedMobs.values().removeIf( m -> m instanceof NPC && ((NPC) m).sleepsAtInn() );
			parkedAt.keySet().retainAll( parkedMobs.keySet() );
			return;
		}
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
		//older saves: the enchanting pedestal moved into the church
		for (Mob m : mobs.toArray( new Mob[0] )){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation){
				mobs.remove( m );
				Actor.remove( m );
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
		parkedMobs.values().removeIf( m -> m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation );
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

		//the adventurer's guide lies beside the arrival tile, so a new player has it
		//before the first stairs. Same rule as the floor-1 copy (EntranceRoom): only
		//while the intro page is unread or the tutorial is on. The plaza cell is its
		//own spawn marker, nothing else in the town claims it
		int plaza = localTownCell( WorldStructures.TOWN_PLAZA );
		if (plaza != -1 && !townSpawned.contains( WorldStructures.TOWN_PLAZA )
				&& (!xyz.gabriwar.warpedpixeldungeon.journal.Document.ADVENTURERS_GUIDE.isPageRead(
						xyz.gabriwar.warpedpixeldungeon.journal.Document.GUIDE_INTRO )
					|| xyz.gabriwar.warpedpixeldungeon.WPDSettings.intro())){
			townSpawned.add( WorldStructures.TOWN_PLAZA );
			int at = freeSpotWithin( plaza + 1, 2 );
			if (at == -1) at = plaza;
			drop( new xyz.gabriwar.warpedpixeldungeon.items.journal.Guidebook(), at );
			xyz.gabriwar.warpedpixeldungeon.journal.Document.ADVENTURERS_GUIDE.deletePage(
					xyz.gabriwar.warpedpixeldungeon.journal.Document.GUIDE_INTRO );
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

		//the rescue of Otiluke seals the mine staircase for good (he himself
		//joins the town through sleeperHomes)
		if (xyz.gabriwar.warpedpixeldungeon.Badges.checkOtilukeRescued()){
			//a memorial stone stands over the sealed stairwell: solid, but not a
			//wall - the fog and wall-blocking passes leave STATUE alone, so the
			//seal reads as something that was DONE here instead of a blank gap
			int mineStairs = localTownCell( WorldStructures.TOWN_MINE_GATE );
			if (mineStairs != -1 && map[mineStairs] != Terrain.STATUE){
				if (Dungeon.level == this
						&& com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene){
					set( mineStairs, Terrain.STATUE );
					xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateMap( mineStairs );
				} else {
					map[mineStairs] = Terrain.STATUE;
				}
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

	//from the first change a rebase makes to the level until its render block has moved the scene
	//too, the map, the exploration and the field of view are not all in one frame (rebase)
	private volatile boolean shifting = false;

	@Override
	public boolean fogHeld(){
		return shifting;
	}

	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ){
		super.updateFieldOfView( c, fieldOfView );
		if (c == Dungeon.hero) seeRockTops( c, fieldOfView );
		boolean own = c == Dungeon.hero && fieldOfView == heroFOV;
		//the ground lit for the hero last time is repainted too: some of it may be out of sight now
		if (own) litDirty = union( litDirty, litNow );
		int[] lit = null;
		if (altitude < 0 && c instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero
				&& c.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness.class ) == null){
			lit = litSight( c, fieldOfView );
		}
		if (own){
			litNow = lit;
			litDirty = union( litDirty, lit );
		}
		if (c != Dungeon.hero || altitude != 0) return;
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

	//how far a lit place is seen from in the dark of the caves, against the slice's eight
	private static final int LIT_SIGHT = 16;
	private boolean[] litBuf;

	//the caves' lanterns, fires and glowing mushrooms (CaveSites) light the ground round them: a
	//hero with a clear line to it sees that ground from twice his own sight, so a camp or a mine
	//reads from across the chamber. returns the rect (window x0, y0, x1, y1) it showed, or null
	private int[] litSight( Char c, boolean[] fieldOfView ){
		int w = width(), cx = c.pos % w, cy = c.pos / w;
		ArrayList<int[]> near = null;
		for (CaveSites.Site s : caveSiteList){
			for (int i = 0; i < s.lights.length; i += 4){
				int x = s.lights[i] - worldX, y = s.lights[i + 1] - worldY;
				if (Math.max( Math.abs( x - cx ), Math.abs( y - cy ) ) > LIT_SIGHT) continue;
				if (near == null) near = new ArrayList<>();
				near.add( new int[]{ x, y } );
			}
		}
		if (near == null) return null;
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		if (litBuf == null || litBuf.length != length()) litBuf = new boolean[length()];
		java.util.Arrays.fill( litBuf, false );
		xyz.gabriwar.warpedpixeldungeon.mechanics.ShadowCaster.castShadow( cx, cy, w, litBuf, losBlocking, LIT_SIGHT );
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = -1, y1 = -1;
		for (int[] l : near){
			for (int dy = -2; dy <= 2; dy++){
				for (int dx = -2; dx <= 2; dx++){
					int x = l[0] + dx, y = l[1] + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= height() - 1) continue;
					int cell = x + y * w;
					if (!litBuf[cell]) continue;
					fieldOfView[cell] = true;
					x0 = Math.min( x0, x ); y0 = Math.min( y0, y );
					x1 = Math.max( x1, x ); y1 = Math.max( y1, y );
				}
			}
		}
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW lit sight", t );
		return x1 == -1 ? null : new int[]{ x0, y0, x1, y1 };
	}

	//the ground litSight showed the hero last, and all it has shown since Dungeon.observe last
	//wrote it into the map (window rects x0, y0, x1, y1, or null)
	private int[] litNow, litDirty;

	//a window rect (x0, y0, x1, y1) after the window moved by (dx, dy), clipped to the interior; null when nothing is left
	private int[] shiftRect( int[] r, int dx, int dy ){
		if (r == null) return null;
		int x0 = Math.max( 1, r[0] - dx ), y0 = Math.max( 1, r[1] - dy );
		int x1 = Math.min( width() - 2, r[2] - dx ), y1 = Math.min( height() - 2, r[3] - dy );
		return x0 > x1 || y0 > y1 ? null : new int[]{ x0, y0, x1, y1 };
	}

	private static int[] union( int[] a, int[] b ){
		if (a == null) return b;
		if (b == null) return a;
		return new int[]{ Math.min( a[0], b[0] ), Math.min( a[1], b[1] ), Math.max( a[2], b[2] ), Math.max( a[3], b[3] ) };
	}

	/** Dungeon.observe, after its own square round the hero: the far ground the caves' lights
	 *  show him is explored, and it and what they showed before are repainted in the fog, so the
	 *  lit camp shows and ground that has gone out of his sight is drawn as remembered. */
	public void observeLit(){
		int[] r = litDirty;
		litDirty = null;
		if (r == null || visited == null || heroFOV == null) return;
		int w = r[2] - r[0] + 1;
		for (int y = r[1]; y <= r[3]; y++){
			com.watabou.utils.BArray.or( visited, heroFOV, r[0] + y * width(), w, visited );
		}
		xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog( r[0], r[1], w, r[3] - r[1] + 1 );
	}

	/** Where a window cell is, for the HUD: the town, a named settlement, or the biome; on a slice,
	 *  the found place whose reach holds it. Render thread: reads the found sites' snapshot alone. */
	public String placeNameAt( int cell ){
		if (altitude < 0){
			int wx = worldX + cell % width(), wy = worldY + cell / width();
			for (FoundSite f : foundSnapshot){
				if (f.kind < 0 || f.kind >= CaveSites.Type.values().length) continue;
				if (wx >= f.x0 && wy >= f.y0 && wx <= f.x1 && wy <= f.y1){
					return Messages.get( CaveSites.class, "place_site", CaveSites.mapName( CaveSites.Type.values()[f.kind] ), -altitude );
				}
			}
		}
		if (altitude > 0){
			int wx = worldX + cell % width(), wy = worldY + cell / width();
			for (FoundSite f : foundSnapshot){
				if (f.kind < 0 || f.kind >= MountainSites.Kind.values().length) continue;
				if (wx >= f.x0 && wy >= f.y0 && wx <= f.x1 && wy <= f.y1){
					return Messages.get( MountainSites.class, "place_site",
							MountainSites.mapName( worldSeed, MountainSites.Kind.values()[f.kind], f.x, f.y ), altitude );
				}
			}
		}
		if (altitude > 0) return Messages.get( this, "place_above", altitude );
		if (altitude < 0) return Messages.get( this, "place_below", -altitude );
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
		//a waystation's signpost names its pass (MountainSites)
		MountainSites.Site pass = altitude > 0 ? MountainSites.signOf( worldSeed, peakSites, wx, wy ) : null;
		if (pass != null){
			final String text = Messages.get( MountainSites.class, "pass_sign", MountainSites.passName( worldSeed, pass.wx, pass.wy ) );
			com.watabou.noosa.Game.runOnRenderThread( () ->
					xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.show(
							new xyz.gabriwar.warpedpixeldungeon.windows.WndMessage( text ) ) );
			return;
		}
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

	static long worldKey( int wx, int wy ){
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
		//the small life about the hero, on every slice: the surface's birds, hares, fish, butterflies
		//and fireflies, the caves' bats, drips, glows and blind fish, the peaks' eagle, marmots, snow
		//plumes and clouds (OverworldCritters, CaveLife, PeakLife). pictures only, so a network
		//mirror shows its own
		if (hasCritters()) v.add( new OverworldCritters.Field( this ) );
		//the settlements' smoke and lamplight on a network client, which runs no hero turn
		//to light them (SettlementAmbience.onHeroTurn lights the host's) - and the caves' and the peaks' places
		v.add( new SettlementAmbience.Mirror( this ) );
		//and the timed events' streaks, smoke, glow and bunting (the host's step shows his)
		if (altitude == 0) v.add( new EventDecorMirror( this ) );
		//the cracks of thin ice stepped on (HazardWatch): the slices' only, the surface's ice never cracks
		v.add( new IceCracks( this ) );
		return v;
	}

	/** Whether the small life about the hero lives here (OverworldCritters.Field): every slice
	 *  of the world has its own, the caves' and the peaks' as much as the surface's. */
	boolean hasCritters(){
		return WorldLayers.exists( altitude );
	}

	// ------------------------------------------------- surface weather hooks

	/** A tree with leaves to lose: a standing (unfrozen) forest cell. */
	public boolean canopyAt( int cell ){
		return forestAt( cell );
	}

	/** The aurora shows only over the far north: the hero on a snowfield or the tundra, at night. */
	public boolean auroraPossible(){
		if (Dungeon.hero == null || !openSky()
				|| !xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.isNight()) return false;
		if (altitude > 0) return true;   //the peaks stand above the weather
		WorldModel.Biome b = biomeAtCell( Dungeon.hero.pos );
		return b == WorldModel.Biome.SNOWFIELD || b == WorldModel.Biome.TUNDRA;
	}
	/** A rainbow needs the sun: daytime, and the rain already over. */
	public boolean rainbowPossible(){
		if (!openSky()) return false;
		xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase phase
				= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
		return (phase == xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAY
				|| phase == xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAWN)
				&& !xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.isRaining();
	}

	/** The swamps steam at dawn: fog of the place, whatever the fronts say (ClimateManager.setLocalFog). */
	public boolean localFog(){
		return Dungeon.hero != null && altitude == 0
				&& xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase()
					== xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAWN
				&& biomeAtCell( Dungeon.hero.pos ) == WorldModel.Biome.SWAMP;
	}

	@Override
	public String tilesTex() {
		return openSky() ? Assets.Environment.TILES_OVERWORLD : Assets.Environment.TILES_CAVES;
	}
	@Override
	public String waterTex() {
		return openSky() ? Assets.Environment.WATER_SEWERS : Assets.Environment.WATER_CAVES;
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
			worldSeed = worldSeedOf( Dungeon.seed );
			if (pendingArrival){
				//a slice entered from another one: its first window opens on
				//the arrival, so it is not derived twice
				worldX = pendingWX - WIDTH/2;
				worldY = pendingWY - HEIGHT/2;
			} else {
				//first entry: centre the window on world origin
				worldX = -WIDTH/2;
				worldY = -HEIGHT/2;
			}
		}
		//first arrival: the town plaza, or wherever the way in leads
		if (pendingArrival){
			arrivalX = pendingWX;
			arrivalY = pendingWY;
		} else {
			arrivalX = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA );
			arrivalY = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		}
		seasonStamp = currentStamp();
		refreshSeasonShift();
		regenWindow();
		placeTransitions();
		//a market's village crowds its well from the first (Settler)
		if (altitude == 0) refreshMarkets( eventTurn() );
		populateTown();

		populate();

		return true;
	}

	//the prepared window for an origin, when the worker finished one for it at this season
	private WindowGenerator.Prepared takePrepared( int ox, int oy ){
		synchronized (pregenLock){
			Prep p = prepFor( ox, oy );
			if (p == null || p.result == null || p.result.season != GameCalendar.season()) return null;
			//the others were guesses at a window the hero did not walk into
			preps.clear();
			return p.result;
		}
	}

	/**
	 * Installs a prepared window: the pristine cache with its frozen and water-depth
	 * arrays, then map[] (copied, or re-derived from pristine plus the LIVE edit store when
	 * an edit landed after the preparation), the dressing data, the town-art layout and the
	 * flag maps. The outer ring is always solid: the whole engine assumes level borders
	 * are impassable, and edge water would make the water-stitcher read neighbours out of
	 * bounds. Nothing is put on screen here - see presentWindowArt.
	 */
	private void adoptPrepared( WindowGenerator.Prepared p ){
		pristine = p.base.terrain;
		veins = p.base.veins;
		adoptSites( p.base );
		frozen = p.base.frozen;
		waterDepth = p.base.waterDepth;
		link = p.base.link;
		field = p.base.field;
		dressSeason = p.season;
		windowShift = p.base.shift;
		int[][] dress = p.dress;
		if (p.diffsVersion == diffsVersion && p.scorchSig == scorchSig( worldX, worldY )){
			System.arraycopy( p.map, 0, map, 0, length() );
		} else {
			System.arraycopy( pristine, 0, map, 0, length() );
			WindowGenerator.overlayDiffs( map, diffs, worldX, worldY );
			WindowGenerator.overlayScorch( map, pristine, link, scorchFor( worldX, worldY ), diffs, worldX, worldY );
			dress = WindowGenerator.dress( worldSeed, worldX, worldY, map, p.base, p.season );
		}
		if (resealTombs()) dress = WindowGenerator.dress( worldSeed, worldX, worldY, map, p.base, p.season );
		noteScorch();
		//ORDER MATTERS: the dressing reads map[] for its edge blends, its deep
		//water shades and its fallen leaves, so the authoritative terrain has
		//to be in place first
		setDressData( dress );
		setBelowData( p.below );
		setTopData( p.base.top, p.base.topBlend, p.base.topCorner, p.base.earth );
		layoutTownArt();

		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		buildFlagMaps();
		cleanWalls();
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW flag maps", t );

		pendingVariance = p.variance;
		windowVersion++;
		hazards.windowChanged( p.base.firedamp );
	}

	//installs a generated window with the HOST's authoritative map (a network mirror), or
	//the pristine plus the player's recorded edits; presented at once, where it is drawn
	private void adoptWindow( WindowGenerator.Window w, int[] override, GameCalendar.Season season ){
		pristine = w.terrain;
		veins = w.veins;
		adoptSites( w );
		frozen = w.frozen;
		waterDepth = w.waterDepth;
		link = w.link;
		field = w.field;
		dressSeason = season;
		windowShift = w.shift;
		if (override != null){
			System.arraycopy( override, 0, map, 0, length() );
		} else {
			System.arraycopy( pristine, 0, map, 0, length() );
			WindowGenerator.overlayDiffs( map, diffs, worldX, worldY );
			resealTombs();
		}
		setDressData( WindowGenerator.dress( worldSeed, worldX, worldY, map, w, season ) );
		setBelowData( WindowGenerator.belowLayers( worldSeed, worldX, worldY, w ) );
		setTopData( w.top, w.topBlend, w.topCorner, w.earth );
		layoutTownArt();
		presentWindowArt();
		buildFlagMaps();
		cleanWalls();
		pendingVariance = WindowGenerator.variance( worldSeed, worldX, worldY );
		windowVersion++;
		hazards.windowChanged( w.firedamp );
	}

	/** re-derives the window for the current origin and puts its art on screen */
	private void regenWindow(){
		regenWindow( true );
	}

	//present == false leaves the art to the caller: a rebase presents it inside the render
	//thread's atomic block, together with the sprites and the camera
	private void regenWindow( boolean present ){
		long t0 = System.currentTimeMillis();
		WindowGenerator.Prepared p = takePrepared( worldX, worldY );
		boolean hit = p != null;
		if (p == null){
			p = WindowGenerator.prepare( worldSeed, altitude, worldX, worldY, GameCalendar.season(),
					diffs.isEmpty() ? null : diffs, diffsVersion, scorchFor( worldX, worldY ), scorchSig( worldX, worldY ) );
		}
		adoptPrepared( p );
		if (present) presentWindowArt();
		System.out.println( "[OW] window regen " + (System.currentTimeMillis()-t0) + "ms"
				+ (hit ? " (prepared)" : " (synchronous)") );
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
	public static OverworldLevel forNetwork( int altitude, long worldSeed, int wx, int wy, float seasonShift,
			GameCalendar.Season season, int[] map, int w, int h ){
		if (w != WIDTH || h != HEIGHT) return null;
		if (map == null || map.length != WIDTH * HEIGHT) return null;
		OverworldLevel l = new OverworldLevel( altitude );
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
	public WindowGenerator.Window stageNetworkWindow( long worldSeed, int wx, int wy, float seasonShift ){
		WorldModel.holdSeasonShift( seasonShift );
		return WindowGenerator.generate( worldSeed, altitude, wx, wy );
	}
	/**
	 * The cheap half: installs an already-generated window (see
	 * stageNetworkWindow) and the host's map. Render thread only, for the
	 * reasons applyNetworkWindow documents.
	 */
	public boolean adoptNetworkWindow( long worldSeed, int wx, int wy, float seasonShift,
			GameCalendar.Season season, WindowGenerator.Window staged, int[] map ){
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
			litNow = shiftRect( litNow, wx - worldX, wy - worldY );
			litDirty = shiftRect( litDirty, wx - worldX, wy - worldY );
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
		boolean changed = false;
		for (int y = 1; y < HEIGHT-1; y++){
			for (int x = 1; x < WIDTH-1; x++){
				int cell = x + y * width();
				int p = pristine[cell];
				if (map[cell] != p){
					long key = worldKey( worldX + x, worldY + y );
					//a fallen star's scorch, laid by the event itself and taken back by it: never the
					//player's edit (WorldEvents)
					if (map[cell] == Terrain.EMBERS && laidScorch.contains( key )) continue;
					//cosmetic degradation (burned or trampled vegetation) is NOT
					//recorded: nature reclaims it once the window moves away. this
					//also keeps the diff store from growing without bound - a big
					//wildfire would otherwise mint thousands of permanent diffs
					if (naturalDecay( p, map[cell] )){
						changed |= diffs.remove( key ) != null;
					} else {
						Integer old = diffs.put( key, map[cell] );
						changed |= old == null || old != map[cell];
					}
				} else if (!diffs.isEmpty()){
					//an edit that was later restored needs its stale diff dropped
					changed |= diffs.remove( worldKey( worldX + x, worldY + y ) ) != null;
				}
			}
		}
		if (changed) diffsVersion++;
	}

	//one full generator pass to rebuild the pristine cache (load path only)
	private void rebuildPristine(){
		WindowGenerator.Window w = WindowGenerator.generate( worldSeed, altitude, worldX, worldY );
		pristine = w.terrain;
		veins = w.veins;
		adoptSites( w );
		frozen = w.frozen;
		waterDepth = w.waterDepth;
		link = w.link;
		field = w.field;
		hazards.windowChanged( w.firedamp );
	}

	/**
	 * Fills the tile-variance table from WORLD coordinates instead of the
	 * per-level random fill: every world cell keeps the same alt-art variant
	 * forever, no matter where the window sits. Without this each rebase
	 * re-rolled the whole window's art - a full-screen shimmer. The table for
	 * the current window was prepared with it; computed here only when not.
	 */
	public void worldAnchorVariance(){
		byte[] table = xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.tileVariance;
		if (table == null || table.length != length()) return;
		if (pendingVariance == null || pendingVariance.length != length()){
			pendingVariance = WindowGenerator.variance( worldSeed, worldX, worldY );
		}
		System.arraycopy( pendingVariance, 0, table, 0, length() );
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
				//a place laid since it was left there (a prop, a tomb's wall) never buries it: it is
				//set down on the nearest open ground
				if (solid[cell]) cell = openGroundNear( cell, true );
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
		//thin ice under a hero's foot (HazardWatch: the host's, any hero's)
		if (ch instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero){
			hazards.stepped( (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) ch );
		}

		//a soak in a hot spring of the peaks (MountainSites): warms through, mends, keeps the cold off
		if (!network && altitude > 0 && ch instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero
				&& !ch.flying && hotSpring( ch.pos ) && water[ch.pos]){
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( ch,
					xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpringSoak.class ).steep();
		}

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
			int[] next = nextOrigin( x, y, MARGIN );
			if (next != null){
				//inside the margin: the rebase waits for its preparation while there is
				//room to walk, and goes synchronous past half the margin rather than let
				//the hero reach the border
				boolean urgent = x < MARGIN/2 || y < MARGIN/2
						|| x >= width() - MARGIN/2 || y >= height() - MARGIN/2;
				if (urgent || preparedFor( next[0], next[1] )){
					rebase();
				} else {
					schedulePrep( next[0], next[1] );
				}
			} else {
				//approaching an edge: prepare the window the hero is WALKING toward. an axis
				//counts only when he is heading for that edge, or the guess would be diagonal
				//almost every time and the real origin would miss it
				int lx = lastHeroCell >= 0 ? lastHeroCell % width() : x;
				int ly = lastHeroCell >= 0 ? lastHeroCell / width() : y;
				next = headingOrigin( x, y, Integer.signum( x - lx ), Integer.signum( y - ly ), MARGIN + PREP_BAND );
				if (next != null) schedulePrep( next[0], next[1] );
			}
			lastHeroCell = ch.pos;

			//the hour turned: cull the wildlife of the hour before (the
			//rebase pass does it too, but a hero who never reaches the
			//margin would keep the night's wolves all day)
			xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase phaseNow
					= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
			if (phaseNow != faunaCullPhase){
				faunaCullPhase = phaseNow;
				OverworldFauna.cull( this );
			}
			tickWorld( ch.pos );
			stepTicked = true;
			if (altitude > 0) mountainStep( ch.pos );
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
			if (altitude == 0
					&& WorldStructures.siteType( worldSeed, sx, sy ) == WorldStructures.Site.RUIN
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

	//the surface's own clock, at a step of the host's (or a solo game's) hero or at the start of
	//a turn after one he took none on (onHeroTurn)
	private void tickWorld( int heroPos ){
		//the day turned: the caravans strike camp and pitch again further
		//along their roads (a hero who never reaches the margin would
		//otherwise keep the same stall standing all week)
		if (altitude == 0
				&& (xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.weekday().ordinal() != caravanDay
					|| xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day() != caravanAbsDay)){
			placeCaravans();
		}
		//the road's life: walkers set out on the clock (a caravan's outlaws watch for the
		//heroes from the stall's own turn, Caravaneer.act)
		if (altitude == 0){
			long tTraffic = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
			long now = RoadTraffic.now();
			if (nextTraffic == Long.MIN_VALUE || now >= nextTraffic || now < nextTraffic - TRAFFIC_EVERY){
				nextTraffic = now + TRAFFIC_EVERY;
				placeTravellers( true );
				placePatrols( true );
				placeCaravanTrains( true );
				//a stall whose cart nobody saw come in goes up once its leg is over
				pitchCaravans( true );
			}
			xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW traffic", tTraffic );
		}
		//the world's timed events: a star coming down, a market come to town (WorldEvents)
		if (altitude == 0) tickEvents( heroPos );
		//the slices' places: found when first seen, and the rifts' heat and flares (CaveSites)
		if (altitude != 0) tickLayer( heroPos );
	}

	private void tickLayer( int heroPos ){
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		if (altitude < 0){
			for (CaveSites.Site s : caveSiteList){
				discover( s.key, s.type.ordinal(), s.standX, s.standY, new int[]{ s.x0, s.y0, s.x1, s.y1 }, CaveSites.foundLine( s ) );
			}
			CaveSites.tick( this, heroPos );
		} else {
			//the peaks' places are kept by their anchor's cell (MountainSites.Site.key is a hash)
			for (MountainSites.Site s : peakSites){
				discover( worldKey( s.wx, s.wy ), s.kind.ordinal(), s.seeX, s.seeY, new int[]{ s.x0, s.y0, s.x1, s.y1 },
						Messages.get( MountainSites.class, "found_" + s.kind.lower() ) );
			}
		}
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW layer tick", t );
	}

	//a step of the hero's has ticked the surface's clock since his last turn began, and the time
	//that turn began at: a turn he waits in for the player begins again, at the same time, when
	//the player acts - it is no new turn. neither is bundled: a load's first turn may tick once
	//more, which changes nothing
	private boolean stepTicked = false;
	private float turnBegan = -1f;

	/**
	 * DayNightCycle.onHeroTurn, as a turn of the host's (or a solo game's) hero begins. A turn he
	 * spent without a step - resting, searching, reading - ticked the surface's clock nowhere, so
	 * its tick runs now: a raid still goes live, a traveller or a patrol still sets out and a hunt
	 * still starts and ends round a co-op guest who walks up while his host stands, and a hero
	 * resting by a well still sees the day's caravan move on and the market come. A mirror ticks
	 * nothing: its host does.
	 */
	public static void onHeroTurn(){
		Level l = Dungeon.level;
		if (l instanceof OverworldLevel) ((OverworldLevel) l).hostTurn();
	}

	private void hostTurn(){
		if (network || Dungeon.hero == null) return;
		float now = xyz.gabriwar.warpedpixeldungeon.actors.Actor.now();
		if (now == turnBegan) return;
		turnBegan = now;
		if (!stepTicked) tickWorld( Dungeon.hero.pos );
		stepTicked = false;
		//fires on firedamp, healing cracks, refilling pockets (HazardWatch)
		hazards.worldTurn();
	}

	/**
	 * Re-centres the window on the hero: captures the player's edits, shifts the world
	 * origin, commits the prepared window (array copies), translates everything living in
	 * the window by the shift, computes the map layers' new visuals, and has the render
	 * thread apply all of it in one atomic block. A rebase is a pure coordinate
	 * re-labelling: the frame after it renders pixel-identically to the frame before.
	 * Every phase is a hot path the lag detector names.
	 */
	private void rebase(){

		long tRebase = System.currentTimeMillis();
		long tAll = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();

		int heroX = Dungeon.hero.pos % width(), heroY = Dungeon.hero.pos / width();
		//QUANTIZED shift: a fixed step per axis keeps the next origin
		//predictable, which is what lets the preparation land a hit
		int dx = heroX < MARGIN ? -SHIFT_Q : heroX >= WIDTH - MARGIN ? SHIFT_Q : 0;
		int dy = heroY < MARGIN ? -SHIFT_Q : heroY >= HEIGHT - MARGIN ? SHIFT_Q : 0;
		if (dx == 0 && dy == 0) return;

		//the render thread keeps drawing while this thread moves the origin, adopts the new map and
		//translates the exploration one after another: a fog frame painted in between mixes the
		//two windows, and the cells it painted wrong stay wrong once the fog's texture is blitted
		//(explored ground left black). the fog waits (FogOfWar.refresh) until the render block
		//below has moved it with everything else - and a paint already under way ends first
		synchronized (xyz.gabriwar.warpedpixeldungeon.tiles.FogOfWar.PAINTING){
			shifting = true;
		}
		try {
			shiftWindow( dx, dy, tRebase, tAll );
		} catch (RuntimeException | Error e){
			shifting = false;
			throw e;
		}
	}

	private void shiftWindow( int dx, int dy, long tRebase, long tAll ){
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		captureDiffs();
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW captureDiffs", t );

		worldX += dx;
		worldY += dy;
		xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature.shift(this, dx, dy);

		//blobs SLIDE with the window BEFORE the flag maps are rebuilt, so
		//Web-style onBuildFlagMaps overrides stamp solid/flamable at the
		//post-translate cells (stamping first left phantom solids behind)
		t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		for (xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob b
				: blobs.values().toArray( new xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob[0] )){
			b.translate( dx, dy, width(), height() );
		}
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW blobs", t );

		//the prepared window, or a synchronous derivation when the hero outran the worker.
		//the art is presented later, in the render block
		t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		regenWindow( false );
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW adopt window", t );

		//tile variance is WORLD-anchored, and the strips computed below read it
		//for the new origin - or every cell re-rolls its alt art at each rebase
		worldAnchorVariance();

		//translate everything living in the window by (-dx, -dy)
		t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		int shift = -dx - dy * width();

		Dungeon.hero.pos += shift;

		for (Mob m : mobs.toArray( new Mob[0] )){
			int nx = m.pos % width() - dx, ny = m.pos / width() - dy;
			if (nx <= 0 || ny <= 0 || nx >= width()-1 || ny >= height()-1){
				//walked out of the window: parked at its world position (the
				//pre-shift origin, since m.pos is still in pre-shift coords)
				//and put back when the window slides over it again. never
				//destroy()ed - that would count as a kill, and a shopkeeper's
				//destroy() takes his shelf down with him
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
		//a cave-in about to come down marks raw cells too: they follow their ground
		hazards.rebased( dx, dy, netHeroes );
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW actors and heaps", t );

		//the town's doorways are FIXED world landmarks: transitions exist
		//exactly when their world cells sit inside the window
		t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		placeTransitions();
		populateTown();
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW transitions and town", t );

		//slide the exploration state along with the window
		t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		translateExploration( dx, dy );
		//the ground the caves' lights showed is kept by window rect: it moves with the rest, or the
		//next observe repaints a rect 32 cells off and leaves the lit ground drawn as in sight
		litNow = shiftRect( litNow, dx, dy );
		litDirty = shiftRect( litDirty, dx, dy );
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW exploration", t );

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

		t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		populate();
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW populate", t );

		//the map layers' visuals as they will be after the shift: the overlap moves by
		//arraycopy, only the exposed strips run the tile pipeline. computed here, on the
		//actor thread, against the new window; the render thread only swaps them in
		final xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.MapShift mapShift = xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.prepareMapShift( dx, dy );

		//THE INVISIBLE STREAM: a rebase is a pure coordinate re-labelling.
		//every visual - sprites WITH their in-flight motion tweens, live
		//particles, damage numbers, projectiles - the map layers, the art and
		//the camera all slide by the same pixel delta, so the frame after the
		//rebase renders pixel-identically to the frame before. the whole
		//visual application runs as ONE atomic block on the render thread (the
		//actor thread waits up to a frame), so no half-shifted frame can ever
		//be drawn - that tear was the one-frame "teleport to the edge" flash
		final float vsx = -dx * xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.SIZE;
		final float vsy = -dy * xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.SIZE;
		final java.util.concurrent.CountDownLatch applied = new java.util.concurrent.CountDownLatch( 1 );
		final long enqueueFrame = xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.frameId;
		final int fdx = dx, fdy = dy;
		com.watabou.noosa.Game.runOnRenderThread( () -> {
			long tApply = System.nanoTime();
			long tRender = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
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
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.shiftWorldVisuals( vsx, vsy );
				//the marks of rocks about to fall live in a group of their own, keyed by cell
				xyz.gabriwar.warpedpixeldungeon.effects.TargetedCell.shiftAll( fdx, fdy, width(), height() );
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.applyMapShift( mapShift );
				//the town art and the dressing move in the same block as everything else
				presentWindowArt( fdx, fdy );
				Dungeon.observe();
				//the camera slides too, MATRIX INCLUDED: the matrix is otherwise rebuilt in
				//update(), which runs after the next draw - offsetting only the scroll here left
				//that draw with the old camera and every shifted sprite and tile jumped 32 cells
				//for one frame, the "blink" at every crossing. snapTo would recentre and eat the
				//deadzone offset, which read as a visible jerk
				com.watabou.noosa.Camera.main.shiftInstant( vsx, vsy );
			} finally {
				//the fog moved with the rest (applyMapShift) and its queue with it: it may paint again
				shifting = false;
				xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW render apply", tRender );
				System.out.println( "[OW] visual apply " + ((System.nanoTime()-tApply)/1000000) + "ms"
						+ " (enq f" + enqueueFrame + " app f"
						+ xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.frameId + ")" );
				applied.countDown();
			}
		} );
		t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		try {
			applied.await( 150, java.util.concurrent.TimeUnit.MILLISECONDS );
		} catch (InterruptedException ignored){}
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW apply wait", t );

		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW rebase total (actor)", tAll );
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

	static long structHash( long a, long b ){
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

	//the hash a settlement's family is rolled from: house h is layout entry 1 + 2h
	//(populateSettlement). VillageRoutine reads the same number for their trades
	static long familyHash( int sx, int sy, int house ){
		return structHash( sx * 131 + 1 + house * 2, sy );
	}

	/** How far the nearest hero is from a cell (Chebyshev), or Integer.MAX_VALUE when none is
	 *  on the level: the host's own hero and, on a host, every claimed remote one still in
	 *  play. What "could a player see this?" is answered by - heroFOV is all-true under the
	 *  debug no-fog, a step stale inside occupyCell, and remote heroes have none on the host */
	public static int heroDistance( Level level, int cell ){
		int best = remoteHeroDistance( level, cell );
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h = Dungeon.hero;
		if (h != null && Dungeon.level == level && h.isAlive() && h.pos >= 0 && h.pos < level.length()){
			best = Math.min( best, level.distance( cell, h.pos ) );
		}
		return best;
	}

	/** Every hero in play on a level, the ones heroDistance counts: the host's own and, on a
	 *  host, every claimed remote one still in play. */
	public static ArrayList<xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero> heroesOn( Level level ){
		ArrayList<xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero> out = new ArrayList<>();
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h = Dungeon.hero;
		if (h != null && Dungeon.level == level && h.isAlive() && h.pos >= 0 && h.pos < level.length()) out.add( h );
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()){
			for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh
					: xyz.gabriwar.warpedpixeldungeon.net.NetManager.getNetHeroes()){
				if (nh.isAlive() && !nh.atExit && nh.pos >= 0 && nh.pos < level.length()) out.add( nh );
			}
		}
		return out;
	}

	/** ...the other players' heroes alone (on a host; Integer.MAX_VALUE otherwise): the ones
	 *  whose field of view the host does not hold. */
	public static int remoteHeroDistance( Level level, int cell ){
		int best = Integer.MAX_VALUE;
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()){
			for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh
					: xyz.gabriwar.warpedpixeldungeon.net.NetManager.getNetHeroes()){
				if (!nh.isAlive() || nh.atExit || nh.pos < 0 || nh.pos >= level.length()) continue;
				best = Math.min( best, level.distance( cell, nh.pos ) );
			}
		}
		return best;
	}

	//does one of this settlement's vendors keep shop in the house centred here? (live, or
	//parked by the window; two cells of slack, as an unparked keeper may be set down that far
	//from his post - the next house is six away): an older save's villager working out its
	//home skips shop houses
	boolean shopAt( long sectorKey, int wx, int wy ){
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper
					&& homeSectorOf( m ) == sectorKey
					&& Math.abs( worldX + m.pos % width() - wx ) <= 2
					&& Math.abs( worldY + m.pos / width() - wy ) <= 2) return true;
		}
		for (HashMap.Entry<Long, Mob> e : parkedMobs.entrySet()){
			if (e.getValue() instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper
					&& homeSectorOf( e.getValue() ) == sectorKey
					&& Math.abs( (int)(e.getKey() & 0xFFFFFFFFL) - wx ) <= 2
					&& Math.abs( (int)(e.getKey() >> 32) - wy ) <= 2) return true;
		}
		return false;
	}

	//is any mob of this class near the cell? parked mobs count: a site whose
	//guardians sit in the store (the window slid back before they were put
	//down, or their cells were taken) must not grow a second set
	private boolean mobNear( Class<?> cls, int cell, int radius ){
		for (Mob m : mobs){
			if (sentOnEvent( m )) continue;
			if (cls.isInstance( m )
					&& Math.abs( m.pos % width() - cell % width() ) <= radius
					&& Math.abs( m.pos / width() - cell / width() ) <= radius){
				return true;
			}
		}
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		for (HashMap.Entry<Long, Mob> e : parkedMobs.entrySet()){
			if (sentOnEvent( e.getValue() )) continue;
			if (cls.isInstance( e.getValue() )
					&& Math.abs( (int)(e.getKey() & 0xFFFFFFFFL) - wx ) <= radius
					&& Math.abs( (int)(e.getKey() >> 32) - wy ) <= radius){
				return true;
			}
		}
		return false;
	}

	//an outlaw sent on a world event (a stall's ambush) is no camp's: a camp near the stall
	//still counts only its own
	private static boolean sentOnEvent( Mob m ){
		return m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit
				&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit) m).sentOnEvent();
	}

	//the mobs waiting in the parked store (RaidEvent counts a raid's band there too)
	java.util.Collection<Mob> parked(){
		return parkedMobs.values();
	}

	//the raiders of past days' raids still in the parked store: their raid is over, and one that
	//came back would only make off again (Raider.act)
	void forgetStaleRaiders( int day ){
		java.util.Iterator<HashMap.Entry<Long, Mob>> it = parkedMobs.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (!(e.getValue() instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider)
					|| ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider) e.getValue()).day == day) continue;
			it.remove();
			parkedAt.remove( e.getKey() );
		}
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

	boolean addMob( Mob m, int cell ){
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
		if (altitude == 0) populateSites(); else populateLayerSites();
		populateFauna();
	}

	//one entry for the slices: cave-sites' places below, mountain-sites' above (each idempotent: once-keys, censuses)
	void populateLayerSites(){ if (altitude < 0) populateCaveSites(); else populateMountainSites(); }

	//the caves' places wholly in the window (their reach and one round it inside the window's
	//populated band, so a half-seen place never burns its once-keys) are peopled (CaveSites.populate)
	private void populateCaveSites(){
		if (network) return;
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		sweepDue();
		for (CaveSites.Site s : caveSiteList){
			if (s.x0 - 1 - worldX < 2 || s.y0 - 1 - worldY < 2
					|| s.x1 + 1 - worldX > WIDTH - 3 || s.y1 + 1 - worldY > HEIGHT - 3) continue;
			CaveSites.populate( this, s );
		}
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "CaveSites.populate", t );
	}

	/** The living eagles of an eyrie, in the window and parked. */
	public int eyrieGuards( long eyrieKey ){
		int n = 0;
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle && m.isAlive()
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle) m).eyrieKey == eyrieKey) n++;
		}
		for (Mob m : parkedMobs.values()){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle && m.isAlive()
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle) m).eyrieKey == eyrieKey) n++;
		}
		return n;
	}

	//is a hermit of this hut, or the cairn of this summit, in the window or parked?
	private boolean siteFolk( long key ){
		for (Mob m : mobs){
			if (siteKeyOf( m ) == key) return true;
		}
		for (Mob m : parkedMobs.values()){
			if (siteKeyOf( m ) == key) return true;
		}
		return false;
	}

	private static long siteKeyOf( Mob m ){
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Hermit)
			return ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Hermit) m).siteKey;
		if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn)
			return ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn) m).siteKey;
		return Long.MIN_VALUE;
	}

	//the places on the mountains (MountainSites): the hermit at home, the eyrie's pair and its hoard,
	//the summit's cairn, the frozen climber's remains, the watchtower's lair, the waystation's
	//bedroll. a site half outside the window waits until it is whole in it, so a once-key never
	//burns on a hoard that could not be laid
	void populateMountainSites(){
		if (network || altitude <= 0) return;
		long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		sweepDue();
		for (MountainSites.Site s : peakSites){
			if (s.x0 - 1 - worldX < 2 || s.y0 - 1 - worldY < 2
					|| s.x1 + 1 - worldX > WIDTH - 3 || s.y1 + 1 - worldY > HEIGHT - 3) continue;
			int c = localCell( s.wx, s.wy );
			switch (s.kind){
				case HERMIT:
					if (!sitesCleared.contains( s.key ) && !siteFolk( s.key )){
						xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Hermit h
								= xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Hermit.of( s.key, s.wx, s.wy, altitude );
						h.openShop();
						addMob( h, c );
					}
					break;
				case EYRIE: {
					//the pair, each once ever and claimed only once it is down (a wolf on the nest
					//leaves it for the next populate): kept forever, the eyrie cleared when both are dead
					if (!sitesCleared.contains( s.key )){
						int[] perch = { c, localCell( s.wx - MountainSites.DX[s.dir], s.wy - MountainSites.DY[s.dir] ) };
						for (int i = 0; i < 2; i++){
							long once = structHash( s.key ^ 0xEA61EL, i );
							if (hoardLaid.contains( once )) continue;
							int at = freeSpotWithin( perch[i], 2 );
							if (at != -1 && addMob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle.of( s.key, s.wx, s.wy ), at )){
								claimHoard( once );
							}
						}
					}
					//what they keep in the nest, once: the gold they hoarded, in one nest of three a
					//bright stone they carried up from the shallow caves (Ores), and on top the eggs
					if (claimHoard( s.key )){
						drop( new xyz.gabriwar.warpedpixeldungeon.items.Gold(
								com.watabou.utils.Random.IntRange( 30 + 15 * altitude, 60 + 25 * altitude ) ), c );
						if (Math.floorMod( structHash( s.key, 3 ), 3L ) == 0){
							drop( com.watabou.utils.Reflection.newInstance(
									Ores.rollGem( -1, (int) Math.floorMod( structHash( s.key, 4 ), 100L ) ).item ), c );
						}
						drop( new xyz.gabriwar.warpedpixeldungeon.items.food.EagleEgg()
								.quantity( 2 + (int) (structHash( s.key, 1 ) & 1) ), c );
					}
					break;
				}
				case CAIRN:
					if (!siteFolk( s.key )){
						xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn cairn
								= xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn.of( s );
						//a stone already added stays added, whatever became of the pile
						cairn.topped = hoardLaid.contains( s.key );
						addMob( cairn, c );
					}
					break;
				case CLIMBER:
					//the climber's last gear, once: warmth that came too late, food, his purse, now
					//and then his pick
					if (claimHoard( s.key )){
						drop( new xyz.gabriwar.warpedpixeldungeon.items.Gold(
								com.watabou.utils.Random.IntRange( 20, 50 + 10 * altitude ) ), c );
						drop( new xyz.gabriwar.warpedpixeldungeon.items.food.Food(), c );
						drop( new xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfChilli()
								.quantity( 1 + (int) (structHash( s.key, 5 ) & 1) ), c );
						if (Math.floorMod( structHash( s.key, 7 ), 4L ) == 0){
							drop( new xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe(), c );
						}
						drop( new xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfWarmth(), c ).type = Heap.Type.SKELETON;
					}
					break;
				case TOWER: {
					//something lairs in the old tower some weeks: at most one lot a week, and never
					//while the last lot is still about
					long lairKey = structHash( s.key, 0x7011L );
					int lair = (int) Math.floorMod( lairKey, 3L );
					if (lair == 2) break;
					if (mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti.class, c, 11 )
							|| mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit.class, c, 11 )) break;
					if (!dueOnce( lairKey, 7 )) break;
					if (lair == 0){
						addMob( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti(), c );
					} else {
						addMob( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit(), c - 1 );
						addMob( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit(), c + 1 );
					}
					break;
				}
				case PASS: {
					int[] at = MountainSites.bedrollCell( s );
					int b = localCell( at[0], at[1] );
					if (b != -1 && !mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll.class, b, 1 )){
						xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll bed
								= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll();
						bed.shelter = true;
						addMob( bed, b );
					}
					break;
				}
				default:
			}
		}
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW mountain populate", t );
	}

	//a host hero's step on a peak: the land shows from a watchtower's top, every time; a frozen
	//climber's journal is read once, standing beside him
	private void mountainStep( int pos ){
		int wx = worldX + pos % width(), wy = worldY + pos / width();
		for (MountainSites.Site s : peakSites){
			if (s.kind == MountainSites.Kind.TOWER && s.wx == wx && s.wy == wy){
				revealAround( pos, MountainSites.TOWER_VIEW );
				GLog.i( Messages.get( MountainSites.class, "tower_view" ) );
			} else if (s.kind == MountainSites.Kind.CLIMBER
					&& Math.max( Math.abs( wx - s.wx ), Math.abs( wy - s.wy ) ) <= 1
					&& claimHoard( s.key ^ 0x10A2E1L )){
				final String text = "_" + Messages.get( MountainSites.class, "journal_title" ) + "_\n\n"
						+ Messages.get( MountainSites.class, "journal_" + Math.floorMod( structHash( s.key, 11 ), 4L ) );
				com.watabou.noosa.Game.runOnRenderThread( () -> xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.show(
						new xyz.gabriwar.warpedpixeldungeon.windows.WndMessage( text ) ) );
			}
		}
	}

	/** The land within radius of a cell shows as if mapped (a summit's cairn, a watchtower's top):
	 *  the actor thread, as the hero's own sight. */
	public void revealAround( int cell, int radius ){
		int w = width(), cx = cell % w, cy = cell / w;
		for (int dy = -radius; dy <= radius; dy++){
			for (int dx = -radius; dx <= radius; dx++){
				if (dx * dx + dy * dy > radius * radius) continue;
				int x = cx + dx, y = cy + dy;
				if (x <= 0 || y <= 0 || x >= w - 1 || y >= height() - 1) continue;
				int c = x + y * w;
				if (discoverable[c]) mapped[c] = true;
			}
		}
		if (liveScene()) xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog( cell, radius );
	}

	//the sites' folk, guardians and hoards, then the road's fishermen, caravans, travellers
	//and town watches
	private void populateSites(){
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
		placeTravellers( false );
		placePatrols( false );
		placeCaravanTrains( false );
		//the world's events: what ended while the window was away goes, what is on is laid down
		//(no visuals: this runs inside a rebase, whose slide would carry them off)
		expireEvents( eventTurn(), false );
		placeEvents( false );
	}

	//the human and gnoll settlements of the window alone (no bandit camps, ruins, lairs,
	//hoards, caravans or wildlife): what a far arrival finds peopled at once
	private void populateSettlements(){
		for (int sy = sector0Y(); sy <= sector1Y(); sy++){
			for (int sx = sector0X(); sx <= sector1X(); sx++){
				if (WorldStructures.siteType( worldSeed, sx, sy ) != WorldStructures.Site.VILLAGE
						|| WorldStructures.faction( worldSeed, sx, sy ) == WorldStructures.Faction.BANDIT) continue;
				int cwx = WorldStructures.siteX( worldSeed, sx, sy ) - worldX;
				int cwy = WorldStructures.siteY( worldSeed, sx, sy ) - worldY;
				if (cwx < 2 || cwy < 2 || cwx >= WIDTH-2 || cwy >= HEIGHT-2) continue;
				populateSettlement( sx, sy, WorldStructures.sectorOf( sx, sy ), cwx + cwy * width() );
			}
		}
	}

	//the wildlife of the slice, topped up away from the hero
	private void populateFauna(){
		//the hour's cull first: the night's wolves are gone by day, the
		//day's bunnies after dusk (OverworldFauna.cull)
		OverworldFauna.cull( this );
		//fauna by biome x hour x weather (OverworldFauna's table), topped up
		//away from the hero, out of the town and the settlements' streets;
		//a pack counts per member. a slice's places keep their guards (a cavern's
		//wisps, a tomb's dead, a tower's yeti) in their berths: those are no wildlife
		int fauna = 0;
		for (Mob m : mobs){
			if (OverworldFauna.isFauna( m ) && !inSiteBerth( m.pos )) fauna++;
		}
		xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase phase
				= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
		//nothing is set down within 16 cells of the hero - or, on a slice's first build, of where
		//he is about to arrive
		boolean live = Dungeon.hero != null && Dungeon.level == this;
		int ref = live ? Dungeon.hero.pos : localCell( arrivalX, arrivalY );
		int tries = 60;
		while (fauna < 12 && tries-- > 0){
			int cell = com.watabou.utils.Random.Int( length() );
			if (!passable[cell] || inTown( cell ) || Actor.findChar( cell ) != null) continue;
			//the ground it is (OverworldFauna.habitat): none on the surface's water, or on a slice's
			//water that is no deep pool's shelf
			int hab = OverworldFauna.habitat( this, cell );
			if (hab == 0) continue;
			int heroDist = Integer.MAX_VALUE;
			if (ref != -1){
				heroDist = Math.max( Math.abs( cell % width() - ref % width() ),
						Math.abs( cell / width() - ref / width() ) );
				if (heroDist < 16) continue;
			}
			//...nor within 16 of a co-op guest's hero on the slice: the abyss's and the peaks'
			//beasts are no surprise to set down at anyone's side
			if (live && remoteHeroDistance( this, cell ) < 16) continue;
			if (altitude == 0 && OverworldFauna.nearSettlement( worldSeed,
					worldX + cell % width(), worldY + cell / width() )) continue;
			//the slices' places keep their own: no wildlife in a camp, a grotto, a hut or an eyrie
			if (inSiteBerth( cell )) continue;
			ArrayList<Mob> beasts = OverworldFauna.roll( altitude, biomeAtCell( cell ), phase, hab );
			int placed = 0;
			for (int i = 0; i < beasts.size(); i++){
				int at = cell + (i == 0 ? 0 : com.watabou.utils.PathFinder.NEIGHBOURS8[i % 8]);
				//a slice's pack member only where its kind lives: a fish never beside the water, a wisp by its seam
				if (i > 0 && altitude != 0 && (!OverworldFauna.livesAt( this, beasts.get( i ), at ) || inSiteBerth( at ))) continue;
				if (addMob( beasts.get( i ), at )) placed++;
			}
			fauna += placed;
			if (live && placed >= 2 && heroDist <= 25 && OverworldFauna.isPack( beasts )) OverworldFauna.howl();
		}
	}

	/** Inside the reach of a place of the slice (CaveSites, MountainSites): no hazard of the slices
	 *  starts there (HazardWatch). The window's own sites: any thread, never resolves. */
	public boolean inSiteReach( int cell ){
		if (altitude == 0 || cell < 0 || cell >= length()) return false;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		for (CaveSites.Site s : caveSiteList){
			if (s.holds( wx, wy )) return true;
		}
		for (MountainSites.Site s : peakSites){
			if (MountainSites.inReach( s, wx, wy )) return true;
		}
		return false;
	}

	//inside the berth of a place of the slice (its reach and four round it, a rift's reach alone:
	//CaveSites, MountainSites), where no wildlife is set down. the window's own sites: never resolves
	private boolean inSiteBerth( int cell ){
		if (altitude == 0 || cell < 0 || cell >= length()) return false;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		if (altitude < 0) return CaveSites.inBerth( layerSites, wx, wy );
		for (MountainSites.Site s : peakSites){
			if (MountainSites.inBerth( s, wx, wy )) return true;
		}
		return false;
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
		int vendors = VillageRoutine.vendorHouses( houses );
		int guards  = houses >= 18 ? 4 : houses >= 9 ? 2 : 0;
		//families spawn for the most central houses only: big cities keep
		//most houses quiet, capping the mob count per settlement
		int populatedHouses = WorldStructures.populatedHouses( houses );

		long sh = structHash( sx, sy );
		int vendorBase = (int)Math.floorMod( sh, xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper.SPECIALITIES );

		int vendorsPlaced = 0;
		for (int i = 1; i + 1 < layout.length && (i-1)/2 < populatedHouses; i += 2){
			int houseCell = center + layout[i] + layout[i+1] * width();
			long fh = familyHash( sx, sy, (i-1)/2 );

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
			int hx = WorldStructures.siteX( worldSeed, sx, sy ) + layout[i];
			int hy = WorldStructures.siteY( worldSeed, sx, sy ) + layout[i+1];
			for (int mIdx = 0; mIdx < members; mIdx++){
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler folk;
				if (fac == WorldStructures.Faction.HUMAN){
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager v
							= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager();
					v.look = (int)Math.floorMod( fh >> 8, 3 );
					v.tint = (int)Math.floorMod( fh >> 16, 8 );
					v.homeSector = key;
					folk = v;
				} else {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager g
							= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager();
					g.tint = (int)Math.floorMod( fh >> 16, 8 );
					g.homeSector = key;
					folk = g;
				}
				//the family's house and each one's trade, for the day's round (VillageRoutine),
				//and straight to wherever the day has got to
				folk.house = (i-1)/2;
				folk.member = mIdx;
				folk.role = VillageRoutine.roleOf( fh, mIdx, fac == WorldStructures.Faction.GNOLL ).ordinal();
				folk.homeX = hx;
				folk.homeY = hy;
				int at = VillageRoutine.arrivalCell( this, folk, hx, hy );
				addMob( folk, at != -1 ? at
						: houseCell + (mIdx == 0 ? 0 : com.watabou.utils.PathFinder.NEIGHBOURS8[mIdx % 8] * 3) );
			}
		}

		//guards flank the well by day, more of them in the big places, and keep its gates by night
		if (fac == WorldStructures.Faction.HUMAN){
			for (int g = 0; g < guards; g++){
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard guard
						= xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard.random( key );
				guard.post = g;
				int at = VillageRoutine.arrivalCell( this, guard,
						WorldStructures.siteX( worldSeed, sx, sy ), WorldStructures.siteY( worldSeed, sx, sy ) );
				addMob( guard, at != -1 ? at : center + com.watabou.utils.PathFinder.NEIGHBOURS8[(g * 2) % 8] * 2 );
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
	 * around HIM go with him, and nobody else's shelf is touched. Today's goes
	 * up once its cart has made the morning's leg (RoadTraffic.caravanLeg,
	 * CaravanTrain): pitchCaravans.
	 */
	private void placeCaravans(){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.weekday().ordinal();
		int absToday = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		caravanDay = today;
		caravanAbsDay = absToday;

		for (Mob m : mobs.toArray( new Mob[0] )){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer
					&& stale( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m, today, absToday )){
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c
						= (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m;
				//the day is out with the outlaws still at the stall: they had the goods first
				if (c.ambush == xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.AMBUSH_BESIEGED){
					CaravanAmbush.outlastedTheDay( this, c );
				}
				strikeCaravan( c );
			}
		}
		java.util.Iterator<HashMap.Entry<Long, Mob>> it = parkedMobs.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (!(e.getValue() instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer)) continue;
			if (!stale( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) e.getValue(), today, absToday )) continue;
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
		//the outlaws of a stall that is gone (struck, or lost some other way) go with it
		java.util.HashSet<Long> standing = new java.util.HashSet<>();
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer){
				standing.add( ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m).stall );
			}
		}
		for (Mob m : parkedMobs.values()){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer){
				standing.add( ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m).stall );
			}
		}
		CaravanAmbush.disbandOrphans( this, standing, parkedMobs, parkedAt );

		pitchCaravans( false );
	}

	//today's stalls on the window's roads whose cart is in: each at the road's caravan spot,
	//never while its cart is still on the way (it pitches its own on arrival, caravanArrives) -
	//and the one whose cart nobody saw come in goes up once the clock says it is there. With
	//`fresh` false (a new day, a new window) any road with no stall near its spot pitches again,
	//as a keeper chased off always has; with `fresh` (the traffic's looks) only the ones not up
	//yet today, so a stall never springs back the moment its keeper is chased off
	private void pitchCaravans( boolean fresh ){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.weekday().ordinal();
		int absToday = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		int dayOfSeason = xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.dayOfSeason();
		long now = RoadTraffic.now();
		int day = RoadTraffic.day( now ), tod = RoadTraffic.turnOfDay( now ), w0 = RoadTraffic.setOut();
		java.util.HashSet<Long> rolling = new java.util.HashSet<>();
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain){
				rolling.add( ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain) m).road );
			}
		}
		java.util.HashSet<Long> up = caravansPitched();
		for (int[] r : RoadTraffic.roads( worldSeed, sector0X(), sector0Y(), sector1X(), sector1Y() )){
			RoadTraffic.Trip leg = RoadTraffic.caravanLeg( worldSeed, r[0], r[1], r[2], r[3], today, dayOfSeason, day, w0 );
			if (leg == null) continue;
			long road = RoadTraffic.roadHash( worldSeed, r[0], r[1], r[2], r[3] );
			if (fresh && up.contains( road )) continue;
			int cell = localCell( Math.round( leg.route[2] ), Math.round( leg.route[3] ) );
			if (cell == -1) continue;
			//its stall stands already (pitched by its cart, or brought back by a load): no cart
			//sets out for it either
			if (mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.class, cell, 10 )){
				up.add( road );
				continue;
			}
			if (tod < leg.arrive || rolling.contains( road )) continue;
			int spot = roadSpotNear( cell );
			if (spot == -1) continue;
			pitchCaravan( r[0], r[1], r[2], r[3], spot, today, absToday );
		}
	}

	//the stall of the road (sx,sy)-(nx,ny) put up on a road cell for today
	private boolean pitchCaravan( int sx, int sy, int nx, int ny, int cell, int today, int absToday ){
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c
				= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer();
		c.day = today;
		c.pitched = absToday;
		c.road = RoadTraffic.roadHash( worldSeed, sx, sy, nx, ny );
		//one stall in six has outlaws lying in wait today (CaravanAmbush) - once a day to a
		//road, however often its keeper is chased off and pitches again
		c.ambush = !ambushesFought().contains( c.road ) && RoadTraffic.ambushed( worldSeed, sx, sy, nx, ny, absToday )
				? xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.AMBUSH_PENDING
				: xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.AMBUSH_NONE;
		if (!addMob( c, cell )) return false;
		c.stall = structHash( worldKey( worldX + c.pos % width(), worldY + c.pos / width() ), absToday );
		c.openShop();
		caravansPitched().add( c.road );
		return true;
	}

	//the roads (RoadTraffic.roadHash) whose stall has gone up today, and the day they are for:
	//a look of the traffic pitches only the others. not bundled - a load re-pitches the window's
	//through placeCaravans, which finds the standing stalls and fills it again
	private final java.util.HashSet<Long> caravansUp = new java.util.HashSet<>();
	private int caravansUpDay = Integer.MIN_VALUE;

	private java.util.HashSet<Long> caravansPitched(){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		if (caravansUpDay != today){
			caravansUp.clear();
			caravansUpDay = today;
		}
		return caravansUp;
	}

	/** A caravan's cart has drawn up (CaravanTrain, already gone from the level, last standing on
	 *  `at`): the road's stall goes up where the cart stopped when that is road by its spot, else
	 *  on the road by the spot - unless the stall stands there already. A spot past the window's
	 *  edge waits for the window to get there (placeCaravans). */
	public void caravanArrives( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain t, int at ){
		if (network || altitude != 0) return;
		int spot = localCell( Math.round( t.route[2] ), Math.round( t.route[3] ) );
		if (spot == -1) return;
		int cell = at >= 0 && at < length() && map[at] == Terrain.DIRT_PATH && passable[at]
				&& Actor.findChar( at ) == null && distance( at, spot ) <= 4 ? at : roadSpotNear( spot );
		if (cell == -1) return;
		if (mobNear( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.class, cell, 10 )){
			caravansPitched().add( t.road );
			return;
		}
		if (pitchCaravan( t.fromSx, t.fromSy, t.toSx, t.toSy, cell,
				xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.weekday().ordinal(),
				xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day() )
				&& Dungeon.level == this && com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene
				&& xyz.gabriwar.warpedpixeldungeon.net.NetManager.anyHeroSees( cell )){
			//the cart unloaded: the stall goes up in a puff of the road's dust
			xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( cell ).burst(
					xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.DUST ), 6 );
		}
	}

	//a stall belongs to the weekday it was pitched on - and, once it knows it, to that very
	//day (one from an older save keeps its weekday rule alone)
	private static boolean stale( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c, int weekday, int day ){
		return c.day != weekday || (c.pitched != -1 && c.pitched != day);
	}

	//the weekday the caravans standing in the window were pitched for; not
	//bundled - the first step after a load re-checks it against the calendar
	private int caravanDay = -1;
	//...and the day (WorldClock.day): a week's sleep brings the weekday round again
	private int caravanAbsDay = Integer.MIN_VALUE;

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
	//destroy() - a Shopkeeper's destroy() leaves all but one ware of a bigger
	//heap lying about, free for the taking
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

	// ------------------------------------------------- the road's own life

	/** Off the level without a death: no experience, no loot, no tally, no Bestiary, no shelf
	 *  swept (never die() or destroy()). Actor.remove takes its buffs off with it. Works on any
	 *  level. */
	public static void vanish( Level level, Mob m ){
		level.mobs.remove( m );
		Actor.remove( m );
		if (m.sprite != null) m.sprite.killAndErase();
	}

	//which window cells are road to the road's walkers; rebuilt when the window changes
	private boolean[] roadMask;
	private int roadMaskVersion = -1;

	/** Road to the road's walkers: paths, bridges and doorways (a village's footpaths join every
	 *  door to its well, and the road runs to the well). The terrain alone - whether a cell can be
	 *  stood on just now is the walker's to check. */
	public boolean[] roadMask(){
		if (roadMask == null || roadMask.length != length() || roadMaskVersion != windowVersion){
			boolean[] m = new boolean[length()];
			for (int i = 0; i < m.length; i++){
				int t = map[i];
				m[i] = t == Terrain.DIRT_PATH || t == Terrain.BRIDGE || t == Terrain.DOOR || t == Terrain.OPEN_DOOR;
			}
			roadMask = m;
			roadMaskVersion = windowVersion;
		}
		return roadMask;
	}

	/** Is this world point inside the window's interior, three cells clear of its ring? */
	public boolean inWindow( float wx, float wy ){
		int x = Math.round( wx ) - worldX, y = Math.round( wy ) - worldY;
		return x >= 3 && y >= 3 && x < WIDTH - 3 && y < HEIGHT - 3;
	}

	/** The window cell a road walker stands on for a world point (clamped into the interior):
	 *  the nearest open road cell within six (a road wobbles that far off its straight line),
	 *  else the nearest ground a settler would walk within two (ice the road does not pave), else
	 *  -1. Who stands there is not its business. */
	public int trafficCell( float wx, float wy ){
		float px = Math.max( 3, Math.min( WIDTH - 4, wx - worldX ) ), py = Math.max( 3, Math.min( HEIGHT - 4, wy - worldY ) );
		int x = Math.round( px ), y = Math.round( py );
		boolean[] road = roadMask();
		int best = -1;
		float bestD = Float.MAX_VALUE;
		for (int dy = -6; dy <= 6; dy++){
			for (int dx = -6; dx <= 6; dx++){
				int cx = x + dx, cy = y + dy;
				if (cx < 2 || cy < 2 || cx >= WIDTH - 2 || cy >= HEIGHT - 2) continue;
				int c = cx + cy * width();
				if (!road[c] || !passable[c] || avoid[c]) continue;
				float d = (cx - px) * (cx - px) + (cy - py) * (cy - py);
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		if (best != -1) return best;
		for (int r = 0; r <= 2; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = (x + dx) + (y + dy) * width();
					if (VillageRoutine.walkable( this, c )) return c;
				}
			}
		}
		return -1;
	}

	private static final int MAX_TRAVELLERS = 4;   //on the window's roads at once
	private static final int MAX_PATROLS = 3;      //towns whose watch is out in the window at once
	//the surface's sight (viewDistance 20) and a cell: nobody new appears mid-road nearer a hero than this
	private static final int UNSEEN = 21;
	//a walk no further than this past its door may start at the door itself, in plain sight: he just stepped out
	private static final int FRESH = 12;
	//traffic turns between two looks for walkers due on the road
	private static final int TRAFFIC_EVERY = 20;
	//the traffic time of the next look; not bundled - the first step after a load looks
	private long nextTraffic = Long.MIN_VALUE;

	//may a walker come into being on this cell? never where a player could see it happen:
	//further than any hero's sight, and - outside a rebase, whose field of view is not yet the
	//new window's - nowhere a hero's eyes or a mind vision reach either
	private boolean unseenSpawn( int cell, boolean settled ){
		int d = heroDistance( this, cell );
		if (d <= UNSEEN) return false;
		if (Dungeon.level != this || Dungeon.hero == null) return true;
		if (d <= Dungeon.hero.viewDistance + 1) return false;
		return !settled || (!xyz.gabriwar.warpedpixeldungeon.net.NetManager.anyHeroSees( cell )
				&& Dungeon.hero.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision.class ) == null);
	}

	//where a walker leaving by a door (world wx, wy) first stands: the road just outside it, or
	//open ground there (never the doorway itself, nor back inside the house), or -1
	private int doorStep( float wx, float wy ){
		if (!inWindow( wx, wy )) return -1;
		int door = localCell( Math.round( wx ), Math.round( wy ) );
		if (door == -1 || (map[door] != Terrain.DOOR && map[door] != Terrain.OPEN_DOOR)) return -1;
		boolean[] road = roadMask();
		int open = -1;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = door + n;
			int t = map[c];
			if (t == Terrain.DOOR || t == Terrain.OPEN_DOOR || t == Terrain.EMPTY_SP || occupied( c )) continue;
			if (road[c] && passable[c] && !avoid[c]) return c;
			if (open == -1 && VillageRoutine.walkable( this, c )) open = c;
		}
		return open;
	}

	//the travellers: whoever RoadTraffic says is out on a road through the window now, set
	//down where the clock says he has got to - never out of thin air before a player's eyes
	//(only stepping out of his door), at most MAX_TRAVELLERS, the nearest to the hero first.
	//`settled`: the heroes' sight is this window's (not inside a rebase)
	private void placeTravellers( boolean settled ){
		if (altitude != 0 || network || RoadTraffic.nightForced()) return;
		long now = RoadTraffic.now();
		int day = RoadTraffic.day( now ), tod = RoadTraffic.turnOfDay( now );
		int w0 = RoadTraffic.setOut(), w1 = RoadTraffic.indoors();
		if (tod < w0 || tod >= w1) return;
		java.util.HashSet<Long> walking = new java.util.HashSet<>();
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller){
				walking.add( ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller) m).key );
			}
		}
		if (walking.size() >= MAX_TRAVELLERS) return;
		boolean live = Dungeon.level == this && Dungeon.hero != null;
		final int from = live ? Dungeon.hero.pos : width()/2 + height()/2 * width();
		ArrayList<Object[]> due = new ArrayList<>();   //{trip, cell}
		for (int[] r : RoadTraffic.travelPairs( worldSeed, sector0X() - 1, sector0Y() - 1, sector1X() + 1, sector1Y() + 1 )){
			if (!RoadTraffic.travelled( worldSeed, r[0], r[1], r[2], r[3] )) continue;
			int lanes = RoadTraffic.lanes( worldSeed, r[0], r[1], r[2], r[3] );
			for (int lane = 0; lane < lanes; lane++){
				RoadTraffic.Trip trip = RoadTraffic.tripAt( worldSeed, r[0], r[1], r[2], r[3], lane, day, tod, w0, w1 );
				if (trip == null || walking.contains( trip.key )) continue;
				float s = trip.distanceAt( tod );
				float[] p = RoadTraffic.point( trip.route, s );
				int cell = inWindow( p[0], p[1] ) ? trafficCell( p[0], p[1] ) : -1;
				if (cell != -1 && live && !unseenSpawn( cell, settled )) cell = -1;
				//the walk has only just begun: he steps out of his door, seen or not
				if (cell == -1 && s <= FRESH) cell = doorStep( trip.route[0], trip.route[1] );
				if (cell != -1) due.add( new Object[]{ trip, cell } );
			}
		}
		java.util.Collections.sort( due, (a, b) -> distance( from, (Integer) a[1] ) - distance( from, (Integer) b[1] ) );
		for (Object[] d : due){
			if (walking.size() >= MAX_TRAVELLERS) break;
			int at = freeSpotWithin( (Integer) d[1], 2 );
			if (at == -1) continue;
			RoadTraffic.Trip trip = (RoadTraffic.Trip) d[0];
			if (addMob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller.of( trip ), at )) walking.add( trip.key );
		}
	}

	//the town watches: the pair a big human town sends walking its road (RoadTraffic.beat),
	//set down where their beat has got to by the same rules as the travellers, at most
	//MAX_PATROLS towns' worth in the window
	private void placePatrols( boolean settled ){
		if (altitude != 0 || network || RoadTraffic.nightForced()) return;
		long now = RoadTraffic.now();
		int day = RoadTraffic.day( now ), tod = RoadTraffic.turnOfDay( now );
		int w0 = RoadTraffic.setOut(), w1 = RoadTraffic.indoors();
		if (tod < w0 || tod >= w1) return;
		java.util.HashSet<Long> towns = new java.util.HashSet<>(), walking = new java.util.HashSet<>();
		for (Mob m : mobs){
			if (!(m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol)) continue;
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol g = (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol) m;
			towns.add( g.homeSector );
			walking.add( g.homeSector * 31 + g.rank );
		}
		boolean live = Dungeon.level == this && Dungeon.hero != null;
		for (int sy = sector0Y(); sy <= sector1Y(); sy++){
			for (int sx = sector0X(); sx <= sector1X(); sx++){
				long home = WorldStructures.sectorOf( sx, sy );
				if (!towns.contains( home ) && towns.size() >= MAX_PATROLS) continue;
				RoadTraffic.Beat beat = RoadTraffic.beat( worldSeed, sx, sy, day, w0, w1 );
				if (beat == null) continue;
				for (int rank = 0; rank < 2; rank++){
					if (walking.contains( home * 31 + rank )) continue;
					float s = beat.distanceAt( tod, rank );
					if (s < 0) continue;
					float[] p = RoadTraffic.point( beat.route, s );
					int cell = inWindow( p[0], p[1] ) ? trafficCell( p[0], p[1] ) : -1;
					if (cell != -1 && live && !unseenSpawn( cell, settled )) cell = -1;
					if (cell == -1 && s <= FRESH) cell = doorStep( beat.route[0], beat.route[1] );
					if (cell == -1 || (cell = freeSpotWithin( cell, 2 )) == -1) continue;
					int tint = (int) Math.floorMod( structHash( sx, sy + 13 ), 6L );
					if (addMob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol.of( beat, home, rank, tint ), cell )){
						towns.add( home );
						walking.add( home * 31 + rank );
					}
				}
			}
		}
	}

	//the caravans on their morning leg: today's cart of each road through the window, driving from
	//where yesterday's stall stood to where today's goes up (RoadTraffic.caravanLeg), set down where
	//the clock says it has got to by the travellers' rules - never out of thin air before a
	//player's eyes, save breaking camp: in the first look after it set out it may start off from
	//where it stood the night, in plain sight, and make up the way. At most MAX_TRAINS
	private void placeCaravanTrains( boolean settled ){
		if (altitude != 0 || network || RoadTraffic.nightForced()) return;
		long now = RoadTraffic.now();
		int day = RoadTraffic.day( now ), tod = RoadTraffic.turnOfDay( now ), w0 = RoadTraffic.setOut();
		if (tod < w0) return;
		java.util.HashSet<Long> rolling = new java.util.HashSet<>();
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain){
				rolling.add( ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain) m).road );
			}
		}
		boolean live = Dungeon.level == this && Dungeon.hero != null;
		int weekday = xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.weekday().ordinal();
		int dayOfSeason = xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.dayOfSeason();
		java.util.HashSet<Long> up = caravansPitched();
		for (int[] r : RoadTraffic.roads( worldSeed, sector0X(), sector0Y(), sector1X(), sector1Y() )){
			if (rolling.size() >= MAX_TRAINS) break;
			long road = RoadTraffic.roadHash( worldSeed, r[0], r[1], r[2], r[3] );
			if (rolling.contains( road ) || up.contains( road )) continue;
			RoadTraffic.Trip leg = RoadTraffic.caravanLeg( worldSeed, r[0], r[1], r[2], r[3], weekday, dayOfSeason, day, w0 );
			if (leg == null || tod < leg.depart || tod >= leg.arrive) continue;
			float s = leg.distanceAt( tod );
			float[] p = RoadTraffic.point( leg.route, s );
			int cell = inWindow( p[0], p[1] ) ? trafficCell( p[0], p[1] ) : -1;
			if (cell != -1 && live && !unseenSpawn( cell, settled )) cell = -1;
			if (cell == -1 && s <= BREAK_CAMP && inWindow( leg.route[0], leg.route[1] )){
				cell = trafficCell( leg.route[0], leg.route[1] );
			}
			if (cell == -1 || (cell = freeSpotWithin( cell, 2 )) == -1) continue;
			if (addMob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain.of( leg, road ), cell )) rolling.add( road );
		}
	}

	//carts on the window's roads at once
	private static final int MAX_TRAINS = 3;
	//how far along its leg a cart may be on the first look after it set out (a look every
	//TRAFFIC_EVERY turns at its pace, and a cell over): it may still be seen breaking camp
	private static final float BREAK_CAMP = RoadTraffic.CARAVAN_PACE * TRAFFIC_EVERY + 1f;

	/** A marked stall's outlaws break from the verge (CaravanAmbush) as a hero comes in sight of
	 *  it - the host's or a co-op guest's, whoever's step it was - on the caravaneer's own turn. */
	public void springIfDue( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c ){
		if (!network && CaravanAmbush.due( this, c )) CaravanAmbush.spring( this, c );
	}

	/** A stall's siege settles (CaravanAmbush.settle), on the caravaneer's own turn. One fought
	 *  out is remembered for the rest of the day, so a stall pitched again on its road (its keeper
	 *  chased off) is not fallen on twice. */
	public void settleSiege( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c ){
		if (network) return;
		if (CaravanAmbush.settle( this, c, parkedMobs, parkedAt ) && c.road != Long.MIN_VALUE) ambushesFought().add( c.road );
	}

	//the roads (RoadTraffic.roadHash) whose caravan has had its ambush today, and the day they
	//are for; bundled, so a reload cannot pitch a fought-out stall afresh
	private java.util.HashSet<Long> ambushDone = new java.util.HashSet<>();
	private int ambushDoneDay = Integer.MIN_VALUE;

	private java.util.HashSet<Long> ambushesFought(){
		int today = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
		if (ambushDoneDay != today){
			ambushDone.clear();
			ambushDoneDay = today;
		}
		return ambushDone;
	}

	/** Has the caravan standing within six cells of this world cell had its day's trouble already
	 *  (or none to have) - live in the window or parked? A traveller then tells of the stall alone. */
	public boolean caravanAtPeace( int wx, int wy ){
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer
					&& Math.abs( worldX + m.pos % width() - wx ) <= 6 && Math.abs( worldY + m.pos / width() - wy ) <= 6){
				return atPeace( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m );
			}
		}
		for (HashMap.Entry<Long, Mob> e : parkedMobs.entrySet()){
			if (e.getValue() instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer
					&& Math.abs( (int)(e.getKey() & 0xFFFFFFFFL) - wx ) <= 6 && Math.abs( (int)(e.getKey() >> 32) - wy ) <= 6){
				return atPeace( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) e.getValue() );
			}
		}
		return false;
	}

	private static boolean atPeace( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer c ){
		return c.ambush != xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.AMBUSH_PENDING
				&& c.ambush != xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.AMBUSH_BESIEGED;
	}

	/** Debug scene (DebugScenes "overworld-road"): the four kinds of traveller on the road
	 *  (sx,sy)-(nx,ny) around the hero (6 and 12 cells ahead and behind), the road's caravan
	 *  coming up ten cells behind him to pitch its stall eight cells ahead, the watch of town
	 *  (tx,ty) eight cells behind him walking out, and two outlaws on its road eighteen cells
	 *  ahead of him. Returns {travellers set down, 1 when the caravan was}. */
	public int[] debugStageRoad( int sx, int sy, int nx, int ny, int tx, int ty ){
		long now = RoadTraffic.now();
		int day = RoadTraffic.day( now ), tod = RoadTraffic.turnOfDay( now );
		float hx = worldX + Dungeon.hero.pos % width(), hy = worldY + Dungeon.hero.pos / width();
		float[] ahead = { 6, -6, 12, -12 };
		int placed = 0;
		for (int kind = 0; kind < RoadTraffic.KINDS; kind++){
			boolean forward = kind % 2 == 0;
			RoadTraffic.Trip trip = RoadTraffic.forcedTrip( worldSeed, forward ? sx : nx, forward ? sy : ny,
					forward ? nx : sx, forward ? ny : sy, kind, day, tod, hx, hy, ahead[kind], Long.MIN_VALUE + kind );
			float[] p = RoadTraffic.point( trip.route, trip.distanceAt( tod ) );
			int cell = trafficCell( p[0], p[1] );
			if (cell == -1 || (cell = freeSpotWithin( cell, 2 )) == -1) continue;
			if (addMob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller.of( trip ), cell )) placed++;
		}
		RoadTraffic.Trip leg = RoadTraffic.forcedCaravanLeg( worldSeed, sx, sy, nx, ny, day, tod, hx, hy, 8, 18 );
		float[] lp = RoadTraffic.point( leg.route, leg.distanceAt( tod ) );
		int cart = trafficCell( lp[0], lp[1] );
		boolean rolling = cart != -1 && (cart = freeSpotWithin( cart, 2 )) != -1
				&& addMob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain.of( leg,
						RoadTraffic.roadHash( worldSeed, sx, sy, nx, ny ) ), cart );
		RoadTraffic.Beat beat = RoadTraffic.forcedBeat( worldSeed, tx, ty, day, tod, hx, hy, -8 );
		if (beat != null){
			long home = WorldStructures.sectorOf( tx, ty );
			int tint = (int) Math.floorMod( structHash( tx, ty + 13 ), 6L );
			for (int rank = 0; rank < 2; rank++){
				float[] p = RoadTraffic.point( beat.route, Math.max( 0f, beat.distanceAt( tod, rank ) ) );
				int cell = trafficCell( p[0], p[1] );
				if (cell != -1 && (cell = freeSpotWithin( cell, 2 )) != -1){
					addMob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol.of( beat, home, rank, tint ), cell );
				}
			}
			//the outlaws lie up on the road ahead, short of the neighbour's well and its folk
			float[] end = RoadTraffic.point( beat.route, Math.min( RoadTraffic.length( beat.route ) - 12f,
					RoadTraffic.project( beat.route, hx, hy ) + 18f ) );
			int c = trafficCell( end[0], end[1] );
			for (int i = 0; i < 2 && c != -1; i++){
				int at = freeSpotWithin( c, 3 );
				if (at == -1) break;
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit b = new xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit();
				b.state = b.WANDERING;
				addMob( b, at );
			}
		}
		return new int[]{ placed, rolling ? 1 : 0 };
	}

	/** Debug scene (DebugScenes "overworld-ambush"): pitches today's stalls if a far arrival has
	 *  not yet, and marks the one nearest the hero (within 40 cells) for an ambush. */
	public xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer debugAmbushNearest(){
		placeCaravans();
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer best = null;
		int bestD = 41;
		for (Mob m : mobs){
			if (!(m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer)) continue;
			int d = distance( m.pos, Dungeon.hero.pos );
			if (d < bestD){
				best = (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer) m;
				bestD = d;
			}
		}
		if (best != null && best.ambush != xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.AMBUSH_BESIEGED){
			int day = xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.day();
			best.ambush = xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.AMBUSH_PENDING;
			best.pitched = day;
			if (best.stall == xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit.NO_AMBUSH){
				best.stall = structHash( worldKey( worldX + best.pos % width(), worldY + best.pos / width() ), day );
			}
		}
		return best;
	}

	// ------------------------------------------------------- world events

	//what the world's timed events (WorldEvents) have come to here: heard of, settled, a star's
	//fragment down, debug-forced. the only part of them a save holds: where and when is pure
	private final WorldEventLog eventLog = new WorldEventLog();
	//the events that can reach the window: anchored within EVENT_RADIUS sectors of its centre (a
	//hero is never more than a sector off it, so this covers a market's three as well). cached
	//per centre sector, day and forced set; actor thread only
	static final int EVENT_RADIUS = 4;
	private java.util.List<WorldEvents.Event> nearEvents = java.util.Collections.emptyList();
	private long nearEventsKey = Long.MIN_VALUE;
	private int nearEventsDay = Integer.MIN_VALUE, nearEventsForced = -1;
	//each near star's crater (WorldEvents.craterCells), worked out once
	private final HashMap<Long, int[]> craterCache = new HashMap<>();
	//the world keys of the window cells the fallen stars' scorch lies on: the event's own work,
	//never captured as the player's edit, and given back to the land by it when the scar is gone
	private final java.util.HashSet<Long> laidScorch = new java.util.HashSet<>();
	//where each event was on the last step (1 on, 2 a star's cooling scar); null settles the
	//window's events on the next one (a load, a far arrival, a forced event)
	private HashMap<Long, Integer> liveEvents = null;
	private int eventDay = Integer.MIN_VALUE;
	//the settlements a travelling market is in now (sector keys): their folk read it every turn
	private volatile java.util.Set<Long> marketSectors = java.util.Collections.emptySet();
	//the craters' smoke and the markets' bunting on screen, and what was last posted to it
	private final xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor decor
			= new xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor();
	private int decorScene = 0;
	private java.util.List<xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Crater> decorCraters
			= java.util.Collections.emptyList();
	private java.util.List<xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Fair> decorFairs
			= java.util.Collections.emptyList();
	//a star lays its fragment, and a market its stalls, only this far inside the window: the
	//crater and every stall's shelf whole in it
	private static final int STAR_MARGIN = 3, MARKET_MARGIN = 14;
	//a market's stalls stand on the well's rings 4 to 8 off the roads, 4 to MARKET_RING failing that
	private static final int MARKET_RING = 12;

	/** The events' turn now: the world's (WorldClock.turn) through the log's clock, which never
	 *  runs back as a real-clock run's wall clock can - so nothing over comes round again. Every
	 *  event time here is read through it, and debug scenes force their events at it. Actor thread. */
	public int eventTurn(){
		return eventLog.clock( xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.turn() );
	}

	/** The events' day now (eventTurn's) for a reader on any thread: the log's mark is only
	 *  looked at, never moved on, so it agrees with the actor thread's eventTurn. */
	public int eventDayNow(){
		return WorldEvents.day( eventLog.peekClock( xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.turn() ) );
	}

	//the events about the window, for its day: recomputed only when the window's centre sector,
	//the day or the forced set changed
	java.util.List<WorldEvents.Event> nearEvents(){
		int cx = Math.floorDiv( worldX + WIDTH/2, WorldStructures.SECTOR );
		int cy = Math.floorDiv( worldY + HEIGHT/2, WorldStructures.SECTOR );
		int day = WorldEvents.day( eventTurn() );
		long key = WorldStructures.sectorOf( cx, cy );
		int forced = eventLog.forcedVersion();
		if (day != nearEventsDay || key != nearEventsKey || forced != nearEventsForced){
			long t = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
			nearEvents = WorldEvents.eventsNear( worldSeed, cx, cy, EVENT_RADIUS, day, eventLog.forced );
			nearEventsDay = day;
			nearEventsKey = key;
			nearEventsForced = forced;
			java.util.HashSet<Long> ids = new java.util.HashSet<>();
			for (WorldEvents.Event e : nearEvents) ids.add( e.id );
			craterCache.keySet().retainAll( ids );
			xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW events near", t );
		}
		return nearEvents;
	}

	private int[] craterCells( WorldEvents.Event e ){
		int[] cells = craterCache.get( e.id );
		if (cells == null){
			cells = WorldEvents.craterCells( worldSeed, e );
			craterCache.put( e.id, cells );
		}
		return cells;
	}

	//does a star's crater reach into the window at (ox, oy)?
	private static boolean craterReaches( WorldEvents.Event e, int ox, int oy ){
		return e.wx + 2 >= ox && e.wy + 2 >= oy && e.wx - 2 < ox + WIDTH && e.wy - 2 < oy + HEIGHT;
	}

	//the fallen stars' scorch on the window at (ox, oy) now: world key -> EMBERS, null for none.
	//WindowGenerator.overlayScorch lays it where that window's own ground takes it
	private HashMap<Long, Integer> scorchFor( int ox, int oy ){
		if (network || altitude != 0) return null;
		int turn = eventTurn();
		HashMap<Long, Integer> out = null;
		for (WorldEvents.Event e : nearEvents()){
			if (!e.scarredAt( turn ) || !craterReaches( e, ox, oy )) continue;
			int[] cells = craterCells( e );
			for (int i = 0; i + 1 < cells.length; i += 2){
				int x = cells[i] - ox, y = cells[i+1] - oy;
				if (x <= 0 || y <= 0 || x >= WIDTH-1 || y >= HEIGHT-1) continue;
				if (out == null) out = new HashMap<>();
				out.put( worldKey( cells[i], cells[i+1] ), Terrain.EMBERS );
			}
		}
		return out;
	}

	//the stars whose scorch reaches the window at (ox, oy) now, as one number (0 for none): a
	//window prepared for another set is derived again when it is taken
	private long scorchSig( int ox, int oy ){
		if (network || altitude != 0) return 0;
		int turn = eventTurn();
		long sig = 0;
		for (WorldEvents.Event e : nearEvents()){
			if (e.scarredAt( turn ) && craterReaches( e, ox, oy )) sig = sig * 0x9E3779B97F4A7C15L + e.id;
		}
		return sig;
	}

	//which of the stars' cells the window just derived carries
	private void noteScorch(){
		laidScorch.clear();
		HashMap<Long, Integer> scorch = scorchFor( worldX, worldY );
		if (scorch == null) return;
		for (Long key : scorch.keySet()){
			int cell = localCell( (int)(key & 0xFFFFFFFFL), (int)(key >> 32) );
			if (cell != -1 && map[cell] == Terrain.EMBERS && !diffs.containsKey( key )) laidScorch.add( key );
		}
	}

	//the standing window's scorch brought in line with the stars down now: a star that came
	//down on it is stamped, one whose scar is gone gives the land back its own ground
	private void stampCraters(){
		if (network || altitude != 0 || pristine == null) return;
		HashMap<Long, Integer> want = scorchFor( worldX, worldY );
		boolean live = liveScene();
		java.util.Iterator<Long> it = laidScorch.iterator();
		while (it.hasNext()){
			long key = it.next();
			if (want != null && want.containsKey( key )) continue;
			it.remove();
			int cell = localCell( (int)(key & 0xFFFFFFFFL), (int)(key >> 32) );
			if (cell == -1 || map[cell] != Terrain.EMBERS || diffs.containsKey( key )) continue;
			//a bush does not grow back round someone standing in it: that cell heals with the
			//next window instead (burnt brush is the land's to mend, never a player's edit)
			if ((Terrain.flags[pristine[cell]] & Terrain.SOLID) != 0
					&& (occupied( cell ) || heaps.get( cell ) != null)) continue;
			set( cell, pristine[cell], this );
			if (live) xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateMap( cell );
		}
		if (want == null) return;
		for (Long key : want.keySet()){
			if (laidScorch.contains( key )) continue;
			int cell = localCell( (int)(key & 0xFFFFFFFFL), (int)(key >> 32) );
			if (cell == -1 || diffs.containsKey( key ) || link[cell] != WindowGenerator.LINK_NONE
					|| !WorldEvents.scorchable( pristine[cell] ) || plants.get( cell ) != null) continue;
			//the player's own work on the cell stands; trampled or burnt growth is the land's
			if (map[cell] != pristine[cell] && !naturalDecay( pristine[cell], map[cell] )) continue;
			set( cell, Terrain.EMBERS, this );
			laidScorch.add( key );
			if (live) xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateMap( cell );
		}
	}

	//the hero's step on the surface: the window's events set to rights when one came on, went
	//off or cooled (or the day turned), the markets' crowds, what the hero hears of, and what
	//is shown of them
	private void tickEvents( int heroPos ){
		long t0 = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.begin();
		int turn = eventTurn();
		int day = WorldEvents.day( turn );
		HashMap<Long, Integer> live = new HashMap<>();
		for (WorldEvents.Event e : nearEvents()){
			int stage = e.activeAt( turn ) ? 1 : e.scarredAt( turn ) ? 2 : 0;
			if (stage != 0) live.put( e.id, stage );
		}
		if (day != eventDay || !live.equals( liveEvents )){
			eventDay = day;
			liveEvents = live;
			settleEvents( turn );
		}
		refreshMarkets( turn );
		announceEvents( heroPos, turn );
		//the events that keep their own book: raids on the villages, then the packs' night hunts
		RaidEvent.onHeroStep( this );
		HuntEvent.onHeroStep( this );
		showEventDecor( turn );
		xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.end( "OW events", t0 );
	}

	private void settleEvents( int turn ){
		expireEvents( turn, true );
		stampCraters();
		placeEvents( true );
	}

	//the settlements a market is in now and still trading (one chased off is over)
	private void refreshMarkets( int turn ){
		java.util.HashSet<Long> now = new java.util.HashSet<>();
		for (WorldEvents.Event e : nearEvents()){
			if (e.type == WorldEvents.Type.MARKET && e.activeAt( turn ) && !eventLog.resolved.containsKey( e.id )){
				now.add( WorldStructures.sectorOf( e.sx, e.sy ) );
			}
		}
		if (!now.equals( marketSectors )) marketSectors = now;
	}

	/** Is a travelling market up in this settlement (a sector key) now? Its folk crowd the well
	 *  (Settler). Any thread: a snapshot the hero's steps keep. */
	public boolean marketActiveAt( long sectorKey ){
		return marketSectors.contains( sectorKey );
	}

	/** Settles an event for good (a star looted, a market chased off): it is laid, announced and
	 *  pinned no more, and remembered until keepUntil (the later of two asks). */
	public void resolveEvent( long id, int keepUntil ){
		eventLog.resolved.merge( id, keepUntil, Math::max );
	}

	/** Is this event settled for good? Any thread: the log's concurrent map. */
	public boolean eventResolved( long id ){
		return eventLog.resolved.containsKey( id );
	}

	//has the party heard of this event? (it is pinned on the map while it is on)
	boolean eventAnnounced( long id ){
		return eventLog.announced.containsKey( id );
	}

	//an event that says itself (a raid by its smoke, a hunt by its howl) has been heard of
	void markAnnounced( WorldEvents.Event e ){
		eventLog.announced.put( e.id, e.endTurn );
	}

	//is this event under way on the ground (a hunt's pack loosed)? heard of is not begun: a hunt
	//is heard from afar as night falls and begins only when a hero comes near its ground
	boolean eventBegun( long id ){
		return eventLog.begun.containsKey( id );
	}

	void markBegun( WorldEvents.Event e ){
		eventLog.begun.put( e.id, e.endTurn );
	}

	/** A player's own words about something on the surface (tellParty): handed his hero and the
	 *  world cell he stands on, null for nothing to say. */
	interface PartyLine {
		String of( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero, int wx, int wy );
	}

	//a line for every player, each in his own words: the host's is his and his spectators' (who
	//watch through his hero's eyes and are known by the id -1), each co-op guest's his alone
	void tellParty( PartyLine line ){
		int w = width();
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero host = Dungeon.hero;
		String mine = line.of( host, worldX + host.pos % w, worldY + host.pos / w );
		if (mine != null){
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( host, mine );
			if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()){
				xyz.gabriwar.warpedpixeldungeon.net.StateSerializer.recordLogMessageForHero( -1, mine );
			}
		}
		if (!xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) return;
		for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh : xyz.gabriwar.warpedpixeldungeon.net.NetManager.getNetHeroes()){
			if (!nh.isAlive() || nh.pos < 0 || nh.pos >= length()) continue;
			String theirs = line.of( nh, worldX + nh.pos % w, worldY + nh.pos / w );
			if (theirs != null) xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( nh, theirs );
		}
	}

	//does a hero see a cell of the window: the host's by the level's field of view, a co-op
	//guest's by his own
	boolean heroSees( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero, int cell ){
		boolean[] fov = hero == Dungeon.hero ? heroFOV : hero.fieldOfView;
		return fov != null && cell >= 0 && cell < fov.length && fov[cell];
	}

	/** Debug scenes: an event forced through the very registry the world reads (WorldEvents.eventsNear). */
	public void forceEvent( WorldEvents.Event e ){
		eventLog.force( e );
		liveEvents = null;
	}

	/** Debug scenes: the hero's step pass now, so a forced event happens in front of the tester. */
	public void pollEvents(){
		if (network || altitude != 0 || Dungeon.hero == null || Dungeon.level != this) return;
		liveEvents = null;
		tickEvents( Dungeon.hero.pos );
	}

	/** The world map (render thread): the events on now that the party has heard of and that are
	 *  not settled, inside the chart's [ox, ox+span) x [oy, oy+span). Pure placement and the
	 *  log's concurrent maps: safe off the actor thread. A co-op guest's mirror reads the log its
	 *  host shipped it (adoptSharedEvents), so his map pins what the host's does. */
	public ArrayList<WorldEvents.Event> mapEvents( int ox, int oy, int span ){
		if (altitude != 0) return new ArrayList<>();
		int turn = eventLog.peekClock( xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.turn() );
		int cx = Math.floorDiv( ox + span/2, WorldStructures.SECTOR ), cy = Math.floorDiv( oy + span/2, WorldStructures.SECTOR );
		return WorldEvents.mapMarkers( WorldEvents.eventsNear( worldSeed, cx, cy, span / 2 / WorldStructures.SECTOR + 1,
						WorldEvents.day( turn ), eventLog.forced ),
				eventLog.announced, eventLog.resolved, turn, ox, oy, span );
	}

	/** Today's raids anchored about the window that are on and not beaten off, worked out afresh
	 *  from the world and the log - pure placement and the log's concurrent maps, so any thread
	 *  may ask. What RaidEvent reads where no snapshot of its own answers for the level: above all
	 *  a co-op guest's mirror, which takes no stock and holds the raids its host settled (and any
	 *  he forced) through adoptSharedEvents. */
	public ArrayList<WorldEvents.Event> raidsNow(){
		ArrayList<WorldEvents.Event> out = new ArrayList<>();
		if (altitude != 0) return out;
		int turn = eventLog.peekClock( xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.turn() );
		int cx = Math.floorDiv( worldX + WIDTH/2, WorldStructures.SECTOR );
		int cy = Math.floorDiv( worldY + HEIGHT/2, WorldStructures.SECTOR );
		for (WorldEvents.Event e : WorldEvents.eventsNear( worldSeed, cx, cy, EVENT_RADIUS, WorldEvents.day( turn ), eventLog.forced )){
			if (e.type == WorldEvents.Type.RAID && e.activeAt( turn ) && !eventLog.resolved.containsKey( e.id )) out.add( e );
		}
		return out;
	}

	/** Co-op host (StateSerializer): a fingerprint of what his guests' maps are built from, so it
	 *  is shipped again only when it changed (an event heard of, settled, forgotten or forced). */
	public long sharedEventsSig(){
		return eventLog.sharedSig();
	}

	/** Co-op host: what his guests' maps are built from (WorldEventLog.storeShared), as a bundle's
	 *  text. Any thread: the log's shared part is concurrent. */
	public String sharedEvents(){
		Bundle b = new Bundle();
		eventLog.storeShared( b );
		return b.toString();
	}

	/** A co-op guest's mirror takes over its host's heard-of, settled and forced events
	 *  (sharedEvents): the world map pins them as the host's does. Called before the mirror is
	 *  the level, or on the render thread, which is the only one reading it then. */
	public void adoptSharedEvents( String text ){
		if (!network || text == null || text.isEmpty()) return;
		try {
			eventLog.restoreShared( Bundle.read( new java.io.ByteArrayInputStream( text.getBytes() ) ) );
		} catch (java.io.IOException e){
			//Bundle.read has reported it. the map stays as it was; the next change ships it whole again
		}
	}

	/** Co-op host (StateSerializer): a fingerprint of the cracked ice his guests see (HazardWatch). */
	public long sharedHazardsSig(){
		return hazards.sharedSig();
	}

	/** Co-op host: the cracked ice as his guests see it, as a bundle's text. Any thread (a snapshot). */
	public String sharedHazards(){
		return hazards.shared();
	}

	/** A co-op guest's mirror takes over its host's cracked ice (sharedHazards): drawn and examined
	 *  as the host's. Called before the mirror is the level, or on the render thread. */
	public void adoptSharedHazards( String text ){
		if (!network || text == null || text.isEmpty()) return;
		try {
			hazards.adoptShared( Bundle.read( new java.io.ByteArrayInputStream( text.getBytes() ) ) );
		} catch (java.io.IOException e){
			//Bundle.read has reported it. the cracks stay as they were; the next change ships them again
		}
	}

	//every event on now laid down in the window: a star's fragment in its crater, a market's
	//stalls round its well. idempotent (lootAt, marketOpen); `fx` false inside a rebase
	private void placeEvents( boolean fx ){
		if (network || altitude != 0) return;
		int turn = eventTurn();
		for (WorldEvents.Event e : nearEvents()){
			if (!e.activeAt( turn ) || eventLog.resolved.containsKey( e.id )) continue;
			switch (e.type){
				case FALLEN_STAR: layStar( e ); break;
				case MARKET: openMarket( e, fx ); break;
				//a raid's band and a hunt's beasts come out only on the hero's step (RaidEvent, HuntEvent)
				case RAID: case HUNT: break;
			}
		}
	}

	//the star's fragment, tagged with it, on its heart or the nearest open crater ground
	private void layStar( WorldEvents.Event e ){
		//laid already: on the ground, or in the heap store
		if (eventLog.lootAt.containsKey( e.id )) return;
		int cell = localCell( e.wx, e.wy );
		if (cell == -1) return;
		int w = width(), x = cell % w, y = cell / w;
		if (x < STAR_MARGIN || y < STAR_MARGIN || x >= w - STAR_MARGIN || y >= height() - STAR_MARGIN) return;
		int at = -1;
		for (int r = 0; r <= 2 && at == -1; r++){
			for (int dy = -r; dy <= r && at == -1; dy++){
				for (int dx = -r; dx <= r && at == -1; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = cell + dx + dy * w;
					if (passable[c] && !water[c] && heaps.get( c ) == null && link[c] == WindowGenerator.LINK_NONE
							&& WorldEvents.scorchable( pristine[c] )) at = c;
				}
			}
		}
		if (at == -1){
			//none of the crater is open ground at this point of the year (a pond froze over it):
			//nothing to find there, so nothing to hear of or pin
			resolveEvent( e.id, e.endTurn );
			return;
		}
		xyz.gabriwar.warpedpixeldungeon.items.StarFragment f = new xyz.gabriwar.warpedpixeldungeon.items.StarFragment();
		f.eventId = e.id;
		f.eventEnd = e.endTurn;
		drop( f, at );
		eventLog.lootAt.put( e.id, worldKey( worldX + at % w, worldY + at / w ) );
		eventLog.lootEnds.put( e.id, e.endTurn );
	}

	//is any trader of this market about, in the window or parked?
	private boolean marketOpen( long id ){
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) m).eventId == id) return true;
		}
		for (Mob m : parkedMobs.values()){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) m).eventId == id) return true;
		}
		return false;
	}

	//the market's two or three stalls round the well, spread round it from a hashed bearing,
	//each a different speciality: off the roads on the rings 4 to 8 where there is room, on any
	//open ground out to MARKET_RING where there is not - never out past the houses, to the fence
	private void openMarket( WorldEvents.Event e, boolean fx ){
		if (marketOpen( e.id )) return;
		int well = localCell( e.wx, e.wy );
		if (well == -1) return;
		int w = width(), x = well % w, y = well / w;
		if (x < MARKET_MARGIN || y < MARKET_MARGIN || x >= w - MARKET_MARGIN || y >= height() - MARKET_MARGIN) return;
		int[] layout = WorldStructures.settlementLayout( worldSeed, e.sx, e.sy );
		int reach = Math.min( MARKET_RING, layout[0] );
		int stalls = 2 + (int)((e.id >>> 11) & 1L);
		int bearing = (int) Math.floorMod( e.id >>> 17, 8L );
		int first = (int) Math.floorMod( e.id >>> 23, (long) xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant.SPECIALITIES );
		ArrayList<Integer> picked = new ArrayList<>();
		for (int i = 0; i < stalls; i++){
			int spot = -1;
			for (int pass = 0; pass < 2 && spot == -1; pass++){
				for (int r = 4; r <= (pass == 0 ? Math.min( 8, reach ) : reach) && spot == -1; r++){
					int perim = 8 * r;
					//each stall aims at its own share of the ring, and takes the nearest room to it
					int aim = (bearing * perim / 8 + i * perim / stalls) % perim;
					for (int k = 0; k < perim && spot == -1; k++){
						int idx = Math.floorMod( aim + ((k & 1) == 0 ? k / 2 : -(k + 1) / 2), perim );
						int dx = VillageRoutine.ringX( r, idx ), dy = VillageRoutine.ringY( r, idx );
						int c = well + dx + dy * w;
						if (stallSpot( c, dx, dy, layout, picked, pass == 0 )) spot = c;
					}
				}
			}
			if (spot != -1) picked.add( spot );
		}
		boolean show = fx && liveScene();
		int opened = 0;
		for (int i = 0; i < picked.size(); i++){
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant m
					= xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant.of( first + i, e.id, e.endTurn );
			if (!addMob( m, picked.get( i ) )) continue;
			m.openShop();
			opened++;
			//the wagon rolls up in a cloud of road dust
			if (show) xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( m.pos ).burst(
					xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.DUST ), 8 );
		}
		//not a stall's room anywhere round the well: no market to hear of or pin
		if (opened == 0) resolveEvent( e.id, e.endTurn );
	}

	//may a trader pitch his stall here: open, dry, unclaimed ground with room for his four wares
	//round him, a clear cell between his shelf and every house, and room between the stalls and
	//from any other keeper's goods (whose shelf is what his strike would sweep)
	private boolean stallSpot( int cell, int dx, int dy, int[] layout, ArrayList<Integer> picked, boolean offRoad ){
		int w = width(), x = cell % w, y = cell / w;
		if (x < 3 || y < 3 || x > w - 4 || y > height() - 4) return false;
		if (!passable[cell] || water[cell] || occupied( cell ) || heaps.get( cell ) != null) return false;
		int t = map[cell];
		if (t == Terrain.DOOR || t == Terrain.OPEN_DOOR || t == Terrain.SIGN || t == Terrain.BRIDGE) return false;
		if (offRoad && t == Terrain.DIRT_PATH) return false;
		for (int i = 1; i + 1 < layout.length; i += 2){
			if (Math.max( Math.abs( dx - layout[i] ), Math.abs( dy - layout[i+1] ) ) < 5) return false;
		}
		for (int p : picked){
			if (distance( p, cell ) < 4) return false;
		}
		for (Mob m : mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper && distance( m.pos, cell ) < 4) return false;
		}
		int open = 0;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = cell + n;
			Heap h = heaps.get( c );
			if (h != null && h.type == Heap.Type.FOR_SALE) return false;
			if (passable[c] && !water[c] && h == null && map[c] != Terrain.DOOR && map[c] != Terrain.OPEN_DOOR) open++;
		}
		return open >= 4;
	}

	//whatever has run its time goes: a market's traders with their stalls (in the window or
	//parked), a star's fragment nobody picked up; then the log forgets what is over. `fx`
	//false inside a rebase or a load
	private void expireEvents( int turn, boolean fx ){
		if (network || altitude != 0) return;
		for (Mob m : mobs.toArray( new Mob[0] )){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) m).endTurn <= turn){
				strikeMerchant( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) m, fx );
			}
		}
		java.util.Iterator<HashMap.Entry<Long, Mob>> it = parkedMobs.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (!(e.getValue() instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant)) continue;
			xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant m
					= (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) e.getValue();
			if (m.endTurn > turn) continue;
			sweepStall( m, (int)(e.getKey() & 0xFFFFFFFFL), (int)(e.getKey() >> 32) );
			it.remove();
			parkedAt.remove( e.getKey() );
		}
		for (Long id : new ArrayList<>( eventLog.lootEnds.keySet() )){
			if (eventLog.lootEnds.get( id ) > turn) continue;
			Long key = eventLog.lootAt.remove( id );
			eventLog.lootEnds.remove( id );
			if (key != null) takeBackFragment( id, key, fx );
		}
		eventLog.prune( turn );
	}

	//a trader's goods, laid out round the cell he pitched on (his stall's world cell; failing
	//that, where he stands): on the window and in the heap store. never Heap.destroy(), which
	//reads Dungeon.level - not this level during a load
	private void sweepStall( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant m, int wx, int wy ){
		int sx = m.stallX != Integer.MIN_VALUE ? m.stallX : wx, sy = m.stallY != Integer.MIN_VALUE ? m.stallY : wy;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				long key = worldKey( sx + dx, sy + dy );
				Heap away = storedHeaps.get( key );
				if (away != null && away.type == Heap.Type.FOR_SALE) storedHeaps.remove( key );
				int cell = localCell( sx + dx, sy + dy );
				Heap h = cell != -1 ? heaps.get( cell ) : null;
				if (h == null || h.type != Heap.Type.FOR_SALE) continue;
				heaps.remove( cell );
				if (h.sprite != null) h.sprite.kill();
				h.items.clear();
			}
		}
	}

	//the stall comes down with the trader, and only his own shelf. never his destroy(): a
	//shopkeeper's takes his flight with it (TravellingMerchant routes it here)
	private void strikeMerchant( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant m, boolean fx ){
		sweepStall( m, worldX + m.pos % width(), worldY + m.pos / width() );
		if (fx && liveScene()) xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( m.pos ).burst(
				xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle.FACTORY, 4 );
		vanish( this, m );
	}

	/** A travelling trader chased off (TravellingMerchant.flee, destroy): the whole troupe packs up
	 *  - every stall of the market, in the window or parked, each with only its own shelf - and
	 *  the market is over for good, its pin gone and its crowd dispersed. */
	public void merchantFled( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant m ){
		//one already gone with his troupe (every keeper on the level told to flee at once)
		if (network || !mobs.contains( m )) return;
		boolean live = liveScene();
		GLog.n( Messages.get( m, "flee" ) );
		for (Mob o : mobs.toArray( new Mob[0] )){
			if (!(o instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant)
					|| ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) o).eventId != m.eventId) continue;
			if (live) xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( o.pos ).burst(
					xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle.FACTORY, 6 );
			strikeMerchant( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) o, false );
		}
		java.util.Iterator<HashMap.Entry<Long, Mob>> it = parkedMobs.entrySet().iterator();
		while (it.hasNext()){
			HashMap.Entry<Long, Mob> e = it.next();
			if (!(e.getValue() instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant)
					|| ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) e.getValue()).eventId != m.eventId) continue;
			sweepStall( (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) e.getValue(),
					(int)(e.getKey() & 0xFFFFFFFFL), (int)(e.getKey() >> 32) );
			it.remove();
			parkedAt.remove( e.getKey() );
		}
		resolveEvent( m.eventId, m.endTurn );
		refreshMarkets( eventTurn() );
	}

	//a star nobody reached cools: only its own tagged fragment is taken back - whatever a
	//player left in the heap with it stays
	private void takeBackFragment( long id, long key, boolean fx ){
		int cell = localCell( (int)(key & 0xFFFFFFFFL), (int)(key >> 32) );
		Heap h = cell != -1 ? heaps.get( cell ) : null;
		//a heap the window could not put back (its cell taken) is still in the store
		boolean stored = h == null || fragmentIn( h, id ) == null;
		if (stored) h = storedHeaps.get( key );
		if (h == null) return;
		for (Item it; (it = fragmentIn( h, id )) != null; ) h.items.remove( it );
		if (!h.items.isEmpty()){
			if (!stored && h.sprite != null && liveScene()) h.sprite.view( h ).place( h.pos );
			return;
		}
		if (stored){
			storedHeaps.remove( key );
			return;
		}
		heaps.remove( cell );
		if (h.sprite != null) h.sprite.kill();
		if (fx && liveScene()) xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( cell ).burst(
				xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle.FACTORY, 6 );
	}

	//a star's own fragment in a heap, or null
	private static Item fragmentIn( Heap h, long id ){
		for (Item it : h.items){
			if (it instanceof xyz.gabriwar.warpedpixeldungeon.items.StarFragment
					&& ((xyz.gabriwar.warpedpixeldungeon.items.StarFragment) it).eventId == id) return it;
		}
		return null;
	}

	/** StarFragment.doPickUp: the star is settled for good, and its fixed chance at something more
	 *  (WorldEvents.starBonus) turns up where the fragment lay - once the pickup is over, on the
	 *  next actor turn: Hero.actPickUp takes the heap's top item after doPickUp, so a drop now
	 *  would be what it took. */
	public void starLooted( long id, int endTurn, int cell ){
		if (network) return;
		resolveEvent( id, endTurn );
		eventLog.lootAt.remove( id );
		eventLog.lootEnds.remove( id );
		int kind = WorldEvents.starBonus( id );
		if (kind < 0) return;
		final Item gift = xyz.gabriwar.warpedpixeldungeon.items.Generator.random( kind == 0
				? xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.RING : kind == 1
				? xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.WAND
				: xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.ARTIFACT );
		//what a star brings down is clean
		gift.cursed = false;
		Actor.add( new Actor(){
			{
				actPriority = VFX_PRIO;
			}

			@Override
			protected boolean act(){
				//once: off the scheduler before it returns
				Actor.remove( this );
				Heap h = drop( gift, cell );
				if (liveScene() && h.sprite != null) h.sprite.drop();
				GLog.p( Messages.get( WorldEvents.class, "star_bonus" ) );
				return true;
			}
		} );
	}

	//what the party hears of, each event once, as the host's hero steps: every player in his own
	//words - the way it lies and whether it is close, from where his own hero stands. the host's
	//line is his alone and his spectators' (who watch through his hero's eyes and are known by the
	//id -1), each co-op guest's goes to him alone. then what the host's screen shows of it
	private void announceEvents( int heroPos, int turn ){
		int w = width(), hwx = worldX + heroPos % w, hwy = worldY + heroPos / w;
		for (WorldEvents.Event e : WorldEvents.toAnnounce( nearEvents(), eventLog.announced, eventLog.resolved,
				Math.floorDiv( hwx, WorldStructures.SECTOR ), Math.floorDiv( hwy, WorldStructures.SECTOR ), turn,
				worldX, worldY, WIDTH, HEIGHT )){
			eventLog.announced.put( e.id, e.endTurn );
			String line = GLog.HIGHLIGHT + eventLine( e, turn, hwx, hwy );
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( Dungeon.hero, line );
			if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()){
				xyz.gabriwar.warpedpixeldungeon.net.StateSerializer.recordLogMessageForHero( -1, line );
				for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh
						: xyz.gabriwar.warpedpixeldungeon.net.NetManager.getNetHeroes()){
					if (!nh.isAlive() || nh.pos < 0 || nh.pos >= length()) continue;
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( nh,
							GLog.HIGHLIGHT + eventLine( e, turn, worldX + nh.pos % w, worldY + nh.pos / w ) );
				}
			}
			if (e.type == WorldEvents.Type.FALLEN_STAR) showFall( e, heroPos, hwx, hwy, turn );
			else if (liveScene()) com.watabou.noosa.audio.Sample.INSTANCE.play( Assets.Sounds.GOLD, 0.7f );
		}
	}

	//how a hero on world cell (hwx, hwy) hears of an event. a star heard of as it falls: close by,
	//screaming down; further off, streaking across the sky the way it went, or flaring through
	//the clouds. heard of later, by the glow it left while its crater is hot, by the crater once
	//it is cold. a market by its bunting and music
	private String eventLine( WorldEvents.Event e, int turn, int hwx, int hwy ){
		String dir = WorldEvents.direction( e.wx - hwx, e.wy - hwy );
		boolean near = dir.equals( "here" );
		String where = near ? "" : Messages.get( WorldEvents.class, "dir_" + dir );
		if (e.type == WorldEvents.Type.MARKET){
			String name = WorldStructures.villageName( worldSeed, e.sx, e.sy );
			int berth = WorldStructures.settlementLayout( worldSeed, e.sx, e.sy )[0] + 3;
			//"here" reaches only STAR_NEAR, so past it there is always a way to name
			if (Math.max( Math.abs( e.wx - hwx ), Math.abs( e.wy - hwy ) ) <= Math.max( berth, WorldEvents.STAR_NEAR )){
				return Messages.get( WorldEvents.class, "market_here", name );
			}
			return Messages.get( WorldEvents.class, "market_opens", name, where );
		}
		int since = turn - e.startTurn;
		if (since >= WorldEvents.STAR_HOT){
			return near ? Messages.get( WorldEvents.class, "star_cold_near" ) : Messages.get( WorldEvents.class, "star_cold", where );
		}
		if (since >= WorldEvents.STAR_FRESH){
			return near ? Messages.get( WorldEvents.class, "star_glow_near" ) : Messages.get( WorldEvents.class, "star_glow", where );
		}
		if (near) return Messages.get( WorldEvents.class, "star_falls_near" );
		if (clouded()) return Messages.get( WorldEvents.class, "star_falls_clouded", where );
		return Messages.get( WorldEvents.class, "star_falls", where );
	}

	//cloud, rain or snow overhead: a falling star is only a flash through it
	private static boolean clouded(){
		return xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.isRaining()
				|| xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.isStorming()
				|| xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.isSnowing();
	}

	//what a player's screen shows of a star heard of as it falls: close by it screams down onto
	//its crater and the birds and the hares bolt from it; further off it streaks across the sky
	//the way it went - or only rumbles, unseen through the clouds. the host's on his step, a
	//co-op guest's from his mirror (EventDecorMirror), each from where his own hero stands
	void showFall( WorldEvents.Event e, int heroPos, int hwx, int hwy, int turn ){
		if (turn - e.startTurn >= WorldEvents.STAR_FRESH) return;
		boolean live = liveScene();
		if (WorldEvents.direction( e.wx - hwx, e.wy - hwy ).equals( "here" )){
			int impact = localCell( e.wx, e.wy );
			OverworldCritters.noise( impact );
			if (live){
				final com.watabou.utils.PointF to = xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld( impact );
				//down out of the sky over the hero's side, onto the crater
				final float side = e.wx >= hwx ? 1 : -1;
				com.watabou.noosa.Game.runOnRenderThread( () -> xyz.gabriwar.warpedpixeldungeon.effects.StarStreak.fall(
						new com.watabou.utils.PointF( to.x - side * 6 * 16, to.y - 9 * 16 ), to, 0.4f, true ) );
				com.watabou.noosa.audio.Sample.INSTANCE.play( Assets.Sounds.FALLING, 0.7f, 1.4f );
			}
			return;
		}
		if (!live) return;
		if (clouded()){
			com.watabou.noosa.audio.Sample.INSTANCE.play( Assets.Sounds.ROCKS, 0.5f, 0.7f );
			return;
		}
		//from high over the hero's shoulder away across the sky toward it, sinking as it goes
		final com.watabou.utils.PointF p = xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld( heroPos );
		float len = (float) Math.hypot( e.wx - hwx, e.wy - hwy );
		final float ux = (e.wx - hwx) / len, uy = (e.wy - hwy) / len;
		com.watabou.noosa.Game.runOnRenderThread( () -> xyz.gabriwar.warpedpixeldungeon.effects.StarStreak.fall(
				new com.watabou.utils.PointF( p.x - ux * 4 * 16, p.y - uy * 4 * 16 - 6 * 16 ),
				new com.watabou.utils.PointF( p.x + ux * 14 * 16, p.y + uy * 14 * 16 - 2 * 16 ), 0.9f, false ) );
		com.watabou.noosa.audio.Sample.INSTANCE.play( Assets.Sounds.FALLING, 0.6f, 1.3f );
	}

	//what the window shows of its events: the craters still hot, and the markets with their
	//traders about. posted to the render thread only when that changed (or the scene did),
	//positions taken now - after this step's slide, so they are the new frame's
	private void showEventDecor( int turn ){
		if (!liveScene()) return;
		final int sceneId = System.identityHashCode( com.watabou.noosa.Game.scene() );
		final ArrayList<xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Crater> craters = new ArrayList<>();
		final ArrayList<xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Fair> fairs = new ArrayList<>();
		eventDecor( nearEvents(), turn, OverworldLevel::tradesFor, craters, fairs );
		if (sceneId == decorScene && xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.same(
				craters, fairs, decorCraters, decorFairs )) return;
		decorScene = sceneId;
		decorCraters = craters;
		decorFairs = fairs;
		final xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor d = decor;
		com.watabou.noosa.Game.runOnRenderThread( () -> d.show( sceneId, craters, fairs ) );
	}

	//the host's traders: each knows the market he came with
	static boolean tradesFor( Mob m, WorldEvents.Event e ){
		return m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant
				&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant) m).eventId == e.id;
	}

	/** A co-op guest's traders: the host's stand-ins under a market stall's sprite round the
	 *  market's well (no two markets share a village). Render thread, on the guest's mirror. */
	boolean guestTradesFor( Mob m, WorldEvents.Event e ){
		if (!(m instanceof xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver.SpectatorMob) || m.spriteClass == null
				|| !xyz.gabriwar.warpedpixeldungeon.sprites.TravellingMerchantSprite.class.isAssignableFrom( m.spriteClass )) return false;
		int well = localCell( e.wx, e.wy );
		return well != -1 && m.pos >= 0 && m.pos < length() && distance( m.pos, well ) <= MARKET_RING + 2;
	}

	/** What the window shows of its events (effects/WorldEventDecor), laid out in its scene
	 *  pixels: the craters still hot, and the markets on and not settled with their traders
	 *  about (`trader`: does a mob trade for that market). The host's step (showEventDecor) and a
	 *  co-op guest's mirror (EventDecorMirror) both build theirs here, so every screen shows the
	 *  same smoke, glow and bunting. */
	void eventDecor( java.util.List<WorldEvents.Event> events, int turn,
			java.util.function.BiPredicate<Mob, WorldEvents.Event> trader,
			java.util.List<xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Crater> craters,
			java.util.List<xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Fair> fairs ){
		int w = width();
		for (WorldEvents.Event e : events){
			int c = localCell( e.wx, e.wy );
			if (c == -1) continue;
			com.watabou.utils.PointF at = xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileToWorld( c );
			if (e.type == WorldEvents.Type.FALLEN_STAR){
				int since = turn - e.startTurn;
				if (since < 0 || since >= WorldEvents.STAR_HOT) continue;
				craters.add( new xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Crater(
						e.id, at.x, at.y, 3 - since * 3 / WorldEvents.STAR_HOT ) );
			} else if (e.type == WorldEvents.Type.MARKET && e.activeAt( turn ) && !eventLog.resolved.containsKey( e.id )){
				ArrayList<Mob> traders = new ArrayList<>();
				for (Mob m : mobs){
					if (trader.test( m, e )) traders.add( m );
				}
				if (traders.isEmpty()) continue;
				//by the world cell each stands on: the same order whatever the window
				java.util.Collections.sort( traders, (a, b) -> Long.compare(
						worldKey( worldX + a.pos % w, worldY + a.pos / w ), worldKey( worldX + b.pos % w, worldY + b.pos / w ) ) );
				long[] keys = new long[traders.size()];
				float[] xy = new float[traders.size() * 2];
				for (int i = 0; i < traders.size(); i++){
					int pos = traders.get( i ).pos;
					keys[i] = worldKey( worldX + pos % w, worldY + pos / w );
					com.watabou.utils.PointF p = xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileToWorld( pos );
					xy[2 * i] = p.x;
					xy[2 * i + 1] = p.y;
				}
				fairs.add( new xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor.Fair( e.id, at.x, at.y, keys, xy ) );
			}
		}
	}

	//the log of the window's events, for a co-op guest's mirror (EventDecorMirror): the shared
	//part its host shipped, read as the world map reads it
	WorldEventLog eventLog(){
		return eventLog;
	}

	private static final String WORLD_SEED = "world_seed";
	private static final String ALTITUDE   = "altitude";
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
	private static final String AMBUSH_DONE_DAY = "ambush_done_day";
	private static final String AMBUSH_DONE = "ambush_done";
	//the slices' found places (CaveSites, MountainSites) and their periodic keys (dueOnce)
	private static final String FOUND_AT   = "layer_sites_found_at";
	private static final String FOUND_KIND = "layer_sites_found_kind";
	private static final String DUE_KEYS   = "layer_due_keys";
	private static final String DUE_DAYS   = "layer_due_days";
	private static final String ORE_DROP_DAY = "ore_drop_day";
	private static final String ORE_DROPS = "ore_drops";

	@Override
	public void storeInBundle( Bundle bundle ) {
		captureDiffs();
		super.storeInBundle( bundle );
		bundle.put( WORLD_SEED, worldSeed );
		bundle.put( ALTITUDE, altitude );
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
		long[] fought = new long[ambushDone.size()];
		i = 0;
		for (Long k : ambushDone) fought[i++] = k;
		bundle.put( AMBUSH_DONE_DAY, ambushDoneDay );
		bundle.put( AMBUSH_DONE, fought );
		long[] foundAt = new long[foundSites.size()];
		int[] foundKind = new int[foundSites.size()];
		i = 0;
		for (java.util.Map.Entry<Long, Integer> e : foundSites.entrySet()){
			foundAt[i] = e.getKey();
			foundKind[i++] = e.getValue();
		}
		bundle.put( FOUND_AT, foundAt );
		bundle.put( FOUND_KIND, foundKind );
		long[] dueKeys = new long[dueDays.size()];
		int[] dueVals = new int[dueDays.size()];
		i = 0;
		for (java.util.Map.Entry<Long, Integer> e : dueDays.entrySet()){
			dueKeys[i] = e.getKey();
			dueVals[i++] = e.getValue();
		}
		bundle.put( DUE_KEYS, dueKeys );
		bundle.put( DUE_DAYS, dueVals );
		bundle.put( ORE_DROP_DAY, oreDropDay );
		bundle.put( ORE_DROPS, oreDrops );
		eventLog.storeInBundle( bundle );
		hazards.storeInBundle( bundle );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		setAltitude( bundle.getInt( ALTITUDE ) );
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

		//Recover the five town-art layers from the save (ground under the hero,
		//roofs over). Saves from the roofless builds only carry three; placeTownArt
		//then rebuilds the full set.
		java.util.ArrayList<TownRemixedTiles.Layer> found = new java.util.ArrayList<>();
		for (xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap c : customTiles){
			if (c instanceof TownRemixedTiles.Base || c instanceof TownRemixedTiles.Deco
					|| c instanceof TownRemixedTiles.Deco2) found.add( (TownRemixedTiles.Layer) c );
		}
		for (xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap c : customWalls){
			if (c instanceof TownRemixedTiles.RoofBase || c instanceof TownRemixedTiles.RoofDeco)
				found.add( (TownRemixedTiles.Layer) c );
		}
		if (found.size() == 5) townArt = found.toArray( new TownRemixedTiles.Layer[0] );
		else { customTiles.removeAll( found ); customWalls.removeAll( found ); }
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
		ambushDone.clear();
		ambushDoneDay = bundle.contains( AMBUSH_DONE_DAY ) ? bundle.getInt( AMBUSH_DONE_DAY ) : Integer.MIN_VALUE;
		long[] fought = bundle.contains( AMBUSH_DONE ) ? bundle.getLongArray( AMBUSH_DONE ) : null;
		if (fought != null) for (long k : fought) ambushDone.add( k );
		//older saves found no places: none known, none due
		foundSites.clear();
		foundBoxes.clear();
		long[] foundAt = bundle.contains( FOUND_AT ) ? bundle.getLongArray( FOUND_AT ) : null;
		int[] foundKind = bundle.contains( FOUND_KIND ) ? bundle.getIntArray( FOUND_KIND ) : null;
		if (foundAt != null && foundKind != null){
			for (int i = 0; i < Math.min( foundAt.length, foundKind.length ); i++){
				//a kind this build does not know is skipped, never guessed at
				if (foundKind[i] < 0 || (altitude < 0 && foundKind[i] >= CaveSites.Type.values().length)
						|| (altitude > 0 && foundKind[i] >= MountainSites.Kind.values().length)) continue;
				foundSites.put( foundAt[i], foundKind[i] );
			}
		}
		rebuildFoundSnapshot();
		dueDays.clear();
		long[] dueKeys = bundle.contains( DUE_KEYS ) ? bundle.getLongArray( DUE_KEYS ) : null;
		int[] dueVals = bundle.contains( DUE_DAYS ) ? bundle.getIntArray( DUE_DAYS ) : null;
		if (dueKeys != null && dueVals != null){
			for (int i = 0; i < Math.min( dueKeys.length, dueVals.length ); i++) dueDays.put( dueKeys[i], dueVals[i] );
		}
		oreDropDay = bundle.contains( ORE_DROP_DAY ) ? bundle.getInt( ORE_DROP_DAY ) : -1;
		oreDrops = bundle.contains( ORE_DROPS ) ? bundle.getInt( ORE_DROPS ) : 0;
		//before the window is derived: a star a debug scene forced scorches it too
		eventLog.restoreFromBundle( bundle );
		//spent firedamp and cracked ice, by world position (absent from older saves: none)
		hazards.restoreFromBundle( bundle );

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
		//a save from before a place stood here may have the hero (or a co-op guest) on a cell its
		//walls or props took since, or shut inside a tomb: each is set down on the nearest open
		//ground outside it. a mob or a heap left where a prop or a wall stands now comes out too.
		//solid only: a hero saved floating over a drop (levitating, flying) stays where he was
		if (!network && Dungeon.depth == WorldLayers.depthOf( altitude )){
			if (Dungeon.hero != null && Dungeon.hero.pos >= 0 && Dungeon.hero.pos < length()
					&& (solid[Dungeon.hero.pos] || inSealedTomb( Dungeon.hero.pos ))){
				Dungeon.hero.pos = openGroundNear( Dungeon.hero.pos, false );
			}
			for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh
					: xyz.gabriwar.warpedpixeldungeon.net.NetManager.allKnownNetHeroes()){
				if (nh.pos >= 0 && nh.pos < length() && (solid[nh.pos] || inSealedTomb( nh.pos ))){
					nh.pos = openGroundNear( nh.pos, false );
				}
			}
			for (Mob m : mobs){
				if (m.pos < 0 || m.pos >= length() || !solid[m.pos]) continue;
				int to = freeSpotWithin( m.pos, 6 );
				m.pos = to != -1 ? to : openGroundNear( m.pos, false );
			}
			for (Heap h : heaps.valueList()){
				if (!solid[h.pos]) continue;
				heaps.remove( h.pos );
				h.pos = openGroundNear( h.pos, true );
				heaps.put( h.pos, h );
			}
		}
		if (altitude == 0){
			//a market's village crowds its well from the first (Settler)
			refreshMarkets( eventTurn() );
			//a raid is read from the first too, not from the hero's first step: a level read
			//back is a new one, and until then its shops would trade and a raider cut down would
			//settle nothing (RaidEvent)
			RaidEvent.takeStock( this );
			resettleFolk();
		}
		populateTown();
		//what ended while the save lay waiting goes before any scene shows it
		if (altitude == 0) expireEvents( eventTurn(), false );
	}

	//a saved window's settlements, as it comes back: whoever's part of the day turned while it
	//lay in the save (the hero was below, or away) is put straight where the day has got to,
	//before any sprite exists - one still on the way in the same part of it walks on. the
	//guards are put at the hour's post
	private void resettleFolk(){
		for (Mob m : mobs){
			int wx = worldX + m.pos % width(), wy = worldY + m.pos / width();
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler
					? !((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler) m).dayMovedOn( this, wx, wy )
					: !(m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard)) continue;
			int at = VillageRoutine.arrivalCell( this, m, wx, wy );
			if (at != -1) m.pos = at;
		}
	}

	/** The biome at a window cell, in world coordinates. */
	public WorldModel.Biome biomeAtCell( int cell ){
		return WorldModel.biomeAt( worldSeed,
				worldX + cell % width(), worldY + cell / width() );
	}

	@Override
	public String tileNameAt( int cell ){
		String place = placeKind( cell );
		if (place != null) return Messages.get( CaveSites.class, place );
		if (hazards.crackedAt( cell ) && map[cell] == Terrain.FROZEN_WATER) return Messages.get( HazardWatch.class, "cracked_name" );
		String kind = builtWall( cell );
		if (kind != null) return Messages.get( this, kind );
		String peak = placeCell( cell );
		if (peak != null) return Messages.get( MountainSites.class, peak );
		Ores.Kind ore = shownOre( cell );
		return ore == null ? null : Messages.capitalize( Messages.get( this, "vein", Messages.get( ore.item, "name" ) ) );
	}

	@Override
	public String tileDescAt( int cell ){
		String place = placeKind( cell );
		if (place != null) return Messages.get( CaveSites.class, place + "_desc" );
		if (hazards.crackedAt( cell ) && map[cell] == Terrain.FROZEN_WATER) return Messages.get( HazardWatch.class, "cracked_desc" );
		String kind = builtWall( cell );
		if (kind != null) return Messages.get( this, kind + "_desc" );
		String peak = placeCell( cell );
		if (peak != null) return Messages.get( MountainSites.class, peak + "_desc" );
		Ores.Kind ore = shownOre( cell );
		return ore == null ? null : Messages.get( this, "vein_desc", Messages.capitalize( Messages.get( ore.item, "name" ) ) );
	}

	//a cave place's prop, crack or wall on this cell, by its string key (CaveSites), else null
	private String placeKind( int cell ){
		if (altitude >= 0 || cell < 0 || cell >= length()) return null;
		return CaveSites.cellKind( layerSites, worldX + cell % width(), worldY + cell / width(), map[cell] );
	}

	//a wall of the surface that people built, by its string key: a village house's or a gnoll
	//hut's shell (one shape for all, WorldStructures), else null - the land's own rock
	private String builtWall( int cell ){
		if (cell < 0 || cell >= length() || map[cell] != Terrain.WALL) return null;
		int wx = worldX + cell % width(), wy = worldY + cell / width();
		//on the peaks: the hermit's hut, a waystation's shelter, a watchtower's ring (MountainSites)
		if (altitude > 0) return MountainSites.wallKey( worldSeed, peakSites, wx, wy );
		if (altitude != 0) return null;
		if (WorldStructures.wallSite( worldSeed, wx, wy ) != WorldStructures.Site.VILLAGE) return null;
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		//the nearest village's faction: a house wall stands within its own sector's reach
		WorldStructures.Faction f = null;
		int best = Integer.MAX_VALUE;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (WorldStructures.siteType( worldSeed, sx+dx, sy+dy ) != WorldStructures.Site.VILLAGE) continue;
				int d = Math.abs( WorldStructures.siteX( worldSeed, sx+dx, sy+dy ) - wx )
						+ Math.abs( WorldStructures.siteY( worldSeed, sx+dx, sy+dy ) - wy );
				if (d < best){
					best = d;
					f = WorldStructures.faction( worldSeed, sx+dx, sy+dy );
				}
			}
		}
		return f == WorldStructures.Faction.HUMAN ? "house_wall" : "hut_wall";
	}

	//a place of the peaks' own cell by its string key: an eyrie's nest, a waystation's hearth, a
	//hot spring, a watchtower's top (MountainSites), else null
	private String placeCell( int cell ){
		if (altitude <= 0 || cell < 0 || cell >= length()) return null;
		return MountainSites.cellKey( worldSeed, peakSites, worldX + cell % width(), worldY + cell / width(), map[cell] );
	}

	@Override
	public String cellDescExtra( int cell ){
		//the biome is the surface's: under it, in the caves, it would name the sky's weather above
		if (altitude < 0) return null;
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
			case Terrain.ENTRANCE:
				return Messages.get( this, altitude < 0 ? "way_up" : "steps_up" );
			case Terrain.EXIT:
				return Messages.get( this, altitude == 0 ? "cave_mouth" : altitude > 0 ? "steps_down" : "way_down" );
			case Terrain.CHASM:
				return Messages.get( this, altitude > 0 ? "drop" : "pit" );
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
			case Terrain.MINE_CRYSTAL:
				return Messages.get( this, "crystal" );
			case Terrain.MINE_BOULDER:
				return Messages.get( this, "loose_rock" );
			case Terrain.SHRUB:
				return Messages.get( this, "shrub" );
			case Terrain.SNOW:
				return Messages.get( this, "snow" );
			case Terrain.DIRT_PATH:
				//the surface's roads; on the peaks the same ground is a tunnel's floor
				return Messages.get( this, altitude == 0 ? "road" : "tunnel" );
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.WALL:
				return Messages.get( this, altitude < 0 ? "cave_wall_desc" : altitude > 0 ? "peak_wall_desc" : "wall_desc" );
			case Terrain.ENTRANCE:
				return Messages.get( this, altitude < 0 ? "way_up_desc" : "steps_up_desc" );
			case Terrain.EXIT:
				return Messages.get( this, altitude == 0 ? "cave_mouth_desc" : altitude > 0 ? "steps_down_desc" : "way_down_desc" );
			case Terrain.CHASM:
				return Messages.get( this, altitude > 0 ? "drop_desc" : "pit_desc" );
			case Terrain.MINE_CRYSTAL:
				return Messages.get( this, "crystal_desc" );
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
			case Terrain.MINE_BOULDER:
				return Messages.get( this, "loose_rock_desc" );
			case Terrain.SNOW:
				return Messages.get( this, "snow_desc" );
			case Terrain.DIRT_PATH:
				return Messages.get( this, altitude == 0 ? "road_desc" : "tunnel_desc" );
			default:
				return super.tileDesc( tile );
		}
	}
}
