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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.Random;

public class FinesseGrip extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	private static final int SLIP_CHANCE = 15;

	{
		tag = "CA";
		name = "Finesse Grip";
		image = 141;
		tier = 4;
	}

	//live while unarmed or holding a light (tier 1-3) melee weapon
	private boolean qualifies(){
		if (level <= 0 || Dungeon.hero == null) return false;
		KindOfWeapon w = Dungeon.hero.belongings.weapon();
		if (w == null) return true;
		if (!(w instanceof MeleeWeapon)) return false;
		return ((MeleeWeapon)w).tier <= 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a light melee hit can flick in a quick second cut: 15% / 25% / 35% chance for 40% of the hit.
	//the cut stops one short of killing, so the finishing blow (and every on-kill effect) stays the hero's
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || enemy == null || !enemy.isAlive() || damage <= 0 || !qualifies()
				|| Random.Int( 100 ) >= 5 + 10 * level){
			return damage;
		}
		int cut = Math.min( Math.round( damage * 0.4f ), enemy.HP - 1 );
		if (cut <= 0){
			return damage;
		}
		enemy.damage( cut, this );
		Wound.hit( enemy );
		Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 0.8f, 1.4f );
		//+3: a second cut that lands can flick in a third at the same odds
		if (level >= MAX_LEVEL && enemy.isAlive() && enemy.HP > 1 && Random.Int( 100 ) < 5 + 10 * level){
			int third = Math.min( Math.round( damage * 0.4f ), enemy.HP - 1 );
			if (third > 0){
				enemy.damage( third, this );
				Wound.hit( enemy );
				Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 0.8f, 1.6f );
			}
		}
		return damage;
	}

}
