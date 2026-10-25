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
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.GatheringSlice;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Axe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BattleAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.util.function.IntPredicate;

/**
 * The gathering's debug scenes (the Scenes menu's "Homestead" group; homestead/gathering):
 *
 * homestead-forest: in a forest, the hero set beside an oak and a pine, every axe in the pack
 * (hand axe, axe, battle axe, greataxe, woodcutter's axe). Tap a tree beside you: the weapon in
 * hand is used when it is an axe, else the quickest in the pack.
 *
 * homestead-quarry: in the foothills, the hero beside a boulder with a pickaxe and a mason's
 * hammer; the nearest rock wall is named (the pick breaks it in a blow and it gives rough stone;
 * drop the pick and the hammer breaks it in three).
 *
 * homestead-riverbank: where a river meets the sea (a swamp when there is none near), the hero on
 * a bank that gives clay, with a shovel and a sickle; the nearest beach sand and tall grass are
 * named.
 *
 * homestead-stumps: summer, on the surface: a row of oak stumps and one of pine stumps, felled 0,
 * 3, 4, 13 and 14 growing days ago - stump, stump (a sapling tomorrow), sapling, sapling (a tree
 * tomorrow), tree. Sleep or wait a day and watch them grow.
 */
final class GatheringScenes {

	private GatheringScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Forest(),
			new Quarry(),
			new Riverbank(),
			new Stumps()
	};

	//a biome scene: find the ground off the render thread, travel there, then set the hero up
	private abstract static class Search implements DebugScenes.Scene {
		//set while the hero is on his way
		boolean travelling;

		abstract String what();
		abstract int[] find( long seed, float shift );
		abstract void arrive( Hero hero, OverworldLevel ow );

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				if (ow.isMirror()){
					GLog.w( "Scene: only the host can gather" );
					return;
				}
				arrive( hero, ow );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( what(), () -> find( seed, shift ), spot -> {
				if (spot == null){
					GLog.w( "Scene: found no " + what() + " near the origin in this world" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, spot[0], spot[1] );
			} );
		}
	}

	private static final class Forest extends Search {
		@Override public String id(){ return "homestead-forest"; }
		@Override public String title(){ return "Homestead: fell trees in a forest with every axe"; }
		@Override String what(){ return "a forest"; }
		@Override int[] find( long seed, float shift ){
			return DebugScenes.biomeGround( seed, shift, WorldModel.Biome.FOREST );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			IntPredicate oak = c -> ow.map[c] == Terrain.TREE_OAK && Gathering.refusal( hero, c, Gathering.Job.CHOP ) == null;
			IntPredicate pine = c -> ow.map[c] == Terrain.TREE_PINE && Gathering.refusal( hero, c, Gathering.Job.CHOP ) == null;
			for (Class<? extends Item> axe : new Class[]{ HandAxe.class, Axe.class, BattleAxe.class, Greataxe.class, WoodcuttersAxe.class }){
				give( hero, axe );
			}
			int spot = nearest( ow, hero.pos, 30, c -> standable( ow, c ) && beside( ow, c, oak ) && beside( ow, c, pine ) );
			if (spot == -1) spot = nearest( ow, hero.pos, 30, c -> standable( ow, c ) && (beside( ow, c, oak ) || beside( ow, c, pine )) );
			if (spot == -1){
				GLog.w( "Scene: no tree to fell within 30 cells; the axes are in your pack" );
				return;
			}
			ScrollOfTeleportation.appear( hero, spot );
			Dungeon.observe();
			GLog.i( "Scene: every axe is in your pack. Tap a tree beside you to cut it down: the axe in your hand"
					+ " is used, else the quickest you carry (woodcutter's axe 3 strokes, greataxe 4, battle axe 5,"
					+ " axe 6, hand axe 8). It falls away from you; oaks give 3-4 logs, pines 2-3 and sometimes resin." );
		}
	}

	private static final class Quarry extends Search {
		@Override public String id(){ return "homestead-quarry"; }
		@Override public String title(){ return "Homestead: break boulders and rock in the foothills"; }
		@Override String what(){ return "the foothills"; }
		@Override int[] find( long seed, float shift ){
			return DebugScenes.biomeGround( seed, shift, WorldModel.Biome.FOOTHILLS );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			give( hero, Pickaxe.class );
			give( hero, MasonsHammer.class );
			IntPredicate boulder = c -> ow.map[c] == Terrain.BOULDER && Gathering.refusal( hero, c, Gathering.Job.QUARRY ) == null;
			int spot = nearest( ow, hero.pos, 30, c -> standable( ow, c ) && beside( ow, c, boulder ) );
			if (spot != -1){
				ScrollOfTeleportation.appear( hero, spot );
				Dungeon.observe();
			}
			int rock = nearest( ow, hero.pos, 60, c -> (ow.map[c] == Terrain.WALL || ow.map[c] == Terrain.WALL_DECO)
					&& !ow.protectedForBuilding( c ) && ow.insideMap( c ) );
			GLog.i( "Scene: a pickaxe and a mason's hammer. " + (spot == -1 ? "No boulder within 30 cells."
					: "Tap the boulder beside you: the hammer breaks it in 2 blows (the pick alone in 3), for 2-3 rough stone.") );
			GLog.i( "Scene: " + (rock == -1 ? "no rock wall within 60 cells."
					: "the nearest rock wall is " + where( ow, hero.pos, rock ) + ": the pick breaks it in a blow, for its ore"
					+ " and 1-2 rough stone; drop the pick and the hammer breaks it in 3.") );
		}
	}

	private static final class Riverbank extends Search {
		@Override public String id(){ return "homestead-riverbank"; }
		@Override public String title(){ return "Homestead: dig clay, sand and farmland, reap tall grass"; }
		@Override String what(){ return "a river by the sea"; }
		@Override int[] find( long seed, float shift ){
			int[] mouth = riverMouth( seed, shift );
			return mouth != null ? mouth : DebugScenes.biomeGround( seed, shift, WorldModel.Biome.SWAMP );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			give( hero, Shovel.class );
			give( hero, Sickle.class );
			IntPredicate clay = c -> Gathering.dig( ow, c ) == Gathering.Dig.CLAY && !ow.protectedForBuilding( c ) && !ow.frozenAt( c );
			int spot = nearest( ow, hero.pos, 40, c -> standable( ow, c ) && (clay.test( c ) || beside( ow, c, clay )) );
			if (spot != -1){
				ScrollOfTeleportation.appear( hero, spot );
				Dungeon.observe();
			}
			int sand = nearest( ow, hero.pos, 80, c -> Gathering.dig( ow, c ) == Gathering.Dig.SAND && !ow.protectedForBuilding( c ) );
			int grass = nearest( ow, hero.pos, 80, c -> ow.map[c] == Terrain.HIGH_GRASS && !ow.protectedForBuilding( c ) );
			GLog.i( "Scene: a shovel and a sickle. Use the shovel's DIG and pick a spot beside you: "
					+ (spot == -1 ? "no clay bank within 40 cells" : "you stand on a bank that gives clay")
					+ "; grass or bare earth is tilled into farmland." );
			GLog.i( "Scene: " + (sand == -1 ? "no beach or desert sand in this window" : "sand " + where( ow, hero.pos, sand ))
					+ "; " + (grass == -1 ? "no tall grass in this window" : "tall grass for the sickle's REAP " + where( ow, hero.pos, grass )) + "." );
		}
	}

	private static final class Stumps implements DebugScenes.Scene {
		//set while the hero is on his way to the surface
		boolean travelling;
		//the growing days before today each stump was felled at
		static final int[] AGES = { 0, 3, 4, 13, 14 };

		@Override public String id(){ return "homestead-stumps"; }
		@Override public String title(){ return "Homestead: stumps growing back, a stage a column"; }

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
				GLog.w( "Scene: only the host can gather" );
				return;
			}
			if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.3f )){
				GLog.w( "Scene: a real-clock run keeps its own season: the stumps grow only in spring and summer" );
			}
			GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
			int origin = HomesteadScenes.findGround( ow, hero.pos );
			if (slice == null || origin == -1){
				GLog.w( "Scene: no open ground near the hero for the stumps; walk somewhere flatter" );
				return;
			}
			int w = ow.width(), today = WorldClock.day();
			int[] laid = new int[2 * AGES.length];
			for (int i = 0; i < AGES.length; i++){
				laid[2 * i] = lay( ow, slice, origin + 1 + 2 * i + w, Terrain.STUMP_OAK, felledFor( today, AGES[i] ) );
				laid[2 * i + 1] = lay( ow, slice, origin + 1 + 2 * i + 3 * w, Terrain.STUMP_PINE, felledFor( today, AGES[i] ) );
			}
			//redrawn as one, as the stumps growing back are
			Gathering.changed( ow, laid );
			slice.regrow( ow, today );
			Dungeon.observe();
			GLog.i( "Scene: oak stumps in the top row, pines below, felled (west to east) 0, 3, 4, 13 and 14 growing"
					+ " days ago: a stump, a stump (a sapling tomorrow), a sapling, a sapling (a tree tomorrow), a tree."
					+ " Only spring and summer days count. Wait a day and watch them grow." );
		}

		//the day a stump must have been felled to have `growing` spring or summer days behind it today
		private static int felledFor( int today, int growing ){
			int day = today;
			for (int n = 0; n < growing && day > today - 800; ){
				GameCalendar.Season s = GameCalendar.seasonOfDay( day );
				if (s == GameCalendar.Season.SPRING || s == GameCalendar.Season.SUMMER) n++;
				day--;
			}
			return day;
		}

		private static int lay( OverworldLevel ow, GatheringSlice slice, int cell, int stump, int felled ){
			Level.set( cell, stump, ow );
			slice.felled( ow, cell, felled );
			return cell;
		}
	}

	// ----------------------------------------------------------------- helpers

	//a tool in the pack (known, uncursed), at the hero's feet when the pack is full
	static void give( Hero hero, Class<? extends Item> cls ){
		Item item = Reflection.newInstance( cls );
		if (item == null) return;
		item.cursed = false;
		item.identify( false );
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}

	//a cell the hero may be set down on
	static boolean standable( OverworldLevel ow, int c ){
		return ow.passable[c] && !ow.pit[c] && (Actor.findChar( c ) == null || c == Dungeon.hero.pos)
				&& (Terrain.flags[ow.map[c]] & Terrain.LIQUID) == 0;
	}

	//anything beside the cell (its 8 neighbours) the test likes
	static boolean beside( OverworldLevel ow, int c, IntPredicate test ){
		for (int n : PathFinder.NEIGHBOURS8){
			int m = c + n;
			if (m >= 0 && m < ow.length() && Math.abs( m % ow.width() - c % ow.width() ) <= 1 && test.test( m )) return true;
		}
		return false;
	}

	//the cell nearest `from` (rings out to r, inside the window's rim) the test likes, -1 for none
	static int nearest( OverworldLevel ow, int from, int r, IntPredicate test ){
		int w = ow.width(), fx = from % w, fy = from / w;
		for (int d = 0; d <= r; d++){
			for (int dy = -d; dy <= d; dy++){
				for (int dx = -d; dx <= d; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != d) continue;
					int x = fx + dx, y = fy + dy;
					if (x < 2 || y < 2 || x >= w - 2 || y >= ow.height() - 2) continue;
					if (test.test( x + y * w )) return x + y * w;
				}
			}
		}
		return -1;
	}

	//"12 cells to the north-east" of `from`: north is up the screen
	static String where( OverworldLevel ow, int from, int cell ){
		int w = ow.width(), dx = cell % w - from % w, dy = cell / w - from / w;
		int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
		if (d == 0) return "under you";
		return d + (d == 1 ? " cell" : " cells") + " to the "
				+ DebugScenes.compass( (float) Math.toDegrees( Math.atan2( dx, -dy ) ) );
	}

	/**
	 * Debug and tooling: a river's water with a beach within 24 cells, out of every town and
	 * village, nearest the world's origin (rings 64 cells apart out to 2560); null when none.
	 * Pure: it runs off the render thread (DebugScenes.search).
	 */
	static int[] riverMouth( long seed, float shift ){
		WorldModel.Sample s = new WorldModel.Sample();
		final int step = 64, rings = 40;
		for (int r = 0; r <= rings; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int cx = i * step, cy = j * step;
					//a river crossing the probe's row
					for (int x = cx - step / 2; x < cx + step / 2; x += 4){
						if (WorldModel.sample( seed, x, cy, shift, s ).biome != WorldModel.Biome.RIVER) continue;
						if (WorldStructures.townCell( x, cy ) != -1 || OverworldFauna.nearSettlement( seed, x, cy )) continue;
						if (beachNear( seed, shift, x, cy, s )) return new int[]{ x, cy };
					}
				}
			}
		}
		return null;
	}

	private static boolean beachNear( long seed, float shift, int x, int y, WorldModel.Sample s ){
		for (int dy = -24; dy <= 24; dy += 4){
			for (int dx = -24; dx <= 24; dx += 4){
				if (WorldModel.sample( seed, x + dx, y + dy, shift, s ).biome == WorldModel.Biome.BEACH) return true;
			}
		}
		return false;
	}
}
