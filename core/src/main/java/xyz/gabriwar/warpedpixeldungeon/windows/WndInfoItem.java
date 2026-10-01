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

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemSlot;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;

/**
 * The item window. Items with a {@link Quality} get a row of rarity gems along the top
 * (the item's own tier selected) and the gamma / beta / alpha plates as tabs along the
 * bottom (the item's own type selected). Picking another gem or plate swaps the quality
 * paragraphs under the description for what that tier or type would give.
 */
public class WndInfoItem extends WndTabbed {

	private static final float GAP	= 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	//only one WndInfoItem can appear at a time
	private static WndInfoItem INSTANCE;

	public WndInfoItem( Heap heap ) {
		super( chromeFor( heap.type == Heap.Type.HEAP ? heap.peek() : null ) );

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;

		if (heap.type == Heap.Type.HEAP) {
			fillFields( heap.peek() );

		} else {
			fillFields( heap );

		}
	}

	public WndInfoItem( Item item ) {
		super( chromeFor( item ) );

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;

		fillFields( item );
	}

	private static Chrome.Type chromeFor( Item item ){
		return item != null && item.quality != null ? Chrome.Type.TAB_SET : Chrome.Type.WINDOW;
	}

	@Override
	protected int tabHeight() {
		return tabs.isEmpty() ? 0 : super.tabHeight();
	}

	@Override
	public void hide() {
		super.hide();
		if (INSTANCE == this){
			INSTANCE = null;
		}
	}

	private void fillFields(Heap heap ) {

		IconTitle titlebar = new IconTitle( heap );
		titlebar.color( TITLE_COLOR );

		RenderedTextBlock txtInfo = PixelScene.renderTextBlock( heap.info(), 6 );

		layoutFields(titlebar, txtInfo);
	}

	// ---------------------------------------------------------------- quality pages

	private Quality quality;
	private Item qualityItem;
	private IconButton[] rarityGems;
	private ColorBlock rarityMark;
	private RenderedTextBlock[] rarityPages;
	private RenderedTextBlock[] typePages;
	private float rarityPageHeight, typePageHeight;
	private Rarity shownRarity;
	private ItemType shownType;

	private static final int GEM_STRIP = 18;

	private void fillFields( Item item ) {

		int color = TITLE_COLOR;
		if (item.levelKnown && item.level() > 0) {
			color = ItemSlot.UPGRADED;
		} else if (item.levelKnown && item.level() < 0) {
			color = ItemSlot.DEGRADED;
		}

		quality = item.quality;
		qualityItem = item;
		if (quality != null){
			if (quality.rarity != Rarity.COMMON || quality.fullyMasterworked()) color = quality.titleColor();
			buildQualityPages();
		}

		IconTitle titlebar = new IconTitle( item );
		titlebar.color( color );

		RenderedTextBlock txtInfo = PixelScene.renderTextBlock( item.info(), 6 );

		layoutFields(titlebar, txtInfo);
	}

	private void buildQualityPages(){
		//rings, trinkets and artifacts carry a type only: no rarity strip
		if (Quality.hasRarity( qualityItem )) buildRarityStrip();

		//bottom: the type plates as real tabs
		typePages = new RenderedTextBlock[ItemType.values().length];
		for (final ItemType t : ItemType.values()){
			typePages[t.ordinal()] = PixelScene.renderTextBlock( quality.describeType( qualityItem, t ), 6 );
			typePages[t.ordinal()].visible = false;
			add( typePages[t.ordinal()] );
			add( new IconTab( WndItemQuality.icon( t.icon() ) ){
				@Override
				protected void select( boolean value ){
					super.select( value );
					if (value) showType( t );
				}
			} );
		}
	}

