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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * A reflected hit (Quality.reflect: Guard's riposte, Warden's thorns) raises its number once, by the
 * damage itself, outside a blow.
 */
public class ReflectNumberTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private Hero savedHero;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
		savedHero = Dungeon.hero;
	}

	@After
	public void tearDown(){
		Dungeon.hero = savedHero;
		HitCard.reset();
		rig.down();
	}

	@Test
	public void aReflectedHitShowsItsNumberOnce(){
		Hero hero = new Hero();
		hero.HP = hero.HT = 20;
		Dungeon.hero = hero;
		List<String> numbers = new ArrayList<>();
		Rat attacker = new Rat();
		attacker.HP = attacker.HT = 50;
		attacker.pos = at( 6, 6 );
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override
			public void showStatusWithIcon( int color, String text, int icon, Object... args ){
				if (text.matches( "\\d+" )) numbers.add( text );
			}
		};
		s.ch = attacker;
		attacker.sprite = s;
		Quality.reflect( attacker, hero, 6 );
		assertEquals( 44, attacker.HP );
		assertEquals( "one number, the damage's", Arrays.asList( "6" ), numbers );
	}
}
