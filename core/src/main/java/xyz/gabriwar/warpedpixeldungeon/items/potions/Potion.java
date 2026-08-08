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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.ItemStatusHandler;
import xyz.gabriwar.warpedpixeldungeon.items.Recipe;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.AquaBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCleansing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfHellstorm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfMagicFire;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfOrb;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfShroudingFog;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfSleepParalysis;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfSnapFreeze;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfSoil;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfSowing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfTsunami;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.Ballcrop;
import xyz.gabriwar.warpedpixeldungeon.plants.Bananabean;
import xyz.gabriwar.warpedpixeldungeon.plants.Blindweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Blueeyedsusan;
import xyz.gabriwar.warpedpixeldungeon.plants.Butterlion;
import xyz.gabriwar.warpedpixeldungeon.plants.Chandaliertail;
import xyz.gabriwar.warpedpixeldungeon.plants.Chillisnapper;
import xyz.gabriwar.warpedpixeldungeon.plants.Clitbalm;
import xyz.gabriwar.warpedpixeldungeon.plants.Clockcypress;
import xyz.gabriwar.warpedpixeldungeon.plants.Cocostuft;
import xyz.gabriwar.warpedpixeldungeon.plants.Combflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Crimsoncrown;
import xyz.gabriwar.warpedpixeldungeon.plants.Crimsonpepper;
import xyz.gabriwar.warpedpixeldungeon.plants.Dewcatcher;
import xyz.gabriwar.warpedpixeldungeon.plants.Dirtdaisy;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Eyeeuonymus;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Feelerfern;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Firefoxglove;
import xyz.gabriwar.warpedpixeldungeon.plants.Flowertree;
import xyz.gabriwar.warpedpixeldungeon.plants.Frostcorn;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Goograss;
import xyz.gabriwar.warpedpixeldungeon.plants.Grasslilly;
import xyz.gabriwar.warpedpixeldungeon.plants.Grassvine;
import xyz.gabriwar.warpedpixeldungeon.plants.Hypnohemp;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Kiwivetch;
import xyz.gabriwar.warpedpixeldungeon.plants.Larvaleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Lavenderlantern;
import xyz.gabriwar.warpedpixeldungeon.plants.Lightninglily;
import xyz.gabriwar.warpedpixeldungeon.plants.Mageroyal;
import xyz.gabriwar.warpedpixeldungeon.plants.Musclemoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Nightshadeonion;
import xyz.gabriwar.warpedpixeldungeon.plants.Parasiteshrub;
import xyz.gabriwar.warpedpixeldungeon.plants.Peanutpetal;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Poppoplar;
import xyz.gabriwar.warpedpixeldungeon.plants.Rose;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.plants.Shadowbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Snowhedge;
import xyz.gabriwar.warpedpixeldungeon.plants.Sorrowmoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Sourpitcher;
import xyz.gabriwar.warpedpixeldungeon.plants.Starflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Steamweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Stormvine;
import xyz.gabriwar.warpedpixeldungeon.plants.Sunbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Suncarnivore;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Swiftthistle;
import xyz.gabriwar.warpedpixeldungeon.plants.Tankcabbage;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.plants.Waterweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Willowcane;
import xyz.gabriwar.warpedpixeldungeon.plants.Witherfennel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class Potion extends Item {

	public static final String AC_DRINK = "DRINK";
	
	//used internally for potions that can be drunk or thrown
	public static final String AC_CHOOSE = "CHOOSE";

	private static final float TIME_TO_DRINK = 1f;

	private static final LinkedHashMap<String, Integer> colors = new LinkedHashMap<String, Integer>() {
		{
			put("crimson",ItemSpriteSheet.POTION_CRIMSON);
			put("amber",ItemSpriteSheet.POTION_AMBER);
			put("golden",ItemSpriteSheet.POTION_GOLDEN);
			put("jade",ItemSpriteSheet.POTION_JADE);
			put("turquoise",ItemSpriteSheet.POTION_TURQUOISE);
			put("azure",ItemSpriteSheet.POTION_AZURE);
			put("indigo",ItemSpriteSheet.POTION_INDIGO);
			put("magenta",ItemSpriteSheet.POTION_MAGENTA);
			put("bistre",ItemSpriteSheet.POTION_BISTRE);
			put("charcoal",ItemSpriteSheet.POTION_CHARCOAL);
			put("silver",ItemSpriteSheet.POTION_SILVER);
			put("ivory",ItemSpriteSheet.POTION_IVORY);
			put("poison_green",ItemSpriteSheet.POTION_POISON_GREEN);
			put("snow_white",ItemSpriteSheet.POTION_SNOW_WHITE);
			put("muddy_green",ItemSpriteSheet.POTION_MUDDY_GREEN);
			put("muddy_yellow",ItemSpriteSheet.POTION_MUDDY_YELLOW);
			put("honey",ItemSpriteSheet.POTION_HONEY);
			put("bloody",ItemSpriteSheet.POTION_BLOODY);
			put("orange",ItemSpriteSheet.POTION_ORANGE);
			put("violett",ItemSpriteSheet.POTION_VIOLETT);
			put("yellow",ItemSpriteSheet.POTION_YELLOW);
			put("white",ItemSpriteSheet.POTION_WHITE);
			put("brown",ItemSpriteSheet.POTION_BROWN);
			put("bright_blue",ItemSpriteSheet.POTION_BRIGHT_BLUE);
			put("rainbow",ItemSpriteSheet.POTION_RAINBOW);
			put("bright_orange",ItemSpriteSheet.POTION_BRIGHT_ORANGE);
			put("dark_blue",ItemSpriteSheet.POTION_DARK_BLUE);
			put("black",ItemSpriteSheet.POTION_BLACK);
			put("yellow_orange",ItemSpriteSheet.POTION_YELLOW_ORANGE);
			put("grass_green",ItemSpriteSheet.POTION_GRASS_GREEN);
			put("sky_blue",ItemSpriteSheet.POTION_SKY_BLUE);
			put("green_blue",ItemSpriteSheet.POTION_GREEN_BLUE);
			put("blue",ItemSpriteSheet.POTION_BLUE);
			put("flat_blue",ItemSpriteSheet.POTION_FLAT_BLUE);
			put("parasitic",ItemSpriteSheet.POTION_PARASITIC);
			put("maroon",ItemSpriteSheet.POTION_MAROON);
			put("punch",ItemSpriteSheet.POTION_PUNCH);
			put("beige",ItemSpriteSheet.POTION_BEIGE);
			put("scarlet",ItemSpriteSheet.POTION_SCARLET);
			put("water_blue",ItemSpriteSheet.POTION_WATER_BLUE);
			put("bright_green",ItemSpriteSheet.POTION_BRIGHT_GREEN);
			put("indigo_purple",ItemSpriteSheet.POTION_INDIGO_PURPLE);
			put("lime_green",ItemSpriteSheet.POTION_LIME_GREEN);
			put("rose",ItemSpriteSheet.POTION_ROSE);
			put("bright_purple",ItemSpriteSheet.POTION_BRIGHT_PURPLE);
			put("dark_rose",ItemSpriteSheet.POTION_DARK_ROSE);
			put("corn_yellow",ItemSpriteSheet.POTION_CORN_YELLOW);
			put("dark_purple",ItemSpriteSheet.POTION_DARK_PURPLE);
			put("lightning_blue",ItemSpriteSheet.POTION_LIGHTNING_BLUE);
			put("vine_red",ItemSpriteSheet.POTION_VINE_RED);
			put("pure_white",ItemSpriteSheet.POTION_PURE_WHITE);
			put("light_lime_green",ItemSpriteSheet.POTION_LIGHT_LIME_GREEN);
			put("pale_purple",ItemSpriteSheet.POTION_PALE_PURPLE);
			put("light_violett",ItemSpriteSheet.POTION_LIGHT_VIOLETT);
			put("black_white",ItemSpriteSheet.POTION_BLACK_WHITE);
			put("white_black",ItemSpriteSheet.POTION_WHITE_BLACK);
			put("pale_violett",ItemSpriteSheet.POTION_PALE_VIOLETT);
			put("brown_green",ItemSpriteSheet.POTION_BROWN_GREEN);
			put("dark_brown_green",ItemSpriteSheet.POTION_DARK_BROWN_GREEN);
			put("mossy_green",ItemSpriteSheet.POTION_MOSSY_GREEN);
			put("purple_violett",ItemSpriteSheet.POTION_PURPLE_VIOLETT);
			put("light_blue_green",ItemSpriteSheet.POTION_LIGHT_BLUE_GREEN);
			put("light_rose",ItemSpriteSheet.POTION_LIGHT_ROSE);
			put("yellow_purple",ItemSpriteSheet.POTION_YELLOW_PURPLE);
			put("teal",ItemSpriteSheet.POTION_TEAL);
		}
	};

	protected static final HashSet<Class<?extends Potion>> mustThrowPots = new HashSet<>();
	static{
		mustThrowPots.add(PotionOfToxicGas.class);
		mustThrowPots.add(PotionOfLiquidFlame.class);
		mustThrowPots.add(PotionOfParalyticGas.class);
		mustThrowPots.add(PotionOfFrost.class);
		
		//exotic
		mustThrowPots.add(PotionOfCorrosiveGas.class);
		mustThrowPots.add(PotionOfSnapFreeze.class);
		mustThrowPots.add(PotionOfShroudingFog.class);
		mustThrowPots.add(PotionOfStormClouds.class);
		//OvergrownPD throw-only AoE potions. The four with an apply() only centre the
		//blast on the drinker (self-immolation / self-freeze / self-infection), which
		//is not a real drink effect — OV flagged all of these must-throw.
		mustThrowPots.add(PotionOfHellstorm.class);
		mustThrowPots.add(PotionOfHypno.class);
		mustThrowPots.add(PotionOfHarvest.class);
		mustThrowPots.add(PotionOfHydrogenFire.class);
		mustThrowPots.add(PotionOfIceStorm.class);
		mustThrowPots.add(PotionOfInfection.class);
		//these define only shatter(), so the inherited apply() detonates them on the
		//drinker: a fire/acid/steam blob on your own cell, or a hostile MagicOrb and
		//SleepParalysisDemon spawned right on top of you.
		mustThrowPots.add(PotionOfMagicFire.class);
		mustThrowPots.add(PotionOfOrb.class);
		mustThrowPots.add(PotionOfSleepParalysis.class);
		mustThrowPots.add(PotionOfSoil.class);
		mustThrowPots.add(PotionOfSowing.class);
		mustThrowPots.add(PotionOfTsunami.class);
		mustThrowPots.add(PotionOfDigesting.class);
		mustThrowPots.add(PotionOfSlowness.class);
		mustThrowPots.add(PotionOfSmoke.class);
		mustThrowPots.add(PotionOfSteam.class);
		//these do write an apply(), but it turns the effect on the drinker: Water/Butter/Dirt
		//just call shatter(hero.pos), Withering applies its own NEGATIVE buff, Vine drops a
		//hostile VineLasher on your cell, Firestorm and Snowstorm wreck your body temperature
		//while surrounding you in fire or ice, and Ultraviolett blinds you.
		mustThrowPots.add(PotionOfWater.class);
		mustThrowPots.add(PotionOfButter.class);
		mustThrowPots.add(PotionOfDirt.class);
		mustThrowPots.add(PotionOfWithering.class);
		mustThrowPots.add(PotionOfVine.class);
		mustThrowPots.add(PotionOfFirestorm.class);
		mustThrowPots.add(PotionOfSnowstorm.class);
		mustThrowPots.add(PotionOfUltraviolett.class);
		//deliberately NOT here, despite the audit flagging them: Goo spawns an Alignment.ALLY
		//GooPlant, Sun's apply() is killUndead(), and Kiwi just grows grass. All fine to drink.

		//also all brews except unstable, hardcoded
	}
	
	protected static final HashSet<Class<?extends Potion>> canThrowPots = new HashSet<>();
	static{
		canThrowPots.add(PotionOfPurity.class);
		canThrowPots.add(PotionOfLevitation.class);
		
		//exotic
		canThrowPots.add(PotionOfCleansing.class);
		
		//elixirs
		canThrowPots.add(ElixirOfHoneyedHealing.class);
	}
	
	protected static ItemStatusHandler<Potion> handler;
	
	protected String color;

	//affects how strongly on-potion talents trigger from this potion
	protected float talentFactor = 1;
	//the chance (0-1) of whether on-potion talents trigger from this potion
	protected float talentChance = 1;
	
	{
		stackable = true;
		defaultAction = AC_DRINK;
	}
	
	@SuppressWarnings("unchecked")
	public static void initColors() {
		handler = new ItemStatusHandler<>( (Class<? extends Potion>[])Generator.Category.POTION.classes, colors );
	}

	public static void clearColors() {
		handler = null;
	}
	
	public static void save( Bundle bundle ) {
		handler.save( bundle );
	}

	public static void saveSelectively( Bundle bundle, ArrayList<Item> items ) {
		ArrayList<Class<?extends Item>> classes = new ArrayList<>();
		for (Item i : items){
			if (i instanceof ExoticPotion){
				if (!classes.contains(ExoticPotion.exoToReg.get(i.getClass()))){
					classes.add(ExoticPotion.exoToReg.get(i.getClass()));
				}
			} else if (i instanceof Potion){
				if (!classes.contains(i.getClass())){
					classes.add(i.getClass());
				}
			}
		}
		handler.saveClassesSelectively( bundle, classes );
	}
	
	@SuppressWarnings("unchecked")
	public static void restore( Bundle bundle ) {
		handler = new ItemStatusHandler<>( (Class<? extends Potion>[])Generator.Category.POTION.classes, colors, bundle );
	}
	
	public Potion() {
		super();
		reset();
	}
	
	//anonymous potions are always IDed, do not affect ID status,
	//and their sprite is replaced by a placeholder if they are not known,
	//useful for items that appear in UIs, or which are only spawned for their effects
	protected boolean anonymous = false;
	public void anonymize(){
		if (!isKnown()) image = ItemSpriteSheet.POTION_HOLDER;
		anonymous = true;
	}

	@Override
	public void reset(){
		super.reset();
		if (handler != null && handler.contains(this)) {
			image = handler.image(this);
			color = handler.label(this);
		} else {
			image = ItemSpriteSheet.POTION_CRIMSON;
			color = "crimson";
		}
	}

	@Override
	public String defaultAction() {
		if (isKnown() && mustThrowPots.contains(this.getClass())) {
			return AC_THROW;
		} else if (isKnown() &&canThrowPots.contains(this.getClass())){
			return AC_CHOOSE;
		} else {
			return AC_DRINK;
		}
	}
	
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_DRINK );
		return actions;
	}
	
	@Override
	public void execute( final Hero hero, String action ) {

		super.execute( hero, action );
		
		if (action.equals( AC_CHOOSE )){
			
			GameScene.show(new WndUseItem(null, this) );
			
		} else if (action.equals( AC_DRINK )) {
			
			if (isKnown() && mustThrowPots.contains(getClass())) {
				
					GameScene.show(
						new WndOptions(new ItemSprite(this),
								Messages.get(Potion.class, "harmful"),
								Messages.get(Potion.class, "sure_drink"),
								Messages.get(Potion.class, "yes"), Messages.get(Potion.class, "no") ) {
							@Override
							protected void onSelect(int index) {
								if (index == 0) {
									drink( hero );
								}
							}
						}
					);
					
				} else {
					drink( hero );
				}
			
		}
	}
	
	@Override
	public void doThrow( final Hero hero ) {

		if (isKnown()
				&& !mustThrowPots.contains(this.getClass())
				&& !canThrowPots.contains(this.getClass())) {
		
			GameScene.show(
				new WndOptions(new ItemSprite(this),
						Messages.get(Potion.class, "beneficial"),
						Messages.get(Potion.class, "sure_throw"),
						Messages.get(Potion.class, "yes"), Messages.get(Potion.class, "no") ) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							Potion.super.doThrow( hero );
						}
					}
				}
			);
			
		} else {
			super.doThrow( hero );
		}
	}
	
	protected void drink( Hero hero ) {
		
		detach( hero.belongings.backpack );
		
		hero.spend( TIME_TO_DRINK );
		hero.busy();
		apply( hero );
		
		Sample.INSTANCE.play( Assets.Sounds.DRINK );
		
		hero.sprite.operate( hero.pos );

		if (!anonymous) {
			Catalog.countUse(getClass());
			if (Random.Float() < talentChance) {
				Talent.onPotionUsed(curUser, curUser.pos, talentFactor);
			}
		}
	}
	
	@Override
	protected void onThrow( int cell ) {
		if (Dungeon.level.map[cell] == Terrain.WELL || Dungeon.level.pit[cell]) {
			
			super.onThrow( cell );
			
		} else  {

			//aqua brew and storm clouds specifically don't press cells, so they can disarm traps
			if (!(this instanceof AquaBrew) && !(this instanceof PotionOfStormClouds)){
				Dungeon.level.pressCell( cell );
			}
			shatter( cell );

			if (!anonymous) {
				Catalog.countUse(getClass());
				if (Random.Float() < talentChance) {
					Talent.onPotionUsed(curUser, cell, talentFactor);
				}
			}
			
		}
	}
	
	public void apply( Hero hero ) {
		shatter( hero.pos );
	}
	
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			GLog.i( Messages.get(Potion.class, "shatter") );
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
		}
	}

	@Override
	public void cast( final Hero user, int dst ) {
			super.cast(user, dst);
	}
	
	public boolean isKnown() {
		return anonymous || (handler != null && handler.isKnown( this ));
	}
	
	public void setKnown() {
		if (!anonymous) {
			if (!isKnown()) {
				handler.know(this);
				updateQuickslot();
			}
			
			if (Dungeon.hero.isAlive()) {
				Catalog.setSeen(getClass());
				Statistics.itemTypesDiscovered.add(getClass());
			}
		}
	}
	
	@Override
	public Item identify( boolean byHero ) {
		super.identify(byHero);

		if (!isKnown()) {
			setKnown();
		}
		return this;
	}
	
	@Override
	public String name() {
		return isKnown() ? super.name() : Messages.get(this, color);
	}

	@Override
	public String info() {
		//skip custom notes if anonymized and un-Ided
		return (anonymous && (handler == null || !handler.isKnown( this ))) ? desc() : super.info();
	}

	@Override
	public String desc() {
		return isKnown() ? super.desc() : Messages.get(this, "unknown_desc");
	}
	
	@Override
	public boolean isIdentified() {
		return isKnown();
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	public static HashSet<Class<? extends Potion>> getKnown() {
		return handler.known();
	}
	
	public static HashSet<Class<? extends Potion>> getUnknown() {
		return handler.unknown();
	}
	
	public static boolean allKnown() {
		return handler != null && handler.known().size() == Generator.Category.POTION.classes.length;
	}
	
	protected int splashColor(){
		return anonymous ? 0x00AAFF : ItemSprite.pick( image, 5, 9 );
	}
	
	protected void splash( int cell ) {
		Fire fire = (Fire)Dungeon.level.blobs.get( Fire.class );
		if (fire != null) {
			fire.clear(cell);
		}

		Char ch = Actor.findChar(cell);
		if (ch != null && ch.alignment == Char.Alignment.ALLY) {
			Buff.detach(ch, Burning.class);
			Buff.detach(ch, Ooze.class);
		}

		if (Dungeon.level.heroFOV[cell]) {
			if (ch != null) {
				Splash.at(ch.sprite.center(), splashColor(), 5);
			} else {
				Splash.at(cell, splashColor(), 5);
			}
		}
	}
	
	@Override
	public int value() {
		return 30 * quantity;
	}

	@Override
	public int energyVal() {
		return 6 * quantity;
	}

	public static class PlaceHolder extends Potion {
		
		{
			image = ItemSpriteSheet.POTION_HOLDER;
		}
		
		@Override
		public boolean isSimilar(Item item) {
			return ExoticPotion.regToExo.containsKey(item.getClass())
					|| ExoticPotion.regToExo.containsValue(item.getClass());
		}
		
		@Override
		public String info() {
			return "";
		}
	}
	
	public static class SeedToPotion extends Recipe {
		
		public static HashMap<Class<?extends Plant.Seed>, Class<?extends Potion>> types = new HashMap<>();
		static {
			types.put(Blindweed.Seed.class,     PotionOfInvisibility.class);
			types.put(Mageroyal.Seed.class,     PotionOfPurity.class);
			types.put(Earthroot.Seed.class,     PotionOfParalyticGas.class);
			types.put(Fadeleaf.Seed.class,      PotionOfMindVision.class);
			types.put(Firebloom.Seed.class,     PotionOfLiquidFlame.class);
			types.put(Icecap.Seed.class,        PotionOfFrost.class);
			types.put(Rotberry.Seed.class,      PotionOfStrength.class);
			types.put(Sorrowmoss.Seed.class,    PotionOfToxicGas.class);
			types.put(Starflower.Seed.class,    PotionOfExperience.class);
			types.put(Stormvine.Seed.class,     PotionOfLevitation.class);
			types.put(Sungrass.Seed.class,      PotionOfHealing.class);
			types.put(Swiftthistle.Seed.class,  PotionOfHaste.class);
			// Overgrown PD Group 1 potions
			types.put(Ballcrop.Seed.class,      PotionOfBall.class);
			types.put(Blueeyedsusan.Seed.class, PotionOfBlessing.class);
			types.put(Chillisnapper.Seed.class, PotionOfChilli.class);
			types.put(Dewcatcher.Seed.class,    PotionOfDew.class);
			types.put(Eyeeuonymus.Seed.class,   PotionOfEye.class);
			types.put(Chandaliertail.Seed.class,PotionOfGlowing.class);
			types.put(Grasslilly.Seed.class,    PotionOfGrass.class);
			types.put(Combflower.Seed.class,    PotionOfHoney.class);
			types.put(Lavenderlantern.Seed.class,PotionOfLantern.class);
			types.put(Parasiteshrub.Seed.class, PotionOfParasites.class);
			types.put(Peanutpetal.Seed.class,   PotionOfPeanuts.class);
			types.put(Crimsonpepper.Seed.class, PotionOfPepper.class);
			types.put(Cocostuft.Seed.class,     PotionOfProtection.class);
			types.put(Feelerfern.Seed.class,    PotionOfRegrowth.class);
			types.put(Shadowbloom.Seed.class,   PotionOfShadows.class);
			types.put(Rose.Seed.class,          PotionOfShield.class);
			types.put(Gobgrape.Seed.class,      PotionOfWine.class);
			types.put(Witherfennel.Seed.class,  PotionOfWithering.class);
			// Overgrown PD Group 2 potions
			types.put(Bananabean.Seed.class,    PotionOfBanana.class);
			types.put(Eggbloom.Seed.class,      PotionOfEgg.class);
			types.put(Flowertree.Seed.class,    PotionOfFlora.class);
			types.put(Apricobush.Seed.class,    PotionOfHunger.class);
			types.put(Clitbalm.Seed.class,      PotionOfLove.class);
			types.put(Musclemoss.Seed.class,    PotionOfMuscle.class);
			types.put(Clockcypress.Seed.class,  PotionOfTime.class);
			types.put(Tomatobush.Seed.class,    PotionOfTomatoSoup.class);
			types.put(Crimsoncrown.Seed.class,  PotionOfFirelightning.class);
			types.put(Lightninglily.Seed.class, PotionOfLightning.class);

			types.put(Sourpitcher.Seed.class,     PotionOfDigesting.class);
			types.put(Firefoxglove.Seed.class,    PotionOfFirestorm.class);
			types.put(Tankcabbage.Seed.class,     PotionOfHydrogenFire.class);
			types.put(Hypnohemp.Seed.class,       PotionOfHypno.class);
			types.put(Frostcorn.Seed.class,       PotionOfIceStorm.class);
			types.put(Kiwivetch.Seed.class,       PotionOfKiwi.class);
			types.put(Willowcane.Seed.class,      PotionOfSlowness.class);
			types.put(Nightshadeonion.Seed.class, PotionOfSmoke.class);
			types.put(Snowhedge.Seed.class,       PotionOfSnowstorm.class);
			types.put(Poppoplar.Seed.class,       PotionOfSoda.class);
			types.put(Steamweed.Seed.class,       PotionOfSteam.class);
			types.put(Suncarnivore.Seed.class,    PotionOfUltraviolett.class);

			types.put(Butterlion.Seed.class,      PotionOfButter.class);
			types.put(Dirtdaisy.Seed.class,       PotionOfDirt.class);
			types.put(Goograss.Seed.class,        PotionOfGoo.class);
			types.put(Cornwheat.Seed.class,       PotionOfHarvest.class);
			types.put(Larvaleaf.Seed.class,       PotionOfInfection.class);
			types.put(Sunbloom.Seed.class,        PotionOfSun.class);
			types.put(Grassvine.Seed.class,       PotionOfVine.class);
			types.put(Waterweed.Seed.class,       PotionOfWater.class);
		}
		
		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 3) {
				return false;
			}
			
			for (Item ingredient : ingredients){
				if (!(ingredient instanceof Plant.Seed
						&& ingredient.quantity() >= 1
						&& types.containsKey(ingredient.getClass()))){
					return false;
				}
			}
			return true;
		}
		
		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 0;
		}
		
		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			
			for (Item ingredient : ingredients){
				ingredient.quantity(ingredient.quantity() - 1);
			}
			
			ArrayList<Class<?extends Plant.Seed>> seeds = new ArrayList<>();
			for (Item i : ingredients) {
				if (!seeds.contains(i.getClass())) {
					seeds.add((Class<? extends Plant.Seed>) i.getClass());
				}
			}
			
			Potion result;
			
			if ( (seeds.size() == 2 && Random.Int(4) == 0)
					|| (seeds.size() == 3 && Random.Int(2) == 0)) {
				
				result = (Potion) Generator.randomUsingDefaults( Generator.Category.POTION );
				
			} else {
				result = Reflection.newInstance(types.get(Random.element(ingredients).getClass()));
				
			}
			
			if (seeds.size() == 1){
				result.identify();
			}

			while (result instanceof PotionOfHealing
					&& Random.Int(10) < Dungeon.LimitedDrops.COOKING_HP.count) {

				result = (Potion) Generator.randomUsingDefaults(Generator.Category.POTION);
			}
			
			if (result instanceof PotionOfHealing) {
				Dungeon.LimitedDrops.COOKING_HP.count++;
			}
			
			return result;
		}
		
		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new WndBag.Placeholder(ItemSpriteSheet.POTION_HOLDER){

				@Override
				public String name() {
					return Messages.get(Potion.SeedToPotion.class, "name");
				}
				
				@Override
				public String info() {
					return "";
				}
			};
		}
	}


	//per-potion effect when an arrow soaked in this potion lands (Re-ARranged port)
	public void potionProc(Hero hero, Char enemy, float damage) {
		//nothing by default
	}

	public ItemSprite.Glowing potionGlowing() {
		return null; //no glow by default
	}
}
