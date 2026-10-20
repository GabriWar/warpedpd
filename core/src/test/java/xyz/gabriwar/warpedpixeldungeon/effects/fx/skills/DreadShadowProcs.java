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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MasterThiefCoins;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.AimedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Ambush;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blackout;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Bombvoyage;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DarkPact;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Demoralize;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DreadHowl;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LoneWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MasterThief;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Necrotoxin;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.NinjaBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.PanicHarvest;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SilentDeath;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SoulDrain;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Stealth;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Vanish;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Venom;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The dread-shadow package's procs and casts, played for real on a headless stage (a bare game
 * scene holding the kit's stand-in layers, so the legacy cell emitters work): each one's draws of
 * the game's Random, counted from a seed through the call and two seconds of frames after it (the
 * timeline's steps, a bolt's arrival, a tether's bite all draw on the render thread), and the next
 * value the game's Random gives after them. Written against the API master (72b40617b) already
 * had, so the same file measures master's baseline: with DREAD_BASELINE set, `baseline` writes the
 * table to build/dread-baseline.txt (SfxDreadShadowTest holds it and checks this code against it).
 */
public class DreadShadowProcs {

	static final int W = 16;	//EnchantFxTest.Rig's room
	static final long SEED = 2026;

	/** A proc's stage and its call: built outside the count, run inside it. */
	interface Proc {
		Runnable stage( DreadShadowProcs t ) throws Exception;
	}

