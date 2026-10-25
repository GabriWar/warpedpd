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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BogBlessing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WetlandSites;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SunkenIdolSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/**
 * The idol of a shrine half sunk in a swamp (levels/overworld/WetlandSites): an old god of the
 * bog, standing to its waist in the water of its flooded floor. Something left on the water at its
 * feet - gold, or an item worth a little - and the bog's blessing comes over the giver (BogBlessing:
 * no poison, no rot, no parasite takes hold of him, the bog's water drags at him no more), longer
 * for a finer gift; once in WetlandSites.SHRINE_DAYS world days. By night its drowned acolytes rise
 * round it (DrownedAcolyte, laid by OverworldLevel's wetlands). Furniture: nothing hurts it,
 * nothing moves it.
 */
public class SunkenIdol extends NPC {

	{
		spriteClass = SunkenIdolSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	public long siteKey = 0;
	//the world day the idol last took an offering
	public int lastOffer = Integer.MIN_VALUE;

	public static SunkenIdol of( long siteKey ){
		SunkenIdol i = new SunkenIdol();
		i.siteKey = siteKey;
		return i;
	}

	/** The days until the idol takes another offering, 0 when it would now. */
	public int waitDays(){
		return WetlandSites.shrineWait( lastOffer, WorldClock.day() );
	}

	@Override
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;
		int wait = waitDays();
		if (wait > 0){
			GLog.i( Messages.get( this, wait == 1 ? "still_one" : "still", wait ) );
			return true;
		}
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
				description() + "\n\n" + Messages.get( SunkenIdol.class, "prompt", WetlandSites.SHRINE_DAYS ),
				Messages.get( SunkenIdol.class, "offer_gold", WetlandSites.OFFER_GOLD ),
				Messages.get( SunkenIdol.class, "offer_item" ),
				Messages.get( SunkenIdol.class, "leave" ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0){
					if (offerGold( hero )) hero.spendAndNext( 1f );
				} else if (index == 1){
					GameScene.selectItem( gift( hero ) );
				}
			}
		} ) );
		return true;
	}

	/** The gold offering: OFFER_GOLD sinks, the blessing comes over the hero. False, nothing taken,
	 *  when he has not the gold or the idol has had its offering. */
	public boolean offerGold( Hero hero ){
		if (waitDays() > 0) return false;
		if (Dungeon.gold < WetlandSites.OFFER_GOLD){
			GLog.w( Messages.get( this, "poor", WetlandSites.OFFER_GOLD ) );
			return false;
		}
		Dungeon.gold -= WetlandSites.OFFER_GOLD;
		bless( hero, WetlandSites.BLESS_TURNS, true );
		return true;
	}

	/** Can this item go on the water: one of it worth OFFER_WORTH at least, not worn or held, not
	 *  a bag. */
	public static boolean offerable( Item item, Hero hero ){
		if (item instanceof Bag || item.isEquipped( hero ) || item.quantity() <= 0) return false;
		return item.value() / item.quantity() >= WetlandSites.OFFER_WORTH;
	}

	/** The item offering: one of it sinks, and the blessing is the longer the more it was worth. */
	public boolean offerItem( Hero hero, Item item ){
		if (waitDays() > 0 || !offerable( item, hero ) || !hero.belongings.contains( item )) return false;
		Item one = item.detach( hero.belongings.backpack );
		if (one == null) return false;
		Item.updateQuickslot();
		bless( hero, WetlandSites.blessTurns( one.value() ), false );
		return true;
	}

	//render thread, as the bag's choices are
	private WndBag.ItemSelector gift( final Hero hero ){
		return new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( SunkenIdol.class, "gift_prompt" );
			}

			@Override
			public Class<? extends Bag> preferredBag(){
				return Belongings.Backpack.class;
			}

			@Override
			public boolean itemSelectable( Item item ){
				return offerable( item, hero );
			}

			@Override
			public void onSelect( Item item ){
				if (item == null || !hero.isAlive()) return;
				if (offerItem( hero, item )) hero.spendAndNext( 1f );
			}
		};
	}

	private void bless( Hero hero, float turns, boolean gold ){
		lastOffer = WorldClock.day();
		Buff.prolong( hero, BogBlessing.class, turns ).laid( turns );
		if (sprite != null && sprite.parent != null){
			CellEmitter.center( pos ).burst( Speck.factory( Speck.LIGHT ), 8 );
			SpatialSound.play( gold ? Assets.Sounds.GOLD : Assets.Sounds.WATER, pos );
			SpatialSound.play( Assets.Sounds.CHARMS, pos, 0.6f, 0.8f );
		}
		if (hero.sprite != null && hero.sprite.parent != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 5 );
		}
		GLog.p( Messages.get( SunkenIdol.class, gold ? "blessed_gold" : "blessed_item" ) );
		GuideGraph.reveal( GuideGraph.TAG_SUNKEN_SHRINE );
	}

	@Override
	public boolean isImmune( Class effect ){
		return Blob.class.isAssignableFrom( effect ) || super.isImmune( effect );
	}

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public int defenseSkill( Char enemy ){
		return INFINITE_EVASION;
	}

	@Override
	public boolean reset(){
		return true;
	}

	private static final String SITE = "idol_site";
	private static final String LAST_OFFER = "last_offer";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SITE, siteKey );
		bundle.put( LAST_OFFER, lastOffer );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		siteKey = bundle.contains( SITE ) ? bundle.getLong( SITE ) : 0;
		lastOffer = bundle.contains( LAST_OFFER ) ? bundle.getInt( LAST_OFFER ) : Integer.MIN_VALUE;
	}
}
