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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.AnimalSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Animals;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leashed;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.TiePost;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The world's peaceful beasts on the hero's rope (homestead/animals): leading one (it follows,
 * it is pulled when stuck, the rope slips past six cells, two at most), tying it to a post, a pen
 * keeping it for good through the window moving and the hour's cull, butchering, and a roped
 * animal going along to another slice.
 */
public class HomesteadAnimalsTest extends FarmTestBase {

	private static Leashed.Mode mode( Mob m ){
		Leashed t = Roping.rope( m );
		return t == null ? Leashed.Mode.FREE : t.mode;
	}

	//the window with a deer beside the hero, who carries `ropes` ropes
	private OverworldLevel field( int[] at, int heroCell, int deerCell, int ropes ){
		OverworldLevel ow = quiet( at );
		stand( ow, heroCell );
		if (ropes > 0) give( new Rope().quantity( ropes ) );
		put( ow, new Deer(), deerCell );
		return ow;
	}

	private static Deer deer( OverworldLevel ow ){
		for (Mob m : ow.mobs) if (m instanceof Deer) return (Deer) m;
		return null;
	}

	@Test
	public void aRopedAnimalFollowsTheHero(){
		int[] at = site( 12, 3 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 2 + W, o + 1 + W, 1 );
		Deer d = deer( ow );
		assertFalse( "nobody leads a monster", Roping.leash( hero, put( ow, new Rat(), o + 1 + 2 * W ) ) );
		assertTrue( Roping.leash( hero, d ) );
		assertEquals( Leashed.Mode.LEASHED, mode( d ) );
		assertEquals( "the rope went from the pack onto it", 0, Material.count( hero, Rope.class ) );
		assertTrue( d.homesteadKept() );
		assertTrue( "a tap talks to it now", d.heroShouldInteract() );

		//the hero walks off east: it walks after him and never bolts
		for (int step = 0; step < 6; step++){
			hero.pos += 1;
			for (int i = 0; i < 2; i++) Animals.act( d );
		}
		assertTrue( ow.distance( d.pos, hero.pos ) <= 2 );

		//let go: the rope comes back, it is wild again
		Roping.letGo( hero, d );
		assertEquals( 1, Material.count( hero, Rope.class ) );
		assertEquals( Leashed.Mode.FREE, mode( d ) );
		assertFalse( d.homesteadKept() );
		assertFalse( d.heroShouldInteract() );
	}

	@Test
	public void aRopedAnimalThatCannotFollowIsPulled(){
		int[] at = site( 12, 7 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 3 + 3 * W, o + 2 + 3 * W, 1 );
		Deer d = deer( ow );
		assertTrue( Roping.leash( hero, d ) );
		Animals.act( d );
		//the hero steps off east and a fence is shut round the deer: no way after him
		hero.pos = o + 6 + 3 * W;
		ring( ow, o + 1 + 2 * W, 3, 3, "fence" );
		Animals.act( d );
		assertTrue( "pulled to the hero's side", ow.adjacent( d.pos, hero.pos ) );

		//shut in again, the hero a step further on: pulled to the cell he just left
		d.pos = o + 2 + 3 * W;
		hero.pos = o + 7 + 3 * W;
		Animals.act( d );
		assertEquals( o + 6 + 3 * W, d.pos );
		assertEquals( Leashed.Mode.LEASHED, mode( d ) );
	}

	@Test
	public void theRopeSlipsPastSixCells(){
		int[] at = site( 12, 3 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 1 + W, o + 2 + W, 1 );
		Deer d = deer( ow );
		assertTrue( Roping.leash( hero, d ) );
		hero.pos = o + 10 + W;
		Animals.act( d );
		assertEquals( Leashed.Mode.FREE, mode( d ) );
		assertEquals( "back in the pack", 1, Material.count( hero, Rope.class ) );
	}

	@Test
	public void twoAnimalsOnARopeAtMost(){
		int[] at = site( 5, 5 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 2 + 2 * W, o + 1 + 2 * W, 3 );
		Goat g = put( ow, new Goat(), o + 3 + 2 * W );
		Bunny b = put( ow, new Bunny(), o + 2 + W );
		assertTrue( Roping.leash( hero, deer( ow ) ) );
		assertTrue( Roping.leash( hero, g ) );
		assertFalse( Roping.leash( hero, b ) );
		assertEquals( 1, Material.count( hero, Rope.class ) );
		assertEquals( 2, Roping.leashed( ow ).size() );
	}

