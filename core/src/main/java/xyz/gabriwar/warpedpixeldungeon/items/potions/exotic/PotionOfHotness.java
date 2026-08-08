/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.potions.exotic;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dehydrated;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfHotness extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_HOTNESS;
	}

	@Override
	public void apply(Hero hero) {
		identify();

		// Hotter ambient → longer Dehydrated; mirrors PotionOfFrost's cold bonus
		float ambientBonus = ClimateManager.localTemp() * 0.5f;
		float duration = Dehydrated.DURATION * 3f + ambientBonus;

		int count = 0;
		for (Mob mob : hero.getVisibleEnemies().toArray(new Mob[0])) {
			Buff.prolong(mob, Dehydrated.class, duration);

			// Spike tile temperature around mob — can trigger Heatstroke
			for (int offset : PathFinder.NEIGHBOURS9) {
				int cell = mob.pos + offset;
				if (cell >= 0 && cell < Dungeon.level.length() && !Dungeon.level.solid[cell]) {
					TileTemperature.depositHeat(cell, 20f);
				}
			}

			// Push bodyTemp up — mirrors PotionOfFrost's -15f nudge
			if (!Float.isNaN(mob.bodyTemp)) {
				mob.bodyTemp += 20f;
			} else {
				mob.bodyTemp = ClimateManager.localTemp() + 20f;
			}

			count++;
		}

		if (count > 0) GLog.i(Messages.get(this, "mobs_effected", count));
		else GLog.i(Messages.get(this, "no_targets"));
	}
}
