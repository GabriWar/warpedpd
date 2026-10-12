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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildPlacer;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildSelection;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Signal;

/**
 * The build bar: what stands along the bottom of the screen in place of the toolbar while the hero
 * builds (homestead/building/BuildPlacer, GameScene.showBuildBar). Its top line is the piece chosen
 * (its picture, its name, its cost per piece with what the hero has, and how many he can build),
 * or the mode when no piece is in play, and under it what to do next. Build and Cancel come up
 * on its right while a line, a rectangle or a cell picked waits. Its bottom row: one at a time,
 * line, area, demolish, repair, inspect, the build menu, and done.
 *
 * On a keyboard: 1 to 8 pick the Recent pieces, L, A, D, R and I switch modes, Enter builds what
 * is shown, Esc stops (BuildPlacer.key).
 */
public class BuildBar extends Component {

	private static final int PAD = 2;
	//the top part's least height: the picture's slot and two lines of words; more words make it taller
	private static final int INFO_H = 20;
	private static final int BTN_H = 18;
	private static final int SLOT = 18;
	/** The bar's height in ui units at its least (it grows with longer words). */
	public static final int HEIGHT = PAD + INFO_H + PAD + BTN_H + PAD;

	private final BuildPlacer placer;

	private NinePatch bg;
	//the whole bar takes the taps its buttons don't, so none falls through to the map under it
	private PointerArea blocker;
	private ColorBlock slot;
	private Image icon;
	private RenderedTextBlock title, status;
	private CostChips chips;
	private RedButton btnConfirm;
	private IconButton btnCancel;
	private ModeButton[] modes;

	private final Signal.Listener<KeyEvent> keys;
	private boolean stale = true;
	//the height its words need (layout), the bar's own when it last stood
	private int needed = HEIGHT;

	public BuildBar( BuildPlacer placer ){
		super();
		this.placer = placer;
		height = HEIGHT;
		//before the scene's and the toolbar's own: keys pressed while the bar is up are its own
		//first (a window opened over it still takes them before it, being newer)
		KeyEvent.addKeyListener( keys = event -> visible && placer.key( event ) );
		btnConfirm.visible = btnConfirm.active = false;
		btnCancel.visible = btnCancel.active = false;
	}

	@Override
	protected void createChildren(){
		bg = Chrome.get( Chrome.Type.TOAST_TR );
		add( bg );

		//before the buttons: the newest pointer listener hears a tap first, so they still get theirs
		blocker = new PointerArea( 0, 0, 0, 0 );
		blocker.blockLevel = PointerArea.ALWAYS_BLOCK;
		add( blocker );

		slot = new ColorBlock( SLOT, SLOT, 0x66000000 );
		add( slot );

		title = PixelScene.renderTextBlock( 7 );
		title.hardlight( Window.TITLE_COLOR );
		add( title );

		status = PixelScene.renderTextBlock( 6 );
		add( status );

		btnConfirm = new RedButton( Messages.get( this, "confirm" ), 7 ){
			@Override
			protected void onClick(){
				BuildPlacer p = BuildPlacer.current();
				if (p != null) p.confirm();
			}
		};
		add( btnConfirm );

		btnCancel = new IconButton( Icons.get( Icons.CLOSE ) ){
			@Override
			protected void onClick(){
				BuildPlacer p = BuildPlacer.current();
				if (p != null) p.cancel();
			}

			@Override
			protected String hoverText(){
				return Messages.get( BuildBar.class, "cancel" );
			}
		};
		add( btnCancel );

		modes = new ModeButton[]{
				new ModeButton( BuildIcons.PLACE, "single", "" ){
					@Override protected void act( BuildPlacer p ){ p.mode( BuildSelection.Mode.SINGLE ); }
				},
				new ModeButton( BuildIcons.LINE, "line", "L" ){
					@Override protected void act( BuildPlacer p ){ p.mode( BuildSelection.Mode.LINE ); }
				},
				new ModeButton( BuildIcons.AREA, "area", "A" ){
					@Override protected void act( BuildPlacer p ){ p.mode( BuildSelection.Mode.AREA ); }
				},
				new ModeButton( BuildIcons.DEMOLISH, "demolish", "D" ){
					@Override protected void act( BuildPlacer p ){ p.mode( BuildSelection.Mode.DEMOLISH ); }
				},
				new ModeButton( BuildIcons.REPAIR, "repair", "R" ){
					@Override protected void act( BuildPlacer p ){ p.mode( BuildSelection.Mode.REPAIR ); }
				},
				new ModeButton( BuildIcons.INSPECT, "inspect", "I" ){
					@Override protected void act( BuildPlacer p ){ p.mode( BuildSelection.Mode.INSPECT ); }
				},
				new ModeButton( BuildIcons.MENU, "menu", "" ){
					@Override protected void act( BuildPlacer p ){ p.menu(); }
				},
				new ModeButton( BuildIcons.DONE, "done", "Esc" ){
					//on the next frame: the bar (this button with it) comes down
					@Override protected void act( BuildPlacer p ){ Game.runOnRenderThread( BuildPlacer::stop ); }
				}
		};
		for (ModeButton b : modes) add( b );
	}