	static final LinkedHashMap<String, Proc> PROCS = new LinkedHashMap<>();
	static {
		PROCS.put( "demoralize", t -> {
			Demoralize d = skill( new Demoralize() );
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> { for (int i = 0; i < 5; i++) d.onHitProc( r, 5, false ); };
		} );
		PROCS.put( "lone-wolf", t -> {
			LoneWolf l = skill( new LoneWolf() );
			Rat dead = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 4, 3 ) );
			t.rat( t.at( 2, 5 ) );
			return () -> l.onKill( dead, false );
		} );
		PROCS.put( "dread-howl", t -> {
			DreadHowl d = skill( new DreadHowl() );
			Buff.affect( t.rat( t.at( 6, 4 ) ), Terror.class, 5 );
			Buff.affect( t.rat( t.at( 3, 2 ) ), Terror.class, 5 );
			t.rat( t.at( 5, 6 ) );
			t.rat( t.at( 1, 4 ) );
			t.hero.MP = t.hero.MT = 200;
			return () -> d.execute( t.hero, Skill.AC_CAST );
		} );
		PROCS.put( "panic-harvest", t -> {
			PanicHarvest p = skill( new PanicHarvest() );
			Rat dead = t.rat( t.at( 5, 4 ) );
			Buff.affect( dead, Terror.class, 5 );
			t.rat( t.at( 6, 4 ) );
			t.rat( t.at( 5, 3 ) );
			return () -> {
				p.onHitProc( dead, 5, false );
				p.onKill( dead, false );
			};
		} );
		PROCS.put( "hex", t -> {
			Hex h = skill( new Hex() );
			Rat r = t.rat( t.at( 7, 4 ) );
			t.hero.MP = t.hero.MT = 200;
			Class<?> c = Class.forName( Hex.class.getName() + "$Curser" );
			Constructor<?> k = c.getDeclaredConstructor( Hex.class );
			k.setAccessible( true );
			CellSelector.Listener l = (CellSelector.Listener) k.newInstance( h );
			return () -> l.onSelect( r.pos );
		} );
		PROCS.put( "silent-death", t -> {
			SilentDeath s = skill( new SilentDeath() );
			Rat r = t.rat( t.at( 4, 4 ) );
			Rat sleeper = t.rat( t.at( 6, 3 ) );
			sleeper.state = sleeper.SLEEPING;
			r.state = r.SLEEPING;
			r.HP = 1;
			//the blow found it asleep (Mob.attackProc's note)
			set( Mob.class, r, "blowFoundAsleep", true );
			set( Mob.class, r, "blowAt", Actor.now() );
			return () -> {
				s.instantKill( r, 5 );
				s.onKill( r, false );
			};
		} );
		PROCS.put( "vanish", t -> {
			Vanish v = skill( new Vanish() );
			t.hero.MP = t.hero.MT = 200;
			for (int[] c : new int[][]{ { 5, 4 }, { 4, 2 } }){
				Rat r = t.rat( t.at( c[0], c[1] ) );
				r.state = r.HUNTING;
				r.aggro( t.hero );
			}
			return () -> v.execute( t.hero, Skill.AC_CAST );
		} );
		PROCS.put( "dark-pact", t -> {
			DarkPact d = skill( new DarkPact() );
			t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 3, 3 ) );
			t.hero.HP = t.hero.HT = 100;
			return () -> d.execute( t.hero, Skill.AC_CAST );
		} );
		PROCS.put( "ambush", t -> {
			Ambush a = skill( new Ambush() );
			Rat r = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 4 ) );
			return () -> {
				try {
					Method m = Ambush.class.getDeclaredMethod( "stake", Char.class );
					m.setAccessible( true );
					m.invoke( a, r );
				} catch (ReflectiveOperationException e){
					throw new AssertionError( e );
				}
			};
		} );
		PROCS.put( "shadow-link", t -> {
			DoubleStab d = skill( new DoubleStab() );
			d.active = true;
			t.hero.MP = t.hero.MT = 200;
			Rat r = t.rat( t.at( 5, 4 ) );
			t.rat( t.at( 7, 3 ) );
			return () -> d.onHitProc( r, 5, false );
		} );
		PROCS.put( "soul-drain", t -> {
			SoulDrain s = skill( new SoulDrain() );
			Rat dead = t.rat( t.at( 4, 5 ) );
			Buff.affect( dead, SoulMark.class, 5 );
			t.rat( t.at( 6, 4 ) );
			t.hero.HP = 10;
			t.hero.HT = 100;
			return () -> {
				s.onEnemyDeath( dead, null );
				t.runDeferred();
			};
		} );
		PROCS.put( "master-thief", t -> {
			MasterThief m = skill( new MasterThief() );
			Rat r = t.rat( t.at( 5, 4 ) );
			t.rat( t.at( 6, 5 ) );
			Buff.affect( t.hero, MasterThiefCoins.class ).load( 4, 6 );
			return () -> m.onDefendProc( r, 5 );
		} );
		PROCS.put( "necrotoxin", t -> {
			Necrotoxin n = skill( new Necrotoxin() );
			Rat r = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 3 ) );
			return () -> {
				for (int i = 0; i < 6; i++){
					Buff.affect( r, Poison.class ).set( 6 );
					n.onHitProc( r, 5, false );
				}
			};
		} );
		PROCS.put( "venom", t -> {
			Venom v = skill( new Venom() );
			return () -> { for (int i = 0; i < 20; i++) v.venomousAttack(); };
		} );
		PROCS.put( "aimed-shot", t -> {
			AimedShot a = skill( new AimedShot() );
			t.hero.MP = t.hero.MT = 200;
			Rat r = t.rat( t.at( 7, 4 ) );
			return () -> {
				a.execute( t.hero, Skill.AC_ACTIVATE );
				a.active = true;
				a.onHitProc( r, 5, true );
			};
		} );
		PROCS.put( "double-shot", t -> {
			DoubleShot d = skill( new DoubleShot() );
			t.hero.MP = t.hero.MT = 200;
			return () -> {
				d.execute( t.hero, Skill.AC_ACTIVATE );
				d.active = true;
				for (int i = 0; i < 4; i++) d.doubleShot();
			};
		} );
		PROCS.put( "bombvoyage", t -> {
			Bombvoyage b = skill( new Bombvoyage() );
			t.hero.MP = t.hero.MT = 200;
			t.rat( t.at( 7, 4 ) ).HP = 500;
			return () -> {
				b.execute( t.hero, Skill.AC_ACTIVATE );
				b.active = true;
				b.arrowToBomb();
				b.execute( t.hero, Skill.AC_DEACTIVATE );
			};
		} );
		PROCS.put( "stealth", t -> {
			Stealth st = skill( new Stealth() );
			Rat r = t.rat( t.at( 7, 4 ) );
			r.state = r.HUNTING;
			return () -> st.onEnemySpotted( r );
		} );
		PROCS.put( "ninja-bomb", t -> {
			t.hero.heroSkills = new xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills( HeroClass.ROGUE );
			Skill nb = t.hero.heroSkills.get( NinjaBomb.class );
			nb.level = Skill.MAX_LEVEL;
			t.hero.MP = t.hero.MT = 200;
			t.rat( t.at( 7, 4 ) );
			Field f = NinjaBomb.class.getDeclaredField( "thrower" );
			f.setAccessible( true );
			CellSelector.Listener l = (CellSelector.Listener) f.get( null );
			return () -> l.onSelect( t.at( 7, 4 ) );
		} );
		PROCS.put( "blackout", t -> {
			Blackout b = skill( new Blackout() );
			t.hero.MP = t.hero.MT = 200;
			t.rat( t.at( 6, 3 ) );
			t.rat( t.at( 7, 5 ) );
			Field f = Blackout.class.getDeclaredField( "thrower" );
			f.setAccessible( true );
			CellSelector.Listener l = (CellSelector.Listener) f.get( b );
			return () -> l.onSelect( t.at( 6, 4 ) );
		} );
		PROCS.put( "auras", t -> () -> {
			for (Class<?> c : RogueHuntressAuras.class.getDeclaredClasses()){
				try {
					Object buff = c.getDeclaredConstructor().newInstance();
					Field target = Buff.class.getDeclaredField( "target" );
					target.setAccessible( true );
					target.set( buff, t.hero );
					Method build = c.getDeclaredMethod( "build" );
					build.setAccessible( true );
					t.rig.stage.add( (Group) build.invoke( buff ) );
				} catch (ReflectiveOperationException e){
					throw new AssertionError( c.getName(), e );
				}
			}
		} );
	}

	static void set( Class<?> c, Object o, String name, Object v ) throws ReflectiveOperationException {
		Field f = c.getDeclaredField( name );
		f.setAccessible( true );
		f.set( o, v );
	}

	static <S extends Skill> S skill( S s ){
		s.level = Skill.MAX_LEVEL;
		return s;
	}

	// ------------------------------------------------------------------ the stage

	EnchantFxTest.Rig rig;
	Hero hero;

	int at( int x, int y ){
		return x + y * W;
	}

	Rat rat( int cell ){
		Rat m = new Rat();
		m.HP = m.HT = 50;
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

	//the deferred steps (SkillInteractions.defer) run now, as the actor thread would next
	void runDeferred() throws RuntimeException {
		try {
			Method act = Actor.class.getDeclaredMethod( "act" );
			act.setAccessible( true );
			for (Actor a : Actor.all().toArray( new Actor[0] )){
				if (a instanceof Char) continue;
				act.invoke( a );
			}
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private Object savedScene;

	void up() throws Exception {
		rig = new EnchantFxTest.Rig();
		rig.up();
		Actor.clear();
		PathFinder.setMapSize( W, W );
		savedScene = sceneField().get( null );
		bareScene( rig.layers, rig.stage );
		hero = new Hero();
		hero.heroClass = HeroClass.ROGUE;
		hero.HP = hero.HT = 100;
		hero.pos = at( 3, 4 );
		hero.fieldOfView = new boolean[W * W];
		java.util.Arrays.fill( hero.fieldOfView, true );
		Dungeon.hero = hero;
		HeroSprite hs = new HeroSprite( hero );
		rig.stage.add( hs );
		hs.place( hero.pos );
		hs.visible = true;
		Actor.add( hero );
	}

	void down() throws Exception {
		sceneField().set( null, savedScene );
		Actor.clear();
		Dungeon.hero = null;
		rig.down();
		if (Dungeon.level != null) PathFinder.setMapSize( Dungeon.level.width(), Dungeon.level.height() );
	}

	private static Field sceneField() throws Exception {
		Field f = GameScene.class.getDeclaredField( "scene" );
		f.setAccessible( true );
		return f;
	}

	//a bare game scene (no constructor run) holding the stand-in layers (ElementProcsTest's)
	private static void bareScene( Group[] l, Group stage ) throws Exception {
		Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
		u.setAccessible( true );
		Object unsafe = u.get( null );
		Object scene = unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, GameScene.class );
		set( scene, "floorEmitters", l[GameScene.Layer.FLOOR.ordinal()] );
		set( scene, "ripples", l[GameScene.Layer.SURFACE.ordinal()] );
		set( scene, "subsurface", l[GameScene.Layer.SUBSURFACE.ordinal()] );
		set( scene, "sheen", l[GameScene.Layer.SHEEN.ordinal()] );
		set( scene, "emitters", l[GameScene.Layer.EFFECTS.ordinal()] );
		set( scene, "lights", l[GameScene.Layer.LIGHTS.ordinal()] );
		set( scene, "effects", new Group() );
		set( scene, "gases", new Group() );
		set( scene, "emoicons", new Group() );
		set( scene, "statuses", new Group() );
		set( scene, "healthIndicators", new Group() );
		set( scene, "mobs", stage );
		set( scene, "blobs", new Group() );
		sceneField().set( null, scene );
	}

	private static void set( Object o, String name, Object v ) throws Exception {
		Field f;
		try {
			f = GameScene.class.getDeclaredField( name );
		} catch (NoSuchFieldException e){
			return;
		}
		f.setAccessible( true );
		f.set( o, v );
	}

	// ------------------------------------------------------------------ the count

	/** Each proc's draws of the game's Random and the next value after them: { draws, next * 1e6 }. */
	public static LinkedHashMap<String, long[]> measure() throws Exception {
		WarpedRoomsTest.boot();
		//NetVisuals' static setup draws the game's Random once (a factory it registers): loaded
		//before the count, as a game has it long before any skill, or the first proc to load it pays
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals", true, DreadShadowProcs.class.getClassLoader() );
		LinkedHashMap<String, long[]> out = new LinkedHashMap<>();
		for (Map.Entry<String, Proc> e : PROCS.entrySet()){
			DreadShadowProcs t = new DreadShadowProcs();
			t.up();
			try {
				Runnable run = e.getValue().stage( t );
				Random.pushGenerator( SEED );
				run.run();
				t.rig.frames( 120, true );
				float next = Random.Float();
				Random.popGenerator();
				out.put( e.getKey(), new long[]{ count( next ), Math.round( next * 1e6 ) } );
			} finally {
				t.down();
			}
		}
		return out;
	}

	//how many draws from SEED come before `next`
	private static int count( float next ){
		Random.pushGenerator( SEED );
		try {
			for (int k = 0; k < 4096; k++){
				if (Random.Float() == next) return k;
			}
			return -1;
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void baseline() throws Exception {
		if (System.getenv( "DREAD_BASELINE" ) == null) return;
		new File( "build" ).mkdirs();
		try (Writer w = new FileWriter( new File( "build/dread-baseline.txt" ) )){
			for (Map.Entry<String, long[]> e : measure().entrySet()){
				w.write( "{ \"" + e.getKey() + "\", " + e.getValue()[0] + ", " + e.getValue()[1] + " },\n" );
			}
		}
	}
}
