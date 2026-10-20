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
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.LineLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemSlot;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The item window. On an item with a {@link Quality} the text under the description has
 * the item's own rarity and type as parts, each headed by its icon and its name in its
 * colour; they open folded and a tap on a header opens or folds one. In the rarity part each line
 * that puts a chip on a blow's card, and the vestige, has a row of its own led by that chip. The text scrolls in a
 * pane that never grows past the screen, and rows a subclass adds under it (buttons,
 * prices) stay under the pane as it grows, shrinks or scrolls.
 *
 * The journal's catalog opens the same window to browse a kind of item: a row of rarity
 * gems along the top and the gamma / beta / alpha plates as tabs along the bottom, each
 * swapping its part for what that tier or type would give, so a player can see what a
 * reroll or a type shift could make of it.
 */
public class WndInfoItem extends WndTabbed {

	private static final float GAP	= 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	//a part's header row, and the least a pane shrinks to under a tall window's buttons
	private static final int HEADER_H = 10;
	private static final int MIN_PANE = 40;

	//the parts, as bits of folds
	private static final int ABOUT  = 1;
	private static final int RARITY = 2;
	private static final int TYPE   = 4;

	//only one WndInfoItem can appear at a time
	private static WndInfoItem INSTANCE;

	public WndInfoItem( Heap heap ) {
		super( Chrome.Type.WINDOW );

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
		super( Chrome.Type.WINDOW );

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;

		fillFields( item );
	}

	/**
	 * The catalog's window for a kind of item: its picture, title and catalog text, then
	 * every rarity and type it can have, picked with the gems and plates. {@code kind} is a
	 * fresh item given a plain quality by {@link Quality#sample}.
	 */
	public WndInfoItem( Item kind, Image icon, String title, String desc ) {
		super( Chrome.Type.TAB_SET );

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;

		browse = true;
		folds = 0;
		quality = kind.quality;
		qualityItem = kind;
		content = new Component();
		buildQualityPages();

		IconTitle titlebar = new IconTitle( icon, title );
		titlebar.color( TITLE_COLOR );

		layoutFields( titlebar, PixelScene.renderTextBlock( desc, 6 ) );
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
	private Rows[] rarityPages;
	private TypePage[] typePages;
	private float rarityPageHeight, typePageHeight;
	private Rarity shownRarity;
	private ItemType shownType;
	//the catalog's browsing window: every tier and type, with gems and plates to pick them
	private boolean browse;

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
		content = new Component();
		if (quality != null){
			if (quality.rarity != Rarity.COMMON || quality.fullyMasterworked()) color = quality.titleColor();
			buildQualityPages();
		}

		IconTitle titlebar = new IconTitle( item );
		titlebar.color( color );

		RenderedTextBlock txtInfo = PixelScene.renderTextBlock( item.info(), 6 );

		layoutFields(titlebar, txtInfo);
	}

	//a page per tier and per type; an item's own window shows only its own, the catalog's all
	private void buildQualityPages(){
		if (Quality.hasRarity( qualityItem )) buildRarityPages();

		typePages = new TypePage[ItemType.values().length];
		Quality.Family fam = Quality.family( qualityItem );
		int perkIcon = fam == Quality.Family.WEAPON || fam == Quality.Family.MISSILE ? TypeFx.perkIcon( WeaponPerk.of( qualityItem ) ) : -1;
		for (final ItemType t : ItemType.values()){
			String body = browse
					? Messages.get( Quality.class, "tab_type", Quality.typeName( t ) ) + "\n\n" + Quality.perkText( qualityItem, t )
					: quality.describeType( qualityItem, t );
			typePages[t.ordinal()] = new TypePage( body, Quality.perkText( qualityItem, t ), perkIcon );
			typePages[t.ordinal()].visible = false;
			content.add( typePages[t.ordinal()] );
			//bottom: the type plates as real tabs
			if (browse) add( new IconTab( WndItemQuality.icon( t.icon() ) ){
				@Override
				protected void select( boolean value ){
					super.select( value );
					if (value) showType( t );
				}
			} );
		}
	}

