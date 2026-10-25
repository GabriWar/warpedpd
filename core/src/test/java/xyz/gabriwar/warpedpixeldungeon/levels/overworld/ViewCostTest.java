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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.debug.ViewScenes;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * What the views of the other bands cost the window worker, on the three windows the debug scenes
 * show them in (debug/ViewScenes): a pit of the caves at -2, a drop of +1 over open water, a cliff of
 * the surface with a tarn on its top. CPU time over the calling thread and the generator's pool
 * (WindowGenerator.forRows), not the clock, so a loaded machine does not fail it; medians of five.
 * The log line ([views] ...) is the timing record: generate (the rock's tops and their dressing ride
 * on it), belowLayers (the view down) and the whole preparation a rebase waits on.
 */
public class ViewCostTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT, RUNS = 5;
	//the view down's own pass over a window, ms of CPU: the worker's budget for it
	private static final double VIEW_BUDGET = 21;

	private static final ThreadMXBean MX = ManagementFactory.getThreadMXBean();

	private static long cpuOfGenerators(){
		long t = MX.getCurrentThreadCpuTime();
		for (Thread th : Thread.getAllStackTraces().keySet()){
			if (th.getName().equals( "ow-gen" )) t += Math.max( 0, MX.getThreadCpuTime( th.getId() ) );
		}
		return t;
	}

	private static double cpuMs( Runnable r ){
		long t = cpuOfGenerators();
		r.run();
		return (cpuOfGenerators() - t) / 1e6;
	}

	//...for a pass that runs on the calling thread alone (belowLayers): no walk over the threads
	private static double cpuHereMs( Runnable r ){
		long t = MX.getCurrentThreadCpuTime();
		r.run();
		return (MX.getCurrentThreadCpuTime() - t) / 1e6;
	}

	private static double median( double[] a ){
		double[] s = a.clone();
		Arrays.sort( s );
		return s[s.length / 2];
	}

	//{generate, belowLayers, prepare} medians, ms of CPU, for the window centred on (cx, cy)
	private static double[] measure( String what, int altitude, int cx, int cy, float shift ){
		final int ox = cx - W / 2, oy = cy - H / 2;
		final WindowGenerator.Window[] w = new WindowGenerator.Window[1];
		for (int i = 0; i < 2; i++){
			w[0] = WindowGenerator.generate( SEED, altitude, ox, oy, shift );
			WindowGenerator.belowLayers( SEED, ox, oy, w[0] );
		}
		double[] gen = new double[RUNS], below = new double[RUNS], prep = new double[RUNS];
		for (int i = 0; i < RUNS; i++){
			gen[i] = cpuMs( () -> w[0] = WindowGenerator.generate( SEED, altitude, ox, oy, shift ) );
			below[i] = cpuHereMs( () -> WindowGenerator.belowLayers( SEED, ox, oy, w[0] ) );
			prep[i] = cpuMs( () -> WindowGenerator.prepare( SEED, altitude, ox, oy, GameCalendar.Season.SUMMER, null, 0, null, 0L ) );
		}
		double[] out = { median( gen ), median( below ), median( prep ) };
		System.out.println( String.format( java.util.Locale.ENGLISH,
				"[views] %s: generate %.1f ms CPU, belowLayers %.2f ms CPU, prepare %.1f ms CPU (medians of %d)",
				what, out[0], out[1], out[2], RUNS ) );
		return out;
	}

	@Test
	public void theViewsStayInTheWorkersBudget(){
		float summer = WorldModel.calendarShift( GameCalendar.Season.SUMMER, 0.5f );
		int[] pit = ViewScenes.pit( SEED, -2 );
		int[] water = ViewScenes.waterBelow( SEED, 1, summer );
		int[] tarn = ViewScenes.tarnAbove( SEED, 0, summer );
		assertNotNull( pit );
		assertNotNull( water );
		assertNotNull( tarn );
		double[][] all = {
				measure( "a pit of the caves at -2", -2, pit[0], pit[1], 0f ),
				measure( "a drop of +1 over open water", 1, water[0], water[1], summer ),
				measure( "a cliff of the surface with a tarn on top", 0, tarn[0], tarn[1], summer ) };
		for (double[] m : all){
			assertTrue( "the view down took " + m[1] + " ms CPU", m[1] < VIEW_BUDGET );
		}
	}
}
