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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.sprites.GrayWolfSprite;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import com.watabou.utils.Random;

//ported from Unleashed PD (stat model converted from UL fields to SPD method overrides)
public class GrayWolf extends Mob {

	{
		spriteClass = GrayWolfSprite.class;

		HP = HT = 110;
		defenseSkill = 26;

		EXP = 12;
		maxLvl = 29;
		loot = MysteryMeat.class;
		lootChance = 0.2f / 4f;

		//built for the cold: the deep freeze is home, the thaw is what hurts
		thermal = Thermal.COLD_DWELLER;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 20, 35 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 38;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 18 );
	}

	//a pack out on a hunt (HuntPack) runs its quarry down, feeds at the kill, and turns on
	//the hero only when he crowds it or draws blood; every other wolf chooses as before
	@Override
	protected Char chooseEnemy() {
		HuntPack pack = buff( HuntPack.class );
		if (pack != null && pack.steer( this )) return pack.choice;
		return super.chooseEnemy();
	}

	//blood drawn by the hero's side turns the whole pack on him
	@Override
	public void damage( int dmg, Object src ) {
		HuntPack pack = buff( HuntPack.class );
		if (pack != null && HuntPack.byHero( src )) pack.provoked();
		super.damage( dmg, src );
	}

	//UL pack behaviour: waking one wolf calls the rest of the pack
	@Override
	protected boolean act() {
		boolean justAlerted = alerted;
		boolean result = super.act();

		if (justAlerted && !Dungeon.level.mobs.isEmpty()) {
			for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
				if ((mob instanceof BrownWolf || mob instanceof GrayWolf) && mob != this) {
					mob.beckon( target );
				}
			}
		}

		return result;
	}
}
