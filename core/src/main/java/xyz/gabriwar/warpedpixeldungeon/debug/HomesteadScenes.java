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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

/**
 * The homesteads' debug scenes (the Scenes menu's "Homestead" group, ids homestead-*):
 *
 * homestead-shell: on the surface, near the hero (taken there first from anywhere else), a 7x5
 * stone house with a barred door on its south side, a window in its north wall, plank floors and a
 * slate roof, and beside it a 5x4 log hut with a plain door and a thatch roof - all laid through
 * the Homestead API (Homestead.build), on the first stretch of open ground near him.
 *
 * homestead-materials: a builder's satchel (when the hero has none) and fifty of every material.
 *
 * homestead-winter-room: the clock walked on to a winter night, the hero taken to a snowfield (or
 * the tundra) and set down inside a closed 7x5 brick room (a plain door south, a window north,
 * plank floors, a tile roof); beside it a 5x4 log hut under thatch with a gap in its east wall, so
 * no room. The log tells the air outside, the brick room's and what the hut is: feel the
 * difference walking from one to the other (homestead/Rooms, Shelter).
 */
final class HomesteadScenes {

	private HomesteadScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Shell(),
			new Materials(),
			new WinterRoom()
	};

	//the house's and the hut's footprints, side by side: house 7x5, two cells of ground, hut 5x4
	static final int HOUSE_W = 7, HOUSE_H = 5, HUT_X = 9, HUT_W = 5, HUT_H = 4;
	static final int SPAN_W = HUT_X + HUT_W, SPAN_H = HOUSE_H;

	private static final class Shell implements DebugScenes.Scene {
		//set while the hero is on his way to the surface
		boolean travelling;

		@Override public String id(){ return "homestead-shell"; }
		@Override public String title(){ return "Homestead: a stone house and a log hut"; }

		@Override public void apply( Hero hero ){
			if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
				if (travelling){
					travelling = false;
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				travelling = true;
				//south of the town, off its streets
				DebugScenes.travelThen( this, 0, WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
						WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) + 40 );
				return;
			}
			travelling = false;
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			if (ow.isMirror()){
				GLog.w( "Scene: only the host can build" );
				return;
			}
			int origin = findGround( ow, hero.pos );
			if (origin == -1){
				GLog.w( "Scene: no open ground near the hero for a house; walk somewhere flatter" );
				return;
			}
			buildShell( ow, origin, hero );
			GLog.i( "Scene: a stone house (barred door south, window north, slate roof) and a log hut"
					+ " (plain door, thatch roof). Step inside: the roof over you fades." );
		}
	}

	/** The top-left window cell of the nearest open stretch (SPAN_W x SPAN_H, and a cell of
	 *  ground round it) that keeps two cells from the hero, -1 within 40 cells of none. */
	static int findGround( OverworldLevel ow, int heroPos ){
		int w = ow.width(), hx = heroPos % w, hy = heroPos / w;
		for (int r = 3; r <= 40; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int ox = hx + dx, oy = hy + dy;
					if (fits( ow, ox, oy, hx, hy )) return ox + oy * w;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int ox, int oy, int hx, int hy ){
		int w = ow.width();
		for (int y = oy - 1; y <= oy + SPAN_H; y++){
			for (int x = ox - 1; x <= ox + SPAN_W; x++){
				if (x < 2 || y < 2 || x >= w - 2 || y >= ow.height() - 2) return false;
				if (Math.abs( x - hx ) <= 1 && Math.abs( y - hy ) <= 1) return false;
				int c = x + y * w;
				int t = ow.map[c];
				if (!ow.passable[c] || ow.pit[c] || (Terrain.flags[t] & Terrain.LIQUID) != 0
						|| t == Terrain.FROZEN_WATER || t == Terrain.BRIDGE || Terrain.homestead( t )
						|| ow.protectedForBuilding( c ) || Actor.findChar( c ) != null || ow.heaps.get( c ) != null){
					return false;
				}
			}
		}
		return true;
	}

	/** The stone house and the log hut, with their top-left at a window cell. */
	static void buildShell( OverworldLevel ow, int origin, Hero hero ){
		for (Object[] step : shellPlan( ow, origin )){
			Homestead.build( ow, (Integer) step[0], BuildCatalog.byId( (String) step[1] ), hero );
		}
	}

	/** The stone house and the log hut as building steps in order, each {window cell, piece id}:
	 *  walls, door, window and floors first, then the roofs once their walls stand to hold them.
	 *  The tours (ScreenshotTour, TrailerTour) raise it the same way, the trailer a step at a time. */
	static java.util.ArrayList<Object[]> shellPlan( OverworldLevel ow, int origin ){
		java.util.ArrayList<Object[]> plan = new java.util.ArrayList<>();
		int w = ow.width();
		for (int y = 0; y < HOUSE_H; y++){
			for (int x = 0; x < HOUSE_W; x++){
				int c = origin + x + y * w;
				boolean edge = x == 0 || y == 0 || x == HOUSE_W - 1 || y == HOUSE_H - 1;
				String id = !edge ? "floor_plank"
						: x == HOUSE_W / 2 && y == HOUSE_H - 1 ? "door_barred"
						: x == HOUSE_W / 2 && y == 0 ? "window_stone"
						: "wall_stone";
				plan.add( new Object[]{ c, id } );
			}
		}
		int hut = origin + HUT_X;
		for (int y = 0; y < HUT_H; y++){
			for (int x = 0; x < HUT_W; x++){
				boolean edge = x == 0 || y == 0 || x == HUT_W - 1 || y == HUT_H - 1;
				if (!edge) continue;
				String id = x == HUT_W / 2 && y == HUT_H - 1 ? "door" : "wall_log";
				plan.add( new Object[]{ hut + x + y * w, id } );
			}
		}
		//the roofs go on once their walls stand to hold them
		for (int y = 0; y < HOUSE_H; y++){
			for (int x = 0; x < HOUSE_W; x++) plan.add( new Object[]{ origin + x + y * w, "roof_slate" } );
		}
		for (int y = 0; y < HUT_H; y++){
			for (int x = 0; x < HUT_W; x++) plan.add( new Object[]{ hut + x + y * w, "roof_thatch" } );
		}
		return plan;
	}

	private static final class WinterRoom implements DebugScenes.Scene {
		//set while the hero is on his way to the snow
		boolean travelling;

		@Override public String id(){ return "homestead-winter-room"; }
		@Override public String title(){ return "Homestead: a brick room and a leaky log hut on a winter night"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive( hero );
				return;
			}
			if (!DebugScenes.walkClock( GameCalendar.Season.WINTER, DayNightCycle.Phase.NIGHT, 0.2f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "a snowfield near the origin",
					() -> DebugScenes.biomeGround( seed, shift, WorldModel.Biome.SNOWFIELD, WorldModel.Biome.TUNDRA ),
					spot -> {
						if (spot == null){
							GLog.w( "Scene: no snowfield or tundra near the origin in this world" );
							return;
						}
						travelling = true;
						DebugScenes.travelThen( this, 0, spot[0], spot[1] );
					} );
		}

		private void arrive( Hero hero ){
			if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
				GLog.w( "Scene: could not reach the surface" );
				return;
			}
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			if (ow.isMirror()){
				GLog.w( "Scene: only the host can build" );
				return;
			}
			int origin = findGround( ow, hero.pos );
			if (origin == -1){
				GLog.w( "Scene: no open ground near the hero for a house; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			//the brick room: shut all round, a door south and a window north, under tiles
			for (int y = 0; y < HOUSE_H; y++){
				for (int x = 0; x < HOUSE_W; x++){
					boolean edge = x == 0 || y == 0 || x == HOUSE_W - 1 || y == HOUSE_H - 1;
					String id = !edge ? "floor_plank"
							: x == HOUSE_W / 2 && y == HOUSE_H - 1 ? "door"
							: x == HOUSE_W / 2 && y == 0 ? "window_brick"
							: "wall_brick";
					Homestead.build( ow, origin + x + y * w, BuildCatalog.byId( id ), hero );
				}
			}
			//the log hut, its east wall's middle left open
			int hut = origin + HUT_X;
			for (int y = 0; y < HUT_H; y++){
				for (int x = 0; x < HUT_W; x++){
					boolean edge = x == 0 || y == 0 || x == HUT_W - 1 || y == HUT_H - 1;
					if (!edge || (x == HUT_W - 1 && y == HUT_H / 2)) continue;
					String id = x == HUT_W / 2 && y == HUT_H - 1 ? "door" : "wall_log";
					Homestead.build( ow, hut + x + y * w, BuildCatalog.byId( id ), hero );
				}
			}
			for (int y = 0; y < HOUSE_H; y++){
				for (int x = 0; x < HOUSE_W; x++) Homestead.build( ow, origin + x + y * w, BuildCatalog.byId( "roof_tile" ), hero );
			}
			for (int y = 0; y < HUT_H; y++){
				for (int x = 0; x < HUT_W; x++) Homestead.build( ow, hut + x + y * w, BuildCatalog.byId( "roof_thatch" ), hero );
			}
			int inside = origin + HOUSE_W / 2 + (HOUSE_H / 2) * w;
			ScrollOfTeleportation.appear( hero, inside );
			Dungeon.observe();
			float outside = ClimateManager.airAt( ow, inside );
			Rooms.Room room = Rooms.at( ow, inside );
			GLog.i( "Scene: a winter night in the snow. Outside the air is "
					+ Messages.decimalFormat( "#.#", outside ) + "C; in the brick room it is "
					+ (room == null ? "no room (it should be one)" : Messages.decimalFormat( "#.#", room.air( outside ) ) + "C") + "." );
			GLog.i( "Scene: the log hut east of it: " + Rooms.check( ow, hut + 1 + w ).message()
					+ " Walk over and feel the cold come back." );
		}
	}

	private static final class Materials implements DebugScenes.Scene {
		@Override public String id(){ return "homestead-materials"; }
		@Override public String title(){ return "Homestead: a builder's satchel full of materials"; }

		@Override public void apply( Hero hero ){
			if (hero.belongings.getItem( BuildersSatchel.class ) == null){
				new BuildersSatchel().collect( hero.belongings.backpack );
			}
			for (Class<? extends Material> cls : Material.ALL){
				Item item = Reflection.newInstance( cls );
				if (item == null) continue;
				item.quantity( 50 );
				if (!item.collect( hero.belongings.backpack )){
					Heap h = Dungeon.level.drop( item, hero.pos );
					if (h.sprite != null) h.sprite.drop();
				}
			}
			GLog.i( "Scene: fifty of every building material, in the builder's satchel." );
		}
	}
}
