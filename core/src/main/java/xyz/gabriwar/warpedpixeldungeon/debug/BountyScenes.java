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
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.bounty.Bounties;
import xyz.gabriwar.warpedpixeldungeon.bounty.Bounty;
import xyz.gabriwar.warpedpixeldungeon.bounty.BountyActions;
import xyz.gabriwar.warpedpixeldungeon.bounty.BountySlice;
import xyz.gabriwar.warpedpixeldungeon.bounty.BountyWorld;
import xyz.gabriwar.warpedpixeldungeon.bounty.NoticeBoard;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The villages' notice boards' debug scenes (the Scenes menu's "Surface" group), both at mid-morning
 * with the rain held off so the folk are out:
 *
 * overworld-bounty-board: the hero beside the notice board of the human village nearest him (one whose
 * parcels have somewhere to go), known there already (two bounties done), with this period's hunt taken
 * and its kills made and its gathering taken with the goods in his pack: the board shows the coin of a
 * reward waiting. Tap it: collect both (the second rank's word comes with the first), and take the
 * delivery down to get its parcel.
 *
 * overworld-bounty-elder: the hero beside an elder of a human village with a parcel for them from a
 * village nearby, due in the usual time. Talk to the village elder to hand it over and be paid; talk to
 * anyone else of the village for the way to the elder's house.
 */
final class BountyScenes {

	private BountyScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new BoardScene(),
			new ElderScene()
	};

	// ------------------------------------------------------------------ helpers

	//the world cell the hero stands on (the origin when he is off the surface)
	private static int[] heroCell( Hero hero ){
		if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			return new int[]{ ow.worldX + hero.pos % ow.width(), ow.worldY + hero.pos / ow.width() };
		}
		return new int[]{ 0, 0 };
	}

	//mid-morning of the day it is, the sky held dry: the folk are out, the elders on their benches
	private static void dayTime(){
		if (!DebugScenes.walkClock( null, DayNightCycle.Phase.DAY, 0.3f )){
			GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
		}
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugForceStorm = false;
	}

	//the hero set down on a free cell beside a mob, else left where he is
	private static void beside( Hero hero, Mob m ){
		for (int n : PathFinder.NEIGHBOURS8){
			int c = m.pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar( c ) == null){
				ScrollOfTeleportation.appear( hero, c );
				Dungeon.observe();
				return;
			}
		}
		GLog.w( "Scene: no free cell beside the " + m.name() );
	}

	private static NoticeBoard boardOf( OverworldLevel ow, long key ){
		for (Mob m : ow.mobs) if (m instanceof NoticeBoard && ((NoticeBoard) m).key == key) return (NoticeBoard) m;
		return null;
	}

	//a gathering's goods, as many as it asks for (seeds of any kind: the grass's own)
	private static void giveGoods( Hero hero, Bounty b ){
		Bounties.Good g = Bounties.good( b.target );
		if (g == null) return;
		for (int left = b.need; left > 0; ){
			Item it = g.cls == Plant.Seed.class ? Generator.random( Generator.Category.SEED ) : Reflection.newInstance( g.cls );
			if (it == null) return;
			//a rotberry seed is a quest's own piece, which no board takes: another
			if (it.unique) continue;
			int n = it.stackable && g.cls != Plant.Seed.class ? left : 1;
			it.quantity( n );
			if (!it.collect( hero.belongings.backpack )) Dungeon.level.drop( it, hero.pos );
			left -= n;
		}
	}

	// ------------------------------------------------------------------ the scenes

	private static final class BoardScene implements DebugScenes.Scene {
		//set while the hero is on his way there, so the arrival does not send him off again
		boolean travelling;
		long key;

		@Override public String id(){ return "overworld-bounty-board"; }
		@Override public String title(){ return "Surface: a notice board with rewards to collect"; }

		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				arrive( hero, seed );
				return;
			}
			int[] at = heroCell( hero );
			int[] found = null;
			for (int[] s : WorldStructures.settlementsNear( seed, at[0], at[1], WorldStructures.Faction.HUMAN, Bounties.SMALL_HOUSES, 8 )){
				if (Bounties.deliverTargets( seed, WorldStructures.sectorOf( s[0], s[1] ) ).isEmpty()) continue;
				found = s;
				break;
			}
			if (found == null){
				GLog.w( "Scene: no human village of " + Bounties.SMALL_HOUSES + " houses or more within eight sectors" );
				return;
			}
			key = WorldStructures.sectorOf( found[0], found[1] );
			dayTime();
			travelling = true;
			DebugScenes.travelThen( this, 0, found[2], found[3] + 3 );
		}

		private void arrive( Hero hero, long seed ){
			if (!(Dungeon.level instanceof OverworldLevel)) return;
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			NoticeBoard board = boardOf( ow, key );
			if (board == null){
				BountySlice slice = ow.homestead().section( BountySlice.class );
				if (slice != null) slice.materialize( ow );
				board = boardOf( ow, key );
			}
			if (board == null){
				GLog.w( "Scene: the board of " + Bounties.villageName( seed, key ) + " could not be put down here" );
				return;
			}
			BountyWorld w = BountyWorld.get();
			//known there already: two done, so the next one finished makes the village trust him
			if (w.points( key ) < 2) w.setPoints( key, 2 );
			ArrayList<Bounty> open = Bounties.openOffers( seed, key, WorldClock.day(), w );
			StringBuilder done = new StringBuilder();
			for (Bounty b : open){
				if (b.kind == Bounty.Kind.DELIVER) continue;
				if (!BountyActions.take( hero, seed, b )) continue;
				if (b.kind == Bounty.Kind.HUNT){
					for (int i = 0; i < b.need; i++) w.count( b );
				} else {
					giveGoods( hero, b );
				}
				done.append( done.length() == 0 ? "" : ", " ).append( Bounties.title( seed, b ) );
			}
			BountyActions.refreshReady();
			beside( hero, board );
			GLog.i( "Scene: the board of " + board.place + ". Ready to collect: " + (done.length() == 0 ? "nothing" : done)
					+ ". Tap the board to collect, and take its delivery down for a parcel." );
		}
	}

	private static final class ElderScene implements DebugScenes.Scene {
		boolean travelling;
		long village, from;

		@Override public String id(){ return "overworld-bounty-elder"; }
		@Override public String title(){ return "Surface: a parcel for a village elder"; }

		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				arrive( hero, seed );
				return;
			}
			int[] at = heroCell( hero );
			int[] found = null;
			for (int[] s : WorldStructures.settlementsNear( seed, at[0], at[1], WorldStructures.Faction.HUMAN, 1, 8 )){
				long k = WorldStructures.sectorOf( s[0], s[1] );
				if (Bounties.elderHouse( seed, k ) == -1 || Bounties.deliverTargets( seed, k ).isEmpty()) continue;
				found = s;
				village = k;
				//a neighbour with a board sends it: one of the villages its own parcels go to
				from = Bounties.deliverTargets( seed, k ).get( 0 );
				break;
			}
			if (found == null){
				GLog.w( "Scene: no human village with an elder within eight sectors" );
				return;
			}
			dayTime();
			travelling = true;
			DebugScenes.travelThen( this, 0, found[2], found[3] + 3 );
		}

		private void arrive( Hero hero, long seed ){
			if (!(Dungeon.level instanceof OverworldLevel)) return;
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			int day = WorldClock.day();
			Bounty b = new Bounty();
			b.period = Bounties.period( seed, from, day );
			//a slot no board shows: closing it hides none of the sender's own notices
			b.slot = 7;
			b.id = Bounties.offerId( from, b.period, b.slot );
			b.village = from;
			b.kind = Bounty.Kind.DELIVER;
			b.dest = village;
			b.dist = Bounties.distance( seed, from, village );
			b.coin = 25 + b.dist / 3;
			if (!BountyActions.take( hero, seed, b )){
				GLog.w( "Scene: the parcel could not be taken (" + BountyActions.refusal( b ) + ")" );
				return;
			}
			Villager elder = null;
			for (Mob m : ow.mobs){
				if (m instanceof Villager && ((Villager) m).settlementKey() == village
						&& ((Villager) m).role == VillageRoutine.Role.ELDER.ordinal()){
					elder = (Villager) m;
					break;
				}
			}
			if (elder == null){
				GLog.w( "Scene: no elder of " + Bounties.villageName( seed, village ) + " in the window: anyone of the village takes it" );
				return;
			}
			beside( hero, elder );
			GLog.i( "Scene: a parcel from " + Bounties.villageName( seed, from ) + " for the elder of "
					+ Bounties.villageName( seed, village ) + " beside you. Talk to the village elder to hand it over." );
		}
	}
}
