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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FetidRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.AntiEntropy;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Bulk;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Overgrowth;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Stench;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Adaptation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Bounce;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Entanglement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Potential;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Purity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Repulsion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Satisfying;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Dazzling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Explosive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Pressurized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Crystal;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Parasitic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.NeptunusTrident;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The elements' and nature's rows (docs/fx/enchant.md, "Elements and nature"), headless:
 *
 *  - every row's real proc (the enchantment's, glyph's or curse's own code, a buff's tick, the two
 *    lines in Char.damage) struck a couple of dozen times on dummies draws the same of the game's
 *    Random with its pictures as without them (the kit's light layer withheld: no picture of the
 *    enchantments can show; the legacy emitters' layer kept, as a game has it), and the same again
 *    with every post deferred (struck on another thread, the posts run after: the actor thread's
 *    path) - and shows something;
 *  - every row's look, staged at every tier, plain, as an echo and hexed, dry and on water, keeps to
 *    its tier's cap (an echo's, a join's), two lights, and its decals (one, but the rows that leave
 *    marks of their own); the moments of no proc to their own caps;
 *  - the 11 rows a vestige can carry play as an echo from a vestigial weapon (no stamp, a muffled
 *    sting), and the 9 glyphs one can carry from a vestigial armour (a muffled sting);
 *  - the seed's and the surge's states start and end with their buffs;
 *  - a census: every file the rows touched calls the game's Random, and its legacy pictures that
 *    draw it, as many times as it did at 37d849aba.
 */
public class ElementProcsTest {

	static final int W = EnchantFxTest.W;
	/** How many times a fight strikes. */
	static final int STRIKES = 24;
	static final long SEED = 4321;

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private final List<Object[]> heard = new ArrayList<>();
	private Object savedScene;

	@Before
	public void setUp() throws Exception {
		rig = new EnchantFxTest.Rig();
		rig.up();
		Sting.out = ( sample, at, volume, pitch ) -> heard.add( new Object[]{ sample, at, volume, pitch } );
		Actor.clear();
		PathFinder.setMapSize( W, W );
		savedScene = sceneField().get( null );
	}

	@After
	public void tearDown() throws Exception {
		sceneField().set( null, savedScene );
		Actor.clear();
		Dungeon.hero = null;
		rig.down();
		if (Dungeon.level != null) PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
	}

	// ================================================================== the stage

	//where the hero's sprite stands: beside the stage, never updated (a mob's sprite stands in for
	//his, and a mob's sprite's update reads its character as a mob)
	private final Group aside = new Group();

	//the hero
	private Hero hero( int cell ){
		Hero h = new Hero();
		h.HP = h.HT = 500;
		h.pos = cell;
		CharSprite s = new RatSprite();
		s.ch = h;
		h.sprite = s;
		aside.add( s );
		s.place( cell );
		s.visible = true;
		Dungeon.hero = h;
		Actor.add( h );
		return h;
	}

	//a mob standing still at a cell, sturdy, its sprite a rat's
	private <M extends Mob> M body( M m, int cell, int hp ){
		m.HP = m.HT = hp;
		m.pos = cell;
		m.state = m.PASSIVE;
		CharSprite s = new RatSprite();
		s.ch = m;
		m.sprite = s;
		rig.stage.add( s );
		s.place( cell );
		s.visible = true;
		Actor.add( m );
		Dungeon.level.mobs.add( m );
		return m;
	}

