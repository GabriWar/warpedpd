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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.MinotaurSprite;
import com.watabou.utils.Random;

//ported from Unleashed PD: gores, then backs off while the wound bleeds
public class Minotaur extends Mob {

	{
		spriteClass = MinotaurSprite.class;

		HP = HT = 120;
		defenseSkill = 22;

		EXP = 11;
		maxLvl = 28;

		state = HUNTING;
		properties.add( Property.DEMONIC );

		resistances.add( Grim.class );
		resistances.add( ScrollOfPsionicBlast.class );

		immunities.add( Terror.class );
		immunities.add( Vertigo.class );
		immunities.add( Charm.class );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 12, 30 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 32;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 14 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (Random.Int( 3 ) == 0) {
			Buff.affect( enemy, Bleeding.class ).set( Random.IntRange( 5, 7 ) );
			Buff.prolong( enemy, Cripple.class, Cripple.DURATION );
			//having drawn blood it wheels away, and comes back when the wound closes
			state = FLEEING;
		}

		return damage;
	}

	@Override
	public void notice() {
		super.notice();
		yell( Messages.get(this, "notice") );
	}

	@Override
	protected boolean act() {
		boolean result = super.act();
		if (state == FLEEING && enemy != null && enemySeen && enemy.buff( Bleeding.class ) == null) {
			state = HUNTING;
		}
		return result;
	}
}
