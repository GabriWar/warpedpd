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

package xyz.gabriwar.warpedpixeldungeon.homestead.torches;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.StandingTorchSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * A torch set in the ground on its pole (Torch's PLACE, out in the world). It burns for good: it
 * lights the ground round it at night (StandingTorchSprite) and, like a lit hearth, keeps wild
 * animals from being set down within HomeAnchors.RADIUS cells (TorchAnchors). Tapping it takes the
 * torch back. A HomesteadActor, so it stays where it was put through every move of the world and
 * every save.
 */
public class StandingTorch extends NPC implements HomesteadActor {

	{
		spriteClass = StandingTorchSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
	}

	@Override
	protected boolean act(){
		spend( TICK );
		return true;
	}

	@Override
	public void damage( int dmg, Object src ){
		//a pole in the ground: nothing to hurt
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public void beckon( int cell ){
	}

	@Override
	public boolean reset(){
		return true;
	}

	/** Tapped by the host's hero: the torch comes out of the ground and back into his pack. */
	@Override
	public boolean interact( Char c ){
		if (c != Dungeon.hero) return true;
		Torch torch = new Torch();
		if (!torch.collect( Dungeon.hero.belongings.backpack )) Dungeon.level.drop( torch, pos ).sprite.drop();
		GLog.i( Messages.get( this, "taken" ) );
		SpatialSound.play( Assets.Sounds.PUFF, pos );
		destroy();
		if (sprite != null) sprite.killAndErase();
		Dungeon.hero.spend( 1f );
		return true;
	}
}
