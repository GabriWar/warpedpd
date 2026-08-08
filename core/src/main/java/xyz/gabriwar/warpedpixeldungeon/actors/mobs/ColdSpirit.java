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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ColdSpiritSprite;
import com.watabou.utils.Random;

//ported from Remixed PD's Ice Caves. Fast, flying, heavily armoured, and it leaves
//freezing air where it strikes.
public class ColdSpirit extends Mob {

	{
		spriteClass = ColdSpiritSprite.class;

		HP = HT = 50;
		defenseSkill = 16;

		baseSpeed = 1.3f;
		flying = true;

		EXP = 8;
		maxLvl = 20;

		properties.add( Property.ICY );
		properties.add( Property.UNDEAD );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 12, 15 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 22;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 22 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (Random.Int( 4 ) == 0) {
			GameScene.add( Blob.seed( enemy.pos, 10, Freezing.class ) );
		}

		return damage;
	}
}
