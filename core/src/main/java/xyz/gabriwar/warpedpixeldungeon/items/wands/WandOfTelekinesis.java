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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class WandOfTelekinesis extends Wand {

	{
		image = ItemSpriteSheet.WAND_LIVING_EARTH;

		collisionProperties = Ballistica.STOP_SOLID;
	}

	@Override
	public void onZap(Ballistica bolt) {

		boolean mapUpdated = false;
		int maxDistance = buffedLvl() + 4;
		int dist = Math.min(bolt.dist, maxDistance);

		Heap heap = null;

		for (int i = 1; i <= dist; i++) {
			int c = bolt.path.get(i);

			Char ch = Actor.findChar(c);
			if (ch != null) {
				if (i < dist) {
					int next = bolt.path.get(i + 1);
					if ((Dungeon.level.passable[next] || Dungeon.level.avoid[next])
							&& Actor.findChar(next) == null) {

						Actor.add(new Pushing(ch, ch.pos, next));
						ch.pos = next;
						if (ch instanceof Mob) {
							Dungeon.level.occupyCell(ch);
						}
					} else {
						ch.damage(maxDistance - 1 - i, this);
					}
				} else {
					ch.damage(maxDistance - 1 - i, this);
				}
				wandProc(ch, chargesPerCast());
			}

			if (heap == null) {
				heap = Dungeon.level.heaps.get(c);
				if (heap != null) {
					switch (heap.type) {
						case HEAP:
							//no pulling items across the deep journal/sokoban floors, or the
							//prizes can be telekinesed out without solving the puzzles
							if (Dungeon.depth <= 50) {
								Item item = heap.pickUp();
								if (item.doPickUp(curUser)) {
									GLog.i(Messages.get(this, "pickup", item.name()));
								} else {
									Dungeon.level.drop(item, curUser.pos).sprite.drop();
								}
							}
							break;
						case CHEST:
							//opening a chest from across the room is what the wand is for.
							//The source never gated this on depth, only the item pull.
							heap.type = Heap.Type.HEAP;
							heap.sprite.link();
							heap.sprite.drop();
							break;
						default:
					}
				}
			}

			int before = Dungeon.level.map[c];
			Dungeon.level.pressCell(c);

			if (before == Terrain.OPEN_DOOR && Actor.findChar(c) == null) {
				Dungeon.level.set(c, Terrain.DOOR);
				GameScene.updateMap(c);
			} else if (Dungeon.level.water[c]) {
				GameScene.ripple(c);
			}

			if (!mapUpdated && Dungeon.level.map[c] != before) {
				mapUpdated = true;
			}
		}

		if (mapUpdated) {
			Dungeon.observe();
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(
				curUser.sprite.parent,
				MagicMissile.FORCE,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//no on-hit effect
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x88CCFF);
		particle.am = 0.6f;
		particle.setLifespan(0.8f);
		particle.acc.set(10, -10);
		particle.setSize(0.5f, 2f);
		particle.shuffleXY(1f);
	}
}
