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

import xyz.gabriwar.warpedpixeldungeon.levels.features.Door;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class MirrorDoll extends MeleeWeapon {

	{
		image = ItemSpriteSheet.MIRROR_DOLL;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.2f;

		tier = 2;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  5*(tier) +      //10 base, down from 15
				lvl*(tier);     //+2 per level, down from +3
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
			Buff.prolong(defender, Blindness.class, 5f);
		}
		return super.proc(attacker, defender, damage);
	}


	// ---- Duelist ability: the doll holds up its mirror and the two of you trade places ----

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		switchAbility(hero, target, this);
	}

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		return 2;
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

	/** swap places with any enemy in view; it comes out of the mirror blinded */
	public static void switchAbility(Hero hero, Integer target, MeleeWeapon wep){
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}
		//any enemy in view: the mirror does not care about distance, only about sight
		if (hero.rooted || enemy.rooted
				|| enemy.properties().contains(Char.Property.IMMOVABLE)
				|| (Char.hasProp(enemy, Char.Property.LARGE) && !Dungeon.level.openSpace[hero.pos])){
			GLog.w(Messages.get(wep, "ability_bad_position"));
			if (hero.rooted) PixelScene.shake(1, 1f);
			return;
		}
		wep.beforeAbilityUsed(hero, enemy);
		int from = hero.pos, to = enemy.pos;
		new Flare(6, 16).color(0xDDEEFF, true).show(hero.sprite, 0.5f);
		if (enemy.sprite != null) new Flare(6, 16).color(0xDDEEFF, true).show(enemy.sprite, 0.5f);
		CellEmitter.get(from).burst(Speck.factory(Speck.LIGHT), 6);
		CellEmitter.get(to).burst(Speck.factory(Speck.LIGHT), 6);
		if (Dungeon.level.map[from] == Terrain.OPEN_DOOR) Door.leave(from);
		if (Dungeon.level.map[to] == Terrain.OPEN_DOOR) Door.leave(to);
		hero.pos = to;
		enemy.pos = from;
		Dungeon.level.occupyCell(hero);
		Dungeon.level.occupyCell(enemy);
		hero.sprite.place(to);
		if (enemy.sprite != null) enemy.sprite.place(from);
		Dungeon.observe();
		GameScene.updateFog();
		Buff.prolong(enemy, Blindness.class, 3 + wep.buffedLvl());
		SpatialSound.play(Assets.Sounds.MELD, hero, 1f, 1.3f);
		Invisibility.dispel();
		hero.spendAndNext(hero.attackDelay());
		wep.afterAbilityUsed(hero);
	}
}
