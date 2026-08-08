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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.MasterThievesArmband;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blackberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blueberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cloudberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.Moonberry;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ThiefSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class BlueCat extends Mob {

	public Item stolenItem;

	{
		spriteClass = ThiefSprite.class;

		HP = HT = 20 + (Dungeon.depth * Random.NormalIntRange(1, 3));
		defenseSkill = 8;

		EXP = 5;

		loot = new MasterThievesArmband().identify();
		lootChance = 0.01f;

		FLEEING = new Fleeing();

		state = HUNTING;
	}

	private static final String STOLEN_ITEM = "stolen_item";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STOLEN_ITEM, stolenItem );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		stolenItem = (Item) bundle.get( STOLEN_ITEM );
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public float attackDelay() {
		return super.attackDelay() * 0.5f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 7 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 120;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 3);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (enemy instanceof Hero && stolenItem == null) {
			Hero hero = (Hero) enemy;
			Item amulet = hero.belongings.getItem( Amulet.class );
			if (amulet != null) {
				stolenItem = amulet.detach( hero.belongings.backpack );
				if (stolenItem != null) {
					GLog.w( Messages.get(this, "stole", stolenItem.name()) );
					state = FLEEING;
				}
			}
		}

		return damage;
	}

	@Override
	public int defenseProc( Char enemy, int damage ) {
		if (state == FLEEING) {
			Dungeon.level.drop( new Gold(), pos ).sprite.drop();
		}
		return super.defenseProc( enemy, damage );
	}

	@Override
	public Item createLoot() {
		if (!Dungeon.LimitedDrops.ARMBAND.dropped()) {
			Dungeon.LimitedDrops.ARMBAND.drop();
			return super.createLoot();
		} else {
			return new Gold( Random.NormalIntRange(100, 250) );
		}
	}

	@Override
	public void rollToDropLoot() {
		super.rollToDropLoot();
		// Sprouted BERRY category: Blackberry(20), Blueberry(4), Cloudberry(16), Moonberry(2)
		int roll = Random.Int(42);
		Food berry;
		if (roll < 20)      berry = new Blackberry();
		else if (roll < 24) berry = new Blueberry();
		else if (roll < 40) berry = new Cloudberry();
		else                berry = new Moonberry();
		Dungeon.level.drop( berry, pos ).sprite.drop();
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
	}

	@Override
	protected void dropExtraLoot() {
		if (stolenItem != null) {
			trackedDrop(stolenItem, 0);
		}
	}

	@Override
	public String description() {
		String desc = super.description();

		if (stolenItem != null) {
			desc += Messages.get(this, "carries", stolenItem.name());
		}

		return desc;
	}

	private class Fleeing extends Mob.Fleeing {
		@Override
		protected void nowhereToRun() {
			if (buff( Terror.class ) == null) {
				sprite.showStatus( CharSprite.NEGATIVE, Messages.get(Mob.class, "rage") );
				state = HUNTING;
			} else {
				super.nowhereToRun();
			}
		}
	}
}
