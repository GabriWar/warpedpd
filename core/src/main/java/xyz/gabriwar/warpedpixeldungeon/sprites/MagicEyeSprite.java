/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.TextureFilm;

public class MagicEyeSprite extends MobSprite {

	private int attackPos;

	public MagicEyeSprite() {
		super();

		texture( Assets.Sprites.MAGIC_EYE );

		TextureFilm frames = new TextureFilm( texture, 16, 18 );

		idle = new Animation( 8, true );
		idle.frames( frames, 0, 1, 2 );

		run = new Animation( 12, true );
		run.frames( frames, 5, 6 );

		attack = new Animation( 8, false );
		attack.frames( frames, 4, 3 );

		die = new Animation( 8, false );
		die.frames( frames, 7, 8, 9 );

		play( idle );
	}

	@Override
	public void attack( int cell ) {
		attackPos = cell;
		super.attack( cell );
	}

	@Override
	public void zap( int pos ) {
		attackPos = pos;
		turnTo( ch.pos, pos );
		play( attack );
	}

	@Override
	public void onComplete( Animation anim ) {
		super.onComplete( anim );
		if (anim == attack) {
			if (Dungeon.level.heroFOV[ch.pos] || Dungeon.level.heroFOV[attackPos]) {
				parent.add(new Beam.DeathRay(center(), DungeonTilemap.raisedTileCenterToWorld(attackPos)));
			}
		}
	}
}
