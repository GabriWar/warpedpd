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

package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Awareness;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Spectacles;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BerryRegeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class Blueberry extends Food {

	{
		image = ItemSpriteSheet.BLUEBERRY_FOOD;
		energy = (Hunger.STARVING - Hunger.HUNGRY) / 10f;
		bones = false;
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		//the deepest floors ward off scrying (Sprouted): below depth 50 the berry
		//only maps if the hero can already see magically (Spectacles' MagicSight).
		boolean noticed = false;
		if (Dungeon.depth <= 50 || hero.buff(Spectacles.MagicSight.class) != null) {
			noticed = revealMap();
			GLog.p( Messages.get(this, "knowledge") );
			Buff.affect(hero, Awareness.class, 10f);
			Dungeon.observe();
		} else {
			GLog.w( Messages.get(this, "preventing") );
		}

		if (noticed) {
			Sample.INSTANCE.play(Assets.Sounds.SECRET);
		}

		if (Random.Float() >= 0.75f) {
			GLog.p( Messages.get(this, "energy") );
			Buff.affect(hero, BerryRegeneration.class).set(hero.HT * 2);
		}
	}

	private boolean revealMap() {
		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean[] mapped = Dungeon.level.mapped;
		boolean[] discoverable = Dungeon.level.discoverable;

		boolean noticed = false;

		for (int i = 0; i < length; i++) {
			int terr = map[i];

			if (discoverable[i]) {
				mapped[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {
					Dungeon.level.discover(i);

					if (Dungeon.level.heroFOV[i]) {
						GameScene.discoverTile(i, terr);
						CellEmitter.get(i).start(Speck.factory(Speck.DISCOVER), 0.1f, 4);
						noticed = true;
					}
				}
			}
		}

		Dungeon.observe();
		return noticed;
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}
