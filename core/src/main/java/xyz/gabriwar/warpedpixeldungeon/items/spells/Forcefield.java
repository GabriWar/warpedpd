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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfShielding;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

public class Forcefield extends Spell {

	{
		image = ItemSpriteSheet.FORCEFIELD;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void onCast(Hero hero) {
		Buff.affect(hero, Barrier.class).incShield(15);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		Invisibility.dispel();
		detach(curUser.belongings.backpack);
		updateQuickslot();
		curUser.spendAndNext(Actor.TICK);
	}

	@Override
	public int value() {
		return Math.round(quantity * ((30 + 30) / 2f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 2;

		{
			inputs     = new Class[]{ScrollOfRecharging.class, PotionOfShielding.class};
			inQuantity = new int[]{1, 1};
			cost       = 4;
			output     = Forcefield.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
