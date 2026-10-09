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

import xyz.gabriwar.warpedpixeldungeon.debug.DebugMenu;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugMenu.Control;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.OptionSlider;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The debug window: one scrolling page per tab of {@link DebugMenu}, each laid out in titled
 * sections. Every control changes in place (nothing closes and reopens the window to show a new
 * state), and every on/off control looks the same: a tick on the right and a green label when on.
 */
public class WndDebug extends WndTabbed {

	private static final int WIDTH_P = 122;
	private static final int WIDTH_L = 200;
	//at this width or more, buttons and toggles sit two to a row (sliders keep the full width:
	//their titles carry the value, and would run into the end labels at half of it)
	static final int WIDE = 180;
	static final int GAP = 2;
	static final int BTN_HEIGHT = 18;
	private static final int SLIDER_HEIGHT = 21;
	private static final float SYNC_EVERY = 0.25f;

	//the window comes back on the tab, and at the scroll, it was left on
	private static int lastTab = 0;
	private static final float[] lastScroll = new float[16];

	private final ArrayList<Page> pages = new ArrayList<>();
	private final RenderedTextBlock title;
	private Page shown;
	private float sinceSync = 0;

	/** As wide as the screen lets it be in portrait (122 on the narrowest phone), 200 in
	 *  landscape: the debug window and the lists it opens share it. */
	static int width( int marginHor ) {
		return PixelScene.landscape() ? WIDTH_L
				: (int)Math.max( 100, Math.min( WIDTH_L, PixelScene.uiCamera.width - marginHor - 1 ) );
	}

	public WndDebug() {
		super();

		int width = width( chrome.marginHor() );

		title = PixelScene.renderTextBlock( 9 );
		title.hardlight( TITLE_COLOR );
		title.text( "Debug" );
		add( title );
		float top = title.height() + GAP * 2;

		List<DebugMenu.Tab> tabs = DebugMenu.tabs();
		float tallest = 0;
		for (DebugMenu.Tab tab : tabs){
			Page p = new Page( tab, width );
			pages.add( p );
			tallest = Math.max( tallest, p.contentHeight );
		}

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.max( BTN_HEIGHT, Math.min( tallest, maxH - top ) );

		//resize() BEFORE the panes go in: their cameras are placed from the window's
		resize( width, (int)Math.ceil( top + paneH ) );

		for (int i = 0; i < pages.size(); i++){
			final Page p = pages.get( i );
			final int index = i;
			add( p.pane );
			p.pane.setRect( 0, top, width, paneH );
			p.pane.scrollTo( 0, lastScroll[i] );
			add( new IconTab( Icons.get( p.tab.icon ) ){
				@Override
				protected void select( boolean value ){
					super.select( value );
					p.pane.visible = p.pane.active = value;
					if (!value) p.pane.releaseKeys();
					if (value){
						lastTab = index;
						shown = p;
						p.sync();
						title.text( "Debug: " + p.tab.title );
						if (title.width() > WndDebug.this.width) title.text( p.tab.title );
						title.setPos( (WndDebug.this.width - title.width()) / 2f, 0 );
						PixelScene.align( title );
					}
				}
			} );
		}

		layoutTabs();
		select( Math.min( lastTab, pages.size() - 1 ) );
	}

	@Override
	public synchronized void update() {
		super.update();
		//readouts are live: the climate moves, events run out, the profiler counts up
		if ((sinceSync += Game.elapsed) >= SYNC_EVERY && shown != null){
			sinceSync = 0;
			shown.sync();
		}
	}

	@Override
	public void destroy() {
		for (int i = 0; i < pages.size() && i < lastScroll.length; i++){
			lastScroll[i] = pages.get( i ).pane.content().camera.scroll.y;
		}
		super.destroy();
	}

	//a control was used: the action, then every page shows what it changed
	private void run( Control c ) {
		boolean closes = c.closes.getAsBoolean();
		if (closes) hide();
		c.action.run();
		if (!closes && parent != null) syncAll();
	}

	private void syncAll() {
		for (Page p : pages) p.sync();
	}

	// =====================================================================
	// A tab's page: its controls, made once, laid out in sections and rows
	// =====================================================================

