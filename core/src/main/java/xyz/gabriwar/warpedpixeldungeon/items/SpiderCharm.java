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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderServant;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//ported from Remixed PD: the Spider Queen's trinket. Each use pays a quarter of the
//bearer's max HP (and any sungrass healing) to call a spider servant to their side.
public class SpiderCharm extends Item {

	public static final String AC_USE = "USE";

	private static final ItemSprite.Glowing WHITE = new ItemSprite.Glowing( 0xFFFFFF );

	{
		image = ItemSpriteSheet.SPIDER_CHARM;

		defaultAction = AC_USE;
		unique = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_USE );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_USE )) {
			Wound.hit( hero );
			hero.damage( hero.HT / 4, this );
			Buff.detach( hero, Sungrass.Health.class );

			if (!hero.isAlive()) {
				Dungeon.fail( this );
				GLog.n( Messages.get( this, "ondeath" ) );
				return;
			}

			ArrayList<Integer> spawnPoints = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8) {
				int p = hero.pos + i;
				if (Actor.findChar( p ) == null && Dungeon.level.passable[p]) {
					spawnPoints.add( p );
				}
			}

			if (!spawnPoints.isEmpty()) {
				SpiderServant pet = SpiderServant.pet();
				pet.pos = Random.element( spawnPoints );
				GameScene.add( pet );
				Actor.addDelayed( new Pushing( pet, hero.pos, pet.pos ), -1 );
			}
		}
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return WHITE;
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
