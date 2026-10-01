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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.RouletteWheel;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

/**
 * The smith's rarity reroll, played on a roulette wheel. Its 37 pockets are shared out
 * among the rarities by the stake (one to ten masterwork cores, Rarity.rerollPockets):
 * exotic is the green zero, and every pocket is equally likely, so what the wheel shows
 * is exactly the odds. The ball lands, stars burst out of the pocket in its colour, and
 * the item takes that rarity.
 *
 * The cores are taken and the pocket is picked the moment the wheel starts; the spin
 * only shows what already happened, so closing the window cannot dodge a bad roll.
 */
public class WndRarityWheel extends Window {

	private static final int WIDTH  = 150;
	private static final float GAP  = 3;
	private static final float SPIN_TIME = 6f;

	private final Hero hero;
	private final Item item;

	private int stake = 1;
	private Rarity[] wheelRarities;

	private RouletteWheel wheel;
	private Emitter stars;

	private final RenderedTextBlock[] odds = new RenderedTextBlock[Rarity.values().length];
	private RenderedTextBlock stakeText;
	private RenderedTextBlock result;
	private RedButton less, more, spin;

	public WndRarityWheel( Hero hero, Item item ){
		super();
		this.hero = hero;
		this.item = item;

		IconTitle title = new IconTitle( item );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		RenderedTextBlock info = PixelScene.renderTextBlock( Messages.get( this, "info",
				MasterworkCore.quality( item ).rarity.title() ), 6 );
		info.maxWidth( WIDTH );
		info.setPos( 0, title.bottom() + GAP );
		add( info );

		//as big as the screen allows, up to 144 across (even, for a centred hub)
		int room = (int)(PixelScene.uiCamera.height - 150);
		int size = Math.max( 96, Math.min( 144, room ) ) & ~1;
		wheel = new RouletteWheel( "rarity-roulette", size );
		wheel.setPos( (WIDTH - size) / 2f, info.bottom() + GAP );
		add( wheel );

		stars = new Emitter();
		add( stars );

		float y = wheel.bottom() + GAP;
		Rarity[] all = Rarity.values();
		for (int i = 0; i < all.length; i++){
			odds[i] = PixelScene.renderTextBlock( 6 );
			odds[i].hardlight( all[i].color );
			odds[i].text( all[i].title() + " 00/37" );
			add( odds[i] );
		}
		float rowH = odds[0].height() + 1;
		for (int i = 0; i < all.length; i++){
			odds[i].setPos( i < 3 ? 2 : WIDTH / 2f + 2, y + (i % 3) * rowH );
		}
		y += 3 * rowH + GAP;

		less = new RedButton( "-" ){
			@Override protected void onClick(){ setStake( stake - 1 ); }
		};
		less.setRect( 0, y, 18, 16 );
		add( less );
		more = new RedButton( "+" ){
			@Override protected void onClick(){ setStake( stake + 1 ); }
		};
		more.setRect( WIDTH - 18, y, 18, 16 );
		add( more );
		stakeText = PixelScene.renderTextBlock( 7 );
		add( stakeText );
		y += 16 + GAP;

		spin = new RedButton( Messages.get( this, "spin", 1 ) ){
			@Override protected void onClick(){ startSpin(); }
		};
		spin.setRect( 0, y, WIDTH, 18 );
		add( spin );
		y += 18 + GAP;

		result = PixelScene.renderTextBlock( 8 );
		result.setPos( 0, y );
		add( result );

		resize( WIDTH, (int)(y + 12) );
		setStake( 1 );
	}

	private int depth(){
		return Math.max( 1, Statistics.deepestFloor );
	}

	private void setStake( int n ){
		if (wheel.spinning()) return;
		int held = MasterworkCore.held( hero );
		stake = Math.max( 1, Math.min( Math.min( Rarity.MAX_STAKE, Math.max( 1, held ) ), n ) );
		int[] pockets = Rarity.rerollPockets( depth(), stake );
		wheelRarities = Rarity.rerollWheel( depth(), stake );
		int[] rgb = new int[RouletteWheel.POCKETS];
		String[] letters = new String[RouletteWheel.POCKETS];
		for (int i = 0; i < rgb.length; i++){
			rgb[i] = wheelRarities[i].color;
			letters[i] = wheelRarities[i].name().substring( 0, 1 );
		}
		wheel.pockets( rgb, letters );
		Rarity[] all = Rarity.values();
		for (int i = 0; i < all.length; i++){
			odds[i].text( all[i].title() + " " + pockets[i] + "/" + Rarity.POCKETS );
		}
		stakeText.text( Messages.get( this, "stake", stake, held ) );
		stakeText.setPos( (WIDTH - stakeText.width()) / 2f, less.top() + (less.height() - stakeText.height()) / 2f );
		less.enable( stake > 1 );
		more.enable( stake < Math.min( Rarity.MAX_STAKE, held ) );
		spin.enable( held >= stake );
		spin.text( Messages.get( this, "spin", stake ) );
	}

	private void startSpin(){
		if (wheel.spinning()) return;
		//exactly the stake, and only while the pack still holds it
		if (!MasterworkCore.spend( hero, stake )){
			setStake( stake );
			return;
		}
		//every pocket equally likely: the wheel's colours are the odds
		final int pocket = Random.Int( RouletteWheel.POCKETS );
		final Rarity outcome = wheelRarities[pocket];
		result.text( "" );
		less.enable( false );
		more.enable( false );
		spin.enable( false );
		wheel.spin( pocket, SPIN_TIME, new Callback(){
			@Override
			public void call(){
				landed( outcome );
			}
		} );
	}

	private void landed( Rarity outcome ){
		//the stars burst out of the pocket the ball sits in, in its colour
		final int color = outcome.color;
		stars.pos( wheel.ballX(), wheel.ballY() );
		stars.burst( new Emitter.Factory(){
			@Override
			public void emit( Emitter emitter, int index, float x, float y ){
				Speck s = (Speck) emitter.recycle( Speck.class );
				s.reset( index, x, y, Speck.STAR );
				s.hardlight( color );
			}
			@Override
			public boolean lightMode(){ return true; }
		}, 10 + 6 * outcome.ordinal() );
		switch (outcome){
			case EXOTIC:
				Sample.INSTANCE.play( Assets.Sounds.MASTERY );
				Sample.INSTANCE.play( Assets.Sounds.BADGE, 0.8f );
				break;
			case LEGENDARY: Sample.INSTANCE.play( Assets.Sounds.LEVELUP ); break;
			case RARE:      Sample.INSTANCE.play( Assets.Sounds.EVOKE ); break;
			case UNCOMMON:  Sample.INSTANCE.play( Assets.Sounds.GOLD ); break;
			default:        Sample.INSTANCE.play( Assets.Sounds.ITEM ); break;
		}
		MasterworkCore.rerolled( hero, item, outcome );
		result.text( Messages.get( this, "result", outcome.title().toUpperCase( java.util.Locale.ENGLISH ) ) );
		result.hardlight( outcome.color );
		result.setPos( (WIDTH - result.width()) / 2f, result.top() );
		setStake( stake );
	}

	@Override
	public void onBackPressed(){
		//the wheel is turning: let the ball land first
		if (!wheel.spinning()) super.onBackPressed();
	}
}
