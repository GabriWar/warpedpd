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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Battlemage's Mana Shield: a barrier that shatters when a blow breaks it, flinging
 * shards into the nearest enemies. At max level it reforms once, at half strength.
 * Fading away on its own, or being eaten by a tick of poison or fire, is not breaking.
 */
public class ManaShieldWard extends Barrier {

	/** the game time of the blow being resolved right now; set by ManaShield just before shields absorb */
	public static float blowAt = -1f;
	/** where that blow comes from (a cell; -1: nowhere known), set with it: the side the ward takes it on */
	public static int blowFrom = -1;

	private boolean decaying = false;
	private boolean reformed = false;
	private int strength = 0;

	/** a fresh cast: full strength, and the one reform is available again */
	public void raise( int shield ){
		strength = shield;
		reformed = false;
		setShield( shield );
	}

	@Override
	public boolean act() {
		decaying = true;
		boolean result = super.act();
		decaying = false;
		return result;
	}

	@Override
	public int absorbDamage( int dmg ) {
		Char owner = target;
		boolean hadShield = shielding() > 0;
		boolean blow = !decaying && blowAt == Actor.now();
		int left = super.absorbDamage( dmg );
		//a blow the ward takes and holds: its runes lock in on the side it came from
		if (blow && hadShield && shielding() > 0 && owner != null && owner.sprite != null) absorbed( owner.sprite, blowFrom );
		if (blow && hadShield && shielding() <= 0 && owner != null){
			blowAt = -1f;
			int level = CurrentSkills.skillLevel( ManaShield.class );
			if (level > 0) chargeWeapon( owner, level );
			if (level >= Skill.MAX_LEVEL && !reformed && owner.isAlive()){
				ManaShieldWard again = Buff.affect( owner, ManaShieldWard.class );
				again.strength = strength;
				again.reformed = true;
				again.setShield( Math.max( 1, strength / 2 ) );
				if (owner.sprite != null){
					xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.chipOn( ManaShield.class, owner );
					owner.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 8 );
					//and the ward is raised again: its runes locking in once the broken ones are gone
					reformed( owner.sprite );
				}
				SpatialSound.play( Assets.Sounds.CHARGEUP, owner, 0.8f, 1.4f );
			}
		}
		return left;
	}

	//the shards sink into the Battlemage's weapon, one per coming melee hit
	private static void chargeWeapon( Char owner, int level ){
		if (owner.sprite != null){
			broke( owner.sprite );
			owner.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 10 );
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.counterOn( ManaShield.class, owner, 1 + level, CharSprite.POSITIVE );
		}
		SpatialSound.play( Assets.Sounds.SHATTER, owner, 1f, 1.2f );
		Fx.shake( 1, 0.2f );
		Buff.affect( owner, ManaShield.Charged.class ).set( 1 + level );
	}

	/** A blow taken: ABSORB runes locking in on an arc SPREAD rad wide facing where it came from
	 *  (ManaShield's ring, RuneFX.lock's arc). Nothing held after. Any thread. */
	public static final int ABSORB = 2;
	public static final float SPREAD = 1.2f;

	public static void absorbed( final CharSprite s, final int from ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null) return;
			//toward the attacker (0 east, PI/2 the near side); with none known, the body's front
			float angle = s.flipHorizontal ? (float) Math.PI : 0f;
			if (SkillVfx.valid( from )){
				float dx = SkillVfx.cx( from ) - SkillVfx.midX( s ), dy = SkillVfx.cy( from ) - SkillVfx.midY( s );
				if (dx * dx + dy * dy >= 1f) angle = (float) Math.atan2( dy, dx );
			}
			RuneFX.lock( s, ABSORB, SkillPalette.RUNE_MANA, ManaShield.RX, ManaShield.RY, ManaShield.GAP, ManaShield.HOLD,
					angle, SPREAD );
		} );
	}

	/** The +3 ward raised again as it breaks: its runes locking in (ManaShield.raised) once the
	 *  broken ward's have scattered, SCATTER_LIFE s on, never both rings at once. Any thread. */
	public static void reformed( final CharSprite s ){
		if (s == null) return;
		Fx.post( () -> FxTimeline.start().at( SCATTER_LIFE, () -> {
			if (s.parent != null) ManaShield.raised( s );
		} ) );
	}

	/** The ward breaking into the weapon: a small flare of mana blue at the chest (S for FLARE s,
	 *  never washing the body white), a ring of it spreading from the feet, its SCATTER runes thrown
	 *  apart from outside the body (from the raised ward's RX x RY ring, SCATTER_FROM px, out to 0.6
	 *  to 1 of the way on to SCATTER_DIST px over SCATTER_LIFE s, never above the head: the ward's
	 *  ring seen breaking) and SHARDS
	 *  shards of it flicked off, falling to the feet over SHARD_LIFE s (SHARD_GLASS lit the mana's
	 *  66CCFF, at most 12 px out). Nothing held after. Any thread. */
	public static final int SCATTER = 6, SHARDS = 3;
	public static final float SCATTER_FROM = 8f, SCATTER_DIST = 15f, SCATTER_LIFE = 0.45f, SHARD_LIFE = 0.35f, FLARE = 0.3f;

	public static void broke( final CharSprite s ){
		PixelFlare.show( SkillVfx.midX( s ), SkillVfx.midY( s ), SkillPalette.MANA_LIGHT, PixelFlare.S, FLARE );
		RuneFX.scatter( s, SCATTER, SkillPalette.RUNE_MANA, SCATTER_FROM, SCATTER_DIST, SCATTER_LIFE );
		Fx.post( () -> {
			if (s.parent == null) return;
			float x = SkillVfx.midX( s ), y = SkillVfx.midY( s ), feet = s.y + s.height() - 3;
			FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, feet, 9, 31, 0.4f, SkillPalette.MANA_LIGHT, 0.7f, true );
			MoteFX.flick( x, y, SHARDS, FxFrames.SHARD_GLASS, SkillPalette.mid( SkillPalette.RUNE_MANA ), 25f, 40f, 3f, 60f,
					SHARD_LIFE, feet - y, FxBudget.P2 );
		} );
	}

	private static final String REFORMED = "reformed";
	private static final String STRENGTH = "strength";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( REFORMED, reformed );
		bundle.put( STRENGTH, strength );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		reformed = bundle.getBoolean( REFORMED );
		strength = bundle.contains( STRENGTH ) ? bundle.getInt( STRENGTH ) : shielding();
	}
}
