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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Heartseeker;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/** Heartseeker's lodged arrow: the heart beats three times, then bursts. */
public class HeartseekerArrow extends Buff {

	public static final int BEATS = 3;
	public static final int REACH = 4;

	private int beats = BEATS;
	private int rank = 1;

	{
		type = buffType.NEGATIVE;
	}

	public void set( int rank ){
		this.rank = rank;
		beats = BEATS;
	}

	@Override
	public boolean act(){
		if (!target.isAlive() || target.alignment != Char.Alignment.ENEMY){
			detach();
			return true;
		}
		if (--beats > 0){
			pulse();
			spend( TICK );
			return true;
		}
		burst();
		return true;
	}

	//each beat louder and fuller than the last, counting down to the burst
	private void pulse(){
		if (target.sprite == null || !Dungeon.level.heroFOV[target.pos]) return;
		int beat = BEATS - beats;
		target.sprite.emitter().burst( Speck.factory( Speck.HEART ), 1 + beat );
		target.sprite.showStatus( CharSprite.WARNING, Integer.toString( beats ) );
		Sample.INSTANCE.play( Assets.Sounds.HEALTH_WARN, 0.4f + 0.2f * beat, 1f + 0.15f * beat );
	}

	private void burst(){
		Char victim = target;
		int pos = victim.pos;
		//8% / 15% / 22% of its full health
		int damage = Math.round( victim.HT * (0.01f + 0.07f * rank) );
		if (Char.hasProp( victim, Char.Property.BOSS ) || Char.hasProp( victim, Char.Property.MINIBOSS )) damage /= 2;
		damage = Math.max( 1, damage );

		detach();
		if (victim.sprite != null && Dungeon.level.heroFOV[pos]){
			victim.sprite.emitter().burst( BloodParticle.FACTORY, 12 );
			new Flare( 6, 24 ).color( 0xCC2222, true ).show( victim.sprite, 0.7f );
			victim.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( Heartseeker.class, "burst" ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 0.9f );
		victim.damage( damage, this );

		if (rank >= 3 && !victim.isAlive()) seekNext( victim, pos );
	}

	//fully trained: the arrow tears free of a dead heart and finds the nearest wounded one
	private void seekNext( Char dead, int from ){
		Mob next = null;
		for (Mob m : Dungeon.level.mobs){
			if (m == dead || m.alignment != Char.Alignment.ENEMY || !m.isAlive()
					|| m.buff( HeartseekerArrow.class ) != null || m.HP * 2 > m.HT
					|| Dungeon.level.distance( from, m.pos ) > REACH || !SkillInteractions.clear( from, m.pos )) continue;
			if (next == null || Dungeon.level.trueDistance( from, m.pos ) < Dungeon.level.trueDistance( from, next.pos )){
				next = m;
			}
		}
		if (next == null) return;

		Buff.affect( next, HeartseekerArrow.class ).set( rank );
		SpiritBow bow = Dungeon.hero != null ? Dungeon.hero.belongings.getItem( SpiritBow.class ) : null;
		final Mob struck = next;
		SkillFX.streak( from, struck.pos, bow != null ? bow.knockArrow() : new Dart(), () -> {
			SkillFX.flash( struck );
			if (struck.sprite != null) struck.sprite.emitter().burst( Speck.factory( Speck.HEART ), 3 );
		} );
		Sample.INSTANCE.play( Assets.Sounds.HIT_ARROW, 1f, 0.8f );
	}

	private static final String BEATS_LEFT = "beats";
	private static final String RANK = "rank";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( BEATS_LEFT, beats );
		bundle.put( RANK, rank );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		beats = bundle.getInt( BEATS_LEFT );
		rank = bundle.getInt( RANK );
	}
}
