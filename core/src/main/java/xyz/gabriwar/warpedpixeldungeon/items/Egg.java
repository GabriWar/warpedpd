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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.BlueDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Fairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.GreenDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.PET;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.RedDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Scorpion;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Spider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.SugarplumFairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Velocirooster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.VioletDragon;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Calendar;

public class Egg extends Item {

	public static final float TIME_TO_USE = 1;

	public static final String AC_BREAK = "BREAK";
	public static final String AC_SHAKE = "SHAKE";

	// Hatching thresholds from Sprouted
	public static final int RED_DRAGON = 30;
	public static final int GREEN_DRAGON = 5;
	public static final int BLUE_DRAGON = Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER ? 1 : 5;
	public static final int VIOLET_DRAGON = 5;
	public static final int SPIDER = 2000;
	public static final int SCORPION = 10000;
	public static final int VELOCIROOSTER = 1;
	public static final int FAIRY = 10;

	{
		image = ItemSpriteSheet.PET_EGG;
		unique = true;
		stackable = false;
	}

	public int startMoves = 0;
	public int moves = 0;
	public int burns = 0;
	public int freezes = 0;
	public int poisons = 0;
	public int lits = 0;
	public int summons = 0;

	//what the egg was subjected to while lying on the floor; decides what hatches.
	//Heap calls this from its lit()/summon()/poison() hooks, fire and frost go through
	//Heap.burn()/freeze() directly.
	public enum Incubation {
		LIT, SUMMON, POISON
	}

	public void incubate( Incubation type ) {
		switch (type) {
			case LIT:    lits++;    break;
			case SUMMON: summons++; break;
			case POISON: poisons++; break;
		}
	}

	private static final String STARTMOVES = "startMoves";
	private static final String MOVES = "moves";
	private static final String BURNS = "burns";
	private static final String FREEZES = "freezes";
	private static final String POISONS = "poisons";
	private static final String LITS = "lits";
	private static final String SUMMONS = "summons";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STARTMOVES, startMoves);
		bundle.put(MOVES, moves);
		bundle.put(BURNS, burns);
		bundle.put(FREEZES, freezes);
		bundle.put(POISONS, poisons);
		bundle.put(LITS, lits);
		bundle.put(SUMMONS, summons);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		startMoves = bundle.getInt(STARTMOVES);
		moves = bundle.getInt(MOVES);
		burns = bundle.getInt(BURNS);
		freezes = bundle.getInt(FREEZES);
		poisons = bundle.getInt(POISONS);
		lits = bundle.getInt(LITS);
		summons = bundle.getInt(SUMMONS);
	}

	public int checkMoves() { return moves; }
	public int checkBurns() { return burns; }
	public int checkFreezes() { return freezes; }
	public int checkPoisons() { return poisons; }
	public int checkLits() { return lits; }
	public int checkSummons() { return summons; }

	@Override
	public boolean doPickUp(Hero hero, int pos) {

		GLog.w(Messages.get(this, "warm"));

		Egg egg = hero.belongings.getItem(Egg.class);
		if (egg != null) {
			GLog.w(Messages.get(this, "onlyone"));
		}

		return super.doPickUp(hero, pos);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_BREAK);
		actions.add(AC_SHAKE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_BREAK)) {

			//"matched" = a development threshold was reached (egg is ripe), NOT
			//"a pet spawned". eggHatch itself keeps the egg + says "notready" when
			//it can't hatch right now (haspet / no free cell), so only a truly
			//undeveloped egg falls through to the destructive yolk branch.
			boolean matched = false;
			boolean isDecember = Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER;

			if (checkSummons() >= FAIRY) {
				matched = true;
				eggHatch(isDecember ? new SugarplumFairy() : new Fairy());
			} else if (checkFreezes() >= BLUE_DRAGON) {
				matched = true;
				eggHatch(new BlueDragon());
			} else if (checkPoisons() >= VIOLET_DRAGON) {
				matched = true;
				eggHatch(new VioletDragon());
			} else if (checkLits() >= GREEN_DRAGON) {
				matched = true;
				eggHatch(new GreenDragon());
			} else if (checkBurns() >= RED_DRAGON) {
				matched = true;
				eggHatch(new RedDragon());
			} else if (checkBurns() >= VELOCIROOSTER) {
				matched = true;
				eggHatch(new Velocirooster());
			} else if (checkMoves() >= SCORPION) {
				matched = true;
				eggHatch(new Scorpion());
			} else if (checkMoves() >= SPIDER) {
				matched = true;
				eggHatch(new Spider());
			}

			if (!matched) {
				detach(Dungeon.hero.belongings.backpack);
				GLog.w(Messages.get(this, "yolk"));
			}

			hero.next();

		} else if (action.equals(AC_SHAKE)) {

			boolean alive = false;

			if (checkSummons() >= FAIRY) {
				GLog.w(Messages.get(this, "zap"));
				CellEmitter.center(Dungeon.hero.pos).burst(SparkParticle.FACTORY, 3);
				Dungeon.hero.sprite.flash();
				Dungeon.hero.damage(1, new Electricity());
				alive = true;
			} else if (checkFreezes() >= BLUE_DRAGON) {
				GLog.w(Messages.get(this, "kicks"));
				alive = true;
			} else if (checkPoisons() >= VIOLET_DRAGON) {
				GLog.w(Messages.get(this, "kicks"));
				alive = true;
			} else if (checkLits() >= GREEN_DRAGON) {
				GLog.w(Messages.get(this, "kicks"));
				alive = true;
			} else if (checkBurns() >= RED_DRAGON) {
				GLog.w(Messages.get(this, "kicks"));
				alive = true;
			} else if (checkBurns() >= VELOCIROOSTER) {
				GLog.w(Messages.get(this, "scratch"));
				alive = true;
			} else if (checkMoves() >= SCORPION) {
				GLog.w(Messages.get(this, "slithers"));
				alive = true;
			} else if (checkMoves() >= SPIDER) {
				GLog.w(Messages.get(this, "slithers"));
				alive = true;
			}

			if (!alive) {
				GLog.w(Messages.get(this, "slosh"));
			}
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

	public void eggHatch(PET pet) {
		int spawnPos = getSpawnPos();
		if (spawnPos != -1 && !Dungeon.hero.haspet) {
			pet.spawn(1);
			pet.HP = pet.HT;
			pet.pos = spawnPos;
			pet.state = pet.HUNTING;

			GameScene.add(pet);
			Actor.addDelayed(new Pushing(pet, Dungeon.hero.pos, spawnPos), -1f);

			pet.sprite.alpha(0);
			pet.sprite.parent.add(new AlphaTweener(pet.sprite, 1, 0.15f));

			detach(Dungeon.hero.belongings.backpack);
			GLog.w(Messages.get(this, "hatch"));
			Dungeon.hero.haspet = true;
			pet.syncToHero();
		} else {
			//ripe but can't hatch now (already have a pet / no free tile) —
			//keep the egg, don't destroy it.
			Dungeon.hero.spend(Egg.TIME_TO_USE);
			GLog.w(Messages.get(this, "notready"));
		}
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
