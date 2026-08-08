/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.sprites.BatSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class Bat extends Mob {

	{
		spriteClass = BatSprite.class;
		
		HP = HT = 40 + (Dungeon.depth * Random.NormalIntRange(2, 5));
		defenseSkill = 15;
		baseSpeed = 2f;
		
		EXP = 7;
		maxLvl = 15;
		
		flying = true;
		
		loot = PotionOfMending.class;
		lootChance = 0.1667f; //by default, see lootChance()

		lootOther = new MonsterMeat();
		lootChanceOther = 0.05f;

		resistances.add(Vampiric.class);
	}
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 15, 22 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 16;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 4);
	}

	@Override
	public void die(Object cause) {
		flying = false;
		super.die(cause);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		int reg = Math.min( damage - 4, HT - HP );
		
		if (reg > 0) {
			HP += reg;
			sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(reg), FloatingText.HEALING);
		}
		
		return damage;
	}
	
	@Override
	public float lootChance(){
		return super.lootChance() * ((7f - Dungeon.LimitedDrops.BAT_HP.count) / 7f);
	}
	
	@Override
	public Item createLoot(){
		Dungeon.LimitedDrops.BAT_HP.count++;
		return super.createLoot();
	}
	
}
