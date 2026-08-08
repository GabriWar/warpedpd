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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCleansing;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class ForcePush extends Spell {

	{
		image = ItemSpriteSheet.FORCE_PUSH;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void onCast(Hero hero) {
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (Dungeon.level.heroFOV[mob.pos]) {
				int opposite;
				int tries = 0;
				do {
					opposite = mob.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
					tries++;
				} while (tries < 20 && (opposite < 0 || opposite >= Dungeon.level.length()
						|| Dungeon.level.map[opposite] == Terrain.WALL
						|| Dungeon.level.map[opposite] == Terrain.WALL_DECO));
				if (tries < 20) {
					Ballistica trajectory = new Ballistica(hero.pos, opposite, Ballistica.MAGIC_BOLT);
					WandOfBlastWave.throwChar(mob, trajectory, 100, false, true, this);
				}
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		Invisibility.dispel();
		detach(curUser.belongings.backpack);
		updateQuickslot();
		curUser.spendAndNext(Actor.TICK);
	}

	@Override
	public int value() {
		return Math.round(quantity * ((30 + 30) / 16f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 16;

		{
			inputs     = new Class[]{ScrollOfPsionicBlast.class, PotionOfCleansing.class};
			inQuantity = new int[]{1, 1};
			cost       = 8;
			output     = ForcePush.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