	//an armoured statue standing still at a cell, its armour a +3 cloth of `g`
	private ArmoredStatue statue( int cell, Armor.Glyph g ){
		ArmoredStatue st = body( new ArmoredStatue(), cell, 500 );
		try {
			Field f = ArmoredStatue.class.getDeclaredField( "armor" );
			f.setAccessible( true );
			ClothArmor a = armor( g );
			a.level( 3 );
			f.set( st, a );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
		return st;
	}

	private Rat rat( int cell ){
		return body( new Rat(), cell, 500 );
	}

	private static Sword sword( Weapon.Enchantment e ){
		Sword s = new Sword();
		s.enchant( e );
		return s;
	}

	private static ClothArmor armor( Armor.Glyph g ){
		ClothArmor a = new ClothArmor();
		a.inscribe( g );
		return a;
	}

	//a legendary sword whose vestige is `e` (and no enchantment of its own)
	private static Longsword vestigial( Weapon.Enchantment e ){
		Longsword s = new Longsword();
		Quality.roll( s, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		s.enchant( null );
		s.quality.vestige = e;
		s.identify();
		return s;
	}

	// ------------------------------------------------------------------ a scene for the legacy emitters

	private static Field sceneField() throws Exception {
		Field f = GameScene.class.getDeclaredField( "scene" );
		f.setAccessible( true );
		return f;
	}

	/**
	 * A bare game scene (no constructor run) holding the stand-in layers, so the legacy cell emitters
	 * (CellEmitter, which wants the scene's emitter pool) work headless: the floor, surface,
	 * subsurface, sheen, effects and light layers are the rig's (the light one null when withheld),
	 * with groups for its effects and gases.
	 */
	private void bareScene( Group[] l ) throws Exception {
		Object unsafe;
		Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
		u.setAccessible( true );
		unsafe = u.get( null );
		Object scene = unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, GameScene.class );
		set( scene, "floorEmitters", l[GameScene.Layer.FLOOR.ordinal()] );
		set( scene, "ripples", l[GameScene.Layer.SURFACE.ordinal()] );
		set( scene, "subsurface", l[GameScene.Layer.SUBSURFACE.ordinal()] );
		set( scene, "sheen", l[GameScene.Layer.SHEEN.ordinal()] );
		set( scene, "emitters", l[GameScene.Layer.EFFECTS.ordinal()] );
		set( scene, "lights", l[GameScene.Layer.LIGHTS.ordinal()] );
		set( scene, "effects", new Group() );
		set( scene, "gases", new Group() );
		sceneField().set( null, scene );
	}

	private static void set( Object o, String name, Object v ) throws Exception {
		Field f = GameScene.class.getDeclaredField( name );
		f.setAccessible( true );
		f.set( o, v );
	}

	// ================================================================== a fight, three ways

	/** How a run shows: with every layer, with the kit's light layer withheld (no enchantment's
	 *  picture can show), with every post deferred (struck on another thread). */
	enum Mode { PICTURES, NONE, DEFERRED }

	/** A row's fight: its stage built (outside the measured draws), then one strike of the real
	 *  thing at a time. */
	interface Fight {
		Runnable build( ElementProcsTest t );
	}

	/** A row: its name, whether its legacy pictures want a scene's emitters, its fight. */
	static final class Row {
		final String name;
		final boolean scene;
		final Fight fight;

		Row( String name, boolean scene, Fight fight ){
			this.name = name;
			this.scene = scene;
			this.fight = fight;
		}
	}

	//the fight run one way: the game's Random's next number after it, and the pictures it made
	private long[] run( Row row, Mode mode ) throws Exception {
		rig.layers( true );
		//the layers the game's scene shows: the rig's, the light one withheld for NONE (the rig still
		//steps its own)
		Group[] shown = rig.layers.clone();
		if (mode == Mode.NONE){
			shown[GameScene.Layer.LIGHTS.ordinal()] = null;
			GameScene.useLayersForTests( shown );
			Fx.sceneCreated();
		}
		sceneField().set( null, null );
		if (row.scene) bareScene( shown );
		Actor.clear();
		Dungeon.level = EnchantFxTest.room();
		//the mobs walked in the order they came (a plain set walks them by their hashes, which differ
		//from run to run: Loki's poison and Neptune's shock roll for each in turn)
		Dungeon.level.mobs = new LinkedHashSet<>();
		Dungeon.level.customTiles = new ArrayList<>();
		Dungeon.level.customWalls = new ArrayList<>();
		PathFinder.setMapSize( W, W );
		heard.clear();
		Runnable strike = row.fight.build( this );

		IdentityHashMap<Gizmo, Boolean> before = new IdentityHashMap<>(), now = new IdentityHashMap<>();
		rig.alive( before );
		int made = 0;
		Random.pushGenerator( SEED );
		try {
			for (int i = 0; i < STRIKES; i++){
				if (mode == Mode.DEFERRED){
					deferred( strike );
				} else {
					strike.run();
				}
				for (int f = 0; f < 3; f++){
					rig.frames( 1, true );
					now.clear();
					rig.alive( now );
					for (Gizmo g : now.keySet()) if (!before.containsKey( g )) made++;
					before.clear();
					before.putAll( now );
				}
			}
			rig.frames( 30, true );
			return new long[]{ Random.Long(), made };
		} finally {
			Random.popGenerator();
			sceneField().set( null, null );
		}
	}

	//a strike from another thread, its posts handed back and run here after it (the kit's own;
	//a floating text's are the game's and want a scene's texts)
	private void deferred( Runnable strike ) throws Exception {
		final List<Runnable> posted = new ArrayList<>();
		Application saved = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				( p, m, args ) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) args[0] );
						}
						return null;
					}
					return saved != null ? m.invoke( saved, args ) : null;
				} );
		final Throwable[] failed = new Throwable[1];
		try {
			Thread actor = new Thread( () -> {
				try {
					strike.run();
				} catch (Throwable t){
					failed[0] = t;
				}
			} );
			actor.start();
			actor.join();
		} finally {
			Gdx.app = saved;
		}
		if (failed[0] != null) throw new AssertionError( "on the actor thread", failed[0] );
		for (Runnable r : posted) if (kit( r )) r.run();
	}

	//whether a posted runnable is the kit's (Fx.post's), not the game's floating text
	private static boolean kit( Runnable r ){
		for (Field f : r.getClass().getDeclaredFields()){
			try {
				f.setAccessible( true );
				Object c = f.get( r );
				if (c != null && c.getClass().getName().startsWith( "xyz.gabriwar.warpedpixeldungeon.effects.FloatingText" )) return false;
			} catch (ReflectiveOperationException | RuntimeException e){
				return true;
			}
		}
		return true;
	}

	// ================================================================== the rows' fights

	//the hero at (5, 6) striking a rat at (6, 6) with a sword of `e`, as its proc is called
	private static Row weapon( String name, boolean scene, Supplier<Weapon.Enchantment> e ){
		return new Row( name, scene, t -> {
			Weapon.Enchantment ench = e.get();
			Sword s = sword( ench );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 6, 6 ) );
			return () -> ench.proc( s, h, r, 10 );
		} );
	}

	//a rat at (6, 6) striking the hero at (5, 6), who wears a cloth armour of `g`, as its proc is called
	private static Row glyph( String name, boolean scene, Supplier<Armor.Glyph> g ){
		return new Row( name, scene, t -> {
			Armor.Glyph gl = g.get();
			ClothArmor a = armor( gl );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			Rat r = t.rat( at( 6, 6 ) );
			return () -> gl.proc( a, r, h, 10 );
		} );
	}

	static final List<Row> ROWS = new ArrayList<>();

	static {
		// ------------------------------------------------------------- weapon enchantments
		ROWS.add( weapon( "Blazing", false, Blazing::new ) );
		ROWS.add( weapon( "Chilling", false, Chilling::new ) );
		ROWS.add( new Row( "Shocking", false, t -> {
			Shocking e = new Shocking();
			Sword s = sword( e );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 6, 6 ) );
			t.rat( at( 7, 6 ) );
			t.rat( at( 7, 7 ) );
			return () -> e.proc( s, h, r, 10 );
		} ) );
		ROWS.add( new Row( "Kinetic", false, t -> {
			Kinetic e = new Kinetic();
			Sword s = sword( e );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 6, 6 ) );
			return () -> {
				Buff.affect( h, Kinetic.ConservedDamage.class ).setBonus( 30 );
				e.proc( s, h, r, 10 );
			};
		} ) );
		ROWS.add( weapon( "Venomous", true, Venomous::new ) );
		ROWS.add( weapon( "Blocking", false, Blocking::new ) );
		ROWS.add( new Row( "Elastic", false, t -> {
			Elastic e = new Elastic();
			Sword s = sword( e );
			Hero h = t.hero( at( 3, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 4, 6 ) );
			return () -> {
				//back beside the hero for each blow (a push is only begun: no actor runs here)
				Actor.clear();
				Actor.add( h );
				Actor.add( r );
				r.pos = at( 4, 6 );
				e.proc( s, h, r, 10 );
			};
		} ) );
		ROWS.add( weapon( "Crystal", false, Crystal::new ) );
		ROWS.add( weapon( "Paralysis", false, Paralysis::new ) );
		ROWS.add( weapon( "Surging", false, Surging::new ) );
		ROWS.add( weapon( "Parasitic", false, Parasitic::new ) );
		ROWS.add( new Row( "Holy", false, t -> {
			Holy e = new Holy();
			Sword s = sword( e );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Skeleton undead = t.body( new Skeleton(), at( 6, 6 ), 500 );
			FetidRat demon = t.body( new FetidRat(), at( 4, 6 ), 500 );
			final int[] n = { 0 };
			return () -> e.proc( s, h, n[0]++ % 2 == 0 ? undead : demon, 10 );
		} ) );
		ROWS.add( new Row( "Ancient", false, t -> {
			Ancient e = new Ancient();
			Sword s = sword( e );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 6, 6 ) );
			return () -> {
				//a blow that would kill: a kill counted, the foe left standing for the next
				r.HP = 12;
				e.proc( s, h, r, 20 );
			};
		} ) );
		ROWS.add( new Row( "LokisPoison", true, t -> {
			LokisPoison e = new LokisPoison();
			Sword s = sword( e );
			s.level( 2 );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 6, 6 ) );
			t.rat( at( 7, 6 ) );
			t.rat( at( 5, 8 ) );
			return () -> e.proc( s, h, r, 10 );
		} ) );
		ROWS.add( new Row( "NeptuneShock", false, t -> {
			NeptuneShock e = new NeptuneShock();
			NeptunusTrident s = new NeptunusTrident();
			s.enchant( e );
			s.level( 2 );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 6, 6 ) );
			t.rat( at( 7, 6 ) );
			t.rat( at( 10, 6 ) );
			return () -> {
				s.charge = 1000;
				e.proc( s, h, r, 10 );
			};
		} ) );
		ROWS.add( weapon( "Poison", true, Poison::new ) );

		// ------------------------------------------------------------- glyphs
		ROWS.add( new Row( "Adaptation", false, t -> {
			Adaptation g = new Adaptation();
			ClothArmor a = armor( g );
			Level.set( at( 5, 6 ), Terrain.GRASS );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			Rat r = t.rat( at( 6, 6 ) );
			return () -> g.proc( a, r, h, 10 );
		} ) );
		ROWS.add( glyph( "Bounce", false, Bounce::new ) );
		ROWS.add( new Row( "Brimstone", false, t -> {
			ClothArmor a = armor( new Brimstone() );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			//fire catching on an immune wearer: refused (Burning's own branch)
			return () -> Buff.affect( h, Burning.class ).reignite( h, 8f );
		} ) );
		ROWS.add( new Row( "Camouflage", false, t -> {
			ClothArmor a = armor( new Camouflage() );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			return () -> Camouflage.activate( h, h.glyphLevel( Camouflage.class ) );
		} ) );
		ROWS.add( new Row( "Crystal glyph", false, t -> {
			xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal g = new xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal();
			ClothArmor a = armor( g );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			Rat r = t.rat( at( 6, 6 ) );
			return () -> g.proc( a, r, h, 80 );
		} ) );
		ROWS.add( glyph( "Entanglement", true, Entanglement::new ) );
		ROWS.add( new Row( "Potential", false, t -> {
			Potential g = new Potential();
			ClothArmor a = armor( g );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			new WandOfMagicMissile().charge( h );
			Rat r = t.rat( at( 6, 6 ) );
			return () -> g.proc( a, r, h, 10 );
		} ) );
		ROWS.add( new Row( "Purity", false, t -> {
			Purity g = new Purity();
			ClothArmor a = armor( g );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			Rat r = t.rat( at( 6, 6 ) );
			return () -> {
				Buff.affect( h, Weakness.class, 5f );
				g.proc( a, r, h, 10 );
			};
		} ) );
		ROWS.add( glyph( "Repulsion", false, Repulsion::new ) );
		ROWS.add( new Row( "Satisfying", false, t -> {
			Satisfying g = new Satisfying();
			ClothArmor a = armor( g );
			a.level( 2 );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			Buff.affect( h, Hunger.class );
			Rat r = t.rat( at( 6, 6 ) );
			return () -> g.proc( a, r, h, 30 );
		} ) );
		ROWS.add( glyph( "Stone", false, Stone::new ) );
		ROWS.add( new Row( "Viscosity", false, t -> {
			Viscosity g = new Viscosity();
			ClothArmor a = armor( g );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.armor = a;
			Rat r = t.rat( at( 6, 6 ) );
			//its proc sets the tracker, Char.attack then defers through it
			return () -> {
				g.proc( a, r, h, 10 );
				h.buff( Viscosity.ViscosityTracker.class ).deferDamage( 10 );
			};
		} ) );

		// ------------------------------------------------------------- curses
		ROWS.add( weapon( "Dazzling", false, Dazzling::new ) );
		ROWS.add( new Row( "Explosive", false, t -> {
			Explosive e = new Explosive();
			Sword s = sword( e );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 6, 6 ) );
			final int[] n = { 0 };
			return () -> {
				//its fuse burnt down to the warm warning, then the hot one, never to the blast
				if (n[0]++ % 8 == 0) setDurability( e, 58 );
				e.proc( s, h, r, 10 );
			};
		} ) );
		ROWS.add( weapon( "Polarized", false, Polarized::new ) );
		ROWS.add( weapon( "Pressurized", false, Pressurized::new ) );
		ROWS.add( glyph( "AntiEntropy", true, AntiEntropy::new ) );
		ROWS.add( new Row( "Bulk", false, t -> {
			Level.set( at( 5, 6 ), Terrain.DOOR );
			ArmoredStatue st = t.statue( at( 5, 6 ), new Bulk() );
			t.hero( at( 3, 3 ) );
			//its real site: the wearer's speed, asked in a doorway
			return () -> {
				st.speed();
				st.state = st.PASSIVE;
			};
		} ) );
		ROWS.add( glyph( "Corrosion", false, Corrosion::new ) );
		ROWS.add( glyph( "Overgrowth", true, Overgrowth::new ) );
		ROWS.add( glyph( "Stench", false, Stench::new ) );

		// ------------------------------------------------------------- the buffs' ticks and Char's lines
		ROWS.add( new Row( "a parasitic seed's tick", false, t -> {
			Rat host = t.rat( at( 6, 6 ) );
			Hero fed = t.hero( at( 5, 6 ) );
			return () -> {
				fed.HP = 100;
				Parasitic.ParasiticSeed seed = Buff.affect( host, Parasitic.ParasiticSeed.class );
				seed.set( Parasitic.ParasiticSeed.DURATION, 2 );
				seed.act();
			};
		} ) );
		ROWS.add( new Row( "a deferred blow's tick", false, t -> {
			Rat r = t.rat( at( 6, 6 ) );
			return () -> {
				Viscosity.DeferedDamage d = Buff.affect( r, Viscosity.DeferedDamage.class );
				d.extend( 30 );
				d.act();
			};
		} ) );
		ROWS.add( new Row( "Kinetic's store (Char.damage)", false, t -> {
			Hero h = t.hero( at( 5, 6 ) );
			//a dummy that does not die of it (the store comes before the death)
			Rat r = t.body( new Rat(){
				@Override
				public void die( Object cause ){ }
			}, at( 6, 6 ), 500 );
			return () -> {
				Buff.affect( h, Kinetic.KineticTracker.class ).conservedDamage = 0;
				r.HP = 500;
				r.damage( 520, h );
				r.HP = 500;
			};
		} ) );
		ROWS.add( new Row( "AntiMagic's absorb (Char.damage)", false, t -> {
			ArmoredStatue st = t.statue( at( 6, 6 ), new AntiMagic() );
			t.hero( at( 3, 3 ) );
			return () -> {
				st.HP = 500;
				st.damage( 10, new Blazing() );
				//(a statue struck wakes: kept still, as every dummy here)
				st.state = st.PASSIVE;
			};
		} ) );
	}

	private static void setDurability( Explosive e, int d ){
		try {
			Field f = Explosive.class.getDeclaredField( "durability" );
			f.setAccessible( true );
			f.setInt( e, d );
		} catch (ReflectiveOperationException x){
			throw new AssertionError( x );
		}
	}

	@Test
	public void everyRowsRealProcDrawsTheSameOfTheGamesRandomWithItsPicturesAsWithoutOnBothThreads() throws Exception {
		StringBuilder failures = new StringBuilder();
		for (Row row : ROWS){
			//once first, unmeasured: a class first loaded may draw the game's Random in its static
			//initialiser (WindParticle does), which would tell the first run from the others
			run( row, Mode.PICTURES );
			long[] pictures = run( row, Mode.PICTURES );
			long[] none = run( row, Mode.NONE );
			long[] deferred = run( row, Mode.DEFERRED );
			if (pictures[0] != none[0]) failures.append( row.name ).append( ": with and without its pictures\n" );
			if (pictures[0] != deferred[0]) failures.append( row.name ).append( ": on the actor thread\n" );
			if (pictures[1] <= none[1]) failures.append( row.name ).append( ": shows nothing (" ).append( pictures[1] ).append( " quads, " )
					.append( none[1] ).append( " without)\n" );
			if (deferred[1] <= none[1]) failures.append( row.name ).append( ": shows nothing deferred\n" );
		}
		assertEquals( failures.toString(), 0, failures.length() );
	}

	/** Blooming's grass wants the game's tilemaps headless, so its fight has nothing to grow: the rolls
	 *  of a proc that grows nothing draw the same with and without pictures; and its look, handed the
	 *  cells a proc grew, the same on both threads. */
	@Test
	public void bloomingsRollsAndItsLookOnTheCellsItGrew() throws Exception {
		Row rolls = new Row( "Blooming (nothing to grow)", false, t -> {
			Blooming e = new Blooming();
			Sword s = sword( e );
			Hero h = t.hero( at( 5, 6 ) );
			h.belongings.weapon = s;
			Rat r = t.rat( at( 10, 6 ) );
			//the pool round it: no cell takes grass
			return () -> e.proc( s, h, r, 10 );
		} );
		Row look = new Row( "Blooming's look", false, t -> {
			Hero h = t.hero( at( 5, 6 ) );
			Rat r = t.rat( at( 6, 6 ) );
			int[] cells = { at( 6, 6 ), at( 7, 6 ), at( 6, 7 ), at( 7, 7 ) };
			EnchantLook l = EnchantLooks.of( Blooming.class );
			return () -> EnchantFx.proc( l, h, r, ElementRows.bloomStrength( cells.length, 0, 1f ), 0, 0, ElementRows.blooming( cells, r.pos ) );
		} );
		run( rolls, Mode.PICTURES );
		long[] a = run( rolls, Mode.PICTURES ), b = run( rolls, Mode.NONE ), c = run( rolls, Mode.DEFERRED );
		assertEquals( a[0], b[0] );
		assertEquals( a[0], c[0] );
		run( look, Mode.PICTURES );
		a = run( look, Mode.PICTURES );
		b = run( look, Mode.NONE );
		c = run( look, Mode.DEFERRED );
		assertEquals( a[0], b[0] );
		assertEquals( a[0], c[0] );
		assertTrue( "its look shows: " + a[1], a[1] > b[1] && c[1] > b[1] );
	}

	// ================================================================== caps

	/** How a staged row plays: a weapon's proc, a glyph's ward, a glyph's quiet moment, Blocking's
	 *  ward on the wielder. */
	enum Kind { PROC, WARD, PASSIVE, BLOCK }

	static final class Look {
		final String name;
		final Class<?> of;
		final Kind kind;
		final Supplier<EnchantFx.Twist> twist;
		final boolean echo, marks;

		Look( String name, Class<?> of, Kind kind, boolean echo, boolean marks, Supplier<EnchantFx.Twist> twist ){
			this.name = name;
			this.of = of;
			this.kind = kind;
			this.echo = echo;
			this.marks = marks;
			this.twist = twist;
		}
	}

	static final List<Look> LOOKS = new ArrayList<>();

	static {
		int[] cells = { at( 6, 6 ), at( 7, 6 ), at( 6, 7 ), at( 7, 5 ) };
		LOOKS.add( new Look( "Blazing ignite", Blazing.class, Kind.PROC, true, false, () -> ElementRows.blazing( true, false ) ) );
		LOOKS.add( new Look( "Blazing flare", Blazing.class, Kind.PROC, true, false, () -> ElementRows.blazing( false, true ) ) );
		LOOKS.add( new Look( "Blazing both", Blazing.class, Kind.PROC, true, false, () -> ElementRows.blazing( true, true ) ) );
		LOOKS.add( new Look( "Chilling", Chilling.class, Kind.PROC, true, true, () -> ElementRows.chilling( 6f ) ) );
		LOOKS.add( new Look( "Shocking first", Shocking.class, Kind.PROC, true, false, () -> ElementRows.shock( 0, -1 ) ) );
		LOOKS.add( new Look( "Shocking struck", Shocking.class, Kind.PROC, true, false, () -> ElementRows.shock( 2, -1 ) ) );
		LOOKS.add( new Look( "Kinetic", Kinetic.class, Kind.PROC, false, false, () -> ElementRows.kinetic( 1.2f ) ) );
		LOOKS.add( new Look( "Venomous", Venomous.class, Kind.PROC, true, true, ElementRows::venomous ) );
		LOOKS.add( new Look( "Blocking", Blocking.class, Kind.BLOCK, true, false, ElementRows::blocking ) );
		LOOKS.add( new Look( "Blooming", Blooming.class, Kind.PROC, true, false, () -> ElementRows.blooming( cells, at( 6, 6 ) ) ) );
		LOOKS.add( new Look( "Elastic", Elastic.class, Kind.PROC, true, false, ElementRows::elastic ) );
		LOOKS.add( new Look( "Paralysis", Paralysis.class, Kind.PROC, true, false, () -> ElementRows.paralysis( 2.5f ) ) );
		LOOKS.add( new Look( "Surging", Surging.class, Kind.PROC, true, false, () -> ElementRows.surging( 5, 4 ) ) );
		LOOKS.add( new Look( "Parasitic", Parasitic.class, Kind.PROC, true, false, ElementRows::parasitic ) );
		LOOKS.add( new Look( "Holy undead", Holy.class, Kind.PROC, true, false, () -> null ) );
		LOOKS.add( new Look( "Holy demonic", Holy.class, Kind.PROC, true, false, () -> ElementRows.holy( true ) ) );
		LOOKS.add( new Look( "Ancient", Ancient.class, Kind.PROC, false, false, () -> ElementRows.ancient( true ) ) );
		LOOKS.add( new Look( "LokisPoison", LokisPoison.class, Kind.PROC, false, false, () -> ElementRows.plague( 5 ) ) );
		LOOKS.add( new Look( "NeptuneShock", NeptuneShock.class, Kind.PROC, false, false, () -> ElementRows.shock( 0, 5 ) ) );
		LOOKS.add( new Look( "Poison", Poison.class, Kind.PROC, false, false, ElementRows::poison ) );
		LOOKS.add( new Look( "Adaptation grass", Adaptation.class, Kind.WARD, true, false, () -> ElementRows.adaptation( ElementRows.GRASS ) ) );
		LOOKS.add( new Look( "Adaptation water", Adaptation.class, Kind.WARD, true, false, () -> ElementRows.adaptation( ElementRows.WATER ) ) );
		LOOKS.add( new Look( "Adaptation tall grass", Adaptation.class, Kind.WARD, true, false, () -> ElementRows.adaptation( ElementRows.TALL_GRASS ) ) );
		LOOKS.add( new Look( "Adaptation embers", Adaptation.class, Kind.WARD, true, false, () -> ElementRows.adaptation( ElementRows.EMBERS ) ) );
		LOOKS.add( new Look( "Adaptation trap", Adaptation.class, Kind.WARD, true, false, () -> ElementRows.adaptation( ElementRows.TRAP ) ) );
		LOOKS.add( new Look( "AntiMagic", AntiMagic.class, Kind.WARD, false, false, () -> ElementRows.nullified( 9 ) ) );
		LOOKS.add( new Look( "AntiMagic absorb", AntiMagic.class, Kind.PASSIVE, false, false, () -> ElementRows.nullified( 9 ) ) );
		LOOKS.add( new Look( "Bounce", Bounce.class, Kind.WARD, true, false, ElementRows::bounce ) );
		LOOKS.add( new Look( "Brimstone", Brimstone.class, Kind.PASSIVE, false, false, () -> null ) );
		LOOKS.add( new Look( "Camouflage", Camouflage.class, Kind.WARD, false, false, ElementRows::camouflage ) );
		LOOKS.add( new Look( "Crystal glyph", xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal.class, Kind.WARD, true, false,
				ElementRows::crystalWard ) );
		LOOKS.add( new Look( "Entanglement", Entanglement.class, Kind.WARD, true, false, ElementRows::entanglement ) );
		LOOKS.add( new Look( "Potential", Potential.class, Kind.WARD, true, false, () -> ElementRows.potential( 2 ) ) );
		LOOKS.add( new Look( "Purity", Purity.class, Kind.WARD, true, false, () -> ElementRows.purity( 3 ) ) );
		LOOKS.add( new Look( "Repulsion", Repulsion.class, Kind.WARD, true, false, ElementRows::repulsion ) );
		LOOKS.add( new Look( "Satisfying", Satisfying.class, Kind.WARD, true, false, ElementRows::satisfying ) );
		LOOKS.add( new Look( "Stone", Stone.class, Kind.WARD, false, false, ElementRows::stone ) );
		LOOKS.add( new Look( "Viscosity", Viscosity.class, Kind.PASSIVE, true, false, ElementRows::viscosity ) );
		LOOKS.add( new Look( "Dazzling", Dazzling.class, Kind.PROC, false, false, ElementRows::dazzling ) );
		LOOKS.add( new Look( "Explosive hot", Explosive.class, Kind.PROC, false, false, () -> ElementRows.explosive( true ) ) );
		LOOKS.add( new Look( "Polarized", Polarized.class, Kind.PROC, false, false, ElementRows::polarized ) );
		LOOKS.add( new Look( "Pressurized", Pressurized.class, Kind.PROC, false, false, ElementRows::pressurized ) );
		LOOKS.add( new Look( "AntiEntropy", AntiEntropy.class, Kind.WARD, false, true, () -> ElementRows.antiEntropy( at( 6, 6 ) ) ) );
		LOOKS.add( new Look( "Corrosion", Corrosion.class, Kind.WARD, false, true, () -> ElementRows.corrosion( at( 6, 6 ) ) ) );
		LOOKS.add( new Look( "Overgrowth", Overgrowth.class, Kind.WARD, false, false, () -> null ) );
		LOOKS.add( new Look( "Stench", Stench.class, Kind.WARD, false, false, ElementRows::stench ) );
	}

	//a row staged at a tier on a defender d by an attacker a; its flags
	private static void stage( Look l, CharSprite a, CharSprite d, int tier, int flags ){
		EnchantLook look = EnchantLooks.of( l.of );
		if (look.curse) flags |= EnchantFx.CURSE;
		d.ch.HP = tier == EnchantFx.FINISHER && l.kind == Kind.PROC ? 0 : d.ch.HT;
		a.ch.HP = tier == EnchantFx.FINISHER && (l.kind == Kind.WARD || l.kind == Kind.BLOCK) ? 0 : a.ch.HT;
		float s = EnchantFx.strengthOf( tier );
		switch (l.kind){
			case PROC:
				EnchantFx.proc( look, a.ch, d.ch, s, 1, flags, l.twist.get() );
				break;
			case WARD:
				EnchantFx.ward( look, d.ch, a.ch, s, 1, flags, l.twist.get() );
				break;
			case PASSIVE:
				EnchantFx.passive( look, d.ch, EnchantFx.ABSORB, s, flags, l.twist.get() );
				break;
			case BLOCK:
				ElementRows.blocking( look, d.ch, a.ch, s, 1, (flags & EnchantFx.ECHO) != 0 );
				break;
		}
	}

	@Test
	public void everyRowsLookKeepsToItsCaps(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), wet = rig.rat( at( 10, 6 ) ), wa = rig.rat( at( 9, 6 ) );
		StringBuilder failures = new StringBuilder();
		for (Look l : LOOKS){
			for (int k = 0; k < 2; k++){
				CharSprite def = k == 0 ? d : wet, att = k == 0 ? a : wa;
				for (int tier = EnchantFx.WEAK; tier <= EnchantFx.FINISHER; tier++){
					for (int flags : l.echo ? new int[]{ 0, EnchantFx.ECHO } : new int[]{ 0 }){
						Game.timeTotal += 2f;
						final int t = tier, fl = flags;
						int[] m = rig.made( () -> stage( l, att, def, t, fl ), 90 );
						boolean ward = l.kind == Kind.WARD || l.kind == Kind.BLOCK;
						int top = l.kind == Kind.PASSIVE ? EnchantFx.WEAK : ward ? Math.min( t, EnchantFx.STRONG ) : t;
						boolean killed = ward && t == EnchantFx.FINISHER && (fl & EnchantFx.ECHO) == 0;
						int cap = (fl & EnchantFx.ECHO) != 0 ? EnchantFx.ECHO_CAP : EnchantFx.CAP[top] + (killed ? 6 : 0);
						//(a ward that killed its attacker: the attacker's death accent, a beat of its own, a mark too)
						int decals = EnchantFx.DECALS + (l.marks ? ElementRows.EXTRA_DECALS : 0) + (killed ? 1 : 0);
						String what = l.name + " tier " + t + " flags " + fl + (k == 1 ? " on water" : "");
						if (m[0] > cap) failures.append( what ).append( ": " ).append( m[0] ).append( " quads > " ).append( cap ).append( '\n' );
						if (m[0] == 0) failures.append( what ).append( ": shows nothing\n" );
						if (m[1] > EnchantFx.LIGHTS) failures.append( what ).append( ": " ).append( m[1] ).append( " lights\n" );
						if (m[2] > decals) failures.append( what ).append( ": " ).append( m[2] ).append( " decals\n" );
						rig.frames( 150, false );
					}
				}
			}
		}
		assertEquals( failures.toString(), 0, failures.length() );
		a.ch.HP = d.ch.HP = wet.ch.HP = wa.ch.HP = 50;
	}

	/** A twist's own pieces this beat: the quads, lights and decals it took of the beat's tally. */
	static final class Counted implements EnchantFx.Twist {
		final EnchantFx.Twist real;
		int spent = -1;

		Counted( EnchantFx.Twist real ){
			this.real = real;
		}

		@Override
		public void play( EnchantFx.Beat b ){
			int q = b.left(), li = b.lightsLeft(), d = b.decalsLeft();
			real.play( b );
			spent = (q - b.left()) + (li - b.lightsLeft()) + (d - b.decalsLeft());
		}
	}

	//the rows whose twist rightly adds nothing at a tier, and why
	static final Map<String, String> ADDS_NOTHING = new LinkedHashMap<>();
	static {
		ADDS_NOTHING.put( "Blocking tier 0", "its runes start at MID, the ward family draws the weak look" );
		ADDS_NOTHING.put( "Blocking tier 0 echo", "its runes start at MID" );
		ADDS_NOTHING.put( "Shocking struck tier 2", "a strong shock's etch and pool are the family's, a third target adds no flash" );
		ADDS_NOTHING.put( "Shocking first tier 0 echo", "an echo leaves no etch and no pool and flashes no impact: its kept lightning is its picture" );
		ADDS_NOTHING.put( "Shocking struck tier 0 echo", "an echo leaves no etch and no pool, a third target no flash" );
	}

	/** Every row's twist shows its own pieces at WEAK and at STRONG (and a vestige's at WEAK as an
	 *  echo): it keeps its share of the tier's quads from the family's draw, so a weak proc is not
	 *  the family's look alone. */
	@Test
	public void everyRowsTwistShowsItsOwnPiecesWeakAndStrong(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		StringBuilder failures = new StringBuilder();
		for (Look l : LOOKS){
			if (l.twist.get() == null) continue;
			int[] tiers = l.kind == Kind.PASSIVE ? new int[]{ EnchantFx.WEAK } : new int[]{ EnchantFx.WEAK, EnchantFx.STRONG };
			for (int tier : tiers){
				for (boolean echo : l.echo && tier == EnchantFx.WEAK ? new boolean[]{ false, true } : new boolean[]{ false }){
					Game.timeTotal += 2f;
					a.ch.HP = a.ch.HT;
					d.ch.HP = d.ch.HT;
					EnchantLook look = EnchantLooks.of( l.of );
					int flags = (look.curse ? EnchantFx.CURSE : 0) | (echo ? EnchantFx.ECHO : 0);
					float st = EnchantFx.strengthOf( tier );
					final Counted c = new Counted( l.twist.get() );
					rig.made( () -> {
						switch (l.kind){
							case PROC:
								EnchantFx.proc( look, a.ch, d.ch, st, 1, flags, c );
								break;
							case WARD:
								EnchantFx.ward( look, d.ch, a.ch, st, 1, flags, c );
								break;
							case PASSIVE:
								EnchantFx.passive( look, d.ch, EnchantFx.ABSORB, st, flags, c );
								break;
							case BLOCK:
								//as ElementRows.blocking: a ward on the wielder, an echo the proc's swapped
								if (echo) EnchantFx.proc( look, d.ch, a.ch, st, 1, EnchantFx.ECHO, c );
								else EnchantFx.ward( look, a.ch, d.ch, st, 1, 0, c );
								break;
						}
					}, 60 );
					String what = l.name + " tier " + tier + (echo ? " echo" : "");
					if (c.spent < 0) failures.append( what ).append( ": its twist never played\n" );
					else if (c.spent == 0 && !ADDS_NOTHING.containsKey( what )) failures.append( what ).append( ": its twist added nothing\n" );
					else if (c.spent > 0 && ADDS_NOTHING.containsKey( what )) failures.append( what ).append( ": listed as adding nothing, adds " )
							.append( c.spent ).append( '\n' );
					rig.frames( 150, false );
				}
			}
		}
		assertEquals( failures.toString(), 0, failures.length() );
	}

	/** A vestige's shock past its first RICH_TARGETS targets echoes as a pale minimal star, never a
	 *  full echo. */
	@Test
	public void aShocksEchoPastItsRichTargetsIsMinimal(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		EnchantLook l = EnchantLooks.of( Shocking.class );
		Game.timeTotal += 2f;
		int[] rich = rig.made( () -> EnchantFx.proc( l, a.ch, d.ch, 0.45f, 0, EnchantFx.ECHO | EnchantFx.multi( 0 ), ElementRows.shock( 0, -1 ) ), 60 );
		rig.frames( 150, false );
		Game.timeTotal += 2f;
		int[] past = rig.made( () -> EnchantFx.proc( l, a.ch, d.ch, 0.45f, 0, EnchantFx.ECHO | EnchantFx.multi( EnchantFx.RICH_TARGETS ),
				ElementRows.shock( EnchantFx.RICH_TARGETS, -1 ) ), 60 );
		assertTrue( "the fourth's echo shows: " + past[0], past[0] > 0 );
		assertTrue( "the fourth's echo is minimal: " + past[0], past[0] <= EnchantFx.MINIMAL_CAP );
		assertEquals( "the fourth's echo throws no light", 0, past[1] );
		assertTrue( "the first's echo is fuller: " + rich[0] + " > " + past[0], rich[0] > past[0] );
	}

	/** Blazing on a foe fire cannot touch, Venomous on one poison cannot, Chilling on one cold cannot,
	 *  and AntiMagic's roll on a magic that did nothing: nothing happens, so nothing shows and nothing
	 *  stings; the same blows on a plain rat do. */
	@Test
	public void aBiteThatTookNothingShowsNothing() throws Exception {
		Object[][] cases = {
				{ Blazing.class, Burning.class },
				{ Venomous.class, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison.class },
				{ Chilling.class, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill.class } };
		Random.pushGenerator( SEED );
		try {
			for (Object[] c : cases){
				for (boolean immune : new boolean[]{ true, false }){
					rig.layers( true );
					Actor.clear();
					Dungeon.level = EnchantFxTest.room();
					Dungeon.level.mobs = new LinkedHashSet<>();
					PathFinder.setMapSize( W, W );
					@SuppressWarnings("unchecked")
					Weapon.Enchantment e = ((Class<? extends Weapon.Enchantment>) c[0]).newInstance();
					Sword w = sword( e );
					Hero h = hero( at( 5, 6 ) );
					h.belongings.weapon = w;
					final Class<?> shrugs = (Class<?>) c[1];
					Rat foe = immune ? body( new Rat(){
						{
							immunities.add( shrugs );
						}
					}, at( 6, 6 ), 500 ) : rat( at( 6, 6 ) );
					if (c[0] == Venomous.class) bareScene( rig.layers );
					int lights = 0;
					heard.clear();
					for (int i = 0; i < 24; i++){
						Game.timeTotal += 1f;
						foe.HP = 500;
						for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff b : foe.buffs().toArray( new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff[0] )){
							b.detach();
						}
						lights += rig.made( () -> e.proc( w, h, foe, 10 ), 6 )[1];
					}
					String what = ((Class<?>) c[0]).getSimpleName() + (immune ? " on a foe it cannot touch" : " on a rat");
					if (immune){
						assertEquals( what + ": no light", 0, lights );
						assertTrue( what + ": no sting", heard.isEmpty() );
					} else {
						assertTrue( what + ": it shows", lights > 0 && !heard.isEmpty() );
					}
					sceneField().set( null, null );
					Dungeon.hero = null;
				}
			}
		} finally {
			Random.popGenerator();
		}
		//AntiMagic's roll on a magic of no damage: nothing to absorb
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Game.timeTotal += 2f;
		heard.clear();
		int[] m = rig.made( () -> ElementRows.antiMagic( d.ch, a.ch, 0, 5 ), 30 );
		assertEquals( "AntiMagic on nothing: no quad", 0, m[0] );
		assertTrue( "AntiMagic on nothing: no sting", heard.isEmpty() );
		Game.timeTotal += 2f;
		assertTrue( "AntiMagic on a blow it took from shows", rig.made( () -> ElementRows.antiMagic( d.ch, a.ch, 4, 9 ), 30 )[0] > 0 );
	}

	@Test
	public void theMomentsOfNoProcKeepToTheirOwnCaps(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), e = rig.rat( at( 7, 6 ) );
		List<Char> fed = new ArrayList<>();
		fed.add( a.ch );
		fed.add( e.ch );
		Object[][] moments = {
				{ "Kinetic's store", ElementRows.STORE_MOTES, (Runnable) () -> ElementRows.kineticStore( d.ch, a.ch, 20 ) },
				{ "Crystal's hit", 3, (Runnable) () -> ElementRows.crystalHit( a.ch, d.ch ) },
				{ "Crystal shattering", EnchantFx.CAP[EnchantFx.STRONG], (Runnable) () -> ElementRows.crystalShatter( a.ch ) },
				{ "a seed's tick", ElementRows.TICK_CAP, (Runnable) () -> ElementRows.seedTick( d.ch, fed ) },
				{ "a deferred blow's tick", 1, (Runnable) () -> ElementRows.tarTick( d.ch ) },
				{ "Polarized's fizzle", 2, (Runnable) () -> ElementRows.polarFizzle( a.ch, d.ch ) },
				{ "Ancient's kill rune", 1, (Runnable) () -> ElementRows.killRune( a.ch, d.ch ) },
				{ "Ancient attuning", EnchantFx.CAP[EnchantFx.FINISHER], (Runnable) () -> ElementRows.attuned( a.ch ) },
				{ "Brimstone refusing", EnchantFx.CAP[EnchantFx.WEAK], (Runnable) () -> ElementRows.brimstone( d.ch ) },
				{ "Bulk in a door", EnchantFx.CAP[EnchantFx.WEAK], (Runnable) () -> ElementRows.bulk( d.ch ) } };
		for (Object[] mo : moments){
			Game.timeTotal += 3f;
			int[] m = rig.made( (Runnable) mo[2], 90 );
			assertTrue( mo[0] + ": " + m[0], m[0] > 0 && m[0] <= (Integer) mo[1] );
			assertTrue( mo[0] + " lights " + m[1], m[1] <= EnchantFx.LIGHTS );
			assertTrue( mo[0] + " decals " + m[2], m[2] <= EnchantFx.DECALS );
			rig.frames( 120, false );
		}
		//Brimstone and Bulk at most once in their gaps on one character
		Game.timeTotal += 3f;
		ElementRows.brimstone( d.ch );
		int[] again = rig.made( () -> ElementRows.brimstone( d.ch ), 30 );
		assertEquals( "Brimstone again within its gap", 0, again[0] );
		Game.timeTotal += ElementRows.BRIMSTONE_GAP + 0.01f;
		assertTrue( rig.made( () -> ElementRows.brimstone( d.ch ), 30 )[0] > 0 );
		Game.timeTotal += 3f;
		ElementRows.bulk( e.ch );
		assertEquals( "Bulk again within its gap", 0, rig.made( () -> ElementRows.bulk( e.ch ), 30 )[0] );
		//a doorway's slowdown is no blow: no sting
		Game.timeTotal += 3f;
		heard.clear();
		assertTrue( rig.made( () -> ElementRows.bulk( a.ch ), 30 )[0] > 0 );
		assertTrue( "Bulk stings nothing", heard.isEmpty() );
		//and nothing on an unseen character
		rig.frames( 120, false );
		Game.timeTotal += 3f;
		d.visible = false;
		a.visible = false;
		assertEquals( 0, rig.made( () -> ElementRows.crystalHit( a.ch, d.ch ), 30 )[0] );
	}

	// ================================================================== echoes

	/** The rows a vestige can carry. */
	static final List<Class<? extends Weapon.Enchantment>> VESTIGES = new ArrayList<>();
	static {
		VESTIGES.add( Blazing.class );
		VESTIGES.add( Chilling.class );
		VESTIGES.add( Shocking.class );
		VESTIGES.add( Venomous.class );
		VESTIGES.add( Blocking.class );
		VESTIGES.add( Blooming.class );
		VESTIGES.add( Elastic.class );
		VESTIGES.add( Paralysis.class );
		VESTIGES.add( Surging.class );
		VESTIGES.add( Holy.class );
		VESTIGES.add( Parasitic.class );
	}

	@Test
	public void theElevenAVestigeCanCarryPlayAsAnEcho() throws Exception {
		for (Class<? extends Weapon.Enchantment> c : VESTIGES){
			assertTrue( c.getSimpleName() + " can be a vestige", Quality.vestigePool().contains( c ) );
			String src = source( "items/weapon/enchantments/" + c.getSimpleName() + ".java" );
			assertTrue( c.getSimpleName() + " passes its echo", src.contains( "Quality.isVestige( weapon, this )" ) );
		}
		//from a vestigial weapon: never a stamp, a muffled sting; from its own weapon: a stamp, a full sting
		Random.pushGenerator( SEED );
		try {
			for (Class<? extends Weapon.Enchantment> c : VESTIGES){
				if (c == Blooming.class) continue;
				for (boolean vestige : new boolean[]{ false, true }){
					rig.layers( true );
					Actor.clear();
					Dungeon.level = EnchantFxTest.room();
					PathFinder.setMapSize( W, W );
					Weapon.Enchantment e = c.newInstance();
					Weapon w = vestige ? vestigial( e ) : sword( e );
					Hero h = hero( at( 5, 6 ) );
					h.belongings.weapon = w;
					Char foe = c == Holy.class ? body( new Skeleton(), at( 6, 6 ), 500 ) : rat( at( 6, 6 ) );
					if (c == Shocking.class) rat( at( 7, 6 ) );
					if (c == Venomous.class) bareScene( rig.layers );
					EnchantLook look = EnchantLooks.of( c );
					boolean fired = false;
					for (int i = 0; i < 60 && !fired; i++){
						Game.timeTotal += 1f;
						heard.clear();
						foe.sprite.blowLook = null;
						foe.sprite.blowUntil = 0;
						int lightnings = count( Lightning.class );
						e.proc( w, h, foe, 10 );
						rig.frames( 4, true );
						boolean stung = !heard.isEmpty();
						fired = stung || count( Lightning.class ) > lightnings;
						if (!fired) continue;
						if (look.fam != Family.WARD){
							if (vestige) assertNull( c.getSimpleName() + ": an echo never stamps", foe.sprite.blowLook );
						}
						if (stung){
							float v = (Float) heard.get( 0 )[2];
							float full = Sting.volume( look, EnchantFx.WEAK, false );
							if (vestige) assertTrue( c.getSimpleName() + ": an echo's sting " + v, v <= Sting.LOUDEST * Sting.ECHO_VOL + 1e-4f );
							else assertTrue( c.getSimpleName() + ": its own sting " + v + " < " + full, v >= full - 1e-4f );
						}
					}
					assertTrue( c.getSimpleName() + (vestige ? " as a vestige" : "") + " fired", fired );
					sceneField().set( null, null );
					Dungeon.hero = null;
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	/** The glyphs of these rows an armour's vestige can carry. */
	static final List<Class<? extends Armor.Glyph>> GLYPH_VESTIGES = new ArrayList<>();
	static {
		GLYPH_VESTIGES.add( Adaptation.class );
		GLYPH_VESTIGES.add( Bounce.class );
		GLYPH_VESTIGES.add( xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal.class );
		GLYPH_VESTIGES.add( Entanglement.class );
		GLYPH_VESTIGES.add( Potential.class );
		GLYPH_VESTIGES.add( Purity.class );
		GLYPH_VESTIGES.add( Repulsion.class );
		GLYPH_VESTIGES.add( Satisfying.class );
		GLYPH_VESTIGES.add( Viscosity.class );
	}

	//a legendary cloth armour whose vestige is `g` (and no glyph of its own)
	private static ClothArmor vestigialArmor( Armor.Glyph g ){
		ClothArmor a = new ClothArmor();
		Quality.roll( a, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		a.inscribe( null );
		a.quality.vestige = g;
		a.identify();
		return a;
	}

	@Test
	public void theNineGlyphsAVestigeCanCarryAnswerAsAnEcho() throws Exception {
		for (Class<? extends Armor.Glyph> c : GLYPH_VESTIGES){
			assertTrue( c.getSimpleName() + " can be a vestige", Quality.glyphVestigePool().contains( c ) );
			String src = source( "items/armor/glyphs/" + c.getSimpleName() + ".java" );
			assertTrue( c.getSimpleName() + " passes its echo", src.contains( "Quality.isVestige( armor, this )" ) );
		}
		//from a vestigial armour: a muffled sting; from its own armour: a full sting
		Random.pushGenerator( SEED );
		try {
			for (Class<? extends Armor.Glyph> c : GLYPH_VESTIGES){
				for (boolean vestige : new boolean[]{ false, true }){
					rig.layers( true );
					Actor.clear();
					Dungeon.level = EnchantFxTest.room();
					PathFinder.setMapSize( W, W );
					Armor.Glyph g = c.newInstance();
					ClothArmor a = vestige ? vestigialArmor( g ) : armor( g );
					if (c == Satisfying.class) a.level( 2 );
					if (c == Adaptation.class) Level.set( at( 5, 6 ), Terrain.GRASS );
					Hero h = hero( at( 5, 6 ) );
					h.belongings.armor = a;
					if (c == Potential.class) new WandOfMagicMissile().charge( h );
					if (c == Satisfying.class) Buff.affect( h, Hunger.class );
					if (c == Entanglement.class) bareScene( rig.layers );
					Rat r = rat( at( 6, 6 ) );
					//a heavy blow for Crystal, a big one to feed on for Satisfying
					int blow = c == xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal.class ? 80 : c == Satisfying.class ? 30 : 10;
					EnchantLook look = EnchantLooks.of( c );
					boolean fired = false;
					for (int i = 0; i < 60 && !fired; i++){
						Game.timeTotal += 1f;
						heard.clear();
						if (c == Purity.class) Buff.affect( h, Weakness.class, 5f );
						g.proc( a, r, h, blow );
						//its proc sets the tracker, Char.attack then defers through it
						if (c == Viscosity.class) h.buff( Viscosity.ViscosityTracker.class ).deferDamage( 10 );
						rig.frames( 4, true );
						fired = !heard.isEmpty();
						if (!fired) continue;
						float v = (Float) heard.get( 0 )[2];
						float full = Sting.volume( look, EnchantFx.WEAK, false );
						if (vestige) assertTrue( c.getSimpleName() + ": an echo's sting " + v, v <= Sting.LOUDEST * Sting.ECHO_VOL + 1e-4f );
						else assertTrue( c.getSimpleName() + ": its own sting " + v + " < " + full, v >= full - 1e-4f );
					}
					assertTrue( c.getSimpleName() + (vestige ? " as a vestige" : "") + " answered", fired );
					sceneField().set( null, null );
					Dungeon.hero = null;
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	//how many of a kind of picture are alive on the stage
	private int count( Class<?> kind ){
		int n = 0;
		for (Gizmo g : rig.stage.membersView()) if (kind.isInstance( g ) && g.alive) n++;
		for (Gizmo g : aside.membersView()) if (kind.isInstance( g ) && g.alive) n++;
		return n;
	}

	// ================================================================== states

	@Test
	public void theSeedAndTheSurgeStartAndEndWithTheirBuffs(){
		Rat r = rat( at( 6, 6 ) );
		CharSprite s = r.sprite;
		int none = s.looksCount();
		Buff.affect( r, Parasitic.ParasiticSeed.class ).set( 5f, 1 );
		rig.frames( 3, true );
		assertEquals( "seeded", none + 1, s.looksCount() );
		Buff.prolong( r, Surging.SurgeTracker.class, Surging.SurgeTracker.DURATION );
		rig.frames( 3, true );
		assertEquals( "and surging", none + 2, s.looksCount() );
		r.buff( Parasitic.ParasiticSeed.class ).detach();
		rig.frames( 30, true );
		assertEquals( "the seed gone", none + 1, s.looksCount() );
		r.buff( Surging.SurgeTracker.class ).detach();
		rig.frames( 30, true );
		assertEquals( "the surge gone", none, s.looksCount() );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			Buff.affect( r, Parasitic.ParasiticSeed.class ).set( 5f, 1 );
			Buff.prolong( r, Surging.SurgeTracker.class, Surging.SurgeTracker.DURATION );
			rig.frames( 10, true );
			Buff.detach( r, Parasitic.ParasiticSeed.class );
			Buff.detach( r, Surging.SurgeTracker.class );
			rig.frames( 30, true );
		} ) );
	}

	// ================================================================== the census

	/** Every file the rows touched: its calls of the game's Random, and its legacy pictures that draw
	 *  it (a burst, a pour, a start, a splash, a lightning), at 37d849aba. */
	static final Object[][] CENSUS = {
			{ "actors/Char.java", 14, 4 },   //15 till the English-only "dooked" miss word (a 1-in-10 roll) became the miss card
			{ "actors/buffs/Burning.java", 6, 2 },
			{ "effects/Pushing.java", 0, 1 },
			{ "items/armor/curses/AntiEntropy.java", 1, 1 },
			{ "items/armor/curses/Bulk.java", 0, 1 },
			{ "items/armor/curses/Corrosion.java", 1, 1 },
			{ "items/armor/curses/Overgrowth.java", 1, 1 },
			{ "items/armor/curses/Stench.java", 1, 0 },
			{ "items/armor/glyphs/Adaptation.java", 1, 0 },
			{ "items/armor/glyphs/Bounce.java", 1, 0 },
			{ "items/armor/glyphs/Camouflage.java", 0, 0 },
			{ "items/armor/glyphs/Crystal.java", 1, 0 },
			{ "items/armor/glyphs/Entanglement.java", 1, 1 },
			{ "items/armor/glyphs/Potential.java", 1, 1 },
			{ "items/armor/glyphs/Purity.java", 1, 1 },
			{ "items/armor/glyphs/Repulsion.java", 1, 0 },
			{ "items/armor/glyphs/Satisfying.java", 1, 1 },
			{ "items/armor/glyphs/Stone.java", 0, 0 },
			{ "items/armor/glyphs/Viscosity.java", 0, 0 },
			{ "items/wands/WandOfBlastWave.java", 2, 0 },
			{ "items/weapon/curses/Dazzling.java", 1, 0 },
			{ "items/weapon/curses/Explosive.java", 1, 2 },
			{ "items/weapon/curses/Polarized.java", 1, 0 },
			{ "items/weapon/curses/Pressurized.java", 1, 0 },
			{ "items/weapon/enchantments/Ancient.java", 3, 0 },
			{ "items/weapon/enchantments/Blazing.java", 2, 1 },
			{ "items/weapon/enchantments/Blocking.java", 1, 1 },
			{ "items/weapon/enchantments/Blooming.java", 3, 1 },
			{ "items/weapon/enchantments/Chilling.java", 1, 1 },
			{ "items/weapon/enchantments/Crystal.java", 1, 1 },
			{ "items/weapon/enchantments/Elastic.java", 1, 0 },
			{ "items/weapon/enchantments/Holy.java", 4, 0 },
			{ "items/weapon/enchantments/Kinetic.java", 0, 0 },
			{ "items/weapon/enchantments/LokisPoison.java", 2, 0 },
			{ "items/weapon/enchantments/NeptuneShock.java", 2, 2 },
			{ "items/weapon/enchantments/Paralysis.java", 2, 0 },
			{ "items/weapon/enchantments/Parasitic.java", 2, 1 },
			{ "items/weapon/enchantments/Poison.java", 1, 0 },
			{ "items/weapon/enchantments/Shocking.java", 1, 2 },
			{ "items/weapon/enchantments/Surging.java", 1, 1 },
			{ "items/weapon/enchantments/Venomous.java", 1, 1 },
			//the area's module (new since 37d849aba: as the core left it) and the rows' own, never a
			//call of the game's Random
			{ "effects/fx/modules/EnchantArea.java", 0, 0 },
			{ "effects/fx/enchant/rows/ElementRows.java", 0, 0 },
			{ "effects/fx/enchant/rows/ElementExhibits.java", 0, 0 } };

	static final Pattern RANDOM = Pattern.compile( "(?<![A-Za-z0-9_])Random\\." ),
			LEGACY = Pattern.compile( "\\.burst\\(|\\.start\\(|\\.startDelayed\\(|Splash\\.at\\(|new Lightning\\(|\\.pour\\(" );

	@Test
	public void everyTouchedFileCallsTheGamesRandomAsOftenAsItDid() throws Exception {
		Map<String, int[]> found = new LinkedHashMap<>();
		for (Object[] row : CENSUS){
			String src = source( (String) row[0] );
			int[] n = { matches( RANDOM, src ), matches( LEGACY, src ) };
			found.put( (String) row[0], n );
			assertEquals( row[0] + ": the game's Random", (int) (Integer) row[1], n[0] );
			if (((String) row[0]).contains( "/rows/" )){
				assertFalse( row[0] + " imports the game's Random", src.contains( "import com.watabou.utils.Random;" ) );
			} else {
				assertEquals( row[0] + ": its legacy pictures", (int) (Integer) row[2], n[1] );
			}
		}
		assertNotNull( found );
	}

	private static int matches( Pattern p, String s ){
		Matcher m = p.matcher( s );
		int n = 0;
		while (m.find()) n++;
		return n;
	}

	static String source( String path ) throws Exception {
		File f = new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/" + path );
		if (!f.exists()) f = new File( "core/src/main/java/xyz/gabriwar/warpedpixeldungeon/" + path );
		assertTrue( "no source " + f.getAbsolutePath(), f.exists() );
		return new String( Files.readAllBytes( f.toPath() ), "UTF-8" );
	}
}
