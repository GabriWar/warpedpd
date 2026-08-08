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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfAthmosphericCompression extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_ATMOCOMPRESS;
	}

	@Override
	public void apply(Hero hero) {
		identify();

		int length = Dungeon.level.length();
		boolean[] water = Dungeon.level.water;
		int count = 0;

		for (int i = 0; i < length; i++) {
			if (water[i]) {
				Level.set(i, Terrain.EMPTY);
				GameScene.updateMap(i);
				Char ch = Actor.findChar(i);
				if (ch instanceof Mob && !(ch instanceof NPC) && !ch.isImmune(this.getClass())) {
					ch.damage(5, this);
					count++;
				}
				if (Dungeon.level.heroFOV[i]) {
					CellEmitter.get(i).start(Speck.factory(Speck.STEAM), 0.2f, 8);
				}
			}
		}

		if (count > 0) GLog.i(Messages.get(this, "mobs_effected", count));
		else GLog.i(Messages.get(this, "no_targets"));
	}
}
