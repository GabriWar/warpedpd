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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.MasterThievesArmband;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.CurrencyIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class WndTradeItem extends WndInfoItem {

	private static final float GAP		= 2;
	private static final int BTN_HEIGHT	= 18;

	private WndBag owner;

	private boolean selling = false;

	//selling
	public WndTradeItem( final Item item, WndBag owner ) {

		super(item);

		selling = true;

		this.owner = owner;

		float pos = height;

		//the keeper we're trading with: the nearest one, not merely the first on the
		//level. the surface carries a dozen of them at once (settlement vendors,
		//fishermen, caravans), and picking any of them would stash the sale on a
		//stranger's buyback shelf
		final Shopkeeper finalShop = Shopkeeper.nearestShopkeeper(
				Dungeon.hero != null ? Dungeon.hero.pos : -1);

		if (item.quantity() == 1 || (item instanceof MissileWeapon && item.isUpgradable())) {

			if (item instanceof MissileWeapon && ((MissileWeapon) item).extraThrownLeft){
				RenderedTextBlock warn = PixelScene.renderTextBlock(Messages.get(WndUpgrade.class, "thrown_dust"), 6);
				warn.hardlight(CharSprite.WARNING);
				warn.maxWidth(this.width);
				warn.setPos(0, pos + GAP);
				add(warn);
				pos = warn.bottom();
			}

			RedButton btnSell = new RedButton( Messages.get(this, "sell", item.value()) ) {
				@Override
				protected void onClick() {
					sell( item, finalShop);
					hide();
				}
			};
			btnSell.setRect( 0, pos + GAP, width, BTN_HEIGHT );
			btnSell.icon(new ItemSprite(ItemSpriteSheet.GOLD));
			add( btnSell );

			pos = btnSell.bottom();

		} else {

			int priceAll= item.value();
			RedButton btnSell1 = new RedButton( Messages.get(this, "sell_1", priceAll / item.quantity()) ) {
				@Override
				protected void onClick() {
					sellOne( item, finalShop );
					hide();
				}
			};
			btnSell1.setRect( 0, pos + GAP, width, BTN_HEIGHT );
			btnSell1.icon(new ItemSprite(ItemSpriteSheet.GOLD));
			add( btnSell1 );
			RedButton btnSellAll = new RedButton( Messages.get(this, "sell_all", priceAll ) ) {
				@Override
				protected void onClick() {
					sell( item, finalShop );
					hide();
				}
			};
			btnSellAll.setRect( 0, btnSell1.bottom() + 1, width, BTN_HEIGHT );
			btnSellAll.icon(new ItemSprite(ItemSpriteSheet.GOLD));
			add( btnSellAll );

			pos = btnSellAll.bottom();

		}

		resize( width, (int)pos );
	}

	//buying
	public WndTradeItem( final Heap heap ) {

		super(heap);

		selling = false;
		CurrencyIndicator.showGold = true;

		Item item = heap.peek();

		float pos = height;

		final int price = Shopkeeper.sellPrice( item );

		//surface shops shut at night: the goods stay on show, the till is closed
		final boolean closed = Shopkeeper.closedForNight();
		if (closed) {
			RenderedTextBlock warn = PixelScene.renderTextBlock(Messages.get(this, "closed"), 6);
			warn.hardlight(CharSprite.WARNING);
			warn.maxWidth(this.width);
			warn.setPos(0, pos + GAP);
			add(warn);
			pos = warn.bottom();
		}

		RedButton btnBuy = new RedButton( Messages.get(this, "buy", price) ) {
			@Override
			protected void onClick() {
				hide();
				buy( heap );
			}
		};
		btnBuy.setRect( 0, pos + GAP, width, BTN_HEIGHT );
		btnBuy.icon(new ItemSprite(ItemSpriteSheet.GOLD));
		btnBuy.enable( !closed && price <= Dungeon.gold );
		add( btnBuy );

		pos = btnBuy.bottom();

		final MasterThievesArmband.Thievery thievery = Dungeon.hero.buff(MasterThievesArmband.Thievery.class);
		if (thievery != null && !thievery.isCursed() && thievery.chargesToUse(item) > 0) {
			final float chance = thievery.stealChance(item);
			final int chargesToUse = thievery.chargesToUse(item);
			RedButton btnSteal = new RedButton(Messages.get(this, "steal", Math.min(100, (int) (chance * 100)), chargesToUse), 6) {
				@Override
				protected void onClick() {
					if (chance >= 1){
						thievery.steal(item);
						Hero hero = Dungeon.hero;
						Item item = heap.pickUp();
						hide();

						if (!item.doPickUp(hero)) {
							Dungeon.level.drop(item, heap.pos).sprite.drop();
						}
					} else {
						GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.ARTIFACT_ARMBAND),
								Messages.titleCase(Messages.get(MasterThievesArmband.class, "name")),
								Messages.get(WndTradeItem.class, "steal_warn"),
								Messages.get(WndTradeItem.class, "steal_warn_yes"),
								Messages.get(WndTradeItem.class, "steal_warn_no")){
							@Override
							protected void onSelect(int index) {
								super.onSelect(index);
								if (index == 0){
									if (thievery.steal(item)) {
										Hero hero = Dungeon.hero;
										Item item = heap.pickUp();
										WndTradeItem.this.hide();

										if (!item.doPickUp(hero)) {
											Dungeon.level.drop(item, heap.pos).sprite.drop();
										}
									} else {
										for (Mob mob : Dungeon.level.mobs) {
											if (mob instanceof Shopkeeper) {
												mob.yell(Messages.get(mob, "thief"));
												((Shopkeeper) mob).flee();
												break;
											}
										}
										WndTradeItem.this.hide();
									}
								}
							}
						});
					}
				}
			};
			btnSteal.setRect(0, pos + 1, width, BTN_HEIGHT);
			btnSteal.icon(new ItemSprite(ItemSpriteSheet.ARTIFACT_ARMBAND));
			add(btnSteal);

			pos = btnSteal.bottom();

		}

		resize(width, (int) pos);
	}
	
	@Override
	public void hide() {
		
		super.hide();
		CurrencyIndicator.showGold = false;
		
		if (owner != null) {
			owner.hide();
		}
		if (selling) Shopkeeper.sell();
	}

	public static void sell( Item item ) {
		sell(item, null);
	}

	public static void sell( Item item, Shopkeeper shop ) {

		if (Shopkeeper.closedForNight()) {
			GLog.w( Messages.get(WndTradeItem.class, "closed") );
			return;
		}

		// Client: the shop runs on the host. Ship the whole-stack sale and let the host
		// apply it to this player's belongings (same window, just routed).
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isPlayer()) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendInventoryAction("shop_sell_all", item.name(), -1);
			return;
		}

		Hero hero = Dungeon.hero;

		if (item.isEquipped( hero ) && !((EquipableItem)item).doUnequip( hero, false )) {
			return;
		}
		item.detachAll( hero.belongings.backpack );

		if (item instanceof MissileWeapon && item.isUpgradable()){
			Buff.affect(hero, MissileWeapon.UpgradedSetTracker.class).levelThresholds.put(((MissileWeapon) item).setID, Integer.MAX_VALUE);
		}

		//selling items in the sell interface doesn't spend time
		hero.spend(-hero.cooldown());

		new Gold( item.value() ).doPickUp( hero );

		if (shop != null){
			shop.buybackItems.add(item);
			while (shop.buybackItems.size() > Shopkeeper.MAX_BUYBACK_HISTORY){
				shop.buybackItems.remove(0);
			}
		}
	}

	public static void sellOne( Item item ) {
		sellOne( item, null );
	}

	public static void sellOne( Item item, Shopkeeper shop ) {

		if (Shopkeeper.closedForNight()) {
			GLog.w( Messages.get(WndTradeItem.class, "closed") );
			return;
		}

		// Client: ship a single-unit sale to the host.
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isPlayer()) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendInventoryAction("shop_sell", item.name(), -1);
			return;
		}

		if (item.quantity() <= 1) {
			sell( item, shop );
		} else {
			
			Hero hero = Dungeon.hero;
			
			item = item.detach( hero.belongings.backpack );

			//selling items in the sell interface doesn't spend time
			hero.spend(-hero.cooldown());

			new Gold( item.value() ).doPickUp( hero );

			if (shop != null){
				shop.buybackItems.add(item);
				while (shop.buybackItems.size() > Shopkeeper.MAX_BUYBACK_HISTORY){
					shop.buybackItems.remove(0);
				}
			}
		}
	}
	
	private void buy( Heap heap ) {

		if (Shopkeeper.closedForNight()) {
			GLog.w( Messages.get(WndTradeItem.class, "closed") );
			return;
		}
		
		Item item = heap.pickUp();
		if (item == null) return;
		
		int price = Shopkeeper.sellPrice( item );
		Dungeon.gold -= price;
		Catalog.countUses(Gold.class, price);
		
		if (!item.doPickUp( Dungeon.hero )) {
			Dungeon.level.drop( item, heap.pos ).sprite.drop();
		}

		//the town store rotates its whole stock after every purchase; a bought
		//orb of zot leaves the catalog forever
		if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.OrbOfZot){
			Dungeon.orbofzotshopsold = true;
		}
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.TownShopLevel){
			((xyz.gabriwar.warpedpixeldungeon.levels.TownShopLevel) Dungeon.level).rotateStock();
		}
	}
}
