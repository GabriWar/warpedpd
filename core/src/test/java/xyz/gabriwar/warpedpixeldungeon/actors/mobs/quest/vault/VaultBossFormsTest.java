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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.quest.vault;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The vault's greater elemental changes form once for each fifth of its health it loses, from a
 * pool of two uses of each form. A Double Stab focus (a CUT mark linked to its own carrier) echoes
 * a hit back onto the boss inside that hit's damage call, and the echo and the hit each changed the
 * form for the same fifth, the hit's clamp then giving back the fifth the echo took: the fight ran
 * through more forms than the pool holds, and the next change drew index -1 (AIOOBE, 3 reports).
 * A boss healed back across its fifths (a healing dart) can outlast the pool too: it still changes.
 */
public class VaultBossFormsTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private Hero savedHero;
	private Boss boss;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		savedHero = Dungeon.hero;
		//damage from a source that is not a character reads the hero's aura
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		if (boss != null) Actor.remove( boss );
		boss = null;
		Dungeon.hero = savedHero;
		rig.down();
	}

	private static final Field FORM;
	static {
		try {
			FORM = VaultBossElemental.class.getDeclaredField( "form" );
			FORM.setAccessible( true );
		} catch (NoSuchFieldException e){
			throw new ExceptionInInitializerError( e );
		}
	}

	//the boss with its form changes counted; the sprite, the bursts and the sounds of a change
	//are the scene's, and a death is only marked
	private static class Boss extends VaultBossElemental {
		int changes;
		@Override
		public void setElementalForm( ElementalForm form ){
			changes++;
			try {
				FORM.set( this, form );
			} catch (IllegalAccessException e){
				throw new AssertionError( e );
			}
		}
		@Override
		public void die( Object cause ){
		}
	}

	//in the actors (the link finds its other end by id), with a sprite that only keeps still
	private Boss boss( int cell ){
		boss = new Boss();
		boss.pos = cell;
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){ }
		};
		s.ch = boss;
		boss.sprite = s;
		Actor.add( boss );
		return boss;
	}

	@Test(timeout = 20000)
	public void aFocusedCutChangesTheFormOncePerFifth(){
		Boss b = boss( at( 6, 6 ) );
		//the focus: a CUT mark on the boss linked to the boss itself
		SkillInteractions.Mark m = new SkillInteractions.Mark();
		m.kind = SkillInteractions.Mark.CUT;
		m.rank = 3;
		m.other = b.id();
		m.left = 1000;
		m.attachTo( b );

		for (int hit = 0; hit < 200 && b.HP > 0; hit++){
			b.damage( 12, new Object() );
		}
		assertTrue( "the fight ended", b.HP <= 0 );
		//one change at each of 480, 360, 240 and 120
		assertEquals( 4, b.changes );
	}

	@Test(timeout = 20000)
	public void aBossHealedAcrossItsFifthsStillChangesForm(){
		Boss b = boss( at( 6, 6 ) );
		//healed back to full three times, down through three fifths each time: nine changes
		for (int round = 0; round < 3; round++){
			b.HP = b.HT;
			while (b.HP > 130){
				VaultBossElemental.ElementalForm before = b.curForm();
				int changes = b.changes;
				b.damage( 25, new Object() );
				if (b.changes > changes) assertNotEquals( before, b.curForm() );
			}
		}
		assertEquals( 9, b.changes );
	}
}
