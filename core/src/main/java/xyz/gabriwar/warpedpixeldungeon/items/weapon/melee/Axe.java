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

import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import com.watabou.utils.Callback;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class Axe extends MeleeWeapon {

	{
		image = ItemSpriteSheet.AXE_SPROUTED;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		tier = 4;
		DLY = 1.2f; //~0.83x speed
	}

	@Override
	public int max(int lvl) {
		return  6*(tier+1) +    //30 base, up from 25
				lvl*(tier+1);   //scaling unchanged
	}


	//Sprouted weapon: no Duelist ability was ever designed for it

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	private int boost(){ return augment.damageFactor( 6 + buffedLvl() ); }

	/** a chop: heavy and certain, and twice as heavy on an enemy already under half health */
	@Override
	protected void duelistAbility( Hero hero, Integer target ){
		final Char enemy = aim( hero, target );
		if (enemy == null) return;
		final int boost = enemy.HP * 2 <= enemy.HT ? boost() * 2 : boost();
		hero.sprite.attack( enemy.pos, new Callback() {
			@Override
			public void call() {
				beforeAbilityUsed( hero, enemy );
				AttackIndicator.target( enemy );
				if (hero.attack( enemy, 1f, boost, Char.INFINITE_ACCURACY )){
					Wound.hit( enemy );
					Camera.main.shake( 2, 0.25f );
					Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 1f, 0.7f );
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
		int b = levelKnown ? 6 + buffedLvl() : 6;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+b), augment.damageFactor(max()+b), augment.damageFactor(min()+2*b), augment.damageFactor(max()+2*b));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+b, max(0)+b, min(0)+2*b, max(0)+2*b);
		}
	}

	public String upgradeAbilityStat(int level){
		int b = 6 + level;
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
