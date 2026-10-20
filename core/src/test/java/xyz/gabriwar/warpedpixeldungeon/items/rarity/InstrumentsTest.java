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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Harp;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Triangolo;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Trumpet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Wardrum;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.HERO;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.W;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.arena;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.dummy;

/** The five instruments' resonance: every note strikes the characters beside the one struck,
 *  but never the wielder's own side or the neutral, and with only enemies beside it draws the
 *  game's Random exactly as it always did. */
public class InstrumentsTest {

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;

	@BeforeClass
	public static void boot() throws Exception {
		QualitySourceTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedVersion = Game.version;
		Game.version = "test";
		arena();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.version = savedVersion;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	private static List<Supplier<MeleeWeapon>> instruments(){
		return Arrays.asList( Flute::new, Triangolo::new, Wardrum::new, Trumpet::new, Harp::new );
	}

	@Test
	public void resonanceNeverHurtsTheWieldersSide(){
		for (Supplier<MeleeWeapon> make : instruments()){
			Hero hero = arena();
			MeleeWeapon w = make.get();
			String name = w.getClass().getSimpleName();
			int t = HERO + 3;
			Char target = dummy( t, Char.Alignment.ENEMY, 100000 );
			Char friend = dummy( t + W, Char.Alignment.ALLY, 1000 );
			Char bystander = dummy( t - W, Char.Alignment.NEUTRAL, 1000 );
			Char foe = dummy( t + 1, Char.Alignment.ENEMY, 1000 );
			Random.pushGenerator( 0x1A57L );
			try {
				for (int i = 0; i < 6; i++) w.proc( hero, target, 10 );
			} finally {
				Random.popGenerator();
			}
			assertEquals( name + ": an ally beside", 1000, friend.HP );
			assertEquals( name + ": a neutral beside", 1000, bystander.HP );
			assertNull( name, friend.buff( Vertigo.class ) );
			assertNull( name, friend.buff( Terror.class ) );
			assertTrue( name + ": the enemy beside still takes the note", foe.HP < 1000 );
		}
	}

	@Test
	public void anEnemysInstrumentStillStrikesTheHerosSide(){
		for (Supplier<MeleeWeapon> make : instruments()){
			Hero hero = arena();
			MeleeWeapon w = make.get();
			String name = w.getClass().getSimpleName();
			//a rival statue's instrument: the hero is the one struck, and its allies are the other side
			Char rival = dummy( HERO - 2, Char.Alignment.ENEMY, 1000 );
			Char friend = dummy( HERO + W, Char.Alignment.ALLY, 1000 );
			Char itsOwn = dummy( HERO - W, Char.Alignment.ENEMY, 1000 );
			Random.pushGenerator( 0x1A58L );
			try {
				for (int i = 0; i < 6; i++) w.proc( rival, hero, 10 );
			} finally {
				Random.popGenerator();
			}
			assertTrue( name + ": the hero's ally takes the note", friend.HP < 1000 );
			assertEquals( name + ": the rival's own side is spared", 1000, itsOwn.HP );
		}
	}

	@Test
	public void withOnlyEnemiesBesideItDrawsWhatItDidBefore(){
		for (Supplier<MeleeWeapon> make : instruments()){
			for (boolean sparedToo : new boolean[]{ false, true }){
				Hero hero = arena();
				MeleeWeapon w = make.get();
				String name = w.getClass().getSimpleName() + (sparedToo ? " with an ally and a neutral beside" : "");
				int t = HERO + 3;
				Char target = dummy( t, Char.Alignment.ENEMY, 100000 );
				dummy( t + 1, Char.Alignment.ENEMY, 100000 );
				dummy( t + W, Char.Alignment.ENEMY, 100000 );
				if (sparedToo){
					dummy( t - W, Char.Alignment.ALLY, 100000 );
					dummy( t - 1, Char.Alignment.NEUTRAL, 100000 );
				}
				//the draws the resonance always made: one damage roll for each of the two enemies, and the
				//small knock off it every instrument but the harp takes
				long expected;
				Random.pushGenerator( 0x1A59L );
				try {
					for (int i = 0; i < 2; i++){
						Random.NormalIntRange( w.min(), w.max() );
						if (!(w instanceof Harp)) Random.IntRange( 0, 1 );
					}
					expected = Random.Long();
				} finally {
					Random.popGenerator();
				}
				Random.pushGenerator( 0x1A59L );
				try {
					w.proc( hero, target, 10 );
					assertEquals( name, expected, Random.Long() );
				} finally {
					Random.popGenerator();
				}
			}
		}
	}
}
