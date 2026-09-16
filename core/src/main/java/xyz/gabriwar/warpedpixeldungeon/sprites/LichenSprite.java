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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.TextureFilm;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The lichen: a crust of pale green shed off the hero, rooted where it lands. Sprouted
 * never drew one (it borrowed Mr Destructo's sprite), so this one is painted at native
 * size: a low mound of grey-green scales with rust-coloured cups, breathing slowly,
 * flaring its edge when it lashes, and crumbling to dust when it dies.
 */
public class LichenSprite extends MobSprite {

	private static final Object KEY = "lichen-sprite";
	private static final int F = 16;          //frame size
	private static final int FRAMES = 7;      //idle a, idle b, attack a, attack b, die a, die b, die c

	public LichenSprite() {
		super();

		texture( sheet() );
		TextureFilm frames = new TextureFilm( texture, F, F );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1 );

		run = new Animation( 2, true );
		run.frames( frames, 0, 1 );

		attack = new Animation( 12, false );
		attack.frames( frames, 2, 3, 2, 0 );

		die = new Animation( 8, false );
		die.frames( frames, 4, 5, 6 );

		play( idle );
	}

	@Override
	public int blood() {
		return 0xFF8fae6a;
	}

	private static SmartTexture sheet(){
		SmartTexture tx;
		if (TextureCache.contains( KEY )){
			tx = TextureCache.get( KEY );
		} else {
			tx = TextureCache.create( KEY, F * FRAMES, F );
			Pixmap pm = tx.bitmap;
			pm.setBlending( Pixmap.Blending.None );
			pm.setColor( 0 ); pm.fill();
			for (int i = 0; i < FRAMES; i++) paint( pm, i * F, i );
			tx.bitmap( pm );
			tx.filter( Texture.NEAREST, Texture.NEAREST );
		}
		return tx;
	}

	/**
	 * One frame. The mound is an ellipse low in the cell; its edge is a ring of scales,
	 * lighter toward the top-left. Frame 1 breathes (a pixel taller), frames 2 and 3
	 * throw the edge outward as spines, frames 4 to 6 shrink and grey out.
	 */
	public static void paint( Pixmap pm, int ox, int frame ){
		int outline = 0x1e2a1a, scale = 0x8fae6a, scaleLit = 0xc4d8a4, scaleDark = 0x5c7a48;
		int crust = 0x6f8c58, cup = 0xb86a3a, cupLit = 0xe0a060, dust = 0x8a8a80;
		float breathe = frame == 1 ? 1f : 0f;
		float dying = frame >= 4 ? (frame - 3) / 3f : 0f;       //0.33, 0.66, 1
		boolean spines = frame == 2 || frame == 3;
		float rx = 6.5f - dying * 2f, ry = 4.2f + breathe * 0.6f - dying * 1.5f;
		float cx = 8f, cy = 11f - breathe * 0.4f;

		for (int y = 0; y < F; y++){
			for (int x = 0; x < F; x++){
				float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
				float d = dx * dx + dy * dy;
				if (d > 1.35f) continue;
				int col;
				if (d > 1f){
					//just outside: the outline, and on the lash frames a ring of spines
					boolean spine = spines && ((x + y + frame) % 3 == 0) && d < 1.35f;
					if (d < 1.15f || spine) col = spine ? scaleDark : outline; else continue;
				} else if (d > 0.72f){
					//the rim of scales, lit from the top-left
					float lit = clamp01( 0.5f - dx * 0.5f - dy * 0.6f );
					col = mix( scaleDark, scaleLit, lit );
					if (((x * 7 + y * 3) & 3) == 0) col = mix( col, scale, 0.5f );
				} else {
					//the crust, with cups scattered over it
					int n = hash( 0x11C4E, x, y );
					col = mix( crust, scale, (n % 5) / 8f );
					if (n % 11 == 0 && d < 0.6f) col = cup;
					if (n % 11 == 1 && d < 0.6f) col = cupLit;
				}
				if (dying > 0){
					col = mix( col, dust, dying * 0.8f );
					if (hash( 0xD157, x, y + frame ) % 3 == 0 && dying > 0.5f) continue;   //crumbling away
				}
				px( pm, ox + x, y, col );
			}
		}
		//a few motes above a dying lichen
		if (dying > 0){
			for (int k = 0; k < 4; k++){
				int mx = 3 + (hash( 0x5A, k, frame ) % 10), my = 2 + (hash( 0x5B, k, frame ) % 5);
				px( pm, ox + mx, my, dust );
			}
		}
	}
}
