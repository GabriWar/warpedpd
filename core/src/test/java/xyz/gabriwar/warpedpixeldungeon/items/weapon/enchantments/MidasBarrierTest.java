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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cocoshield;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RoseBarrier;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * Midas on a foe a barrier holds (a player's "stacked enemies, multiple lives", 2026-11-10): a gnoll
 * shaman under a rose barrier, which turns every blow away whole (Char.damage, its card IMMUNE with
 * the rose beside it), still paid a Midas weapon's killing-blow gold on every swing and drew its
 * fountain of coins, for each blow was at least its health: it seemed to die at each swing and stand
 * there whole, and each swing dropped a heap of gold. A blow a rose barrier or a cocoshield turns
 * away now bleeds no gold; a killing blow on a foe with no barrier still pays.
 */
public class MidasBarrierTest {

	private static final int SWINGS = 20;

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void up(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
		Actor.clear();
		Dungeon.level.mobs = new LinkedHashSet<>();
		PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
		Random.pushGenerator( 1234 );
	}

	@After
	public void down(){
		Random.popGenerator();
		HitCard.reset();
		Actor.clear();
		rig.down();
	}

	/** A foe that has seen the hero (no surprise blow) and never dodges. */
	public static class Foe extends Rat {
		{
			enemySeen = true;
		}

		@Override
		public int defenseSkill( Char enemy ){
			return 0;
		}
	}

	//the hero, sturdy and strong, a rat's sprite standing in for his
	private static Hero hero( int cell ){
		Hero h = new Hero();
		h.HP = h.HT = 9999;
		h.STR = 20;
		//(an interrupt wants a game scene's cell selector)
		h.damageInterrupt = false;
		h.pos = cell;
		CharSprite s = new RatSprite();
		s.ch = h;
		h.sprite = s;
		new Group().add( s );
		s.place( cell );
		Dungeon.hero = h;
		Actor.add( h );
		return h;
	}

	private Foe foe( int cell, int hp ){
		Foe f = new Foe();
		f.HP = f.HT = hp;
		f.pos = cell;
		f.state = f.PASSIVE;
		CharSprite s = new RatSprite();
		s.ch = f;
		f.sprite = s;
		rig.stage.add( s );
		s.place( cell );
		s.visible = true;
		Actor.add( f );
		Dungeon.level.mobs.add( f );
		return f;
	}

	//a heap with a sprite on the cell, as the scene would give the gold's (Midas drops it, `.sprite.drop()`)
	private static Heap heapOn( int cell ){
		Heap heap = new Heap();
		heap.pos = cell;
		ItemSprite s = heap.sprite = new ItemSprite();
		s.link( heap );
		Dungeon.level.heaps.put( cell, heap );
		return heap;
	}

	private static int gold( Heap heap ){
		int n = 0;
		for (Item i : heap.items) if (i instanceof Gold) n += i.quantity();
		return n;
	}

	private static Sword midasSword(){
		Sword sword = new Sword();
		sword.enchant( new Midas() );
		sword.identify();
		return sword;
	}

	@Test
	public void swingsAtAFoeARoseBarrierHoldsBleedNoGold(){
		Hero h = hero( at( 5, 6 ) );
		h.belongings.weapon = midasSword();
		//one health: every blow that lands is a killing blow's size
		Foe f = foe( at( 6, 6 ), 1 );
		Buff.affect( f, RoseBarrier.class, RoseBarrier.DURATION );
		Heap heap = heapOn( f.pos );

		for (int i = 0; i < SWINGS; i++) assertTrue( "the swing lands", h.attack( f ) );

		assertEquals( "the barrier turned every blow away", 1, f.HP );
		assertEquals( "and no blow bled gold", 0, gold( heap ) );
	}

	@Test
	public void aBlowACocoshieldTurnsAwayBleedsNoGold(){
		Foe f = foe( at( 6, 6 ), 1 );
		Buff.affect( f, Cocoshield.class, Cocoshield.DURATION );
		Heap heap = heapOn( f.pos );
		Sword sword = midasSword();

		//killing blows' size and glancing ones (Midas's chance of a few coins)
		for (int i = 0; i < SWINGS; i++){
			sword.enchantment.proc( sword, new Rat(), f, 999 );
			sword.enchantment.proc( sword, new Rat(), f, 0 );
		}

		assertEquals( 0, gold( heap ) );
	}

	@Test
	public void aKillingBlowOnAFoeWithNoBarrierStillPays(){
		Foe f = foe( at( 6, 6 ), 1 );
		Heap heap = heapOn( f.pos );
		Sword sword = midasSword();

		sword.enchantment.proc( sword, new Rat(), f, 1 );

		assertTrue( gold( heap ) > 0 );
	}
}
