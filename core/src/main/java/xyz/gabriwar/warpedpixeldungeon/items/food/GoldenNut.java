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

package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class GoldenNut extends Nut {

	{
		image = ItemSpriteSheet.GOLDEN_NUT;
		energy = Hunger.STARVING;
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		switch (Random.Int(2)) {
			case 0:
				hero.HT += 20;
				hero.STR += 2;
				hero.sprite.showStatus(CharSprite.POSITIVE, "+2 str, +20 ht");
				GLog.p( Messages.get(this, "blessing") );
				Badges.validateStrengthAttained();
				break;
			case 1:
				hero.HT += 50;
				hero.STR += 5;
				hero.sprite.showStatus(CharSprite.POSITIVE, "+5 str, +50 ht");
				GLog.p( Messages.get(this, "highest_blessing") );
				Badges.validateStrengthAttained();
				break;
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}
