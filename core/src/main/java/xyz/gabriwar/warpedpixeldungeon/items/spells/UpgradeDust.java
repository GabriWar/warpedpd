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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArmorEnhance;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WeaponEnhance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.LiquidMetal;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAugmentation;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.text.DecimalFormat;

public class UpgradeDust extends Spell {
	
	{
		image = ItemSpriteSheet.UPGRADE_DUST;
		talentChance = 1/(float) Recipe.OUT_QUANTITY;
	}
	
	@Override
	protected void onCast(Hero hero) {
		Buff.affect(hero, WeaponEnhance.class).set(1+hero.lvl/10, 20);
		Buff.affect(hero, ArmorEnhance.class).set(1+hero.lvl/10, 20);

		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play( Assets.Sounds.EVOKE );
		CellEmitter.center( hero.pos ).burst( Speck.factory( Speck.STAR ), 7 );

		GLog.p( Messages.get(this, "empower") );
		
		detach( curUser.belongings.backpack );
		updateQuickslot();
		Invisibility.dispel();
		hero.spendAndNext( 1f );
	}

	@Override
	public int value() {
		//prices of ingredients, divided by output quantity
		return Math.round((40 + 30)/(float) Recipe.OUT_QUANTITY);
	}

	@Override
	public int energyVal() {
		return (int)(8 * (quantity/(float) Recipe.OUT_QUANTITY));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		private static final int OUT_QUANTITY = 3;

		{
			inputs =  new Class[]{StoneOfAugmentation.class, LiquidMetal.class};
			inQuantity = new int[]{1, 20};

			cost = 3;

			output = UpgradeDust.class;
			outQuantity = 3;
		}

	}
}
