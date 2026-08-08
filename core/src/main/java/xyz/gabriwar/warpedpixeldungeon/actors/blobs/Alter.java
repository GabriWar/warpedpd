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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.NornStone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.AresSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.CromCruachAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.LokisFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.NeptunusTrident;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Alter extends Blob {

	protected int pos;

	private static final int NORNSTONES_REQUIRED = 3;

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);

		// Empty blobs do not persist their arrays. This can happen when the
		// overworld altar has slid outside the active window.
		if (cur == null) return;

		for (int i = 0; i < cur.length; i++) {
			if (cur[i] > 0) {
				pos = i;
				break;
			}
		}
	}

	@Override
	protected void evolve() {
		volume = off[pos] = cur[pos];
	}

	@Override
	public void seed(xyz.gabriwar.warpedpixeldungeon.levels.Level level, int cell, int amount) {
		super.seed(level, cell, 0);
		cur[pos] = 0;
		pos = cell;
		volume = cur[pos] = amount;
	}

	public static void transmute(int cell) {
		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap != null) {
			Item result = consecrate(heap);
			if (result != null) {
				Dungeon.level.drop(result, cell).sprite.drop(cell);
			}
		}
	}

	private static Item consecrate(Heap heap) {
		CellEmitter.get(heap.pos).burst(Speck.factory(Speck.FORGE), 3);
		Splash.at(heap.pos, 0xFFFFFF, 3);

		int count = 0;
		int type = 0;

		for (Item item : heap.items) {
			if (item instanceof NornStone) {
				count += item.quantity();
				if (type == 0) {
					type = ((NornStone) item).type;
				} else if (Random.Int(3) < item.quantity()) {
					type = ((NornStone) item).type;
				}
			} else {
				count = 0;
				break;
			}
		}

		if (count >= NORNSTONES_REQUIRED) {
			CellEmitter.get(heap.pos).burst(Speck.factory(Speck.WOOL), 6);
			Sample.INSTANCE.play(Assets.Sounds.PUFF);

			heap.destroy();

			Weapon weapon;
			switch (type) {
				case 1:  weapon = new JupitersWraith();  weapon.enchant(new JupitersHorror()); break;
				case 2:  weapon = new AresSword();       weapon.enchant(new AresLeech());      break;
				case 3:  weapon = new CromCruachAxe();   weapon.enchant(new CromLuck());       break;
				case 4:  weapon = new LokisFlail();      weapon.enchant(new LokisPoison());    break;
				case 5:  weapon = new NeptunusTrident(); weapon.enchant(new NeptuneShock());   break;
				default: weapon = new AresSword();       weapon.enchant(new AresLeech());      break;
			}

			return weapon;
		} else {
			return null;
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.start(Speck.factory(Speck.LIGHT), 0.4f, 0);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
