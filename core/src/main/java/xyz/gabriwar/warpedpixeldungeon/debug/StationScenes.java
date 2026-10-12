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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.HeatStation;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Hearth;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

/**
 * The stations' debug scenes (the Scenes menu's "Homestead" group):
 *
 * homestead-workshop: on the surface near the hero (taken there first from anywhere else), a closed
 * 9x7 stone room under slate with a barred door south and every station in it - workbench, mason's
 * bench, quern, tanning rack, cooking pot, alchemy pot and enchanting pedestal along the north
 * wall, kiln, forge and hearth along the south - the kiln, the forge and the hearth fuelled and
 * lit; a workbench and other stations in one room make it a workshop. The hero, inside, gets a
 * builder's satchel with thirty of every material and the cooking pot's and the hearth's food:
 * raw meat, cornwheat, iron ore, a blandfruit, seeds, a pasty and a ration.
 *
 * homestead-hearth-winter: the clock walked on to a winter night, the hero taken to a snowfield (or
 * the tundra) and set down inside a closed 7x5 log room under thatch, with an unlit hearth against
 * its north wall and logs in its fuel slot (and ten more in the pack). The log tells the air
 * outside and the room's: light the hearth (tap it) and watch the room's air climb by
 * Hearth.ROOM_HEAT in the inspector.
 */
final class StationScenes {

