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
import xyz.gabriwar.warpedpixeldungeon.items.Recipe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//direct-pot crafting for the alchemy weapons: ingredients in, weapon out.
//adapted from Re-ARranged PD's BluePrint.Recipe (the blueprint item itself was not ported).
public class AlchemyWeaponRecipe extends Recipe {

    public static final ArrayList<ArrayList<Class<?extends Item>>> validIngredients = new ArrayList<>();
    static {
        validIngredients.add( new TrueRunicBlade().weaponRecipe() );
        validIngredients.add( new Lance().weaponRecipe() );
        validIngredients.add( new ObsidianShield().weaponRecipe() );
        validIngredients.add( new ChainWhip().weaponRecipe() );
        validIngredients.add( new LanceNShield().weaponRecipe() );
        validIngredients.add( new ChainFlail().weaponRecipe() );
        validIngredients.add( new UnformedBlade().weaponRecipe() );
        validIngredients.add( new SpearNShield().weaponRecipe() );
        validIngredients.add( new AssassinsSpear().weaponRecipe() );
        validIngredients.add( new ForceGlove().weaponRecipe() );
        validIngredients.add( new UnholyBible().weaponRecipe() );
        validIngredients.add( new HugeSword().weaponRecipe() );
        validIngredients.add( new MeisterHammer().weaponRecipe() );
        validIngredients.add( new BeamSaber().weaponRecipe() );
        validIngredients.add( new HolySword().weaponRecipe() );
        validIngredients.add( new DualGreatSword().weaponRecipe() );
        validIngredients.add( new SharpKatana().weaponRecipe() );
    }

    public static final LinkedHashMap<Integer, Class<?extends MeleeWeapon>> indexToOutput = new LinkedHashMap<>();
    static {
        indexToOutput.put( 0, TrueRunicBlade.class );
        indexToOutput.put( 1, Lance.class );
        indexToOutput.put( 2, ObsidianShield.class );
        indexToOutput.put( 3, ChainWhip.class );
        indexToOutput.put( 4, LanceNShield.class );
        indexToOutput.put( 5, ChainFlail.class );
        indexToOutput.put( 6, UnformedBlade.class );
        indexToOutput.put( 7, SpearNShield.class );
        indexToOutput.put( 8, AssassinsSpear.class );
        indexToOutput.put( 9, ForceGlove.class );
        indexToOutput.put( 10, UnholyBible.class );
        indexToOutput.put( 11, HugeSword.class );
        indexToOutput.put( 12, MeisterHammer.class );
        indexToOutput.put( 13, BeamSaber.class );
        indexToOutput.put( 14, HolySword.class );
        indexToOutput.put( 15, DualGreatSword.class );
        indexToOutput.put( 16, SharpKatana.class );
    }

    public static final LinkedHashMap<Integer, Integer> costs = new LinkedHashMap<>();
    static {
        costs.put( 0, 0 );
        costs.put( 1, 0 );
        costs.put( 2, 0 );
        costs.put( 3, 0 );
        costs.put( 4, 5 );
        costs.put( 5, 5 );
        costs.put( 6, 0 );
        costs.put( 7, 5 );
        costs.put( 8, 5 );
        costs.put( 9, 5 );
        costs.put( 10, 0 );
        costs.put( 11, 0 );
        costs.put( 12, 0 );
        costs.put( 13, 0 );
        costs.put( 14, 5 );
        costs.put( 15, 5 );
        costs.put( 16, 0 );
    }

    private static ArrayList<Class<?extends Item>> classList(ArrayList<Item> ingredients){
        ArrayList<Class<?extends Item>> list = new ArrayList<>();
        for (Item i : ingredients) {
            list.add(i.getClass());
        }
        return list;
    }

    private static int recipeIndex(ArrayList<Item> ingredients){
        ArrayList<Class<?extends Item>> classes = classList(ingredients);
        for (ArrayList<Class<?extends Item>> a : validIngredients) {
            if (classes.containsAll(a) && a.containsAll(classes)) {
                return validIngredients.indexOf(a);
            }
        }
        return -1;
    }

    @Override
    public boolean testIngredients(ArrayList<Item> ingredients) {
        return recipeIndex(ingredients) != -1;
    }

    @Override
    public int cost(ArrayList<Item> ingredients) {
        int index = recipeIndex(ingredients);
        return index == -1 ? 0 : costs.get(index);
    }

    @Override
    public Item brew(ArrayList<Item> ingredients) {
        int index = recipeIndex(ingredients);

        for (Item i : ingredients) {
            i.quantity(i.quantity()-1);
        }

        return Reflection.newInstance(indexToOutput.get(index));
    }

    @Override
    public Item sampleOutput(ArrayList<Item> ingredients) {
        int index = recipeIndex(ingredients);
        return index == -1 ? null : Reflection.newInstance(indexToOutput.get(index));
    }
}
