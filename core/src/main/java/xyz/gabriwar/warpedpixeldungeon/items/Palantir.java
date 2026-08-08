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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class Palantir extends Item {

	public static final float TIME_TO_USE = 1;

	public static final String AC_PORT = "PORT";

	private int specialLevel = 99;
	private int returnDepth = -1;
	private int returnPos;
	private boolean used = false;

	{
		image = ItemSpriteSheet.PALANTIR;
		stackable = false;
		unique = true;
		defaultAction = AC_PORT;
	}

	private static final String DEPTH = "depth";
	private static final String POS = "pos";
	private static final String USED = "used";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEPTH, returnDepth);
		bundle.put(USED, used);
		if (returnDepth != -1) {
			bundle.put(POS, returnPos);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		returnDepth = bundle.getInt(DEPTH);
		returnPos = bundle.getInt(POS);
		used = bundle.getBoolean(USED);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if ((!used || Dungeon.depth == specialLevel) && !Dungeon.level.locked && !hero.petfollow) {
			actions.add(AC_PORT);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_PORT)) {

			hero.spend(TIME_TO_USE);

			//same trap the TenguKey had: on a boss floor neither branch below fits, and the
			//RETURN one fires with returnDepth still -1, sending the hero to a DeadEndLevel
			if (Dungeon.bossLevel()) {
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			if (Dungeon.depth < specialLevel) {
				returnDepth = Dungeon.depth;
				returnPos = hero.pos;
				used = true;

				Level.beforeTransition();
				InterlevelScene.mode = InterlevelScene.Mode.PALANTIR;
				InterlevelScene.returnDepth = returnDepth;
				InterlevelScene.returnPos = returnPos;
				Game.switchScene(InterlevelScene.class);
			} else {
				// Returning from the special level (depth 99)
				Level.beforeTransition();
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = returnDepth;
				InterlevelScene.returnPos = returnPos;
				Game.switchScene(InterlevelScene.class);
			}
		}
	}

	public void reset() {
		returnDepth = -1;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}
}
