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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.catching.CritterJar;
import xyz.gabriwar.warpedpixeldungeon.items.food.BlueMilk;
import xyz.gabriwar.warpedpixeldungeon.items.food.DeathCap;
import xyz.gabriwar.warpedpixeldungeon.items.food.EagleEgg;
import xyz.gabriwar.warpedpixeldungeon.items.food.Earthstar;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenJelly;
import xyz.gabriwar.warpedpixeldungeon.items.food.JackOLantern;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.PixieParasol;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GatorTooth;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.VultureFeather;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.BogLantern;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.WadingBalm;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.WitchsRemedy;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SwampWitchSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;

/**
 * The witch of a hut in the bog (levels/overworld/WetlandSites). She keeps a shelf of what only
 * the swamp makes - her remedy, a bog lantern, a wading balm - and a potion or two, at the bog's
 * mark-up; she brews: any three herbs, mushrooms or critter parts from the hero's pack go into her
 * pot and come out a potion (brewKind); and for a fee she lifts a curse off an item, telling him
 * for nothing when there was none to lift.
 *
 * She keeps the shops' hours: by night she is out in the bog, gathering. Kept forever in the
 * parked store as every NPC is; driven off (a shopkeeper's flight) she never comes back. A co-op
 * guest is shown her plain shop.
 */
public class SwampWitch extends Shopkeeper {

	{
		spriteClass = SwampWitchSprite.class;
	}

	//what she charges over a shop's shelf price: everything out here came through the bog
	public static final float MARK_UP = 1.25f;
	//a brew's fee in gold, less this for each critter part in the pot (three are her whole fee)
	public static final int BREW_FEE = 90, CRITTER_OFF = 30;
	//lifting a curse
	public static final int CURSE_FEE = 250;
	//what goes in her pot
	public static final int POT = 3;

	public long siteKey = 0;
	private boolean firstStock = false;

	public static SwampWitch of( long siteKey ){
		SwampWitch w = new SwampWitch();
		w.siteKey = siteKey;
		return w;
	}

	/** Requests the first shelf fill, on her first act (the level is live by then). */
	public void openShop(){
		firstStock = true;
	}

