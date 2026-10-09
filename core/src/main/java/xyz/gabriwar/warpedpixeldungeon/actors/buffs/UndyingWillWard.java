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

import com.watabou.noosa.Camera;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.UndyingWill;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The barrier Undying Will raises. It decays like any Barrier. Every point it soaks feeds the
 * Berserker's rage, and when blows break it (a fade does not count) its bearer roars and the
 * enemies nearby turn on him.
 */
public class UndyingWillWard extends Barrier {

	private static final int TAUNT_RADIUS = 4;

	private boolean fading = false;

	public void raise( int amount ){
		setShield( amount );
	}

	@Override
	public boolean act(){
		fading = true;
		try {
			return super.act();
		} finally {
			fading = false;
		}
	}

	@Override
	public int absorbDamage( int dmg ){
		int before = shielding();
		Char bearer = target;
		int left = super.absorbDamage( dmg );
		int soaked = before - shielding();
		if (soaked > 0 && !fading && bearer instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero
				&& ((xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) bearer).subClass == xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass.BERSERKER){
			Buff.affect( bearer, Berserk.class ).damage( 2 * soaked );
			if (bearer.sprite != null) bearer.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), Math.min( 5, 1 + soaked / 3 ) );
		}
		if (before > 0 && shielding() <= 0 && !fading && bearer != null && bearer.isAlive()){
			roar( bearer );
		}
		return left;
	}

	private void roar( Char bearer ){
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive()
					&& Dungeon.level.distance( bearer.pos, mob.pos ) <= TAUNT_RADIUS){
				mob.aggro( bearer );
				if (mob.sprite != null && mob.sprite.visible) mob.sprite.emitter().burst( Speck.factory( Speck.SCREAM ), 2 );
			}
		}
		if (bearer.sprite != null){
			bearer.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 10 );
			bearer.sprite.emitter().burst( Speck.factory( Speck.SCREAM ), 4 );
			bearer.sprite.showStatus( CharSprite.WARNING, Messages.get( UndyingWill.class, "shatter" ) );
		}
		SpatialSound.play( Assets.Sounds.CHALLENGE, bearer, 1f, 0.6f );
		Camera.main.shake( 2, 0.25f );
	}
}
