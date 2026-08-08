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

package xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Mirroring extends Armor.Glyph {

	private static ItemSprite.Glowing PINK = new ItemSprite.Glowing( 0xCCAA88 );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		int level = Math.max(0, armor.buffedLvl());

		// lvl 0 - 17%, lvl 1 - 25%, lvl 2 - 30%
		float procChance = (level + 1f) / (level + 6f) * procChanceMultiplier(defender);
		if (Random.Float() < procChance && defender instanceof Hero) {

			float images = power(); //scales with glyph level
			if (Random.Float() < images%1){
				images = (float)Math.ceil(images);
			} else {
				images = (float)Math.floor(images);
			}

			ArrayList<Integer> spawnPoints = new ArrayList<>();

			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
				int p = defender.pos + PathFinder.NEIGHBOURS8[i];
				if (Actor.findChar(p) == null
						&& (Dungeon.level.passable[p] || Dungeon.level.avoid[p])) {
					spawnPoints.add(p);
				}
			}

			while (images > 0 && spawnPoints.size() > 0) {
				MirrorImage mob = new MirrorImage();
				mob.duplicate((Hero) defender);
				mob.pos = spawnPoints.remove(Random.index(spawnPoints));
				GameScene.add(mob);
				ScrollOfTeleportation.appear(mob, mob.pos);
				images--;
			}
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return PINK;
	}

}
