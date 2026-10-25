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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * A star come out in the dark of a solar eclipse (the weather's CORONA, beside its motes;
 * levels/overworld/EclipseEvent): a pale point held still on the screen - a star is as far off as
 * anything gets, so it keeps its place in the view as the hero walks - that comes up slowly as the
 * moon covers the sun, breathes softly brighter and dimmer (never a flicker) and fades again. Only
 * in the deepest of the dark and under the open sky: none is made while the sun is still half out,
 * nor under the rock. One a little under every EVERY seconds, each LIFE seconds or so: a few in the
 * sky at once. A bright one in four is a small cross, the rest a single point. Its numbers are the
 * effects' own (FxRandom), drawn as it is made, never for one that would not show.
 */
public class EclipseStarParticle extends WeatherParticle {

	/** A star comes out every this many seconds. */
	public static final float EVERY = 1.3f;
	/** ...and lasts this long, give or take. */
	public static final float LIFE = 8f;
	/** It shows only where the moon covers more of the sun than this, fully from FULL. */
	public static final float FROM = 0.55f, FULL = 0.9f;

	public static final Emitter.Factory FACTORY = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			//none made while it would not show: the sun still half out, or no sky over the hero
			//(the corona's air reaches the dungeon's upper floors and the world's caves)
			if (ClimateManager.eclipseDepth() <= FROM || !(Dungeon.level instanceof OverworldLevel)
					|| !((OverworldLevel) Dungeon.level).openSky()) return;
			((EclipseStarParticle) emitter.recycle( EclipseStarParticle.class )).reset( x, y );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	//where it stands on the screen, from the camera's scroll; its breathing
	private float sx, sy, phase, rate, peak;

	public EclipseStarParticle(){
		super();
	}

	public void reset( float x, float y ){
		revive();
		Camera cam = Camera.main;
		sx = x - (cam != null ? cam.scroll.x : 0f);
		sy = y - (cam != null ? cam.scroll.y : 0f);
		this.x = x;
		this.y = y;
		left = lifespan = LIFE * FxRandom.Float( 0.8f, 1.25f );
		boolean bright = FxRandom.Int( 4 ) == 0;
		frame( bright ? WeatherSprites.FLAKE_3 : WeatherSprites.FLAKE_1 );
		//cold white, now and then a warmer one
		color( FxRandom.Int( 3 ) == 0 ? 0xFFF2D8 : 0xE6EEFF );
		speed.set( 0, 0 );
		acc.set( 0, 0 );
		phase = FxRandom.Float( 6.283f );
		rate = FxRandom.Float( 1.2f, 2.2f );
		peak = bright ? 0.95f : FxRandom.Float( 0.55f, 0.8f );
	}

	@Override
	public void update(){
		super.update();
		Camera cam = Camera.main;
		if (cam != null){
			x = cam.scroll.x + sx;
			y = cam.scroll.y + sy;
		}
		phase += Game.elapsed * rate;
		float breathe = 0.78f + 0.22f * (float) Math.sin( phase );
		float d = ClimateManager.eclipseDepth();
		float dark = Math.max( 0f, Math.min( 1f, (d - FROM) / (FULL - FROM) ) );
		am = envelope( 0.25f, 0.25f, peak ) * breathe * dark;
	}
}
