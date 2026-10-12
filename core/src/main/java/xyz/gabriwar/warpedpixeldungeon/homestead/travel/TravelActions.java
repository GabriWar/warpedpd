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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeActions;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import java.util.List;

/**
 * What the travel part offers at a home (HomeActions.PROVIDERS): "travel", the stones' travel window
 * from its hearthstone - for the host's hero only, the one who travels.
 */
public class TravelActions implements HomeActions.Provider {

	/** The id of the travel action, which the hearthstone's menu lists first. */
	public static final String TRAVEL = "travel";

	@Override
	public void offer( final HomesteadWorld.Home home, Hero hero, List<HomeActions.Action> out ){
		if (hero != Dungeon.hero) return;
		out.add( new HomeActions.Action( TRAVEL, TravelActions.class, "travel",
				() -> GameScene.show( new WndTravel( Travel.Place.of( home ) ) ) ) );
	}
}