	private void buildRarityPages(){
		rarityPages = new Rows[Rarity.values().length];
		for (Rarity r : Rarity.values()){
			boolean own = !browse && r == quality.rarity;
			String body = own ? quality.describeRarity( qualityItem ) : quality.describeRarityTier( qualityItem, r );
			rarityPages[r.ordinal()] = new Rows( body, own ? qualityItem : null );
			rarityPages[r.ordinal()].visible = false;
			content.add( rarityPages[r.ordinal()] );
		}
		if (!browse) return;

		//top: one gem per tier, the shown one lit, the others dim
		rarityGems = new IconButton[Rarity.values().length];
		for (final Rarity r : Rarity.values()){
			IconButton gem = new IconButton( WndItemQuality.icon( r.icon() ) ){
				@Override
				protected void onClick(){
					showRarity( r );
					unfold( RARITY );
				}
			};
			rarityGems[r.ordinal()] = gem;
			add( gem );
		}
		rarityMark = new ColorBlock( 16, 1, 0xFFFFFFFF );
		add( rarityMark );
	}

	private void showRarity( Rarity r ){
		shownRarity = r;
		if (rarityGems != null){
			for (Rarity o : Rarity.values()){
				rarityGems[o.ordinal()].icon().am = o == r ? 1f : 0.5f;
			}
			rarityMark.hardlight( r == quality.rarity && quality.fullyMasterworked() ? Rarity.MASTERWORK_COLOR : r.color );
			rarityMark.x = rarityGems[r.ordinal()].left();
			rarityMark.y = rarityGems[r.ordinal()].bottom();
			PixelScene.align( rarityMark );
		}
		Part p = part( RARITY );
		if (p != null) p.show( r == quality.rarity ? quality.titleColor() : r.color,
				Messages.get( WndInfoItem.class, "part_rarity", Messages.capitalize( r.title() ) ), r.icon() );
		showPages();
	}

	private void showType( ItemType t ){
		shownType = t;
		Part p = part( TYPE );
		if (p != null) p.show( t.color, Messages.get( WndInfoItem.class, "part_type", Messages.capitalize( t.title() ) ), t.icon() );
		showPages();
	}

	//only the selected page of an open part is drawn
	private void showPages(){
		if (rarityPages != null) for (Rarity o : Rarity.values()){
			rarityPages[o.ordinal()].visible = o == shownRarity && !folded( RARITY );
		}
		if (typePages != null) for (ItemType o : ItemType.values()){
			typePages[o.ordinal()].visible = o == shownType && !folded( TYPE );
		}
		if (info != null) info.visible = !folded( ABOUT );
	}

	// ---------------------------------------------------------------- the scrolling, folding body

	private Component content;
	private ScrollPane pane;
	private RenderedTextBlock info;
	private final ArrayList<Part> parts = new ArrayList<>();
	private float paneTop;
	//the height under the pane: its padding, and whatever rows a subclass added there
	private float below = 2;
	//set while this window lays itself out, so its own resizes are not taken for a subclass's
	private boolean laying;
	//the parts folded shut: an item's rarity and type open folded, the description open
	private int folds = RARITY | TYPE;

	private boolean folded( int bit ){
		return (folds & bit) != 0;
	}

	private void unfold( int bit ){
		if (!folded( bit ) || part( bit ) == null) return;
		folds &= ~bit;
		showPages();
		relayout( true );
	}

	//a plate tapped opens the type part (the window's own first select does not)
	@Override
	protected void onClick( Tab tab ) {
		super.onClick( tab );
		unfold( TYPE );
	}

	private Part part( int bit ){
		for (Part p : parts) if (p.bit == bit) return p;
		return null;
	}

	//a part's header: a fold arrow, the part's icon, its name in its colour and a rule to the
	//window's edge, all in that colour
	private class Part {
		final int bit;
		final ColorBlock[] arrow = new ColorBlock[3];
		final Image icon;
		final RenderedTextBlock label;
		final ColorBlock rule;
		float top;

		Part( int bit, String text, boolean withIcon ){
			this.bit = bit;
			for (int i = 0; i < arrow.length; i++){
				arrow[i] = new ColorBlock( 1, 1, 0xFFFFFFFF );
				content.add( arrow[i] );
			}
			if (withIcon){
				//the slot's type badge size: the 16px rarity sheet at half scale
				icon = new Image( Assets.Sprites.RARITY_ICONS );
				icon.scale.set( 0.5f );
				content.add( icon );
			} else {
				icon = null;
			}
			label = PixelScene.renderTextBlock( 6 );
			//a block read as one line never parses Messages.tint: give it a width first
			label.maxWidth( width );
			content.add( label );
			rule = new ColorBlock( 1, 1, 0xFFFFFFFF );
			rule.alpha( 0.3f );
			content.add( rule );
			show( TITLE_COLOR, text, -1 );
		}

