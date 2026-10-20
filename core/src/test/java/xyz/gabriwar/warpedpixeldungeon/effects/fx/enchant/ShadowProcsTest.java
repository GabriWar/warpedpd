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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Displacement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Metabolism;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Multiplicity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Affection;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Flow;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirrorimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirroring;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Phasing;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Siphoning;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Swiftness;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Friendly;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wayward;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wondrous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Horror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Luck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vorpal;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Chainsaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.AresSword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The blood, shadow, fortune and mirage rows (ShadowRows), each from its real proc, glyph proc,
 * curse proc or speed line on headless dummies: the game's Random is the same after it with its
 * pictures drawn at once, with no layers at all, with every picture handed to the render thread
 * from the actor's (the actor thread's path) and with its pictures switched off (no light layer:
 * every enchantment entry draws nothing then); each row is reached and sends its look (co-op's
 * event), a vestige's as an echo, the deferred sites in Char too; each row's own quads keep to
 * its tier's cap; a walker's steps answer at most every 0.2 s; and every file these rows touch
 * holds as many calls of the game's Random as it did before them.
 */
public class ShadowProcsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private EnchantFxTest.Rig rig;
	private Application savedApp;
	private String savedVersion;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		Sting.out = ( sample, at, volume, pitch ) -> { };
		savedApp = Gdx.app;
		savedVersion = Game.version;
		Game.version = "test";
		Actor.clear();
		PathFinder.setMapSize( EnchantFxTest.W, EnchantFxTest.W );
		NetVisuals.clear();
	}

	@After
	public void tearDown() throws Exception {
		TestParty.leave();
		NetVisuals.clear();
		Gdx.app = savedApp;
		Game.version = savedVersion;
		Actor.clear();
		rig.down();
	}

	static int at( int x, int y ){
		return EnchantFxTest.at( x, y );
	}

	// ------------------------------------------------------------------ the dummies

	/** A rat with no idle icon over it (the icons are the scene's). */
	public static class Dummy extends Rat {
		@Override
		public boolean showsUnaware(){
			return false;
		}
	}

	/** A dummy that takes its death as a blow and stays (no loot, no score). */
	public static class Sturdy extends Dummy {
		@Override
		public void die( Object cause ){ }
	}

	/** The hero, whose interruption has no scene's keys to reset. */
	public static class Quiet extends Hero {
		@Override
		public void interrupt(){ }
	}

	/** A hero no blow lands on. */
	public static class Dodger extends Quiet {
		@Override
		public int defenseSkill( Char enemy ){
			return INFINITE_EVASION;
		}
	}

	/** Who stands where: the hero (h, a rat's sprite standing in), a foe (f), a rat striking (a),
	 *  a rat watching (w); a sword (s) and a cloth armour (c). */
	final class World {
		Hero h;
		Mob f, a, w;
		Weapon s = new Sword();
		Armor c = new ClothArmor();

		World( Hero hero ){
			h = hero;
			Dungeon.hero = h;
			place( h, at( 5, 6 ) );
			h.HP = h.HT = 100;
			f = place( new Sturdy(), at( 6, 6 ) );
			a = place( new Dummy(), at( 6, 7 ) );
			w = place( new Dummy(), at( 4, 8 ) );
		}

		<T extends Char> T put( T ch, int cell ){
			return place( ch, cell );
		}
	}

	/** A sprite for the hero (a mob's would read him as a mob): the rat's frames. */
	public static class Stand extends CharSprite {
		public Stand(){
			super();
			texture( Assets.Sprites.RAT );
			TextureFilm frames = new TextureFilm( texture, 16, 15 );
			idle = new Animation( 2, true );
			idle.frames( frames, 0, 0, 0, 1 );
			run = new Animation( 10, true );
			run.frames( frames, 6, 7, 8, 9, 10 );
			attack = new Animation( 15, false );
			attack.frames( frames, 2, 3, 4, 5, 0 );
			die = new Animation( 10, false );
			die.frames( frames, 11, 12, 13, 14 );
			play( idle );
		}
	}

	<T extends Char> T place( T ch, int cell ){
		ch.pos = cell;
		if (ch instanceof Mob){
			((Mob) ch).state = ((Mob) ch).PASSIVE;
			ch.HP = ch.HT = 50;
		}
		CharSprite sp = ch instanceof Mob ? new RatSprite() : new Stand();
		sp.ch = ch;
		ch.sprite = sp;
		rig.stage.add( sp );
		sp.place( cell );
		sp.visible = true;
		return ch;
	}

	//a heap with a sprite on the cell, as the scene would have one (a drop's `.sprite.drop()`)
	static void heapOn( int cell ){
		Heap heap = new Heap();
		heap.pos = cell;
		ItemSprite s = heap.sprite = new ItemSprite();
		s.link( heap );
		Dungeon.level.heaps.put( cell, heap );
	}

	//the cell a teleport leaves out of the hero's sight (its puff of light is a CellEmitter's, the
	//scene's own emitter)
	static void unseen( Char ch ){
		Dungeon.level.heroFOV[ch.pos] = false;
		//and in the actors, so a teleport does not land it on its own cell
		Actor.add( ch );
	}

	interface Scene {
		void play( World w ) throws Exception;
	}

	/** A row: its look's class, what plays it, whether its game code draws the same with no layers
	 *  as with them (Vorpal's blood cone, Splash, draws only with an effects layer: such a row is
	 *  held to the same draws with its pictures switched off, the light layer gone, instead). */
	static final class Row {
		final String name;
		final Class<?> look;
		final Scene scene;
		final boolean bareSame;
		final boolean hero;
		//whether its site draws without an enchantment entry (so sends nothing to guests)
		final boolean eventless;

		Row( String name, Class<?> look, Scene scene, boolean bareSame, boolean hero ){
			this( name, look, scene, bareSame, hero, false );
		}

		Row( String name, Class<?> look, Scene scene, boolean bareSame, boolean hero, boolean eventless ){
			this.name = name;
			this.look = look;
			this.scene = scene;
			this.bareSame = bareSame;
			this.hero = hero;
			this.eventless = eventless;
		}
	}

	static final List<Row> ROWS = new ArrayList<>();

	static void row( Class<?> look, Scene scene ){
		ROWS.add( new Row( look.getSimpleName(), look, scene, true, false ) );
	}

	static {
		row( Eldritch.class, w -> {
			w.w.fieldOfView = new boolean[Dungeon.level.length()];
			Arrays.fill( w.w.fieldOfView, true );
			Actor.add( w.f );
			Actor.add( w.w );
			new Eldritch().proc( w.s, w.h, w.f, 5 );
		} );
		row( Lucky.class, w -> {
			new Lucky().proc( w.s, w.h, w.f, 5 );
			new Lucky().proc( w.s, w.h, w.f, 5 );
		} );
		row( Unstable.class, w -> new Unstable().proc( w.s, w.h, w.f, 5 ) );
		ROWS.add( new Row( "Vorpal", Vorpal.class, w -> {
			new Vorpal().proc( w.s, w.h, w.f, 5 );
			w.f.damage( 5, w.h );
		}, false, false ) );
		//the tracker set (its tendrils); its conversion as the foe dies needs the scene to give the
		//corrupted copy a sprite (Corruption.corruptionHeal): its pictures are played below
		ROWS.add( new Row( "Corrupting", Corrupting.class, w -> new Corrupting().proc( w.s, w.h, w.f, 5 ), true, false, true ) );
		row( Grim.class, w -> {
			new Grim().proc( w.s, w.h, w.f, 5 );
			w.f.HP = 3;
			w.f.damage( 1, w.h );
		} );
		row( Vampiric.class, w -> {
			w.h.HP = 20;
			new Vampiric().proc( w.s, w.h, w.f, 10 );
		} );
		row( Horror.class, w -> new Horror().proc( w.s, w.h, w.f, 5 ) );
		row( JupitersHorror.class, w -> new JupitersHorror().proc( w.s, w.h, w.f, 5 ) );
		row( Hunting.class, w -> {
			heapOn( w.f.pos );
			new Hunting().proc( w.s, w.h, w.f, 100 );
		} );
		row( Midas.class, w -> {
			heapOn( w.f.pos );
			new Midas().proc( w.s, w.h, w.f, 5 );
			new Midas().proc( w.s, w.h, w.f, 999 );
		} );
		row( Vicious.class, w -> {
			for (int i = 0; i < 4; i++) new Vicious().proc( w.s, w.a, w.f, 5 );
		} );
		row( Luck.class, w -> new Luck().proc( w.s, w.a, w.f, 1 ) );
		row( Slashing.class, w -> new Slashing().proc( w.s, w.a, w.f, 1 ) );
		row( CromLuck.class, w -> new CromLuck().proc( w.s, w.a, w.f, 1 ) );
		row( AresLeech.class, w -> {
			AresSword sword = new AresSword();
			sword.level( 2 );
			w.h.HP = 20;
			Dungeon.level.mobs.add( (Mob) w.f );
			Dungeon.level.mobs.add( w.a );
			new AresLeech().proc( sword, w.h, w.f, 12 );
		} );
		row( BuzzSaw.class, w -> {
			Waterskin skin = new Waterskin();
			skin.fill();
			Chainsaw saw = new Chainsaw();
			saw.turnedOn = true;
			w.h.belongings.backpack.items.add( skin );
			w.h.belongings.backpack.items.add( saw );
			new BuzzSaw().proc( saw, w.h, w.f, 5 );
		} );
		row( Nomnom.class, w -> {
			w.a.HP = 10;
			new Nomnom().proc( w.s, w.a, w.f, 8 );
		} );
		row( Osmose.class, w -> {
			w.h.MP = 0;
			w.h.MT = 20;
			new Osmose().proc( w.s, w.h, w.f, 10 );
		} );

		row( Affection.class, w -> new Affection().proc( w.c, w.f, w.h, 5 ) );
		ROWS.add( new Row( "Afterimage", Afterimage.class, w -> {
			w.h.belongings.armor = (Armor) new ClothArmor().inscribe( new Afterimage() );
			w.a.attack( w.h );
		}, true, true ) );
		row( Flow.class, w -> {
			Mob r = w.put( new Dummy(), at( 10, 6 ) );
			Flow.speedBoost( r, 0 );
		} );
		row( Swiftness.class, w -> Swiftness.speedBoost( w.w, 0 ) );
		row( Phasing.class, w -> {
			unseen( w.f );
			w.f.HP = 10;
			new Phasing().proc( w.c, w.a, w.f, 5 );
		} );
		row( Siphoning.class, w -> {
			w.h.HP = 50;
			new Siphoning().proc( w.c, w.a, w.h, 5 );
		} );
		row( Thorns.class, w -> new Thorns().proc( w.c, w.a, w.h, 5 ) );

		row( Annoying.class, w -> new Annoying().proc( w.s, w.h, w.f, 5 ) );
		row( Displacing.class, w -> {
			unseen( w.f );
			new Displacing().proc( w.s, w.h, w.f, 5 );
		} );
		row( Friendly.class, w -> new Friendly().proc( w.s, w.h, w.f, 5 ) );
		row( Sacrificial.class, w -> new Sacrificial().proc( w.s, w.h, w.f, 5 ) );
		row( Wayward.class, w -> new Wayward().proc( w.s, w.h, w.f, 5 ) );
		//the cursed wand's own effects (a splash among them) draw only with an effects layer
		ROWS.add( new Row( "Wondrous", Wondrous.class, w -> new Wondrous().proc( w.s, w.h, w.f, 5 ), false, false ) );
		row( Displacement.class, w -> {
			unseen( w.f );
			new Displacement().proc( w.c, w.a, w.f, 5 );
		} );
		row( Metabolism.class, w -> {
			w.h.HP = 50;
			new Metabolism().proc( w.c, w.a, w.h, 5 );
		} );
	}

	// ------------------------------------------------------------------ the runs

	static final int INLINE = 0, BARE = 1, DEFERRED = 2, OFF = 3;

	//a fresh world for a run, its pictures drawn as the mode has it
	private World world( int mode, boolean dodger ){
		rig.layers( mode != BARE );
		if (mode == OFF){
			Group[] noLight = rig.layers.clone();
			noLight[GameScene.Layer.LIGHTS.ordinal()] = null;
			GameScene.useLayersForTests( noLight );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.sceneCreated();
		}
		rig.stage.clear();
		Actor.clear();
		Dungeon.level.mobs.clear();
		Dungeon.level.heaps.clear();
		Arrays.fill( Dungeon.level.heroFOV, true );
		return new World( dodger ? new Dodger() : new Quiet() );
	}

	//the scene played on the actor's thread, its pictures handed over and run after it here
	private void deferred( Scene scene, World w ) throws Exception {
		final List<Runnable> posted = new ArrayList<>();
		final Application app = Gdx.app;
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(), new Class<?>[]{ Application.class },
				( p, m, args ) -> {
					if (m.getName().equals( "postRunnable" )){
						synchronized (posted){
							posted.add( (Runnable) args[0] );
						}
						return null;
					}
					return app != null ? m.invoke( app, args ) : null;
				} );
		final Throwable[] failed = new Throwable[1];
		try {
			Thread actor = new Thread( () -> {
				try {
					scene.play( w );
				} catch (Throwable t){
					failed[0] = t;
				}
			} );
			actor.start();
			actor.join();
		} finally {
			Gdx.app = app;
		}
		if (failed[0] != null) throw new AssertionError( failed[0] );
		for (int i = 0; i < posted.size(); i++) posted.get( i ).run();
	}

	//the game's Random's next number after the row's scene played in a mode from a seed
	private long run( Row r, int seed, int mode ) throws Exception {
		World w = world( mode, r.hero );
		Game.timeTotal += 3f;
		Random.pushGenerator( seed );
		try {
			if (mode == DEFERRED) deferred( r.scene, w );
			else r.scene.play( w );
			peak = 0;
			for (int f = 0; f < 20; f++){
				if (mode != BARE) peak = Math.max( peak, onLayers() );
				rig.frames( 1, true );
			}
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}

	static final int SEEDS = 24;

	//the most pictures alive on the layers over the last run's frames
	private int peak;

	private int onLayers(){
		java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> alive = new java.util.IdentityHashMap<>();
		for (Group g : rig.layers) if (g != null) collectLayer( g, alive );
		return alive.size();
	}

	@Test
	public void everyRowDrawsTheSameOfTheGamesRandomWithItsPicturesWithoutThemAndFromTheActorsThread() throws Exception {
		for (Row r : ROWS){
			boolean pictured = false;
			for (int seed = 1; seed <= SEEDS; seed++){
				long drawn = run( r, seed, INLINE );
				int withPictures = peak;
				long off = run( r, seed, OFF );
				pictured |= withPictures > peak;
				long late = run( r, seed, DEFERRED );
				assertEquals( r.name + " seed " + seed + ": pictures off", off, drawn );
				assertEquals( r.name + " seed " + seed + ": from the actor's thread", drawn, late );
				if (r.bareSame) assertEquals( r.name + " seed " + seed + ": no layers", drawn, run( r, seed, BARE ) );
			}
			assertTrue( r.name + " drew its pictures at its site", pictured );
		}
	}

	// ------------------------------------------------------------------ reached, and echoes

	//the looks' events a scene sent as a host: (kind, numbers...)
	private List<int[]> sent( Row r, int seed, Scene scene ) throws Exception {
		World w = world( INLINE, r.hero );
		Game.timeTotal += 3f;
		NetVisuals.clear();
		Random.pushGenerator( seed );
		try {
			scene.play( w );
			rig.frames( 3, true );
		} finally {
			Random.popGenerator();
		}
		List<int[]> out = new ArrayList<>();
		JSONArray a = NetVisuals.drainEvents();
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

	//the first PROC or GLYPH event of a look a scene sends over the seeds, null if none
	private int[] firstOf( Row r, Class<?> look, Scene scene ) throws Exception {
		int code = EnchantLooks.of( look ).code;
		for (int seed = 1; seed <= 80; seed++){
			for (int[] e : sent( r, seed, scene )){
				if ((e[0] == EnchantNet.PROC || e[0] == EnchantNet.GLYPH) && e[1] == code) return e;
			}
		}
		return null;
	}

	//whether a row's scene leaves pictures alive on the layers for one of the seeds
	private boolean drawsAtItsSite( Row r ) throws Exception {
		for (int seed = 1; seed <= 80; seed++){
			World w = world( INLINE, r.hero );
			Game.timeTotal += 3f;
			Random.pushGenerator( seed );
			try {
				r.scene.play( w );
			} finally {
				Random.popGenerator();
			}
			rig.frames( 3, true );
			if (onLayers() > 0) return true;
		}
		return false;
	}

	private static void collectLayer( Group g, java.util.IdentityHashMap<com.watabou.noosa.Gizmo, Boolean> into ){
		for (com.watabou.noosa.Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof Group) collectLayer( (Group) m, into );
			else if (m instanceof com.watabou.noosa.Visual) into.put( m, Boolean.TRUE );
		}
	}

	//a PROC event's flags (index 6) and tier (7); a GLYPH's flags (6), moment (7), tier (8)
	static int flagsOf( int[] e ){
		return e[6];
	}

	@Test
	public void everyRowIsReachedAndAVestigeEchoes() throws Exception {
		TestParty.join( new Hero() );
		for (Row r : ROWS){
			if (r.eventless){
				assertTrue( r.name + " draws at its site", drawsAtItsSite( r ) );
				continue;
			}
			int[] e = firstOf( r, r.look, r.scene );
			assertNotNull( r.name + " plays its look", e );
			assertEquals( r.name + " no echo of its own", 0, flagsOf( e ) & EnchantFx.ECHO );
			boolean curse = EnchantLooks.of( r.look ).curse;
			assertEquals( r.name + " hexed as a curse", curse, (flagsOf( e ) & EnchantFx.CURSE) != 0 );
		}
		//the eleven a vestige can be, each echoing from a legendary sword's vestige
		Map<Class<? extends Weapon.Enchantment>, Scene> echoes = new LinkedHashMap<>();
		echoes.put( Eldritch.class, w -> {
			w.w.fieldOfView = new boolean[Dungeon.level.length()];
			Arrays.fill( w.w.fieldOfView, true );
			Actor.add( w.w );
			vestige( w, new Eldritch() ).proc( w.s, w.h, w.f, 5 );
		} );
		echoes.put( Lucky.class, w -> vestige( w, new Lucky() ).proc( w.s, w.h, w.f, 5 ) );
		echoes.put( Vorpal.class, w -> {
			vestige( w, new Vorpal() ).proc( w.s, w.h, w.f, 5 );
			w.f.damage( 5, w.h );
		} );
		echoes.put( Luck.class, w -> vestige( w, new Luck() ).proc( w.s, w.a, w.f, 1 ) );
		echoes.put( Slashing.class, w -> vestige( w, new Slashing() ).proc( w.s, w.a, w.f, 1 ) );
		echoes.put( Midas.class, w -> {
			heapOn( w.f.pos );
			vestige( w, new Midas() ).proc( w.s, w.h, w.f, 999 );
		} );
		echoes.put( Hunting.class, w -> {
			heapOn( w.f.pos );
			vestige( w, new Hunting() ).proc( w.s, w.h, w.f, 5 );
		} );
		echoes.put( Grim.class, w -> {
			vestige( w, new Grim() ).proc( w.s, w.h, w.f, 5 );
			w.f.HP = 3;
			w.f.damage( 1, w.h );
		} );
		echoes.put( Vampiric.class, w -> {
			w.h.HP = 20;
			vestige( w, new Vampiric() ).proc( w.s, w.h, w.f, 10 );
		} );
		echoes.put( Vicious.class, w -> {
			for (int i = 0; i < 4; i++) vestige( w, new Vicious() ).proc( w.s, w.a, w.f, 5 );
		} );
		assertEquals( 10, echoes.size() );
		//the eleventh, Corrupting: its tracker keeps the vestige's echo for the conversion, whose
		//rising is an echo then
		World cw = world( INLINE, false );
		Weapon.Enchantment cv = vestige( cw, new Corrupting() );
		boolean marked = false;
		for (int seed = 1; seed <= 80 && !marked; seed++){
			Random.pushGenerator( seed );
			try {
				cv.proc( cw.s, cw.h, cw.f, 5 );
			} finally {
				Random.popGenerator();
			}
			Buff t = cw.f.buff( Corrupting.CorruptingTracker.class );
			if (t != null){
				java.lang.reflect.Field echo = Corrupting.CorruptingTracker.class.getDeclaredField( "echo" );
				echo.setAccessible( true );
				assertTrue( "the tracker keeps the echo", echo.getBoolean( t ) );
				marked = true;
			}
		}
		assertTrue( "a vestige's corruption marked its foe", marked );
		NetVisuals.clear();
		ShadowRows.corruptRise( cw.a, true );
		boolean risen = false;
		JSONArray ev = NetVisuals.drainEvents();
		for (int i = 0; ev != null && i < ev.length(); i++){
			JSONArray n = ev.getJSONObject( i ).getJSONArray( "i" );
			if (n.getInt( 0 ) == EnchantLooks.of( Corrupting.class ).code && (n.getInt( 5 ) & EnchantFx.ECHO) != 0) risen = true;
		}
		assertTrue( "the risen ally's look an echo", risen );
		Row any = new Row( "echo", null, null, true, false );
		for (Map.Entry<Class<? extends Weapon.Enchantment>, Scene> en : echoes.entrySet()){
			int[] e = firstOf( any, en.getKey(), en.getValue() );
			assertNotNull( en.getKey().getSimpleName() + " as a vestige plays", e );
			assertTrue( en.getKey().getSimpleName() + " as a vestige echoes", (flagsOf( e ) & EnchantFx.ECHO) != 0 );
		}

		//the glyphs of these rows an armour's vestige can carry, each echoing from a legendary
		//armour's vestige (Mirror image's and Mirroring's with their images, below)
		Map<Class<? extends Armor.Glyph>, Scene> glyphEchoes = new LinkedHashMap<>();
		glyphEchoes.put( Affection.class, w -> glyphVestige( w, new Affection() ).proc( w.c, w.f, w.h, 5 ) );
		glyphEchoes.put( Phasing.class, w -> {
			unseen( w.f );
			w.f.HP = 10;
			glyphVestige( w, new Phasing() ).proc( w.c, w.a, w.f, 5 );
		} );
		glyphEchoes.put( Siphoning.class, w -> {
			w.h.HP = 50;
			glyphVestige( w, new Siphoning() ).proc( w.c, w.a, w.h, 5 );
		} );
		glyphEchoes.put( Thorns.class, w -> glyphVestige( w, new Thorns() ).proc( w.c, w.a, w.h, 5 ) );
		for (Map.Entry<Class<? extends Armor.Glyph>, Scene> en : glyphEchoes.entrySet()){
			assertTrue( en.getKey().getSimpleName() + " can be a vestige", Quality.glyphVestigePool().contains( en.getKey() ) );
			int[] e = firstOf( any, en.getKey(), en.getValue() );
			assertNotNull( en.getKey().getSimpleName() + " as a vestige plays", e );
			assertTrue( en.getKey().getSimpleName() + " as a vestige echoes", (flagsOf( e ) & EnchantFx.ECHO) != 0 );
		}
		for (Class<?> c : new Class<?>[]{ Mirrorimage.class, Mirroring.class }){
			assertTrue( c.getSimpleName() + " can be a vestige", Quality.glyphVestigePool().contains( c ) );
			assertTrue( c.getSimpleName() + " passes its echo",
					ElementProcsTest.source( "items/armor/glyphs/" + c.getSimpleName() + ".java" ).contains( "Quality.isVestige( armor, this )" ) );
		}

		//the deferred sites: Grim's execute always the killing look (hollow as an echo, above);
		//Vorpal's turned bleed; Afterimage on its wearer's dodge, a quiet moment of its own
		int[] grim = firstOf( any, Grim.class, ROWS.get( index( "Grim" ) ).scene );
		assertEquals( "the execute kills", EnchantFx.FINISHER, grim[7] );
		int[] dodge = firstOf( ROWS.get( index( "Afterimage" ) ), Afterimage.class, ROWS.get( index( "Afterimage" ) ).scene );
		assertEquals( EnchantNet.GLYPH, dodge[0] );
		assertEquals( "a dodge", EnchantFx.DODGE, dodge[7] );
	}

	//the enchantment set as the vestige of the world's sword, a legendary one now
	static Weapon.Enchantment vestige( World w, Weapon.Enchantment e ){
		Sword s = new Sword();
		Quality.roll( s, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		s.quality.vestige = e;
		w.s = s;
		assertTrue( Quality.isVestige( s, e ) );
		return e;
	}

	//the glyph set as the vestige of the world's armour, a legendary cloth one now with no glyph
	//of its own
	static Armor.Glyph glyphVestige( World w, Armor.Glyph g ){
		ClothArmor c = new ClothArmor();
		Quality.roll( c, Rarity.LEGENDARY, ItemType.GAMMA, 0 );
		c.inscribe( null );
		c.quality.vestige = g;
		w.c = c;
		assertTrue( Quality.isVestige( c, g ) );
		return g;
	}

	static int index( String name ){
		for (int i = 0; i < ROWS.size(); i++) if (ROWS.get( i ).name.equals( name )) return i;
		throw new AssertionError( name );
	}

	// ------------------------------------------------------------------ the images (no headless scene for them)

	//Mirror image, Mirroring and Multiplicity make a mob and call appear on its sprite, which only
	//the game's scene gives it: their pictures are played here from the same calls the procs make,
	//with images standing where the procs would put them
	@Test
	public void theImagesPicturesDrawNoneOfTheGamesRandomAndKeepToTheirCaps() throws Exception {
		for (int mode : new int[]{ INLINE, BARE, DEFERRED }){
			World w = world( mode, false );
			final Set<Mob> had = ShadowRows.images();
			Mob image = place( new MirrorImage(), at( 5, 5 ) );
			Dungeon.level.mobs.add( image );
			Scene play = x -> {
				ShadowRows.mirrorimage( x.h, x.a, 0, 0, had );
				ShadowRows.mirroring( x.h, x.a, 0, 0, had );
				EnchantFx.ward( EnchantLooks.of( Multiplicity.class ), x.h, x.a, 0.45f, 0, EnchantFx.CURSE, ShadowRows.multiplied( at( 4, 6 ) ) );
			};
			int draws = EnchantFxTest.draws( () -> {
				try {
					if (mode == DEFERRED) deferred( play, w );
					else play.play( w );
				} catch (Exception e){
					throw new AssertionError( e );
				}
				rig.frames( 20, true );
			} );
			assertEquals( "mode " + mode, 0, draws );
		}
		World w = world( INLINE, false );
		Set<Mob> had = ShadowRows.images();
		Mob image = place( new MirrorImage(), at( 5, 5 ) );
		Dungeon.level.mobs.add( image );
		Game.timeTotal += 3f;
		int[] m = rig.made( () -> ShadowRows.mirrorimage( w.h, w.a, 0, 0, had ), 30 );
		assertTrue( "mirror image: " + m[0], m[0] > 0 && m[0] <= EnchantFx.CAP[EnchantFx.STRONG] );
		Game.timeTotal += 3f;
		m = rig.made( () -> ShadowRows.mirroring( w.h, w.a, 0, 0, had ), 30 );
		assertTrue( "mirroring: " + m[0], m[0] > 0 && m[0] <= EnchantFx.CAP[EnchantFx.STRONG] );
		Game.timeTotal += 3f;
		m = rig.made( () -> EnchantFx.ward( EnchantLooks.of( Multiplicity.class ), w.h, w.a, 0.45f, 0, EnchantFx.CURSE,
				ShadowRows.multiplied( at( 4, 6 ) ) ), 30 );
		assertTrue( "multiplicity: " + m[0], m[0] > 0 && m[0] <= EnchantFx.CAP[EnchantFx.MID] );
		//a vestige's: its echo, within an echo's cap
		Game.timeTotal += 3f;
		m = rig.made( () -> ShadowRows.mirrorimage( w.h, w.a, 0, EnchantFx.ECHO, had ), 30 );
		assertTrue( "mirror image's echo: " + m[0], m[0] > 0 && m[0] <= EnchantFx.ECHO_CAP );
		Game.timeTotal += 3f;
		m = rig.made( () -> ShadowRows.mirroring( w.h, w.a, 0, EnchantFx.ECHO, had ), 30 );
		assertTrue( "mirroring's echo: " + m[0], m[0] > 0 && m[0] <= EnchantFx.ECHO_CAP );
		//no image made: nothing
		Game.timeTotal += 3f;
		m = rig.made( () -> ShadowRows.mirrorimage( w.h, w.a, 0, 0, ShadowRows.images() ), 30 );
		assertEquals( 0, m[0] );
	}

	// ------------------------------------------------------------------ caps

	/** A row's twist and the flags it is drawn with, played as a proc (or a ward) on two rats. */
	static final class Twisted {
		final Class<?> look;
		final EnchantFx.Twist twist;
		final boolean ward, echoes;

		Twisted( Class<?> look, EnchantFx.Twist twist, boolean ward, boolean echoes ){
			this.look = look;
			this.twist = twist;
			this.ward = ward;
			this.echoes = echoes;
		}
	}

	@Test
	public void everyRowKeepsToItsTiersCap(){
		World w = world( INLINE, false );
		CharSprite a = w.h.sprite, d = w.f.sprite, wet = place( new Sturdy(), at( 10, 6 ) ).sprite;
		List<Char> witnesses = new ArrayList<>();
		witnesses.add( w.w );
		witnesses.add( w.a );
		List<Twisted> all = Arrays.asList(
				new Twisted( Eldritch.class, ShadowRows.eldritch( witnesses ), false, true ),
				new Twisted( Lucky.class, ShadowRows.LUCKY, false, true ),
				new Twisted( Unstable.class, ShadowRows.DICE, false, false ),
				new Twisted( Vorpal.class, ShadowRows.VORPAL, false, true ),
				new Twisted( Grim.class, ShadowRows.GRIM, false, true ),
				new Twisted( Corrupting.class, ShadowRows.RISE, false, true ),
				new Twisted( Vampiric.class, ShadowRows.VAMPIRIC, false, true ),
				new Twisted( Horror.class, ShadowRows.HORROR, false, false ),
				new Twisted( JupitersHorror.class, ShadowRows.JUPITERS, false, false ),
				new Twisted( Hunting.class, ShadowRows.HUNTING, false, true ),
				new Twisted( Midas.class, ShadowRows.midas( true ), false, true ),
				new Twisted( Vicious.class, ShadowRows.VICIOUS, false, true ),
				new Twisted( Vicious.class, ShadowRows.BACKFIRE, false, true ),
				new Twisted( Luck.class, null, false, true ),
				new Twisted( Slashing.class, ShadowRows.SLASHING, false, true ),
				new Twisted( CromLuck.class, ShadowRows.CROM, false, false ),
				new Twisted( AresLeech.class, ShadowRows.ARES_RING, false, false ),
				new Twisted( BuzzSaw.class, ShadowRows.buzz( 2 ), false, false ),
				new Twisted( Nomnom.class, ShadowRows.NOMNOM, false, false ),
				new Twisted( Osmose.class, ShadowRows.osmose( w.h ), false, false ),
				new Twisted( Thorns.class, ShadowRows.THORNS, false, true ),
				new Twisted( Annoying.class, ShadowRows.ANNOYING, false, false ),
				new Twisted( Displacing.class, ShadowRows.displaced( at( 3, 3 ) ), false, false ),
				new Twisted( Friendly.class, ShadowRows.FRIENDLY, false, false ),
				new Twisted( Sacrificial.class, ShadowRows.SACRIFICIAL, false, false ),
				new Twisted( Wayward.class, ShadowRows.WAYWARD, false, false ),
				new Twisted( Wondrous.class, ShadowRows.WONDROUS, false, false ),
				new Twisted( Affection.class, null, true, true ),
				new Twisted( Siphoning.class, null, true, true ),
				new Twisted( Phasing.class, ShadowRows.phase( at( 3, 3 ) ), true, true ),
				new Twisted( Displacement.class, ShadowRows.displacement( at( 3, 3 ) ), true, false ),
				new Twisted( Metabolism.class, ShadowRows.METABOLISM, true, false ),
				new Twisted( Multiplicity.class, ShadowRows.multiplied( at( 3, 3 ) ), true, false ) );
		for (Twisted t : all){
			EnchantLook l = EnchantLooks.of( t.look );
			int base = l.curse ? EnchantFx.CURSE : 0;
			for (CharSprite def : new CharSprite[]{ d, wet }){
				for (int tier = EnchantFx.WEAK; tier <= EnchantFx.FINISHER; tier++){
					for (int flags : t.echoes ? new int[]{ base, base | EnchantFx.ECHO } : new int[]{ base }){
						if (t.ward && tier == EnchantFx.FINISHER) continue;
						Game.timeTotal += 2f;
						final int tr = tier, fl = flags;
						int[] m = rig.made( () -> {
							if (t.ward) EnchantFx.runWard( l, def.ch, def, a.ch, a, EnchantFx.strengthOf( tr ), 1, fl, -1, tr, true, false, true,
									t.twist );
							else EnchantFx.stage( l, a, def, tr, 1, fl, t.twist );
						}, 75 );
						int cap = (fl & EnchantFx.ECHO) != 0 ? EnchantFx.ECHO_CAP : EnchantFx.CAP[tr];
						String what = l + " tier " + tr + " flags " + fl + (def == wet ? " on water" : "");
						assertTrue( what + ": " + m[0] + " quads", m[0] <= cap );
						assertTrue( what + ": something shows", m[0] > 0 );
						assertTrue( what + ": " + m[1] + " lights", m[1] <= EnchantFx.LIGHTS );
						assertTrue( what + ": " + m[2] + " decals", m[2] <= EnchantFx.DECALS );
						rig.frames( 90, false );
					}
				}
			}
		}
	}

	//a glyph's quiet moments with their twists: a step (the family's two motes, a ghost or a streak)
	//within MINIMAL_CAP, a ghost step's ghost among them; Afterimage's dodge within a weak answer's
	@Test
	public void theGlyphsQuietMomentsKeepToTheirCaps(){
		World w = world( INLINE, false );
		CharSprite a = w.a.sprite, d = w.f.sprite, wet = place( new Sturdy(), at( 10, 6 ) ).sprite;
		Object[][] quiet = {
				{ Flow.class, ShadowRows.FLOW, EnchantFx.STEP, EnchantFx.MINIMAL_CAP },
				{ Flow.class, ShadowRows.FLOW_GHOST, EnchantFx.STEP, EnchantFx.MINIMAL_CAP },
				{ Swiftness.class, ShadowRows.SWIFT, EnchantFx.STEP, EnchantFx.MINIMAL_CAP },
				{ Swiftness.class, ShadowRows.SWIFT_GHOST, EnchantFx.STEP, EnchantFx.MINIMAL_CAP },
				{ Afterimage.class, ShadowRows.dodge( a ), EnchantFx.DODGE, EnchantFx.CAP[EnchantFx.WEAK] },
				{ Afterimage.class, ShadowRows.dodge( null ), EnchantFx.DODGE, EnchantFx.CAP[EnchantFx.WEAK] },
		};
		for (Object[] q : quiet){
			final EnchantLook l = EnchantLooks.of( (Class<?>) q[0] );
			final EnchantFx.Twist twist = (EnchantFx.Twist) q[1];
			final int moment = (Integer) q[2], cap = (Integer) q[3];
			final boolean ghost = twist == ShadowRows.FLOW_GHOST || twist == ShadowRows.SWIFT_GHOST;
			for (CharSprite def : new CharSprite[]{ d, wet }){
				Runnable answer = () -> EnchantFx.runWard( l, def.ch, def, null, null, 0.2f, 0, 0, moment, EnchantFx.WEAK, true, false,
						true, twist );
				String what = l + " moment " + moment + (def == wet ? " on water" : "");
				Game.timeTotal += 2f;
				int[] m = rig.made( answer, 75 );
				assertTrue( what + ": " + m[0] + " quads", m[0] > 0 && m[0] <= cap );
				rig.frames( 90, false );
				//its ghost made, within the tally (it is drawn before the streak would be); a dodge's
				//beside the family's own
				Game.timeTotal += 2f;
				int live = xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage.live();
				answer.run();
				int now = xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage.live();
				if (moment == EnchantFx.DODGE) assertTrue( what + ": its ghost", now > live );
				else assertEquals( what + ": its ghost", live + (ghost ? 1 : 0), now );
				rig.frames( 90, false );
			}
		}
	}

	@Test
	public void eldritchOpensARedEyeUnderItsVioletRim(){
		EnchantLook e = EnchantLooks.of( Eldritch.class ), h = EnchantLooks.of( Horror.class );
		assertEquals( 0xA070E0, e.rim );
		assertEquals( ShadowRows.EYE_RED, e.eye );
		assertEquals( "another row's eye is its rim", h.rim, h.eye );
	}

	@Test
	public void aMarkedFoesLootIsAnEnchantmentEntry() throws Exception {
		TestParty.join( new Hero() );
		Row loot = new Row( "loot", null, null, true, false );
		int[] e = firstOf( loot, Lucky.class, w -> ShadowRows.luckyDrop( w.f.sprite ) );
		assertNotNull( "the loot's look goes to guests", e );
		assertEquals( EnchantNet.PROC, e[0] );
	}

	@Test
	public void theMomentsThatAreNoBlowKeepToTheirs() throws Exception {
		World w = world( INLINE, false );
		Game.timeTotal += 3f;
		int[] m = rig.made( () -> ShadowRows.corruptMark( w.f, false ), 30 );
		assertTrue( "tendrils: " + m[0], m[0] > 0 && m[0] <= EnchantFx.CAP[EnchantFx.WEAK] );
		m = rig.made( () -> ShadowRows.corruptCollapse( w.a.sprite, false ), 30 );
		assertTrue( "the ink collapsing: " + m[0], m[0] > 0 && m[0] <= EnchantFx.CAP[EnchantFx.FINISHER] );
		m = rig.made( () -> ShadowRows.corruptCollapse( w.a.sprite, true ), 30 );
		assertTrue( "an echo's: " + m[0], m[0] > 0 && m[0] <= EnchantFx.ECHO_CAP );
		m = rig.made( () -> ShadowRows.luckyDrop( w.f.sprite ), 30 );
		assertTrue( "the lucky fountain: " + m[0], m[0] > 0 && m[0] <= EnchantFx.CAP[EnchantFx.FINISHER] );
		m = rig.made( () -> ShadowRows.luckFizzle( w.f ), 30 );
		assertTrue( "the fizzle: " + m[0], m[0] > 0 && m[0] <= 2 );
		m = rig.made( () -> ShadowRows.huntMeat( w.f.pos ), 30 );
		assertTrue( "the meat's ring: " + m[0], m[0] > 0 && m[0] <= 2 );
		//a luck mark's star on the mark's orbit while it lasts, and only if the orbit is free
		Buff.affect( w.f, Lucky.LuckProc.class );
		rig.frames( 1, true );
		assertNotNull( xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit.get( w.f.sprite, xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit.Slot.MARK ) );
		Buff.detach( w.f, Lucky.LuckProc.class );
		rig.frames( 1, true );
		assertEquals( null, xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit.get( w.f.sprite, xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit.Slot.MARK ) );
	}

	// ------------------------------------------------------------------ steps

	@Test
	public void aWalkersStepIsAnsweredAtMostEveryFifthOfASecond() throws Exception {
		TestParty.join( new Hero() );
		World w = world( INLINE, false );
		Mob wader = place( new Dummy(), at( 10, 6 ) );
		Game.timeTotal += 3f;
		NetVisuals.clear();
		for (int i = 0; i < 5; i++){
			Flow.speedBoost( wader, 0 );
			Swiftness.speedBoost( w.w, 0 );
		}
		assertEquals( "one a walker in the same moment", 2, steps() );
		Game.timeTotal += EnchantFx.STEP_GAP * 0.5f;
		Flow.speedBoost( wader, 0 );
		assertEquals( "not yet", 0, steps() );
		Game.timeTotal += EnchantFx.STEP_GAP;
		Flow.speedBoost( wader, 0 );
		assertEquals( "past the gap", 1, steps() );
		//unseen: none
		wader.sprite.visible = false;
		Game.timeTotal += 1f;
		Flow.speedBoost( wader, 0 );
		assertEquals( 0, steps() );
		//every second answered step leaves a ghost (Afterimage's own count), never more than two alive
		wader.sprite.visible = true;
		int most = 0;
		for (int i = 0; i < 12; i++){
			Game.timeTotal += EnchantFx.STEP_GAP + 0.01f;
			Flow.speedBoost( wader, 0 );
			rig.frames( 6, false );
			most = Math.max( most, xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage.live() );
		}
		assertTrue( "ghosts: " + most, most >= 1 && most <= 2 );
	}

	//the step answers sent since the last call
	private static int steps() throws Exception {
		JSONArray a = NetVisuals.drainEvents();
		int n = 0;
		if (a == null) return 0;
		for (int i = 0; i < a.length(); i++){
			JSONObject e = a.getJSONObject( i );
			if (!"eh".equals( e.getString( "t" ) ) || e.getInt( "k" ) != EnchantNet.GLYPH) continue;
			if (e.getJSONArray( "i" ).getInt( 6 ) == EnchantFx.STEP) n++;
		}
		return n;
	}

	// ------------------------------------------------------------------ the census

	/** Every file the rows touch and its count of the game's Random call sites at 37d849aba (a
	 *  name not preceded by a letter: FxRandom's are the pictures' own). */
	static final Object[][] CENSUS = {
			{ "items/weapon/enchantments/Eldritch.java", 1 },
			{ "items/weapon/enchantments/Lucky.java", 1 },
			{ "items/weapon/enchantments/Projecting.java", 0 },
			{ "items/weapon/enchantments/Unstable.java", 1 },
			{ "items/weapon/enchantments/Vorpal.java", 1 },
			{ "items/weapon/enchantments/Corrupting.java", 1 },
			{ "items/weapon/enchantments/Grim.java", 0 },
			{ "items/weapon/enchantments/Vampiric.java", 1 },
			{ "items/weapon/enchantments/Horror.java", 1 },
			{ "items/weapon/enchantments/Hunting.java", 2 },
			{ "items/weapon/enchantments/Midas.java", 3 },
			{ "items/weapon/enchantments/Vicious.java", 4 },
			{ "items/weapon/enchantments/Luck.java", 0 },
			{ "items/weapon/enchantments/Slashing.java", 0 },
			{ "items/weapon/enchantments/CromLuck.java", 0 },
			{ "items/weapon/enchantments/AresLeech.java", 1 },
			{ "items/weapon/enchantments/BuzzSaw.java", 1 },
			{ "items/weapon/enchantments/JupitersHorror.java", 1 },
			{ "items/weapon/enchantments/Nomnom.java", 2 },
			{ "items/weapon/enchantments/Osmose.java", 1 },
			{ "items/armor/glyphs/Affection.java", 1 },
			{ "items/armor/glyphs/Afterimage.java", 0 },
			{ "items/armor/glyphs/Flow.java", 1 },
			{ "items/armor/glyphs/Mirrorimage.java", 1 },
			{ "items/armor/glyphs/Mirroring.java", 3 },
			{ "items/armor/glyphs/Obfuscation.java", 0 },
			{ "items/armor/glyphs/Phasing.java", 1 },
			{ "items/armor/glyphs/Siphoning.java", 2 },
			{ "items/armor/glyphs/Swiftness.java", 1 },
			{ "items/armor/glyphs/Thorns.java", 1 },
			{ "items/weapon/curses/Annoying.java", 4 },
			{ "items/weapon/curses/Displacing.java", 1 },
			{ "items/weapon/curses/Friendly.java", 1 },
			{ "items/weapon/curses/Sacrificial.java", 2 },
			{ "items/weapon/curses/Wayward.java", 1 },
			{ "items/weapon/curses/Wondrous.java", 2 },
			{ "items/armor/curses/Displacement.java", 1 },
			{ "items/armor/curses/Metabolism.java", 1 },
			{ "items/armor/curses/Multiplicity.java", 3 },
			{ "actors/Char.java", 14 },   //15 till the English-only "dooked" miss word (a 1-in-10 roll) became the miss card
			{ "effects/fx/enchant/rows/ShadowRows.java", 0 },
			{ "effects/fx/enchant/rows/ShadowExhibits.java", 0 },
			{ "effects/fx/enchant/EnchantLooks.java", 0 },
			{ "effects/fx/modules/EnchantArea.java", 0 },
	};

	static final Pattern GAME_RANDOM = Pattern.compile( "(?<![A-Za-z_])Random\\." );

	@Test
	public void everyTouchedFileCallsTheGamesRandomAsOftenAsItDid() throws Exception {
		for (Object[] c : CENSUS){
			File f = new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/" + c[0] );
			String src = new String( Files.readAllBytes( f.toPath() ), StandardCharsets.UTF_8 );
			Matcher m = GAME_RANDOM.matcher( src );
			int n = 0;
			while (m.find()) n++;
			assertEquals( (String) c[0], ((Integer) c[1]).intValue(), n );
		}
	}
}
