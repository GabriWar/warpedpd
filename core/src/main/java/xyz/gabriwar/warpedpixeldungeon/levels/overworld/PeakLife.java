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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.effects.CloudSea;
import xyz.gabriwar.warpedpixeldungeon.effects.SliceCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowPlumeParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * The life of the world's mountains (the slices above the surface): an eagle circling high
 * over the hero's part of the mountain by day, its shadow passing over the ground; marmots
 * popping up by the boulders of the alpine meadow, whistling when he comes near and diving
 * into their burrows when he comes nearer; snow streaming off the frozen ridges in a strong
 * wind; and from the high slices, the sea of clouds below, drifting with the wind. The
 * peaks keep the surface's hours and weather (OverworldCritters.sky), and are colder.
 */
public final class PeakLife extends SliceLife {

	//caps: the eagle in the sky, marmots up at once, ridges streaming snow
	static final int EAGLES = 0, MARMOTS = 1, PLUMES = 2;
	static final int[] CAP = { 1, 3, 6 };
	/** The slices from which the clouds lie below: +4 and up. */
	public static final int CLOUD_ALTITUDE = 4;
	/** The wind (m/s) that tears snow off a frozen ridge. */
	public static final float PLUME_WIND = 6f;

	//reach, scene pixels: a marmot dives as the hero comes within three cells and whistles at six,
	//anyone else passing within two; a fight within six
	private static final float DIVE_PX = 52f, WHISTLE_PX = 96f, PASS_MARMOT_PX = 36f, NOISE_MARMOT_PX = 96f;

	// ------------------------------------------------------------ the rules

	/** Alpine meadow a marmot lives on: thawed ground, a boulder beside it (north, east or west - a
	 *  boulder below would be drawn over it, see OverworldCritters.clearBelow) for its burrow. */
	static boolean marmotGround( int[] map, int w, int cell, boolean frozen ){
		if (frozen) return false;
		int t = map[cell];
		if (t != Terrain.GRASS && t != Terrain.EMPTY && t != Terrain.FLOWER_PATCH) return false;
		return OverworldCritters.clearBelow( map, w, cell ) && boulderBeside( map, w, cell ) >= 0;
	}

	/** The boulder north, east or west of the cell (in that order), or -1. */
	static int boulderBeside( int[] map, int w, int cell ){
		for (int n : new int[]{ cell - w, cell + 1, cell - 1 }){
			if (n >= 0 && n < map.length && map[n] == Terrain.BOULDER) return n;
		}
		return -1;
	}

	/** The neighbour downwind, as a cell offset: the wind blowing toward `dirDeg` (AmbientSnowParticle's
	 *  vector: x along its sine, y against its cosine). */
	static int downwind( int w, float dirDeg ){
		double rad = Math.toRadians( dirDeg );
		double s = Math.sin( rad ), c = -Math.cos( rad );
		int dx = s > 0.38 ? 1 : s < -0.38 ? -1 : 0;
		int dy = c > 0.38 ? 1 : c < -0.38 ? -1 : 0;
		return dx + dy * w;
	}

	/** A frozen ridge the wind tears snow off: rock of a higher band with open ground on its lee. */
	static boolean ridge( int[] map, int w, int h, int cell, boolean frozen, int down ){
		if (!frozen || map[cell] != Terrain.WALL) return false;
		int lee = cell + down;
		if (lee < 0 || lee >= w * h) return false;
		int t = map[lee];
		return t != Terrain.WALL && t != Terrain.WALL_DECO;
	}

	/** Where a ridge's snow is torn off, scene pixels: its crest. With the lee to the north, the
	 *  rock's top edge the cell above shows; otherwise the top of the face itself, in the cell the
	 *  hero sees - the rock behind the face is hidden from him, and so would a puff born over it be. */
	static int plumeY( int cell, int w, int down ){
		int top = (cell / w) * DungeonTilemap.SIZE;
		return down <= 1 - w ? top - 5 : top;
	}

