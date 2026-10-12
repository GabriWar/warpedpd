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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildPlan;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A build piece's cost as a row of chips (the build menu's cards and detail, the build bar): each
 * material's own item sprite and "have/need", green when the hero has enough of it and red when
 * he has not. Small chips draw the sprite at half size, so two fit side by side on a phone's card;
 * the row wraps to its width. A piece that costs nothing says "free".
 */
public class CostChips extends Component {

	public static final int ENOUGH = 0x88EE66;
	public static final int SHORT = 0xFF6655;

	private static final int GAP = 2;

	//one sprite per material class: the item's own image, read once
	private static final HashMap<Class<? extends Item>, Integer> IMAGES = new HashMap<>();

	private final ArrayList<ItemSprite> icons = new ArrayList<>();
	private final ArrayList<RenderedTextBlock> labels = new ArrayList<>();
	private final float scale;

	/** The chips of `cost` taken `times` over, against what the stock holds. small: half-size
	 *  sprites and the smallest words. */
	public CostChips( List<BuildPiece.Cost> cost, int times, BuildPlan.Stock stock, boolean small ){
		super();
		scale = small ? 0.5f : 1f;
		int size = small ? 5 : 6;
		List<BuildPiece.Cost> all = BuildPlan.aggregate( cost, times );
		if (all.isEmpty()){
			RenderedTextBlock free = PixelScene.renderTextBlock( Messages.get( this, "free" ), size );
			free.hardlight( ENOUGH );
			add( free );
			labels.add( free );
			icons.add( null );
			return;
		}
		for (BuildPiece.Cost c : all){
			ItemSprite icon = new ItemSprite( imageOf( c.material ), null );
			icon.scale.set( scale );
			add( icon );
			icons.add( icon );
			int have = stock.have( c.material );
			//what he has, cut short at 999 (or at the need, past it: a gold price)
			RenderedTextBlock t = PixelScene.renderTextBlock( Math.min( have, Math.max( 999, c.quantity ) ) + "/" + c.quantity, size );
			t.hardlight( have >= c.quantity ? ENOUGH : SHORT );
			add( t );
			labels.add( t );
		}
	}

	/** The item sprite index of a material. */
	public static int imageOf( Class<? extends Item> material ){
		Integer img = IMAGES.get( material );
		if (img == null){
			Item i = Reflection.newInstance( material );
			img = i == null ? 0 : i.image();
			IMAGES.put( material, img );
		}
		return img;
	}

	//lays the chips left to right, wrapping at the width, and takes the height they need
	@Override
	protected void layout(){
		float x = this.x, y = this.y, rowH = 0;
		for (int i = 0; i < labels.size(); i++){
			ItemSprite icon = icons.get( i );
			RenderedTextBlock t = labels.get( i );
			float iw = icon == null ? 0 : icon.width() + 1;
			float ih = icon == null ? 0 : icon.height();
			float cw = iw + t.width();
			if (x > this.x && x + cw > this.x + width){
				x = this.x;
				y += rowH + 1;
				rowH = 0;
			}
			float ch = Math.max( ih, t.height() );
			if (icon != null){
				icon.x = x;
				icon.y = y + (ch - ih) / 2f;
				PixelScene.align( icon );
			}
			t.setPos( x + iw, y + (ch - t.height()) / 2f );
			PixelScene.align( t );
			x += cw + GAP;
			rowH = Math.max( rowH, ch );
		}
		height = y - this.y + rowH;
	}

	/** The width the chips take on one row. */
	public float reqWidth(){
		float w = 0;
		for (int i = 0; i < labels.size(); i++){
			ItemSprite icon = icons.get( i );
			w += (icon == null ? 0 : icon.width() + 1) + labels.get( i ).width() + (i > 0 ? GAP : 0);
		}
		return w;
	}

	public void alpha( float a ){
		for (ItemSprite i : icons) if (i != null) i.alpha( a );
		for (RenderedTextBlock t : labels) t.alpha( a );
	}
}
