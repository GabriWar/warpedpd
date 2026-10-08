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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.CaveGlow;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.SliceCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CaveMoteParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientPlayer;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * The life of the world's caves (the slices below the surface): bats hanging in colonies on
 * the rock faces that drop and flutter off when the hero comes near or a fight breaks out,
 * a lone one flitting through a chamber; water dripping from the roof into the pools and by
 * the walls; the fungus glowing and the crystal seams glinting, bluer and stranger the
 * deeper the slice; dust hanging in the still air, spores and embers in the deep caverns;
 * blind pale fish in the deep pools; now and then a drip, a rumble or a squeak echoing far
 * off. No hour and no weather down here.
 */
public final class CaveLife extends SliceLife {

	//caps: hanging colonies, bats flitting through, fish out at once, glows lit
	static final int COLONIES = 0, FLITTERS = 1, FISH = 2, GLOWS = 3;
	static final int[] CAP = { 2, 2, 2, 12 };
	//of the twelve glows, crystal or ore glints at most
	static final int MAX_GLINTS = 6;
	//a glow is lit within nine cells of the hero, and kept to eleven: walking about does not
	//switch the patches at the edge on and off
	static final int GLOW_RADIUS = 9, GLOW_KEEP = 11;
	//which patches and crystals glow at all: one in three, the same ones every time (glows)
	private static final long GLOW_SALT = 0x6C0B7EL;
	static final int DRIP_NONE = 0, DRIP_POOL = 1, DRIP_FLOOR = 2;
	//never more drops a second than this
	static final float MAX_DRIPS = 6f;

	//the caves' far sounds, in one place: what, how loud, how high. the ambience's own, on its
	//channel (AmbientPlayer), so the ambience volume and switch govern them
	static final int SOUND_DRIP = 0, SOUND_RUMBLE = 1, SOUND_CHIRP = 2;
	static final AmbientSound[] SOUND = { AmbientSound.DRIP, AmbientSound.RUMBLE, AmbientSound.BAT };
	private static final float[] SOUND_VOLUME = { 0.8f, 0.85f, 0.75f };
	private static final float[] SOUND_PITCH_LO = { 0.85f, 0.85f, 0.95f }, SOUND_PITCH_HI = { 1.15f, 1.05f, 1.1f };
	//a squeak's echo: a moment later, a little higher
	private static final float ECHO_DELAY = 0.14f, ECHO_RISE = 1.12f;
	//turns between two sounds at least
	static final int SOUND_TURNS = 6;

	//reach, scene pixels: a colony drops off as the hero comes within three cells, stirs at five
	//and a half; anyone else passing within two; a fight within eight
	private static final float SCARE_BAT_PX = 52f, WARY_BAT_PX = 88f, PASS_BAT_PX = 36f, NOISE_BAT_PX = 128f;

	// ------------------------------------------------------------ the rules

	/** Rock whose face shows over open ground, where a bat can hang and be seen from below. */
	static boolean batWall( int[] map, int w, int cell ){
		int t = map[cell];
		if (t != Terrain.WALL && t != Terrain.WALL_DECO) return false;
		int below = cell + w;
		return below < map.length && (Terrain.flags[map[below]] & (Terrain.PASSABLE | Terrain.LIQUID)) != 0;
	}

	//open to walk on or swim in
	private static boolean open( int t ){
		return (Terrain.flags[t] & (Terrain.PASSABLE | Terrain.LIQUID)) != 0;
	}

