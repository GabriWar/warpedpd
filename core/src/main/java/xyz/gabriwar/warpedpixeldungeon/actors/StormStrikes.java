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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.StormCloud;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A storm's ground bolts, decided on the game's turn (docs/ambience.md, "Weather"): while
 * ClimateManager.isStorming(), under the open sky only (the overworld's surface and peaks, never
 * indoors, in its caves or on a dungeon floor), a bolt now and then on a cell around a hero
 * (the host's or a guest's) - not aimed at him - that shocks whoever stands there, scorches the
 * ground and lights the grass round about. Once a world turn from DayNightCycle.onHeroTurn: the
 * host's actor thread.
 *
 * Where it lands it runs on as the wand of lightning's zap does (discharge): to whoever stands
 * near whoever it hit, further through water, and out over the water it falls in or beside.
 *
 * What it struck is handed over to be drawn (WeatherOverlay, on the render thread) through a
 * queue, and to a co-op guest's overlay through NetVisuals; a storm cloud's bolts go the same way,
 * its blow already dealt (StormCloud.strike).
 */
public final class StormStrikes {

	//how far round the hero a bolt may land, in cells each way: some of them out of his sight
	static final int REACH = 10;

	//how many steps out a bolt runs over the water it falls in or beside
	static final int CRAWL = 3;

	//the blow's source: the sky's bolt is a storm cloud's (what is immune to the cloud's
	//lightning is immune to it, the cloud's death message tells of it)
	private static final StormCloud SKY = new StormCloud();

	/** A bolt that came down: on which level and where, for the overlay to draw; a storm cloud's
	 *  (StormCloud.strike) or the open sky's; where it ran (discharge), its arcs from one it hit to
	 *  the next and its crawl over the water, as pairs of cells (from, to), none when it ran nowhere. */
	public static final class Bolt {
		public final Level level;
		public final int cell;
		public final boolean cloud;
		public final int[] arcs, water;

		Bolt( Level level, int cell, boolean cloud, int[] arcs, int[] water ){
			this.level = level;
			this.cell = cell;
			this.cloud = cloud;
			this.arcs = arcs;
			this.water = water;
		}
	}

	/** Where a bolt ran (discharge): whom it hit, the struck first, and whom its arcs reached from
	 *  him on (they share its blow); whom its crawl over the water ran into (a splash each); its
	 *  arcs from one it hit to the next and into those in the water, and its crawl where that
	 *  reached no one, as pairs of cells (from, to); whether it struck standing water, when those
	 *  it hit take the whole of the blow. */
	public static final class Discharge {
		public final ArrayList<Char> hit = new ArrayList<>();
		public final ArrayList<Char> wet = new ArrayList<>();
		public int[] arcs = NONE, water = NONE;
		public boolean full;
	}

	private static final int[] NONE = new int[0];

	//the actor thread (or a guest's net replay) adds, the render thread takes
	private static final ConcurrentLinkedQueue<Bolt> landed = new ConcurrentLinkedQueue<>();

	private StormStrikes(){}

	/** The storm's fury: the rain or the wind, whichever is harder, 0 to 1. */
	public static float fury(){
		return Math.min( 1f, Math.max( ClimateManager.localPrecipRate(), ClimateManager.localWindSpeed() / 18f ) );
	}

	//a bolt on a turn: 0.1 in the mildest storm to 0.35 in a full one
	static float chance(){
		return 0.1f + 0.25f * fury();
	}

	/** Where the sky is open overhead: the overworld's surface and its peaks. */
	public static boolean underOpenSky( Level level ){
		return level instanceof OverworldLevel && ((OverworldLevel) level).openSky();
	}

	/** Once a world turn (DayNightCycle.onHeroTurn). */
	public static void onHeroTurn(){
		Level level = Dungeon.level;
		if (level == null || Dungeon.hero == null || !ClimateManager.isStorming() || !underOpenSky( level )) return;
		if (Random.Float() >= chance()) return;
		int cell = pick( level, around( level ) );
		if (cell != -1) strike( cell );
	}

	//whose surroundings a bolt falls in: any hero of the party on the level alike, the host's or
	//a co-op guest's (each kept with a chance of one in those seen so far)
	private static int around( Level level ){
		Hero at = Dungeon.hero;
		if (NetManager.isHost()){
			int seen = 1;
			for (Hero h : NetManager.getNetHeroes()){
				if (!h.isAlive() || h.atExit || h.pos < 0 || h.pos >= level.length()) continue;
				if (Random.Int( ++seen ) == 0) at = h;
			}
		}
		return at.pos;
	}

	/** A cell within REACH of `from`, any of them alike, open ground: -1 when a few tries find
	 *  none. */
	static int pick( Level level, int from ){
		int w = level.width(), h = level.height();
		int fx = from % w, fy = from / w;
		for (int i = 0; i < 10; i++){
			int x = fx + Random.IntRange( -REACH, REACH ), y = fy + Random.IntRange( -REACH, REACH );
			if (x < 0 || y < 0 || x >= w || y >= h) continue;
			int cell = x + y * w;
			if (ground( level, cell )) return cell;
		}
		return -1;
	}

