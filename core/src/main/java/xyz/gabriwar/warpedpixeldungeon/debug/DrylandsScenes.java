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
import xyz.gabriwar.warpedpixeldungeon.actors.Sandstorm;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.EmptyBottle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Delves;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Drylands;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * The desert's debug scenes (the Scenes menu's "Surface" group; levels/overworld/Drylands,
 * actors/Sandstorm), each going to the place of its kind nearest the world's origin, with the fog
 * back on - what players see:
 *
 * overworld-oasis: a summer's day just south of an oasis' grass, an empty bottle in the pack. The
 * trader is on his rug, ripe dates lie under a few palms, the pool fills the bottle, and the log
 * says how much cooler the shade is than the sand.
 *
 * overworld-oasis-dusk: the same oasis at dusk, the hero 24 cells south of its water and out of
 * sight of it: deer come down to drink while nobody is near, and are there when he walks up.
 *
 * overworld-buried-temple: a summer's day just south of the buried temple's top: its name, level
 * and floors in the log, the stairwell two cells north.
 *
 * overworld-temple-guardian: down that temple's crypt to its deepest floor at once, the hero set
 * beside its guardian statue (strike it to wake it; its hoard holds the Sun Scarab).
 *
 * overworld-sandstorm: desert ground on a summer's day, the wind held at 18 m/s from the east: the
 * sand driven west, the hero's sight, speed and eyes taken, and the nearest lee named (the cell
 * west of something solid). The oasis trader sells the goggles and the scarf that keep it off.
 */
final class DrylandsScenes {