		void show( int color, String text, int cell ){
			label.text( "_" + Messages.tint( color, text ) + "_" );
			for (ColorBlock a : arrow) a.hardlight( color );
			rule.hardlight( color );
			if (icon != null && cell >= 0) icon.frame( cell * 16, 0, 16, 16 );
		}

		//at the top of a row of HEADER_H: a 5px arrow pointing down when open, right when folded
		void place( float y, float width ){
			top = y;
			boolean open = !folded( bit );
			float ax = 1, ay = y + (HEADER_H - 5) / 2f;
			for (int i = 0; i < arrow.length; i++){
				int len = 5 - 2 * i;
				if (open){
					arrow[i].size( len, 1 );
					arrow[i].x = ax + i;
					arrow[i].y = ay + 1 + i;
				} else {
					arrow[i].size( 1, len );
					arrow[i].x = ax + 1 + i;
					arrow[i].y = ay + i;
				}
				PixelScene.align( arrow[i] );
			}
			float lx = 9;
			if (icon != null){
				icon.x = lx;
				icon.y = y + (HEADER_H - icon.height()) / 2f;
				PixelScene.align( icon );
				lx += icon.width() + 2;
			}
			label.setPos( lx, y + (HEADER_H - label.height()) / 2f );
			PixelScene.align( label );
			float from = label.right() + 3;
			rule.size( Math.max( 0, width - from ), 1 );
			rule.x = from;
			rule.y = y + HEADER_H / 2f;
			PixelScene.align( rule );
		}
	}

	private void layoutFields(IconTitle title, RenderedTextBlock info){
		this.info = info;
		if (content == null) content = new Component();
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

		content.add( info );

		//a plain item is one page of text; an item with a quality gets its parts' headers
		if (quality != null){
			this.width = width;
			parts.add( new Part( ABOUT, Messages.get( WndInfoItem.class, "part_about" ), false ) );
			if (rarityPages != null) parts.add( new Part( RARITY, "", true ) );
			parts.add( new Part( TYPE, "", true ) );
		}

		pane = new ScrollPane( content ){
			@Override
			public void onClick( float x, float y ) {
				tapAt( y );
			}
		};
		add( pane );
		paneTop = title.bottom() + GAP;
		this.width = width;

		if (quality != null){
			if (rarityPages != null) showRarity( quality.rarity );
			showType( quality.type );
		}
		showPages();
		relayout( false );

		if (browse){
			select( quality.type.ordinal() );
		}
	}

	private void tapAt( float y ){
		for (Part p : parts){
			if (y >= p.top && y < p.top + HEADER_H){
				folds ^= p.bit;
				Sample.INSTANCE.play( Assets.Sounds.CLICK );
				showPages();
				relayout( true );
				return;
			}
		}
	}

	//the parts top to bottom; returns the content's height
	private float layoutContent(){
		float y = 0;
		if (parts.isEmpty()){
			info.setPos( 0, 0 );
			return info.height();
		}
		for (Part p : parts){
			p.place( y, width );
			y += HEADER_H + GAP;
			if (folded( p.bit )) continue;
			float h;
			if (p.bit == ABOUT){
				info.setPos( 0, y );
				h = info.height();
			} else if (p.bit == RARITY){
				for (Rows page : rarityPages) page.setPos( 0, y );
				h = rarityPageHeight;
			} else {
				for (TypePage page : typePages) page.setPos( 0, y );
				h = typePageHeight;
			}
			y += h + 2 * GAP;
		}
		return y - GAP;
	}

	/**
	 * Fits the pane to its text and the screen, then sizes the window round it. Rows under
	 * the pane follow its bottom edge when it moves ({@code shiftBelow}: not on the first
	 * layout, when nothing is under it yet and the title sits above it).
	 */
	private void relayout( boolean shiftBelow ){
		laying = true;
		float oldBottom = pane.bottom();

		float h = layoutContent();
		content.setSize( width, h );
		float room = PixelScene.uiCamera.height * 0.85f - paneTop - below - tabHeight();
		float paneH = Math.min( h, Math.max( MIN_PANE, room ) );

		resize( width, (int)Math.ceil( paneTop + paneH + below ) );
		pane.setRect( 0, paneTop, width, paneH );
		pane.scrollTo( content.camera.scroll.x, content.camera.scroll.y );

		float shift = pane.bottom() - oldBottom;
		if (shiftBelow && shift != 0){
			for (Gizmo g : membersView()){
				if (g == pane || g == chrome || g == shadow || g == blocker || g instanceof Tab) continue;
				if (g instanceof Component){
					Component c = (Component) g;
					if (c.top() >= oldBottom) c.setPos( c.left(), c.top() + shift );
				} else if (g instanceof Visual){
					Visual v = (Visual) g;
					if (v.y >= oldBottom) v.y += shift;
				}
			}
		}

		if (browse) layoutTabs();
		laying = false;
	}

