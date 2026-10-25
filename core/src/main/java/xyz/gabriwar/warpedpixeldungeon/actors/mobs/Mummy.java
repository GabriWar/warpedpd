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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.sprites.MummySprite;
import com.watabou.utils.Random;

/**
 * The embalmed dead of the buried temples (levels/SandTempleLevel). As strong as the floor it walks
 * is deep - the temples' floors take the dungeon's depths - slow to anger and hard to put down, its
 * grip carrying the tomb's curse (Weakness, one blow in four). Its old linen is dry as tinder: fire
 * hurts it half again as much. Now and then it still has the gold it was buried with.
 */
public class Mummy extends Mob {

	{
		spriteClass = MummySprite.class;

		properties.add( Property.UNDEAD );
		immunities.add( Grim.class );

		loot = Gold.class;
		lootChance = 0.25f;
	}

	//the floor's depth its strength was made for
	private static int depth(){
		return Math.max( 1, Dungeon.depth );
	}

	public Mummy(){
		super();
		int d = depth();
		HP = HT = 10 + 6 * d;
		defenseSkill = 4 + d;
		EXP = 3 + d / 2;
		maxLvl = d + 8;
	}

	@Override
	public int damageRoll(){
		int d = depth();
		return Random.NormalIntRange( 1 + d / 2, 4 + d );
	}

	@Override
	public int attackSkill( Char target ){
		return 9 + depth();
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange( 0, depth() / 2 );
	}

	@Override
	public int attackProc( Char enemy, int damage ){
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 4 ) == 0) Buff.prolong( enemy, Weakness.class, Weakness.DURATION / 2f );
		return damage;
	}

	@Override
	public void damage( int dmg, Object src ){
		if (src instanceof Burning) dmg = Math.round( dmg * 1.5f );
		super.damage( dmg, src );
	}
}
