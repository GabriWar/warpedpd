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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.SunScarab;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The buried temples' crypts (Delves, a temple Entry; SandTempleLevel): opened like a barrow but
 * marked a temple, kept so through a save (an older save holds barrows only), 2 to 4 floors built
 * as the sand temple with its stairs between them, its own dead walking them, and on the deepest a
 * guardian statue whose fall pays out the temple's relic.
 */
public class DelvesTempleTest {

	private static final long SEED = 0x7E3B1EL;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
		com.watabou.utils.FileUtils.setDefaultFileProperties( com.badlogic.gdx.Files.FileType.Absolute,
				new java.io.File( "build/test-saves" ).getAbsolutePath() + "/" );
	}

	@Test
	public void aTempleIsOpenedAndSavedAsATemple(){
		Delves.reset();
		Delves.Entry barrow = Delves.open( SEED, 3, 4 );
		Delves.Entry temple = Delves.openTemple( SEED, -5, 2 );
		assertFalse( barrow.temple );
		assertTrue( temple.temple );
		assertEquals( Delves.BRANCH_BASE + 1, temple.branch );
		assertSame( temple, Delves.openTemple( SEED, -5, 2 ) );
		assertEquals( Delves.templeLevel( SEED, -5, 2 ), temple.level );
		assertEquals( Delves.templeFloors( SEED, -5, 2 ), temple.floors );
		assertTrue( temple.floors >= Delves.TEMPLE_MIN_FLOORS && temple.floors <= Delves.TEMPLE_MAX_FLOORS );
		assertTrue( Delves.newLevel( temple.branch, temple.firstDepth() ) instanceof SandTempleLevel );
		assertFalse( Delves.newLevel( barrow.branch, barrow.firstDepth() ) instanceof SandTempleLevel );
		temple.cleared = true;

		Bundle bundle = new Bundle();
		Delves.storeInBundle( bundle );
		Delves.reset();
		Delves.restoreFromBundle( bundle );
		assertTrue( Delves.bySector( -5, 2 ).temple );
		assertTrue( Delves.cleared( -5, 2 ) );
		assertFalse( Delves.bySector( 3, 4 ).temple );

		//a save from before the temples: every entry a barrow, loaded as before
		Bundle old = new Bundle();
		Delves.storeInBundle( old );
		assertTrue( old.remove( "delve_temple" ) );
		Delves.reset();
		Delves.restoreFromBundle( old );
		assertFalse( Delves.bySector( -5, 2 ).temple );
		assertEquals( 2, Delves.entries().size() );
		Delves.reset();
	}

	//its arrival writes the temples' page of the guide, a barrow's the barrows'
	@Test
	public void goingDownWritesTheTemplesPage(){
		Delves.reset();
		Delves.Entry barrow = Delves.open( SEED, 3, 4 );
		Delves.Entry temple = Delves.openTemple( SEED, -5, 2 );
		assertTrue( GuideGraph.arrivalTags( temple.firstDepth(), temple.branch ).contains( GuideGraph.TAG_TEMPLE ) );
		assertFalse( GuideGraph.arrivalTags( temple.firstDepth(), temple.branch ).contains( GuideGraph.TAG_BARROW ) );
		assertTrue( GuideGraph.arrivalTags( barrow.firstDepth(), barrow.branch ).contains( GuideGraph.TAG_BARROW ) );
		Delves.reset();
	}

	//every region's temple floors build: the sand temple, its stairs between its own floors, its
	//dead, and on the deepest no way down and a guardian statue, whose fall gives the relic
	@Test
	public void everyRegionsTempleFloorsBuild(){
		Dungeon.seed = 0x7E3B1EL;
		xyz.gabriwar.warpedpixeldungeon.GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();
		Delves.reset();
		for (int region = 0; region <= 4; region++){
			Delves.Entry e = Delves.openTemple( Dungeon.seed, region, 9 );
			e.region = region;
			e.floors = region == 2 ? Delves.TEMPLE_MAX_FLOORS : Delves.TEMPLE_MIN_FLOORS;
			e.level = 1 + region * 5;
			Dungeon.branch = e.branch;
			int dead = 0;
			for (int f = 0; f < e.floors; f++){
				int depth = e.depthOf( f );
				Dungeon.depth = depth;
				String what = "temple region " + region + " floor " + f + " depth " + depth;
				Level level = Dungeon.newLevel();
				assertTrue( what, level instanceof SandTempleLevel );
				assertNotNull( what + ": no way in", level.getTransition( LevelTransition.Type.REGULAR_ENTRANCE ) );
				boolean down = false;
				for (LevelTransition t : level.transitions){
					if (t.type == LevelTransition.Type.REGULAR_EXIT || t.type == LevelTransition.Type.BRANCH_EXIT){
						down = true;
						assertEquals( what, e.depthOf( f + 1 ), t.destDepth );
						assertEquals( what, e.branch, t.destBranch );
					}
				}
				Mob guardian = null;
				for (Mob m : level.mobs){
					if (m.buff( Delves.Guardian.class ) != null) guardian = m;
					assertTrue( what + ": " + m.getClass().getSimpleName(), m == guardian
							|| SandTempleLevel.rotation( depth ).contains( m.getClass() ) || !m.originalgen );
					if (m != guardian) dead++;
				}
				for (Heap heap : level.heaps.valueList()){
					for (Item item : heap.items) assertFalse( what + ": " + item.getClass().getSimpleName(), Delves.banned( item ) );
				}
				if (f < e.floors - 1){
					assertTrue( what + ": no way down", down );
					continue;
				}
				assertFalse( what + ": the deepest floor has a way down", down );
				assertTrue( what + ": the guardian is the temple's statue", guardian instanceof Statue );
				assertSame( what + ": it stands still until struck", guardian.PASSIVE, guardian.state );
				Dungeon.level = level;
				Delves.onDeath( guardian );
				assertTrue( what, e.cleared );
				Heap hoard = level.heaps.get( guardian.pos );
				assertNotNull( what + ": no hoard", hoard );
				assertEquals( Heap.Type.CHEST, hoard.type );
				boolean relic = false;
				for (Item item : hoard.items){
					relic |= item instanceof SunScarab;
					assertFalse( item.cursed );
				}
				assertTrue( what + ": no relic", relic );
				Dungeon.level = null;
			}
			assertTrue( "nobody walks the temple of region " + region, dead > 0 );
		}
		Dungeon.branch = 0;
		Delves.reset();
	}

	//the relic: harder on the undead the higher it is, nothing without it
	@Test
	public void theRelicIsHarderOnTheUndead(){
		assertEquals( 1f, SunScarab.undeadFactor( -1 ), 0f );
		assertEquals( 1.15f, SunScarab.undeadFactor( 0 ), 1e-6f );
		assertTrue( SunScarab.undeadFactor( 3 ) > SunScarab.undeadFactor( 2 ) );
	}
}