	private StationScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Workshop(),
			new HearthWinter()
	};

	//the workshop's footprint, walls and all
	private static final int SHOP_W = 9, SHOP_H = 7;
	//the stations along its inside walls, by interior x (north row, then south row); the door's
	//column in the south row is left clear
	private static final String[] NORTH = { "workbench", "masons_bench", "quern", "tanning_rack", "cooking_pot", "alchemy_pot", "enchanting_pedestal" };
	private static final String[] SOUTH = { "kiln", "forge", null, null, null, "hearth_stone", null };

	private static final class Workshop implements DebugScenes.Scene {
		//set while the hero is on his way to the surface
		boolean travelling;

		@Override public String id(){ return "homestead-workshop"; }
		@Override public String title(){ return "Homestead: a stone workshop with every station, its fires lit"; }

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
			int o = findGround( ow, hero.pos, SHOP_W, SHOP_H );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero for a workshop; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			ring( ow, o, SHOP_W, SHOP_H, "wall_stone", "floor_stone", "door_barred" );
			roof( ow, o, SHOP_W, SHOP_H, "roof_slate" );
			for (int x = 0; x < NORTH.length; x++) build( ow, o + (x + 1) + w, NORTH[x] );
			for (int x = 0; x < SOUTH.length; x++){
				if (SOUTH[x] != null) build( ow, o + (x + 1) + (SHOP_H - 2) * w, SOUTH[x] );
			}
			int inside = o + SHOP_W / 2 + (SHOP_H / 2) * w;
			ScrollOfTeleportation.appear( hero, inside );

			//the fires fuelled (six logs and two charcoal each) and lit
			for (Mob m : ow.mobs.toArray( new Mob[0] )){
				if (!(m instanceof HeatStation)) continue;
				HeatStation h = (HeatStation) m;
				h.addFuel( hero, new Log().quantity( 6 ) );
				h.addFuel( hero, new Charcoal().quantity( 2 ) );
				h.light();
			}

			stock( hero );
			Dungeon.observe();
			Rooms.Room room = Rooms.at( ow, inside );
			GLog.i( "Scene: a stone workshop with every station, the kiln, the forge and the hearth lit. "
					+ (room == null ? "It is no room (it should be one)." : Rooms.describe( ow, inside )) );
			GLog.i( "Scene: tap a station to open its window. Thirty of every material and the pot's and the hearth's food are in your pack." );
		}
	}

	private static final class HearthWinter implements DebugScenes.Scene {
		//set while the hero is on his way to the snow
		boolean travelling;

		@Override public String id(){ return "homestead-hearth-winter"; }
		@Override public String title(){ return "Homestead: a log room with an unlit hearth on a winter night"; }

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
			int o = findGround( ow, hero.pos, 7, 5 );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero for a room; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			ring( ow, o, 7, 5, "wall_log", "floor_plank", "door" );
			roof( ow, o, 7, 5, "roof_thatch" );
			int hearth = o + 3 + w;
			build( ow, hearth, "hearth_stone" );
			int inside = o + 3 + 3 * w;
			ScrollOfTeleportation.appear( hero, inside );

			for (Mob m : ow.mobs){
				if (m instanceof HeatStation && m.pos == hearth) ((HeatStation) m).addFuel( hero, new Log().quantity( 6 ) );
			}
			give( hero, new Log().quantity( 10 ) );
			Dungeon.observe();

			float outside = ClimateManager.airAt( ow, inside );
			Rooms.Room room = Rooms.at( ow, inside );
			GLog.i( "Scene: a winter night. Outside the air is " + Messages.decimalFormat( "#.#", outside ) + "C; in the log room it is "
					+ (room == null ? "no room (it should be one)" : Messages.decimalFormat( "#.#", room.air( outside ) ) + "C") + "." );
			GLog.i( "Scene: the hearth has six logs in it, unlit. Tap it and light it: the room's air (the inspector) climbs by "
					+ Messages.decimalFormat( "#", Hearth.ROOM_HEAT ) + "C." );
		}
	}

	// ----------------------------------------------------------------- building

	private static void build( OverworldLevel ow, int cell, String id ){
		//no builder: a piece laid by a scene gives nothing back for what it covers
		Homestead.build( ow, cell, BuildCatalog.byId( id ), null );
	}

	//a ring of `wall` w x h at o, `floor` inside, a `door` in the middle of its south side
	private static void ring( OverworldLevel ow, int o, int w, int h, String wall, String floor, String door ){
		int width = ow.width();
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * width;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				build( ow, c, !edge ? floor : x == w / 2 && y == h - 1 ? door : wall );
			}
		}
	}

	//a roof of a kind over a whole w x h rect at o, once its walls stand to hold it
	private static void roof( OverworldLevel ow, int o, int w, int h, String kind ){
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++) build( ow, o + x + y * ow.width(), kind );
		}
	}

	//the top-left window cell of the nearest open w x h stretch (and a cell of ground round it)
	//two cells clear of the hero, -1 within 40 cells of none
	private static int findGround( OverworldLevel ow, int heroPos, int w, int h ){
		int width = ow.width(), hx = heroPos % width, hy = heroPos / width;
		for (int r = 3; r <= 40; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (fits( ow, hx + dx, hy + dy, w, h, hx, hy )) return hx + dx + (hy + dy) * width;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int ox, int oy, int w, int h, int hx, int hy ){
		int width = ow.width();
		for (int y = oy - 1; y <= oy + h; y++){
			for (int x = ox - 1; x <= ox + w; x++){
				if (x < 2 || y < 2 || x >= width - 2 || y >= ow.height() - 2) return false;
				if (Math.abs( x - hx ) <= 1 && Math.abs( y - hy ) <= 1) return false;
				int c = x + y * width;
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

	// ----------------------------------------------------------------- the hero's pack

	//a builder's satchel with thirty of every material, and food for the pot and the hearth
	private static void stock( Hero hero ){
		if (hero.belongings.getItem( BuildersSatchel.class ) == null){
			new BuildersSatchel().collect( hero.belongings.backpack );
		}
		for (Class<? extends Material> cls : Material.ALL){
			Item item = Reflection.newInstance( cls );
			if (item != null) give( hero, item.quantity( 30 ) );
		}
		give( hero, new MysteryMeat().quantity( 4 ) );
		give( hero, new MonsterMeat().quantity( 2 ) );
		give( hero, new Cornwheatshaft().quantity( 10 ) );
		give( hero, new IronOre().quantity( 5 ) );
		give( hero, new Blandfruit().quantity( 2 ) );
		give( hero, new Firebloom.Seed().quantity( 2 ) );
		give( hero, new Sungrass.Seed() );
		give( hero, new Pasty() );
		give( hero, new Food() );
	}

	private static void give( Hero hero, Item item ){
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}
}
