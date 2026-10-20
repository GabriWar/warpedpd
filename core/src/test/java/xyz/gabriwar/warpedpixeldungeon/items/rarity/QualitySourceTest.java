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

import com.watabou.noosa.particles.Emitter;
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ElfBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MegaCannon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.TaurcenBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.LG.LG;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** A launcher's shot counts as its launcher: its quality, its rarity damage and accuracy,
 *  its armor pierce and its enchantments, both of a bow's slots included. */
public class QualitySourceTest {

	static final int W = 19;
	//the hero stands here; dummies go along the row
	static final int HERO = 9 * W + 3;

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//a level's cells reach the co-op layer, which reads the game's version as it starts
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

	// ---------------------------------------------------------------- fixture, shared with the other two tests

	/** an open 19x19 floor and a hero on it, as the game's Dungeon.level and Dungeon.hero */
	static Hero arena(){
		Actor.clear();
		Level floor = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		floor.setSize( W, W );
		floor.traps = new com.watabou.utils.SparseArray<>();
		floor.plants = new com.watabou.utils.SparseArray<>();
		floor.heaps = new com.watabou.utils.SparseArray<>();
		floor.blobs = new java.util.HashMap<>();
		floor.mobs = new HashSet<>();
		java.util.Arrays.fill( floor.solid, true );
		for (int y = 1; y < W - 1; y++) for (int x = 1; x < W - 1; x++) Level.set( y * W + x, Terrain.EMPTY, floor );
		java.util.Arrays.fill( floor.heroFOV, true );
		Dungeon.level = floor;
		Hero hero = new Hero();
		hero.pos = HERO;
		hero.HP = hero.HT = 100;
		hero.sprite = quietSprite();
		Dungeon.hero = hero;
		//the thrower a shot reads, as an action would set it
		try {
			java.lang.reflect.Field user = Item.class.getDeclaredField( "curUser" );
			user.setAccessible( true );
			user.set( null, hero );
		} catch (ReflectiveOperationException e){
			throw new IllegalStateException( e );
		}
		return hero;
	}

