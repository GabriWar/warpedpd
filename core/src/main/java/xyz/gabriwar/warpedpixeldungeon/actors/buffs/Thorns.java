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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class Thorns extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	protected float level;

	private static final String LEVEL	= "level";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEVEL, level );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		level = bundle.getFloat( LEVEL );
	}

	public void set( float level ) {
		this.level = Math.max(this.level, level);
	}

	@Override
	public boolean attachTo( Char target ) {
		if (!target.flying && super.attachTo( target )) {
			target.rooted = true;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean act() {
		if (target.isAlive()) {

			level = Random.NormalFloat(level / 2f, level);
			int dmg = Math.round(level);

			if (dmg > 0) {

				target.damage( dmg, this );
				if (target.sprite.visible) {
					Splash.at( target.sprite.center(), -PointF.PI / 2, PointF.PI / 6,
							target.sprite.blood(), Math.min( 10 * dmg / target.HT, 10 ) );
				}

				if (target == Dungeon.hero && !target.isAlive()) {
					Dungeon.fail( getClass() );
					GLog.n( Messages.get(this, "ondeath") );
				}

				spend( TICK );
			} else {
				detach();
			}

		} else {

			detach();

		}

		return true;
	}

	@Override
	public void detach() {
		target.rooted = false;
		super.detach();
	}

	@Override
	public int icon() {
		return BuffIndicator.THORNS;
	}

	@Override
	public String toString() {
		return Messages.get(this, "name");
	}

	@Override
	public String heroMessage() {
		return Messages.get(this, "heromsg");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Math.round(level));
	}
}
