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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AvatarOfLightHalo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChargedShotDraw;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.TrueEdge;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Random;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The links package's moments beyond DreadShadowProcs' (which SfxDreadShadowTest holds against
 * 72b40617b), played for real on its headless stage: Charged Shot's release and its draw, Avatar of
 * Light's sear and its +3 mend, Transcendence's split lance, True Edge's crits, Cinder
 * Trail's launches, a shards mark mending an ally, Shadow Link's shared cut and its marks, and a
 * soul mote stepped on. Each one's draws of the game's Random, counted from a seed through the call
 * and two seconds of frames after it, and the next value after them. Written against the API
 * f0783eacd already had, so the same file measures that baseline: with RFX_LINKS_BASELINE set,
 * `baseline` writes the table to build/rfx-links-baseline.txt (SfxRfxLinksTest holds it).
 */
public class RfxLinksProcs {

	static final long SEED = 2028;

	interface Proc {
		Runnable stage( DreadShadowProcs t ) throws Exception;
	}

	static final LinkedHashMap<String, Proc> PROCS = new LinkedHashMap<>();
	static {
		//the lance loosed through two enemies on her line (no bow: a spirit arrow)
		PROCS.put( "charged-shot", t -> {
			t.rat( t.at( 5, 4 ) );
			t.rat( t.at( 8, 4 ) );
			return () -> ChargedShot.fire( t.hero, t.at( 12, 4 ), 3 );
		} );
		//two turns of the draw: its aim line and her light gathering
		PROCS.put( "charged-draw", t -> {
			t.rat( t.at( 8, 4 ) );
			ChargedShotDraw d = Buff.append( t.hero, ChargedShotDraw.class );
			return () -> {
				d.set( t.at( 12, 4 ), 3, 5 );
				d.act();
				d.act();
			};
		} );
		//the halo at +3: an enemy seared, an ally mended
		PROCS.put( "avatar-halo", t -> {
			t.rat( t.at( 4, 4 ) );
			Rat ally = t.rat( t.at( 3, 3 ) );
			ally.alignment = Char.Alignment.ALLY;
			ally.HP = 10;
			AvatarOfLightHalo h = Buff.affect( t.hero, AvatarOfLightHalo.class );
			return () -> {
				h.set( 3, 5 );
				h.act();
			};
		} );
		//the lance split three ways as it leaves his back (+3; the pass itself moves the hero and the
		//fog, which the bare stage has none of: its stroke draws nothing of the game's Random)
		PROCS.put( "transcendence", t -> {
			Transcendence tr = DreadShadowProcs.skill( new Transcendence() );
			t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 5 ) );
			t.rat( t.at( 5, 3 ) );
			Method split = Transcendence.class.getDeclaredMethod( "splitLance", Hero.class, int.class, int.class, int.class, int.class );
			split.setAccessible( true );
			return () -> {
				try {
					split.invoke( tr, t.hero, t.hero.pos, 1, 5, 10 );
				} catch (ReflectiveOperationException e){
					throw new AssertionError( e );
				}
			};
		} );
		//crits cutting on through the ones behind (+3: 15% a blow, so twenty blows)
		PROCS.put( "true-edge", t -> {
			TrueEdge te = DreadShadowProcs.skill( new TrueEdge() );
			Rat a = t.rat( t.at( 4, 4 ) );
			a.HP = a.HT = 5000;
			t.rat( t.at( 5, 4 ) ).HP = 5000;
			return () -> { for (int i = 0; i < 20; i++) te.onHitProc( a, 6, false ); };
		} );
		//an orb of fire and one of frost launched
		PROCS.put( "cinder-launch", t -> {
			Rat a = t.rat( t.at( 7, 3 ) ), b = t.rat( t.at( 7, 6 ) );
			return () -> {
				ElementalOrbitFX.launch( t.hero, a.pos, 0, 2, () -> {} );
				ElementalOrbitFX.launch( t.hero, b.pos, 1, 2, () -> {} );
			};
		} );
		//a shards mark on an enemy mending an ally beside it (rank 2)
		PROCS.put( "shards-heal", t -> {
			Rat e = t.rat( t.at( 5, 4 ) );
			Rat ally = t.rat( t.at( 6, 4 ) );
			ally.alignment = Char.Alignment.ALLY;
			ally.HP = 10;
			SkillInteractions.Mark m = SkillInteractions.mark( e, SkillInteractions.Mark.SHARDS, 2, 5 );
			m.power = 6;
			return m::act;
		} );
		//Shadow Link's pair and its shared cut, and a lone link's focus
		PROCS.put( "linked-cut", t -> {
			Rat a = t.rat( t.at( 5, 4 ) ), b = t.rat( t.at( 7, 3 ) ), c = t.rat( t.at( 6, 6 ) );
			return () -> {
				DoubleStab.link( a, b, 3 );
				SkillInteractions.shareLinkedDamage( a, 10, t.hero );
				DoubleStab.link( c, c, 3 );
				SkillInteractions.shareLinkedDamage( c, 10, t.hero );
			};
		} );
		//a soul mote left by a detonation, stepped on
		PROCS.put( "soul-field", t -> {
			t.hero.MT = 100;
			t.hero.MP = 10;
			SkillField.place( t.hero, SkillField.SOUL, 3, 6, Collections.singleton( t.hero.pos ) );
			return () -> SkillInteractions.onMove( t.hero, t.at( 2, 4 ) );
		} );
	}

	/** Each one's draws of the game's Random and the next value after them: { draws, next * 1e6 }. */
	public static LinkedHashMap<String, long[]> measure() throws Exception {
		WarpedRoomsTest.boot();
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals", true, RfxLinksProcs.class.getClassLoader() );
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
		if (System.getenv( "RFX_LINKS_BASELINE" ) == null) return;
		new File( "build" ).mkdirs();
		try (Writer w = new FileWriter( new File( "build/rfx-links-baseline.txt" ) )){
			for (Map.Entry<String, long[]> e : measure().entrySet()){
				w.write( "{ \"" + e.getKey() + "\", " + e.getValue()[0] + ", " + e.getValue()[1] + " },\n" );
			}
		}
	}
}