	/** a sprite that draws nothing and takes particles without a scene */
	static CharSprite quietSprite(){
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void operate( int cell ){}
		};
		s.visible = false;
		return s;
	}

	/** a character that only keeps count of its health, at a cell of the arena */
	static Char dummy( int pos, Char.Alignment alignment, int hp ){
		Char ch = new Char(){
			@Override public void damage( int amount, Object source ){ HP -= amount; }
		};
		ch.pos = pos;
		ch.HP = ch.HT = hp;
		ch.alignment = alignment;
		ch.sprite = quietSprite();
		Actor.add( ch );
		return ch;
	}

	/** a weapon's quality with exactly these lines, every one at this roll */
	static Quality only( Item item, Rarity rarity, float roll, RarityLine... lines ){
		Quality.roll( item, rarity, ItemType.GAMMA, 0 );
		Quality q = item.quality;
		q.lines.clear();
		q.rolls.clear();
		for (RarityLine l : lines){
			q.lines.add( l );
			q.rolls.add( roll );
		}
		if (item instanceof Weapon) ((Weapon) item).enchant( null );
		return q;
	}

	// ---------------------------------------------------------------- tests

	@Test
	public void shotsResolveToTheirLauncher(){
		Hero hero = Dungeon.hero;
		FT ft = new FT();
		Gun.Bullet bullet = ft.knockBullet();
		assertSame( ft, bullet.magicSource() );
		assertSame( ft, Quality.source( bullet, hero ) );
		LG lg = new LG();
		assertSame( lg, Quality.source( lg.knockBullet(), hero ) );
		Bow bow = new Bow();
		BowWeapon.Arrow arrow = bow.knockArrow();
		assertSame( bow, Quality.source( arrow, hero ) );
		MiniGun mini = new MiniGun();
		assertSame( mini, Quality.source( mini.knockShot(), hero ) );
		ShootGun shotgun = new ShootGun();
		assertSame( shotgun, Quality.source( shotgun.knockShot(), hero ) );
		assertSame( shotgun, Quality.source( shotgun.knockEndShot(), hero ) );
		MegaCannon cannon = new MegaCannon();
		assertSame( cannon, Quality.source( cannon.knockShot(), hero ) );
		Dagger dagger = new Dagger();
		assertSame( dagger, dagger.magicSource() );
		assertSame( dagger, Quality.source( dagger, hero ) );
	}

	@Test
	public void aBlazingFlamethrowersShotsBurn(){
		FT ft = new FT();
		ft.enchant( new Blazing() );
		Gun.Bullet bullet = ft.knockBullet();
		Char target = dummy( HERO + 2, Char.Alignment.ENEMY, 100000 );
		int burnt = 0, hits = 600;
		Random.pushGenerator( 0xB1A2EL );
		try {
			for (int i = 0; i < hits; i++){
				bullet.proc( Dungeon.hero, target, 10 );
				Burning b = target.buff( Burning.class );
				if (b != null){
					burnt++;
					b.detach();
				}
			}
		} finally {
			Random.popGenerator();
		}
		//a bare blazing at +0 burns a third of the time
		float rate = burnt / (float) hits;
		assertTrue( "burnt " + rate, rate >= 0.25f && rate <= 0.41f );
	}

	@Test
	public void aBowsArrowsFireItsSecondEnchantment(){
		Bow bow = new Bow();
		bow.enchant( new Projecting() );
		bow.addEnchant( new Blazing() );
		assertTrue( bow.enchantment2 instanceof Blazing );
		BowWeapon.Arrow arrow = bow.knockArrow();
		Char target = dummy( HERO + 2, Char.Alignment.ENEMY, 100000 );
		int burnt = 0, hits = 600;
		Random.pushGenerator( 0xA22011L );
		try {
			for (int i = 0; i < hits; i++){
				arrow.proc( Dungeon.hero, target, 10 );
				Burning b = target.buff( Burning.class );
				if (b != null){
					burnt++;
					b.detach();
				}
			}
		} finally {
			Random.popGenerator();
		}
		float rate = burnt / (float) hits;
		assertTrue( "burnt " + rate, rate >= 0.25f && rate <= 0.41f );
	}

	@Test
	public void launcherDamageCountsItsRarity(){
		LG common = new LG(), legendary = new LG();
		only( common, Rarity.COMMON, 0f );
		only( legendary, Rarity.LEGENDARY, 0f );
		assertTrue( legendary.bulletMax() > common.bulletMax() );
		assertTrue( legendary.bulletMin() >= common.bulletMin() );
		//the shot deals what the window prints
		assertEquals( legendary.bulletMax(), legendary.knockBullet().max() );
		assertEquals( legendary.bulletMin(), legendary.knockBullet().min() );

		//a DAMAGE_MAX line lifts the shot as it lifts a sword
		FT plain = new FT(), lined = new FT();
		only( plain, Rarity.RARE, 1f );
		only( lined, Rarity.RARE, 1f, RarityLine.DAMAGE_MAX );
		int base = plain.bulletMax();
		assertTrue( lined.bulletMax() > base );

		Bow bowCommon = new Bow(), bowLegendary = new Bow();
		only( bowCommon, Rarity.COMMON, 0f );
		only( bowLegendary, Rarity.LEGENDARY, 0f );
		assertTrue( bowLegendary.arrowMax() > bowCommon.arrowMax() );
		assertEquals( bowLegendary.arrowMax(), bowLegendary.knockArrow().max() );
	}

	@Test
	public void accuracyReachesAGunsBullet(){
		Char target = dummy( HERO + 4, Char.Alignment.ENEMY, 100 );
		FT plain = new FT(), aimed = new FT();
		only( plain, Rarity.RARE, 1f );
		only( aimed, Rarity.RARE, 1f, RarityLine.ACCURACY );
		float without = plain.knockBullet().accuracyFactor( Dungeon.hero, target );
		float with = aimed.knockBullet().accuracyFactor( Dungeon.hero, target );
		assertEquals( 1f + RarityLine.ACCURACY.max(), with / without, 1e-4f );
	}

	@Test
	public void accuracyReachesTheSpiritElfAndTaurcenArrows(){
		Char target = dummy( HERO + 4, Char.Alignment.ENEMY, 100 );
		SpiritBow spiritPlain = new SpiritBow(), spiritAimed = new SpiritBow();
		only( spiritPlain, Rarity.RARE, 1f );
		only( spiritAimed, Rarity.RARE, 1f, RarityLine.ACCURACY );
		assertEquals( 1f + RarityLine.ACCURACY.max(),
				spiritAimed.knockArrow().accuracyFactor( Dungeon.hero, target )
						/ spiritPlain.knockArrow().accuracyFactor( Dungeon.hero, target ), 1e-4f );
		ElfBow elfPlain = new ElfBow(), elfAimed = new ElfBow();
		only( elfPlain, Rarity.RARE, 1f );
		only( elfAimed, Rarity.RARE, 1f, RarityLine.ACCURACY );
		assertEquals( 1f + RarityLine.ACCURACY.max(),
				elfAimed.knockArrow().accuracyFactor( Dungeon.hero, target )
						/ elfPlain.knockArrow().accuracyFactor( Dungeon.hero, target ), 1e-4f );
		TaurcenBow taurcenPlain = new TaurcenBow(), taurcenAimed = new TaurcenBow();
		only( taurcenPlain, Rarity.RARE, 1f );
		only( taurcenAimed, Rarity.RARE, 1f, RarityLine.ACCURACY );
		assertEquals( 1f + RarityLine.ACCURACY.max(),
				taurcenAimed.knockArrow().accuracyFactor( Dungeon.hero, target )
						/ taurcenPlain.knockArrow().accuracyFactor( Dungeon.hero, target ), 1e-4f );
	}

	@Test
	public void anAlphaBowPiercesAtRange(){
		Hero hero = Dungeon.hero;
		Bow bow = new Bow();
		Quality.roll( bow, Rarity.LEGENDARY, ItemType.ALPHA, 0 );
		hero.belongings.weapon = bow;
		hero.belongings.thrownWeapon = bow.knockArrow();
		//the alpha Marksman cuts through armor from 6 - 3 tiles out
		Char far = dummy( HERO + 4, Char.Alignment.ENEMY, 100 );
		assertEquals( Math.min( 0.40f, 0.10f * 3 ), Quality.drPierce( hero, far ), 1e-5f );
		Char near = dummy( HERO + 1, Char.Alignment.ENEMY, 100 );
		assertEquals( 0f, Quality.drPierce( hero, near ), 1e-5f );
		hero.belongings.thrownWeapon = null;
	}
}