	/** How many rows the first chasm row under `above` keeps clear of cloud: what its tile draws
	 *  (DungeonTileSheet.stitchChasmTile: a wall's face, a spill of water, a floor's lip), and
	 *  under anything at least the ground's shadow, so the cloud tucks in under the drop. */
	static int cloudLip( int above ){
		int tile = DungeonTileSheet.stitchChasmTile( above );
		if (tile == DungeonTileSheet.CHASM_WALL || tile == DungeonTileSheet.CHASM_FLOOR_SP) return 14;
		return tile == DungeonTileSheet.CHASM_WATER ? 6 : 4;
	}

	/**
	 * How the cloud lies on a chasm cell, or -1 where none does: the rows it keeps clear at the
	 * top (0 under more air, cloudLip under the ground's edge) in the low bits, and the sides
	 * where the cloud meets something standing in front of it (CUT_TOP, CUT_LEFT, CUT_RIGHT),
	 * which CloudSea frays instead of cutting square.
	 */
	static int cloudEdge( int[] map, int w, int cell ){
		if (map[cell] != Terrain.CHASM || cell < w) return -1;
		int above = map[cell - w];
		int e = above == Terrain.CHASM ? 0 : cloudLip( above ) | CloudSea.CUT_TOP;
		if (cell % w > 0 && map[cell - 1] != Terrain.CHASM) e |= CloudSea.CUT_LEFT;
		if (cell % w < w - 1 && map[cell + 1] != Terrain.CHASM) e |= CloudSea.CUT_RIGHT;
		return e;
	}

	/** The cells of the rectangle (inclusive, window cells) the sea of clouds covers, into `out`,
	 *  each one's cloudEdge into `edges`; how many. */
	static int seaCells( int[] map, int w, int h, int x0, int y0, int x1, int y1, int[] out, int[] edges ){
		int n = 0;
		for (int y = Math.max( 1, y0 ); y <= Math.min( h - 2, y1 ); y++){
			for (int x = Math.max( 1, x0 ); x <= Math.min( w - 2, x1 ); x++){
				int c = x + y * w;
				int e = cloudEdge( map, w, c );
				if (e >= 0 && n < out.length){
					edges[n] = e;
					out[n++] = c;
				}
			}
		}
		return n;
	}

	/** How thick the sea of clouds lies: none below CLOUD_ALTITUDE, more the higher, more under a cloudy sky. */
	static float cloudAlpha( int altitude, float cover ){
		if (altitude < CLOUD_ALTITUDE) return 0f;
		cover = Math.max( 0f, Math.min( 1f, cover ) );
		return Math.min( 0.75f, 0.45f + 0.05f * Math.min( 4, altitude - CLOUD_ALTITUDE ) + 0.1f * cover );
	}

	/** The deeper layer of the sea against the near one: thin under a clear sky, thick under a grey one. */
	static float deepAlpha( float cover ){
		return 0.35f + 0.5f * Math.max( 0f, Math.min( 1f, cover ) );
	}

	/** The light on the cloud tops: pink at dawn, white by day, gold then blue-grey at dusk, grey-blue at night. */
	static int cloudTint( Phase phase, float p ){
		switch (phase){
			case DAWN: return lerp( 0xFFC0B0, 0xFFFFFF, p );
			case DAY:  return 0xFFFFFF;
			case DUSK: return p < 0.6f ? lerp( 0xFFFFFF, 0xF0A890, p / 0.6f ) : lerp( 0xF0A890, 0x8A94B8, (p - 0.6f) / 0.4f );
			default:   return 0x8A94B8;
		}
	}

