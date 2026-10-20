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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLog;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageSummary;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.HitRecord;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.Roll;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.Side;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideLayout;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.ChipArt;
import xyz.gabriwar.warpedpixeldungeon.ui.ChipLegend;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.LiveCrop;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;
import com.watabou.utils.Reflection;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The hero window's Damage tab: the hero's last hits dealt and taken (Hero.damageLog, the last
 * DamageLog.KEEP of each), every one shown as the card it made, and opened (one at a time) to every
 * step that made its number.
 *
 * Two views switched by two buttons (the view and each view's scroll and opened row are kept while
 * the game runs), a summary plate (average, best or worst, crits or what armour and shields
 * stopped, how often it hit or was dodged, the three largest sources and a bar of their shares),
 * and the list, newest first: the struck one's (or the attacker's) picture, its name, the card's
 * chips, the card's left icon and number in its colour, a skull for a kill. Rows of one blow or one
 * turn share a band. An opened row shows TO HIT (both stats and both rolls, rule by rule, and the
 * chance), DAMAGE (from the base roll to the total, each rule's change and the running value),
 * WHAT IT SHOWED (every icon of its card, named), both sides (a living picture, health before and
 * after, the stats of the moment, armour, shield, the hero's gear) and both sides' buffs (tap one
 * for its description).
 *
 * A snapshot taken when the tab or a view is chosen (the game waits while the window is up).
 * Colours are good or bad for the hero: dealt, more is green and less orange; taken, more is red
 * and less green. Nothing is kept on a co-op guest (the host keeps the list).
 */
public class DamageTab extends Component {

	private static final String KEY = "windows.wndhero$damagetab.";

	static final int WIDTH = 140, HEIGHT = 200;
	static final int SWITCH_H = 16, PLATE_Y = 19, PLATE_H = 34, LIST_Y = 56;
	static final int ROW_H = 18, LINE_H = 9;
	/** where the change and value columns end, and how far text may run */
	static final int CHANGE_R = 108, VALUE_R = 134, TEXT_R = 134;

	static final int ACCENT_DEALT = 0xFFCC44, ACCENT_TAKEN = 0xE04848;
	static final int PLATE_FILL = 0xFF0E0C0A, RULE = 0xFF222222;
	static final int GREY = 0x999999, LABEL = 0xCCCCCC, NAME = 0xDDDDDD, SMALL = 0xD8D8D8, ARROW = 0x777777;
	static final int CRIT_RED = 0xFF4444;
	static final int[] SHARE_COLORS = { 0xE8C060, 0x60A8E8, 0xC060C0 };
	static final int OTHER_COLOR = 0x777777;
	static final int HP_AFTER = 0xFF00EE00, HP_LOST = 0xFFFF4040, HP_REST = 0xFF3A0000, SHIELD = 0xFFFFFFFF;

	static final int DEALT = 0, TAKEN = 1;
	private static final int NONE = Integer.MIN_VALUE;

	/** The view shown, each view's scroll and its opened row (a record's serial): kept while the
	 *  game runs, as the journal's catalog keeps its own. */
	static int view = DEALT;
	private static final float[] scrolls = new float[2];
	private static final int[] opened = { NONE, NONE };
	private static final boolean[] barShown = new boolean[2];

	private RedButton[] switches;
	private Content plate;
	private ScrollPane pane;
	private Content content;

	private final ArrayList<Row> rows = new ArrayList<>();
	private List<HitRecord> records = new ArrayList<>();
	private List<HitRecord> everything = new ArrayList<>();
	private RenderedTextBlock footer;
	private DamageSummary summary;

	//the summary bar's segments and their full widths, growing in
	private final ArrayList<ColorBlock> bar = new ArrayList<>();
	private final ArrayList<Float> barFull = new ArrayList<>();
	private float barAge = 1f;

	/** True until it is built, and again once it is hidden: WndHero rebuilds it when chosen. */
	boolean stale = true;

	static String txt( String key, Object... args ){
		return Messages.get( (Class<?>) null, KEY + key, args );
	}

	static boolean dealtView(){
		return view == DEALT;
	}

	static int accent(){
		return dealtView() ? ACCENT_DEALT : ACCENT_TAKEN;
	}

	static char sep(){
		try {
			return DecimalFormatSymbols.getInstance( Messages.locale() ).getDecimalSeparator();
		} catch (RuntimeException e){
			return '.';
		}
	}

	static String num( float v ){
		return DamageSummary.value( v, sep() );
	}

	//a colour darker or lighter by a share (CiteChip's)
	static int darker( int c, float f ){
		int r = (int) (((c >> 16) & 0xFF) * (1 - f)), g = (int) (((c >> 8) & 0xFF) * (1 - f)), b = (int) ((c & 0xFF) * (1 - f));
		return (r << 16) | (g << 8) | b;
	}

	@Override
	protected void createChildren(){
		super.createChildren();
		switches = new RedButton[2];
		for (int i = 0; i < 2; i++){
			final int v = i;
			switches[i] = new RedButton( txt( i == DEALT ? "dealt" : "taken" ), 8 ){
				@Override
				protected void onClick(){
					super.onClick();
					if (view == v) return;
					keepScroll();
					view = v;
					build();
				}
			};
			switches[i].icon( cellImage( i == DEALT ? FloatingText.PHYS_DMG : FloatingText.HIT_ARM ) );
			add( switches[i] );
		}
		plate = new Content();
		add( plate );
		content = new Content();
		pane = new ScrollPane( content ){
			@Override
			public void onClick( float x, float y ){
				tap( x, y );
			}
		};
		add( pane );
	}

	@Override
	protected void layout(){
		super.layout();
		float half = (WIDTH - 2) / 2f;
		switches[DEALT].setRect( x, y, half, SWITCH_H );
		switches[TAKEN].setRect( x + half + 2, y, half, SWITCH_H );
		PixelScene.align( switches[DEALT] );
		PixelScene.align( switches[TAKEN] );
		plate.setPos( x, y + PLATE_Y );
		//the plate's pictures were placed where it stood: placed again where it stands now
		if (summary != null) buildPlate( summary );
		pane.setRect( x, y + LIST_Y, WIDTH, HEIGHT - LIST_Y );
	}

	private void keepScroll(){
		if (content != null && content.camera != null) scrolls[view] = content.camera.scroll.y;
	}

	@Override
	public void update(){
		super.update();
		if (visible && content.camera != null) scrolls[view] = content.camera.scroll.y;
		if (barAge < 1f){
			barAge = Math.min( 1f, barAge + Game.elapsed / 0.3f );
			float e = GuideLayout.ease( barAge );
			for (int i = 0; i < bar.size(); i++) bar.get( i ).size( Math.round( barFull.get( i ) * e ), 3 );
		}
	}

	// ------------------------------------------------------------------ building

	/** Takes the hero's lists as they are now and builds the chosen view. */
	public void build(){
		stale = false;
		boolean guest = NetManager.isNetClient();
		Hero hero = Dungeon.hero;
		DamageLog log = hero != null && !guest ? hero.damageLog : null;
		records = log == null ? new ArrayList<HitRecord>() : (dealtView() ? log.dealt() : log.taken());
		everything = new ArrayList<>( records );
		if (log != null) everything.addAll( dealtView() ? log.taken() : log.dealt() );

		for (int i = 0; i < 2; i++){
			boolean on = i == view;
			switches[i].textColor( on ? Window.TITLE_COLOR : 0xAAAAAA );
			switches[i].icon().alpha( on ? 1f : 0.6f );
		}

		summary = new DamageSummary( records );
		buildPlate( summary );

		content.wipe();
		rows.clear();
		footer = null;
		if (records.isEmpty()){
			buildEmpty( guest );
		} else {
			HashMap<String, Image> pictures = new HashMap<>();
			int band = 0;
			HitRecord prev = null;
			for (HitRecord r : records){
				if (prev != null && !sameGroup( prev, r )) band++;
				Row row = new Row( r, band % 2 == 1, pictures );
				content.add( row );
				rows.add( row );
				prev = r;
			}
			footer = PixelScene.renderTextBlock( txt( "only_last" ), 5 );
			footer.setHightlighting( false );
			footer.hardlight( GREY );
			content.add( footer );
			for (Row row : rows){
				if (row.r.serial == opened[view]) row.open( false );
			}
		}
		relayout();
		pane.scrollTo( 0, scrolls[view] );
	}

	//rows of one blow (its side damage) or one turn read as one group
	private static boolean sameGroup( HitRecord a, HitRecord b ){
		if (root( a ) == root( b )) return true;
		return Math.abs( a.turn - b.turn ) < 0.001f;
	}

	private static int root( HitRecord r ){
		return r.kind == HitRecord.BLOW ? r.serial : r.parent != 0 ? r.parent : r.serial;
	}

	//the list restacked: each row, its opened panel under it, the footer
	private void relayout(){
		float y = 0;
		for (Row row : rows){
			row.setPos( 0, y );
			y += ROW_H;
			if (row.panel != null){
				row.panel.setPos( 0, y );
				y += row.panel.height();
			}
		}
		if (footer != null){
			y += 3;
			footer.setPos( (WIDTH - footer.width()) / 2f, y );
			PixelScene.align( footer );
			y += footer.height() + 4;
		}
		content.setSize( WIDTH, Math.max( y, HEIGHT - LIST_Y ) );
		pane.setRect( x, this.y + LIST_Y, WIDTH, HEIGHT - LIST_Y );
	}

	private void buildEmpty( boolean guest ){
		float h = HEIGHT - LIST_Y;
		Image icon = cellImage( dealtView() ? FloatingText.PHYS_DMG : FloatingText.HIT_ARM );
		icon.scale.set( 2f );
		icon.alpha( 0.35f );
		String s = txt( dealtView() ? "empty_dealt" : "empty_taken" );
		if (guest) s += "\n\n" + txt( "coop" );
		RenderedTextBlock t = PixelScene.renderTextBlock( 6 );
		t.maxWidth( 110 );
		t.text( s );
		t.setHightlighting( false );
		t.align( RenderedTextBlock.CENTER_ALIGN );
		t.hardlight( GREY );
		float top = (h - (icon.height() + 6 + t.height())) / 2f;
		icon.x = (WIDTH - icon.width()) / 2f;
		icon.y = top;
		PixelScene.align( icon );
		t.setPos( (WIDTH - t.width()) / 2f, top + icon.height() + 6 );
		PixelScene.align( t );
		content.add( icon );
		content.add( t );
	}

	// ------------------------------------------------------------------ the summary plate

	private void buildPlate( DamageSummary s ){
		plate.wipe();
		bar.clear();
		barFull.clear();
		float px = plate.left(), py = plate.top();
		int frame = darker( accent(), 0.35f );
		ColorBlock fill = new ColorBlock( WIDTH - 2, PLATE_H - 2, PLATE_FILL );
		fill.alpha( 0.55f );
		fill.x = px + 1;
		fill.y = py + 1;
		plate.add( fill );
		edges( plate, px, py, WIDTH, PLATE_H, frame );

		boolean dealt = dealtView();
		boolean any = s.landed > 0;
		String[] values = new String[4];
		int[] colors = new int[4];
		String[] captions = new String[4];
		values[0] = any ? Messages.decimalFormat( "#.#", s.average ) : "–";
		colors[0] = any ? 0xFFFFFF : GREY;
		captions[0] = txt( "average" );
		values[1] = any ? Integer.toString( s.top.total() ) : "–";
		colors[1] = any ? cardColor( s.top ) : GREY;
		captions[1] = txt( dealt ? "best" : "worst" );
		if (dealt){
			boolean c = DamageSummary.known( s.crits );
			values[2] = c ? DamageSummary.percent( s.crits ) + "%" : "–";
			colors[2] = c ? CRIT_RED : GREY;
			captions[2] = txt( "crits" );
			boolean h = DamageSummary.known( s.hits );
			values[3] = h ? DamageSummary.percent( s.hits ) + "%" : "–";
			colors[3] = h ? 0xFFFFFF : GREY;
			captions[3] = txt( "hits" );
		} else {
			boolean b = DamageSummary.known( s.blocked );
			values[2] = b ? DamageSummary.percent( s.blocked ) + "%" : "–";
			colors[2] = b ? CharSprite.POSITIVE : GREY;
			captions[2] = txt( "blocked" );
			boolean h = DamageSummary.known( s.hits );
			values[3] = h ? DamageSummary.percent( 1f - s.hits ) + "%" : "–";
			colors[3] = h ? CharSprite.POSITIVE : GREY;
			captions[3] = txt( "dodged" );
		}
		float cell = (WIDTH - 2) / 4f;
		for (int i = 0; i < 4; i++){
			float cx = px + 1 + cell * i + cell / 2f;
			RenderedTextBlock v = PixelScene.renderTextBlock( values[i], 8 );
			v.setHightlighting( false );
			v.hardlight( colors[i] );
			Image x = null;
			if (i == 1 && any && s.top.crit) x = cellImage( FloatingText.CRIT );
			float w = v.width() + (x != null ? FloatingText.ICON_WIDTH + 1 : 0);
			float left = cx - w / 2f;
			if (x != null){
				x.x = left;
				x.y = py + 2;
				PixelScene.align( x );
				plate.add( x );
				left += FloatingText.ICON_WIDTH + 1;
			}
			v.setPos( left, py + 2 );
			PixelScene.align( v );
			plate.add( v );
			RenderedTextBlock c = PixelScene.renderTextBlock( captions[i], 5 );
			c.setHightlighting( false );
			c.hardlight( GREY );
			c.setPos( cx - c.width() / 2f, py + 12 );
			PixelScene.align( c );
			plate.add( c );
			if (i > 0){
				ColorBlock div = new ColorBlock( 1, 16, 0xFFFFFFFF );
				div.hardlight( frame );
				div.alpha( 0.6f );
				div.x = px + 1 + cell * i;
				div.y = py + 2;
				PixelScene.align( div );
				plate.add( div );
			}
		}

		//the three largest sources, each in its share's colour, then the bar of the shares
		float sx = px + 5;
		for (int i = 0; i < s.sourceIcons.length; i++){
			Image ic = cellImage( s.sourceIcons[i] );
			ic.x = sx;
			ic.y = py + 18;
			PixelScene.align( ic );
			plate.add( ic );
			RenderedTextBlock p = PixelScene.renderTextBlock( DamageSummary.percent( s.sourceShares[i] ) + "%", 6 );
			p.setHightlighting( false );
			p.hardlight( SHARE_COLORS[i] );
			p.setPos( sx + FloatingText.ICON_WIDTH + 2, py + 19 );
			PixelScene.align( p );
			plate.add( p );
			sx = p.right() + 8;
		}
		if (s.sourceIcons.length == 0){
			RenderedTextBlock p = PixelScene.renderTextBlock( txt( "none" ), 5 );
			p.setHightlighting( false );
			p.hardlight( GREY );
			p.setPos( px + 5, py + 19 );
			PixelScene.align( p );
			plate.add( p );
		}
		float bx = px + 4, by = py + PLATE_H - 5, bw = WIDTH - 8;
		ColorBlock track = new ColorBlock( bw, 3, RULE );
		track.x = bx;
		track.y = by;
		plate.add( track );
		float at = bx;
		for (int i = 0; i <= s.sourceShares.length; i++){
			float share = i < s.sourceShares.length ? s.sourceShares[i] : s.otherShare;
			int color = i < s.sourceShares.length ? SHARE_COLORS[i] : OTHER_COLOR;
			float full = Math.round( bw * share );
			if (i == s.sourceShares.length) full = Math.max( 0, Math.round( bx + bw - at ) );
			if (full <= 0 || share <= 0f) continue;
			ColorBlock seg = new ColorBlock( full, 3, 0xFF000000 | color );
			seg.x = at;
			seg.y = by;
			plate.add( seg );
			bar.add( seg );
			barFull.add( full );
			at += full;
		}
		if (!barShown[view] && !bar.isEmpty()){
			barShown[view] = true;
			barAge = 0f;
			for (ColorBlock b : bar) b.size( 0, 3 );
		} else {
			barAge = 1f;
		}
	}

	//a 1 px frame around a box
	static void edges( Component into, float x, float y, float w, float h, int color ){
		float[][] e = { { x, y, w, 1 }, { x, y + h - 1, w, 1 }, { x, y + 1, 1, h - 2 }, { x + w - 1, y + 1, 1, h - 2 } };
		for (float[] r : e){
			ColorBlock b = new ColorBlock( r[2], r[3], 0xFFFFFFFF );
			b.hardlight( color );
			b.x = r[0];
			b.y = r[1];
			into.add( b );
		}
	}

	// ------------------------------------------------------------------ what a record shows as

	/** The text cell a record's card had on its left (its damage icon, or its hit's reason),
	 *  the red X aside; -1 for none. */
	static int leftIcon( HitRecord r ){
		if (r.cardIcon >= 0) return r.cardIcon;
		if (r.outcome == HitRecord.MISS) return r.roll != null && r.roll.reasonIcon >= 0 ? r.roll.reasonIcon : -1;
		int icon = r.sourceIcon;
		//a blow's reason takes the plain damage icon's place, as Char.damage does it
		if (r.roll != null && r.roll.hit && r.roll.reasonIcon >= 0
				&& (icon == FloatingText.PHYS_DMG || icon == FloatingText.PHYS_DMG_NO_BLOCK)){
			int reason = r.roll.reasonIcon;
			if (icon == FloatingText.PHYS_DMG_NO_BLOCK && reason < FloatingText.HIT_WEP + 18) reason += 18;
			return reason;
		}
		return icon;
	}

	/** The number's colour on its card (a hit no card showed: the damage red). */
	static int cardColor( HitRecord r ){
		return r.cardColor != 0 ? r.cardColor & 0xFFFFFF : CharSprite.NEGATIVE;
	}

	/** A short word for a hit that took nothing; null for a hit that did. */
	static String outcomeWord( HitRecord r ){
		switch (r.outcome){
			case HitRecord.MISS:    return txt( "missed" );
			case HitRecord.IMMUNE:  return txt( "immune" );
			case HitRecord.BARRIER: return txt( "barrier" );
			case HitRecord.BLEED:   return txt( "bleed" );
			case HitRecord.STASIS:  return txt( "stasis" );
			case HitRecord.NOTHING: return "0";
			default:                return null;
		}
	}

	/** Whose name a row reads: dealt, the struck one; taken, who struck (or what). */
	static String rowName( HitRecord r, boolean dealt ){
		Side s = dealt ? r.defender : r.attacker;
		if (s != null && s.name != null) return s.name;
		String n = DamageLedger.sourceName( r );
		return n != null ? n : "?";
	}

	static Image cellImage( int cell ){
		Image i = new Image( Assets.Effects.TEXT_ICONS );
		i.frame( FloatingText.iconFilm.get( cell ) );
		return i;
	}

	//a still picture of a side, cut to w x h (a creature's frame centred across and cut a third from
	//the top, as the guide's pictures are); null when none can be made
	static Image still( Side s, int w, int h, HashMap<String, Image> made ){
		if (s == null) return null;
		String key = s.hero ? "hero:" + s.heroClass + ":" + s.heroTier : s.cls;
		if (key == null) return null;
		Image proto = made != null ? made.get( key ) : null;
		if (proto == null){
			proto = picture( s );
			if (proto == null) return null;
			if (made != null) made.put( key, proto );
		}
		Image img = new Image( proto );
		fit( img, w, h );
		return img;
	}

	@SuppressWarnings("unchecked")
	private static Image picture( Side s ){
		try {
			if (s.hero){
				HeroClass[] all = HeroClass.values();
				HeroClass c = s.heroClass >= 0 && s.heroClass < all.length ? all[s.heroClass] : HeroClass.WARRIOR;
				return HeroSprite.avatar( c, s.heroTier );
			}
			Class<?> c = Reflection.forName( s.cls );
			if (c == null || !Mob.class.isAssignableFrom( c )) return null;
			CharSprite sprite = GuidePuppet.make( (Class<? extends Mob>) c );
			if (sprite == null) return null;
			sprite.idle();
			Image img = new Image( sprite );
			try {
				sprite.destroy();
			} catch (RuntimeException e){
				//a puppet never on a scene has nothing to let go
			}
			return img;
		} catch (RuntimeException e){
			return null;
		}
	}

	//a living picture of a side (the opened row's): the creature acts on its own now and then
	@SuppressWarnings("unchecked")
	static Image alive( Side s, int w, int h ){
		if (s == null) return null;
		try {
			CharSprite sprite = null;
			if (s.hero){
				HeroClass[] all = HeroClass.values();
				HeroClass c = s.heroClass >= 0 && s.heroClass < all.length ? all[s.heroClass] : HeroClass.WARRIOR;
				sprite = GuidePuppet.hero( c, s.heroTier );
			} else if (s.cls != null){
				Class<?> c = Reflection.forName( s.cls );
				if (c != null && Mob.class.isAssignableFrom( c )) sprite = GuidePuppet.make( (Class<? extends Mob>) c );
			}
			if (sprite != null) return new LiveCrop( sprite, w, h, 3.5f, 7f );
		} catch (RuntimeException e){
			//falls to the still one
		}
		return still( s, w, h, null );
	}

	static void fit( Image img, int w, int h ){
		RectF f = img.frame();
		if (f == null || img.texture == null || (img.width <= w && img.height <= h)) return;
		float tw = img.texture.width, th = img.texture.height;
		if (img.width > w){
			int cut = (int) ((img.width - w) / 2);
			f.left += cut / tw;
			f.right = f.left + w / tw;
		}
		if (img.height > h){
			int cut = (int) ((img.height - h) / 3);
			f.top += cut / th;
			f.bottom = f.top + h / th;
		}
		img.frame( f );
	}

	//what stands for a source with no creature: its item, else its damage icon
	static Image sourcePicture( HitRecord r, int w, int h ){
		if (r.sourceItem >= 0){
			Image i = new ItemSprite( r.sourceItem );
			fit( i, w, h );
			return i;
		}
		return cellImage( r.sourceIcon >= 0 ? r.sourceIcon : FloatingText.PHYS_DMG );
	}

	// ------------------------------------------------------------------ taps

	private void tap( float x, float y ){
		for (Row row : rows){
			if (y >= row.top() && y < row.top() + ROW_H){
				toggle( row );
				return;
			}
			if (row.panel != null && row.panel.inside( x, y )){
				row.panel.tap( x - row.panel.left(), y - row.panel.top() );
				return;
			}
		}
	}

	private void toggle( Row row ){
		Sample.INSTANCE.play( Assets.Sounds.CLICK );
		if (row.panel != null){
			row.close();
			opened[view] = NONE;
			relayout();
			return;
		}
		for (Row o : rows) if (o.panel != null) o.close();
		row.open( true );
		opened[view] = row.r.serial;
		relayout();
		pane.scrollTo( 0, row.top() );
	}

	static void showWindow( Window w ){
		if (WarpedPixelDungeon.scene() instanceof GameScene) GameScene.show( w );
		else WarpedPixelDungeon.scene().addToFront( w );
	}

	// ------------------------------------------------------------------ parts that move as one

	/** Pictures kept at offsets from its corner: moved, and faded in, as one. */
	static class Strip extends Component {

		final ArrayList<Gizmo> parts = new ArrayList<>();
		private final ArrayList<float[]> at = new ArrayList<>();
		float lift;

		<T extends Gizmo> T put( T g, float dx, float dy ){
			return put( g, dx, dy, 1f );
		}

		/** dx, dy from its corner; a: its own alpha (a fade multiplies it). */
		<T extends Gizmo> T put( T g, float dx, float dy, float a ){
			parts.add( g );
			at.add( new float[]{ dx, dy, a } );
			add( g );
			place( g, x + dx, y + dy + lift );
			return g;
		}

		/** Moves a part to new offsets. */
		void move( Gizmo g, float dx, float dy ){
			int k = parts.indexOf( g );
			if (k < 0) return;
			at.get( k )[0] = dx;
			at.get( k )[1] = dy;
			place( g, x + dx, y + dy + lift );
		}

		@Override
		protected void layout(){
			super.layout();
			for (int i = 0; i < parts.size(); i++){
				float[] p = at.get( i );
				place( parts.get( i ), x + p[0], y + p[1] + lift );
			}
		}

		private static void place( Gizmo g, float px, float py ){
			if (g instanceof Component){
				((Component) g).setPos( PixelScene.align( px ), PixelScene.align( py ) );
			} else if (g instanceof Visual){
				((Visual) g).x = PixelScene.align( px );
				((Visual) g).y = PixelScene.align( py );
			}
		}

		void fade( float f ){
			for (int i = 0; i < parts.size(); i++){
				Gizmo g = parts.get( i );
				float a = at.get( i )[2] * f;
				if (g instanceof RenderedTextBlock) ((RenderedTextBlock) g).alpha( a );
				else if (g instanceof ChipArt) ((ChipArt) g).alpha( a );
				else if (g instanceof Visual) ((Visual) g).alpha( a );
			}
		}

		RenderedTextBlock text( String s, int size, int color, float dx, float dy ){
			RenderedTextBlock t = PixelScene.renderTextBlock( s, size );
			t.setHightlighting( false );
			t.hardlight( color );
			return put( t, dx, dy );
		}

		ColorBlock block( float w, float h, int color, float a, float dx, float dy ){
			ColorBlock b = new ColorBlock( w, h, 0xFFFFFFFF );
			b.hardlight( color & 0xFFFFFF );
			b.alpha( a );
			return put( b, dx, dy, a );
		}

		void frame( float dx, float dy, float w, float h, int color ){
			block( w, 1, color, 1f, dx, dy );
			block( w, 1, color, 1f, dx, dy + h - 1 );
			block( 1, h - 2, color, 1f, dx, dy + 1 );
			block( 1, h - 2, color, 1f, dx + w - 1, dy + 1 );
		}

		@Override
		public void destroy(){
			super.destroy();
			parts.clear();
			at.clear();
		}
	}

	//the list's content: wiped and built again on every view
	private static class Content extends Component {
		void wipe(){
			for (Gizmo g : members.toArray( new Gizmo[0] )){
				if (g != null) g.destroy();
			}
			clear();
		}
	}

	// ------------------------------------------------------------------ a row

	private class Row extends Strip {

		final HitRecord r;
		Panel panel;
		private final ColorBlock[] arrow = new ColorBlock[3];

		Row( HitRecord r, boolean shaded, HashMap<String, Image> pictures ){
			super();
			this.r = r;
			boolean dealt = dealtView();
			width = WIDTH;
			height = ROW_H;

			if (shaded) block( WIDTH, ROW_H - 1, 0xFFFFFF, 0.06f, 0, 1 );
			block( WIDTH, 1, RULE, 1f, 0, 0 );
			for (int i = 0; i < 3; i++) arrow[i] = block( 1, 1, ARROW, 1f, 0, 0 );
			arrow( false );

			//the picture: the struck one (dealt) or who struck (taken), else what
			ColorBlock fill = block( 14, 14, PLATE_FILL, 1f, 9, 2 );
			fill.alpha( 1f );
			frame( 8, 1, 16, 16, darker( accent(), 0.35f ) );
			Side who = dealt ? r.defender : r.attacker;
			Image pic = still( who, 14, 14, pictures );
			if (pic == null) pic = who == null ? sourcePicture( r, 14, 14 ) : new ItemSprite( ItemSpriteSheet.SOMETHING );
			fit( pic, 14, 14 );
			put( pic, 9 + (14 - pic.width()) / 2f, 2 + 14 - pic.height() );

			//the right: [skull] [left icon or X] number [xN], or a word for a hit that took nothing
			float right = 136;
			String word = outcomeWord( r );
			float groupLeft;
			if (word != null){
				RenderedTextBlock w = PixelScene.renderTextBlock( word, 6 );
				if (w.width() > 44) w = PixelScene.renderTextBlock( word, 5 );
				//a word too long even small is in its opened panel
				if (w.width() > 44) w = PixelScene.renderTextBlock( "0", 9 );
				w.setHightlighting( false );
				w.hardlight( GREY );
				float wy = (ROW_H - w.height()) / 2f;
				put( w, right - w.width(), wy );
				groupLeft = right - w.width();
				int left = leftIcon( r );
				if (left >= 0){
					groupLeft -= FloatingText.ICON_WIDTH + 1;
					put( cellImage( left ), groupLeft, 5 );
				}
			} else {
				if (r.ticks > 1){
					RenderedTextBlock n = PixelScene.renderTextBlock( "x" + r.ticks, 6 );
					n.setHightlighting( false );
					n.hardlight( SMALL );
					right -= n.width();
					put( n, right, 7 );
					right -= 2;
				}
				RenderedTextBlock num = PixelScene.renderTextBlock( Integer.toString( r.total() ), 9 );
				num.setHightlighting( false );
				num.hardlight( cardColor( r ) );
				put( num, right - num.width(), (ROW_H - num.height()) / 2f );
				groupLeft = right - num.width();
				int left = r.crit ? FloatingText.CRIT : leftIcon( r );
				if (left >= 0){
					groupLeft -= FloatingText.ICON_WIDTH + 1;
					put( cellImage( left ), groupLeft, 5 );
				}
			}
			if (r.killed){
				Image skull = Icons.get( Icons.SKULL );
				groupLeft -= skull.width() + 1;
				put( skull, groupLeft, 5 );
			}

			//the name over the chips, as wide as the room before the number
			float nameX = 27, room = groupLeft - 3 - nameX;
			RenderedTextBlock name = fitted( rowName( r, dealt ), room );
			put( name, nameX, 1 );

			//the card's chips, at its pitch; "+N" for those that do not fit (as the card's own)
			float cx = nameX, limit = groupLeft - 2;
			int n = Math.min( r.chips.length, r.chipTints.length );
			int fit = 0;
			float used = 0;
			while (fit < n && cx + used + ChipArt.pitch( r.chips[fit] ) - 1 <= limit){
				used += ChipArt.pitch( r.chips[fit] );
				fit++;
			}
			RenderedTextBlock more = null;
			while (fit < n){
				more = PixelScene.renderTextBlock( "+" + (n - fit), 6 );
				if (fit == 0 || cx + used + more.width() <= limit) break;
				fit--;
				used -= ChipArt.pitch( r.chips[fit] );
			}
			for (int i = 0; i < fit; i++){
				ChipArt c = new ChipArt( r.chips[i], r.chipTints[i] );
				put( c, cx, ROW_H - 1 - c.height() );
				cx += ChipArt.pitch( r.chips[i] );
			}
			if (fit < n && more != null){
				more.setHightlighting( false );
				more.hardlight( SMALL );
				put( more, cx, ROW_H - 2 - more.height() );
			}
		}

		//the fold arrow: right when folded, down when open
		void arrow( boolean open ){
			int color = open ? Window.TITLE_COLOR : ARROW;
			float ay = (ROW_H - 5) / 2f;
			for (int i = 0; i < 3; i++){
				int len = 5 - 2 * i;
				ColorBlock a = arrow[i];
				a.hardlight( color );
				if (open){
					a.size( len, 1 );
					move( a, 1 + i, ay + 1 + i );
				} else {
					a.size( 1, len );
					move( a, 2 + i, ay + i );
				}
			}
		}

		void open( boolean animate ){
			arrow( true );
			panel = new Panel( r, dealtView(), everything, animate );
			content.add( panel );
		}

		void close(){
			arrow( false );
			if (panel != null){
				content.remove( panel );
				panel.destroy();
				panel = null;
			}
		}
	}

	// ------------------------------------------------------------------ an opened row

	/** Everything behind one record, section by section, under its row. */
	private static class Panel extends Strip {

		private final HitRecord r;
		private final boolean dealt;
		private final List<HitRecord> all;
		private final int accent, bad;
		private final ColorBlock bg, edge;
		private float py;
		private float age = 1f;
		//what a tap inside it does: areas (left, top, right, bottom) and their actions
		private final ArrayList<float[]> areas = new ArrayList<>();
		private final ArrayList<Runnable> actions = new ArrayList<>();

		Panel( HitRecord r, boolean dealt, List<HitRecord> all, boolean animate ){
			super();
			this.r = r;
			this.dealt = dealt;
			this.all = all;
			this.accent = dealt ? ACCENT_DEALT : ACCENT_TAKEN;
			this.bad = dealt ? CharSprite.WARNING : CharSprite.NEGATIVE;
			width = WIDTH;
			bg = block( WIDTH, 1, PLATE_FILL, 0.55f, 0, 0 );
			edge = block( 1, 1, accent, 0.6f, 3, 0 );
			py = 1;
			toHit();
			damage();
			showed();
			sides();
			buffs();
			footer();
			height = Math.round( py + 3 );
			bg.size( WIDTH, height );
			edge.size( 1, height );
			if (animate){
				age = 0f;
				lift = 2f;
				fade( 0f );
				layout();
			}
		}

		@Override
		public void update(){
			super.update();
			if (age < 1f){
				age = Math.min( 1f, age + Game.elapsed / 0.25f );
				float e = GuideLayout.ease( age );
				lift = 2f * (1f - e);
				fade( e );
				layout();
			}
		}

		void tap( float x, float y ){
			for (int i = 0; i < areas.size(); i++){
				float[] a = areas.get( i );
				if (x >= a[0] && x < a[2] && y >= a[1] && y < a[3]){
					actions.get( i ).run();
					return;
				}
			}
		}

		private void onTap( float l, float t, float w, float h, Runnable action ){
			areas.add( new float[]{ l, t, l + w, t + h } );
			actions.add( action );
		}

		// ---------------------------------------------------------------- lines

		private static RenderedTextBlock make( String s, int size, int color ){
			RenderedTextBlock t = PixelScene.renderTextBlock( s, size );
			t.setHightlighting( false );
			t.hardlight( color );
			return t;
		}

		//a block of words that wraps at `room`: 6 px when it fits on one line, else 5 px
		private static RenderedTextBlock wrapped( String s, int color, float room ){
			RenderedTextBlock t = make( s, 6, color );
			if (t.width() <= room) return t;
			t = make( s, 5, color );
			if (t.width() <= room) return t;
			t = PixelScene.renderTextBlock( 5 );
			t.maxWidth( (int) room );
			t.text( s );
			t.setHightlighting( false );
			t.hardlight( color );
			return t;
		}

		//a section's head: its name in the title colour and a faint rule to the edge
		private void head( String s ){
			py += 3;
			RenderedTextBlock t = make( s, 6, Window.TITLE_COLOR );
			put( t, 7, py + (8 - t.height()) / 2f );
			float from = 7 + t.width() + 3;
			if (from < TEXT_R) block( TEXT_R - from, 1, Window.TITLE_COLOR, 0.3f, from, py + 4 );
			py += 10;
		}

		//a grey line with nothing to show
		private void none( String s ){
			RenderedTextBlock t = make( s, 5, GREY );
			put( t, 19, py + 1 );
			py += 8;
		}

		//an icon, left of a line LINE_H high
		private void icon( int code, int tint, float top, float h ){
			if (code == Step.NO_ICON) return;
			ChipArt c = new ChipArt( code, tint );
			put( c, 7 + (10 - c.width()) / 2f, top + (h - c.height()) / 2f );
		}

		//one rule of a chain: its icon, its name, its change and the value after it
		private void step( Step s, boolean goodIfMore, int fallbackIcon, String extra ){
			float top = py;
			char sep = sep();
			String change = DamageSummary.change( s, sep );
			int mood = DamageSummary.mood( s, goodIfMore );
			int ccol = mood > 0 ? CharSprite.POSITIVE : mood < 0 ? bad : GREY;
			RenderedTextBlock v = make( DamageSummary.value( s.after, sep ), 6, s.op == Step.BASE ? LABEL : 0xFFFFFF );
			RenderedTextBlock c = change.isEmpty() ? null : make( change, 6, ccol );
			float labelR = c != null ? CHANGE_R - c.width() - 3 : VALUE_R - v.width() - 4;
			String label = s.label != null ? s.label : "?";
			if (extra != null) label += " " + extra;
			RenderedTextBlock l = wrapped( label, s.op == Step.BASE ? NAME : LABEL, labelR - 19 );
			float h = Math.max( LINE_H, l.height() + 3 );
			int code = s.icon != Step.NO_ICON ? s.icon : fallbackIcon;
			if (code != Step.NO_ICON) icon( code, s.icon != Step.NO_ICON ? s.tint : 0, top, h );
			else {
				//a rule with no icon of its own: a small dot where its icon would be
				block( 2, 2, 0x666666, 1f, 11, top + h / 2f - 1 );
			}
			put( l, 19, top + (h - l.height()) / 2f );
			if (c != null) put( c, CHANGE_R - c.width(), top + (h - c.height()) / 2f );
			put( v, VALUE_R - v.width(), top + (h - v.height()) / 2f );
			py += h;
		}

		//a chain of steps, its base with an icon of its own when it has none; one line when empty
		private void chain( Step[] steps, boolean goodIfMore, int baseIcon, String baseExtra ){
			for (int i = 0; i < steps.length; i++){
				step( steps[i], goodIfMore, i == 0 ? baseIcon : Step.NO_ICON, i == 0 ? baseExtra : null );
			}
		}

		//the dotted rule over a total
		private void dots(){
			py += 1;
			for (float x = 7; x < VALUE_R; x += 2) block( 1, 1, 0x555555, 1f, x, py );
			py += 2;
		}

		// ---------------------------------------------------------------- sections

		private void toHit(){
			head( txt( "to_hit" ) );
			Roll ro = r.roll;
			if (ro == null){
				none( txt( "none" ) );
				return;
			}
			//the attacker's accuracy: more is good for the hero when he struck
			chain( ro.accSteps, dealt, Step.NO_ICON, null );
			chain( ro.accRollSteps, dealt, Step.NO_ICON, ro.sure ? null : "(0–" + num( ro.accStat ) + ")" );
			py += 2;
			chain( ro.defSteps, !dealt, Step.NO_ICON, null );
			chain( ro.defRollSteps, !dealt, Step.NO_ICON, ro.untouchable ? null : "(0–" + num( ro.defStat ) + ")" );
			dots();

			//the outcome: its reason's icon, the chance and the two rolls, hit or missed
			float top = py;
			String said;
			if (ro.untouchable) said = txt( "untouchable" );
			else if (ro.sure) said = txt( "sure" );
			else said = txt( "chance", DamageSummary.percent( ro.chance() ) ) + " · "
					+ txt( "rolls", num( ro.accRoll ), num( ro.defRoll ) );
			boolean good = ro.hit == dealt;
			RenderedTextBlock res = make( ro.hit ? txt( "landed" ) : txt( "missed" ), 6, good ? CharSprite.POSITIVE : bad );
			RenderedTextBlock l = wrapped( said, LABEL, VALUE_R - res.width() - 4 - 19 );
			float h = Math.max( LINE_H, l.height() + 3 );
			if (ro.reasonIcon >= 0) icon( ro.reasonIcon, 0, top, h );
			put( l, 19, top + (h - l.height()) / 2f );
			put( res, VALUE_R - res.width(), top + (h - res.height()) / 2f );
			py += h;
		}

		private void damage(){
			head( txt( "damage" ) );
			if (r.steps.length == 0){
				String w = outcomeWord( r );
				none( w != null && !w.equals( "0" ) ? w : txt( "none" ) );
			} else {
				if (r.ticks > 1) none( txt( "latest_tick" ) );
				chain( r.steps, dealt, r.sourceIcon >= 0 ? r.sourceIcon : Step.NO_ICON, null );
			}
			//nothing came of it (a miss, an immunity with no rule): its word above says all
			if (r.steps.length == 0 && r.ticks <= 1) return;
			if (r.ticks > 1 && r.tickAmounts.length > 0){
				StringBuilder sum = new StringBuilder();
				for (int i = 0; i < r.tickAmounts.length; i++){
					if (i > 0) sum.append( " + " );
					sum.append( r.tickAmounts[i] );
				}
				RenderedTextBlock t = PixelScene.renderTextBlock( 5 );
				t.maxWidth( TEXT_R - 19 );
				t.text( txt( "ticks", r.ticks ) + ": " + sum );
				t.setHightlighting( false );
				t.hardlight( GREY );
				put( t, 19, py + 1 );
				py += t.height() + 3;
			}
			dots();

			//the total: the card's left icon, TOTAL, and the row's own number
			float top = py, h = 12;
			int left = r.crit ? FloatingText.CRIT : leftIcon( r );
			if (left >= 0) icon( left, 0, top, h );
			RenderedTextBlock t = make( txt( "total" ), 6, Window.TITLE_COLOR );
			put( t, 19, top + (h - t.height()) / 2f );
			RenderedTextBlock n;
			float nx;
			if (r.outcome == HitRecord.HIT){
				n = make( Integer.toString( r.total() ), 9, cardColor( r ) );
			} else {
				n = make( "0", 9, GREY );
			}
			nx = VALUE_R - n.width();
			put( n, nx, top + (h - n.height()) / 2f );
			String word = r.outcome == HitRecord.HIT ? (r.executed ? txt( "executed" ) : null) : outcomeWord( r );
			if (r.killed){
				Image skull = Icons.get( Icons.SKULL );
				nx -= skull.width() + 2;
				put( skull, nx, top + (h - skull.height()) / 2f );
			}
			if (word != null && !word.equals( "0" )){
				RenderedTextBlock w = make( word, 5, GREY );
				put( w, nx - w.width() - 3, top + (h - w.height()) / 2f );
			}
			py += h;
		}

		private void showed(){
			head( txt( "showed" ) );
			boolean any = false;
			if (r.crit || r.cardIcon >= 0){
				legend( r.crit ? FloatingText.CRIT : r.cardIcon, 0 );
				any = true;
			}
			int n = Math.min( r.chips.length, r.chipTints.length );
			for (int i = 0; i < n; i++){
				legend( r.chips[i], r.chipTints[i] );
				any = true;
			}
			//its card summed what came with the blow (its procs, the rows under it in its band)
			if (r.outcome == HitRecord.HIT && r.cardValue >= 0 && r.cardValue != r.total()){
				RenderedTextBlock t = wrapped( txt( "card_sum", r.cardValue ), GREY, TEXT_R - 19 );
				put( t, 19, py + 1 );
				py += t.height() + 3;
			}
			if (any || r.outcome != HitRecord.HIT) {
				if (!any) none( txt( "none" ) );
				return;
			}
			if (r.parent != 0){
				//damage inside a blow: its number joined what the blow showed
				none( txt( "with_blow" ) );
			} else if (r.kind != HitRecord.BLOW && leftIcon( r ) >= 0){
				//damage outside a blow floats as a number with its icon (every tick of a run)
				legend( leftIcon( r ), 0 );
			} else {
				none( txt( "none" ) );
			}
		}

		private void legend( int code, int tint ){
			float top = py;
			RenderedTextBlock l = wrapped( ChipLegend.name( code, tint, r, all ), LABEL, TEXT_R - 19 );
			float h = Math.max( LINE_H + 1, l.height() + 3 );
			icon( code, tint, top, h );
			put( l, 19, top + (h - l.height()) / 2f );
			py += h;
		}

		private void sides(){
			Side you = dealt ? r.attacker : r.defender;
			Side foe = dealt ? r.defender : r.attacker;
			if (dealt){
				side( txt( "on_target", Messages.upperCase( rowName( r, true ) ) ), foe, true );
				side( txt( "you_head" ), you, false );
			} else {
				String from = foe != null && foe.name != null ? foe.name : rowName( r, false );
				side( txt( "from_attacker", Messages.upperCase( from ) ), foe, false );
				side( txt( "on_you" ), you, true );
			}
		}

		//one side: a living picture on a plate, its health (before and after, when struck), the
		//stats of the moment and, for the hero, his gear
		private void side( String title, final Side s, boolean struck ){
			head( title );
			float top = py;
			frame( 8, top, 20, 20, darker( accent, 0.35f ) );
			block( 18, 18, PLATE_FILL, 1f, 9, top + 1 );
			Image pic = s != null ? alive( s, 18, 18 ) : sourcePicture( r, 18, 18 );
			if (pic == null) pic = new ItemSprite( ItemSpriteSheet.SOMETHING );
			fit( pic, 18, 18 );
			put( pic, 9 + (18 - pic.width()) / 2f, top + 1 + 18 - pic.height() );
			if (pic instanceof LiveCrop){
				final LiveCrop crop = (LiveCrop) pic;
				onTap( 8, top, 20, 20, crop::act );
			}

			float lx = 32, room = TEXT_R - lx, ly = top;
			ArrayList<String> stats = new ArrayList<>();
			Roll ro = r.roll;
			boolean attacker = s != null && s == r.attacker;
			if (s == null){
				RenderedTextBlock t = wrapped( rowName( r, false ), NAME, room );
				put( t, lx, ly );
				ly += t.height() + 2;
				//a fall, a falling tree...: what it was and what it does, in a line
				String why = DamageLedger.sourceDesc( r );
				if (why != null){
					RenderedTextBlock d = PixelScene.renderTextBlock( 5 );
					d.maxWidth( (int) room );
					d.text( why );
					d.setHightlighting( false );
					d.hardlight( LABEL );
					put( d, lx, ly );
					ly += d.height() + 2;
				}
			} else if (struck){
				int lost = Math.max( 0, r.hpLost - r.overkill );
				int after = Math.max( 0, s.hp - lost );
				String hp = txt( "hp", s.hp, after, s.ht );
				if (s.shield > 0) hp += "  +" + s.shield;
				RenderedTextBlock t = make( hp, 6, 0xFFFFFF );
				put( t, lx, ly );
				ly += t.height() + 2;
				strip( lx, ly, s, after, lost );
				ly += 6;
			} else {
				RenderedTextBlock t = make( txt( "hp_now", s.hp, s.ht ), 6, 0xFFFFFF );
				put( t, lx, ly );
				ly += t.height() + 2;
			}
			if (ro != null && s != null){
				//an endless stat reads as what it meant, not as a million
				if (attacker) stats.add( ro.sure ? txt( "sure" ) : txt( "accuracy", Math.round( ro.accStat ) ) );
				else stats.add( ro.untouchable ? txt( "untouchable" ) : txt( "defense", Math.round( ro.defStat ) ) );
				if (struck) stats.add( txt( "chance", DamageSummary.percent( ro.chance() ) ) );
			}
			if (struck){
				if (r.dr >= 0){
					String a = txt( "armor", r.drRaw >= 0 ? r.drRaw : r.dr );
					if (r.drPierce > 0f || (r.drRaw >= 0 && r.drRaw != r.dr)){
						a += " (" + txt( "pierced", DamageSummary.percent( r.drPierce ), r.dr ) + ")";
					}
					stats.add( a );
				}
				if (r.drNote != null) stats.add( r.drNote );
				if (r.shieldLost > 0) stats.add( txt( "shield", r.shieldLost ) );
				if (r.overkill > 0) stats.add( txt( "overkill", r.overkill ) );
			}
			if (!stats.isEmpty()){
				StringBuilder b = new StringBuilder();
				for (int i = 0; i < stats.size(); i++){
					if (i > 0) b.append( " · " );
					b.append( stats.get( i ) );
				}
				RenderedTextBlock t = PixelScene.renderTextBlock( 5 );
				t.maxWidth( (int) room );
				t.text( b.toString() );
				t.setHightlighting( false );
				t.hardlight( GREY );
				put( t, lx, ly );
				ly += t.height() + 2;
			}
			if (s != null && s.hero){
				ly = gear( lx, ly, room, s.weapon, s.weaponRarity, s.weaponColor );
				ly = gear( lx, ly, room, s.armor, s.armorRarity, s.armorColor );
			}
			py = Math.max( top + 21, ly ) + 1;
		}

		//the hero's weapon or armour as its window names it, in its rarity's colour
		private float gear( float lx, float ly, float room, String name, String rarity, int color ){
			if (name == null) return ly;
			String s = rarity != null ? name + " · " + rarity : name;
			RenderedTextBlock t = wrapped( s, color != 0 ? color & 0xFFFFFF : LABEL, room );
			put( t, lx, ly );
			return ly + t.height() + 2;
		}

		//health before and after: what is left, what this hit took, what its shield took, the rest
		private void strip( float lx, float ly, Side s, int after, int lost ){
			float w = 100f;
			float scale = s.ht > 0 ? w / Math.max( s.ht, s.hp ) : 0f;
			float a = Math.round( after * scale );
			float l = Math.min( w - a, Math.max( lost > 0 ? 1 : 0, Math.round( lost * scale ) ) );
			float sh = Math.min( w - a - l, Math.max( r.shieldLost > 0 ? 1 : 0, Math.round( r.shieldLost * scale ) ) );
			block( w, 3, HP_REST, 1f, lx, ly );
			if (a > 0) block( a, 3, HP_AFTER, 1f, lx, ly );
			if (l > 0) block( l, 3, HP_LOST, 1f, lx + a, ly );
			if (sh > 0) block( sh, 3, SHIELD, 1f, lx + a + l, ly );
		}

		private void buffs(){
			head( txt( "buffs" ) );
			Side you = dealt ? r.attacker : r.defender;
			Side foe = dealt ? r.defender : r.attacker;
			buffRow( txt( "you" ), you );
			buffRow( txt( "foe" ), foe );
		}

		//a side's buffs at the moment of the hit, framed as on a card; tap one for what it does
		private void buffRow( String who, final Side s ){
			float top = py;
			RenderedTextBlock l = make( who, 6, GREY );
			put( l, 8, top + (11 - l.height()) / 2f );
			float x0 = 32, cx = x0, cy = top;
			int n = s == null ? 0 : Math.min( Math.min( s.buffIcons.length, s.buffTints.length ),
					Math.min( s.buffNames.length, s.buffClasses.length ) );
			if (n == 0){
				RenderedTextBlock t = make( txt( "none" ), 5, GREY );
				put( t, x0, top + (11 - t.height()) / 2f );
				py += 11;
				return;
			}
			for (int i = 0; i < n; i++){
				if (cx + 9 > TEXT_R){
					cx = x0;
					cy += 11;
				}
				ChipArt c = new ChipArt( -(s.buffIcons[i] + 1), s.buffTints[i] );
				put( c, cx, cy + 1 );
				final int k = i;
				onTap( cx, cy, 10, 11, () -> buffWindow( s, k ) );
				cx += ChipArt.pitch( c.code );
			}
			py = cy + 12;
		}

		private static void buffWindow( Side s, int k ){
			String name = s.buffNames[k];
			String desc = "";
			try {
				Buff b = (Buff) Reflection.newInstanceUnhandled( Reflection.forNameUnhandled( s.buffClasses[k] ) );
				String d = b.desc();
				if (d != null && !d.equals( Messages.NO_TEXT_FOUND )) desc = d;
			} catch (Exception e){
				//a buff that cannot describe itself away from its creature shows its name only
			}
			Image icon = new BuffIcon( s.buffIcons[k], true );
			if (s.buffTints[k] != 0) icon.hardlight( s.buffTints[k] );
			showWindow( new WndTitledMessage( icon, Messages.titleCase( name ), desc ) );
		}

		private void footer(){
			py += 3;
			String turn = num( r.turn );
			if (r.ticks > 1 && r.lastTurn > r.turn) turn += "–" + num( r.lastTurn );
			RenderedTextBlock t = make( txt( "turn_depth", turn, r.depth ), 5, GREY );
			put( t, VALUE_R - t.width(), py );
			py += t.height() + 1;
		}
	}

	/** A name in 6 px, else 5, else cut with ".." to fit. */
	static RenderedTextBlock fitted( String s, float room ){
		RenderedTextBlock t = PixelScene.renderTextBlock( s, 6 );
		if (t.width() > room) t = PixelScene.renderTextBlock( s, 5 );
		String cut = s;
		while (t.width() > room && cut.length() > 1){
			cut = cut.substring( 0, cut.length() - 1 ).trim();
			t = PixelScene.renderTextBlock( cut + "..", 5 );
		}
		t.setHightlighting( false );
		t.hardlight( NAME );
		return t;
	}
}
