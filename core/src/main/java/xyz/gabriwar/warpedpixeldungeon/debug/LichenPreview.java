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

import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;
import xyz.gabriwar.warpedpixeldungeon.sprites.LichenSprite;

import java.io.File;

/** The seven lichen frames on a sewer-green ground, upscaled, for judging headlessly. */
public class LichenPreview {

	public static void main( String[] args ) throws Exception {
		GdxNativesLoader.load();
		File out = new File( args.length > 0 ? args[0] : "/tmp/lichen.png" );
		int scale = args.length > 1 ? Integer.parseInt( args[1] ) : 8;
		int W = 7 * 20 + 4, H = 24;
		Pixmap pm = SkillTreePreview.fresh( W, H );
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) pm.drawPixel( x, y, SkyPaint.rgba( 0x2f3a2a, 0xFF ) );
		for (int i = 0; i < 7; i++) LichenSprite.paint( pm, 4 + i * 20, i );
		//paint() writes frame i at x offset; shift rows down so the mound sits on the ground line
		Pixmap big = SkillTreePreview.fresh( W * scale, H * scale );
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++){
			int p = pm.getPixel( x, y );
			for (int sy = 0; sy < scale; sy++) for (int sx = 0; sx < scale; sx++) big.drawPixel( x * scale + sx, y * scale + sy, p );
		}
		PixmapIO.writePNG( new FileHandle( out ), big );
		System.out.println( "wrote " + out );
	}
}
