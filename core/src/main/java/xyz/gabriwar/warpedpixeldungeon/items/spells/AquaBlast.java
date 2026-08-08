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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import xyz.gabriwar.warpedpixeldungeon.items.quest.MetalShard;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class AquaBlast extends TargetedSpell {

	{
		image = ItemSpriteSheet.AQUA_BLAST;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void affectTarget(Ballistica bolt, Hero hero) {
		int cell = bolt.collisionPos;

		// Blue water splash
		Splash.at(cell, 0x4488FF, 10);

		for (int i : PathFinder.NEIGHBOURS9) {
			if (i == 0 || Random.Int(5) != 0) {
				int terr = Dungeon.level.map[cell + i];
				if (terr == Terrain.EMPTY || terr == Terrain.GRASS
						|| terr == Terrain.EMBERS || terr == Terrain.EMPTY_SP
						|| terr == Terrain.HIGH_GRASS || terr == Terrain.FURROWED_GRASS
						|| terr == Terrain.EMPTY_DECO) {
					Level.set(cell + i, Terrain.WATER);
					GameScene.updateMap(cell + i);
				}
			}
		}

		Char target = Actor.findChar(cell);
		if (target != null && target != hero) {
			Buff.prolong(target, Paralysis.class, 1f);
		}

		onSpellused();
	}

	@Override
	public int value() {
		return Math.round(quantity * ((60 + 40) / 12f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 12;

		{
			inputs     = new Class[]{PotionOfStormClouds.class, MetalShard.class};
			inQuantity = new int[]{1, 1};
			cost       = 4;
			output     = AquaBlast.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
