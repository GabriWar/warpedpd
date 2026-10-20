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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.LG.LG;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.HERO;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.dummy;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.only;

/** The five weapon lines: where they roll, what they read, and that each acts at its value. */
public class RarityLineWeaponTest {

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;
	private int savedBullets;

	@BeforeClass
	public static void boot() throws Exception {
		QualitySourceTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//a level's cells reach the co-op layer, which reads the game's version as it starts
		savedVersion = Game.version;
		Game.version = "test";
		savedBullets = Dungeon.bullet;
		QualitySourceTest.arena();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.version = savedVersion;
		Dungeon.bullet = savedBullets;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//a monster's blow: no hero perk joins in, the lines act for any wielder
	private static Char brute( int pos ){
		return dummy( pos, Char.Alignment.ENEMY, 100 );
	}

	@Test
	public void launcherLinesRollOnlyWhereTheyFit(){
		ArrayList<Supplier<Item>> kinds = new ArrayList<>();
		kinds.add( Longsword::new );
		kinds.add( Dart::new );
		kinds.add( Javelin::new );
		kinds.add( SpiritBow::new );
		kinds.add( Bow::new );
		kinds.add( FT::new );
		kinds.add( LG::new );
		kinds.add( MiniGun::new );
		kinds.add( ShootGun::new );
		boolean rangeSeen = false, reloadSeen = false, ammoSeen = false;
		Random.pushGenerator( 0x11E5L );
		try {
			for (int i = 0; i < 500; i++){
				for (Supplier<Item> kind : kinds){
					Item item = kind.get();
					Quality.roll( item, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
					String what = item.getClass().getSimpleName();
					boolean gun = item instanceof Gun;
					boolean reloads = gun || item instanceof MiniGun || item instanceof ShootGun;
					for (RarityLine l : item.quality.lines){
						if (l == RarityLine.RANGE){ assertTrue( what, gun ); rangeSeen = true; }
						if (l == RarityLine.RELOAD){ assertTrue( what, reloads ); reloadSeen = true; }
						if (l == RarityLine.AMMO){ assertTrue( what, reloads || item instanceof BowWeapon ); ammoSeen = true; }
					}
				}
			}
		} finally {
			Random.popGenerator();
		}
		assertTrue( rangeSeen && reloadSeen && ammoSeen );
	}

	@Test
	public void theFallbacksNeverGiveASwordALauncherLine(){
		for (int i = 0; i < 300; i++){
			Longsword sword = new Longsword();
			Quality q = only( sword, Rarity.COMMON, 0f );
			q.masterworkStep( sword );
			assertEquals( 1, q.lines.size() );
			//a fallback line is a plain bonus, never a trait (a launcher's lines are traits)
			assertTrue( q.lines.toString(), RarityLine.plain( Quality.Family.WEAPON, sword ).contains( q.lines.get( 0 ) ) );

			Longsword other = new Longsword();
			Quality q2 = only( other, Rarity.COMMON, 0f );
			q2.masterwork = 2;
			q2.rerollRarity( other, Rarity.COMMON );
			for (RarityLine l : q2.lines){
				assertTrue( l.toString(), RarityLine.plain( Quality.Family.WEAPON, other ).contains( l ) );
			}
		}
		ArrayList<RarityLine> sword = RarityLine.pool( Quality.Family.WEAPON, new Longsword() );
		assertFalse( sword.contains( RarityLine.AMMO ) );
		assertFalse( sword.contains( RarityLine.RELOAD ) );
		assertFalse( sword.contains( RarityLine.RANGE ) );
		assertFalse( sword.contains( RarityLine.VESTIGE ) );
		assertTrue( sword.contains( RarityLine.CRIT ) );
		assertTrue( sword.contains( RarityLine.LIFESTEAL ) );
		//five plain lines, crit and lifesteal, and the kind's two traits (LineSlotsTest)
		assertEquals( 9, sword.size() );
		assertEquals( 10, RarityLine.pool( Quality.Family.WEAPON, new Bow() ).size() );
		assertEquals( 11, RarityLine.pool( Quality.Family.WEAPON, new FT() ).size() );
		assertEquals( 10, RarityLine.pool( Quality.Family.WEAPON, new MiniGun() ).size() );
		assertEquals( 9, RarityLine.pool( Quality.Family.WEAPON, new SpiritBow() ).size() );
		assertEquals( 9, RarityLine.pool( Quality.Family.MISSILE, new Javelin() ).size() );
		//the four kinds of the rt2 pass: nine each, their own two traits among them
		poolHolds( new HandAxe(), RarityLine.EXECUTIONER, RarityLine.WARM_GRIP );
		poolHolds( new WarHammer(), RarityLine.SLAYER, RarityLine.WARM_GRIP );
		poolHolds( new Flute(), RarityLine.MOONLIT, RarityLine.STORMBORN );
		poolHolds( new Whip(), RarityLine.HUNTER, RarityLine.EXECUTIONER );
		//armor: four plain lines; warmth, cooling, rainproof, fleet and hushed on light armor, warmth,
		//windbreak, sure footing and resolve on heavy
		assertEquals( 9, RarityLine.pool( Quality.Family.ARMOR, new ClothArmor() ).size() );
		assertEquals( 8, RarityLine.pool( Quality.Family.ARMOR, new PlateArmor() ).size() );
	}

	//a melee weapon's pool: nine lines, these two traits in it
	private static void poolHolds( Item weapon, RarityLine a, RarityLine b ){
		ArrayList<RarityLine> pool = RarityLine.pool( Quality.Family.WEAPON, weapon );
		String what = weapon.getClass().getSimpleName() + " " + pool;
		assertEquals( what, 9, pool.size() );
		assertTrue( what, pool.contains( a ) );
		assertTrue( what, pool.contains( b ) );
	}

	@Test
	public void critFiresAtItsValue(){
		Longsword sword = new Longsword();
		only( sword, Rarity.RARE, 1f, RarityLine.CRIT );
		Char attacker = brute( HERO );
		Char target = dummy( HERO + 1, Char.Alignment.ALLY, 100000 );
		int crits = 0, hits = 20000;
		Random.pushGenerator( 0xC217L );
		try {
			for (int i = 0; i < hits; i++){
				int dealt = Quality.weaponHit( sword, attacker, target, 100 );
				if (dealt == 150) crits++;
				else assertEquals( 100, dealt );
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( RarityLine.CRIT.max(), crits / (float) hits, 0.01f );
	}

	@Test
	public void aWildFlailCritsOnceABlow(){
		Hero hero = Dungeon.hero;
		Flail flail = new Flail();
		Quality q = only( flail, Rarity.RARE, 8f, RarityLine.CRIT );
		q.type = ItemType.ALPHA;
		assertEquals( WeaponPerk.WILD, WeaponPerk.of( flail ) );
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 100000 );
		//alpha Wild crits 30% for x1.9; the line at 96% would crit what is left, never on top
		int wild = Math.round( 100 * 1.9f ), line = 150;
		int wildSeen = 0, lineSeen = 0;
		Random.pushGenerator( 0xF1A1L );
		try {
			for (int i = 0; i < 4000; i++){
				int dealt = Quality.weaponHit( flail, hero, target, 100 );
				assertTrue( "dealt " + dealt, dealt == 100 || dealt == wild || dealt == line );
				if (dealt == wild) wildSeen++;
				if (dealt == line) lineSeen++;
				for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff b : target.buffs()) b.detach();
			}
		} finally {
			Random.popGenerator();
		}
		assertTrue( wildSeen > 0 && lineSeen > 0 );
	}

	@Test
	public void lifestealHealsAThirdWithinItsCaps(){
		Longsword sword = new Longsword();
		//rolled past its maximum so it always fires
		only( sword, Rarity.RARE, 20f, RarityLine.LIFESTEAL );
		Char attacker = brute( HERO );
		attacker.HP = 50;
		Quality.weaponHit( sword, attacker, dummy( HERO + 1, Char.Alignment.ALLY, 1000 ), 30 );
		assertEquals( 60, attacker.HP );

		//capped by what the attacker is missing
		attacker.HP = 95;
		Quality.weaponHit( sword, attacker, dummy( HERO + 19, Char.Alignment.ALLY, 1000 ), 30 );
		assertEquals( 100, attacker.HP );

		//capped by what the target has left: overkill heals nothing
		attacker.HP = 50;
		Quality.weaponHit( sword, attacker, dummy( HERO - 19, Char.Alignment.ALLY, 4 ), 30 );
		assertEquals( 54, attacker.HP );

		//never off an ally, nor off something neutral
		attacker.HP = 50;
		Quality.weaponHit( sword, attacker, dummy( HERO + 20, Char.Alignment.ENEMY, 1000 ), 30 );
		Quality.weaponHit( sword, attacker, dummy( HERO + 18, Char.Alignment.NEUTRAL, 1000 ), 30 );
		assertEquals( 50, attacker.HP );
	}

	@Test
	public void aSparedShotKeepsItsRound(){
		FT ft = new FT();
		only( ft, Rarity.RARE, 1f, RarityLine.AMMO );
		Gun.Bullet bullet = ft.knockBullet();
		int spared = 0, shots = 20000;
		Random.pushGenerator( 0xA330L );
		try {
			for (int i = 0; i < shots; i++){
				ft.quickReload();
				bullet.onShoot();
				if (ft.round() == ft.maxRound()) spared++;
				else assertEquals( ft.maxRound() - 1, ft.round() );
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( RarityLine.AMMO.max(), spared / (float) shots, 0.01f );

		//a bow's arrow: the same chance, capped at a half however high the line rolls
		Bow bow = new Bow();
		only( bow, Rarity.RARE, 20f, RarityLine.AMMO );
		BowWeapon.Arrow arrow = bow.knockArrow();
		Dungeon.bullet = shots;
		Random.pushGenerator( 0xA331L );
		try {
			for (int i = 0; i < shots; i++) arrow.onShoot();
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.5f, Dungeon.bullet / (float) shots, 0.01f );
	}

	@Test
	public void theShotgunsEndShotAlwaysEmpties() throws Exception {
		ShootGun shotgun = new ShootGun();
		only( shotgun, Rarity.RARE, 20f, RarityLine.AMMO );
		Method use = ShootGun.ShotAmmo.class.getDeclaredMethod( "useAmmo", Hero.class );
		use.setAccessible( true );
		int spared = 0;
		Random.pushGenerator( 0xE5D0L );
		try {
			for (int i = 0; i < 2000; i++){
				shotgun.charge = shotgun.fullcharge;
				use.invoke( shotgun.knockEndShot(), Dungeon.hero );
				assertEquals( 0, shotgun.charge );
				shotgun.charge = shotgun.fullcharge;
				use.invoke( shotgun.knockShot(), Dungeon.hero );
				if (shotgun.charge == shotgun.fullcharge) spared++;
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0.5f, spared / 2000f, 0.04f );
	}

	@Test
	public void aFreeReloadTakesNoTimeButStillSpendsBullets(){
		Hero hero = Dungeon.hero;
		FT ft = new FT();
		only( ft, Rarity.RARE, 20f, RarityLine.RELOAD );
		int free = 0, reloads = 400;
		Random.pushGenerator( 0x2E10L );
		try {
			for (int i = 0; i < reloads; i++){
				Dungeon.bullet = 100;
				ft.useRound();
				ft.useRound();
				float before = hero.cooldown();
				ft.reload();
				assertEquals( ft.maxRound(), ft.round() );
				//a flamethrower's two rounds cost two bullets each
				assertEquals( 96, Dungeon.bullet );
				if (hero.cooldown() == before) free++;
				else assertEquals( before + ft.reloadTime( hero ), hero.cooldown(), 1e-4f );
			}
		} finally {
			Random.popGenerator();
		}
		//capped at a half however high the line rolls
		assertEquals( 0.5f, free / (float) reloads, 0.08f );
	}

	@Test
	public void rangeAddsWholeTilesToTheFlamethrowerAndTheLaser(){
		FT ft = new FT();
		LG lg = new LG();
		Quality qf = only( ft, Rarity.EXOTIC, 1f, RarityLine.RANGE );
		Quality ql = only( lg, Rarity.EXOTIC, 1f, RarityLine.RANGE );
		qf.masterwork = ql.masterwork = Quality.MASTERWORK_MAX;
		assertEquals( 6, ft.baseRange() );
		assertEquals( 12, lg.baseRange() );
		assertEquals( 2, Quality.rangeBonus( ft, ft.baseRange() ) );
		assertEquals( 5, Quality.rangeBonus( lg, lg.baseRange() ) );
		assertEquals( "+2 range", RarityLine.RANGE.describe( ft, qf.roll( RarityLine.RANGE ) ) );
		//the faintest roll still reaches a tile further
		qf.masterwork = 0;
		qf.rolls.set( 0, 0.01f );
		assertEquals( 1, Quality.rangeBonus( ft, ft.baseRange() ) );
		qf.lines.clear();
		qf.rolls.clear();
		assertEquals( 0, Quality.rangeBonus( ft, ft.baseRange() ) );
	}

	@Test
	public void theLaunchersDrawNothingWithoutTheirLines(){
		FT ft = new FT();
		only( ft, Rarity.RARE, 1f, RarityLine.ACCURACY );
		long expected = firstLong( 0x5EEDL );
		Random.pushGenerator( 0x5EEDL );
		try {
			//a typed flamethrower is a Gunner, whose own free reloads draw (WeaponPerkTest);
			//its spared rounds wait for alpha
			assertFalse( Quality.rollFreeReload( new FT(), Dungeon.hero ) );
			assertFalse( Quality.rollSpareAmmo( ft, Dungeon.hero ) );
			assertFalse( Quality.rollFreeReload( new MiniGun(), Dungeon.hero ) );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void aCommonSwingDrawsWhatItDidBefore(){
		Hero hero = Dungeon.hero;
		Longsword sword = new Longsword();
		only( sword, Rarity.COMMON, 0.3f, RarityLine.ACCURACY );
		sword.identify();
		Char target = dummy( HERO + 1, Char.Alignment.ENEMY, 1000 );
		//no enchantment, a Momentum perk and no new line: nothing in the blow is left to chance
		long expected = firstLong( 0xC0DEL );
		Random.pushGenerator( 0xC0DEL );
		try {
			sword.proc( hero, target, 10 );
			assertEquals( expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}

		//with a real enchantment it draws exactly what that enchantment draws
		sword.enchant( new Blazing() );
		Char a = dummy( HERO + 19, Char.Alignment.ENEMY, 1000 );
		Random.pushGenerator( 0xC0DFL );
		long viaProc;
		try {
			sword.proc( hero, a, 10 );
			viaProc = Random.Long();
		} finally {
			Random.popGenerator();
		}
		Char b = dummy( HERO - 19, Char.Alignment.ENEMY, 1000 );
		Random.pushGenerator( 0xC0DFL );
		long direct;
		try {
			new Blazing().proc( sword, hero, b, 10 );
			direct = Random.Long();
		} finally {
			Random.popGenerator();
		}
		assertEquals( direct, viaProc );
	}

	@Test
	public void everyNewLineHasText(){
		FT ft = new FT();
		Quality q = only( ft, Rarity.LEGENDARY, 1f, RarityLine.CRIT, RarityLine.LIFESTEAL,
				RarityLine.AMMO, RarityLine.RELOAD, RarityLine.RANGE );
		for (RarityLine l : q.lines){
			String text = l.describe( ft, q.roll( l ) );
			assertFalse( l + ": " + text, text.contains( Messages.NO_TEXT_FOUND ) );
		}
		assertEquals( "+12% chance to land a critical hit for half again the damage",
				RarityLine.CRIT.describe( ft, 1f ) );
	}

	private static long firstLong( long seed ){
		Random.pushGenerator( seed );
		try {
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}
}