	/** What the bar shows has changed: laid out again on its next frame. */
	public void refresh(){
		stale = true;
	}

	@Override
	public void update(){
		super.update();
		//read while the hero waits for orders: the game's own thread is still then, and the
		//materials counted are the ones in his pack
		if (stale && Dungeon.hero != null && Dungeon.hero.ready){
			stale = false;
			fill();
		}
		//last: this may take the bar down (BuildPlacer.stop), and nothing of it runs after
		placer.update();
	}

	//everything that changes with the piece, the mode and the picks
	private void fill(){
		BuildPiece piece = placer.piece();
		BuildSelection.Mode mode = placer.mode();
		boolean placing = mode.places() && piece != null;

		if (icon != null){
			icon.destroy();
			remove( icon );
		}
		icon = placing ? piece.preview() : modeIcon( mode ).get();
		add( icon );

		String name;
		if (placing){
			int n = placer.affordable();
			name = Messages.titleCase( piece.name() ) + "  " + (n == Integer.MAX_VALUE
					? Messages.get( this, "free" ) : Messages.get( this, "count", Math.min( n, 999 ) ));
		} else {
			name = Messages.get( this, "mode_" + mode.name().toLowerCase( java.util.Locale.ENGLISH ) );
		}
		title.text( name );

		if (chips != null){
			chips.destroy();
			remove( chips );
			chips = null;
		}
		//the cost of one piece; with a line or an area shown, of all of it that fits
		int times = placer.pending() ? placer.planned() : 1;
		if (placing && times > 0){
			chips = new CostChips( piece.cost, times, m -> Material.count( Dungeon.hero, m ), true );
			add( chips );
		}

		status.text( placer.status() );

		btnConfirm.visible = btnConfirm.active = placer.pending();
		btnCancel.visible = btnCancel.active = placer.picked() || placer.building();

		modes[0].selected( mode == BuildSelection.Mode.SINGLE );
		modes[1].selected( mode == BuildSelection.Mode.LINE );
		modes[2].selected( mode == BuildSelection.Mode.AREA );
		modes[3].selected( mode == BuildSelection.Mode.DEMOLISH );
		modes[4].selected( mode == BuildSelection.Mode.REPAIR );
		modes[5].selected( mode == BuildSelection.Mode.INSPECT );
		modes[1].enable( piece == null || piece.canLine );
		modes[2].enable( piece == null || piece.canArea );
		layout();
		//grown or shrunk with its words: stood on the screen's foot again
		if (needed != height) GameScene.layoutBuildBar( this, needed );
	}

	private static BuildIcons modeIcon( BuildSelection.Mode mode ){
		switch (mode){
			case LINE:     return BuildIcons.LINE;
			case AREA:     return BuildIcons.AREA;
			case DEMOLISH: return BuildIcons.DEMOLISH;
			case REPAIR:   return BuildIcons.REPAIR;
			case INSPECT:  return BuildIcons.INSPECT;
			default:       return BuildIcons.PLACE;
		}
	}

