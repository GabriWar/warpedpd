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

package xyz.gabriwar.warpedpixeldungeon.items.artifacts;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class RingOfDisintegration extends Artifact {

	{
		image = ItemSpriteSheet.ARTIFACT_DISINTEGRATION;

		levelCap = 10;

		charge = 0;
		partialCharge = 0;
		chargeCap = 100;

		defaultAction = AC_BLAST;
	}

	private int consumedpts = 0;

	public static final String AC_BLAST = "BLAST";
	public static final String AC_ADD = "ADD";

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && charge == chargeCap && !cursed)
			actions.add(AC_BLAST);
		if (isEquipped(hero) && level() < levelCap && !cursed)
			actions.add(AC_ADD);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_BLAST)) {

			if (!isEquipped(hero))
				GLog.i(Messages.get(this, "not_equipped"));
			else if (charge != chargeCap)
				GLog.i(Messages.get(this, "not_charged"));
			else {
				blast(hero.pos);
				charge = 0;
				updateQuickslot();
				GLog.p(Messages.get(this, "blast"));
			}

		} else if (action.equals(AC_ADD)) {
			GameScene.selectItem(itemSelector);
		}
	}

	private int distance() {
		return level() + 2;
	}

	public void blast(int cell) {
		int[] neighbours;
		if (level() < 5) {
			neighbours = PathFinder.NEIGHBOURS4;
		} else if (level() < 10) {
			neighbours = PathFinder.NEIGHBOURS8;
		} else {
			//the top tier fires at the whole 5x5 ring. levelCap is 10, so this is precisely
			//the payoff for maxing the ring - and it was unreachable, making the last five
			//levels worth nothing.
			int w = Dungeon.level.width();
			neighbours = new int[]{
					-2*w-2, -2*w-1, -2*w, -2*w+1, -2*w+2,
					  -w-2,   -w-1,   -w,   -w+1,   -w+2,
					    -2,     -1,           1,       2,
					   w-2,    w-1,    w,    w+1,    w+2,
					 2*w-2,  2*w-1,  2*w,  2*w+1,  2*w+2 };
		}

		for (int n : neighbours) {
			int c = cell + n;
			discharge(c);
		}
	}

	private void discharge(int cell) {
		Ballistica bolt = new Ballistica(curUser.pos, cell, Ballistica.MAGIC_BOLT);
		int hitcell = bolt.collisionPos;

		// Visual effect
		curUser.sprite.parent.add(new Beam.DeathRay(
				curUser.sprite.center(),
				DungeonTilemap.raisedTileCenterToWorld(hitcell)));

		zap(bolt);
	}

	private void zap(Ballistica bolt) {
		boolean terrainAffected = false;

		int maxDistance = distance();
		int dist = Math.min(bolt.dist, maxDistance);

		ArrayList<Char> chars = new ArrayList<>();

		for (int i = 1; i <= dist; i++) {
			if (i >= bolt.path.size()) break;

			int c = bolt.path.get(i);

			Char ch;
			if ((ch = Actor.findChar(c)) != null) {
				chars.add(ch);
			}

			int terr = Dungeon.level.map[c];
			if (terr == Terrain.DOOR || terr == Terrain.BARRICADE) {
				Dungeon.level.set(c, Terrain.EMBERS);
				GameScene.updateMap(c);
				terrainAffected = true;
			} else if (terr == Terrain.HIGH_GRASS) {
				Dungeon.level.set(c, Terrain.GRASS);
				GameScene.updateMap(c);
				terrainAffected = true;
			}

			CellEmitter.center(c).burst(PurpleParticle.BURST, Random.IntRange(1, 2));
		}

		if (terrainAffected) {
			Dungeon.observe();
		}

		int lvl = level() + chars.size();
		int dmgMin = lvl * lvl;
		int dmgMax = 10 + lvl * lvl * 3;
		for (Char ch : chars) {
			ch.damage(Random.NormalIntRange(dmgMin, dmgMax), this);
			ch.sprite.centerEmitter().burst(PurpleParticle.BURST, Random.IntRange(1, 2));
			ch.sprite.flash();
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new DisintegrationRecharge();
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		if (isEquipped(Dungeon.hero)) {
			desc += "\n\n";
			if (charge < chargeCap)
				desc += Messages.get(this, "desc_restrained");
			else
				desc += Messages.get(this, "desc_ready");
		}
		return desc;
	}

	public class DisintegrationRecharge extends ArtifactBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap) {
				partialCharge += 1 + (level() * level());
				if (partialCharge >= 10) {
					charge++;
					partialCharge -= 10;
					if (charge >= chargeCap) {
						charge = chargeCap;
						partialCharge = 0;
					}
				}
			} else {
				partialCharge = 0;
			}

			updateQuickslot();
			spend(TICK);
			return true;
		}
	}

	private static final String CONSUMED = "consumedpts";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CONSUMED, consumedpts);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		consumedpts = bundle.getInt(CONSUMED);
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(RingOfDisintegration.class, "prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Scroll && item.isIdentified();
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				Hero hero = Dungeon.hero;
				consumedpts += 10;

				hero.sprite.operate(hero.pos);
				hero.busy();
				hero.spend(2f);
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				hero.sprite.emitter().burst(ElmoParticle.FACTORY, 12);

				item.detach(hero.belongings.backpack);
				GLog.h(Messages.get(RingOfDisintegration.class, "consumed", consumedpts));

				int levelChk = ((level() * level() / 2) + 1) * 10;

				if (consumedpts > levelChk && level() < levelCap) {
					upgrade();
					GLog.p(Messages.get(RingOfDisintegration.class, "looks_better"));
				}
			}
		}
	};
}
