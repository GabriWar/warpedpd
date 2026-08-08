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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.StenchGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.GreyRatSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class GreyRat extends Mob {

	private static final float SPAWN_DELAY = 2f;

	{
		spriteClass = GreyRatSprite.class;

		HP = HT = 9 + (Dungeon.depth * Random.NormalIntRange(1, 3));
		defenseSkill = 3 + (Math.round(Dungeon.depth / 2));

		EXP = 2;

		loot = new MonsterMeat();
		lootChance = 0.05f;

immunities.add( Amok.class );
		immunities.add( Sleep.class );
		immunities.add( Terror.class );
		immunities.add( Burning.class );
		immunities.add( Vertigo.class );
		immunities.add( Poison.class );
		immunities.add( ToxicGas.class );
		immunities.add( StenchGas.class );
		immunities.add( Doom.class );

		resistances.add( Grim.class );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 2, 5 + Dungeon.depth );
	}

	@Override
	public int attackSkill( Char target ) {
		return 5 + Dungeon.depth;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 2 );
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	public static void spawnAround( int pos ) {
		for (int n : PathFinder.NEIGHBOURS4) {
			int cell = pos + n;
			if (Dungeon.level.passable[cell] && Actor.findChar( cell ) == null) {
				spawnAt( cell );
			}
		}
	}

	public static GreyRat spawnAt( int pos ) {
		GreyRat r = new GreyRat();
		r.pos = pos;
		r.state = r.HUNTING;
		GameScene.add( r, SPAWN_DELAY );
		return r;
	}
}
