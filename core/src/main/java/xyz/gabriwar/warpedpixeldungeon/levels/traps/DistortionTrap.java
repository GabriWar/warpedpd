/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.levels.traps;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Acidic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Albino;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredBrute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CausticSlime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM201;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MobSpawner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Senior;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RatKing;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

public class DistortionTrap extends Trap{

	private static final float DELAY = 2f;

	{
		color = TEAL;
		shape = LARGE_DOT;
	}

	@Override
	public void activate() {

		int nMobs = 3;
		if (Random.Int( 2 ) == 0) {
			nMobs++;
			if (Random.Int( 2 ) == 0) {
				nMobs++;
			}
		}

		ArrayList<Integer> candidates = new ArrayList<>();

		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int p = pos + PathFinder.NEIGHBOURS8[i];
			if (Actor.findChar( p ) == null && (Dungeon.level.passable[p] || Dungeon.level.avoid[p])) {
				candidates.add( p );
			}
		}

		ArrayList<Integer> respawnPoints = new ArrayList<>();

		while (nMobs > 0 && candidates.size() > 0) {
			int index = Random.index( candidates );

			respawnPoints.add( candidates.remove( index ) );
			nMobs--;
		}

		ArrayList<Mob> mobs = new ArrayList<>();

		int summoned = 0;
		for (Integer point : respawnPoints) {
			summoned++;
			Mob mob;
			switch (summoned){
				case 1:
					if (Dungeon.depth != 5 && Random.Int(100) == 0){
						mob = new RatKing();
						break;
					}
				case 3: case 5 : default:
					int floor;
					do {
						floor = Random.Int(25);
					} while( Dungeon.bossLevel(floor));
					mob = Reflection.newInstance(MobSpawner.getMobRotation(floor).get(0));
					break;
				case 2:
					switch (Random.Int(4)){
						case 0: default:
							Wraith.spawnAt(point);
							continue; //wraiths spawn themselves, no need to do more
						case 1:
							//yes it's intended that these are likely to die right away
							mob = Piranha.random();
							break;
						case 2:
							mob = Mimic.spawnAt(point, false);
							((Mimic)mob).stopHiding();
							mob.alignment = Char.Alignment.ENEMY;
							break;
						case 3:
							mob = Statue.random(false);
							break;
					}
					break;
				case 4:
					mob = Reflection.newInstance(Random.element(MobSpawner.RARE_ALTS.values()));
					break;
			}

			if (Char.hasProp(mob, Char.Property.LARGE) && !Dungeon.level.openSpace[point]){
				continue;
			}

			mob.maxLvl = Hero.MAX_LEVEL-1;
			if (mob.state != mob.PASSIVE) {
				mob.state = mob.WANDERING;
			}
			mob.pos = point;
			GameScene.add(mob, DELAY);
			mobs.add(mob);
		}

		//important to process the visuals and pressing of cells last, so spawned mobs have a chance to occupy cells first
		Trap t;
		for (Mob mob : mobs){
			//manually trigger traps first to avoid sfx spam
			if ((t = Dungeon.level.traps.get(mob.pos)) != null && t.active){
				if (t.disarmedByActivation) t.disarm();
				t.reveal();
				Bestiary.setSeen(t.getClass());
				Bestiary.countEncounter(t.getClass());
				t.activate();
			}
			ScrollOfTeleportation.appear(mob, mob.pos);
			Dungeon.level.occupyCell(mob);
		}

	}
}
