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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.MagicWorldMap;
import xyz.gabriwar.warpedpixeldungeon.items.catching.BugNet;
import xyz.gabriwar.warpedpixeldungeon.items.catching.CatchWorld;
import xyz.gabriwar.warpedpixeldungeon.items.catching.CaughtFish;
import xyz.gabriwar.warpedpixeldungeon.items.catching.Critter;
import xyz.gabriwar.warpedpixeldungeon.items.catching.CritterJar;
import xyz.gabriwar.warpedpixeldungeon.items.catching.Earthworm;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishSpecies;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishingRod;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishingWaters;
import xyz.gabriwar.warpedpixeldungeon.items.catching.MessageBottle;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.function.IntPredicate;

/**
 * The bug net's and the fishing rod's debug scenes (the Scenes menu's "Homestead" group;
 * items/catching):
 *
 * homestead-catch-pond: a summer's day where a meadow, a river and a wood meet
 * (OverworldCritters.findCritterGround), the hero set down by the water with the net, the rod,
 * worms and a few critters in jars; butterflies flutter up round him (OverworldCritters.showcase).
 *
 * homestead-catch-night: the same place on a summer's night: fireflies to net, and the night's
 * catfish to catch.
 *
 * homestead-catch-ice: a winter's day on the frozen tundra, the hero beside the ice with the rod,
 * worms and a firefly: a hole is cut in the ice beside him (arctic char).
 *
 * homestead-catch-cave: a cave's pool three slices down, the rod and glowing bait (cave fish).
 *
 * homestead-catch-bottle: a message in a bottle, a shovel and a world map in the pack, and a cache
 * already buried a few cells off (its mark on the map); a bottle read marks another farther off.
 *
 * homestead-catch-fisher: a village by the water at work, the hero beside one of its fishers with a
 * few raw fish and gold: talk to him to trade.
 */
final class CatchScenes {

