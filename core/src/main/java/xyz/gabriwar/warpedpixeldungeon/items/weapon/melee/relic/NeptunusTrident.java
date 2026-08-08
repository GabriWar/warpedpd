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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class NeptunusTrident extends RelicMeleeWeapon {

	{
		image = ItemSpriteSheet.RELIC_TRIDENT;
	}

	public static final String AC_FLOOD = "FLOOD";

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && charge >= chargeCap)
			actions.add(AC_FLOOD);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_FLOOD)) {
			GLog.p(Messages.get(this, "activate"));
			flood(Math.round(level() / 5f), hero);
		}
	}

	private void flood(int distance, Hero hero) {
		charge = 0;
		int length = Dungeon.level.length();
		int width = Dungeon.level.width();

		for (int i = width; i < length - width; i++) {
			int dist = Dungeon.level.distance(hero.pos, i);
			if (dist < distance) {
				if (isFloodable(i)) {
					Dungeon.level.set(i, Terrain.WATER);
					GameScene.updateMap(i);

					Char ch = Actor.findChar(i);
					if (ch != null && ch != hero) {
						Buff.affect(ch, Slow.class, Slow.DURATION / 3f + level());
					}
				}
			}
		}
		Dungeon.observe();
		updateQuickslot();
	}

	private boolean isFloodable(int cell) {
		int terrain = Dungeon.level.map[cell];
		if (terrain == Terrain.ENTRANCE || terrain == Terrain.EXIT) {
			return false;
		}
		return terrain == Terrain.EMPTY
				|| terrain == Terrain.GRASS
				|| terrain == Terrain.HIGH_GRASS
				|| terrain == Terrain.EMBERS
				|| terrain == Terrain.EMPTY_DECO
				|| terrain == Terrain.FURROWED_GRASS
				|| terrain == Terrain.WATER;
	}

	@Override
	protected WeaponBuff passiveBuff() {
		return new Flooding();
	}

	public class Flooding extends WeaponBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap) {
				charge += level();
				if (charge >= chargeCap) {
					charge = chargeCap;
					GLog.w(Messages.get(NeptunusTrident.class, "charged"));
				}
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}
}