	@Override
	protected boolean act(){
		if (firstStock){
			firstStock = false;
			lastRestock = restockClock();
			restock();
		}
		return super.act();
	}

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		stock.add( new WitchsRemedy().quantity( 2 ) );
		stock.add( new BogLantern() );
		stock.add( new WadingBalm().quantity( 2 ) );
		Item potion = Generator.randomUsingDefaults( Generator.Category.POTION );
		if (potion != null) stock.add( potion );
		for (Item it : stock){
			it.cursed = false;
			it.identify( false );
		}
		return stock;
	}

	@Override
	public float priceFactor(){
		return MARK_UP;
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( SwampWitch.class, "talk_" + Math.floorMod( WorldClock.day() + (int) siteKey, 5 ) );
	}

	@Override
	public boolean interact( Char c ){
		if (c instanceof Hero && closedForNight()){
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( (Hero) c, Messages.get( SwampWitch.class, "night" ) );
			return true;
		}
		//a guest gets the plain shop
		if (!(c instanceof Hero) || ((Hero) c).isRemote || c != Dungeon.hero || tradeBlock() != null){
			return super.interact( c );
		}
		final Hero hero = (Hero) c;
		beginTrade( hero );
		final String talk = chatTextFor( hero );
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ), talk,
				Messages.get( SwampWitch.class, "opt_trade" ),
				Messages.get( SwampWitch.class, "opt_brew" ),
				Messages.get( SwampWitch.class, "opt_curse", CURSE_FEE ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0){
					showShopMenu( Messages.titleCase( name() ), description(), talk, buybackItems, Dungeon.gold, false );
				} else if (index == 1){
					GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
							Messages.get( SwampWitch.class, "brew_rules", BREW_FEE, CRITTER_OFF ),
							Messages.get( SwampWitch.class, "brew_start" ), Messages.get( SwampWitch.class, "leave" ) ){
						@Override
						protected void onSelect( int index ){
							if (index == 0) pick( hero, new ArrayList<>() );
						}
					} );
				} else if (index == 2){
					GameScene.selectItem( curser( hero ) );
				}
			}
		} ) );
		return true;
	}

	// ------------------------------------------------------------ her pot

	/** A herb: a seed with a potion of its own (Potion.SeedToPotion). */
	public static boolean herb( Item item ){
		return item instanceof Plant.Seed && Potion.SeedToPotion.types.containsKey( item.getClass() );
	}

	/** A mushroom: the caps the woods, the swamps and the caves grow. */
	public static boolean mushroom( Item item ){
		return item instanceof BlueMilk || item instanceof DeathCap || item instanceof Earthstar
				|| item instanceof GoldenJelly || item instanceof JackOLantern || item instanceof PixieParasol;
	}

	/** A critter part: raw meat, a beast's hide, an egg, a vulture's feather, an alligator's tooth,
	 *  or a whole small critter in its jar (items/catching). */
	public static boolean critterPart( Item item ){
		return item instanceof MysteryMeat || item instanceof MonsterMeat || item instanceof Hide
				|| item instanceof EagleEgg || item instanceof VultureFeather || item instanceof GatorTooth
				|| item instanceof CritterJar;
	}

	public static boolean ingredient( Item item ){
		return herb( item ) || mushroom( item ) || critterPart( item );
	}

	/** Her fee for a pot holding this many critter parts. */
	public static int brewFee( int critterParts ){
		return Math.max( 0, BREW_FEE - CRITTER_OFF * critterParts );
	}

	/**
	 * What her pot brews, by the game's Random: the herbs decide the potion - three of one kind
	 * always brew that kind's, else one of them at random by how many of each went in (a pot with
	 * none brews any potion) - and each mushroom gives a third of a chance it comes out exotic,
	 * where the potion has an exotic form. Identified when the herbs were all of one kind.
	 */
	public static Potion brewKind( List<Class<? extends Plant.Seed>> herbs, int mushrooms ){
		Class<? extends Potion> kind;
		boolean pure = !herbs.isEmpty();
		for (Class<? extends Plant.Seed> h : herbs) pure &= h == herbs.get( 0 );
		if (herbs.isEmpty()){
			Item any = Generator.randomUsingDefaults( Generator.Category.POTION );
			kind = any instanceof Potion ? ((Potion) any).getClass() : xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing.class;
		} else {
			kind = Potion.SeedToPotion.types.get( pure ? herbs.get( 0 ) : Random.element( herbs ) );
		}
		boolean exotic = false;
		for (int i = 0; i < mushrooms; i++) exotic |= Random.Int( 3 ) == 0;
		if (exotic && ExoticPotion.regToExo.containsKey( kind )) kind = ExoticPotion.regToExo.get( kind );
		Potion potion = Reflection.newInstance( kind );
		if (potion != null && pure && herbs.size() == POT) potion.identify();
		return potion;
	}

	//what is in the pot of one item already (a stack may go in more than once)
	private static int inPot( List<Item> pot, Item item ){
		int n = 0;
		for (Item i : pot) if (i == item) n++;
		return n;
	}

	//one ingredient after another, on the render thread as the bag's choices are
	private void pick( final Hero hero, final ArrayList<Item> pot ){
		GameScene.selectItem( new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( SwampWitch.class, "brew_prompt", pot.size() + 1, POT );
			}

			@Override
			public Class<? extends Bag> preferredBag(){
				return Belongings.Backpack.class;
			}

			@Override
			public boolean itemSelectable( Item item ){
				return ingredient( item ) && item.quantity() > inPot( pot, item );
			}

			@Override
			public void onSelect( Item item ){
				if (item == null || !hero.isAlive()){
					if (!pot.isEmpty()) GLog.i( Messages.get( SwampWitch.class, "brew_cancel" ) );
					return;
				}
				pot.add( item );
				if (pot.size() < POT) pick( hero, pot );
				else brew( hero, pot );
			}
		} );
	}

	private void brew( Hero hero, ArrayList<Item> pot ){
		ArrayList<Class<? extends Plant.Seed>> herbs = new ArrayList<>();
		int mushrooms = 0, parts = 0;
		for (Item i : pot){
			if (herb( i )){
				@SuppressWarnings("unchecked")
				Class<? extends Plant.Seed> c = (Class<? extends Plant.Seed>) i.getClass();
				herbs.add( c );
			} else if (mushroom( i )){
				mushrooms++;
			} else {
				parts++;
			}
		}
		int fee = brewFee( parts );
		if (Dungeon.gold < fee){
			GLog.w( Messages.get( SwampWitch.class, "poor", fee ) );
			return;
		}
		//everything still in the pack, a stack's count for each time it went in
		for (Item i : pot){
			if (i.quantity() < inPot( pot, i ) || !hero.belongings.contains( i )){
				GLog.w( Messages.get( SwampWitch.class, "brew_cancel" ) );
				return;
			}
		}
		Dungeon.gold -= fee;
		for (Item i : pot) i.detach( hero.belongings.backpack );
		Potion potion = brewKind( herbs, mushrooms );
		if (potion == null) return;
		Item.updateQuickslot();
		stir();
		if (!potion.collect()){
			xyz.gabriwar.warpedpixeldungeon.items.Heap h = Dungeon.level.drop( potion, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
		GLog.p( Messages.get( SwampWitch.class, potion instanceof ExoticPotion ? "brewed_strong" : "brewed",
				Messages.titleCase( potion.title() ) ) );
		hero.spendAndNext( 1f );
	}

	//her cauldron boils up as she brews, and she stirs
	private void stir(){
		WitchCauldron pot = cauldron();
		int at = pot != null ? pot.pos : pos;
		if (sprite != null && sprite.parent != null){
			sprite.operate( at );
			CellEmitter.center( at ).burst( Speck.factory( Speck.BUBBLE ), 6 );
			SpatialSound.play( Assets.Sounds.DRINK, at, 0.8f, 0.6f );
		}
	}

	//her cauldron: the one in her hut, beside her
	private WitchCauldron cauldron(){
		for (Char ch : Actor.chars()){
			if (ch instanceof WitchCauldron && Dungeon.level.distance( ch.pos, pos ) <= 3) return (WitchCauldron) ch;
		}
		return null;
	}

	// ------------------------------------------------------------ curses

	/** Is there a curse on it to lift (the item's own, a weapon's cursed enchantment, an armour's glyph)? */
	public static boolean cursed( Item item ){
		return item.cursed
				|| (item instanceof Weapon && ((Weapon) item).hasCurseEnchant())
				|| (item instanceof Armor && ((Armor) item).hasCurseGlyph());
	}

	//render thread, as the bag's choices are
	private WndBag.ItemSelector curser( final Hero hero ){
		return new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( SwampWitch.class, "curse_prompt" );
			}

			@Override
			public boolean itemSelectable( Item item ){
				return ScrollOfRemoveCurse.uncursable( item );
			}

			@Override
			public void onSelect( Item item ){
				if (item == null || !hero.isAlive()) return;
				if (!cursed( item )){
					//she tells him for nothing that there is none
					item.cursedKnown = true;
					Item.updateQuickslot();
					GLog.i( Messages.get( SwampWitch.class, "curse_none" ) );
					return;
				}
				if (Dungeon.gold < CURSE_FEE){
					GLog.w( Messages.get( SwampWitch.class, "poor", CURSE_FEE ) );
					return;
				}
				Dungeon.gold -= CURSE_FEE;
				ScrollOfRemoveCurse.uncurse( hero, item );
				if (hero.sprite != null && hero.sprite.parent != null){
					hero.sprite.emitter().burst( ShadowParticle.UP, 8 );
					SpatialSound.play( Assets.Sounds.CURSED, hero, 0.7f, 0.8f );
				}
				GLog.p( Messages.get( SwampWitch.class, "curse_lifted" ) );
				hero.spendAndNext( 1f );
			}
		};
	}

	// ------------------------------------------------------------ her flight

	//driven off: her shelf comes down with her and the hut stays empty - no portal of the
	//dungeon's to close up here (Shopkeeper.flee)
	@Override
	public void flee(){
		destroy();
		Notes.remove( landmark() );
		GLog.newLine();
		GLog.n( Messages.get( this, "flee" ) );
		if (sprite != null){
			sprite.killAndErase();
			CellEmitter.get( pos ).burst( ElmoParticle.FACTORY, 6 );
		}
		if (Dungeon.level instanceof OverworldLevel && siteKey != 0){
			((OverworldLevel) Dungeon.level).markSiteCleared( siteKey );
		}
	}

	private static final String SITE = "witch_site";
	private static final String FIRST_STOCK = "first_stock";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SITE, siteKey );
		bundle.put( FIRST_STOCK, firstStock );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		siteKey = bundle.contains( SITE ) ? bundle.getLong( SITE ) : 0;
		firstStock = bundle.contains( FIRST_STOCK ) && bundle.getBoolean( FIRST_STOCK );
	}
}
