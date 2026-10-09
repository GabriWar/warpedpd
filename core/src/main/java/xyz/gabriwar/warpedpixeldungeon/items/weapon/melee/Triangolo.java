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

import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//SPS-PD's triangolo: resonates on-hit, striking everything adjacent to the target
public class Triangolo extends MeleeWeapon {

	{
		image = ItemSpriteSheet.TRIANGOLO;
		hitSound = Assets.Sounds.HIT_PARRY;
		hitSoundPitch = 1.3f;

		tier = 1;
	}

	//SPS-PD grew reach by 1 per upgrade, to a max of 4
	@Override
	public int reachFactor(Char owner) {
		int reach = super.reachFactor(owner);
		if (!(owner instanceof Hero && RingOfForce.fightingUnarmed((Hero) owner))){
			reach += Math.min(3, Math.max(0, buffedLvl()));
		}
		return reach;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//resonance: full weapon damage to every other char adjacent to the target
		int p = defender.pos;
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(p + n);
			if (ch != null && ch != defender && ch != attacker && ch.isAlive()) {
				int dmg = Math.max(Random.NormalIntRange(min(), max()) - Random.IntRange(0, 1), 0);
				ch.damage(dmg, this);
			}
		}
		return super.proc(attacker, defender, damage);
	}

	// ---- Duelist ability: one clear chime that sets every skull nearby ringing ----

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		chimeAbility(hero, this);
	}

	@Override
	public String abilityInfo() {
		int turns = levelKnown ? 2 + buffedLvl() / 2 : 2;
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", turns);
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(2 + level / 2);
	}

	/** every enemy within 2 tiles in view reels with vertigo and takes a quarter of a strike */
	public static void chimeAbility(Hero hero, MeleeWeapon wep){
		boolean any = false;
		for (Char ch : Actor.chars()){
			if (ch == hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			if (!Dungeon.level.heroFOV[ch.pos] || Dungeon.level.distance(hero.pos, ch.pos) > 2) continue;
			any = true;
		}
		if (!any){
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}
		wep.beforeAbilityUsed(hero, null);
		int turns = 2 + wep.buffedLvl() / 2;
		for (Char ch : Actor.chars()){
			if (ch == hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			if (!Dungeon.level.heroFOV[ch.pos] || Dungeon.level.distance(hero.pos, ch.pos) > 2) continue;
			Buff.prolong(ch, Vertigo.class, turns);
			ch.damage(Math.max(1, wep.damageRoll(hero) / 4), wep);
			if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.NOTE), 3);
			SkillFX.flash(ch);
		}
		hero.sprite.operate(hero.pos);
		new Flare(6, 18).color(0xFFE080, true).show(hero.sprite, 0.6f).angularSpeed = 180;
		SpatialSound.play(Assets.Sounds.HIT_PARRY, hero, 1f, 1.6f);
		SpatialSound.playDelayed(Assets.Sounds.HIT_PARRY, 0.15f, hero, 0.8f, 1.9f);
		Invisibility.dispel();
		hero.spendAndNext(hero.attackDelay());
		wep.afterAbilityUsed(hero);
	}
}
