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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.ui.WorldChart;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.ScrollEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ScrollArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;

import java.util.ArrayList;

/**
 * The world at a size you can read, with you on it.
 *
 * The map is a viewport: the scroll wheel zooms toward the cursor, dragging pans, and
 * while the window is up every scroll belongs to the map - the game camera behind it
 * stays put. Tapping sets a waypoint the hero marches toward; tapping the current
 * waypoint clears it. A tap on the world itself pauses that march (Hero.handle), and
 * the hud tag resumes it.
 */
public class WndWorldMap extends Window {

	private static final int MARKER          = 0xFFFF3333;
	private static final int WAYPOINT_MARKER = 0xFF3366FF;

	//at 12x the fit zoom a chart pixel is several screen pixels - past that is just mud
	private static final float MAX_VIEW = 12f;

	private final float baseZoom;
	private float viewZoom = 1f;
	private final int side;

	private final OverworldLevel level;
	private final WorldChart chart;
	private final Component page;
	private final ScrollPane pane;
	private final ArrayList<Pin> pins = new ArrayList<>();

	private static class Pin {
		final Image dot;
		float x, y;
		Pin( Image dot, float x, float y ){
			this.dot = dot;
			this.x = x;
			this.y = y;
		}
	}

	//settlement names sit beside the chart's site dots. Neighbouring sectors take
	//opposite corners of their dot (side-by-side sectors alternate above/below, stacked
	//ones left/right, by sector parity) so close names do not pile up
	private static class Label {
		final RenderedTextBlock text;
		final float x, y;
		final boolean right, below;
		Label( RenderedTextBlock text, float x, float y, boolean right, boolean below ){
			this.text = text;
			this.x = x;
			this.y = y;
			this.right = right;
			this.below = below;
		}
	}
	private final ArrayList<Label> labels = new ArrayList<>();

	//the hero's pin tracks them live - the world keeps running under an open map
	private Pin heroPin;

	//the view rides the hero until the player takes the wheel by panning or zooming
	private boolean following = true;

	public WndWorldMap( OverworldLevel level ){
		super();

		this.level = level;

		//while the map is up, the wheel belongs to the map. Registered before the
		//pane's own controller, so the dispatch stack reads: pane first, then this,
		//and the game's cell selector - registered at scene creation - never
		ScrollArea scrollBlocker = new ScrollArea(
				0, 0, PixelScene.uiCamera.width, PixelScene.uiCamera.height ){};
		scrollBlocker.camera = PixelScene.uiCamera;
		//scrolls are eaten, but taps must fall through to the window's own blocker
		//or tapping outside the map would no longer close it
		scrollBlocker.blockLevel = com.watabou.noosa.PointerArea.NEVER_BLOCK;
		add( scrollBlocker );

		//as large as the screen allows. A chart that fits gets whole pixels (fractional
		//upscaling smears the coastline); one bigger than the screen is downsampled
		int room = (int)Math.min( PixelScene.uiCamera.width, PixelScene.uiCamera.height ) - 16;
		baseZoom = room >= WorldChart.SIZE
				? room / WorldChart.SIZE
				: room / (float)WorldChart.SIZE;
		side = (int)(WorldChart.SIZE * baseZoom);

		int heroWx = level.worldX + Dungeon.hero.pos % level.width();
		int heroWy = level.worldY + Dungeon.hero.pos / level.width();

		page = new Component();
		chart = new WorldChart( level.worldSeed, heroWx, heroWy );
		page.add( chart );

		if (level.waypointActive){
			pin( WAYPOINT_MARKER, level.waypointX, level.waypointY );
		}
		heroPin = pin( MARKER, heroWx, heroWy );

		labelSites();

		applyView();

		//size (and thereby centre) the window BEFORE the pane lays out its clip
		//camera - the scissor is computed from the window camera's screen position,
		//and a zero-sized window puts it somewhere the map is not
		resize( side, side );

		pane = new ScrollPane( page ){
			@Override
			public void onClick( float cx, float cy ){
				tap( cx, cy );
			}

			@Override
			protected void createChildren(){
				controller = new PointerController(){
					{
						//a map wants to pan on a nudge, not after a long haul
						dragThreshold = PixelScene.defaultZoom * 2;
					}

					@Override
					protected void onDrag( com.watabou.input.PointerEvent event ){
						//the player is steering now - stop dragging the view back
						//to the hero under their finger
						following = false;
						super.onDrag( event );
					}

					@Override
					protected void onScroll( ScrollEvent event ){
						//zoom toward the cursor rather than scrolling: on a map
						//the wheel means magnify, and the anchor point is where
						//the player is already looking
						PointF p = content.camera.screenToCamera(
								(int) event.pos.x, (int) event.pos.y );
						zoomAt( p.x, p.y, (float) Math.pow( 1.15f, -event.amount ) );
					}
				};
				add( controller );

				thumb = new ColorBlock( 1, 1, THUMB_COLOR );
				thumb.am = THUMB_ALPHA;
				add( thumb );
			}
		};
		add( pane );
		pane.setRect( 0, 0, side, side );

		centreOnHero();

		IconButton close = new IconButton( Icons.get( Icons.CLOSE ) ){
			@Override
			protected void onClick(){
				hide();
			}
		};
		close.setRect( side - 12, 0, 12, 12 );
		add( close );

		//the wheel is a desktop luxury - on a touchscreen the only other way in is a
		//pinch, so the map carries its own zoom
		RedButton zoomOut = new RedButton( "-", 9 ){
			@Override
			protected void onClick(){
				zoomBy( 1 / 1.5f );
			}
		};
		zoomOut.setRect( side - 30, side - 14, 14, 14 );
		add( zoomOut );

		RedButton zoomIn = new RedButton( "+", 9 ){
			@Override
			protected void onClick(){
				zoomBy( 1.5f );
			}
		};
		zoomIn.setRect( side - 15, side - 14, 14, 14 );
		add( zoomIn );

		resize( side, side );
	}

