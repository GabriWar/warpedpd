/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Dewcatcher;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.plants.Sorrowmoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Starflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Stormvine;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Swiftthistle;
import xyz.gabriwar.warpedpixeldungeon.sprites.FarmCropSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A seed sown on tilled soil in the safe zone. It ripens on its own game-clock
 * (growing even while the hero is off on other floors), and on each finished
 * cycle either withers or drops a fresh seed of its kind on an adjacent tile,
 * up to a per-species cap. A sickle harvests a guaranteed seed from a ripe
 * crop - but kills the plant.
 */
public class FarmCrop extends Mob {

	{
		spriteClass = FarmCropSprite.class;

		HP = HT = 1;
		defenseSkill = 0;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
	}

	//per-species farming profile: {growth turns per cycle, max seeds, wither % per cycle}
	//growth is measured on the global game clock, so slow crops ripen across many floors
	private static final HashMap<Class<? extends Plant>, int[]> PROFILES = new HashMap<>();
	private static final int[] DEFAULT_PROFILE = {500, 3, 20};
	static {
		//the crown jewel: agonizingly slow and very likely to die, but clones rotberries
		PROFILES.put(Rotberry.class,    new int[]{6000, 2, 65});
		//rare, hugely valuable (any potion): slow and fickle
		PROFILES.put(Starflower.class,  new int[]{3500, 2, 50});
		//uncommon, potent: notably slow
		PROFILES.put(BlandfruitBush.class, new int[]{2500, 2, 40});
		PROFILES.put(Fadeleaf.class,    new int[]{1500, 2, 30});
		PROFILES.put(Dewcatcher.class,  new int[]{1200, 2, 25});
		PROFILES.put(Phaseshift.class,  new int[]{1200, 2, 30});
		PROFILES.put(Sungrass.class,    new int[]{ 900, 3, 20});
		//common combat seeds: fast, generous, hardy
		PROFILES.put(Firebloom.class,   new int[]{ 500, 4, 15});
		PROFILES.put(Icecap.class,      new int[]{ 500, 4, 15});
		PROFILES.put(Sorrowmoss.class,  new int[]{ 500, 4, 15});
		PROFILES.put(Swiftthistle.class,new int[]{ 500, 4, 15});
		PROFILES.put(Stormvine.class,   new int[]{ 500, 4, 15});
	}

	public static int[] profileFor(Class<? extends Plant> plantCls){
		int[] p = PROFILES.get(plantCls);
		return p != null ? p : DEFAULT_PROFILE;
	}

	//moisture-loving plants: adjacent water speeds their growth and helps them
	//survive each cycle. surround the plot with the builder's tool for a boost
	private static final java.util.HashSet<Class<? extends Plant>> WATER_LOVERS = new java.util.HashSet<>();
	static {
		WATER_LOVERS.add(Rotberry.class);     //the whole point: water makes cloning it feasible
		WATER_LOVERS.add(Dewcatcher.class);
		WATER_LOVERS.add(Sungrass.class);
		WATER_LOVERS.add(Icecap.class);
		WATER_LOVERS.add(BlandfruitBush.class);
		WATER_LOVERS.add(Sorrowmoss.class);
	}

	public static boolean lovesWater(Class<? extends Plant> plantCls){
		return WATER_LOVERS.contains(plantCls);
	}

	public Plant.Seed seed = null;
	//the game-clock time this cycle started growing
	public float cycleStart = 0;
	public int seedsGiven = 0;

	//the true continuous game clock: Statistics.duration only folds in elapsed
	//time when fixTime() runs (saves/level switches), so the live component
	//sits in Actor.now(). using both makes growth tick in real time
	public static float clock(){
		return Statistics.duration + Actor.now();
	}

	public void sow( Plant.Seed seed ){
		this.seed = seed;
		this.cycleStart = clock();
	}

	private int[] profile(){
		return seed != null ? profileFor(seed.getPlantClass()) : DEFAULT_PROFILE;
	}

	//number of adjacent water tiles (0-4) that count toward a water-loving crop
	public int adjacentWater(){
		if (seed == null || !lovesWater(seed.getPlantClass())) return 0;
		int n = 0;
		for (int d : PathFinder.NEIGHBOURS8){
			int c = pos + d;
			if (c >= 0 && c < Dungeon.level.length()
					&& Dungeon.level.map[c] == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.WATER){
				n++;
			}
		}
		return Math.min(4, n);
	}

	//water-loving crops grow up to 40% faster with 4 adjacent water tiles
	private int effectiveGrowth(){
		int base = profile()[0];
		return Math.round( base * (1f - 0.10f * adjacentWater()) );
	}

	//and shed up to 40 percentage points off their wither chance
	private int effectiveWither(){
		return Math.max( 5, profile()[2] - 10 * adjacentWater() );
	}

	//how far along the current cycle is, on the global clock
	private float elapsed(){
		return Math.max(0, clock() - cycleStart);
	}