	private class Page {

		final DebugMenu.Tab tab;
		final ScrollPane pane;
		final float width;
		final ArrayList<Item> items = new ArrayList<>();
		float contentHeight;

		Page( DebugMenu.Tab tab, float width ) {
			this.tab = tab;
			this.width = width;
			//the pane before its controls: pointer listeners fire newest first, so a pane made
			//after the buttons on it would take their presses
			pane = new ScrollPane( new Component() );
			for (Control c : tab.controls){
				if (c.shown.getAsBoolean()) items.add( make( c ) );
			}
			for (Item it : items) pane.content().add( it.view );
			for (Item it : items) it.sync();
			layout();
			//a page is mostly buttons: dragged from any of them (or up and down a slider), it scrolls
			pane.dragOverButtons();
		}

		private Item make( Control c ) {
			switch (c.kind){
				case HEADER:   return new HeaderItem( c );
				case TEXT:     return new TextItem( c );
				case READOUT:  return new ReadoutItem( c );
				case OVERRIDE: return new OverrideItem( c );
				case VALUE:    return new ValueItem( c );
				case ACTION: case TOGGLE: default:
					return new ButtonItem( c );
			}
		}

		//full-width items one under the other; two that pair up share a row, and one left
		//without a partner takes the whole row
		void layout() {
			boolean wide = width >= WIDE;
			float half = (width - GAP) / 2f;
			float pos = 0;
			Item left = null;
			for (Item it : items){
				if (left != null && left.pairsWith( it, wide )){
					it.place( half + GAP, pos, half );
					pos += Math.max( left.height(), it.height() ) + GAP;
					left = null;
					continue;
				}
				if (left != null){
					left.place( 0, pos, width );
					pos += left.height() + GAP;
					left = null;
				}
				if (it.pairs( wide )){
					it.place( 0, pos, half );
					left = it;
					continue;
				}
				//a little air above each section but the first
				if (it instanceof HeaderItem && pos > 0) pos += GAP * 2;
				it.place( 0, pos, width );
				pos += it.height() + GAP;
			}
			if (left != null){
				left.place( 0, pos, width );
				pos += left.height() + GAP;
			}
			contentHeight = Math.max( 0, pos - GAP );
			pane.content().setSize( width, contentHeight );
		}

		void sync() {
			boolean moved = false;
			for (Item it : items) moved |= it.sync();
			//a readout that grew a line pushes the rest down
			if (moved){
				layout();
				if (pane.parent != null) pane.setRect( pane.left(), pane.top(), pane.width(), pane.height() );
			}
		}
	}

	// =====================================================================
	// The items a page is made of
	// =====================================================================

	private abstract static class Item {
		Component view;
		//can sit beside another one on a row
		boolean pairs( boolean wide ) { return false; }
		//this one, on the left, and the next one share a row
		boolean pairsWith( Item next, boolean wide ) { return false; }
		abstract void place( float x, float y, float w );
		float height() { return view.height(); }
		/** shows the control's live state; true when its height changed */
		boolean sync() { return false; }
	}

	private static class HeaderItem extends Item {
		HeaderItem( Control c ) {
			view = new Header( c.label, c.icon != null ? c.icon.get() : null );
		}
		@Override void place( float x, float y, float w ) { view.setRect( x, y, w, 0 ); }
	}

	private static class TextItem extends Item {
		final RenderedTextBlock text;
		TextItem( Control c ) {
			text = PixelScene.renderTextBlock( c.textSize );
			text.setHightlighting( false );
			text.text( c.label );
			text.hardlight( 0xBBBBBB );
			view = text;
		}
		@Override void place( float x, float y, float w ) {
			text.maxWidth( (int)w );
			text.setPos( x, y );
			PixelScene.align( text );
		}
	}

	private static class ReadoutItem extends Item {
		final Control c;
		final RenderedTextBlock text;
		ReadoutItem( Control c ) {
			this.c = c;
			text = PixelScene.renderTextBlock( c.textSize );
			text.setHightlighting( false );
			view = text;
		}
		@Override void place( float x, float y, float w ) {
			text.maxWidth( (int)w );
			text.setPos( x, y );
			PixelScene.align( text );
		}
		@Override boolean sync() {
			float before = text.height();
			String now = c.text.get();
			if (!now.equals( text.text() )){
				text.text( now );
				text.setPos( text.left(), text.top() );
			}
			text.hardlight( c.color.getAsInt() );
			return text.height() != before;
		}
	}

