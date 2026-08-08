/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

// OV: fire immunity + melee attacks ignite enemies
public class SoulFire extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
		immunities.add( Burning.class );
	}

	public static final float DURATION = 10f;

	// called from Char.java attack sequence, same pattern as FireImbue.proc()
	public void proc( Char enemy ) {
		Buff.affect( enemy, Burning.class ).reignite( enemy );
		enemy.sprite.emitter().burst( FlameParticle.FACTORY, 2 );
	}

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			// soul fire extinguishes regular fire
			Buff.detach( target, Burning.class );
			return true;
		}
		return false;
	}

	@Override
	public void fx( boolean on ) {
		if (on) target.sprite.add( CharSprite.State.BURNING );
		else    target.sprite.remove( CharSprite.State.BURNING );
	}

	@Override
	public int icon() {
		return BuffIndicator.FIRE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.3f, 0.3f, 1f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}

	@Override
	public String toString() {
		return Messages.get( this, "name" );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", dispTurns() );
	}
}
