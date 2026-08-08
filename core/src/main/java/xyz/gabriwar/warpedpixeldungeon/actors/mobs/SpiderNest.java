/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SpiderEggSprite;
import com.watabou.utils.PathFinder;

//ported from Remixed PD's Spider Nest: a rooted sac that keeps birthing brood on a
//timer, but only up to the population cap so the farm loop stays under control
public class SpiderNest extends Mob {

	private static final int SPAWN_DELAY = 20;

	{
		spriteClass = SpiderEggSprite.class;

		HP = HT = 10;
		defenseSkill = 2;

		baseSpeed = 0f;

		EXP = 0;
		maxLvl = 9;

		loot = PotionOfHealing.class;
		lootChance = 0.2f;

		state = PASSIVE;
		properties.add( Property.IMMOVABLE );
	}

	public SpiderNest() {
		spend( SPAWN_DELAY );
	}

	@Override
	protected boolean act() {
		if (Dungeon.level.heroFOV[pos]
				&& SpiderServant.population() < SpiderServant.POPULATION_CAP) {
			int cell = spawnCell();
			if (cell != -1) {
				SpiderServant spider = new SpiderServant();
				spider.pos = cell;
				spider.state = spider.HUNTING;
				GameScene.add( spider );
			}
		}
		spend( SPAWN_DELAY );
		return true;
	}

	private int spawnCell() {
		for (int i : PathFinder.NEIGHBOURS8) {
			int c = pos + i;
			if (c >= 0 && c < Dungeon.level.length()
					&& Dungeon.level.passable[c] && Actor.findChar( c ) == null) {
				return c;
			}
		}
		return -1;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
