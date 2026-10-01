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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MarketBodyguard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BlackMarketDealer;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenSkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.GearPerk;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.TypeShifter;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The black market's ledger and its clock, kept on the floor itself. The one cell it
 * marks is only an anchor - nothing is drawn on it; everything it knows is in its fields:
 * where the dealer stands, where his guards stand, and what is under the counter this week.
 *
 * The surface shops shut at night (Shopkeeper.closedForNight), so this one keeps the
 * other half of the day: after dark the dealer and his two guards are simply there,
 * and at dawn they are simply gone. The people are made fresh every evening and taken
 * off the floor every morning; only the stock and the grudges are kept here. An empty
 * market by day really is empty, with nothing hidden in it to find.
 */
public class BlackMarket extends Blob {

	{
		//after the mobs have moved, so nobody is spawned onto a cell being walked into
		actPriority = MOB_PRIO - 1;
	}

	/** One line of the dealer's list. A tip has no item: it is a service. */
	public static class Offer implements Bundlable {

		public static final int GEAR = 0, CORE = 1, SHIFTER = 2, KEY = 3, TIP = 4;

		public int kind;
		public Item item;
		public int price;
		public boolean sold;

		public Offer(){
		}

		public Offer( int kind, Item item, int price ){
			this.kind = kind;
			this.item = item;
			this.price = price;
		}

		private static final String KIND  = "kind";
		private static final String ITEM  = "item";
		private static final String PRICE = "price";
		private static final String SOLD  = "sold";

		@Override
		public void storeInBundle( Bundle bundle ){
			bundle.put( KIND, kind );
			if (item != null) bundle.put( ITEM, item );
			bundle.put( PRICE, price );
			bundle.put( SOLD, sold );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			kind = bundle.getInt( KIND );
			item = bundle.contains( ITEM ) ? (Item) bundle.get( ITEM ) : null;
			price = bundle.getInt( PRICE );
			sold = bundle.getBoolean( SOLD );
		}
	}

	public int home = -1;
	public int[] posts = new int[0];

	public ArrayList<Offer> stock = new ArrayList<>();
	private int stockWeek = Integer.MIN_VALUE;

	//this floor's market saw steel drawn: its guards fight and its dealer is gone
	public boolean hostile = false;

	public static BlackMarket of( xyz.gabriwar.warpedpixeldungeon.levels.Level level ){
		return level == null ? null : (BlackMarket) level.blobs.get( BlackMarket.class );
	}

	// ---------------------------------------------------------------- the clock