	//a subclass adds its rows under the text and then sizes the window to them: the pane makes
	//room for those rows and they move with its bottom edge from then on
	@Override
	public void resize( int w, int h ) {
		if (pane == null || laying){
			super.resize( w, h );
			return;
		}
		below = Math.max( 2, h - pane.bottom() );
		relayout( true );
	}

	//the tallest page a part can show, so picking a gem or a plate does not jump the window;
	//an item's own window shows only its own pages
	private void measurePages( int width ){
		rarityPageHeight = typePageHeight = 0;
		if (rarityPages != null) for (Rarity r : Rarity.values()){
			Rows p = rarityPages[r.ordinal()];
			p.maxWidth( width );
			if (browse || r == quality.rarity) rarityPageHeight = Math.max( rarityPageHeight, p.height() );
		}
		for (ItemType t : ItemType.values()){
			TypePage p = typePages[t.ordinal()];
			p.maxWidth( width );
			if (browse || t == quality.type) typePageHeight = Math.max( typePageHeight, p.height() );
		}
	}

	//a type's page: its text, the perk's paragraph led by the silver icon its event puts on a hit card
	//(when the type has one and the perk is shown)
	private static final class TypePage extends Component {

		private static final int ICON_W = FloatingText.ICON_WIDTH + 2;

		private final RenderedTextBlock head, perk, tail;
		private final Image icon;

		TypePage( String body, String perkText, int iconCell ){
			int at = iconCell >= 0 && perkText != null && !perkText.isEmpty() ? body.indexOf( perkText ) : -1;
			if (at < 0){
				head = PixelScene.renderTextBlock( body, 6 );
				perk = tail = null;
				icon = null;
			} else {
				head = PixelScene.renderTextBlock( body.substring( 0, at ).trim(), 6 );
				perk = PixelScene.renderTextBlock( perkText, 6 );
				String rest = body.substring( at + perkText.length() ).trim();
				tail = rest.isEmpty() ? null : PixelScene.renderTextBlock( rest, 6 );
				icon = new Image( Assets.Effects.TEXT_ICONS );
				icon.frame( FloatingText.iconFilm.get( iconCell ) );
			}
			add( head );
			if (perk != null){
				add( icon );
				add( perk );
			}
			if (tail != null) add( tail );
		}

		void maxWidth( int w ){
			head.maxWidth( w );
			if (perk != null) perk.maxWidth( w - ICON_W );
			if (tail != null) tail.maxWidth( w );
			layout();
		}

		@Override
		protected void layout(){
			head.setPos( x, y );
			float bottom = head.bottom();
			if (perk != null){
				//a paragraph's gap, as the text's own blank line makes it
				float line = (perk.height() - 2f * (perk.nLines - 1)) / Math.max( 1, perk.nLines );
				float top = bottom + line + 4f;
				icon.x = x;
				icon.y = top;
				PixelScene.align( icon );
				perk.setPos( x + ICON_W, top );
				bottom = perk.bottom();
				if (tail != null){
					tail.setPos( x, bottom + line + 4f );
					bottom = tail.bottom();
				}
			}
			height = bottom - y;
		}
	}

	/**
	 * A rarity page in rows: its text as it reads in one block, but each rarity line that puts a
	 * chip on a blow's card (Slayer with its family's emblem, Executioner, Moonlit, Stormborn,
	 * Defiant: LineLooks.chipOf) and the vestige (its family's hollow mark, pale, as the card shows
	 * an echo) a row of its own led by that chip, so the player meets the icons here first. Rows
	 * are found by their text, so a line the page does not show keeps its place in the plain text.
	 */
	private static class Rows extends Component {

		//the chip's room before its row's text
		private static final int INDENT = FloatingText.ICON_WIDTH + 2;

