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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideLayout;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.journal.Journal;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.WndGuideNode;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.input.ScrollEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ScrollArea;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;
import com.watabou.utils.Reflection;
import com.watabou.utils.Signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

//The Descent Guide: the dead delver's journal as a living map of chapters and pages.
//
//Pixel rules: the graph has a camera of its own that only ever settles on whole zoom steps
//(GuideLayout.zoomLevels), every card, icon and connector sits on whole graph pixels at
//scale 1, connectors are runs of 1-pixel blocks, and card text is rendered for the zoom
//step it is shown at - so one graph pixel is always the same whole number of screen pixels.
//
//Motion: cards spring out of their chapter and connectors draw out behind them, collapse
//folds them back, fresh pages glow and ink flows down the lines towards them, locked pages
//scribble, bosses idle, sparks and dust drift. Off-screen cards stop updating.
public class GuideScene extends PixelScene {

	//where back leads: the title journal by default, or the game when opened from the
	//in-game guidebook tab. one-shot - resets after use.
	public static Class<? extends PixelScene> returnScene = JournalScene.class;

	//a page to open the guide on (the one just found), one-shot
	public static String focusKey = null;

	//kept between visits, for this session
	private static final HashSet<String> savedExpanded = new HashSet<>();
	private static boolean savedState = false;
	private static int savedZoom;
	private static float savedCX, savedCY;
	private static boolean savedOutline;
	//whether the screen was wide when the layout was chosen: turned, the guide picks again
	private static boolean savedWide;
	//how many pages had been found the last time the guide opened: it opens on the newest
	//unread page only when something was found since (or on its first opening)
	private static int foundAtLastOpen = -1;

	private static final int CARD_W = 96;
	private static final int PAD = 4;
	private static final int ICON_BOX = 16;
	private static final int ICON_MAX = 20;
	private static final int GAP = 3;
	private static final int TEXT_SIZE = 6;
	private static final int TOP_BAR = 24;
	//narrower than this (in interface pixels) and the guide reads as an outline
	private static final int WIDE_LAYOUT = 240;

	private static final float ANIM_TIME = 0.3f;
	private static final float ZOOM_TIME = 0.18f;
	private static final float PAN_TIME = 0.4f;
	//how long a fresh page's card waits before its reveal, while the scene fades in from black
	//(PixelScene's fade takes a second; by half way the card shows through)
	private static final float REVEAL_WAIT = 0.5f;

	private GuideGraph.Node root;
	private boolean outline;
	//whether this scene's screen was wide: a turned screen has its new size by the time the
	//old scene is destroyed, so it is read when the scene is made
	private boolean wideAtCreate;

	private GraphCamera cam;
	//the interface's camera, kept: Game.switchScene resets Camera.main to a bare zoom-1 camera
	//before destroy() saves the view, which is worked out on this one
	private Camera mainCam;
	private int[] zoomLevels;
	//the zoom step text is rendered for; the camera may sit between steps mid-gesture
	private int level;

	private Group edgeLayer, cardLayer, fxLayer;
	private GraphInput input;

	private final HashMap<GuideGraph.Node, Card> cards = new HashMap<>();
	//visible and leaving cards, in tree order
	private final ArrayList<Card> shown = new ArrayList<>();

	private float time = 0;

	//the graph's part on screen this frame, for culling
	private float viewX0, viewY0, viewX1, viewY1;

	//camera motion
	private float zoomFrom, zoomTo, zoomT = 1f, zoomFX, zoomFY;
	private float panFromX, panFromY, panToX, panToY, panT = 1f;
	private float velX, velY;

	//interface, in interface pixels
	private int w, h;
	private RectF insets;
	private RenderedTextBlock pagesLine;
	private StyledButton btnLayout;
	private Toast toast;

	private String lastQuery = "";
	private int lastMatchIdx = 0;

	private Signal.Listener<KeyEvent> keyListener;

	@Override
	public void create() {
		super.create();

		//guide pages unlock from global progress (visited depths, seen bosses)
		Journal.loadGlobal();
		//opened, by any way in: the journal button need not flash for a fresh page any more
		GameScene.guideFlashSeen();

		uiCamera.visible = false;

		mainCam = Camera.main;
		w = mainCam.width;
		h = mainCam.height;
		insets = getCommonInsets();

		//the living title-screen backdrop, dimmed so the graph stays readable, and dust
		add( new TitleBackground( w, h ) );
		add( new ColorBlock( w, h, 0xC4101014 ) );
		add( new Motes( w, h ) );

		zoomLevels = GuideLayout.zoomLevels( defaultZoom );

		root = GuideGraph.build();
		wideAtCreate = wideScreen();
		if (savedState){
			for (GuideGraph.Node n : GuideGraph.all( root )){
				n.expanded = n == root || savedExpanded.contains( n.id );
			}
			outline = savedWide == wideAtCreate ? savedOutline : !wideAtCreate;
			level = GuideLayout.nearest( zoomLevels, savedZoom );
		} else {
			outline = !wideAtCreate;
			level = GuideLayout.nearest( zoomLevels, defaultZoom );
		}

		cam = new GraphCamera( level );
		Camera.add( cam );

		edgeLayer = new Group();
		edgeLayer.camera = cam;
		add( edgeLayer );
		cardLayer = new Group();
		cardLayer.camera = cam;
		add( cardLayer );
		fxLayer = new Sparks();
		fxLayer.camera = cam;
		add( fxLayer );

		//added after the graph and before the interface: the buttons take pointers first
		input = new GraphInput( w, h );
		add( input );

		createInterface();

		relayout( false );

		GuideGraph.Node target = null;
		if (focusKey != null){
			target = GuideGraph.find( root, focusKey );
			focusKey = null;
		} else if (GuideProgress.foundCount() != foundAtLastOpen){
			String newest = GuideProgress.newestUnread();
			if (newest != null) target = GuideGraph.find( root, newest );
		}
		foundAtLastOpen = GuideProgress.foundCount();

		if (target != null && GuideGraph.unlocked( target )){
			for (GuideGraph.Node p = target.parent; p != null; p = p.parent) p.expanded = true;
			relayout( false );
			centerOn( target, false );
			Card c = cards.get( target );
			//under the scene's fade from black the card waits as a silhouette, so its reveal
			//plays where it can be seen (noFade: this scene opens without one)
			if (c != null) c.reveal( noFade ? 0 : REVEAL_WAIT );
		} else if (savedState && outline == savedOutline){
			setCenter( savedCX, savedCY );
		} else {
			fitStart( false );
		}
		clampView();

		KeyEvent.addKeyListener( keyListener = new Signal.Listener<KeyEvent>() {
			@Override
			public boolean onSignal( KeyEvent event ) {
				return onKey( event );
			}
		} );

		fadeIn();
	}

	//wide enough for the tree; narrower (a phone held upright) reads as an outline
	private static boolean wideScreen(){
		return Game.width / (float)defaultZoom >= WIDE_LAYOUT;
	}

	@Override
	protected void onBackPressed() {
		Class<? extends PixelScene> dest = returnScene;
		returnScene = JournalScene.class;
		if (dest == JournalScene.class){
			WarpedPixelDungeon.switchNoFade( JournalScene.class );
		} else {
			WarpedPixelDungeon.switchScene( dest );
		}
	}

	@Override
	public void destroy() {
		saveState();
		Journal.saveGlobal();
		if (keyListener != null) KeyEvent.removeKeyListener( keyListener );
		super.destroy();
		Camera.remove( cam );
	}

	private void saveState(){
		if (root == null || cam == null) return;
		savedExpanded.clear();
		for (GuideGraph.Node n : GuideGraph.all( root )){
			if (n.expanded) savedExpanded.add( n.id );
		}
		savedZoom = level;
		savedOutline = outline;
		savedWide = wideAtCreate;
		PointF c = graphAt( contentCenterX(), contentCenterY() );
		savedCX = c.x;
		savedCY = c.y;
		savedState = true;
	}

	//--- interface ---

