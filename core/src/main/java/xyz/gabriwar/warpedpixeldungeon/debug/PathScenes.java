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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.MarchPlanner;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.Arrays;

/**
 * The magic map's march (levels/overworld/MarchPlanner, OverworldLevel.marchDestination) has one
 * scene, overworld-march-lake: the hero set down on the shore of a big lake (or a bay) near him - the
 * nearest whose water runs 30 to 140 cells across, with the way round it by land half again as long
 * as the straight line or more - and the waypoint set on the far shore, so the march starts at once.
 * Watch it go round instead of pacing along the near shore. The fog is back on and the window he
 * lands in is unexplored: he knows what he sees, as in play. Tapping the world pauses the march, the
 * map tag resumes it.
 */
final class PathScenes {

	private PathScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new LakeMarch()
	};

	private static final class LakeMarch implements DebugScenes.Scene {
		//set while the hero is on his way to the shore
		boolean travelling;
		//the shore he is set down on, the far shore, the way round's length and the water's width
		int[] found;

		@Override public String id(){ return "overworld-march-lake"; }
		@Override public String title(){ return "Surface: the map's march round a big lake"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive();
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.seasonShift();
			int cx = 0, cy = 0;
			if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0){
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				cx = ow.worldX + hero.pos % ow.width();
				cy = ow.worldY + hero.pos / ow.width();
			}
			final int fx = cx, fy = cy;
			DebugScenes.search( "a big lake to march round", () -> lake( seed, shift, fx, fy ), spot -> {
				if (spot == null){
					GLog.w( "Scene: no lake 30 to 140 cells across within 1500 cells with a way round it" );
					return;
				}
				found = spot;
				travelling = true;
				DebugScenes.travelThen( this, 0, spot[0], spot[1] );
			} );
		}

		private void arrive(){
			if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
				GLog.w( "Scene: could not reach the surface" );
				return;
			}
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			if (ow.isMirror()){
				GLog.w( "Scene: only the host marches" );
				return;
			}
			//the fog back on, and the window unexplored: the march walks what the hero has seen
			Dungeon.debugNoFog = false;
			Arrays.fill( ow.visited, false );
			Arrays.fill( ow.mapped, false );
			ow.setWaypoint( found[2], found[3] );
			GLog.i( "Scene: " + found[5] + " cells of water between you and the waypoint (" + found[2] + "," + found[3]
					+ ", blue on the world map), " + found[4] + " cells round by land. The march goes round it;"
					+ " tap the world to pause, the map tag resumes. (fog on: what players see)" );
		}
	}

	//the four ways across a lake a candidate looks along
	private static final int[][] AXES = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };

	/**
	 * The nearest big lake to (cx, cy), searched on rings 64 cells apart out to 1500: a water cell with
	 * 30 to 140 cells of water across it along one of four lines, land three cells past each shore, and
	 * the planner's own way between those (on the untouched world) reaching the far one at half again
	 * the straight line or more. {hero x, y, waypoint x, y, route length, water width}, or null.
	 */
	static int[] lake( long seed, float shift, int cx, int cy ){
		MarchPlanner.Ground ground = MarchPlanner.ground( seed, 0, shift );
		for (int r = 0; r <= 1500; r += 64){
			for (int dy = -r; dy <= r; dy += 64){
				for (int dx = -r; dx <= r; dx += 64){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cx + dx, y = cy + dy;
					if (WorldModel.terrainAt( seed, x, y ) != Terrain.WATER) continue;
					for (int[] a : AXES){
						int[] spot = across( seed, ground, x, y, a[0], a[1] );
						if (spot != null) return spot;
					}
				}
			}
		}
		return null;
	}

	//the shores either side of the water cell (x, y) along (ax, ay) and the march's way between them, if
	//they make a lake worth going round
	private static int[] across( long seed, MarchPlanner.Ground ground, int x, int y, int ax, int ay ){
		int back = 0, ahead = 0;
		while (back <= 140 && WorldModel.terrainAt( seed, x - ax * back, y - ay * back ) == Terrain.WATER) back++;
		while (ahead <= 140 && WorldModel.terrainAt( seed, x + ax * ahead, y + ay * ahead ) == Terrain.WATER) ahead++;
		int width = back + ahead - 1;
		if (width < 30 || width > 140) return null;
		int sx = x - ax * (back + 3), sy = y - ay * (back + 3);
		int gx = x + ax * (ahead + 3), gy = y + ay * (ahead + 3);
		if (!land( seed, sx, sy ) || !land( seed, gx, gy )) return null;
		MarchPlanner.Route route = MarchPlanner.plan( new MarchPlanner.Request( ground, MarchPlanner.LAND,
				sx, sy, gx, gy, new MarchPlanner.LongLongMap(), new MarchPlanner.LongLongMap(), MarchPlanner.BUDGET ) );
		if (route.status != MarchPlanner.Status.REACHED || !route.endsAtGoal) return null;
		float length = route.along[route.last()];
		float straight = Math.max( Math.abs( gx - sx ), Math.abs( gy - sy ) );
		if (length < 1.5f * straight || length > 8f * straight) return null;
		return new int[]{ sx, sy, gx, gy, Math.round( length ), width };
	}

	private static boolean land( long seed, int x, int y ){
		int t = WorldModel.terrainAt( seed, x, y );
		return t != Terrain.WATER && MarchPlanner.LAND.cost( t ) > 0;
	}
}
