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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.SliceCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * The surface's small life: birds that scatter when the hero walks up, hares that bolt,
 * fish leaping in the rivers, butterflies over the summer grass. None of it is an Actor
 * and none of it is saved - pictures that come and go around the hero, on the render
 * thread, through the Field below. What may turn up where and when is decided by the
 * pure rules here (biome, hour, season, weather, ground), which the tests pin.
 *
 * In multiplayer every machine draws its own, the night's fireflies too: they are not in
 * step, and need not be. A client's critters still shy from every hero and mob it shows
 * (it knows them by its level's mobs - a client registers nobody with the scheduler), but
 * only the host (or a solo game) hears a fight scatter them - Char.attack, which raises
 * the noise, runs there.
 *
 * On the slices above and below, the Field hands its turn to the slice's own life
 * (SliceLife: CaveLife's bats, drips, glows and blind fish; PeakLife's eagle, marmots,
 * snow plumes and sea of clouds).
 */
public final class OverworldCritters {

	private OverworldCritters(){}

	public enum Species { CROW, GULL, FINCH, OWL, HARE, BUTTERFLY, FISH }

	/** What the weather lets out: everything, half of it, or nothing. */
	public enum Sky { CLEAR, WET, FOUL }

	/** Whose life a slice has: the surface's, the caves', the peaks'. */
	enum Slice { SURFACE, CAVES, PEAKS }

	static Slice sliceOf( int altitude ){
		return altitude < 0 ? Slice.CAVES : altitude > 0 ? Slice.PEAKS : Slice.SURFACE;
	}

	//caps by kind: bird flocks (crows, gulls and finches together), owls, hares,
	//butterflies, fish in the air at once
	static final int FLOCKS = 0, OWLS = 1, HARES = 2, BUTTERFLIES = 3, FISH = 4;
	static final int[] CAP = { 2, 1, 2, 4, 2 };

	//reach, in scene pixels (16 to a cell): a flock lifts as anything comes within three
	//cells and looks up at five and a half; a hare bolts at four and sits up at six and a
	//half; a fight in sight scatters birds within eight cells and hares within six
	private static final float SCARE_BIRD_PX = 52f, WARY_PX = 88f, BOLT_PX = 68f, ALERT_PX = 104f,
			NOISE_BIRD_PX = 128f, NOISE_HARE_PX = 96f, STARTLE_PX = 20f, DESPAWN_PX = 224f;
	//random cells tried per spawn attempt: a handful, never a scan of the window
	private static final int PROBES = 6;

	//the render thread's own dice: never the dungeon's seeded generators
	static final java.util.Random RNG = new java.util.Random();

	static float rf( float min, float max ){
		return min + RNG.nextFloat() * (max - min);
	}

	static int ri( int min, int max ){
		return min + RNG.nextInt( max - min + 1 );
	}

	static float cheb( float ax, float ay, float bx, float by ){
		return Math.max( Math.abs( ax - bx ), Math.abs( ay - by ) );
	}

	// ------------------------------------------------------------ the rules

	static Sky skyOf( boolean storming, float faunaWeather, PrecipType type, float rate ){
		if (storming || faunaWeather <= 0f) return Sky.FOUL;
		float r = type == PrecipType.NONE ? 0f : rate;
		if (r > 0.4f) return Sky.FOUL;
		if (r > 0.01f) return Sky.WET;
		return Sky.CLEAR;
	}

	/** The weather as the critters see it: storms, blizzards and downpours keep everything in. */
	public static Sky sky(){
		return skyOf( ClimateManager.isStorming(), OverworldFauna.weatherFactor(),
				ClimateManager.localPrecipType(), ClimateManager.localPrecipRate() );
	}

	static float skyFactor( Sky sky ){
		return sky == Sky.CLEAR ? 1f : sky == Sky.WET ? 0.5f : 0f;
	}

	/** The birds of a biome at an hour: crows on the grass and the woods' edges by day, gulls on the beach from dawn to dusk, the desert's little finches at dawn and dusk only. */
	static Species birdFor( WorldModel.Biome biome, Phase phase ){
		switch (biome){
			case PLAINS: case MEADOW: case FOREST:
				return phase == Phase.DAWN || phase == Phase.DAY ? Species.CROW : null;
			case BEACH:
				return phase != Phase.NIGHT ? Species.GULL : null;
			case DESERT:
				return phase == Phase.DAWN || phase == Phase.DUSK ? Species.FINCH : null;
			default:
				return null;
		}
	}

	/** Is this the hour (and the weather) for these birds to be out? The owl keeps the night. */
	static boolean birdHour( Species s, Phase phase, Sky sky ){
		if (sky == Sky.FOUL) return false;
		switch (s){
			case CROW:  return phase == Phase.DAWN || phase == Phase.DAY;
			case GULL:  return phase != Phase.NIGHT;
			case FINCH: return phase == Phase.DAWN || phase == Phase.DUSK;
			case OWL:   return phase == Phase.NIGHT;
			default:    return false;
		}
	}

	static boolean hareHome( WorldModel.Biome biome ){
		return biome == WorldModel.Biome.MEADOW || biome == WorldModel.Biome.PLAINS
				|| biome == WorldModel.Biome.FOREST || biome == WorldModel.Biome.TUNDRA;
	}

	static boolean hareHour( Phase phase, Sky sky ){
		return sky != Sky.FOUL && (phase == Phase.DAWN || phase == Phase.DAY);
	}

	/** Butterflies: a calm, dry day in spring or summer. */
	static boolean butterflyHour( Phase phase, GameCalendar.Season season, Sky sky, float wind ){
		return phase == Phase.DAY && sky == Sky.CLEAR && wind < 10f
				&& (season == GameCalendar.Season.SPRING || season == GameCalendar.Season.SUMMER);
	}

	static boolean flutterGround( int terrain ){
		return terrain == Terrain.GRASS || terrain == Terrain.HIGH_GRASS || terrain == Terrain.FLOWER_PATCH
				|| terrain == Terrain.FURROWED_GRASS || terrain == Terrain.EMPTY;
	}

	/** Over the meadows and the plains, or wherever flowers grow. */
	static boolean butterflyGround( WorldModel.Biome biome, int terrain ){
		return flutterGround( terrain ) && (biome == WorldModel.Biome.MEADOW
				|| biome == WorldModel.Biome.PLAINS || terrain == Terrain.FLOWER_PATCH);
	}

	/** Leaps per unit of the base rate: twice as many at dawn and dusk, few at night, fewer in the rain, none in a storm. */
	static float fishRate( Phase phase, Sky sky ){
		if (sky == Sky.FOUL) return 0f;
		float rate = phase == Phase.DAWN || phase == Phase.DUSK ? 2f : phase == Phase.DAY ? 1f : 0.45f;
		return sky == Sky.WET ? rate * 0.7f : rate;
	}

	/** The ground each kind stands on. Never water, never walls, never long grass (it would vanish in it). */
	static boolean groundFor( Species s, int t ){
		switch (s){
			case CROW:
				return t == Terrain.GRASS || t == Terrain.EMPTY || t == Terrain.EMPTY_DECO
						|| t == Terrain.DIRT_PATH || t == Terrain.FLOWER_PATCH || t == Terrain.SNOW;
			case GULL:
				return t == Terrain.EMPTY_SP || t == Terrain.EMPTY || t == Terrain.SNOW;
			case FINCH:
				return t == Terrain.EMPTY_SP || t == Terrain.EMPTY || t == Terrain.DIRT_PATH;
			case HARE:
				return t == Terrain.GRASS || t == Terrain.FLOWER_PATCH || t == Terrain.EMPTY || t == Terrain.SNOW;
			default:
				return false;
		}
	}

	/** Nothing tall stands in the cell below: an oak's crown or a rock's top drawn over this one would be drawn under the critter. */
	static boolean clearBelow( int[] map, int w, int cell ){
		int t = map[cell + w];
		return t != Terrain.TREE_OAK && t != Terrain.TREE_PINE && t != Terrain.BOULDER
				&& t != Terrain.WALL && t != Terrain.WALL_DECO
				&& t != Terrain.HIGH_GRASS && t != Terrain.FURROWED_GRASS;
	}

	/**
	 * No wall, door or fence within two cells, no statue or sign alongside: not inside a hut
	 * (a village hut is a 5x5 ring round a 3x3 floor, so its middle has no wall alongside),
	 * not under its roof art, not against a palisade. The cell must be two cells inside the map.
	 */
	static boolean openAround( int[] map, int w, int cell ){
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				if (dx == 0 && dy == 0) continue;
				int t = map[cell + dx + dy * w];
				if (t == Terrain.WALL || t == Terrain.WALL_DECO || t == Terrain.DOOR || t == Terrain.OPEN_DOOR
						|| t == Terrain.LOCKED_DOOR || t == Terrain.BARRICADE || t == Terrain.TOWN_SOLID) return false;
				if (Math.abs( dx ) <= 1 && Math.abs( dy ) <= 1
						&& (t == Terrain.STATUE || t == Terrain.SIGN)) return false;
			}
		}
		return true;
	}

	/** Is (gx, gy) more than `px` (Chebyshev, scene pixels) from every point of `pts` (x0,y0,x1,y1...)? */
	static boolean clearOfChars( float gx, float gy, float[] pts, float px ){
		for (int i = 0; i + 1 < pts.length; i += 2){
			if (cheb( gx, gy, pts[i], pts[i+1] ) <= px) return false;
		}
		return true;
	}

	/** The edge of a wood or a glade in it: no tree alongside, but one within two cells. */
	static boolean forestEdge( int[] map, int w, int h, int cell ){
		int x = cell % w, y = cell / w;
		boolean near = false;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int cx = x + dx, cy = y + dy;
				if ((dx == 0 && dy == 0) || cx < 0 || cy < 0 || cx >= w || cy >= h) continue;
				int t = map[cx + cy * w];
				if (t != Terrain.TREE_OAK && t != Terrain.TREE_PINE) continue;
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) == 1) return false;
				near = true;
			}
		}
		return near;
	}

	static boolean liquid( int terrain ){
		return (Terrain.flags[terrain] & Terrain.LIQUID) != 0;
	}

	/** Open water with room to leap: liquid, five of its eight neighbours too. No puddles. */
	static boolean fishWater( int[] map, int w, int h, int cell ){
		int x = cell % w, y = cell / w;
		if (x < 1 || y < 1 || x > w - 2 || y > h - 2 || !liquid( map[cell] )) return false;
		int n = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && liquid( map[cell + dx + dy * w] )) n++;
			}
		}
		return n >= 5;
	}

	/** Where a hare hides: long grass, a bush, a tree, a rock. */
	static boolean isCover( int terrain ){
		return terrain == Terrain.HIGH_GRASS || terrain == Terrain.FURROWED_GRASS || terrain == Terrain.SHRUB
				|| terrain == Terrain.TREE_PINE || terrain == Terrain.TREE_OAK || terrain == Terrain.BOULDER;
	}

	/** Ground a hare can cross: dry and open, or the cover it dives into. */
	static boolean runnable( int terrain ){
		int f = Terrain.flags[terrain];
		return ((f & Terrain.PASSABLE) != 0 && (f & Terrain.LIQUID) == 0) || isCover( terrain );
	}

	/** The first cover cell alongside (north, then clockwise), or -1. */
	static int coverNextTo( int[] map, int w, int cell ){
		int[] around = { -w, -w + 1, 1, w + 1, w, w - 1, -1, -w - 1 };
		for (int o : around){
			if (isCover( map[cell + o] )) return cell + o;
		}
		return -1;
	}

	/** Is the owl's perch drawn? The dressing hangs no crown over a doorway, a sign or a shrine. */
	static boolean crownDrawn( int terrainAbove ){
		return !WindowGenerator.blocksSight( terrainAbove );
	}

	static boolean nearSegment( float px, float py, float ax, float ay, float bx, float by, float r ){
		float vx = bx - ax, vy = by - ay, l2 = vx * vx + vy * vy;
		float t = l2 <= 0f ? 0f : Math.max( 0f, Math.min( 1f, ((px - ax) * vx + (py - ay) * vy) / l2 ) );
		float qx = ax + vx * t - px, qy = ay + vy * t - py;
		return qx * qx + qy * qy <= r * r;
	}

	static int groupOf( Species s ){
		switch (s){
			case OWL:       return OWLS;
			case HARE:      return HARES;
			case BUTTERFLY: return BUTTERFLIES;
			case FISH:      return FISH;
			default:        return FLOCKS;
		}
	}

	static boolean roomFor( Species s, int[] alive ){
		int g = groupOf( s );
		return alive[g] < CAP[g];
	}

	/** A hare's dash: waypoints in cell coordinates (centres at +0.5), x0,y0,x1,y1..., and whether it ends in cover. */
	static final class Route {
		float[] pts;
		boolean cover;
	}

	/**
	 * Away from (srcX, srcY), in cell coordinates: a zig-zag of legs about a cell and a half
	 * long, ending in the nearest cover ahead (within eight cells, straight on first, then a
	 * cell or two either side) or, with none, ten cells out, where it fades from sight. Every
	 * leg runs over ground a hare can cross; a leg that would not stops the dash short of it.
	 * With no map (the window is mid-rebase under the picture) it runs straight out and fades.
	 */
	static Route boltRoute( int[] map, int w, int h, int start, float srcX, float srcY, java.util.Random rng ){
		float sx = start % w + 0.5f, sy = start / w + 0.5f;
		float dx = sx - srcX, dy = sy - srcY;
		float len = (float)Math.hypot( dx, dy );
		if (len < 0.01f){
			double a = rng.nextDouble() * Math.PI * 2;
			dx = (float)Math.cos( a );
			dy = (float)Math.sin( a );
		} else {
			dx /= len;
			dy /= len;
		}
		float px = -dy, py = dx;
		Route r = new Route();
		float tx = 0f, ty = 0f;
		if (map != null){
			search:
			for (int k = 3; k <= 8; k++){
				for (int lat : new int[]{ 0, -1, 1, -2, 2 }){
					int cx = (int)Math.floor( sx + dx * k + px * lat ), cy = (int)Math.floor( sy + dy * k + py * lat );
					if (cx < 1 || cy < 1 || cx > w - 2 || cy > h - 2) continue;
					if (isCover( map[cx + cy * w] )){
						tx = cx + 0.5f;
						ty = cy + 0.5f;
						r.cover = true;
						break search;
					}
				}
			}
		}
		if (!r.cover){
			tx = Math.max( 1.5f, Math.min( w - 1.5f, sx + dx * 10f ) );
			ty = Math.max( 1.5f, Math.min( h - 1.5f, sy + dy * 10f ) );
		}
		float dist = (float)Math.hypot( tx - sx, ty - sy );
		int legs = Math.max( 2, Math.min( 6, Math.round( dist / 1.8f ) ) );
		float[] pts = new float[legs * 2];
		int n = 0;
		float side = rng.nextBoolean() ? 1f : -1f;
		float lx = sx, ly = sy;
		for (int i = 1; i <= legs; i++){
			float f = i / (float)legs;
			float bx = sx + (tx - sx) * f, by = sy + (ty - sy) * f;
			float zig = i == legs ? 0f : side * (0.6f + rng.nextFloat() * 0.6f);
			side = -side;
			float wx = bx + px * zig, wy = by + py * zig;
			if (map != null && !legRunnable( map, w, h, lx, ly, wx, wy )){
				//the zig would cross something: straight on instead, or stop short of it
				wx = bx;
				wy = by;
				if (!legRunnable( map, w, h, lx, ly, wx, wy )){
					r.cover = false;
					break;
				}
			}
			pts[n++] = wx;
			pts[n++] = wy;
			lx = wx;
			ly = wy;
		}
		if (n == 0){
			//hemmed in: a startled jump on the spot, and it fades there
			r.pts = new float[]{ sx + dx * 0.3f, sy + dy * 0.3f };
		} else {
			r.pts = Arrays.copyOf( pts, n );
		}
		return r;
	}

	//every half cell along the leg is ground a hare can cross
	private static boolean legRunnable( int[] map, int w, int h, float ax, float ay, float bx, float by ){
		int steps = Math.max( 1, (int)Math.ceil( Math.hypot( bx - ax, by - ay ) * 2 ) );
		for (int i = 1; i <= steps; i++){
			float f = i / (float)steps;
			int cx = (int)Math.floor( ax + (bx - ax) * f ), cy = (int)Math.floor( ay + (by - ay) * f );
			if (cx < 1 || cy < 1 || cx > w - 2 || cy > h - 2 || !runnable( map[cx + cy * w] )) return false;
		}
		return true;
	}

	// --------------------------------------------------------- debug scenes

	/**
	 * Debug scenes: the nearest place to the world's origin where a meadow, a river and a
	 * wood meet, sampled like WindowGenerator.findRockyEdge at the given seasonal shift - and
	 * at a cold snap's and a heat wave's either side of it, since the live window adds the
	 * weather's band to the calendar (OverworldLevel.refreshSeasonShift). The landing is the
	 * meadow sample nearest the river, out of the town and every village. Null when there is
	 * none within 3000 cells.
	 */
	public static int[] findCritterGround( long seed, float shift ){
		WorldModel.Sample s = new WorldModel.Sample();
		final int step = 120, rings = 25;
		for (int r = 0; r <= rings; r++){
			int[] best = null;
			long bestDist = Long.MAX_VALUE;
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int[] land = critterGroundAt( seed, i * step, j * step, shift, s );
					if (land == null) continue;
					long d = (long)land[0] * land[0] + (long)land[1] * land[1];
					if (d < bestDist){
						bestDist = d;
						best = land;
					}
				}
			}
			if (best != null) return best;
		}
		return null;
	}

	//one candidate: 7x7 samples six cells apart around (cx, cy). It takes a dozen of meadow,
	//four of woodland and three of open river water - each the same at the shift and a
	//climate band either side - and lands on the meadow sample nearest the water, one sample
	//step (six cells) from it at most
	static int[] critterGroundAt( long seed, int cx, int cy, float shift, WorldModel.Sample s ){
		final int MEADOW = 1, FOREST = 2, WATER = 3;
		int[] kind = new int[49];
		float[] shifts = { shift, shift - WorldModel.CLIMATE_SHIFT, shift + WorldModel.CLIMATE_SHIFT };
		for (int pass = 0; pass < shifts.length; pass++){
			int meadow = 0, forest = 0, water = 0;
			for (int k = 0; k < 49; k++){
				if (pass > 0 && kind[k] == 0) continue;
				int wx = cx + (k % 7) * 6 - 18, wy = cy + (k / 7) * 6 - 18;
				WorldModel.sample( seed, wx, wy, shifts[pass], s );
				int here = s.biome == WorldModel.Biome.MEADOW ? MEADOW
						: s.biome == WorldModel.Biome.FOREST ? FOREST
						: s.biome == WorldModel.Biome.RIVER && WorldModel.wildTerrain( seed, wx, wy, s ) == Terrain.WATER ? WATER
						: 0;
				//a sample counts only for what it is at every shift
				kind[k] = pass == 0 || kind[k] == here ? here : 0;
				if (kind[k] == MEADOW) meadow++;
				else if (kind[k] == FOREST) forest++;
				else if (kind[k] == WATER) water++;
			}
			if (meadow < 12 || forest < 4 || water < 3) return null;
		}
		int meadow = 0, water = 0;
		int[] mx = new int[49], my = new int[49], rx = new int[49], ry = new int[49];
		for (int k = 0; k < 49; k++){
			int wx = cx + (k % 7) * 6 - 18, wy = cy + (k / 7) * 6 - 18;
			if (kind[k] == MEADOW){
				mx[meadow] = wx;
				my[meadow] = wy;
				meadow++;
			} else if (kind[k] == WATER){
				rx[water] = wx;
				ry[water] = wy;
				water++;
			}
		}
		int[] best = null;
		int bestD = 7;
		for (int k = 0; k < meadow; k++){
			if (WorldStructures.townCell( mx[k], my[k] ) != -1
					|| OverworldFauna.nearSettlement( seed, mx[k], my[k] )) continue;
			int d = Integer.MAX_VALUE;
			for (int r = 0; r < water; r++){
				d = Math.min( d, Math.max( Math.abs( mx[k] - rx[r] ), Math.abs( my[k] - ry[r] ) ) );
			}
			if (d < bestD){
				bestD = d;
				best = new int[]{ mx[k], my[k] };
			}
		}
		return best;
	}

	/** Debug scenes: a full set at once around the hero, every gate but the ground's waived. Render thread. */
	public static String showcase( boolean night ){
		Field f = current;
		if (f == null || Dungeon.level != f.level || Dungeon.hero == null || Dungeon.hero.sprite == null){
			return "no critters on this level";
		}
		return f.showcase( night );
	}

	// ------------------------------------------------------------ the noise

	//the field showing now: the noise goes nowhere without one
	private static volatile Field current;

	//fights seen since the last frame, as world cells packed like OverworldLevel.worldKey
	//(a rebase between the fight and the frame changes nothing): written by whichever
	//thread resolved the attack, drained by the Field
	private static final ArrayList<Long> NOISES = new ArrayList<>();

	/** Char.attack: a fight in sight scatters the birds and the hares around it - on a slice the
	 *  bats and the marmots. Any thread. */
	public static void noise( int cell ){
		Level l = Dungeon.level;
		if (current == null || !(l instanceof OverworldLevel)) return;
		OverworldLevel ow = (OverworldLevel) l;
		long key = ((long)(ow.worldY() + cell / ow.width()) << 32) | ((ow.worldX() + cell % ow.width()) & 0xFFFFFFFFL);
		synchronized (NOISES){
			if (NOISES.size() < 16) NOISES.add( key );
		}
	}

	// ------------------------------------------------------------ the field

	/** Birds that came together and leave together; an owl is a flock of one. */
	static final class Flock {
		final Species species;
		final ArrayList<CritterSprite.Bird> birds = new ArrayList<>();
		boolean scared;

		Flock( Species species ){
			this.species = species;
		}

		boolean gone(){
			for (CritterSprite.Bird b : birds) if (b.exists) return false;
			return true;
		}

		float cx(){
			float x = 0f;
			int n = 0;
			for (CritterSprite.Bird b : birds) if (b.exists){ x += b.gx(); n++; }
			return n == 0 ? 0f : x / n;
		}

		float cy(){
			float y = 0f;
			int n = 0;
			for (CritterSprite.Bird b : birds) if (b.exists){ y += b.gy(); n++; }
			return n == 0 ? 0f : y / n;
		}

		//the nearest bird still on the ground; one still gliding in counts where it will stand
		float nearest( float px, float py ){
			float best = Float.MAX_VALUE;
			for (CritterSprite.Bird b : birds){
				if (!b.exists || !b.grounded()) continue;
				best = Math.min( best, b.landing() ? cheb( b.ax, b.ay, px, py ) : cheb( b.gx(), b.gy(), px, py ) );
			}
			return best;
		}

		void wary( boolean on ){
			for (CritterSprite.Bird b : birds) if (b.exists) b.wary( on );
		}

		//all of them, away from (sx, sy) on headings a little apart, the nearest first
		void scare( final float sx, final float sy ){
			if (scared) return;
			scared = true;
			float cx = cx(), cy = cy();
			float away = Math.abs( cx - sx ) + Math.abs( cy - sy ) < 2f
					? rf( 0f, 6.2832f ) : (float)Math.atan2( cy - sy, cx - sx );
			ArrayList<CritterSprite.Bird> order = new ArrayList<>( birds );
			Collections.sort( order, (a, b) -> Float.compare(
					cheb( a.gx(), a.gy(), sx, sy ), cheb( b.gx(), b.gy(), sx, sy ) ) );
			float delay = 0f;
			for (CritterSprite.Bird b : order){
				if (!b.exists) continue;
				b.takeOff( away + rf( -0.45f, 0.45f ), delay );
				delay += rf( 0.03f, 0.10f );
			}
		}
	}

	private static CritterSprite.Bird.Kind kindOf( Species s ){
		switch (s){
			case GULL:  return CritterSprite.Bird.Kind.GULL;
			case FINCH: return CritterSprite.Bird.Kind.FINCH;
			case OWL:   return CritterSprite.Bird.Kind.OWL;
			default:    return CritterSprite.Bird.Kind.CROW;
		}
	}

	/**
	 * The critters around the hero, on the render thread. OverworldLevel.addVisuals puts
	 * one among the level's visuals (it has no place of its own to slide); it puts the
	 * critters in the scene's effects group, which every rebase slides. Twice a second it
	 * tries a handful of random cells around the hero for one kind of critter, in turn;
	 * every frame it watches the hero (and four times a second everyone else) come near
	 * the ones already out, and at each of his steps it stirs the night's fireflies.
	 *
	 * The level's map is read here, off the actor thread, only while it is the map the
	 * pictures stand on: a host's rebase changes the window (actor thread) a moment before
	 * the scene slides (render thread), and in between nothing new is placed. The slide is
	 * seen through a sentinel kept in the effects group - it moves with every other world
	 * visual - and a network mirror, whose window is re-labelled without a slide, moves its
	 * critters with the ground by hand.
	 */
	static final class Field extends Gizmo {

		final OverworldLevel level;
		private final Visual sentinel = new Visual( 0, 0, 0, 0 );
		//the world cell drawn at the scene's cell (0,0): the window origin the pictures know
		int visX, visY;
		final ArrayList<CritterSprite> live = new ArrayList<>();
		//a slice's own life, which this field runs in place of the surface's (null on the surface)
		final SliceLife life;
		private final ArrayList<Flock> flocks = new ArrayList<>();
		private final int[] alive = new int[CAP.length];
		private float thinkIn = 0.5f, charsIn = 0f, fishIn = 1.5f, owlRest = 0f;
		private int turn, forcedFish, biomeBudget;
		//the hero's last cell and the way he last stepped: nothing turns up on his path
		private int lastCell = -1, headX, headY;
		//the cell he last stirred fireflies from
		private int fireflyCell = -1;

		Field( OverworldLevel level ){
			this.level = level;
			visX = level.worldX();
			visY = level.worldY();
			sentinel.visible = false;
			sentinel.active = false;
			synchronized (NOISES){
				NOISES.clear();
			}
			current = this;
			life = SliceLife.of( this, level );
		}

		@Override
		public void destroy(){
			super.destroy();
			if (current == this) current = null;
			if (life != null) life.destroy();
			live.clear();
			flocks.clear();
		}

		@Override
		public void update(){
			if (Dungeon.level != level || Dungeon.hero == null || Dungeon.hero.sprite == null) return;
			long tm = LagMonitor.begin();
			if (sentinel.parent == null) GameScene.effect( sentinel );
			if (sentinel.x != 0f || sentinel.y != 0f){
				visX -= Math.round( sentinel.x / DungeonTilemap.SIZE );
				visY -= Math.round( sentinel.y / DungeonTilemap.SIZE );
				sentinel.x = sentinel.y = 0f;
				//his last cell is the old frame's: read against the new one it would turn his
				//heading round. he keeps the heading he had until his next step is seen
				lastCell = -1;
				if (life != null) life.rebased();
			}
			//the scene's groups update in order and this one comes before the effects: a slide
			//this frame has moved the hero's sprite already, but the critters' own updates have
			//not yet carried it into their ground points. carry it now, before anything measures
			for (CritterSprite c : live) c.follow();
			//a network mirror re-labels its window without sliding the scene: the ground moved
			//under the pictures, so they move with the ground. the hero's sprite may be set down
			//in either frame this once, so nothing is measured against it until the next
			boolean slid = false;
			if (NetManager.isNetClient() && (level.worldX() != visX || level.worldY() != visY)){
				int dx = level.worldX() - visX, dy = level.worldY() - visY;
				for (CritterSprite c : live){
					c.ax -= dx * DungeonTilemap.SIZE;
					c.ay -= dy * DungeonTilemap.SIZE;
					c.resync();
				}
				visX = level.worldX();
				visY = level.worldY();
				lastCell = -1;
				slid = true;
				if (life != null){
					life.slide( -dx * DungeonTilemap.SIZE, -dy * DungeonTilemap.SIZE );
					life.rebased();
				}
			}
			prune();
			if (!Emitter.freezeEmitters && !slid){
				float dt = Game.elapsed;
				CharSprite hs = Dungeon.hero.sprite;
				float hx = hs.x + hs.width() / 2f, hy = hs.y + hs.height() - 2f;
				if (life != null){
					//a slice's own life: what lives in the caves, or on the peaks
					hear();
					life.react( hx, hy, true );
					charsIn -= dt;
					if (charsIn <= 0f && frameAgrees()){
						charsIn = 0.25f;
						for (CharSprite s : others()) life.react( s.x + s.width() / 2f, s.y + s.height() - 2f, false );
					}
					life.update( dt, hx, hy );
					LagMonitor.end( "OW critters", tm );
					return;
				}
				hear();
				react( hx, hy, true );
				//everyone else four times a second - but not while a rebase is between the
				//window and the scene: a mob put down by populate() stands in the new frame
				//until the slide, 32 cells off, and would scare birds it is nowhere near
				charsIn -= dt;
				if (charsIn <= 0f && frameAgrees()){
					charsIn = 0.25f;
					if (!live.isEmpty()){
						for (CharSprite s : others()){
							react( s.x + s.width() / 2f, s.y + s.height() - 2f, false );
						}
					}
				}
				//the night's fireflies, a few stirred by each of his steps (OverworldFauna.fireflies)
				int pos = Dungeon.hero.pos;
				if (pos != fireflyCell && frameAgrees()){
					fireflyCell = pos;
					OverworldFauna.fireflies( level, pos, RNG );
				}
				for (CritterSprite c : live){
					if (cheb( c.gx(), c.gy(), hx, hy ) > DESPAWN_PX) c.leave( 0.4f );
				}
				owlRest -= dt;
				if ((thinkIn -= dt) <= 0f){
					thinkIn = rf( 0.4f, 0.6f );
					think( hx, hy );
				}
				if ((fishIn -= dt) <= 0f) fishIn = leap( hx, hy );
			}
			LagMonitor.end( "OW critters", tm );
		}

		//in front of every effect already out: the group fills empty slots first, so without
		//this a new hare could sort behind an old one, and a flock's nearer birds (added
		//top to bottom) behind its farther ones
		void add( CritterSprite c ){
			GameScene.effect( c );
			if (c.parent != null) c.parent.bringToFront( c );
			live.add( c );
		}

		private void prune(){
			for (int i = live.size() - 1; i >= 0; i--) if (!live.get( i ).exists) live.remove( i );
			for (int i = flocks.size() - 1; i >= 0; i--) if (flocks.get( i ).gone()) flocks.remove( i );
		}

		private void census(){
			Arrays.fill( alive, 0 );
			for (Flock f : flocks) if (!f.scared) alive[groupOf( f.species )]++;
			for (CritterSprite c : live){
				if (c instanceof CritterSprite.Hare && ((CritterSprite.Hare) c).sitting()) alive[HARES]++;
				else if (c instanceof CritterSprite.Butterfly && !c.leaving()) alive[BUTTERFLIES]++;
				else if (c instanceof CritterSprite.Fish) alive[FISH]++;
			}
		}

		//the level's map is the one the pictures stand on: the window has not moved under
		//them, and the hero is where his sprite is drawn
		boolean frameAgrees(){
			if (level.worldX() != visX || level.worldY() != visY) return false;
			CharSprite hs = Dungeon.hero.sprite;
			int w = level.width();
			int sx = (int)((hs.x + hs.width() / 2f) / DungeonTilemap.SIZE);
			int sy = (int)((hs.y + hs.height() - 2f) / DungeonTilemap.SIZE);
			int p = Dungeon.hero.pos;
			return Math.abs( p % w - sx ) <= 2 && Math.abs( p / w - sy ) <= 2;
		}

		//fights seen since the last frame
		private void hear(){
			long[] heard;
			synchronized (NOISES){
				if (NOISES.isEmpty()) return;
				heard = new long[NOISES.size()];
				for (int i = 0; i < heard.length; i++) heard[i] = NOISES.get( i );
				NOISES.clear();
			}
			for (long k : heard){
				float px = ((int)(k & 0xFFFFFFFFL) - visX + 0.5f) * DungeonTilemap.SIZE;
				float py = ((int)(k >> 32) - visY + 0.5f) * DungeonTilemap.SIZE;
				if (life != null){
					life.noise( px, py );
					continue;
				}
				for (Flock f : flocks){
					if (!f.scared && f.nearest( px, py ) <= NOISE_BIRD_PX) f.scare( px, py );
				}
				for (CritterSprite c : live){
					if (c instanceof CritterSprite.Hare && ((CritterSprite.Hare) c).sitting()
							&& hareDistance( (CritterSprite.Hare) c, px, py ) <= NOISE_HARE_PX){
						bolt( (CritterSprite.Hare) c, px, py );
					}
				}
			}
		}

		//a hare still hopping out of cover is measured where it will sit
		private static float hareDistance( CritterSprite.Hare h, float px, float py ){
			return h.emerging() ? cheb( h.ax, h.ay, px, py ) : cheb( h.gx(), h.gy(), px, py );
		}

		//someone at (px, py): the hero (who also makes them wary) or anyone else
		private void react( float px, float py, boolean hero ){
			for (Flock f : flocks){
				if (f.scared) continue;
				float d = f.nearest( px, py );
				if (d <= SCARE_BIRD_PX) f.scare( px, py );
				else if (hero) f.wary( d <= WARY_PX );
			}
			for (CritterSprite c : live){
				if (c instanceof CritterSprite.Hare){
					CritterSprite.Hare h = (CritterSprite.Hare) c;
					if (!h.sitting()) continue;
					float d = hareDistance( h, px, py );
					if (d <= BOLT_PX) bolt( h, px, py );
					else if (hero) h.alert( d <= ALERT_PX );
				} else if (hero && c instanceof CritterSprite.Butterfly
						&& cheb( c.gx(), c.gy(), px, py ) <= STARTLE_PX){
					((CritterSprite.Butterfly) c).startle( px, py );
				}
			}
		}

		//the hare's dash, on the window's map while it is the one the pictures stand on
		private void bolt( CritterSprite.Hare h, float sx, float sy ){
			int w = level.width();
			int cx = Math.max( 1, Math.min( w - 2, (int)(h.gx() / DungeonTilemap.SIZE) ) );
			int cy = Math.max( 1, Math.min( level.height() - 2, (int)(h.gy() / DungeonTilemap.SIZE) ) );
			Route r = boltRoute( frameAgrees() ? level.map : null, w, level.height(), cx + cy * w,
					sx / DungeonTilemap.SIZE, sy / DungeonTilemap.SIZE, RNG );
			//cell coordinates to offsets from the hare's ground point (its feet are 3px below the cell's middle)
			float[] pts = new float[r.pts.length];
			for (int i = 0; i + 1 < pts.length; i += 2){
				pts[i]   = r.pts[i] * DungeonTilemap.SIZE - h.ax;
				pts[i+1] = r.pts[i+1] * DungeonTilemap.SIZE + 3f - h.ay;
			}
			h.bolt( pts, r.cover );
		}

		private void think( float hx, float hy ){
			if (!frameAgrees()) return;
			int w = level.width();
			int hcell = (int)(hx / DungeonTilemap.SIZE) + (int)(hy / DungeonTilemap.SIZE) * w;
			if (hcell != lastCell){
				if (lastCell >= 0){
					headX = Integer.signum( hcell % w - lastCell % w );
					headY = Integer.signum( hcell / w - lastCell / w );
				}
				lastCell = hcell;
			}
			Phase phase = DayNightCycle.phase();
			Sky sky = sky();
			boolean butterflies = butterflyHour( phase, GameCalendar.season(), sky, ClimateManager.localWindSpeed() );
			//the hour or the weather turned against some of them: they go, a few at a time
			for (Flock f : flocks){
				if (!f.scared && !birdHour( f.species, phase, sky ) && RNG.nextFloat() < 0.15f){
					float a = rf( 0f, 6.2832f );
					f.scare( f.cx() + (float)Math.cos( a ) * 48f, f.cy() + (float)Math.sin( a ) * 48f );
				}
			}
			for (CritterSprite c : live){
				if (c instanceof CritterSprite.Hare && ((CritterSprite.Hare) c).sitting()
						&& !hareHour( phase, sky ) && RNG.nextFloat() < 0.15f){
					float a = rf( 0f, 6.2832f );
					bolt( (CritterSprite.Hare) c, c.gx() + (float)Math.cos( a ) * 48f, c.gy() + (float)Math.sin( a ) * 48f );
				} else if (c instanceof CritterSprite.Butterfly && !butterflies){
					c.leave( 1f );
				}
			}
			if (sky == Sky.FOUL) return;
			census();
			biomeBudget = 3;
			switch (turn++ & 3){
				case 0:  spawnFlock( hcell, phase, sky, false ); break;
				case 1:  spawnHare( hcell, phase, sky, false ); break;
				case 2:  if (butterflies) spawnButterflies( hcell, false ); break;
				default: spawnOwl( hcell, phase, sky, false );
			}
		}

		//a random cell on the ring of a random radius between min and max around the hero,
		//or -1 when it falls off the window's interior
		int probe( int hcell, int min, int max ){
			int w = level.width(), h = level.height();
			int r = ri( min, max ), t = ri( -r, r );
			int dx, dy;
			switch (RNG.nextInt( 4 )){
				case 0:  dx = t;  dy = -r; break;
				case 1:  dx = r;  dy = t;  break;
				case 2:  dx = t;  dy = r;  break;
				default: dx = -r; dy = t;
			}
			int x = hcell % w + dx, y = hcell / w + dy;
			if (x < 2 || y < 2 || x > w - 3 || y > h - 3) return -1;
			return x + y * w;
		}

		boolean onScreen( int cell ){
			Camera cam = Camera.main;
			if (cam == null) return false;
			float x = (cell % level.width()) * DungeonTilemap.SIZE, y = (cell / level.width()) * DungeonTilemap.SIZE;
			return x >= cam.scroll.x + 8 && x + DungeonTilemap.SIZE <= cam.scroll.x + cam.width - 8
					&& y >= cam.scroll.y + 8 && y + DungeonTilemap.SIZE <= cam.scroll.y + cam.height - 8;
		}

		//on the line he is walking (to where he is headed, and five cells straight on)
		private boolean onPath( int cell, int hcell ){
			int w = level.width();
			float x = cell % w + 0.5f, y = cell / w + 0.5f;
			float hx = hcell % w + 0.5f, hy = hcell / w + 0.5f;
			HeroAction a = Dungeon.hero.curAction;
			if (a != null && a.dst >= 0 && a.dst < level.length()
					&& nearSegment( x, y, hx, hy, a.dst % w + 0.5f, a.dst / w + 0.5f, 1.5f )) return true;
			return (headX != 0 || headY != 0) && nearSegment( x, y, hx, hy, hx + 5 * headX, hy + 5 * headY, 1.5f );
		}

		//may a new critter turn up here? In sight and on screen (a forced showcase skips both:
		//the camera has not found the hero yet), out of the town, nobody on the cell, off his
		//path, and three cells clear of every critter already out (of where it stands or will
		//stand: a flock still gliding in is still 56px out in the air)
		boolean free( int cell, int hcell, boolean forced ){
			if (!forced && (level.heroFOV == null || !level.heroFOV[cell] || !onScreen( cell ))) return false;
			if (level.inTown( cell ) || standing( cell ) || onPath( cell, hcell )) return false;
			float px = (cell % level.width() + 0.5f) * DungeonTilemap.SIZE;
			float py = (cell / level.width() + 0.5f) * DungeonTilemap.SIZE;
			for (CritterSprite c : live){
				//the peaks' eagle is high in the sky: it stands on nothing
				if (c instanceof SliceCritterSprite.Eagle) continue;
				if (cheb( c.ax, c.ay, px, py ) < 48f) return false;
			}
			return true;
		}

		//everyone but the hero who is drawn here, by sprite - each read once, since the actor
		//thread may drop one at any moment (stasis, a chasm, a hero leaving). a network client
		//registers nobody with the scheduler: the host's mobs and the other heroes live only in
		//its level's mobs, which the host's deltas change on this thread, and whatever Actor
		//still holds there is a game the player has left
		ArrayList<CharSprite> others(){
			ArrayList<CharSprite> out = new ArrayList<>();
			if (NetManager.isNetClient()){
				for (Mob m : level.mobs){
					CharSprite s = m.sprite;
					if (s != null) out.add( s );
				}
			} else {
				for (Char ch : Actor.chars()){
					CharSprite s = ch.sprite;
					if (ch != Dungeon.hero && s != null) out.add( s );
				}
			}
			return out;
		}

		//someone stands on the cell (asked where others() looks)
		boolean standing( int cell ){
			return NetManager.isNetClient() ? level.findMob( cell ) != null : Actor.findChar( cell ) != null;
		}

		//where everyone stands, the hero first, in scene pixels (x0,y0,x1,y1...): taken once a
		//spawn attempt, so nothing turns up within its scare reach of anyone - a flock beside a
		//guard, a stall or a sleeping wolf, an owl in the oak just below the hero - only to be
		//scared off the moment it touches down. birds keep 68px and hares 84px from everyone
		//(their scare reach plus a cell)
		float[] people(){
			ArrayList<CharSprite> others = others();
			float[] pts = new float[others.size() * 2 + 2];
			CharSprite hs = Dungeon.hero.sprite;
			pts[0] = hs.x + hs.width() / 2f;
			pts[1] = hs.y + hs.height() - 2f;
			int n = 2;
			for (CharSprite s : others){
				pts[n++] = s.x + s.width() / 2f;
				pts[n++] = s.y + s.height() - 2f;
			}
			return pts;
		}

		//a cell's middle, in scene pixels, kept clear of everyone by `reach`
		boolean clearOfPeople( int cell, float[] people, float reach ){
			int w = level.width();
			return clearOfChars( (cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE,
					people, reach );
		}

		//the ground a bird of this kind lands on here: its own (groundFor) and, for the crows, the
		//villages' fields - drawn flat, crop and furrow, with no tuft for a bird to vanish in
		private boolean birdGround( Species s, int cell ){
			return groundFor( s, level.map[cell] ) || (s == Species.CROW && level.fieldAt( cell ));
		}

		//nothing tall drawn over this cell from the one below it (clearBelow): a field's flat crop is nothing
		private boolean clearBelowHere( int cell ){
			return level.fieldAt( cell + level.width() ) || clearBelow( level.map, level.width(), cell );
		}

		//a biome sample is the dear part: three a think at most
		private boolean spendBiome( boolean forced ){
			return forced || biomeBudget-- > 0;
		}

		private boolean spawnFlock( int hcell, Phase phase, Sky sky, boolean forced ){
			if (!forced && (!roomFor( Species.CROW, alive ) || RNG.nextFloat() >= 0.35f * skyFactor( sky ))) return false;
			int w = level.width();
			float[] people = people();
			for (int i = 0; i < (forced ? 60 : PROBES); i++){
				int cell = forced ? probe( hcell, 5, 8 ) : probe( hcell, 5, 10 );
				if (cell < 0 || !free( cell, hcell, forced )) continue;
				if (!birdGround( Species.CROW, cell ) && !birdGround( Species.GULL, cell ) && !birdGround( Species.FINCH, cell )) continue;
				if (!clearBelowHere( cell ) || !openAround( level.map, w, cell )) continue;
				if (!clearOfPeople( cell, people, SCARE_BIRD_PX + 16f )) continue;
				//a field asks no biome: nothing to spend on it
				if (!level.fieldAt( cell ) && !spendBiome( forced )) return false;
				Species s = flockFor( cell, phase, sky, forced );
				if (s != null && settle( s, cell, people )) return true;
			}
			return false;
		}

		//the birds that come down on this cell at this hour, or null. a village's field draws the
		//crows whatever country it was ploughed in - a foothills farm has none of its own, and a
		//field in a wood's clearing lies too far from the trees for the edge rule - at the crows'
		//hour. anywhere else the biome's own birds, in a wood only at its edge or a glade's
		private Species flockFor( int cell, Phase phase, Sky sky, boolean forced ){
			if (level.fieldAt( cell )) return birdHour( Species.CROW, phase, sky ) ? Species.CROW : null;
			WorldModel.Biome b = level.biomeAtCell( cell );
			Species s = birdFor( b, forced ? Phase.DAY : phase );
			if (s == null && forced) s = Species.CROW;
			if (s == null || !birdGround( s, cell )) return null;
			if (b == WorldModel.Biome.FOREST && !forestEdge( level.map, level.width(), level.height(), cell )) return null;
			return s;
		}

		//a flock of three to six (gulls three to five) on the cell and around it, gliding in
		//together; false when the ground there has no room for three
		private boolean settle( Species s, int cell, float[] people ){
			int w = level.width();
			int n = s == Species.GULL ? ri( 3, 5 ) : ri( 3, 6 );
			ArrayList<float[]> spots = new ArrayList<>();
			for (int k = 0; k < n * 4 && spots.size() < n; k++){
				int c = cell + ri( -1, 1 ) + ri( -1, 1 ) * w;
				if (!birdGround( s, c ) || !clearBelowHere( c ) || standing( c )) continue;
				float px = (c % w) * DungeonTilemap.SIZE + ri( 3, 13 ), py = (c / w) * DungeonTilemap.SIZE + ri( 8, 13 );
				if (!clearOfChars( px, py, people, SCARE_BIRD_PX + 16f )) continue;
				boolean crowded = false;
				for (float[] o : spots){
					if (Math.abs( o[0] - px ) < 6f && Math.abs( o[1] - py ) < 4f) crowded = true;
				}
				if (!crowded) spots.add( new float[]{ px, py } );
			}
			if (spots.size() < 3) return false;
			//the farther up the screen first, so the nearer stand in front
			Collections.sort( spots, (a, b) -> Float.compare( a[1], b[1] ) );
			Flock f = new Flock( s );
			float heading = rf( 0f, 6.2832f ), delay = 0f;
			for (float[] o : spots){
				CritterSprite.Bird b = new CritterSprite.Bird( kindOf( s ), o[0], o[1], 0f );
				b.land( heading + rf( -0.3f, 0.3f ), delay );
				delay += rf( 0.06f, 0.14f );
				add( b );
				f.birds.add( b );
			}
			flocks.add( f );
			return true;
		}

		private boolean spawnHare( int hcell, Phase phase, Sky sky, boolean forced ){
			if (!forced && (!roomFor( Species.HARE, alive ) || !hareHour( phase, sky )
					|| RNG.nextFloat() >= 0.25f * skyFactor( sky ))) return false;
			int w = level.width();
			float[] people = people();
			for (int i = 0; i < (forced ? 60 : PROBES); i++){
				int cell = forced ? probe( hcell, 6, 8 ) : probe( hcell, 6, 10 );
				if (cell < 0 || !free( cell, hcell, forced )) continue;
				if (!groundFor( Species.HARE, level.map[cell] ) || !clearBelow( level.map, w, cell )
						|| !openAround( level.map, w, cell )) continue;
				//where it will sit, its feet a little below the cell's middle
				float ax = (cell % w) * DungeonTilemap.SIZE + 8, ay = (cell / w) * DungeonTilemap.SIZE + 11;
				if (!clearOfChars( ax, ay, people, BOLT_PX + 16f )) continue;
				if (!spendBiome( forced )) return false;
				WorldModel.Biome b = level.biomeAtCell( cell );
				if (!forced && !hareHome( b )) continue;
				if (OverworldFauna.nearSettlement( level.worldSeed, visX + cell % w, visY + cell / w )) continue;
				//the winter coat only where the snow is the country's own: elsewhere a white
				//hare would pass for the bunny that roams there, and that one bites back
				boolean snowy = b == WorldModel.Biome.TUNDRA || b == WorldModel.Biome.SNOWFIELD;
				CritterSprite.Hare h = new CritterSprite.Hare( snowy, ax, ay );
				int cover = coverNextTo( level.map, w, cell );
				if (cover >= 0){
					h.emerge( (cover % w) * DungeonTilemap.SIZE + 8 - ax, (cover / w) * DungeonTilemap.SIZE + 11 - ay );
				} else {
					h.appear();
				}
				add( h );
				return true;
			}
			return false;
		}

		private int spawnButterflies( int hcell, boolean forced ){
			int room = CAP[BUTTERFLIES] - alive[BUTTERFLIES];
			if (room <= 0 || (!forced && RNG.nextFloat() >= 0.6f)) return 0;
			int w = level.width();
			for (int i = 0; i < (forced ? 60 : PROBES); i++){
				int cell = forced ? probe( hcell, 2, 5 ) : probe( hcell, 2, 7 );
				if (cell < 0 || !free( cell, hcell, forced )) continue;
				int t = level.map[cell];
				if (!flutterGround( t )) continue;
				if (!forced){
					if (!spendBiome( false )) return 0;
					if (!butterflyGround( level.biomeAtCell( cell ), t )) continue;
				}
				int n = Math.min( room, forced ? 2 : ri( 1, 2 ) );
				for (int k = 0; k < n; k++){
					add( new CritterSprite.Butterfly( (cell % w) * DungeonTilemap.SIZE + ri( 2, 14 ),
							(cell / w) * DungeonTilemap.SIZE + ri( 6, 14 ) ) );
				}
				alive[BUTTERFLIES] += n;
				return n;
			}
			return 0;
		}

		private boolean spawnOwl( int hcell, Phase phase, Sky sky, boolean forced ){
			if (!forced && (owlRest > 0f || !roomFor( Species.OWL, alive ) || !birdHour( Species.OWL, phase, sky )
					|| RNG.nextFloat() >= 0.08f)) return false;
			int w = level.width();
			float[] people = people();
			for (int i = 0; i < (forced ? 150 : PROBES); i++){
				int cell = forced ? probe( hcell, 4, 14 ) : probe( hcell, 4, 9 );
				if (cell < 0 || level.map[cell] != Terrain.TREE_OAK) continue;
				//the perch is the crown, drawn over the cell above the trunk: the owl sits a row
				//nearer to a hero below it than the trunk stands, so it is measured where it sits
				int crown = cell - w;
				if (!crownDrawn( level.map[crown] ) || level.inTown( crown ) || !free( cell, hcell, forced )) continue;
				float ax = (cell % w) * DungeonTilemap.SIZE + 8, ay = (crown / w) * DungeonTilemap.SIZE + 11;
				if (!clearOfChars( ax, ay, people, SCARE_BIRD_PX + 16f )) continue;
				if (OverworldFauna.nearSettlement( level.worldSeed, visX + cell % w, visY + cell / w )) continue;
				if (!spendBiome( forced )) return false;
				if (!forced && level.biomeAtCell( cell ) != WorldModel.Biome.FOREST) continue;
				CritterSprite.Bird owl = new CritterSprite.Bird( CritterSprite.Bird.Kind.OWL, ax, ay, 13f );
				owl.land( rf( 0f, 6.2832f ), 0f );
				Flock f = new Flock( Species.OWL );
				f.birds.add( owl );
				flocks.add( f );
				add( owl );
				owlRest = 40f;
				return true;
			}
			return false;
		}

		//a fish out of the water near the hero; the seconds until the next try
		private float leap( float hx, float hy ){
			boolean forced = forcedFish > 0;
			float rate = forced ? 2f : fishRate( DayNightCycle.phase(), sky() );
			if (rate <= 0f) return 2f;
			census();
			if (alive[FISH] >= CAP[FISH] || !frameAgrees()) return 0.5f;
			int w = level.width(), h = level.height();
			int hcell = (int)(hx / DungeonTilemap.SIZE) + (int)(hy / DungeonTilemap.SIZE) * w;
			for (int i = 0; i < (forced ? 40 : 8); i++){
				int cell = probe( hcell, 2, forced ? 14 : 10 );
				if (cell < 0 || !fishWater( level.map, w, h, cell ) || level.frozenAt( cell )) continue;
				if (!forced && (level.heroFOV == null || !level.heroFOV[cell] || !onScreen( cell ))) continue;
				if (standing( cell )) continue;
				//across the water, never toward the bank
				boolean left = liquid( level.map[cell - 1] ), right = liquid( level.map[cell + 1] );
				int dir = left && right ? (RNG.nextBoolean() ? 1 : -1) : right ? 1 : -1;
				boolean river = level.biomeAtCell( cell ) != WorldModel.Biome.OCEAN && RNG.nextFloat() < 0.6f;
				add( new CritterSprite.Fish( river, (cell % w) * DungeonTilemap.SIZE + 8 + rf( -3f, 3f ),
						(cell / w) * DungeonTilemap.SIZE + 10, dir ) );
				if (forced) forcedFish--;
				break;
			}
			return forced ? rf( 0.6f, 1.2f ) : rf( 1.2f, 3.5f ) / rate;
		}

		String showcase( boolean night ){
			if (life != null) return life.showcase();
			int hcell = Dungeon.hero.pos;
			lastCell = hcell;
			headX = headY = 0;
			census();
			String made;
			if (night){
				boolean owl = spawnOwl( hcell, Phase.NIGHT, Sky.CLEAR, true );
				OverworldFauna.scatterFireflies( level, hcell, 10, RNG );
				made = (owl ? "an owl in an oak" : "no oak in reach for an owl") + ", fireflies";
			} else {
				int f = 0, h = 0, b = 0;
				for (int k = 0; k < 2; k++){
					if (spawnFlock( hcell, Phase.DAY, Sky.CLEAR, true )) f++;
				}
				for (int k = 0; k < 2; k++){
					if (spawnHare( hcell, Phase.DAY, Sky.CLEAR, true )) h++;
				}
				for (int k = 0; k < 2; k++) b += spawnButterflies( hcell, true );
				made = f + " flocks, " + h + " hares, " + b + " butterflies";
			}
			forcedFish = 3;
			fishIn = 0.3f;
			return made + "; three fish leap from the nearest open water";
		}
	}
}
