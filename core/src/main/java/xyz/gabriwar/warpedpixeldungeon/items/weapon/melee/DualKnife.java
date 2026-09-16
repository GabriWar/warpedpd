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

import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class DualKnife extends MeleeWeapon {

	{
		image = ItemSpriteSheet.DUAL_KNIFE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.1f;

		tier = 2;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//second blade: an extra strike for 25-50% of the attacker's damage roll
		int exdmg = attacker.damageRoll();
		defender.damage(Random.Int(exdmg/4, exdmg/2), this);
		return super.proc(attacker, defender, damage);
	}

	// ---- Duelist ability: both blades, one after the other ----

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		twinCutAbility(hero, target, this);
	}

	private static int cutPercent(int level){
		return Math.min(100, 65 + 5 * level);
	}

	@Override
	public String abilityInfo() {
		int pct = levelKnown ? cutPercent(buffedLvl()) : cutPercent(0);
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", pct);
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return cutPercent(level) + "%";
	}

	/** two unmissable strikes in the time of one, each at a share of full damage */
	public static void twinCutAbility(Hero hero, Integer target, MeleeWeapon wep){
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}
		hero.belongings.abilityWeapon = wep;
		if (!hero.canAttack(enemy)){
			GLog.w(Messages.get(wep, "ability_target_range"));
			hero.belongings.abilityWeapon = null;
			return;
		}
		hero.belongings.abilityWeapon = null;
		final float share = cutPercent(wep.buffedLvl()) / 100f;
		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				wep.beforeAbilityUsed(hero, enemy);
				AttackIndicator.target(enemy);
				boolean hit = hero.attack(enemy, share, 0, Char.INFINITE_ACCURACY);
				if (hit) Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1f, 1.1f);
				if (enemy.isAlive()){
					if (hero.attack(enemy, share, 0, Char.INFINITE_ACCURACY)){
						Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1f, 1.4f);
						Wound.hit(enemy);
					}
				}
				Invisibility.dispel();
				if (!enemy.isAlive()) wep.onAbilityKill(hero, enemy);
				hero.spendAndNext(hero.attackDelay());
				wep.afterAbilityUsed(hero);
			}
		});
	}
}
