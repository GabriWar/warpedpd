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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class FightGloves extends MeleeWeapon {

	{
		image = ItemSpriteSheet.FIGHTGLOVES;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.2f;

		tier = 2;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//SPS shipped 40%; rebalanced to WPD stun norms
		if (Random.Int(100) < 20) {
			Buff.prolong(defender, Paralysis.class, 2);
		}
		return super.proc(attacker, defender, damage);
	}

	// ---- Duelist ability: three quick jabs ----

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		return 2;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		flurryAbility(hero, target, this);
	}

	private static int jabPercent(int level){
		return Math.min(80, 50 + 4 * level);
	}

	@Override
	public String abilityInfo() {
		int pct = levelKnown ? jabPercent(buffedLvl()) : jabPercent(0);
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", pct);
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return jabPercent(level) + "%";
	}

	/** three jabs at a share of full damage each; if all three land the target is dazed for a turn */
	public static void flurryAbility(Hero hero, Integer target, MeleeWeapon wep){
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
		final float share = jabPercent(wep.buffedLvl()) / 100f;
		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				wep.beforeAbilityUsed(hero, enemy);
				AttackIndicator.target(enemy);
				int landed = 0;
				for (int i = 0; i < 3 && enemy.isAlive(); i++){
					if (hero.attack(enemy, share, 0, Char.INFINITE_ACCURACY)){
						landed++;
						SpatialSound.play(Assets.Sounds.HIT_CRUSH, enemy, 1f, 1.1f + 0.2f * i);
						if (enemy.sprite != null) enemy.sprite.emitter().burst(Speck.factory(Speck.STAR), 2);
					}
				}
				if (landed == 3 && enemy.isAlive()){
					Buff.prolong(enemy, Paralysis.class, 1f);
					Wound.hit(enemy);
				}
				Invisibility.dispel();
				if (!enemy.isAlive()) wep.onAbilityKill(hero, enemy);
				hero.spendAndNext(hero.attackDelay());
				wep.afterAbilityUsed(hero);
			}
		});
	}
}
