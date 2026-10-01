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

import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * A streak of moving air in The Bellows: a pale line, drawn out along the wind, that
 * crosses a tile or two in the direction the draught blows and fades at both ends.
 */
public class DraftParticle extends PixelParticle {

	public DraftParticle(){
		super();
		color( 0xDDEEF2 );
		lifespan = 0.55f;
	}

	public void reset( float x, float y, int dx, int dy ){
		revive();
		left = lifespan = Random.Float( 0.5f, 0.9f );
		this.x = x;
		this.y = y;
		float pace = Random.Float( 30f, 48f );
		speed.set( dx * pace, dy * pace );
		//drawn out along the wind: a line of air, four to seven pixels long and one thick
		float length = Random.Float( 4f, 7f );
		scale.set( dx != 0 ? length : 1f, dy != 0 ? length : 1f );
		am = 0;
	}

	@Override
	public void update(){
		super.update();
		float p = left / lifespan;
		am = (p < 0.5f ? p : 1f - p) * 1.6f;
	}
}
