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
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildInfo;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildPlacer;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildPlan;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildRecent;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildSelection;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;
import xyz.gabriwar.warpedpixeldungeon.ui.CostChips;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The build menu (the Builder's Tool's BUILD): nearly the whole screen on a phone, either way up,
 * and a comfortable panel on a desktop.
 *
 * - Along the top, the group's name and the tools that need no piece: demolish, repair, inspect
 *   and close. Under it a strip of the groups' icons (Recent first, the run's last pieces built;
 *   groups with nothing in them left out), the one shown lit; it scrolls sideways when it runs
 *   wider than the screen.
 * - Under that, a scrolling grid of the group's pieces, a card each: the piece as it looks, its
 *   name, what it costs against what the hero carries (green enough, red short) and how many of it
 *   he can build. Cards he can't afford are dimmed but can still be looked at.
 * - A card picked opens its details (under the grid on a phone held upright, beside it otherwise):
 *   a bigger picture, what it is, its health against the strongest piece's, whether it burns, how
 *   long it takes, where it may stand, what is missing, and Place / Line / Area, which close the
 *   menu and put the build bar up (homestead/building/BuildPlacer). Holding a card places it at once.
 *
 * Opened over the build bar (its menu button), placing goes on under it (BuildPlacer.Owned).
 */
public class WndBuild extends Window implements BuildPlacer.Owned {

	//where the menu was left: it opens there again
	private static BuildCategory lastCategory;
	private static String lastPiece;

	private static final int GAP = 2;
	private static final int HEADER_H = 16;
	private static final int TAB = 20;
	private static final int CARD_MIN_W = 54;
	private static final int SLOT = 18;

	private final BuildersTool tool;
	private final boolean landscape;

	private BuildCategory category;
	private BuildPiece selected;

	//everything inside the window, made again when the group or the piece picked changes
	private Component body;
	private ScrollPane tabPane, gridPane;
	private float tabScroll, gridScroll;

	public WndBuild( BuildersTool tool ){
		super();
		this.tool = tool;
		landscape = PixelScene.landscape();
		//nearly the whole screen on a phone, a comfortable panel on a desktop (Window's own width
		//and height from here on)
		resize( (int) Math.min( landscape ? 300 : 220, PixelScene.uiCamera.width - chrome.marginHor() - 4 ),
				(int) Math.min( landscape ? 200 : 300, PixelScene.uiCamera.height - chrome.marginVer() - 8 ) );

		List<BuildCategory> shown = categories();
		category = lastCategory != null && shown.contains( lastCategory ) ? lastCategory
				: shown.contains( BuildCategory.RECENT ) ? BuildCategory.RECENT
				: shown.isEmpty() ? BuildCategory.WALLS : shown.get( 0 );
		BuildPiece last = BuildCatalog.byId( lastPiece );
		if (last != null && pieces( category ).contains( last )) selected = last;
		rebuild();
	}

	//the groups with anything in them, in menu order: Recent while it holds a piece
	private List<BuildCategory> categories(){
		ArrayList<BuildCategory> out = new ArrayList<>();
		for (BuildCategory c : BuildCategory.values()){
			if (!pieces( c ).isEmpty()) out.add( c );
		}
		return out;
	}

	private List<BuildPiece> pieces( BuildCategory c ){
		return c == BuildCategory.RECENT ? BuildRecent.pieces( tool.recent ) : BuildCatalog.in( c );
	}

	private static BuildPlan.Stock stock(){
		return m -> Material.count( Dungeon.hero, m );
	}

	// ----------------------------------------------------------------- laying it out

	//a tap on a tab or a card lays the window out again on the next frame, not inside the tap:
	//the button tapped is one of those made again
	private boolean rebuildDue;

	private void rebuildSoon(){
		rebuildDue = true;
	}

	@Override
	public synchronized void update(){
		super.update();
		if (rebuildDue){
			rebuildDue = false;
			rebuild();
		}
	}

