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

import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ParryStance;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Parry Stance: the blade that just turned a blow comes straight back at the attacker.
 * Acts right after the parried attack, the way the Gladiator's riposte does, so the
 * counter-swing is a real attack of the hero's and never nested inside the enemy's.
 */
public class ParryRiposte extends Buff {

	public static final float DAMAGE = 0.6f;

	{
		actPriority = VFX_PRIO;
	}

	//not bundled on purpose: a riposte lives for the instant after a parry, never across a save
	public Char enemy;
	public boolean sweep;

	@Override
	public boolean act() {
		final Char foe = enemy;
		if (!(target instanceof Hero) || !target.isAlive() || target.sprite == null
				|| foe == null || !foe.isAlive() || !Dungeon.level.adjacent( target.pos, foe.pos )){
			detach();
			return true;
		}
		final Hero hero = (Hero) target;
		final boolean all = sweep;
		detach();
		hero.sprite.attack( foe.pos, new Callback() {
			@Override
			public void call() {
				strike( hero, foe );
				//+3: the riposte sweeps every enemy next to the hero
				if (all){
					for (int n : PathFinder.NEIGHBOURS8){
						Char other = Actor.findChar( hero.pos + n );
						if (other != null && other != foe && other.alignment == Char.Alignment.ENEMY){
							strike( hero, other );
						}
					}
				}
				next();
			}
		} );
		return false;
	}

	private static void strike( Hero hero, Char foe ){
		if (!foe.isAlive()) return;
		if (foe.sprite != null){
			foe.sprite.showStatus( CharSprite.WARNING, Messages.get( ParryStance.class, "riposte" ) );
			Wound.hit( foe );
		}
		SpatialSound.play( Assets.Sounds.HIT_SLASH, foe, 1f, 1.2f );
		hero.attack( foe, DAMAGE, 0f, 1f );
	}
}
