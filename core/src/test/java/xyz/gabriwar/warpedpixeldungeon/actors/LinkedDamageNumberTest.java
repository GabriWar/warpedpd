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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
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
import static org.junit.Assert.assertFalse;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * A hit's number when the Double Stab focus (a CUT mark linked to its own carrier) finishes the
 * struck one inside the nested damage call (SkillInteractions.shareLinkedDamage): the echo's number
 * and the hit's own both show, as they do when the echo does not kill; a plain kill shows its one.
 */
public class LinkedDamageNumberTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private Hero savedHero;
	private final List<Rat> added = new ArrayList<>();

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
		savedHero = Dungeon.hero;
		//damage from a source that is not a character reads the hero's aura
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		for (Rat r : added) Actor.remove( r );
		added.clear();
		Dungeon.hero = savedHero;
		HitCard.reset();
		rig.down();
	}

	//a rat in the actors (the link finds its other end by id) whose sprite keeps the numbers raised
	//over it; its death is only counted
	private Rat rat( int cell, int hp, List<String> numbers, int[] deaths ){
		Rat r = new Rat(){
			@Override
			public void die( Object src ){
				deaths[0]++;
			}
		};
		r.HP = r.HT = hp;
		r.pos = cell;
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override
			public void showStatusWithIcon( int color, String text, int icon, Object... args ){
				if (text.matches( "\\d+" )) numbers.add( text );
			}
		};
		s.ch = r;
		r.sprite = s;
		Actor.add( r );
		added.add( r );
		return r;
	}

	//the focus: a CUT mark on the rat linked to itself
	private static void focus( Rat r, int rank ){
		SkillInteractions.Mark m = new SkillInteractions.Mark();
		m.kind = SkillInteractions.Mark.CUT;
		m.rank = rank;
		m.other = r.id();
		m.attachTo( r );
	}

	@Test
	public void aFocusEchoThatKillsLeavesTheHitsOwnNumber(){
		List<String> numbers = new ArrayList<>();
		int[] deaths = { 0 };
		Rat r = rat( at( 6, 6 ), 10, numbers, deaths );
		focus( r, 3 );
		int echo = Math.round( 8 * DoubleStab.focusedPercent( 3 ) / 100f );
		r.damage( 8, new Object() );
		assertFalse( "the echo finished it", r.isAlive() );
		assertEquals( "died once", 1, deaths[0] );
		assertEquals( "the echo's number, then the hit's own", Arrays.asList( Integer.toString( echo ), "8" ), numbers );
	}

	@Test
	public void aFocusEchoThatDoesNotKillShowsBothNumbersAsEver(){
		List<String> numbers = new ArrayList<>();
		int[] deaths = { 0 };
		Rat r = rat( at( 6, 6 ), 40, numbers, deaths );
		focus( r, 3 );
		int echo = Math.round( 8 * DoubleStab.focusedPercent( 3 ) / 100f );
		r.damage( 8, new Object() );
		assertEquals( 0, deaths[0] );
		assertEquals( Arrays.asList( Integer.toString( echo ), "8" ), numbers );
	}

	@Test
	public void aPlainKillShowsItsNumberOnce(){
		List<String> numbers = new ArrayList<>();
		int[] deaths = { 0 };
		Rat r = rat( at( 6, 6 ), 5, numbers, deaths );
		r.damage( 9, new Object() );
		assertEquals( 1, deaths[0] );
		assertEquals( Arrays.asList( "9" ), numbers );
	}
}
