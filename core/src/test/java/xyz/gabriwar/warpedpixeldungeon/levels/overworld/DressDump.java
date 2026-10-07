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
import org.junit.Assume;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Dumps one surface window with its dressing as JSON for tools/dress_preview.py,
 * which composes it with the real tilesets into a PNG. Runs only when asked:
 *
 *   ./gradlew :core:test --tests '*DressDump' -Ddress.dump=/tmp/dress.json \
 *       [-Ddress.seed=N] [-Ddress.x=X -Ddress.y=Y] [-Ddress.season=WINTER] [-Ddress.altitude=A]
 *
 * Without a centre, the nearest place where rocky foothills meet a snowfield
 * is used (WindowGenerator.findRockyEdge), the same spot the overworld-edges
 * debug scene goes to. On another slice (-Ddress.altitude, caves below 0, peaks
 * above) it is the place the ore scenes go to (DebugScenes.oreShowcase): veins of
 * iron, silver and gold in the caves, gold and skyiron on the peaks.
 */
public class DressDump {

	@Test
	public void dump() throws IOException {
		String path = System.getProperty( "dress.dump" );
		Assume.assumeTrue( "set -Ddress.dump=<file> to dump a dressed window", path != null && !path.isEmpty() );
		long seed = Long.getLong( "dress.seed", 0x5EED0F7EA7L );
		GameCalendar.Season season = GameCalendar.Season.valueOf( System.getProperty( "dress.season", "WINTER" ) );
		int altitude = Integer.getInteger( "dress.altitude", 0 );
		int cx, cy;
		if (System.getProperty( "dress.x" ) != null){
			cx = Integer.getInteger( "dress.x" );
			cy = Integer.getInteger( "dress.y" );
		} else if (altitude != 0){
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest.boot();
			int[] c = altitude < 0
					? xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes.oreShowcase( seed, altitude, Ores.Kind.IRON, Ores.Kind.SILVER, Ores.Kind.GOLD )
					: xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes.oreShowcase( seed, altitude, Ores.Kind.GOLD, Ores.Kind.SKYIRON );
			Assume.assumeTrue( "no ore showcase on slice " + altitude + " near the origin", c != null );
			cx = c[0]; cy = c[1];
		} else {
			int[] c = WindowGenerator.findRockyEdge( seed );
			Assume.assumeTrue( "no rocky snow edge near the origin", c != null );
			cx = c[0]; cy = c[1];
		}
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		int ox = cx - w/2, oy = cy - h/2;
		float shift = WorldModel.calendarShift( season, 0.5f );
		WindowGenerator.Window win = WindowGenerator.generate( seed, altitude, ox, oy, shift );
		int[][] dress = WindowGenerator.dress( seed, ox, oy, win.terrain, win, season );

		StringBuilder sb = new StringBuilder();
		sb.append( "{\"seed\":" ).append( seed ).append( ",\"ox\":" ).append( ox ).append( ",\"oy\":" ).append( oy )
				.append( ",\"w\":" ).append( w ).append( ",\"h\":" ).append( h )
				.append( ",\"season\":\"" ).append( season ).append( '"' )
				.append( ",\"altitude\":" ).append( altitude );
		array( sb, "terrain", win.terrain );
		int[] frozen = new int[win.frozen.length];
		int[] under = new int[win.terrain.length];
		int[] tall = new int[win.terrain.length];
		for (int i = 0; i < frozen.length; i++){
			frozen[i] = win.frozen[i] ? 1 : 0;
			under[i] = win.terrain[i] == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.BOULDER
					? WindowGenerator.rockGround( win.terrain, win.frozen, i ) : -1;
			tall[i] = win.terrain[i] == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.BOULDER
					&& WindowGenerator.tallRock( seed, altitude, ox + i % w, oy + i / w ) ? 1 : 0;
		}
		array( sb, "frozen", frozen );
		array( sb, "rockGround", under );
		array( sb, "tallRock", tall );
		array( sb, "ground", dress[0] );
		array( sb, "edges", dress[1] );
		array( sb, "canopy", dress[2] );
		sb.append( '}' );
		try (FileWriter out = new FileWriter( path )){
			out.write( sb.toString() );
		}
		System.out.println( "dressed window dumped to " + path + " (centre " + cx + "," + cy + ")" );
	}

	private static void array( StringBuilder sb, String name, int[] a ){
		sb.append( ",\"" ).append( name ).append( "\":[" );
		for (int i = 0; i < a.length; i++){
			if (i > 0) sb.append( ',' );
			sb.append( a[i] );
		}
		sb.append( ']' );
	}
}
