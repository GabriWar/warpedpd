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

import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Door;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class AssassinsKnife extends MeleeWeapon {

	{
		image = ItemSpriteSheet.ASSASSINS_KNIFE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.2f;

		tier = 2;
		DLY = 0.67f; //~1.5x speed
	}

	@Override
	public int max(int lvl) {
		return  4*(tier+1) +    //12 base, down from 15
				lvl*(tier+1);   //scaling unchanged
	}

	// ---- Duelist ability: a step behind the target and a knife under the ribs ----

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
		backstabAbility(hero, target, this);
	}

	@Override
	public String abilityInfo() {
		int boost = levelKnown ? 3 + buffedLvl() : 3;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+boost), augment.damageFactor(max()+boost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+boost, max(0)+boost);
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		int boost = 3 + level;
		return augment.damageFactor(min(level)+boost) + "-" + augment.damageFactor(max(level)+boost);
	}

	/** blink to the far side of an enemy within 4 tiles and strike it there, unmissable, for surprise damage */
	public static void backstabAbility(Hero hero, Integer target, MeleeWeapon wep){
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}
		if (hero.rooted || Dungeon.level.distance(hero.pos, target) > 4){
			GLog.w(Messages.get(wep, "ability_target_range"));
			if (hero.rooted) PixelScene.shake(1, 1f);
			return;
		}
		//the cell behind the target, as seen from the hero; failing that, any free cell beside it
		int w = Dungeon.level.width();
		int dx = Integer.signum((target % w) - (hero.pos % w)), dy = Integer.signum((target / w) - (hero.pos / w));
		int behind = target + dx + dy * w;
		int dest = -1;
		if (behind >= 0 && behind < Dungeon.level.length() && Dungeon.level.passable[behind] && Actor.findChar(behind) == null){
			dest = behind;
		} else {
			for (int n : PathFinder.NEIGHBOURS8){
				int c = target + n;
				if (c < 0 || c >= Dungeon.level.length() || c == hero.pos) continue;
				if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
				if (dest == -1 || Dungeon.level.trueDistance(c, hero.pos) > Dungeon.level.trueDistance(dest, hero.pos)) dest = c;
			}
		}
		if (dest == -1){
			GLog.w(Messages.get(wep, "ability_bad_position"));
			return;
		}
		final int landing = dest;
		final int boost = wep.augment.damageFactor(3 + wep.buffedLvl());
		hero.busy();
		CellEmitter.get(hero.pos).burst(Speck.factory(Speck.WOOL), 6);
		Sample.INSTANCE.play(Assets.Sounds.PUFF, 0.8f, 1.2f);
		hero.sprite.jump(hero.pos, landing, 0, 0.1f, new Callback() {
			@Override
			public void call() {
				if (Dungeon.level.map[hero.pos] == Terrain.OPEN_DOOR) Door.leave(hero.pos);
				hero.pos = landing;
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();
				CellEmitter.get(landing).burst(Speck.factory(Speck.WOOL), 4);
				hero.belongings.abilityWeapon = wep;
				if (enemy.isAlive() && hero.canAttack(enemy)){
					hero.sprite.attack(enemy.pos, new Callback() {
						@Override
						public void call() {
							wep.beforeAbilityUsed(hero, enemy);
							AttackIndicator.target(enemy);
							if (hero.attack(enemy, 1.5f, boost, Char.INFINITE_ACCURACY)){
								Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1f, 0.9f);
								Wound.hit(enemy);
								if (!enemy.isAlive()) wep.onAbilityKill(hero, enemy);
							}
							Invisibility.dispel();
							hero.spendAndNext(hero.attackDelay());
							wep.afterAbilityUsed(hero);
						}
					});
				} else {
					hero.belongings.abilityWeapon = null;
					Charger charger = Buff.affect(hero, Charger.class);
					charger.partialCharge -= 1;
					while (charger.partialCharge < 0 && charger.charges > 0) { charger.charges--; charger.partialCharge++; }
					updateQuickslot();
					GLog.w(Messages.get(wep, "ability_no_target"));
					hero.spendAndNext(1 / hero.speed());
				}
			}
		});
	}
}
