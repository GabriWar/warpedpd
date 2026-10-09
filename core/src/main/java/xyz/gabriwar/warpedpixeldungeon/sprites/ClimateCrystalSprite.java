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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.ClimateCrystalWard;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;

//a placed Climate Crystal: an ice shard with an ember core floating over its shadow,
//bobbing, pulsing and shedding glints (frames 0-7). When its day is spent it does not
//break: it drops, bounces once and rests dim on the ground (frames 8-12).
public class ClimateCrystalSprite extends MobSprite {

	private Animation fall;
	private Animation dimmed;

	private Emitter glints;

	public ClimateCrystalSprite() {
		super();

		texture( Assets.Sprites.CLIMATE_CRYSTAL );
		TextureFilm frames = new TextureFilm( texture, 16, 20 );

		idle = new Animation( 8, true );
		idle.frames( frames, 0, 1, 2, 3, 4, 5, 6, 7 );

		fall = new Animation( 12, false );
		fall.frames( frames, 8, 9, 10, 11, 12 );

		dimmed = new Animation( 1, true );
		dimmed.frames( frames, 12 );

		//it never moves or fights, but the slots must be distinct objects
		run = idle.clone();
		attack = idle.clone();

		die = new Animation( 10, false );
		die.frames( frames, 12 );

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		glints = emitter();
		glints.pour( Speck.factory( Speck.LIGHT ), 0.5f );
		if (ch instanceof ClimateCrystalWard && ((ClimateCrystalWard) ch).dormant()) {
			glints.on = false;
			idle = dimmed;
			play( dimmed );
		}
	}

	//its day is spent: the glow goes out and it drops to the floor
	public void dim() {
		if (glints != null) glints.on = false;
		idle = dimmed;
		play( fall );
	}

	@Override
	public void onComplete( Animation anim ) {
		super.onComplete( anim );
		if (anim == fall) {
			SpatialSound.play( Assets.Sounds.ROCKS_LIGHT, ch, 0.5f, 1.4f );
			play( dimmed );
		}
	}

	@Override
	public void update() {
		super.update();
		if (glints != null) glints.visible = visible;
	}

	@Override
	public void kill() {
		super.kill();
		if (glints != null) glints.on = false;
	}
}
