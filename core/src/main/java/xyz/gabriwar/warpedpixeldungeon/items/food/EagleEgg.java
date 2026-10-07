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

package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * An egg from an eyrie on the mountains (levels/overworld/MountainSites): a good meal, if the
 * eagles let you have it. Taken from their nest, it sets every eagle guarding that nest on the
 * thief.
 */
public class EagleEgg extends Food {

	{
		image = ItemSpriteSheet.EAGLE_EGG;
		energy = Hunger.HUNGRY / 2f;
		bones = false;
	}

	@Override
	public boolean doPickUp( Hero hero, int pos ){
		if (Dungeon.level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			int wx = ow.worldX() + pos % ow.width(), wy = ow.worldY() + pos / ow.width();
			boolean robbed = false;
			for (Mob m : ow.mobs.toArray( new Mob[0] )){
				if (m instanceof Eagle && m.isAlive() && ((Eagle) m).guards( wx, wy )){
					((Eagle) m).enrage( hero );
					robbed = true;
				}
			}
			if (robbed) NetManager.heroLog( hero, GLog.WARNING + Messages.get( Eagle.class, "robbed" ) );
		}
		return super.doPickUp( hero, pos );
	}

	@Override
	public int value(){
		return 12 * quantity;
	}
}
