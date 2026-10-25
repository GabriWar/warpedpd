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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Alligator;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bison;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Camel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Pike;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Vulture;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.FishingSpear;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;

import java.util.function.IntPredicate;

/**
 * The surface's beasts of the open lands, the swamp and the rivers (actors/mobs/Bison, Camel,
 * Vulture, Alligator, Pike; the Scenes menu's "Surface" group). Each walks the clock on to a
 * morning (the bison, the camels and the vultures are out only by day), finds the ground of its
 * beast nearest the world's origin and sets the hero down there, the fog kept off:
 *
 * overworld-beasts-bison: a herd of four grazing six cells off on the plains (a meadow when there
 * are none near). Strike one and the whole herd charges; get out of its sight for 8 turns and it
 * calms down.
 *
 * overworld-beasts-camels: two camels beside the hero in the desert. Strike one: it spits (a hit
 * dazes him 3 turns), then makes off.
 *
 * overworld-beasts-vultures: two vultures over the desert and a carcass (two pieces of mystery
 * meat) six cells off: they come down to it and eat a piece every 3 turns. Walk up to them and
 * they take off.
 *
 * overworld-beasts-vulture-dive: the same, the hero's debug health off and his health down to a
 * quarter: they dive on him, a strike and a climb, until he heals above 40%.
 *
 * overworld-beasts-alligator: an alligator lurking in a swamp pool that has water behind it, the
 * hero two cells from the bank beside it. Step onto the bank: it bursts out, bites and drags him
 * into the water.
 *
 * overworld-beasts-pike: two pike in a river's open water, the hero on its bank with three fishing
 * spears. Wade in, or throw.
 */
final class BeastScenes {

