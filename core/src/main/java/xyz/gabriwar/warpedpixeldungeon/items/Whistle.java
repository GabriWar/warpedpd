/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 Dachhack
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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.BlueDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Fairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.GreenDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.PET;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.RedDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Scorpion;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.ShadowDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Spider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.SugarplumFairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Velocirooster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.VioletDragon;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Whistle extends Item {

	public static final float TIME_TO_USE = 1;

	public static final String AC_CALL = "CALL";

	{
		image = ItemSpriteSheet.WHISTLE_ITEM;
		unique = true;
		stackable = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_CALL);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_CALL)) {
			petCall();
			GLog.w(Messages.get(this, "tweet"));
		}
	}

	public int getSpawnPos() {
		int newPos = -1;
		int pos = Dungeon.hero.pos;
		ArrayList<Integer> candidates = new ArrayList<>();

		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) {
				candidates.add(c);
			}
		}

		newPos = candidates.size() > 0 ? Random.element(candidates) : -1;

		return newPos;
	}

	public boolean petCall() {

		boolean callResult = false;
		int spawnPos = getSpawnPos();

		if (spawnPos != -1 && Dungeon.hero.haspet) {

			// Destroy any existing pet on the level first
			PET existing = checkPet();
			if (existing != null) {
				existing.destroy();
				existing.sprite.killAndErase();
			}

			PET pet = createPetByType(Dungeon.hero.petType);
			if (pet != null) {
				spawnPet(pet, spawnPos, Dungeon.hero.pos);
				callResult = true;
				GLog.w(Messages.get(this, "arrive"));
			}

			Dungeon.hero.spend(Whistle.TIME_TO_USE);
		} else {
			Dungeon.hero.spend(Whistle.TIME_TO_USE);
		}

		return callResult;
	}

	private PET checkPet() {
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof PET) {
				return (PET) mob;
			}
		}
		return null;
	}

	private PET createPetByType(int type) {
		switch (type) {
			case 1:  return new Spider();
			case 2:  return new Bee();
			case 3:  return new Velocirooster();
			case 4:  return new RedDragon();
			case 5:  return new GreenDragon();
			case 6:  return new VioletDragon();
			case 7:  return new BlueDragon();
			case 8:  return new Scorpion();
			case 9:  return new Bunny();
			case 10: return new Fairy();
			case 11: return new SugarplumFairy();
			case 12: return new ShadowDragon();
			default: return null;
		}
	}

	private void spawnPet(PET pet, int petPos, int heroPos) {
		pet.spawn(Dungeon.hero.petLevel);
		pet.HP = Dungeon.hero.petHP;
		pet.pos = petPos;
		pet.state = pet.HUNTING;
		pet.kills = Dungeon.hero.petKills;
		pet.experience = Dungeon.hero.petExperience;
		pet.cooldown = Dungeon.hero.petCooldown;

		GameScene.add(pet);
		Actor.addDelayed(new Pushing(pet, heroPos, petPos), -1f);
	}

	@Override
	public int value() {
		return 500 * quantity;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}
}
