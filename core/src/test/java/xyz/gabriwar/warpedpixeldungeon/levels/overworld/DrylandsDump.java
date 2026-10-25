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
 * Dumps the windows round the oasis and the buried temple nearest the origin, with their dressing
 * and their props (Drylands.art), as JSON for tools/dress_preview.py. Runs only when asked:
 *
 *   ./gradlew :core:test --tests '*DrylandsDump' -Ddress.dump=/tmp/dry [-Ddress.seed=N]
 *
 * writes /tmp/dry.oasis.json and /tmp/dry.temple.json.
 */
public class DrylandsDump {

	@Test
	public void dump() throws IOException {
		String path = System.getProperty( "dress.dump" );
		Assume.assumeTrue( "set -Ddress.dump=<file> to dump the desert's places", path != null && !path.isEmpty() );
		long seed = Long.getLong( "dress.seed", 0xBA77011L );
		for (WorldStructures.Site kind : new WorldStructures.Site[]{ WorldStructures.Site.OASIS, WorldStructures.Site.BURIED_TEMPLE }){
			java.util.ArrayList<Drylands.Place> found = Drylands.near( seed, 0, 0, kind, 40 );
			Assume.assumeFalse( "no " + kind + " near the origin", found.isEmpty() );
			Drylands.Place p = found.get( 0 );
			int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
			int ox = p.cx - w / 2, oy = p.cy - h / 2;
			GameCalendar.Season season = GameCalendar.Season.SUMMER;
			WindowGenerator.Window win = WindowGenerator.generate( seed, 0, ox, oy, WorldModel.calendarShift( season, 0.5f ) );
			int[][] dress = WindowGenerator.dress( seed, ox, oy, win.terrain, win, season );
			int[][] dry = Drylands.art( seed, ox, oy, win.terrain, w, h );
			StringBuilder sb = new StringBuilder();
			sb.append( "{\"seed\":" ).append( seed ).append( ",\"ox\":" ).append( ox ).append( ",\"oy\":" ).append( oy )
					.append( ",\"w\":" ).append( w ).append( ",\"h\":" ).append( h )
					.append( ",\"season\":\"" ).append( season ).append( "\",\"altitude\":0" );
			array( sb, "terrain", win.terrain );
			int[] frozen = new int[win.frozen.length], under = new int[w * h], tall = new int[w * h];
			for (int i = 0; i < frozen.length; i++){
				frozen[i] = win.frozen[i] ? 1 : 0;
				under[i] = win.terrain[i] == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.BOULDER
						? WindowGenerator.rockGround( win.terrain, win.frozen, i ) : -1;
				tall[i] = win.terrain[i] == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.BOULDER
						&& WindowGenerator.tallRock( seed, 0, ox + i % w, oy + i / w ) ? 1 : 0;
			}
			array( sb, "frozen", frozen );
			array( sb, "rockGround", under );
			array( sb, "tallRock", tall );
			array( sb, "ground", dress[0] );
			array( sb, "edges", dress[1] );
			array( sb, "canopy", dress[2] );
			array( sb, "dryGround", dry[0] );
			array( sb, "dryCanopy", dry[1] );
			sb.append( '}' );
			String out = path + (kind == WorldStructures.Site.OASIS ? ".oasis.json" : ".temple.json");
			try (FileWriter f = new FileWriter( out )){
				f.write( sb.toString() );
			}
			System.out.println( kind + " at " + p.cx + "," + p.cy + " dumped to " + out );
		}
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
