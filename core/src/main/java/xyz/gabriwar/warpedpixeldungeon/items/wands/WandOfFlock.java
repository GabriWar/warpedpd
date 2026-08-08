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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class WandOfFlock extends Wand {

	{
		image = ItemSpriteSheet.WAND_FLOCK;

		collisionProperties = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID;
	}

	@Override
	public void onZap(Ballistica bolt) {

		//no conjuring sheep on the deep journal/sokoban floors — the puzzles are made
		//of sheep, so summoning extra ones lets you cheese them
		if (Dungeon.depth > 50) {
			GLog.w(Messages.get(this, "no_flock"));
			return;
		}

		int level = buffedLvl();
		int n = level + 2;
		float lifespan = level + 3;

		int cell = bolt.collisionPos;
		if (Actor.findChar(cell) != null && bolt.dist > 1) {
			cell = bolt.path.get(bolt.dist - 1);
		}

		//the flock spreads outward as far as it needs to. Confined to NEIGHBOURS9 it could
		//never place more than 9 sheep, so from +8 (n = level + 2) the full flock was
		//arithmetically impossible and upgrading the wand stopped doing anything.
		boolean[] passable = BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null);
		for (Char ch : Actor.chars()) {
			passable[ch.pos] = false;
		}
		PathFinder.buildDistanceMap(cell, passable, n);

		int spawned = 0;
		for (int d = 0; d <= n && spawned < n; d++) {
			for (int c = 0; c < Dungeon.level.length() && spawned < n; c++) {
				if (PathFinder.distance[c] != d) continue;

				Sheep sheep = new Sheep();
				sheep.pos = c;
				GameScene.add(sheep);
				Dungeon.level.occupyCell(sheep);
				sheep.initialize(lifespan);

				CellEmitter.get(c).burst(Speck.factory(Speck.WOOL), 4);
				spawned++;
			}
		}

		wandProc(curUser, chargesPerCast());
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(
				curUser.sprite.parent,
				MagicMissile.WARD,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//no on-hit effect
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xFFFFFF);
		particle.am = 0.8f;
		particle.setLifespan(0.8f);
		particle.setSize(1f, 2f);
		particle.shuffleXY(0.5f);
	}
}
