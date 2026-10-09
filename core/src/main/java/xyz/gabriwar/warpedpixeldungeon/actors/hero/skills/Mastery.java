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


import com.watabou.noosa.Camera;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Mastery extends PassiveSkillB3 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Mastery";
		image = 11;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//the weapon's reach: an enemy that steps up next to the hero may be struck before it can swing.
	//deferred by the core, so the strike lands after the enemy's move and before anyone else acts
	@Override
	public void onEnemyStepsAdjacent( Char enemy, int from ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || enemy == null || !enemy.isAlive()
				|| enemy.alignment != Char.Alignment.ENEMY || hero.paralysed > 0
				|| !Dungeon.level.adjacent( hero.pos, enemy.pos )) return;
		KindOfWeapon wep = hero.belongings.weapon();
		if (wep == null || wep instanceof MissileWeapon) return;
		if (Random.Int( 100 ) >= 15 + 15 * level) return;

		if (hero.sprite != null){
			hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "riposte" ) );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
		}
		Wound.hit( enemy );
		if (enemy.sprite != null && enemy.sprite.visible){
			enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		}
		SpatialSound.play( Assets.Sounds.HIT_PARRY, enemy, 1f, 1.1f );
		Camera.main.shake( 1, 0.15f );
		enemy.damage( Math.max( 0, wep.damageRoll( hero ) - enemy.drRoll() ), this );

		//+3: the blow throws it back the way it came
		if (level >= MAX_LEVEL && enemy.isAlive() && !enemy.properties().contains( Char.Property.BOSS )){
			SkillInteractions.push( enemy, hero.pos, 1, 0 );
		}
	}
}