	private void createInterface(){
		ColorBlock bar = new ColorBlock( w, insets.top + TOP_BAR, 0xE6101014 );
		add( bar );
		ColorBlock rule = new ColorBlock( w, 1, 0xFF3A3530 );
		rule.y = insets.top + TOP_BAR;
		add( rule );

		boolean wide = w - insets.left - insets.right >= 180;

		StyledButton btnSearch = new StyledButton( Chrome.Type.GREY_BUTTON_TR,
				wide ? Messages.get( this, "search" ) : "" ){
			@Override
			protected void onClick() {
				showSearch();
			}

			@Override
			protected String hoverText() {
				return Messages.get( GuideScene.class, "search" );
			}
		};
		btnSearch.icon( Icons.MAGNIFY.get() );
		btnSearch.setRect( insets.left + 2, insets.top + 4, wide ? 52 : 18, 16 );
		add( btnSearch );

		ExitButton btnExit = new ExitButton(){
			@Override
			protected void onClick() {
				onBackPressed();
			}
		};
		btnExit.setPos( w - insets.right - btnExit.width(), insets.top + 2 );
		add( btnExit );

		float left = btnSearch.right() + 2, right = btnExit.left() - 2;
		barLeft = left;
		barRight = right;
		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get( this, "title" ), 9 );
		if (title.width() > right - left){
			title.killAndErase();
			title = PixelScene.renderTextBlock( Messages.get( this, "title" ), 7 );
		}
		title.hardlight( 0xFFFF44 );
		title.setPos( left + (right - left - title.width()) / 2f, insets.top + 2 );
		align( title );
		add( title );

		pagesLine = PixelScene.renderTextBlock( 6 );
		pagesLine.hardlight( 0x9A9A9A );
		add( pagesLine );
		updatePagesLine( title );

		float by = h - insets.bottom - 20;

		StyledButton btnIn = new StyledButton( Chrome.Type.GREY_BUTTON_TR, "+" ){
			@Override
			protected void onClick() {
				zoomStep( +1, contentCenterX(), contentCenterY() );
			}

			@Override
			protected String hoverText() {
				return Messages.get( GuideScene.class, "zoom_in" );
			}
		};
		btnIn.setRect( w - insets.right - 20, by - 20, 18, 18 );
		add( btnIn );

		StyledButton btnOut = new StyledButton( Chrome.Type.GREY_BUTTON_TR, "-" ){
			@Override
			protected void onClick() {
				zoomStep( -1, contentCenterX(), contentCenterY() );
			}

			@Override
			protected String hoverText() {
				return Messages.get( GuideScene.class, "zoom_out" );
			}
		};
		btnOut.setRect( w - insets.right - 20, by, 18, 18 );
		add( btnOut );

		StyledButton btnCenter = new StyledButton( Chrome.Type.GREY_BUTTON_TR, "" ){
			@Override
			protected void onClick() {
				fitStart( true );
			}

			@Override
			protected String hoverText() {
				return Messages.get( GuideScene.class, "recenter" );
			}
		};
		btnCenter.icon( Icons.TARGET.get() );
		btnCenter.setRect( insets.left + 2, by, 18, 18 );
		add( btnCenter );

		btnLayout = new StyledButton( Chrome.Type.GREY_BUTTON_TR, "" ){
			@Override
			protected void onClick() {
				outline = !outline;
				layoutIcon();
				relayout( true );
				fitStart( true );
			}

			@Override
			protected String hoverText() {
				return Messages.get( GuideScene.class, outline ? "layout_tree" : "layout_list" );
			}
		};
		btnLayout.setRect( insets.left + 2, by - 20, 18, 18 );
		layoutIcon();
		add( btnLayout );

