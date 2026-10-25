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
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.PearlOyster;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.PixieParasol;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfAccuracy;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.Pearl;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WetlandSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The wetlands' debug scenes (the Scenes menu's "Surface" group; levels/overworld/WetlandSites),
 * each going to the place of its kind nearest the world's origin, found off the render thread:
 *
 * overworld-witch-hut: by day, on the bank at the end of a witch's boardwalk, with three sungrass
 * seeds, two pixie parasols, two raw meats and a hide for her pot, a cursed ring for her to lift
 * and 1500 gold. Tap her to trade, brew or lift the curse.
 *
 * overworld-sunken-shrine: a night (a fresh one: the acolytes rise once a night), six cells south of
 * a sunken shrine, so its drowned acolytes rise at once; 500 gold and a pearl to offer. The fog is
 * back on: what a player sees.
 *
 * overworld-tide-pools: a day's low water at a beach's tide pools, the shells and pearls lying in
 * them and a pearl oyster set in one to prise open. Wait for the tide to come back and see the sea
 * take back what is left.
 *
 * overworld-swamp-mist: dawn in a swamp (at a hut or a shrine, the surest swamp there is near), the
 * mist at its thickest, the fog of war back on so its cost to the sight shows.
 */
final class WetlandScenes {

	private WetlandScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new WitchHut(),
			new SunkenShrine(),
			new TidePools(),
			new SwampMist()
	};

	//how far out to look for a place, in sectors (96 cells each)
	private static final int SECTORS = 40;

	//a scene at the place of a kind nearest the origin: found off the render thread, travelled to,
	//then set up where it stands
	private abstract static class AtPlace implements DebugScenes.Scene {
		boolean travelling;
		WetlandSites.Place found;

		abstract WorldStructures.Site kind();

		//the world cell the hero is set down on
		abstract int[] stand( WetlandSites.Place p );

		abstract void arrive( Hero hero, OverworldLevel ow, WetlandSites.Place p );

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				if (ow.isMirror()){
					GLog.w( "Scene: only the host can set the wetlands' places up" );
					return;
				}
				ow.wetlands().populate();
				arrive( hero, ow, found );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final String what = WetlandSites.mapName( kind() ).toLowerCase( java.util.Locale.ENGLISH );
			DebugScenes.search( "a " + what, () -> WetlandSites.nearest( seed, kind(), 0, 0, SECTORS ), p -> {
				if (p == null){
					GLog.w( "Scene: no " + what + " within " + SECTORS + " sectors of the origin in this world" );
					return;
				}
				found = p;
				travelling = true;
				int[] at = stand( p );
				DebugScenes.travelThen( this, 0, at[0], at[1] );
			} );
		}
	}

	private static final class WitchHut extends AtPlace {
		@Override public String id(){ return "overworld-witch-hut"; }
		@Override public String title(){ return "Surface: a swamp witch's hut, gold and ingredients for her pot"; }
		@Override WorldStructures.Site kind(){ return WorldStructures.Site.WITCH_HUT; }
		@Override int[] stand( WetlandSites.Place p ){ return new int[]{ p.cx, p.cy + WetlandSites.WALK }; }

		@Override void arrive( Hero hero, OverworldLevel ow, WetlandSites.Place p ){
			DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, 0.3f );
			give( hero, new Sungrass.Seed().quantity( 3 ) );
			give( hero, new PixieParasol().quantity( 2 ) );
			give( hero, new MysteryMeat().quantity( 2 ) );
			give( hero, new Hide() );
			RingOfAccuracy ring = new RingOfAccuracy();
			ring.cursed = true;
			ring.cursedKnown = true;
			give( hero, ring );
			Dungeon.gold += 1500;
			GLog.i( "Scene: the witch's hut at " + p.cx + ", " + p.cy + ", by day."
					+ " In your pack: three sungrass seeds, two pixie parasols, two raw meats and a hide for her pot, a cursed"
					+ " ring for her to lift, and 1500 gold. Walk up the boardwalk and tap her: trade, brew, lift a curse." );
		}
	}

	private static final class SunkenShrine extends AtPlace {
		@Override public String id(){ return "overworld-sunken-shrine"; }
		@Override public String title(){ return "Surface: a sunken shrine by night, its acolytes rising"; }
		@Override WorldStructures.Site kind(){ return WorldStructures.Site.SUNKEN_SHRINE; }
		@Override int[] stand( WetlandSites.Place p ){ return new int[]{ p.cx, p.cy + 6 }; }

		@Override void arrive( Hero hero, OverworldLevel ow, WetlandSites.Place p ){
			//a fresh night: its acolytes rise once a night
			int night = WorldClock.night();
			DebugScenes.walkClock( null, DayNightCycle.Phase.NIGHT, 0.2f );
			if (WorldClock.night() == night && !Dungeon.isChallenged( xyz.gabriwar.warpedpixeldungeon.Challenges.REAL_CLOCK )){
				Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
			}
			give( hero, new Pearl() );
			Dungeon.gold += 500;
			Dungeon.debugNoFog = false;
			GLog.i( "Scene: a sunken shrine at " + p.cx + ", " + p.cy + ", at night, the fog back on. Its drowned acolytes"
					+ " rise out of the water round it as you stand within " + xyz.gabriwar.warpedpixeldungeon.levels.overworld.Wetlands.GUARD_NEAR
					+ " cells. 500 gold and a pearl to offer: wade up to the idol and tap it." );
		}
	}

	private static final class TidePools extends AtPlace {
		@Override public String id(){ return "overworld-tide-pools"; }
		@Override public String title(){ return "Surface: a beach's tide pools at low water, an oyster in one"; }
		@Override WorldStructures.Site kind(){ return WorldStructures.Site.TIDE_POOLS; }
		@Override int[] stand( WetlandSites.Place p ){ return new int[]{ p.cx, p.cy }; }

		@Override void arrive( Hero hero, OverworldLevel ow, WetlandSites.Place p ){
			DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, 0.1f );
			//on to the day's low water
			if (!Dungeon.isChallenged( xyz.gabriwar.warpedpixeldungeon.Challenges.REAL_CLOCK )){
				for (int i = 0; i < DayNightCycle.FULL_CYCLE / 10
						&& !(WetlandSites.tideOut( WorldClock.turn() ) && DayNightCycle.phase() == DayNightCycle.Phase.DAY); i++){
					Dungeon.cycleTurn += 10;
				}
			}
			//a pearl oyster in a pool that has none lying in it
			int[] pools = p.pools();
			boolean oyster = false;
			for (Mob m : ow.mobs) oyster |= m instanceof PearlOyster && ((PearlOyster) m).poolsKey == p.key();
			for (int i = 0; i < pools.length && !oyster; i += 2){
				int c = ow.localCell( pools[i], pools[i + 1] );
				if (c == -1 || ow.map[c] != Terrain.WATER || Actor.findChar( c ) != null) continue;
				oyster = ow.addMob( PearlOyster.of( p.key() ), c );
			}
			Dungeon.observe();
			GLog.i( "Scene: tide pools at " + p.cx + ", " + p.cy + ". The tide is " + (WetlandSites.tideOut( WorldClock.turn() ) ? "out" : "in")
					+ " and turns in " + WetlandSites.turnsToTurn( WorldClock.turn() ) + " turns. Step on a shell or a pearl in a pool"
					+ " to take it; tap the oyster from beside it. What is left goes back to the sea when the tide comes in." );
		}
	}

	private static final class SwampMist implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "overworld-swamp-mist"; }
		@Override public String title(){ return "Surface: a swamp at dawn, its mist at its thickest"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				DebugScenes.walkClock( null, DayNightCycle.Phase.DAWN, 0.3f );
				Dungeon.debugNoFog = false;
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				Dungeon.observe();
				GLog.i( "Scene: a swamp at dawn, the fog back on. The mist is at its thickest: "
						+ WetlandSites.mistSight( ow.mistDensity() ) + " cells off your sight here, counted as fog. By day it"
						+ " thins to a haze (1 cell), at dusk it thickens again. Step into its water and feel it drag (a quarter slower)." );
				return;
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "a swamp", () -> {
				WetlandSites.Place hut = WetlandSites.nearest( seed, WorldStructures.Site.WITCH_HUT, 0, 0, SECTORS );
				WetlandSites.Place shrine = WetlandSites.nearest( seed, WorldStructures.Site.SUNKEN_SHRINE, 0, 0, SECTORS );
				WetlandSites.Place p = hut == null ? shrine : shrine == null ? hut
						: (long) hut.cx * hut.cx + (long) hut.cy * hut.cy <= (long) shrine.cx * shrine.cx + (long) shrine.cy * shrine.cy ? hut : shrine;
				if (p != null) return new int[]{ p.cx + 4, p.cy + 4 };
				return DebugScenes.biomeGround( seed, shift, WorldModel.Biome.SWAMP );
			}, spot -> {
				if (spot == null){
					GLog.w( "Scene: found no swamp near the origin in this world" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, spot[0], spot[1] );
			} );
		}
	}

	//an item in the pack, at the hero's feet when the pack is full
	private static void give( Hero hero, Item item ){
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}
}
