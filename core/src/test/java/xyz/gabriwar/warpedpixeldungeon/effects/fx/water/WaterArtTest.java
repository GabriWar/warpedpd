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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import org.junit.Assume;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The live water's art (tools/water_live_gen.py): every liquid that cycles has eight frames of
 * its texture, painted only in its own colours and its highlight, its accents where they were,
 * each frame a step of a crest running across; the sheens, the pocks and the lava's glow are
 * white light that tiles at a power of two; and the tool paints the same bytes every time.
 */
public class WaterArtTest {

	//the base texture, its cycle, and the liquid it shows
	private static final Object[][] CYCLES = {
			{ Assets.Environment.WATER_SEWERS, Assets.Environment.WATER_SEWERS_CYCLE },
			{ Assets.Environment.WATER_PRISON, Assets.Environment.WATER_PRISON_CYCLE },
			{ Assets.Environment.WATER_CAVES, Assets.Environment.WATER_CAVES_CYCLE },
			{ Assets.Environment.WATER_CITY, Assets.Environment.WATER_CITY_CYCLE },
			{ Assets.Environment.WATER_HALLS, Assets.Environment.WATER_HALLS_CYCLE },
			{ Assets.Environment.WATER_TEMPLE, Assets.Environment.WATER_TEMPLE_CYCLE } };

	private static BufferedImage image( String asset ) throws IOException {
		File f = new File( "src/main/assets/" + asset );
		assertTrue( asset + " is not in the assets", f.exists() );
		return ImageIO.read( f );
	}

	private static Map<Integer, Integer> counts( BufferedImage img, int x0, int w, int h ){
		Map<Integer, Integer> out = new HashMap<>();
		for (int y = 0; y < h; y++){
			for (int x = x0; x < x0 + w; x++) out.merge( img.getRGB( x, y ) & 0xFFFFFF, 1, Integer::sum );
		}
		return out;
	}

	@Test
	public void everyLiquidCyclesEightFramesInItsOwnColours() throws IOException {
		for (Object[] c : CYCLES){
			String baseAsset = (String) c[0], cycleAsset = (String) c[1];
			Element.Liquid liquid = Element.Liquid.of( baseAsset );
			BufferedImage base = image( baseAsset ), cycle = image( cycleAsset );
			assertEquals( cycleAsset, 32, base.getWidth() );
			assertEquals( cycleAsset, 8 * 32, cycle.getWidth() );
			assertEquals( cycleAsset, 32, cycle.getHeight() );
			Map<Integer, Integer> baseCounts = counts( base, 0, 32, 32 );
			HashSet<Integer> allowed = new HashSet<>( baseCounts.keySet() );
			allowed.add( liquid.highlight );
			HashSet<Integer> frames = new HashSet<>();
			for (int k = 0; k < 8; k++){
				int hash = 0;
				for (int y = 0; y < 32; y++){
					for (int x = 0; x < 32; x++){
						int argb = cycle.getRGB( k * 32 + x, y );
						assertEquals( cycleAsset + " is opaque", 0xFF, argb >>> 24 );
						int rgb = argb & 0xFFFFFF;
						assertTrue( cycleAsset + " frame " + k + " paints " + Integer.toHexString( rgb ), allowed.contains( rgb ) );
						//an accent (a colour under 5% of the texture) stays where it was
						int was = base.getRGB( x, y ) & 0xFFFFFF;
						if (baseCounts.get( was ) < 0.05f * 1024) assertEquals( cycleAsset + " accent at " + x + "," + y, was, rgb );
						hash = hash * 31 + rgb;
					}
				}
				frames.add( hash );
			}
			assertEquals( cycleAsset + ": eight different frames", 8, frames.size() );
			//the crest is there: the highlight shows in every frame
			for (int k = 0; k < 8; k++){
				assertTrue( cycleAsset + " frame " + k + " has its crest",
						counts( cycle, k * 32, 32, 32 ).getOrDefault( liquid.highlight, 0 ) > 0 );
			}
		}
	}

	@Test
	public void theFrozenWaterHasNoCycle(){
		assertFalse( new File( "src/main/assets/environment/water_frozen_cycle.png" ).exists() );
	}

	@Test
	public void theSheensPocksAndGlowAreWhiteLightThatTiles() throws IOException {
		String[] light = { Assets.Effects.WATER_SHEEN_A, Assets.Effects.WATER_SHEEN_B, Assets.Effects.WATER_POCKS, Assets.Effects.LAVA_GLOW };
		for (String asset : light){
			BufferedImage img = image( asset );
			assertEquals( asset, 64, img.getHeight() );
			assertTrue( asset, img.getWidth() == 64 || img.getWidth() == 256 );
			int lit = 0;
			for (int y = 0; y < img.getHeight(); y++){
				for (int x = 0; x < img.getWidth(); x++){
					int argb = img.getRGB( x, y );
					if (argb >>> 24 == 0) continue;
					lit++;
					assertEquals( asset + " is white at " + x + "," + y, 0xFFFFFF, argb & 0xFFFFFF );
				}
			}
			assertTrue( asset + " has something in it", lit > 0 );
			//sparse: light laid over the water, never a wash
			if (!asset.equals( Assets.Effects.LAVA_GLOW )){
				assertTrue( asset + " is sparse: " + lit, lit < img.getWidth() * img.getHeight() / 4 );
			}
		}
	}

	@Test
	public void theToolPaintsTheSameBytesEveryTime() throws Exception {
		File tool = new File( "../tools/water_live_gen.py" );
		Assume.assumeTrue( tool.exists() );
		Process p;
		try {
			p = new ProcessBuilder( "python3", tool.getPath(), "--check" ).redirectErrorStream( true ).start();
		} catch (IOException e){
			Assume.assumeNoException( "no python3 here", e );
			return;
		}
		StringBuilder out = new StringBuilder();
		try (BufferedReader r = new BufferedReader( new InputStreamReader( p.getInputStream() ) )){
			String line;
			while ((line = r.readLine()) != null) out.append( line ).append( '\n' );
		}
		assertTrue( p.waitFor( 120, TimeUnit.SECONDS ) );
		//no PIL on this machine: nothing to compare with
		Assume.assumeFalse( out.toString().contains( "No module named" ) );
		assertEquals( out.toString(), 0, p.exitValue() );
	}
}
