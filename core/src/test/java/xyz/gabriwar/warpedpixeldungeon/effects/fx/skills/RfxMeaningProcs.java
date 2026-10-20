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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeartseekerArrow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.UndyingWillWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BloodDance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.EmberArrows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Faith;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostNova;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Grace;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HealingPrayer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.InnerPeace;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Litany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MassHeal;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Meditation;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Purify;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SereneFocus;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Spirituality;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.UndyingWill;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The meaning package's skills (heals, mana gains, blood, fury, fire, frost) played for real on
 * DreadShadowProcs' headless stage: each one's draws of the game's Random, counted from a seed
 * through the call and two seconds of frames after it (the timelines' beats, a bolt's arrival, the
 * nova's rings), and the next value after them. Written against the API f0783eacd already had, so
 * the same file measures that baseline: with RFX_BASELINE set, `baseline` writes the table to
 * build/rfx-meaning-baseline.txt (SfxRfxMeaningTest holds it and checks this code against it).
 */
public class RfxMeaningProcs {

	static final long SEED = 2028;

	interface Proc {
		Runnable stage( DreadShadowProcs t ) throws Exception;
	}

	static final LinkedHashMap<String, Proc> PROCS = new LinkedHashMap<>();
	static {
		//the wings cast, then a turn of them mending the cleric and two hurt allies
		PROCS.put( "mass-heal", t -> {
			MassHeal m = DreadShadowProcs.skill( new MassHeal() );
			t.hero.MP = t.hero.MT = 200;
			t.hero.HP = 60;
			ally( t, 4, 3 );
			ally( t, 3, 5 );
			t.rat( t.at( 6, 4 ) );
			return () -> {
				m.execute( t.hero, Skill.AC_CAST );
				MassHeal.Wings w = t.hero.buff( MassHeal.Wings.class );
				if (w != null) w.act();
			};
		} );
		//three verses (the third blesses), mending the cleric and two allies
		PROCS.put( "litany", t -> {
			Litany l = DreadShadowProcs.skill( new Litany() );
			t.hero.HP = 60;
			ally( t, 4, 3 );
			ally( t, 2, 5 );
			return () -> { for (int i = 0; i < 3; i++) l.onSkillCast( l ); };
		} );
		PROCS.put( "healing-prayer", t -> {
			HealingPrayer p = DreadShadowProcs.skill( new HealingPrayer() );
			t.hero.MP = t.hero.MT = 200;
			t.hero.HP = 90;
			return () -> p.execute( t.hero, Skill.AC_CAST );
		} );
		//the purge reaching two allies in sight
		PROCS.put( "purify", t -> {
			Purify p = DreadShadowProcs.skill( new Purify() );
			t.hero.MP = t.hero.MT = 200;
			ally( t, 5, 3 );
			ally( t, 2, 6 );
			return () -> p.execute( t.hero, Skill.AC_CAST );
		} );
		//a kill sending mana home, and one past a full pool sending a heal
		PROCS.put( "grace", t -> {
			Grace g = DreadShadowProcs.skill( new Grace() );
			t.hero.MT = 50;
			t.hero.MP = 0;
			t.hero.HP = 60;
			Rat a = t.rat( t.at( 7, 4 ) ), b = t.rat( t.at( 6, 2 ) );
			return () -> {
				g.onKill( a, false );
				t.hero.MP = t.hero.MT;
				g.onKill( b, false );
			};
		} );
		PROCS.put( "faith", t -> {
			Faith f = DreadShadowProcs.skill( new Faith() );
			t.hero.MT = 50;
			t.hero.MP = 0;
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> f.incomingDamageReduction( 60, r );
		} );
		PROCS.put( "meditation", t -> {
			Meditation m = DreadShadowProcs.skill( new Meditation() );
			t.hero.MT = 50;
			t.hero.MP = 0;
			Method pulse = Meditation.class.getDeclaredMethod( "pulse", Hero.class );
			pulse.setAccessible( true );
			return () -> {
				try {
					pulse.invoke( m, t.hero );
				} catch (ReflectiveOperationException e){
					throw new AssertionError( e );
				}
			};
		} );
		//a zap landing with the pool three quarters full: the struck and its neighbours calmed
		PROCS.put( "serene-focus", t -> {
			for (HeroClass c : HeroClass.values()){
				CurrentSkills cs = new CurrentSkills( c );
				if (cs.get( SereneFocus.class ) != null){
					t.hero.heroSkills = cs;
					break;
				}
			}
			t.hero.heroSkills.get( SereneFocus.class ).level = Skill.MAX_LEVEL;
			t.hero.MP = t.hero.MT = 100;
			Rat r = t.rat( t.at( 6, 4 ) );
			t.rat( t.at( 7, 4 ) );
			return () -> SereneFocus.onZap( r );
		} );
		//a blow jolting mana loose, then with the pool full lashing back
		PROCS.put( "spirituality", t -> {
			Spirituality s = DreadShadowProcs.skill( new Spirituality() );
			t.hero.MT = 50;
			t.hero.MP = 0;
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> {
				s.onDefendProc( r, 6 );
				t.hero.MP = t.hero.MT;
				s.onDefendProc( r, 6 );
			};
		} );
		//twelve harms met: those breathed out restore mana
		PROCS.put( "inner-peace", t -> {
			InnerPeace p = DreadShadowProcs.skill( new InnerPeace() );
			t.hero.MT = 50;
			t.hero.MP = 0;
			return () -> { for (int i = 0; i < 12; i++) p.shrugsOffDebuff( new Cripple() ); };
		} );
		//the dawn's two turns: mending the cleric and two hurt allies, then its spire on an enemy
		PROCS.put( "dawn", t -> {
			t.hero.HP = 60;
			ally( t, 4, 4 );
			ally( t, 3, 3 );
			t.rat( t.at( 2, 4 ) );
			return () -> {
				SkillSequence s = SkillSequence.start( t.hero, SkillSequence.DAWN, 3, t.hero.pos, 5, 2, Collections.emptyList() );
				s.act();
				s.act();
			};
		} );
		PROCS.put( "blood-dance", t -> {
			BloodDance b = DreadShadowProcs.skill( new BloodDance() );
			t.hero.MP = t.hero.MT = 200;
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> {
				b.execute( t.hero, Skill.AC_CAST );
				b.onHitProc( r, 5, false );
			};
		} );
		//the lodged arrow's beats and its burst
		PROCS.put( "heartseeker", t -> {
			Rat r = t.rat( t.at( 6, 4 ) );
			return () -> {
				Buff.affect( r, HeartseekerArrow.class ).set( 3 );
				for (int i = 0; i < 3; i++){
					HeartseekerArrow h = r.buff( HeartseekerArrow.class );
					if (h != null) h.act();
				}
			};
		} );
		//the ward raised, broken by a blow (the roar), and death refused
		PROCS.put( "undying-will", t -> {
			UndyingWill u = DreadShadowProcs.skill( new UndyingWill() );
			t.hero.MP = t.hero.MT = 200;
			t.hero.subClass = HeroSubClass.BERSERKER;
			t.rat( t.at( 5, 4 ) );
			t.rat( t.at( 4, 2 ) );
			return () -> {
				u.execute( t.hero, Skill.AC_CAST );
				u.incomingDamageReduction( 500, null );
				UndyingWillWard w = t.hero.buff( UndyingWillWard.class );
				if (w != null) w.absorbDamage( 500 );
			};
		} );
		//the pitch lit, an arrow catching and leaping, a burning kill bursting
		PROCS.put( "ember-arrows", t -> {
			EmberArrows e = DreadShadowProcs.skill( new EmberArrows() );
			t.hero.MP = t.hero.MT = 200;
			Rat a = t.rat( t.at( 6, 4 ) );
			t.rat( t.at( 7, 4 ) );
			Rat dead = t.rat( t.at( 6, 6 ) );
			return () -> {
				e.execute( t.hero, Skill.AC_ACTIVATE );
				e.active = true;
				e.onHitProc( a, 5, true );
				Buff.affect( dead, Burning.class ).reignite( dead );
				e.onKill( dead, true );
			};
		} );
		//the nova catching three
		PROCS.put( "frost-nova", t -> {
			FrostNova f = DreadShadowProcs.skill( new FrostNova() );
			t.hero.MP = t.hero.MT = 200;
			t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 3 ) );
			t.rat( t.at( 3, 6 ) );
			return () -> f.execute( t.hero, Skill.AC_CAST );
		} );
		//every stance aura of the rogue and the huntress built
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

	//a hurt ally rat in sight
	static Rat ally( DreadShadowProcs t, int x, int y ){
		Rat r = t.rat( t.at( x, y ) );
		r.alignment = Char.Alignment.ALLY;
		r.HP = 20;
		return r;
	}

	/** Each one's draws of the game's Random and the next value after them: { draws, next * 1e6 }. */
	public static LinkedHashMap<String, long[]> measure() throws Exception {
		WarpedRoomsTest.boot();
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals", true, RfxMeaningProcs.class.getClassLoader() );
		LinkedHashMap<String, long[]> out = new LinkedHashMap<>();
		for (Map.Entry<String, Proc> e : PROCS.entrySet()){
			DreadShadowProcs t = new DreadShadowProcs();
			t.up();
			int depth = Dungeon.depth;
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
				Dungeon.depth = depth;
				t.down();
			}
		}
		return out;
	}

	//how many draws from SEED come before `next`
	private static int count( float next ){
		Random.pushGenerator( SEED );
		try {
			for (int k = 0; k < 8192; k++){
				if (Random.Float() == next) return k;
			}
			return -1;
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void baseline() throws Exception {
		if (System.getenv( "RFX_BASELINE" ) == null) return;
		new File( "build" ).mkdirs();
		try (Writer w = new FileWriter( new File( "build/rfx-meaning-baseline.txt" ) )){
			for (Map.Entry<String, long[]> e : measure().entrySet()){
				w.write( "{ \"" + e.getKey() + "\", " + e.getValue()[0] + ", " + e.getValue()[1] + " },\n" );
			}
		}
	}
}