	private BeastScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Herd(),
			new Camels(),
			new Vultures( false ),
			new Vultures( true ),
			new Gator(),
			new River()
	};

	//a beast's scene: walk on to the morning, find its ground off the render thread, travel there,
	//then set the beasts down round the hero
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
					GLog.w( "Scene: only the host can set beasts down" );
					return;
				}
				arrive( hero, ow );
				return;
			}
			if (!DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, 0.2f )){
				GLog.w( "Scene: a real-clock run keeps its own hour (bison, camels and vultures are out by day only)" );
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

	//open dry ground nobody stands on
	private static boolean open( OverworldLevel ow, int c ){
		return ow.insideMap( c ) && ow.passable[c] && !ow.pit[c] && !ow.water[c] && !ow.avoid[c] && Actor.findChar( c ) == null;
	}

	//the surface's open water nobody is in
	private static boolean shallows( OverworldLevel ow, int c ){
		return (OverworldFauna.habitat( ow, c ) & OverworldFauna.Habitat.SHALLOWS.bit) != 0 && Actor.findChar( c ) == null;
	}

	//the cell nearest `from` at least `min` cells off (rings out to r) the test likes, -1 for none
	private static int nearestFrom( OverworldLevel ow, int from, int min, int r, IntPredicate test ){
		return GatheringScenes.nearest( ow, from, r, c -> ow.distance( from, c ) >= min && test.test( c ) );
	}

	private static final class Herd extends Search {
		@Override public String id(){ return "overworld-beasts-bison"; }
		@Override public String title(){ return "Surface: a bison herd grazing on the plains"; }
		@Override String what(){ return "the plains"; }
		@Override int[] find( long seed, float shift ){
			int[] at = DebugScenes.biomeGround( seed, shift, WorldModel.Biome.PLAINS );
			return at != null ? at : DebugScenes.biomeGround( seed, shift, WorldModel.Biome.MEADOW );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			int centre = nearestFrom( ow, hero.pos, 6, 20, c -> open( ow, c ) );
			if (centre == -1){
				GLog.w( "Scene: no open ground for the herd within 20 cells" );
				return;
			}
			DebugScenes.spawn( Bison.class, centre );
			int herd = 1;
			for (int n : PathFinder.NEIGHBOURS8){
				if (herd == 4) break;
				int c = centre + n * 2;
				if (c > 0 && c < ow.length() && open( ow, c ) && ow.distance( c, hero.pos ) >= 4){
					DebugScenes.spawn( Bison.class, c );
					herd++;
				}
			}
			GLog.i( "Scene: a herd of " + herd + " bison grazes " + GatheringScenes.where( ow, hero.pos, centre )
					+ ". Strike one and every bison within 8 cells charges you, half again as fast as you; a blow"
					+ " at the end of a run hits half again as hard and throws you back. Get out of the herd's sight"
					+ " for 8 turns and it calms down." );
		}
	}

	private static final class Camels extends Search {
		@Override public String id(){ return "overworld-beasts-camels"; }
		@Override public String title(){ return "Surface: camels in the desert"; }
		@Override String what(){ return "a desert"; }
		@Override int[] find( long seed, float shift ){
			return DebugScenes.biomeGround( seed, shift, WorldModel.Biome.DESERT );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			int n = 0;
			for (int i = 0; i < 2; i++){
				int c = nearestFrom( ow, hero.pos, 3, 12, cell -> open( ow, cell ) );
				if (c == -1) break;
				DebugScenes.spawn( Camel.class, c );
				n++;
			}
			GLog.i( "Scene: " + n + " camels chew beside you; come within 2 cells and they amble off. Strike one:"
					+ " it spits at you from up to 3 cells (a hit dazes you for 3 turns: half your aim and dodge),"
					+ " then makes off. Rope one and lead it, as a deer." );
		}
	}

	private static final class Vultures extends Search {
		final boolean hurt;
		Vultures( boolean hurt ){
			this.hurt = hurt;
		}
		@Override public String id(){ return hurt ? "overworld-beasts-vulture-dive" : "overworld-beasts-vultures"; }
		@Override public String title(){
			return hurt ? "Surface: vultures dive on a badly hurt hero" : "Surface: vultures come down to a carcass";
		}
		@Override String what(){ return "a desert"; }
		@Override int[] find( long seed, float shift ){
			return DebugScenes.biomeGround( seed, shift, WorldModel.Biome.DESERT );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			int n = 0;
			for (int i = 0; i < 2; i++){
				int c = nearestFrom( ow, hero.pos, 5, 14, cell -> !ow.solid[cell] && ow.insideMap( cell ) && Actor.findChar( cell ) == null );
				if (c == -1) break;
				DebugScenes.spawn( Vulture.class, c );
				n++;
			}
			if (hurt){
				hero.debugInfiniteHealth = false;
				hero.HP = Math.max( 1, hero.HT / 4 );
				GLog.i( "Scene: " + n + " vultures over the desert. Your debug health is off and you are down to a"
						+ " quarter of your health: they dive on you, strike and climb off a cell, and again, until you"
						+ " heal above 40%. While you are above 30% but under 60% they only circle you." );
				return;
			}
			int carcass = nearestFrom( ow, hero.pos, 6, 16, c -> open( ow, c ) && ow.heaps.get( c ) == null );
			if (carcass != -1){
				Heap h = ow.drop( new MysteryMeat().quantity( 2 ), carcass );
				if (h.sprite != null) h.sprite.drop();
			}
			GLog.i( "Scene: " + n + " vultures circle over the desert" + (carcass == -1 ? "."
					: "; a carcass (2 mystery meat) lies " + GatheringScenes.where( ow, hero.pos, carcass )
					+ ": they come down to it and eat a piece every 3 turns. Walk within 2 cells and they take off.") );
		}
	}

	private static final class Gator extends Search {
		@Override public String id(){ return "overworld-beasts-alligator"; }
		@Override public String title(){ return "Surface: an alligator lurking in a swamp pool"; }
		@Override String what(){ return "a swamp"; }
		@Override int[] find( long seed, float shift ){
			return DebugScenes.biomeGround( seed, shift, WorldModel.Biome.SWAMP );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			//a pool with water behind it to drag into, and a bank beside it to step onto
			int[] bank = new int[]{ -1 };
			int pool = GatheringScenes.nearest( ow, hero.pos, 30, c -> {
				if (!shallows( ow, c )) return false;
				for (int n : PathFinder.NEIGHBOURS8){
					int b = c + n;
					if (b > 0 && b < ow.length() && open( ow, b ) && Alligator.backInto( ow, c, b ) != -1){
						bank[0] = b;
						return true;
					}
				}
				return false;
			} );
			if (pool == -1){
				GLog.w( "Scene: no swamp pool with water behind it within 30 cells" );
				return;
			}
			DebugScenes.spawn( Alligator.class, pool );
			int stand = nearestFrom( ow, bank[0], 2, 6, c -> GatheringScenes.standable( ow, c ) && ow.distance( c, pool ) >= 3 );
			if (stand != -1){
				ScrollOfTeleportation.appear( hero, stand );
				Dungeon.observe();
			}
			GLog.i( "Scene: an alligator lurks in the pool " + GatheringScenes.where( ow, hero.pos, pool )
					+ ", only its eyes above the water. Step onto the bank " + GatheringScenes.where( ow, hero.pos, bank[0] )
					+ ": it bursts out, its first bite never misses, and it drags you into the water. Get more than"
					+ " 6 cells away and it goes back to lurk." );
		}
	}

	private static final class River extends Search {
		@Override public String id(){ return "overworld-beasts-pike"; }
		@Override public String title(){ return "Surface: pike in a river"; }
		@Override String what(){ return "a river bank"; }
		@Override int[] find( long seed, float shift ){
			return riverBank( seed, shift );
		}

		@Override void arrive( Hero hero, OverworldLevel ow ){
			IntPredicate river = c -> shallows( ow, c ) && ow.biomeAtCell( c ) == WorldModel.Biome.RIVER;
			int bank = GatheringScenes.nearest( ow, hero.pos, 20, c -> GatheringScenes.standable( ow, c )
					&& GatheringScenes.beside( ow, c, river ) );
			if (bank != -1){
				ScrollOfTeleportation.appear( hero, bank );
				Dungeon.observe();
			}
			int n = 0;
			for (int i = 0; i < 2; i++){
				int c = nearestFrom( ow, hero.pos, 3, 12, river );
				if (c == -1) break;
				DebugScenes.spawn( Pike.class, c );
				n++;
			}
			for (int i = 0; i < 3; i++) GatheringScenes.give( hero, FishingSpear.class );
			GLog.i( "Scene: " + n + " pike in the river beside you, and 3 fishing spears in your pack (one takes at"
					+ " least half a pike's health). Wade into the open water and they come for you, twice as fast"
					+ " as you." );
		}
	}

	/**
	 * Debug and tooling: dry ground beside a river's open water (not frozen), out of every town and
	 * village, nearest the world's origin (rings 48 cells apart out to 2400); null when none. Pure:
	 * it runs off the render thread (DebugScenes.search).
	 */
	static int[] riverBank( long seed, float shift ){
		WorldModel.Sample s = new WorldModel.Sample();
		final int step = 48, rings = 50;
		for (int r = 0; r <= rings; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int cy = j * step;
					//a river crossing the probe's row, with dry ground beside its water
					for (int x = i * step - step / 2; x < i * step + step / 2; x += 2){
						if (WorldModel.sample( seed, x, cy, shift, s ).biome != WorldModel.Biome.RIVER
								|| WorldModel.wildTerrain( seed, x, cy, s ) != Terrain.WATER) continue;
						for (int d = -3; d <= 3; d++){
							int bx = x + d;
							if (WorldModel.sample( seed, bx, cy, shift, s ).biome == WorldModel.Biome.RIVER) continue;
							int t = WorldModel.wildTerrain( seed, bx, cy, s );
							if ((Terrain.flags[t] & Terrain.PASSABLE) == 0 || (Terrain.flags[t] & Terrain.LIQUID) != 0) continue;
							if (WorldStructures.townCell( bx, cy ) != -1 || OverworldFauna.nearSettlement( seed, bx, cy )) continue;
							return new int[]{ bx, cy };
						}
					}
				}
			}
		}
		return null;
	}
}
