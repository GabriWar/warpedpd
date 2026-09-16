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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class WeaponEnhance extends Buff {

    {
        type = buffType.POSITIVE;
    }

    private int hit = 0;
    private int maxHit = 0;
    private int level = 0;

    public void set( int level, int hit ) {
        this.maxHit = hit;
        this.hit = this.maxHit;
        this.level = level;
        Item.updateQuickslot();
    }

    @Override
    public void detach() {
        super.detach();
        Item.updateQuickslot();
    }

    @Override
    public float iconFadePercent() {
        return Math.max((maxHit - hit)/(float) maxHit, 0);
    }

    public void attackProc() {
        hit--;
        if (hit <= 0) {
            detach();
        }
        BuffIndicator.refreshHero();
    }

    public int weaponLevel(int weaponLevel){
        weaponLevel += this.level;
        return weaponLevel;
    }

    @Override
    public int icon() {
        return BuffIndicator.UPGRADE;
    }

    @Override
    public void tintIcon(Image icon) {
        icon.hardlight(1, 0, 0);
    }

    @Override
    public String toString() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", level, hit);
    }

    private static final String HIT = "hit";
    private static final String MAX_HIT = "maxHit";
    private static final String LEVEL = "level";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(HIT, hit);
        bundle.put(MAX_HIT, maxHit);
        bundle.put(LEVEL, level);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        hit = bundle.getInt(HIT);
        maxHit = bundle.getInt(MAX_HIT);
        level = bundle.getInt(LEVEL);
    }
}
