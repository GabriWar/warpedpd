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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.input.ScrollEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ScrollArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Signal;

public class ScrollPane extends Component {

	protected static final int THUMB_COLOR		= 0xFF7b8073;
	protected static final float THUMB_ALPHA	= 0.5f;

	protected PointerController controller;
	protected Signal.Listener<KeyEvent> keyListener;
	protected Component content;
	protected ColorBlock thumb;

	private float keyScroll = 0;
	//the scroll keys this pane saw pressed and not yet let go
	private boolean keyIn, keyOut;

	//dragged over its buttons too (dragOverButtons): they let presses through to the pane
	private boolean overButtons = false;

	public ScrollPane( Component content ) {
		super();

		this.content = content;
		addToBack( content );

		width = content.width();
		height = content.height();

		content.camera = new Camera( 0, 0, 1, 1, PixelScene.defaultZoom );
		Camera.add( content.camera );

		KeyEvent.addKeyListener(keyListener = new Signal.Listener<KeyEvent>() {
			@Override
			public boolean onSignal(KeyEvent keyEvent) {
				GameAction action = KeyBindings.getActionForKey(keyEvent);
				if (action != WPDAction.ZOOM_IN && action != WPDAction.ZOOM_OUT) return false;
				//a hidden pane (another tab's) leaves the keys to the one on screen
				if (!isVisible() || !isActive()) return false;
				//a key let go that was pressed before this pane came on screen changes nothing
				//here (it used to scroll the other way until the next press), but is still taken
				if (action == WPDAction.ZOOM_IN) keyIn = keyEvent.pressed;
				else keyOut = keyEvent.pressed;
				keyScroll = (keyIn ? 1 : 0) - (keyOut ? 1 : 0);
				return true;
			}
		});
	}

	/** Forgets the scroll keys held down: for a pane going off screen, which will not hear
	 *  them let go (another tab's pane takes the keys while this one is hidden). */
	public void releaseKeys() {
		keyIn = keyOut = false;
		keyScroll = 0;
	}

	@Override
	public void destroy() {
		super.destroy();
		Camera.remove( content.camera );
		KeyEvent.removeKeyListener(keyListener);
	}

	public void scrollTo( float x, float y ) {
		Camera c = content.camera;
		c.scroll.set( x, y );
		if (c.scroll.x + width > content.width()) {
			c.scroll.x = content.width() - width;
		}
		if (c.scroll.x < 0) {
			c.scroll.x = 0;
		}
		if (c.scroll.y + height > content.height()) {
			c.scroll.y = content.height() - height;
		}
		if (c.scroll.y < 0) {
			c.scroll.y = 0;
		}
		thumb.y = this.y + height * c.scroll.y / content.height();
	}

	@Override
	public synchronized void update() {
		super.update();
		if (keyScroll != 0){
			scrollTo(content.camera.scroll.x, content.camera.scroll.y + (keyScroll * 150 * Game.elapsed));
		}
		if (overButtons) letThrough( content );
	}

	@Override
	protected void createChildren() {
		controller = new PointerController();
		add( controller );

		thumb = new ColorBlock( 1, 1, THUMB_COLOR );
		thumb.am = THUMB_ALPHA;
		add( thumb );
	}

	@Override
	protected void layout() {

		content.setPos( 0, 0 );
		controller.x = x;
		controller.y = y;
		controller.width = width;
		controller.height = height;

		Point p = camera().cameraToScreen( x, y );
		Camera cs = content.camera;
		cs.x = p.x;
		cs.y = p.y;
		cs.resize( (int)width, (int)height );

		thumb.visible = height < content.height();
		if (thumb.visible) {
			thumb.scale.set( 2, height * height / content.height() );
			thumb.x = right() - thumb.width();
			thumb.y = y + height * content.camera.scroll.y / content.height();
		}
	}

	public Component content() {
		return content;
	}

