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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BlackMarketDealer;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.CurrencyIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * The black market's trade-up contract. Three pieces of the same family and the same
 * rarity go over the counter, and one piece of that family comes back a rarity higher:
 * a random one, carrying the lowest upgrade level of the three so that nothing is
 * laundered upward. It is the one place a bag of spare uncommons is worth anything.
 *
 * Legendary is the top of it unless the floor is deep enough for exotics to exist at
 * all (Rarity.EXOTIC.weight), and the dealer's fee grows with the rarity asked for.
 */
public class WndTradeUp extends Window {

	private static final int WIDTH    = 130;
	private static final int SLOT     = 26;
	private static final float GAP    = 3;

	private final BlackMarketDealer dealer;
	private final Item[] given = new Item[3];
	private final ItemButton[] slots = new ItemButton[3];

	private final RenderedTextBlock outcome;
	private final RedButton sign;

	public WndTradeUp( BlackMarketDealer dealer ){
		super();
		this.dealer = dealer;

		CurrencyIndicator.showGold = true;

		IconTitle title = new IconTitle( dealer.sprite(), Messages.titleCase( Messages.get( this, "title" ) ) );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		RenderedTextBlock info = PixelScene.renderTextBlock( Messages.get( this, "info" ), 6 );
		info.maxWidth( WIDTH );
		info.setPos( 0, title.bottom() + GAP );
		add( info );

		float top = info.bottom() + 2 * GAP;
		float left = (WIDTH - 3 * SLOT - 2 * GAP) / 2f;
		for (int i = 0; i < 3; i++){
			final int index = i;
			slots[i] = new ItemButton(){
				@Override
				protected void onClick(){
					if (given[index] != null){
						given[index] = null;
						refresh();
					} else {
						GameScene.selectItem( selector( index ) );
					}
				}
			};
			slots[i].setRect( left + i * (SLOT + GAP), top, SLOT, SLOT );
			add( slots[i] );
		}

		outcome = PixelScene.renderTextBlock( 6 );
		outcome.maxWidth( WIDTH );
		outcome.setPos( 0, top + SLOT + 2 * GAP );
		add( outcome );

		sign = new RedButton( Messages.get( this, "sign" ) ){
			@Override
			protected void onClick(){
				sign();
			}
		};
		add( sign );

		refresh();
	}

	//the family and rarity the contract is being written for: whatever the first piece
	//on the counter is. null while the counter is empty
	private Item first(){
		for (Item item : given) if (item != null) return item;
		return null;
	}

	private static Rarity above( Rarity from ){
		if (from.ordinal() >= Rarity.EXOTIC.ordinal()) return null;
		Rarity next = Rarity.values()[from.ordinal() + 1];
		//an exotic cannot be made where an exotic could not be found
		if (next == Rarity.EXOTIC && Rarity.EXOTIC.weight( WarpedRooms.threat() ) <= 0) return null;
		return next;
	}

	private boolean fits( Item item ){
		if (!Quality.hasRarity( item ) || Quality.of( item ) == null) return false;
		if (item.isEquipped( Dungeon.hero )) return false;
		if (item.unique) return false;
		for (Item taken : given) if (taken == item) return false;
		if (above( Quality.of( item ).rarity ) == null) return false;
		Item lead = first();
		if (lead == null) return true;
		return Quality.family( item ) == Quality.family( lead )
				&& Quality.of( item ).rarity == Quality.of( lead ).rarity;
	}

	private WndBag.ItemSelector selector( final int index ){
		return new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( WndTradeUp.class, first() == null ? "pick_first" : "pick_more" );
			}
			@Override
			public Class<? extends Bag> preferredBag(){
				return Belongings.Backpack.class;
			}
			@Override
			public boolean itemSelectable( Item item ){
				return fits( item );
			}
			@Override
			public void onSelect( Item item ){
				if (item == null || !fits( item )) return;
				given[index] = item;
				refresh();
			}
		};
	}

	private int lowestLevel(){
		int lowest = Integer.MAX_VALUE;
		for (Item item : given){
			if (item != null) lowest = Math.min( lowest, item.level() );
		}
		return lowest == Integer.MAX_VALUE ? 0 : Math.max( 0, lowest );
	}

	private boolean complete(){
		return given[0] != null && given[1] != null && given[2] != null;
	}

	private void refresh(){
		for (int i = 0; i < 3; i++){
			if (given[i] != null) slots[i].item( given[i] );
			else slots[i].clear();
		}

		Item lead = first();
		Rarity to = lead == null ? null : above( Quality.of( lead ).rarity );
		int fee = to == null ? 0 : BlackMarket.contractFee( to );

		if (lead == null){
			outcome.text( Messages.get( this, "empty" ) );
			outcome.resetColor();
		} else {
			outcome.text( Messages.get( this, "outcome",
					Quality.of( lead ).rarity.title(), family( lead ), to.title(), lowestLevel(), fee ) );
			outcome.hardlight( to.color );
		}

		sign.setRect( 0, outcome.bottom() + GAP, WIDTH, 18 );
		sign.enable( complete() && fee <= Dungeon.gold );
		resize( WIDTH, (int) sign.bottom() );
	}

	private String family( Item item ){
		return Messages.get( this, "family_" + Quality.family( item ).name().toLowerCase( java.util.Locale.ENGLISH ) );
	}

	private void sign(){
		if (!complete()) return;
		Hero hero = Dungeon.hero;
		Rarity to = above( Quality.of( given[0] ).rarity );
		if (to == null) return;
		int fee = BlackMarket.contractFee( to );
		if (fee > Dungeon.gold) return;
		for (Item item : given){
			if (!hero.belongings.contains( item ) || item.isEquipped( hero )) return;
		}

		Quality.Family family = Quality.family( given[0] );
		int level = lowestLevel();
		//one piece from each stack: a bundle of darts is still one dart on the counter
		for (Item item : given) item.detach( hero.belongings.backpack );

		Item made;
		switch (family){
			case ARMOR:   made = Generator.random( Generator.Category.ARMOR ); break;
			case MISSILE: made = Generator.randomUsingDefaults( Generator.Category.MISSILE ); break;
			case WAND:    made = Generator.randomUsingDefaults( Generator.Category.WAND ); break;
			default:      made = Generator.randomUsingDefaults( Generator.Category.WEAPON ); break;
		}
		made.cursed = false;
		made.level( level );
		WarpedRooms.forceQuality( made, to, null );
		made.identify();

		WndBlackMarket.pay( fee );
		WndBlackMarket.drawHeat();
		Sample.INSTANCE.play( Assets.Sounds.EVOKE );
		GLog.p( Messages.get( this, "signed", made.name() ) );
		hide();
		WndBlackMarket.handOver( made );
	}

	@Override
	public void hide(){
		super.hide();
		CurrencyIndicator.showGold = false;
	}
}