	private class ButtonItem extends Item {
		final Control c;
		final DebugButton btn;
		ButtonItem( Control c ) {
			this.c = c;
			btn = new DebugButton( c.label, c.icon != null ? c.icon.get() : null, c.kind == DebugMenu.Kind.TOGGLE ){
				@Override
				protected void onClick() {
					run( c );
				}
			};
			view = btn;
		}
		@Override boolean pairs( boolean wide ) { return c.half || wide; }
		//halves pair with halves (Make drowsy | Clear sleepiness); on a wide window the rest pair
		//up among themselves, so a full-width control never splits a pair of halves
		@Override boolean pairsWith( Item next, boolean wide ) {
			return next instanceof ButtonItem && next.pairs( wide ) && ((ButtonItem) next).c.half == c.half;
		}
		@Override void place( float x, float y, float w ) { btn.setRect( x, y, w, BTN_HEIGHT ); }
		@Override boolean sync() {
			if (c.kind == DebugMenu.Kind.TOGGLE) btn.on( c.isOn() );
			btn.label( c.text != null ? c.label + c.text.get() : c.label );
			boolean enabled = c.enabled.getAsBoolean();
			if (enabled != btn.active) btn.enable( enabled );
			return false;
		}
	}

	//the tick holds (or lets go of) the value; letting go of the slider holds where it stands
	private class OverrideItem extends Item {
		final Control c;
		final DebugButton tick;
		final OptionSlider slider;
		OverrideItem( Control c ) {
			this.c = c;
			view = new Component();
			tick = new DebugButton( "", null, true ){
				@Override
				protected void onClick() {
					c.tick( slider.getSelectedValue() );
					syncAll();
				}
			};
			slider = new OptionSlider( c.text.get(), c.minLabel, c.maxLabel, c.min, c.max ){
				@Override
				protected void onChange() {
					c.slide( getSelectedValue() );
					syncAll();
				}
			};
			view.add( tick );
			view.add( slider );
		}
		//the tick on the right, where every other on/off control has it
		@Override void place( float x, float y, float w ) {
			slider.setRect( x, y, w - BTN_HEIGHT - GAP, SLIDER_HEIGHT );
			tick.setRect( x + w - BTN_HEIGHT, y, BTN_HEIGHT, SLIDER_HEIGHT );
			view.setRect( x, y, w, SLIDER_HEIGHT );
		}
		@Override float height() { return SLIDER_HEIGHT; }
		@Override boolean sync() {
			boolean on = c.isOn();
			tick.on( on );
			syncSlider( slider, c, on ? DebugMenu.ON_COLOR : WHITE );
			return false;
		}
	}

	private class ValueItem extends Item {
		final Control c;
		final OptionSlider slider;
		ValueItem( Control c ) {
			this.c = c;
			slider = new OptionSlider( c.text.get(), c.minLabel, c.maxLabel, c.min, c.max ){
				@Override
				protected void onChange() {
					c.set.accept( getSelectedValue() );
					syncAll();
				}
			};
			view = slider;
		}
		@Override void place( float x, float y, float w ) { slider.setRect( x, y, w, SLIDER_HEIGHT ); }
		@Override boolean sync() {
			syncSlider( slider, c, WHITE );
			return false;
		}
	}

	//the knob is left alone while a finger holds it
	private static void syncSlider( OptionSlider slider, Control c, int color ) {
		if (!slider.pressed()){
			int v = Math.max( c.min, Math.min( c.max, c.value.getAsInt() ) );
			if (v != slider.getSelectedValue()) slider.setSelectedValue( v );
		}
		String t = c.text.get();
		if (!t.equals( slider.getTitle() )) slider.setTitle( t );
		slider.titleColor( color );
	}

	// =====================================================================
	// Widgets
	// =====================================================================

