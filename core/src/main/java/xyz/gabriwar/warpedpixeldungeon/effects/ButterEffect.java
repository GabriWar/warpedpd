/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

public class ButterEffect extends Gizmo {

    private static final int BUTTER_COLOR = 0xF0E691;

    private float phase;
    private CharSprite target;

    public ButterEffect(CharSprite target) {
        super();
        this.target = target;
        phase = 0;
    }

    @Override
    public void update() {
        super.update();
        if ((phase += Game.elapsed * 2) < 1) {
            target.tint(BUTTER_COLOR, phase * 0.6f);
        } else {
            target.tint(BUTTER_COLOR, 0.6f);
        }
    }

    public void dissapear() {
        target.resetColor();
        killAndErase();
        if (visible) {
            Splash.at(target.center(), BUTTER_COLOR, 5);
        }
    }

    public static ButterEffect butter(CharSprite sprite) {
        ButterEffect butter = new ButterEffect(sprite);
        if (sprite.parent != null)
            sprite.parent.add(butter);
        return butter;
    }
}
