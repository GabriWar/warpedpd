/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeActions;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHomeLedger;

import java.util.List;

/**
 * The threats' part of a home's menu (HomeActions): its LEDGER is the threats' window
 * (WndHomeLedger) - the home's notice part by part against the raid line, then what happened
 * there - in place of the plain list of lines the menu has built in, at the same place in the
 * list. The host's own hero only: the notice and the ledger are the host's.
 */
public class ThreatActions implements HomeActions.Provider {

	/** The id of the ledger's action, the built-in one's. */
	public static final String LEDGER = "ledger";

	@Override
	public void offer( HomesteadWorld.Home home, Hero hero, List<HomeActions.Action> out ){
		if (hero != Dungeon.hero) return;
		HomeActions.Action ledger = new HomeActions.Action( LEDGER, ThreatActions.class, "ledger",
				() -> GameScene.show( new WndHomeLedger( home ) ) );
		for (int i = 0; i < out.size(); i++){
			if (LEDGER.equals( out.get( i ).id )){
				out.set( i, ledger );
				return;
			}
		}
		out.add( ledger );
	}
}
