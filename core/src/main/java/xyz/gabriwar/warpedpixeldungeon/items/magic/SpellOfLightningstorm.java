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

package xyz.gabriwar.warpedpixeldungeon.items.magic;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public class SpellOfLightningstorm extends ManaSpell {

	{
		mpCost = 1;
		selfCast = false;
		magicType = MagicFamily.LIT;
		spellNum = 21;
	}

	private int dmgDiv = 1;
	private float jmpChnce = 1.0f;

	private ArrayList<Char> affected = new ArrayList<>();
	private ArrayList<Lightning.Arc> arcs = new ArrayList<>();

	private void hit(Char ch, int damage) {
		if (damage < 1) {
			return;
		}

		if (ch == Dungeon.hero) {
			Camera.main.shake(2, 0.3f);
		}

		affected.add(ch);

		if (checkFam(curUser)) {
			damage *= 4;
		}

		if (Dungeon.level.water[ch.pos] && !ch.flying) {
			damage *= 2;
		}

		ch.damage(damage, this);

		ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
		ch.sprite.flash();

		// Build arc from previous position to this target
		if (affected.size() > 1) {
			arcs.add(new Lightning.Arc(
					DungeonTilemap.raisedTileCenterToWorld(affected.get(affected.size() - 2).pos),
					DungeonTilemap.raisedTileCenterToWorld(ch.pos)));
		}

		// Chain to ONE random neighbor
		HashSet<Char> ns = new HashSet<>();
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			Char n = Actor.findChar(ch.pos + PathFinder.NEIGHBOURS8[i]);
			if (n != null && !affected.contains(n) && Random.Float() < jmpChnce) {
				ns.add(n);
			}
		}

		if (ns.size() > 0) {
			hit(Random.element(ns), Random.Int(damage / dmgDiv, damage));
		}
	}

	@Override
	protected void onZap(int cell) {
		// Post-damage logic only; damage is done in fx()
	}

	@Override
	protected void fx(int cell, Callback callback) {
		affected.clear();
		arcs.clear();

		Char ch = Actor.findChar(cell);
		if (ch != null) {
			int lvl = spellLevel();
			int damage = Random.Int(lvl / 2, lvl);

			// Arc from caster to first target
			arcs.add(new Lightning.Arc(
					DungeonTilemap.raisedTileCenterToWorld(curUser.pos),
					DungeonTilemap.raisedTileCenterToWorld(cell)));

			hit(ch, damage);
		} else {
			// No target - just show arc to empty cell
			arcs.add(new Lightning.Arc(
					DungeonTilemap.raisedTileCenterToWorld(curUser.pos),
					DungeonTilemap.raisedTileCenterToWorld(cell)));
			CellEmitter.center(cell).burst(SparkParticle.FACTORY, 3);
		}

		curUser.sprite.parent.add(new Lightning(arcs, callback));
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	@Override
	protected int magicDivisor() {
		return 3;
	}
}
