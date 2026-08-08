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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;

public class MrDestructoSprite extends MobSprite {

	private int zapPos;

	public MrDestructoSprite() {
		super();

		texture( Assets.Sprites.MR_DESTRUCTO );

		TextureFilm frames = new TextureFilm( texture, 16, 18 );

		idle = new Animation( 2, true );
		idle.frames( frames, 1, 2, 3, 4 );

		run = new Animation( 12, true );
		run.frames( frames, 2, 3, 4 );

		attack = new Animation( 8, false );
		attack.frames( frames, 1, 5 );
		zap = attack.clone();

		die = new Animation( 8, false );
		die.frames( frames, 1, 0, 6 );

		play( idle );
	}

	@Override
	public void zap( int pos ) {
		zapPos = pos;
		super.zap( pos );
	}

	@Override
	public void onComplete( Animation anim ) {
		super.onComplete( anim );

		if (anim == zap) {
			idle();
			if (Actor.findChar(zapPos) != null) {
				parent.add(new Beam.DeathRay(center(), Actor.findChar(zapPos).sprite.center()));
			} else {
				parent.add(new Beam.DeathRay(center(), DungeonTilemap.raisedTileCenterToWorld(zapPos)));
			}
			Sample.INSTANCE.play( Assets.Sounds.RAY );
			((xyz.gabriwar.warpedpixeldungeon.actors.mobs.MrDestructo) ch).deathRay();
			ch.next();
		}
	}
}
