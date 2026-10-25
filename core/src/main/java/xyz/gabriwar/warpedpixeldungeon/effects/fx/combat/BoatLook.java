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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Image;

/**
 * A sailor's boat (sea/Afloat's BOAT_RAFT and BOAT_SAIL, by name to co-op guests): the raft or the
 * sailboat drawn round him from sprites/boats.png (tools/assets/sea_art.py), its back under him and
 * its front over his feet, so he stands on the raft or in the boat; turned the way he faces; riding
 * the swell, the boat and he a pixel up, back, a pixel down and back over SWELL s (CharSprite.lift,
 * one step at a time on a slow beat: nothing flickers); a wake of rings off its stern while it moves,
 * and the water lapping at its sides now and then while it lies still. The rings are the water's own
 * (WaterFX), so they take the sea's colour and stir its fish.
 */
final class BoatLook extends StateLooks.Look {

	static final String SHEET = "sprites/boats.png";
	//a frame's size and where the sailor's middle and feet stand on it (the row below his last)
	static final int SIZE = 32, FEET_X = 16, FEET_Y = 27;
	/** A rise and fall of the swell, s, and its four steps. */
	static final float SWELL = 2.4f;
	static final int[] RISE = { 0, -1, 0, 1 };
	/** A wake's ring every WAKE_EVERY s while it moves; the water lapping every LAP_MIN-LAP_MAX s. */
	static final float WAKE_EVERY = 0.15f, LAP_MIN = 1.2f, LAP_MAX = 2.4f;
	static final float WAKE_ALPHA = 0.55f, LAP_ALPHA = 0.4f;
	//its stern and its sides from the sailor's middle, px
	static final float STERN = 10f, SIDE = 11f;

	private final int backFrame, frontFrame;
	private Image back, front;
	private float wakeIn, lapIn = 1f;
	private float lastX = Float.NaN, lastY = Float.NaN;

	BoatLook( CharSprite s, boolean sail ){
		super( s );
		backFrame = sail ? 2 : 0;
		frontFrame = sail ? 3 : 1;
	}

	/** The swell's rise `age` s in: none, a pixel up, none, a pixel down. */
	static int rise( float age ){
		return RISE[(int)(age / SWELL * RISE.length) % RISE.length];
	}

	@Override
	boolean step( float dt ){
		if (stopping){
			s.lift( 0 );
			return false;
		}
		s.lift( rise( age ) );
		float x = cx(), y = feet();
		boolean moving = !Float.isNaN( lastX ) && (Math.abs( x - lastX ) > 0.05f || Math.abs( y - lastY ) > 0.05f);
		lastX = x;
		lastY = y;
		if (!s.visible) return true;
		if (moving){
			if ((wakeIn -= dt) <= 0){
				wakeIn = WAKE_EVERY;
				float wx = x + (s.flipHorizontal ? STERN : -STERN), wy = y - 2f;
				if (WaterFX.onWater( wx, wy )){
					WaterFX.ring( wx, wy, WaterFX.S, WaterFX.liquid().foam, WAKE_ALPHA, FxBudget.P2 );
				}
			}
		} else if ((lapIn -= dt) <= 0){
			lapIn = FxRandom.Float( LAP_MIN, LAP_MAX );
			float lx = x + (FxRandom.chance( 0.5f ) ? SIDE : -SIDE), ly = y - 1f;
			if (WaterFX.onWater( lx, ly )){
				WaterFX.quietRing( lx, ly, WaterFX.S, WaterFX.liquid().foam, LAP_ALPHA, FxBudget.P3 );
			}
		}
		return true;
	}

	@Override
	void drawBehind(){
		back = draw( back, backFrame );
	}

	@Override
	void drawOver(){
		front = draw( front, frontFrame );
	}

	//one of its frames drawn at the sailor's feet, the way he faces, risen as he is
	private Image draw( Image im, int frame ){
		if (im == null){
			im = new Image( SHEET );
			im.frame( frame * SIZE, 0, SIZE, SIZE );
		}
		im.camera = s.camera();
		im.flipHorizontal = s.flipHorizontal;
		im.alpha( s.alpha() );
		im.x = cx() - FEET_X;
		im.y = feet() - FEET_Y + s.lift();
		im.draw();
		return im;
	}

	@Override
	void release(){
		s.lift( 0 );
		Image b = back, f = front;
		back = null;
		front = null;
		if (b != null) Fx.dispose( b );
		if (f != null) Fx.dispose( f );
	}
}