	private void buildRarityStrip(){
		//top: one gem per tier, the item's own lit, the others dim
		rarityGems = new IconButton[Rarity.values().length];
		for (final Rarity r : Rarity.values()){
			IconButton gem = new IconButton( WndItemQuality.icon( r.icon() ) ){
				@Override
				protected void onClick(){
					showRarity( r );
				}
			};
			rarityGems[r.ordinal()] = gem;
			add( gem );
		}
		rarityMark = new ColorBlock( 16, 1, 0xFFFFFFFF );
		add( rarityMark );

		rarityPages = new RenderedTextBlock[Rarity.values().length];
		for (Rarity r : Rarity.values()){
			String body = r == quality.rarity ? quality.describeRarity( qualityItem ) : quality.describeRarityTier( r );
			rarityPages[r.ordinal()] = PixelScene.renderTextBlock( body, 6 );
			rarityPages[r.ordinal()].visible = false;
			add( rarityPages[r.ordinal()] );
		}
	}

	private void showRarity( Rarity r ){
		shownRarity = r;
		for (Rarity o : Rarity.values()){
			rarityPages[o.ordinal()].visible = o == r;
			rarityGems[o.ordinal()].icon().am = o == r ? 1f : 0.5f;
		}
		rarityMark.hardlight( r == quality.rarity && quality.fullyMasterworked() ? Rarity.MASTERWORK_COLOR : r.color );
		rarityMark.x = rarityGems[r.ordinal()].left();
		rarityMark.y = rarityGems[r.ordinal()].bottom();
		PixelScene.align( rarityMark );
	}

	private void showType( ItemType t ){
		shownType = t;
		for (ItemType o : ItemType.values()) typePages[o.ordinal()].visible = o == t;
	}

	private void layoutFields(IconTitle title, RenderedTextBlock info){
		int width = WIDTH_MIN;

		info.maxWidth(width);
		if (quality != null) measurePages( width );

		//window can go out of the screen on landscape, so widen it as appropriate
		while (PixelScene.landscape()
				&& info.height() + rarityPageHeight + typePageHeight > 100
				&& width < WIDTH_MAX){
			width += 20;
			info.maxWidth(width);
			if (quality != null) measurePages( width );
		}

		float top = 0;
		if (rarityGems != null){
			//the gem strip runs along the top edge, the item's tier first lit
			//WndUseItem keeps the journal and rename buttons in the top-right corner
			float strip = this instanceof WndUseItem ? width - 32 : width;
			float gemW = strip / (float) rarityGems.length;
			for (int i = 0; i < rarityGems.length; i++){
				rarityGems[i].setRect( i * gemW + (gemW - 16) / 2f, 0, 16, 16 );
			}
			top = GEM_STRIP;
		}

		//leaves some space to add the journal + rename buttons in WndUseItem. This is messy I know.
		if (this instanceof WndUseItem){
			title.setRect( 0, top, width-32, 0 );
		} else {
			title.setRect( 0, top, width, 0 );
		}
		add( title );

		info.setPos(title.left(), title.bottom() + GAP);
		add( info );

		float bottom = info.bottom();
		if (quality != null){
			float y = info.bottom() + 2 * GAP;
			if (rarityPages != null){
				for (RenderedTextBlock p : rarityPages) p.setPos( title.left(), y );
				y += rarityPageHeight + 2 * GAP;
			}
			for (RenderedTextBlock p : typePages) p.setPos( title.left(), y );
			bottom = y + typePageHeight;
			if (rarityGems != null) showRarity( quality.rarity );
		}

		resize( width, (int)(bottom + 2) );

		if (quality != null){
			layoutTabs();
			select( quality.type.ordinal() );
		}
	}

	private void measurePages( int width ){
		rarityPageHeight = typePageHeight = 0;
		if (rarityPages != null) for (RenderedTextBlock p : rarityPages){
			p.maxWidth( width );
			rarityPageHeight = Math.max( rarityPageHeight, p.height() );
		}
		for (RenderedTextBlock p : typePages){
			p.maxWidth( width );
			typePageHeight = Math.max( typePageHeight, p.height() );
		}
	}
}
