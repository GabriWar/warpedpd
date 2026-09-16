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

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;

import xyz.gabriwar.warpedpixeldungeon.effects.SupporterArt;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;

import java.io.File;

/**
 * The three supporter emblems on a window-coloured ground, each with its halo added
 * as light, the way the thank-you window shows them. For judging the art headlessly.
 *
 * java -cp ... xyz.gabriwar.warpedpixeldungeon.debug.SupporterPreview <out.png> [scale]
 */
public class SupporterPreview {

	public static void main( String[] args ) throws Exception {
		GdxNativesLoader.load();
		File out = new File( args.length > 0 ? args[0] : "/tmp/supporter.png" );
		int scale = args.length > 1 ? Integer.parseInt( args[1] ) : 6;

		Pixmap sheet = SkillTreePreview.fresh( 128, 32 );
		SupporterArt.paintSheet( sheet );

		int W = 3 * 40 + 8, H = 44;
		Pixmap pm = SkillTreePreview.fresh( W, H );
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) pm.drawPixel( x, y, SkyPaint.rgba( 0x2a2226, 0xFF ) );   //window chrome brown
		for (int t = 1; t <= 3; t++){
			int x = 8 + (t - 1) * 40 + 4, y = 10;
			SkillTreePreview.addBlit( pm, sheet, SupporterArt.HALO, x - 4, y - 4, SupporterArt.HALO_COLOR[t], SupporterArt.HALO_ALPHA[t] );
			SkillTreePreview.blit( pm, sheet, SupporterArt.emblem( t ), x, y, 0xffffff, 1f );
		}

		Pixmap big = SkillTreePreview.fresh( W * scale, H * scale );
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++){
			int p = pm.getPixel( x, y );
			for (int sy = 0; sy < scale; sy++) for (int sx = 0; sx < scale; sx++) big.drawPixel( x * scale + sx, y * scale + sy, p );
		}
		PixmapIO.writePNG( new FileHandle( out ), big );
		System.out.println( "wrote " + out );
	}
}
