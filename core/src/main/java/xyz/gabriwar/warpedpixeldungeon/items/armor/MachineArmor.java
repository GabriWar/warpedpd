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

package xyz.gabriwar.warpedpixeldungeon.items.armor;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

//a machine, not a simple armor: massive defense,
//but its bulk makes dodging much harder
public class MachineArmor extends Armor {

	{
		image = ItemSpriteSheet.MACHINE_ARMOR;
	}

	public MachineArmor() {
		super( 5 );
	}

	@Override
	public int DRMax(int lvl) {
		return super.DRMax(lvl) + 5;
	}

	@Override
	public float evasionFactor(Char owner, float evasion) {
		return super.evasionFactor(owner, evasion) * 0.75f;
	}

	//a running machine idles warm, and its sealed bulk barely exchanges heat
	@Override
	public float thermalOffset() {
		return super.thermalOffset() + 1.0f;
	}

	@Override
	public float thermalMass() {
		return 0.35f;
	}
}
