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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BellowsValveSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

/**
 * The gas canister at the windward end of The Bellows: a tank of mine gas plumbed into
 * the wall, with a wheel to let it out. Turning the wheel empties it once, and it goes
 * off as one cloud over the whole gallery - the cell the hero stands on included - after
 * which the draught carries the lot downwind. It says what it is and asks before it is
 * opened - the room's puzzle is what the wind will do with the gas, not what the wheel
 * is for. It is the room's offered tool, not its only one: the draught carries any cloud
 * the hero brings.
 */
public class BellowsValve extends NPC {

	{
		spriteClass = BellowsValveSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	//the canister goes off as one room-wide cloud, so this is what each cell of the
	//gallery gets: roughly the turns of gas that stand in it before it thins out
	private static final int CHARGE = 20;
	//what a save made before the canister filled the room let out of the pipe, in one cell
	private static final int CANISTER = 500;

	/** where the pipe lets out: the cell downwind of the valve, set by the room */
	public int outlet = -1;
	/** every cell the gas fills at once: the room's interior, set by the room */
	public int[] gallery = null;
	private boolean spent = false;

	public boolean spent(){
		return spent;
	}

	@Override
	public boolean interact( Char c ){
		if (!(c instanceof Hero)) return true;
		final Hero hero = (Hero) c;
		if (spent || outlet < 0){
			if (c == Dungeon.hero) GLog.i( Messages.get( this, "empty" ) );
			return true;
		}
		//a remote player has no window to be asked in: their hand on the wheel is the answer
		if (hero.isRemote){
			open( hero );
			return true;
		}
		Game.runOnRenderThread( new Callback(){
			@Override
			public void call(){
				GameScene.show( new WndOptions( sprite(),
						Messages.titleCase( name() ),
						Messages.get( BellowsValve.class, "ask" ),
						Messages.get( BellowsValve.class, "ask_yes" ),
						Messages.get( BellowsValve.class, "ask_no" ) ){
					@Override
					protected void onSelect( int index ){
						if (index == 0) open( hero );
					}
				} );
			}
		} );
		return true;
	}

	private void open( Hero hero ){
		if (spent) return;
		spent = true;
		hero.sprite.operate( pos );
		hero.spendAndNext( 1f );
		SpatialSound.play( Assets.Sounds.UNLOCK, pos, 1f, 0.7f );
		SpatialSound.play( Assets.Sounds.GAS, pos );
		if (gallery != null && gallery.length > 0){
			for (int cell : gallery) GameScene.add( Blob.seed( cell, CHARGE, ToxicGas.class ) );
		} else {
			GameScene.add( Blob.seed( outlet, CANISTER, ToxicGas.class ) );
		}
		if (sprite instanceof BellowsValveSprite) ((BellowsValveSprite) sprite).show( true );
		GLog.w( Messages.get( this, "opened" ) );
	}

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public String description(){
		return Messages.get( this, spent ? "desc_spent" : "desc" );
	}

	private static final String OUTLET  = "outlet";
	private static final String GALLERY = "gallery";
	private static final String SPENT   = "spent";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( OUTLET, outlet );
		if (gallery != null) bundle.put( GALLERY, gallery );
		bundle.put( SPENT, spent );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		outlet = bundle.getInt( OUTLET );
		gallery = bundle.contains( GALLERY ) ? bundle.getIntArray( GALLERY ) : null;
		spent = bundle.getBoolean( SPENT );
	}
}
