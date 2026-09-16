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

import com.watabou.utils.PathFinder;
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class TrickSand extends MeleeWeapon {

	{
		image = ItemSpriteSheet.TRICK_SAND;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.3f;

		tier = 1;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  5*(tier) +      //5 base, down from 10
				lvl*(tier);     //+1 per level, down from +2
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//deals its damage a second time against magically shielded targets
		if (defender.shielding() > 0){
			defender.damage(damage, this);
		}
		//blinds on hit, or deals 50% bonus damage to already blinded targets
		if (defender.buff(Blindness.class) != null) {
			defender.damage(Math.round(damage * 0.5f), this);
		} else {
			Buff.prolong(defender, Blindness.class, 6f);
		}
		return super.proc(attacker, defender, damage);
	}


	// ---- Duelist ability: a fistful of the sand thrown up in a cloud ----

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		cloudAbility(hero, target, this);
	}

	@Override
	public String abilityInfo() {
		int turns = levelKnown ? 4 + buffedLvl() : 4;
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", turns);
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(4 + level);
	}

	/** a cloud on a tile within reach: every enemy in it and beside it is blinded, no damage */
	public static void cloudAbility(Hero hero, Integer target, MeleeWeapon wep){
		if (target == null) return;
		if (!Dungeon.level.heroFOV[target] || Dungeon.level.distance(hero.pos, target) > wep.reachFactor(hero)){
			GLog.w(Messages.get(wep, "ability_target_range"));
			return;
		}
		wep.beforeAbilityUsed(hero, null);
		int turns = 4 + wep.buffedLvl();
		hero.sprite.zap(target);
		for (int n : PathFinder.NEIGHBOURS9){
			int c = target + n;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 6);
			Char ch = Actor.findChar(c);
			if (ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY){
				Buff.prolong(ch, Blindness.class, turns);
				SkillFX.flash(ch);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.PUFF, 1f, 0.8f);
		Invisibility.dispel();
		hero.spendAndNext(hero.attackDelay());
		wep.afterAbilityUsed(hero);
	}
}