		toast = new Toast();
		add( toast );
	}

	private void layoutIcon(){
		btnLayout.icon( (outline ? Icons.DISPLAY_LAND : Icons.DISPLAY_PORT).get() );
	}

	//the line under the title: between the search and exit buttons, shortened to fit
	private float barLeft, barRight;

	private void updatePagesLine( RenderedTextBlock title ){
		int[] p = GuideGraph.progress( root );
		pagesLine.text( p[2] > 0
				? Messages.get( this, "pages_new", p[0], p[1], p[2] )
				: Messages.get( this, "pages", p[0], p[1] ) );
		if (pagesLine.width() > barRight - barLeft){
			pagesLine.text( p[2] > 0
					? Messages.get( this, "pages_short_new", p[0], p[1], p[2] )
					: Messages.get( this, "pages_short", p[0], p[1] ) );
		}
		pagesLine.setPos( barLeft + (barRight - barLeft - pagesLine.width()) / 2f,
				title != null ? title.bottom() + 1 : pagesLine.top() );
		align( pagesLine );
	}

	private boolean windowOpen(){
		if (members == null) return false;
		for (Gizmo g : members){
			if (g instanceof Window && g.exists) return true;
		}
		return false;
	}

	//--- content area and coordinates ---

	private float contentCenterX(){
		return mainCam.x + (insets.left + (w - insets.right)) / 2f * mainCam.zoom;
	}

	private float contentCenterY(){
		return mainCam.y + (insets.top + TOP_BAR + (h - insets.bottom)) / 2f * mainCam.zoom;
	}

	private float contentLeft(){
		return mainCam.x + insets.left * mainCam.zoom;
	}

	private float contentTop(){
		return mainCam.y + (insets.top + TOP_BAR) * mainCam.zoom;
	}

	private float contentWidth(){
		return (w - insets.left - insets.right) * mainCam.zoom;
	}

	private float contentHeight(){
		return (h - insets.bottom - insets.top - TOP_BAR) * mainCam.zoom;
	}

	//the graph point under a screen point
	private PointF graphAt( float sx, float sy ){
		return new PointF( cam.scroll.x + (sx - cam.x) / cam.zoom, cam.scroll.y + (sy - cam.y) / cam.zoom );
	}

	//puts a graph point under a screen point
	private void place( float gx, float gy, float sx, float sy ){
		cam.scroll.set( gx - (sx - cam.x) / cam.zoom, gy - (sy - cam.y) / cam.zoom );
	}

	private void setCenter( float gx, float gy ){
		place( gx, gy, contentCenterX(), contentCenterY() );
	}

	//--- zoom ---

	private void zoomStep( int dir, float sx, float sy ){
		int target = GuideLayout.step( zoomLevels, zoomT < 1 ? zoomTo : cam.zoom, dir );
		zoomTween( target, sx, sy );
	}

	private void zoomTween( int target, float sx, float sy ){
		if (target == cam.zoom && zoomT >= 1){
			settleZoom( target );
			return;
		}
		zoomFrom = cam.zoom;
		zoomTo = target;
		zoomT = 0;
		zoomFX = sx;
		zoomFY = sy;
		velX = velY = 0;
	}

	//sets the camera's zoom keeping the graph point under (sx, sy) where it is
	private void zoomAround( float zoom, float sx, float sy ){
		PointF g = graphAt( sx, sy );
		cam.zoom( zoom );
		place( g.x, g.y, sx, sy );
	}

	//text is rendered anew for the zoom step it now shows at
	private void settleZoom( int z ){
		if (z == level) return;
		level = z;
		boolean resized = false;
		for (Card c : cards.values()){
			c.textStale = true;
		}
		for (Card c : shown){
			resized |= c.measure();
		}
		if (resized){
			PointF g = graphAt( contentCenterX(), contentCenterY() );
			relayout( false );
			setCenter( g.x, g.y );
		}
	}

	//--- camera moves ---

	private void panTo( float scrollX, float scrollY, boolean animate ){
		velX = velY = 0;
		if (!animate){
			cam.scroll.set( scrollX, scrollY );
			panT = 1;
			return;
		}
		panFromX = cam.scroll.x;
		panFromY = cam.scroll.y;
		panToX = scrollX;
		panToY = scrollY;
		panT = 0;
	}

	private void centerOn( GuideGraph.Node n, boolean animate ){
		float gx = n.lx + n.lw / 2f, gy = n.ly + n.lh / 2f;
		float sx = cam.scroll.x, sy = cam.scroll.y;
		setCenter( gx, gy );
		float tx = cam.scroll.x, ty = cam.scroll.y;
		cam.scroll.set( sx, sy );
		panTo( tx, ty, animate );
	}

	//the opening view: the root and its first ring, as large as they fit
	private void fitStart( boolean animate ){
		int x0 = root.lx, y0 = root.ly, x1 = root.lx + root.lw, y1 = root.ly + root.lh;
		if (GuideLayout.showsChildren( root )){
			for (GuideGraph.Node c : root.children){
				x0 = Math.min( x0, c.lx );
				y0 = Math.min( y0, c.ly );
				x1 = Math.max( x1, c.lx + c.lw );
				y1 = Math.max( y1, c.ly + c.lh );
			}
		}
		int floor = GuideLayout.step( zoomLevels, GuideLayout.nearest( zoomLevels, defaultZoom ), -1 );
		int z = GuideLayout.fit( zoomLevels, x1 - x0, y1 - y0, 6,
				(int)contentWidth(), (int)contentHeight(), defaultZoom );
		z = Math.max( z, floor );
		if (!animate){
			cam.zoom( z );
			zoomT = 1;
			settleZoom( z );
		} else if (z != cam.zoom){
			//zoom and pan together: the pan is worked out for the zoom it ends at, and as it
			//is applied after the zoom each frame, it is where the view comes to rest
			zoomFrom = cam.zoom;
			zoomTo = z;
			zoomT = 0;
			zoomFX = contentCenterX();
			zoomFY = contentCenterY();
		}
		float tx, ty;
		float viewW = contentWidth() / z, viewH = contentHeight() / z;
		float left = (contentLeft() - cam.x) / z, top = (contentTop() - cam.y) / z;
		if (outline || (x1 - x0) > viewW){
			tx = root.lx - 6 - left;
		} else {
			tx = (x0 + x1) / 2f - viewW / 2f - left;
		}
		if (outline){
			ty = root.ly - 6 - top;
		} else {
			ty = root.ly + root.lh / 2f - viewH / 2f - top;
		}
		panTo( tx, ty, animate );
	}

	//pans as little as possible to bring a box of the graph into view
	private void reveal( int x0, int y0, int x1, int y1 ){
		float viewW = contentWidth() / cam.zoom, viewH = contentHeight() / cam.zoom;
		float left = (contentLeft() - cam.x) / cam.zoom, top = (contentTop() - cam.y) / cam.zoom;
		float sx = (panT < 1 ? panToX : cam.scroll.x), sy = (panT < 1 ? panToY : cam.scroll.y);
		float vx0 = sx + left, vy0 = sy + top;
		float m = 6;
		float nx = vx0, ny = vy0;
		if (x1 + m > nx + viewW) nx = x1 + m - viewW;
		if (x0 - m < nx) nx = x0 - m;
		if (y1 + m > ny + viewH) ny = y1 + m - viewH;
		if (y0 - m < ny) ny = y0 - m;
		if (nx != vx0 || ny != vy0){
			panTo( nx - left, ny - top, true );
		}
	}

	private void clampView(){
		if (shown.isEmpty()) return;
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
		for (Card c : shown){
			if (c.state != Card.SHOWN) continue;
			GuideGraph.Node n = c.node;
			x0 = Math.min( x0, n.lx );
			y0 = Math.min( y0, n.ly );
			x1 = Math.max( x1, n.lx + n.lw );
			y1 = Math.max( y1, n.ly + n.lh );
		}
		if (x0 > x1) return;
		//kept in the content area, under the top bar, not merely on the screen
		float viewW = contentWidth() / cam.zoom, viewH = contentHeight() / cam.zoom;
		float left = (contentLeft() - cam.x) / cam.zoom, top = (contentTop() - cam.y) / cam.zoom;
		float vx = cam.scroll.x + left, vy = cam.scroll.y + top;
		int margin = 40;
		float nx = GuideLayout.clamp( vx, viewW, x0, x1, margin );
		float ny = GuideLayout.clamp( vy, viewH, y0, y1, margin );
		if (nx != vx){
			cam.scroll.x = nx - left;
			velX = 0;
		}
		if (ny != vy){
			cam.scroll.y = ny - top;
			velY = 0;
		}
	}

	//--- graph building & layout ---

	private void collectVisible( GuideGraph.Node node, ArrayList<GuideGraph.Node> out ){
		out.add( node );
		if (GuideLayout.showsChildren( node )){
			for (GuideGraph.Node c : node.children) collectVisible( c, out );
		}
	}

	private Card cardFor( GuideGraph.Node n ){
		Card c = cards.get( n );
		if (c == null){
			c = new Card( n );
			cards.put( n, c );
			cardLayer.add( c );
		}
		return c;
	}

	//lays the visible tree out again. Animated: cards still shown slide to their new places,
	//new ones spring out of their parent (connectors drawing out behind them), and cards no
	//longer shown fold back into the nearest ancestor that stays
	private void relayout( boolean animate ){
		ArrayList<GuideGraph.Node> visible = new ArrayList<>();
		collectVisible( root, visible );
		HashSet<GuideGraph.Node> visibleSet = new HashSet<>( visible );

		for (GuideGraph.Node n : visible){
			Card c = cardFor( n );
			if (c.textStale) c.measure();
			n.lw = c.cw;
			n.lh = c.ch;
		}
		GuideLayout.layout( root, outline );

		//leaving cards
		for (Card c : shown){
			if (visibleSet.contains( c.node )) continue;
			if (animate && c.state == Card.SHOWN){
				GuideGraph.Node keep = c.node.parent;
				while (keep != null && !visibleSet.contains( keep )) keep = keep.parent;
				c.leave( keep != null ? keep.lx + keep.lw / 2 - c.cw / 2 : c.dispX,
						keep != null ? keep.ly + keep.lh / 2 - c.ch / 2 : c.dispY );
			} else if (!animate){
				c.hide();
			}
		}

		ArrayList<Card> next = new ArrayList<>();
		HashMap<GuideGraph.Node, Integer> stagger = new HashMap<>();
		for (GuideGraph.Node n : visible){
			Card c = cards.get( n );
			if (!animate){
				c.snap();
			} else if (c.state == Card.SHOWN || c.state == Card.LEAVING){
				c.moveTo();
			} else {
				Card pc = n.parent != null ? cards.get( n.parent ) : null;
				int idx = stagger.containsKey( n.parent ) ? stagger.get( n.parent ) : 0;
				stagger.put( n.parent, idx + 1 );
				float fx = pc != null && pc.state != Card.HIDDEN ? pc.dispX + pc.cw / 2f - c.cw / 2f : n.lx;
				float fy = pc != null && pc.state != Card.HIDDEN ? pc.dispY + pc.ch / 2f - c.ch / 2f : n.ly;
				c.appear( fx, fy, Math.min( 0.25f, idx * 0.025f ) );
			}
			next.add( c );
		}
		for (Card c : shown){
			if (c.state == Card.LEAVING && !next.contains( c )) next.add( c );
		}
		shown.clear();
		shown.addAll( next );

		for (Card c : shown) c.syncEdge();
	}

	private void toggle( GuideGraph.Node n ){
		n.expanded = !n.expanded;
		Card c = cards.get( n );
		if (c != null) c.refresh();
		relayout( true );
		if (n.expanded){
			int x0 = n.lx, y0 = n.ly, x1 = n.lx + n.lw, y1 = n.ly + n.lh;
			for (GuideGraph.Node k : n.children){
				x0 = Math.min( x0, k.lx );
				y0 = Math.min( y0, k.ly );
				x1 = Math.max( x1, k.lx + k.lw );
				y1 = Math.max( y1, k.ly + k.lh );
			}
			//never push the opened card itself out of view to show its pages
			float viewH = contentHeight() / cam.zoom;
			if (y1 - y0 > viewH) y1 = (int)(y0 + viewH) - 12;
			reveal( x0, y0, x1, y1 );
		}
	}

	//a page was read: its card and every chapter above it count again
	private void refreshUp( GuideGraph.Node n ){
		boolean resized = false;
		for (GuideGraph.Node p = n; p != null; p = p.parent){
			Card c = cards.get( p );
			if (c != null){
				int ow = c.cw, oh = c.ch;
				c.refresh();
				resized |= ow != c.cw || oh != c.ch;
			}
		}
		updatePagesLine( null );
		if (resized) relayout( true );
	}

	//--- update ---

	@Override
	public synchronized void update() {
		super.update();

		float dt = Game.elapsed;
		time += dt;

		//zoom tween, then settle on the step
		if (zoomT < 1){
			zoomT = Math.min( 1f, zoomT + dt / ZOOM_TIME );
			float z = zoomFrom + (zoomTo - zoomFrom) * GuideLayout.ease( zoomT );
			zoomAround( zoomT >= 1 ? zoomTo : z, zoomFX, zoomFY );
			if (zoomT >= 1) settleZoom( (int)zoomTo );
		}

		//pan tween
		if (panT < 1){
			panT = Math.min( 1f, panT + dt / PAN_TIME );
			float e = GuideLayout.ease( panT );
			cam.scroll.set( panFromX + (panToX - panFromX) * e, panFromY + (panToY - panFromY) * e );
		}

		//drag momentum
		if (!input.dragging && (velX != 0 || velY != 0)){
			cam.scroll.offset( velX * dt, velY * dt );
			float f = (float)Math.exp( -5f * dt );
			velX *= f;
			velY *= f;
			if (Math.abs( velX ) < 4 && Math.abs( velY ) < 4) velX = velY = 0;
		}

		if (panT >= 1 && !input.pinching) clampView();

		//cards and their connectors
		float vx0 = cam.scroll.x, vy0 = cam.scroll.y;
		float vx1 = vx0 + Game.width / cam.zoom, vy1 = vy0 + Game.height / cam.zoom;
		viewX0 = vx0; viewY0 = vy0; viewX1 = vx1; viewY1 = vy1;
		for (int i = shown.size() - 1; i >= 0; i--){
			Card c = shown.get( i );
			c.animate( dt );
			if (c.state == Card.HIDDEN){
				shown.remove( i );
				continue;
			}
			boolean on = c.dispX + c.cw + 2 >= vx0 && c.dispX - 2 <= vx1
					&& c.dispY + c.ch + 2 >= vy0 && c.dispY - 2 <= vy1;
			c.visible = c.active = on;
		}
		for (Card c : shown){
			if (c.edge != null) c.edge.update();
		}
	}

	//--- input ---

	private Card cardAt( float sx, float sy ){
		//the top bar covers the graph: a tap on its title is not a tap on a card under it
		if (sy < contentTop()) return null;
		PointF g = graphAt( sx, sy );
		for (int i = shown.size() - 1; i >= 0; i--){
			Card c = shown.get( i );
			if (c.state != Card.SHOWN || !c.visible) continue;
			if (g.x >= c.dispX && g.x < c.dispX + c.cw && g.y >= c.dispY && g.y < c.dispY + c.ch){
				return c;
			}
		}
		return null;
	}

	private boolean onKey( KeyEvent event ){
		if (!event.pressed || windowOpen()) return false;
		GameAction action = KeyBindings.getActionForKey( event );
		if (action == WPDAction.ZOOM_IN){
			zoomStep( +1, contentCenterX(), contentCenterY() );
			return true;
		} else if (action == WPDAction.ZOOM_OUT){
			zoomStep( -1, contentCenterX(), contentCenterY() );
			return true;
		}
		int dx = 0, dy = 0;
		if (action == WPDAction.N) dy = -1;
		else if (action == WPDAction.S) dy = 1;
		else if (action == WPDAction.W) dx = -1;
		else if (action == WPDAction.E) dx = 1;
		if (dx != 0 || dy != 0){
			float step = 48;
			float sx = panT < 1 ? panToX : cam.scroll.x, sy = panT < 1 ? panToY : cam.scroll.y;
			panTo( sx + dx * step, sy + dy * step, true );
			return true;
		}
		return false;
	}

	//one area over the whole screen for the graph: drag to pan (with momentum), pinch or
	//wheel to zoom, tap a card. Nothing on the graph blocks a drag, so a drag that starts on
	//a card pans, and only a short tap opens it
	private class GraphInput extends ScrollArea {

		boolean dragging = false;
		boolean pinching = false;
		private boolean moved = false;
		private PointerEvent another;
		private float startSpan, startZoom;
		private final PointF last = new PointF();
		private float lastTime;
		private Card pressed;
		private Card hovered;

		GraphInput( int w, int h ){
			super( 0, 0, w, h );
			blockLevel = NEVER_BLOCK;
		}

		private float threshold(){
			return defaultZoom * 4;
		}

		@Override
		public boolean onSignal( PointerEvent event ) {
			if (event != null && event.type == PointerEvent.Type.HOVER && !windowOpen()){
				Card c = cardAt( event.current.x, event.current.y );
				if (c != hovered){
					if (hovered != null) hovered.hover( false );
					hovered = c;
					if (hovered != null) hovered.hover( true );
				}
			}
			return super.onSignal( event );
		}

		@Override
		protected void onPointerDown( PointerEvent event ) {
			if (event != curEvent && another == null){
				if (curEvent.type == PointerEvent.Type.UP){
					curEvent = event;
					onPointerDown( event );
					return;
				}
				//a second finger: pinch
				pinching = true;
				another = event;
				startSpan = Math.max( 1, PointF.distance( curEvent.current, another.current ) );
				startZoom = cam.zoom;
				zoomT = 1;
				release();
				return;
			} else if (event != curEvent){
				reset();
				return;
			}
			moved = false;
			dragging = true;
			velX = velY = 0;
			panT = 1;
			last.set( event.current );
			lastTime = Game.timeTotal;
			pressed = cardAt( event.current.x, event.current.y );
			if (pressed != null) pressed.press( true );
		}

		@Override
		protected void onDrag( PointerEvent event ) {
			if (pinching && another != null){
				float span = PointF.distance( curEvent.current, another.current );
				float z = startZoom * span / startSpan;
				z = Math.max( zoomLevels[0], Math.min( zoomLevels[zoomLevels.length - 1], z ) );
				float mx = (curEvent.current.x + another.current.x) / 2f;
				float my = (curEvent.current.y + another.current.y) / 2f;
				zoomAround( z, mx, my );
				return;
			}
			if (!moved && PointF.distance( event.current, event.start ) > threshold()){
				moved = true;
				release();
				last.set( event.current );
				lastTime = Game.timeTotal;
			}
			if (moved){
				float dx = (event.current.x - last.x) / cam.zoom;
				float dy = (event.current.y - last.y) / cam.zoom;
				cam.scroll.offset( -dx, -dy );
				float t = Math.max( 0.008f, Game.timeTotal - lastTime );
				velX = velX * 0.5f + (-dx / t) * 0.5f;
				velY = velY * 0.5f + (-dy / t) * 0.5f;
				last.set( event.current );
				lastTime = Game.timeTotal;
			}
		}

		@Override
		protected void onPointerUp( PointerEvent event ) {
			if (pinching && (event == curEvent || event == another)){
				pinching = false;
				float mx = (curEvent.current.x + another.current.x) / 2f;
				float my = (curEvent.current.y + another.current.y) / 2f;
				zoomTween( GuideLayout.nearest( zoomLevels, cam.zoom ), mx, my );
				if (event == curEvent){
					curEvent = another;
				}
				another = null;
				last.set( curEvent.current );
				lastTime = Game.timeTotal;
				moved = true;
				velX = velY = 0;
				return;
			}
			dragging = false;
			//a drag let go after a pause doesn't fling
			if (Game.timeTotal - lastTime > 0.08f) velX = velY = 0;
			float cap = 1600f / cam.zoom;
			velX = Math.max( -cap, Math.min( cap, velX ) );
			velY = Math.max( -cap, Math.min( cap, velY ) );
			if (!moved) velX = velY = 0;
			if (pressed != null) pressed.press( false );
		}

		@Override
		protected void onClick( PointerEvent event ) {
			if (moved || pinching || windowOpen()) return;
			Card c = cardAt( event.current.x, event.current.y );
			if (c != null && c == pressed) c.tap();
			pressed = null;
		}

		private void release(){
			if (pressed != null){
				pressed.press( false );
				pressed = null;
			}
		}

		//what a smooth wheel or a trackpad has scrolled short of a whole notch
		private final float[] wheel = new float[1];

		@Override
		protected void onScroll( ScrollEvent event ) {
			if (windowOpen()) return;
			int steps = GuideLayout.wheelSteps( wheel, event.amount );
			for (int i = 0; i < Math.abs( steps ); i++){
				zoomStep( Integer.signum( steps ), event.pos.x, event.pos.y );
			}
		}

		@Override
		public void reset() {
			super.reset();
			another = null;
			dragging = false;
			release();
			//a pinch cut short (a third finger) still comes to rest on a whole zoom step
			if (pinching){
				pinching = false;
				zoomTween( GuideLayout.nearest( zoomLevels, cam.zoom ), contentCenterX(), contentCenterY() );
			}
		}
	}

	//--- search ---

	private void showSearch(){
		addToFront( new WndTextInput( Messages.get( this, "search_title" ), null, lastQuery, 30, false,
				Messages.get( this, "search" ), Messages.get( this, "cancel" ) ){
			@Override
			public void onSelect( boolean positive, String text ) {
				if (positive && text != null && !text.trim().isEmpty()){
					doSearch( text.trim() );
				}
			}
		});
	}

	private void doSearch( String query ){
		ArrayList<GuideGraph.Node> matches = new ArrayList<>();
		String q = query.toLowerCase( Locale.ENGLISH );
		for (GuideGraph.Node n : GuideGraph.all( root )){
			//locked knowledge is invisible to search: no spoilers
			if (n == root || !GuideGraph.unlocked( n )) continue;
			if (contains( n.title(), q ) || contains( n.subtitle(), q ) || contains( n.text(), q )){
				matches.add( n );
			}
		}

		if (matches.isEmpty()){
			lastQuery = query;
			lastMatchIdx = 0;
			toast.show( Messages.get( this, "no_match", query ) );
			return;
		}

		//same query again cycles through matches
		if (query.equalsIgnoreCase( lastQuery )){
			lastMatchIdx = (lastMatchIdx + 1) % matches.size();
		} else {
			lastMatchIdx = 0;
		}
		lastQuery = query;

		GuideGraph.Node target = matches.get( lastMatchIdx );
		toast.show( Messages.get( this, "match", lastMatchIdx + 1, matches.size(), GuideGraph.fullTitle( target ) ) );

		boolean opened = false;
		for (GuideGraph.Node p = target.parent; p != null; p = p.parent){
			if (!p.expanded){
				p.expanded = true;
				opened = true;
				Card pc = cards.get( p );
				if (pc != null) pc.refresh();
			}
		}
		if (opened) relayout( true );
		centerOn( target, true );
		Card c = cards.get( target );
		if (c != null) c.ring();
	}

	private static boolean contains( String s, String q ){
		return s != null && s.toLowerCase( Locale.ENGLISH ).contains( q );
	}

	//--- opening a node ---

	private void open( GuideGraph.Node node ){
		Sample.INSTANCE.play( Assets.Sounds.CLICK, 0.7f );
		if (node == root){
			int[] p = GuideGraph.progress( root );
			addToFront( new WndTitledMessage( Icons.get( Icons.WPD ), node.title(),
					Messages.get( this, "root_body", p[0], p[1], p[2] ) ) );
			return;
		}
		if (!GuideGraph.unlocked( node )){
			Image img = Icons.get( Icons.MAGNIFY );
			img.brightness( 0.5f );
			addToFront( new WndTitledMessage( img, Messages.get( this, "locked_title" ),
					Messages.get( this, "locked_body", GuideGraph.lockHint( node ) ) ) );
			return;
		}
		if (!node.children.isEmpty()){
			toggle( node );
			return;
		}

		String text = node.text();
		String sub = node.subtitle();
		String body;
		ArrayList<Mob.DropInfo> drops = null;
		if (node.mob != null){
			String msg = "";
			try {
				Mob mob = (Mob) Reflection.newInstance( node.mob );
				if (mob != null){
					msg = mob.description() + mob.dropsInfo();
					drops = mob.getDrops();
				}
			} catch (Exception e){
				//no game state to describe it from (the title screen): the journal alone
				msg = "";
				drops = null;
			}
			if (text != null){
				msg += (msg.isEmpty() ? "" : "\n\n") + Messages.get( this, "from_journal" ) + "\n\n" + text;
			}
			body = msg;
		} else {
			body = (sub != null ? "_" + sub + "_\n\n" : "")
					+ (text != null ? text : Messages.get( this, "blank" ));
		}

		//read as it opens; its card lights down, sparks flying, as it closes: the window
		//covers most of a phone's screen while it is up
		final boolean fresh = node.key != null && !GuideProgress.isRead( node.key );
		if (fresh) GuideProgress.markRead( node.key );
		addToFront( new WndGuideNode( windowIcon( node ), node.title(), body, drops ){
			@Override
			public void hide() {
				super.hide();
				if (fresh) pageClosed( node );
			}
		} );
		Sample.INSTANCE.play( Assets.Sounds.READ, 0.5f );
	}

	//a fresh page was read and its window closed: sparks fly off its card, and it and every
	//chapter above it count again
	private void pageClosed( GuideGraph.Node node ){
		Card c = cards.get( node );
		if (c != null) c.burst();
		refreshUp( node );
	}

	private Image windowIcon( GuideGraph.Node node ){
		if (node.mob != null){
			CharSprite s = bossSprite( node );
			if (s != null) return new Image( s );
		}
		return Icons.get( node.icon != null ? node.icon : Icons.INFO );
	}

	private static CharSprite bossSprite( GuideGraph.Node node ){
		try {
			Mob mob = (Mob) Reflection.newInstance( node.mob );
			CharSprite sprite = mob == null ? null : mob.sprite();
			if (sprite != null) sprite.idle();
			return sprite;
		} catch (Exception e){
			return null;
		}
	}

	//--- colours ---

	private static int lighter( int c, float f ){
		int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
		r += (int)((255 - r) * f);
		g += (int)((255 - g) * f);
		b += (int)((255 - b) * f);
		return (r << 16) | (g << 8) | b;
	}

	private static int darker( int c, float f ){
		int r = (int)(((c >> 16) & 0xFF) * (1 - f)), g = (int)(((c >> 8) & 0xFF) * (1 - f)), b = (int)((c & 0xFF) * (1 - f));
		return (r << 16) | (g << 8) | b;
	}

	//--- a card ---

	private class Card extends Group {

		static final int HIDDEN = 0, SHOWN = 1, LEAVING = 2;

		final GuideGraph.Node node;
		int state = HIDDEN;

		boolean locked, fresh, chapter;
		int unread;
		//pages found, pages in all, pages unread, below this card
		private int[] prog;

		//size and where it is drawn, in graph pixels
		int cw = CARD_W, ch = 24;
		int dispX, dispY;

		private float fromX, fromY;
		private float t = 1f, delay = 0f;
		private boolean appearing;
		private float alphaV = 1f;

		private ColorBlock shadow;
		private NinePatch frame;
		private Visual icon;
		private RenderedTextBlock title, sub;
		private BitmapText count, mark, fold;
		private ColorBlock barBg, barFill;
		private final ColorBlock[] glow = new ColorBlock[4];
		private ColorBlock[] ringBlocks;

		boolean textStale = true;
		private int iconW, iconH, textX;
		private int frameColor;

		private final float phase = Random.Float( 6.28f );
		private float ringT = -1, revealT = -1, revealWait = 0;
		private boolean pressedNow, hoveredNow;
		private float scribbleAt = 0;
		private int scribbleX = 0;

		Edge edge;

		Card( GuideGraph.Node node ){
			this.node = node;
			visible = active = false;
			if (node.parent != null){
				edge = new Edge( this );
			}
			refresh();
		}

		//rebuilds what the card shows from the guide's state (locked, read, counts)
		void refresh(){
			locked = !GuideGraph.unlocked( node );
			chapter = !node.children.isEmpty();
			int[] p = GuideGraph.progress( node );
			prog = p;
			unread = p[2];
			fresh = !locked && (GuideGraph.isNew( node ) || (chapter && unread > 0));

			clear();
			for (int i = 0; i < glow.length; i++) glow[i] = null;
			ringBlocks = null;

			frameColor = locked ? 0x5A5A5A : node.color;

			shadow = new ColorBlock( 1, 1, 0xFF000000 );
			shadow.alpha( 0.45f );
			add( shadow );

			if (fresh){
				int gc = 0xFF000000 | lighter( node.color, 0.35f );
				for (int i = 0; i < 4; i++){
					glow[i] = new ColorBlock( 1, 1, gc );
					add( glow[i] );
				}
			}

			frame = Chrome.get( node.parent == null || chapter ? Chrome.Type.TOAST_TR_HEAVY : Chrome.Type.TOAST_TR );
			frame.hardlight( frameColor );
			add( frame );

			icon = makeIcon();
			add( icon );

			if (chapter && !locked){
				barBg = new ColorBlock( 1, 1, 0xFF000000 | darker( node.color, 0.7f ) );
				add( barBg );
				barFill = new ColorBlock( 1, 1, 0xFF000000 | node.color );
				add( barFill );
				count = new BitmapText( p[0] + "/" + p[1], PixelScene.pixelFont );
				count.hardlight( lighter( node.color, 0.3f ) );
				count.measure();
				add( count );
			} else {
				barBg = barFill = null;
				count = null;
			}

			if (chapter && !locked && node.parent != null){
				fold = new BitmapText( node.expanded ? "-" : "+", PixelScene.pixelFont );
				fold.hardlight( lighter( node.color, 0.4f ) );
				fold.measure();
				add( fold );
			} else {
				fold = null;
			}

			if (fresh){
				mark = new BitmapText( "!", PixelScene.pixelFont );
				mark.hardlight( 0xFFFF66 );
				mark.measure();
				add( mark );
			} else {
				mark = null;
			}

			textStale = true;
			measure();
			if (state != HIDDEN) place( dispX, dispY, true );
		}

		private Visual makeIcon(){
			if (locked){
				//the ink refuses to take shape
				BitmapText q = new BitmapText( "?", PixelScene.pixelFont );
				q.scale.set( 2 );
				q.hardlight( 0x8A8A8A );
				q.measure();
				return q;
			}
			if (node.mob != null){
				CharSprite s = bossSprite( node );
				if (s != null) return new Portrait( s, ICON_MAX, ICON_MAX );
			}
			return Icons.get( node.icon != null ? node.icon : Icons.INFO );
		}

		//(re)renders the text for the current zoom step and works the card's size out.
		//true when the size changed
		boolean measure(){
			int ow = cw, oh = ch;
			iconW = Math.min( ICON_MAX, (int)Math.ceil( icon.width() ) );
			iconH = Math.min( ICON_MAX, (int)Math.ceil( icon.height() ) );
			int box = Math.max( ICON_BOX, iconW );
			textX = PAD + box + GAP;
			//room on the right for the fold sign and the fresh mark
			int textW = CARD_W - textX - PAD - 3;

			if (textStale){
				if (title != null) title.killAndErase();
				if (sub != null) sub.killAndErase();
				title = text( locked ? Messages.get( GuideScene.class, "locked_title" ) : node.title(),
						locked ? 0x8A8A8A : (node.parent == null ? 0xFFFF44 : node.color), textW );
				//the root's count is its bar; everything else says what it is, or where to look
				String s = node.parent == null ? null
						: locked ? GuideGraph.lockHint( node ) : node.subtitle();
				sub = s == null || s.isEmpty() ? null : text( s, locked ? 0x707070 : 0x9A9A9A, textW );
				title.alpha( alphaV );
				if (sub != null) sub.alpha( alphaV );
				textStale = false;
				//the new words are laid where the card next stands
				placedX = Integer.MIN_VALUE;
			}

			int th = (int)Math.ceil( title.height() );
			if (sub != null) th += 1 + (int)Math.ceil( sub.height() );
			//the pixel font's glyphs stand 7 rows tall with their outline
			if (count != null) th += 2 + 7;
			cw = CARD_W;
			ch = 2 * PAD + Math.max( iconH, th );
			return ow != cw || oh != ch;
		}

		private RenderedTextBlock text( String s, int color, int maxW ){
			int scale = Math.max( 1, Math.round( level * DeviceCompat.getRealPixelScaleX() ) );
			RenderedTextBlock b = new CardText( TEXT_SIZE * scale, level );
			b.zoom( 1f / scale );
			b.text( s, maxW );
			b.hardlight( color );
			add( b );
			return b;
		}

		//--- placement ---

		private int placedX = Integer.MIN_VALUE, placedY = Integer.MIN_VALUE;

		void place( int x, int y, boolean force ){
			if (!force && x == placedX && y == placedY) return;
			placedX = x;
			placedY = y;

			shadow.x = x + 1;
			shadow.y = y + 1;
			shadow.size( cw, ch );

			frame.x = x;
			frame.y = y;
			frame.size( cw, ch );

			int box = Math.max( ICON_BOX, iconW );
			icon.x = x + PAD + (box - iconW) / 2 + (locked ? scribbleX : 0);
			icon.y = y + (ch - iconH) / 2;

			int ty = y + PAD;
			title.setPos( x + textX, ty );
			ty += (int)Math.ceil( title.height() );
			if (sub != null){
				sub.setPos( x + textX, ty + 1 );
				ty += 1 + (int)Math.ceil( sub.height() );
			}
			if (count != null){
				int cy = ty + 2;
				int cwid = (int)Math.ceil( count.width() );
				count.x = x + cw - PAD - cwid;
				count.y = cy;
				int barW = cw - PAD - cwid - 3 - textX;
				int[] p = prog;
				barBg.x = x + textX;
				barBg.y = cy + 3;
				barBg.size( Math.max( 1, barW ), 1 );
				barFill.x = barBg.x;
				barFill.y = barBg.y;
				int fill = p[1] == 0 ? 0 : Math.round( barW * p[0] / (float)p[1] );
				barFill.size( Math.max( 0, fill ), 1 );
				barFill.visible = fill > 0;
			}
			if (fold != null){
				fold.x = x + cw - PAD - (int)Math.ceil( fold.width() );
				fold.y = y + PAD;
			}
			if (mark != null){
				mark.x = x + cw - 3;
				mark.y = y - 2;
			}
			if (glow[0] != null){
				glow[0].x = x - 1; glow[0].y = y - 1; glow[0].size( cw + 2, 1 );
				glow[1].x = x - 1; glow[1].y = y + ch; glow[1].size( cw + 2, 1 );
				glow[2].x = x - 1; glow[2].y = y; glow[2].size( 1, ch );
				glow[3].x = x + cw; glow[3].y = y; glow[3].size( 1, ch );
			}
		}

		void setAlpha( float a ){
			alphaV = a;
			shadow.alpha( 0.45f * a );
			frame.alpha( a );
			icon.alpha( a );
			title.alpha( a );
			if (sub != null) sub.alpha( a );
			if (count != null){
				count.alpha( a );
				barBg.alpha( a );
				barFill.alpha( a );
			}
			if (fold != null) fold.alpha( a );
			if (mark != null) mark.alpha( a );
		}

		//--- states ---

		void snap(){
			state = SHOWN;
			appearing = false;
			t = 1;
			delay = 0;
			dispX = node.lx;
			dispY = node.ly;
			setAlpha( 1 );
			place( dispX, dispY, true );
		}

		void hide(){
			state = HIDDEN;
			visible = active = false;
			if (edge != null) edge.hideAll();
		}

		void moveTo(){
			fromX = dispX;
			fromY = dispY;
			state = SHOWN;
			appearing = false;
			t = 0;
			delay = 0;
			setAlpha( 1 );
		}

		void appear( float fx, float fy, float delay ){
			if (textStale) measure();
			fromX = fx;
			fromY = fy;
			dispX = Math.round( fx );
			dispY = Math.round( fy );
			state = SHOWN;
			appearing = true;
			t = 0;
			this.delay = delay;
			setAlpha( 0 );
			place( dispX, dispY, true );
		}

		private int leaveX, leaveY;

		void leave( int tx, int ty ){
			fromX = dispX;
			fromY = dispY;
			leaveX = tx;
			leaveY = ty;
			state = LEAVING;
			appearing = false;
			t = 0;
			delay = 0;
		}

		//how far the connector to this card is drawn, 0..1
		float edgeProgress(){
			if (state == LEAVING) return 1f - GuideLayout.ease( t );
			if (appearing) return GuideLayout.ease( t );
			return 1f;
		}

		void animate( float dt ){
			if (t < 1){
				if (delay > 0){
					delay -= dt;
				} else {
					t = Math.min( 1f, t + dt / ANIM_TIME );
				}
				float tx = state == LEAVING ? leaveX : node.lx;
				float ty = state == LEAVING ? leaveY : node.ly;
				float e = appearing ? GuideLayout.easeOutBack( t ) : GuideLayout.ease( t );
				dispX = Math.round( fromX + (tx - fromX) * e );
				dispY = Math.round( fromY + (ty - fromY) * e );
				if (appearing) setAlpha( Math.min( 1f, t * 2.5f ) );
				else if (state == LEAVING) setAlpha( 1f - GuideLayout.ease( t ) );
				if (t >= 1){
					if (state == LEAVING){
						hide();
						return;
					}
					appearing = false;
				}
			}
			place( dispX, dispY, false );
		}

		void syncEdge(){
			if (edge != null) edge.attach();
		}

		//--- life ---

		@Override
		public void update() {
			super.update();

			if (glow[0] != null){
				float a = (0.3f + 0.4f * (0.5f + 0.5f * (float)Math.sin( time * 4f + phase ))) * alphaV;
				for (ColorBlock g : glow) g.alpha( a );
			}
			if (mark != null){
				mark.y = placedY - 2 + (Math.sin( time * 5f + phase ) > 0.3 ? -1 : 0);
			}
			if (locked && time >= scribbleAt){
				//the ink twitches, refusing to settle
				scribbleAt = time + Random.Float( 0.25f, 1.1f );
				scribbleX = Random.Int( 4 ) == 0 ? 1 : 0;
				icon.alpha( Random.Float( 0.45f, 0.9f ) * alphaV );
				int box = Math.max( ICON_BOX, iconW );
				icon.x = placedX + PAD + (box - iconW) / 2 + scribbleX;
			}
			if (ringT >= 0){
				if (ringBlocks == null) makeRing();
				ringT += Game.elapsed;
				float life = 0.45f;
				if (ringT >= life){
					ringT = -1;
					for (ColorBlock b : ringBlocks) b.visible = false;
				} else {
					int d = 2 + (int)(ringT / life * 6);
					float a = 1f - ringT / life;
					ringBlocks[0].x = placedX - d; ringBlocks[0].y = placedY - d; ringBlocks[0].size( cw + 2 * d, 1 );
					ringBlocks[1].x = placedX - d; ringBlocks[1].y = placedY + ch + d - 1; ringBlocks[1].size( cw + 2 * d, 1 );
					ringBlocks[2].x = placedX - d; ringBlocks[2].y = placedY - d; ringBlocks[2].size( 1, ch + 2 * d );
					ringBlocks[3].x = placedX + cw + d - 1; ringBlocks[3].y = placedY - d; ringBlocks[3].size( 1, ch + 2 * d );
					for (ColorBlock b : ringBlocks){
						b.visible = true;
						b.alpha( a * alphaV );
					}
				}
			}
			if (revealT >= 0){
				if (revealWait > 0){
					//a silhouette with no words yet, until the reveal starts
					revealWait -= Game.elapsed;
					if (revealWait <= 0){
						ring();
						burst();
					}
				} else {
					revealT += Game.elapsed;
				}
				float life = 0.9f;
				float k = Math.min( 1f, revealT / life );
				//from silhouette to colour, the words inking in after
				icon.lightness( 0.5f * GuideLayout.ease( k ) );
				title.alpha( GuideLayout.ease( Math.max( 0, k * 1.6f - 0.6f ) ) * alphaV );
				if (sub != null) sub.alpha( GuideLayout.ease( Math.max( 0, k * 1.6f - 0.6f ) ) * alphaV );
				if (revealWait <= 0 && k < 0.2f) frame.tint( 0xFFFFFF, 0.6f * (1 - k / 0.2f) );
				else applyFrameColor();
				if (k >= 1){
					revealT = -1;
					icon.resetColor();
					icon.alpha( alphaV );
					applyFrameColor();
				}
			}
		}

		private void applyFrameColor(){
			int c = frameColor;
			if (pressedNow) c = lighter( c, 0.45f );
			else if (hoveredNow) c = lighter( c, 0.25f );
			frame.hardlight( c );
		}

		void press( boolean down ){
			pressedNow = down;
			applyFrameColor();
		}

		void hover( boolean on ){
			hoveredNow = on;
			applyFrameColor();
		}

		private void makeRing(){
			ringBlocks = new ColorBlock[4];
			int c = 0xFF000000 | lighter( frameColor, 0.6f );
			for (int i = 0; i < 4; i++){
				ringBlocks[i] = new ColorBlock( 1, 1, c );
				ringBlocks[i].visible = false;
				add( ringBlocks[i] );
			}
		}

		void ring(){
			if (ringBlocks == null) makeRing();
			ringT = 0;
		}

		//from a silhouette to the page, with a ring and a burst: after `wait` seconds
		void reveal( float wait ){
			revealT = 0;
			revealWait = wait;
			if (wait <= 0){
				ring();
				burst();
			}
		}

		void burst(){
			((Sparks)fxLayer).burst( this );
		}

		void tap(){
			ring();
			open( node );
		}
	}

	//a card's words, each on the pixel grid of the zoom step it is drawn at: a text block lays
	//its words on the interface's grid, which is the screen's own only at steps that are a
	//multiple of the interface's zoom
	private static class CardText extends RenderedTextBlock {

		private final int grid;

		CardText( int size, int grid ){
			super( size );
			this.grid = grid;
		}

		@Override
		protected synchronized void layout() {
			super.layout();
			for (Gizmo g : members){
				if (g instanceof Visual){
					Visual v = (Visual) g;
					v.x = Math.round( v.x * grid ) / (float) grid;
					v.y = Math.round( v.y * grid ) / (float) grid;
				}
			}
		}
	}

	//a boss's live idle animation, cropped to a card's icon box so a giant stays the size of
	//the rest (never scaled: cropping keeps every pixel whole)
	private static class Portrait extends Image {

		private final CharSprite src;
		private final int maxW, maxH;
		//the source frame last cropped, by value: CharSprite.frame() hands out a fresh copy
		private final RectF lastFrame = new RectF( -1, -1, -1, -1 );
		private final RectF crop = new RectF();

		Portrait( CharSprite src, int maxW, int maxH ){
			super( src );
			this.src = src;
			this.maxW = maxW;
			this.maxH = maxH;
			cropFrame();
		}

		private boolean still = false;

		@Override
		public void update() {
			super.update();
			if (still) return;
			try {
				src.update();
			} catch (Exception e){
				//a sprite that can't live without its creature in a level: keep its first frame
				still = true;
				return;
			}
			cropFrame();
		}

		private void cropFrame(){
			RectF f = src.frame();
			if (f == null || texture == null) return;
			if (f.left == lastFrame.left && f.top == lastFrame.top
					&& f.right == lastFrame.right && f.bottom == lastFrame.bottom) return;
			lastFrame.set( f.left, f.top, f.right, f.bottom );
			float tw = texture.width, th = texture.height;
			float fw = Math.round( f.width() * tw ), fh = Math.round( f.height() * th );
			crop.set( f.left, f.top, f.right, f.bottom );
			if (fw > maxW){
				int cut = (int)((fw - maxW) / 2f);
				crop.left = f.left + cut / tw;
				crop.right = crop.left + maxW / tw;
			}
			if (fh > maxH){
				int cut = (int)((fh - maxH) / 3f);
				crop.top = f.top + cut / th;
				crop.bottom = crop.top + maxH / th;
			}
			frame( crop );
		}
	}

	//the right-angled line from a parent's card to a child's: three runs of 1-pixel blocks,
	//drawn out from the parent as the child appears; ink flows down it towards fresh pages
	private class Edge {

		private final Card child;
		private final ColorBlock[] legs = new ColorBlock[3];
		private ColorBlock pulse;
		private final int[] at = new int[2];
		private boolean attached = false;

		//what the legs were last laid for: they are only worked out again when it changes
		private final int[] laid = new int[11];
		private int[] path;
		private int len;
		private boolean lit;
		//which legs are drawn out, and the box round the whole line
		private final boolean[] legOn = new boolean[3];
		private int bx0, by0, bx1, by1;

		Edge( Card child ){
			this.child = child;
		}

		void attach(){
			if (attached) return;
			attached = true;
			for (int i = 0; i < legs.length; i++){
				legs[i] = new ColorBlock( 1, 1, 0xFFFFFFFF );
				legs[i].visible = false;
				edgeLayer.add( legs[i] );
			}
			pulse = new ColorBlock( 1, 1, 0xFFFFFFFF );
			pulse.visible = false;
			edgeLayer.add( pulse );
			laid[0] = Integer.MIN_VALUE;
		}

		void hideAll(){
			if (!attached) return;
			for (ColorBlock b : legs) b.visible = false;
			pulse.visible = false;
			laid[0] = Integer.MIN_VALUE;
		}

		void update(){
			if (!attached) attach();
			Card pc = cards.get( child.node.parent );
			if (child.state == Card.HIDDEN || pc == null || pc.state == Card.HIDDEN){
				hideAll();
				return;
			}
			int drawn = -1;
			if (path != null) drawn = Math.round( len * child.edgeProgress() );
			if (laid[0] != pc.dispX || laid[1] != pc.dispY || laid[2] != pc.cw || laid[3] != pc.ch
					|| laid[4] != child.dispX || laid[5] != child.dispY || laid[6] != child.cw
					|| laid[7] != child.ch || laid[8] != drawn || laid[9] != (outline ? 1 : 0)
					|| laid[10] != (child.locked ? 1 : 0)){
				path = GuideLayout.connector( pc.dispX, pc.dispY, pc.cw, pc.ch,
						child.dispX, child.dispY, child.cw, child.ch, outline );
				len = GuideLayout.length( path );
				drawn = Math.round( len * child.edgeProgress() );
				laid[0] = pc.dispX; laid[1] = pc.dispY; laid[2] = pc.cw; laid[3] = pc.ch;
				laid[4] = child.dispX; laid[5] = child.dispY; laid[6] = child.cw; laid[7] = child.ch;
				laid[8] = drawn; laid[9] = outline ? 1 : 0; laid[10] = child.locked ? 1 : 0;

				int[][] r = GuideLayout.legs( path, drawn );
				lit = !child.locked;
				int color = lit ? darker( child.node.color, 0.25f ) : 0x4A4A4A;
				bx0 = by0 = Integer.MAX_VALUE;
				bx1 = by1 = Integer.MIN_VALUE;
				for (int i = 0; i < legs.length; i++){
					ColorBlock b = legs[i];
					legOn[i] = i < r.length && r[i][2] != 0 && r[i][3] != 0;
					if (!legOn[i]) continue;
					b.x = r[i][0];
					b.y = r[i][1];
					b.size( r[i][2], r[i][3] );
					b.hardlight( color );
					b.alpha( 1f );
					bx0 = Math.min( bx0, r[i][0] );
					by0 = Math.min( by0, r[i][1] );
					bx1 = Math.max( bx1, r[i][0] + r[i][2] );
					by1 = Math.max( by1, r[i][1] + r[i][3] );
				}
			}
			//a line wholly off screen is not drawn
			boolean seen = bx1 >= viewX0 && bx0 <= viewX1 && by1 >= viewY0 && by0 <= viewY1;
			for (int i = 0; i < legs.length; i++) legs[i].visible = legOn[i] && seen;
			if (child.state == Card.LEAVING){
				for (ColorBlock b : legs) b.alpha( child.alphaV );
			}

			//ink running down towards something not yet read
			if (seen && child.fresh && drawn >= len && len > 4){
				float speed = 36f;
				float cycle = len / speed + 0.7f;
				int d = (int)(((time + child.phase) % cycle) * speed);
				if (d <= len){
					GuideLayout.pointAt( path, d, at );
					pulse.visible = true;
					pulse.x = at[0];
					pulse.y = at[1];
					pulse.hardlight( lighter( child.node.color, 0.7f ) );
				} else {
					pulse.visible = false;
				}
			} else {
				pulse.visible = false;
			}
		}
	}

	//sparks winking on the frames of what is known, and bursts when a page is read
	private class Sparks extends Group {

		private float next = 0.5f;

		private class Spark extends Group {
			final ColorBlock dot = new ColorBlock( 1, 1, 0xFFFFFFFF );
			final ColorBlock h = new ColorBlock( 3, 1, 0xFFFFFFFF );
			final ColorBlock v = new ColorBlock( 1, 3, 0xFFFFFFFF );
			float life, left;
			int x, y;
			float vx, vy, fx, fy;

			Spark(){
				add( dot );
				add( h );
				add( v );
			}

			void start( int x, int y, int color, float life, float vx, float vy ){
				this.x = x;
				this.y = y;
				fx = x;
				fy = y;
				this.vx = vx;
				this.vy = vy;
				this.life = left = life;
				dot.hardlight( color );
				h.hardlight( color );
				v.hardlight( color );
				revive();
				visible = active = true;
			}

			@Override
			public void update() {
				super.update();
				left -= Game.elapsed;
				if (left <= 0){
					kill();
					return;
				}
				fx += vx * Game.elapsed;
				fy += vy * Game.elapsed;
				int px = Math.round( fx ), py = Math.round( fy );
				float k = 1f - left / life;
				boolean cross = k > 0.3f && k < 0.6f;
				dot.x = px;
				dot.y = py;
				h.x = px - 1;
				h.y = py;
				v.x = px;
				v.y = py - 1;
				h.visible = v.visible = cross;
				dot.visible = !cross;
				float a = k < 0.3f ? k / 0.3f : 1f - (k - 0.3f) / 0.7f;
				dot.alpha( a );
				h.alpha( a * 0.8f );
				v.alpha( a * 0.8f );
			}
		}

		private Spark obtain(){
			Spark s = (Spark) getFirstAvailable( Spark.class );
			if (s == null){
				if (members.size() >= 24) return null;
				s = new Spark();
				add( s );
			}
			return s;
		}

		@Override
		public void update() {
			super.update();
			next -= Game.elapsed;
			if (next > 0) return;
			next = Random.Float( 0.2f, 0.5f );
			//a wink on a random known card that is on screen
			Card c = null;
			for (int tries = 0; tries < 6 && c == null; tries++){
				if (shown.isEmpty()) return;
				Card k = shown.get( Random.Int( shown.size() ) );
				if (k.visible && k.state == Card.SHOWN && !k.locked && k.t >= 1) c = k;
			}
			if (c == null) return;
			Spark s = obtain();
			if (s == null) return;
			int x, y;
			if (Random.Int( 2 ) == 0){
				x = c.dispX + Random.Int( c.cw );
				y = Random.Int( 2 ) == 0 ? c.dispY : c.dispY + c.ch - 1;
			} else {
				x = Random.Int( 2 ) == 0 ? c.dispX : c.dispX + c.cw - 1;
				y = c.dispY + Random.Int( c.ch );
			}
			s.start( x, y, lighter( c.node.color, 0.6f ), Random.Float( 0.5f, 0.8f ), 0, 0 );
		}

		void burst( Card c ){
			int cx = c.dispX + c.cw / 2, cy = c.dispY + c.ch / 2;
			for (int i = 0; i < 10; i++){
				Spark s = obtain();
				if (s == null) return;
				float a = Random.Float( 6.28f );
				float sp = Random.Float( 20, 45 );
				s.start( cx + Random.IntRange( -c.cw / 3, c.cw / 3 ), cy + Random.IntRange( -c.ch / 3, c.ch / 3 ),
						lighter( c.node.color, 0.5f ), Random.Float( 0.5f, 0.9f ),
						(float)Math.cos( a ) * sp, (float)Math.sin( a ) * sp );
			}
		}
	}

	//dust and the odd ember drifting up through the gloom, in whole interface pixels
	private static class Motes extends Group {

		private static final int COUNT = 26;
		private final ColorBlock[] dots = new ColorBlock[COUNT];
		private final float[] px = new float[COUNT], py = new float[COUNT], sp = new float[COUNT], ph = new float[COUNT];
		private final int w, h;
		private float time;

		Motes( int w, int h ){
			this.w = w;
			this.h = h;
			for (int i = 0; i < COUNT; i++){
				boolean ember = Random.Int( 5 ) == 0;
				dots[i] = new ColorBlock( 1, 1, ember ? 0xFFFF9A48 : 0xFFE8DCC0 );
				dots[i].alpha( ember ? Random.Float( 0.35f, 0.6f ) : Random.Float( 0.12f, 0.3f ) );
				px[i] = Random.Float( w );
				py[i] = Random.Float( h );
				sp[i] = Random.Float( 2f, 7f ) * (ember ? 1.6f : 1f);
				ph[i] = Random.Float( 6.28f );
				add( dots[i] );
			}
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			for (int i = 0; i < COUNT; i++){
				py[i] -= sp[i] * Game.elapsed;
				if (py[i] < -2){
					py[i] = h + 1;
					px[i] = Random.Float( w );
				}
				dots[i].x = (int)(px[i] + 2f * Math.sin( time * 0.6f + ph[i] ));
				dots[i].y = (int)py[i];
			}
		}
	}

	//a short line at the bottom of the screen (search results), fading after a while
	private class Toast extends Group {

		private NinePatch bg;
		private RenderedTextBlock text;
		private float left = 0;

		Toast(){
			visible = false;
		}

		void show( String msg ){
			clear();
			bg = Chrome.get( Chrome.Type.TOAST_TR );
			add( bg );
			int maxW = (int)(w - insets.left - insets.right - 52);
			text = PixelScene.renderTextBlock( msg, 6 );
			text.maxWidth( maxW );
			add( text );
			int bw = (int)Math.ceil( text.width() ) + bg.marginHor();
			int bh = (int)Math.ceil( text.height() ) + bg.marginVer();
			bg.size( bw, bh );
			bg.x = (int)(insets.left + (w - insets.left - insets.right - bw) / 2f);
			bg.y = (int)(h - insets.bottom - bh - 4);
			text.setPos( bg.x + bg.marginLeft(), bg.y + bg.marginTop() );
			align( text );
			left = 2.6f;
			visible = true;
			alpha( 1 );
		}

		private void alpha( float a ){
			if (bg != null) bg.alpha( a );
			if (text != null) text.alpha( a );
		}

		@Override
		public void update() {
			super.update();
			if (left > 0){
				left -= Game.elapsed;
				if (left < 0.5f) alpha( Math.max( 0, left / 0.5f ) );
				if (left <= 0) visible = false;
			}
		}
	}

	//the graph's camera: snaps its scroll to whole screen pixels, so at a whole zoom step
	//every graph pixel covers the same block of screen pixels
	private static class GraphCamera extends Camera {

		GraphCamera( float zoom ){
			super( 0, 0, (int)Math.ceil( Game.width / zoom ), (int)Math.ceil( Game.height / zoom ), zoom );
			fullScreen = true;
		}

		@Override
		protected void updateMatrix() {
			float sx = Math.round( (scroll.x + shakeX) * zoom ) / zoom;
			float sy = Math.round( (scroll.y + shakeY) * zoom ) / zoom;

			matrix[0] = +zoom * invW2;
			matrix[5] = -zoom * invH2;

			matrix[12] = -1 + x * invW2 - sx * matrix[0];
			matrix[13] = +1 - y * invH2 - sy * matrix[5];
		}
	}
}