	private CatchScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Pond( false ),
			new Pond( true ),
			new Ice(),
			new Cave(),
			new Bottle(),
			new Fisher()
	};

	//a scene somewhere in the world: found off the render thread, travelled to, then set up there
	private abstract static class Search implements DebugScenes.Scene {
		//set while the hero is on his way
		boolean travelling;

		abstract String what();
		abstract int altitude();
		abstract int[] find( long seed, float shift );
		abstract void before();
		abstract void arrive( Hero hero, OverworldLevel ow );

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != altitude()){
					GLog.w( "Scene: could not reach the slice" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				if (ow.isMirror()){
					GLog.w( "Scene: only the host can net and fish" );
					return;
				}
				arrive( hero, ow );
				return;
			}
			before();
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( what(), () -> find( seed, shift ), spot -> {
				if (spot == null){
					GLog.w( "Scene: found no " + what() + " near the origin in this world" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, altitude(), spot[0], spot[1] );
			} );
		}
	}

	private static final class Pond extends Search {
		final boolean night;

		Pond( boolean night ){
			this.night = night;
		}

		@Override public String id(){ return night ? "homestead-catch-night" : "homestead-catch-pond"; }
		@Override public String title(){
			return night ? "Catching: fireflies and night fishing by a river" : "Catching: net butterflies and fish a river on a summer day";
		}
		@Override String what(){ return "a meadow by a river and a wood"; }
		@Override int altitude(){ return 0; }
		@Override int[] find( long seed, float shift ){
			return OverworldCritters.findCritterGround( seed, shift );
		}

		@Override void before(){
			if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, night ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DAY, 0.3f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			DebugScenes.clearWeather();
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			give( hero, new BugNet() );
			give( hero, new FishingRod() );
			give( hero, new Earthworm().quantity( 12 ) );
			give( hero, new CritterJar( Critter.BRIMSTONE ).quantity( 2 ) );
			give( hero, new CritterJar( Critter.BEETLE ).quantity( 2 ) );
			give( hero, new CritterJar( Critter.FROG ) );
			int spot = byWater( ow, hero.pos, 40 );
			if (spot != -1){
				ScrollOfTeleportation.appear( hero, spot );
				Dungeon.observe();
			}
			GLog.i( "Scene: " + OverworldCritters.showcase( night ) );
			GLog.i( "Scene: the net, the rod, 12 worms and jars in your pack. SWING the net at a critter up to "
					+ BugNet.REACH + " cells off (butterflies by day, fireflies at night); CAST the rod at open water up to 4 cells off."
					+ " You rest while the bobber floats; a bite rouses you: REEL within 3 turns. BAIT picks what goes on the hook." );
			if (spot == -1) GLog.w( "Scene: no open water within 40 cells to stand by" );
		}
	}

	private static final class Ice extends Search {
		@Override public String id(){ return "homestead-catch-ice"; }
		@Override public String title(){ return "Catching: fish through a hole in the ice"; }
		@Override String what(){ return "frozen tundra"; }
		@Override int altitude(){ return 0; }
		@Override int[] find( long seed, float shift ){
			return DebugScenes.biomeGround( seed, shift, WorldModel.Biome.TUNDRA, WorldModel.Biome.SNOWFIELD );
		}

		@Override void before(){
			if (!DebugScenes.walkClock( GameCalendar.Season.WINTER, DayNightCycle.Phase.DAY, 0.3f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			DebugScenes.clearWeather();
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			give( hero, new FishingRod() );
			give( hero, new Earthworm().quantity( 12 ) );
			give( hero, new CritterJar( Critter.FIREFLY ).quantity( 3 ) );
			IntPredicate ice = c -> ow.map[c] == Terrain.FROZEN_WATER && !ow.inTown( c );
			int spot = nearest( ow, hero.pos, 50, c -> standable( ow, c ) && beside( ow, c, ice ) );
			if (spot != -1){
				ScrollOfTeleportation.appear( hero, spot );
				Dungeon.observe();
			}
			GLog.i( "Scene: winter, the rod, 12 worms and 3 fireflies. " + (spot == -1 ? "No ice within 50 cells."
					: "CAST at the ice beside you: a hole is cut and the line goes through it (arctic char, perch, pike;"
					+ " worms and a glowing firefly are the char's favourites).") );
		}
	}

	private static final class Cave extends Search {
		static final int ALTITUDE = -3;

		@Override public String id(){ return "homestead-catch-cave"; }
		@Override public String title(){ return "Catching: blind cave fish in an underground pool"; }
		@Override String what(){ return "a cave chamber by a pool"; }
		@Override int altitude(){ return ALTITUDE; }
		@Override int[] find( long seed, float shift ){
			return DebugScenes.faunaChamber( seed, ALTITUDE );
		}

		@Override void before(){
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			give( hero, new FishingRod() );
			give( hero, new Earthworm().quantity( 6 ) );
			give( hero, new CritterJar( Critter.FIREFLY ).quantity( 3 ) );
			give( hero, new CritterJar( Critter.EMBER_BEETLE ).quantity( 2 ) );
			int spot = byWater( ow, hero.pos, 40 );
			if (spot != -1){
				ScrollOfTeleportation.appear( hero, spot );
				Dungeon.observe();
			}
			GLog.i( "Scene: three slices down, the rod, worms, fireflies and ember beetles. " + (spot == -1
					? "No pool within 40 cells." : "CAST at the pool: blind cave fish come to a glowing bait best; no hour or weather counts down here.") );
		}
	}

	private static final class Bottle implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "homestead-catch-bottle"; }
		@Override public String title(){ return "Catching: a message in a bottle and its buried cache"; }

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
						WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) + 60 );
				return;
			}
			travelling = false;
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			CatchWorld world = CatchWorld.get();
			if (ow.isMirror() || world == null){
				GLog.w( "Scene: only the host keeps the bottles' caches" );
				return;
			}
			give( hero, new MessageBottle().quantity( 2 ) );
			if (hero.belongings.getItem( Shovel.class ) == null) give( hero, new Shovel() );
			if (hero.belongings.getItem( MagicWorldMap.class ) == null) give( hero, new MagicWorldMap() );
			int w = ow.width(), hx = hero.pos % w, hy = hero.pos / w;
			int cache = nearest( ow, hero.pos, 12, c -> Math.max( Math.abs( c % w - hx ), Math.abs( c / w - hy ) ) >= 4
					&& Gathering.dig( ow, c ) != null && Gathering.dig( ow, c ) != Gathering.Dig.STUMP
					&& Gathering.dig( ow, c ) != Gathering.Dig.SAPLING && !ow.protectedForBuilding( c ) && !ow.frozenAt( c ) );
			if (cache != -1){
				world.addCache( ow.worldX() + cache % w, ow.worldY() + cache / w );
				GLog.i( "Scene: a cache is buried " + where( ow, hero.pos, cache )
						+ ", marked on the world map: dig it (or beside it) with the shovel's DIG for a chest." );
			} else {
				GLog.w( "Scene: no open ground to bury a cache in near you; walk somewhere greener" );
			}
			GLog.i( "Scene: 2 messages in bottles, a shovel and a world map. READ a bottle under the open sky: its note marks a"
					+ " cache 40 to 120 cells off, and the bottle is left empty." );
		}
	}

	private static final class Fisher extends Search {
		//the village found: {sx, sy, cx, cy, houses}
		int[] found;

		@Override public String id(){ return "homestead-catch-fisher"; }
		@Override public String title(){ return "Catching: trade with a village fisher"; }
		@Override String what(){ return "a village by the water"; }
		@Override int altitude(){ return 0; }
		@Override int[] find( long seed, float shift ){
			ArrayList<int[]> near = WorldStructures.settlementsNear( seed, 0, 0, WorldStructures.Faction.HUMAN, 5, 12 );
			for (int i = 0; i < near.size() && i < 16; i++){
				if (VillageRoutine.shoreNear( seed, near.get( i )[0], near.get( i )[1] )){
					found = near.get( i );
					return new int[]{ found[2], found[3] };
				}
			}
			return null;
		}

		@Override void before(){
			if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.25f )){
				GLog.w( "Scene: a real-clock run keeps its own hour" );
			}
			DebugScenes.clearWeather();
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			give( hero, new CaughtFish( FishSpecies.TROUT ).quantity( 2 ) );
			give( hero, new CaughtFish( FishSpecies.PIKE ) );
			Dungeon.gold += 300;
			Settler fisher = null;
			int best = Integer.MAX_VALUE, w = ow.width();
			for (Mob m : ow.mobs){
				if (!(m instanceof Settler)) continue;
				Settler s = (Settler) m;
				//folk just laid down with the window learn their trade on their first turn: learn it now
				if (s.role < 0){
					VillageRoutine.Settlement home = VillageRoutine.settlement( ow.worldSeed(), s.settlementKey() );
					if (home != null && home.gnoll == (s instanceof GnollVillager)){
						s.adopt( home, ow.worldX() + s.pos % w, ow.worldY() + s.pos / w, ow );
					}
				}
				if (s.role != VillageRoutine.Role.FISHER.ordinal() || s.state == s.SLEEPING) continue;
				int d = ow.distance( hero.pos, s.pos );
				if (d < best){
					best = d;
					fisher = s;
				}
			}
			if (fisher == null){
				GLog.w( "Scene: no fisher of this village is about; wait for the day's work, or try another village" );
				return;
			}
			final int at = fisher.pos;
			int spot = nearest( ow, at, 4, c -> c != at && standable( ow, c ) );
			if (spot != -1){
				ScrollOfTeleportation.appear( hero, spot );
				Dungeon.observe();
			}
			GLog.i( "Scene: a fisher of " + WorldStructures.villageName( OverworldLevel.worldSeedOf( Dungeon.seed ), found[0], found[1] )
					+ " is " + where( ow, hero.pos, at ) + "; 3 raw fish and 300 gold in your pack. Talk to him:"
					+ " he buys your catch and sells rods and worms." );
		}
	}

	// ----------------------------------------------------------------- helpers

	//into the pack (known, uncursed), at the hero's feet when it is full
	private static void give( Hero hero, Item item ){
		item.cursed = false;
		item.identify( false );
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}

	//a dry cell the hero may be set down on
	private static boolean standable( OverworldLevel ow, int c ){
		return ow.passable[c] && !ow.pit[c] && (Actor.findChar( c ) == null || c == Dungeon.hero.pos)
				&& (Terrain.flags[ow.map[c]] & Terrain.LIQUID) == 0;
	}

	//a standable cell within two of open water a line can go into, nearest `from`; -1 for none
	private static int byWater( OverworldLevel ow, int from, int r ){
		IntPredicate water = c -> FishingWaters.kindAt( ow, c ) != null && ow.map[c] != Terrain.FROZEN_WATER;
		int spot = nearest( ow, from, r, c -> standable( ow, c ) && beside( ow, c, water ) );
		if (spot != -1) return spot;
		return nearest( ow, from, r, c -> standable( ow, c ) && within( ow, c, 2, water ) );
	}

	//anything beside the cell (its 8 neighbours) the test likes
	private static boolean beside( OverworldLevel ow, int c, IntPredicate test ){
		for (int n : PathFinder.NEIGHBOURS8){
			int m = c + n;
			if (m >= 0 && m < ow.length() && Math.abs( m % ow.width() - c % ow.width() ) <= 1 && test.test( m )) return true;
		}
		return false;
	}

	private static boolean within( OverworldLevel ow, int c, int r, IntPredicate test ){
		int w = ow.width(), cx = c % w, cy = c / w;
		for (int y = Math.max( 0, cy - r ); y <= Math.min( ow.height() - 1, cy + r ); y++){
			for (int x = Math.max( 0, cx - r ); x <= Math.min( w - 1, cx + r ); x++){
				if (test.test( x + y * w )) return true;
			}
		}
		return false;
	}

	//the cell nearest `from` (rings out to r, inside the window's rim) the test likes, -1 for none
	private static int nearest( OverworldLevel ow, int from, int r, IntPredicate test ){
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
	private static String where( OverworldLevel ow, int from, int cell ){
		int w = ow.width(), dx = cell % w - from % w, dy = cell / w - from / w;
		int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
		if (d == 0) return "under you";
		return d + (d == 1 ? " cell" : " cells") + " to the "
				+ DebugScenes.compass( (float) Math.toDegrees( Math.atan2( dx, -dy ) ) );
	}
}
