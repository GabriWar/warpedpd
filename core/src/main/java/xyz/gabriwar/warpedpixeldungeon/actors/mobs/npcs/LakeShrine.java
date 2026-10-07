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
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CaveShrineSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * The idol of a lake shrine in the caves (levels/overworld/CaveSites): miners toss gold into its
 * still pool for luck. A hero may too, once a world day (WorldClock.day): a little gold for a
 * short blessing, more for a longer one - the dungeon's own Bless. The price grows with the
 * depth of the slice. The idol is furniture: nothing hurts it, nothing moves it.
 */
public class LakeShrine extends NPC {

	{
		spriteClass = CaveShrineSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	//what each offering is worth, in base offerings, and the blessing it buys, in turns
	public static final int[] TIERS = { 1, 3, 8 };
	public static final float[] TURNS = { 40f, 100f, 200f };

	/** The smallest offering on a slice: dearer the deeper the pool. */
	public static int baseOffer( int altitude ){
		return 40 + 20 * Math.max( 1, -altitude );
	}

	//the world day the idol last took an offering
	public int lastOffer = Integer.MIN_VALUE;

	public boolean blessedToday(){
		return lastOffer == WorldClock.day();
	}

	private static int altitudeHere(){
		int a = WorldLayers.altitudeOf( Dungeon.depth );
		return a == Integer.MIN_VALUE ? -1 : a;
	}

	@Override
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;
		if (blessedToday()){
			GLog.i( Messages.get( this, "already" ) );
			return true;
		}
		final int base = baseOffer( altitudeHere() );
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
				description() + "\n\n" + Messages.get( LakeShrine.class, "prompt" ),
				Messages.get( LakeShrine.class, "offer", base * TIERS[0] ),
				Messages.get( LakeShrine.class, "offer", base * TIERS[1] ),
				Messages.get( LakeShrine.class, "offer", base * TIERS[2] ),
				Messages.get( LakeShrine.class, "leave" ) ){
			@Override
			protected void onSelect( int index ){
				if (index < 0 || index >= TIERS.length) return;
				if (pay( hero, index )){
					if (sprite != null && sprite.parent != null){
						CellEmitter.center( pos ).burst( Speck.factory( Speck.LIGHT ), 6 );
						Sample.INSTANCE.play( Assets.Sounds.GOLD );
					}
					GLog.p( Messages.get( LakeShrine.class, index == 0 ? "blessed_small" : index == 1 ? "blessed_mid" : "blessed_large" ) );
					hero.spendAndNext( 1f );
				}
			}
		} ) );
		return true;
	}

	/** The offering of a tier, if the hero has the gold and the idol has had none today: the gold
	 *  sinks, the blessing is laid on. False, and nothing taken, otherwise. */
	public boolean pay( Hero hero, int tier ){
		if (blessedToday()){
			GLog.i( Messages.get( this, "already" ) );
			return false;
		}
		int price = baseOffer( altitudeHere() ) * TIERS[tier];
		if (Dungeon.gold < price){
			GLog.w( Messages.get( this, "poor" ) );
			return false;
		}
		Dungeon.gold -= price;
		lastOffer = WorldClock.day();
		Buff.prolong( hero, Bless.class, TURNS[tier] );
		return true;
	}

	@Override
	public boolean isImmune( Class effect ){
		return Blob.class.isAssignableFrom( effect ) || super.isImmune( effect );
	}

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	private static final String LAST_OFFER = "last_offer";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LAST_OFFER, lastOffer );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		lastOffer = bundle.contains( LAST_OFFER ) ? bundle.getInt( LAST_OFFER ) : Integer.MIN_VALUE;
	}
}
