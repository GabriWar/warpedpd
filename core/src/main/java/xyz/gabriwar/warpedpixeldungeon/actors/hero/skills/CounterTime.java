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


import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class CounterTime extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "D3";
		name = "Counter-Time";
		image = 139;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//an enemy that steps back out of your reach gets cut as it goes: 20% / 35% / 50%
	@Override
	public void onCharMoved( Char ch, int from, boolean travelling ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || ch == null || ch == hero || !travelling || !ch.isAlive()
				|| ch.alignment != Char.Alignment.ENEMY || hero.paralysed > 0
				|| !Dungeon.level.adjacent( from, hero.pos ) || Dungeon.level.adjacent( ch.pos, hero.pos )) return;
		if (Random.Int( 100 ) >= 5 + 15 * level) return;
		final Char fleeing = ch;
		SkillInteractions.defer( () -> strike( hero, fleeing ) );
	}

	private void strike( Hero hero, Char enemy ){
		if (!enemy.isAlive() || !hero.isAlive()) return;
		enemy.damage( Math.max( 1, Math.round( weaponRoll() * 0.5f ) ), this );
		Wound.hit( enemy );
		if (level >= MAX_LEVEL && enemy.isAlive()){
			Buff.prolong( enemy, Cripple.class, 2f );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1.0f );
		if (hero.sprite != null){
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "cast" ) );
			hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		}
	}

	//a raw roll of whatever is in hand. Deliberately not Hero.damageRoll(): that path runs the
	//whole skill tree's damage modifiers, so an active toggle like Lunge would spend mana and
	//yell its cast text, and one-shot buffs (PhysicalEmpower, moon fury) would be eaten - none
	//of which a passive counter is allowed to do.
	private static int weaponRoll(){
		Hero hero = Dungeon.hero;
		KindOfWeapon wep = hero.belongings.weapon();
		return wep != null ? wep.damageRoll( hero ) : RingOfForce.damageRoll( hero );
	}
}
