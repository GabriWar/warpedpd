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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Ores;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The ore scenes' finder (DebugScenes.oreShowcase): the place it picks, generated and dressed as
 * the game does it, really shows the wanted metals within eight cells of where the hero lands -
 * iron, silver and gold at -6, and skyiron on the summits seen from the slices under them.
 */
public class OreScenesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	//the metals glinting within 8 of the window's centre, in the real dressing of the real window
	private static boolean[] shown( long seed, int altitude, int[] spot ){
		int ox = spot[0] - W / 2, oy = spot[1] - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( seed, altitude, ox, oy, 0f );
		int[][] dress = WindowGenerator.dress( seed, ox, oy, w.terrain, w, GameCalendar.Season.SUMMER );
		boolean[] out = new boolean[Ores.Kind.values().length];
		int c0 = W / 2 + (H / 2) * W;
		for (int dy = -8; dy <= 8; dy++){
			for (int dx = -8; dx <= 8; dx++){
				Ores.Kind k = Ores.faceKind( dress[0][c0 + dx + dy * W] );
				if (k != null) out[k.ordinal()] = true;
			}
		}
		return out;
	}

	@Test
	public void cavesShowIronSilverAndGold(){
		long t = System.currentTimeMillis();
		int[] spot = DebugScenes.oreShowcase( SEED, -6, Ores.Kind.IRON, Ores.Kind.SILVER, Ores.Kind.GOLD );
		System.out.println( "[ore scenes] -6 found in " + (System.currentTimeMillis() - t) + " ms" );
		assertNotNull( spot );
		boolean[] shown = shown( SEED, -6, spot );
		assertTrue( "iron", shown[Ores.Kind.IRON.ordinal()] );
		assertTrue( "silver", shown[Ores.Kind.SILVER.ordinal()] );
		assertTrue( "gold", shown[Ores.Kind.GOLD.ordinal()] );
	}

	//every run of the scene goes somewhere with ore of the peaks in sight: both metals on most
	//seeds, the best place there is on the others - never nowhere
	@Test
	public void peaksShowGoldAndSkyiron(){
		int both = 0;
		for (long seed = SEED; seed < SEED + 10; seed++){
			long t = System.currentTimeMillis();
			int[] spot = DebugScenes.oreShowcase( seed, 8, Ores.Kind.GOLD, Ores.Kind.SKYIRON );
			assertNotNull( "seed " + (seed - SEED) + ": nowhere to go", spot );
			boolean[] shown = shown( seed, 8, spot );
			boolean gold = shown[Ores.Kind.GOLD.ordinal()], sky = shown[Ores.Kind.SKYIRON.ordinal()];
			System.out.println( "[ore scenes] +8 seed " + (seed - SEED) + ": gold " + gold + ", skyiron " + sky
					+ " in " + (System.currentTimeMillis() - t) + " ms" );
			assertTrue( "seed " + (seed - SEED) + ": neither metal in sight", gold || sky );
			if (gold && sky) both++;
		}
		assertTrue( "no seed of ten has skyiron and gold in sight on +8", both > 0 );
	}

	//skyiron is the summits' rock: seen from the slices under them on real peaks, in the real dressing
	@Test
	public void skyironShowsOnRealPeaks(){
		int found = 0;
		for (long seed = SEED; seed < SEED + 10 && found == 0; seed++){
			for (int a : new int[]{ 7, 8, 6 }){
				int[] spot = DebugScenes.oreShowcase( seed, a, Ores.Kind.SKYIRON );
				if (spot == null) continue;
				assertTrue( "skyiron on +" + a, shown( seed, a, spot )[Ores.Kind.SKYIRON.ordinal()] );
				found++;
			}
		}
		assertTrue( found > 0 );
	}
}
