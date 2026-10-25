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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WarmthBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The views of the other bands (WindowGenerator.belowLayers, Window.top) have three debug scenes in
 * the Scenes menu's "Caves and peaks" group. Each finds its spot off the render thread, sends the
 * hero there and puts the fog back on once he stands in it: what players see.
 *
 * layers-view-pit: the pit of the caves at -2 nearest the world's origin, the hero beside it. Through
 * it the floor of -3 shows, hazed dark, with the rims' shade round it.
 *
 * layers-view-water: a summer's day on a drop's lip at +1, above the most open water of the land
 * below near the origin (a river, a lake or the sea): the water seen from above, with its shores.
 *
 * layers-view-tarn: a summer's day on the surface at the foot of a cliff whose top holds a tarn
 * within sight: the water up on the rock, with its shores, seen from below.
 */
public final class ViewScenes {

	private ViewScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new PitScene(),
			new WaterScene(),
			new TarnScene()
	};

	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	//windows are searched in rings round the origin, overlapping a little
	private static final int STEP = W - 16, RINGS = 5;
	//how far round a stand the water below is counted
	private static final int REACH = 8;
	//enough water below to be worth the trip: the first ring holding a stand with this much wins
	private static final int WATER_ENOUGH = 40, TARN_ENOUGH = 4;

	// ------------------------------------------------------------------ finders

	/** Debug and tooling: the pit of a cave slice nearest the world's origin with plain floor all round
	 *  it (else with plain floor beside it), window by window outwards: {pit x, pit y, stand x, stand y}
	 *  in world cells, or null. */
	public static int[] pit( long seed, int altitude ){
		int[] beside = null;
		long besideD = Long.MAX_VALUE;
		for (int r = 0; r <= RINGS; r++){
			int[] best = null;
			long bestD = Long.MAX_VALUE;
			for (int[] o : ring( r )){
				WindowGenerator.Window w = WindowGenerator.generate( seed, altitude, o[0], o[1], 0f );
				for (int y = 2; y < H - 2; y++){
					for (int x = 2; x < W - 2; x++){
						int c = x + y * W;
						if (w.terrain[c] != Terrain.CHASM) continue;
						int stand = floorBeside( w.terrain, c );
						if (stand < 0) continue;
						long wx = o[0] + x, wy = o[1] + y, d = wx * wx + wy * wy;
						int[] found = { (int) wx, (int) wy, o[0] + stand % W, o[1] + stand / W };
						if (floorAllRound( w.terrain, c )){
							if (d < bestD){
								bestD = d;
								best = found;
							}
						} else if (d < besideD){
							besideD = d;
							beside = found;
						}
					}
				}
			}
			if (best != null) return best;
		}
		return beside;
	}

	/** Debug and tooling: a stand on a mountain slice's ground at a drop's lip with the most open water
	 *  of the land below within REACH cells (a river, a lake or the sea in the view down: water with
	 *  water beside it, not a marsh's lone pools), nearest the world's origin at the season `shift`:
	 *  {stand x, stand y, water cells in reach}, or null. */
	public static int[] waterBelow( long seed, int altitude, float shift ){
		int[] best = null;
		for (int r = 0; r <= RINGS; r++){
			for (int[] o : ring( r )){
				if (!mayLookOnWater( seed, altitude, o, shift )) continue;
				WindowGenerator.Window w = WindowGenerator.generate( seed, altitude, o[0], o[1], shift );
				if (w.below == null) continue;
				int[] water = new int[W * H];
				for (int c = W; c < W * (H - 1); c++){
					if (!viewWater( w, c )) continue;
					int beside = (viewWater( w, c - 1 ) ? 1 : 0) + (viewWater( w, c + 1 ) ? 1 : 0)
							+ (viewWater( w, c - W ) ? 1 : 0) + (viewWater( w, c + W ) ? 1 : 0);
					water[c] = beside >= 2 ? 1 : 0;
				}
				int[] sum = integral( water );
				for (int y = REACH + 1; y < H - REACH - 1; y++){
					for (int x = REACH + 1; x < W - REACH - 1; x++){
						int c = x + y * W;
						if (!plainGround( w.terrain[c] ) || !besideDrop( w.terrain, c )) continue;
						int n = boxSum( sum, x - REACH, y - REACH, x + REACH, y + REACH );
						if (best == null || n > best[2]) best = new int[]{ o[0] + x, o[1] + y, n };
					}
				}
			}
			if (best != null && best[2] >= WATER_ENOUGH) return best;
		}
		return best != null && best[2] > 0 ? best : null;
	}

	/** Debug and tooling: a stand on a slice's own ground at the foot of a cliff (open land in front of
	 *  it, not a tunnel's floor) whose top, as far as a look carries into the rock (two cells past its
	 *  face), holds open water: a tarn up there seen from below. Nearest the world's origin at the
	 *  season `shift`: {stand x, stand y, water tops in sight}, or null. */
	public static int[] tarnAbove( long seed, int altitude, float shift ){
		int[] best = null;
		for (int r = 0; r <= RINGS; r++){
			for (int[] o : ring( r )){
				if (!mayHoldTarns( seed, altitude, o )) continue;
				WindowGenerator.Window w = WindowGenerator.generate( seed, altitude, o[0], o[1], shift );
				if (w.top == null) continue;
				for (int y = 5; y < H - 5; y++){
					for (int x = 4; x < W - 4; x++){
						int c = x + y * W;
						if (!plainGround( w.terrain[c] ) || w.terrain[c - W] != Terrain.WALL || !openInFront( w.terrain, c )) continue;
						int n = 0;
						for (int dy = -3; dy <= -2; dy++){
							for (int dx = -3; dx <= 3; dx++){
								int t = c + dx + dy * W;
								if (w.top[t] != -1 && DungeonTileSheet.wallStitcheable( w.terrain[t + W] )
										&& openWater( w.topTerrain[t] )) n++;
							}
						}
						if (best == null || n > best[2]) best = new int[]{ o[0] + x, o[1] + y, n };
					}
				}
			}
			if (best != null && best[2] >= TARN_ENOUGH) return best;
		}
		return best != null && best[2] > 0 ? best : null;
	}

	//the window origins of a ring of windows round the origin (0: the one centred on it)
	private static java.util.ArrayList<int[]> ring( int r ){
		java.util.ArrayList<int[]> out = new java.util.ArrayList<>();
		for (int dy = -r; dy <= r; dy++){
			for (int dx = -r; dx <= r; dx++){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
				out.add( new int[]{ dx * STEP - W / 2, dy * STEP - H / 2 } );
			}
		}
		return out;
	}

	//a cheap look before a window is generated: ground of the slice and, below it, a water biome
	private static boolean mayLookOnWater( long seed, int altitude, int[] o, float shift ){
		WorldModel.Sample s = new WorldModel.Sample();
		boolean ground = false, water = false;
		for (int y = 4; y < H - 4 && !(ground && water); y += 8){
			for (int x = 4; x < W - 4; x += 8){
				WorldModel.sample( seed, o[0] + x, o[1] + y, shift, s );
				int b = WorldLayers.band( s.elev );
				if (b == altitude) ground = true;
				else if (b < altitude && (s.biome == WorldModel.Biome.OCEAN || s.biome == WorldModel.Biome.RIVER
						|| s.biome == WorldModel.Biome.SWAMP)) water = true;
			}
		}
		return ground && water;
	}

	//...and rock above the slice with a tarn's hollow on it
	private static boolean mayHoldTarns( long seed, int altitude, int[] o ){
		for (int y = 4; y < H - 4; y += 6){
			for (int x = 4; x < W - 4; x += 6){
				int b = WorldLayers.band( WorldModel.elevation( seed, o[0] + x, o[1] + y ) );
				if (b > altitude && WorldModel.tarnAt( seed, o[0] + x, o[1] + y, b )) return true;
			}
		}
		return false;
	}

	private static boolean openWater( int t ){
		return t == Terrain.WATER || t == Terrain.DEEP_WATER;
	}

	//open water in a mountain window's view down
	private static boolean viewWater( WindowGenerator.Window w, int c ){
		return w.below[c] != -1 && openWater( w.belowTerrain[c] );
	}

	//no rock in the three rows in front of (south of) a stand, two cells either side: the open land
	//at a cliff's foot, not a tunnel through the rock
	private static boolean openInFront( int[] terrain, int c ){
		for (int dy = 1; dy <= 3; dy++){
			for (int dx = -2; dx <= 2; dx++){
				if (DungeonTileSheet.wallStitcheable( terrain[c + dx + dy * W] )) return false;
			}
		}
		return true;
	}

	//plain floor on all eight cells round a pit
	private static boolean floorAllRound( int[] terrain, int c ){
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && !plainGround( terrain[c + dx + dy * W] )) return false;
			}
		}
		return true;
	}

	//ground a hero can stand on that is no way between the slices and no water
	private static boolean plainGround( int t ){
		return (Terrain.flags[t] & Terrain.PASSABLE) != 0 && (Terrain.flags[t] & (Terrain.LIQUID | Terrain.PIT)) == 0
				&& t != Terrain.ENTRANCE && t != Terrain.EXIT;
	}

	private static boolean besideDrop( int[] terrain, int c ){
		return terrain[c - 1] == Terrain.CHASM || terrain[c + 1] == Terrain.CHASM
				|| terrain[c - W] == Terrain.CHASM || terrain[c + W] == Terrain.CHASM;
	}

	//plain floor beside a pit to stand on, the cell south of it first (the pit then lies ahead on
	//screen), -1 for none
	private static int floorBeside( int[] terrain, int c ){
		for (int n : new int[]{ c + W, c - 1, c + 1, c - W }){
			if (plainGround( terrain[n] )) return n;
		}
		return -1;
	}

	//a summed-area table of a window-sized grid: (W+1) x (H+1)
	private static int[] integral( int[] v ){
		int[] s = new int[(W + 1) * (H + 1)];
		for (int y = 0; y < H; y++){
			int row = 0;
			for (int x = 0; x < W; x++){
				row += v[x + y * W];
				s[(x + 1) + (y + 1) * (W + 1)] = s[(x + 1) + y * (W + 1)] + row;
			}
		}
		return s;
	}

	//the sum over the cells x0..x1, y0..y1 (inclusive)
	private static int boxSum( int[] s, int x0, int y0, int x1, int y1 ){
		int w = W + 1;
		return s[(x1 + 1) + (y1 + 1) * w] - s[x0 + (y1 + 1) * w] - s[(x1 + 1) + y0 * w] + s[x0 + y0 * w];
	}

	// ------------------------------------------------------------------ scenes

	private static boolean onSlice( int altitude ){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == altitude;
	}

	//a summer's day with the rain held off: the water below is open and in the light
	private static float summerDay(){
		if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.3f )){
			GLog.w( "Scene: a real-clock run keeps its own season and hour (a frozen lake has no shore)" );
		}
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugForceStorm = false;
		return WorldModel.calendarShift();
	}

	private static final class PitScene implements DebugScenes.Scene {
		static final int ALTITUDE = -2;
		//set while the hero is on his way there, so the arrival sets the scene instead of sending him off again
		boolean travelling;
		int[] spot;
		@Override public String id(){ return "layers-view-pit"; }
		@Override public String title(){ return "Caves -2: a pit, the cavern below in sight"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSlice( ALTITUDE )){
					GLog.w( "Scene: could not reach slice " + ALTITUDE );
					return;
				}
				Dungeon.debugNoFog = false;
				GLog.i( "Scene: a pit beside you at " + spot[0] + "," + spot[1] + ": through it the floor of the caves at "
						+ (ALTITUDE - 1) + " shows, hazed dark, the rims' shade round it (fog on: what players see)" );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			DebugScenes.search( "the pit nearest the origin at " + ALTITUDE, () -> pit( seed, ALTITUDE ), found -> {
				if (found == null){
					GLog.w( "Scene: no pit near the origin at " + ALTITUDE );
					return;
				}
				spot = found;
				travelling = true;
				DebugScenes.travelThen( this, ALTITUDE, found[2], found[3] );
			} );
		}
	}

	private static final class WaterScene implements DebugScenes.Scene {
		static final int ALTITUDE = 1;
		boolean travelling;
		int[] spot;
		@Override public String id(){ return "layers-view-water"; }
		@Override public String title(){ return "Peaks +1: a lake or river seen from a drop"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSlice( ALTITUDE )){
					GLog.w( "Scene: could not reach slice " + ALTITUDE );
					return;
				}
				Dungeon.debugNoFog = false;
				Buff.prolong( hero, WarmthBuff.class, 2000f );
				GLog.i( "Scene: " + spot[2] + " cells of open water in the land below within " + REACH
						+ " cells of you, their shores drawn as underfoot (fog on: what players see)" );
				return;
			}
			final float shift = summerDay();
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			DebugScenes.search( "a drop over open water at +" + ALTITUDE, () -> waterBelow( seed, ALTITUDE, shift ), found -> {
				if (found == null){
					GLog.w( "Scene: no drop of +" + ALTITUDE + " over open water near the origin" );
					return;
				}
				spot = found;
				travelling = true;
				DebugScenes.travelThen( this, ALTITUDE, found[0], found[1] );
			} );
		}
	}

	private static final class TarnScene implements DebugScenes.Scene {
		static final int ALTITUDE = 0;
		boolean travelling;
		int[] spot;
		@Override public String id(){ return "layers-view-tarn"; }
		@Override public String title(){ return "Surface: a tarn on a cliff top, seen from below"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSlice( ALTITUDE )){
					GLog.w( "Scene: could not reach slice " + ALTITUDE );
					return;
				}
				Dungeon.debugNoFog = false;
				GLog.i( "Scene: the cliff north of you holds a tarn on its top, " + spot[2]
						+ " cells of it in sight, its shores drawn as underfoot (fog on: what players see)" );
				return;
			}
			final float shift = summerDay();
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			DebugScenes.search( "a cliff with a tarn on its top", () -> tarnAbove( seed, ALTITUDE, shift ), found -> {
				if (found == null){
					GLog.w( "Scene: no cliff with a tarn on its top near the origin" );
					return;
				}
				spot = found;
				travelling = true;
				DebugScenes.travelThen( this, ALTITUDE, found[0], found[1] );
			} );
		}
	}
}
