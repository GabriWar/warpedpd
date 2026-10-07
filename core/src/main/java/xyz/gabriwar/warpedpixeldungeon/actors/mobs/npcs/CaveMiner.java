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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.food.SmallRation;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Gem;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLantern;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CaveMinerSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The dwarf who keeps a miners' camp in the caves (levels/overworld/CaveSites). He buys ore and
 * gems for half again what any shop up top pays (ORE_RATE) - straight into the purse, nothing
 * put by for buying back, so selling to him and buying back is no pump - sells a miner's
 * supplies (never ore: nothing in the world sells ore, Ores' economy), buys other goods like any
 * keeper, and knows what lies round his camp: every word of it from the world's own places
 * (CaveSites.rumours). Kept for good like every keeper (an NPC is never pruned from the parked
 * store); one who fled is replaced on the camp's next peopling.
 */
public class CaveMiner extends Shopkeeper {

	{
		spriteClass = CaveMinerSprite.class;
	}

	/** what he pays for ore and gems, against a shop's price */
	public static final float ORE_RATE = 1.5f;

	/** what he pays for a stack (or a lump) of ore or a gem */
	public static int orePrice( Item it ){
		return Math.round( it.value() * ORE_RATE );
	}

	public static boolean minersGoods( Item it ){
		return it instanceof Ore || it instanceof Gem;
	}

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		//a pick for a hero who came down without one
		if (Dungeon.hero != null && Dungeon.hero.belongings.getItem( Pickaxe.class ) == null){
			stock.add( new Pickaxe().identify( false ) );
		}
		stock.add( new Torch().quantity( 3 ) );
		stock.add( new SmallRation().quantity( 2 ) );
		Item way;
		switch (Random.Int( 3 )){
			case 0:  way = new ScrollOfMagicMapping(); break;
			case 1:  way = new ScrollOfTeleportation(); break;
			default: way = new PotionOfLantern(); break;
		}
		stock.add( way.identify( false ) );
		return stock;
	}

	@Override
	public String chatText(){
		return chatTextFor( null );
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( this, "talk" );
	}

	private boolean firstStock = false;

	/** His shelf is filled on his first act, when the level is live (Fisherman.openShop). */
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
	public boolean interact( Char c ){
		if (!(c instanceof Hero)) return true;
		//a co-op guest trades at his shelf and sells as at any shop (the premium is the host's
		//window alone)
		if (((Hero) c).isRemote) return super.interact( c );
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;
		//what he has heard, worked out here on the actor thread: the window only shows it
		final String news = Dungeon.level instanceof OverworldLevel
				? ((OverworldLevel) Dungeon.level).caveRumours( pos ) : Messages.get( this, "rumour_none" );
		final boolean buyback = !buybackItems.isEmpty();
		Game.runOnRenderThread( () -> {
			String[] options = buyback
					? new String[]{ Messages.get( CaveMiner.class, "sell_ore" ), Messages.get( CaveMiner.class, "sell_other" ),
							Messages.get( CaveMiner.class, "news" ), Messages.get( CaveMiner.class, "buyback" ) }
					: new String[]{ Messages.get( CaveMiner.class, "sell_ore" ), Messages.get( CaveMiner.class, "sell_other" ),
							Messages.get( CaveMiner.class, "news" ) };
			GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
					description() + "\n\n" + Messages.get( CaveMiner.class, "greeting" ), options ){
				@Override
				protected void onSelect( int index ){
					if (index == 0) sellOre( hero );
					else if (index == 1) Shopkeeper.sell();
					else if (index == 2) GameScene.show( new WndTitledMessage( new CaveMinerSprite(),
							Messages.titleCase( CaveMiner.this.name() ), news ) );
					else if (index == 3) showShopMenu( Messages.titleCase( CaveMiner.this.name() ), description(),
							chatText(), buybackItems, Dungeon.gold, false );
				}
			} );
		} );
		return true;
	}

	private void sellOre( final Hero hero ){
		GameScene.selectItem( new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( CaveMiner.class, "ore_prompt" );
			}
			@Override
			public Class<? extends Bag> preferredBag(){
				return Belongings.Backpack.class;
			}
			@Override
			public boolean itemSelectable( Item item ){
				return minersGoods( item ) && canSell( item );
			}
			@Override
			public void onSelect( Item item ){
				if (item != null && minersGoods( item ) && hero.isAlive()) offer( hero, item );
			}
		} );
	}

	//his price for it all, or for one, against what a shop would give
	private void offer( final Hero hero, final Item item ){
		final int all = orePrice( item ), one = Math.round( item.value() / (float) item.quantity() * ORE_RATE );
		String[] options = item.quantity() > 1
				? new String[]{ Messages.get( this, "ore_all", all ), Messages.get( this, "ore_one", one ), Messages.get( this, "ore_cancel" ) }
				: new String[]{ Messages.get( this, "ore_all", all ), Messages.get( this, "ore_cancel" ) };
		GameScene.show( new WndOptions( sprite(), Messages.titleCase( item.title() ),
				Messages.get( this, "ore_offer", all, item.value() ), options ){
			@Override
			protected void onSelect( int index ){
				if (index == 0) sell( hero, item, true );
				else if (index == 1 && item.quantity() > 1) sell( hero, item, false );
			}
		} );
	}

	/** Sells the stack (or one of it) to him at his price: straight into the purse (Shopkeeper.
	 *  paySale), none of it kept for buying back. Selling takes no time, as at any shop. */
	public int sell( Hero hero, Item item, boolean all ){
		if (!hero.belongings.contains( item )) return 0;
		Item sold = all ? item.detachAll( hero.belongings.backpack ) : item.detach( hero.belongings.backpack );
		if (sold == null) return 0;
		int price = orePrice( sold );
		Item.updateQuickslot();
		paySale( hero, price );
		GLog.i( Messages.get( this, "sold", price ) );
		hero.spend( -hero.cooldown() );
		return price;
	}

	private static final String FIRST_STOCK = "first_stock";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( FIRST_STOCK, firstStock );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		firstStock = bundle.contains( FIRST_STOCK ) && bundle.getBoolean( FIRST_STOCK );
	}
}
