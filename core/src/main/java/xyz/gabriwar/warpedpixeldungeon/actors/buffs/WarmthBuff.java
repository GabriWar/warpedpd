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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

//elixir of warmth: immune to the cold, and melee hits can set the target alight
public class WarmthBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;

		immunities.add( Frost.class );
		immunities.add( Chill.class );
		immunities.add( Freezing.class );
	}

	public static final float DURATION = 60f;
	public static final float BURN_CHANCE = 0.25f;

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			Buff.detach( target, Frost.class );
			Buff.detach( target, Chill.class );
			return true;
		} else {
			return false;
		}
	}

	public void proc( Char enemy ) {
		//melee only: thrown weapons and bows leave the hand before the heat can reach the target
		if (target instanceof Hero && ((Hero) target).belongings.thrownWeapon != null) return;

		if (Random.Float() < BURN_CHANCE) {
			Buff.affect( enemy, Burning.class ).reignite( enemy );
			if (enemy.sprite != null) enemy.sprite.emitter().burst( FlameParticle.FACTORY, 2 );
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.IMBUE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 2f, 0.4f, 0.1f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}
}
