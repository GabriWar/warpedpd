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
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/**
 * An item's rarity and types, one tab each: the rarity gem tab lists the tier, its bonus
 * lines and masterwork progress; the gamma / beta / alpha tabs show what the item's perk
 * does at each type, the item's own type selected first.
 */
public class WndItemQuality extends WndTabbed {

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;
	private static final int GAP = 2;

	private final ArrayList<RenderedTextBlock> texts = new ArrayList<>();

	public static Image icon( int cell ){
		Image img = new Image( Assets.Sprites.RARITY_ICONS );
		img.frame( cell * 16, 0, 16, 16 );
		return img;
	}

	public WndItemQuality( Item item ){
		super();
		Quality q = item.quality;
		int width = WIDTH_MIN;

		IconTitle titlebar = new IconTitle( new ItemSprite( item ), Messages.titleCase( item.name() ) );
		titlebar.color( q.titleColor() );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );

		//tab 0: rarity
		addPage( titlebar, width, q.describeRarity( item ), icon( q.rarity.icon() ) );
		//tabs 1..3: the perk at each type
		for (ItemType t : ItemType.values()){
			addPage( titlebar, width, q.describeType( item, t ), icon( t.icon() ) );
		}

		RenderedTextBlock largest = largest();
		while (PixelScene.landscape() && largest.bottom() > PixelScene.MIN_HEIGHT_L - 20 && width < WIDTH_MAX){
			width += 20;
			titlebar.setRect( 0, 0, width, 0 );
			for (RenderedTextBlock text : texts){
				text.maxWidth( width );
				text.setPos( titlebar.left(), titlebar.bottom() + 2 * GAP );
			}
			largest = largest();
		}
		bringToFront( titlebar );

		resize( width, (int) largest.bottom() + 2 );
		layoutTabs();
		select( 1 + q.type.ordinal() );
	}

	private void addPage( IconTitle titlebar, int width, String body, Image tabIcon ){
		RenderedTextBlock text = PixelScene.renderTextBlock( 6 );
		text.text( body, width );
		text.setPos( titlebar.left(), titlebar.bottom() + 2 * GAP );
		text.visible = false;
		add( text );
		texts.add( text );
		final RenderedTextBlock page = text;
		add( new IconTab( tabIcon ){
			@Override
			protected void select( boolean value ){
				super.select( value );
				page.visible = value;
			}
		} );
	}

	private RenderedTextBlock largest(){
		RenderedTextBlock largest = null;
		for (RenderedTextBlock t : texts){
			if (largest == null || t.height() > largest.height()) largest = t;
		}
		return largest;
	}
}