		//each row's text, its chip (null for plain text) and the blank lines before it
		private final ArrayList<RenderedTextBlock> blocks = new ArrayList<>();
		private final ArrayList<Image> icons = new ArrayList<>();
		private final ArrayList<Integer> gaps = new ArrayList<>();

		//`item`: the item whose own lines the text shows, null for a page with none
		Rows( String text, Item item ){
			HashMap<String, int[]> chips = chips( item );
			ArrayList<String> group = new ArrayList<>();
			int gap = 0;
			for (String line : text.split( "\n", -1 )){
				int[] chip = chips.get( line );
				if (chip == null){
					group.add( line );
					continue;
				}
				gap = flush( group, gap );
				row( line, chip, gap );
				gap = 0;
			}
			flush( group, gap );
		}

		//the rows' texts as the page writes them (Quality.appendLines), with their chips: { cell,
		//tint (0 plain), 1 for an echo's }
		private static HashMap<String, int[]> chips( Item item ){
			HashMap<String, int[]> out = new HashMap<>();
			Quality q = Quality.of( item );
			if (q == null || !item.isIdentified()) return out;
			for (RarityLine line : q.lines){
				if (line == RarityLine.VESTIGE) continue;
				int code = LineLooks.chipOf( line, q.slayer );
				if (code >= 0) out.put( "_" + line.describe( item, q.roll( line ) ) + "_", new int[]{ code, 0, 0 } );
			}
			Object v = Quality.vestigeMagic( item );
			EnchantLook l = v != null ? EnchantLooks.of( v.getClass() ) : null;
			if (l != null && q.has( RarityLine.VESTIGE )){
				out.put( "_" + RarityLine.VESTIGE.describe( item, q.roll( RarityLine.VESTIGE ) ) + "_",
						new int[]{ EnchantFx.chipOf( l, true ), EnchantFx.chipTintOf( l, true ), 1 } );
			}
			return out;
		}

		//the plain lines gathered so far as one row, its blank lines at either end kept as gaps; the
		//blank lines the next row starts after
		private int flush( ArrayList<String> group, int gap ){
			while (!group.isEmpty() && group.get( 0 ).isEmpty()){
				group.remove( 0 );
				gap++;
			}
			int after = 0;
			while (!group.isEmpty() && group.get( group.size() - 1 ).isEmpty()){
				group.remove( group.size() - 1 );
				after++;
			}
			if (group.isEmpty()) return gap + after;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < group.size(); i++){
				if (i > 0) sb.append( '\n' );
				sb.append( group.get( i ) );
			}
			group.clear();
			row( sb.toString(), null, gap );
			return after;
		}

		private void row( String text, int[] chip, int gap ){
			RenderedTextBlock block = PixelScene.renderTextBlock( text, 6 );
			add( block );
			Image icon = null;
			if (chip != null){
				icon = new Image( Assets.Effects.TEXT_ICONS );
				icon.frame( FloatingText.iconFilm.get( chip[0] ) );
				if (chip[1] != 0) icon.hardlight( chip[1] );
				if (chip[2] != 0) icon.alpha( HitCard.ECHO_ALPHA );
				add( icon );
			}
			blocks.add( block );
			icons.add( icon );
			gaps.add( gap );
		}

		void maxWidth( int w ){
			for (int i = 0; i < blocks.size(); i++) blocks.get( i ).maxWidth( icons.get( i ) != null ? w - INDENT : w );
			layout();
		}

		//one line's height in these texts (a block of n lines is n of them and 2 px between each)
		private float lineHeight(){
			for (RenderedTextBlock b : blocks){
				if (b.nLines > 0 && b.height() > 0) return (b.height() - 2f * (b.nLines - 1)) / b.nLines;
			}
			return 0;
		}

		@Override
		protected void layout(){
			float line = lineHeight();
			float y = this.y, w = 0;
			for (int i = 0; i < blocks.size(); i++){
				if (i > 0) y += 2f;
				y += gaps.get( i ) * (line + 2f);
				RenderedTextBlock b = blocks.get( i );
				Image icon = icons.get( i );
				float lx = x + (icon != null ? INDENT : 0);
				b.setPos( lx, y );
				PixelScene.align( b );
				if (icon != null){
					//on the row's first line, its middle on the line's
					icon.x = x;
					icon.y = y + (line - icon.height()) / 2f;
					PixelScene.align( icon );
				}
				w = Math.max( w, lx - x + b.width() );
				y += b.height();
			}
			width = w;
			height = y - this.y;
		}
	}
}
