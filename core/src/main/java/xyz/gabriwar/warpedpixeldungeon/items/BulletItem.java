/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfEarthenArmor;
import xyz.gabriwar.warpedpixeldungeon.items.quest.GooBlob;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class BulletItem extends Item {

	private static final String TXT_VALUE	= "%+d";

	{
		image = ItemSpriteSheet.BULLET;
		
		stackable = true;
	}

	public BulletItem() {
		this( 1 );
	}

	public BulletItem( int value ) {
		this.quantity = value;
	}

	@Override
	public ArrayList<String> actions(Hero hero ) {
		return new ArrayList<>();
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {

		Dungeon.bullet += quantity;

		GameScene.pickUp( this, pos );
		hero.sprite.showStatus( 0xFFFFFF, TXT_VALUE, quantity );
		hero.spendAndNext( TIME_TO_PICK_UP );

		Sample.INSTANCE.play( Assets.Sounds.ITEM );

		updateQuickslot();

		return true;
	}

	@Override
	public boolean collect( Bag container ) {
		Dungeon.bullet += quantity;
		return true;
	}

	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	@Override
	public int value() {
		return 1;
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{LiquidMetal.class};
			inQuantity = new int[]{1};

			cost = 0;

			output = BulletItem.class;
			outQuantity = 1;
		}

	}

}
