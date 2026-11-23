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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingStone;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The starting kit stays plain: a thrown starting stone was given a rarity where it landed
 * (Level.drop rolls one for anything without), so it no longer stacked with the rest of its set
 * nor matched its quickslot, and the warrior ended up with three stones of three sets.
 */
public class StarterKitStaysPlainTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aThrownStartingStoneStillStacks(){
		Dungeon.seed = 1234;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();
		MissileWeapon stones = Dungeon.hero.belongings.getItem( ThrowingStone.class );
		assertTrue( stones.starterKit );

		Item thrown = stones.split( 1 );
		Quality.roll( thrown ); //what landing does to it
		assertNull( thrown.quality );
		assertTrue( stones.isSimilar( thrown ) );
	}

	/**
	 * Plain until it is worked: a masterwork core, the reroll wheel, the black market's wheel, a
	 * type shifter or the tempering anvil offered every class's starting pieces and crashed on the
	 * quality they never got (67 reports from 24.5.6). The first use rolls it, as for any other
	 * item that came without one.
	 */
	@Test
	public void aWorkedStartingPieceGetsItsQuality(){
		for (HeroClass cl : HeroClass.values()){
			Dungeon.seed = 1234;
			GamesInProgress.selectedClass = cl;
			Dungeon.init();
			for (Item kit : Dungeon.hero.belongings){
				if (!MasterworkCore.workable( kit )) continue;
				assertTrue( kit.starterKit );
				assertNotNull( cl + " " + kit.getClass().getSimpleName(), MasterworkCore.quality( kit ) );
				assertFalse( kit.starterKit );
			}
		}
	}
}
