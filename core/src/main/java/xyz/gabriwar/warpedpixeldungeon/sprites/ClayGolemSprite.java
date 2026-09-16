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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.noosa.TextureFilm;

//remastered from the original Unleashed PD golem: same frames, fired clay, kiln eyes
public class ClayGolemSprite extends MobSprite {

    public ClayGolemSprite() {
        super();

        texture( Assets.Sprites.CLAY_GOLEM );

        TextureFilm frames = new TextureFilm( texture, 16, 16 );

        idle = new Animation( 4, true );
        idle.frames( frames, 0, 1 );

        run = new Animation( 12, true );
        run.frames( frames, 2, 3, 4, 3 );

        attack = new Animation( 10, false );
        attack.frames( frames, 9, 10, 9 );

        die = new Animation( 15, false );
        die.frames( frames, 5, 6, 7, 8 );

        play( idle );
    }

    @Override
    public int blood() {
        return 0xFF9A5E34;
    }

    @Override
    public void onComplete( Animation anim ) {
        if (anim == die) {
            emitter().burst( Speck.factory( Speck.ROCK ), 5 );
        }
        super.onComplete( anim );
    }
}
