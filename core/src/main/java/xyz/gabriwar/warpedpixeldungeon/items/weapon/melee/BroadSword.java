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

import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import com.watabou.utils.Callback;
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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class BroadSword extends MeleeWeapon {

	{
		image = ItemSpriteSheet.BROAD_SWORD;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 3;
	}


	//Sprouted weapon: no Duelist ability was ever designed for it

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	private int boost(){ return augment.damageFactor( 3 + buffedLvl() ); }

	/** a wide arc: the target, and whoever stands on the two cells beside it, all cut in one swing */
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
				int w = Dungeon.level.width();
				int dx = (enemy.pos % w) - (hero.pos % w), dy = (enemy.pos / w) - (hero.pos / w);
				int side = -dy + dx * w;
				boolean any = false;
				for (int c : new int[]{ enemy.pos, enemy.pos + side, enemy.pos - side }){
					if (c < 0 || c >= Dungeon.level.length()) continue;
					Char ch = Actor.findChar( c );
					if (ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY) continue;
					hero.belongings.abilityWeapon = BroadSword.this;
					boolean can = hero.canAttack( ch );
					hero.belongings.abilityWeapon = null;
					if (!can) continue;
					if (hero.attack( ch, 1f, boost, Char.INFINITE_ACCURACY )){
						any = true;
						Wound.hit( ch );
						if (!ch.isAlive()) onAbilityKill( hero, ch );
					}
				}
				if (any) SpatialSound.play( Assets.Sounds.HIT_SLASH, hero, 1f, 0.8f );
				for (int n : PathFinder.NEIGHBOURS8){
					if (Dungeon.level.heroFOV[hero.pos + n]) CellEmitter.get( hero.pos + n ).burst( Speck.factory( Speck.DUST ), 1 );
				}
				Invisibility.dispel();
				hero.spendAndNext( hero.attackDelay() );
				afterAbilityUsed( hero );
			}
		} );
	}

	@Override
	public String abilityInfo() {
		int b = levelKnown ? 3 + buffedLvl() : 3;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+b), augment.damageFactor(max()+b));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+b, max(0)+b);
		}
	}

	public String upgradeAbilityStat(int level){
		int b = 3 + level;
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