	/**
	 * Lets the pane be dragged from anywhere, its buttons included. A pane whose content is all
	 * buttons (a debug tab, a list of rows) could otherwise only be dragged from the slivers
	 * between them: every button took the press. Its buttons now let presses through to the pane
	 * (they still click when tapped), and once a press turns into a drag none of them clicks or
	 * long-clicks when it ends. A slider keeps a sideways drag (it moves the knob), but a drag
	 * that starts on it and goes mostly up or down scrolls the pane and leaves the slider as it was.
	 * The pane must have been made before its buttons (pointer listeners fire newest first).
	 */
	public void dragOverButtons() {
		overButtons = true;
		letThrough( content );
	}

	//every button in the content passes its presses on; checked each frame, so buttons added
	//later (a tab rebuilt, a list filtered) pass them on too
	private static void letThrough( Group g ) {
		for (Gizmo m : g.membersView()) {
			if (m instanceof Button) {
				((Button) m).hotArea.blockLevel = PointerArea.NEVER_BLOCK;
			}
			if (m instanceof OptionSlider) {
				((OptionSlider) m).letPressesThrough();
			}
			if (m instanceof Group && !(m instanceof OptionSlider)) {
				letThrough( (Group) m );
			}
		}
	}

	//the press under the finger, if it is one of this pane's buttons, is called off: the pane is moving
	private void dragStarted() {
		OptionSlider s = OptionSlider.pressedSlider;
		if (overButtons && s != null && inContent( s )) s.cancelPress();
		Button b = Button.pressedButton;
		if (!overButtons || b == null) return;
		if (inContent( b )) b.cancelPress();
	}

	private boolean inContent( Gizmo g ) {
		for (Group p = g.parent; p != null; p = p.parent) {
			if (p == content) return true;
		}
		return false;
	}

	//a drag that starts on one of this pane's sliders and goes sideways is the slider's
	private boolean sliderKeeps( PointerEvent event ) {
		OptionSlider s = OptionSlider.pressedSlider;
		if (!overButtons || s == null || !inContent( s )) return false;
		return Math.abs( event.current.x - event.start.x ) >= Math.abs( event.current.y - event.start.y );
	}

	public void onClick( float x, float y ) {
	}

	public class PointerController extends ScrollArea {

		protected float dragThreshold;

		public PointerController() {
			super( 0, 0, 0, 0 );
			dragThreshold = PixelScene.defaultZoom * 8;
		}
		
		@Override
		protected void onScroll(ScrollEvent event) {
			PointF newPt = new PointF(lastPos);
			newPt.y -= event.amount * content.camera.zoom * 10;
			scroll(newPt);
			dragging = false;
		}

		@Override
		protected void onPointerUp( PointerEvent event ) {
			if (event == curEvent && dragging) {
				dragging = false;
				thumb.am = THUMB_ALPHA;
				curEvent = null; //cancel here so onClick is skipped
			}
		}

		@Override
		protected void onClick(PointerEvent event) {
			PointF p = content.camera.screenToCamera((int) event.current.x, (int) event.current.y);
			ScrollPane.this.onClick(p.x, p.y);
		}

		private boolean dragging = false;
		private PointF lastPos = new PointF();

		@Override
		protected void onDrag( PointerEvent event ) {
			if (dragging) {

				scroll(event.current);

			} else if (PointF.distance( event.current, event.start ) > dragThreshold) {

				if (sliderKeeps( event )) {
					//this whole press is the slider's: the pane stays put until it ends
					curEvent = null;
					return;
				}
				dragging = true;
				lastPos.set( event.current );
				thumb.am = 1;
				dragStarted();

			}
		}
		
		private void scroll( PointF current ){
			
			Camera c = content.camera;
			
			c.shift( PointF.diff( lastPos, current ).invScale( c.zoom ) );
			if (c.scroll.x + width > content.width()) {
				c.scroll.x = content.width() - width;
			}
			if (c.scroll.x < 0) {
				c.scroll.x = 0;
			}
			if (c.scroll.y + height > content.height()) {
				c.scroll.y = content.height() - height;
			}
			if (c.scroll.y < 0) {
				c.scroll.y = 0;
			}
			
			thumb.y = y + height * c.scroll.y / content.height();
			
			lastPos.set( current );
			
		}
		
	}
}
