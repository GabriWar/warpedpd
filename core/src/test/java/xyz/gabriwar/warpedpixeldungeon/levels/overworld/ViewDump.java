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
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Dumps the views of the other bands for tools/view_preview.py, which composes each into a PNG the
 * way the game draws a slice: the view down under the terrain (hazed by its depth, the rims' shade
 * over it), the slice's own ground and dressing, the rock's tops and their rims. Runs only when asked:
 *
 *   ./gradlew :core:test --tests '*ViewDump' -Dview.dump=DIR [-Dview.seed=N]
 *
 * Three windows, each centred on a debug scene's spot (debug/ViewScenes): a pit of the caves at -2,
 * a drop of +1 over the most open water near the origin, a cliff of the surface with a tarn on its
 * top, written to DIR/pit.json, water.json and tarn.json. Every int, byte and boolean array the
 * window and its views carry goes in under its field's name, so the tool draws whatever parts the
 * views have.
 */
public class ViewDump {

	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	@Test
	public void dump() throws Exception {
		String dir = System.getProperty( "view.dump" );
		Assume.assumeTrue( "set -Dview.dump=<dir> to dump the views", dir != null && !dir.isEmpty() );
		long seed = Long.getLong( "view.seed", 0x5EED0F7EA7L );
		float summer = WorldModel.calendarShift( GameCalendar.Season.SUMMER, 0.5f );
		new File( dir ).mkdirs();

		int[] pit = ViewScenes.pit( seed, -2 );
		Assume.assumeTrue( "no pit near the origin", pit != null );
		write( dir, "pit", seed, -2, pit[0], pit[1], 0f );

		int[] water = ViewScenes.waterBelow( seed, 1, summer );
		Assume.assumeTrue( "no drop over water near the origin", water != null );
		write( dir, "water", seed, 1, water[0], water[1], summer );

		int[] tarn = ViewScenes.tarnAbove( seed, 0, summer );
		Assume.assumeTrue( "no cliff with a tarn on top near the origin", tarn != null );
		write( dir, "tarn", seed, 0, tarn[0], tarn[1], summer );
	}

	private static void write( String dir, String scene, long seed, int altitude, int cx, int cy, float shift )
			throws IOException, IllegalAccessException {
		int ox = cx - W / 2, oy = cy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( seed, altitude, ox, oy, shift );
		WindowGenerator.Views v = WindowGenerator.belowLayers( seed, ox, oy, w );
		int[][] dress = WindowGenerator.dress( seed, ox, oy, w.terrain, w, GameCalendar.Season.SUMMER );

		StringBuilder sb = new StringBuilder();
		sb.append( "{\"scene\":\"" ).append( scene ).append( '"' )
				.append( ",\"seed\":" ).append( seed ).append( ",\"altitude\":" ).append( altitude )
				.append( ",\"ox\":" ).append( ox ).append( ",\"oy\":" ).append( oy )
				.append( ",\"w\":" ).append( W ).append( ",\"h\":" ).append( H )
				.append( ",\"cx\":" ).append( W / 2 ).append( ",\"cy\":" ).append( H / 2 );
		for (Field f : WindowGenerator.Window.class.getFields()) array( sb, f.getName(), f.get( w ) );
		if (v != null){
			for (int d = 0; d < v.depth.length; d++){
				for (Field f : WindowGenerator.View.class.getFields()) array( sb, "view" + d + "_" + f.getName(), f.get( v.depth[d] ) );
			}
			array( sb, "shade", v.shade );
		}
		array( sb, "dress_ground", dress[0] );
		array( sb, "dress_edges", dress[1] );
		array( sb, "dress_canopy", dress[2] );
		sb.append( '}' );
		File out = new File( dir, scene + ".json" );
		try (FileWriter fw = new FileWriter( out )){
			fw.write( sb.toString() );
		}
		System.out.println( "[views] " + scene + ": slice " + altitude + " centred on " + cx + "," + cy + " -> " + out );
	}

	//an int, byte or boolean array as a list of numbers under its name; anything else is left out
	private static void array( StringBuilder sb, String name, Object a ){
		int n;
		if (a instanceof int[]) n = ((int[]) a).length;
		else if (a instanceof byte[]) n = ((byte[]) a).length;
		else if (a instanceof boolean[]) n = ((boolean[]) a).length;
		else return;
		sb.append( ",\"" ).append( name ).append( "\":[" );
		for (int i = 0; i < n; i++){
			if (i > 0) sb.append( ',' );
			if (a instanceof int[]) sb.append( ((int[]) a)[i] );
			else if (a instanceof byte[]) sb.append( ((byte[]) a)[i] );
			else sb.append( ((boolean[]) a)[i] ? 1 : 0 );
		}
		sb.append( ']' );
	}
}
