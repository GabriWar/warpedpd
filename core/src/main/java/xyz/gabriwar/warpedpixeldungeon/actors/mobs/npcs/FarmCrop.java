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
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Crop;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.FarmRules;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
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
import com.watabou.utils.Reflection;

import java.util.HashMap;

/**
 * A crop growing in farmland (homestead/farming): on the world's slices the FarmSlice's record is
 * the crop, and this shows it and answers for it. It ripens on the global clock, at its season's
 * pace (Farming.tend), catching up on everything while the hero is away; ripe, it waits for the
 * sickle - food from the food plants, its own seed from the rest - and each cycle after drops a
 * seed beside it or withers, up to a per-species cap of seeds.
 */
public class FarmCrop extends Mob implements xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor {

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

	/** Its crop: on a world slice the slice's own record (homestead/farming/FarmSlice), which this
	 *  only shows and is touched for; anywhere else its own. */
	public Crop crop = new Crop();

	//the true continuous game clock: Statistics.duration only folds in elapsed
	//time when fixTime() runs (saves/level switches), so the live component
	//sits in Actor.now(). using both makes growth tick in real time
	public static float clock(){
		return Statistics.duration + Actor.now();
	}

	/** Sows a seed here, off the world's slices (on them, Farming.plant sows). */
	public void sow( Plant.Seed seed ){
		crop = new Crop( seed.getClass(), clock(), WorldClock.day() );
	}

	private Class<? extends Plant> plant(){
		return crop == null ? null : crop.plant();
	}

	//the water it has (0-4: homestead/farming/FarmRules.water)
	public int adjacentWater(){
		return Farming.waterAt( Dungeon.level, pos );
	}

	//the turns of growth its cycle takes, with its water
	private int cycleTurns(){
		Class<? extends Plant> p = plant();
		return p == null ? DEFAULT_PROFILE[0] : FarmRules.cycleTurns( p, adjacentWater() );
	}

	//whole turns until it ripens at today's pace; -1 while it is not growing (winter in the open)
	public int turnsLeft(){
		float rate = Farming.rateAt( Dungeon.level, pos );
		if (rate <= 0f) return -1;
		return Math.max( 0, (int) Math.ceil( (cycleTurns() - crop.growth) / rate ) );
	}

	/** Ripe: it waits for the sickle. */
	public boolean mature(){
		return crop != null && crop.ripe();
	}

	//plant food: a quarter of the growth left in its cycle, so each dose does less than the last;
	//false when it would not help (ripe, or almost)
	public boolean fertilize(){
		shown = -1f;
		return crop != null && Farming.feed( Dungeon.level, pos, crop );
	}

	public int growthPercent(){
		if (mature()) return 100;
		return Math.min( 99, (int) (100 * crop.growth / cycleTurns()) );
	}

	//how grown its sprite shows it (0..1), worked out again once after it acts or is fed: the
	//sprite asks every frame, and growthPercent looks at the water round it
	private float shown = -1f;

	public float grownShown(){
		if (shown < 0f) shown = mature() ? 1f : growthPercent() / 100f;
		return shown;
	}

	//the plant art frame, for the sprite
	public int plantImage(){
		Class<? extends Plant> p = plant();
		if (p != null){
			Plant pl = Reflection.newInstance( p );
			if (pl != null) return pl.image;
		}
		return 0;
	}

	@Override
	public String name() {
		return crop != null && crop.seed != null
				? Messages.get(this, "name", Farming.name( crop ))
				: Messages.get(this, "name_generic");
	}

	@Override
	public String description() {
		Class<? extends Plant> p = plant();
		if (p == null) return Messages.get(this, "name_generic");
		int water = adjacentWater();
		StringBuilder desc = new StringBuilder();
		if (mature()){
			desc.append( Messages.get(this, "desc_ripe", crop.seeds, FarmRules.maxSeeds( p ), FarmRules.witherChance( p, water )) );
		} else {
			int left = turnsLeft();
			desc.append( left < 0
					? Messages.get(this, "desc_dormant", growthPercent())
					: Messages.get(this, "desc", growthPercent(), left) );
		}
		if (FarmRules.food( p )) desc.append( "\n\n" ).append( Messages.get(this, "food") );
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			desc.append( "\n\n" ).append( Farming.greenhouse( Dungeon.level, pos )
					? Messages.get(this, "glass")
					: Messages.get(this, "season", Messages.titleCase( GameCalendar.season().name().toLowerCase( java.util.Locale.ENGLISH ) ),
							Math.round( 100 * Farming.rateAt( Dungeon.level, pos ) )) );
		}
		desc.append( "\n\n" ).append( water > 0
				? Messages.get(this, lovesWater( p ) ? "water_active" : "water_some", water)
				: Messages.get(this, lovesWater( p ) ? "water_hint" : "water_none") );
		if (crop.fed > 0) desc.append( "\n\n" ).append( Messages.get(this, "fed") );
		return desc.toString();
	}

	@Override
	protected boolean act() {
		Level level = Dungeon.level;
		if (crop == null || plant() == null){
			vanish();
			return true;
		}
		//on a world slice: the crop of the record on its cell, or a stray leaving
		if (level instanceof OverworldLevel && !Farming.owns( (OverworldLevel) level, this )){
			vanish();
			return true;
		}
		if (!Farming.waiting( level, crop )){
			shown = -1f;
			Farming.Outcome o = Farming.tend( level, pos, crop, true );
			if (o != Farming.Outcome.GROWING){
				Farming.dies( level, pos, crop, o, true );
				if (isAlive() && level.mobs.contains( this )) vanish();
				return true;
			}
		}
		spend(TICK);
		return true;
	}

	//gone without a word: a stray, or a crop its record no longer holds
	private void vanish(){
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	@Override
	public void damage(int dmg, Object src) {
		//rough handling destroys the crop
		Farming.dies( Dungeon.level, pos, crop, Farming.Outcome.WITHERED, true );
		if (Dungeon.level.mobs.contains( this )) vanish();
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
		if (c != Dungeon.hero){
			//the fields are the host's to reap in a shared game
			if (c instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero){
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) c,
						Messages.get(this, "host_only") );
			}
			return true;
		}

		//a crop whose seed class no longer exists (removed/renamed content) just clears
		if (plant() == null){
			vanish();
			return true;
		}

		if (mature() && Farming.hasSickle( Dungeon.hero )){
			Farming.harvest( Dungeon.hero, this );
		} else if (mature()){
			GLog.i(Messages.get(this, "ready_no_sickle"));
		} else {
			int left = turnsLeft();
			GLog.i(left < 0 ? Messages.get(this, "progress_dormant", growthPercent())
					: Messages.get(this, "progress", growthPercent(), left));
		}
		return true;
	}

	private static final String CROP   = "crop";
	//a save from before the fields of the world: the old crop's own fields
	private static final String SEED   = "seed";
	private static final String START  = "cycle_start";
	private static final String GIVEN  = "seeds_given";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CROP, crop);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(CROP)){
			crop = (Crop) bundle.get(CROP);
		} else {
			//an old crop: what had grown of its cycle carries over, and one that had given a seed is ripe
			Plant.Seed seed = (Plant.Seed) bundle.get(SEED);
			float now = clock();
			crop = new Crop( seed == null ? null : seed.getClass(), now, WorldClock.day() );
			crop.growth = Math.max( 0, now - bundle.getFloat(START) );
			crop.seeds = bundle.getInt(GIVEN);
			crop.cycles = crop.seeds > 0 ? 1 : 0;
		}
		if (crop == null) crop = new Crop();
	}
}
