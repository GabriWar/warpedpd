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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

/**
 * The build menu's debug scenes (the Scenes menu's "Homestead" group):
 *
 * homestead-build: a meadow with a wood and a river close by (found from the world's origin), the
 * hero there with the Builder's Tool (in a quickslot), a builder's satchel and 99 of every
 * material, and a few pieces already up: a stone wall with a door into a log wall, two of the
 * stones and a log knocked about, a plank floor under a thatch roof with one roof cell damaged,
 * a fence with a broken rail and a heap of rubble - something to build beside, mend, take down,
 * clear and look at.
 *
 * homestead-roofs: five rooms in a row under the five roofs (thatch, plank, slate, tile, glass),
 * a door in each, the hero outside: walk into one and only that room's roof fades.
 */
final class BuildScenes {

	private BuildScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Build(),
			new Roofs()
	};

	// ----------------------------------------------------------------- homestead-build

	//the pieces' footprint: 11 x 4, its top-left the origin
	private static final int BUILD_W = 11, BUILD_H = 4;

	private static final class Build implements DebugScenes.Scene {
		//set while the hero is on his way to the meadow
		boolean travelling;

		@Override public String id(){ return "homestead-build"; }
		@Override public String title(){ return "Homestead: the build menu, by a wood and a river"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive( hero );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "a meadow by a wood and a river", () -> meadow( seed, shift ), spot -> {
				if (spot == null){
					GLog.w( "Scene: no meadow by a wood and a river near the origin; building where the hero stands" );
					if (Dungeon.level instanceof OverworldLevel) arrive( hero );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, spot[0], spot[1] );
			} );
		}

		private void arrive( Hero hero ){
			if (!(Dungeon.level instanceof OverworldLevel)){
				GLog.w( "Scene: could not reach the surface" );
				return;
			}
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			if (ow.isMirror()){
				GLog.w( "Scene: only the host can build" );
				return;
			}
			equip( hero );
			int o = ground( ow, hero.pos, BUILD_W, BUILD_H );
			if (o == -1){
				GLog.w( "Scene: tool and materials given, but no open ground near the hero for the sample pieces" );
				return;
			}
			int w = ow.width();
			//a stone wall, a plain door, a log wall; a heap of rubble where a log wall stood
			for (int x = 0; x <= 4; x++) build( ow, o + x, "wall_stone" );
			build( ow, o + 5, "door" );
			for (int x = 6; x <= 8; x++) build( ow, o + x, "wall_log" );
			build( ow, o + 9, "wall_log" );
			Homestead.damage( ow, o + 9, 999, null );
			//knocked about: two stones and a log
			Homestead.damage( ow, o + 1, 70, null );
			Homestead.damage( ow, o + 3, 100, null );
			Homestead.damage( ow, o + 7, 30, null );
			//a plank floor under a thatch roof (the stone wall holds it up), one cell of it damaged
			for (int y = 1; y <= 2; y++){
				for (int x = 0; x <= 2; x++){
					build( ow, o + x + y * w, "floor_plank" );
					build( ow, o + x + y * w, "roof_thatch" );
				}
			}
			Homestead.damageRoof( ow, o + 1 + w, 12 );
			//a fence, one rail broken
			for (int x = 5; x <= 9; x++) build( ow, o + x + 3 * w, "fence" );
			Homestead.damage( ow, o + 7 + 3 * w, 15, null );

			//the hero just south of it all, the pieces within reach of most of it
			int stand = o + 5 + (BUILD_H + 1) * w;
			if (ow.passable[stand] && Actor.findChar( stand ) == null) ScrollOfTeleportation.appear( hero, stand );
			Dungeon.observe();
			GLog.i( "Scene: the Builder's Tool is in a quickslot, with 99 of every material. Use it (Build) for the"
					+ " menu: pick a piece, Place / Line / Area put the build bar up. Two stones, a log, a roof cell"
					+ " and a fence rail are damaged (Repair), there is rubble to clear (Demolish), and Inspect looks at it all."
					+ " Tap beyond reach to walk with the bar up; try a roof Area over a whole room." );
		}
	}

	//the tool (quickslotted), a satchel and 99 of every material
	private static void equip( Hero hero ){
		BuildersTool tool = hero.belongings.getItem( BuildersTool.class );
		if (tool == null){
			tool = new BuildersTool();
			if (!tool.collect( hero.belongings.backpack )) tool = null;
		}
		if (tool != null && !Dungeon.quickslot.contains( tool )){
			for (int i = 0; i < xyz.gabriwar.warpedpixeldungeon.QuickSlot.SIZE; i++){
				if (Dungeon.quickslot.getItem( i ) == null){
					Dungeon.quickslot.setSlot( i, tool );
					QuickSlotButton.refresh();
					break;
				}
			}
		}
		if (hero.belongings.getItem( BuildersSatchel.class ) == null){
			new BuildersSatchel().collect( hero.belongings.backpack );
		}
		for (Class<? extends Material> cls : Material.ALL){
			Item item = Reflection.newInstance( cls );
			if (item == null) continue;
			item.quantity( 99 );
			if (!item.collect( hero.belongings.backpack )){
				Heap h = Dungeon.level.drop( item, hero.pos );
				if (h.sprite != null) h.sprite.drop();
			}
		}
	}

	private static void build( OverworldLevel ow, int cell, String id ){
		Homestead.build( ow, cell, BuildCatalog.byId( id ), Dungeon.hero );
	}

	/**
	 * A meadow (open grass of the plains or a meadow) with trees and running water close by: on
	 * rings 32 cells apart out from the origin, a dry cell whose 9x9 is open ground, with at least
	 * 10 trees and 6 cells of water within 12 cells (counted every other cell), away from the town
	 * and every settlement. Null when none within 2500 cells. Pure: run off the render thread.
	 */
	static int[] meadow( long seed, float shift ){
		WorldModel.Sample s = new WorldModel.Sample();
		final int step = 32, rings = 80;
		for (int r = 1; r <= rings; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int cx = i * step, cy = j * step;
					if (meadowAt( seed, shift, cx, cy, s )) return new int[]{ cx, cy };
				}
			}
		}
		return null;
	}

	private static boolean meadowAt( long seed, float shift, int cx, int cy, WorldModel.Sample s ){
		WorldModel.Biome b = WorldModel.sample( seed, cx, cy, shift, s ).biome;
		if (b != WorldModel.Biome.MEADOW && b != WorldModel.Biome.PLAINS) return false;
		if (WorldStructures.townCell( cx, cy ) != -1 || OverworldFauna.nearSettlement( seed, cx, cy )) return false;
		int trees = 0, water = 0;
		for (int y = -12; y <= 12; y += 2){
			for (int x = -12; x <= 12; x += 2){
				int t = terrain( seed, shift, cx + x, cy + y, s );
				if (t == Terrain.TREE_OAK || t == Terrain.TREE_PINE) trees++;
				else if (t == Terrain.WATER || t == Terrain.DEEP_WATER) water++;
			}
		}
		if (trees < 10 || water < 6) return false;
		for (int y = -4; y <= 4; y++){
			for (int x = -4; x <= 4; x++){
				int t = terrain( seed, shift, cx + x, cy + y, s );
				if ((Terrain.flags[t] & Terrain.PASSABLE) == 0 || (Terrain.flags[t] & Terrain.LIQUID) != 0) return false;
			}
		}
		return true;
	}

	private static int terrain( long seed, float shift, int wx, int wy, WorldModel.Sample s ){
		return WorldModel.wildTerrain( seed, wx, wy, WorldModel.sample( seed, wx, wy, shift, s ) );
	}

	// ----------------------------------------------------------------- homestead-roofs

	//five rooms of 2x2 floor in a row, sharing their side walls: 16 x 4
	private static final int ROOMS = 5, ROOM = 3, ROOFS_W = ROOMS * ROOM + 1, ROOFS_H = 4;
	private static final String[] ROOF_IDS = { "roof_thatch", "roof_plank", "roof_slate", "roof_tile", "roof_glass" };

	private static final class Roofs implements DebugScenes.Scene {
		//set while the hero is on his way to the surface
		boolean travelling;

		@Override public String id(){ return "homestead-roofs"; }
		@Override public String title(){ return "Homestead: five rooms under the five roofs"; }

		@Override public void apply( Hero hero ){
			if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
				if (travelling){
					travelling = false;
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				travelling = true;
				//south of the town, off its streets (as homestead-shell)
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
			int o = ground( ow, hero.pos, ROOFS_W, ROOFS_H );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero for five rooms; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			for (int y = 0; y < ROOFS_H; y++){
				for (int x = 0; x < ROOFS_W; x++){
					boolean wall = y == 0 || y == ROOFS_H - 1 || x % ROOM == 0;
					String id = !wall ? "floor_plank" : y == ROOFS_H - 1 && x % ROOM == 1 ? "door" : "wall_stone";
					build( ow, o + x + y * w, id );
				}
			}
			//each room's roof over its floor and its walls; a shared wall takes the eastern room's
			for (int k = 0; k < ROOMS; k++){
				for (int y = 0; y < ROOFS_H; y++){
					for (int x = k * ROOM; x <= k * ROOM + ROOM; x++) build( ow, o + x + y * w, ROOF_IDS[k] );
				}
			}
			int stand = o + 1 + (ROOFS_H + 1) * w;
			if (ow.passable[stand] && Actor.findChar( stand ) == null) ScrollOfTeleportation.appear( hero, stand );
			Dungeon.observe();
			GLog.i( "Scene: five rooms side by side under thatch, plank, slate, tile and glass, a door in the"
					+ " south of each. Walk into one: only its roof fades, over 0.2 s, and comes back as you step out." );
		}
	}

	// ----------------------------------------------------------------- open ground

	/** The top-left window cell of the nearest open stretch w x h (and a cell of ground round it,
	 *  and two rows south of it for the hero to stand on) at least two cells from the hero; -1
	 *  within 40 cells of none. */
	static int ground( OverworldLevel ow, int heroPos, int w, int h ){
		int mw = ow.width(), hx = heroPos % mw, hy = heroPos / mw;
		for (int r = 3; r <= 40; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (fits( ow, hx + dx, hy + dy, w, h + 2, hx, hy )) return hx + dx + (hy + dy) * mw;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int ox, int oy, int w, int h, int hx, int hy ){
		int mw = ow.width();
		for (int y = oy - 1; y <= oy + h; y++){
			for (int x = ox - 1; x <= ox + w; x++){
				if (x < 2 || y < 2 || x >= mw - 2 || y >= ow.height() - 2) return false;
				if (Math.abs( x - hx ) <= 1 && Math.abs( y - hy ) <= 1) return false;
				int c = x + y * mw;
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
