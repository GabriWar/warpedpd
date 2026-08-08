/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HalomethaneFire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class HalomethaneBurning extends Buff implements Hero.Doom {

	private static final float DURATION = 8f;

	private float left;

	private static final String LEFT	= "left";

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		left = bundle.getFloat( LEFT );
	}

	@Override
	public boolean act() {

		if (target.isAlive() && !target.isImmune(getClass())) {

			//more damage than normal fire but doesnt burn items
			int damage = Random.NormalIntRange( 2, 4 + Dungeon.depth/4 );
			Buff.detach( target, Chill.class);

			if (target instanceof Hero) {

				Hero hero = (Hero)target;
				hero.damage( damage, this );

			} else {
				target.damage( damage, this );
			}

		} else {

			detach();
		}

		if (Dungeon.level.flamable[target.pos] && Blob.volumeAt(target.pos, HalomethaneFire.class) == 0) {
			GameScene.add( Blob.seed( target.pos, 4, HalomethaneFire.class ) );
		}

		spend( TICK );
		left -= TICK;

		if (left <= 0 || (Dungeon.level.water[target.pos] && !target.flying)) {

			detach();
		}

		return true;
	}

	public void reignite( Char ch ) {
		reignite( ch, DURATION );
	}

	public void reignite( Char ch, float duration ) {
		left = duration;
	}

	@Override
	public int icon() {
		return BuffIndicator.HALOMETHANEBURNING;
	}

	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add(CharSprite.State.HALOMETHANEBURNING);
		else target.sprite.remove(CharSprite.State.HALOMETHANEBURNING);
	}

	@Override
	public String heroMessage() {
		return Messages.get(this, "heromsg");
	}

	@Override
	public String toString() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns(left));
	}

	@Override
	public void onDeath() {

		Badges.validateDeathFromFire();

		Dungeon.fail( getClass() );
		GLog.n( Messages.get(this, "ondeath") );
	}
}
