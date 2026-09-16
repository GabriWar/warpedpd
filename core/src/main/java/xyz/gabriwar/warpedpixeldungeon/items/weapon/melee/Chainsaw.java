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

import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.utils.Callback;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Chainsaw extends MeleeWeapon {

	{
		image = ItemSpriteSheet.CHAINSAW;
		tier = 5;
		DLY = 0.75f;
		ACC = 1.2f;

		bones = false;
		unique = true;
		cursed = true;
	}

	public boolean turnedOn = false;

	public static final String AC_ON  = "ON";
	public static final String AC_OFF = "OFF";

	@Override
	public int min(int lvl) { return 1 + lvl; }

	@Override
	public int max(int lvl) { return 12 + lvl * 6; }

	@Override
	public int STRReq(int lvl) { return 16 - lvl; }

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)) {
			if (turnedOn) {
				actions.add(AC_OFF);
			} else {
				actions.add(AC_ON);
			}
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_ON)) {
			turnedOn = true;
			GLog.i(Messages.get(this, "turn_on"));
			hero.next();
		} else if (action.equals(AC_OFF)) {
			turnedOn = false;
			GLog.i(Messages.get(this, "turn_off"));
			hero.next();
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (enchantment == null) {
			enchant(new BuzzSaw());
		}
		if (defender instanceof Gullin || defender instanceof Kupua
				|| defender instanceof MineSentinel || defender instanceof Otiluke
				|| defender instanceof Zot || defender instanceof ZotPhase) {
			defender.damage(Random.Int(damage, damage * 4), this);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	private static final String TURNED_ON = "turnedOn";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TURNED_ON, turnedOn);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		turnedOn = bundle.getBoolean(TURNED_ON);
	}


	// ---- Duelist: Rip. The saw is held in the wound: three bites in one swing, each ----
	// ---- at 60% damage, each opening a bleed. Loud, and the screen shakes with it. ----

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
				int bleed = 2 + buffedLvl() / 2;
				boolean any = false;
				for (int i = 0; i < 3 && enemy.isAlive(); i++){
					if (hero.attack(enemy, 0.6f, 0, Char.INFINITE_ACCURACY)){
						any = true;
						Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.7f + 0.15f * i);
						if (enemy.isAlive()) Buff.affect(enemy, Bleeding.class).set(bleed);
						if (enemy.sprite != null) enemy.sprite.emitter().burst(BloodParticle.BURST, 4);
					}
				}
				if (any){
					Wound.hit(enemy);
					Camera.main.shake(2, 0.3f);
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
		int bleed = levelKnown ? 2 + buffedLvl() / 2 : 2;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(Math.round(min()*0.6f)), augment.damageFactor(Math.round(max()*0.6f)), bleed);
		} else {
			return Messages.get(this, "typical_ability_desc", Math.round(min(0)*0.6f), Math.round(max(0)*0.6f), bleed);
		}
	}

	@Override
	public String upgradeAbilityStat(int level){
		return augment.damageFactor(Math.round(min(level)*0.6f)) + "-" + augment.damageFactor(Math.round(max(level)*0.6f)) + " x3, " + (2 + level/2);
	}
}
