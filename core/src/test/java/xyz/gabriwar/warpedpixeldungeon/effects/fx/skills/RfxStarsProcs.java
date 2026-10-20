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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Accuracy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aggression;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aplomb;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Awareness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeMastery;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CounterTime;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DragonKick;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HeadShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hunting;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.IronStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LastStand;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Mastery;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ParryStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.PiercingFocus;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Precision;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Rampage;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.RiposteStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Scorpion;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SecondWind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sidestep;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SixthSense;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Trumpet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Wardrum;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WornShortsword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The rfx-stars package's touched moments played for real on DreadShadowProcs' headless stage, at
 * mastery: each one's draws of the game's Random, counted from a seed through the call and two
 * seconds of frames after it (a jump's landing, a deferred answer, the knockdown's actor, the bind's
 * bite all draw later), and the next value after them. A swapped burst keeps its call, emitter and
 * quantity, so these must be f0783eacd's to the draw. Written against the API f0783eacd already had,
 * so the same file measures that baseline: with RFX_STARS_BASELINE set, `baseline` writes the table
 * to build/rfx-stars-baseline.txt (SfxRfxStarsTest holds it and checks this code against it).
 */
public class RfxStarsProcs {

	static final long SEED = 2028;

	interface Proc {
		Runnable stage( DreadShadowProcs t ) throws Exception;
	}

