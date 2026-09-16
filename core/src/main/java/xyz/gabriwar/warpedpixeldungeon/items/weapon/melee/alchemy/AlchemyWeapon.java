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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UpgradeDust;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public interface AlchemyWeapon {
    ArrayList<Class<?extends Item>> weaponRecipe();

    static String hintString(ArrayList<Class<?extends Item>> recipe) {
        if (recipe.get(1).isAssignableFrom(UpgradeDust.class)) {
            return Messages.get(Item.class, "discover_hint_alchemy_one", Reflection.newInstance(recipe.get(0)).name());
        } else {
            return Messages.get(Item.class, "discover_hint_alchemy_two", Reflection.newInstance(recipe.get(0)).name(), Reflection.newInstance(recipe.get(1)).name());
        }
    }

    /*
    If a class implements this interface, you should override the following method with this for catalog item hint:

    @Override
    public String discoverHint() {
        return AlchemyWeapon.hintString(weaponRecipe());
    }
    */
}
