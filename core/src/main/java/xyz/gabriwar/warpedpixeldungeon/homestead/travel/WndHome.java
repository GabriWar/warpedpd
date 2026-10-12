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
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeActions;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeAnchors;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * A hearthstone's menu: its home's name and where it is, then what can be done there - Travel first,
 * Rename second, then everything else the homesteads' parts offer at a home, in their order
 * (HomeActions.forHome: the ledger, the threats' notices...). A choice runs its action.
 */
public class WndHome extends WndOptions {

	private final List<HomeActions.Action> actions;

	public WndHome( HomesteadWorld.Home home ){
		this( home, ordered( HomeActions.forHome( home, Dungeon.hero ) ) );
	}

	private WndHome( HomesteadWorld.Home home, List<HomeActions.Action> actions ){
		super( WndTravel.icon( Travel.Kind.HOME ), home.title(),
				Messages.get( WndHome.class, "body", WndTravel.slice( home.depth ), HomeAnchors.RADIUS ),
				labels( actions ) );
		this.actions = actions;
	}

	//Travel, then Rename, then the rest as they came
	static List<HomeActions.Action> ordered( List<HomeActions.Action> all ){
		ArrayList<HomeActions.Action> out = new ArrayList<>();
		for (String first : new String[]{ TravelActions.TRAVEL, "rename" }){
			for (HomeActions.Action a : all) if (a.id.equals( first )) out.add( a );
		}
		for (HomeActions.Action a : all) if (!out.contains( a )) out.add( a );
		return out;
	}

	private static String[] labels( List<HomeActions.Action> actions ){
		String[] out = new String[actions.size()];
		for (int i = 0; i < out.length; i++) out[i] = actions.get( i ).label();
		return out;
	}

	@Override
	protected void onSelect( int index ){
		if (index >= 0 && index < actions.size()) actions.get( index ).callback.call();
	}
}
