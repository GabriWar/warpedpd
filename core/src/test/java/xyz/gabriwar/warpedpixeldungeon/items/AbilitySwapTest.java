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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Endure;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A hero who chose an armor ability through the armor kit's class armor and then wore the Dwarf
 * King's crown kept both abilities' talents in the fourth tier, the old ones unusable with the
 * points spent on them, and lost those points from the tree on loading. The crown's ability now
 * takes the old one's place and its points come back.
 */
public class AbilitySwapTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void theNewAbilityReplacesTheOldAndRefundsIt(){
		Dungeon.seed = 99;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();
		Hero h = Dungeon.hero;

		KingsCrown.takeAbility( h, new HeroicLeap() );
		Talent oldTalent = h.talents.get( 3 ).keySet().iterator().next();
		h.talents.get( 3 ).put( oldTalent, 2 );
		int before = h.heroSkills.availableSkill;

		KingsCrown.takeAbility( h, new Endure() );
		assertFalse( "the old ability's talent is gone", h.talents.get( 3 ).containsKey( oldTalent ) );
		for (Talent t : new Endure().talents()) assertTrue( h.talents.get( 3 ).containsKey( t ) );
		assertEquals( "its two points came back", before + 2, h.heroSkills.availableSkill );
	}
}
