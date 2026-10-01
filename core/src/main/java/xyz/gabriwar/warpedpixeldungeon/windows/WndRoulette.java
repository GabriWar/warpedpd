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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.RouletteWheel;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The bar's roulette (RouletteTable): a European wheel of 37 numbers, played for
 * masterwork cores at the real odds and the real payouts.
 *
 * The board is the table's: the zero, the numbers 1-36 in their three rows, a column
 * bet at the end of each row, the three dozens, and the even-money bets (1-18, even,
 * red, black, odd, 19-36). Pick a chip and tap spots to stack bets; the stakes are
 * taken when the wheel starts, the number is drawn then (one pocket in 37, the zero
 * included, so the house keeps its edge on every bet), and winning bets pay their
 * stake back with the winnings: 35 to 1 on a number, 2 to 1 on a dozen or a column,
 * even money on the rest. The zero loses every outside bet.
 */
public class WndRoulette extends Window {

	private static final float GAP = 3;
	private static final float SPIN_TIME = 7f;

	private static final int[] CHIPS = { 1, 5, 10, 25 };

	//bet ids: 0-36 the numbers; then the columns (top, middle, bottom row), the
	//dozens, and the even-money bets in table order
	private static final int COL_TOP = 37, COL_MID = 38, COL_LOW = 39;
	private static final int DOZEN_1 = 40, DOZEN_2 = 41, DOZEN_3 = 42;
	private static final int LOW = 43, EVEN = 44, RED = 45, BLACK = 46, ODD = 47, HIGH = 48;

	private final Hero hero;
	//the table fills what the screen allows: a board cell (felt gap included) of 11
	//to 16 pixels, fourteen of them across (the zero, twelve columns, the column bets)
	private final int CELL;
	private final int WIDTH;
	private final int labelSize;
	private final Map<Integer, Integer> bets = new LinkedHashMap<>();
	private Map<Integer, Integer> lastBets = new LinkedHashMap<>();
	private final Map<Integer, Spot> spots = new LinkedHashMap<>();
	private int chip = 1;

	private RouletteWheel wheel;
	private Emitter stars;
	private RenderedTextBlock status, result;
	private final RedButton[] chipBtns = new RedButton[CHIPS.length];
	private RedButton clear, rebet, spin;

	public WndRoulette( Hero hero ){
		super();
		this.hero = hero;

		CELL = Math.max( 11, Math.min( 16, (int)((PixelScene.uiCamera.width - 16) / 14) ) );
		WIDTH = CELL * 14;
		labelSize = CELL >= 14 ? 8 : 6;

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get( this, "title" ), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (WIDTH - title.width()) / 2f, 1 );
		add( title );

		//the wheel takes the height the rest of the table leaves, up to 176 across
		int below = 5 * CELL + 90;
		int size = Math.max( 96, Math.min( Math.min( 176, WIDTH - 8 ),
				(int)(PixelScene.uiCamera.height - 30 - title.height() - below) ) ) & ~1;
		wheel = new RouletteWheel( "casino-roulette", size );
		wheel.pockets( RouletteWheel.europeanColors(), RouletteWheel.europeanLabels() );
		wheel.setPos( (WIDTH - size) / 2f, title.bottom() + GAP );
		add( wheel );

		stars = new Emitter();
		add( stars );

		result = PixelScene.renderTextBlock( 9 );
		result.setPos( 0, wheel.bottom() + 1 );
		add( result );

		float y = wheel.bottom() + 14;
		y = buildBoard( y ) + GAP;

		//chips
		float cw = (WIDTH - (CHIPS.length - 1) * 2) / (float)CHIPS.length;
		for (int i = 0; i < CHIPS.length; i++){
			final int value = CHIPS[i];
			chipBtns[i] = new RedButton( String.valueOf( value ) ){
				@Override protected void onClick(){ chip = value; refresh(); }
			};
			chipBtns[i].setRect( i * (cw + 2), y, cw, 16 );
			add( chipBtns[i] );
		}
		y += 16 + 2;

		clear = new RedButton( Messages.get( this, "clear" ) ){
			@Override protected void onClick(){ bets.clear(); refresh(); }
		};
		clear.setRect( 0, y, WIDTH / 2f - 1, 16 );
		add( clear );
		rebet = new RedButton( Messages.get( this, "rebet" ) ){
			@Override protected void onClick(){
				bets.clear();
				if (total( lastBets ) <= MasterworkCore.held( WndRoulette.this.hero )) bets.putAll( lastBets );
				refresh();
			}
		};
		rebet.setRect( WIDTH / 2f + 1, y, WIDTH / 2f - 1, 16 );
		add( rebet );
		y += 16 + 2;

		status = PixelScene.renderTextBlock( 7 );
		status.setPos( 0, y );
		add( status );
		y += 11;

