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
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobRed;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobViolet;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobYellow;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import com.watabou.utils.Random;

public class WaterOfUpgradeEating extends WellWater {

	@Override
	protected boolean affectHero(Hero hero) {
		return false;
	}

	@Override
	protected Item affectItem(Item item, int pos) {

		if (item.isUpgradable()) {
			return eatUpgradable(item);
		} else if (item instanceof Scroll || item instanceof Potion) {
			return eatStandard(item);
		} else {
			return null;
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.start(Speck.factory(Speck.CHANGE), 0.2f, 0);
	}

	private Item eatUpgradable(Item item) {
		int ups = item.level();

		if (Random.Float() < (ups / 10f)) {
			return new UpgradeBlobViolet();
		} else if (Random.Float() < (ups / 5f)) {
			return new UpgradeBlobRed();
		} else if (Random.Float() < (ups / 3f)) {
			return new UpgradeBlobYellow();
		} else {
			return (Plant.Seed) Generator.random(Generator.Category.SEED);
		}
	}

	private Item eatStandard(Item item) {
		if (Random.Float() < 0.1f) {
			return new UpgradeBlobYellow();
		} else {
			return (Plant.Seed) Generator.random(Generator.Category.SEED);
		}
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
