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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfHighgrass extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_HIGHGRASS;
	}

	@Override
	public void apply(Hero hero) {
		identify();

		int count = 0;
		for (int i = 0; i < Dungeon.level.length(); i++) {
			if (Dungeon.level.map[i] == Terrain.GRASS && Dungeon.level.heroFOV[i]) {
				Level.set(i, Terrain.HIGH_GRASS);
				GameScene.updateMap(i);
				CellEmitter.get(i).start(EarthParticle.FACTORY, 0.2f, 4);
				count++;
			}
		}

		if (count > 0) GLog.i(Messages.get(this, "grown", count));
		else GLog.i(Messages.get(this, "no_tiles"));
	}
}
