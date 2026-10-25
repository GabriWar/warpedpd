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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Mounts;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Taming;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Saddle;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The horses' debug scenes (the Scenes menu's "Homestead" group, homestead/mounts):
 *
 * homestead-horse: a summer day on the surface (south of the town when the hero is elsewhere): a
 * tamed, saddled horse of his beside him, a spare saddle, a rope and feed in the pack. Tap it to
 * ride; ride at a door, a cave mouth or a drop; strike something to jump down.
 *
 * homestead-horse-herd: a summer day in a meadow or on the plains near the world's origin: a herd
 * of four wild horses grazing eight cells off, and feed, a saddle and ropes in the pack: tame one.
 *
 * homestead-stable: a day in the human village of five houses or more nearest the origin, by its
 * well, where the stablehand keeps a saddled horse for sale; gold in the purse to buy it.
 */
final class HorseScenes {

	private HorseScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Saddled(),
			new Herd(),
			new Stable()
	};

	//a tamed, saddled horse beside the hero
	private static final class Saddled implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "homestead-horse"; }
		@Override public String title(){ return "Homestead: a tamed, saddled horse beside you"; }

		@Override public void apply( Hero hero ){
			if (!onSurface()){
				if (travelling){
					travelling = false;
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
						WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) + 40 );
				return;
			}
			travelling = false;
			if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.2f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			if (ow.isMirror()){
				GLog.w( "Scene: only the host rides" );
				return;
			}
			int cell = besideHero( ow, hero );
			if (cell == -1){
				GLog.w( "Scene: no free ground beside the hero; step somewhere open" );
				return;
			}
			Horse h = new Horse();
			h.tame = true;
			h.trust = Taming.TAME_TRUST;
			h.saddled = true;
			h.setLook();
			h.pos = cell;
			GameScene.add( h );
			give( hero, new Saddle() );
			give( hero, new Rope() );
			give( hero, new Thatch().quantity( 10 ) );
			give( hero, new Cornwheatshaft().quantity( 3 ) );
			GLog.i( "Scene: your saddled horse beside you, a spare saddle, a rope and feed in your pack. Tap it to ride"
					+ " (hold the tap for its menu: unsaddle, feed, rope, set free); tap yourself to get down. Ride at a"
					+ " door or a cave mouth to see it wait outside, at a drop to see it refuse, into a fight to jump down." );
		}
	}

	//a herd of wild horses on the grass, and feed to tame one
	private static final class Herd implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "homestead-horse-herd"; }
		@Override public String title(){ return "Homestead: a herd of wild horses on the grass"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive( hero );
				return;
			}
			if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.1f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "a meadow or plains near the origin",
					() -> DebugScenes.biomeGround( seed, shift, WorldModel.Biome.MEADOW, WorldModel.Biome.PLAINS ),
					spot -> {
						if (spot == null){
							GLog.w( "Scene: no meadow or plains near the origin in this world" );
							return;
						}
						travelling = true;
						DebugScenes.travelThen( this, 0, spot[0], spot[1] );
					} );
		}

		private void arrive( Hero hero ){
			if (!onSurface() || ((OverworldLevel) Dungeon.level).isMirror()){
				GLog.w( "Scene: could not reach the surface" );
				return;
			}
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			int w = ow.width(), placed = 0;
			int[][] at = { { 8, 0 }, { 9, 1 }, { 8, 2 }, { 10, -1 } };
			for (int[] d : at){
				int c = freeNear( ow, hero.pos + d[0] + d[1] * w );
				if (c == -1) continue;
				Horse h = new Horse();
				h.pos = c;
				GameScene.add( h );
				placed++;
			}
			give( hero, new Thatch().quantity( 12 ) );
			give( hero, new Cornwheatshaft().quantity( 4 ) );
			give( hero, new Aprico().quantity( 2 ) );
			give( hero, new Saddle() );
			give( hero, new Rope().quantity( 2 ) );
			GLog.i( "Scene: " + placed + " wild horses grazing eight cells east. They bolt within " + Taming.SHY
					+ " cells. Throw thatch near one and stand back: it walks over and eats, and lets you a cell closer"
					+ " each meal. After three, tap it to feed it by hand: the fourth makes it yours. Then saddle it from"
					+ " its menu (hold the tap) and ride." );
		}
	}

	//the nearest village's stable
	private static final class Stable implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "homestead-stable"; }
		@Override public String title(){ return "Homestead: the nearest village's stable"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				Dungeon.gold += 2000;
				GLog.i( "Scene: by the village well. The stablehand stands a few steps off with a saddled horse for sale,"
						+ " a saddle and hay on his rail: tap the horse to buy it (2000 gold added to your purse)." );
				return;
			}
			if (!DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, 0.1f )){
				GLog.w( "Scene: a real-clock run keeps its own hour" );
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			DebugScenes.search( "a village with a stable near the origin", () -> village( seed ), spot -> {
				if (spot == null){
					GLog.w( "Scene: no village of five houses or more near the origin in this world" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, spot[0], spot[1] + 2 );
			} );
		}

		//the well of the human village of five houses or more nearest the origin, null for none
		private static int[] village( long seed ){
			for (int r = 0; r < 8; r++){
				for (int sy = -r; sy <= r; sy++){
					for (int sx = -r; sx <= r; sx++){
						if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
						VillageRoutine.Settlement s = VillageRoutine.settlement( seed, WorldStructures.sectorOf( sx, sy ) );
						if (s != null && !s.gnoll && VillageRoutine.vendorHouses( s.houses() ) >= 1){
							return new int[]{ s.cx, s.cy };
						}
					}
				}
			}
			return null;
		}
	}

	// ----------------------------------------------------------------- helpers

	private static boolean onSurface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0;
	}

	//a free open cell beside the hero a horse could be ridden from, -1 for none
	private static int besideHero( OverworldLevel ow, Hero hero ){
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (c >= 0 && c < ow.length() && Mounts.rideable( ow, c ) && ow.passable[c] && !ow.pit[c] && !ow.avoid[c]
					&& !ow.water[c] && Actor.findChar( c ) == null) return c;
		}
		return -1;
	}

	//the free open ground nearest a cell (within 4), -1 for none
	private static int freeNear( OverworldLevel ow, int cell ){
		int width = ow.width(), cx = cell % width, cy = cell / width;
		for (int r = 0; r <= 4; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cx + dx, y = cy + dy;
					if (x < 2 || y < 2 || x >= width - 2 || y >= ow.height() - 2) continue;
					int c = x + y * width;
					if (ow.passable[c] && !ow.pit[c] && !ow.water[c] && !ow.avoid[c] && !Mounts.indoors( ow, c )
							&& Actor.findChar( c ) == null) return c;
				}
			}
		}
		return -1;
	}

	private static void give( Hero hero, Item item ){
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}
}
