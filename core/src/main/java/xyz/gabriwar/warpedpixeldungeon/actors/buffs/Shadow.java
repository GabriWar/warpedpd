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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class Shadow extends FlavourBuff {

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	public static final float DURATION = 20f;

	@SuppressWarnings("unchecked")
	public static final Class<? extends Buff>[] removableBuffs = new Class[]{
			Burning.class,
			Ooze.class,
			Corrosion.class,
			Poison.class,
			Bleeding.class,
			Blindness.class,
			Cripple.class,
			Vertigo.class,
			Slow.class,
			Haste.class,
			Bless.class,
			Weakness.class,
			Amok.class,
			Charm.class,
			Daze.class,
			Doom.class,
			Dread.class,
			Drowsy.class,
			Hex.class,
			MagicalSleep.class,
			Degrade.class,
			Chill.class,
			Frost.class,
			Dehydrated.class,
			PlagueAura.class,
			Starving.class,
	};

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			target.invisible++;
			if (target instanceof Hero && ((Hero) target).subClass == HeroSubClass.ASSASSIN) {
				Buff.affect( target, Preparation.class );
			}
			stripBuffs();
			return true;
		}
		return false;
	}

	@Override
	public boolean act() {
		stripBuffs();
		return super.act();
	}

	@Override
	public void detach() {
		if (target.invisible > 0) target.invisible--;
		super.detach();
	}

	private void stripBuffs() {
		for (Class<? extends Buff> cls : removableBuffs) {
			Buff b = target.buff( cls );
			if (b != null) b.detach();
		}
	}

	@Override
	public void fx( boolean on ) {
		if (on) target.sprite.add( CharSprite.State.INVISIBLE );
		else if (target.invisible == 0) target.sprite.remove( CharSprite.State.INVISIBLE );
	}

	public static void dispel() {
		if (Dungeon.hero == null) return;
		dispel( Dungeon.hero );
	}

	public static void dispel( Char ch ) {
		for (Buff shadow : ch.buffs( Shadow.class )) {
			shadow.detach();
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.INVISIBLE;
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
