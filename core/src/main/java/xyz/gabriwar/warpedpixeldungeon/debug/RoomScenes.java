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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.homestead.rooms.RoomTypes;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The homesteads' room scenes (homestead/rooms; the Scenes menu's "Homestead" group):
 *
 * homestead-bedroom-night: a winter night on a snowfield, a closed brick bedroom - barred door
 * south, a window north, plank floor, tile roof - with a fur bed, two rugs, a statue (so it is a
 * shrine too) and two pairs of bookshelves along the north wall either side of the window, the
 * hero beside the bed, in line with the window, and three grey wolves prowling outside. The look at
 * the room says it is sealed: the wolves seen through the window do not stop him lying down, nobody
 * finds him, and he wakes Well Rested and blessed.
 *
 * homestead-hut-gap: the same, but its east wall battered below half its health: still a room,
 * but not sealed - the look says so, a wolf in sight keeps him from lying down, and a night in the
 * bed may be cut short by the wolves.
 *
 * homestead-rooms-tour: on the surface, six small stone rooms in two rows, one of each type: a
 * bedroom (a straw bed), a kitchen (the stations' hearth), a workshop (the stations' workbench and
 * another station), a storehouse (two chests), a greenhouse (farmland under glass) and a shrine (a
 * statue). The hero stands south of them; the log names each one as its look does. The workshop and
 * the greenhouse are named by the stations' and the farming's own traits, the kitchen's stove is
 * the stations': a room this build cannot fill or name is listed in the log.
 */
final class RoomScenes {

