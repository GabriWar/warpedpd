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

package xyz.gabriwar.warpedpixeldungeon.items.trinkets;

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndSundialGuide;

import java.util.ArrayList;

public class DimensionalSundial extends Trinket {

	{
		image = ItemSpriteSheet.SUNDIAL;

		defaultAction = AC_CHECK;
	}

	public static final String AC_CHECK = "CHECK";
	public static final String AC_GUIDE = "GUIDE";

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_CHECK);
		actions.add(AC_GUIDE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_GUIDE)) {
			GameScene.show(new WndSundialGuide());
			return;
		}

		if (action.equals(AC_CHECK)) {
			GLog.i(GameCalendar.dateString());

			DayNightCycle.Phase phase = DayNightCycle.phase();
			String phaseMsg = Messages.get(DayNightCycle.class, phase.name().toLowerCase());
			if (phaseMsg != null && !phaseMsg.startsWith("!!!")) {
				GLog.i(phaseMsg);
			}

			String moonMsg = GameCalendar.moonPhaseString();
			if (moonMsg != null && !moonMsg.startsWith("!!!")) {
				GLog.i(Messages.get(this, "moon_phase", moonMsg));
			}
		}
	}

	@Override
	protected int upgradeEnergyCost() {
		//6 -> 8(14) -> 10(24) -> 12(36)
		return 6+2*level();
	}

	@Override
	public String statsDesc() {
		if (isIdentified()){
			return Messages.get(this,
					"stats_desc",
					(int)(100*(1f - enemySpawnMultiplierDaytime(buffedLvl()))),
					(int)(100*(enemySpawnMultiplierNighttime(buffedLvl())-1f)));
		} else {
			return Messages.get(this, "typical_stats_desc",
					(int)(100*(1f - enemySpawnMultiplierDaytime(0))),
					(int)(100*(enemySpawnMultiplierNighttime(0)-1f)));
		}
	}

	public static boolean sundialWarned = false;

	public static float spawnMultiplierAtCurrentTime(){
		if (trinketLevel(DimensionalSundial.class) != -1) {
			DayNightCycle.Phase phase = DayNightCycle.phase();
			if (phase == DayNightCycle.Phase.NIGHT || phase == DayNightCycle.Phase.DUSK) {
				if (!sundialWarned){
					GLog.w(Messages.get(DimensionalSundial.class, "warning"));
					sundialWarned = true;
				}
				return enemySpawnMultiplierNighttime();
			} else {
				return enemySpawnMultiplierDaytime();
			}
		} else {
			return 1f;
		}
	}

	public static float enemySpawnMultiplierDaytime(){
		return enemySpawnMultiplierDaytime(trinketLevel(DimensionalSundial.class));
	}

	public static float enemySpawnMultiplierDaytime( int level ){
		if (level == -1){
			return 1f;
		} else {
			return 0.95f - 0.05f*level;
		}
	}

	public static float enemySpawnMultiplierNighttime(){
		return enemySpawnMultiplierNighttime(trinketLevel(DimensionalSundial.class));
	}

	public static float enemySpawnMultiplierNighttime( int level ){
		if (level == -1){
			return 1f;
		} else {
			return 1.25f + 0.25f*level;
		}
	}
}
