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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leashed;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Mounts;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Riding;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.StableWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Stables;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Taming;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.TanningRack;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Saddle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HorseSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The horses (actors/mobs/Horse, homestead/mounts): herds on the grass by day, a wild horse that
 * bolts and takes no rope, food left on the ground and then from the hand taming it, the saddle,
 * getting on and off, the riding rules (speed, a drop, a doorway, a level left, a strike, a blow
 * shared), a village's stable, and every one of it through a save.
 */
public class MountsTest extends FarmTestBase {

	//a mob's own turn, as the scheduler gives it
	private static void act( Actor a ){
		try {
			Method m = Actor.class.getDeclaredMethod( "act" );
			m.setAccessible( true );
			m.invoke( a );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//a tame, saddled horse of the hero's on a cell
	private static Horse tameHorse( OverworldLevel ow, int cell ){
		Horse h = put( ow, new Horse(), cell );
		h.tame = true;
		h.trust = Taming.TAME_TRUST;
		h.saddled = true;
		h.setLook();
		return h;
	}

	//the hero on horseback beside the open ground of a site: {top-left cell}
	private OverworldLevel riding( int[] at, int heroCell, int horseCell ){
		OverworldLevel ow = quiet( at );
		stand( ow, heroCell );
		Horse h = tameHorse( ow, horseCell );
		Mounts.mount( hero, h );
		assertTrue( Mounts.rides( hero ) );
		return ow;
	}

	private static int count( Heap h, Class<?> cls ){
		int n = 0;
		if (h != null) for (Item i : h.items) if (cls.isInstance( i )) n += i.quantity();
		return n;
	}

	// ------------------------------------------------------------------ the herds

	@Test
	public void wildHerdsGrazeTheGrassByDayOnly(){
		Random.pushGenerator( 97 );
		float precip = ClimateManager.debugPrecipOverride;
		ClimateManager.debugPrecipOverride = 0f;
		try {
			for (WorldModel.Biome b : new WorldModel.Biome[]{ WorldModel.Biome.PLAINS, WorldModel.Biome.MEADOW }){
				int herds = 0;
				for (int i = 0; i < 400; i++){
					ArrayList<Mob> day = OverworldFauna.roll( b, DayNightCycle.Phase.DAY );
					if (day.isEmpty() || !(day.get( 0 ) instanceof Horse)) continue;
					herds++;
					assertTrue( b + ": a herd of " + day.size(), day.size() >= 2 && day.size() <= 4 );
					for (Mob m : day){
						assertTrue( m instanceof Horse );
						assertFalse( "wild", ((Horse) m).tame );
					}
				}
				assertTrue( b + " has herds by day: " + herds, herds > 10 );
				for (int i = 0; i < 400; i++){
					for (Mob m : OverworldFauna.roll( b, DayNightCycle.Phase.NIGHT )) assertFalse( b + " by night", m instanceof Horse );
				}
			}
			for (WorldModel.Biome b : new WorldModel.Biome[]{ WorldModel.Biome.FOREST, WorldModel.Biome.DESERT,
					WorldModel.Biome.SNOWFIELD, WorldModel.Biome.SWAMP, WorldModel.Biome.BEACH }){
				for (int i = 0; i < 300; i++){
					for (Mob m : OverworldFauna.roll( b, DayNightCycle.Phase.DAY )) assertFalse( b.toString(), m instanceof Horse );
				}
			}
			//the mountains' and the caves' bands keep no horses
			for (int alt : new int[]{ 2, 5, 9, -2, -6 }){
				assertFalse( OverworldFauna.kindsAt( alt ).contains( Horse.class ) );
			}
		} finally {
			ClimateManager.debugPrecipOverride = precip;
			Random.popGenerator();
		}
	}

	@Test
	public void aWildHorseBoltsAndTakesNoRope(){
		int[] at = site( 12, 5 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		stand( ow, o + 2 + 2 * W );
		give( new Rope().quantity( 2 ) );
		Horse h = put( ow, new Horse(), o + 4 + 2 * W );
		assertTrue( "a tap talks to it", h.heroShouldInteract() );
		assertFalse( h.homesteadKept() );

		assertEquals( "too_wild", Roping.lassoBlock( hero, h.pos ) );
		assertFalse( Roping.leash( hero, h ) );
		assertEquals( 2, Material.count( hero, Rope.class ) );

		int before = ow.distance( hero.pos, h.pos );
		for (int i = 0; i < 3; i++) act( h );
		assertTrue( "it bolted", ow.distance( hero.pos, h.pos ) > before );
		assertEquals( 0, h.trust );
	}

	// ------------------------------------------------------------------ taming

	@Test
	public void foodOnTheGroundAndThenFromTheHandTamesIt(){
		int[] at = site( 14, 5 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		//the hero eight cells off, the food a cell from the horse
		stand( ow, o + 12 + 2 * W );
		Horse h = put( ow, new Horse(), o + 2 + 2 * W );
		int food = o + 3 + 2 * W;
		ow.drop( new Thatch().quantity( 4 ), food );
		assertEquals( food, Taming.foodNear( h ) );

		for (int meal = 1; meal <= 3; meal++){
			act( h );
			assertEquals( "meal " + meal, meal, h.trust );
			assertEquals( Taming.SHY - meal, Taming.shy( h.trust ) );
		}
		//the last meal is from his hand: what is left lying stays there
		assertEquals( "three pieces eaten", 1, count( ow.heaps.get( food ), Thatch.class ) );
		assertEquals( -1, Taming.foodNear( h ) );
		act( h );
		assertEquals( 3, h.trust );
		assertEquals( 1, count( ow.heaps.get( food ), Thatch.class ) );
		assertFalse( h.tame );

		//it shies from nobody now: the hero walks up beside it and it does not bolt
		stand( ow, h.pos + 1 );
		act( h );
		assertTrue( ow.distance( hero.pos, h.pos ) <= 2 );

		//food from his hand: his
		give( new Cornwheatshaft() );
		Taming.handFeed( hero, h );
		assertTrue( h.tame );
		assertEquals( 0, count( hero, Cornwheatshaft.class ) );
		assertTrue( h.homesteadKept() );
		assertNull( h.ropeRefusal() );

		//the tame horse waits where it is, whoever comes
		int stay = h.pos;
		stand( ow, o + 9 + 2 * W );
		for (int i = 0; i < 20; i++) act( h );
		assertEquals( stay, h.pos );

		//and takes a rope now
		give( new Rope() );
		assertTrue( Roping.leash( hero, h ) );
		assertEquals( Leashed.Mode.LEASHED, Roping.rope( h ).mode );
	}

	@Test
	public void aBlowMakesAWildHorseForgetAndOnlyItsFoodIsEaten(){
		assertTrue( Taming.isFeed( new Thatch() ) );
		assertTrue( Taming.isFeed( new Cornwheatshaft() ) );
		assertTrue( Taming.isFeed( new Aprico() ) );
		assertTrue( Taming.isFeed( new Blandfruit() ) );
		assertFalse( Taming.isFeed( new MysteryMeat() ) );
		assertFalse( Taming.isFeed( new Rope() ) );

		int[] at = site( 12, 5 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		stand( ow, o + 11 + 2 * W );
		Horse h = put( ow, new Horse(), o + 2 + 2 * W );
		h.trust = 2;
		h.damage( 1, new Rat() );
		assertEquals( 0, h.trust );

		//meat lying by it is no food; nor are a shop's wares
		ow.drop( new MysteryMeat(), o + 3 + 2 * W );
		ow.drop( new Thatch(), o + 1 + 2 * W ).type = Heap.Type.FOR_SALE;
		assertEquals( -1, Taming.foodNear( h ) );
	}

	// ------------------------------------------------------------------ the saddle and riding

	@Test
	public void aSaddledHorseIsRiddenTwiceAsFastAndLeftBeside(){
		int[] at = site( 9, 5 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		stand( ow, o + 4 + 2 * W );
		Horse h = put( ow, new Horse(), o + 5 + 2 * W );
		h.tame = true;
		h.trust = Taming.TAME_TRUST;
		assertEquals( "no_saddle", Mounts.mountBlock( hero, h ) );

		give( new Saddle() );
		Mounts.saddle( hero, h );
		assertTrue( h.saddled );
		assertSame( HorseSprite.classFor( h.coat, true ), h.spriteClass );
		assertNull( hero.belongings.getItem( Saddle.class ) );
		assertNull( Mounts.mountBlock( hero, h ) );

		assertEquals( 1f, Mounts.speedFactor( hero ), 0f );
		int cell = h.pos;
		Mounts.mount( hero, h );
		assertTrue( Mounts.rides( hero ) );
		assertEquals( cell, hero.pos );
		assertFalse( "off the level, under him", ow.mobs.contains( h ) );
		assertSame( h, hero.buff( Riding.class ).horse() );
		//Hero.speed multiplies by it: twice as fast on horseback
		assertEquals( Mounts.RIDE_SPEED, Mounts.speedFactor( hero ), 0f );

		assertTrue( Mounts.getDown( hero ) );
		assertFalse( Mounts.rides( hero ) );
		assertNull( hero.buff( Riding.class ) );
		assertTrue( ow.mobs.contains( h ) );
		assertTrue( ow.adjacent( hero.pos, h.pos ) );
		assertEquals( 1f, Mounts.speedFactor( hero ), 0f );

		//the saddle comes off into the pack
		Mounts.unsaddle( hero, h );
		assertFalse( h.saddled );
		assertNotNull( hero.belongings.getItem( Saddle.class ) );
	}

	@Test
	public void aHeldTapShowsTheMenuAndAPlainOneRides() throws Exception {
		int[] at = site( 9, 5 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		stand( ow, o + 4 + 2 * W );
		Horse h = tameHorse( ow, o + 5 + 2 * W );

		//the hold is read as the tap is handled and kept on the action: the press is long over by
		//the time the hero acts on it
		CellSelector.heldTap = true;
		try {
			assertTrue( hero.handle( h.pos ) );
		} finally {
			CellSelector.heldTap = false;
		}
		assertTrue( ((HeroAction.Interact) hero.curAction).held );
		assertTrue( hero.handle( h.pos ) );
		assertFalse( ((HeroAction.Interact) hero.curAction).held );

		//held: its menu, and he stays on foot; a plain tap takes him up
		Field held = Hero.class.getDeclaredField( "interactHeld" );
		held.setAccessible( true );
		held.set( hero, true );
		Mounts.tapped( hero, h );
		assertFalse( Mounts.rides( hero ) );
		held.set( hero, false );
		Mounts.tapped( hero, h );
		assertTrue( Mounts.rides( hero ) );
	}

	@Test
	public void aRiderSetDownWithNoRoomRoundHimLeavesHisHorseOnTheNearestOpenGround(){
		int[] at = site( 9, 5 );
		int o = at[2];
		int mid = o + 4 + 2 * W;
		OverworldLevel ow = riding( at, mid - 1, mid );
		assertEquals( mid, hero.pos );
		Horse h = hero.buff( Riding.class ).horse();
		//every cell within four of him walled up: the nearest open ground is five off
		for (int dy = -4; dy <= 4; dy++){
			for (int dx = -4; dx <= 4; dx++){
				if (dx != 0 || dy != 0) ow.map[mid + dx + dy * W] = Terrain.WALL;
			}
		}
		ow.buildFlagMaps();
		Mounts.setDownHere( hero );
		assertFalse( Mounts.rides( hero ) );
		assertTrue( ow.mobs.contains( h ) );
		assertTrue( "never on his own cell", h.pos != hero.pos );
		assertEquals( 5, ow.distance( hero.pos, h.pos ) );
	}

	@Test
	public void noRidingUndergroundOrIndoors(){
		int[] at = site( 9, 5 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		stand( ow, o + 4 + 2 * W );
		Horse h = tameHorse( ow, o + 5 + 2 * W );
		assertTrue( Mounts.ridingSlice( ow ) );
		assertTrue( Mounts.rideable( ow, h.pos ) );
		ow.map[h.pos] = Terrain.DOOR;
		ow.buildFlagMaps();
		assertTrue( Mounts.indoors( ow, h.pos ) );
		assertEquals( "indoors", Mounts.mountBlock( hero, h ) );

		//the slices under the surface are no riding slices; the mountains are
		assertTrue( WorldLayers.isLayerDepth( WorldLayers.depthOf( -1 ) ) );
		set( ow, "altitude", -1 );
		assertFalse( Mounts.ridingSlice( ow ) );
		set( ow, "altitude", 3 );
		assertTrue( Mounts.ridingSlice( ow ) );
		set( ow, "altitude", 0 );
	}

	@Test
	public void aRiderGetsDownAtADoorAndNeverGoesOverADrop(){
		int[] at = site( 9, 5 );
		int o = at[2];
		OverworldLevel ow = riding( at, o + 3 + 2 * W, o + 4 + 2 * W );
		int door = hero.pos + 1, drop = hero.pos - W;
		ow.map[door] = Terrain.DOOR;
		ow.map[drop] = Terrain.CHASM;
		ow.buildFlagMaps();

		assertFalse( "a horse won't go over the edge", Mounts.stepAllowed( hero, drop ) );
		assertTrue( Mounts.stepAllowed( hero, hero.pos + W ) );
		assertFalse( Mounts.getsDownBefore( hero, hero.pos + W ) );
		assertTrue( Mounts.getsDownBefore( hero, door ) );

		//the step through the door, as the hero takes it: on foot, the horse on the cell he left
		int from = hero.pos;
		hero.move( door, false );
		Mounts.stepped( hero, from, true );
		assertFalse( Mounts.rides( hero ) );
		Horse h = null;
		for (Mob m : ow.mobs) if (m instanceof Horse) h = (Horse) m;
		assertNotNull( h );
		assertEquals( from, h.pos );
		//on foot a drop is the hero's own business
		assertTrue( Mounts.stepAllowed( hero, drop ) );
	}

	@Test
	public void goingUnderTheWorldLeavesTheHorseAtTheThreshold(){
		int[] at = site( 9, 5 );
		int o = at[2];
		InterlevelScene.Mode mode = InterlevelScene.mode;
		LevelTransition transition = InterlevelScene.curTransition;
		int returnDepth = InterlevelScene.returnDepth, returnBranch = InterlevelScene.returnBranch;
		try {
			OverworldLevel ow = riding( at, o + 3 + 2 * W, o + 4 + 2 * W );
			int mouth = hero.pos;

			//up the steps of a mountain: it goes on under him
			InterlevelScene.mode = InterlevelScene.Mode.ASCEND;
			InterlevelScene.curTransition = new LevelTransition( ow, mouth, LevelTransition.Type.REGULAR_ENTRANCE,
					WorldLayers.depthOf( 1 ), 0, LevelTransition.Type.REGULAR_EXIT );
			Mob.holdAllies( ow, mouth );
			assertTrue( Mounts.rides( hero ) );

			//a waystone to the surface: on too
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = WorldLayers.SURFACE_DEPTH;
			InterlevelScene.returnBranch = 0;
			Mob.holdAllies( ow, mouth );
			assertTrue( Mounts.rides( hero ) );

			//down a cave mouth: he gets down, the horse waits beside the mouth
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
			InterlevelScene.curTransition = new LevelTransition( ow, mouth, LevelTransition.Type.REGULAR_EXIT,
					WorldLayers.depthOf( -1 ), 0, LevelTransition.Type.REGULAR_ENTRANCE );
			Mob.holdAllies( ow, mouth );
			assertFalse( Mounts.rides( hero ) );
			Horse h = null;
			for (Mob m : ow.mobs) if (m instanceof Horse) h = (Horse) m;
			assertNotNull( "it waits on the level left", h );
			assertTrue( ow.adjacent( mouth, h.pos ) );
		} finally {
			InterlevelScene.mode = mode;
			InterlevelScene.curTransition = transition;
			InterlevelScene.returnDepth = returnDepth;
			InterlevelScene.returnBranch = returnBranch;
		}
	}

	@Test
	public void attackingDismountsAndTheHorseSharesTheBlows(){
		int[] at = site( 9, 5 );
		int o = at[2];
		OverworldLevel ow = riding( at, o + 4 + 2 * W, o + 5 + 2 * W );
		Riding r = hero.buff( Riding.class );
		Horse h = r.horse();
		Rat rat = put( ow, new Rat(), hero.pos + 1 );

		//a third of a blow (rounded) is the horse's; fire, poison and hunger are the rider's alone
		int hp = h.HP;
		assertEquals( 6, Mounts.shareBlow( hero, 9, rat ) );
		assertEquals( hp - 3, h.HP );
		assertEquals( 1, Mounts.shareBlow( hero, 1, rat ) );
		assertEquals( 7, Mounts.shareBlow( hero, 7, new Burning() ) );
		assertEquals( hp - 3, h.HP );

		//a strike from the saddle: he jumps down first, the horse away from the foe
		assertTrue( Mounts.beforeStrike( hero, rat ) );
		assertFalse( Mounts.rides( hero ) );
		assertTrue( ow.mobs.contains( h ) );
		assertTrue( ow.distance( h.pos, rat.pos ) >= 2 );
		//on foot a strike is a strike
		assertTrue( Mounts.beforeStrike( hero, rat ) );
	}

	@Test
	public void aHorseRiddenToDeathFallsAndLeavesItsSaddle(){
		int[] at = site( 9, 5 );
		int o = at[2];
		OverworldLevel ow = riding( at, o + 4 + 2 * W, o + 5 + 2 * W );
		Horse h = hero.buff( Riding.class ).horse();
		Rat rat = put( ow, new Rat(), hero.pos + 1 );
		h.HP = 2;
		Mounts.shareBlow( hero, 30, rat );
		assertFalse( Mounts.rides( hero ) );
		assertFalse( h.isAlive() );
		assertFalse( ow.mobs.contains( h ) );
		int saddles = 0, hides = 0, meat = 0;
		for (Heap heap : ow.heaps.valueList()){
			saddles += count( heap, Saddle.class );
			hides += count( heap, Hide.class );
			meat += count( heap, MysteryMeat.class );
		}
		assertEquals( 1, saddles );
		assertEquals( 1, hides );
		assertEquals( 2, meat );
	}

	// ------------------------------------------------------------------ saves

	@Test
	public void aHorseAndARideComeBackFromASave() throws Exception {
		Horse h = new Horse();
		h.coat = 2;
		h.trust = 3;
		h.saddled = true;
		h.forSale = true;
		h.stable = 1234567L;
		h.stallX = -40;
		h.stallY = 77;
		Bundle b = new Bundle();
		h.storeInBundle( b );
		Horse back = new Horse();
		back.restoreFromBundle( b );
		assertEquals( 2, back.coat );
		assertEquals( 3, back.trust );
		assertTrue( back.saddled && back.forSale && !back.tame );
		assertEquals( 1234567L, back.stable );
		assertEquals( -40, back.stallX );
		assertEquals( 77, back.stallY );
		assertSame( HorseSprite.classFor( 2, true ), back.spriteClass );

		//the ride, saved with the hero
		int[] at = site( 9, 5 );
		int o = at[2];
		riding( at, o + 4 + 2 * W, o + 5 + 2 * W );
		Riding r = hero.buff( Riding.class );
		r.horse().HP = 23;
		int coat = r.horse().coat;
		Bundle rb = new Bundle();
		r.storeInBundle( rb );
		Riding again = new Riding();
		again.restoreFromBundle( rb );
		assertNotNull( again.horse() );
		assertEquals( 23, again.horse().HP );
		assertEquals( coat, again.horse().coat );
		assertTrue( again.horse().tame && again.horse().saddled );

		//a tame horse left on the window comes back with the window's save
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		assertTrue( Mounts.getDown( hero ) );
		OverworldLevel loaded = saveAndLoad( ow );
		Horse kept = null;
		for (Mob m : loaded.mobs) if (m instanceof Horse) kept = (Horse) m;
		assertNotNull( kept );
		assertTrue( kept.tame && kept.saddled && kept.homesteadKept() );
	}

	// ------------------------------------------------------------------ the stable and the saddle's making

	//the human village of five houses or more nearest the world's origin: {well x, well y, sector key}
	private static long[] village(){
		for (int r = 0; r < 8; r++){
			for (int sy = -r; sy <= r; sy++){
				for (int sx = -r; sx <= r; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					long key = WorldStructures.sectorOf( sx, sy );
					VillageRoutine.Settlement s = VillageRoutine.settlement( SEED, key );
					if (s != null && !s.gnoll && VillageRoutine.vendorHouses( s.houses() ) >= 1) return new long[]{ s.cx, s.cy, key };
				}
			}
		}
		throw new AssertionError( "no village near the origin" );
	}

	@Test
	public void aVillageStableStandsInTheSamePlaceAndSellsASaddledHorse(){
		long[] v = village();
		int cx = (int) v[0], cy = (int) v[1];
		long key = v[2];
		//the stable's place: the same world cell from two windows over the village
		OverworldLevel a = windowAt( cx + 20, cy - 10 );
		int sa = Stables.stall( a, cx, cy, key );
		assertTrue( sa != -1 );
		int wxA = a.worldX + sa % W, wyA = a.worldY + sa / W;
		OverworldLevel ow = windowAt( cx, cy );
		int sb = Stables.stall( ow, cx, cy, key );
		assertEquals( wxA, ow.worldX + sb % W );
		assertEquals( wyA, ow.worldY + sb / W );
		assertTrue( Math.max( Math.abs( wxA - cx ), Math.abs( wyA - cy ) ) == Stables.STALL );

		ow.mobs.clear();
		Stables.settle( ow, key );
		OverworldShopkeeper keeper = null;
		for (Mob m : ow.mobs){
			if (m instanceof OverworldShopkeeper && ((OverworldShopkeeper) m).speciality == OverworldShopkeeper.STABLE) keeper = (OverworldShopkeeper) m;
		}
		assertNotNull( keeper );
		assertTrue( HomesteadWorld.section( StableWorld.class ).has( key ) );
		//once only
		Stables.settle( ow, key );
		int keepers = 0;
		for (Mob m : ow.mobs) if (m instanceof OverworldShopkeeper) keepers++;
		assertEquals( 1, keepers );

		//its rail: a saddled horse for sale beside it, a saddle and hay
		actorsOn( ow );
		Stables.lay( keeper );
		Horse h = Stables.horseFor( ow, key );
		assertNotNull( h );
		assertTrue( ow.adjacent( h.pos, keeper.pos ) );
		assertTrue( h.forSale && h.saddled && !h.tame );
		assertTrue( h.homesteadKept() );
		assertEquals( "not_yours", h.ropeRefusal() );
		boolean saddle = false, hay = false;
		for (Heap heap : ow.heaps.valueList()){
			if (heap.type != Heap.Type.FOR_SALE) continue;
			saddle |= heap.peek() instanceof Saddle;
			hay |= heap.peek() instanceof Thatch;
		}
		assertTrue( saddle && hay );

		//bought: the hero's, its price paid; the next restock brings another
		int price = Stables.price( keeper );
		assertEquals( TownLedger.scaled( Stables.HORSE_PRICE ), price );
		Dungeon.gold = price + 5;
		Stables.buy( hero, h, price );
		assertEquals( 5, Dungeon.gold );
		assertTrue( h.tame && !h.forSale );
		assertNull( h.ropeRefusal() );
		Stables.lay( keeper );
		Horse next = Stables.horseFor( ow, key );
		assertNotNull( next );
		assertTrue( next != h );

		//wherever its horse stands (a keeper unparked a few cells off his stall), he lays no second one
		int far = -1;
		for (int c = 0; c < ow.length() && far == -1; c++){
			if (ow.distance( c, keeper.pos ) == 5 && ow.passable[c] && Actor.findChar( c ) == null) far = c;
		}
		assertTrue( far != -1 );
		next.pos = far;
		Stables.lay( keeper );
		int forSale = 0;
		for (Mob m : ow.mobs) if (m instanceof Horse && ((Horse) m).forSale) forSale++;
		assertEquals( 1, forSale );
	}

	@Test
	public void theWindowsOwnPeoplingOfAVillageSetsUpItsStablehand() throws Exception {
		long key = village()[2];
		VillageRoutine.Settlement s = VillageRoutine.settlement( SEED, key );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		Method populate = OverworldLevel.class.getDeclaredMethod( "populateSettlements" );
		populate.setAccessible( true );
		populate.invoke( ow );
		int stablehands = 0;
		for (Mob m : ow.mobs){
			if (m instanceof OverworldShopkeeper && ((OverworldShopkeeper) m).speciality == OverworldShopkeeper.STABLE
					&& ((OverworldShopkeeper) m).homeSector == key) stablehands++;
		}
		assertEquals( 1, stablehands );
		assertTrue( HomesteadWorld.section( StableWorld.class ).has( key ) );
		assertEquals( "stablehand", OverworldShopkeeper.of( OverworldShopkeeper.STABLE, key ).name() );
	}

	@Test
	public void theTanningRackStitchesASaddle(){
		StationRecipe r = StationRecipes.byId( "saddle" );
		assertNotNull( r );
		assertSame( TanningRack.class, r.station );
		assertTrue( r.output.sample() instanceof Saddle );
		int hides = 0, ropes = 0;
		for (StationRecipe.Input in : r.inputs){
			if (in.matches( new Hide(), null )) hides += in.quantity;
			if (in.matches( new Rope(), null )) ropes += in.quantity;
		}
		assertEquals( 2, hides );
		assertEquals( 1, ropes );
		assertTrue( new Saddle().value() > 0 );
	}

	@Test
	public void aDeerIsStillRopedAsBefore(){
		int[] at = site( 9, 5 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		stand( ow, o + 3 + 2 * W );
		give( new Rope() );
		Deer d = put( ow, new Deer(), o + 4 + 2 * W );
		assertNull( d.ropeRefusal() );
		assertTrue( Roping.leash( hero, d ) );
	}

	private int count( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero, Class<? extends Item> cls ){
		int n = 0;
		for (Item i : hero.belongings) if (cls.isInstance( i )) n += i.quantity();
		return n;
	}
}
