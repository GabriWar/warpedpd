/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

public class Light extends FlavourBuff {
	
	{
		type = buffType.POSITIVE;
	}

	public static final float DURATION	= 250f;
	public static final int DISTANCE	= 6;

	//true when the light is an open flame the hero carries (a torch), rather
	//than a magical glow: only a fire takes the edge off the cold
	public boolean flame = false;

	private static final String FLAME = "flame";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FLAME, flame );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle( bundle );
		flame = bundle.getBoolean( FLAME );
	}

	/** Lights (or re-lights) a hero's torch: a flame that also warms. */
	public static Light burn( xyz.gabriwar.warpedpixeldungeon.actors.Char ch, float duration ) {
		Light light = Buff.affect( ch, Light.class, duration );
		light.flame = true;
		return light;
	}
	
	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			if (Dungeon.level != null) {
				target.viewDistance = Math.max( Dungeon.level.viewDistance, DISTANCE );
				Dungeon.observe();
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void detach() {
		target.viewDistance = Dungeon.level.viewDistance;
		Dungeon.observe();
		super.detach();
	}

	public void weaken( int amount ){
		spend(-amount);
	}
	
	@Override
	public int icon() {
		return BuffIndicator.LIGHT;
	}
	
	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add(CharSprite.State.ILLUMINATED);
		else target.sprite.remove(CharSprite.State.ILLUMINATED);
	}
}
