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

package xyz.gabriwar.warpedpixeldungeon.items.sea;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sea.LanternGlare;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/**
 * A lighthouse keeper's storm lantern (actors/mobs/npcs/LighthouseKeeper), its glass full of his oil:
 * lit, it burns as a torch does (a flame: light and a little warmth) for DURATION turns, and its glare
 * and bitter smoke keep the sharks off its bearer all the while (sea/LanternGlare).
 */
public class SeaLantern extends Item {

	public static final String AC_LIGHT = "LIGHT";
	/** How long its oil burns, turns: twice a torch. */
	public static final float DURATION = 2 * Light.DURATION;

	{
		image = ItemSpriteSheet.SEA_LANTERN;
		stackable = true;
		defaultAction = AC_LIGHT;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_LIGHT );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (!action.equals( AC_LIGHT )) return;
		hero.spend( 1f );
		hero.busy();
		if (hero.sprite != null) hero.sprite.operate( hero.pos );
		detach( hero.belongings.backpack );
		Catalog.countUse( getClass() );
		Light.burn( hero, DURATION );
		Buff.affect( hero, LanternGlare.class, DURATION );
		SpatialSound.play( Assets.Sounds.BURNING, hero );
		NetManager.heroLog( hero, Messages.get( this, "lit" ) );
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
		return 40 * quantity;
	}
}
