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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BlackMarketDealer;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The black market dealer's counter: what he sells, the trade-up contract, the fence
 * and the rigged wheel. What he says above the buttons is the only place the sky's hand
 * in his prices ever shows, and it never says which way.
 */
public class WndBlackMarket extends WndOptions {

	private static final int WARES = 0, CONTRACT = 1, FENCE = 2, WHEEL = 3;

	private final BlackMarketDealer dealer;
	private final BlackMarket market;

	public WndBlackMarket( BlackMarketDealer dealer, BlackMarket market ){
		super( dealer.sprite(),
				Messages.titleCase( dealer.name() ),
				patter(),
				Messages.get( WndBlackMarket.class, "wares" ),
				Messages.get( WndBlackMarket.class, "contract" ),
				Messages.get( WndBlackMarket.class, "fence" ),
				Messages.get( WndBlackMarket.class, "wheel", BlackMarket.wheelPrice() ) );
		this.dealer = dealer;
		this.market = market;
	}

	//what the dealer has to say tonight. he talks about the night, never about the price
	private static String patter(){
		StringBuilder s = new StringBuilder( Messages.get( WndBlackMarket.class, "greet" ) );
		if (GameCalendar.weekday() == GameCalendar.Weekday.SHADOWDAY){
			s.append( "\n\n" ).append( Messages.get( WndBlackMarket.class, "sky_shadowday" ) );
		}
		if (GameCalendar.isFullMoon()){
			s.append( "\n\n" ).append( Messages.get( WndBlackMarket.class, "sky_fullmoon" ) );
		} else if (GameCalendar.isNewMoon()){
			s.append( "\n\n" ).append( Messages.get( WndBlackMarket.class, "sky_newmoon" ) );
		}
		WeatherState sky = ClimateManager.weatherState();
		if (sky == WeatherState.FOG){
			s.append( "\n\n" ).append( Messages.get( WndBlackMarket.class, "sky_fog" ) );
		} else if (sky == WeatherState.STORM){
			s.append( "\n\n" ).append( Messages.get( WndBlackMarket.class, "sky_storm" ) );
		}
		if (WarpedRooms.heat >= WarpedRooms.HEAT_LIMIT){
			s.append( "\n\n" ).append( Messages.get( WndBlackMarket.class, "heat_hunted" ) );
		} else if (WarpedRooms.heat == WarpedRooms.HEAT_LIMIT - 1){
			s.append( "\n\n" ).append( Messages.get( WndBlackMarket.class, "heat_close" ) );
		}
		return s.toString();
	}

	@Override
	protected boolean hasInfo( int index ){
		return index != WARES;
	}

	@Override
	protected void onInfo( int index ){
		String key = index == CONTRACT ? "contract" : index == FENCE ? "fence" : "wheel";
		GameScene.show( new WndTitledMessage( dealer.sprite(),
				Messages.titleCase( Messages.get( WndBlackMarket.class, key + "_title" ) ),
				Messages.get( WndBlackMarket.class, key + "_info" ) ) );
	}

	@Override
	protected void onSelect( int index ){
		switch (index){
			case WARES:
				GameScene.show( new WndMarketWares( dealer, market ) );
				break;
			case CONTRACT:
				GameScene.show( new WndTradeUp( dealer ) );
				break;
			case FENCE:
				GameScene.selectItem( fenceSelector );
				break;
			case WHEEL:
				GameScene.selectItem( wheelSelector );
				break;
		}
	}

	// ---------------------------------------------------------------- shared

	/** every deal with the market is noticed by somebody: one more point of heat, and a
	 *  word of warning the moment it becomes one too many */
	public static void drawHeat(){
		boolean wasSafe = WarpedRooms.heat < WarpedRooms.HEAT_LIMIT;
		WarpedRooms.addHeat( 1 );
		if (wasSafe && WarpedRooms.heat >= WarpedRooms.HEAT_LIMIT){
			GLog.n( Messages.get( WndBlackMarket.class, "heat_tripped" ) );
		}
	}