	/** A section title: icon, yellow text and a rule out to the edge. */
	static class Header extends Component {
		//every header is this tall (their icons are small ones, up to 10 high), so the
		//sections are spaced the same
		private static final float HEIGHT = 10;
		private final RenderedTextBlock text;
		private final Image icon;
		private final ColorBlock rule;

		Header( String label, Image icon ) {
			super();
			text = PixelScene.renderTextBlock( label, 7 );
			text.hardlight( TITLE_COLOR );
			add( text );
			this.icon = icon;
			if (icon != null) add( icon );
			rule = new ColorBlock( 1, 1, 0xFF7B8073 );
			add( rule );
		}

		@Override
		protected void layout() {
			float h = Math.max( HEIGHT, text.height() );
			//the skill picker's emblems and calling portraits are drawn whole, never shrunk: their
			//headers grow to fit them
			if (icon != null) h = Math.max( h, icon.height() );
			float tx = x;
			if (icon != null){
				icon.x = x;
				icon.y = y + (h - icon.height()) / 2f;
				PixelScene.align( icon );
				tx = icon.x + icon.width() + 2;
			}
			text.setPos( tx, y + (h - text.height()) / 2f );
			PixelScene.align( text );
			rule.x = text.right() + 3;
			rule.y = (int)(y + h / 2f);
			rule.size( Math.max( 0, x + width - rule.x ), 1 );
			height = h;
		}
	}

	/**
	 * A button with an optional icon on the left and, for an on/off control, the tick on the
	 * right; its label turns green while it is on, and shrinks to fit a half-width button.
	 */
	static class DebugButton extends RedButton {

		private final Image glyph;
		private final Image tick;
		private String label;
		private int size = 9;
		private int color = WHITE;
		private boolean on = false;

		DebugButton( String label, Image glyph, boolean toggle ) {
			super( label, 9 );
			this.label = label;
			this.glyph = glyph;
			if (glyph != null) add( glyph );
			tick = toggle ? Icons.get( Icons.UNCHECKED ) : null;
			if (tick != null) add( tick );
		}

		void on( boolean value ) {
			if (tick == null || on == value) return;
			on = value;
			tick.copy( Icons.get( on ? Icons.CHECKED : Icons.UNCHECKED ) );
			color = on ? DebugMenu.ON_COLOR : WHITE;
			text.hardlight( color );
		}

		void label( String value ) {
			if (value.equals( label )) return;
			label = value;
			size = 9;
			remake();
			//not placed yet: layout() would shrink it to fit no width at all
			if (width > 0) layout();
		}

		private void remake() {
			remove( text );
			text.destroy();
			text = PixelScene.renderTextBlock( label, size );
			text.hardlight( color );
			text.alpha( active ? 1f : 0.3f );
			add( text );
		}

		@Override
		public void enable( boolean value ) {
			super.enable( value );
			if (glyph != null) glyph.alpha( value ? 1f : 0.3f );
			if (tick != null) tick.alpha( value ? 1f : 0.3f );
		}

		@Override
		protected void layout() {
			super.layout();
			//from the full size every time: a label that had to shrink may fit a wider button
			if (size != 9){
				size = 9;
				remake();
			}
			if (glyph == null && tick == null) {
				//a plain button keeps the label centred, shrunk only when it would not fit
				while (size > 5 && text.width() > width - 6){
					size--;
					remake();
				}
				text.setPos( x + (width - text.width()) / 2f, y + (height - text.height()) / 2f );
				PixelScene.align( text );
				return;
			}
			float left = x + 3;
			float right = x + width - 3;
			if (glyph != null){
				glyph.x = left;
				glyph.y = y + (height - glyph.height()) / 2f;
				PixelScene.align( glyph );
				left = glyph.x + glyph.width() + 2;
			}
			if (tick != null){
				tick.x = right - tick.width();
				tick.y = y + (height - tick.height()) / 2f;
				PixelScene.align( tick );
				right = tick.x - 2;
			}
			while (size > 5 && text.width() > right - left){
				size--;
				remake();
			}
			text.setPos( left, y + (height - text.height()) / 2f );
			PixelScene.align( text );
		}
	}
}
