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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.CurrencyIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.RouletteWheel;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

/**
 * The black market's rarity wheel. It is the smith's wheel (WndRarityWheel) cut the way
 * a four-core stake would cut it - three cores better than the single one the dealer's
 * gold buys - and paid for in coin, not cores. The catch is painted on it in plain
 * sight: one of the common pockets is black, and an item that lands there stays with
 * the dealer. As on the smith's wheel the coin is taken and the pocket picked the moment
 * it starts turning, so shutting the window changes nothing.
 */
public class WndRiggedWheel extends Window {

	private static final int WIDTH  = 150;
	private static final float GAP  = 3;
	private static final float SPIN_TIME = 6f;
	//the stake the smith would want for a wheel cut like this one
	private static final int TILT = 4;
	private static final int SEIZED_COLOR = 0x0D0B0F;

	private final Hero hero;
	private final Item item;

	private final Rarity[] wheelRarities;
	private final int seizedPocket;

	private final RouletteWheel wheel;
	private final Emitter stars;
	private final RenderedTextBlock result;
	private final RedButton spin;
	private boolean taken = false;

	public WndRiggedWheel( Hero hero, Item item ){
		super();
		this.hero = hero;
		this.item = item;

		CurrencyIndicator.showGold = true;

		IconTitle title = new IconTitle( item );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		RenderedTextBlock info = PixelScene.renderTextBlock( Messages.get( this, "info",
				MasterworkCore.quality( item ).rarity.title() ), 6 );
		info.maxWidth( WIDTH );
		info.setPos( 0, title.bottom() + GAP );
		add( info );

		int depth = WarpedRooms.threat();
		int[] counts = Rarity.rerollPockets( depth, TILT );
		wheelRarities = Rarity.rerollWheel( depth, TILT );
		//the black pocket is cut out of the commons, never out of anything worth landing on
		int found = 0;
		for (int i = 0; i < wheelRarities.length; i++){
			if (wheelRarities[i] == Rarity.COMMON){ found = i; break; }
		}
		seizedPocket = found;
		counts[Rarity.COMMON.ordinal()] = Math.max( 0, counts[Rarity.COMMON.ordinal()] - 1 );

		int room = (int) (PixelScene.uiCamera.height - 140);
		int size = Math.max( 96, Math.min( 144, room ) ) & ~1;
		wheel = new RouletteWheel( "rigged-roulette", size );
		wheel.setPos( (WIDTH - size) / 2f, info.bottom() + GAP );
		add( wheel );

		int[] rgb = new int[RouletteWheel.POCKETS];
		String[] letters = new String[RouletteWheel.POCKETS];
		for (int i = 0; i < rgb.length; i++){
			boolean seized = i == seizedPocket;
			rgb[i] = seized ? SEIZED_COLOR : wheelRarities[i].color;
			letters[i] = seized ? "X" : wheelRarities[i].name().substring( 0, 1 );
		}
		wheel.pockets( rgb, letters );

		stars = new Emitter();
		add( stars );

		float y = wheel.bottom() + GAP;
		Rarity[] all = Rarity.values();
		RenderedTextBlock[] odds = new RenderedTextBlock[all.length + 1];
		for (int i = 0; i < odds.length; i++){
			odds[i] = PixelScene.renderTextBlock( 6 );
			if (i < all.length){
				odds[i].text( all[i].title() + " " + counts[i] + "/" + Rarity.POCKETS );
				odds[i].hardlight( all[i].color );
			} else {
				odds[i].text( Messages.get( this, "seized_odds", 1, Rarity.POCKETS ) );
				odds[i].hardlight( 0xCC3333 );
			}
			add( odds[i] );
		}
		float rowH = odds[0].height() + 1;
		int rows = (odds.length + 1) / 2;
		for (int i = 0; i < odds.length; i++){
			odds[i].setPos( i < rows ? 2 : WIDTH / 2f + 2, y + (i % rows) * rowH );
		}
		y += rows * rowH + GAP;

		spin = new RedButton( Messages.get( this, "spin", BlackMarket.wheelPrice() ) ){
			@Override
			protected void onClick(){
				startSpin();
			}
		};
		spin.setRect( 0, y, WIDTH, 18 );
		add( spin );
		y += 18 + GAP;

		result = PixelScene.renderTextBlock( 8 );
		result.setPos( 0, y );
		add( result );

		resize( WIDTH, (int) (y + 12) );
		updateSpin();
	}

	private void updateSpin(){
		spin.text( Messages.get( this, "spin", BlackMarket.wheelPrice() ) );
		spin.enable( !taken && !wheel.spinning() && BlackMarket.wheelPrice() <= Dungeon.gold
				&& hero.belongings.contains( item ) );
	}

	private void startSpin(){
		if (wheel.spinning() || taken) return;
		int price = BlackMarket.wheelPrice();
		if (price > Dungeon.gold || !hero.belongings.contains( item ) || item.isEquipped( hero )){
			updateSpin();
			return;
		}
		WndBlackMarket.pay( price );
		WndBlackMarket.drawHeat();
		Sample.INSTANCE.play( Assets.Sounds.GOLD );

		final int pocket = Random.Int( RouletteWheel.POCKETS );
		result.text( "" );
		spin.enable( false );
		wheel.spin( pocket, SPIN_TIME, new Callback(){
			@Override
			public void call(){
				landed( pocket );
			}
		} );
	}

	private void landed( int pocket ){
		final boolean seized = pocket == seizedPocket;
		final Rarity outcome = wheelRarities[pocket];
		final int color = seized ? 0xCC3333 : outcome.color;

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
		}, seized ? 8 : 10 + 6 * outcome.ordinal() );

		if (seized){
			//the house pocket: the item goes under the counter and does not come back
			taken = true;
			if (hero.belongings.contains( item )) item.detachAll( hero.belongings.backpack );
			Sample.INSTANCE.play( Assets.Sounds.DEGRADE );
			GLog.n( Messages.get( this, "seized", item.name() ) );
			result.text( Messages.get( this, "seized_result" ) );
		} else {
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
		}
		result.hardlight( color );
		result.setPos( (WIDTH - result.width()) / 2f, result.top() );
		updateSpin();
	}

	@Override
	public void onBackPressed(){
		//the wheel is turning: let the ball land first
		if (!wheel.spinning()) super.onBackPressed();
	}

	@Override
	public void hide(){
		super.hide();
		CurrencyIndicator.showGold = false;
	}
}