	@Test
	public void aTiedAnimalKeepsByItsPost(){
		int[] at = site( 9, 9 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 4 + 4 * W, o + 5 + 4 * W, 1 );
		Deer d = deer( ow );
		int post = o + 4 + 3 * W;
		build( ow, post, "tie_post" );
		TiePost tp = null;
		for (Mob m : ow.mobs) if (m instanceof TiePost) tp = (TiePost) m;
		assertNotNull( tp );
		actorsOn( ow );
		assertTrue( Roping.tiePoint( ow, post ) );
		assertTrue( Roping.leash( hero, d ) );
		Roping.tie( hero, d, post );
		assertEquals( Leashed.Mode.TIED, mode( d ) );
		assertEquals( 1, Roping.tiedTo( ow, post ) );

		//the hero walks away: it stays; dragged off, it comes back within a step of the post
		hero.pos = o + 8 + 8 * W;
		d.pos = o + 7 + 6 * W;
		for (int i = 0; i < 6; i++) Animals.act( d );
		assertTrue( ow.distance( d.pos, post ) <= 1 );
		for (int i = 0; i < 60; i++){
			Animals.act( d );
			assertTrue( ow.distance( d.pos, post ) <= 1 );
		}
		assertEquals( Leashed.Mode.TIED, mode( d ) );

		//the post taken down: it is loose, the rope at its feet
		stand( ow, o + 4 + 5 * W );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, post ) );
		Animals.act( d );
		assertEquals( Leashed.Mode.FREE, mode( d ) );
		Heap h = ow.heaps.get( d.pos );
		assertNotNull( h );
		assertTrue( h.peek() instanceof Rope );
	}

	@Test
	public void aPennedAnimalIsKeptThroughTheCullAndTheWindowMoving(){
		int[] at = site( 9, 9 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		ring( ow, o, 7, 7, "fence" );
		build( ow, o + 3 + 6 * W, "gate" );
		Deer penned = put( ow, new Deer(), o + 3 + 3 * W );
		Deer wild = put( ow, new Deer(), o + 8 + 8 * W );
		Roping.lookAtPen( ow, penned, true );
		Roping.lookAtPen( ow, wild, true );
		assertTrue( penned.homesteadKept() );
		assertFalse( wild.homesteadKept() );

		//it stays in its pen, gate held open or not
		ow.map[o + 3 + 6 * W] = xyz.gabriwar.warpedpixeldungeon.levels.Terrain.FENCE_GATE_OPEN;
		ow.buildFlagMaps();
		HashSet<Integer> pen = new HashSet<>();
		for (int y = 1; y < 6; y++) for (int x = 1; x < 6; x++) pen.add( o + x + y * W );
		//the hero far off, on the side of the window with room
		int away = (o % W) < W / 2 ? 50 : -50;
		stand( ow, o + 3 + away + 3 * W );
		for (int i = 0; i < 200; i++){
			Animals.act( penned );
			assertTrue( pen.contains( penned.pos ) );
		}
		assertTrue( penned.homesteadKept() );

		//the night's cull takes the wild deer out of sight, not the penned one
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		Arrays.fill( ow.heroFOV, false );
		OverworldFauna.cull( ow );
		assertTrue( ow.mobs.contains( penned ) );
		assertFalse( ow.mobs.contains( wild ) );

		//the window goes far away and comes back: the penned deer is put back in its pen
		int wx = ow.worldX + penned.pos % W, wy = ow.worldY + penned.pos / W;
		assertNotNull( "the slice keeps an animals' section", ow.homestead().section( AnimalSlice.class ) );
		jump( ow, wx + 900, wy );
		assertTrue( "parked, still penned", penned.homesteadKept() );
		jump( ow, wx + 40, wy - 30 );
		int back = ow.localCell( wx, wy );
		Deer again = null;
		for (Mob m : ow.mobs) if (m == penned) again = (Deer) m;
		assertNotNull( "kept through the window's moves", again );
		assertTrue( ow.distance( again.pos, back ) <= 2 );
	}

	@Test
	public void butcheringLeavesMeatAndAHideAndNoExperience(){
		int[] at = site( 5, 5 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 2 + 2 * W, o + 3 + 2 * W, 1 );
		Deer d = deer( ow );
		int post = o + 3 + W;
		build( ow, post, "fence" );
		assertTrue( Roping.leash( hero, d ) );
		Roping.tie( hero, d, post );
		int exp = hero.exp, cell = d.pos;
		Roping.butcher( hero, d );
		assertFalse( ow.mobs.contains( d ) );
		assertEquals( exp, hero.exp );
		assertEquals( "the rope comes back", 1, Material.count( hero, Rope.class ) );
		Heap h = ow.heaps.get( cell );
		assertNotNull( h );
		int meat = 0, hides = 0;
		for (Item i : h.items){
			if (i instanceof MysteryMeat) meat += i.quantity();
			if (i instanceof Hide) hides += i.quantity();
		}
		assertEquals( 2, meat );
		assertEquals( 1, hides );

		//a free, unpenned beast is not the hero's to butcher
		Bunny b = put( ow, new Bunny(), o + 1 + W );
		Roping.butcher( hero, b );
		assertTrue( ow.mobs.contains( b ) );
	}

	@Test
	public void aRopedAnimalBesideTheHeroGoesAlongToAnotherSlice(){
		int[] at = site( 9, 3 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 1 + W, o + 2 + W, 2 );
		Deer d = deer( ow );
		Bunny far = put( ow, new Bunny(), o + 1 + 2 * W );
		assertTrue( Roping.leash( hero, d ) );
		assertTrue( Roping.leash( hero, far ) );
		far.pos = o + 6 + W;

		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = WorldLayers.depthOf( -1 );
		InterlevelScene.returnBranch = 0;
		assertTrue( Roping.goesAlong( d, hero.pos ) );
		assertFalse( "too far behind: its rope slips", Roping.goesAlong( far, hero.pos ) );
		assertEquals( Leashed.Mode.FREE, mode( far ) );

		//into a dungeon floor: it stays, the rope slips
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		InterlevelScene.curTransition = new LevelTransition( ow, hero.pos, LevelTransition.Type.REGULAR_EXIT, 1, 0,
				LevelTransition.Type.REGULAR_ENTRANCE );
		try {
			assertFalse( Roping.goesAlong( d, hero.pos ) );
			assertEquals( Leashed.Mode.FREE, mode( d ) );
		} finally {
			InterlevelScene.curTransition = null;
		}
		assertEquals( 2, Material.count( hero, Rope.class ) );
	}

	@Test
	public void aRopeThrownOverAGoatHoldsItBeforeItBolts(){
		int[] at = site( 12, 3 );
		int o = at[2];
		OverworldLevel ow = quiet( at );
		stand( ow, o + W );
		give( new Rope().quantity( 2 ) );
		//past a goat's shyness: it grazes on, and the rope reaches it
		Goat g = put( ow, new Goat(), o + 5 + W );
		assertTrue( Roping.lasso( hero, g.pos ) );
		assertEquals( Leashed.Mode.LEASHED, mode( g ) );
		assertEquals( 1, Material.count( hero, Rope.class ) );
		//the hero walks on towards it and past: it never bolts, it comes after him
		for (int step = 0; step < 8; step++){
			hero.pos += 1;
			for (int i = 0; i < 2; i++) Animals.act( g );
			assertEquals( Leashed.Mode.LEASHED, mode( g ) );
		}
		assertTrue( ow.distance( g.pos, hero.pos ) <= 2 );

		//out of the rope's reach, or with someone in its way, it does not take
		Bunny far = put( ow, new Bunny(), hero.pos - Roping.LASSO - 1 );
		assertFalse( Roping.lasso( hero, far.pos ) );
		Bunny behind = put( ow, new Bunny(), hero.pos - 4 );
		put( ow, new Bunny(), hero.pos - 2 );
		assertFalse( "another beast stands in the rope's way", Roping.lasso( hero, behind.pos ) );
		assertEquals( Leashed.Mode.FREE, mode( behind ) );
		assertEquals( 1, Material.count( hero, Rope.class ) );
	}

	@Test
	public void aTiedAnimalStaysTiedWhileTheWindowDoesNotHoldItsPost(){
		int[] at = site( 5, 5 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 2 + 2 * W, o + 3 + 2 * W, 1 );
		Deer d = deer( ow );
		int post = o + 3 + W;
		build( ow, post, "fence" );
		assertTrue( Roping.leash( hero, d ) );
		Roping.tie( hero, d, post );
		Leashed t = Roping.rope( d );
		//the post's world cell on the window's rim (after a move of the window), then past it
		int[][] posts = { { ow.worldX, ow.worldY + post / W }, { ow.worldX - 50, ow.worldY + 10 } };
		for (int[] p : posts){
			t.postX = p[0];
			t.postY = p[1];
			assertEquals( -1, ow.localCell( p[0], p[1] ) );
			for (int i = 0; i < 5; i++) Animals.act( d );
			assertEquals( Leashed.Mode.TIED, mode( d ) );
			assertNull( "no rope dropped", ow.heaps.get( d.pos ) );
		}
	}

	@Test
	public void aGuestHeroIsNotTheOneToTieUntieOrButcher(){
		int[] at = site( 5, 5 );
		int o = at[2];
		OverworldLevel ow = field( at, o + 2 + 2 * W, o + 3 + 2 * W, 1 );
		Deer d = deer( ow );
		int post = o + 3 + W;
		build( ow, post, "fence" );
		assertTrue( Roping.leash( hero, d ) );
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero guest = new xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero();
		guest.pos = o + 4 + 2 * W;
		Roping.tie( guest, d, post );
		assertEquals( "still the host's to lead", Leashed.Mode.LEASHED, mode( d ) );
		Roping.letGo( guest, d );
		assertEquals( Leashed.Mode.LEASHED, mode( d ) );
		Roping.tie( hero, d, post );
		Roping.butcher( guest, d );
		assertTrue( ow.mobs.contains( d ) );
		assertEquals( Leashed.Mode.TIED, mode( d ) );
	}
}
