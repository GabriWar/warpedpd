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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWater;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfCorruption;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.EclipseEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.FloodEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WildfireEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * The nature events' debug scenes (the Scenes menu's "Surface" group; levels/overworld:
 * WildfireEvent, FloodEvent, EclipseEvent). Each forces its event through the world's own registry
 * and runs the hero's step pass at once, so it happens in front of the tester.
 *
 * overworld-wildfire: the nearest woodland or meadow a fire could burn out from, searched off the
 * render thread; the clock walked on to the day, the rain held off (the Weather tab's rain, or the
 * weather-rain scenes, put it out); the hero set down eight cells west of where the fire caught,
 * a fence of five pieces two cells east of him (the homestead it warns), six bottles of water in
 * his pack. The fire caught a thousand turns ago, so it is well alight: a ring of flames and a
 * black, stumped heart, smoke over the fog. Pour or throw the water on its edge to stop it there.
 *
 * overworld-flood: the nearest low river bank, the clock walked on to the day; the rivers forced
 * over their banks to the fifth level as at the height of a flood (the water up a moment ago), a
 * field of two crops beside the hero - one on the low bank, which drowns, one on higher ground,
 * which is spared. Walk the muddy road.
 *
 * overworld-flood-mud: the same bank after the water drained: the banks under mud, a crop sown on
 * the silt (it grows half again as fast).
 *
 * overworld-eclipse: on the surface by day, an eclipse forced to darken the sun in ten turns, a
 * wand of corruption in the pack (half again as strong in the dark). Watch the light fall to
 * dusk, the corona and the stars, the night's beasts come out.
 *
 * overworld-eclipse-eve: the nearest human village by day, an eclipse forced for half a day on:
 * the elders' word in the log, and its elder (talk to the old folk at the well) speaks of it.
 */
final class NatureEventScenes {