	@Override
	protected void layout(){
		bg.x = x;
		bg.y = y;
		bg.size( width, height );
		blocker.x = x;
		blocker.y = y;
		blocker.width = width;
		blocker.height = height;

		float top = y + PAD;

		//Build and Cancel on the right while something waits
		float right = x + width - PAD;
		if (btnCancel.visible){
			btnCancel.setRect( right - 12, top + 1, 12, 12 );
			right = btnCancel.left() - PAD;
		}
		if (btnConfirm.visible){
			float bw = btnConfirm.reqWidth() + 6;
			btnConfirm.setRect( right - bw, top, bw, 14 );
			right = btnConfirm.left() - PAD;
		}

		float textX = x + PAD + SLOT + PAD;
		int textW = (int) Math.max( 20, right - textX );
		title.maxWidth( textW );
		title.setPos( textX, top );
		PixelScene.align( title );
		float under = title.bottom() + 1;
		if (chips != null){
			float room = right - title.right() - 4;
			if (chips.reqWidth() <= room){
				//beside the name when it fits, under it otherwise
				chips.setRect( title.right() + 4, top, room, 0 );
				chips.setPos( chips.left(), top + (title.height() - chips.height()) / 2f );
			} else {
				chips.setRect( textX, under, textW, 0 );
				under = chips.bottom() + 1;
			}
		}
		status.maxWidth( textW );
		status.setPos( textX, under );
		PixelScene.align( status );

		//the top part is as tall as its words (the picture's slot at least)
		float infoH = Math.max( INFO_H, status.bottom() - top );
		needed = (int) Math.ceil( PAD + infoH + PAD + BTN_H + PAD );
		slot.x = x + PAD;
		slot.y = top + (Math.min( infoH, INFO_H ) - SLOT) / 2f;
		if (icon != null){
			icon.x = slot.x + (SLOT - icon.width()) / 2f;
			icon.y = slot.y + (SLOT - icon.height()) / 2f;
			//a station's tall frame (the watchtower) stands in the slot from its foot
			if (icon.height() > SLOT) icon.y = slot.y + SLOT - icon.height();
			PixelScene.align( icon );
		}

		//the mode buttons, evenly along the bottom
		float bw = Math.min( 30, (width - PAD * 2) / modes.length );
		float bx = x + (width - bw * modes.length) / 2f;
		float by = y + height - PAD - BTN_H;
		for (ModeButton b : modes){
			b.setRect( bx, by, bw, BTN_H );
			bx += bw;
		}
	}

	@Override
	public synchronized void destroy(){
		super.destroy();
		KeyEvent.removeKeyListener( keys );
	}

	//one of the bottom row: an icon on a dark plate, lit gold while its mode is on
	private abstract class ModeButton extends Button {

		private final String key, hotkey;
		private NinePatch plate;
		private Image img;
		private boolean selected;

		ModeButton( BuildIcons kind, String key, String hotkey ){
			super();
			this.key = key;
			this.hotkey = hotkey;
			img = kind.get();
			add( img );
		}

		@Override
		protected void createChildren(){
			super.createChildren();
			plate = Chrome.get( Chrome.Type.GREY_BUTTON_TR );
			add( plate );
		}

		protected abstract void act( BuildPlacer p );

		@Override
		protected void onClick(){
			BuildPlacer p = BuildPlacer.current();
			if (p != null) act( p );
		}

		@Override
		protected void onPointerDown(){
			plate.brightness( 1.4f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp(){
			selected( selected );
		}

		@Override
		protected String hoverText(){
			String label = Messages.get( BuildBar.class, "btn_" + key );
			return hotkey.isEmpty() ? label : label + " (" + hotkey + ")";
		}

		void selected( boolean on ){
			selected = on;
			plate.resetColor();
			if (on) plate.hardlight( 0xFFCC44 );
		}

		void enable( boolean on ){
			active = on;
			img.alpha( on ? 1f : 0.3f );
		}

		@Override
		protected void layout(){
			super.layout();
			plate.x = x + 1;
			plate.y = y;
			plate.size( width - 2, height );
			img.x = x + (width - img.width()) / 2f;
			img.y = y + (height - img.height()) / 2f;
			PixelScene.align( img );
		}
	}
}
