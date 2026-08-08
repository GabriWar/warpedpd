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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barkskin;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FullMoonStrength;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Strength;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class FullMoonberry extends Food {

	{
		image = ItemSpriteSheet.FULLMOONBERRY;
		energy = (Hunger.STARVING - Hunger.HUNGRY) / 10f;
		bones = false;
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		GLog.w( Messages.get(this, "fury") );
		Buff.affect(hero, Strength.class);
		Buff.affect(hero, FullMoonStrength.class);
		Buff.affect(hero, Light.class, Light.DURATION);

		if (Random.Int(2) == 1) {
			Barkskin.conditionallyAppend(hero, hero.HT * 2, 1);
			GLog.p( Messages.get(this, "barkskin") );
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}
