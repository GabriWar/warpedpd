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

import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.PeakLife;

/**
 * Snow torn off a frozen ridge by the wind (levels/overworld/PeakLife): a puff that streams
 * away downwind and spreads as it goes, a few loose flakes glittering in it. Nothing when
 * the wind drops below a plume's strength.
 */
public class SnowPlumeParticle extends WeatherParticle {

	public static final Emitter.Factory PLUME = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			float wind = ClimateManager.localWindSpeed();
			if (wind < PeakLife.PLUME_WIND) return;
			((SnowPlumeParticle) emitter.recycle( SnowPlumeParticle.class )).reset( x, y, wind );
			if (Random.Int( 3 ) == 0){
				((AmbientSnowParticle) emitter.recycle( AmbientSnowParticle.class )).resetDrift( x, y );
			}
		}
	};

	private boolean grown;

	public SnowPlumeParticle(){
		super();
		lifespan = 1.4f;
	}

	public void reset( float x, float y, float wind ){
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float( 1.0f, 1.8f );
		frame( WeatherSprites.PUFF_S );
		grown = false;
		color( Random.Int( 2 ) == 0 ? 0xF4F8FF : 0xE4ECF8 );
		//along the wind, AmbientSnowParticle's vector: x by the sine, y against the cosine; fast
		//enough at a plume's least wind to stream a cell off the ridge whichever way it blows
		double rad = Math.toRadians( ClimateManager.surfaceWindDir() );
		float v = Math.min( 60f, 6f + wind * 2.2f );
		speed.set( (float)Math.sin( rad ) * v + Random.Float( -3f, 3f ),
				-(float)Math.cos( rad ) * v * 0.6f );
		acc.set( 0, 0 );
	}

	@Override
	public void update(){
		super.update();
		//it spreads as it streams away
		if (!grown && left < lifespan * 0.5f){
			grown = true;
			frame( WeatherSprites.PUFF_L );
		}
		am = envelope( 0.15f, 0.5f, 0.35f );
		fov();
	}
}