	static final LinkedHashMap<String, Proc> PROCS = new LinkedHashMap<>();
	static {
		//a feeble blow bounces off and the attacker is dazed (and held, +3)
		PROCS.put( "aplomb", t -> {
			Aplomb a = DreadShadowProcs.skill( new Aplomb() );
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> a.incomingDamageReduction( 3, r );
		} );
		//the kick into a wall: the slam's daze, the +3 daze on the two beside where it lands
		PROCS.put( "dragon-kick", t -> {
			DragonKick d = DreadShadowProcs.skill( new DragonKick() );
			Rat victim = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 3 ) );
			t.rat( t.at( 5, 5 ) );
			int wall = t.at( 6, 4 );
			Dungeon.level.solid[wall] = true;
			Dungeon.level.passable[wall] = false;
			Method kick = DragonKick.class.getDeclaredMethod( "kick", Hero.class, Char.class );
			kick.setAccessible( true );
			return () -> call( kick, d, t.hero, victim );
		} );
		//stepping round an enemy dazes it
		PROCS.put( "sidestep", t -> {
			Sidestep s = DreadShadowProcs.skill( new Sidestep() );
			t.rat( t.at( 4, 4 ) );
			return () -> s.onCharMoved( t.hero, t.at( 3, 3 ), true );
		} );
		//the sense takes a quarter off a great blow; +3 dazes the striker
		PROCS.put( "sixth-sense", t -> {
			SixthSense s = DreadShadowProcs.skill( new SixthSense() );
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> s.incomingDamageReduction( 40, r );
		} );
		//the stare dazes everything in view (2: the +3's mind vision wants a whole scene's fog)
		PROCS.put( "piercing-focus", t -> {
			PiercingFocus p = DreadShadowProcs.skill( new PiercingFocus() );
			p.level = 2;
			t.hero.MP = t.hero.MT = 200;
			t.rat( t.at( 5, 4 ) );
			t.rat( t.at( 6, 2 ) );
			return () -> p.execute( t.hero, Skill.AC_CAST );
		} );
		//a head shot (shot until one lands), its knockdown and the +3 ring-out on the two beside it
		PROCS.put( "head-shot", t -> {
			HeadShot h = DreadShadowProcs.skill( new HeadShot() );
			Rat r = t.rat( t.at( 6, 4 ) );
			t.rat( t.at( 7, 4 ) );
			t.rat( t.at( 6, 5 ) );
			return () -> {
				for (int i = 0; i < 16; i++) if (h.onHitProc( r, 10, true ) != 10) break;
				t.runDeferred();
			};
		} );
		//a parried blow; +3 the attacker reels
		PROCS.put( "iron-stance", t -> {
			IronStance s = DreadShadowProcs.skill( new IronStance() );
			t.hero.MP = t.hero.MT = 200;
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> s.onDodge( r );
		} );
		//the stand at a quarter of health; +3 holds a rat beside him and only shakes a boss
		PROCS.put( "last-stand", t -> {
			LastStand l = DreadShadowProcs.skill( new LastStand() );
			Rat r = t.rat( t.at( 4, 4 ) );
			Rat boss = t.rat( t.at( 2, 4 ) );
			properties( boss ).add( Char.Property.BOSS );
			return () -> l.incomingDamageReduction( 90, r );
		} );
		//stings on a poisoned, crippled mark: at mastery one pins it
		PROCS.put( "scorpion", t -> {
			Scorpion s = DreadShadowProcs.skill( new Scorpion() );
			Rat r = t.rat( t.at( 4, 4 ) );
			Buff.affect( r, Poison.class ).set( 20 );
			Buff.prolong( r, Cripple.class, 20 );
			return () -> { for (int i = 0; i < 8; i++) s.onHitProc( r, 2, false ); };
		} );
		//a killing swing cleaves into the one beside him
		PROCS.put( "aggression", t -> {
			Aggression a = DreadShadowProcs.skill( new Aggression() );
			Rat dead = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 2, 4 ) );
			return () -> {
				a.onHitProc( dead, 20, false );
				dead.HP = 0;
				a.onKill( dead, false );
			};
		} );
		//an enemy stepping in is met by the blade (stepping in until it is)
		PROCS.put( "mastery", t -> {
			Mastery m = DreadShadowProcs.skill( new Mastery() );
			t.hero.belongings.weapon = new WornShortsword();
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> {
				for (int i = 0; i < 16 && r.HP == r.HT; i++) m.onEnemyStepsAdjacent( r, t.at( 5, 4 ) );
			};
		} );
		//an ambush on a sleeper, and the stun that lands after it
		PROCS.put( "hunting", t -> {
			Hunting h = DreadShadowProcs.skill( new Hunting() );
			Rat r = t.rat( t.at( 4, 4 ) );
			r.state = r.SLEEPING;
			return () -> {
				h.onHitProc( r, 5, false );
				t.runDeferred();
			};
		} );
		//a parried blow (blows until one is)
		PROCS.put( "parry", t -> {
			ParryStance p = DreadShadowProcs.skill( new ParryStance() );
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> { for (int i = 0; i < 16; i++) if (p.onDefendProc( r, 5 ) == 0) break; };
		} );
		//the stance up, then a missed swing answered
		PROCS.put( "riposte", t -> {
			RiposteStance s = DreadShadowProcs.skill( new RiposteStance() );
			t.hero.MP = t.hero.MT = 200;
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> {
				s.execute( t.hero, Skill.AC_ACTIVATE );
				s.onHeroMissed( r, true );
			};
		} );
		//the counter on one stepping away
		PROCS.put( "counter-time", t -> {
			CounterTime c = DreadShadowProcs.skill( new CounterTime() );
			Rat r = t.rat( t.at( 5, 4 ) );
			Method strike = CounterTime.class.getDeclaredMethod( "strike", Hero.class, Char.class );
			strike.setAccessible( true );
			return () -> call( strike, c, t.hero, r );
		} );
		//a read (and the +3's on its neighbour), then a dodge read off the blade
		PROCS.put( "blade-mastery", t -> {
			BladeMastery b = DreadShadowProcs.skill( new BladeMastery() );
			Rat r = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 4 ) );
			return () -> {
				b.onHitProc( r, 5, false );
				b.onDodge( r );
			};
		} );
		//the Gap mark hopping on
		PROCS.put( "precision", t -> {
			Precision p = DreadShadowProcs.skill( new Precision() );
			Rat r = t.rat( t.at( 5, 4 ) );
			Method mark = Precision.class.getDeclaredMethod( "mark", Char.class, int.class );
			mark.setAccessible( true );
			return () -> call( mark, p, r, t.at( 4, 4 ) );
		} );
		//a dodge sensed and answered
		PROCS.put( "awareness", t -> {
			Awareness a = DreadShadowProcs.skill( new Awareness() );
			Rat r = t.rat( t.at( 5, 4 ) );
			return () -> {
				a.onDodge( r );
				t.runDeferred();
			};
		} );
		//the stance up
		PROCS.put( "rampage", t -> {
			Rampage r = DreadShadowProcs.skill( new Rampage() );
			t.hero.MP = t.hero.MT = 200;
			return () -> r.execute( t.hero, Skill.AC_ACTIVATE );
		} );
		//a bullseye (the first ranged hit at +3)
		PROCS.put( "accuracy", t -> {
			Accuracy a = DreadShadowProcs.skill( new Accuracy() );
			Rat r = t.rat( t.at( 6, 4 ) );
			return () -> a.onHitProc( r, 5, true );
		} );
		//the weapon's charges back
		PROCS.put( "second-wind", t -> {
			SecondWind s = DreadShadowProcs.skill( new SecondWind() );
			t.hero.MP = t.hero.MT = 200;
			Buff.affect( t.hero, MeleeWeapon.Charger.class ).charges = 0;
			return () -> s.execute( t.hero, Skill.AC_CAST );
		} );
		//blades bound to the one beside her: the tethers, the bite on the chain's frame
		PROCS.put( "blade-bind", t -> {
			BladeBind b = DreadShadowProcs.skill( new BladeBind() );
			t.hero.MP = t.hero.MT = 200;
			t.rat( t.at( 4, 4 ) );
			Method bind = BladeBind.class.getDeclaredMethod( "bind", Hero.class, int.class );
			bind.setAccessible( true );
			return () -> call( bind, b, t.hero, t.at( 4, 4 ) );
		} );
		//the fanfare: an enemy held, an ally blessed, the hero blessed
		PROCS.put( "trumpet", t -> {
			Rat foe = t.rat( t.at( 5, 4 ) );
			Rat ally = t.rat( t.at( 2, 3 ) );
			ally.alignment = Char.Alignment.ALLY;
			Trumpet w = new Trumpet();
			Method ability = Trumpet.class.getDeclaredMethod( "duelistAbility", Hero.class, Integer.class );
			ability.setAccessible( true );
			return () -> call( ability, w, t.hero, null );
		} );
		//the war beat: an ally hasted, an enemy beside him terrified
		PROCS.put( "wardrum", t -> {
			t.rat( t.at( 4, 4 ) );
			Rat ally = t.rat( t.at( 2, 3 ) );
			ally.alignment = Char.Alignment.ALLY;
			Wardrum w = new Wardrum();
			Method ability = Wardrum.class.getDeclaredMethod( "duelistAbility", Hero.class, Integer.class );
			ability.setAccessible( true );
			return () -> call( ability, w, t.hero, null );
		} );
	}

	private static void call( Method m, Object on, Object... args ){
		try {
			m.invoke( on, args );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( m.getName(), e );
		}
	}

	@SuppressWarnings( "unchecked" )
	private static HashSet<Char.Property> properties( Char c ) throws ReflectiveOperationException {
		Field f = Char.class.getDeclaredField( "properties" );
		f.setAccessible( true );
		return (HashSet<Char.Property>) f.get( c );
	}

	/** Each one's draws of the game's Random and the next value after them: { draws, next * 1e6 }. */
	public static LinkedHashMap<String, long[]> measure() throws Exception {
		WarpedRoomsTest.boot();
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals", true, RfxStarsProcs.class.getClassLoader() );
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
			} catch (Throwable x){
				throw new AssertionError( e.getKey(), x );
			} finally {
				t.down();
			}
		}
		return out;
	}

	/** Each one's emitters' factories as they burst (a sprite's, a cell's), by proc: what each moment
	 *  really showed, read off every emitter on the stage and the layers after the call and after
	 *  each of the frames that follow it. */
	public static LinkedHashMap<String, Set<Emitter.Factory>> bursts() throws Exception {
		WarpedRoomsTest.boot();
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals", true, RfxStarsProcs.class.getClassLoader() );
		Field factory = Emitter.class.getDeclaredField( "factory" );
		factory.setAccessible( true );
		LinkedHashMap<String, Set<Emitter.Factory>> out = new LinkedHashMap<>();
		for (Map.Entry<String, Proc> e : PROCS.entrySet()){
			DreadShadowProcs t = new DreadShadowProcs();
			t.up();
			try {
				Runnable run = e.getValue().stage( t );
				Set<Emitter.Factory> seen = Collections.newSetFromMap( new IdentityHashMap<>() );
				Random.pushGenerator( SEED );
				run.run();
				for (int f = 0; f <= 120; f++){
					collect( t.rig.stage, factory, seen );
					for (Group g : t.rig.layers) collect( g, factory, seen );
					t.rig.frames( 1, true );
				}
				Random.popGenerator();
				out.put( e.getKey(), seen );
			} finally {
				t.down();
			}
		}
		return out;
	}

	private static void collect( Group g, Field factory, Set<Emitter.Factory> into ) throws IllegalAccessException {
		for (Gizmo m : g.membersView()){
			if (m instanceof Emitter){
				Object f = factory.get( m );
				if (f != null) into.add( (Emitter.Factory) f );
			}
			if (m instanceof Group) collect( (Group) m, factory, into );
		}
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
		if (System.getenv( "RFX_STARS_BASELINE" ) == null) return;
		new File( "build" ).mkdirs();
		try (Writer w = new FileWriter( new File( "build/rfx-stars-baseline.txt" ) )){
			for (Map.Entry<String, long[]> e : measure().entrySet()){
				w.write( "{ \"" + e.getKey() + "\", " + e.getValue()[0] + ", " + e.getValue()[1] + " },\n" );
			}
		}
	}
}