		spin = new RedButton( Messages.get( this, "spin" ) ){
			@Override protected void onClick(){ startSpin(); }
		};
		spin.setRect( 0, y, WIDTH, 20 );
		add( spin );
		y += 20;

		resize( WIDTH, (int)y + 2 );
		refresh();
	}

	// ----------------------------------------------------------------- the board

	private float buildBoard( float top ){
		float x0 = 1;
		int rows = 3;
		ColorBlock felt = new ColorBlock( WIDTH + 1, rows * CELL + 2 * CELL + 1, 0xFF0F5C2E );
		felt.x = x0 - 1;
		felt.y = top - 1;
		add( felt );

		//the zero spans the three rows at the left
		spot( 0, "0", RouletteWheel.GREEN, x0, top, CELL - 1, rows * CELL - 1 );
		for (int c = 0; c < 12; c++){
			for (int r = 0; r < rows; r++){
				int n = 3 * (c + 1) - r;
				spot( n, String.valueOf( n ), RouletteWheel.colorOf( n ), x0 + CELL * (c + 1), top + r * CELL, CELL - 1, CELL - 1 );
			}
		}
		//the column bets at the end of each row
		int[] cols = { COL_TOP, COL_MID, COL_LOW };
		for (int r = 0; r < rows; r++){
			spot( cols[r], "2:1", 0x0F5C2E, x0 + CELL * 13, top + r * CELL, CELL - 2, CELL - 1 );
		}
		float y = top + rows * CELL;
		int[] dozens = { DOZEN_1, DOZEN_2, DOZEN_3 };
		String[] dozenLabels = { "1st 12", "2nd 12", "3rd 12" };
		for (int d = 0; d < 3; d++){
			spot( dozens[d], dozenLabels[d], 0x0F5C2E, x0 + CELL * (1 + 4 * d), y, CELL * 4 - 1, CELL - 1 );
		}
		y += CELL;
		int[] outside = { LOW, EVEN, RED, BLACK, ODD, HIGH };
		String[] outsideLabels = { "1-18", "EVEN", "RED", "BLK", "ODD", "19-36" };
		int[] outsideColors = { 0x0F5C2E, 0x0F5C2E, RouletteWheel.RED, RouletteWheel.BLACK, 0x0F5C2E, 0x0F5C2E };
		for (int i = 0; i < 6; i++){
			spot( outside[i], outsideLabels[i], outsideColors[i], x0 + CELL * (1 + 2 * i), y, CELL * 2 - 1, CELL - 1 );
		}
		return y + CELL;
	}

	private void spot( int id, String label, int rgb, float x, float y, float w, float h ){
		Spot s = new Spot( id, label, rgb );
		s.setRect( x, y, w, h );
		add( s );
		spots.put( id, s );
	}

	private class Spot extends Button {

		private final int id;
		private ColorBlock bg, chipMark;
		private RenderedTextBlock text;
		private BitmapText amount;

		Spot( int id, String label, int rgb ){
			super();
			this.id = id;
			bg.hardlight( rgb );
			text = PixelScene.renderTextBlock( label, labelSize );
			text.hardlight( 0xF2EEE4 );
			add( text );
			//the chip sits over the label
			bringToFront( chipMark );
			bringToFront( amount );
		}

		@Override
		protected void createChildren(){
			super.createChildren();
			bg = new ColorBlock( 1, 1, 0xFFFFFFFF );
			add( bg );
			chipMark = new ColorBlock( 1, 1, 0xFFE8C04A );
			chipMark.visible = false;
			add( chipMark );
			amount = new BitmapText( PixelScene.pixelFont );
			amount.hardlight( 0x1A120C );
			amount.visible = false;
			add( amount );
		}

		@Override
		protected void layout(){
			super.layout();
			bg.x = x;
			bg.y = y;
			bg.size( width, height );
			if (text != null){
				text.setPos( x + (width - text.width()) / 2f, y + (height - text.height()) / 2f );
				PixelScene.align( text );
			}
			chipMark.size( Math.min( width, 9 ), 7 );
			chipMark.x = x + width - chipMark.width();
			chipMark.y = y + height - chipMark.height();
			amount.x = chipMark.x + (chipMark.width() - amount.width()) / 2f;
			amount.y = chipMark.y + 1;
			PixelScene.align( amount );
		}

		void show( int stake, boolean won ){
			chipMark.visible = amount.visible = stake > 0;
			if (stake > 0){
				amount.text( stake > 99 ? "99" : String.valueOf( stake ) );
				amount.measure();
			}
			bg.alpha( won ? 1f : 0.92f );
			if (won) bg.brightness( 1.6f ); else bg.resetColor();
			layout();
		}

		@Override
		protected void onClick(){
			if (wheel.spinning()) return;
			int held = MasterworkCore.held( hero );
			if (total( bets ) + chip > held){
				GLog.w( Messages.get( WndRoulette.class, "not_enough", held ) );
				return;
			}
			bets.put( id, bets.getOrDefault( id, 0 ) + chip );
			Sample.INSTANCE.play( Assets.Sounds.CLICK, 0.5f, 0.9f );
			refresh();
		}
	}

	// ----------------------------------------------------------------- the game

	private static int total( Map<Integer, Integer> m ){
		int t = 0;
		for (int v : m.values()) t += v;
		return t;
	}

	/** Does this bet win on this number? */
	static boolean wins( int bet, int n ){
		if (bet <= 36) return bet == n;
		if (n == 0) return false;
		switch (bet){
			case COL_TOP: return n % 3 == 0;
			case COL_MID: return n % 3 == 2;
			case COL_LOW: return n % 3 == 1;
			case DOZEN_1: return n <= 12;
			case DOZEN_2: return n >= 13 && n <= 24;
			case DOZEN_3: return n >= 25;
			case LOW:     return n <= 18;
			case HIGH:    return n >= 19;
			case EVEN:    return n % 2 == 0;
			case ODD:     return n % 2 == 1;
			case RED:     return RouletteWheel.isRed( n );
			case BLACK:   return !RouletteWheel.isRed( n );
			default:      return false;
		}
	}

	/** What a winning bet pays, stake not counted: 35 to 1, 2 to 1 or even money. */
	static int odds( int bet ){
		if (bet <= 36) return 35;
		if (bet <= DOZEN_3) return 2;
		return 1;
	}

	private void refresh(){
		boolean idle = !wheel.spinning();
		int held = MasterworkCore.held( hero );
		for (int i = 0; i < CHIPS.length; i++){
			chipBtns[i].enable( idle && CHIPS[i] <= held );
			chipBtns[i].textColor( CHIPS[i] == chip ? TITLE_COLOR : 0xFFFFFF );
		}
		clear.enable( idle && !bets.isEmpty() );
		rebet.enable( idle && !lastBets.isEmpty() && total( lastBets ) <= held );
		spin.enable( idle && !bets.isEmpty() && total( bets ) <= held );
		for (Map.Entry<Integer, Spot> e : spots.entrySet()){
			e.getValue().show( bets.getOrDefault( e.getKey(), 0 ), false );
		}
		status.text( Messages.get( this, "status", total( bets ), held, chip ) );
		status.setPos( (WIDTH - status.width()) / 2f, status.top() );
	}

	private void startSpin(){
		if (wheel.spinning() || bets.isEmpty()) return;
		final int staked = total( bets );
		//exactly the stakes, and only while the pack still holds them
		if (!MasterworkCore.spend( hero, staked )){
			refresh();
			return;
		}
		lastBets = new LinkedHashMap<>( bets );
		//the number is drawn now: one pocket in thirty-seven
		final int pocket = Random.Int( RouletteWheel.POCKETS );
		final int number = RouletteWheel.EUROPEAN[pocket];
		result.text( "" );
		wheel.spin( pocket, SPIN_TIME, new Callback(){
			@Override
			public void call(){
				settle( number, staked );
			}
		} );
		refresh();
	}

	private void settle( int number, int staked ){
		int back = 0;
		for (Map.Entry<Integer, Integer> b : lastBets.entrySet()){
			if (wins( b.getKey(), number )) back += b.getValue() * (odds( b.getKey() ) + 1);
		}
		if (back > 0){
			MasterworkCore won = new MasterworkCore();
			won.quantity( back );
			if (!won.collect()) Dungeon.level.drop( won, hero.pos ).sprite.drop();
		}

		String colour = number == 0 ? Messages.get( this, "green" )
				: RouletteWheel.isRed( number ) ? Messages.get( this, "red" ) : Messages.get( this, "black" );
		result.text( number + " " + colour );
		result.hardlight( number == 0 ? 0x3FD06A : RouletteWheel.isRed( number ) ? 0xFF5A4A : 0xE8E4DC );
		result.setPos( (WIDTH - result.width()) / 2f, result.top() );

		if (back > 0){
			final int burst = back >= staked * 10 ? 30 : back > staked ? 16 : 8;
			stars.pos( wheel.ballX(), wheel.ballY() );
			stars.burst( Speck.factory( Speck.STAR ), burst );
			Sample.INSTANCE.play( back >= staked * 10 ? Assets.Sounds.LEVELUP : Assets.Sounds.GOLD );
			GLog.p( Messages.get( this, "won", number, colour, back ) );
		} else {
			Sample.INSTANCE.play( Assets.Sounds.MISS, 0.8f );
			GLog.i( Messages.get( this, "lost", number, colour, staked ) );
		}

		bets.clear();
		refresh();
		//light the spots that paid
		for (Map.Entry<Integer, Spot> e : spots.entrySet()){
			if (wins( e.getKey(), number )) e.getValue().show( 0, true );
		}
	}

	@Override
	public void onBackPressed(){
		//the ball is still rolling: let it land
		if (!wheel.spinning()) super.onBackPressed();
	}
}
