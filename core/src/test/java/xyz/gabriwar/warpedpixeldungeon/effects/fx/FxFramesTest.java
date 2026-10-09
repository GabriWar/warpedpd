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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The kit's frame tables (FxFrames, FxFrames.Light) against the sheets the tools painted: every
 * frame lies inside its sheet, has something drawn in it and overlaps no other; a light frame is
 * white (but the prism's coloured rows), a matter frame grey at full alpha; the rings and glows
 * are where their lookups say.
 */
public class FxFramesTest {

	private static final class Named {
		final String name;
		final int[] r;
		Named( String name, int[] r ){ this.name = name; this.r = r; }
	}

	//every { x, y, w, h } a class declares, sequences flattened
	private static List<Named> frames( Class<?> c ) throws IllegalAccessException {
		List<Named> out = new ArrayList<>();
		for (Field f : c.getDeclaredFields()){
			if (!Modifier.isStatic( f.getModifiers() ) || !Modifier.isPublic( f.getModifiers() )) continue;
			//lookup tables, not frames
			if (f.getName().endsWith( "_WIDTHS" ) || f.getName().endsWith( "_SIZES" )
					|| f.getName().equals( "RINGS" ) || f.getName().equals( "GLOWS" ) || f.getName().equals( "POOLS" )) continue;
			Object v = f.get( null );
			if (v instanceof int[]){
				out.add( new Named( f.getName(), (int[]) v ) );
			} else if (v instanceof int[][]){
				int[][] seq = (int[][]) v;
				assertTrue( f.getName() + " is an empty sequence", seq.length > 0 );
				for (int i = 0; i < seq.length; i++) out.add( new Named( f.getName() + "[" + i + "]", seq[i] ) );
			}
		}
		return out;
	}

	private static BufferedImage sheet( String asset ) throws IOException {
		File f = new File( "src/main/assets/" + asset );
		assertTrue( asset + " is not in the assets", f.exists() );
		return ImageIO.read( f );
	}

	//every frame inside the sheet, drawn, grey (white light or grey matter: the game tints them),
	//but the prism's coloured rows; none overlapping another
	private static void check( List<Named> frames, BufferedImage sheet ){
		assertTrue( sheet.getWidth() <= 256 && sheet.getHeight() <= 256 );
		for (Named n : frames){
			int[] r = n.r;
			assertEquals( n.name, 4, r.length );
			assertTrue( n.name + " out of the sheet", r[0] >= 0 && r[1] >= 0 && r[2] > 0 && r[3] > 0
					&& r[0] + r[2] <= sheet.getWidth() && r[1] + r[3] <= sheet.getHeight() );
			boolean drawn = false;
			for (int y = r[1]; y < r[1] + r[3]; y++){
				for (int x = r[0]; x < r[0] + r[2]; x++){
					int argb = sheet.getRGB( x, y );
					int a = argb >>> 24, red = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
					if (a == 0) continue;
					drawn = true;
					if (n.name.startsWith( "PRISM" )) continue;
					assertTrue( n.name + " is not grey at " + x + "," + y, red == g && g == b );
				}
			}
			assertTrue( n.name + " has nothing drawn in it", drawn );
		}
		for (int i = 0; i < frames.size(); i++){
			int[] a = frames.get( i ).r;
			for (int j = i + 1; j < frames.size(); j++){
				int[] b = frames.get( j ).r;
				boolean apart = a[0] + a[2] <= b[0] || b[0] + b[2] <= a[0] || a[1] + a[3] <= b[1] || b[1] + b[3] <= a[1];
				assertTrue( frames.get( i ).name + " overlaps " + frames.get( j ).name, apart );
			}
		}
	}

	@Test
	public void theSpriteSheetsFramesLieInsideItApartAndDrawn() throws Exception {
		List<Named> frames = frames( FxFrames.class );
		assertTrue( frames.size() > 150 );
		check( frames, sheet( Assets.Effects.FX_SHEET ) );
	}

	@Test
	public void theLightSheetsFramesLieInsideItApartAndDrawn() throws Exception {
		List<Named> frames = frames( FxFrames.Light.class );
		assertTrue( frames.size() > 50 );
		BufferedImage sheet = sheet( Assets.Effects.FX_LIGHT );
		check( frames, sheet );
		//white light but the prism's rows, and a glow's middle pixel at full
		for (Named n : frames){
			if (n.name.startsWith( "PRISM" )) continue;
			for (int y = n.r[1]; y < n.r[1] + n.r[3]; y++){
				for (int x = n.r[0]; x < n.r[0] + n.r[2]; x++){
					int argb = sheet.getRGB( x, y );
					if ((argb >>> 24) != 0) assertEquals( n.name, 0xFFFFFF, argb & 0xFFFFFF );
				}
			}
		}
		for (int i = 0; i < FxFrames.Light.GLOWS.length; i++){
			int[] g = FxFrames.Light.GLOWS[i];
			assertEquals( FxFrames.Light.GLOW_SIZES[i], g[2] );
			assertEquals( 255, sheet.getRGB( g[0] + g[2] / 2, g[1] + g[3] / 2 ) >>> 24 );
		}
	}

	@Test
	public void theRingsAreTwoToOneAndFoundByWidth(){
		for (int i = 0; i < FxFrames.RINGS.length; i++){
			int[] r = FxFrames.RINGS[i];
			assertEquals( FxFrames.RING_WIDTHS[i], r[2] );
			assertTrue( "2:1", Math.abs( r[2] - 2 * r[3] ) <= 7 );
			assertEquals( i, FxFrames.ringIndex( FxFrames.RING_WIDTHS[i] ) );
		}
		assertEquals( 0, FxFrames.ringIndex( 1 ) );
		assertEquals( 2, FxFrames.ringIndex( 16 ) );
		assertEquals( FxFrames.RINGS.length - 1, FxFrames.ringIndex( 500 ) );
		assertEquals( FxFrames.Light.GLOW_15, FxFrames.Light.glow( 15 ) );
		assertEquals( FxFrames.Light.GLOW_15, FxFrames.Light.glow( 14 ) );
		assertEquals( FxFrames.Light.POOL_63, FxFrames.Light.pool( 200 ) );
		assertFalse( FxFrames.Light.pool( 15 ) == FxFrames.Light.pool( 23 ) );
	}
}
