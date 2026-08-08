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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite.Glowing;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class BookOfLife extends Item {

	public static final float TIME_TO_USE = 1;

	public static final String AC_READ = "READ";

	private int specialLevel = 32;
	private int returnDepth = -1;
	private int returnPos;

	{
		image = ItemSpriteSheet.BOOK_OF_LIFE;
		unique = true;
		defaultAction = AC_READ;
	}

	private static final String DEPTH = "depth";
	private static final String POS = "pos";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEPTH, returnDepth);
		if (returnDepth != -1) {
			bundle.put(POS, returnPos);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		returnDepth = bundle.getInt(DEPTH);
		returnPos = bundle.getInt(POS);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_READ);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_READ)) {

			if (Dungeon.bossLevel() || hero.petfollow) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			if (Dungeon.depth == specialLevel && hero.pos != Dungeon.level.exit()) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing2"));
				return;
			}

			if (Dungeon.depth != specialLevel && Dungeon.depth >= Dungeon.POSTGAME_DEPTH) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing2"));
				return;
			}

			hero.spend(TIME_TO_USE);


			if (Dungeon.depth < Dungeon.POSTGAME_DEPTH) {
				returnDepth = Dungeon.depth;
				returnPos = hero.pos;

				Level.beforeTransition();
				InterlevelScene.mode = InterlevelScene.Mode.PORT2;
				InterlevelScene.returnDepth = returnDepth;
				InterlevelScene.returnPos = returnPos;
				Game.switchScene(InterlevelScene.class);
			} else {
				// Returning from the special level
				Level.beforeTransition();
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = returnDepth;
				InterlevelScene.returnPos = returnPos;
				Game.switchScene(InterlevelScene.class);
			}
		}
	}

	@Override
	public int value() {
		if (!Statistics.amuletObtained) {
			return 9000 * quantity;
		} else {
			return 500 * quantity;
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

	private static final Glowing GREEN = new Glowing(0x00FF00);

	@Override
	public Glowing glowing() {
		return GREEN;
	}
}
