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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import com.watabou.noosa.particles.Emitter;

//the guardian core shares the guardians' frames, but glows pale blue and sheds snow,
//so it reads as the heart of the fight rather than one more guardian
public class IceGuardianCoreSprite extends IceGuardianSprite {

	private Emitter frost;

	@Override
	public void link( Char ch ) {
		super.link( ch );
		resetColor();
		frost = emitter();
		frost.pour( SnowParticle.FACTORY, 0.12f );
	}

	@Override
	public void resetColor() {
		super.resetColor();
		tint( 0x7FE8FF, 0.45f );
	}

	@Override
	public void update() {
		super.update();
		if (frost != null) frost.visible = visible;
	}

	@Override
	public void die() {
		super.die();
		if (frost != null) frost.on = false;
	}
}
