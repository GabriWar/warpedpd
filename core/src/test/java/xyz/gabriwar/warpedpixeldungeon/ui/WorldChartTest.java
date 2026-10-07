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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The world chart paints a peak as its slices (WorldChart.bandColour): a frozen band is
 * snow, a warm one meadow, a tarn its own blue, and every band a shade paler than the one
 * under it, so the contours a climber takes one at a time can be counted on the map.
 */
public class WorldChartTest {

	private static int luma( int rgb ){
		return ((rgb >> 16) & 0xFF) * 3 + ((rgb >> 8) & 0xFF) * 6 + (rgb & 0xFF);
	}

	//a cell of the seed no slice holds a tarn on, so the ground colours are the ground's
	private static int[] dryCell( long seed ){
		for (int wx = 0; wx < 400; wx++){
			boolean dry = true;
			for (int b = 1; b <= WorldLayers.MAX_ABOVE && dry; b++) dry = !WorldModel.tarnAt( seed, wx, 7, b );
			if (dry) return new int[]{ wx, 7 };
		}
		throw new AssertionError( "every cell of the row holds a tarn on some slice" );
	}

	@Test
	public void frozenBandsAreSnowAndWarmOnesMeadowEachPalerThanTheLast(){
		long seed = 11L;
		int[] c = dryCell( seed );
		WorldModel.Sample cold = new WorldModel.Sample();
		cold.temperature = 0.1f;
		WorldModel.Sample warm = new WorldModel.Sample();
		warm.temperature = 0.9f;
		int lastSnow = -1, lastMeadow = -1;
		for (int b = 1; b <= WorldLayers.MAX_ABOVE; b++){
			int snow = WorldChart.bandColour( seed, c[0], c[1], b, cold );
			int meadow = WorldChart.bandColour( seed, c[0], c[1], b, warm );
			//snow is white-ish: no channel far from another; the meadow is green
			assertTrue( "snow at band " + b, (snow & 0xFF) > 200 && ((snow >> 16) & 0xFF) > 200 );
			assertTrue( "meadow at band " + b, ((meadow >> 8) & 0xFF) > ((meadow >> 16) & 0xFF) );
			assertTrue( "paler snow at band " + b, luma( snow ) > lastSnow );
			assertTrue( "paler meadow at band " + b, luma( meadow ) > lastMeadow );
			lastSnow = luma( snow );
			lastMeadow = luma( meadow );
		}
	}

	@Test
	public void aTarnIsBlueAndIceWhenFrozen(){
		long seed = 11L;
		outer:
		for (int wy = 0; wy < 400; wy++){
			for (int wx = 0; wx < 400; wx++){
				if (!WorldModel.tarnAt( seed, wx, wy, 3 )) continue;
				WorldModel.Sample cold = new WorldModel.Sample();
				cold.temperature = 0.1f;
				WorldModel.Sample warm = new WorldModel.Sample();
				warm.temperature = 0.9f;
				int water = WorldChart.bandColour( seed, wx, wy, 3, warm );
				int ice = WorldChart.bandColour( seed, wx, wy, 3, cold );
				assertTrue( "water is blue", (water & 0xFF) > ((water >> 16) & 0xFF) + 60 );
				assertTrue( "ice is pale", luma( ice ) > luma( water ) + 400 );
				assertNotEquals( water, ice );
				break outer;
			}
		}
	}

	@Test
	public void theSurfaceKeepsItsBiomeColours(){
		assertEquals( 0x1b4d7a, WorldChart.biomeColour( WorldModel.Biome.OCEAN ) );
		assertEquals( 0xeef4f8, WorldChart.biomeColour( WorldModel.Biome.SNOWFIELD ) );
	}
}
