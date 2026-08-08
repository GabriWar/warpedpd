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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
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

public class SanChikarah extends Item {

	public static final float TIME_TO_USE = 1;

	public static final String AC_PORT = "PORT";

	private int returnDepth = -1;
	private int returnPos;

	{
		image = ItemSpriteSheet.SANCHIKARAH;
		stackable = false;
		unique = true;
		defaultAction = AC_PORT;
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
		actions.add(AC_PORT);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_PORT)) {

			if (Dungeon.bossLevel()) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			if (Dungeon.depth >= Dungeon.POSTGAME_DEPTH && !Dungeon.shadowyogkilled) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing2"));
				return;
			}

			hero.spend(TIME_TO_USE);

			//drop a running time-freeze before changing levels, like the other
			//porting items do — otherwise the hourglass buff rides across the
			//transition and leaves the new floor frozen.
			Buff timeFreeze = hero.buff(TimekeepersHourglass.timeFreeze.class);
			if (timeFreeze != null) timeFreeze.detach();

			if (Dungeon.depth < Dungeon.POSTGAME_DEPTH) {
				returnDepth = Dungeon.depth;
				returnPos = hero.pos;

				Level.beforeTransition();
				InterlevelScene.mode = InterlevelScene.Mode.PORT4;
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

	private static final Glowing WHITE = new Glowing(0xFFFFCC);

	@Override
	public Glowing glowing() {
		return WHITE;
	}
}
