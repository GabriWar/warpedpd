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

import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonKingSprite;
import com.watabou.utils.Random;

public class SkeletonKing extends Mob {

	{
		spriteClass = SkeletonKingSprite.class;

		HP = HT = 550;
		defenseSkill = 30;

		EXP = 10;
		maxLvl = 20;

		flying = true;

		loot = PotionOfLiquidFlame.class;
		lootChance = 0.1f;

		properties.add(Property.BOSS);
		properties.add(Property.UNDEAD);

		immunities.add(Burning.class);
		immunities.add(Blazing.class);
		immunities.add(WandOfFireblast.class);

		declareExtraLoot(AdamantWeapon.class, 1f);

		//no metabolism to disturb: only the extremes reach it
		thermal = Thermal.INSENSATE;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(20, 40);
	}

	@Override
	public int attackSkill(Char target) {
		return 25;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 25);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (Random.Int(2) == 0 && enemy == Dungeon.hero) {
			Buff.prolong(enemy, Weakness.class, Weakness.DURATION);
			state = FLEEING;
		}
		return damage;
	}

	@Override
	protected boolean act() {
		boolean result = super.act();
		if (state == FLEEING
				&& buff(Terror.class) == null
				&& enemySeen
				&& enemy != null
				&& enemy.buff(Weakness.class) == null) {
			state = HUNTING;
		}
		return result;
	}

	@Override
	public void die(Object cause) {
		Dungeon.skeletonkingkilled = true;
		GameScene.bossSlain();
		super.die(cause);
		yell(Messages.get(this, "die"));
	}

	@Override
	protected void dropExtraLoot() {
		trackedDrop(new Gold(Random.IntRange(1900, 4000)), 0);
		trackedDrop(new AdamantWeapon(), 1);
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss( this );
		yell(Messages.get(this, "notice"));
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss( this );
		super.damage( dmg, src );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (enemySeen || HP < HT) BossHealthBar.assignBoss( this );
	}
}
