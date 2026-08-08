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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo2;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo2;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTransmutation;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class WaterOfTransmutation extends WellWater {

	@Override
	protected boolean affectHero(Hero hero) {
		return false;
	}

	@Override
	protected Item affectItem(Item item, int pos) {
		//Sprouted: the well is the Mr Destructo recharge/upgrade bench.
		//Inactive -> Active, Active -> mk2, Inactive mk2 -> Active mk2.
		//(Without this the Inactive units are dead ends.)
		if (item instanceof InactiveMrDestructo) {
			return new ActiveMrDestructo();
		} else if (item instanceof InactiveMrDestructo2) {
			return new ActiveMrDestructo2();
		} else if (item instanceof ActiveMrDestructo) {
			return new ActiveMrDestructo2();
		}

		//Sprouted's only real source of might. Its generator weight is 0, and the exotic
		//mapping is a closed loop - Might makes AdrenalineSurge and AdrenalineSurge makes
		//Might - so without this neither can ever be obtained, and Potion.allKnown() can
		//never come true, which locks the potion catalog badge out.
		if (item instanceof PotionOfStrength) {
			return new PotionOfMight();
		}

		Item result = ScrollOfTransmutation.changeItem(item);
		return result;
	}

	@Override
	public Notes.Landmark landmark() {
		return Notes.Landmark.WELL_OF_TRANSMUTATION;
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.start(Speck.factory(Speck.CHANGE), 0.2f, 0);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
