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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.items.catching.CaughtFish;
import xyz.gabriwar.warpedpixeldungeon.items.catching.Earthworm;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FisherTrade;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishingRod;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.CurrencyIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/**
 * A village fisher's trade (items/catching/FisherTrade): what he says, then the hero's catch sold to
 * him in one go, a fishing rod, a handful of earthworms. Each choice that cannot be paid for (or with
 * no fish to sell) is greyed out; after a trade the window comes back for the next, the purse shown.
 */
public class WndFisher extends WndOptions {

	private static final int SELL = 0, ROD = 1, WORMS = 2;

	private final Settler fisher;
	private final String line;

	public WndFisher( Settler fisher, String line ){
		super( fisher.sprite(), Messages.titleCase( Messages.get( WndFisher.class, "title" ) ),
				line + "\n\n" + Messages.get( WndFisher.class, "offer" ), options() );
		this.fisher = fisher;
		this.line = line;
		CurrencyIndicator.showGold = true;
	}

	private static String[] options(){
		Hero hero = Dungeon.hero;
		int n = FisherTrade.fishCount( hero );
		return new String[]{
				n > 0 ? Messages.get( WndFisher.class, "sell", n, FisherTrade.fishOffer( hero ) )
						: Messages.get( WndFisher.class, "sell_none" ),
				Messages.get( WndFisher.class, "rod", FisherTrade.rodPrice() ),
				Messages.get( WndFisher.class, "worms", FisherTrade.HANDFUL, FisherTrade.wormsPrice() ) };
	}

	@Override
	protected boolean enabled( int index ){
		Hero hero = Dungeon.hero;
		if (hero == null) return false;
		switch (index){
			case SELL:  return FisherTrade.fishCount( hero ) > 0;
			case ROD:   return Dungeon.gold >= FisherTrade.rodPrice();
			case WORMS: return Dungeon.gold >= FisherTrade.wormsPrice();
			default:    return true;
		}
	}

	@Override
	protected boolean hasIcon( int index ){
		return true;
	}

	@Override
	protected Image getIcon( int index ){
		switch (index){
			case SELL: {
				ArrayList<CaughtFish> fish = Dungeon.hero == null ? new ArrayList<>()
						: Dungeon.hero.belongings.getAllItems( CaughtFish.class );
				return new ItemSprite( fish.isEmpty() ? ItemSpriteSheet.FISH_PERCH : fish.get( 0 ).image() );
			}
			case ROD:  return new ItemSprite( ItemSpriteSheet.FISHING_ROD );
			default:   return new ItemSprite( ItemSpriteSheet.EARTHWORMS );
		}
	}

	@Override
	protected void onSelect( int index ){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		boolean done;
		switch (index){
			case SELL:
				int gold = FisherTrade.sellFish( hero );
				done = gold > 0;
				if (done) GLog.p( Messages.get( WndFisher.class, "sold", gold ) );
				break;
			case ROD:
				done = FisherTrade.buy( hero, new FishingRod(), FisherTrade.rodPrice() );
				if (done) GLog.p( Messages.get( WndFisher.class, "bought_rod" ) );
				break;
			default:
				done = FisherTrade.buy( hero, new Earthworm().quantity( FisherTrade.HANDFUL ), FisherTrade.wormsPrice() );
				if (done) GLog.p( Messages.get( WndFisher.class, "bought_worms", FisherTrade.HANDFUL ) );
		}
		//the next trade, the purse as it is now
		if (done) GameScene.show( new WndFisher( fisher, line ) );
	}

	@Override
	public void hide(){
		super.hide();
		CurrencyIndicator.showGold = false;
	}
}
