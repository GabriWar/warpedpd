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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
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

public class Nunchakus extends MeleeWeapon {

	{
		image = ItemSpriteSheet.NUNCHAKUS;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.1f;

		tier = 3;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//SPS-PD applied HolyStun here; WPD has no such buff, Paralysis is the closest match
		//SPS shipped 20%; rebalanced to WPD stun norms
		if (Random.Int(100) < 25) {
			Buff.prolong(defender, Paralysis.class, 2);
		}
		return super.proc(attacker, defender, damage);
	}

	//SPS-PD weapon: no Duelist ability was ever designed for it

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	/** a whirl: two strikes in one motion, the first at full weight, the second at three quarters; neither can miss */
	@Override
	protected void duelistAbility( Hero hero, Integer target ){
		final Char enemy = aim( hero, target );
		if (enemy == null) return;
		final int boost = augment.damageFactor( 2 + buffedLvl() / 2 );
		hero.sprite.attack( enemy.pos, new Callback() {
			@Override
			public void call() {
				beforeAbilityUsed( hero, enemy );
				AttackIndicator.target( enemy );
				new Flare( 5, 14 ).color( 0xCCCCCC, true ).show( hero.sprite, 0.4f ).angularSpeed = 720;
				if (hero.attack( enemy, 1f, boost, Char.INFINITE_ACCURACY )){
					SpatialSound.play( Assets.Sounds.HIT_CRUSH, enemy, 1f, 1.2f );
					if (enemy.isAlive()){
						if (hero.attack( enemy, 0.75f, boost, Char.INFINITE_ACCURACY )){
							Wound.hit( enemy );
							SpatialSound.play( Assets.Sounds.HIT_CRUSH, enemy, 1f, 1.4f );
						}
					}
					if (!enemy.isAlive()) onAbilityKill( hero, enemy );
				}
				Invisibility.dispel();
				hero.spendAndNext( hero.attackDelay() );
				afterAbilityUsed( hero );
			}
		} );
	}

	@Override
	public String abilityInfo() {
		int b = levelKnown ? 2 + buffedLvl() / 2 : 2;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+b), augment.damageFactor(max()+b));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+b, max(0)+b);
		}
	}

	public String upgradeAbilityStat(int level){
		int b = 2 + level / 2;
		return augment.damageFactor(min(level)+b) + "-" + augment.damageFactor(max(level)+b);
	}

	//the enemy under the cursor, if it is one the hero can reach; null (with a message) if not
	private Char aim( Hero hero, Integer target ){
		if (target == null) return null;
		Char enemy = Actor.findChar( target );
		if (enemy == null || enemy == hero || hero.isCharmedBy( enemy ) || !Dungeon.level.heroFOV[target]){
			GLog.w( Messages.get( this, "ability_no_target" ) );
			return null;
		}
		hero.belongings.abilityWeapon = this;
		boolean can = hero.canAttack( enemy );
		hero.belongings.abilityWeapon = null;
		if (!can){
			GLog.w( Messages.get( this, "ability_target_range" ) );
			return null;
		}
		return enemy;
	}
}
