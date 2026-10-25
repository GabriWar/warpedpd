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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MarshLight;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * The swamp witch's bog lantern (actors/mobs/npcs/SwampWitch), sold by her alone: a jar with a
 * marsh light caught in it. Unstoppered, its cold green glow lights the dark round the hero as a
 * torch would (a light, no flame: it warms nobody) and burns through the swamp's mist, which no
 * longer cuts his sight (MarshLight), until the light goes out.
 */
public class BogLantern extends Item {

	public static final String AC_LIGHT = "LIGHT";

	/** How long its light lasts, in turns. */
	public static final float DURATION = 600f;

	{
		image = ItemSpriteSheet.BOG_LANTERN;
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
		if (action.equals( AC_LIGHT )){
			detach( hero.belongings.backpack );
			hero.spend( 1f );
			hero.busy();
			SpatialSound.play( Assets.Sounds.BURNING, hero, 0.6f, 1.4f );
			hero.sprite.operate( hero.pos );
			Buff.prolong( hero, MarshLight.class, DURATION );
			Buff.prolong( hero, Light.class, DURATION );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 5 );
			GLog.p( Messages.get( this, "lit" ) );
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
		return 30 * quantity;
	}
}