	private RoomScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Bedroom( false ),
			new Bedroom( true ),
			new Tour()
	};

	//the bedroom: 7x5 outer, inside 5x3
	private static final int BED_W = 7, BED_H = 5;

	private static final class Bedroom implements DebugScenes.Scene {
		final boolean broken;
		//set while the hero is on his way to the snow
		boolean travelling;

		Bedroom( boolean broken ){
			this.broken = broken;
		}

		@Override public String id(){ return broken ? "homestead-hut-gap" : "homestead-bedroom-night"; }
		@Override public String title(){
			return broken ? "Homestead: a bedroom with a battered wall, wolves outside"
					: "Homestead: a sealed bedroom on a winter night, wolves outside";
		}

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
			OverworldLevel ow = builder();
			if (ow == null) return;
			int room = findGround( ow, hero.pos, BED_W, BED_H );
			if (room == -1){
				GLog.w( "Scene: no open ground near the hero for a bedroom; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			room( ow, room, BED_W, BED_H, "wall_brick", "door_barred", "window_brick", "roof_tile", hero );
			//shelves along the north wall, two either side of the window (open floor in front of each)
			for (int x : new int[]{ 1, 2, 4, 5 }) build( ow, room + x + w, "bookshelf", hero );
			build( ow, room + 2 + 3 * w, "bed_fur", hero );
			build( ow, room + 5 + 3 * w, "statue", hero );
			build( ow, room + 3 + 2 * w, "rug", hero );
			build( ow, room + 3 + 3 * w, "rug", hero );
			if (broken){
				//the east wall's middle battered past half its health: standing, but no longer whole
				int east = room + BED_W - 1 + 2 * w;
				Homestead.damage( ow, east, Homestead.hpAt( ow, east ) * 3 / 5, null );
			}
			//beside the bed, under the window
			int beside = room + 3 + 2 * w;
			ScrollOfTeleportation.appear( hero, beside );

			//three wolves on the snow round the room, six to nine cells off
			int placed = 0;
			for (int ring = 6; ring <= 9 && placed < 3; ring++){
				for (int[] d : new int[][]{ { -ring, 0 }, { ring, 0 }, { 0, -ring }, { 0, ring }, { ring, ring }, { -ring, -ring } }){
					int c = beside + d[0] + d[1] * w;
					if (placed >= 3 || c < 0 || c >= ow.length() || !ow.passable[c] || Rooms.at( ow, c ) != null
							|| Actor.findChar( c ) != null) continue;
					DebugScenes.spawn( GrayWolf.class, c );
					placed++;
				}
			}
			Dungeon.observe();
			GLog.i( "Scene: " + Rooms.describe( ow, beside ).replace( '\n', ' ' ) );
			GLog.i( broken
					? "Scene: the east wall is battered below half its health, so the room is not sealed. While a wolf is in sight you can't lie down; once none is, sleep in the fur bed until dawn: the wolves may find you."
					: "Scene: the room is sealed. Watch the wolves through the window, then sleep in the fur bed until dawn with them still in sight: nothing finds you, and you wake well rested and blessed (the statue makes it a shrine). Check the four shelves along the north wall are drawn with their fronts, none blacked out." );
		}
	}

	//the tour: six 5x5 rooms, three to a row, a cell between them
	private static final int ROOM = 5, GAP = 1, TOUR_W = 3 * ROOM + 2 * GAP, TOUR_H = 2 * ROOM + GAP;

	private static final class Tour implements DebugScenes.Scene {
		//set while the hero is on his way to the surface
		boolean travelling;

		@Override public String id(){ return "homestead-rooms-tour"; }
		@Override public String title(){ return "Homestead: one room of each type, side by side"; }

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
			OverworldLevel ow = builder();
			if (ow == null) return;
			//the rooms, and two rows of ground south of them for the hero
			int origin = findGround( ow, hero.pos, TOUR_W, TOUR_H + 2 );
			if (origin == -1){
				GLog.w( "Scene: no open ground near the hero for six rooms; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			int[] at = new int[6];
			for (int i = 0; i < 6; i++) at[i] = origin + (i % 3) * (ROOM + GAP) + (i / 3) * (ROOM + GAP) * w;
			String[] roofs = { "roof_slate", "roof_slate", "roof_slate", "roof_slate", "roof_glass", "roof_slate" };
			for (int i = 0; i < 6; i++){
				//the greenhouse's floor is farmland, the others' plank
				room( ow, at[i], ROOM, ROOM, "wall_stone", "door", null, i == 4 ? null : roofs[i], hero );
			}
			StringBuilder missing = new StringBuilder();

			//bedroom: a straw bed
			build( ow, at[0] + 1 + w, "bed_straw", hero );

			//kitchen: the stations' brick hearth, or their cooking pot
			BuildPiece stove = BuildCatalog.byId( "hearth_brick" ) != null ? BuildCatalog.byId( "hearth_brick" ) : BuildCatalog.byId( "cooking_pot" );
			if (stove != null) Homestead.build( ow, at[1] + 2 + w, stove, hero );
			else missing.append( " room 2 is left empty (no hearth or cooking pot in this build);" );

			//workshop: the workbench and the first other station
			BuildPiece bench = BuildCatalog.byId( "workbench" ), other = null;
			for (BuildPiece p : BuildCatalog.in( BuildCategory.STATIONS )){
				if (p != bench && p.kind == BuildPiece.Kind.ACTOR){
					other = p;
					break;
				}
			}
			if (bench != null && other != null){
				Homestead.build( ow, at[2] + 1 + w, bench, hero );
				Homestead.build( ow, at[2] + 3 + w, other, hero );
			} else missing.append( " room 3 is left empty (no workbench and second station in this build);" );

			//storehouse: two chests
			for (int c : new int[]{ at[3] + 1 + w, at[3] + 3 + w }){
				StorageChest chest = new StorageChest();
				chest.pos = c;
				GameScene.add( chest );
			}

			//greenhouse: farmland under glass (the farming's piece when there is one)
			BuildPiece farmland = BuildCatalog.byId( "farmland" );
			for (int y = 1; y < ROOM - 1; y++){
				for (int x = 1; x < ROOM - 1; x++){
					int c = at[4] + x + y * w;
					if (farmland != null) Homestead.build( ow, c, farmland, hero );
					else {
						Level.set( c, Terrain.FARMLAND, ow );
						Homestead.refresh( ow, c );
					}
				}
			}
			for (int y = 0; y < ROOM; y++){
				for (int x = 0; x < ROOM; x++) build( ow, at[4] + x + y * w, "roof_glass", hero );
			}

			//shrine: a statue
			build( ow, at[5] + 2 + w, "statue", hero );

			int front = origin + (TOUR_W / 2) + (TOUR_H + 1) * w;
			ScrollOfTeleportation.appear( hero, front );
			Dungeon.observe();
			String[] types = { RoomTypes.BEDROOM, RoomTypes.KITCHEN, RoomTypes.WORKSHOP,
					RoomTypes.STOREHOUSE, RoomTypes.GREENHOUSE, RoomTypes.SHRINE };
			for (int i = 0; i < 6; i++){
				int inside = at[i] + 2 + 3 * w;
				GLog.i( "Scene: room " + (i + 1) + ": " + Rooms.describe( ow, inside ).replace( '\n', ' ' ) );
				Rooms.Room r = Rooms.at( ow, inside );
				//the workshop and the greenhouse are the stations' and the farming's traits
				boolean empty = (i == 1 && stove == null) || (i == 2 && (bench == null || other == null));
				if (!empty && (r == null || !RoomTypes.has( ow, r, types[i] ))){
					missing.append( " room " ).append( i + 1 ).append( " is no " ).append( types[i] )
							.append( " in this build;" );
				}
			}
			if (missing.length() > 0) GLog.w( "Scene: missing:" + missing );
			GLog.i( "Scene: bedroom, kitchen, workshop on the north row; storehouse, greenhouse, shrine on the south. Inspect each." );
		}
	}

	// ----------------------------------------------------------------- helpers

	//the shown level when it is a slice the host may build on, else null (and why)
	private static OverworldLevel builder(){
		if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
			GLog.w( "Scene: could not reach the surface" );
			return null;
		}
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (ow.isMirror()){
			GLog.w( "Scene: only the host can build" );
			return null;
		}
		return ow;
	}

	private static void build( OverworldLevel ow, int cell, String id, Hero hero ){
		Homestead.build( ow, cell, BuildCatalog.byId( id ), hero );
	}

	//a ring of `wall` w x h at o: `door` in the middle of its south side, `window` (or none) in the
	//middle of its north side, plank floor inside (none under a null roof: the caller lays its own),
	//and `roof` (or none) over all of it once the walls stand
	private static void room( OverworldLevel ow, int o, int w, int h, String wall, String door, String window,
							  String roof, Hero hero ){
		int lw = ow.width();
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * lw;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				if (!edge){
					if (roof != null) build( ow, c, "floor_plank", hero );
				} else if (x == w / 2 && y == h - 1) build( ow, c, door, hero );
				else if (window != null && x == w / 2 && y == 0) build( ow, c, window, hero );
				else build( ow, c, wall, hero );
			}
		}
		if (roof == null) return;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++) build( ow, o + x + y * lw, roof, hero );
		}
	}

	/** The top-left window cell of the nearest open w x h stretch (and a cell of ground round it)
	 *  that keeps two cells from the hero, -1 within 40 cells of none. */
	private static int findGround( OverworldLevel ow, int heroPos, int sw, int sh ){
		int w = ow.width(), hx = heroPos % w, hy = heroPos / w;
		for (int r = 2; r <= 40; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int ox = hx + dx, oy = hy + dy;
					if (fits( ow, ox, oy, sw, sh, hx, hy )) return ox + oy * w;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int ox, int oy, int sw, int sh, int hx, int hy ){
		int w = ow.width();
		for (int y = oy - 1; y <= oy + sh; y++){
			for (int x = ox - 1; x <= ox + sw; x++){
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
}
