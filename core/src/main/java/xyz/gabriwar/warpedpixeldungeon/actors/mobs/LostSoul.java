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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.LostSoulSprite;
import com.watabou.utils.Random;

//ported from Unleashed PD: a burning undead. Fire feeds it, cold tears it apart,
//and standing in water when the cold hits is fatal.
public class LostSoul extends Mob {

	{
		spriteClass = LostSoulSprite.class;

		HP = HT = 100;
		defenseSkill = 32;

		EXP = 12;
		maxLvl = 32;

		baseSpeed = 2f;
		flying = true;

		properties.add( Property.UNDEAD );
		properties.add( Property.DEMONIC );
		properties.add( Property.FIERY );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 22, 35 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 46;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 15 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (Random.Int( 3 ) == 0) {
			Buff.affect( enemy, Burning.class ).reignite( enemy );
		}

		return damage;
	}

	@Override
	public synchronized boolean add( Buff buff ) {
		if (buff instanceof Burning) {
			//fire mends it rather than harming it
			if (HP < HT) {
				HP++;
				if (sprite != null) {
					sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
				}
			}
			return false;
		} else if (buff instanceof Frost || buff instanceof Chill) {
			if (Dungeon.level.water[pos]) {
				damage( Random.NormalIntRange( HT / 2, HT ), buff );
			} else {
				damage( Random.NormalIntRange( 1, HT * 2 / 3 ), buff );
			}
			return false;
		} else {
			return super.add( buff );
		}
	}
}
