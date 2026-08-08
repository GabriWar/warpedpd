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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MrDestructo2dot0;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class ActiveMrDestructo2 extends Item {

	private static final String AC_ACTIVATE = "ACTIVATE";

	{
		image = ItemSpriteSheet.ACTIVE_MRD2;
		defaultAction = AC_ACTIVATE;
		stackable = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_ACTIVATE );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		if (action.equals( AC_ACTIVATE )) {
			action = AC_THROW;
		}
		super.execute( hero, action );
	}

	@Override
	protected void onThrow( int cell ) {
		if (Actor.findChar( cell ) != null) {
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8) {
				if (Dungeon.level.passable[cell + i]) {
					candidates.add( cell + i );
				}
			}
			int newCell = candidates.isEmpty() ? cell : Random.element( candidates );
			if (!Dungeon.level.pit[newCell]) {
				MrDestructo2dot0.spawnAt( newCell );
			} else {
				Dungeon.level.drop( this, newCell ).sprite.drop( cell );
			}
		} else if (!Dungeon.level.pit[cell]) {
			MrDestructo2dot0.spawnAt( cell );
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
}