	//whole turns until this cycle completes
	public int turnsLeft(){
		return Math.max(0, (int)Math.ceil(effectiveGrowth() - elapsed()));
	}

	public boolean mature(){
		return elapsed() >= effectiveGrowth();
	}

	//plant food: advances a quarter of the REMAINING growth. no cap, but the
	//returns diminish sharply - each application does less than the last, so
	//near maturity a dose is almost nothing. returns false when it can't help
	public boolean fertilize(){
		if (seed == null || mature()) return false;
		float remaining = effectiveGrowth() - elapsed();
		float add = remaining * 0.25f;
		if (add < 1f) return false;
		cycleStart -= add;
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(LeafParticle.GENERAL, 5);
		}
		return true;
	}

	public int growthPercent(){
		return Math.min(100, (int)(100 * elapsed() / effectiveGrowth()));
	}

	//the plant art frame, for the sprite
	public int plantImage(){
		if (seed != null){
			Plant p = Reflection.newInstance(seed.getPlantClass());
			if (p != null) return p.image;
		}
		return 0;
	}

	@Override
	public String name() {
		return seed != null
				? Messages.get(this, "name", seed.name())
				: Messages.get(this, "name_generic");
	}

	@Override
	public String description() {
		int[] p = profile();
		String desc = Messages.get(this, "desc", growthPercent(), turnsLeft(), seedsGiven, p[1], effectiveWither());
		if (seed != null && lovesWater(seed.getPlantClass())){
			int w = adjacentWater();
			desc += "\n\n" + (w > 0
					? Messages.get(this, "water_active", w)
					: Messages.get(this, "water_hint"));
		}
		return desc;
	}

	@Override
	protected boolean act() {

		if (seed == null){
			destroy();
			if (sprite != null) sprite.killAndErase();
			return true;
		}

		int[] p = profile();

		//catch up on every full cycle that elapsed since the last act (including
		//time spent away on other floors, since the clock is global). the growth
		//and wither values are recomputed each cycle so live water changes apply
		while (elapsed() >= effectiveGrowth() && seed != null){

			cycleStart += effectiveGrowth();

			if (Random.Int(100) < effectiveWither()){
				//the plant gives out and withers with nothing to show
				wither(true);
				return true;
			}

			//a fresh seed drops on a free neighbouring cell
			ArrayList<Integer> spots = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				int c = pos + n;
				if (c >= 0 && c < Dungeon.level.length()
						&& Dungeon.level.passable[c] && Actor.findChar(c) == null
						&& Dungeon.level.heaps.get(c) == null){
					spots.add(c);
				}
			}
			int spot = spots.isEmpty() ? pos : Random.element(spots);
			Plant.Seed drop = (Plant.Seed) Reflection.newInstance(seed.getClass());
			Dungeon.level.drop(drop, spot).sprite.drop();
			seedsGiven++;

			if (Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(ShaftParticle.FACTORY, 4);
				GLog.p(Messages.get(this, "yield", seed.name()));
			}

			if (seedsGiven >= p[1]){
				//spent: the plant withers after its last seed
				wither(false);
				return true;
			}
		}

		spend(TICK);
		return true;
	}

	private void wither(boolean fruitless){
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(LeafParticle.GENERAL, 6);
			GLog.w(Messages.get(this, fruitless ? "wither" : "spent", seed != null ? seed.name() : ""));
		}
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	@Override
	public void damage(int dmg, Object src) {
		//rough handling destroys the crop
		wither(true);
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero) return true;

		//a crop whose seed class no longer exists (removed/renamed content) just clears
		if (seed == null){
			destroy();
			if (sprite != null) sprite.killAndErase();
			return true;
		}

		if (mature() && Dungeon.hero.belongings.getItem(Sickle.class) != null){
			//sickle harvest: one guaranteed seed, but it kills the plant
			Plant.Seed harvest = (Plant.Seed) Reflection.newInstance(seed.getClass());
			if (!harvest.collect()){
				Dungeon.level.drop(harvest, pos).sprite.drop();
			}
			GLog.p(Messages.get(this, "harvest", seed.name()));
			CellEmitter.get(pos).burst(LeafParticle.GENERAL, 8);
			destroy();
			if (sprite != null) sprite.killAndErase();
			Dungeon.hero.spendAndNext(1f);
		} else if (mature()){
			GLog.i(Messages.get(this, "ready_no_sickle"));
		} else {
			GLog.i(Messages.get(this, "progress", growthPercent(), turnsLeft()));
		}
		return true;
	}

	private static final String SEED   = "seed";
	private static final String START  = "cycle_start";
	private static final String GIVEN  = "seeds_given";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SEED, seed);
		bundle.put(START, cycleStart);
		bundle.put(GIVEN, seedsGiven);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		seed = (Plant.Seed) bundle.get(SEED);
		cycleStart = bundle.getFloat(START);
		seedsGiven = bundle.getInt(GIVEN);
	}
}
