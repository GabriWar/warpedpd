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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.School;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;


public class ArcaneEcho extends SubSkill3 {

	{
		name = "Arcane Echo";
		image = 178;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && enemy.isAlive() && Random.Int(100) < 10 * level){
			int echo = Random.NormalIntRange( 2, 4 + 2 * level );
			//at max level the echo jumps on to one more enemy standing next to the first
			Char chained = level >= MAX_LEVEL ? nextTo( enemy ) : null;

			enemy.damage( echo, this );
			//the echo arcs back along the blow in arcane violet (never the Shocking enchantment's
			//blue-white), and the blow itself rings again over the struck: a crescent and runes
			if (enemy.sprite != null && Dungeon.hero.sprite != null){
				Dungeon.hero.sprite.parent.add( new Lightning( Dungeon.hero.pos, enemy.pos, null ).tint( Lightning.ARCANE ) );
				enemy.sprite.emitter().burst( SkillBursts.ARCANE_SPARK, 3 + level );
				enemy.sprite.flash();
				echo( enemy.pos, Dungeon.hero.pos );
			}
			if (chained != null){
				if (chained.sprite != null && Dungeon.hero.sprite != null){
					Dungeon.hero.sprite.parent.add( new Lightning( enemy.pos, chained.pos, null ).tint( Lightning.ARCANE ) );
					chained.sprite.emitter().burst( SkillBursts.ARCANE_SPARK, 3 + level );
					chained.sprite.flash();
					echo( chained.pos, enemy.pos );
				}
				chained.damage( echo, this );
			}
			SpatialSound.play( Assets.Sounds.ZAP, enemy, 0.6f, 1.3f );
		}
		return damage;
	}

	/** The echo's arcane blue-violet (never the rogue's lilac), its crescent's radius, spin (deg/s)
	 *  and life, and when it rings after the arc. */
	public static final int VIOLET = School.ARCANE.color;
	public static final float RADIUS = 10f, SPIN = 900f, RING_FOR = 0.18f, DELAY = 0.06f;
	/** Its runes: RUNES locking round the struck's middle LOCK_GAP s apart on LOCK_RX x LOCK_RY (all of
	 *  them in LOCK_FOR s), then lifting off at LIFT px/s for LIFT_FOR s; and a ring of RING_N turning
	 *  RING_TURN rad/s round its feet (RING_RX x RING_RY) for RING_FOR_S s. */
	public static final int RUNES = 5, RING_N = 6;
	public static final float LOCK_RX = 8f, LOCK_RY = 4f, LOCK_GAP = 0.04f, LOCK_FOR = 0.2f, LIFT = -12f, LIFT_FOR = 0.35f,
			RING_RX = 6f, RING_RY = 2.5f, RING_TURN = 3f, RING_FOR_S = 0.45f;
	//a sprite on a cell: its middle (a pixel low: the lifted runes stay on the body) and its feet
	//below where it stands (raisedY)
	private static final float MID = 2f, FEET = 6f;

	/** The blow echoing over the struck at `cell`, from `from`: a beat after the arc an arcane
	 *  crescent sweeps round it from the side the blow came from, RUNES runes lock round it and lift
	 *  off (the spell said again), and a ring of runes turns at its feet where the arc lands. Nothing
	 *  of the game's Random. Any thread. */
	public static void echo( final int cell, final int from ){
		if (!SkillVfx.valid( cell ) || !SkillVfx.valid( from )) return;
		final float toward = (float) Math.toDegrees( Math.atan2( SkillVfx.cy( from ) - SkillVfx.cy( cell ), SkillVfx.cx( from ) - SkillVfx.cx( cell ) ) );
		FxTimeline.start().at( DELAY, () -> {
			if (!SkillVfx.seen( cell )) return;
			ArcSpinFX.at( cell, VIOLET, RADIUS, 0.25f, toward, FxRandom.sign() * SPIN, RING_FOR );
			float x = SkillVfx.cx( cell ), y = SkillVfx.raisedY( cell );
			final RuneFX.Runes said = RuneFX.lock( x, y + MID, RUNES, SkillPalette.RUNE_ARCANE, LOCK_RX, LOCK_RY, LOCK_GAP,
					RuneFX.LOCK_HOLD );
			RuneFX.ring( x, y + FEET, RING_N, SkillPalette.RUNE_ARCANE, RING_RX, RING_RY, RING_TURN, RING_FOR_S );
			FxTimeline.start().at( LOCK_FOR, () -> said.lift( LIFT, LIFT_FOR ) );
		} );
	}

	private Char nextTo( Char enemy ){
		for (int offset : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( enemy.pos + offset );
			if (ch != null && ch != Dungeon.hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive())
				return ch;
		}
		return null;
	}
}
