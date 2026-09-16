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

package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MasterThiefCoins;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/** Small gold coins spinning around the hero, one per coin held. */
public class MasterThiefCoinsFX extends Group {

	private final Char owner;
	private final MasterThiefCoins state;
	private final ItemSprite[] coins = new ItemSprite[MasterThiefCoins.MAX_COINS];
	private float time;

	public MasterThiefCoinsFX( Char owner, MasterThiefCoins state ){
		this.owner = owner;
		this.state = state;
		for (int i = 0; i < coins.length; i++){
			coins[i] = new ItemSprite( ItemSpriteSheet.GOLD );
			coins[i].scale.set( 0.55f );
			coins[i].origin.set( coins[i].width / 2, coins[i].height / 2 );
			add( coins[i] );
		}
	}

	@Override
	public void update(){
		super.update();
		if (owner.sprite == null || !owner.sprite.exists){ killAndErase(); return; }
		time += Game.elapsed;
		int count = state.coins();
		PointF center = owner.sprite.center();
		for (int i = 0; i < coins.length; i++){
			ItemSprite coin = coins[i];
			coin.visible = owner.sprite.visible && i < count;
			if (!coin.visible) continue;
			double a = time * 2.4 + i * Math.PI * 2 / count;
			coin.x = center.x + (float) Math.cos( a ) * 11 - coin.width / 2;
			coin.y = center.y - 2 + (float) Math.sin( a ) * 5 - coin.height / 2;
		}
	}
}
