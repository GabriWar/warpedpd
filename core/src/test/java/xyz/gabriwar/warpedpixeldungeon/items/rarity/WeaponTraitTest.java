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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.HERO;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.dummy;
import static xyz.gabriwar.warpedpixeldungeon.items.rarity.QualitySourceTest.only;

/** The weapon traits: each acts where it says, at its value, and draws only where its spec says. */
public class WeaponTraitTest {

	private Level savedLevel;
	private Hero savedHero;
	private String savedVersion;
	private int savedDepth, savedChallenges, savedTurn;
	private DayNightCycle.Phase savedPhase;
	private float savedWindField, savedWindDir;

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
		savedChallenges = Dungeon.challenges;
		savedTurn = Dungeon.cycleTurn;
		savedPhase = DayNightCycle.debugPhaseOverride;
		savedWindField = climateField( "localWindSpeed" );
		savedWindDir = climateField( "surfaceWindDir" );
		Dungeon.challenges = 0;
		QualitySourceTest.arena();
	}

	@After
	public void tearDown(){
		Actor.clear();
		ClimateManager.debugTempOverride = Float.NaN;
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugPrecipTypeOverride = null;
		setClimateField( "localWindSpeed", savedWindField );
		setClimateField( "surfaceWindDir", savedWindDir );
		DayNightCycle.debugPhaseOverride = savedPhase;
		Dungeon.cycleTurn = savedTurn;
		Dungeon.challenges = savedChallenges;
		Dungeon.depth = savedDepth;
		HomesteadWorld.reset();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Game.version = savedVersion;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	private static float climateField( String name ){
		try {
			Field f = ClimateManager.class.getDeclaredField( name );
			f.setAccessible( true );
			return f.getFloat( null );
		} catch (ReflectiveOperationException e){
			throw new IllegalStateException( e );
		}
	}

	private static void setClimateField( String name, float value ){
		try {
			Field f = ClimateManager.class.getDeclaredField( name );
			f.setAccessible( true );
			f.setFloat( null, value );
		} catch (ReflectiveOperationException e){
			throw new IllegalStateException( e );
		}
	}

	//a monster's blow: no hero perk joins in, the lines act for any wielder
	private static Char brute(){
		return dummy( HERO, Char.Alignment.ENEMY, 100 );
	}

	private static Char foe( int pos, int hp ){
		Char ch = dummy( pos, Char.Alignment.ALLY, 100 );
		ch.HP = hp;
		return ch;
	}

	@Test
	public void everyTraitReadsPlainly(){
		Longsword sword = new Longsword();
		RarityLine[] traits = { RarityLine.WARM_GRIP, RarityLine.SLAYER, RarityLine.EXECUTIONER, RarityLine.MOONLIT,
				RarityLine.STORMBORN, RarityLine.DEFIANT, RarityLine.HUNTER, RarityLine.STEADY };
		Quality q = only( sword, Rarity.LEGENDARY, 1f, traits );
		q.slayer = SlayerFamily.UNDEAD;
		for (RarityLine l : traits){
			String text = l.describe( sword, q.roll( l ) );
			assertFalse( l + ": " + text, text.contains( xyz.gabriwar.warpedpixeldungeon.messages.Messages.NO_TEXT_FOUND ) );
			assertFalse( l + " shows a raw placeholder: " + text, text.matches( "(?s).*%\\d?\\$?[ds].*" ) );
			assertFalse( l + ": " + text, text.contains( "!" ) );
		}
		assertEquals( "+3.0°C warmth in the cold while held", RarityLine.WARM_GRIP.describe( sword, 1f ) );
		assertEquals( "+30% damage against the undead", RarityLine.SLAYER.describe( sword, 1f ) );
		assertEquals( "+15% damage and accuracy at night, half at dusk and dawn, 5% less around noon",
				RarityLine.MOONLIT.describe( sword, 1f ) );
		assertEquals( "wind spoils 50% less of your aim", RarityLine.STEADY.describe( sword, 1f ) );
	}

	// ---------------------------------------------------------------- warm grip

	@Test
	public void warmGripOnlyFightsTheColdAndOnlyInHand(){
		Hero hero = Dungeon.hero;
		Longsword sword = new Longsword();
		only( sword, Rarity.RARE, 1f, RarityLine.WARM_GRIP );
		float w = RarityLine.WARM_GRIP.max();
		ClimateManager.debugWindOverride = 0f;

		ClimateManager.debugTempOverride = -10f;
		float bare = TileTemperature.feelsLikeAt( hero.pos, hero );
		hero.belongings.weapon = sword;
		assertEquals( bare + w, TileTemperature.feelsLikeAt( hero.pos, hero ), 1e-4f );
		//the hero's own cell only: a cell beside him reads the air
		assertEquals( TileTemperature.tileTemp( hero.pos + 1 ), TileTemperature.feelsLikeAt( hero.pos + 1, hero ), 1e-4f );

		//never past comfort
		ClimateManager.debugTempOverride = 17f;
		hero.belongings.weapon = null;
		float mild = TileTemperature.feelsLikeAt( hero.pos, hero );
		hero.belongings.weapon = sword;
		assertEquals( Math.min( 18f, mild + w ), TileTemperature.feelsLikeAt( hero.pos, hero ), 1e-4f );

		//nothing in the heat
		ClimateManager.debugTempOverride = 30f;
		hero.belongings.weapon = null;
		float hot = TileTemperature.feelsLikeAt( hero.pos, hero );
		hero.belongings.weapon = sword;
		assertEquals( hot, TileTemperature.feelsLikeAt( hero.pos, hero ), 1e-4f );

		//in the pack it warms nothing
		ClimateManager.debugTempOverride = -10f;
		hero.belongings.weapon = null;
		hero.belongings.backpack.items.add( sword );
		assertEquals( bare, TileTemperature.feelsLikeAt( hero.pos, hero ), 1e-4f );
	}

	// ---------------------------------------------------------------- the edge lines

	@Test
	public void executionerReadsHealthBeforeTheBlow(){
		Longsword sword = new Longsword();
		Quality q = only( sword, Rarity.RARE, 1f, RarityLine.EXECUTIONER );
		Char attacker = brute();
		float v = q.value( RarityLine.EXECUTIONER );
		assertEquals( Math.round( 1000 * (1f + v) ), Quality.weaponHit( sword, attacker, foe( HERO + 1, 49 ), 1000 ) );
		//exactly half is not under half
		assertEquals( 1000, Quality.weaponHit( sword, attacker, foe( HERO + 2, 50 ), 1000 ) );
		//a blow that takes it under half is not lifted by it: the health read is before the blow
		Char whole = foe( HERO + 3, 60 );
		assertEquals( 100, Quality.weaponHit( sword, attacker, whole, 100 ) );
		assertEquals( 60, whole.HP );
	}

	@Test
	public void defiantOnlyUnderHalf(){
		Longsword sword = new Longsword();
		Quality q = only( sword, Rarity.RARE, 1f, RarityLine.DEFIANT );
		float v = q.value( RarityLine.DEFIANT );
		Char attacker = brute();
		Char target = foe( HERO + 1, 100 );
		attacker.HP = 49;
		assertEquals( Math.round( 1000 * (1f + v) ), Quality.weaponHit( sword, attacker, target, 1000 ) );
		attacker.HP = 50;
		assertEquals( 1000, Quality.weaponHit( sword, attacker, target, 1000 ) );
		attacker.HP = 100;
		assertEquals( 1000, Quality.weaponHit( sword, attacker, target, 1000 ) );
	}

	@Test
	public void moonlitFollowsThePhase(){
		Longsword sword = new Longsword(), plain = new Longsword();
		Quality q = only( sword, Rarity.RARE, 1f, RarityLine.MOONLIT );
		only( plain, Rarity.RARE, 1f );
		float v = q.value( RarityLine.MOONLIT );
		Char attacker = brute();
		Char target = foe( HERO + 1, 100 );
		int dayLen = DayNightCycle.phaseDuration( DayNightCycle.Phase.DAY );

		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		assertEquals( 1f, Quality.moonFactor(), 0f );
		assertEquals( Math.round( 1000 * (1f + v) ), Quality.weaponHit( sword, attacker, target, 1000 ) );
		assertEquals( 1f + v, sword.accuracyFactor( attacker, target ) / plain.accuracyFactor( attacker, target ), 1e-5f );

		for (DayNightCycle.Phase p : new DayNightCycle.Phase[]{ DayNightCycle.Phase.DUSK, DayNightCycle.Phase.DAWN }){
			DayNightCycle.debugPhaseOverride = p;
			assertEquals( 0.5f, Quality.moonFactor(), 0f );
			assertEquals( Math.round( 1000 * (1f + v * 0.5f) ), Quality.weaponHit( sword, attacker, target, 1000 ) );
			assertEquals( 1f + v * 0.5f, sword.accuracyFactor( attacker, target ) / plain.accuracyFactor( attacker, target ), 1e-5f );
		}

		//the middle of the day: a third of it lost
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		Dungeon.cycleTurn = DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN ) + dayLen / 2;
		assertEquals( -1f / 3f, Quality.moonFactor(), 0f );
		assertEquals( Math.round( 1000 * (1f - v / 3f) ), Quality.weaponHit( sword, attacker, target, 1000 ) );
		assertEquals( 1f - v / 3f, sword.accuracyFactor( attacker, target ) / plain.accuracyFactor( attacker, target ), 1e-5f );

		//the morning: nothing
		Dungeon.cycleTurn = DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN ) + dayLen / 10;
		assertEquals( 0f, Quality.moonFactor(), 0f );
		assertEquals( 1000, Quality.weaponHit( sword, attacker, target, 1000 ) );
		assertEquals( 1f, sword.accuracyFactor( attacker, target ) / plain.accuracyFactor( attacker, target ), 1e-5f );
	}

	@Test
	public void stormbornNeedsWeatherAndOpenSky(){
		Longsword sword = new Longsword();
		Quality q = only( sword, Rarity.RARE, 1f, RarityLine.STORMBORN );
		float v = q.value( RarityLine.STORMBORN );
		Char attacker = brute();
		Char target = foe( HERO + 1, 100 );

		ClimateManager.debugPrecipOverride = 0f;
		assertEquals( 1000, Quality.weaponHit( sword, attacker, target, 1000 ) );
		ClimateManager.debugPrecipOverride = 0.8f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.NONE;
		assertEquals( 1000, Quality.weaponHit( sword, attacker, target, 1000 ) );
		for (PrecipType t : new PrecipType[]{ PrecipType.RAIN, PrecipType.SNOW, PrecipType.HAIL }){
			ClimateManager.debugPrecipTypeOverride = t;
			assertEquals( t.toString(), Math.round( 1000 * (1f + v) ), Quality.weaponHit( sword, attacker, target, 1000 ) );
		}

		//out on the world's surface: no roof is open sky, slate and thatch are not, and asking
		//under thatch rolls no leak
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		int[] cells = leanTo();
		Hero hero = Dungeon.hero;
		hero.pos = cells[2];
		assertTrue( Quality.weatherOn( hero ) );
		LineSlotsTest.Counter c = LineSlotsTest.Counter.push( 0x7A7CL );
		try {
			for (int i = 0; i < 200; i++){
				hero.pos = cells[0];
				assertFalse( "slate", Quality.weatherOn( hero ) );
				hero.pos = cells[1];
				assertFalse( "thatch", Quality.weatherOn( hero ) );
			}
		} finally {
			Random.popGenerator();
		}
		assertEquals( 0, c.ints + c.floats );
	}

	//a surface window with a log wall, a slate roof and a thatch roof beside it (a lean-to with no
	//room), and an open cell: { slate, thatch, open }
	static int[] leanTo(){
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		Dungeon.depth = OverworldLevel.DEPTH;
		HomesteadWorld.reset();
		for (int[] at : new int[][]{ { 300, 300 }, { -400, 260 }, { 600, -500 }, { -900, -700 } }){
			PathFinder.setMapSize( w, h );
			float s = WorldModel.calendarShift();
			int ox = at[0] - w / 2, oy = at[1] - h / 2;
			WindowGenerator.Window win = WindowGenerator.generate( 0x5EED0F7EA7L, 0, ox, oy, s );
			OverworldLevel ow = OverworldLevel.forNetwork( 0, 0x5EED0F7EA7L, ox, oy, s, GameCalendar.season(), win.terrain, w, h );
			try {
				Field f = OverworldLevel.class.getDeclaredField( "network" );
				f.setAccessible( true );
				f.set( ow, false );
			} catch (ReflectiveOperationException e){
				throw new IllegalStateException( e );
			}
			Dungeon.level = ow;
			for (int y = h / 2 - 20; y < h / 2 + 20; y++){
				for (int x = w / 2 - 20; x < w / 2 + 20; x++){
					int o = x + y * w;
					if (!open( ow, o, w ) || !open( ow, o + 1, w ) || !open( ow, o + w, w ) || !open( ow, o + w + 1, w )
							|| !open( ow, o + 3, w )) continue;
					Homestead.build( ow, o, BuildCatalog.byId( "wall_log" ), null );
					Homestead.build( ow, o + w, BuildCatalog.byId( "wall_log" ), null );
					Homestead.build( ow, o + 1, BuildCatalog.byId( "roof_slate" ), null );
					Homestead.build( ow, o + w + 1, BuildCatalog.byId( "roof_thatch" ), null );
					assertNotEquals( -1, Homestead.roofAt( ow, o + 1 ) );
					assertNotEquals( -1, Homestead.roofAt( ow, o + w + 1 ) );
					return new int[]{ o + 1, o + w + 1, o + 3 };
				}
			}
		}
		throw new AssertionError( "no open ground in any test window" );
	}

	private static boolean open( OverworldLevel ow, int c, int w ){
		int t = ow.map[c];
		return ow.passable[c] && !ow.pit[c] && (Terrain.flags[t] & Terrain.LIQUID) == 0 && t != Terrain.FROZEN_WATER
				&& t != Terrain.BRIDGE && !Terrain.homestead( t ) && !ow.protectedForBuilding( c )
				&& ow.pristineAt( c ) == t && ow.heaps.get( c ) == null && c % w > 3 && c % w < w - 4;
	}

	@Test
	public void theEdgeIsOneSum(){
		Longsword sword = new Longsword();
		Quality q = only( sword, Rarity.RARE, 1f, RarityLine.SLAYER, RarityLine.EXECUTIONER, RarityLine.MOONLIT,
				RarityLine.STORMBORN, RarityLine.DEFIANT );
		q.slayer = SlayerFamily.BEASTS;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		ClimateManager.debugPrecipOverride = 0.8f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		Char attacker = brute();
		attacker.HP = 10;
		Rat rat = new Rat();
		rat.pos = HERO + 1;
		rat.HT = 100;
		rat.HP = 10;
		rat.sprite = QualitySourceTest.quietSprite();
		float sum = q.value( RarityLine.SLAYER ) + q.value( RarityLine.EXECUTIONER ) + q.value( RarityLine.MOONLIT )
				+ q.value( RarityLine.STORMBORN ) + q.value( RarityLine.DEFIANT );
		assertEquals( sum, Quality.edge( q, attacker, rat ), 1e-5f );
		LineSlotsTest.Counter c = LineSlotsTest.Counter.push( 0xED6EL );
		int dealt;
		try {
			dealt = Quality.weaponHit( sword, attacker, rat, 1000 );
		} finally {
			Random.popGenerator();
		}
		//one multiplication of the summed lines, drawing nothing
		assertEquals( Math.round( 1000 * (1f + sum) ), dealt );
		assertEquals( 0, c.ints + c.floats );

		//another family is not slain the better
		q.slayer = SlayerFamily.UNDEAD;
		assertEquals( sum - q.value( RarityLine.SLAYER ), Quality.edge( q, attacker, rat ), 1e-5f );

		//a crit multiplies the lifted blow
		q.lines.add( RarityLine.CRIT );
		q.rolls.add( 100f );
		Char low = foe( HERO + 2, 10 );
		float noSlayer = sum - q.value( RarityLine.SLAYER );
		assertEquals( Math.round( Math.round( 1000 * (1f + noSlayer) ) * 1.5f ), Quality.weaponHit( sword, attacker, low, 1000 ) );
	}

	// ---------------------------------------------------------------- hunter

	@Test
	public void hunterDrawsOnlyOnABeastKillWithTheLine(){
		Hero hero = Dungeon.hero;
		Longsword sword = new Longsword();
		Quality q = only( sword, Rarity.RARE, 20f, RarityLine.HUNTER );
		hero.belongings.weapon = sword;

		int base = drawsOnKill( new Skeleton(), HERO + 1 );
		assertNull( Dungeon.level.heaps.get( HERO + 1 ) );

		//a beast: one more draw, and meat where it fell
		assertEquals( base + 1, drawsOnKill( new Rat(), HERO + 2 ) );
		assertTrue( meatAt( HERO + 2 ) );

		//the bow's line counts for its arrow
		Bow bow = new Bow();
		only( bow, Rarity.RARE, 20f, RarityLine.HUNTER );
		hero.belongings.weapon = new Longsword();
		only( hero.belongings.weapon, Rarity.RARE, 1f );
		hero.belongings.thrownWeapon = bow.knockArrow();
		assertEquals( base + 1, drawsOnKill( new Rat(), HERO + 3 ) );
		assertTrue( meatAt( HERO + 3 ) );
		hero.belongings.thrownWeapon = null;

		//no line: no draw, no meat
		hero.belongings.weapon = sword;
		q.lines.clear();
		q.rolls.clear();
		assertEquals( base, drawsOnKill( new Rat(), HERO + 4 ) );
		assertNull( Dungeon.level.heaps.get( HERO + 4 ) );
	}

	private static int drawsOnKill( Mob mob, int pos ){
		mob.pos = pos;
		LineSlotsTest.Counter c = LineSlotsTest.Counter.push( 0x4D7EL );
		try {
			Quality.onEnemyKilled( mob, Dungeon.hero );
		} finally {
			Random.popGenerator();
		}
		return c.ints + c.floats;
	}

	private static boolean meatAt( int cell ){
		Heap h = Dungeon.level.heaps.get( cell );
		return h != null && h.peek() instanceof MysteryMeat;
	}

	// ---------------------------------------------------------------- steady

	@Test
	public void steadyUndoesTheWindNotTheTailwind(){
		Hero hero = Dungeon.hero;
		hero.adjustAttackSkill( 100000 );
		Dart steady = new Dart(), plain = new Dart();
		Quality q = only( steady, Rarity.RARE, 1f, RarityLine.STEADY );
		only( plain, Rarity.RARE, 1f );
		float s = q.value( RarityLine.STEADY );
		Char target = dummy( HERO + 4, Char.Alignment.ENEMY, 100 );

		//windswept, with the wind across the throw: the line undoes its share of the 40% lost
		setClimateField( "localWindSpeed", 0f );
		ClimateManager.debugWindOverride = 25f;
		Buff.affect( hero, Windswept.class );
		float windswept = new Windswept().rangedAccuracyFactor();
		assertEquals( 0.6f, windswept, 1e-6f );
		assertEquals( (1f - (1f - windswept) * (1f - s)) / windswept, ratio( hero, steady, plain, target ), 1e-3f );
		assertTrue( "windswept: the line's chip", steadied( hero, steady, target ) );
		assertFalse( "no line, no chip", steadied( hero, plain, target ) );
		hero.buff( Windswept.class ).detach();
		ClimateManager.debugWindOverride = Float.NaN;

		//a headwind (the throw east, the wind blowing west): its share undone
		setClimateField( "localWindSpeed", 25f );
		setClimateField( "surfaceWindDir", 270f );
		float head = ClimateManager.windProjectileFactor( hero.pos, target.pos, false );
		assertTrue( head < 1f );
		assertEquals( (1f - (1f - head) * (1f - s)) / head, ratio( hero, steady, plain, target ), 1e-3f );
		assertTrue( "a headwind: the line's chip", steadied( hero, steady, target ) );

		//a tailwind helps both alike
		setClimateField( "surfaceWindDir", 90f );
		assertTrue( ClimateManager.windProjectileFactor( hero.pos, target.pos, false ) > 1f );
		assertEquals( 1f, ratio( hero, steady, plain, target ), 1e-4f );
		assertFalse( "a tailwind took nothing: no chip", steadied( hero, steady, target ) );

		//a full masterwork cancels the wind outright
		q.masterwork = Quality.MASTERWORK_MAX;
		setClimateField( "surfaceWindDir", 270f );
		assertEquals( 1f / head, ratio( hero, steady, plain, target ), 1e-3f );
	}

	private static boolean steadied( Hero hero, Dart dart, Char target ){
		hero.belongings.thrownWeapon = dart;
		boolean s = Quality.steadied( hero, target );
		hero.belongings.thrownWeapon = null;
		return s;
	}

	private static float ratio( Hero hero, Dart steady, Dart plain, Char target ){
		hero.belongings.thrownWeapon = steady;
		float a = hero.attackSkill( target );
		hero.belongings.thrownWeapon = plain;
		float b = hero.attackSkill( target );
		hero.belongings.thrownWeapon = null;
		return a / b;
	}
}
