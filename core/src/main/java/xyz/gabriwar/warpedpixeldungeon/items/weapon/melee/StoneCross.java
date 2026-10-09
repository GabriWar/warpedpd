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

import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

public class StoneCross extends MeleeWeapon {

	{
		image = ItemSpriteSheet.STONE_CROSS;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.9f;

		tier = 5;
		ACC = 0.8f; //0.8x accuracy
		DLY = 1.2f; //~0.83x speed
	}

	private static final int CHARGE_CAP = 20;

	private int charge = 0;

	@Override
	public int max(int lvl) {
		return  6*(tier+1) +    //36 base, up from 30
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int damageRoll(Char owner) {
		int damage = super.damageRoll(owner);
		if (charge >= CHARGE_CAP) {
			damage *= 5;
			SpatialSound.play(Assets.Sounds.HIT_STRONG, owner);
		}
		return damage;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (charge >= CHARGE_CAP) {
			charge = 0;
		}
		charge++;

		return super.proc(attacker, defender, damage);
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc") + " " + Messages.get(this, "charge", charge, CHARGE_CAP);
	}


	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
	}

	// ---- Duelist: Sanctify. The cross comes down on one enemy for a heavy blow, ----
	// ---- doubled against the undead and the demonic, and the ground around the ----
	// ---- blow is hallowed: every ally standing there, the Duelist included, is blessed. ----

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		return 2;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	private int holyBoost(){
		return 8 + 2 * buffedLvl();
	}

	private int blessTurns(){
		return 3 + buffedLvl() / 2;
	}

	private static boolean unholy(Char ch){
		return ch.properties().contains(Char.Property.UNDEAD) || ch.properties().contains(Char.Property.DEMONIC);
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(this, "ability_no_target"));
			return;
		}
		hero.belongings.abilityWeapon = this;
		if (!hero.canAttack(enemy)){
			GLog.w(Messages.get(this, "ability_target_range"));
			hero.belongings.abilityWeapon = null;
			return;
		}
		hero.belongings.abilityWeapon = null;

		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				beforeAbilityUsed(hero, enemy);
				AttackIndicator.target(enemy);
				float multi = unholy(enemy) ? 2f : 1f;
				int boost = augment.damageFactor(holyBoost());
				if (hero.attack(enemy, multi, boost, Char.INFINITE_ACCURACY)){
					SpatialSound.play(Assets.Sounds.HIT_STRONG, enemy, 1f, 0.8f);
				}
				SkillFX.pillar(enemy.pos, 0xFFEE99);
				SpatialSound.play(Assets.Sounds.CHARMS, enemy, 0.8f, 1.1f);
				//hallowed ground: the cell and its ring
				int turns = blessTurns();
				for (int n : PathFinder.NEIGHBOURS9){
					int c = enemy.pos + n;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					if (Dungeon.level.heroFOV[c] && !Dungeon.level.solid[c]) CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 2);
					Char ch = Actor.findChar(c);
					if (ch != null && ch.isAlive() && (ch == hero || ch.alignment == Char.Alignment.ALLY)){
						Buff.prolong(ch, Bless.class, turns);
					}
				}
				Invisibility.dispel();
				if (!enemy.isAlive()){
					hero.next();
					onAbilityKill(hero, enemy);
				} else {
					hero.spendAndNext(hero.attackDelay());
				}
				afterAbilityUsed(hero);
			}
		});
	}

	@Override
	public String abilityInfo() {
		int boost = levelKnown ? holyBoost() : 8;
		int turns = levelKnown ? blessTurns() : 3;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+boost), augment.damageFactor(max()+boost), turns);
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+boost, max(0)+boost, turns);
		}
	}

	@Override
	public String upgradeAbilityStat(int level){
		int boost = 8 + 2 * level;
		return augment.damageFactor(min(level)+boost) + "-" + augment.damageFactor(max(level)+boost) + ", " + (3 + level/2);
	}
}
