/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.items.misc;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class AutoPotion extends MiscEquippable {

	{
		image = ItemSpriteSheet.ARTIFACT_TOOLKIT;
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new AutoHealPotion();
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	//A marker buff. The trigger lives in Hero.damage(), which is where it belongs: it has to
	//fire on the blow that would kill you, not on the hero's next turn. It also used to call
	//potion.execute(AC_DRINK) - the player-action path, which spends a turn, sets busy() and
	//can pop a confirmation window - from inside Buff.act(), re-entering the actor loop from
	//a buff's own turn.
	public class AutoHealPotion extends ArtifactBuff {
	}

	//called from Hero.damage() once the blow has landed
	public static void trigger(Hero hero) {
		if (hero.buff(AutoHealPotion.class) == null) return;
		if (!hero.isAlive() || hero.HP >= hero.HT * 0.1f) return;

		PotionOfHealing potion = hero.belongings.getItem(PotionOfHealing.class);
		if (potion != null) {
			potion.detach(hero.belongings.backpack);
			potion.apply(hero);
			GLog.i(Messages.get(AutoPotion.class, "auto_heal"));
		}
	}
}
