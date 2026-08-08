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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfDisintegration2 extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_DISINTEGRATION;

		collisionProperties = Ballistica.STOP_SOLID;
	}

	@Override
	public int min(int lvl) {
		return lvl;
	}

	@Override
	public int max(int lvl) {
		return 8 + lvl * lvl / 3;
	}

	@Override
	public void onZap(Ballistica bolt) {

		boolean terrainAffected = false;
		int level = buffedLvl();
		int maxDist = level + 4;
		int dist = Math.min(bolt.dist, maxDist);

		ArrayList<Char> chars = new ArrayList<>();

		for (int i = 1; i <= dist; i++) {
			int c = bolt.path.get(i);

			Char ch = Actor.findChar(c);
			if (ch != null) {
				chars.add(ch);
			}

			int terr = Dungeon.level.map[c];
			if (terr == Terrain.DOOR || terr == Terrain.BARRICADE) {
				Dungeon.level.set(c, Terrain.EMBERS);
				GameScene.updateMap(c);
				terrainAffected = true;
			} else if (terr == Terrain.HIGH_GRASS || terr == Terrain.FURROWED_GRASS) {
				Dungeon.level.set(c, Terrain.GRASS);
				GameScene.updateMap(c);
				terrainAffected = true;
			}

			CellEmitter.center(c).burst(PurpleParticle.BURST, Random.IntRange(1, 2));
		}

		if (terrainAffected) {
			Dungeon.observe();
		}

		int lvl = level + chars.size();
		for (Char ch : chars) {
			int dmg = Random.NormalIntRange(lvl, 8 + lvl * lvl / 3);
			ch.damage(dmg, this);
			ch.sprite.centerEmitter().burst(PurpleParticle.BURST, Random.IntRange(1, 2));
			ch.sprite.flash();
			wandProc(ch, chargesPerCast());
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		int cell = bolt.path.get(Math.min(bolt.dist, buffedLvl() + 4));
		curUser.sprite.parent.add(
				new Beam.DeathRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(cell)));
		callback.call();
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//no on-hit effect
	}

	@Override
	public String statsDesc() {
		int range = buffedLvl() + 4;
		if (levelKnown)
			return Messages.get(this, "stats_desc", range, min(), max());
		else
			return Messages.get(this, "stats_desc", range, min(0), max(0));
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x220022);
		particle.am = 0.6f;
		particle.setLifespan(0.6f);
		particle.acc.set(10, -10);
		particle.setSize(0.5f, 2f);
		particle.shuffleXY(1f);
	}
}
