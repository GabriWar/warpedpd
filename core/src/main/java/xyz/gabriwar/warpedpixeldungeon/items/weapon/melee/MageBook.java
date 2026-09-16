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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class MageBook extends MeleeWeapon {

	{
		image = ItemSpriteSheet.MAGEBOOK;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 1;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//SPS shipped 50%; rebalanced to WPD stun norms
		if (Random.Int(100) < 15) {
			Buff.prolong(defender, Paralysis.class, 2);
		}
		return super.proc(attacker, defender, damage);
	}

	// ---- Duelist ability: a page torn out and read at the enemy ----

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		pageAbility(hero, target, this);
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

	/** a bolt of the book's own magic, 3 tiles, cannot miss, ignores armour, and a sip of mana back */
	public static void pageAbility(Hero hero, Integer target, MeleeWeapon wep){
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
		hero.MP = Math.min(hero.MT, hero.MP + 1);
		hero.sprite.zap(cell);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.MAGIC_MISSILE, hero.sprite, cell, () -> SkillFX.flash(enemy));
		Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 1.1f);
		Sample.INSTANCE.play(Assets.Sounds.READ, 0.6f, 1.3f);
		if (enemy.sprite != null) enemy.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
		Invisibility.dispel();
		if (!enemy.isAlive()) wep.onAbilityKill(hero, enemy);
		hero.spendAndNext(hero.attackDelay());
		wep.afterAbilityUsed(hero);
	}
}
