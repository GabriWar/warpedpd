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

import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

public class TinkererSprite extends MobSprite {

    private int cellToAttack;

    public TinkererSprite() {
        super();

        texture( Assets.Sprites.UL_KING );

        TextureFilm frames = new TextureFilm( texture, 16, 16 );

        idle = new Animation( 8, true );
        idle.frames( frames, 16, 16, 16, 19 );

        run = new Animation( 12, true );
        run.frames( frames, 19, 20, 21, 22, 23, 24 );

        attack = new Animation( 12, false );
        attack.frames( frames, 16, 18, 19 );

        zap = attack.clone();

        die = new Animation( 8, false );
        die.frames( frames, 28, 29, 30, 31 );

        play( idle );
    }

    @Override
    public void attack( int cell ) {
        if (!Dungeon.level.adjacent(cell, ch.pos)) {

            cellToAttack = cell;
            turnTo( ch.pos , cell );
            play( zap );

        } else {

            super.attack( cell );

        }
    }

    @Override
    public void onComplete( Animation anim ) {
        if (anim == zap) {
            idle();

            ((MissileSprite)parent.recycle( MissileSprite.class )).
                    reset(ch.pos, cellToAttack, new Dart(), new Callback() {
                        @Override
                        public void call() {
                            ch.onAttackComplete();
                        }
                    });
        } else {
            super.onComplete(anim);
        }
    }
}
