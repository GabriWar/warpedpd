/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2024 Gabriel Batista
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

package xyz.gabriwar.warpedpixeldungeon.items.potions.exotic;

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfWorm extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_WORM;
	}

	@Override
	public void apply(Hero hero) {
		identify();
		//blasting every enemy in sight is an attack; you don't get to stay hidden through it
		Invisibility.dispel();
		int count = 0;
		for (Mob mob : hero.getVisibleEnemies().toArray(new Mob[0])) {
			Ballistica shot = new Ballistica(hero.pos, mob.pos, Ballistica.STOP_SOLID);
			int maxDist = Math.min(hero.lvl * 5, shot.dist);
			int cell = shot.path.get(Math.min(shot.dist, maxDist));
			hero.sprite.parent.add(new Beam.DeathRay(hero.sprite.center(),
					DungeonTilemap.raisedTileCenterToWorld(cell)));

			ArrayList<Char> chars = new ArrayList<>();
			for (int c : shot.subPath(1, maxDist)) {
				Char ch = Actor.findChar(c);
				if (ch != null) chars.add(ch);
				CellEmitter.center(c).burst(LeafParticle.GENERAL, Random.IntRange(1, 2));
			}
			for (Char ch : chars) {
				if (!(ch instanceof NPC) && !ch.isImmune(this.getClass())) {
					ch.damage(hero.attackSkill(ch), this);
					ch.sprite.centerEmitter().burst(LeafParticle.GENERAL, Random.IntRange(1, 2));
					ch.sprite.flash();
				}
			}
			count++;
		}
		if (count > 0) GLog.i(Messages.get(this, "mobs_effected", count));
		else GLog.i(Messages.get(this, "no_targets"));
	}
}
