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


import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AccuracyMark;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Accuracy extends PassiveSkillB1 {

	{
		name = "Accuracy";
		image = 81;
		tier = 1;
	}

	public static final float BLIND_TURNS = 3f;

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a ranged hit can find the eyes: a bullseye blinds the target for a few turns.
	//Fully trained, the first ranged hit on each enemy is always a bullseye
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || level <= 0 || enemy == null || !enemy.isAlive()) return damage;
		boolean firstHit = enemy.buff( AccuracyMark.class ) == null;
		if (firstHit) Buff.affect( enemy, AccuracyMark.class );
		boolean bullseye = (level >= MAX_LEVEL && firstHit) || Random.Float() < 0.1f * level;
		if (!bullseye) return damage;

		Buff.prolong( enemy, Blindness.class, BLIND_TURNS );
		if (enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]){
			new Flare( 4, 16 ).color( 0xFF4444, true ).show( enemy.sprite, 0.5f );
			enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "bullseye" ) );
		}
		SpatialSound.play( Assets.Sounds.HIT_ARROW, enemy, 1f, 1.4f );
		return damage;
	}
}
