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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.pieces.TravelPieces;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Hearthstone;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.TravelWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystone;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystones;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.WndHome;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;


/**
 * The travel part's debug scenes (the Scenes menu's "Homestead" group):
 *
 * homestead-waystones: a tour that ends in the town - the hero is taken to a meadow and a hearthstone
 * goes up beside him (a home), then down to the first cave slice under it for a second home, then to
 * the town, where the town's waystone and those of the two nearest big settlements are attuned and he
 * is set down beside the town's. Tap the waystone (or open the world map) to see them all.
 *
 * homestead-travel-hungry: the hero in the town beside its waystone, the town's and the nearest big
 * settlement's attuned, his hunger a little short of hungry: every trip in the waystone's window
 * warns that he would arrive hungry.
 *
 * homestead-hearthstone: the materials for a hearthstone given (a builder's satchel, ten rough stones
 * and two cut ones), the hearthstone put up beside him through the real placing (cost and all), and
 * its menu opened: choose Rename to name the home.
 */
final class TravelScenes {

	private TravelScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new WaystonesTour(),
			new TravelHungry(),
			new PlaceHearthstone()
	};

	// ------------------------------------------------------------------ helpers

	private static boolean onSurface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0;
	}

	private static boolean inTown( Hero hero ){
		return onSurface() && ((OverworldLevel) Dungeon.level).inTown( hero.pos );
	}

	//off to the town's plaza, the scene run again there
	private static void toTown( DebugScenes.Scene scene ){
		DebugScenes.travelThen( scene, 0, WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) );
	}

	/** The nearest cell to the hero within reach where a piece may go up (Homestead.canPlaceSite),
	 *  -1 for none. */
	static int siteNear( Hero hero, BuildPiece piece ){
		Level level = Dungeon.level;
		int w = level.width(), hx = hero.pos % w, hy = hero.pos / w;
		for (int r = 1; r <= Homestead.RANGE; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = hx + dx, y = hy + dy;
					if (x < 1 || y < 1 || x >= w - 1 || y >= level.height() - 1) continue;
					int c = x + y * w;
					if (Homestead.canPlaceSite( hero, c, piece ).ok()) return c;
				}
			}
		}
		return -1;
	}

	//a hearthstone up beside the hero with no cost (Homestead.build), the rock of a cave carved for
	//it when nothing round him takes one; false when it could not go up
	private static boolean buildHome( Hero hero ){
		if (HomesteadWorld.homes().size() >= TravelPieces.MAX_HOMES){
			GLog.w( "Scene: you keep " + TravelPieces.MAX_HOMES + " homes already; take one down to see another go up" );
			return false;
		}
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		BuildPiece piece = BuildCatalog.byId( "hearthstone" );
		Dungeon.observe();
		int c = siteNear( hero, piece );
		if (c == -1){
			for (int n : PathFinder.NEIGHBOURS4){
				int at = hero.pos + n;
				int t = ow.map[at];
				if ((t == Terrain.WALL || t == Terrain.WALL_DECO) && !ow.protectedForBuilding( at )){
					Level.set( at, Terrain.EMPTY_DECO, ow );
					GameScene.updateMap( at );
					Dungeon.observe();
					c = siteNear( hero, piece );
					break;
				}
			}
		}
		if (c == -1){
			GLog.w( "Scene: nowhere beside the hero takes a hearthstone" );
			return false;
		}
		Homestead.build( ow, c, piece, hero );
		return true;
	}

	//the waystone of a key standing on the level, or null
	private static Waystone stoneOf( long key ){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Waystone && ((Waystone) m).key == key) return (Waystone) m;
		}
		return null;
	}

	//the town's waystone and the nearest `n` big settlements' attuned (each by its stone when it
	//stands, else by its well); the town's stone, or null when it does not stand here
	private static Waystone attuneTownAnd( int n ){
		TravelWorld tw = TravelWorld.get();
		long seed = ((OverworldLevel) Dungeon.level).worldSeed;
		Waystone town = stoneOf( Waystones.TOWN );
		tw.attune( Waystones.TOWN, Waystones.townWX(), Waystones.townWY(), Waystones.placeName( seed, Waystones.TOWN ) );
		int done = 0;
		for (int[] s : WorldStructures.settlementsNear( seed, 0, 0, null, Waystones.MIN_HOUSES, 6 )){
			if (done >= n) break;
			if (!Waystones.keepsOne( seed, s[0], s[1] )) continue;
			long key = WorldStructures.sectorOf( s[0], s[1] );
			Waystone stone = stoneOf( key );
			tw.attune( key, stone != null ? stone.wx : s[2], stone != null ? stone.wy : s[3], Waystones.placeName( seed, key ) );
			GLog.i( "Scene: attuned the waystone of " + Waystones.placeName( seed, key ) + " (" + s[4] + " houses)" );
			done++;
		}
		if (done < n) GLog.w( "Scene: only " + done + " big settlements near the origin in this world" );
		return town;
	}

	//the hero set down on a free cell beside a mob
	private static void besides( Hero hero, Mob m ){
		for (int n : PathFinder.NEIGHBOURS8){
			int c = m.pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar( c ) == null){
				ScrollOfTeleportation.appear( hero, c );
				Dungeon.observe();
				return;
			}
		}
	}

	// ------------------------------------------------------------------ the scenes

	private static final class WaystonesTour implements DebugScenes.Scene {
		//where the tour has got to: 0 not under way, 1 bound for the meadow, 2 for the caves, 3 the town
		int step;
		int[] meadow;

		@Override public String id(){ return "homestead-waystones"; }
		@Override public String title(){ return "Homestead: waystones attuned, a home in a meadow and one in the caves"; }

		@Override public void apply( Hero hero ){
			switch (step){
				case 1:
					if (!onSurface()){
						fail( "could not reach the meadow" );
						return;
					}
					buildHome( hero );
					step = 2;
					DebugScenes.travelThen( this, -1, meadow[0], meadow[1] );
					return;
				case 2:
					if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != -1){
						fail( "could not reach the caves" );
						return;
					}
					buildHome( hero );
					step = 3;
					toTown( this );
					return;
				case 3:
					if (!onSurface()){
						fail( "could not reach the town" );
						return;
					}
					step = 0;
					Waystone town = attuneTownAnd( 2 );
					if (town != null) besides( hero, town );
					GLog.i( "Scene: " + HomesteadWorld.homes().size() + " homes and "
							+ TravelWorld.get().attuned().size() + " attuned waystones. Tap the town's waystone to travel, or open the world map to see them pinned." );
					return;
				default:
					final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
					final float shift = WorldModel.calendarShift();
					DebugScenes.search( "a meadow near the origin",
							() -> DebugScenes.biomeGround( seed, shift, WorldModel.Biome.MEADOW, WorldModel.Biome.PLAINS ),
							spot -> {
								if (spot == null){
									GLog.w( "Scene: no meadow or plains near the origin in this world" );
									return;
								}
								meadow = spot;
								step = 1;
								DebugScenes.travelThen( this, 0, spot[0], spot[1] );
							} );
			}
		}

		private void fail( String why ){
			step = 0;
			GLog.w( "Scene: " + why );
		}
	}

	private static final class TravelHungry implements DebugScenes.Scene {
		//set while the hero is on his way to the town
		boolean travelling;

		@Override public String id(){ return "homestead-travel-hungry"; }
		@Override public String title(){ return "Homestead: almost hungry beside the town's waystone"; }

		@Override public void apply( Hero hero ){
			if (!inTown( hero )){
				if (travelling){
					travelling = false;
					GLog.w( "Scene: could not reach the town" );
					return;
				}
				travelling = true;
				toTown( this );
				return;
			}
			travelling = false;
			Waystone town = attuneTownAnd( 1 );
			if (town == null){
				GLog.w( "Scene: the town's waystone is not standing (its cell was taken); walk off and back" );
				return;
			}
			besides( hero, town );
			Hunger hunger = hero.buff( Hunger.class );
			if (hunger != null) hunger.setLevel( Hunger.HUNGRY - 30 );
			GLog.i( "Scene: your hunger is " + (int)(Hunger.HUNGRY - 30) + ", hungry at " + (int)Hunger.HUNGRY
					+ ". Tap the waystone: every trip warns that you would arrive hungry." );
		}
	}

	private static final class PlaceHearthstone implements DebugScenes.Scene {
		//set while the hero is on his way to the surface
		boolean travelling;

		@Override public String id(){ return "homestead-hearthstone"; }
		@Override public String title(){ return "Homestead: put up a hearthstone and name the home"; }

		@Override public void apply( Hero hero ){
			if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).inTown( hero.pos )){
				if (travelling){
					travelling = false;
					GLog.w( "Scene: could not reach open ground" );
					return;
				}
				travelling = true;
				//south of the town, off its streets
				DebugScenes.travelThen( this, 0, WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
						WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) + 40 );
				return;
			}
			travelling = false;
			if (((OverworldLevel) Dungeon.level).isMirror()){
				GLog.w( "Scene: only the host can build" );
				return;
			}
			if (hero.belongings.getItem( BuildersSatchel.class ) == null) new BuildersSatchel().collect( hero.belongings.backpack );
			give( hero, RoughStone.class, 10 );
			give( hero, CutStone.class, 2 );
			if (HomesteadWorld.homes().size() >= TravelPieces.MAX_HOMES){
				GLog.w( "Scene: the materials are in your satchel, but you keep " + TravelPieces.MAX_HOMES
						+ " homes already: the build menu will say so. Take one down first." );
				return;
			}
			BuildPiece piece = BuildCatalog.byId( "hearthstone" );
			int cell = siteNear( hero, piece );
			if (cell == -1){
				GLog.w( "Scene: the materials are in your satchel; nothing beside you takes a hearthstone, walk somewhere open" );
				return;
			}
			Homestead.Reason r = Homestead.place( hero, cell, piece );
			if (!r.ok()){
				GLog.w( "Scene: " + r.message() );
				return;
			}
			for (Mob m : Dungeon.level.mobs){
				if (m.pos == cell && m instanceof Hearthstone && ((Hearthstone) m).home() != null){
					GameScene.show( new WndHome( ((Hearthstone) m).home() ) );
				}
			}
			GLog.i( "Scene: the hearthstone is up, paid for with 10 rough stone and 2 cut stone. Choose Rename to name the home." );
		}

		private static void give( Hero hero, Class<? extends Item> cls, int n ){
			Item item = Reflection.newInstance( cls );
			if (item == null) return;
			item.quantity( n );
			if (!item.collect( hero.belongings.backpack )){
				Heap h = Dungeon.level.drop( item, hero.pos );
				if (h.sprite != null) h.sprite.drop();
			}
		}
	}
}
