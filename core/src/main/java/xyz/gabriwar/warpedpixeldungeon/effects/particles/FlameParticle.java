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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.ColorMath;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/**
 * A tongue of fire: one of three hand-drawn flames off the weather sheet, rising,
 * leaning as it goes, and cooling along a heat ramp from white through yellow and
 * orange into a dying red. Most of them are small licks; now and then a big one
 * stands up out of the fire. Drawn in light mode, so a bed of them glows.
 *
 * Everything that burns pours these: the fire blob, torches, firebombs, the
 * blazing enchantment, elementals and the rest, all through FACTORY.
 */
public class FlameParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((FlameParticle)emitter.recycle( FlameParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	//the heat ramp, hottest first: a flame is born near white and dies deep red
	private static final int WHITE  = 0xFFF3C8;
	private static final int YELLOW = 0xFFD24A;
	private static final int ORANGE = 0xFF8418;
	private static final int RED    = 0xC22A06;

	private float sway, swayPhase, swayRate;

	public FlameParticle() {
		super();
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		//mostly small licks, with the occasional tall flame among them
		float roll = Random.Float();
		int[] f = roll < 0.55f ? WeatherSprites.FLAME_1
				: (roll < 0.88f ? WeatherSprites.FLAME_2 : WeatherSprites.FLAME_3);
		frame( f );
		//the bigger the tongue, the longer it stands
		left = lifespan = (f == WeatherSprites.FLAME_1 ? 0.42f : (f == WeatherSprites.FLAME_2 ? 0.6f : 0.8f))
				* Random.Float( 0.85f, 1.15f );

		//it lifts off, gathering speed, and drifts a little to one side
		speed.set( Random.Float( -4f, 4f ), Random.Float( -14f, -6f ) );
		acc.set( 0, Random.Float( -34f, -22f ) );

		sway = Random.Float( 3f, 9f );
		swayRate = Random.Float( 5f, 9f );
		swayPhase = Random.Float( (float)(Math.PI * 2) );

		color( WHITE );
		am = 1f;
		scale.set( 1f );
	}

	@Override
	public void update() {
		super.update();

		//p runs 0 at birth to 1 at the end of the life
		float p = 1f - left / lifespan;

		//cooling: white to yellow over the first breath, then orange, then red
		int c;
		if (p < 0.22f)      c = ColorMath.interpolate( WHITE, YELLOW, p / 0.22f );
		else if (p < 0.55f) c = ColorMath.interpolate( YELLOW, ORANGE, (p - 0.22f) / 0.33f );
		else                c = ColorMath.interpolate( ORANGE, RED, (p - 0.55f) / 0.45f );
		color( c );

		//a flame narrows as it climbs, and wags while it does
		swayPhase += com.watabou.noosa.Game.elapsed * swayRate;
		speed.x += ((float)Math.sin( swayPhase ) * sway - speed.x) * Math.min( 1f, com.watabou.noosa.Game.elapsed * 4f );
		scale.set( 1f - 0.45f * p, 1f - 0.25f * p );

		//full through the burn, snuffed out at the very end
		am = p > 0.7f ? (1f - p) / 0.3f : 1f;

		fov();
	}
}
