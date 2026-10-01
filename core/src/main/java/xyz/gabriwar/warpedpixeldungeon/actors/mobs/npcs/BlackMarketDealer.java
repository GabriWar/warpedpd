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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BlobImmunity;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BlackMarketDealerSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBlackMarket;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

/**
 * The man under the hood in the black market. He holds nothing himself: the stock, the
 * prices and the grudge are the market's (the BlackMarket blob), which is also what puts
 * him on the floor at nightfall and takes him off it at dawn. All he does is open its
 * window, and tell it when somebody has been stupid enough to hit him.
 */
public class BlackMarketDealer extends NPC {

	{
		spriteClass = BlackMarketDealerSprite.class;

		properties.add( Property.IMMOVABLE );
	}

	@Override
	protected boolean act(){
		if (sprite != null && Dungeon.hero != null) sprite.turnTo( pos, Dungeon.hero.pos );
		spend( TICK );
		return super.act();
	}

	@Override
	public boolean isImmune( Class effect ){
		return Blob.class.isAssignableFrom( effect ) || super.isImmune( effect );
	}

	@Override
	public void damage( int dmg, Object src ){
		//a gas drifting through the cellar is weather, not an attack
		if (src instanceof Blob || src instanceof Class && Blob.class.isAssignableFrom( (Class) src )) return;
		provoke();
	}

	//a gas that paralyses or chokes him is still the weather: only a buff somebody meant
	//for him counts as being hit - not what the climate lays on everyone (the shopkeeper's
	//own list) and not what a cloud standing on him does
	@Override
	public boolean add( Buff buff ){
		if (buff.type == Buff.buffType.NEGATIVE && !Shopkeeper.environmental( buff )
				&& !inHarmfulBlob( pos ) && Dungeon.level.heroFOV[pos]) provoke();
		return false;
	}

	/** true while one of the blobs a BlobImmunity covers lies on the cell */
	public static boolean inHarmfulBlob( int cell ){
		for (Class c : new BlobImmunity().immunities()){
			Blob blob = Dungeon.level.blobs.get( c );
			if (blob != null && blob.volume > 0 && blob.cur[cell] > 0) return true;
		}
		return false;
	}

	private void provoke(){
		BlackMarket market = BlackMarket.of( Dungeon.level );
		if (market != null) market.provoke();
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public boolean interact( Char c ){
		if (sprite != null) sprite.turnTo( pos, c.pos );
		//the stock and the hero's standing are one shared ledger, and every window on
		//it is the host's: a remote player is told so instead of getting a dead click
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;

		final BlackMarket market = BlackMarket.of( Dungeon.level );
		if (market == null) return true;

		Game.runOnRenderThread( new Callback(){
			@Override
			public void call(){
				GameScene.show( new WndBlackMarket( BlackMarketDealer.this, market ) );
			}
		} );
		return true;
	}
}
