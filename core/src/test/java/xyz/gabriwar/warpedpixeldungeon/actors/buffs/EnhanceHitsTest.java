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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Crossbow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ElfBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * Upgrade Dust raises the equipped weapon and the worn armor for 20 hits each. The weapon's count
 * took a hit off for every blow the hero struck, a thrown weapon or a Spirit Bow arrow too, which
 * the enhancement does not raise. It now counts only the hits of a weapon it raises: the equipped
 * one, or a shot its launcher fires (a bow's arrow, a crossbow's dart). The armor's count takes the
 * blows the hero takes in armor.
 */
public class EnhanceHitsTest {

	private static final int W = 9;
	private static final int HERO = 4 * W + 4;

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;

	private Hero hero;
	private Char foe;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//a level's cells reach the co-op layer, which reads the game's version as it starts
		savedVersion = Game.version;
		Game.version = "test";
		Actor.clear();

		Level floor = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		floor.setSize( W, W );
		floor.mobs = new HashSet<>();
		floor.heaps = new SparseArray<>();
		floor.blobs = new HashMap<>();
		floor.plants = new SparseArray<>();
		floor.traps = new SparseArray<>();
		Arrays.fill( floor.map, Terrain.EMPTY );
		floor.buildFlagMaps();
		Arrays.fill( floor.heroFOV, true );
		PathFinder.setMapSize( W, W );
		Dungeon.level = floor;

		hero = new Hero();
		hero.pos = HERO;
		hero.HP = hero.HT = 100;
		hero.sprite = quietSprite();
		Dungeon.hero = hero;
		Actor.add( hero );
		//the thrower a shot reads, as an action would set it
		java.lang.reflect.Field user = Item.class.getDeclaredField( "curUser" );
		user.setAccessible( true );
		user.set( null, hero );

		foe = new Char(){
			@Override public void damage( int amount, Object source ){ HP -= amount; }
		};
		foe.pos = HERO + 1;
		foe.HP = foe.HT = 100_000;
		foe.alignment = Char.Alignment.ENEMY;
		foe.sprite = quietSprite();
		Actor.add( foe );

		Buff.affect( hero, WeaponEnhance.class ).set( 5, 20 );
		Buff.affect( hero, ArmorEnhance.class ).set( 5, 20 );
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.version = savedVersion;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//a sprite that draws nothing and takes particles without a scene
	private static CharSprite quietSprite(){
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void operate( int cell ){}
		};
		s.visible = false;
		return s;
	}

	private static int hitsLeft( Buff b ){
		Bundle bundle = new Bundle();
		b.storeInBundle( bundle );
		return bundle.getInt( "hit" );
	}

	private int weaponHits(){
		return hitsLeft( hero.buff( WeaponEnhance.class ) );
	}

	private int armorHits(){
		return hitsLeft( hero.buff( ArmorEnhance.class ) );
	}

	//one blow of the hero's that lands, struck with what he holds and what he throws
	private void strike( KindOfWeapon held, KindOfWeapon thrown ){
		hero.belongings.weapon = held;
		hero.belongings.thrownWeapon = thrown;
		hero.attackProc( foe, 10 );
		hero.belongings.thrownWeapon = null;
	}

	@Test
	public void aBlowWithTheEquippedWeaponCounts(){
		Sword sword = new Sword();
		hero.belongings.weapon = sword;
		assertEquals( "the dust raises the sword", sword.level() + 5, sword.buffedLvl() );
		strike( sword, null );
		assertEquals( 19, weaponHits() );
	}

	@Test
	public void aThrownWeaponLeavesTheCountBe(){
		Sword sword = new Sword();
		ThrowingKnife knife = new ThrowingKnife();
		assertEquals( "the dust does not raise a thrown knife", knife.level(), knife.buffedLvl() );
		strike( sword, knife );
		assertEquals( 20, weaponHits() );
	}

	@Test
	public void aSpiritBowArrowLeavesTheCountBe(){
		Sword sword = new Sword();
		SpiritBow bow = new SpiritBow();
		strike( sword, bow.knockArrow() );
		assertEquals( 20, weaponHits() );
	}

	@Test
	public void anUnarmedBlowLeavesTheCountBe(){
		strike( null, null );
		assertEquals( 20, weaponHits() );
	}

	@Test
	public void theEquippedBowsArrowCounts(){
		Bow bow = new Bow();
		hero.belongings.weapon = bow;
		BowWeapon.Arrow arrow = bow.knockArrow();
		assertEquals( "the dust raises the arrows of the bow it raises", bow.level() + 5, arrow.buffedLvl() );
		strike( bow, arrow );
		assertEquals( 19, weaponHits() );
	}

	@Test
	public void theEquippedElfBowsArrowCounts(){
		ElfBow bow = new ElfBow();
		strike( bow, bow.knockArrow() );
		assertEquals( 19, weaponHits() );
	}

	@Test
	public void aDartCountsOnlyFromTheEquippedCrossbow(){
		strike( new Crossbow(), new Dart() );
		assertEquals( 19, weaponHits() );
		strike( new Sword(), new Dart() );
		assertEquals( 19, weaponHits() );
	}

	@Test
	public void theArmorCountsTheBlowsItTakes(){
		hero.belongings.armor = new LeatherArmor();
		strike( new Sword(), null );
		assertEquals( "the hero's own blows leave the armor's count be", 20, armorHits() );
		hero.defenseProc( foe, 10 );
		assertEquals( 19, armorHits() );
	}

	@Test
	public void noArmorLeavesTheArmorsCountBe(){
		hero.belongings.armor = null;
		hero.defenseProc( foe, 10 );
		assertEquals( 20, armorHits() );
	}
}