	@Override
	public void offset( int xOffset, int yOffset ){
		super.offset( xOffset, yOffset );
		if (pane != null) pane.setRect( 0, 0, side, side );
	}

	private Pin pin( int colour, int wx, int wy ){
		Image dot = new Image( TextureCache.createSolid( colour ) );
		page.add( dot );
		Pin p = new Pin( dot, chart.chartX( wx ), chart.chartY( wy ) );
		pins.add( p );
		place( p );
		return p;
	}

	//one name per village on the chart's slab (the same sectors the chart dotted),
	//and the town on the world origin
	private void labelSites(){
		int s0x = Math.floorDiv( chart.originX, WorldStructures.SECTOR ) - 1;
		int s0y = Math.floorDiv( chart.originY, WorldStructures.SECTOR ) - 1;
		int span = WorldChart.SPAN / WorldStructures.SECTOR + 2;
		for (int sy = s0y; sy <= s0y + span; sy++){
			for (int sx = s0x; sx <= s0x + span; sx++){
				if (WorldStructures.siteType( level.worldSeed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				label( WorldStructures.villageName( level.worldSeed, sx, sy ),
						chart.chartX( WorldStructures.siteX( level.worldSeed, sx, sy ) ),
						chart.chartY( WorldStructures.siteY( level.worldSeed, sx, sy ) ),
						(sy & 1) == 0, (sx & 1) == 0 );
			}
		}
		label( Messages.get( WndWorldMap.class, "town" ), chart.chartX( 0 ), chart.chartY( 0 ), true, false );
	}

	private void label( String name, float x, float y, boolean right, boolean below ){
		RenderedTextBlock text = PixelScene.renderTextBlock( name, 6 );
		page.add( text );
		Label l = new Label( text, x, y, right, below );
		labels.add( l );
		place( l );
	}

	//labels, like pins, keep their on-screen size; they hang off a corner of their dot
	private void place( Label l ){
		float s = baseZoom * viewZoom;
		l.text.visible = l.x >= 0 && l.y >= 0
				&& l.x < WorldChart.SIZE && l.y < WorldChart.SIZE;
		//the chart's site dots are 3 chart pixels wide
		float half = 1.5f * s + 1;
		float tx = l.right ? l.x * s + half : l.x * s - half - l.text.width();
		float ty = l.below ? l.y * s + half : l.y * s - half - l.text.height();
		l.text.setPos( tx, ty );
	}

	//pins keep a constant on-screen size; only their anchors scale
	private void place( Pin p ){
		float s = baseZoom * viewZoom;
		float size = Math.max( 3, baseZoom * 3 );
		//a pin whose world position has left the painted slab has nothing to mark
		p.dot.visible = p.x >= 0 && p.y >= 0
				&& p.x < WorldChart.SIZE && p.y < WorldChart.SIZE;
		p.dot.scale.set( size );
		p.dot.x = p.x * s - size / 2f;
		p.dot.y = p.y * s - size / 2f;
	}

	@Override
	public synchronized void update(){
		super.update();
		//the map is a window over a running world: the hero keeps walking while
		//it is open, so their pin has to walk too
		if (heroPin != null && Dungeon.level == level && Dungeon.hero != null){
			float hx = chart.chartX( level.worldX + Dungeon.hero.pos % level.width() );
			float hy = chart.chartY( level.worldY + Dungeon.hero.pos / level.width() );
			if (hx != heroPin.x || hy != heroPin.y){
				heroPin.x = hx;
				heroPin.y = hy;
				place( heroPin );
				if (following) centreOnHero();
			}
		}
	}

	/** Puts the hero's pin in the middle of the viewport. */
	private void centreOnHero(){
		if (heroPin == null || pane == null) return;
		float s = baseZoom * viewZoom;
		pane.scrollTo( heroPin.x * s - side / 2f, heroPin.y * s - side / 2f );
	}

	/** Lays the page out for the current zoom: the chart scales, pins follow. */
	private void applyView(){
		float s = baseZoom * viewZoom;

		chart.scale.set( s );
		chart.x = 0;
		chart.y = 0;

		for (Pin p : pins){
			place( p );
		}
		for (Label l : labels){
			place( l );
		}

		page.setSize( WorldChart.SIZE * s, WorldChart.SIZE * s );
	}

	/** Rescales around whatever is in the middle of the viewport. */
	private void zoomBy( float factor ){
		Camera c = page.camera;
		zoomAt( c.scroll.x + side / 2f, c.scroll.y + side / 2f, factor );
	}

	/** Rescales around a content-space anchor, keeping it fixed under the cursor. */
	private void zoomAt( float cx, float cy, float factor ){
		float old = viewZoom;
		viewZoom = GameMath.gate( 1f, viewZoom * factor, MAX_VIEW );
		if (viewZoom == old) return;

		float f = viewZoom / old;
		Camera c = page.camera;
		float keepX = cx - c.scroll.x;
		float keepY = cy - c.scroll.y;

		applyView();
		//while the view still rides the hero, magnifying means magnifying THEM -
		//anchoring on the cursor would slide them off the middle of the map
		if (following){
			centreOnHero();
		} else {
			pane.scrollTo( cx * f - keepX, cy * f - keepY );
		}
	}

	/** A tap in content space: set (or clear) the march waypoint. */
	private void tap( float cx, float cy ){
		if (Dungeon.level != level || Dungeon.hero == null) return;

		float s = baseZoom * viewZoom;
		float px = cx / s, py = cy / s;
		if (px < 0 || py < 0 || px >= WorldChart.SIZE || py >= WorldChart.SIZE){
			return;
		}
		int wx = chart.worldX( px );
		int wy = chart.worldY( py );

		//"the same spot" at map scale: within a marker's blob of the waypoint
		int near = (int)Math.ceil( 4 / s ) * WorldChart.STRIDE;
		if (level.waypointActive
				&& Math.abs( wx - level.waypointX ) <= near
				&& Math.abs( wy - level.waypointY ) <= near){
			level.clearWaypoint();
			GLog.i( Messages.get( WndWorldMap.class, "waypoint_cleared" ) );
		} else {
			level.setWaypoint( wx, wy );
			GLog.p( Messages.get( WndWorldMap.class, "waypoint_set" ) );
		}
		hide();
	}
}
