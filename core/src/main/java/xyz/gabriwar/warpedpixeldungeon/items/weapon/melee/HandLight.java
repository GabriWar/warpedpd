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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class HandLight extends MeleeWeapon {

	{
		image = ItemSpriteSheet.HAND_LIGHT;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.1f;

		tier = 4;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  5*(tier) +      //20 base, down from 25
				lvl*(tier);     //+4 per level, down from +5
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
			Buff.prolong(defender, Blindness.class, 3f);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	private int boost(){ return augment.damageFactor( 4 + buffedLvl() ); }

	/** a sear: the light is pressed into the target, which burns, and flares so bright everyone beside it is blinded */
	@Override
	protected void duelistAbility( Hero hero, Integer target ){
		final Char enemy = aim( hero, target );
		if (enemy == null) return;
		final int boost = boost();
		hero.sprite.attack( enemy.pos, new Callback() {
			@Override
			public void call() {
				beforeAbilityUsed( hero, enemy );
				AttackIndicator.target( enemy );
				if (hero.attack( enemy, 1f, boost, Char.INFINITE_ACCURACY )){
					if (enemy.sprite != null){
						new Flare( 8, 24 ).color( 0xFFFFFF, true ).show( enemy.sprite, 0.6f );
						enemy.sprite.emitter().burst( FlameParticle.FACTORY, 6 );
					}
					SpatialSound.play( Assets.Sounds.BURNING, enemy, 1f, 1.1f );
					if (enemy.isAlive()) Buff.affect( enemy, Burning.class ).reignite( enemy );
					else onAbilityKill( hero, enemy );
					for (int n : PathFinder.NEIGHBOURS8){
						Char near = Actor.findChar( enemy.pos + n );
						if (near != null && near != hero && near.alignment == Char.Alignment.ENEMY){
							Buff.prolong( near, Blindness.class, 3f );
							if (near.sprite != null) near.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 2 );
						}
					}
				}
				Invisibility.dispel();
				hero.spendAndNext( hero.attackDelay() );
				afterAbilityUsed( hero );
			}
		} );
	}

	@Override
	public String abilityInfo() {
		int b = levelKnown ? 4 + buffedLvl() : 4;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+b), augment.damageFactor(max()+b));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+b, max(0)+b);
		}
	}

	public String upgradeAbilityStat(int level){
		int b = 4 + level;
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
