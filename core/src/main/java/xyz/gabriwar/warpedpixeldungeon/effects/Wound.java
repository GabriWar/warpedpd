/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;

//a wound opening: a white gash with a red glow about it (a copy of it twice as thick, added at
//RED_ALPHA), stretched to twice its length for the first half of its TIME_TO_FADE s and back to its
//own for the rest (whole scales, never between), fading; turned the way the blow came, to the
//nearest 45 degrees, when that is known
public class Wound extends Image {

	public static final float TIME_TO_FADE = 0.35f;
	public static final float RED_ALPHA = 0.5f;
	public static final int RED = 0xFF2020;

	private float time;

	public Wound() {
		super( Effects.get( Effects.Type.WOUND ) );
		origin.set( width / 2, height / 2 );
	}

	public void reset( int p ) {
		revive();

		x = (p % Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - width) / 2;
		y = (p / Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - height) / 2;

		time = TIME_TO_FADE;
	}

	public void reset(Visual v) {
		revive();

		point(v.center(this));
		x = Math.round( x );
		y = Math.round( y );

		time = TIME_TO_FADE;
	}

	/** Its length's scale with `p` of its life left: 2, then 1. */
	public static int stretch( float p ){
		return p > 0.5f ? 2 : 1;
	}

	/** An angle to the nearest 45 degrees. */
	public static float snap( float angle ){
		return Math.round( angle / 45f ) * 45f;
	}

	@Override
	public void update() {
		super.update();

		if ((time -= Game.elapsed) <= 0) {
			kill();
		} else {
			float p = time / TIME_TO_FADE;
			alpha((float) Math.sqrt(p));
			scale.x = stretch( p );
		}
	}

	@Override
	public void draw() {
		float a = am;
		//the red about it first, added, twice as thick
		hardlight( RED );
		am = a * RED_ALPHA;
		scale.y = 2;
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
		//then the white gash over it
		resetColor();
		am = a;
		scale.y = 1;
		super.draw();
	}

	public static void hit( Char ch ) {
		hit( ch, 0 );
	}

	public static void hit( Char ch, float angle ) {
		if (ch.sprite.parent != null) {
			Wound w = (Wound) ch.sprite.parent.recycle(Wound.class);
			ch.sprite.parent.bringToFront(w);
			w.reset(ch.sprite);
			w.angle = snap( angle );
		}
	}

	public static void hit( int pos ) {
		hit( pos, 0 );
	}

	public static void hit( int pos, float angle ) {
		Group parent = Dungeon.hero.sprite.parent;
		Wound w = (Wound)parent.recycle( Wound.class );
		parent.bringToFront( w );
		w.reset( pos );
		w.angle = snap( angle );
	}
}
