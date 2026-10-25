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

package xyz.gabriwar.warpedpixeldungeon.items.misc;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Mounts;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/**
 * A riding saddle (homestead/mounts): stitched at a tanning rack from two hides and a rope, or bought
 * from a village stablehand. On the world's slices its SADDLE action puts it on a horse of the
 * hero's beside him, as the horse's own menu does; a saddled horse is ridden.
 */
public class Saddle extends Item {

	public static final String AC_SADDLE = "SADDLE";

	{
		image = ItemSpriteSheet.SADDLE;
		defaultAction = AC_SADDLE;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		if (Dungeon.level instanceof OverworldLevel) actions.add( AC_SADDLE );
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ){
		super.execute( hero, action );
		if (!AC_SADDLE.equals( action ) || !(Dungeon.level instanceof OverworldLevel)) return;
		GameScene.selectCell( new CellSelector.Listener(){
			@Override
			public void onSelect( Integer cell ){
				if (cell != null) Mounts.saddleAt( hero, cell );
			}

			@Override
			public String prompt(){
				return Messages.get( Saddle.class, "prompt" );
			}
		} );
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
		return 4 * quantity;
	}
}
