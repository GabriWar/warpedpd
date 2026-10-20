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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.only;

/** The armor traits: each acts where it says, at its value, and draws what the game drew before. */
public class ArmorTraitTest {

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;
	private int savedDepth;

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
		savedDepth = Dungeon.depth;
		QualitySourceTest.arena();
		ClimateManager.debugWindOverride = 0f;
	}

	@After
	public void tearDown(){
		Actor.clear();
		ClimateManager.debugTempOverride = Float.NaN;
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugPrecipTypeOverride = null;
		Dungeon.depth = savedDepth;
		HomesteadWorld.reset();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.version = savedVersion;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//what the hero feels on his own cell at an air temperature
	private static float feels( Hero hero, float air ){
		ClimateManager.debugTempOverride = air;
		return TileTemperature.feelsLikeAt( hero.pos, hero );
	}

	@Test
	public void fleetCapsAtTenPercent(){
		Hero hero = Dungeon.hero;
		LeatherArmor armor = new LeatherArmor();
		Quality q = only( armor, Rarity.RARE, 0.5f, RarityLine.FLEET );
		hero.belongings.armor = armor;
		assertEquals( q.value( RarityLine.FLEET ), speed( hero ), 1e-5f );
		assertEquals( "+4% movement speed", RarityLine.FLEET.describe( armor, q.roll( RarityLine.FLEET ) ) );
		//worked to its full it would be 16%: the line alone stops at 10%
		q.masterwork = Quality.MASTERWORK_MAX;
		q.rolls.set( 0, 1f );
		assertEquals( 0.16f, q.value( RarityLine.FLEET ), 1e-5f );
		assertEquals( RarityLine.FLEET_CAP, speed( hero ), 1e-5f );
		assertEquals( "+10% movement speed", RarityLine.FLEET.describe( armor, q.roll( RarityLine.FLEET ) ) );
		//the hero-wide sum is not capped: a ring's stride still adds on top
		RingOfHaste ring = new RingOfHaste();
		Quality r = only( ring, Rarity.RARE, 1f, RarityLine.STRIDE );
		hero.belongings.ring = ring;
		assertEquals( RarityLine.FLEET_CAP + r.value( RarityLine.STRIDE ), speed( hero ), 1e-5f );
	}

	//the hero-wide speed of the worn gear's perks and lines, less what the perks alone give
	private static float speed( Hero hero ){
		Armor a = hero.belongings.armor;
		Quality q = a.quality;
		java.util.ArrayList<RarityLine> lines = new java.util.ArrayList<>( q.lines );
		java.util.ArrayList<Float> rolls = new java.util.ArrayList<>( q.rolls );
		float with = GearPerk.heroBonus( hero, GearPerk.Stat.SPEED );
		Item ring = hero.belongings.ring;
		Quality rq = ring == null ? null : ring.quality;
		java.util.ArrayList<RarityLine> ringLines = rq == null ? null : new java.util.ArrayList<>( rq.lines );
		java.util.ArrayList<Float> ringRolls = rq == null ? null : new java.util.ArrayList<>( rq.rolls );
		q.lines.clear();
		q.rolls.clear();
		if (rq != null){
			rq.lines.clear();
			rq.rolls.clear();
		}
		float without = GearPerk.heroBonus( hero, GearPerk.Stat.SPEED );
		q.lines.addAll( lines );
		q.rolls.addAll( rolls );
		if (rq != null){
			rq.lines.addAll( ringLines );
			rq.rolls.addAll( ringRolls );
		}
		return with - without;
	}

	@Test
	public void hushedAddsStealthAndSilencesWetBoots(){
		Hero hero = Dungeon.hero;
		ClothArmor plain = new ClothArmor(), hushed = new ClothArmor();
		only( plain, Rarity.RARE, 1f );
		Quality q = only( hushed, Rarity.RARE, 0.8f, RarityLine.HUSHED );
		float v = q.value( RarityLine.HUSHED );
		hero.belongings.armor = plain;
		float dry = hero.stealth();
		hero.belongings.armor = hushed;
		assertEquals( dry + v, hero.stealth(), 1e-5f );
		assertEquals( "+0.8 stealth, and wet boots make no sound", RarityLine.HUSHED.describe( hushed, q.roll( RarityLine.HUSHED ) ) );

		//wet boots squelch, but not in hushed armor
		Buff.affect( hero, SoakedShoes.class ).add( SoakedShoes.STEP_DURATION );
		assertEquals( dry + v, hero.stealth(), 1e-5f );
		hero.belongings.armor = plain;
		assertEquals( dry + SoakedShoes.STEALTH_PENALTY, hero.stealth(), 1e-5f );
	}

	private static void weatherTurn( Hero hero ){
		try {
			Method m = Hero.class.getDeclaredMethod( "checkWeatherEffects" );
			m.setAccessible( true );
			m.invoke( hero );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void rainproofDelaysTheSoakAndResetsOnADryTurn(){
		Hero hero = Dungeon.hero;
		ClothArmor armor = new ClothArmor();
		Quality q = only( armor, Rarity.RARE, 1f, RarityLine.RAINPROOF );
		assertEquals( 0.4f, q.value( RarityLine.RAINPROOF ), 1e-6f );
		assertEquals( 10, RarityLine.rainTurns( q.value( RarityLine.RAINPROOF ) ) );
		assertEquals( "rain takes 10 turns to soak you, and puddles soak your boots 40% less often",
				RarityLine.RAINPROOF.describe( armor, q.roll( RarityLine.RAINPROOF ) ) );
		ClimateManager.debugTempOverride = 15f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;

		//no line: the first turn of rain soaks
		ClimateManager.debugPrecipOverride = 1f;
		weatherTurn( hero );
		assertNotNull( hero.buff( Drenched.class ) );
		hero.buff( Drenched.class ).detach();
		ClimateManager.debugPrecipOverride = 0f;
		weatherTurn( hero );

		hero.belongings.armor = armor;
		ClimateManager.debugPrecipOverride = 1f;
		for (int i = 1; i < 10; i++){
			weatherTurn( hero );
			assertNull( "turn " + i, hero.buff( Drenched.class ) );
		}
		weatherTurn( hero );
		assertNotNull( "the tenth turn", hero.buff( Drenched.class ) );
		hero.buff( Drenched.class ).detach();

		//a dry turn starts the count again
		ClimateManager.debugPrecipOverride = 0f;
		weatherTurn( hero );
		assertEquals( 0, hero.rainTurns );
		ClimateManager.debugPrecipOverride = 1f;
		for (int i = 1; i < 10; i++) weatherTurn( hero );
		assertNull( hero.buff( Drenched.class ) );
		weatherTurn( hero );
		assertNotNull( hero.buff( Drenched.class ) );
	}

	@Test
	public void rainproofKeepsTheCountWhenAThatchRoofLetsNoDropThrough(){
		ClimateManager.debugTempOverride = 15f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 1f;
		int[] cells = WeaponTraitTest.leanTo();
		Hero hero = Dungeon.hero;
		ClothArmor armor = new ClothArmor();
		Quality q = only( armor, Rarity.RARE, 1f, RarityLine.RAINPROOF );
		int need = RarityLine.rainTurns( q.value( RarityLine.RAINPROOF ) );
		assertEquals( 10, need );
		hero.belongings.armor = armor;

		//under thatch one drop in five gets through: each counts, and a drop that misses neither
		//counts nor starts the count again, so the tenth drop soaks
		hero.pos = cells[1];
		int turns = 0, last = 0;
		Random.pushGenerator( 0x7A7C4L );
		try {
			while (hero.buff( Drenched.class ) == null){
				assertTrue( "soaked within 1000 turns", ++turns <= 1000 );
				weatherTurn( hero );
				assertTrue( "turn " + turns, hero.rainTurns >= last );
				last = hero.rainTurns;
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( need, hero.rainTurns );
		assertTrue( "some drops missed", turns > need );
		hero.buff( Drenched.class ).detach();

		//a slate roof keeps it all off: the count starts again
		hero.pos = cells[0];
		weatherTurn( hero );
		assertEquals( 0, hero.rainTurns );
		assertNull( hero.buff( Drenched.class ) );
	}

	@Test
	public void aGuestFeelsHisOwnGear(){
		Hero guest = new Hero();
		guest.pos = QualitySourceTest.HERO + 2;
		guest.HP = guest.HT = 100;
		guest.sprite = QualitySourceTest.quietSprite();
		Longsword sword = new Longsword();
		only( sword, Rarity.RARE, 1f, RarityLine.WARM_GRIP );
		ClothArmor cool = new ClothArmor();
		only( cool, Rarity.RARE, 1f, RarityLine.COOLING );
		guest.belongings.weapon = sword;
		guest.belongings.armor = cool;

		//in the cold his body heads for what he feels with his own warm grip, not what the host
		//would feel on his cell
		ClimateManager.debugTempOverride = -10f;
		float own = TileTemperature.feelsLikeAt( guest.pos, guest );
		assertTrue( own > TileTemperature.feelsLikeAt( guest.pos ) );
		weatherTurn( guest );
		assertEquals( own, guest.bodyTemp, 1e-4f );

		//in the heat his own cooling armor eases his heatstroke
		ClimateManager.debugTempOverride = 47f;
		assertTrue( TileTemperature.feelsLikeAt( guest.pos, guest ) < 45f );
		assertTrue( TileTemperature.feelsLikeAt( guest.pos ) >= 45f );
		Heatstroke heat = Buff.affect( guest, Heatstroke.class );
		assertEquals( 1.2f, heat.hungerFactor(), 1e-6f );
	}

	@Test
	public void rainproofShoeSoakDrawsAsOftenAsBefore(){
		Hero hero = Dungeon.hero;
		int a = QualitySourceTest.HERO, b = a + 1;
		Level.set( a, Terrain.WATER, Dungeon.level );
		Level.set( b, Terrain.WATER, Dungeon.level );
		ClothArmor armor = new ClothArmor();
		Quality q = only( armor, Rarity.RARE, 1f, RarityLine.RAINPROOF );
		q.masterwork = Quality.MASTERWORK_MAX;
		float v = q.value( RarityLine.RAINPROOF );
		assertEquals( 0.8f, v, 1e-6f );

		int[] plain = steps( hero, null, a, b );
		int[] dry = steps( hero, armor, a, b );
		//the same draws, step for step
		assertEquals( plain[0], dry[0] );
		assertEquals( 0.02f, plain[1] / 6000f, 0.008f );
		assertEquals( 0.02f * (1f - v), dry[1] / 6000f, 0.003f );
	}

	//6000 steps across two water cells, the boots dried after each: { draws, soakings }
	private static int[] steps( Hero hero, Armor armor, int a, int b ){
		hero.belongings.armor = armor;
		hero.pos = a;
		int soaked = 0;
		LineSlotsTest.Counter c = LineSlotsTest.Counter.push( 0x50A7L );
		try {
			for (int i = 0; i < 6000; i++){
				hero.move( hero.pos == a ? b : a, true );
				SoakedShoes s = hero.buff( SoakedShoes.class );
				if (s != null){
					soaked++;
					s.detach();
				}
			}
		} finally {
			Random.popGenerator();
		}
		return new int[]{ c.ints + c.floats, soaked };
	}

	@Test
	public void coolingOnlyBitesAbove25(){
		Hero hero = Dungeon.hero;
		ClothArmor plain = new ClothArmor(), cool = new ClothArmor();
		only( plain, Rarity.RARE, 1f );
		Quality q = only( cool, Rarity.RARE, 1f, RarityLine.COOLING );
		float v = q.value( RarityLine.COOLING );
		assertEquals( 5f, v, 1e-6f );
		assertEquals( "up to 5.0°C cooler in the heat", RarityLine.COOLING.describe( cool, q.roll( RarityLine.COOLING ) ) );
		hero.belongings.armor = plain;
		float hot = feels( hero, 40f ), warm = feels( hero, 25f ), cold = feels( hero, -5f );
		hero.belongings.armor = cool;
		assertEquals( hot - v, feels( hero, 40f ), 1e-4f );
		//never below 25
		assertEquals( 26.5f, warm, 1e-4f );
		assertEquals( 25f, feels( hero, 25f ), 1e-4f );
		assertEquals( cold, feels( hero, -5f ), 1e-4f );
	}

	@Test
	public void windbreakCutsOnlyTheChill(){
		Hero hero = Dungeon.hero;
		PlateArmor plain = new PlateArmor(), shield = new PlateArmor();
		only( plain, Rarity.RARE, 1f );
		Quality q = only( shield, Rarity.RARE, 1f, RarityLine.WINDBREAK );
		float v = q.value( RarityLine.WINDBREAK );
		ClimateManager.debugWindOverride = 20f;
		hero.belongings.armor = plain;
		float chilled = feels( hero, 0f ), mild = feels( hero, 20f );
		ClimateManager.debugWindOverride = 0f;
		float still = feels( hero, 0f );
		assertEquals( still - 10f, chilled, 1e-4f );

		ClimateManager.debugWindOverride = 20f;
		hero.belongings.armor = shield;
		assertEquals( still - 10f * (1f - v), feels( hero, 0f ), 1e-4f );
		assertEquals( mild, feels( hero, 20f ), 1e-4f );
		//the cell beside him feels the whole of it
		assertEquals( TileTemperature.feelsLikeAt( hero.pos + 1, null ), TileTemperature.feelsLikeAt( hero.pos + 1, hero ), 1e-4f );
	}

	@Test
	public void surefootScalesTheFallButDrawsTheSame() throws Exception {
		Hero hero = Dungeon.hero;
		PlateArmor plain = new PlateArmor(), sure = new PlateArmor();
		only( plain, Rarity.RARE, 1f );
		Quality q = only( sure, Rarity.RARE, 1f, RarityLine.SUREFOOT );
		float v = q.value( RarityLine.SUREFOOT );
		assertEquals( "falls hurt 40% less", RarityLine.SUREFOOT.describe( sure, q.roll( RarityLine.SUREFOOT ) ) );
		//the landing shakes the screen and the blow interrupts the hero's key hold: headless there is
		//no scene, so a camera and a bare cell selector stand in (as in WeaponPerkTest)
		com.watabou.noosa.Camera main = com.watabou.noosa.Camera.main;
		com.watabou.noosa.Camera.main = new com.watabou.noosa.Camera( 0, 0, 100, 100, 1f );
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		Object selector = sel.get( null );
		if (selector == null) sel.set( null, bare( CellSelector.class ) );
		float[] hard, soft;
		try {
			hard = fall( hero, plain );
			soft = fall( hero, sure );
		} finally {
			com.watabou.noosa.Camera.main = main;
			sel.set( null, selector );
		}
		//the same draws, the damage and the limp scaled
		assertEquals( hard[0], soft[0], 0f );
		assertEquals( Math.round( hard[1] * (1f - v) ), soft[1], 1f );
		assertEquals( hard[2] * (1f - v), soft[2], 1e-3f );
		assertTrue( soft[1] < hard[1] );
	}

	@SuppressWarnings("unchecked")
	private static <T> T bare( Class<T> c ) throws ReflectiveOperationException {
		Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
		u.setAccessible( true );
		Object unsafe = u.get( null );
		return (T) unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, c );
	}

	//a landing at full health: { draws, health lost, turns limping }
	private static float[] fall( Hero hero, Armor armor ){
		hero.belongings.armor = armor;
		hero.HP = hero.HT = 100;
		for (Buff b : hero.buffs()) b.detach();
		LineSlotsTest.Counter c = LineSlotsTest.Counter.push( 0xFA77L );
		try {
			Chasm.heroLand();
		} finally {
			Random.popGenerator();
		}
		Cripple limp = hero.buff( Cripple.class );
		assertNotNull( hero.buff( Bleeding.class ) );
		return new float[]{ c.ints + c.floats, 100 - hero.HP, limp == null ? 0f : limp.cooldown() };
	}

	@Test
	public void resolveFromHeavyArmorFeedsTheResist(){
		Hero hero = Dungeon.hero;
		assertTrue( RarityLine.pool( Quality.Family.ARMOR, new PlateArmor() ).contains( RarityLine.RESOLVE ) );
		assertFalse( RarityLine.pool( Quality.Family.ARMOR, new ClothArmor() ).contains( RarityLine.RESOLVE ) );
		PlateArmor plate = new PlateArmor();
		Quality q = only( plate, Rarity.RARE, 1f, RarityLine.RESOLVE );
		hero.belongings.armor = plate;
		float with = GearPerk.heroBonus( hero, GearPerk.Stat.DEBUFF_RESIST );
		q.lines.clear();
		q.rolls.clear();
		assertEquals( RarityLine.RESOLVE.max(), with - GearPerk.heroBonus( hero, GearPerk.Stat.DEBUFF_RESIST ), 1e-5f );
	}

	@Test
	public void everyArmorTraitReadsPlainly(){
		for (Armor a : new Armor[]{ new ClothArmor(), new PlateArmor() }){
			Quality q = only( a, Rarity.LEGENDARY, 1f );
			for (RarityLine l : RarityLine.traits( Quality.Family.ARMOR, a )){
				q.lines.add( l );
				q.rolls.add( 1f );
				String text = l.describe( a, 1f );
				assertFalse( l + ": " + text, text.contains( Messages.NO_TEXT_FOUND ) );
				assertFalse( l + " shows a raw placeholder: " + text, text.matches( "(?s).*%\\d?\\$?[ds].*" ) );
			}
		}
	}

	@Test
	public void warmthNoLongerHeatsInTheHeat(){
		Hero hero = Dungeon.hero;
		ClothArmor plain = new ClothArmor(), warm = new ClothArmor();
		only( plain, Rarity.RARE, 1f );
		Quality q = only( warm, Rarity.RARE, 1f, RarityLine.WARMTH );
		float v = q.value( RarityLine.WARMTH );
		//cloth's own trapped air stays two-sided, and the line is not part of it
		assertEquals( plain.thermalOffset(), warm.thermalOffset(), 0f );
		assertEquals( 1.5f, warm.thermalOffset(), 0f );

		hero.belongings.armor = plain;
		float coldPlain = feels( hero, -10f ), hotPlain = feels( hero, 30f ), mildPlain = feels( hero, 15f );
		hero.belongings.armor = warm;
		assertEquals( coldPlain + v, feels( hero, -10f ), 1e-4f );
		assertEquals( hotPlain, feels( hero, 30f ), 1e-4f );
		//up to comfort and no further
		assertEquals( 16.5f, mildPlain, 1e-4f );
		assertEquals( 18f, feels( hero, 15f ), 1e-4f );
	}
}
