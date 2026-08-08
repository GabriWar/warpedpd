/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 Dachhack
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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class TownReturnBeacon extends Item {

	public static final float TIME_TO_USE = 1;

	public static final String AC_RETURN = "RETURN";
	public static final String AC_RETURNTOWN = "RETURNTOWN";

	private int returnDepth = -1;
	private int returnPos;

	{
		image = ItemSpriteSheet.TOWN_BEACON;

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
		//once Otiluke is rescued the mine is sealed off — no returning to it
		if (Dungeon.depth == xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.DEPTH
				&& returnDepth > 55 && !Badges.checkOtilukeRescued()) {
			actions.add(AC_RETURN);
		}
		if (Dungeon.depth > 55 && Dungeon.townCheck(Dungeon.depth)) {
			actions.add(AC_RETURNTOWN);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_RETURNTOWN)) {

			if (Dungeon.bossLevel() || Dungeon.level.locked || hero.petfollow) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "fail"));
				return;
			}

			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
				if (Actor.findChar(hero.pos + PathFinder.NEIGHBOURS8[i]) != null) {
					GLog.w(Messages.get(this, "creatures"));
					return;
				}
			}

			hero.spend(TIME_TO_USE);

			returnDepth = Dungeon.depth;
			returnPos = hero.pos;

			Buff buff = Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);
			if (buff != null)
				buff.detach();

			//the town is on the surface: land on its plaza
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.arriveInTown(
					xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures.TOWN_PLAZA );
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.travelToSurface();

		} else if (action.equals(AC_RETURN)) {

			hero.spend(TIME_TO_USE);

			Buff buff = Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);
			if (buff != null)
				buff.detach();

			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = returnDepth;
			InterlevelScene.returnBranch = 0;
			InterlevelScene.returnPos = returnPos;
			Level.beforeTransition();
			Game.switchScene(InterlevelScene.class);
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
}
