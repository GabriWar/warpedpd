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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.HeatStation;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.HomesteadStation;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationJob;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.bags.VelvetPouch;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemSlot;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.List;

/**
 * A station's window (homestead/stations/HomesteadStation): its name and picture over one scrolling
 * page that holds, top to bottom,
 *   - for a station with a fire (HeatStation): the fuel gauge, how long the fuel will burn and
 *     whether it is lit, and buttons to add fuel from the pack and to light the fire or put it out;
 *   - the work queued, each job with what it makes, the batches left, a bar for the batch in hand
 *     (waiting, dimmed, while it needs the fire and there is none) and a button to call it off;
 *   - what the station has made, with a button to take it all;
 *   - how many batches the next job is (one, five, or as many as the hero can pay for);
 *   - every recipe: what it makes, each ingredient as an icon with what the hero has of what the
 *     chosen batches need (green when he has it, red when not), the work it takes, whether it needs
 *     the fire, and a button to set the station to it (the materials are taken at once).
 * Each action takes the hero a turn (calling a job off and taking the output do not) and opens the
 * window again on the station as it now is, scrolled where it was. Sized for a phone held either
 * way: as wide as the screen allows up to WIDTH_L, as tall as it allows, scrolling the rest.
 */
public class WndCraft extends Window {

	private static final int WIDTH_L = 190, WIDTH_MIN = 112;
	//an ingredient chip: its icon at three quarters (12 px) and a pixel, then its count
	private static final int GAP = 2, BTN_H = 16, CRAFT_W = 34, ICON = 16, CHIP_W = 34, CHIP_ICON = 13;

	private static final int COLOR_HAVE = 0x55FF55, COLOR_NEED = 0xFF5555, COLOR_DIM = 0xA0A0A0, COLOR_FIRE = 0xFFB050;
	private static final int BAR_BG = 0xFF2A2420, BAR_WORK = 0xFF6FBF5F, BAR_FIRE = 0xFFE8862E, BAR_IDLE = 0xFF5A5A5A;

	/** The batches a job is made of: the three choices, and "as many as he can pay for". */
	private static final int MAX = -1;
	private static final int[] BATCHES = { 1, 5, MAX };
	//the choice, kept from one opening to the next
	private static int batchChoice = 0;

	private final HomesteadStation station;
	private final int width;
	private ScrollPane pane;

	public WndCraft( HomesteadStation station ){
		this( station, 0f );
	}