	@Override
	protected void evolve(){
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (Dungeon.level.insideMap( cell )){
					off[cell] = cur[cell];
					volume += off[cell];
				}
			}
		}
		if (home < 0) return;

		boolean trading = DayNightCycle.isNight() && !WarpedRooms.banned && !hostile;
		BlackMarketDealer dealer = dealer();
		if (trading && dealer == null){
			open();
		} else if (!trading && dealer != null){
			close();
		}
	}

	public BlackMarketDealer dealer(){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof BlackMarketDealer) return (BlackMarketDealer) m;
		}
		return null;
	}

	private void open(){
		int week = GameCalendar.weekIndex();
		if (stock.isEmpty() || week != stockWeek){
			stockWeek = week;
			restock();
		}

		int spot = freeNear( home );
		if (spot == -1) return;
		BlackMarketDealer dealer = new BlackMarketDealer();
		dealer.pos = spot;
		GameScene.add( dealer );
		puff( spot );

		for (int post : posts){
			int at = freeNear( post );
			if (at == -1) continue;
			MarketBodyguard guard = new MarketBodyguard();
			guard.pos = at;
			GameScene.add( guard );
			puff( at );
		}

		if (Dungeon.level.heroFOV[spot]){
			dealer.yell( Messages.get( BlackMarketDealer.class, "opening" ) );
		}
	}

	private void close(){
		boolean seen = false;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m instanceof BlackMarketDealer || (m instanceof MarketBodyguard && m.alignment != Char.Alignment.ENEMY)){
				seen |= Dungeon.level.heroFOV[m.pos];
				puff( m.pos );
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
		if (seen) GLog.i( Messages.get( BlackMarketDealer.class, hostile ? "gone_hostile" : "gone_dawn" ) );
	}

	private void puff( int cell ){
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get( cell ).burst( Speck.factory( Speck.WOOL ), 6 );
		}
	}

	//the cell itself when nobody stands there, else the nearest open one beside it
	private int freeNear( int cell ){
		if (Actor.findChar( cell ) == null && Dungeon.level.passable[cell]) return cell;
		for (int ofs : PathFinder.NEIGHBOURS8){
			int c = cell + ofs;
			if (Dungeon.level.insideMap( c ) && Dungeon.level.passable[c] && Actor.findChar( c ) == null){
				return c;
			}
		}
		return -1;
	}

	/** Steel drawn in the market: the dealer is gone for good, here and everywhere, and
	 *  whoever he paid to stand around stops standing around. */
	public void provoke(){
		if (hostile) return;
		hostile = true;
		WarpedRooms.banned = true;

		BlackMarketDealer dealer = dealer();
		if (dealer != null){
			dealer.yell( Messages.get( BlackMarketDealer.class, "provoked" ) );
			puff( dealer.pos );
			dealer.destroy();
			if (dealer.sprite != null) dealer.sprite.killAndErase();
		}
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof MarketBodyguard) ((MarketBodyguard) m).turnHostile();
		}
		Sample.INSTANCE.play( Assets.Sounds.ALERT );
		GLog.n( Messages.get( BlackMarketDealer.class, "banned" ) );
	}

	// ---------------------------------------------------------------- the stock

	/** what the sky does to the dealer's prices right now: a quarter off on Shadowday,
	 *  a fifth on under a full moon (too bright to be out), and both at once if both */
	public static float skyPriceFactor(){
		float f = 1f;
		if (GameCalendar.weekday() == GameCalendar.Weekday.SHADOWDAY) f *= 0.75f;
		if (GameCalendar.isFullMoon()) f *= 1.2f;
		return f;
	}

	/** the price the buyer pays now: the sky, then a Fortune trinket's discount */
	public static int priceFor( Offer offer, Hero buyer ){
		return Math.max( 1, GearPerk.shopPrice( buyer, Math.round( offer.price * skyPriceFactor() ) ) );
	}

	//the same step the surface shops price by (Shopkeeper.basePrice)
	private static int step(){
		return WarpedRooms.threat() / 5 + 1;
	}

	/** the dealer's fee for a rigged spin */
	public static int wheelPrice(){
		return Math.round( 300 * step() * skyPriceFactor() );
	}

	/** the fee for a trade-up contract towards the given rarity */
	public static int contractFee( Rarity to ){
		return Math.round( 60 * to.ordinal() * step() * skyPriceFactor() );
	}

	private void restock(){
		stock.clear();

		//what is coming down the road tonight depends on the sky over it: fog hides a
		//smuggler and a storm keeps him home
		WeatherState sky = ClimateManager.weatherState();
		boolean fog = sky == WeatherState.FOG;
		boolean storm = sky == WeatherState.STORM;

		int gear = storm ? 1 : 2;
		for (int i = 0; i < gear; i++){
			stock.add( gearOffer( Rarity.LEGENDARY ) );
		}
		//a moonless night is when the things that should not exist change hands
		if (GameCalendar.isNewMoon()){
			boolean exotic = Rarity.EXOTIC.weight( WarpedRooms.threat() ) > 0 && Random.Float() < 0.35f;
			stock.add( gearOffer( exotic ? Rarity.EXOTIC : Rarity.LEGENDARY ) );
		}

		//a core sells to any lawful shop for a thousand: priced so that even on Shadowday, to
		//a buyer with a Fortune discount, the market's never undercut that
		int cores = Random.IntRange( 1, 2 ) + (fog ? 1 : 0);
		for (int i = 0; i < cores; i++){
			stock.add( new Offer( Offer.CORE, new MasterworkCore(), 2000 * step() ) );
		}
		stock.add( new Offer( Offer.SHIFTER, new TypeShifter(), 350 * step() ) );
		if (fog) stock.add( new Offer( Offer.SHIFTER, new TypeShifter(), 350 * step() ) );

		stock.add( new Offer( Offer.KEY, new GoldenSkeletonKey( Dungeon.depth ), 500 * step() ) );
		stock.add( new Offer( Offer.TIP, null, 120 * step() ) );
	}

	//a piece of gear at the rarity asked for, unidentified, and cursed three times in
	//ten with nothing on it to say so: that is what "no refunds" is for
	private static Offer gearOffer( Rarity rarity ){
		Item item;
		float roll = Random.Float();
		if (roll < 0.5f)       item = Generator.randomUsingDefaults( Generator.Category.WEAPON );
		else if (roll < 0.85f) item = Generator.random( Generator.Category.ARMOR );
		else                   item = Generator.randomUsingDefaults( Generator.Category.WAND );

		item.cursed = false;
		WarpedRooms.forceQuality( item, rarity, null );

		if (Random.Float() < 0.3f){
			item.cursed = true;
			if (item instanceof Weapon){
				((Weapon) item).enchant( Weapon.Enchantment.randomCurse() );
			} else if (item instanceof Armor){
				((Armor) item).inscribe( Armor.Glyph.randomCurse() );
			}
		}
		item.cursedKnown = false;
		item.levelKnown = false;

		int worth = Math.max( 60, item.value() );
		return new Offer( Offer.GEAR, item, worth * 8 * step() * (rarity == Rarity.EXOTIC ? 2 : 1) );
	}

	// ---------------------------------------------------------------- save

	private static final String HOME       = "home";
	private static final String POSTS      = "posts";
	private static final String STOCK      = "stock";
	private static final String STOCK_WEEK = "stock_week";
	private static final String HOSTILE    = "hostile";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HOME, home );
		bundle.put( POSTS, posts );
		bundle.put( STOCK, stock );
		bundle.put( STOCK_WEEK, stockWeek );
		bundle.put( HOSTILE, hostile );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		home = bundle.getInt( HOME );
		posts = bundle.getIntArray( POSTS );
		stock = new ArrayList<>();
		for (Bundlable b : bundle.getCollection( STOCK )){
			stock.add( (Offer) b );
		}
		stockWeek = bundle.getInt( STOCK_WEEK );
		hostile = bundle.getBoolean( HOSTILE );
	}
}
