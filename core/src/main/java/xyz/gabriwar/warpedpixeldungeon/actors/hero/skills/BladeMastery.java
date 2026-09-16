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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class BladeMastery extends PassiveSkillB3 {

	private static final float READ_TURNS = 10f;

	{
		name = "Blade Mastery";
		image = 95;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//every enemy you strike is read: a small blade mark shows it, and its blows land less often
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || ranged || enemy == null || !enemy.isAlive()) return damage;
		read( enemy );
		//+3: reading one enemy reads the ones beside it
		if (level >= MAX_LEVEL){
			for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
				Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( enemy.pos + n );
				if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY) read( ch );
			}
		}
		return damage;
	}

	private void read( Char ch ){
		boolean fresh = ch.buff( Read.class ) == null;
		Buff.prolong( ch, Read.class, READ_TURNS );
		if (fresh && ch.sprite != null){
			ch.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "break" ) );
			ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
		}
	}

	//a read enemy's blows land less often: 10% / 20% / 30%
	@Override
	public boolean dodgeChance( Char attacker ){
		return level > 0 && attacker != null && attacker.buff( Read.class ) != null
				&& Random.Int( 100 ) < 10 * level;
	}

	@Override
	public void onDodge( Char attacker ){
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero = xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;
		if (hero != null && hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 0.8f, 1.4f );
	}

	/** the Duelist has read this enemy's form */
	public static class Read extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff {
		{
			type = buffType.NEGATIVE;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
	}
}