	//open ground: not a wall, a tree, a rock, nor a house's doorway (the villagers come and go
	//by it, and a bolt must not burn it away)
	private static boolean ground( Level level, int cell ){
		return !level.solid[cell] && level.map[cell] != Terrain.OPEN_DOOR;
	}

	//the town's ground and fields are not the storm's to burn
	private static boolean inTown( Level level, int cell ){
		return level instanceof OverworldLevel && ((OverworldLevel) level).inTown( cell );
	}

	//where the bolt may light a fire: no one on the cell or by its four sides, the way fire
	//spreads (Fire.evolve) - the surface's burning (harmDepth 97) would kill where the bolt did
	//not, and the rain does not put it out on one already drenched - and no door there for it
	//to burn away
	private static boolean clear( Level level, int cell ){
		if (Actor.findChar( cell ) != null) return false;
		for (int n : PathFinder.NEIGHBOURS4){
			int c = cell + n;
			if (c < 0 || c >= level.length()) continue;
			if (Actor.findChar( c ) != null || level.map[c] == Terrain.DOOR || level.map[c] == Terrain.OPEN_DOOR) return false;
		}
		return true;
	}

	/**
	 * A bolt on a cell of the level shown: whoever stands there takes a fifth to a third of his
	 * health and a turn's paralysis, and it runs on to whoever stands near (discharge), the blow
	 * shared out among them as the wand of lightning's (StormCloud.jolt); the ground it hits is
	 * burnt to embers at once, and the fire it lights on the grass round about burns on its own
	 * turns. Nothing burns in the town, and no fire is lit under anyone or where it could reach a
	 * door.
	 */
	public static void strike( int cell ){
		Level level = Dungeon.level;
		Discharge d = StormCloud.jolt( cell, SKY, StormStrikes::share );
		if (!inTown( level, cell )){
			//the scorch is the bolt's own: the heavy rain a storm brings cannot take it back
			if (level.flamable[cell] && ground( level, cell )){
				boolean blocked = level.losBlocking[cell];
				level.destroy( cell );
				GameScene.updateMap( cell );
				if (blocked) Dungeon.observe();
			}
			for (int n : PathFinder.NEIGHBOURS4){
				int adj = cell + n;
				if (adj >= 0 && adj < level.length() && level.flamable[adj] && ground( level, adj )
						&& !inTown( level, adj ) && clear( level, adj )){
					GameScene.add( Blob.seed( adj, 4, Fire.class ) );
				}
			}
		}
		show( level, cell, false, d.arcs, d.water );
		NetVisuals.recordStrike( cell, false, d.arcs, d.water );
	}

	/**
	 * Where a bolt on the cell runs, the way the wand of lightning's zap does from what it strikes;
	 * nothing is dealt here (StormCloud.jolt deals it). From whoever stands on the cell, unless he
	 * is immune to `src`, on to whoever stands within a cell of him, two where he stands in water
	 * and does not fly, and on from each of them the same way, each one once, never through a wall.
	 * Then over the water beside the cell or under it, up to CRAWL steps out, into whoever stands
	 * in it and does not fly (`wet`: no share of the blow, the splash the water beside a bolt
	 * always carried), and no further on from them. The hero's side (heroes, a co-op guest's too,
	 * and their allies) is only reached from beside one it hit, as the wand's caster is, never by
	 * the water: it flows round them. The town's people (NPCs: the weather harms them not) take
	 * no part, nor does what is immune. On a bare dry cell it runs nowhere. It struck standing
	 * water (`full`) where the cell is water a storm cloud does not hang over: one makes every
	 * cell under it water (StormCloud.evolve), so its every bolt would.
	 */
	public static Discharge discharge( Level level, int cell, Class<?> src ){
		Discharge d = new Discharge();
		d.full = level.water[cell] && !rained( level, cell );
		ArrayList<Integer> arcs = new ArrayList<>(), water = new ArrayList<>();
		Char struck = Actor.findChar( cell );
		if (struck != null && !struck.isImmune( src )){
			d.hit.add( struck );
			if (!(struck instanceof NPC)) chain( level, src, d.hit, arcs );
		}
		crawl( level, cell, src, d.hit, d.wet, arcs, water );
		d.arcs = flat( arcs );
		d.water = flat( water );
		return d;
	}

	//under a storm cloud, whose rain has made the cell water
	private static boolean rained( Level level, int cell ){
		Blob cloud = level.blobs.get( StormCloud.class );
		return cloud != null && cloud.cur != null && cloud.cur[cell] > 0;
	}

