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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import com.watabou.noosa.Game;
import com.watabou.noosa.Visual;

/**
 * A charge's anticipation (an eye, a sentry gathering a beam): a small glow at the point the
 * motes gather into, rising from nothing to PEAK over RISE s and holding there breathing, put out
 * when the charge ends. Its owner calls update() each frame (render thread) and release() when it
 * goes.
 */
public final class ChargeGlow {

	/** Seconds to full, and its alpha at full. */
	public static final float RISE = 0.8f, PEAK = 0.6f;
	/** Its glow's size, px. */
	public static final int SIZE = 7;

	private FxLight light;
	private float level;

	/** The share of PEAK it shows now, 0..1. */
	public float level(){
		return level;
	}

	/** A frame: charging or not, at a visual's middle, in a colour. */
	public void update( Visual at, boolean charging, int color ){
		if (!charging){
			release();
			return;
		}
		level = Math.min( 1f, level + Game.elapsed / RISE );
		if (light == null || !light.exists){
			light = Arc.glow( at.x + at.width() / 2f, at.y + at.height() / 2f, SIZE, color, 0.01f );
			if (light == null) return;
			light.flicker( FxLight.BREATH ).follow( at, 0, 0 );
		}
		light.level( Math.min( 1f, level * PEAK / FxLight.GLOW_CAP ) );
	}

	/** The charge is over (or its owner gone): its glow put out quickly. */
	public void release(){
		if (light != null) light.putOut( 0.15f );
		light = null;
		level = 0;
	}
}
