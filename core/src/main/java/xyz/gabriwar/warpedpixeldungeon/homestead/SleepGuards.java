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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import java.util.List;

/**
 * What keeps WATCH over a sleeper: when something finds the hero asleep (Bedroll.sleep - a bedroll,
 * a bed - rolled that he is found, and made who found him: Bedroll.ambushers), every guard is asked
 * first, and the first that rouses him wakes him before they reach him - the threats' alarm bell
 * rings - so the night's visitors arrive to find him on his feet instead of in his blankets.
 *
 * A guard that rouses him says what he is told (or that it told him itself), and either leaves the
 * visitors to the bed - which sets them down farther off than on a sleeper (Bedroll.ambush) - or
 * has set them down itself (the alarm bell meets them at the edge of the home's ground).
 *
 * A guard is looked up by class name from PROVIDERS (a missing one is skipped), a public class with
 * a public no-argument constructor implementing Guard; one made in code (a test, a debug scene) is
 * added with register. Asked on the host only, once per ambush; a guard plays its own sound and
 * animation when it fires.
 *
 * <pre>
 * public class ThreatGuard implements SleepGuards.Guard {
 *     public SleepGuards.Roused rouse( Level level, int bed, Hero hero, List&lt;Mob&gt; found ){
 *         //the bell rings, tells him so and sets them down at the edge of his home's ground
 *         return AlarmBell.wakeSleeper( level, hero, found ) ? SleepGuards.Roused.placed( null ) : null;
 *     }
 * }
 * </pre>
 */
public final class SleepGuards {

	private SleepGuards(){}

	/** How a guard woke the sleeper. */
	public static final class Roused {
		/** What he is told as he wakes; null when the guard has told him itself. */
		public final String line;
		/** The guard has set the visitors down on the level itself: the bed leaves them be. */
		public final boolean placed;

		private Roused( String line, boolean placed ){
			this.line = line;
			this.placed = placed;
		}

		/** Roused, the visitors left to the bed to set down (farther off than on a sleeper). */
		public static Roused woke( String line ){
			return new Roused( line, false );
		}

		/** Roused, the visitors already set down by the guard. */
		public static Roused placed( String line ){
			return new Roused( line, true );
		}
	}

	/** Something that can wake a sleeper before what found him arrives. */
	public interface Guard {
		/** The hero, asleep on the bed at `bed`, is about to be found by `found` (made, not yet on
		 *  the level): wake him? How it did, or null when it did not. */
		Roused rouse( Level level, int bed, Hero hero, List<Mob> found );
	}

	private static final String PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/** The guards, by class name. */
	public static final String[] PROVIDERS = {
			PKG + "threats.ThreatGuard"     //threats: the alarm bell
	};

	private static final Providers<Guard> providers = new Providers<>( Guard.class, PROVIDERS );

	/** Adds a guard made in code (a test, a debug scene). */
	public static void register( Guard g ){
		providers.add( g );
	}

	public static void unregister( Guard g ){
		providers.remove( g );
	}

	/** The first guard to rouse the sleeper on the bed at `bed` before `found` reach him: how, or
	 *  null when none did and he is found asleep. */
	public static Roused rouse( Level level, int bed, Hero hero, List<Mob> found ){
		for (Guard g : providers.all()){
			Roused r = g.rouse( level, bed, hero, found );
			if (r != null) return r;
		}
		return null;
	}
}
