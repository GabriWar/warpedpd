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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.RatSkull;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MobSpawner extends Actor {
	{
		actPriority = BUFF_PRIO; //as if it were a buff.
	}

	@Override
	protected boolean act() {

		if (Dungeon.level.mobCount() < Dungeon.level.mobLimit()) {

			if (Dungeon.level.spawnMob(12)){
				spend(Dungeon.level.respawnCooldown());
			} else {
				//try again in 1 turn
				spend(TICK);
			}

		} else {
			spend(Dungeon.level.respawnCooldown());
		}

		return true;
	}

	public void resetCooldown(){
		spend(-cooldown());
		spend(Dungeon.level.respawnCooldown());
	}

	public static ArrayList<Class<? extends Mob>> getMobRotation(int depth ){
		//the frozen branch has its own cold-dwelling roster rather than the Halls'
		if (Dungeon.branch == 5) {
			ArrayList<Class<? extends Mob>> nest = spiderNestRotation();
			Random.shuffle(nest);
			return nest;
		}
		if (Dungeon.branch == 2) {
			ArrayList<Class<? extends Mob>> frozen = frozenMobRotation();
			Random.shuffle(frozen);
			return frozen;
		}
		ArrayList<Class<? extends Mob>> mobs = standardMobRotation( depth );
		addRareMobs(depth, mobs);
		addNocturnalMobs(depth, mobs);
		swapMobAlts(mobs);
		Random.shuffle(mobs);
		return mobs;
	}

	//Spider Nest roster: the nest's own brood, thickening with depth
	private static ArrayList<Class<? extends Mob>> spiderNestRotation(){
		ArrayList<Class<? extends Mob>> mobs = new ArrayList<>(Arrays.asList(
				SpiderServant.class, SpiderServant.class, SpiderServant.class,
				SpiderGuard.class,
				SpiderExploding.class));
		if (Dungeon.depth >= 8) mobs.add(SpiderMind.class);
		if (Dungeon.depth >= 8) mobs.add(SpiderGuard.class);
		if (Dungeon.depth >= 9) mobs.add(SpiderExploding.class);
		return mobs;
	}

	//Frozen branch roster (Unleashed PD port): pack hunters and things built for the cold
	private static ArrayList<Class<? extends Mob>> frozenMobRotation(){
		return new ArrayList<>(Arrays.asList(
				BrownWolf.class, BrownWolf.class,
				GrayWolf.class, GrayWolf.class,
				Yeti.class,
				ClayGolem.class,
				ColdSpirit.class,
				KoboldIcemancer.class));
	}

	//returns a rotation of standard mobs, unshuffled.
	private static ArrayList<Class<? extends Mob>> standardMobRotation( int depth ){
		switch(depth){

			// Sewers
			case 1:
				//3x rat, 2x snake, 1x brown bat, 1x grey rat
				return new ArrayList<>(Arrays.asList(
						Rat.class, Rat.class, Rat.class,
						Snake.class, Snake.class,
						BrownBat.class,
						GreyRat.class));
			case 2:
				//2x rat, 2x snake, 2x gnoll, 1x brown bat
				return new ArrayList<>(Arrays.asList(Rat.class, Rat.class,
						Snake.class, Snake.class,
						Gnoll.class, Gnoll.class,
						BrownBat.class));
			case 3:
				//1x rat, 2x snake, 3x gnoll, 1x swarm, 1x crab, 1x brown bat, 1x grey rat
				return new ArrayList<>(Arrays.asList(Rat.class,
						Snake.class, Snake.class,
						Gnoll.class, Gnoll.class, Gnoll.class,
						Swarm.class,
						Crab.class,
						BrownBat.class,
						GreyRat.class,
						Velocirooster.class));
			case 4: case 5:
				//1x gnoll, 1x swarm, 2x crab, 2x slime
				return new ArrayList<>(Arrays.asList(Gnoll.class,
						Swarm.class,
						Crab.class, Crab.class,
						Slime.class, Slime.class,
						SlimeBrown.class,
						Velocirooster.class,
						SlimeRed.class));

			// Prison
			case 6:
				//3x skeleton, 1x thief, 1x swarm, 1x grey rat
				return new ArrayList<>(Arrays.asList(Skeleton.class, Skeleton.class, Skeleton.class,
						Thief.class,
						Swarm.class,
						GreyRat.class));
			case 7:
				//3x skeleton, 1x thief, 1x DM-100, 1x guard, 1x fossil skeleton
				return new ArrayList<>(Arrays.asList(Skeleton.class, Skeleton.class, Skeleton.class,
						Thief.class,
						DM100.class,
						Guard.class,
						FossilSkeleton.class,
						Zombie.class));
			case 8:
				//2x skeleton, 1x thief, 2x DM-100, 2x guard, 1x necromancer, 1x fossil skeleton
				return new ArrayList<>(Arrays.asList(Skeleton.class, Skeleton.class,
						Thief.class,
						DM100.class, DM100.class,
						Guard.class, Guard.class,
						Necromancer.class,
						FossilSkeleton.class,
						Zombie.class));
			case 9: case 10:
				//1x skeleton, 1x thief, 2x DM-100, 2x guard, 2x necromancer
				return new ArrayList<>(Arrays.asList(Skeleton.class,
						Thief.class,
						DM100.class, DM100.class,
						Guard.class, Guard.class,
						Necromancer.class, Necromancer.class,
						Zombie.class));

			// Caves
			case 11:
				//3x bat, 1x brute, 1x shaman
				return new ArrayList<>(Arrays.asList(
						Bat.class, Bat.class, Bat.class,
						Brute.class,
						Shaman.random()));
			case 12:
				//2x bat, 2x brute, 1x shaman, 1x spinner
				return new ArrayList<>(Arrays.asList(
						Bat.class, Bat.class,
						Brute.class, Brute.class,
						Shaman.random(),
						Spinner.class));
			case 13:
				//1x bat, 2x brute, 2x shaman, 2x spinner, 1x DM-200, 1x broken robot
				return new ArrayList<>(Arrays.asList(
						Bat.class,
						Brute.class, Brute.class,
						Shaman.random(), Shaman.random(),
						Spinner.class, Spinner.class,
						DM200.class,
						BrokenRobot.class));
			case 14:
				//1x bat, 1x brute, 2x shaman, 2x spinner, 2x DM-200, 1x broken robot
				return new ArrayList<>(Arrays.asList(
						Bat.class,
						Brute.class,
						Shaman.random(), Shaman.random(),
						Spinner.class, Spinner.class,
						DM200.class, DM200.class,
						BrokenRobot.class,
						ClayGolem.class));
			case 15:
				//1x bat, 1x brute, 2x shaman, 2x spinner, 2x DM-200
				return new ArrayList<>(Arrays.asList(
						Bat.class,
						Brute.class,
						Shaman.random(), Shaman.random(),
						Spinner.class, Spinner.class,
						DM200.class, DM200.class,
						ClayGolem.class));

			// City
			case 16:
				//3x ghoul, 1x elemental, 1x warlock
				return new ArrayList<>(Arrays.asList(
						Ghoul.class, Ghoul.class, Ghoul.class,
						Elemental.random(),
						Warlock.class,
						ClayGolem.class,
						SpiderBot.class,
						Tinkerer.class));
			case 17:
				//1x ghoul, 2x elemental, 1x warlock, 1x monk
				return new ArrayList<>(Arrays.asList(
						Ghoul.class,
						Elemental.random(), Elemental.random(),
						Warlock.class,
						Monk.class,
						ClayGolem.class,
						SpiderBot.class,
						Tinkerer.class));
			case 18:
				//1x ghoul, 1x elemental, 2x warlock, 2x monk, 1x golem
				return new ArrayList<>(Arrays.asList(
						Ghoul.class,
						Elemental.random(),
						Warlock.class, Warlock.class,
						Monk.class, Monk.class,
						Golem.class,
						Minotaur.class));
			case 19: case 20:
				//1x elemental, 2x warlock, 2x monk, 3x golem
				return new ArrayList<>(Arrays.asList(
						Elemental.random(),
						Warlock.class, Warlock.class,
						Monk.class, Monk.class,
						Golem.class, Golem.class, Golem.class,
						Minotaur.class));

			// Halls
			case 21:
				//2x succubus, 1x evil eye
				return new ArrayList<>(Arrays.asList(
						Succubus.class, Succubus.class,
						Eye.class,
						ChaosMage.class));
			case 22:
				//1x succubus, 1x evil eye
				return new ArrayList<>(Arrays.asList(
						Succubus.class,
						Eye.class,
						ChaosMage.class));
			case 23:
				//1x succubus, 2x evil eye, 1x scorpio
				return new ArrayList<>(Arrays.asList(
						Succubus.class,
						Eye.class, Eye.class,
						Scorpio.class,
						LostSoul.class));
			case 24:
				//1x succubus, 2x evil eye, 3x scorpio, 1x brown wolf, 1x gray wolf, 1x demon goo
				return new ArrayList<>(Arrays.asList(
						Succubus.class,
						Eye.class, Eye.class,
						Scorpio.class, Scorpio.class, Scorpio.class,
						DemonGoo.class,
						LostSoul.class));
			case 25: case 26:
				//1x succubus, 2x evil eye, 3x scorpio, 1x brown wolf, 1x gray wolf
				return new ArrayList<>(Arrays.asList(
						Succubus.class,
						Eye.class, Eye.class,
						Scorpio.class, Scorpio.class, Scorpio.class,
						BrownWolf.class,
						GrayWolf.class));

			// Sprouted post-game depths

			case 27: // Field
				//gnoll archers and forest protectors (from Sprouted)
				return new ArrayList<>(Arrays.asList(
						GnollArcher.class, GnollArcher.class, ForestProtector.class));
			case 28: // Battle
				//3x mossy skeleton
				return new ArrayList<>(Arrays.asList(
						MossySkeleton.class, MossySkeleton.class, MossySkeleton.class));
			case 29: // Fishing
				//3x albino piranha
				return new ArrayList<>(Arrays.asList(
						AlbinoPiranha.class, AlbinoPiranha.class, AlbinoPiranha.class));
			case 30: // Vault
				//3x gold thief
				return new ArrayList<>(Arrays.asList(
						GoldThief.class, GoldThief.class, GoldThief.class));
			case 31: // Catacomb
				//3x blue wraith
				return new ArrayList<>(Arrays.asList(
						BlueWraith.class, BlueWraith.class, BlueWraith.class));
			case 32: // Fortress
				//3x oni
				return new ArrayList<>(Arrays.asList(
						Oni.class, Oni.class, Oni.class));
			case 33: // Chasm
				//3x flying protector
				return new ArrayList<>(Arrays.asList(
						FlyingProtector.class, FlyingProtector.class, FlyingProtector.class));
			case 35: // InfestBoss
				//2x grey oni, 2x spectral rat (equal)
				return new ArrayList<>(Arrays.asList(
						GreyOni.class, GreyOni.class,
						SpectralRat.class, SpectralRat.class));
			case 36: // TenguDen (use Eye as stand-in)
				return new ArrayList<>(Arrays.asList(
						Eye.class, Eye.class, Eye.class));
			case 41: // ThiefCatch
				//3x bandit king
				return new ArrayList<>(Arrays.asList(
						BanditKing.class, BanditKing.class, BanditKing.class));
			case 56: case 57: case 58: case 59: case 60:
			case 61: case 62: case 63: case 64: case 65: // Mines
				//3x kupua
				return new ArrayList<>(Arrays.asList(
						Kupua.class, Kupua.class, Kupua.class));

			default:
				if (depth >= Dungeon.POSTGAME_DEPTH) {
					//default post-game: eye
					return new ArrayList<>(Arrays.asList(
							Eye.class, Eye.class, Eye.class));
				}
				//default pre-game: sewers
				return new ArrayList<>(Arrays.asList(
						Rat.class, Rat.class, Rat.class,
						Snake.class));
		}

	}

	//has a chance to add a rarely spawned mobs to the rotation
	public static void addRareMobs( int depth, ArrayList<Class<?extends Mob>> rotation ){

		switch (depth){

			// Sewers
			default:
				return;
			case 1:
				if (Random.Float() < 0.02f) rotation.add(RatBoss.class);
				return;
			case 4:
				if (Random.Float() < 0.025f) rotation.add(Thief.class);
				return;

			// Prison
			case 7: case 8:
				if (Random.Float() < 0.02f) rotation.add(Assassin.class);
				return;
			case 9:
				if (Random.Float() < 0.025f) rotation.add(Bat.class);
				return;

			// Caves
			case 12:
				if (Random.Float() < 0.02f) rotation.add(BrokenRobot.class);
				return;
			case 14:
				if (Random.Float() < 0.025f) rotation.add(Ghoul.class);
				return;

			// City
			case 18: case 19:
				if (Random.Float() < 0.05f) rotation.add(DwarfLich.class);
				if (depth == 19 && Random.Float() < 0.025f) rotation.add(Succubus.class);
				return;

			// Halls
			case 23:
				if (Random.Float() < 0.05f) rotation.add(DemonGoo.class);
				return;

			// Sprouted post-game protectors (~5% chance)
			case 27: // Field
				if (Random.Float() < 0.05f) rotation.add(ForestProtector.class);
				return;
			case 28: // Battle
				if (Random.Float() < 0.05f) rotation.add(GraveProtector.class);
				return;
			case 29: // Fishing
				if (Random.Float() < 0.05f) rotation.add(FishProtector.class);
				return;
			case 30: // Vault
				if (Random.Float() < 0.05f) rotation.add(VaultProtector.class);
				return;

			// Catacomb - DwarfLich (~10% chance)
			case 31:
				if (Random.Float() < 0.10f) rotation.add(DwarfLich.class);
				return;

			// Mines - Gullin (~10% chance)
			case 56: case 57: case 58: case 59: case 60:
			case 61: case 62: case 63: case 64: case 65:
				if (Random.Float() < 0.10f) rotation.add(Gullin.class);
				return;
		}
	}

	//adds nocturnal mobs to the rotation during full moon nights
	public static void addNocturnalMobs( int depth, ArrayList<Class<?extends Mob>> rotation ){
		DayNightCycle.Phase phase = DayNightCycle.phase();
		if (phase != DayNightCycle.Phase.NIGHT && phase != DayNightCycle.Phase.DUSK) {
			return;
		}
		if (!GameCalendar.isFullMoon()) {
			return;
		}

		boolean isNight = (phase == DayNightCycle.Phase.NIGHT);

		if (depth <= 5) {
			// Sewers: wraiths
			if (isNight) rotation.add(Wraith.class);
			if (isNight && Random.Float() < 0.33f) rotation.add(Wraith.class);
		} else if (depth <= 10) {
			// Prison: wraiths and spectral necromancers
			rotation.add(Wraith.class);
			if (isNight) rotation.add(SpectralNecromancer.class);
		} else if (depth <= 15) {
			// Caves: red wraiths
			rotation.add(RedWraith.class);
			if (isNight) rotation.add(RedWraith.class);
		} else if (depth <= 20) {
			// City: blue wraiths and dwarf liches
			rotation.add(BlueWraith.class);
			if (isNight) rotation.add(DwarfLich.class);
		} else if (depth < Dungeon.POSTGAME_DEPTH) {
			// Halls: blue wraiths and spectral rats
			rotation.add(BlueWraith.class);
			if (isNight) rotation.add(SpectralRat.class);
		} else if (depth >= Dungeon.POSTGAME_DEPTH) {
			// Post-game: spectral rats
			rotation.add(SpectralRat.class);
			if (isNight) rotation.add(SpectralRat.class);
		}
	}

	//switches out regular mobs for their alt versions when appropriate
	private static void swapMobAlts(ArrayList<Class<?extends Mob>> rotation) {
		float altChance = 1 / 50f * RatSkull.exoticChanceMultiplier();
		for (int i = 0; i < rotation.size(); i++) {
			if (Random.Float() < altChance) {
				Class<? extends Mob> cl = rotation.get(i);
				Class<? extends Mob> alt = RARE_ALTS.get(cl);
				if (alt != null) {
					rotation.set(i, alt);
				}
			}
		}
	}

	public static final HashMap<Class<?extends Mob>, Class<?extends Mob>> RARE_ALTS = new HashMap<>();
	static {
		RARE_ALTS.put(Rat.class,            Albino.class);
		RARE_ALTS.put(Gnoll.class,          GnollExile.class);
		RARE_ALTS.put(Crab.class,           HermitCrab.class);
		RARE_ALTS.put(Slime.class,          CausticSlime.class);

		RARE_ALTS.put(Thief.class,          Bandit.class);
		RARE_ALTS.put(Necromancer.class,    SpectralNecromancer.class);

		RARE_ALTS.put(Brute.class,          ArmoredBrute.class);
		RARE_ALTS.put(DM200.class,          DM201.class);

		RARE_ALTS.put(Monk.class,           Senior.class);
		//swapping to chaos elemental actually happens in Elemental.random
		RARE_ALTS.put(Elemental.class,      Elemental.ChaosElemental.class);

		RARE_ALTS.put(Scorpio.class,        Acidic.class);
	}
}
