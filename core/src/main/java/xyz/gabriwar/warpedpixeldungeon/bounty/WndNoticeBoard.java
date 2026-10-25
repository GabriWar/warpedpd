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

package xyz.gabriwar.warpedpixeldungeon.bounty;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.NoticeBoardSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * A village's notice board, read (NoticeBoard.interact, the host's own hero): his standing there and
 * what it gets him, the notices he took from this board - each with how far along it is, those he can
 * collect his pay for marked - and the notices still up, then when new ones go up. A tap on a notice
 * asks what to do with it: take it down, collect, give it up or leave it; the board is read again after.
 * The list scrolls; the window fits a phone held either way.
 */
public class WndNoticeBoard extends Window {

	private static final int WIDTH_P = 120, WIDTH_L = 160;
	private static final int GAP = 2;
	/** A notice's state when the hero can collect for it, and when its time is short or gone. */
	static final int READY_COLOR = CharSprite.POSITIVE, DUE_COLOR = CharSprite.WARNING, LATE_COLOR = CharSprite.NEGATIVE;

	private final NoticeBoard board;
	private final long seed;

	public WndNoticeBoard( NoticeBoard board, long seed ){
		super();
		this.board = board;
		this.seed = seed;
		long village = board.key;

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle title = new IconTitle( NoticeBoardSprite.icon(), Messages.get( this, "title", board.place ) );
		title.setRect( 0, 0, width, 0 );
		add( title );
		float top = title.bottom() + GAP * 2;

		RenderedTextBlock standing = PixelScene.renderTextBlock( standing( seed, village ), 6 );
		standing.maxWidth( width );
		standing.setPos( 0, top );
		add( standing );
		top = standing.bottom() + GAP * 2;

		BountyWorld w = BountyWorld.get();
		ArrayList<Bounty> mine = new ArrayList<>();
		if (w != null) for (Bounty b : w.taken()) if (b.village == village) mine.add( b );
		ArrayList<Bounty> open = Dungeon.level instanceof OverworldLevel
				? Bounties.openOffers( (OverworldLevel) Dungeon.level, village, WorldClock.day(), w )
				: Bounties.openOffers( seed, village, WorldClock.day(), w );

		//the pane before its rows: pointer listeners fire newest first, so a pane made after the
		//rows under it would take their taps (see WndTravel); dragged over them it scrolls
		ScrollPane pane = new ScrollPane( new Component() );
		Component content = pane.content();
		float pos = 0;
		if (!mine.isEmpty()){
			pos = header( content, Messages.get( this, "yours" ), pos, width );
			for (Bounty b : mine) pos = row( content, b, true, pos, width );
		}
		pos = header( content, Messages.get( this, "posted" ), pos, width );
		if (open.isEmpty()){
			RenderedTextBlock none = PixelScene.renderTextBlock( Messages.get( this, "bare" ), 6 );
			none.maxWidth( width );
			none.setPos( 0, pos );
			content.add( none );
			pos = none.bottom() + GAP;
		}
		for (Bounty b : open) pos = row( content, b, false, pos, width );
		RenderedTextBlock next = PixelScene.renderTextBlock( nextNotices( seed, village ), 6 );
		next.maxWidth( width );
		next.setPos( 0, pos + GAP );
		content.add( next );
		pos = next.bottom();
		content.setSize( width, pos );
		pane.dragOverButtons();

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.min( content.height(), Math.max( 40, maxH - top ) );

		//resize before the pane goes in, for its camera
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}

	private float header( Component content, String text, float pos, int width ){
		RenderedTextBlock h = PixelScene.renderTextBlock( text, 7 );
		h.hardlight( TITLE_COLOR );
		h.maxWidth( width );
		h.setPos( 0, pos );
		content.add( h );
		return h.bottom() + GAP;
	}

	private float row( Component content, Bounty b, boolean taken, float pos, int width ){
		Row row = new Row( b, taken, width );
		row.setRect( 0, pos, width, row.reqHeight() );
		content.add( row );
		return row.bottom() + GAP;
	}

	/** The hero's standing in a village, and what it gets him there. */
	static String standing( long seed, long village ){
		BountyWorld w = BountyShare.world();
		int points = w == null ? 0 : w.points( village );
		int rank = Bounties.rank( points );
		String name = Bounties.villageName( seed, village );
		if (rank == 0) return Messages.get( WndNoticeBoard.class, "stranger", name );
		return Messages.get( WndNoticeBoard.class, "standing", name, Bounties.rankName( rank ), points,
				Math.round( Bounties.RANK_DISCOUNT * rank * 100 ) );
	}

	//when the board's notices turn over
	private static String nextNotices( long seed, long village ){
		int day = WorldClock.day();
		int left = Bounties.periodEnd( seed, village, Bounties.period( seed, village, day ) ) - day;
		return left <= 1 ? Messages.get( WndNoticeBoard.class, "next_tomorrow" )
				: Messages.get( WndNoticeBoard.class, "next_days", left );
	}

