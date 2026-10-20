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

package xyz.gabriwar.warpedpixeldungeon.levels.features;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BloomBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Nut;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SandalsOfNature;
import xyz.gabriwar.warpedpixeldungeon.items.food.Berry;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.PetrifiedSeed;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.VaultLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barkskin;
import com.watabou.utils.Random;

public class HighGrass {
	
	//prevents items dropped from grass, from trampling that same grass.
	//yes this is a bit ugly, oh well.
	private static boolean freezeTrample = false;

	//the hero cutting this grass with a sickle or a war scythe (Gathering.reap), for the
	//length of that one trample: a Reaper's scythe may double the seed it gives up.
	//Walking through grass is not a harvest and leaves it null
	public static Hero reaper = null;

	//what the grass gives up, in a heap on its cell; a heap no scene draws (a test's) has no sprite
	private static void drop( Level level, Item item, int pos ){
		Heap heap = level.drop( item, pos );
		if (heap.sprite != null) heap.sprite.drop();
	}

	public static void trample( Level level, int pos ) {
		
		if (freezeTrample) return;
		
		Char ch = Actor.findChar(pos);
		
		if (level.map[pos] == Terrain.FURROWED_GRASS){
			if (ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.HUNTRESS){
				//Do nothing
				freezeTrample = true;
			} else {
				Level.set(pos, Terrain.GRASS);
			}
			
		} else {
			if (ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.HUNTRESS){
				if(((Hero) ch).hasTalent(Talent.BARKSKIN)){
					Barkskin.conditionallyAppend(ch, (((Hero) ch).lvl* ((Hero) ch).pointsInTalent(Talent.BARKSKIN))/3, 1 );
				}
				Level.set(pos, Terrain.FURROWED_GRASS);
				freezeTrample = true;
			} else {
				Level.set(pos, Terrain.GRASS);
			}
			
			int naturalismLevel = 0;
			
			if (ch != null) {
				SandalsOfNature.Naturalism naturalism = ch.buff( SandalsOfNature.Naturalism.class );
				if (naturalism != null) {
					if (!naturalism.isCursed()) {
						naturalismLevel = naturalism.itemLevel() + 1;
						naturalism.charge();
					} else {
						naturalismLevel = -1;
					}
				}

				//berries try to drop on floors 2/3/4/6/7/8, to a max of 4/6
				if (ch instanceof Hero && ((Hero) ch).hasTalent(Talent.NATURES_BOUNTY)){
					int berriesAvailable = 2 + 2*((Hero) ch).pointsInTalent(Talent.NATURES_BOUNTY);

					Talent.NatureBerriesDropped dropped = Buff.affect(ch, Talent.NatureBerriesDropped.class);
					berriesAvailable -= dropped.count();

					if (berriesAvailable > 0) {
						int targetFloor = 2 + 2 * ((Hero) ch).pointsInTalent(Talent.NATURES_BOUNTY);
						targetFloor -= berriesAvailable;
						targetFloor += (targetFloor >= 5) ? 3 : 2;

						//If we're behind: 1/10, if we're on page: 1/30, if we're ahead: 1/90
						boolean droppingBerry = false;
						if (Dungeon.depth > targetFloor) droppingBerry = Random.Int(10) == 0;
						else if (Dungeon.depth == targetFloor) droppingBerry = Random.Int(30) == 0;
						else if (Dungeon.depth < targetFloor) droppingBerry = Random.Int(90) == 0;

						if (droppingBerry && Random.Int(4) == 0) {
							dropped.countUp(1);
							drop(level, new Berry(), pos);
						}
					}

				}
			}

			//grass gives 1/3 the normal amount of loot in fungi level
			if (Dungeon.level instanceof MiningLevel
					&& Blacksmith.Quest.Type() == Blacksmith.Quest.FUNGI
					&& Random.Int(3) != 0){
				naturalismLevel = -1;
			}
			
			if (naturalismLevel >= 0) {
				// Seed, scales from 1/25 to 1/9
				float lootChance = 1/(25f - naturalismLevel*4f);

				// absolute max drop rate is ~1/6.5 with footwear of nature, ~1/18 without
				lootChance *= PetrifiedSeed.grassLootMultiplier();

				//vault level spawns significantly fewer seeds from grass
				if (Dungeon.level instanceof VaultLevel){
					lootChance /= 3;
				}

				if (Random.Float() < lootChance) {
					if (Random.Float() < PetrifiedSeed.stoneInsteadOfSeedChance()) {
						drop(level, Generator.randomUsingDefaults(Generator.Category.STONE), pos);
					} else if (Random.Float() < 0.22f) {
						//Sprouted kept the Nut inside the seed pool at weight 48 of ~214, making
						//it the most common thing grass ever dropped. It can't go back in that
						//category here - SEED is declared as Plant.Seed and a dozen call sites
						//hard-cast the result, so a Nut (a Food) would throw - so take its share
						//of the same roll instead.
						if (Random.Int(4) == 0) {
							drop(level, new Nut(), pos);
						}
					} else {
						Item seed = Generator.random(Generator.Category.SEED);
						//elixir of bloom doubles the seeds trampled grass gives up
						if (ch != null && ch.buff(BloomBuff.class) != null) seed.quantity(seed.quantity() * 2);
						//and a Reaper's scythe may cut a second one loose
						if (reaper != null && Quality.secondSeed(reaper, pos)) seed.quantity(seed.quantity() * 2);
						drop(level, seed, pos);
					}
				}
				
				// Sprouted: universal berry drops from grass, decaying chance
				if (Random.Int(40) - Dungeon.LimitedDrops.BERRIES.count >= 0
						&& Random.Int(160) == 0) {
					Dungeon.LimitedDrops.BERRIES.count++;
					drop(level, new Berry(), pos);
				}

				// Dew, scales from 1/6 to 1/4
				lootChance = 1/(6f -naturalismLevel/2f);

				//grassy levels spawn half as much dew
				if (Dungeon.level != null && Dungeon.level.feeling == Level.Feeling.GRASS){
					lootChance /= 2;
				}

				if (Random.Float() < lootChance) {
					drop(level, new Dewdrop(), pos);
				}
			}

			if (ch != null) {
				Camouflage.activate(ch, ch.glyphLevel(Camouflage.class));
			}
			
		}
		
		freezeTrample = false;
		
		if (WarpedPixelDungeon.scene() instanceof GameScene) {
			GameScene.updateMap(pos);
			
			//two leaves; the blades flung and the grass parting are the render side's, as it sees
			//the high grass go (NatureHolyFx: effects/fx/nature/Trample)
			CellEmitter.get(pos).burst(LeafParticle.LEVEL_SPECIFIC, 2);
			if (Dungeon.level.heroFOV[pos]) Dungeon.observe();
		}
	}
}
