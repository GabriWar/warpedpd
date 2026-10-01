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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BlackMarketDealer;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.CurrencyIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

/**
 * What the black market dealer has under the counter: one row per offer, its picture
 * (tap it to look the thing over - gear comes unidentified, and a curse on it shows
 * nowhere), its name in its rarity's colour, and the price as the button that buys it.
 */
public class WndMarketWares extends Window {

	private static final int WIDTH      = 140;
	private static final int ROW_HEIGHT = 18;
	private static final int BTN_WIDTH  = 44;
	private static final float GAP      = 2;

	private final BlackMarketDealer dealer;
	private final BlackMarket market;

	public WndMarketWares( BlackMarketDealer dealer, BlackMarket market ){
		super();
		this.dealer = dealer;
		this.market = market;

		CurrencyIndicator.showGold = true;

		IconTitle title = new IconTitle( dealer.sprite(), Messages.titleCase( Messages.get( this, "title" ) ) );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		RenderedTextBlock terms = PixelScene.renderTextBlock( Messages.get( this, "terms" ), 6 );
		terms.maxWidth( WIDTH );
		terms.setPos( 0, title.bottom() + GAP );
		add( terms );

		float pos = terms.bottom() + 2 * GAP;
		boolean any = false;
		for (final BlackMarket.Offer offer : market.stock){
			if (offer.sold) continue;
			any = true;
			pos = addRow( offer, pos ) + GAP;
		}

		if (!any){
			RenderedTextBlock empty = PixelScene.renderTextBlock( Messages.get( this, "sold_out" ), 6 );
			empty.maxWidth( WIDTH );
			empty.setPos( 0, pos );
			add( empty );
			pos = empty.bottom() + GAP;
		}

		resize( WIDTH, (int) (pos - GAP) );
	}

	private float addRow( final BlackMarket.Offer offer, float pos ){
		final int price = BlackMarket.priceFor( offer, Dungeon.hero );

		ItemButton icon = new ItemButton(){
			@Override
			protected void onClick(){
				if (offer.item != null){
					GameScene.show( new WndInfoItem( offer.item ) );
				} else {
					GameScene.show( new WndTitledMessage( tipIcon(),
							Messages.titleCase( Messages.get( WndMarketWares.class, "tip_name" ) ),
							Messages.get( WndMarketWares.class, "tip_desc" ) ) );
				}
			}
		};
		if (offer.item != null){
			icon.item( offer.item );
		} else {
			//the tip is a service, not a thing: it borrows a map's picture for its row
			icon.item( new xyz.gabriwar.warpedpixeldungeon.items.MagicWorldMap() );
			icon.slot().showExtraInfo( false );
		}
		icon.setRect( 0, pos, ROW_HEIGHT, ROW_HEIGHT );
		add( icon );

		RenderedTextBlock name = PixelScene.renderTextBlock( label( offer ), 6 );
		name.maxWidth( WIDTH - ROW_HEIGHT - BTN_WIDTH - 2 * (int) GAP );
		Quality q = Quality.of( offer.item );
		if (q != null && Quality.hasRarity( offer.item )) name.hardlight( q.rarity.color );
		name.setPos( ROW_HEIGHT + GAP, pos + (ROW_HEIGHT - name.height()) / 2f );
		add( name );

		RedButton buy = new RedButton( Messages.get( this, "price", price ), 6 ){
			@Override
			protected void onClick(){
				buy( offer, price );
			}
		};
		buy.setRect( WIDTH - BTN_WIDTH, pos, BTN_WIDTH, ROW_HEIGHT );
		buy.enable( price <= Dungeon.gold );
		add( buy );

		return pos + ROW_HEIGHT;
	}

	private static Image tipIcon(){
		return new ItemSprite( ItemSpriteSheet.WORLD_MAP );
	}

	private String label( BlackMarket.Offer offer ){
		if (offer.kind == BlackMarket.Offer.TIP) return Messages.get( this, "tip_name" );
		return Messages.titleCase( offer.item.title() );
	}

	private void buy( BlackMarket.Offer offer, int price ){
		if (offer.sold || price > Dungeon.gold) return;
		//he will not take coin for a floor with nothing left to find
		if (offer.kind == BlackMarket.Offer.TIP && hiddenDoors() == 0){
			GLog.i( Messages.get( this, "tip_none" ) );
			return;
		}
		offer.sold = true;
		WndBlackMarket.pay( price );
		Sample.INSTANCE.play( Assets.Sounds.GOLD );

		if (offer.kind == BlackMarket.Offer.TIP){
			revealSecrets();
		} else {
			Item item = offer.item;
			offer.item = null;
			WndBlackMarket.handOver( item );
		}
		WndBlackMarket.drawHeat();

		hide();
		GameScene.show( new WndMarketWares( dealer, market ) );
	}

	private static int hiddenDoors(){
		int n = 0;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.map[i] == Terrain.SECRET_DOOR) n++;
		}
		return n;
	}

	//the tip: every hidden door on the floor, marked on the map as the dealer talks
	private void revealSecrets(){
		int found = 0;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.map[i] != Terrain.SECRET_DOOR) continue;
			Dungeon.level.discover( i );
			ScrollOfMagicMapping.discover( i );
			Dungeon.level.mapped[i] = true;
			found++;
		}
		GameScene.updateFog();
		Dungeon.observe();
		Sample.INSTANCE.play( Assets.Sounds.SECRET );
		GLog.p( Messages.get( this, "tip_found", found ) );
	}

	@Override
	public void hide(){
		super.hide();
		CurrencyIndicator.showGold = false;
	}
}
