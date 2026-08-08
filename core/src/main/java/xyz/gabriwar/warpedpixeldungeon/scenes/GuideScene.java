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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.Journal;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.windows.WndGuideNode;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.windows.WndJournalItem;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ScrollArea;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

//Fullscreen node-graph view of the Descent Guide. POC.
public class GuideScene extends PixelScene {

	//model-space spacing
	private static final float SPACING_X = 150;
	private static final float SPACING_Y = 76;
	private static final float NODE_GAP_Y = 26;

	private static final float MIN_ZOOM = 0.4f;
	private static final float MAX_ZOOM = 3f;

	private GuideGraph.Node root;

	private Group edgeGroup;
	private Group nodeGroup;

	private final HashMap<GuideGraph.Node, NodeWidget> widgets = new HashMap<>();
	private final ArrayList<ColorBlock> edges = new ArrayList<>();

	//view transform: screen = offset + model * zoom
	private float viewZoom = 1f;
	private float offX, offY;

	private String lastQuery = "";
	private int lastMatchIdx = 0;

	//where back leads: the title journal by default, or the game when opened from the
	//in-game guidebook tab. one-shot - resets after use.
	public static Class<? extends PixelScene> returnScene = JournalScene.class;

	@Override
	public void create() {
		super.create();

		//guide pages unlock from global progress (visited depths, seen bosses)
		Journal.loadGlobal();

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;

		//the living title-screen backdrop, dimmed so the graph stays readable
		TitleBackground bg = new TitleBackground( w, h );
		add( bg );
		add( new ColorBlock( w, h, 0xBB15151A ) );

		//pan area, added first so node buttons get pointer priority (zoom is +/- buttons)
		ScrollArea panArea = new ScrollArea( 0, 0, w, h ){
			private final PointF lastPos = new PointF();

			@Override
			protected void onPointerDown( PointerEvent event ) {
				lastPos.set( event.current );
			}

			@Override
			protected void onDrag( PointerEvent event ) {
				//pointer events are in screen pixels, scene is in camera units
				offX += (event.current.x - lastPos.x) / camera.zoom;
				offY += (event.current.y - lastPos.y) / camera.zoom;
				lastPos.set( event.current );
				applyTransform();
			}
		};
		panArea.blockLevel = ScrollArea.NEVER_BLOCK;
		add( panArea );

		edgeGroup = new Group();
		add( edgeGroup );
		nodeGroup = new Group();
		add( nodeGroup );

		root = GuideGraph.build();
		rebuild( null );

		offX = 20;
		offY = h / 2f;
		applyTransform();

		//--- UI overlay ---

		RenderedTextBlock title = PixelScene.renderTextBlock( "Descent Guide", 9 );
		title.hardlight( 0xFFFF44 );
		title.setPos( (w - title.width()) / 2f, 4 );
		align( title );
		add( title );

		StyledButton btnSearch = new StyledButton( Chrome.Type.GREY_BUTTON_TR, "Search" ){
			@Override
			protected void onClick() {
				showSearch();
			}
		};
		btnSearch.icon( Icons.MAGNIFY.get() );
		btnSearch.setRect( 2, 2, 55, 16 );
		add( btnSearch );

		ExitButton btnExit = new ExitButton(){
			@Override
			protected void onClick() {
				onBackPressed();
			}
		};
		btnExit.setPos( w - btnExit.width(), 0 );
		add( btnExit );

		//zoom controls, bottom-right; zoom is centered on the middle of the screen
		StyledButton btnZoomIn = new StyledButton( Chrome.Type.GREY_BUTTON_TR, "+" ){
			@Override
			protected void onClick() {
				zoomAround( w / 2f, h / 2f, viewZoom * 1.3f );
			}
		};
		btnZoomIn.setRect( w - 20, h - 42, 18, 18 );
		add( btnZoomIn );

		StyledButton btnZoomOut = new StyledButton( Chrome.Type.GREY_BUTTON_TR, "-" ){
			@Override
			protected void onClick() {
				zoomAround( w / 2f, h / 2f, viewZoom / 1.3f );
			}
		};
		btnZoomOut.setRect( w - 20, h - 22, 18, 18 );
		add( btnZoomOut );

		RenderedTextBlock hint = PixelScene.renderTextBlock(
				"drag: pan  ·  +/-: zoom  ·  tap: expand", 6 );
		hint.hardlight( 0x888888 );
		hint.setPos( (w - hint.width()) / 2f, h - hint.height() - 2 );
		align( hint );
		add( hint );

		fadeIn();
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

	//--- graph building & layout ---

	//expand/collapse animation state
	private static final float ANIM_TIME = 0.25f;
	private final ArrayList<NodeWidget> ghosts = new ArrayList<>();

	private static float ease( float t ){
		return t * t * (3f - 2f * t);
	}

	//destroys and recreates all widgets/edges for currently visible nodes, then lays out.
	//anchor is the node that was toggled: children slide/fade in from it on expand, and
	//collapse back into it as fading ghosts. null = instant (initial build, search jumps).
	private void rebuild( GuideGraph.Node anchor ){
		//snapshot pre-layout model positions for the slide animation
		HashMap<GuideGraph.Node, PointF> oldPos = new HashMap<>();
		for (GuideGraph.Node n : widgets.keySet()){
			oldPos.put( n, new PointF( n.mx, n.my ) );
		}
		PointF anchorOld = anchor != null ? oldPos.get( anchor ) : null;

		//widgets whose nodes are no longer visible become fading ghosts
		ArrayList<GuideGraph.Node> visibleNow = new ArrayList<>();
		collectVisible( root, visibleNow );
		for (HashMap.Entry<GuideGraph.Node, NodeWidget> e : widgets.entrySet()){
			NodeWidget wdg = e.getValue();
			if (anchorOld != null && !visibleNow.contains( e.getKey() )){
				wdg.dying = true;
				wdg.animT = 0;
				wdg.fromX = e.getKey().mx;
				wdg.fromY = e.getKey().my;
				ghosts.add( wdg );
			} else {
				wdg.destroy();
				wdg.killAndErase();
			}
		}
		widgets.clear();
		for (ColorBlock e : edges){
			e.killAndErase();
		}
		edges.clear();

		buildVisible( root );
		nextY = 0;
		layoutTree( root, 0 );

		//ghosts collapse toward the anchor's settled position
		if (anchor != null){
			for (NodeWidget g : ghosts){
				g.toX = anchor.mx;
				g.toY = anchor.my;
			}
		}

		//surviving widgets slide from where they were; new ones grow out of the anchor
		if (anchorOld != null){
			for (HashMap.Entry<GuideGraph.Node, NodeWidget> e : widgets.entrySet()){
				NodeWidget wdg = e.getValue();
				PointF was = oldPos.get( e.getKey() );
				wdg.animT = 0;
				if (was != null){
					wdg.fromX = was.x;
					wdg.fromY = was.y;
				} else {
					wdg.fromX = anchorOld.x;
					wdg.fromY = anchorOld.y;
					wdg.appearing = true;
				}
			}
		}

		applyTransform();
	}

	@Override
	public synchronized void update() {
		super.update();

		boolean animating = false;
		for (NodeWidget wdg : widgets.values()){
			if (wdg.animT < 1f){
				wdg.animT = Math.min( 1f, wdg.animT + Game.elapsed / ANIM_TIME );
				animating = true;
			}
		}
		for (Iterator<NodeWidget> it = ghosts.iterator(); it.hasNext();){
			NodeWidget g = it.next();
			g.animT = Math.min( 1f, g.animT + Game.elapsed / ANIM_TIME );
			animating = true;
			if (g.animT >= 1f){
				g.destroy();
				g.killAndErase();
				it.remove();
			}
		}
		if (animating) applyTransform();
	}

	private void buildVisible( GuideGraph.Node node ){
		NodeWidget wdg = new NodeWidget( node );
		widgets.put( node, wdg );
		nodeGroup.add( wdg );

		if (node.parent != null){
			ColorBlock edge = new ColorBlock( 1, 1, 0x66000000 | (node.color & 0xFFFFFF) );
			edges.add( edge );
			edgeGroup.add( edge );
		}

		if (node.expanded){
			for (GuideGraph.Node c : node.children){
				buildVisible( c );
			}
		}
	}

	//simple tidy-tree: leaves stack vertically, parents center on their children
	private float nextY = 0;

	private float layoutTree( GuideGraph.Node node, int depth ){
		node.mx = depth * SPACING_X;
		if (!node.expanded || node.children.isEmpty()){
			NodeWidget wdg = widgets.get( node );
			//leaves get at least SPACING_Y, more if their card is tall
			float need = Math.max( SPACING_Y, (wdg != null ? wdg.modelH : 0) + NODE_GAP_Y );
			node.my = nextY + need / 2f;
			nextY += need;
		} else {
			float first = -1, last = -1;
			for (GuideGraph.Node c : node.children){
				float cy = layoutTree( c, depth + 1 );
				if (first < 0) first = cy;
				last = cy;
			}
			node.my = (first + last) / 2f;
		}
		return node.my;
	}

	//--- view transform ---

	private void zoomAround( float sx, float sy, float newZoom ){
		newZoom = GameMath.gate( MIN_ZOOM, newZoom, MAX_ZOOM );
		offX = sx - (sx - offX) * (newZoom / viewZoom);
		offY = sy - (sy - offY) * (newZoom / viewZoom);
		viewZoom = newZoom;
		applyTransform();
	}

	private void applyTransform(){
		int i = 0;
		ArrayList<GuideGraph.Node> ordered = new ArrayList<>();
		collectVisible( root, ordered );

		//interpolated model positions, shared by widgets and edges so they stay glued
		HashMap<GuideGraph.Node, PointF> ipos = new HashMap<>();

		for (GuideGraph.Node node : ordered){
			NodeWidget wdg = widgets.get( node );
			if (wdg == null) continue;
			float mx = node.mx, my = node.my;
			if (wdg.animT < 1f){
				float t = ease( wdg.animT );
				mx = wdg.fromX + (node.mx - wdg.fromX) * t;
				my = wdg.fromY + (node.my - wdg.fromY) * t;
			}
			ipos.put( node, new PointF( mx, my ) );
			wdg.place( offX + mx * viewZoom, offY + my * viewZoom, viewZoom );
			wdg.fade( wdg.appearing ? ease( wdg.animT ) : 1f );
		}

		//collapsing ghosts shrink back into their parent and fade out
		for (NodeWidget g : ghosts){
			float t = ease( g.animT );
			float mx = g.fromX + (g.toX - g.fromX) * t;
			float my = g.fromY + (g.toY - g.fromY) * t;
			g.place( offX + mx * viewZoom, offY + my * viewZoom, viewZoom );
			g.fade( 1f - t );
		}

		//edges, same traversal order as buildVisible minus the root
		for (GuideGraph.Node node : ordered){
			if (node.parent == null) continue;
			if (i >= edges.size()) break;
			ColorBlock edge = edges.get( i++ );

			NodeWidget pw = widgets.get( node.parent );
			NodeWidget cw = widgets.get( node );

			PointF pp = ipos.get( node.parent );
			PointF cp = ipos.get( node );
			float pmx = pp != null ? pp.x : node.parent.mx;
			float pmy = pp != null ? pp.y : node.parent.my;
			float cmx = cp != null ? cp.x : node.mx;
			float cmy = cp != null ? cp.y : node.my;

			float x1 = offX + (pmx * viewZoom) + (pw != null ? pw.boxW / 2f : 9 * viewZoom);
			float y1 = offY + pmy * viewZoom;
			float x2 = offX + (cmx * viewZoom) - (cw != null ? cw.boxW / 2f : 9 * viewZoom);
			float y2 = offY + cmy * viewZoom;

			float dx = x2 - x1, dy = y2 - y1;
			float len = (float)Math.sqrt( dx*dx + dy*dy );
			edge.point( new PointF( x1, y1 ) );
			edge.size( len, 1 );
			edge.angle = (float)Math.toDegrees( Math.atan2( dy, dx ) );
			edge.alpha( cw != null && cw.appearing ? ease( cw.animT ) : 1f );
		}
	}

	private void collectVisible( GuideGraph.Node node, ArrayList<GuideGraph.Node> out ){
		out.add( node );
		if (node.expanded){
			for (GuideGraph.Node c : node.children){
				collectVisible( c, out );
			}
		}
	}

	//--- search ---

	private void showSearch(){
		add( new WndTextInput( "Search the Guide", null, lastQuery, 30, false, "Search", "Cancel" ){
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
		findMatches( root, query.toLowerCase( Locale.ENGLISH ), matches );

		if (matches.isEmpty()){
			lastQuery = query;
			lastMatchIdx = 0;
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

		//expand ancestors so the target is visible
		GuideGraph.Node p = target.parent;
		while (p != null){
			p.expanded = true;
			p = p.parent;
		}
		rebuild( null );

		//center view on target
		offX = Camera.main.width / 2f - target.mx * viewZoom;
		offY = Camera.main.height / 2f - target.my * viewZoom;
		applyTransform();

		NodeWidget wdg = widgets.get( target );
		if (wdg != null) wdg.flash();
	}

	private void findMatches( GuideGraph.Node node, String query, ArrayList<GuideGraph.Node> out ){
		//locked knowledge is invisible to search — no spoilers
		if (!GuideGraph.unlocked( node )) return;
		if (node.title.toLowerCase( Locale.ENGLISH ).contains( query )
				|| (node.subtitle != null && node.subtitle.toLowerCase( Locale.ENGLISH ).contains( query ))){
			out.add( node );
		}
		for (GuideGraph.Node c : node.children){
			findMatches( c, query, out );
		}
	}

	//--- node widget ---

	private class NodeWidget extends Button {

		private static final float ICON_BOX = 18;
		private static final float MAX_TEXT_W = 68;

		private final GuideGraph.Node node;
		private final boolean locked;

		private NinePatch bg;
		private Image icon;
		private RenderedTextBlock label;
		private RenderedTextBlock sub;

		private String labelText;
		private String subText;
		private int curTextSize = -1;

		private float baseIconScale = 1f;
		private float flashTime = 0;

		//screen-space center + zoom + card size, set by place()
		private float cx, cy, z = 1;
		private float boxW, boxH;
		//card height in model space (i.e. at zoom 1), used by the tree layout
		private float modelH;

		//expand/collapse animation: slide from (fromX,fromY) toward the layout position
		//(or toward (toX,toY) for dying ghosts), animT runs 0..1
		float fromX, fromY, toX, toY;
		float animT = 1f;
		boolean appearing = false;
		boolean dying = false;

		private NodeWidget( GuideGraph.Node node ){
			super();
			this.node = node;
			this.locked = !GuideGraph.unlocked( node );

			bg = Chrome.get( Chrome.Type.TOAST_TR );
			bg.hardlight( locked ? 0x555555 : node.color );
			add( bg );

			icon = makeIcon( node );
			if (locked) icon.lightness( 0f );
			//normalize so large boss sprites don't dwarf everything
			float max = Math.max( icon.width(), icon.height() );
			if (max > ICON_BOX * 1.5f){
				baseIconScale = ICON_BOX * 1.5f / max;
			}
			add( icon );

			if (locked){
				labelText = "???";
				subText = GuideGraph.lockHint( node );
			} else {
				labelText = node.title;
				if (!node.children.isEmpty() && node.parent != null){
					labelText += node.expanded ? " [-]" : " [+" + node.children.size() + "]";
				}
				subText = node.subtitle == null ? "" : node.subtitle;
			}

			ensureText( 1f );
			modelH = 2*4 + icon.height() * baseIconScale + 2 + label.height()
					+ (subText.isEmpty() ? 0 : 2 + sub.height());
		}

		//re-renders the text at a font size matching the zoom level, so glyphs
		// stay crisp instead of being texture-scaled (which looks blurry)
		private void ensureText( float zoom ){
			int ts = Math.max( 4, Math.round( 6 * zoom ) );
			if (ts == curTextSize) return;
			curTextSize = ts;

			if (label != null) label.killAndErase();
			label = PixelScene.renderTextBlock( labelText, ts );
			label.hardlight( locked ? 0x888888 : node.color );
			label.maxWidth( (int)Math.ceil( MAX_TEXT_W * zoom ) );
			add( label );

			if (sub != null) sub.killAndErase();
			sub = PixelScene.renderTextBlock( subText, Math.max( 4, Math.round( 5 * zoom ) ) );
			sub.hardlight( 0x999999 );
			sub.maxWidth( (int)Math.ceil( MAX_TEXT_W * zoom ) );
			add( sub );
		}

		private Image makeIcon( GuideGraph.Node node ){
			//root shows the app logo
			if (node.parent == null){
				return new Image( Assets.Interfaces.APP_ICON );
			}
			if (node.mob != null){
				try {
					Mob mob = (Mob) Reflection.newInstance( node.mob );
					CharSprite sprite = mob.sprite();
					if (sprite != null){
						sprite.idle();
						return new Image( sprite );
					}
				} catch (Exception e) {
					//fall through to icon
				}
			}
			if (node.icon != null){
				return Icons.get( node.icon );
			}
			return Icons.get( Icons.SKULL );
		}

		private void place( float cx, float cy, float zoom ){
			this.cx = cx;
			this.cy = cy;
			this.z = zoom;

			icon.scale.set( baseIconScale * zoom );
			ensureText( zoom );
			label.maxWidth( (int)Math.ceil( MAX_TEXT_W * zoom ) );
			sub.maxWidth( (int)Math.ceil( MAX_TEXT_W * zoom ) );

			float pad = 4 * zoom;
			float gap = 2 * zoom;

			float iw = icon.width();
			float ih = icon.height();

			boolean hasSub = sub.text() != null && !sub.text().isEmpty();

			float innerW = Math.max( iw, Math.max( label.width(), hasSub ? sub.width() : 0 ) );
			float innerH = ih + gap + label.height() + (hasSub ? gap + sub.height() : 0);

			boxW = innerW + 2 * pad;
			boxH = innerH + 2 * pad;

			bg.size( boxW, boxH );
			bg.x = cx - boxW / 2f;
			bg.y = cy - boxH / 2f;
			align( bg );

			icon.x = cx - iw / 2f;
			icon.y = bg.y + pad;
			align( icon );

			label.setPos( cx - label.width() / 2f, icon.y + ih + gap );
			align( label );

			if (hasSub){
				sub.setPos( cx - sub.width() / 2f, label.bottom() + gap );
				align( sub );
			}

			if (!dying){
				setRect( bg.x, bg.y, boxW, boxH );
			} else {
				//ghosts are scenery: never clickable
				hotArea.width = hotArea.height = 0;
			}
		}

		//uniform transparency for the whole card (expand/collapse fades)
		private void fade( float a ){
			bg.alpha( a );
			icon.alpha( a );
			if (label != null) label.alpha( a );
			if (sub != null) sub.alpha( a );
		}

		@Override
		protected void layout() {
			super.layout();
			//children are positioned in place(); Button.layout only moves hotArea
		}

		@Override
		public void update() {
			super.update();
			if (flashTime > 0){
				flashTime -= Game.elapsed;
				float v = 0.5f + (float)Math.abs( Math.cos( flashTime * 8 ) ) / 2f;
				label.alpha( v );
				if (flashTime <= 0) label.alpha( 1f );
			}
		}

		private void flash(){
			flashTime = 2f;
		}

		@Override
		protected void onClick() {
			if (node.parent == null){
				//root never collapses
				return;
			}
			if (locked){
				Image img = makeIcon( node );
				img.scale.set( 1 );
				img.lightness( 0f );
				GuideScene.this.addToFront( new WndTitledMessage( img, "???",
						"_" + GuideGraph.lockHint( node ) + "._\n\nThe ink on these pages refuses to take shape. The dungeon does not share what you have not yet lived." ) );
				return;
			}
			if (!node.children.isEmpty()){
				node.expanded = !node.expanded;
				rebuild( node );
			} else if (node.mob != null){
				Mob mob = (Mob) Reflection.newInstance( node.mob );
				if (mob != null){
					Image img = makeIcon( node );
					img.scale.set( 1 );
					String msg = mob.description() + mob.dropsInfo();
					String guideText = node.key == null ? Messages.NO_TEXT_FOUND : Messages.get( "guide." + node.key );
					if (!guideText.equals( Messages.NO_TEXT_FOUND )){
						msg += "\n\n_From the delver's journal:_\n\n" + guideText;
						GuideScene.this.addToFront( new WndGuideNode( img, node.title, msg, mob.getDrops() ) );
					} else {
						GuideScene.this.addToFront( new WndJournalItem( img, node.title, msg, mob.getDrops() ) );
					}
				}
			} else {
				String guideText = node.key == null ? Messages.NO_TEXT_FOUND : Messages.get( "guide." + node.key );
				Image img = makeIcon( node );
				img.scale.set( 1 );
				if (!guideText.equals( Messages.NO_TEXT_FOUND )){
					GuideScene.this.addToFront( new WndGuideNode( img, node.title,
							(node.subtitle != null ? "_" + node.subtitle + "_\n\n" : "") + guideText ) );
				} else {
					GuideScene.this.add( new WndTitledMessage( img, node.title,
							(node.subtitle != null ? "_" + node.subtitle + "_\n\n" : "")
									+ "These pages are still blank." ) );
				}
			}
		}

		@Override
		protected String hoverText() {
			return node.subtitle;
		}
	}
}
