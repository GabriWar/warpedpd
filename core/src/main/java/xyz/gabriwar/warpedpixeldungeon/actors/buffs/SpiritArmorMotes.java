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

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;

/**
 * Spirit Armor's ring: every few points of mana the ward pays condense into a mote circling the
 * hero. When the ring is full, every mote fires at once, each at the nearest enemy still unclaimed.
 */
public class SpiritArmorMotes extends Buff {

	//mana the ward has to pay to condense one mote
	public static final int MANA_PER_MOTE = 3;
	private static final int RANGE = 5;

	public int motes = 0;
	private int stored = 0;
	private int rank = 1;
	private SpiritArmorFX visual;
	private boolean firing = false;

	{
		type = buffType.POSITIVE;
	}

	public static int capacity( int rank ){ return 1 + rank; }

	public void absorb( int mana, int level ){
		rank = level;
		stored += mana;
		int formed = 0;
		while (stored >= MANA_PER_MOTE && motes < capacity( rank )){
			stored -= MANA_PER_MOTE;
			motes++;
			formed++;
		}
		if (motes >= capacity( rank )) stored = 0;
		if (formed > 0 && target != null && target.sprite != null){
			target.sprite.showStatus( CharSprite.NEUTRAL, motes + "/" + capacity( rank ) );
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.4f, 1.6f );
		}
		if (motes >= capacity( rank )) volley();
	}

	private void volley(){
		//a mote's zap can provoke a counter-hit that the ward pays for: that must not fire the ring again
		if (firing) return;
		firing = true;
		try {
			release();
		} finally {
			firing = false;
		}
	}

	private void release(){
		ArrayList<Char> claimed = new ArrayList<>();
		int fired = 0;
		while (motes > 0){
			Char aim = nearest( claimed );
			if (aim == null) break;
			claimed.add( aim );
			motes--;
			fired++;
			if (target.sprite != null && target.sprite.parent != null && aim.sprite != null){
				((MagicMissile) target.sprite.parent.recycle( MagicMissile.class )).reset(
						MagicMissile.WARD, target.sprite.center(), aim.sprite.destinationCenter(), null );
			}
			aim.damage( Random.NormalIntRange( 2 + rank, 4 + 2 * rank ), this );
			if (aim.sprite != null) aim.sprite.flash();
		}
		if (fired == 0) return;
		Sample.INSTANCE.play( Assets.Sounds.ZAP, 0.9f, 1.4f );
		//mastery: the ring's release washes on into your wands
		if (rank >= 3) Buff.prolong( target, Recharging.class, 2f );
		if (motes <= 0) detach();
	}

	private Char nearest( ArrayList<Char> claimed ){
		Char best = null;
		for (Char ch : Actor.chars()){
			if (claimed.contains( ch ) || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()
					|| !Dungeon.level.heroFOV[ch.pos] || Dungeon.level.distance( target.pos, ch.pos ) > RANGE
					|| new Ballistica( target.pos, ch.pos, Ballistica.PROJECTILE ).collisionPos != ch.pos) continue;
			if (best == null || Dungeon.level.trueDistance( target.pos, ch.pos )
					< Dungeon.level.trueDistance( target.pos, best.pos )) best = ch;
		}
		return best;
	}

	@Override
	public boolean act(){
		//a full ring with nothing in sight holds until something steps into view
		if (motes >= capacity( rank )) volley();
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		if (visual != null){ visual.killAndErase(); visual = null; }
		if (on && target.sprite != null && target.sprite.parent != null){
			visual = new SpiritArmorFX( target, this );
			target.sprite.parent.add( visual );
		}
	}

	private static final String MOTES = "motes";
	private static final String STORED = "stored";
	private static final String RANK = "rank";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( MOTES, motes );
		bundle.put( STORED, stored );
		bundle.put( RANK, rank );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		rank = Math.max( 1, Math.min( 3, bundle.getInt( RANK ) ) );
		motes = Math.min( capacity( rank ), bundle.getInt( MOTES ) );
		stored = bundle.getInt( STORED );
	}
}