	/** Dry floor in a chamber, not a one-wide tunnel: six of its eight neighbours open. */
	static boolean chamberCell( int[] map, int w, int cell ){
		int x = cell % w, y = cell / w, h = map.length / w;
		if (x < 1 || y < 1 || x > w - 2 || y > h - 2) return false;
		int f = Terrain.flags[map[cell]];
		if ((f & Terrain.PASSABLE) == 0 || (f & Terrain.LIQUID) != 0) return false;
		int n = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && open( map[cell + dx + dy * w] )) n++;
			}
		}
		return n >= 6;
	}

	/** Open water deep enough for the blind fish: deep water, or the shelf within two cells of it. */
	static boolean caveFishWater( int[] map, int w, int h, int cell ){
		if (!OverworldCritters.fishWater( map, w, h, cell )) return false;
		if (map[cell] == Terrain.DEEP_WATER) return true;
		int x = cell % w, y = cell / w;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int cx = x + dx, cy = y + dy;
				if (cx >= 0 && cy >= 0 && cx < w && cy < h && map[cx + cy * w] == Terrain.DEEP_WATER) return true;
			}
		}
		return false;
	}

	/** Where a drop may fall: into a pool, or on the floor by the rock (a wall or a crystal alongside). */
	static int dripKind( int[] map, int w, int h, int cell ){
		int t = map[cell];
		if (t == Terrain.WATER || t == Terrain.DEEP_WATER) return DRIP_POOL;
		int f = Terrain.flags[t];
		if ((f & Terrain.PASSABLE) == 0 || (f & Terrain.LIQUID) != 0) return DRIP_NONE;
		int x = cell % w, y = cell / w;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int cx = x + dx, cy = y + dy;
				if (cx < 0 || cy < 0 || cx >= w || cy >= h) continue;
				int n = map[cx + cy * w];
				if (n == Terrain.WALL || n == Terrain.WALL_DECO || n == Terrain.MINE_CRYSTAL) return DRIP_FLOOR;
			}
		}
		return DRIP_NONE;
	}

	/** How wet the hero's part of the cave is, 0..1, from the water within seven cells. */
	static float wetness( int water ){
		return Math.min( 1f, water / 24f );
	}

	/** Seconds between drops: two on dry rock, a third by a big pool. */
	static float dripInterval( float wet ){
		return Math.max( 1f / MAX_DRIPS, 1f / (0.5f + 2.5f * wet) );
	}

	/** The next drop's wait, jittered by `roll` (0..1) - and still never sooner than the cap allows. */
	static float nextDrip( float wet, float roll ){
		return Math.max( 1f / MAX_DRIPS, dripInterval( wet ) * (0.6f + 0.8f * roll) );
	}

	/** The fungus's light: green near the surface, cyan below the deep line, violet in the depths. */
	public static int glowColour( int altitude ){
		return altitude > OverworldFauna.DEEP_CAVES ? 0x5CFFBE : altitude > -9 ? 0x4CD8FF : 0xA27CFF;
	}

	/** A crystal's glint, on the same bands: ice blue, deep blue, violet. */
	static int glintColour( int altitude ){
		return altitude > OverworldFauna.DEEP_CAVES ? 0x9CDCFF : altitude > -9 ? 0x7C9CFF : 0xD08CFF;
	}

	/** The light on the shards a crystal cavern's floor is strewn with (HearthLight): the glint of
	 *  the crystals they broke off, on the same bands, a little paler on the chips... */
	public static int shardColour( int altitude ){
		return altitude > OverworldFauna.DEEP_CAVES ? 0xBAE6FF : altitude > -9 ? 0xA3BAFF : 0xDEAEFF;
	}

	/** ...and deeper in the halo round them. */
	public static int shardHaloColour( int altitude ){
		return altitude > OverworldFauna.DEEP_CAVES ? 0x6ACAFF : altitude > -9 ? 0x3E6AFF : 0xB852FF;
	}

	/**
	 * Hot ground the deep air rises off in embers. A burnt patch is hot today; whatever lava,
	 * vent or magma ground the caves come to have is added here.
	 */
	static boolean hotGround( int terrain ){
		return terrain == Terrain.EMBERS;
	}

	/** Does this world cell's fungus (or crystal, or vein) glow at all? One in three, always the same. */
	static boolean glows( long seed, int wx, int wy ){
		return Math.floorMod( WindowGenerator.dressHash( seed, wx, wy, GLOW_SALT ), 3L ) == 0;
	}

	/**
	 * The cells to light around the hero, nearest first: fungus patches and anything that
	 * glints (`glint` != 0) among the `eligible`, the hero has seen. The ones `lit` already
	 * keep their place out to GLOW_KEEP; new ones are taken within GLOW_RADIUS, while there
	 * is room - twelve at most, six of them glints.
	 */
	static int[] pickGlows( int[] map, boolean[] visited, int w, int h, int hcell,
			IntPredicate eligible, IntUnaryOperator glint, IntPredicate lit ){
		int hx = hcell % w, hy = hcell / w;
		ArrayList<int[]> cand = new ArrayList<>();
		for (int y = Math.max( 1, hy - GLOW_KEEP ); y <= Math.min( h - 2, hy + GLOW_KEEP ); y++){
			for (int x = Math.max( 1, hx - GLOW_KEEP ); x <= Math.min( w - 2, hx + GLOW_KEEP ); x++){
				int c = x + y * w;
				if (!visited[c]) continue;
				boolean mushroom = map[c] == Terrain.MUSHROOM_PATCH;
				if (!mushroom && glint.applyAsInt( c ) == 0) continue;
				if (!eligible.test( c )) continue;
				int d = Math.max( Math.abs( x - hx ), Math.abs( y - hy ) );
				boolean kept = lit.test( c );
				if (!kept && d > GLOW_RADIUS) continue;
				int d2 = (x - hx) * (x - hx) + (y - hy) * (y - hy);
				cand.add( new int[]{ kept ? 0 : 1, d, d2, c, mushroom ? 0 : 1 } );
			}
		}
		Collections.sort( cand, (a, b) -> {
			for (int i = 0; i < 4; i++) if (a[i] != b[i]) return Integer.compare( a[i], b[i] );
			return 0;
		} );
		ArrayList<int[]> picked = new ArrayList<>();
		int glints = 0;
		for (int[] k : cand){
			if (picked.size() == CAP[GLOWS]) break;
			if (k[4] == 1){
				if (glints == MAX_GLINTS) continue;
				glints++;
			}
			picked.add( k );
		}
		//nearest first, kept or new
		Collections.sort( picked, (a, b) -> {
			for (int i = 1; i < 4; i++) if (a[i] != b[i]) return Integer.compare( a[i], b[i] );
			return 0;
		} );
		int[] out = new int[picked.size()];
		for (int i = 0; i < out.length; i++) out[i] = picked.get( i )[3];
		return out;
	}

	/** What a glow lit is no longer there (a crystal mined, a patch burnt), or its cell left the window. */
	static boolean outlived( int[] map, int w, int h, int cell, int terrain ){
		return cell < 0 || cell % w == 0 || cell / w == 0 || cell % w == w - 1 || cell / w >= h - 1 || map[cell] != terrain;
	}

	/** Motes let into the air a turn: two in the deep caverns, often one above them. */
	static int motes( int altitude, float roll ){
		if (altitude <= OverworldFauna.DEEP_CAVES) return 2;
		return roll < 0.6f ? 1 : 0;
	}

	/** Which far sound: drips the wetter the cave, rumbles the deeper, squeaks where the bats are. */
	static int pickSound( float roll, float wet, int altitude, boolean bats ){
		float drip = 2f + 4f * wet, rumble = altitude <= OverworldFauna.DEEP_CAVES ? 2f : 1f, chirp = bats ? 2f : 1f;
		float r = roll * (drip + rumble + chirp);
		if (r < drip) return SOUND_DRIP;
		if (r < drip + rumble) return SOUND_RUMBLE;
		return SOUND_CHIRP;
	}

	/** May a far sound be heard now: SOUND_TURNS turns since the last one. */
	static boolean soundDue( int turns, int soundTurn ){
		return turns - soundTurn >= SOUND_TURNS;
	}

	/**
	 * Turns as this machine sees them pass: one for every look that finds any clock moved - the
	 * world's turn (Dungeon.cycleTurn, which stands still under the REAL_CLOCK challenge and only
	 * reaches a spectator from the host), the actors' own clock, the hero's cell. Never runs back.
	 */
	static final class Turns {
		int count;
		private int cycle = Integer.MIN_VALUE, time = Integer.MIN_VALUE, pos = Integer.MIN_VALUE;

		void see( int cycle, int time, int pos ){
			if (cycle != this.cycle || time != this.time || pos != this.pos) count++;
			this.cycle = cycle;
			this.time = time;
			this.pos = pos;
		}
	}

	// --------------------------------------------------------- debug scenes

	/**
	 * Debug scenes: the cave chamber nearest the origin on this slice with pools (a deep one
	 * for the blind fish), fungus and crystal seams, read off the cave field (rings 48 cells
	 * apart, out to 1440). Approximate on purpose - the deep water is the pool going on down
	 * into the slice below, the surface's lakes left out - and the tests check the window.
	 * The landing is dry chamber floor nearest the middle. Null when there is none.
	 */
	public static int[] findChamber( long seed, int altitude ){
		final int R = 12, S = 2 * R + 1;
		WorldModel.CaveSample cs = new WorldModel.CaveSample();
		int[] map = new int[S * S];
		for (int r = 0; r <= 30; r++){
			int[] best = null;
			long bestD = Long.MAX_VALUE;
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int cx = i * 48, cy = j * 48;
					int[] land = chamberAt( seed, altitude, cx, cy, R, S, map, cs );
					if (land == null) continue;
					long d = (long)land[0] * land[0] + (long)land[1] * land[1];
					if (d < bestD){
						bestD = d;
						best = land;
					}
				}
			}
			if (best != null) return best;
		}
		return null;
	}

	private static int[] chamberAt( long seed, int altitude, int cx, int cy, int R, int S, int[] map, WorldModel.CaveSample cs ){
		//coarse: nine by nine samples three apart, mostly open, some water
		int open = 0, water = 0;
		for (int k = 0; k < 81; k++){
			int wx = cx + (k % 9) * 3 - R, wy = cy + (k / 9) * 3 - R;
			WorldModel.caveSample( seed, wx, wy, altitude, cs );
			if (!cs.open) continue;
			open++;
			if (WorldModel.caveTerrain( seed, wx, wy, altitude, cs ) == Terrain.WATER) water++;
		}
		if (open < 40 || water < 4) return null;
		for (int y = 0; y < S; y++){
			for (int x = 0; x < S; x++){
				int wx = cx - R + x, wy = cy - R + y;
				map[x + y * S] = WorldModel.caveTerrain( seed, wx, wy, altitude, WorldModel.caveSample( seed, wx, wy, altitude, cs ) );
			}
		}
		//the pool is deep where it goes on down into the slice below, all round
		boolean below = WorldLayers.exists( altitude - 1 );
		boolean[] deep = new boolean[S * S];
		for (int y = 1; y < S - 1 && below; y++){
			for (int x = 1; x < S - 1; x++){
				int c = x + y * S;
				if (map[c] != Terrain.WATER || !WorldModel.caveWet( seed, cx - R + x, cy - R + y, altitude - 1 )) continue;
				boolean all = true;
				for (int dy = -1; dy <= 1 && all; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (map[c + dx + dy * S] != Terrain.WATER){ all = false; break; }
					}
				}
				deep[c] = all;
			}
		}
		for (int c = 0; c < S * S; c++) if (deep[c]) map[c] = Terrain.DEEP_WATER;
		int pools = 0, fungus = 0, crystal = 0, fish = 0;
		for (int c = 0; c < S * S; c++){
			int t = map[c];
			if (t == Terrain.WATER || t == Terrain.DEEP_WATER) pools++;
			else if (t == Terrain.MUSHROOM_PATCH) fungus++;
			else if (t == Terrain.MINE_CRYSTAL) crystal++;
			if (caveFishWater( map, S, S, c )) fish++;
		}
		if (pools < 6 || fungus < 3 || crystal < 3 || fish < 1) return null;
		int bestC = -1, bestD = Integer.MAX_VALUE;
		for (int y = 2; y < S - 2; y++){
			for (int x = 2; x < S - 2; x++){
				int c = x + y * S;
				if (map[c] != Terrain.EMPTY && map[c] != Terrain.EMPTY_DECO) continue;
				if (!chamberCell( map, S, c )) continue;
				boolean wet = false;
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int t = map[c + dx + dy * S];
						if (t == Terrain.WATER || t == Terrain.DEEP_WATER) wet = true;
					}
				}
				if (wet) continue;
				int d = (x - R) * (x - R) + (y - R) * (y - R);
				if (d < bestD){
					bestD = d;
					bestC = c;
				}
			}
		}
		return bestC < 0 ? null : new int[]{ cx - R + bestC % S, cy - R + bestC / S };
	}

	// ------------------------------------------------------------ the life

	/** Bats that came to hang together and leave together. */
	static final class Colony {
		final ArrayList<SliceCritterSprite.Bat> bats = new ArrayList<>();
		boolean scared;

		boolean gone(){
			for (SliceCritterSprite.Bat b : bats) if (b.exists) return false;
			return true;
		}

		//the nearest bat still on the rock
		float nearest( float px, float py ){
			float best = Float.MAX_VALUE;
			for (SliceCritterSprite.Bat b : bats){
				if (b.exists && b.hanging()) best = Math.min( best, OverworldCritters.cheb( b.ax, b.ay, px, py ) );
			}
			return best;
		}

		void wary( boolean on ){
			for (SliceCritterSprite.Bat b : bats) if (b.exists) b.wary( on );
		}

		//all of them off the rock, the nearest first, out along the wall away from (sx, sy)
		void scare( final float sx, final float sy ){
			if (scared) return;
			scared = true;
			float cx = 0f, cy = 0f;
			int n = 0;
			for (SliceCritterSprite.Bat b : bats) if (b.exists){ cx += b.ax; cy += b.ay; n++; }
			if (n == 0) return;
			cx /= n;
			cy /= n;
			float away = Math.abs( cx - sx ) + Math.abs( cy - sy ) < 2f
					? OverworldCritters.rf( 0f, 6.2832f ) : (float)Math.atan2( cy - sy, cx - sx );
			//they hang on a face with rock behind them: straight away would be into the stone,
			//so they spill out along it and down into the chamber's air
			if (Math.sin( away ) < 0.2f) away = (float)Math.atan2( 0.5f, Math.cos( away ) >= 0 ? 1f : -1f );
			ArrayList<SliceCritterSprite.Bat> order = new ArrayList<>( bats );
			Collections.sort( order, (a, b) -> Float.compare(
					OverworldCritters.cheb( a.ax, a.ay, sx, sy ), OverworldCritters.cheb( b.ax, b.ay, sx, sy ) ) );
			float delay = 0f;
			for (SliceCritterSprite.Bat b : order){
				if (!b.exists) continue;
				b.drop( away + OverworldCritters.rf( -0.6f, 0.6f ), delay );
				delay += OverworldCritters.rf( 0.04f, 0.12f );
			}
		}
	}

	//a glow and the terrain it lights: when the terrain goes, so does the glow
	private static final class Lit {
		final CaveGlow glow;
		final int terrain;

		Lit( CaveGlow glow, int terrain ){
			this.glow = glow;
			this.terrain = terrain;
		}
	}

	private final ArrayList<Colony> colonies = new ArrayList<>();
	private final ArrayList<SliceCritterSprite.Bat> flitters = new ArrayList<>();
	private final ArrayList<CritterSprite> fish = new ArrayList<>();
	//by world cell (OverworldLevel.worldKey): the glows lit, and those going out
	private final HashMap<Long, Lit> lit = new HashMap<>(), fading = new HashMap<>();
	//what the last look round found near the hero
	private int water, batWalls, fishWaters, hotN;
	private final int[] hot = new int[4];
	private float thinkIn = 0.5f, dripIn = 1f, fishIn = 2f, soundIn = 8f, echoIn = -1f, echoPitch;
	//the squeak the echo throws back
	private String echoTake;
	private int turn, forcedFish, forcedDrips;
	private int soundTurn = -SOUND_TURNS;
	private final Turns turns = new Turns();

	CaveLife( OverworldCritters.Field field, OverworldLevel level ){
		super( field, level );
	}

	private boolean allowed( Kind k ){
		return allowed( k, altitude, null, null, false, 0f );
	}

	private long keyOf( int cell ){
		int w = level.width();
		return OverworldLevel.worldKey( field.visX + cell % w, field.visY + cell / w );
	}

	@Override
	void scan( int hcell ){
		int[] map = level.map;
		int w = level.width(), h = level.height();
		int hx = hcell % w, hy = hcell / w;
		boolean embers = allowed( Kind.EMBER );
		water = batWalls = fishWaters = hotN = 0;
		for (int y = Math.max( 1, hy - 8 ); y <= Math.min( h - 2, hy + 8 ); y++){
			for (int x = Math.max( 1, hx - 8 ); x <= Math.min( w - 2, hx + 8 ); x++){
				int c = x + y * w, t = map[c];
				int d = Math.max( Math.abs( x - hx ), Math.abs( y - hy ) );
				if (d <= 7 && (t == Terrain.WATER || t == Terrain.DEEP_WATER)) water++;
				if (batWall( map, w, c )) batWalls++;
				if (caveFishWater( map, w, h, c )) fishWaters++;
				if (embers && d <= 7 && hotN < hot.length && hotGround( t )) hot[hotN++] = c;
			}
		}
		glows( hcell );
	}

	//crystal, or a vein of rich ore on a face (Ores, OverworldLevel.oreGlint): its colour, 0 for nothing
	private int glintAt( int cell ){
		if (level.map[cell] == Terrain.MINE_CRYSTAL) return glintColour( altitude );
		return level.oreGlint( cell );
	}

	private void glows( int hcell ){
		final int w = level.width();
		final int[] map = level.map;
		final long seed = level.worldSeed();
		final int vx = field.visX, vy = field.visY;
		for (Iterator<Lit> it = fading.values().iterator(); it.hasNext(); ){
			if (it.next().glow.parent == null) it.remove();
		}
		int[] pick = pickGlows( map, level.visited, w, level.height(), hcell,
				c -> glows( seed, vx + c % w, vy + c / w ), this::glintAt,
				c -> lit.containsKey( OverworldLevel.worldKey( vx + c % w, vy + c / w ) ) );
		HashSet<Long> keep = new HashSet<>();
		for (int c : pick){
			long key = keyOf( c );
			keep.add( key );
			if (lit.containsKey( key )) continue;
			Lit l = fading.remove( key );
			if (l != null && l.glow.parent != null){
				l.glow.target( 1f );
				lit.put( key, l );
				continue;
			}
			int t = map[c];
			int kind = t == Terrain.MUSHROOM_PATCH ? CaveGlow.MUSHROOM : t == Terrain.MINE_CRYSTAL ? CaveGlow.CRYSTAL : CaveGlow.GEM;
			//a crystal left green or red by a crystal mine's visit (DungeonTileSheet's alts) is no
			//blue crystal: no blue facets on it, and its light is white
			boolean blue = kind == CaveGlow.CRYSTAL && DungeonTileSheet.getVisualWithAlts(
					DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_1, c ) == DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_1;
			int colour = kind == CaveGlow.MUSHROOM ? glowColour( altitude )
					: kind == CaveGlow.CRYSTAL && !blue ? 0xFFFFFF : glintAt( c );
			CaveGlow g = new CaveGlow( kind, colour, (c % w) * DungeonTilemap.SIZE, (c / w) * DungeonTilemap.SIZE, blue );
			g.show();
			g.target( 1f );
			lit.put( key, new Lit( g, t ) );
		}
		for (Iterator<Map.Entry<Long, Lit>> it = lit.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, Lit> e = it.next();
			if (keep.contains( e.getKey() )) continue;
			e.getValue().glow.putOut();
			fading.put( e.getKey(), e.getValue() );
			it.remove();
		}
	}

	//the frame's look at what the glows light: a crystal mined or a patch burnt goes dark at once
	private void checkGlows( HashMap<Long, Lit> glows ){
		int[] map = level.map;
		int w = level.width(), h = level.height();
		for (Iterator<Map.Entry<Long, Lit>> it = glows.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, Lit> e = it.next();
			//a glow that faded out has already taken itself off the scene: never twice
			if (e.getValue().glow.parent == null){
				it.remove();
				continue;
			}
			long k = e.getKey();
			int cell = level.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) );
			if (!outlived( map, w, h, cell, e.getValue().terrain )) continue;
			e.getValue().glow.extinguish();
			it.remove();
		}
	}

	private int census( int group ){
		int n = 0;
		if (group == COLONIES){
			for (Colony c : colonies) if (!c.scared && !c.gone()) n++;
		} else if (group == FLITTERS){
			for (CritterSprite c : flitters) if (c.exists) n++;
		} else {
			for (CritterSprite c : fish) if (c.exists) n++;
		}
		return n;
	}

	@Override
	void tick( float dt, float hx, float hy ){
		thin( flitters, hx, hy );
		thin( fish, hx, hy );
		for (int i = colonies.size() - 1; i >= 0; i--){
			Colony c = colonies.get( i );
			thin( c.bats, hx, hy );
			if (c.gone()) colonies.remove( i );
		}
		boolean agrees = field.frameAgrees();
		if (agrees){
			checkGlows( lit );
			checkGlows( fading );
		}
		int w = level.width();
		int hcell = (int)(hx / DungeonTilemap.SIZE) + (int)(hy / DungeonTilemap.SIZE) * w;
		if ((thinkIn -= dt) <= 0f){
			thinkIn = OverworldCritters.rf( 0.4f, 0.6f );
			if (agrees) think( hcell );
		}
		if ((fishIn -= dt) <= 0f){
			boolean forced = forcedFish > 0;
			fishIn = forced ? OverworldCritters.rf( 0.6f, 1.2f ) : OverworldCritters.rf( 2f, 5f );
			if (agrees) spawnFish( hcell, forced );
		}
		if ((dripIn -= dt) <= 0f){
			dripIn = forcedDrips > 0 ? 0.25f : nextDrip( wetness( water ), OverworldCritters.RNG.nextFloat() );
			if (agrees) drip( hcell );
		}
		sound( dt );
	}

	private void think( int hcell ){
		switch (turn++ & 3){
			case 0:
				if (census( COLONIES ) < CAP[COLONIES] && batWalls > 0 && OverworldCritters.RNG.nextFloat() < 0.3f){
					spawnColony( hcell, false );
				}
				break;
			case 1:
				if (census( FLITTERS ) < CAP[FLITTERS] && OverworldCritters.RNG.nextFloat() < 0.12f){
					spawnFlitter( hcell, false );
				}
				break;
			case 2:
				motes( hcell );
				break;
			default:
				embers();
		}
	}

	private boolean seen( int cell ){
		return level.heroFOV != null && level.heroFOV[cell];
	}

	//a colony of three to six on a rock face in sight, side by side along it
	private boolean spawnColony( int hcell, boolean forced ){
		int w = level.width();
		int[] map = level.map;
		float[] people = field.people();
		for (int i = 0; i < (forced ? 150 : PROBES); i++){
			int cell = field.probe( hcell, forced ? 2 : 4, forced ? 10 : 8 );
			if (cell < 0 || !batWall( map, w, cell ) || !field.free( cell, hcell, forced )) continue;
			if (!forced && !seen( cell + w )) continue;
			ArrayList<Integer> faces = new ArrayList<>();
			faces.add( cell );
			if (batWall( map, w, cell - 1 )) faces.add( cell - 1 );
			if (batWall( map, w, cell + 1 )) faces.add( cell + 1 );
			int n = forced ? 5 : OverworldCritters.ri( 3, 6 );
			ArrayList<float[]> spots = new ArrayList<>();
			for (int k = 0; k < n * 4 && spots.size() < n; k++){
				int face = faces.get( OverworldCritters.RNG.nextInt( faces.size() ) );
				float px = (face % w) * DungeonTilemap.SIZE + OverworldCritters.ri( 2, 13 );
				float py = (face / w) * DungeonTilemap.SIZE + OverworldCritters.ri( 1, 4 );
				if (!OverworldCritters.clearOfChars( px, py + 10f, people, SCARE_BAT_PX + 16f )) continue;
				boolean crowded = false;
				for (float[] o : spots){
					if (Math.abs( o[0] - px ) < 4f && Math.abs( o[1] - py ) < 3f) crowded = true;
				}
				if (!crowded) spots.add( new float[]{ px, py } );
			}
			if (spots.size() < 3) continue;
			Colony col = new Colony();
			for (float[] o : spots){
				//its sight is the floor under the face, four pixels into that cell
				SliceCritterSprite.Bat b = new SliceCritterSprite.Bat( o[0], o[1], DungeonTilemap.SIZE - (o[1] % DungeonTilemap.SIZE) + 4f );
				b.appear();
				field.add( b );
				col.bats.add( b );
			}
			colonies.add( col );
			return true;
		}
		return false;
	}

	//one bat across a chamber, in and out of the dark
	private boolean spawnFlitter( int hcell, boolean forced ){
		int w = level.width();
		for (int i = 0; i < (forced ? 60 : PROBES); i++){
			int c = field.probe( hcell, 3, 7 );
			if (c < 0 || !chamberCell( level.map, w, c )) continue;
			if (!forced && (!seen( c ) || !field.onScreen( c ))) continue;
			SliceCritterSprite.Bat b = SliceCritterSprite.Bat.flitter( (c % w) * DungeonTilemap.SIZE + 8,
					(c / w) * DungeonTilemap.SIZE + 10, OverworldCritters.rf( 14f, 20f ), OverworldCritters.rf( 0f, 6.2832f ) );
			field.add( b );
			flitters.add( b );
			return true;
		}
		return false;
	}

	//a pale fish in a deep pool: gliding under the surface, or now and then a leap
	private void spawnFish( int hcell, boolean forced ){
		if ((fishWaters == 0 && !forced) || census( FISH ) >= CAP[FISH]) return;
		int w = level.width(), h = level.height();
		int[] map = level.map;
		for (int i = 0; i < (forced ? 40 : 8); i++){
			int c = field.probe( hcell, 2, forced ? 12 : 8 );
			if (c < 0 || !caveFishWater( map, w, h, c ) || field.standing( c )) continue;
			if (!forced && (!seen( c ) || !field.onScreen( c ))) continue;
			CritterSprite f;
			if (OverworldCritters.RNG.nextFloat() < 0.7f){
				f = new SliceCritterSprite.Swimmer( (c % w) * DungeonTilemap.SIZE + 8, (c / w) * DungeonTilemap.SIZE + 9,
						OverworldCritters.rf( 0f, 6.2832f ) );
			} else {
				//across the water, never toward the bank
				boolean left = OverworldCritters.liquid( map[c - 1] ), right = OverworldCritters.liquid( map[c + 1] );
				int dir = left && right ? (OverworldCritters.RNG.nextBoolean() ? 1 : -1) : right ? 1 : -1;
				f = CritterSprite.Fish.blind( (c % w) * DungeonTilemap.SIZE + 8 + OverworldCritters.rf( -3f, 3f ),
						(c / w) * DungeonTilemap.SIZE + 10, dir );
			}
			field.add( f );
			fish.add( f );
			if (forcedFish > 0) forcedFish--;
			return;
		}
	}

	//a drop from the roof: into a pool, or by the rock
	private void drip( int hcell ){
		int w = level.width(), h = level.height();
		for (int i = 0; i < 4; i++){
			int c = field.probe( hcell, 1, 7 );
			if (c < 0 || !seen( c ) || !field.onScreen( c )) continue;
			int k = dripKind( level.map, w, h, c );
			if (k == DRIP_NONE || (k == DRIP_FLOOR && OverworldCritters.RNG.nextFloat() >= 0.4f)) continue;
			float x = (c % w) * DungeonTilemap.SIZE + (k == DRIP_POOL ? OverworldCritters.ri( 3, 13 ) : OverworldCritters.ri( 2, 14 ));
			float y = (c / w) * DungeonTilemap.SIZE + (k == DRIP_POOL ? OverworldCritters.ri( 4, 13 ) : OverworldCritters.ri( 2, 7 ));
			Emitter e = emitter();
			if (e == null) return;
			e.pos( x, y );
			e.burst( DripParticle.FALL, 1 );
			if (forcedDrips > 0) forcedDrips--;
			return;
		}
	}

	//dust in a chamber's still air; spores among it in the deep caverns
	private void motes( int hcell ){
		int w = level.width();
		int n = motes( altitude, OverworldCritters.RNG.nextFloat() );
		boolean spores = allowed( Kind.SPORE );
		for (int m = 0; m < n; m++){
			for (int i = 0; i < PROBES; i++){
				int c = field.probe( hcell, 1, 6 );
				if (c < 0 || !chamberCell( level.map, w, c ) || !seen( c )) continue;
				Emitter e = emitter();
				if (e == null) return;
				e.pos( (c % w) * DungeonTilemap.SIZE, (c / w) * DungeonTilemap.SIZE, DungeonTilemap.SIZE, DungeonTilemap.SIZE );
				e.burst( spores && OverworldCritters.RNG.nextFloat() < 0.25f ? CaveMoteParticle.SPORES : CaveMoteParticle.DUST, 1 );
				break;
			}
		}
	}

	//embers rising off hot ground in the deep caverns
	private void embers(){
		int w = level.width();
		for (int i = 0; i < hotN; i++){
			int c = hot[i];
			if (OverworldCritters.RNG.nextFloat() >= 0.5f || !seen( c )) continue;
			Emitter e = emitter();
			if (e == null) return;
			e.pos( (c % w) * DungeonTilemap.SIZE + 2, (c / w) * DungeonTilemap.SIZE + 4, 12, 8 );
			e.burst( CaveMoteParticle.EMBERS, 1 );
		}
	}

	//a far sound now and then, never two within SOUND_TURNS turns
	private void sound( float dt ){
		turns.see( Dungeon.cycleTurn, (int)(Statistics.duration + Actor.now()), Dungeon.hero.pos );
		if (echoIn > 0f && (echoIn -= dt) <= 0f){
			AmbientPlayer.playTake( echoTake, SOUND[SOUND_CHIRP].gain, SOUND_VOLUME[SOUND_CHIRP] * 0.7f, echoPitch, 0f );
		}
		if ((soundIn -= dt) > 0f) return;
		if (!soundDue( turns.count, soundTurn )){
			soundIn = 2f;
			return;
		}
		int s = pickSound( OverworldCritters.RNG.nextFloat(), wetness( water ), altitude, !colonies.isEmpty() );
		//the take and the pitch picked here, once: a squeak's echo is that very squeak thrown back
		String take = SOUND[s].takes[OverworldCritters.RNG.nextInt( SOUND[s].takes.length )];
		float pitch = AmbientPlayer.nudge( OverworldCritters.RNG, OverworldCritters.rf( SOUND_PITCH_LO[s], SOUND_PITCH_HI[s] ) );
		AmbientPlayer.playTake( take, SOUND[s].gain, SOUND_VOLUME[s], pitch, 0f );
		if (s == SOUND_CHIRP){
			echoIn = ECHO_DELAY;
			echoTake = take;
			echoPitch = pitch * ECHO_RISE;
		}
		soundTurn = turns.count;
		soundIn = OverworldCritters.rf( 12f, 28f );
	}

	@Override
	void react( float px, float py, boolean hero ){
		for (Colony c : colonies){
			if (c.scared) continue;
			float d = c.nearest( px, py );
			if (d <= (hero ? SCARE_BAT_PX : PASS_BAT_PX)) c.scare( px, py );
			else if (hero) c.wary( d <= WARY_BAT_PX );
		}
	}

	@Override
	void noise( float px, float py ){
		for (Colony c : colonies){
			if (!c.scared && c.nearest( px, py ) <= NOISE_BAT_PX) c.scare( px, py );
		}
	}

	@Override
	void slide( float dx, float dy ){
		for (Lit l : lit.values()) l.glow.slide( dx, dy );
		for (Lit l : fading.values()) l.glow.slide( dx, dy );
	}

	@Override
	void destroy(){
		for (Lit l : lit.values()) if (l.glow.parent != null) l.glow.extinguish();
		for (Lit l : fading.values()) if (l.glow.parent != null) l.glow.extinguish();
		lit.clear();
		fading.clear();
		colonies.clear();
		flitters.clear();
		fish.clear();
	}

	@Override
	String showcase(){
		int hcell = Dungeon.hero.pos;
		scan( hcell );
		int made = 0;
		for (int k = 0; k < 2; k++) if (spawnColony( hcell, true )) made++;
		boolean flit = spawnFlitter( hcell, true );
		forcedFish = 2;
		fishIn = 0.3f;
		forcedDrips = 4;
		dripIn = 0.2f;
		soundIn = 2f;
		soundTurn = -SOUND_TURNS;
		int glints = 0;
		for (Lit l : lit.values()) if (l.glow.kind != CaveGlow.MUSHROOM) glints++;
		return made + " bat colonies on the walls" + (flit ? ", a bat flitting" : "") + ", "
				+ lit.size() + " glows (" + glints + " crystal or ore)"
				+ (fishWaters > 0 ? "; blind fish and drips in a moment" : "; drips in a moment, but no deep pool in reach for the fish");
	}
}
