/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;

/**
 * Raw sunlight: the tallest shaft (SHAFT_40) in the sun's white-gold (FFFDE8, one colour: it was a
 * random one of every colour under FEFF79), brighter than a holy shaft (0.55), in the light layer.
 * NetVisuals' "sunlight".
 */
public class SunlightParticle extends ShaftParticle {

	public static final int COLOR = Nature.SUN;
	public static final float SUN_PEAK = 0.55f;

	public static final Factory FACTORY = new Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y ) {
			if (!allowed( FxBudget.P2, x, y )) return;
			Emitter carrier = Fx.lightCarrier( SunlightParticle.class );
			Emitter into = carrier != null ? carrier : emitter;
			((SunlightParticle) into.recycle( SunlightParticle.class ))
					.shaft( x, y, FxFrames.Light.SHAFT_40, COLOR, SUN_PEAK, LIFE )
					.delay( FxRandom.Float( MAX_DELAY ) );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** A sun shaft standing at (x, y), at once. */
	@Override
	public SunlightParticle reset( float x, float y ) {
		shaft( x, y, FxFrames.Light.SHAFT_40, COLOR, SUN_PEAK, LIFE );
		return this;
	}
}