	private WndCraft( HomesteadStation station, float scrolled ){
		super();
		this.station = station;
		width = PixelScene.landscape() ? WIDTH_L
				: (int)Math.max( WIDTH_MIN, Math.min( WIDTH_L, PixelScene.uiCamera.width - chrome.marginHor() - 2 ) );

		Image picture = new Image( HomesteadSprites.TEXTURE );
		picture.frame( HomesteadSprites.film().get( station.frame() ) );
		IconTitle title = new IconTitle( picture, Messages.titleCase( station.name() ) );
		title.setRect( 0, 0, width, 0 );
		add( title );
		float top = title.bottom() + GAP;

		//the pane before its buttons: pointer listeners fire newest first (see WndDebugScenes)
		ScrollPane pane = new ScrollPane( new Component() );
		Component content = pane.content();
		float pos = 0;
		if (station instanceof HeatStation) pos = fuel( content, (HeatStation) station, pos );
		pos = jobs( content, pos );
		pos = output( content, pos );
		pos = batches( content, pos );
		pos = recipes( content, pos );
		content.setSize( width, Math.max( 0, pos ) );
		pane.dragOverButtons();

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 16);
		float paneH = Math.min( content.height(), maxH - top );
		//resize before the pane goes in, for its camera (see WndDebugScenes)
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
		if (scrolled > 0) pane.scrollTo( 0, scrolled );
		this.pane = pane;
	}

	// ----------------------------------------------------------------- the fire

	private float fuel( Component c, HeatStation h, float pos ){
		int turns = Math.round( h.fuelTurns() );
		String state = h.lit() ? Messages.get( this, "fire_lit", turns )
				: turns > 0 ? Messages.get( this, "fire_out", turns ) : Messages.get( this, "fire_empty" );
		RenderedTextBlock line = text( c, state, 7, h.lit() ? COLOR_FIRE : COLOR_DIM, 0, pos, width );
		pos = line.bottom() + GAP;

		float full = HeatStation.FUEL_CAP * HeatStation.LOG_TURNS;
		pos = bar( c, 0, pos, width, Math.min( 1f, turns / full ), h.lit() ? BAR_FIRE : BAR_IDLE ) + GAP;

		RenderedTextBlock slot = text( c, Messages.get( this, "fuel_slot", h.fuelCount(), HeatStation.FUEL_CAP ), 6, COLOR_DIM, 0, pos, width );
		pos = slot.bottom() + GAP;

		float half = (width - GAP) / 2f;
		RedButton add = new RedButton( Messages.get( this, "add_fuel" ), 7 ){
			@Override
			protected void onClick(){
				if (!gone()) addFuel( h );
			}
		};
		add.enable( h.fuelRoom() > 0 );
		add.setRect( 0, pos, half, BTN_H );
		c.add( add );
		RedButton fire = new RedButton( Messages.get( this, h.lit() ? "put_out" : "light" ), 7 ){
			@Override
			protected void onClick(){
				if (gone()) return;
				if (h.lit()){
					h.putOut();
					GLog.i( Messages.get( WndCraft.class, "put_out_done", station.name() ) );
				} else if (h.light()){
					SpatialSound.play( Assets.Sounds.BURNING, station.pos );
					GLog.i( Messages.get( WndCraft.class, "lit_done", station.name() ) );
				} else {
					GLog.w( Messages.get( WndCraft.class, "no_fuel" ) );
					return;
				}
				act( true );
			}
		};
		fire.enable( h.lit() || turns > 0 );
		fire.setRect( half + GAP, pos, half, BTN_H );
		c.add( fire );
		return fire.bottom() + GAP * 3;
	}

	private void addFuel( HeatStation h ){
		GameScene.selectItem( new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( WndCraft.class, "fuel_prompt" );
			}

			//logs and charcoal ride in the builder's satchel (the backpack without one)
			@Override
			public Class<? extends Bag> preferredBag(){
				return BuildersSatchel.class;
			}

			@Override
			public boolean itemSelectable( Item item ){
				return HeatStation.isFuel( item );
			}

			@Override
			public void onSelect( Item item ){
				if (item == null || gone()) return;
				int n = h.addFuel( Dungeon.hero, item );
				if (n <= 0){
					GLog.w( Messages.get( WndCraft.class, "fuel_full" ) );
					return;
				}
				GLog.i( Messages.get( WndCraft.class, "fuel_added", n, item.name() ) );
				act( true );
			}
		} );
	}

	// ----------------------------------------------------------------- the work queued

	private float jobs( Component c, float pos ){
		List<StationJob> jobs = station.jobs();
		if (jobs.isEmpty()) return pos;
		RenderedTextBlock head = text( c, Messages.get( this, "queued", jobs.size(), HomesteadStation.MAX_JOBS ), 7, TITLE_COLOR, 0, pos, width );
		pos = head.bottom() + GAP;
		for (int i = 0; i < jobs.size(); i++){
			final int index = i;
			StationJob j = jobs.get( i );
			StationRecipe r = j.recipe();
			boolean going = i == 0 && r != null && station.working();
			Item sample = r == null ? null : r.output.sample();
			if (sample != null){
				ItemSprite icon = new ItemSprite( sample );
				icon.x = 0;
				icon.y = pos;
				c.add( icon );
			}
			float tx = ICON + GAP, tw = width - tx - ICON - GAP;
			String name = r == null ? j.recipe : r.name();
			String what = i == 0 && !going && r != null && r.needsHeat
					? Messages.get( this, "job_waiting", name, j.batches )
					: Messages.get( this, "job", name, j.batches );
			RenderedTextBlock t = text( c, what, 6, going || i > 0 ? WHITE : COLOR_DIM, tx, pos, (int)tw );
			float barY = Math.max( t.bottom() + 1, pos + 10 );
			bar( c, tx, barY, tw, i == 0 ? j.fraction() : 0f, going ? (r.needsHeat ? BAR_FIRE : BAR_WORK) : BAR_IDLE );
			IconButton cancel = new IconButton( Icons.get( Icons.CLOSE ) ){
				@Override
				protected void onClick(){
					if (gone()) return;
					if (station.cancel( index, Dungeon.hero )){
						GLog.i( Messages.get( WndCraft.class, "cancelled", name ) );
					}
					act( false );
				}
			};
			cancel.setRect( width - ICON, pos, ICON, ICON );
			c.add( cancel );
			pos = Math.max( pos + ICON, barY + 4 ) + GAP;
		}
		return pos + GAP * 2;
	}

	// ----------------------------------------------------------------- what it made

	private float output( Component c, float pos ){
		List<Item> made = station.output();
		if (made.isEmpty()) return pos;
		RenderedTextBlock head = text( c, Messages.get( this, "ready" ), 7, TITLE_COLOR, 0, pos, width );
		pos = head.bottom() + GAP;
		float x = 0;
		for (Item i : made){
			if (x + ICON + 2 > width){
				x = 0;
				pos += ICON + 2 + GAP;
			}
			ItemSlot slot = new ItemSlot( i );
			slot.setRect( x, pos, ICON + 2, ICON + 2 );
			c.add( slot );
			x += ICON + 2 + GAP;
		}
		pos += ICON + 2 + GAP;
		RedButton take = new RedButton( Messages.get( this, "take_all" ), 7 ){
			@Override
			protected void onClick(){
				if (gone()) return;
				int n = station.takeAll( Dungeon.hero );
				if (n > 0) GLog.i( Messages.get( WndCraft.class, "taken", station.name() ) );
				act( false );
			}
		};
		take.setRect( 0, pos, width, BTN_H );
		c.add( take );
		return take.bottom() + GAP * 3;
	}

	// ----------------------------------------------------------------- how many

	private float batches( Component c, float pos ){
		RenderedTextBlock label = text( c, Messages.get( this, "batches" ), 7, WHITE, 0, pos + 4, width / 3 );
		float x = label.right() + GAP * 2;
		float bw = (width - x - GAP * 2) / 3f;
		for (int i = 0; i < BATCHES.length; i++){
			final int choice = i;
			String name = BATCHES[i] == MAX ? Messages.get( this, "batch_max" ) : "x" + BATCHES[i];
			RedButton b = new RedButton( name, 7 ){
				@Override
				protected void onClick(){
					batchChoice = choice;
					reopen();
				}
			};
			if (i == batchChoice) b.textColor( TITLE_COLOR );
			b.setRect( x + i * (bw + GAP), pos, bw, BTN_H );
			c.add( b );
		}
		return pos + BTN_H + GAP * 3;
	}

	//how many batches a craft of this recipe would be now, 0 when none can be paid for
	private int batchesFor( StationRecipe r ){
		int can = Math.min( HomesteadStation.MAX_BATCH, r.affordable( Dungeon.hero, null ) );
		int want = BATCHES[batchChoice];
		if (want == MAX) return can;
		return can >= want ? want : 0;
	}

	// ----------------------------------------------------------------- the recipes

	private float recipes( Component c, float pos ){
		boolean room = station.jobs().size() < HomesteadStation.MAX_JOBS;
		int shown = BATCHES[batchChoice] == MAX ? 1 : BATCHES[batchChoice];
		for (final StationRecipe r : station.recipes()){
			ColorBlock rule = new ColorBlock( width, 1, 0xFF3C3632 );
			rule.x = 0;
			rule.y = pos;
			c.add( rule );
			pos += GAP + 1;

			Item sample = r.output.sample();
			ItemSprite icon = new ItemSprite( sample );
			icon.x = 0;
			icon.y = pos;
			c.add( icon );

			RedButton craft = new RedButton( Messages.get( this, "craft" ), 7 ){
				@Override
				protected void onClick(){
					craft( r );
				}
			};
			int n = batchesFor( r );
			craft.enable( room && n > 0 );
			craft.setRect( width - CRAFT_W, pos, CRAFT_W, BTN_H - 2 );
			c.add( craft );

			float tx = ICON + GAP;
			RenderedTextBlock name = text( c, r.name(), 7, WHITE, tx, pos + 3, (int)(width - tx - CRAFT_W - GAP) );
			pos = Math.max( pos + BTN_H - 2, name.bottom() ) + GAP;

			//the ingredients, each with what he has of what the chosen batches need
			int want = n > 0 ? n : shown;
			float x = 0;
			for (StationRecipe.Input in : r.inputs){
				int have = in.count( Dungeon.hero, null ), need = in.quantity * want;
				RenderedTextBlock count = text( c, have + "/" + need, 6, have >= need ? COLOR_HAVE : COLOR_NEED, 0, 0, width );
				float chipW = Math.max( CHIP_W, CHIP_ICON + count.width() + GAP );
				if (x > 0 && x + chipW > width){
					x = 0;
					pos += ICON + GAP;
				}
				Item s = in.sample();
				if (s != null){
					ItemSprite chip = new ItemSprite( s );
					chip.scale.set( 0.75f );
					chip.x = x;
					chip.y = pos;
					c.add( chip );
				}
				count.setPos( x + CHIP_ICON, pos + 3 );
				PixelScene.align( count );
				x += chipW + GAP;
			}
			pos += ICON - 2;

			//what the ingredients are, the work and the fire
			StringBuilder about = new StringBuilder();
			for (StationRecipe.Input in : r.inputs){
				if (about.length() > 0) about.append( ", " );
				about.append( in.quantity ).append( ' ' ).append( in.label() );
			}
			about.append( ". " ).append( Messages.get( this, "turns", r.turns ) );
			if (r.needsHeat) about.append( ' ' ).append( Messages.get( this, "needs_fire" ) );
			if (r.chosenInput() != null) about.append( ' ' ).append( Messages.get( this, "you_choose", r.chosenInput().label() ) );
			RenderedTextBlock info = text( c, about.toString(), 6, COLOR_DIM, 0, pos, width );
			pos = info.bottom() + GAP * 2;
		}
		return pos;
	}

	private void craft( StationRecipe r ){
		if (gone()) return;
		final int n = batchesFor( r );
		if (n <= 0){
			GLog.w( HomesteadStation.Craft.NO_MATERIALS.message() );
			return;
		}
		final StationRecipe.Input choose = r.chosenInput();
		if (choose == null){
			queued( r, station.queue( Dungeon.hero, r, n, null ), n );
			return;
		}
		//the hero picks which (a seed for the blandfruit); the batches as many of that as he has
		GameScene.selectItem( new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( WndCraft.class, "choose_prompt", choose.label() );
			}

			//seeds ride in the seed pouch (the backpack without one)
			@Override
			public Class<? extends Bag> preferredBag(){
				return VelvetPouch.class;
			}

			@Override
			public boolean itemSelectable( Item item ){
				return choose.matches( item, null );
			}

			@Override
			public void onSelect( Item item ){
				if (item == null || gone()) return;
				int m = Math.min( n, r.affordable( Dungeon.hero, item.getClass() ) );
				if (m <= 0){
					GLog.w( HomesteadStation.Craft.NO_MATERIALS.message() );
					return;
				}
				queued( r, station.queue( Dungeon.hero, r, m, item.getClass() ), m );
			}
		} );
	}

	private void queued( StationRecipe r, HomesteadStation.Craft result, int n ){
		if (result != HomesteadStation.Craft.OK){
			GLog.w( result.message() );
			reopen();
			return;
		}
		GLog.i( Messages.get( this, "queued_done", station.name(), r.name(), n ) );
		if (Dungeon.hero.sprite != null) Dungeon.hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "queued_float" ) );
		act( true );
	}

	// ----------------------------------------------------------------- helpers

	//the station was taken down or wrecked while the window was open (a monster, a fire, in the
	//turn the last action spent): the window goes and nothing more is put into it
	private boolean gone(){
		if (station.standing()) return false;
		hide();
		GLog.w( Messages.get( WndCraft.class, "gone", station.name() ) );
		return true;
	}

	//after an action: a turn spent at it (`spend`), and the window opened again on the station as it is now
	private void act( boolean spend ){
		Hero hero = Dungeon.hero;
		if (spend && hero != null){
			if (hero.sprite != null) hero.sprite.operate( station.pos );
			hero.spendAndNext( 1f );
		}
		reopen();
	}

	private void reopen(){
		float scrolled = pane == null ? 0 : pane.content().camera.scroll.y;
		hide();
		//a station gone in the meantime (wrecked, taken down) has no window
		if (Dungeon.level != null && Dungeon.level.mobs.contains( station )){
			GameScene.show( new WndCraft( station, scrolled ) );
		}
	}

	private static RenderedTextBlock text( Component c, String s, int size, int color, float x, float y, int maxWidth ){
		RenderedTextBlock t = PixelScene.renderTextBlock( s, size );
		t.maxWidth( Math.max( 8, maxWidth ) );
		t.hardlight( color );
		t.setPos( x, y );
		PixelScene.align( t );
		c.add( t );
		return t;
	}

	//a bar `fill` (0..1) full; the y under it
	private static float bar( Component c, float x, float y, float w, float fill, int color ){
		ColorBlock bg = new ColorBlock( w, 3, BAR_BG );
		bg.x = x;
		bg.y = y;
		c.add( bg );
		if (fill > 0f){
			ColorBlock fg = new ColorBlock( Math.max( 1f, w * Math.min( 1f, fill ) ), 3, color );
			fg.x = x;
			fg.y = y;
			c.add( fg );
		}
		return y + 3;
	}
}
