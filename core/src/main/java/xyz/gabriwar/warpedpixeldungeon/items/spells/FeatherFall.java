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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.quest.MetalShard;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class FeatherFall extends Spell {

	{
		image = ItemSpriteSheet.FEATHER_FALL;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void onCast(Hero hero) {
		Buff.append(hero, FeatherBuff.class, FeatherBuff.DURATION);
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		hero.sprite.emitter().burst(Speck.factory(Speck.JET), 20);
		GLog.p(Messages.get(this, "light"));
		Invisibility.dispel();
		detach(curUser.belongings.backpack);
		updateQuickslot();
		curUser.spendAndNext(Actor.TICK);
	}

	public static class FeatherBuff extends FlavourBuff {
		// Does nothing — triggered externally by Chasm.heroLand()
		public static final float DURATION = 30f;
	}

	@Override
	public int value() {
		return Math.round(quantity * ((30 + 40) / 2f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 2;

		{
			inputs     = new Class[]{PotionOfLevitation.class, MetalShard.class};
			inQuantity = new int[]{1, 1};
			cost       = 6;
			output     = FeatherFall.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