	//the wand of lightning's arcs (WandOfLightning.arc), from each one hit so far and each it
	//reaches in turn: a pair of cells (from, to) each
	private static void chain( Level level, Class<?> src, ArrayList<Char> hit, ArrayList<Integer> arcs ){
		boolean[] open = BArray.not( level.solid, null );
		int w = level.width(), h = level.height();
		for (int i = 0; i < hit.size(); i++){
			Char from = hit.get( i );
			int reach = level.water[from.pos] && !from.flying ? 2 : 1;
			PathFinder.buildDistanceMap( from.pos, open, reach );
			int fx = from.pos % w, fy = from.pos / w;
			ArrayList<Char> on = new ArrayList<>();
			for (int y = Math.max( 0, fy - reach ); y <= Math.min( h - 1, fy + reach ); y++){
				for (int x = Math.max( 0, fx - reach ); x <= Math.min( w - 1, fx + reach ); x++){
					int c = x + y * w;
					if (PathFinder.distance[c] > reach) continue;
					Char ch = Actor.findChar( c );
					if (ch == null || hit.contains( ch ) || !conducts( ch, src )) continue;
					if (ch.alignment == Char.Alignment.ALLY && PathFinder.distance[c] > 1) continue;
					on.add( ch );
				}
			}
			for (Char ch : on){
				hit.add( ch );
				arcs.add( from.pos );
				arcs.add( ch.pos );
			}
		}
	}

	//the bolt's run over the water from its cell, breadth first and eight ways as PathFinder goes,
	//up to CRAWL steps out: a 7x7 window at most, never a whole lake. Whom it runs into, none its
	//arcs hit, go into `wet`. Each step a pair of cells (whence, whither): into `arcs` where it
	//reaches someone, else into `water`
	private static void crawl( Level level, int cell, Class<?> src, ArrayList<Char> hit, ArrayList<Char> wet,
	                           ArrayList<Integer> arcs, ArrayList<Integer> water ){
		int w = level.width(), h = level.height(), cx = cell % w, cy = cell / w;
		int span = 2 * CRAWL + 1;
		//steps out to each cell of the window, -1 where it has not run
		int[] steps = new int[span * span];
		Arrays.fill( steps, -1 );
		steps[CRAWL + CRAWL * span] = 0;
		int[] queue = new int[span * span];
		int head = 0, tail = 0;
		queue[tail++] = cell;
		while (head < tail){
			int c = queue[head++];
			int x = c % w, y = c / w;
			int s = steps[(x - cx + CRAWL) + (y - cy + CRAWL) * span];
			if (s == CRAWL) continue;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int n = nx + ny * w, at = (nx - cx + CRAWL) + (ny - cy + CRAWL) * span;
					if (!level.water[n] || steps[at] >= 0) continue;
					Char ch = Actor.findChar( n );
					//the hero's side standing in it: it flows round them
					if (ch != null && ch.alignment == Char.Alignment.ALLY && !ch.flying) continue;
					steps[at] = s + 1;
					queue[tail++] = n;
					if (ch != null && !ch.flying && conducts( ch, src ) && !hit.contains( ch )){
						wet.add( ch );
						arcs.add( c );
						arcs.add( n );
					} else {
						water.add( c );
						water.add( n );
					}
				}
			}
		}
	}

	//what a bolt runs into and on from: none immune to it, and none of the town's people (the
	//weather harms them not: they would only share its blow out and pass it through a crowd)
	private static boolean conducts( Char ch, Class<?> src ){
		return !ch.isImmune( src ) && !(ch instanceof NPC);
	}

	private static int[] flat( ArrayList<Integer> cells ){
		int[] a = new int[cells.size()];
		for (int i = 0; i < a.length; i++) a[i] = cells.get( i );
		return a;
	}

	/**
	 * A bolt's blow on the world, the sky's or a storm cloud's: a fifth to a third of the struck's
	 * health, at least 3. By share, not a dungeon floor's 2-4 + depth/3: the world's slot depths
	 * (97 the surface) would make that a killing blow for a hero of any level.
	 */
	public static int share( Char ch ){
		return ch == null ? 0 : Math.max( 3, Math.round( ch.HT * Random.Float( 0.2f, 0.33f ) ) );
	}

	/** Hands a bolt, whose blow is dealt, to the overlay to draw (or, the sky's, only to be heard
	 *  out of the hero's sight); `cloud` for a storm cloud's. */
	public static void show( Level level, int cell, boolean cloud ){
		show( level, cell, cloud, NONE, NONE );
	}

	/** The same, with where it ran (discharge): its arcs and its crawl over the water, as pairs of
	 *  cells (from, to), each drawn where the hero sees it. */
	public static void show( Level level, int cell, boolean cloud, int[] arcs, int[] water ){
		landed.add( new Bolt( level, cell, cloud, arcs, water ) );
	}

	/** The next bolt to draw, or null: the render thread's (WeatherOverlay). */
	public static Bolt next(){
		return landed.poll();
	}
}
