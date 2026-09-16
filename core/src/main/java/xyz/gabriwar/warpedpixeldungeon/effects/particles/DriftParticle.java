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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.Game;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/**
 * The one particle the weather blobs are built from: a sprite from the sheet
 * given a colour, a life, a motion, a wobble, a share of the wind and a pulse.
 * A factory configures it in a line or two, so a cloud of them can mix wisps,
 * flakes, leaves, seeds and embers.
 */
public class DriftParticle extends WeatherParticle {

	private float peak, fadeIn, fadeOut;
	private float wobble, wobblePhase, wobbleRate;
	private float windFactor, pulse;
	private boolean tumble;
	private float tumbleLeft;
	private boolean sourced;
	private float srcX, srcY;

	/** the look and the life; call first, then motion() */
	public DriftParticle look( int[] frame, int color, float life, float peak ) {
		revive();
		frame( frame );
		color( color );
		left = lifespan = life;
		this.peak = peak;
		fadeIn = 0.2f;
		fadeOut = 0.25f;
		wobble = 0;
		windFactor = 0;
		pulse = 0;
		tumble = false;
		acc.set( 0, 0 );
		speed.set( 0, 0 );
		sourced = false;
		return this;
	}

	public DriftParticle at( float x, float y ) {
		this.x = x;
		this.y = y;
		return this;
	}

	/** the ground this one belongs to, when it is drawn away from it: a cloud
	 *  hanging two cells up is seen whenever its own cell is */
	public DriftParticle from( float x, float y ) {
		sourced = true;
		srcX = x;
		srcY = y;
		return this;
	}

	/** a start speed with a random spread, and the pull on it */
	public DriftParticle motion( float vx, float vy, float spreadX, float spreadY, float ax, float ay ) {
		speed.set( vx + Random.Float( -spreadX, spreadX ), vy + Random.Float( -spreadY, spreadY ) );
		acc.set( ax, ay );
		return this;
	}

	/** a sideways sway of the given amplitude, and how much the wind carries it */
	public DriftParticle sway( float amplitude, float rate, float windFactor ) {
		wobble = amplitude;
		wobbleRate = rate;
		wobblePhase = Random.Float( (float)(Math.PI * 2) );
		this.windFactor = windFactor;
		return this;
	}

	/** a brightness that breathes at this rate */
	public DriftParticle pulse( float rate ) {
		pulse = rate;
		return this;
	}

	/** it turns over as it goes: the sprite mirrors now and then */
	public DriftParticle tumbling() {
		tumble = true;
		tumbleLeft = Random.Float( 0.15f, 0.4f );
		return this;
	}

	public DriftParticle fades( float in, float out ) {
		fadeIn = in;
		fadeOut = out;
		return this;
	}

	@Override
	public void update() {
		super.update();
		float dt = Game.elapsed;
		if (wobble > 0 || windFactor != 0) {
			wobblePhase += dt * wobbleRate;
			float wind = 0;
			if (windFactor != 0) {
				float rad = (float)Math.toRadians( ClimateManager.surfaceWindDir() );
				wind = (float)Math.sin( rad ) * ClimateManager.localWindSpeed() * windFactor * (1f + 0.4f * WeatherSprites.gust());
			}
			speed.x += ((float)Math.sin( wobblePhase ) * wobble + wind - speed.x) * Math.min( 1f, dt * 3f );
		}
		if (tumble) {
			tumbleLeft -= dt;
			if (tumbleLeft <= 0) {
				tumbleLeft = Random.Float( 0.15f, 0.4f );
				scale.x = -scale.x;
			}
		}
		float a = envelope( fadeIn, fadeOut, peak );
		if (pulse > 0) a *= 0.6f + 0.4f * (float)Math.sin( wobblePhase * pulse + x );
		am = a;
		if (sourced) fovAt( srcX, srcY ); else fov();
	}
}
