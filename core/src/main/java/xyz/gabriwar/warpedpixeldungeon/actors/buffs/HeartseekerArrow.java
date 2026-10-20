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

import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Heartseeker;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Heartseeker's lodged arrow: the heart beats three times, then bursts. Seen as a heartbeat in the
 * heart's reds (the blood's crimson D01818 for its light, SkillPalette.HEART's CC0000 for its body;
 * never pink, never a charm's hearts nor the hero's low-health alarm): each beat a throb
 * of motes out of the chest, a star of the heart's light there, and a ring of its deep red closing
 * tighter on the feet (the countdown), a thump pitched a step higher; the burst its real blood and
 * a flare of the heart's light.
 */
public class HeartseekerArrow extends Buff {

	public static final int BEATS = 3;
	public static final int REACH = 4;
	/** The heart's body (the countdown ring, CC0000) and its light (the throb's and the burst's
	 *  stars, the blood's crimson D01818: a flare adds its star over a glow of its own colour, and
	 *  the specks' heart FF3355, charm's, adds up to pink), the countdown ring's widest (the first
	 *  beat's), how much tighter each beat after, the width it closes to, its life. */
	public static final int CRIMSON = SkillPalette.HEART.stop( 1 ), HEART_LIGHT = SkillPalette.BLOOD.stop( 1 ),
			RING_FROM = 31, RING_STEP = 4, RING_TO = 7;
	public static final float RING_LIFE = 0.3f;

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
		beat( target.sprite, beat );
		xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.counterOn( xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Heartseeker.class, target, beats, CharSprite.WARNING );
		//a heartbeat's thump, a step higher each beat (never the hero's own low-health warning)
		SpatialSound.play( Assets.Sounds.HIT, target, 0.4f + 0.2f * beat, 0.55f + 0.1f * beat );
	}

	/** The arrow lodging in a heart: its first throb, `n` motes out of the chest. Any thread
	 *  (one burst of the sprite's emitter: the game's Random drawn once, as the hearts did). */
	public static void lodged( CharSprite s, int n ){
		s.emitter().burst( SkillBursts.HEARTBEAT, n );
		heart( s, 0.2f );
	}

	/** A beat (1, 2, ...): 1 + beat motes throbbing out of the chest, and the countdown's ring
	 *  closing on the feet, RING_STEP px tighter each beat. Any thread (one burst, as the hearts). */
	public static void beat( CharSprite s, int beat ){
		s.emitter().burst( SkillBursts.HEARTBEAT, 1 + beat );
		heart( s, 0.25f );
		ClosingRing.on( s, RING_FROM - RING_STEP * beat, RING_TO, RING_LIFE, CRIMSON );
	}

	//the heart itself throbbing: a small star of the heart's light at the chest
	private static void heart( CharSprite s, float life ){
		PixelFlare.show( SkillVfx.midX( s ), SkillVfx.midY( s ) - 1, HEART_LIGHT, PixelFlare.S, life );
	}

	/** The heart bursting: its blood (the burst is a real wound) and a flare of the heart's light at
	 *  its chest. Any thread (one burst, as before). */
	public static void bursting( CharSprite s ){
		s.emitter().burst( BloodParticle.FACTORY, 12 );
		PixelFlare.show( SkillVfx.midX( s ), SkillVfx.midY( s ), HEART_LIGHT, PixelFlare.M, 0.5f );
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
			bursting( victim.sprite );
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.chipOn( Heartseeker.class, victim );
		}
		SpatialSound.play( Assets.Sounds.HIT_STRONG, pos, 1f, 0.9f );
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
		Callback lodges = () -> {
			SkillFX.flash( struck );
			if (struck.sprite != null) lodged( struck.sprite, 3 );
		};
		//the bow's own arrow tearing free; with no bow, a spirit arrow of light (never a dart, an
		//item), landing when the dart would have
		if (bow != null) SkillFX.streak( from, struck.pos, bow.knockArrow(), lodges );
		else SkillFX.streak( from, struck.pos, SkillFX.Streak.SPIRIT_ARROW, new Dart(), lodges );
		SpatialSound.play( Assets.Sounds.HIT_ARROW, struck, 1f, 0.8f );
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
