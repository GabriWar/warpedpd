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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ThermalVent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * What a soak in a Thermal Spring leaves behind. In the water it mends the bather a
 * point every few turns (never a starving one) and takes chill and frost off them; out
 * of it, it is WARMTH degrees of held heat that TileTemperature.feelsLikeAt spends
 * against the cold, for as long as it lasts. Stepping out also leaves the bather what
 * they are - wet.
 *
 * It does nothing about overheating. The pool is as hot as the simulation says it is,
 * and a bather who stays in a scalding one gets heatstroke the ordinary way.
 */
public class SpringSoak extends Buff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	/** degrees of held heat, spent only against the cold: see {@link #warm} */
	public static final float WARMTH = 8f;
	/** the held heat never lifts what the bather feels past this */
	public static final float COMFORT = 18f;

	/** what a character carrying a soak feels at a tile that reads {@code feelsLike} */
	public static float warm( xyz.gabriwar.warpedpixeldungeon.actors.Char ch, float feelsLike ){
		if (ch == null || feelsLike >= COMFORT || ch.buff( SpringSoak.class ) == null) return feelsLike;
		return Math.min( COMFORT, feelsLike + WARMTH );
	}
	public static final float DURATION = 150f;
	private static final int MEND_EVERY = 4;

	private float left = DURATION;
	private int soaked = 0;
	private boolean wasIn = false;

	/** called by the vent each turn the bather is in its water */
	public void steep(){
		left = DURATION;
	}

	@Override
	public boolean act(){
		if (target.isAlive()){
			boolean in = ThermalVent.bathing( target );
			if (in){
				Buff.detach( target, Chill.class );
				Buff.detach( target, Frost.class );
				if (++soaked % MEND_EVERY == 0 && target.HP < target.HT
						&& target.buff( Starving.class ) == null){
					target.HP = Math.min( target.HT, target.HP + 1 );
					if (target.sprite != null) target.sprite.showStatusWithIcon(
							xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.POSITIVE, "1",
							xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.HEALING );
				}
			} else {
				if (wasIn && !target.flying){
					Buff.prolong( target, Drenched.class, Drenched.DURATION );
				}
				left -= TICK;
			}
			wasIn = in;
			if (left <= 0){
				detach();
				return true;
			}
		}
		spend( TICK );
		return true;
	}

	@Override
	public int icon(){
		return BuffIndicator.HEATAURA;
	}

	@Override
	public void tintIcon( Image icon ){
		icon.hardlight( 1f, 0.75f, 0.45f );
	}

	@Override
	public float iconFadePercent(){
		return Math.max( 0, (DURATION - left) / DURATION );
	}

	@Override
	public String iconTextDisplay(){
		return Integer.toString( (int) left );
	}

	@Override
	public String toString(){
		return Messages.get( this, "name" );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", (int) WARMTH, (int) left );
	}

	private static final String LEFT   = "left";
	private static final String SOAKED = "soaked";
	private static final String WAS_IN = "was_in";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
		bundle.put( SOAKED, soaked );
		bundle.put( WAS_IN, wasIn );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		left = bundle.getFloat( LEFT );
		soaked = bundle.getInt( SOAKED );
		wasIn = bundle.getBoolean( WAS_IN );
	}
}