	private NatureEventScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Wildfire(),
			new Flood( false ),
			new Flood( true ),
			new Eclipse( false ),
			new Eclipse( true )
	};

	//how long before now the scene's wildfire caught: well alight by the time the hero stands there
	private static final int FIRE_AGO = 1000;
	//a world column out past the town's land, on its east road
	private static final int EAST_OF_TOWN = 110;

	// ------------------------------------------------------------------ helpers

	private static boolean surface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0;
	}

	//the hero's world cell on the surface, the town's square elsewhere
	private static int[] here( Hero hero ){
		if (!surface()) return new int[]{ 0, 0 };
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		return new int[]{ ow.worldX + hero.pos % ow.width(), ow.worldY + hero.pos / ow.width() };
	}

	private static void give( Hero hero, Item item ){
		if (!item.collect( hero.belongings.backpack )) Dungeon.level.drop( item, hero.pos ).sprite.drop();
	}

	//open dry ground nobody stands on
	private static boolean open( OverworldLevel ow, int cell ){
		return cell > 0 && cell < ow.length() && ow.passable[cell] && !ow.water[cell] && !ow.pit[cell]
				&& Actor.findChar( cell ) == null && ow.heaps.get( cell ) == null && !ow.inTown( cell );
	}

	//a field of one crop on a cell: farmland laid there (the build menu's own piece), the seed sown
	private static boolean field( OverworldLevel ow, int cell, Class<? extends xyz.gabriwar.warpedpixeldungeon.plants.Plant.Seed> seed ){
		if (!open( ow, cell )) return false;
		Homestead.build( ow, cell, BuildCatalog.byId( "farm_plot" ), Dungeon.hero );
		if (ow.map[cell] != Terrain.FARMLAND && ow.map[cell] != Terrain.FARMLAND_WET) return false;
		Farming.plant( ow, cell, seed );
		return true;
	}

	// ------------------------------------------------------------------ the scenes

	private static final class Wildfire implements DebugScenes.Scene {
		//set while the hero is on his way there, so the arrival lights the fire instead of sending him off again
		boolean travelling;
		int spotX, spotY;
		@Override public String id(){ return "overworld-wildfire"; }
		@Override public String title(){ return "Surface: a wildfire, a homestead in its path"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!surface()){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				//held dry: the rain would put it out at once
				ClimateManager.debugPrecipOverride = 0f;
				ClimateManager.debugForceStorm = false;
				int built = 0;
				for (int dy = -2; dy <= 2; dy++){
					int c = ow.localCell( spotX - 6, spotY + dy );
					if (c == -1 || !open( ow, c )) continue;
					Homestead.build( ow, c, BuildCatalog.byId( "fence" ), hero );
					built++;
				}
				give( hero, new PotionOfWater().quantity( 6 ) );
				//never further back than the last rain, which would have put it out
				int ago = FIRE_AGO;
				int rain = ClimateManager.lastRainTurn();
				if (rain != Integer.MIN_VALUE) ago = Math.max( 0, Math.min( ago, ow.eventTurn() - rain - 1 ) );
				WildfireEvent.force( ow, spotX, spotY, ago );
				ow.pollEvents();
				GLog.i( "Scene: a wildfire caught " + ago + " turns ago, eight cells east (" + built
						+ " fence pieces between you and it, 6 bottles of water in your pack). Pour or throw water on"
						+ " its burning edge to stop it there; the rain (Weather tab) puts it all out." );
				return;
			}
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			int[] at = here( hero );
			DebugScenes.search( "woodland a fire could burn", () -> WildfireEvent.findSpot( seed, at[0], at[1], 900 ), spot -> {
				if (spot == null){
					GLog.w( "Scene: no woodland or meadow for a fire near here" );
					return;
				}
				if (!DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, 0.3f )){
					GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
				}
				spotX = spot[0];
				spotY = spot[1];
				travelling = true;
				DebugScenes.travelThen( this, 0, spotX - 8, spotY );
			} );
		}
	}

	private static final class Flood implements DebugScenes.Scene {
		final boolean after;
		boolean travelling;
		Flood( boolean after ){
			this.after = after;
		}
		@Override public String id(){ return after ? "overworld-flood-mud" : "overworld-flood"; }
		@Override public String title(){
			return after ? "Surface: a river after its flood, mud on the banks" : "Surface: a river in flood";
		}
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!surface()){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				ClimateManager.debugPrecipOverride = Float.NaN;
				int turn = ow.eventTurn();
				int start = turn - FloodEvent.RISE_TURNS
						- (after ? FloodEvent.HIGH_TURNS + FloodEvent.FALL_TURNS + 20 : 0);
				FloodEvent.force( start, 5 );
				//a crop on the lowest bank near the hero, and (in the water) one on higher ground
				int low = -1, high = -1;
				int w = ow.width(), hx = hero.pos % w, hy = hero.pos / w;
				for (int r = 1; r <= 6 && (low == -1 || high == -1); r++){
					for (int dy = -r; dy <= r; dy++){
						for (int dx = -r; dx <= r; dx++){
							if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
							int c = hx + dx + (hy + dy) * w;
							if (!open( ow, c ) || ow.map[c] == Terrain.DIRT_PATH) continue;
							int b = ow.bankAt( c );
							if (low == -1 && b > 0 && b <= 3) low = c;
							else if (high == -1 && b == 0) high = c;
						}
					}
				}
				int fields = 0;
				if (low != -1 && field( ow, low, Cornwheat.Seed.class )) fields++;
				if (!after && high != -1 && field( ow, high, Cornwheat.Seed.class )) fields++;
				ow.pollEvents();
				GLog.i( after
						? "Scene: the river flooded and drained; the banks lie under mud for four days (slow going), and "
						+ fields + " crop was sown on the silt (half again as fast)."
						: "Scene: the river is at the height of a flood (two days high water); " + fields
						+ " crops sown - the one on the low bank drowns, the one on higher ground is spared. The roads under"
						+ " the water are mud." );
				return;
			}
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			int[] at = here( hero );
			DebugScenes.search( "a low river bank", () -> FloodEvent.findBank( seed, at[0], at[1], 3, 1600 ), bank -> {
				if (bank == null){
					GLog.w( "Scene: no river bank near here" );
					return;
				}
				if (!DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, 0.3f )){
					GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, bank[0], bank[1] );
			} );
		}
	}

	private static final class Eclipse implements DebugScenes.Scene {
		final boolean eve;
		boolean travelling;
		Eclipse( boolean eve ){
			this.eve = eve;
		}
		@Override public String id(){ return eve ? "overworld-eclipse-eve" : "overworld-eclipse"; }
		@Override public String title(){
			return eve ? "Surface: an eclipse heralded by a village's elders" : "Surface: a solar eclipse at midday";
		}
		@Override public void apply( Hero hero ){
			if (travelling || (!eve && surface())){
				travelling = false;
				if (!surface()){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				if (!DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, eve ? 0.15f : 0.35f )){
					GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
				}
				ClimateManager.debugCloudOverride = 0f;
				if (eve){
					EclipseEvent.force( ow, DayNightCycle.FULL_CYCLE / 2 );
					GLog.i( "Scene: the elders have read an eclipse in the sky for half a day from now - talk to an old"
							+ " villager by the well. The world map pins it." );
				} else {
					EclipseEvent.force( ow, 10 );
					WandOfCorruption wand = new WandOfCorruption();
					wand.identify();
					give( hero, wand );
					GLog.i( "Scene: the moon covers the sun in ten turns, for " + EclipseEvent.TURNS + " turns: the light"
							+ " falls to dusk, the corona and a few stars show, the night's beasts come out. A wand of"
							+ " corruption is in your pack (half again as strong in the dark)." );
				}
				ow.pollEvents();
				return;
			}
			if (!eve){
				//out on the meadows east of the town, where the wild beasts roam
				travelling = true;
				DebugScenes.travelThen( this, 0, EAST_OF_TOWN, 4 );
				return;
			}
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			int[] at = here( hero );
			ArrayList<int[]> near = WorldStructures.settlementsNear( seed, at[0], at[1], WorldStructures.Faction.HUMAN, 3, 8 );
			if (near.isEmpty()){
				GLog.w( "Scene: no human village within eight sectors" );
				return;
			}
			travelling = true;
			DebugScenes.travelThen( this, 0, near.get( 0 )[2], near.get( 0 )[3] + 3 );
		}
	}
}
