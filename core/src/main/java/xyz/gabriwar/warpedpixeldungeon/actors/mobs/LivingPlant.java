/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PlantMobSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.HashSet;

public class LivingPlant extends Mob {

	{
		spriteClass = PlantMobSprite.class; // fallback for AttackIndicator; sprite() overrides with correct id

		HP = HT = 8;
		defenseSkill = 2;

		EXP = 0;

		properties.add(Property.PLANT);

		immunities.add(Corruption.class);
	}

	public Plant plantClass;
	private boolean becomePlant = false;

	public void setBecomePlant() {
		becomePlant = true;
	}

	public LivingPlant() {
		super();
		HP = HT = 8 + Dungeon.depth * 2;
		defenseSkill = 2 + Dungeon.depth * 2;
	}

	private static final String PLANTCLASS = "plantclass";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(PLANTCLASS, plantClass);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		plantClass = (Plant) bundle.get(PLANTCLASS);
	}

	@Override
	public CharSprite sprite() {
		int id = plantClass != null ? (plantClass.livingPlantImage >= 0 ? plantClass.livingPlantImage : plantClass.image) : 0;
		return new PlantMobSprite( id );
	}

	public LivingPlant setPlantClass(Plant plant) {
		this.plantClass = plant;
		if (plant.immunity() != null) {
			immunities.add(plant.immunity());
		}
		return this;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(Dungeon.depth, Dungeon.depth + 3);
	}

	@Override
	public int attackSkill(Char target) {
		return Dungeon.depth;
	}

	@Override
	public int drRoll() {
		return Random.NormalIntRange(0, Dungeon.depth);
	}

	// Colour of this plant's poison particles, for burst effects; -1 if unknown
	private int poisonColor() {
		if (plantClass != null && plantClass.seedClass() != null) {
			Plant.Seed seed = Reflection.newInstance(plantClass.seedClass());
			if (seed != null && seed.poisonEmitterClass() != null) {
				return seed.poisonEmitterClass().getColor();
			}
		}
		return -1;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		if (!enemy.flying && Random.Float() <= 0.3f && plantClass != null) {
			int color = poisonColor();
			if (color != -1 && enemy.sprite != null) {
				enemy.sprite.burst(color, damage);
			}
			plantClass.attackProc(enemy, damage);
		}
		return super.attackProc(enemy, damage);
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		int color = poisonColor();
		if (color != -1 && sprite != null) {
			sprite.burst(color, dmg);
		}
	}

	@Override
	public void die(Object cause) {
		if (plantClass instanceof BlandfruitBush) {
			Dungeon.level.drop(new Blandfruit(), pos);
		}
		int color = poisonColor();
		if (color != -1 && sprite != null) {
			sprite.burst(color, 10);
		}
		super.die(cause);
	}

	@Override
	protected void dropExtraLoot() {
		if (plantClass != null && plantClass.seedClass() != null) {
			float seedChance = buff(Roots.class) != null ? 1f : 0.75f;
			if (Random.Float() < seedChance) {
				Plant.Seed seed = Reflection.newInstance(plantClass.seedClass());
				if (seed != null) {
					trackedDrop(seed, 0);
				}
			}
		}
	}

	@Override
	protected boolean act() {
		if (buff(Roots.class) != null || becomePlant) {
			goToSleep();
			return true;
		}
		return super.act();
	}

	private void goToSleep() {
		if (plantClass != null) {
			Plant.Seed seed = Reflection.newInstance(plantClass.seedClass());
			if (seed != null) {
				Plant plant = Dungeon.level.plant(seed, pos);
				if (plant != null) {
					plant.playerPlanted = false;
				}
			}
		}
		super.die(this);
	}

	@Override
	protected Char chooseEnemy() {
		//try to find a new enemy in these circumstances
		if (enemy == null || !enemy.isAlive() || state == WANDERING
				|| Dungeon.level.distance(enemy.pos, pos) > 2) {

			//find all mobs near the living plant
			HashSet<Char> enemies = new HashSet<>();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob != this
						&& Dungeon.level.distance(mob.pos, pos) <= 5
						&& mob.alignment != Alignment.NEUTRAL
						&& !(alignment == Alignment.ALLY && mob.alignment == Alignment.ALLY)) {
					enemies.add(mob);
				}
			}

			if (Dungeon.level.distance(Dungeon.hero.pos, pos) <= 5) {
				enemies.add(Dungeon.hero);
			}

			//if the hero cannot be found and there are no mobs to have as enemies the plant will go back to sleep
			if (enemies.isEmpty()) {
				goToSleep();
				return null;
			}

			Char mob = Random.element(enemies);
			if (mob != null) {
				if (Dungeon.level.distance(mob.pos, pos) >= Dungeon.level.distance(Dungeon.hero.pos, pos)) {
					if (alignment != Alignment.ALLY) {
						return Dungeon.hero;
					} else {
						return mob;
					}
				} else {
					return mob;
				}
			}
			return Dungeon.hero;
		}
		return enemy;
	}

	@Override
	public String name() {
		if (plantClass != null) {
			return "living " + plantClass.name();
		}
		return super.name();
	}

	@Override
	public String description() {
		String desc = super.description();
		if (plantClass != null) {
			desc += "\n\n" + plantClass.desc();
		}
		return desc;
	}
}
