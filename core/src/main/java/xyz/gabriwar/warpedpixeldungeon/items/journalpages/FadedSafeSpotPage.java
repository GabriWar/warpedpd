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

package xyz.gabriwar.warpedpixeldungeon.items.journalpages;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.legacy.SafeRoomMigration;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/**
 * What an old save's safe spot page comes back as (the Bundle alias in WarpedPixelDungeon): the
 * room it led to is gone. It is never kept: the hero's own copy is traded in when the save loads
 * (SafeRoomMigration.settle), one lying anywhere else when he picks it up, and one that reached
 * his pack some other way (a guest's hero, a chest's contents) when he reads it. Not a
 * JournalPage, so Otiluke's Journal never takes it.
 */
public class FadedSafeSpotPage extends Item {

	public static final String AC_READ = "READ";

	{
		image = ItemSpriteSheet.JOURNAL_PAGE;
		defaultAction = AC_READ;
		stackable = false;
		unique = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_READ );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( AC_READ )){
			detachAll( hero.belongings.backpack );
			SafeRoomMigration.redeem( hero );
			hero.spendAndNext( 1f );
		}
	}

	@Override
	public boolean doPickUp( Hero hero, int pos ){
		GameScene.pickUp( this, pos );
		SafeRoomMigration.redeem( hero );
		hero.spendAndNext( pickupDelay() );
		return true;
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	@Override
	public int value(){
		return 0;
	}
}
