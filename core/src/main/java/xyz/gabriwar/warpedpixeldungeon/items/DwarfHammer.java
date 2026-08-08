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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.MinesBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ZotBossLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class DwarfHammer extends Item {

	public static final float TIME_TO_USE = 1;
	public static final String AC_BREAK = "BREAK";

	{
		image = ItemSpriteSheet.DWARF_HAMMER;
		unique = true;
		defaultAction = AC_BREAK;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_BREAK);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_BREAK)) {

			//bossLevel() only covers 5/10/15/20/25/36/41, but MinesBossLevel (65) and
			//ZotBossLevel (99) seal themselves too — the hammer must never crack open a
			//boss arena. (Right now those are the only floors that seal at all, so this
			//refuses everywhere; Sprouted's sealed Halls, which the hammer was built for,
			//aren't ported.)
			if (Dungeon.bossLevel()
					|| Dungeon.level instanceof MinesBossLevel
					|| Dungeon.level instanceof ZotBossLevel) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			if (!Dungeon.level.locked) {
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			if (hero.pos == Dungeon.level.exit()) {
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			detach(hero.belongings.backpack);

			//unseal() rather than locked=false, so the LockedFloor buff comes off with it
			Dungeon.level.unseal();

			int exit = Dungeon.level.exit();
			Dungeon.level.map[exit] = Terrain.EXIT;
			GameScene.updateMap(exit);
			Dungeon.observe();

			GLog.w(Messages.get(this, "unseal"));
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 500 * quantity;
	}
}
