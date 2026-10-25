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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ParasiticInfection;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * The swamp witch's remedy (actors/mobs/npcs/SwampWitch), sold by her alone: a bitter green
 * draught that draws out what the bog puts in you - poison, the bog's rot (ooze), a parasite's
 * eggs - and closes a bleeding wound.
 */
public class WitchsRemedy extends Item {

	public static final String AC_DRINK = "DRINK";

	{
		image = ItemSpriteSheet.WITCH_REMEDY;
		stackable = true;
		defaultAction = AC_DRINK;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_DRINK );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( AC_DRINK )){
			detach( hero.belongings.backpack );
			hero.spend( 1f );
			hero.busy();
			SpatialSound.play( Assets.Sounds.DRINK, hero );
			hero.sprite.operate( hero.pos );
			boolean cured = cure( hero );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), cured ? 4 : 2 );
			GLog.p( Messages.get( this, cured ? "cured" : "nothing" ) );
		}
	}

	/** Draws the bog's ills out of someone: poison, ooze, a parasite, bleeding. True when one was there. */
	public static boolean cure( Char ch ){
		boolean any = ch.buff( Poison.class ) != null || ch.buff( Ooze.class ) != null
				|| ch.buff( ParasiticInfection.class ) != null || ch.buff( Bleeding.class ) != null;
		Buff.detach( ch, Poison.class );
		//a parasite leaves ooze behind as it goes (ParasiticInfection.detach): the ooze after it
		Buff.detach( ch, ParasiticInfection.class );
		Buff.detach( ch, Ooze.class );
		Buff.detach( ch, Bleeding.class );
		return any;
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
		return 25 * quantity;
	}
}