	private DrylandsScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new OasisDay(),
			new OasisDusk(),
			new TempleTop(),
			new TempleGuardian(),
			new Storm()
	};

	//the place of a kind nearest the world's origin, off the render thread: {cx, cy, sx, sy}, null for none
	private static int[] nearest( long seed, WorldStructures.Site kind ){
		ArrayList<Drylands.Place> found = Drylands.near( seed, 0, 0, kind, 40 );
		if (found.isEmpty()) return null;
		Drylands.Place p = found.get( 0 );
		return new int[]{ p.cx, p.cy, p.sx, p.sy };
	}

	private static Drylands.Place place( long seed, WorldStructures.Site kind, int[] at ){
		for (Drylands.Place p : Drylands.near( seed, at[0], at[1], kind, 0 )) return p;
		return null;
	}

	//a scene that finds its place, travels to a cell by it and sets the hero up there
	private abstract static class ToPlace implements DebugScenes.Scene {
		boolean travelling;
		int[] at;

		abstract WorldStructures.Site kind();
		abstract int[] target( long seed, Drylands.Place p );
		abstract void arrive( Hero hero, OverworldLevel ow, Drylands.Place p );

		@Override public void apply( Hero hero ){
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				arrive( hero, (OverworldLevel) Dungeon.level, place( seed, kind(), at ) );
				return;
			}
			String what = kind() == WorldStructures.Site.OASIS ? "an oasis" : "a buried temple";
			DebugScenes.search( what, () -> nearest( seed, kind() ), p -> {
				if (p == null){
					GLog.w( "Scene: found no " + what + " within 40 sectors of the origin in this world" );
					return;
				}
				at = p;
				int[] to = target( seed, place( seed, kind(), p ) );
				travelling = true;
				DebugScenes.travelThen( this, 0, to[0], to[1] );
			} );
		}
	}

	//"12 cells to the north-east" of `from`
	private static String where( OverworldLevel ow, int from, int cell ){
		int w = ow.width(), dx = cell % w - from % w, dy = cell / w - from / w;
		int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
		if (d == 0) return "under you";
		return d + (d == 1 ? " cell" : " cells") + " to the " + DebugScenes.compass( (float) Math.toDegrees( Math.atan2( dx, -dy ) ) );
	}

	private static void give( Hero hero, Item item ){
		item.identify( false );
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}

	//a calm, dry summer's day (or dusk), the fog back on
	private static void calmDay( DayNightCycle.Phase phase, float into ){
		if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, phase, into )){
			GLog.w( "Scene: a real-clock run keeps its own hours" );
		}
		new DebugScenes.Sky( null, 0f, DebugScenes.CALM, Float.NaN, false ).hold();
		Dungeon.debugNoFog = false;
	}

	private static final class OasisDay extends ToPlace {
		@Override public String id(){ return "overworld-oasis"; }
		@Override public String title(){ return "Surface: an oasis by day - its trader, dates, water and shade"; }
		@Override WorldStructures.Site kind(){ return WorldStructures.Site.OASIS; }
		@Override int[] target( long seed, Drylands.Place p ){ return Drylands.approach( seed, p ); }

		@Override void arrive( Hero hero, OverworldLevel ow, Drylands.Place p ){
			calmDay( DayNightCycle.Phase.DAY, 0.35f );
			give( hero, new EmptyBottle().quantity( 2 ) );
			Dungeon.observe();
			int heart = ow.localCell( p.cx, p.cy );
			int[] rug = Drylands.rugCell( ow.worldSeed(), p.sx, p.sy );
			int rugCell = ow.localCell( rug[0], rug[1] );
			int sand = ow.localCell( p.cx, p.cy + Drylands.OASIS_REACH + 4 );
			GLog.i( "Scene: an oasis, its water " + where( ow, hero.pos, heart ) + ". The trader keeps his rug "
					+ where( ow, hero.pos, rugCell ) + " (goggles, a scarf, dates, bottles); ripe dates lie under a few palms."
					+ " Fill your two empty bottles at the water's edge." );
			if (sand != -1){
				GLog.i( "Scene: the air in the shade by the water is " + Math.round( TileTemperature.tileTemp( heart ) )
						+ "C, out on the sand to the south " + Math.round( TileTemperature.tileTemp( sand ) ) + "C." );
			}
		}
	}

	private static final class OasisDusk extends ToPlace {
		@Override public String id(){ return "overworld-oasis-dusk"; }
		@Override public String title(){ return "Surface: deer come down to an oasis' water at dusk"; }
		@Override WorldStructures.Site kind(){ return WorldStructures.Site.OASIS; }
		@Override int[] target( long seed, Drylands.Place p ){ return new int[]{ p.cx, p.cy + 24 }; }

		@Override void arrive( Hero hero, OverworldLevel ow, Drylands.Place p ){
			calmDay( DayNightCycle.Phase.DUSK, 0.15f );
			Dungeon.observe();
			GLog.i( "Scene: dusk, the oasis' water " + where( ow, hero.pos, ow.localCell( p.cx, p.cy ) )
					+ ". While nobody is within 12 cells of it, two or three deer come down to drink, once a day,"
					+ " out of sight; walk up and they are on the shore. They slip away into the desert when the dusk ends." );
		}
	}

	private static final class TempleTop extends ToPlace {
		@Override public String id(){ return "overworld-buried-temple"; }
		@Override public String title(){ return "Surface: before a buried temple's stairwell"; }
		@Override WorldStructures.Site kind(){ return WorldStructures.Site.BURIED_TEMPLE; }
		@Override int[] target( long seed, Drylands.Place p ){ return Drylands.approach( seed, p ); }

		@Override void arrive( Hero hero, OverworldLevel ow, Drylands.Place p ){
			calmDay( DayNightCycle.Phase.DAY, 0.3f );
			Dungeon.observe();
			long seed = ow.worldSeed();
			GLog.i( "Scene: " + Delves.templeName( seed, p.sx, p.sy ) + ", level " + Delves.templeLevel( seed, p.sx, p.sy )
					+ ", " + Delves.templeFloors( seed, p.sx, p.sy ) + " floors. Its stairwell is "
					+ where( ow, hero.pos, ow.localCell( p.cx, p.cy ) ) + ": step on it to go down (no amulet asked)." );
		}
	}

	private static final class TempleGuardian implements DebugScenes.Scene {
		//the crypt being travelled to, until the hero stands on its deepest floor
		Delves.Entry going;

		@Override public String id(){ return "overworld-temple-guardian"; }
		@Override public String title(){ return "Surface: a buried temple's deepest floor, its guardian and relic"; }

		@Override public void apply( Hero hero ){
			if (going != null){
				Delves.Entry e = going;
				going = null;
				if (Dungeon.branch != e.branch || Dungeon.depth != e.lastDepth()){
					GLog.w( "Scene: could not reach the temple's deepest floor" );
					return;
				}
				arrive( hero, e );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			DebugScenes.search( "a buried temple", () -> nearest( seed, WorldStructures.Site.BURIED_TEMPLE ), p -> {
				if (p == null){
					GLog.w( "Scene: found no buried temple within 40 sectors of the origin in this world" );
					return;
				}
				going = Delves.openTemple( seed, p[2], p[3] );
				DebugScenes.descendThen( this, going.lastDepth(), going.branch );
			} );
		}

		private void arrive( Hero hero, Delves.Entry e ){
			Mob guardian = null;
			for (Mob m : Dungeon.level.mobs){
				if (m.buff( Delves.Guardian.class ) != null) guardian = m;
			}
			if (guardian == null){
				GLog.w( "Scene: " + Delves.name( e ) + " is cleared already: its guardian is gone" );
				return;
			}
			int spot = -1;
			for (int r = 2; r <= 4 && spot == -1; r++){
				for (int c : DebugScenes.cellsAround( hero, 400 )){
					if (Dungeon.level.distance( c, guardian.pos ) == r && Actor.findChar( c ) == null){
						spot = c;
						break;
					}
				}
			}
			if (spot != -1){
				ScrollOfTeleportation.appear( hero, spot );
				Dungeon.observe();
			}
			GLog.i( "Scene: " + Delves.name( e ) + ", floor " + e.floors + " of " + e.floors + ". The statue beside you is its"
					+ " guardian: it stands still until struck. Its hoard holds the Sun Scarab, gold and gear." );
		}
	}

	private static final class Storm implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "overworld-sandstorm"; }
		@Override public String title(){ return "Surface: a sandstorm in the desert, and shelter from it"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				arrive( hero, (OverworldLevel) Dungeon.level );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "the desert", () -> DebugScenes.biomeGround( seed, shift, WorldModel.Biome.DESERT ), spot -> {
				if (spot == null){
					GLog.w( "Scene: found no desert near the origin in this world" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, spot[0], spot[1] );
			} );
		}

		private void arrive( Hero hero, OverworldLevel ow ){
			if (!DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.4f )){
				GLog.w( "Scene: a real-clock run keeps its own hours" );
			}
			//18 m/s toward the west (270): the sand comes from the east
			new DebugScenes.Sky( null, 0f, 18f, 270f, false ).hold();
			Dungeon.debugNoFog = false;
			Sandstorm.onHeroTurn( hero );
			Dungeon.observe();
			float s = Sandstorm.strengthAt( ow, hero.pos );
			int lee = -1;
			for (int c : DebugScenes.cellsAround( hero, 900 )){
				if (Sandstorm.strengthAt( ow, c ) > 0f && Sandstorm.sheltered( ow, c )){
					lee = c;
					break;
				}
			}
			GLog.i( "Scene: a sandstorm at 18 m/s from the east (strength " + Math.round( 100 * s ) + "%): your sight is cut to "
					+ Math.round( 100 * Sandstorm.sightFactor( s ) ) + "%, your speed to " + Math.round( 100 * Sandstorm.speedFactor( s ) )
					+ "%, and the sand gets in your eyes " + Math.round( 100 * Sandstorm.blindChance( s ) ) + "% of turns (blind for 2)." );
			GLog.i( "Scene: " + (lee == -1 ? "no lee near you: stand west of a rock, a bush, a palm or a wall."
					: "the nearest lee is " + where( ow, hero.pos, lee ) + ", west of something solid: the sand does not reach you there.")
					+ " Carry sand goggles or a desert scarf (the oasis trader sells both) and it does not reach you anywhere." );
		}
	}
}
