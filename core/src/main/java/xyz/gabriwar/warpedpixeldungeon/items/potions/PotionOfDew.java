/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.DewInfusion;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfDew extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_DEW;
    }

    @Override
    public void apply(Hero hero) {
        identify();
        Buff.prolong(hero, DewInfusion.class, DewInfusion.DURATION);
    }

    @Override
    public void shatter(int cell) {
        splash(cell);
        if (Dungeon.level.heroFOV[cell]) {
            identify();
            Sample.INSTANCE.play(Assets.Sounds.SHATTER);
        }

        for (int offset : PathFinder.NEIGHBOURS9){
            if (!Dungeon.level.solid[cell+offset]) {
                int nDrops = Random.NormalIntRange(3, 6);

                ArrayList<Integer> candidates = new ArrayList<Integer>();
                for (int i : PathFinder.NEIGHBOURS8){
                    if (Dungeon.level.passable[cell+offset+i]
                            && cell+offset+i != Dungeon.level.entrance()
                            && cell+offset+i != Dungeon.level.exit()){
                        candidates.add(cell+offset+i);
                    }
                }

                for (int i = 0; i < nDrops && !candidates.isEmpty(); i++){
                    Integer c = Random.element(candidates);
                    Dungeon.level.drop(new Dewdrop(), c).sprite.drop(cell+offset);
                    candidates.remove(c);
                }
            }
        }
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}
