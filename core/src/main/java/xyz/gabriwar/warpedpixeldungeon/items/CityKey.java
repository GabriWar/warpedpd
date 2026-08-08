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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenNut;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class CityKey extends Item {

	public static final float TIME_TO_USE = 1;
	public static final String AC_PORT = "PORT";

	private int specialLevel = 30;
	private int returnDepth = -1;
	private int returnPos;

	{
		image = ItemSpriteSheet.ANCIENT_KEY;
		stackable = false;
		unique = true;
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

			if (Dungeon.bossLevel() || hero.petfollow) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			if (Dungeon.depth > 25 && Dungeon.depth != specialLevel) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			if (Dungeon.depth == 1) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			hero.spend(TIME_TO_USE);

			Buff buff = Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);
			if (buff != null) buff.detach();

			if (Dungeon.depth < 25 && !Dungeon.bossLevel()) {
				returnDepth = Dungeon.depth;
				returnPos = hero.pos;
				InterlevelScene.mode = InterlevelScene.Mode.PORTCITY;
			} else {
				if (Statistics.goldThievesKilled > 99
						&& Statistics.skeletonsKilled > 99
						&& Statistics.albinoPiranhasKilled > 99
						&& Statistics.archersKilled > 99) {
					GoldenNut nut = new GoldenNut();
					nut.doPickUp(Dungeon.hero, Dungeon.hero.pos);
				}
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			}
			InterlevelScene.returnDepth = returnDepth;
			InterlevelScene.returnPos = returnPos;
			Level.beforeTransition();
			Game.switchScene(InterlevelScene.class);
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

	private static final ItemSprite.Glowing YELLOW = new ItemSprite.Glowing(0xCCAA44);

	@Override
	public ItemSprite.Glowing glowing() {
		return YELLOW;
	}
}