	/** How a taken notice stands: kills made, goods carried, a parcel's time left. */
	static String state( Bounty b, Hero hero ){
		switch (b.kind){
			case HUNT:
				return b.progress >= b.need ? Messages.get( WndNoticeBoard.class, "hunt_ready" )
						: Messages.get( WndNoticeBoard.class, "hunt_progress", b.progress, b.need );
			case GATHER: {
				Bounties.Good g = Bounties.good( b.target );
				int have = hero == null || g == null ? 0 : BountyActions.carried( hero, g );
				return have >= b.need ? Messages.get( WndNoticeBoard.class, "gather_ready" )
						: Messages.get( WndNoticeBoard.class, "gather_progress", have, b.need );
			}
			default: {
				int left = b.deadline - WorldClock.turn();
				return b.late || left <= 0 ? Messages.get( WndNoticeBoard.class, "deliver_late" )
						: Messages.get( WndNoticeBoard.class, "deliver_due", Bounties.hours( left ) );
			}
		}
	}

	/** The colour a taken notice's state is said in: ready to collect, its time short or gone (0: plain). */
	static int stateColor( Bounty b, Hero hero ){
		if (b.kind == Bounty.Kind.DELIVER){
			int left = b.deadline - WorldClock.turn();
			if (b.late || left <= 0) return LATE_COLOR;
			return left <= (b.deadline - b.takenTurn) / Bounties.DUE_SHARE ? DUE_COLOR : 0;
		}
		return hero != null && BountyActions.ready( hero, b ) ? READY_COLOR : 0;
	}

	//a notice tapped: what to do with it
	private void choose( final Bounty b, boolean taken ){
		final Hero hero = Dungeon.hero;
		if (hero == null) return;
		String head = Bounties.title( seed, b );
		String text = Bounties.hint( seed, b ) + "\n\n" + Messages.get( this, "pays",
				Bounties.pay( taken ? b.reward : Bounties.reward( b ), b.bonus ) );
		if (!taken){
			final String refusal = BountyActions.refusal( b );
			if (refusal != null) text += "\n\n" + refusal;
			GameScene.show( new WndOptions( new ItemSprite( Bounties.icon( b.kind ) ), head, text,
					Messages.get( this, "take" ), Messages.get( this, "leave" ) ){
				@Override
				protected boolean enabled( int index ){
					return index != 0 || refusal == null;
				}
				@Override
				protected void onSelect( int index ){
					if (index == 0 && BountyActions.take( hero, seed, b )) reread();
				}
			} );
			return;
		}
		text += "\n\n" + state( b, hero );
		if (BountyActions.ready( hero, b )){
			GameScene.show( new WndOptions( new ItemSprite( Bounties.icon( b.kind ) ), head, text,
					Messages.get( this, "collect" ), Messages.get( this, "later" ) ){
				@Override
				protected void onSelect( int index ){
					if (index != 0) return;
					if (BountyActions.turnIn( hero, seed, b )) reread();
					else GLog.w( Messages.get( WndNoticeBoard.class, "not_ready" ) );
				}
			} );
			return;
		}
		GameScene.show( new WndOptions( new ItemSprite( Bounties.icon( b.kind ) ), head, text,
				Messages.get( this, "keep" ), Messages.get( this, "give_up" ) ){
			@Override
			protected void onSelect( int index ){
				if (index != 1) return;
				BountyActions.abandon( hero, seed, b );
				reread();
			}
		} );
	}

	//the board read again, as it is now
	private void reread(){
		hide();
		GameScene.show( new WndNoticeBoard( board, seed ) );
	}

	//a notice: its kind's picture, what it asks, what it pays and, once taken, how it stands
	private class Row extends Button {

		private final Bounty b;
		private final boolean taken;
		private NinePatch bg;
		private ItemSprite icon;
		private RenderedTextBlock name, pay, state;

		Row( Bounty b, boolean taken, int width ){
			super();
			this.b = b;
			this.taken = taken;

			bg = Chrome.get( Chrome.Type.GREY_BUTTON_TR );
			addToBack( bg );

			icon = new ItemSprite( Bounties.icon( b.kind ) );
			add( icon );

			int textW = width - 16 - 6 - (int)bg.marginHor();
			name = PixelScene.renderTextBlock( Bounties.title( seed, b ), 7 );
			name.hardlight( TITLE_COLOR );
			name.maxWidth( textW );
			add( name );

			pay = PixelScene.renderTextBlock( Messages.get( WndNoticeBoard.class, "pays",
					Bounties.pay( taken ? b.reward : Bounties.reward( b ), b.bonus ) ), 6 );
			pay.maxWidth( textW );
			add( pay );

			if (taken){
				state = PixelScene.renderTextBlock( state( b, Dungeon.hero ), 6 );
				int color = stateColor( b, Dungeon.hero );
				if (color != 0) state.hardlight( color );
				state.maxWidth( textW );
				add( state );
			}
		}

		float reqHeight(){
			float h = name.height() + 1 + pay.height();
			if (state != null) h += 1 + state.height();
			return Math.max( 20, h + bg.marginVer() + 2 );
		}

		@Override
		protected void layout(){
			super.layout();
			bg.x = x;
			bg.y = y;
			bg.size( width, height );

			icon.x = x + bg.marginLeft() + 1;
			icon.y = y + (height - icon.height()) / 2f;
			PixelScene.align( icon );

			float tx = icon.x + 16 + 4;
			float ty = y + bg.marginTop() + 1;
			name.setPos( tx, ty );
			ty = name.bottom() + 1;
			pay.setPos( tx, ty );
			if (state != null) state.setPos( tx, pay.bottom() + 1 );
		}

		@Override
		protected void onPointerDown(){
			bg.brightness( 1.2f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp(){
			bg.resetColor();
		}

		@Override
		protected void onClick(){
			choose( b, taken );
		}
	}
}
