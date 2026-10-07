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
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaveLife;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

/**
 * The air of the world's caves (levels/overworld/CaveLife): a mote of dust drifting in a
 * chamber's still air, greyish-brown up near the surface and bluer down deep; a spore
 * glowing the colour of the fungus it came off; an ember rising off hot ground.
 */
public class CaveMoteParticle extends WeatherParticle {

	private static final int MOTE = 0, SPORE = 1, EMBER = 2;

	public static final Emitter.Factory DUST = new Factory( MOTE, false );
	public static final Emitter.Factory SPORES = new Factory( SPORE, true );
	public static final Emitter.Factory EMBERS = new Factory( EMBER, true );

	private static class Factory extends Emitter.Factory {
		private final int kind;
		private final boolean light;

		Factory( int kind, boolean light ){
			this.kind = kind;
			this.light = light;
		}

		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			((CaveMoteParticle) emitter.recycle( CaveMoteParticle.class )).reset( kind, x, y );
		}

		@Override
		public boolean lightMode(){
			return light;
		}
	}

	private int kind;
	private float phase, peak;

	public CaveMoteParticle(){
		super();
		lifespan = 5f;
	}

	public void reset( int kind, float x, float y ){
		revive();
		this.kind = kind;
		this.x = x;
		this.y = y;
		phase = Random.Float( 6.283f );
		acc.set( 0, 0 );
		int altitude = Dungeon.level instanceof OverworldLevel ? ((OverworldLevel) Dungeon.level).altitude() : -1;
		switch (kind){
			case MOTE:
				frame( WeatherSprites.SPECK_1 );
				color( altitude <= OverworldFauna.DEEP_CAVES ? 0xAEB8E6 : 0xC9BFA6 );
				speed.set( Random.Float( -2.5f, 2.5f ), Random.Float( -2f, 1.5f ) );
				left = lifespan = Random.Float( 5f, 9f );
				peak = 0.45f;
				break;
			case SPORE:
				frame( WeatherSprites.GLOW_3 );
				color( CaveLife.glowColour( altitude ) );
				speed.set( Random.Float( -2f, 2f ), Random.Float( -3f, -0.5f ) );
				left = lifespan = Random.Float( 4f, 7f );
				peak = 0.4f;
				break;
			default:
				frame( WeatherSprites.EMBER );
				color( 0xFFA040 );
				speed.set( Random.Float( -3f, 3f ), Random.Float( -14f, -8f ) );
				acc.set( 0, -2f );
				left = lifespan = Random.Float( 1.2f, 2.2f );
				peak = 0.85f;
		}
	}

	@Override
	public void update(){
		super.update();
		if (kind == EMBER){
			//cooling as it climbs: orange to a deep red
			float p = 1f - left / lifespan;
			color( lerp( 0xFFA040, 0xFF5020, p ) );
			am = envelope( 0.1f, 0.4f, peak );
		} else {
			//the still air's slow wobble
			phase += Game.elapsed * 1.3f;
			speed.x += (float)Math.sin( phase ) * 1.5f * Game.elapsed;
			am = envelope( 0.25f, 0.3f, peak );
		}
		fov();
	}

	private static int lerp( int a, int b, float p ){
		int r = (int)(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * p);
		int g = (int)(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * p);
		int bl = (int)((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * p);
		return (r << 16) | (g << 8) | bl;
	}
}
