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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Camera;
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
					owner.sprite.showStatus( CharSprite.POSITIVE, Messages.get( ManaShield.class, "reform" ) );
					owner.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 8 );
				}
				SpatialSound.play( Assets.Sounds.CHARGEUP, owner, 0.8f, 1.4f );
			}
		}
		return left;
	}

	//the shards sink into the Battlemage's weapon, one per coming melee hit
	private static void chargeWeapon( Char owner, int level ){
		if (owner.sprite != null){
			new Flare( 8, 28 ).color( 0x55AAFF, true ).show( owner.sprite, 0.8f );
			owner.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 10 );
			owner.sprite.showStatus( CharSprite.POSITIVE, Messages.get( ManaShield.class, "charged", 1 + level ) );
		}
		SpatialSound.play( Assets.Sounds.SHATTER, owner, 1f, 1.2f );
		Camera.main.shake( 1, 0.2f );
		Buff.affect( owner, ManaShield.Charged.class ).set( 1 + level );
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
