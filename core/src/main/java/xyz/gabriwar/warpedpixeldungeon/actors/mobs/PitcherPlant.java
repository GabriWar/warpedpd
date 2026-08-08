/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.PoisonGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.PitcherPlantVariant1Sprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PitcherPlantVariant2Sprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PitcherPlantVariant3Sprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class PitcherPlant extends Mob {

	private static final String LEVEL      = "level";
	private static final String VARIANT    = "variant";

	private int level;
	private int variant;

	{
		variant = Random.Int(1, 3);
		spriteClass = PitcherPlantVariant1Sprite.class;

		viewDistance = 3;

		EXP = 0;

		state = WANDERING;

		properties.add(Property.PLANT);
	}

	public void spawn( int level, int variant ) {
		if (variant == 0){
			this.variant = Random.Int(1, 3);
		}
		this.variant = variant;
		this.level = level;

		HT = HP = (2 + level) * 4;
		defenseSkill = 9 + level;
		switch (variant){
			case 1:
				spriteClass = PitcherPlantVariant1Sprite.class;
				break;
			case 2:
				spriteClass = PitcherPlantVariant2Sprite.class;
				break;
			case 3:
				spriteClass = PitcherPlantVariant3Sprite.class;
				break;
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEVEL, level );
		bundle.put( VARIANT, variant );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		spawn( bundle.getInt( LEVEL ), bundle.getInt( VARIANT ) );
	}

	@Override
	public int attackSkill( Char target ) {
		return defenseSkill;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( HT / 10, HT / 4 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (enemy instanceof Mob) {
			((Mob)enemy).aggro( this );
		}
		Buff.affect( enemy, Poison.class ).set( Random.Int(5, 5+damage) );
		return damage;
	}

	@Override
	public int defenseProc( Char enemy, int damage ) {

		GameScene.add(Blob.seed(pos, 20, PoisonGas.class));

		return super.defenseProc(enemy, damage);
	}
}
