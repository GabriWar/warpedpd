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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ChaosMageSprite;
import com.watabou.utils.Random;

//ported from Unleashed PD: drinks the life out of what it hits and throws
//whatever curse comes to hand
public class ChaosMage extends Mob {

	{
		spriteClass = ChaosMageSprite.class;

		HP = HT = 125;
		defenseSkill = 28;

		EXP = 15;
		maxLvl = 30;

		baseSpeed = 2f;
		state = HUNTING;

		properties.add( Property.DEMONIC );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 20, 35 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 40;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 18 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (enemy instanceof Hero) {
			corrupt( (Hero) enemy );
		}

		if (damage > 0) {
			int healingAmt = Random.NormalIntRange( 0, damage );
			HP = Math.min( HT, HP + healingAmt );
			if (sprite != null) {
				sprite.emitter().burst( ShadowParticle.UP, 2 );
			}
		}

		return damage;
	}

	private void corrupt( Hero hero ) {
		switch (Random.Int( 7 )) {
			case 0:
				Buff.prolong( hero, Vertigo.class, 4 );
				break;
			case 1:
				Buff.affect( hero, Poison.class ).set( 3 );
				break;
			case 2:
				Buff.affect( hero, Burning.class ).reignite( hero );
				break;
			case 3:
				Buff.prolong( hero, Blindness.class, 4 );
				break;
			case 4:
				Buff.prolong( hero, Slow.class, 6 );
				break;
			default:
				//two cases in seven land nothing at all
				break;
		}
	}
}
