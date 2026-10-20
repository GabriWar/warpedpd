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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.LG.LG;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.draws;

/**
 * The new pieces' looks (PieceFx, SenseFx), headless (EnchantFxTest's stage): every entry draws
 * nothing of the game's Random, with the stand-in layers and without, and with its work posted
 * from another thread; each keeps to its cap (quads, one light, one decal); an Ambush blade marks
 * at most six foes, pings once a sense and never for a foe in view; every hand-off line, run in the
 * game's own code under a seed, leaves the game's Random and the game's results as they are without
 * the pictures; and a guest is sent and plays every piece.
 */
public class PieceFxTest {

	private static boolean standIn;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
		//an item's action cancels the scene's cell selector first: headless a bare one stands in
		//(as WeaponPerkTest's)
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		if (sel.get( null ) == null){
			sel.set( null, bare( CellSelector.class ) );
			standIn = true;
		}
	}

	@AfterClass
	public static void unboot() throws Exception {
		if (!standIn) return;
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		sel.set( null, null );
	}

	private EnchantFxTest.Rig rig;
	private EnchantFxTest.Ear ear;
	private String savedVersion;

	@Before
	public void setUp(){
		//the pictures' own dice, the same every run: no assertion on them can come out by chance
		FxRandom.seedForTests( 0x51ECL );
		rig = new EnchantFxTest.Rig();
		rig.up();
		ear = new EnchantFxTest.Ear();
		Sting.out = ear;
		savedVersion = Game.version;
		Game.version = "test";
		NetVisuals.clear();
	}

	@After
	public void tearDown() throws Exception {
		TestParty.leave();
		NetVisuals.setSpectatorMobs( null );
		NetVisuals.clear();
		GameScene.supplyNetTarget( null );
		Actor.clear();
		Game.version = savedVersion;
		rig.down();
		if (Dungeon.level != null) PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
	}

	// ------------------------------------------------------------------ the world

	@SuppressWarnings("unchecked")
	private static <T> T bare( Class<T> c ){
		try {
			Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
			u.setAccessible( true );
			Object unsafe = u.get( null );
			return (T) unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, c );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	//a sprite on the stage for a character, shown where it stands: a plain one that takes
	//particles without a scene (a mob's sprite would read its character as a mob)
	private CharSprite sprite( Char ch ){
		CharSprite s = new CharSprite(){
			@Override public Emitter emitter(){ return new Emitter(); }
			@Override public Emitter centerEmitter(){ return new Emitter(); }
			@Override public void operate( int cell ){}
		};
		s.ch = ch;
		ch.sprite = s;
		rig.stage.add( s );
		s.place( ch.pos );
		s.visible = true;
		return s;
	}

	//a character that only keeps count of its health, its sprite on the stage, among the actors
	private Char dummy( int cell, Char.Alignment alignment, int hp ){
		Char ch = new Char(){
			@Override public void damage( int amount, Object source ){ HP -= amount; }
		};
		ch.pos = cell;
		ch.HP = ch.HT = hp;
		ch.alignment = alignment;
		sprite( ch );
		Actor.add( ch );
		return ch;
	}

	//the hero, his sprite on the stage, the thrower an action would set
	private Hero hero( int cell ){
		Hero h = new Hero();
		h.pos = cell;
		h.HT = 100;
		h.HP = 60;
		sprite( h );
		Dungeon.hero = h;
		Actor.add( h );
		setField( null, Item.class, "curUser", h );
		return h;
	}

	//a weapon's quality of a type with these lines, every one at its best roll, no enchantment
	private static Quality typed( Item item, ItemType type, RarityLine... lines ){
		Quality.roll( item, Rarity.RARE, type, 0 );
		Quality q = item.quality;
		q.type = type;
		q.lines.clear();
		q.rolls.clear();
		for (RarityLine l : lines){
			q.lines.add( l );
			q.rolls.add( 1f );
		}
		if (item instanceof Weapon) ((Weapon) item).enchant( null );
		return q;
	}

	private static Object call( Object on, Class<?> owner, String name, Class<?>[] types, Object... args ){
		try {
			Method m = owner.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( on, args );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private static void setField( Object on, Class<?> owner, String name, Object value ){
		try {
			Field f = owner.getDeclaredField( name );
			f.setAccessible( true );
			f.set( on, value );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private static List<Char> list( Char... chars ){
		return new ArrayList<>( Arrays.asList( chars ) );
	}

	// ------------------------------------------------------------------ every entry, as the game calls it

	//every PieceFx entry and SenseFx's restaging, as the game calls them, on the stage's rats
	private void everyEntry( CharSprite a, CharSprite d, CharSprite n1, CharSprite n2, Item javelin, Weapon mini, Weapon ft,
			Ballistica aim ){
		PieceFx.crit( a.ch, d.ch );
		PieceFx.lifesteal( a.ch, d.ch );
		PieceFx.freeReload( a.ch );
		PieceFx.sparedRound( mini, a.ch );
		PieceFx.sparedRound( ft, a.ch );
		PieceFx.heaveCharge( a.ch, javelin );
		PieceFx.heaveRelease( a.ch, javelin, d.ch.pos );
		PieceFx.heaveHit( a.ch, d.ch, javelin );
		PieceFx.shockwave( a.ch, d.ch, javelin, list( n1.ch, n2.ch ) );
		PieceFx.sweep( a.ch, d.ch, javelin, list( n1.ch, n2.ch ) );
		PieceFx.secondSeed( a.ch, at( 3, 3 ) );
		PieceFx.keenBack( a.ch, d.ch.pos, javelin );
		PieceFx.caught( a.ch );
		PieceFx.bite( a.ch, d.ch, 0, javelin );
		PieceFx.bite( a.ch, n1.ch, 6, javelin );
		PieceFx.telegraph( d.ch.pos, a.ch.pos, a.ch, javelin );
		PieceFx.reach( a.ch, ft, aim, 2, 4 );
		SenseFx.stage( a, new CharSprite[]{ d, n1, n2 }, 0.5f );
	}

	@Test
	public void everyEntryDrawsNothingOfTheGamesRandom() throws Exception {
		//a thrown weapon's enchantment is kept by the hero's set tracker
		Dungeon.hero = new Hero();
		final Javelin javelin = new Javelin();
		javelin.enchant( new Blazing() );
		final MiniGun mini = new MiniGun();
		final FT ft = new FT();
		for (boolean layers : new boolean[]{ true, false }){
			rig.layers( layers );
			final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 7, 6 ) ), n1 = rig.rat( at( 7, 5 ) ), n2 = rig.rat( at( 8, 7 ) );
			final Ballistica aim = new Ballistica( a.ch.pos, at( 13, 6 ), Ballistica.WONT_STOP );
			assertEquals( layers ? "with the layers" : "without", 0, draws( () -> {
				everyEntry( a, d, n1, n2, javelin, mini, ft, aim );
				rig.frames( 60, true );
			} ) );
		}

		//posted from another thread (the actor's): nothing drawn there, nothing of the Random when
		//the work runs on the render thread
		rig.layers( true );
		final CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 7, 6 ) ), n1 = rig.rat( at( 7, 5 ) ), n2 = rig.rat( at( 8, 7 ) );
		final Ballistica aim = new Ballistica( a.ch.pos, at( 13, 6 ), Ballistica.WONT_STOP );
		final List<Runnable> posted = new ArrayList<>();
		Application saved = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				(p, m, args) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) args[0] );
						}
						return null;
					}
					return saved != null ? m.invoke( saved, args ) : null;
				} );
		try {
			IdentityHashMap<Gizmo, Boolean> before = new IdentityHashMap<>(), after = new IdentityHashMap<>();
			rig.alive( before );
			final int[] drawn = new int[1];
			drawn[0] = draws( () -> {
				Thread actor = new Thread( () -> {
					PieceFx.crit( a.ch, d.ch );
					PieceFx.lifesteal( a.ch, d.ch );
					PieceFx.freeReload( a.ch );
					PieceFx.sparedRound( mini, a.ch );
					PieceFx.heaveCharge( a.ch, javelin );
					PieceFx.heaveRelease( a.ch, javelin, d.ch.pos );
					PieceFx.heaveHit( a.ch, d.ch, javelin );
					PieceFx.shockwave( a.ch, d.ch, javelin, list( n1.ch, n2.ch ) );
					PieceFx.sweep( a.ch, d.ch, javelin, list( n1.ch, n2.ch ) );
					PieceFx.secondSeed( a.ch, at( 3, 3 ) );
					PieceFx.keenBack( a.ch, d.ch.pos, javelin );
					PieceFx.caught( a.ch );
					PieceFx.bite( a.ch, d.ch, 0, javelin );
					PieceFx.telegraph( d.ch.pos, a.ch.pos, a.ch, javelin );
					PieceFx.reach( a.ch, ft, aim, 2, 4 );
				} );
				actor.start();
				try {
					actor.join();
				} catch (InterruptedException e){
					throw new AssertionError( e );
				}
			} );
			assertEquals( "nothing of the Random off the render thread", 0, drawn[0] );
			rig.alive( after );
			assertEquals( "nothing drawn off the render thread", before.size(), after.size() );
			assertEquals( "every one posted", 15, posted.size() );
			assertEquals( "nothing of the Random as the posted work runs", 0, draws( () -> {
				for (Runnable r : posted) r.run();
				rig.frames( 60, true );
			} ) );
		} finally {
			Gdx.app = saved;
		}
	}

	// ------------------------------------------------------------------ caps

	//what a piece makes over two seconds: within its quads, one light, one decal; and something
	private void capped( String name, int cap, Runnable piece ){
		Game.timeTotal += 3f;
		int[] m = rig.made( piece, 60 );
		assertTrue( name + " made " + m[0] + " quads, cap " + cap, m[0] > 0 && m[0] <= cap );
		assertTrue( name + " lights " + m[1], m[1] <= 1 );
		assertTrue( name + " decals " + m[2], m[2] <= 1 );
	}

	@Test
	public void everyPieceKeepsToItsCap(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		CharSprite[] three = { rig.rat( at( 6, 5 ) ), rig.rat( at( 6, 7 ) ), rig.rat( at( 7, 6 ) ) };
		CharSprite[] eight = new CharSprite[8];
		for (int i = 0; i < 8; i++) eight[i] = rig.rat( at( 1 + i, 12 ) );
		EnchantLook blazing = EnchantLooks.of( Blazing.class );

		capped( "crit", PieceFx.CRIT_CAP, () -> PieceFx.runCrit( a, d, true, false, false ) );
		d.blowLook = blazing;
		d.blowUntil = Game.timeTotal + 3.1f;
		capped( "a crit on a stamped blow", PieceFx.CRIT_CAP, () -> PieceFx.runCrit( a, d, true, false, false ) );
		capped( "lifesteal", PieceFx.LIFESTEAL_CAP, () -> PieceFx.runLifesteal( a, d, false ) );
		capped( "free reload", PieceFx.RELOAD_CAP, () -> PieceFx.runFreeReload( a, false ) );
		capped( "spared round", PieceFx.SPARE_CAP, () -> PieceFx.runSpare( a, ItemSpriteSheet.MINI_GUN_AMMO, false ) );
		capped( "spared flame", PieceFx.SPARE_CAP, () -> PieceFx.runSpare( a, -1, false ) );
		//the charge, and its armed mote
		capped( "heave charge", PieceFx.CHARGE_CAP + 1, () -> PieceFx.runCharge( a, null, null, false ) );
		//the release, and castBloom's four motes
		capped( "heave release", PieceFx.RELEASE_CAP + 4, () -> PieceFx.runRelease( a, ItemSpriteSheet.JAVELIN, d.ch.pos, blazing, false ) );
		capped( "heave hit", PieceFx.HEAVE_CAP, () -> PieceFx.runHeave( a, d, blazing, true, false, false ) );
		capped( "shockwave", PieceFx.SHOCK_CAP + PieceFx.SHOCK_PER * 3, () -> PieceFx.runShockwave( a, d, three.clone(), null, true, false, false ) );
		capped( "shockwave of eight", PieceFx.SHOCK_CAP + PieceFx.SHOCK_PER * 8, () -> PieceFx.runShockwave( a, d, eight.clone(), null, true, false, false ) );
		capped( "sweep", PieceFx.SWEEP_CAP + PieceFx.SWEEP_PER * 3, () -> PieceFx.runSweep( a, d, three.clone(), blazing, true, false ) );
		capped( "second seed", PieceFx.SEED_CAP, () -> PieceFx.runSeed( null, at( 3, 3 ), true, false ) );
		capped( "keen throw home", PieceFx.KEEN_CAP, () -> PieceFx.runKeenBack( a, at( 11, 6 ), ItemSpriteSheet.JAVELIN, null, false ) );
		capped( "catch", PieceFx.CATCH_CAP, () -> PieceFx.runCatch( a, false ) );
		capped( "a rich bite", PieceFx.BITE_CAP, () -> PieceFx.runBite( a, d, 0, null, true, false ) );
		capped( "a later bite", PieceFx.LATER_BITE, () -> PieceFx.runBite( a, d, 5, null, true, false ) );
		capped( "telegraph", PieceFx.TELEGRAPH_CAP, () -> PieceFx.runTelegraph( at( 13, 6 ), a.ch.pos, a, null, false ) );
		capped( "reach", PieceFx.REACH_CAP, () -> PieceFx.runReach( a, at( 9, 2 ), PieceFx.FLAME_TIP, true, false ) );
		//a sense of eight: one ping, six rings, six exclamations
		capped( "sense", 1 + 2 * SenseFx.MOST, () -> SenseFx.stage( a, eight, 1f ) );
	}

	@Test
	public void piecesRingAsTheyShould(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		for (int i = 0; i < 4; i++) PieceFx.runBite( a, d, i, null, true, false );
		PieceFx.runLifesteal( a, d, false );
		rig.frames( 30, true );
		assertEquals( "a bite each, no sting for a lifesteal: " + ear.heard.size(), 4, ear.heard.size() );
		float last = 0f;
		for (Object[] h : ear.heard){
			float pitch = (Float) h[3];
			assertTrue( "each bite rings higher", pitch > last );
			last = pitch;
		}
	}

	// ------------------------------------------------------------------ sensing

	//the pings so far: the rings spread from the hero in the marks' group (no frame runs between)
	private static int pings(){
		Group g = SenseFx.group();
		int n = 0;
		if (g != null) for (Gizmo m : g.membersView()) if (m instanceof FxRing && m.alive) n++;
		return n;
	}

	//the hero looks round (Level.updateFieldOfView) and, a scan later, his blade's senses are read
	private static void look( Hero h ){
		Dungeon.level.updateFieldOfView( h, Dungeon.level.heroFOV );
		Game.timeTotal += SenseFx.SCAN + 0.01f;
		SenseFx.step( h.sprite );
	}

	private Mob mob( int cell ){
		CharSprite s = rig.rat( cell );
		Mob m = (Mob) s.ch;
		m.alignment = Char.Alignment.ENEMY;
		Dungeon.level.mobs.add( m );
		return m;
	}

	private static void wall( boolean up ){
		Level l = Dungeon.level;
		for (int y = 1; y < EnchantFxTest.W - 1; y++) l.map[at( 7, y )] = up ? Terrain.WALL : Terrain.EMPTY;
		l.buildFlagMaps();
	}

	@Test
	public void anAmbushBladeMarksAtMostSixAndPingsOnceASenseAndNeverWhatIsInView(){
		wall( true );
		Hero h = hero( at( 5, 6 ) );
		Dagger dagger = new Dagger();
		typed( dagger, ItemType.ALPHA );
		h.belongings.weapon = dagger;
		assertEquals( 3, Quality.senseRange( h ) );

		Mob inView = mob( at( 5, 9 ) ), near = mob( at( 8, 6 ) ), far = mob( at( 13, 2 ) );
		look( h );
		assertTrue( "behind the wall and in reach", SenseFx.marked( near.sprite ) );
		assertFalse( "in plain view", SenseFx.marked( inView.sprite ) );
		assertFalse( "out of the blade's reach", SenseFx.marked( far.sprite ) );
		assertEquals( 1, SenseFx.marks() );
		assertEquals( "one ping", 1, pings() );
		look( h );
		look( h );
		assertEquals( "one ping a sense, not a scan", 1, pings() );

		//more behind the wall than it marks
		for (int y = 3; y <= 9; y++) if (y != 6) mob( at( 8, y ) );
		look( h );
		assertEquals( SenseFx.MOST, SenseFx.marks() );
		assertEquals( "no ping for more of the same sense", 1, pings() );

		//the wall gone: all of them in view, the marks let go, and no ping
		wall( false );
		look( h );
		Game.timeTotal += SenseFx.GRACE;
		look( h );
		assertEquals( 0, SenseFx.marks() );
		assertEquals( 1, pings() );

		//a new sense: a ping again
		wall( true );
		look( h );
		assertEquals( SenseFx.MOST, SenseFx.marks() );
		assertEquals( 2, pings() );

		//the blade put away: nothing sensed
		h.belongings.weapon = new Sword();
		look( h );
		Game.timeTotal += SenseFx.GRACE;
		look( h );
		assertEquals( 0, SenseFx.marks() );
	}

	@Test
	public void aGuestsHeroSensesNothingAndAnotherSpriteStepsNothing() throws Exception {
		wall( true );
		Hero h = hero( at( 5, 6 ) );
		Dagger dagger = new Dagger();
		typed( dagger, ItemType.ALPHA );
		h.belongings.weapon = dagger;
		Mob near = mob( at( 8, 6 ) );
		Dungeon.level.updateFieldOfView( h, Dungeon.level.heroFOV );
		Game.timeTotal += 1f;
		SenseFx.step( near.sprite );
		assertEquals( "only the hero's own sprite steps it", 0, SenseFx.marks() );
		TestParty.guest();
		SenseFx.step( h.sprite );
		assertEquals( "a guest's mind vision is the host's", 0, SenseFx.marks() );
	}

	// ------------------------------------------------------------------ the hand-off lines

	//a play of the game's own code under a fixed seed: what it did, read after two seconds of frames
	private interface Play {
		Supplier<String> play();
	}

	/**
	 * The same play with the pictures (the stand-in layers) and without: the same results and the
	 * same draws of the game's Random after, two seconds of frames included; and with the layers,
	 * the line's pictures drew.
	 */
	private void same( String name, Play play ){
		String[] out = new String[2];
		int[] pictures = new int[2];
		for (int k = 0; k < 2; k++){
			boolean layers = k == 0;
			Actor.clear();
			rig.layers( layers );
			rig.stage = new Group();
			Dungeon.level = EnchantFxTest.room();
			Dungeon.hero = null;
			Game.timeTotal = 100f;
			Random.pushGenerator( 0x9EC3L );
			try {
				Supplier<String> state = play.play();
				IdentityHashMap<Gizmo, Boolean> seen = new IdentityHashMap<>();
				for (int f = 0; f < 60; f++){
					rig.frames( 1, true );
					if (rig.layers != null) for (Group g : rig.layers) collect( g, seen );
				}
				pictures[k] = seen.size();
				out[k] = state.get() + " | next " + Random.Long();
			} finally {
				Random.popGenerator();
			}
		}
		assertEquals( name + ": the game with its pictures as without", out[1], out[0] );
		assertTrue( name + ": its pictures drew", pictures[0] > 0 );
	}

	private static void collect( Group g, IdentityHashMap<Gizmo, Boolean> into ){
		for (Gizmo m : g.membersView()){
			if (m == null || !m.alive) continue;
			if (m instanceof Group) collect( (Group) m, into );
			else into.put( m, Boolean.TRUE );
		}
	}

	@Test
	public void aCritAndALifestealLeaveTheGameAsItWas(){
		same( "crit", () -> {
			Hero h = hero( at( 5, 6 ) );
			Sword sword = new Sword();
			typed( sword, ItemType.GAMMA, RarityLine.CRIT );
			h.belongings.weapon = sword;
			Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 1000000 );
			int total = 0;
			for (int i = 0; i < 60; i++) total += Quality.weaponHit( sword, h, foe, 100 );
			assertTrue( "some crits", total > 6000 );
			final int t = total;
			return () -> "damage " + t + " foe " + foe.HP;
		} );
		same( "lifesteal", () -> {
			Hero h = hero( at( 5, 6 ) );
			h.HP = 10;
			Sword sword = new Sword();
			typed( sword, ItemType.GAMMA, RarityLine.LIFESTEAL );
			h.belongings.weapon = sword;
			Char foe = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 1000000 );
			for (int i = 0; i < 60; i++) Quality.weaponHit( sword, h, foe, 30 );
			assertTrue( "some heals", h.HP > 10 );
			return () -> "hero " + h.HP;
		} );
	}

	@Test
	public void aFreeReloadAndASparedRoundLeaveTheGameAsItWas(){
		same( "free reload", () -> {
			Hero h = hero( at( 5, 6 ) );
			FT ft = new FT();
			typed( ft, ItemType.BETA );
			int free = 0;
			for (int i = 0; i < 40; i++) if (Quality.rollFreeReload( ft, h )) free++;
			assertTrue( free > 0 );
			final int n = free;
			return () -> "free " + n;
		} );
		same( "spared round", () -> {
			Hero h = hero( at( 5, 6 ) );
			MiniGun mini = new MiniGun();
			typed( mini, ItemType.ALPHA );
			int spared = 0;
			for (int i = 0; i < 40; i++) if (Quality.rollSpareAmmo( mini, h )) spared++;
			assertTrue( spared > 0 );
			final int n = spared;
			return () -> "spared " + n;
		} );
	}

	@Test
	public void aHeaveFromItsActionToItsShockwaveLeavesTheGameAsItWas(){
		same( "heave", () -> {
			Hero h = hero( at( 5, 6 ) );
			final int[] thrown = { -1 };
			Javelin stack = new Javelin(){
				@Override public void cast( Hero user, int dst ){ thrown[0] = dst; }
			};
			typed( stack, ItemType.ALPHA );
			stack.enchant( new Blazing() );
			stack.identify( false );
			stack.quantity( 3 );
			assertTrue( stack.collect( h.belongings.backpack ) );
			Char target = dummy( at( 8, 6 ), Char.Alignment.ENEMY, 100000 );
			Char foe = dummy( at( 8, 5 ), Char.Alignment.ENEMY, 1000 ), foe2 = dummy( at( 9, 7 ), Char.Alignment.ENEMY, 1000 );
			Char friend = dummy( at( 8, 7 ), Char.Alignment.ALLY, 1000 );
			//armed and let go, through the action (the cell as a co-op action would bring it)
			GameScene.supplyNetTarget( target.pos );
			stack.execute( h, MissileWeapon.AC_HEAVE );
			assertEquals( target.pos, thrown[0] );
			WeaponPerk.Heave heave = h.buff( WeaponPerk.Heave.class );
			assertNotNull( heave );
			//and landing: the hit, the shockwave
			heave.cell = target.pos;
			int dmg = Quality.weaponHit( stack, h, target, 100 );
			assertEquals( 250, dmg );
			assertNull( h.buff( WeaponPerk.Heave.class ) );
			return () -> "dmg " + dmg + " foes " + foe.HP + "/" + foe2.HP + " friend " + friend.HP;
		} );
	}

	@Test
	public void aSweepAndASecondSeedLeaveTheGameAsItWas(){
		same( "sweep", () -> {
			Hero h = hero( at( 5, 6 ) );
			Sickle sickle = new Sickle();
			typed( sickle, ItemType.ALPHA );
			h.belongings.weapon = sickle;
			Level.set( at( 4, 5 ), Terrain.HIGH_GRASS, Dungeon.level );
			Char target = dummy( at( 6, 6 ), Char.Alignment.ENEMY, 100000 );
			Char up = dummy( at( 5, 5 ), Char.Alignment.ENEMY, 1000 ), grass = dummy( at( 4, 5 ), Char.Alignment.ENEMY, 1000 );
			Char down = dummy( at( 6, 7 ), Char.Alignment.ENEMY, 1000 );
			int dmg = Quality.weaponHit( sickle, h, target, 100 );
			assertTrue( up.HP < 1000 && grass.HP < 1000 && down.HP < 1000 );
			return () -> "dmg " + dmg + " swept " + up.HP + "/" + grass.HP + "/" + down.HP + " bleeding "
					+ (up.buff( Bleeding.class ) != null);
		} );
		same( "second seed", () -> {
			Hero h = hero( at( 5, 6 ) );
			Sickle sickle = new Sickle();
			typed( sickle, ItemType.ALPHA );
			h.belongings.weapon = sickle;
			int seeds = 0;
			for (int i = 0; i < 30; i++) if (Quality.secondSeed( h, at( 6, 6 ) )) seeds++;
			assertTrue( seeds > 0 );
			final int n = seeds;
			return () -> "seeds " + n;
		} );
	}

	@Test
	public void aKeenThrowsReturnLeavesTheGameAsItWas(){
		same( "keen return", () -> {
			Hero h = hero( at( 5, 6 ) );
			ThrowingKnife stack = new ThrowingKnife();
			typed( stack, ItemType.GAMMA );
			stack.identify( false );
			int quantity = stack.quantity();
			assertTrue( stack.collect( h.belongings.backpack ) );
			MissileWeapon thrown = (MissileWeapon) stack.detach( h.belongings.backpack );
			Char target = dummy( at( 8, 6 ), Char.Alignment.ENEMY, 1000 );
			Quality.of( thrown ).returnNext = true;
			call( thrown, MissileWeapon.class, "rangedHit", new Class<?>[]{ Char.class, int.class }, target, target.pos );
			return () -> "stack " + stack.quantity() + "/" + quantity + " durability " + stack.durabilityLeft()
					+ " heaps " + Dungeon.level.heaps.valueList().size();
		} );
	}

	@Test
	public void aBoomerangsBitesReturnAndCatchLeaveTheGameAsItWas(){
		same( "boomerang", () -> {
			Hero h = hero( at( 5, 6 ) );
			HeavyBoomerang stack = new HeavyBoomerang();
			typed( stack, ItemType.BETA );
			stack.identify( false );
			assertTrue( stack.collect( h.belongings.backpack ) );
			Char[] line = new Char[3];
			for (int i = 0; i < 3; i++) line[i] = dummy( at( 6 + i, 6 ), Char.Alignment.ENEMY, 100000 );
			int aim = at( 13, 6 );
			int landing = stack.throwPos( h, aim );
			MissileWeapon thrown = (MissileWeapon) stack.detach( h.belongings.backpack );
			setField( null, MissileWeapon.class, "arcAim", aim );
			call( thrown, Item.class, "onThrow", new Class<?>[]{ int.class }, landing );
			HeavyBoomerang.CircleBack back = h.buff( HeavyBoomerang.CircleBack.class );
			assertNotNull( "it circles back", back );
			//its turns run out: it flies home (the telegraph), strikes on the way, and is caught
			for (int i = 0; i < 6 && h.buff( HeavyBoomerang.CircleBack.class ) != null; i++) back.act();
			return () -> "line " + line[0].HP + "/" + line[1].HP + "/" + line[2].HP + " landing " + landing
					+ " back " + (h.buff( HeavyBoomerang.CircleBack.class ) != null) + " in pack "
					+ h.belongings.backpack.contains( thrown );
		} );
	}

	// ------------------------------------------------------------------ guests

	//the "eh" events of a drained batch, as (kind, numbers)
	private static List<int[]> events( JSONArray a ) throws Exception {
		List<int[]> out = new ArrayList<>();
		if (a == null) return out;
		for (int i = 0; i < a.length(); i++){
			JSONObject e = a.getJSONObject( i );
			if (!"eh".equals( e.getString( "t" ) )) continue;
			JSONArray n = e.getJSONArray( "i" );
			int[] row = new int[n.length() + 1];
			row[0] = e.getInt( "k" );
			for (int k = 0; k < n.length(); k++) row[k + 1] = n.getInt( k );
			out.add( row );
		}
		return out;
	}

	@Test
	public void everyPieceGoesOutAndPlaysOnTheGuest() throws Exception {
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), n = rig.rat( at( 6, 5 ) );
		Dungeon.hero = new Hero();
		Javelin javelin = new Javelin();
		javelin.enchant( new Blazing() );
		EnchantLook blazing = EnchantLooks.of( Blazing.class );
		MiniGun mini = new MiniGun();
		LG lg = new LG();
		Ballistica aim = new Ballistica( a.ch.pos, at( 13, 6 ), Ballistica.WONT_STOP );
		TestParty.join( new Hero() );

		PieceFx.crit( a.ch, d.ch );
		PieceFx.lifesteal( a.ch, d.ch );
		PieceFx.freeReload( a.ch );
		PieceFx.sparedRound( mini, a.ch );
		PieceFx.heaveCharge( a.ch, javelin );
		PieceFx.heaveRelease( a.ch, javelin, d.ch.pos );
		PieceFx.heaveHit( a.ch, d.ch, javelin );
		PieceFx.shockwave( a.ch, d.ch, javelin, list( n.ch ) );
		PieceFx.sweep( a.ch, d.ch, javelin, list( n.ch ) );
		PieceFx.secondSeed( a.ch, at( 3, 3 ) );
		PieceFx.keenBack( a.ch, d.ch.pos, javelin );
		PieceFx.caught( a.ch );
		PieceFx.bite( a.ch, d.ch, 2, javelin );
		PieceFx.telegraph( d.ch.pos, a.ch.pos, a.ch, javelin );
		PieceFx.reach( a.ch, lg, aim, 2, 4 );
		rig.frames( 2, false );
		JSONArray sent = NetVisuals.drainEvents();
		List<int[]> ev = events( sent );
		int aid = a.ch.id(), did = d.ch.id(), nid = n.ch.id(), code = blazing.code;
		int[][] expected = {
				{ PieceFx.CRIT, aid, did },
				{ PieceFx.LIFESTEAL, aid, did },
				{ PieceFx.RELOAD, aid },
				{ PieceFx.SPARE, aid, ItemSpriteSheet.MINI_GUN_AMMO },
				{ PieceFx.CHARGE, aid, code },
				{ PieceFx.RELEASE, aid, javelin.image(), d.ch.pos, code },
				{ PieceFx.HEAVE, aid, did, code },
				{ PieceFx.SHOCKWAVE, aid, did, code, nid },
				{ PieceFx.SWEEP, aid, did, code, nid },
				{ PieceFx.SEED, at( 3, 3 ) },
				{ PieceFx.KEEN_BACK, aid, d.ch.pos, javelin.image(), code },
				{ PieceFx.CATCH, aid },
				{ PieceFx.BITE, aid, did, 2, code },
				{ PieceFx.TELEGRAPH, aid, d.ch.pos, code, a.ch.pos },
				{ PieceFx.REACH, aid, at( 9, 6 ), PieceFx.BEAM_TIP } };
		assertEquals( expected.length, ev.size() );
		for (int i = 0; i < expected.length; i++){
			int[] row = ev.get( i );
			assertEquals( EnchantNet.PIECE, row[0] );
			assertEquals( "piece " + i, Arrays.toString( expected[i] ), Arrays.toString( Arrays.copyOfRange( row, 1, row.length ) ) );
		}

		//the guest: his own characters standing for the host's
		TestParty.guest();
		rig.frames( 90, true );
		Game.timeTotal += 3f;
		CharSprite ga = rig.rat( at( 3, 10 ) ), gd = rig.rat( at( 4, 10 ) ), gn = rig.rat( at( 4, 11 ) );
		Map<Integer, Mob> mobs = new HashMap<>();
		mobs.put( aid, (Mob) ga.ch );
		mobs.put( did, (Mob) gd.ch );
		mobs.put( nid, (Mob) gn.ch );
		NetVisuals.setSpectatorMobs( mobs );
		ear.heard.clear();
		final JSONArray batch = sent;
		int[] made = new int[1];
		assertEquals( "a guest's pieces draw nothing of the game's Random", 0, draws( () -> {
			made[0] = rig.made( () -> NetVisuals.replay( batch ), 60 )[0];
		} ) );
		assertTrue( "the guest drew them: " + made[0], made[0] > 30 );
		assertFalse( "and heard their stings", ear.heard.isEmpty() );
		assertNull( "nothing sent on", NetVisuals.drainEvents() );

		//short, unknown or of a character the guest does not have: nothing
		Game.timeTotal += 3f;
		assertEquals( 0, rig.made( () -> {
			EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.CRIT }, null, id -> gd );
			EnchantNet.play( EnchantNet.PIECE, new int[]{ 99, aid, did }, null, id -> gd );
			EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.BITE, aid, did }, null, id -> gd );
			EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.CRIT, aid, did }, null, id -> null );
		}, 30 )[0] );

		//a telegraph from a host that sends no return cell: toward the thrower, as before
		Game.timeTotal += 3f;
		assertTrue( rig.made( () -> EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.TELEGRAPH, aid, at( 9, 10 ), -1 }, null,
				id -> ga ), 30 )[0] > 0 );
	}

	// ------------------------------------------------------------------ what the fixes keep true

	@Test
	public void aSweepStarsEveryFoeItSwept(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		int[][] cells = { { 4, 5 }, { 5, 5 }, { 6, 5 }, { 4, 7 }, { 5, 7 }, { 6, 7 } };
		CharSprite[] six = new CharSprite[cells.length];
		for (int i = 0; i < cells.length; i++) six[i] = rig.rat( at( cells[i][0], cells[i][1] ) );
		//where each foe's star would stand (before any push)
		float[][] mid = new float[six.length][];
		for (int i = 0; i < six.length; i++) mid[i] = new float[]{ six[i].x + six[i].width() / 2f, six[i].y + six[i].height() / 2f };
		Game.timeTotal += 3f;
		PieceFx.runSweep( a, d, six, null, true, false );
		//a particle seen on each foe's middle as the blade goes round (each star lives a few frames)
		boolean[] starred = new boolean[six.length];
		for (int f = 0; f < 30; f++){
			IdentityHashMap<Gizmo, Boolean> now = new IdentityHashMap<>();
			rig.alive( now );
			for (Gizmo g : now.keySet()){
				if (!(g instanceof FxParticle)) continue;
				FxParticle p = (FxParticle) g;
				for (int i = 0; i < six.length; i++){
					if (Math.abs( p.px - mid[i][0] ) < 0.5f && Math.abs( p.py - mid[i][1] ) < 0.5f) starred[i] = true;
				}
			}
			rig.frames( 1, true );
		}
		for (int i = 0; i < six.length; i++) assertTrue( "swept foe " + i + " has its star", starred[i] );
	}

	//the tip a flamethrower's shot from `from` toward `to` marks (its own range 2, 4 with the line), -1 for none
	private int flameTip( int from, int to ) throws Exception {
		CharSprite s = rig.rat( from );
		NetVisuals.drainEvents();
		PieceFx.reach( s.ch, new FT(), new Ballistica( from, to, Ballistica.WONT_STOP ), 2, 4 );
		rig.frames( 1, false );
		for (int[] e : events( NetVisuals.drainEvents() )) if (e[0] == EnchantNet.PIECE && e[1] == PieceFx.REACH) return e[3];
		return -1;
	}

	@Test
	public void aFlamesTipMarksWhereItsConeEnds() throws Exception {
		Dungeon.hero = new Hero();
		TestParty.join( new Hero() );
		Level l = Dungeon.level;
		assertEquals( "open floor: four tiles out", at( 5, 12 ), flameTip( at( 1, 12 ), at( 14, 12 ) ) );
		//the cone (IGNORE_SOFT_SOLID) burns through a closed door and knocks it open
		l.map[at( 3, 14 )] = Terrain.DOOR;
		l.buildFlagMaps();
		assertEquals( "through a closed door", at( 5, 14 ), flameTip( at( 1, 14 ), at( 14, 14 ) ) );
		//a wall two tiles out: the flame never gets past its own range
		l.map[at( 4, 13 )] = Terrain.WALL;
		l.buildFlagMaps();
		assertEquals( "short of a wall", -1, flameTip( at( 1, 13 ), at( 14, 13 ) ) );
		//on a diagonal the cone's four tiles are true distance: three steps, not four
		assertEquals( "a diagonal", at( 4, 4 ), flameTip( at( 1, 1 ), at( 14, 14 ) ) );
	}

	@Test
	public void aHeaveCalledOffOnTheHostTakesTheGuestsArmedMoteToo() throws Exception {
		Hero h = hero( at( 5, 6 ) );
		TestParty.join( new Hero() );
		Buff.affect( h, WeaponPerk.Heave.class, WeaponPerk.Heave.DURATION );
		PieceFx.heaveCharge( h, new Javelin() );
		rig.frames( 10, true );
		assertEquals( "armed", 1, PieceFx.Armed.count() );
		NetVisuals.drainEvents();
		//called off (no cell picked): the buff goes, the mote with it, and the guests are told
		Buff.detach( h, WeaponPerk.Heave.class );
		rig.frames( 10, true );
		assertEquals( 0, PieceFx.Armed.count() );
		List<int[]> ev = events( NetVisuals.drainEvents() );
		assertEquals( 1, ev.size() );
		assertEquals( Arrays.toString( new int[]{ EnchantNet.PIECE, PieceFx.DISARM, h.id() } ), Arrays.toString( ev.get( 0 ) ) );

		//the guest: armed by the host's charge, its mote gone with the DISARM, not 5 s on
		TestParty.guest();
		CharSprite g = rig.rat( at( 3, 10 ) );
		EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.CHARGE, h.id(), -1 }, null, id -> g );
		rig.frames( 10, true );
		assertEquals( 1, PieceFx.Armed.count() );
		EnchantNet.play( EnchantNet.PIECE, new int[]{ PieceFx.DISARM, h.id() }, null, id -> g );
		assertEquals( 0, PieceFx.Armed.count() );
	}

	@Test
	public void aKeenThrowIsCaughtOnlyWhenItReachesThePack() throws Exception {
		Dungeon.hero = new Hero();
		TestParty.join( new Hero() );
		for (boolean full : new boolean[]{ false, true }){
			Hero h = hero( at( 5, 6 ) );
			ThrowingKnife knife = full ? new ThrowingKnife(){
				@Override public boolean collect( Bag container ){ return false; }
			} : new ThrowingKnife();
			typed( knife, ItemType.GAMMA );
			knife.identify( false );
			Char target = dummy( at( 8, 6 ), Char.Alignment.ENEMY, 1000 );
			Quality.of( knife ).returnNext = true;
			NetVisuals.drainEvents();
			call( knife, MissileWeapon.class, "rangedHit", new Class<?>[]{ Char.class, int.class }, target, target.pos );
			rig.frames( 30, true );
			List<Integer> pieces = new ArrayList<>();
			for (int[] e : events( NetVisuals.drainEvents() )) if (e[0] == EnchantNet.PIECE) pieces.add( e[1] );
			assertTrue( "it flies back: " + pieces, pieces.contains( PieceFx.KEEN_BACK ) );
			assertEquals( full ? "a full pack: it falls at his feet, uncaught" : "caught", !full, pieces.contains( PieceFx.CATCH ) );
			assertEquals( "fell at his feet", full, Dungeon.level.heaps.get( h.pos ) != null );
		}
	}
}
