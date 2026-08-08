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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BlobImmunity;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ShopkeeperSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.CurrencyIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTradeItem;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class Shopkeeper extends NPC {

	{
		spriteClass = ShopkeeperSprite.class;

		properties.add(Property.IMMOVABLE);
	}

	public static int MAX_BUYBACK_HISTORY = 3;
	public ArrayList<Item> buybackItems = new ArrayList<>();

	private int turnsSinceHarmed = -1;

	@Override
	public Notes.Landmark landmark() {
		return Notes.Landmark.SHOP;
	}

	/** Are we above ground, where the shops keep hours? Keyed off (depth, branch)
	 *  rather than the level class on purpose: a multiplayer client holds a generic
	 *  mirror level, so an instanceof test evaluates the opposite way there and the
	 *  two machines disagree about whether the till is open. depth and branch are
	 *  both host-authoritative and on the wire, so this reads the same on both. */
	private static boolean surfaceTradingHours(){
		return (Dungeon.depth == xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.DEPTH
					&& Dungeon.branch == 0)
				|| Dungeon.branch == xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel.BRANCH;
	}

	/** Night on the surface: the town and the settlements go to bed. Every shop
	 *  up there is shut and the folk answer with a sleepy line. Shared by all
	 *  surface NPCs; the dungeon shops below never close. */
	public static boolean closedForNight(){
		return xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.isNight()
				&& surfaceTradingHours();
	}

	//every shop restocks on a timer: mostly once an in-game month (30 days of
	//2500 turns), some twice or three times. rolled once per shopkeeper
	public static final float MONTH_TURNS = 30f * 2500f;
	protected float restockInterval = 0;
	protected float lastRestock = -1;

	protected float restockClock(){
		return xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.clock();
	}

	@Override
	protected boolean act() {

		if (restockInterval == 0){
			int roll = com.watabou.utils.Random.Int( 100 );
			restockInterval = roll < 60 ? MONTH_TURNS
					: roll < 85 ? MONTH_TURNS / 2f : MONTH_TURNS / 3f;
		}
		if (lastRestock < 0){
			lastRestock = restockClock();
		} else if (restockClock() - lastRestock >= restockInterval
				//the town shop has its own per-purchase rotation system
				&& !(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.TownShopLevel)){
			lastRestock = restockClock();
			restock();
		}

		if (turnsSinceHarmed >= 0){
			turnsSinceHarmed ++;
		}

		sprite.turnTo( pos, Dungeon.hero.pos );
		spend( TICK );
		return super.act();
	}
	
	/** Fresh goods: fills free cells near the keeper with new FOR_SALE stock,
	 *  up to a modest shelf size. Old unsold goods stay - hence "fresh". */
	public void restock(){
		java.util.ArrayList<Integer> spots = new java.util.ArrayList<>();
		for (int ofs : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = pos + ofs;
			if (c >= 0 && c < Dungeon.level.length()
					&& Dungeon.level.passable[c]
					&& Dungeon.level.heaps.get( c ) == null){
				spots.add( c );
			}
		}
		java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.items.Item> fresh = stockSource();
		com.watabou.utils.Random.shuffle( fresh );
		int placed = 0;
		for (xyz.gabriwar.warpedpixeldungeon.items.Item it : fresh){
			if (spots.isEmpty() || placed >= 4) break;
			int c = spots.remove( com.watabou.utils.Random.Int( spots.size() ) );
			Dungeon.level.drop( it, c ).type = xyz.gabriwar.warpedpixeldungeon.items.Heap.Type.FOR_SALE;
			placed++;
		}
		//heroFOV alone is the host's eyes: a client standing right next to the stall
		//would hear nothing. ask whether ANY online hero can see the keeper
		if (placed > 0 && xyz.gabriwar.warpedpixeldungeon.net.NetManager.anyHeroSees( pos )){
			yell( Messages.get( Shopkeeper.class, "restock" ) );
		}
	}

	/** Where this shop's goods come from - subclasses specialize. The base
	 *  source is consumables only: ShopRoom.generateItems has one-shot side
	 *  effects (hourglass sandbag accounting, limited drops) that must not
	 *  re-run on a timer. */
	protected java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.items.Item> stockSource(){
		java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.items.Item> stock = new java.util.ArrayList<>();
		for (int i = 0; i < 3; i++){
			xyz.gabriwar.warpedpixeldungeon.items.Item it
					= xyz.gabriwar.warpedpixeldungeon.items.Generator.random(
							xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.POTION );
			if (it != null) stock.add( it );
			it = xyz.gabriwar.warpedpixeldungeon.items.Generator.random(
					xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.SCROLL );
			if (it != null) stock.add( it );
		}
		stock.add( new xyz.gabriwar.warpedpixeldungeon.items.food.Food() );
		return stock;
	}

	private static final String RESTOCK_INT  = "restock_interval";
	private static final String LAST_RESTOCK = "last_restock";

	@Override
	public void damage( int dmg, Object src ) {
		processHarm();
	}
	
	@Override
	public boolean add( Buff buff ) {
		if (buff.type == Buff.buffType.NEGATIVE){
			processHarm();
		}
		return false;
	}

	public void processHarm(){

		//do nothing if the shopkeeper is out of the hero's FOV
		if (!Dungeon.level.heroFOV[pos]){
			return;
		}

		if (turnsSinceHarmed == -1){
			turnsSinceHarmed = 0;
			yell(Messages.get(this, "warn"));

			//use a new actor as we can't clear the gas while we're in the middle of processing it
			Actor.add(new Actor() {
				{
					actPriority = VFX_PRIO;
				}

				@Override
				protected boolean act() {
					//cleanses all harmful blobs in the shop
					ArrayList<Blob> blobs = new ArrayList<>();
					for (Class c : new BlobImmunity().immunities()){
						Blob b = Dungeon.level.blobs.get(c);
						if (b != null && b.volume > 0){
							blobs.add(b);
						}
					}

					PathFinder.buildDistanceMap( pos, BArray.not( Dungeon.level.solid, null ), 4 );

					for (int i=0; i < Dungeon.level.length(); i++) {
						if (PathFinder.distance[i] < Integer.MAX_VALUE) {

							boolean affected = false;
							for (Blob blob : blobs) {
								if (blob.cur[i] > 0) {
									blob.clear(i);
									affected = true;
								}
							}

							if (affected && Dungeon.level.heroFOV[i]) {
								CellEmitter.get( i ).burst( Speck.factory( Speck.DISCOVER ), 2 );
							}

						}
					}
					Actor.remove(this);
					return true;
				}
			});

		//There is a 1 turn buffer before more damage/debuffs make the shopkeeper flee
		//This is mainly to prevent stacked effects from causing an instant flee
		} else if (turnsSinceHarmed >= 1) {
			flee();
		}
	}
	
	public void flee() {
		destroy();

		Notes.remove( landmark() );
		GLog.newLine();
		GLog.n(Messages.get(this, "flee"));

		if (sprite != null) {
			sprite.killAndErase();
			CellEmitter.get(pos).burst(ElmoParticle.FACTORY, 6);
		}

		// Portal at this depth is permanently disabled when its guardian is gone.
		xyz.gabriwar.warpedpixeldungeon.Portals.markDead(Dungeon.depth);
		if (Dungeon.level != null) {
			for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob m :
					Dungeon.level.mobs.toArray(new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0])) {
				if (m instanceof PortalGate) {
					((PortalGate) m).setState(PortalGate.State.DEAD);
				}
			}
		}
	}
	
	@Override
	public void destroy() {
		super.destroy();
		for (Heap heap: Dungeon.level.heaps.valueList()) {
			if (heap.type == Heap.Type.FOR_SALE) {
				if (WarpedPixelDungeon.scene() instanceof GameScene) {
					CellEmitter.get(heap.pos).burst(ElmoParticle.FACTORY, 4);
				}
				if (heap.size() == 1) {
					heap.destroy();
				} else {
					heap.items.remove(heap.size()-1);
					heap.type = Heap.Type.HEAP;
				}
			}
		}
	}
	
	@Override
	public boolean reset() {
		return true;
	}

	//shopkeepers are greedy!
	public static int sellPrice(Item item){
		//special surface levels sit at huge depth numbers (overworld = 97);
		//price them like the late-game city instead of 100x base value.
		//keyed off depth/branch, not the level class, so a multiplayer client
		//quotes the same price the host will charge (see surfaceTradingHours)
		int depth = Dungeon.depth;
		if (depth == xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.DEPTH
				&& Dungeon.branch == 0){
			depth = 21;
		}
		return item.value() * 5 * (depth / 5 + 1);
	}

	// --- Host-side handlers for client-routed shop actions (run on actor thread) ---

	// Which keeper a player is currently trading with, hero id -> keeper actor id.
	// The surface carries a dozen Shopkeepers at once (settlement vendors, fishermen,
	// caravans), so "the first Shopkeeper on the level" would stash a fisherman's sale
	// on some villager's buyback shelf and redeem a buyback index against a list the
	// client never saw. Set when the shop window is opened, so every follow-up action
	// lands on the keeper the player is actually standing in front of. Ids, not the
	// objects: a keeper held here would pin its sprite and through it the whole level
	// it belongs to, long after the party walked out of the shop.
	private static final java.util.HashMap<Integer, Integer> tradingWith = new java.util.HashMap<>();

	/** The keeper this hero opened a shop window with, if they're still on this level. */
	private static Shopkeeper findShopkeeper(Hero hero) {
		if (Dungeon.level == null) return null;
		if (hero != null) {
			Integer remembered = tradingWith.get(hero.id());
			if (remembered != null) {
				xyz.gabriwar.warpedpixeldungeon.actors.Actor a =
						xyz.gabriwar.warpedpixeldungeon.actors.Actor.findById(remembered);
				if (a instanceof Shopkeeper && Dungeon.level.mobs.contains(a)) return (Shopkeeper) a;
				tradingWith.remove(hero.id());
			}
		}
		return nearestShopkeeper(hero != null ? hero.pos : -1);
	}

	/** Closest keeper to a cell — the fallback when we have no remembered pairing
	 *  (a reconnect, a keeper that walked off, the host's own local sell window).
	 *  Walks Actor.chars(), a synchronized snapshot, NOT the live level.mobs set:
	 *  WndTradeItem calls this from the render thread while the host's actor loop
	 *  (woken by any client's action) may be adding or removing mobs. */
	public static Shopkeeper nearestShopkeeper(int fromCell) {
		if (Dungeon.level == null) return null;
		Shopkeeper best = null;
		int bestDist = Integer.MAX_VALUE;
		for (xyz.gabriwar.warpedpixeldungeon.actors.Char ch
				: xyz.gabriwar.warpedpixeldungeon.actors.Actor.chars()) {
			if (!(ch instanceof Shopkeeper)) continue;
			if (fromCell < 0) return (Shopkeeper) ch;
			int d = Dungeon.level.distance(ch.pos, fromCell);
			if (d < bestDist) {
				bestDist = d;
				best = (Shopkeeper) ch;
			}
		}
		return best;
	}

	/** Remember who a hero is trading with, so their sells and buybacks hit the
	 *  right shelf. Called when a shop window is opened for that hero. */
	private void beginTrade(Hero hero) {
		if (hero != null) tradingWith.put(hero.id(), id());
	}

	/** Pay a remote player for a sale. Mirrors the local sell path's Gold pickup on the
	 *  ledger side (gold + goldCollected) without its sprite/time side effects, which
	 *  can't run mid-act() for a net hero. hostBuyback debits both again on a refund;
	 *  crediting only Dungeon.gold made goldCollected drift negative in a big party. */
	private static void credit(int amount) {
		if (amount <= 0) return;
		Dungeon.gold += amount;
		Statistics.goldCollected += amount;
		xyz.gabriwar.warpedpixeldungeon.Badges.validateGoldCollected();
	}

	private void stashForBuyback(Item item) {
		if (!canSell(item)) return;
		buybackItems.add(item);
		while (buybackItems.size() > MAX_BUYBACK_HISTORY) {
			buybackItems.remove(0);
		}
	}

	/** Client selected an item to sell from their bag. */
	public static void hostSell(Hero hero, String itemName) {
		if (hero == null || itemName == null || itemName.isEmpty() || closedForNight()) return;
		Item it = hero.findInventoryItemByName(itemName);
		if (it == null) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
					"[NET-HOST] hostSell: item not found in netHero inv name=" + itemName);
			return;
		}
		Item one = it.detach(hero.belongings.backpack);
		if (one == null) return;
		credit(one.value());
		// Stash for buyback so the client can re-buy if they regret it.
		Shopkeeper sk = findShopkeeper(hero);
		if (sk != null) sk.stashForBuyback(one);
	}

	/** Client sold a whole stack from their bag. */
	public static void hostSellAll(Hero hero, String itemName) {
		if (hero == null || itemName == null || itemName.isEmpty() || closedForNight()) return;
		Item it = hero.findInventoryItemByName(itemName);
		if (it == null) return;
		if (it.isEquipped(hero)
				&& !((xyz.gabriwar.warpedpixeldungeon.items.EquipableItem) it).doUnequip(hero, false)) {
			return;
		}
		int value = it.value();
		if (it instanceof MissileWeapon && it.isUpgradable()) {
			Buff.affect(hero, MissileWeapon.UpgradedSetTracker.class)
					.levelThresholds.put(((MissileWeapon) it).setID, Integer.MAX_VALUE);
		}
		it.detachAll(hero.belongings.backpack);
		credit(value);
		Shopkeeper sk = findShopkeeper(hero);
		if (sk != null) sk.stashForBuyback(it);
	}

	/** Client clicked a buyback row. Validates gold + index host-side. */
	public static void hostBuyback(Hero hero, int index) {
		if (hero == null) return;
		Shopkeeper sk = findShopkeeper(hero);
		if (sk == null) return;
		if (index < 0 || index >= sk.buybackItems.size()) return;
		Item returned = sk.buybackItems.get(index);
		if (Dungeon.gold < returned.value()) return;
		Dungeon.gold -= returned.value();
		Statistics.goldCollected -= returned.value();
		sk.buybackItems.remove(index);
		if (returned instanceof MissileWeapon && returned.isUpgradable()){
			Buff.affect(hero, MissileWeapon.UpgradedSetTracker.class)
					.levelThresholds.put(((MissileWeapon) returned).setID, returned.level());
		}
		if (!returned.doPickUp(hero)) {
			Dungeon.level.drop(returned, hero.pos);
		}
	}

	/** The shop main menu — identical for the host's own hero and remote players.
	 *  Sell/Talk run locally on both (WndTradeItem is client-aware); buyback applies
	 *  locally for the host and routes a typed action for remote players. */
	public static void showShopMenu(final String title, final String body, final String chat,
									final java.util.ArrayList<Item> buybacks, final int gold,
									final boolean remote) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				String[] options = new String[2 + buybacks.size()];
				int maxLen = PixelScene.landscape() ? 30 : 25;
				options[0] = Messages.get(Shopkeeper.class, "sell");
				options[1] = Messages.get(Shopkeeper.class, "talk");
				for (int i = 0; i < buybacks.size(); i++) {
					String o = Messages.get(Heap.class, "for_sale", buybacks.get(i).value(),
							Messages.titleCase(buybacks.get(i).title()));
					if (o.length() > maxLen) o = o.substring(0, maxLen - 3) + "...";
					options[2 + i] = o;
				}
				CurrencyIndicator.showGold = true;
				GameScene.show(new WndOptions(new ShopkeeperSprite(), title, body, options) {
					@Override
					protected void onSelect(int index) {
						super.onSelect(index);
						if (index == 0) {
							sell();
						} else if (index == 1) {
							GameScene.show(new WndTitledMessage(new ShopkeeperSprite(), title, chat));
						} else if (index > 1) {
							if (remote) {
								xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendPlayerActionTyped("shop_buyback", index - 2);
							} else {
								hostBuyback(Dungeon.hero, index - 2);
							}
						}
					}

					@Override
					protected boolean enabled(int index) {
						if (index > 1) return gold >= buybacks.get(index - 2).value();
						return super.enabled(index);
					}

					@Override
					protected boolean hasIcon(int index) {
						return index > 1;
					}

					@Override
					protected Image getIcon(int index) {
						return index > 1 ? new ItemSprite(buybacks.get(index - 2)) : null;
					}

					@Override
					public void hide() {
						super.hide();
						CurrencyIndicator.showGold = false;
					}
				});
			}
		});
	}

	/** Client confirmed a FOR_SALE heap purchase. Re-validates adjacency + funds. */
	public static void hostBuyFromHeap(Hero hero, int cell) {
		if (hero == null || Dungeon.level == null) return;
		xyz.gabriwar.warpedpixeldungeon.items.Heap heap = Dungeon.level.heaps.get(cell);
		if (heap == null || heap.type != xyz.gabriwar.warpedpixeldungeon.items.Heap.Type.FOR_SALE) return;
		if (closedForNight()) {
			GLog.w(Messages.get(WndTradeItem.class, "closed"));
			return;
		}
		// Only allow purchase from adjacent/own cell — same constraint as normal pickup.
		if (cell != hero.pos && !Dungeon.level.adjacent(cell, hero.pos)) return;
		Item item = heap.peek();
		if (item == null) return;
		int price = sellPrice(item);
		if (Dungeon.gold < price) {
			GLog.w(Messages.get(WndTradeItem.class, "no_gold"));
			return;
		}
		Dungeon.gold -= price;
		Item bought = heap.pickUp();
		if (bought == null) return;
		bought.identify();
		if (!bought.doPickUp(hero)) {
			Dungeon.level.drop(bought, hero.pos);
		}
	}
	
	public static WndBag sell() {
		return GameScene.selectItem( itemSelector );
	}

	public static boolean canSell(Item item){
		if (item.value() <= 0)                                              return false;
		if (item.unique && !item.stackable)                                 return false;
		if (item instanceof Armor && ((Armor) item).checkSeal() != null)    return false;
		if (item.isEquipped(Dungeon.hero) && item.cursed)                   return false;
		return true;
	}

	private static WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(Shopkeeper.class, "sell");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return Shopkeeper.canSell(item);
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && Dungeon.hero != null && Dungeon.hero.isAlive()) {
				WndBag parentWnd = sell();
				GameScene.show( new WndTradeItem( item, parentWnd ) );
			}
		}
	};

	@Override
	public boolean interact(Char c) {
		if (!(c instanceof Hero)) return true;
		Hero interactor = (Hero) c;

		if (closedForNight()) {
			// yell() lands on the host's screen. A remote player would just see their
			// click do nothing, so send the same line down their own log instead.
			if (interactor.isRemote) {
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( interactor,
						Messages.get( Shopkeeper.class, "closed" ) );
			} else {
				yell( Messages.get( Shopkeeper.class, "closed" ) );
			}
			return true;
		}

		// Remote (netHero) — ship the data and let the client open the SAME shop window
		// (showShopMenu) locally. Sell/Talk run client-side (WndTradeItem is client-aware);
		// buyback rides back as a typed action applied on the actor thread.
		if (interactor.isRemote) {
			beginTrade( interactor );
			try {
				org.json.JSONObject payload = new org.json.JSONObject();
				payload.put("title", Messages.titleCase(name()));
				payload.put("desc", description());
				payload.put("chat", chatTextFor(interactor));
				payload.put("gold", Dungeon.gold);
				org.json.JSONArray bbArr = new org.json.JSONArray();
				for (int i = 0; i < buybackItems.size(); i++) {
					Item bi = buybackItems.get(i);
					org.json.JSONObject jb = new org.json.JSONObject();
					jb.put("name", bi.title());
					jb.put("image", bi.image());
					jb.put("value", bi.value());
					jb.put("lvl", bi.level());
					bbArr.put(jb);
				}
				payload.put("buybacks", bbArr);
				xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.request(
						interactor,
						xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_SHOPKEEPER,
						payload, false);
			} catch (Exception ignored) {}
			return true;
		}

		if (c != Dungeon.hero) return true;
		beginTrade( interactor );
		showShopMenu(Messages.titleCase(name()), description(), chatText(), buybackItems, Dungeon.gold, false);
		return true;
	}

	public String chatText(){
		return chatTextFor(Dungeon.hero);
	}

	/** Variant of {@link #chatText()} that picks the per-class line from a given hero.
	 *  Lets netHero shop interactions use the netHero's class for the intro line. */
	public String chatTextFor(Hero h){
		if (h != null && h.buff(AscensionChallenge.class) != null){
			return Messages.get(this, "talk_ascent");
		}
		switch (Dungeon.depth){
			case 6: default:
				return Messages.get(this, "talk_prison_intro") + "\n\n"
						+ Messages.get(this, "talk_prison_"
								+ (h != null ? h.heroClass.name() : "warrior"));
			case 11:
				return Messages.get(this, "talk_caves");
			case 16:
				return Messages.get(this, "talk_city");
			case 20:
				return Messages.get(this, "talk_halls");
		}
	}

	public static String BUYBACK_ITEMS = "buyback_items";

	public static String TURNS_SINCE_HARMED = "turns_since_harmed";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( RESTOCK_INT, restockInterval );
		bundle.put( LAST_RESTOCK, lastRestock );
		bundle.put(BUYBACK_ITEMS, buybackItems);
		bundle.put(TURNS_SINCE_HARMED, turnsSinceHarmed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		restockInterval = bundle.getFloat( RESTOCK_INT );
		lastRestock = bundle.contains( LAST_RESTOCK ) ? bundle.getFloat( LAST_RESTOCK ) : -1;
		buybackItems.clear();
		if (bundle.contains(BUYBACK_ITEMS)){
			for (Bundlable i : bundle.getCollection(BUYBACK_ITEMS)){
				buybackItems.add((Item) i);
			}
		}
		turnsSinceHarmed = bundle.contains(TURNS_SINCE_HARMED) ? bundle.getInt(TURNS_SINCE_HARMED) : -1;
	}
}
