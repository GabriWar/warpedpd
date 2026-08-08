/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.sprites.DwarfKingTombSprite;
import com.watabou.utils.Random;

public class ControlPanel extends Mob {

	{
		spriteClass = DwarfKingTombSprite.class;

		HP = HT = 6000;
		defenseSkill = 50;

		EXP = 10;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		loot = new RedDewdrop();
		lootChance = 0.05f;

		properties.add( Property.IMMOVABLE );
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill( Char target ) {
		return 0;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 80 );
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public void beckon( int cell ) {
		//ignores beckon
	}
}