	public static void pay( int gold ){
		Dungeon.gold -= gold;
		Catalog.countUses( Gold.class, gold );
		WarpedRooms.paid( gold );
	}

	/** puts something the dealer hands over into the pack, or at the hero's feet */
	public static void handOver( Item item ){
		Hero hero = Dungeon.hero;
		if (!item.doPickUp( hero )){
			Dungeon.level.drop( item, hero.pos ).sprite.drop();
		}
	}

	// ---------------------------------------------------------------- the fence

	//what the fence will take: anything with a price that is not nailed to the hero.
	//armor with a seal on it and the run's unique items stay out, as they do in a shop
	private static boolean fenceable( Item item ){
		if (item.isEquipped( Dungeon.hero )) return false;
		if (item.unique && !item.stackable) return false;
		if (item instanceof Armor && ((Armor) item).checkSeal() != null) return false;
		if (item instanceof MasterworkCore || item instanceof Gold) return false;
		return item.value() > 0 || fenceCores( item ) > 0;
	}

	/** a legendary or exotic piece is paid for in cores, not coin */
	public static int fenceCores( Item item ){
		Quality q = Quality.of( item );
		if (q == null || !Quality.hasRarity( item )) return 0;
		if (q.rarity == Rarity.EXOTIC) return 2;
		if (q.rarity == Rarity.LEGENDARY) return 1;
		return 0;
	}

	/** half again what a lawful shop pays, and no questions about where it came from */
	public static int fenceGold( Item item ){
		return Math.round( item.value() * 1.5f );
	}

	private final WndBag.ItemSelector fenceSelector = new WndBag.ItemSelector(){
		@Override
		public String textPrompt(){
			return Messages.get( WndBlackMarket.class, "fence_prompt" );
		}
		@Override
		public Class<? extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}
		@Override
		public boolean itemSelectable( Item item ){
			return fenceable( item );
		}
		@Override
		public void onSelect( final Item item ){
			if (item == null) return;
			final int cores = fenceCores( item );
			final int gold = fenceGold( item );
			String offer = cores > 0
					? Messages.get( WndBlackMarket.class, "fence_offer_cores", item.name(), cores )
					: Messages.get( WndBlackMarket.class, "fence_offer_gold", item.name(), gold );
			GameScene.show( new WndOptions( dealer.sprite(),
					Messages.titleCase( Messages.get( WndBlackMarket.class, "fence_title" ) ),
					offer,
					Messages.get( WndBlackMarket.class, "fence_yes" ),
					Messages.get( WndBlackMarket.class, "fence_no" ) ){
				@Override
				protected void onSelect( int index ){
					if (index != 0) return;
					Hero hero = Dungeon.hero;
					if (!hero.belongings.contains( item ) || item.isEquipped( hero )) return;
					item.detachAll( hero.belongings.backpack );
					if (cores > 0){
						handOver( new MasterworkCore().quantity( cores ) );
					} else {
						handOver( new Gold( gold ) );
					}
					drawHeat();
					GLog.i( Messages.get( WndBlackMarket.class, "fence_done" ) );
				}
			} );
		}
	};

	// ---------------------------------------------------------------- the wheel

	private final WndBag.ItemSelector wheelSelector = new WndBag.ItemSelector(){
		@Override
		public String textPrompt(){
			return Messages.get( WndBlackMarket.class, "wheel_prompt" );
		}
		@Override
		public Class<? extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}
		@Override
		public boolean itemSelectable( Item item ){
			//the dealer keeps what the black pocket takes, so it has to be able to leave
			return MasterworkCore.workable( item ) && !item.isEquipped( Dungeon.hero );
		}
		@Override
		public void onSelect( Item item ){
			if (item == null) return;
			GameScene.show( new WndRiggedWheel( Dungeon.hero, item ) );
		}
	};
}
