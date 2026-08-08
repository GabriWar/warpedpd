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

package xyz.gabriwar.warpedpixeldungeon.plants;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barkskin;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LivingPlant;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public abstract class Plant implements Bundlable {
	
	public int image;
	public int livingPlantImage = -1; // sprite sheet row in livingplants.png; -1 = use image field
	public int pos;

	protected Class<? extends Plant.Seed> seedClass;

	public Class<? extends Plant.Seed> seedClass() { return seedClass; }

	/**
	 * Temperature contribution (°C) this plant radiates while alive on its tile.
	 * Override in hot/cold plants; default is 0 (no effect).
	 */
	public float temperatureBonus() { return 0f; }

	// Whether this plant was planted by the player (no LivingPlant spawn)
	public boolean playerPlanted = false;

	public void trigger(){

		Char ch = Actor.findChar(pos);

		if (ch instanceof Hero){
			((Hero) ch).interrupt();
		}

		if (Dungeon.level.heroFOV[pos] && Dungeon.hero.hasTalent(Talent.NATURES_AID)){
			// 3/5 turns based on talent points spent
			Barkskin.conditionallyAppend(Dungeon.hero, 2, 1 + 2*(Dungeon.hero.pointsInTalent(Talent.NATURES_AID)));
		}

		// Dynamic LivingPlant spawn chance based on adjacent terrain
		float livingPlantChance = 0.30f;
		for (int i : PathFinder.NEIGHBOURS8) {
			int tile = Dungeon.level.map[pos + i];
			if (tile == Terrain.HIGH_GRASS || tile == Terrain.WALL || tile == Terrain.WALL_DECO)
				livingPlantChance -= 0.0375f;
			if (tile == Terrain.WELL || tile == Terrain.WATER || tile == Terrain.ALCHEMY)
				livingPlantChance += 0.0375f;
		}
		livingPlantChance = Math.max(0f, livingPlantChance);

		if (!playerPlanted && Random.Float() < livingPlantChance
				&& !(this instanceof BlandfruitBush) && !(this instanceof Rotberry)) {
			if (spawnLivingPlant()) {
				wither();
				Bestiary.setSeen(getClass());
				Bestiary.countEncounter(getClass());
				return;
			}
		}

		wither();
		activate( ch );
		Bestiary.setSeen(getClass());
		Bestiary.countEncounter(getClass());
	}

	public boolean spawnLivingPlant() {
		// Find an adjacent empty cell for the LivingPlant
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = pos + i;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])
					&& !Dungeon.level.pit[cell]
					&& Actor.findChar(cell) == null) {
				LivingPlant plant = new LivingPlant();
				plant.setPlantClass(this);
				plant.pos = cell;
				plant.state = plant.HUNTING;
				GameScene.add(plant);
				Actor.addDelayed(new Pushing(plant, pos, cell), 0.0f);
				plant.move(cell);
				if (Dungeon.level.heroFOV[cell]) {
					CellEmitter.get(cell).burst(LeafParticle.GENERAL, 6);
				}
				return true;
			}
		}
		return false;
	}
	
	public abstract void activate( Char ch );

	// Called when the LivingPlant version of this plant attacks an enemy
	public void attackProc( Char enemy, int damage ) { }

	// Called when the seed is eaten as a spice
	public void spiceEffect( Char ch ) { }

	// Returns the blob class this plant's LivingPlant should be immune to
	public Class<?> immunity() { return null; }

	public void wither() {
		Dungeon.level.uproot( pos );

		if (Dungeon.level.heroFOV[pos]) {
			CellEmitter.get( pos ).burst( LeafParticle.GENERAL, 6 );
		}

		float seedChance = 0f;
		for (Char c : Actor.chars()){
			if (c instanceof WandOfRegrowth.Lotus){
				WandOfRegrowth.Lotus l = (WandOfRegrowth.Lotus) c;
				if (l.inRange(pos)){
					seedChance = Math.max(seedChance, l.seedPreservation());
				}
			}
		}

		// Season affects seed preservation
		seedChance += GameCalendar.seasonSeedBonus();
		seedChance = Math.max(0, seedChance);

		if (Random.Float() < seedChance){
			if (seedClass != null && seedClass != Rotberry.Seed.class) {
				Dungeon.level.drop(Reflection.newInstance(seedClass), pos).sprite.drop();
			}
		}
		
	}
	
	private static final String POS	= "pos";
	private static final String PLAYER_PLANTED = "player_planted";

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		pos = bundle.getInt( POS );
		playerPlanted = bundle.getBoolean( PLAYER_PLANTED );
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put( POS, pos );
		bundle.put( PLAYER_PLANTED, playerPlanted );
	}

	public String name(){
		return Messages.get(this, "name");
	}

	public String desc() {
		String desc = Messages.get(this, "desc");
		if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.WARDEN){
			desc += "\n\n" + Messages.get(this, "warden_desc");
		}
		return desc;
	}
	
	public static class Seed extends Item {

		public static final String AC_PLANT = "PLANT";
		public static final String AC_EAT   = "EAT";

		private static final float TIME_TO_PLANT = 1f;
		
		{
			stackable = true;
			defaultAction = AC_THROW;
		}
		
		protected Class<? extends Plant> plantClass;

		public Class<? extends Plant> getPlantClass() {
			return plantClass;
		}

		// Emitter factory for this seed's poison particles (weapon coating visuals)
		public Emitter.Factory getPixelParticle(){
			return null;
		}

		// A particle instance, used only as a colour source for burst effects
		public PixelParticle poisonEmitterClass(){
			return null;
		}

		// Called when a weapon coated with this seed hits a target
		public void onProc( Char attacker, Char defender, int damage ){
			//poison can not affect inorganic, acidic and fiery actors for obvious reasons
			if (!defender.properties().contains(Char.Property.INORGANIC)
					&& !defender.properties().contains(Char.Property.ACIDIC)
					&& !defender.properties().contains(Char.Property.FIERY)){
				if (Dungeon.level.heroFOV[defender.pos] && poisonEmitterClass() != null) {
					defender.sprite.burst(poisonEmitterClass().getColor(), damage);
				}
				procEffect(attacker, defender, damage);
			}
		}

		public void procEffect( Char attacker, Char defender, int damage ){

		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions( hero );
			actions.add( AC_PLANT );
			actions.add( AC_EAT );
			return actions;
		}
		
		@Override
		protected void onThrow( int cell ) {
			if (Dungeon.level.map[cell] == Terrain.ALCHEMY
					|| Dungeon.level.pit[cell]
					|| Dungeon.level.traps.get(cell) != null
					|| Dungeon.isChallenged(Challenges.NO_HERBALISM)) {
				super.onThrow( cell );
			} else {
				Catalog.countUse(getClass());
				Dungeon.level.plant( this, cell );
				if (Dungeon.hero.subClass == HeroSubClass.WARDEN) {
					for (int i : PathFinder.NEIGHBOURS8) {
						int c = Dungeon.level.map[cell + i];
						if ( c == Terrain.EMPTY || c == Terrain.EMPTY_DECO
								|| c == Terrain.EMBERS || c == Terrain.GRASS){
							Level.set(cell + i, Terrain.FURROWED_GRASS);
							GameScene.updateMap(cell + i);
							CellEmitter.get( cell + i ).burst( LeafParticle.LEVEL_SPECIFIC, 4 );
						}
					}
				}
			}
		}
		
		@Override
		public void execute( Hero hero, String action ) {

			super.execute (hero, action );

			if (action.equals( AC_PLANT )) {

				hero.busy();
				((Seed)detach( hero.belongings.backpack )).onThrow( hero.pos );
				hero.spend( TIME_TO_PLANT );

				hero.sprite.operate( hero.pos );

			} else if (action.equals( AC_EAT )) {

				hero.spend( 1f );
				hero.busy();
				detach( hero.belongings.backpack );
				hero.sprite.operate( hero.pos );
				eatEffect( hero );

			}
		}

		protected void eatEffect( Hero hero ) {
			Hunger hunger = hero.buff( Hunger.class );
			if (hunger != null) hunger.affectHunger( 30f );
			GLog.i( Messages.get( Seed.class, "seed_eaten", name() ) );
			Plant plant = Reflection.newInstance( plantClass );
			plant.spiceEffect( hero );
		}
		
		public Plant couch( int pos, Level level ) {
			if (level != null && level.heroFOV != null && level.heroFOV[pos]) {
				Sample.INSTANCE.play(Assets.Sounds.PLANT);
				CellEmitter.get(pos).burst(LeafParticle.GENERAL, 10);
			}
			Plant plant = Reflection.newInstance(plantClass);
			plant.pos = pos;
			plant.playerPlanted = true;
			return plant;
		}
		
		@Override
		public boolean isUpgradable() {
			return false;
		}
		
		@Override
		public boolean isIdentified() {
			return true;
		}
		
		@Override
		public int value() {
			return 10 * quantity;
		}

		@Override
		public int energyVal() {
			return 2 * quantity;
		}

		@Override
		public String desc() {
			String desc = Messages.get(plantClass, "desc");
			if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.WARDEN){
				desc += "\n\n" + Messages.get(plantClass, "warden_desc");
			}
			//farming profile: this seed's behaviour when sown on tilled safe-zone soil
			int[] farm = xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.profileFor(plantClass);
			desc += "\n\n" + Messages.get(Seed.class, "farm_desc", farm[0], farm[1], farm[2]);
			if (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.lovesWater(plantClass)){
				desc += " " + Messages.get(Seed.class, "farm_water");
			}
			return desc;
		}

		@Override
		public String info() {
			return Messages.get( Seed.class, "info", super.info() );
		}
		
		public static class PlaceHolder extends Seed {
			
			{
				image = ItemSpriteSheet.SEED_HOLDER;
			}
			
			@Override
			public boolean isSimilar(Item item) {
				return item instanceof Plant.Seed;
			}
			
			@Override
			public String info() {
				return "";
			}
		}
	}
}
