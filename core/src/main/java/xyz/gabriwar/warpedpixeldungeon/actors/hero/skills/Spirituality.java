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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Spirituality extends PassiveSkillA1 {

	{
		name = "Spirituality";
		image = 25;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a blow that lands jolts mana loose: never more than the blow itself, so trading
	//health for mana is the best a weak attacker can be farmed for
	@Override
	public int onDefendProc( Char enemy, int damage ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage <= 0) return damage;
		int amount = Math.min( damage, 1 + level );
		int effectiveMT = hero.MT + RingOfMagic.manaBonus( hero );

		if (hero.MP < effectiveMT){
			int gain = Math.min( amount, effectiveMT - hero.MP );
			hero.MP += gain;
			if (hero.sprite != null){
				hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 2 + gain );
				hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "jolt", gain ) );
			}
			SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.5f, 1.4f );

		//a full spirit has nowhere to put it: the jolt lashes back at the attacker
		} else if (level >= MAX_LEVEL && enemy != null && enemy != hero && enemy.isAlive()){
			enemy.damage( amount, this );
			if (enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]){
				enemy.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
				enemy.sprite.flash();
			}
			SpatialSound.play( Assets.Sounds.HIT_MAGIC, enemy, 0.8f, 1.2f );
		}
		return damage;
	}
}
