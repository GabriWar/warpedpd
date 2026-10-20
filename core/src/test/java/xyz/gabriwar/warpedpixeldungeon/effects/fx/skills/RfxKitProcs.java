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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Regeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Retribution;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.RighteousStrikes;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Random;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The second pass's three restored skills (Righteous Strikes, Retribution, Regeneration) played for
 * real on DreadShadowProcs' headless stage: each one's draws of the game's Random, counted from a
 * seed through the call and two seconds of frames after it (the chain's leaps, the deferred burst),
 * and the next value after them. Written against the API f0783eacd already had, so the same file
 * measures that baseline: with RFX_BASELINE set, `baseline` writes the table to
 * build/rfx-baseline.txt (SkillKitTest holds it and checks the restored code against it).
 */
public class RfxKitProcs {

	static final long SEED = 2027;

	interface Proc {
		Runnable stage( DreadShadowProcs t ) throws Exception;
	}

	static final LinkedHashMap<String, Proc> PROCS = new LinkedHashMap<>();
	static {
		//three landed blows release the light: it leaps twice and comes home (+3)
		PROCS.put( "righteous-strikes", t -> {
			RighteousStrikes rs = DreadShadowProcs.skill( new RighteousStrikes() );
			Rat a = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 3 ) );
			t.rat( t.at( 6, 4 ) );
			return () -> { for (int i = 0; i < 3; i++) rs.onHitProc( a, 5, false ); };
		} );
		//a great debt paid back, bursting on into the next (+3)
		PROCS.put( "retribution", t -> {
			Retribution r = DreadShadowProcs.skill( new Retribution() );
			Rat a = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 4 ) );
			Object debt = Buff.affect( a, Retribution.Debt.class );
			Field owed = Retribution.Debt.class.getDeclaredField( "owed" );
			owed.setAccessible( true );
			owed.setInt( debt, 20 );
			return () -> {
				r.onHitProc( a, 5, false );
				t.runDeferred();
			};
		} );
		//the skill grows
		PROCS.put( "regeneration-growth", t -> {
			Regeneration g = DreadShadowProcs.skill( new Regeneration() );
			Method up = Regeneration.class.getDeclaredMethod( "upgrade" );
			up.setAccessible( true );
			return () -> {
				try {
					up.invoke( g );
				} catch (ReflectiveOperationException e){
					throw new AssertionError( e );
				}
			};
		} );
		//the kill that leaves no enemy in sight: a second wind
		PROCS.put( "regeneration-kill", t -> {
			Regeneration g = DreadShadowProcs.skill( new Regeneration() );
			Rat dead = t.rat( t.at( 4, 4 ) );
			dead.HP = 0;
			t.hero.HP = 40;
			return () -> g.onKill( dead, false );
		} );
	}

	/** Each one's draws of the game's Random and the next value after them: { draws, next * 1e6 }. */
	public static LinkedHashMap<String, long[]> measure() throws Exception {
		WarpedRoomsTest.boot();
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals", true, RfxKitProcs.class.getClassLoader() );
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
		if (System.getenv( "RFX_BASELINE" ) == null) return;
		new File( "build" ).mkdirs();
		try (Writer w = new FileWriter( new File( "build/rfx-baseline.txt" ) )){
			for (Map.Entry<String, long[]> e : measure().entrySet()){
				w.write( "{ \"" + e.getKey() + "\", " + e.getValue()[0] + ", " + e.getValue()[1] + " },\n" );
			}
		}
	}
}
