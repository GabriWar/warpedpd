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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class MageStaff extends MeleeWeapon {

	{
		image = ItemSpriteSheet.MAGE_STAFF;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		tier = 2;
		DLY = 1.5f; //~0.67x speed
	}

	@Override
	public int max(int lvl) {
		return  7*(tier+1) +    //21 base, up from 15
				lvl*(tier+1);   //scaling unchanged
	}

	// ---- Duelist ability: the staff's end, driven home as a bolt ----

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		thrustAbility(hero, target, this);
	}

	@Override
	public String abilityInfo() {
		int boost = levelKnown ? 2 + buffedLvl() : 2;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+boost), augment.damageFactor(max()+boost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+boost, max(0)+boost);
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		int boost = 2 + level;
		return augment.damageFactor(min(level)+boost) + "-" + augment.damageFactor(max(level)+boost);
	}

	/** a bolt of force down a line of 3: cannot miss, ignores armour, shoves the target one step back */
	public static void thrustAbility(Hero hero, Integer target, MeleeWeapon wep){
		if (target == null) return;
		Ballistica bolt = new Ballistica(hero.pos, target, Ballistica.MAGIC_BOLT);
		int cell = bolt.collisionPos;
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[cell]){
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, cell) > 3){
			GLog.w(Messages.get(wep, "ability_target_range"));
			return;
		}
		wep.beforeAbilityUsed(hero, enemy);
		AttackIndicator.target(enemy);
		int dmg = wep.damageRoll(hero) + wep.augment.damageFactor(2 + wep.buffedLvl());
		dmg = wep.proc(hero, enemy, dmg);
		enemy.damage(dmg, wep);
		hero.sprite.zap(cell);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.FORCE, hero.sprite, cell, () -> SkillFX.flash(enemy));
		SpatialSound.play(Assets.Sounds.ZAP, hero, 1f, 0.8f);
		SpatialSound.play(Assets.Sounds.HIT_MAGIC, enemy, 1f, 1f);
		if (enemy.isAlive() && !Pushing.pushingExistsForChar(enemy)){
			Ballistica shove = new Ballistica(cell, cell + (cell - bolt.path.get(Math.max(0, bolt.dist - 1))), Ballistica.MAGIC_BOLT);
			WandOfBlastWave.throwChar(enemy, shove, 1, true, false, hero);
		}
		Invisibility.dispel();
		if (!enemy.isAlive()) wep.onAbilityKill(hero, enemy);
		hero.spendAndNext(hero.attackDelay());
		wep.afterAbilityUsed(hero);
	}
}
