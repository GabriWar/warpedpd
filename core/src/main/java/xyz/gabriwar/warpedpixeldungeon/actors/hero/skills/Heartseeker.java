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

import com.watabou.noosa.audio.Sample;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeartseekerArrow;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

/**
 * Huntress: a ranged hit that leaves an enemy under half its health lodges an arrow in
 * its heart. A heart pulses over it for three beats and then bursts, for a share of its
 * full health that grows each level. Fully trained, a burst that kills tears the arrow
 * free into the nearest wounded enemy and starts that heart pulsing too.
 */
public class Heartseeker extends Skill {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "PB4";
		name = "Heartseeker";
		castText = "Through the heart.";
		image = 131;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || level <= 0 || enemy == null || !enemy.isAlive()
				|| enemy.alignment != Char.Alignment.ENEMY || enemy.buff( HeartseekerArrow.class ) != null)
			return damage;
		int left = enemy.HP - damage;
		if (left <= 0 || left * 2 > enemy.HT)
			return damage;

		Buff.affect( enemy, HeartseekerArrow.class ).set( level );
		castTextYell();
		if (enemy.sprite != null) enemy.sprite.emitter().burst( Speck.factory( Speck.HEART ), 3 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_ARROW, 1f, 0.8f );
		return damage;
	}
}
