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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drowsy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//SPS-PD's flute: resonates on-hit, striking everything adjacent to the target
public class Flute extends MeleeWeapon {

	{
		image = ItemSpriteSheet.FLUTE;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.3f;

		tier = 2;
		RCH = 2;    //extra reach
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//resonance: half weapon damage to every other char adjacent to the target
		int p = defender.pos;
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(p + n);
			if (ch != null && ch != defender && ch != attacker && ch.isAlive()) {
				int dmg = Math.max(Random.NormalIntRange(min(), max()) - Random.IntRange(0, 1), 0);
				ch.damage(dmg / 2, this);
			}
		}
		return super.proc(attacker, defender, damage);
	}

	// ---- Duelist ability: a lullaby ----

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		return 2;
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		lullabyAbility(hero, this);
	}

	@Override
	public String abilityInfo() {
		int turns = levelKnown ? 3 + buffedLvl() : 3;
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc", turns);
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(3 + level);
	}

	/** every enemy within 3 tiles in view is slowed and grows drowsy; no damage is done */
	public static void lullabyAbility(Hero hero, MeleeWeapon wep){
		boolean any = false;
		for (Char ch : Actor.chars()){
			if (ch == hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			if (!Dungeon.level.heroFOV[ch.pos] || Dungeon.level.distance(hero.pos, ch.pos) > 3) continue;
			any = true;
		}
		if (!any){
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}
		wep.beforeAbilityUsed(hero, null);
		int turns = 3 + wep.buffedLvl();
		for (Char ch : Actor.chars()){
			if (ch == hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			if (!Dungeon.level.heroFOV[ch.pos] || Dungeon.level.distance(hero.pos, ch.pos) > 3) continue;
			Buff.prolong(ch, Slow.class, turns);
			Buff.affect(ch, Drowsy.class);
			if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.NOTE), 3);
		}
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
		Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		Invisibility.dispel();
		hero.spendAndNext(hero.attackDelay());
		wep.afterAbilityUsed(hero);
	}
}
