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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SeekingBomb;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SeekingBombItem extends Item {

	{
		image = ItemSpriteSheet.SEEKING_BOMB;
		defaultAction = AC_LIGHTTHROW;
		stackable = true;
	}

	// FIXME using a static variable for this is kinda gross, should be a better way
	private static boolean seek = false;

	private static final String AC_LIGHTTHROW = "LIGHTTHROW";

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_LIGHTTHROW );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		if (action.equals( AC_LIGHTTHROW )) {
			seek = true;
			action = AC_THROW;
		} else {
			seek = false;
		}
		super.execute( hero, action );
	}

	@Override
	protected void onThrow( int cell ) {
		if (Actor.findChar( cell ) != null) {
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8)
				if (Dungeon.level.passable[cell + i])
					candidates.add( cell + i );
			int newCell = candidates.isEmpty() ? cell : Random.element( candidates );

			if (!Dungeon.level.pit[newCell] && seek) {
				SeekingBomb.spawnAt( newCell );
			} else {
				Dungeon.level.drop( this, newCell ).sprite.drop( cell );
			}

		} else if (!Dungeon.level.pit[cell] && seek) {
			SeekingBomb.spawnAt( cell );

		} else {
			super.onThrow( cell );
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

	@Override
	public int value() {
		return 20 * quantity;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	//Sprouted turned a plain bomb into a seeking one via a "cook" action. WPD has a
	//real alchemy system, so use that instead — without any source at all the item
	//(and its companion SeekingBomb mob) was dead code.
	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs     = new Class[]{ xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb.class,
			                          xyz.gabriwar.warpedpixeldungeon.items.quest.MetalShard.class };
			inQuantity = new int[]{1, 1};
			cost       = 4;
			output     = SeekingBombItem.class;
			outQuantity = 1;
		}
	}
}
