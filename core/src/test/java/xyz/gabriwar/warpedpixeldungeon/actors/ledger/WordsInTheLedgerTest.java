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

package xyz.gabriwar.warpedpixeldungeon.actors.ledger;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CreepingDread;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.ui.ChipArt;
import xyz.gabriwar.warpedpixeldungeon.ui.ChipLegend;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * Where the words-to-icons cards meet the Damage tab: a miss the hero made or took comes back to
 * its record with the verb's icon the card showed (named in the tab), and a skill's chip is named
 * with its old shout. The chip codes are one namespace for the card, the ledger and the tab.
 */
public class WordsInTheLedgerTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private LedgerStage stage;
	private EnchantFxTest.Rig rig;

	@Before
	public void setUp() throws Exception {
		stage = new LedgerStage();
		stage.up();
		rig = stage.rig;
	}

	@After
	public void tearDown() throws Exception {
		stage.down();
	}

	private static final List<Integer> VERBS = Arrays.asList( FloatingText.MISS_DODGE, FloatingText.MISS_BLOCK,
			FloatingText.PARRY, FloatingText.GUARDED );

	//the first miss record a fight of up to 200 blows makes, its card drawn
	private HitRecord firstMiss( Runnable blow, boolean dealt ){
		for (int i = 0; i < 200; i++){
			blow.run();
			rig.frames( 40, true );
			List<HitRecord> l = dealt ? Dungeon.hero.damageLog.dealt() : Dungeon.hero.damageLog.taken();
			if (!l.isEmpty() && l.get( 0 ).outcome == HitRecord.MISS && l.get( 0 ).kind == HitRecord.BLOW) return l.get( 0 );
		}
		return null;
	}

	@Test
	public void aMissComesBackToItsRecordWithItsVerbsIcon(){
		Random.pushGenerator( 811 );
		try {
			Hero h = stage.hero( at( 5, 6 ) );
			h.belongings.weapon = new Sword();
			DamageLedgerTest.AlertRat r = stage.body( new DamageLedgerTest.AlertRat(), at( 6, 6 ), 99999 );

			HitRecord dealt = firstMiss( () -> h.attack( r ), true );
			assertNotNull( "the hero missed in 200 blows", dealt );
			assertTrue( "the dealt miss shows a verb: " + dealt.cardIcon, VERBS.contains( dealt.cardIcon ) );
			assertEquals( "and the tab names it", "Dodged", ChipLegend.name( dealt.cardIcon, 0, dealt, null ) );

			h.HP = h.HT = 99999;
			HitRecord taken = firstMiss( () -> r.attack( h ), false );
			assertNotNull( "the rat missed in 200 blows", taken );
			assertTrue( "the taken miss shows a verb: " + taken.cardIcon, VERBS.contains( taken.cardIcon ) );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void aSkillsChipIsNamedWithItsShout(){
		stage.hero( at( 5, 6 ) );
		CreepingDread dread = new CreepingDread();
		assertEquals( "one namespace", HitCard.SKILL_BASE, ChipArt.SKILL_BASE );
		assertEquals( "one namespace", HitCard.SKILL_BASE, DamageLedger.SKILL_BASE );
		assertEquals( dread.name() + ": " + dread.castText(),
				ChipLegend.name( HitCard.SKILL_BASE + dread.image(), 0, null, null ) );
		assertTrue( dread.castText(), ChipLegend.name( HitCard.SKILL_BASE + dread.image(), 0, null, null ).endsWith( "Boo." ) );
	}
}