	private static int lerp( int a, int b, float p ){
		p = Math.max( 0f, Math.min( 1f, p ) );
		int r = Math.round( ((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * p );
		int g = Math.round( ((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * p );
		int bl = Math.round( (a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * p );
		return (r << 16) | (g << 8) | bl;
	}

	// --------------------------------------------------------- debug scenes

	/**
	 * Debug scenes: thawed alpine meadow on this slice, nearest the origin, by the band's edge
	 * (air below within a few cells) with boulders for the marmots - thawed at `shift` and a
	 * cold snap below it. Rings 64 cells apart out to 2560. The landing is meadow three to
	 * five cells from the edge. {wx, wy}, or null.
	 */
	public static int[] findMeadowEdge( long seed, int altitude, float shift ){
		WorldModel.Sample s = new WorldModel.Sample(), snap = new WorldModel.Sample();
		final int R = 8, S = 2 * R + 1;
		int[] band = new int[S * S], terr = new int[S * S];
		for (int r = 0; r <= 40; r++){
			int[] best = null;
			long bestD = Long.MAX_VALUE;
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int[] land = meadowAt( seed, altitude, shift, i * 64, j * 64, R, S, band, terr, s, snap );
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

	private static int[] meadowAt( long seed, int altitude, float shift, int cx, int cy, int R, int S,
			int[] band, int[] terr, WorldModel.Sample s, WorldModel.Sample snap ){
		WorldModel.sample( seed, cx, cy, shift, s );
		if (WorldLayers.band( s.elev ) != altitude
				|| WorldModel.alpineTemperature( s, altitude ) < WorldModel.FREEZE + 0.03f) return null;
		//a quick look first: five by five samples four apart, some of the band and some air
		int own = 0, air = 0;
		for (int k = 0; k < 25; k++){
			int b = WorldLayers.band( WorldModel.elevation( seed, cx + (k % 5) * 4 - R, cy + (k / 5) * 4 - R ) );
			if (b == altitude) own++;
			else if (b < altitude) air++;
		}
		if (own < 9 || air < 2) return null;
		own = air = 0;
		int cold = 0, boulders = 0;
		for (int y = 0; y < S; y++){
			for (int x = 0; x < S; x++){
				int k = x + y * S, wx = cx - R + x, wy = cy - R + y;
				WorldModel.sample( seed, wx, wy, shift, s );
				band[k] = WorldLayers.band( s.elev );
				terr[k] = -1;
				if (band[k] < altitude){
					air++;
				} else if (band[k] == altitude){
					own++;
					//thawed now, and through a cold snap
					if (WorldModel.alpineTemperature( s, altitude ) < WorldModel.FREEZE
							|| WorldModel.alpineTemperature( WorldModel.sample( seed, wx, wy, shift - WorldModel.CLIMATE_SHIFT, snap ),
								altitude ) < WorldModel.FREEZE) cold++;
					terr[k] = WorldModel.alpineTerrain( seed, wx, wy, altitude, s );
					if (terr[k] == Terrain.BOULDER) boulders++;
				}
			}
		}
		if (own < 90 || cold * 10 > own || air < 12 || boulders < 2) return null;
		int bestK = -1, bestD = Integer.MAX_VALUE;
		for (int k = 0; k < S * S; k++){
			int t = terr[k];
			if (t != Terrain.EMPTY && t != Terrain.GRASS && t != Terrain.FLOWER_PATCH) continue;
			int x = k % S, y = k / S, edge = Integer.MAX_VALUE;
			for (int q = 0; q < S * S; q++){
				if (band[q] < altitude) edge = Math.min( edge, Math.max( Math.abs( q % S - x ), Math.abs( q / S - y ) ) );
			}
			if (edge < 3 || edge > 5) continue;
			int d = (x - R) * (x - R) + (y - R) * (y - R);
			if (d < bestD){
				bestD = d;
				bestK = k;
			}
		}
		return bestK < 0 ? null : new int[]{ cx - R + bestK % S, cy - R + bestK / S };
	}

	/**
	 * Debug scenes: a summit of slice `want` (CLOUD_ALTITUDE or higher) near the origin, with
	 * open air all round below it - or, with none within 3840 cells, the highest summit found.
	 * From every ring sample high enough it climbs (steps of four cells) to the top of its
	 * hill, each hill once, and checks it there: nothing higher within eight cells, half the
	 * ground around below its band. {wx, wy, band} of a landing on the top, or null.
	 */
	public static int[] findPeak( long seed, int want, float shift ){
		HashSet<Long> climbed = new HashSet<>();
		WorldModel.Sample s = new WorldModel.Sample();
		int[] best = null;
		for (int r = 0; r <= 60; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int x = i * 64, y = j * 64;
					float e = WorldModel.elevation( seed, x, y );
					if (WorldLayers.band( e ) < CLOUD_ALTITUDE) continue;
					boolean fresh = true;
					while (true){
						if (!climbed.add( OverworldLevel.worldKey( x, y ) )){
							fresh = false;
							break;
						}
						int nx = x, ny = y;
						float ne = e;
						for (int d = 0; d < 8; d++){
							int sx = x + 4 * (d < 3 ? d - 1 : d == 3 ? -1 : d == 4 ? 1 : d - 6);
							int sy = y + 4 * (d < 3 ? -1 : d < 5 ? 0 : 1);
							float se = WorldModel.elevation( seed, sx, sy );
							if (se > ne){
								ne = se;
								nx = sx;
								ny = sy;
							}
						}
						if (nx == x && ny == y) break;
						x = nx;
						y = ny;
						e = ne;
					}
					if (!fresh) continue;
					int b = WorldLayers.band( e );
					if (b < CLOUD_ALTITUDE || !summit( seed, x, y, b )) continue;
					int[] land = summitLanding( seed, x, y, b, shift, s );
					if (land == null) continue;
					if (b == want) return new int[]{ land[0], land[1], b };
					if (best == null || b > best[2]) best = new int[]{ land[0], land[1], b };
				}
			}
		}
		return best;
	}

	//nothing of a higher band within eight cells, half of the seventeen-square below its band
	private static boolean summit( long seed, int x, int y, int b ){
		int lower = 0, n = 0;
		for (int dy = -8; dy <= 8; dy += 2){
			for (int dx = -8; dx <= 8; dx += 2){
				int sb = WorldLayers.band( WorldModel.elevation( seed, x + dx, y + dy ) );
				if (sb > b) return false;
				if (sb < b) lower++;
				n++;
			}
		}
		return lower * 2 >= n;
	}

	//the band's own snow, bare ground or meadow nearest the top
	private static int[] summitLanding( long seed, int x, int y, int b, float shift, WorldModel.Sample s ){
		for (int r = 0; r <= 6; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					WorldModel.sample( seed, x + dx, y + dy, shift, s );
					if (WorldLayers.band( s.elev ) != b) continue;
					int t = WorldModel.alpineTerrain( seed, x + dx, y + dy, b, s );
					if (t == Terrain.SNOW || t == Terrain.EMPTY || t == Terrain.GRASS) return new int[]{ x + dx, y + dy };
				}
			}
		}
		return null;
	}

	// ------------------------------------------------------------ the life

	private SliceCritterSprite.Eagle eagle;
	//the first eagle a little after arrival, the next a while after one leaves
	private float eagleRest = 8f;
	private final ArrayList<SliceCritterSprite.Marmot> marmots = new ArrayList<>();
	//by world cell (OverworldLevel.worldKey): the ridges streaming snow, and how fast
	private final HashMap<Long, Emitter> plumes = new HashMap<>();
	private float plumeIv;
	private CloudSea sea;
	private int[] seaBuf = new int[0], seaEdges = new int[0];
	private int seaX0 = -1, seaY0, seaX1, seaY1;
	private boolean seaStale = true;
	private int marmotCells;
	private float thinkIn = 0.5f;
	private int turn;

	PeakLife( OverworldCritters.Field field, OverworldLevel level ){
		super( field, level );
	}

	@Override
	void rebased(){
		super.rebased();
		seaStale = true;
	}

	@Override
	void scan( int hcell ){
		int[] map = level.map;
		int w = level.width(), h = level.height();
		int hx = hcell % w, hy = hcell / w;
		marmotCells = 0;
		for (int y = Math.max( 1, hy - 9 ); y <= Math.min( h - 2, hy + 9 ); y++){
			for (int x = Math.max( 1, hx - 9 ); x <= Math.min( w - 2, hx + 9 ); x++){
				int c = x + y * w;
				if (marmotGround( map, w, c, level.frozenAt( c ) )) marmotCells++;
			}
		}
		seaStale = true;
		plumes( hcell );
	}

	//snow off the frozen ridges in sight, downwind: the nearest six
	private void plumes( int hcell ){
		float wind = ClimateManager.localWindSpeed();
		//the gate for the whole slice is the wind's; ridge() asks each rock for its own frost
		if (!allowed( Kind.PLUME, altitude, DayNightCycle.phase(), OverworldCritters.sky(), true, wind )){
			stopPlumes( null );
			return;
		}
		int[] map = level.map;
		int w = level.width(), h = level.height();
		int down = downwind( w, ClimateManager.surfaceWindDir() );
		int hx = hcell % w, hy = hcell / w;
		ArrayList<int[]> found = new ArrayList<>();
		for (int y = Math.max( 1, hy - 9 ); y <= Math.min( h - 2, hy + 9 ); y++){
			for (int x = Math.max( 1, hx - 9 ); x <= Math.min( w - 2, hx + 9 ); x++){
				int c = x + y * w;
				if (level.heroFOV == null || !level.heroFOV[c] || !ridge( map, w, h, c, level.frozenAt( c ), down )) continue;
				found.add( new int[]{ Math.max( Math.abs( x - hx ), Math.abs( y - hy ) ), c } );
			}
		}
		java.util.Collections.sort( found, (a, b) -> a[0] != b[0] ? Integer.compare( a[0], b[0] ) : Integer.compare( a[1], b[1] ) );
		float iv = Math.max( 0.07f, Math.min( 0.3f, 2.4f / wind ) );
		boolean repour = Math.abs( iv - plumeIv ) > 0.02f;
		plumeIv = iv;
		HashSet<Long> keep = new HashSet<>();
		for (int k = 0; k < Math.min( CAP[PLUMES], found.size() ); k++){
			int c = found.get( k )[1];
			long key = OverworldLevel.worldKey( field.visX + c % w, field.visY + c / w );
			keep.add( key );
			Emitter e = plumes.get( key );
			if (e == null){
				e = emitter();
				if (e == null) return;
				e.pos( (c % w) * DungeonTilemap.SIZE, plumeY( c, w, down ), DungeonTilemap.SIZE, 4 );
				e.pour( SnowPlumeParticle.PLUME, iv );
				plumes.put( key, e );
			} else if (repour){
				e.pour( SnowPlumeParticle.PLUME, iv );
			}
		}
		stopPlumes( keep );
	}

	//every plume not kept goes quiet (its last puffs blow away)
	private void stopPlumes( HashSet<Long> keep ){
		for (Iterator<Map.Entry<Long, Emitter>> it = plumes.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, Emitter> e = it.next();
			if (keep != null && keep.contains( e.getKey() )) continue;
			e.getValue().on = false;
			it.remove();
		}
	}

	@Override
	void tick( float dt, float hx, float hy ){
		thin( marmots, hx, hy );
		if (eagle != null){
			if (!eagle.exists){
				eagle = null;
				eagleRest = OverworldCritters.rf( 30f, 60f );
			} else {
				eagle.drift( hx, hy, dt );
			}
		}
		eagleRest -= dt;
		boolean agrees = field.frameAgrees();
		if ((thinkIn -= dt) <= 0f){
			thinkIn = OverworldCritters.rf( 0.4f, 0.6f );
			if (agrees) think( (int)(hx / DungeonTilemap.SIZE) + (int)(hy / DungeonTilemap.SIZE) * level.width() );
		}
		clouds( agrees );
	}

	private void think( int hcell ){
		Phase phase = DayNightCycle.phase();
		OverworldCritters.Sky sky = OverworldCritters.sky();
		//the hour or the weather turned against them: they go, a few at a time
		if (eagle != null && !allowed( Kind.EAGLE, altitude, phase, sky, false, 0f )
				&& OverworldCritters.RNG.nextFloat() < 0.15f) eagle.depart();
		for (SliceCritterSprite.Marmot m : marmots){
			if (m.up() && !allowed( Kind.MARMOT, altitude, phase, sky, false, 0f )
					&& OverworldCritters.RNG.nextFloat() < 0.15f) m.dive();
		}
		if (sky == OverworldCritters.Sky.FOUL) return;
		float f = OverworldCritters.skyFactor( sky );
		if ((turn++ & 1) == 0){
			if (eagle == null && eagleRest <= 0f && allowed( Kind.EAGLE, altitude, phase, sky, false, 0f )
					&& OverworldCritters.RNG.nextFloat() < 0.25f * f) spawnEagle();
		} else if (marmotCells > 0 && up() < CAP[MARMOTS] && OverworldCritters.RNG.nextFloat() < 0.35f * f){
			spawnMarmot( hcell, phase, sky, false );
		}
	}

	private int up(){
		int n = 0;
		for (SliceCritterSprite.Marmot m : marmots) if (m.exists && m.up()) n++;
		return n;
	}

	//how far from an eyrie's nest no picture of an eagle circles (cells)
	static final int EYRIE_CLEAR = 24;

	//is this world cell within EYRIE_CLEAR of an eyrie's nest the window holds? (any thread: the
	//window's immutable list, never resolved)
	static boolean byEyrie( java.util.List<MountainSites.Site> sites, int wx, int wy ){
		for (MountainSites.Site s : sites){
			if (s.kind == MountainSites.Kind.EYRIE && Math.max( Math.abs( wx - s.wx ), Math.abs( wy - s.wy ) ) <= EYRIE_CLEAR) return true;
		}
		return false;
	}

	//the eagle's circle starts over the hero's part of the mountain
	private boolean spawnEagle(){
		if (eagle != null) return false;
		CharSprite hs = Dungeon.hero.sprite;
		float cx = hs.x + hs.width() / 2f + OverworldCritters.rf( -48f, 48f );
		float cy = hs.y + hs.height() - 2f + OverworldCritters.rf( -48f, 48f );
		//never over an eyrie: its eagles are real (MountainSites, Eagle), and a picture circling
		//their nest would pass for one of them
		if (byEyrie( level.mountainSites(), field.visX + (int) Math.floor( cx / DungeonTilemap.SIZE ),
				field.visY + (int) Math.floor( cy / DungeonTilemap.SIZE ) )) return false;
		eagle = new SliceCritterSprite.Eagle( cx, cy );
		//its shadow on the ground, under every other picture
		GameScene.effect( eagle.shadow );
		if (eagle.shadow.parent != null) eagle.shadow.parent.sendToBack( eagle.shadow );
		field.add( eagle );
		return true;
	}

	//a marmot up by a boulder in sight, on thawed meadow, out of everyone's way
	private boolean spawnMarmot( int hcell, Phase phase, OverworldCritters.Sky sky, boolean forced ){
		int[] map = level.map;
		int w = level.width();
		float[] people = field.people();
		for (int i = 0; i < (forced ? 150 : PROBES); i++){
			int cell = field.probe( hcell, 4, forced ? 12 : 9 );
			if (cell < 0 || !field.free( cell, hcell, forced )) continue;
			boolean frozen = level.frozenAt( cell );
			if (!marmotGround( map, w, cell, frozen )) continue;
			if (!forced && !allowed( Kind.MARMOT, altitude, phase, sky, frozen, 0f )) continue;
			if (!field.clearOfPeople( cell, people, DIVE_PX + 32f )) continue;
			int b = boulderBeside( map, w, cell );
			int bx = Integer.signum( b % w - cell % w ), by = Integer.signum( b / w - cell / w );
			SliceCritterSprite.Marmot m = new SliceCritterSprite.Marmot( (cell % w) * DungeonTilemap.SIZE + 8 + bx * 3,
					(cell / w) * DungeonTilemap.SIZE + 12, bx, by );
			m.peek();
			field.add( m );
			marmots.add( m );
			return true;
		}
		return false;
	}

	//the sea of clouds over the open air in view, drifting with the wind, lit by the hour
	private void clouds( boolean agrees ){
		if (!allowed( Kind.CLOUDS, altitude, null, null, false, 0f )) return;
		if (sea == null){
			if (!agrees) return;
			sea = new CloudSea();
			GameScene.floorEffect( sea );
		}
		Camera cam = Camera.main;
		if (agrees && cam != null){
			int w = level.width(), h = level.height();
			int x0 = (int)(cam.scroll.x / DungeonTilemap.SIZE) - 1, y0 = (int)(cam.scroll.y / DungeonTilemap.SIZE) - 1;
			int x1 = (int)((cam.scroll.x + cam.width) / DungeonTilemap.SIZE) + 1, y1 = (int)((cam.scroll.y + cam.height) / DungeonTilemap.SIZE) + 1;
			if (seaStale || x0 != seaX0 || y0 != seaY0 || x1 != seaX1 || y1 != seaY1){
				int need = Math.max( 0, (x1 - x0 + 1) * (y1 - y0 + 1) );
				if (seaBuf.length < need){
					seaBuf = new int[need];
					seaEdges = new int[need];
				}
				sea.cover( seaBuf, seaEdges, seaCells( level.map, w, h, x0, y0, x1, y1, seaBuf, seaEdges ), w, field.visX, field.visY );
				seaX0 = x0;
				seaY0 = y0;
				seaX1 = x1;
				seaY1 = y1;
				seaStale = false;
			}
		}
		float wind = ClimateManager.localWindSpeed();
		double rad = Math.toRadians( ClimateManager.surfaceWindDir() );
		float pace = Math.max( 1.5f, Math.min( 12f, 1.5f + 0.6f * wind ) );
		sea.vx = (float)Math.sin( rad ) * pace;
		sea.vy = -(float)Math.cos( rad ) * pace;
		Phase phase = DayNightCycle.phase();
		float cover = ClimateManager.cloudCover();
		sea.look( cloudAlpha( altitude, cover ) * (phase == Phase.NIGHT ? 0.8f : 1f), deepAlpha( cover ),
				cloudTint( phase, DayNightCycle.phaseProgress() ) );
	}

	@Override
	void react( float px, float py, boolean hero ){
		for (SliceCritterSprite.Marmot m : marmots){
			if (!m.exists || !m.up()) continue;
			float d = OverworldCritters.cheb( m.gx(), m.gy(), px, py );
			if (d <= (hero ? DIVE_PX : PASS_MARMOT_PX)) m.dive();
			else if (hero) m.alert( d <= WHISTLE_PX, px );
		}
	}

	@Override
	void noise( float px, float py ){
		for (SliceCritterSprite.Marmot m : marmots){
			if (m.exists && m.up() && OverworldCritters.cheb( m.gx(), m.gy(), px, py ) <= NOISE_MARMOT_PX) m.dive();
		}
	}

	@Override
	void slide( float dx, float dy ){
		if (sea != null) sea.slide( dx, dy );
		for (Emitter e : plumes.values()){
			e.x += dx;
			e.y += dy;
			for (Gizmo g : e.membersView()){
				if (g instanceof Visual){
					((Visual) g).x += dx;
					((Visual) g).y += dy;
				}
			}
		}
	}

	@Override
	void destroy(){
		stopPlumes( null );
		if (sea != null && sea.parent != null){
			sea.killAndErase();
			sea.destroy();
		}
		sea = null;
		marmots.clear();
		eagle = null;
	}

	@Override
	String showcase(){
		int hcell = Dungeon.hero.pos;
		scan( hcell );
		//the eagle keeps its hours even here: by day only
		boolean day = DayNightCycle.phase() == Phase.DAY;
		boolean bird = day && spawnEagle();
		int m = 0;
		for (int k = 0; k < 2; k++) if (spawnMarmot( hcell, Phase.DAY, OverworldCritters.Sky.CLEAR, true )) m++;
		clouds( true );
		String made = (bird ? "an eagle" : !day ? "no eagle before the day" : eagle != null ? "an eagle already up"
				: "no eagle by an eyrie") + ", " + m + " marmots"
				+ (sea != null ? ", clouds below" : ", no clouds below +" + CLOUD_ALTITUDE);
		return plumes.isEmpty() ? made + "; snow plumes need frozen rock and a wind of " + (int)PLUME_WIND + " or more" : made;
	}
}
