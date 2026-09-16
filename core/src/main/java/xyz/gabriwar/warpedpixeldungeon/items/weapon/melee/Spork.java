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

import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class Spork extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SPORK;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.3f;

		tier = 3;
		DLY = 0.5f; //2x speed
		ACC = 1.2f; //20% more accurate
		reinforced = true; //can be pushed past +15 in the upgrade blobs
	}

	@Override
	public int max(int lvl) {
		return  3*(tier+1) +    //12 base, down from 20 (balanced by speed)
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (defender instanceof Gullin || defender instanceof Kupua
				|| defender instanceof MineSentinel || defender instanceof Otiluke
				|| defender instanceof Zot || defender instanceof ZotPhase) {
			defender.damage(Random.Int(damage, damage * 4), this);
		}
		return super.proc(attacker, defender, damage);
	}


	//Sprouted weapon: no Duelist ability was ever designed for it

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	/** a skewer: driven in and twisted, so the wound keeps bleeding */
	@Override
	protected void duelistAbility( Hero hero, Integer target ){
		final Char enemy = aim( hero, target );
		if (enemy == null) return;
		final int boost = augment.damageFactor( 4 + buffedLvl() );
		final float bleed = 3 + buffedLvl() / 2f;
		hero.sprite.attack( enemy.pos, new Callback() {
			@Override
			public void call() {
				beforeAbilityUsed( hero, enemy );
				AttackIndicator.target( enemy );
				if (hero.attack( enemy, 1f, boost, Char.INFINITE_ACCURACY )){
					Sample.INSTANCE.play( Assets.Sounds.HIT_STAB, 1f, 1.3f );
					if (enemy.isAlive()){
						Buff.affect( enemy, Bleeding.class ).set( bleed );
						Splash.at( enemy.pos, 0xAA1111, 5 );
					} else {
						onAbilityKill( hero, enemy );
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
		int bl = levelKnown ? Math.round( 3 + buffedLvl() / 2f ) : 3;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+b), augment.damageFactor(max()+b), bl);
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+b, max(0)+b, bl);
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
