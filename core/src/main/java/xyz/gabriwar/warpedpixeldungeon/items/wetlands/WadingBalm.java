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

package xyz.gabriwar.warpedpixeldungeon.items.wetlands;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Wading;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * The swamp witch's wading balm (actors/mobs/npcs/SwampWitch), sold by her alone: eel fat and
 * bog myrtle worked into a grease. Rubbed into boots and legs, the bog's mud slides off them: its
 * water no longer slows the hero's steps (Wading) for a good while.
 */
public class WadingBalm extends Item {

	public static final String AC_RUB = "RUB";

	/** How long the grease holds, in turns. */
	public static final float DURATION = 1200f;

	{
		image = ItemSpriteSheet.WADING_BALM;
		stackable = true;
		defaultAction = AC_RUB;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_RUB );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( AC_RUB )){
			detach( hero.belongings.backpack );
			hero.spend( 1f );
			hero.busy();
			SpatialSound.play( Assets.Sounds.UNLOCK, hero, 0.5f, 0.7f );
			hero.sprite.operate( hero.pos );
			Buff.prolong( hero, Wading.class, DURATION );
			GLog.p( Messages.get( this, "rubbed" ) );
		}
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
		return 15 * quantity;
	}
}
