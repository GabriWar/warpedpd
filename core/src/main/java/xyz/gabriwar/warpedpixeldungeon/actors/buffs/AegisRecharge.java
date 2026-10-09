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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aegis;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/** The aegis shield reforming after it caught a blow; when it runs out the shield is back up. */
public class AegisRecharge extends FlavourBuff {

	{
		type = buffType.NEUTRAL;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public boolean act() {
		//the shield rings back into place, so the player knows it will catch again
		if (target.isAlive() && target.sprite != null && target.sprite.parent != null){
			new Flare( 5, 16 ).color( 0xFFEE88, true ).show( target.sprite, 0.4f );
			target.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( Aegis.class, "ready" ) );
			SpatialSound.play( Assets.Sounds.HIT_PARRY, target, 0.4f, 1.4f );
		}
		return super.act();
	}
}
