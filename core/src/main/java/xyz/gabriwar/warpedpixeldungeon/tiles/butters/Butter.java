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

package xyz.gabriwar.warpedpixeldungeon.tiles.butters;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

public abstract class Butter implements Bundlable {

    public int pos;

    public void melt() {
        Dungeon.level.butter.remove(pos);
        GameScene.updateMap(pos);
    }

    public Butter set(int pos) {
        this.pos = pos;
        return this;
    }

    public abstract void stepOnEffect(Char ch);

    private static final String POS = "pos";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(POS, pos);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        pos = bundle.getInt(POS);
    }
}
