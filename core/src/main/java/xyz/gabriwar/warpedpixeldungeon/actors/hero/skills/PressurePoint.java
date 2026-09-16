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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class PressurePoint extends SubSkill1 {

	private static final float LOCKED_MULTIPLIER = 1.5f;

	{
		name = "Pressure Point";
		image = 189;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}


	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || enemy == null || !enemy.isAlive()){
			return damage;
		}
		//+3: a body already locked up takes the follow-through in full
		if (level == MAX_LEVEL && enemy.buff( Paralysis.class ) != null){
			if (enemy.sprite != null){
				new Flare( 5, 18 ).color( 0xFFDD66, true ).show( enemy.sprite, 0.4f );
			}
			Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 1.3f );
			return Math.round( damage * LOCKED_MULTIPLIER );
		}
		//10% / 15% / 20%
		if (!Char.hasProp( enemy, Char.Property.BOSS ) && Random.Int( 100 ) < 5 + 5 * level){
			SkillInteractions.affectAfterHit( enemy, Paralysis.class, 1f );
			if (enemy.sprite != null){
				enemy.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
				enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "cast" ) );
			}
			Sample.INSTANCE.play( Assets.Sounds.HIT_CRUSH, 1f, 1.5f );
		}
		return damage;
	}
}