	private void rebuild(){
		if (tabPane != null) tabScroll = tabPane.content().camera.scroll.x;
		if (gridPane != null) gridScroll = gridPane.content().camera.scroll.y;
		if (body != null){
			body.destroy();
			erase( body );
		}
		body = new Component();
		add( body );
		tabPane = gridPane = null;

		header();
		float top = tabs( HEADER_H + GAP ) + GAP;

		float gridW = width, gridH = height - top;
		float dx = 0, dy = 0, dw = 0, dh = 0;
		if (selected != null){
			if (landscape){
				dw = Math.max( 100, (int) (width * 0.42f) );
				gridW = width - dw - GAP;
				dx = gridW + GAP;
				dy = top;
				dh = height - top;
			} else {
				dh = Math.min( 120, (int) ((height - top) * 0.55f) );
				gridH = height - top - dh - GAP;
				dx = 0;
				dy = top + gridH + GAP;
				dw = width;
			}
		}
		grid( 0, top, gridW, gridH );
		if (selected != null) detail( dx, dy, dw, dh );
	}

	//the group's name, and demolish, repair, inspect and close on the right
	private void header(){
		RenderedTextBlock title = PixelScene.renderTextBlock( category.title(), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( 0, (HEADER_H - title.height()) / 2f );
		PixelScene.align( title );
		body.add( title );

		float x = width;
		IconButton close = new IconButton( Icons.get( Icons.CLOSE ) ){
			@Override
			protected void onClick(){
				hide();
			}
		};
		close.setRect( x - 16, 0, 16, HEADER_H );
		body.add( close );
		x -= 16 + GAP;

		Object[][] tools = {
				{ BuildIcons.INSPECT, BuildSelection.Mode.INSPECT, "inspect" },
				{ BuildIcons.REPAIR, BuildSelection.Mode.REPAIR, "repair" },
				{ BuildIcons.DEMOLISH, BuildSelection.Mode.DEMOLISH, "demolish" }
		};
		for (Object[] t : tools){
			final BuildSelection.Mode mode = (BuildSelection.Mode) t[1];
			final String key = (String) t[2];
			IconButton b = new IconButton( ((BuildIcons) t[0]).get() ){
				@Override
				protected void onClick(){
					hide();
					BuildPlacer.start( tool, selected, mode );
				}

				@Override
				protected String hoverText(){
					return Messages.get( WndBuild.class, key );
				}
			};
			b.setRect( x - 16, 0, 16, HEADER_H );
			body.add( b );
			x -= 16 + GAP;
		}
	}

	//the strip of groups, scrolling sideways when it is wider than the window; its bottom
	private float tabs( float top ){
		List<BuildCategory> shown = categories();
		//the pane before its buttons: pointer listeners fire newest first (see WndDebugScenes)
		tabPane = new ScrollPane( new Component() );
		Component content = tabPane.content();
		float x = 0;
		for (final BuildCategory c : shown){
			Tab t = new Tab( c, c == category );
			t.setRect( x, 0, TAB, TAB );
			content.add( t );
			x += TAB + 1;
		}
		content.setSize( Math.max( 0, x - 1 ), TAB );
		tabPane.dragOverButtons();
		body.add( tabPane );
		tabPane.setRect( 0, top, width, TAB );
		tabPane.scrollTo( tabScroll, 0 );
		return top + TAB;
	}

	//the group's cards, in rows as wide as the grid allows
	private void grid( float x, float y, float w, float h ){
		gridPane = new ScrollPane( new Component() );
		Component content = gridPane.content();
		float inner = w - 3;
		int cols = Math.max( 1, (int) ((inner + GAP) / (CARD_MIN_W + GAP)) );
		float cardW = (inner - (cols - 1) * GAP) / cols;

		List<BuildPiece> list = pieces( category );
		float pos = 0;
		for (int i = 0; i < list.size(); i += cols){
			Card[] row = new Card[Math.min( cols, list.size() - i )];
			float rowH = 0;
			for (int j = 0; j < row.length; j++){
				row[j] = new Card( list.get( i + j ), list.get( i + j ) == selected );
				rowH = Math.max( rowH, row[j].reqHeight( cardW ) );
			}
			for (int j = 0; j < row.length; j++){
				row[j].setRect( j * (cardW + GAP), pos, cardW, rowH );
				content.add( row[j] );
			}
			pos += rowH + GAP;
		}
		if (list.isEmpty()){
			RenderedTextBlock none = PixelScene.renderTextBlock( Messages.get( this, "empty" ), 6 );
			none.maxWidth( (int) inner );
			content.add( none );
			pos = none.height() + GAP;
		}
		content.setSize( inner, Math.max( 0, pos - GAP ) );
		gridPane.dragOverButtons();
		body.add( gridPane );
		gridPane.setRect( x, y, w, h );
		gridPane.scrollTo( 0, gridScroll );
	}

	//the piece picked: its picture, its words, its numbers and the three ways to place it
	private void detail( float x, float y, float w, float h ){
		BuildPiece p = selected;

		ColorBlock rule = new ColorBlock( landscape ? 1 : w, landscape ? h : 1, 0x55FFFFFF );
		rule.x = landscape ? x - GAP / 2f - 0.5f : x;
		rule.y = landscape ? y : y - GAP / 2f - 0.5f;
		body.add( rule );

		//the buttons along its foot
		float btnH = 16;
		ArrayList<RedButton> buttons = new ArrayList<>();
		buttons.add( placeButton( "place", BuildIcons.PLACE, BuildSelection.Mode.SINGLE ) );
		if (p.canLine) buttons.add( placeButton( "line", BuildIcons.LINE, BuildSelection.Mode.LINE ) );
		if (p.canArea) buttons.add( placeButton( "area", BuildIcons.AREA, BuildSelection.Mode.AREA ) );
		float bw = (w - GAP * (buttons.size() - 1)) / buttons.size();
		for (int i = 0; i < buttons.size(); i++){
			buttons.get( i ).setRect( x + i * (bw + GAP), y + h - btnH, bw, btnH );
			body.add( buttons.get( i ) );
		}

		ScrollPane pane = new ScrollPane( new Component() );
		Component c = pane.content();
		float inner = w - 3;

		//the picture, twice its size (a tall frame at its own), in a dark slot
		float slot = 34;
		ColorBlock well = new ColorBlock( slot, slot, 0x66000000 );
		c.add( well );
		Image big = p.preview();
		big.scale.set( big.height() > 16 ? 1f : 2f );
		big.x = (slot - big.width()) / 2f;
		big.y = big.height() > slot ? slot - big.height() : (slot - big.height()) / 2f;
		c.add( big );

		float tx = slot + 4;
		int tw = (int) Math.max( 30, inner - tx );
		RenderedTextBlock name = PixelScene.renderTextBlock( Messages.titleCase( p.name() ), 8 );
		name.hardlight( TITLE_COLOR );
		name.maxWidth( tw );
		name.setPos( tx, 0 );
		c.add( name );

		float ty = name.bottom() + 2;
		if (p.destructible()){
			RenderedTextBlock hp = PixelScene.renderTextBlock( Messages.get( this, "health", p.maxHp ), 6 );
			hp.setPos( tx, ty );
			c.add( hp );
			float barX = hp.right() + 3, barW = Math.max( 8, tx + tw - barX );
			ColorBlock back = new ColorBlock( barW, 3, 0xFF442222 );
			back.x = barX;
			back.y = hp.top() + (hp.height() - 3) / 2f;
			c.add( back );
			ColorBlock fill = new ColorBlock( Math.max( 1, barW * p.maxHp / (float) BuildInfo.strongest() ), 3, 0xFF44DD44 );
			fill.x = barX;
			fill.y = back.y;
			c.add( fill );
			ty = hp.bottom() + 1;
		} else {
			RenderedTextBlock hp = PixelScene.renderTextBlock( Messages.get( this, "unbreakable" ), 6 );
			hp.maxWidth( tw );
			hp.setPos( tx, ty );
			c.add( hp );
			ty = hp.bottom() + 1;
		}
		RenderedTextBlock turns = PixelScene.renderTextBlock( Messages.get( this, "turns", p.buildTurns ), 6 );
		turns.setPos( tx, ty );
		c.add( turns );
		if (p.flammable){
			RenderedTextBlock burns = PixelScene.renderTextBlock( Messages.get( this, "burns" ), 6 );
			burns.hardlight( 0xFF9933 );
			burns.setPos( turns.right() + 4, ty );
			c.add( burns );
		}
		ty = Math.max( slot, turns.bottom() ) + 3;

		RenderedTextBlock desc = PixelScene.renderTextBlock( p.desc(), 6 );
		desc.maxWidth( (int) inner );
		desc.setPos( 0, ty );
		c.add( desc );
		ty = desc.bottom() + 3;

		RenderedTextBlock where = PixelScene.renderTextBlock( BuildInfo.placement( p ), 6 );
		where.hardlight( 0xCCCCCC );
		where.maxWidth( (int) inner );
		where.setPos( 0, ty );
		c.add( where );
		ty = where.bottom() + 3;

		//"Cost:" and the full-size chips, the word level with their first row
		RenderedTextBlock costLabel = PixelScene.renderTextBlock( Messages.get( this, "cost" ), 6 );
		c.add( costLabel );
		CostChips chips = new CostChips( p.cost, 1, stock(), false );
		c.add( chips );
		chips.setRect( costLabel.width() + 3, ty, inner - costLabel.width() - 3, 0 );
		costLabel.setPos( 0, ty + (Math.min( 16, chips.height() ) - costLabel.height()) / 2f );
		PixelScene.align( costLabel );
		ty = Math.max( costLabel.bottom(), chips.bottom() ) + 2;

		List<BuildPiece.Cost> missing = BuildPlan.missing( p, 1, stock() );
		if (!missing.isEmpty()){
			RenderedTextBlock miss = PixelScene.renderTextBlock( Messages.get( this, "missing", BuildInfo.costs( missing ) ), 6 );
			miss.hardlight( CostChips.SHORT );
			miss.maxWidth( (int) inner );
			miss.setPos( 0, ty );
			c.add( miss );
			ty = miss.bottom() + 2;
		}
		c.setSize( inner, ty );

		body.add( pane );
		pane.setRect( x, y, w, h - btnH - GAP );
	}

	private RedButton placeButton( String key, BuildIcons icon, final BuildSelection.Mode mode ){
		RedButton b = new RedButton( Messages.get( this, key ), 7 ){
			@Override
			protected void onClick(){
				place( mode );
			}
		};
		b.icon( icon.get() );
		return b;
	}

	//closes the menu and puts the bar up with the piece picked
	private void place( BuildSelection.Mode mode ){
		if (selected == null) return;
		lastPiece = selected.id;
		hide();
		BuildPlacer.start( tool, selected, mode );
	}

	@Override
	public void hide(){
		lastCategory = category;
		super.hide();
	}

	// ----------------------------------------------------------------- the strip's tabs

	private class Tab extends Button {

		private final BuildCategory cat;
		private final boolean on;
		private NinePatch plate;
		private Image icon;

		Tab( BuildCategory cat, boolean on ){
			super();
			this.cat = cat;
			this.on = on;
			plate = Chrome.get( on ? Chrome.Type.RED_BUTTON : Chrome.Type.GREY_BUTTON );
			addToBack( plate );
			icon = cat.icon.get();
			if (!on) icon.alpha( 0.7f );
			add( icon );
		}

		@Override
		protected void onPointerDown(){
			plate.brightness( 1.3f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp(){
			plate.resetColor();
		}

		@Override
		protected void onClick(){
			if (on) return;
			category = cat;
			lastCategory = cat;
			BuildPiece last = BuildCatalog.byId( lastPiece );
			selected = last != null && pieces( cat ).contains( last ) ? last : null;
			gridScroll = 0;
			rebuildSoon();
		}

		@Override
		protected String hoverText(){
			return cat.title();
		}

		@Override
		protected void layout(){
			super.layout();
			if (plate == null) return;
			plate.x = x;
			plate.y = y;
			plate.size( width, height );
			icon.x = x + (width - icon.width()) / 2f;
			icon.y = y + (height - icon.height()) / 2f;
			PixelScene.align( icon );
		}
	}

	// ----------------------------------------------------------------- the cards

	private class Card extends Button {

		private final BuildPiece piece;
		private final boolean on;
		private final int count;
		private NinePatch plate;
		private ColorBlock well;
		private Image picture;
		private RenderedTextBlock name, times;
		private CostChips chips;

		Card( BuildPiece piece, boolean on ){
			super();
			this.piece = piece;
			this.on = on;
			count = BuildPlan.affordableCount( piece, stock() );

			plate = Chrome.get( Chrome.Type.GREY_BUTTON_TR );
			if (on) plate.hardlight( 0xFFCC44 );
			addToBack( plate );
			well = new ColorBlock( SLOT, SLOT, 0x66000000 );
			add( well );
			picture = piece.preview();
			//a tall frame (the watchtower) shrinks into the slot
			if (picture.height() > 16) picture.scale.set( 16f / picture.height() );
			add( picture );
			name = PixelScene.renderTextBlock( Messages.titleCase( piece.name() ), 6 );
			add( name );
			times = PixelScene.renderTextBlock( count == Integer.MAX_VALUE
					? Messages.get( WndBuild.class, "free" ) : Messages.get( WndBuild.class, "count", Math.min( count, 999 ) ), 5 );
			times.hardlight( count > 0 ? 0xFFDD66 : CostChips.SHORT );
			add( times );
			chips = new CostChips( piece.cost, 1, stock(), true );
			add( chips );
			if (count == 0){
				picture.alpha( 0.45f );
				name.alpha( 0.55f );
				chips.alpha( 0.8f );
			}
		}

		//the height the card needs at a width
		float reqHeight( float w ){
			name.maxWidth( (int) (w - SLOT - 7) );
			chips.setRect( 0, 0, w - 6 - times.width() - 2, 0 );
			return 2 + Math.max( SLOT, name.height() ) + 2 + Math.max( chips.height(), times.height() ) + 3;
		}

		@Override
		protected void onPointerDown(){
			plate.brightness( 1.3f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp(){
			plate.resetColor();
			if (on) plate.hardlight( 0xFFCC44 );
		}

		@Override
		protected void onClick(){
			selected = on ? null : piece;
			if (selected != null) lastPiece = piece.id;
			rebuildSoon();
		}

		//held: placed at once, one at a time
		@Override
		protected boolean onLongClick(){
			selected = piece;
			place( BuildSelection.Mode.SINGLE );
			return true;
		}

		@Override
		protected String hoverText(){
			return Messages.titleCase( piece.name() );
		}

		@Override
		protected void layout(){
			super.layout();
			if (plate == null) return;
			plate.x = x;
			plate.y = y;
			plate.size( width, height );
			well.x = x + 2;
			well.y = y + 2;
			picture.x = well.x + (SLOT - picture.width()) / 2f;
			picture.y = well.y + (SLOT - picture.height()) / 2f;
			PixelScene.align( picture );
			name.maxWidth( (int) (width - SLOT - 7) );
			name.setPos( well.x + SLOT + 3, y + 2 );
			PixelScene.align( name );
			float foot = y + 2 + Math.max( SLOT, name.height() ) + 2;
			times.setPos( x + width - 3 - times.width(), foot );
			PixelScene.align( times );
			chips.setRect( x + 3, foot, width - 6 - times.width() - 2, 0 );
		}
	}
}
