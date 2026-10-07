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
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Identification;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.StewedMeat;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfChilli;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfWarmth;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.MountainSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HermitSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * The old man of a hut on the mountains (levels/overworld/MountainSites). He keeps a small shelf of
 * what the heights call for - a scroll against curses, stew, a chilli draught and an elixir of
 * warmth - at a climber's mark-up; he will look over an item and tell you what it is, once a day
 * for nothing and otherwise for a fee; and he knows the mountains: asked, he tells of a real place
 * near his own (MountainSites.rumour), a new one each day.
 *
 * Kept forever in the parked store as every NPC is; driven off (a shopkeeper's flight), he never
 * comes back. A co-op guest is shown his plain shop.
 */
public class Hermit extends Shopkeeper {

	{
		spriteClass = HermitSprite.class;
	}

	//what he charges over a shop's shelf price: everything up here was carried up
	public static final float MARK_UP = 1.5f;
	//his look at an item costs this share of a scroll of identify from his shelf
	public static final float ID_SHARE = 0.4f;

	public long siteKey = 0;
	public int homeX = Integer.MIN_VALUE, homeY = Integer.MIN_VALUE;
	public int altitude = 1;
	//the world day he last looked at an item for nothing
	public int freeDay = Integer.MIN_VALUE;
	private boolean firstStock = false;

	//today's rumour, worked out once a day (not bundled: a load works it out again)
	private String rumourText;
	private int rumourDay = Integer.MIN_VALUE;

	public static Hermit of( long siteKey, int homeX, int homeY, int altitude ){
		Hermit h = new Hermit();
		h.siteKey = siteKey;
		h.homeX = homeX;
		h.homeY = homeY;
		h.altitude = altitude;
		return h;
	}

	/** Requests the first shelf fill, on his first act (the level is live by then). */
	public void openShop(){
		firstStock = true;
	}

	@Override
	protected boolean act(){
		if (firstStock){
			firstStock = false;
			lastRestock = restockClock();
			restock();
		}
		return super.act();
	}

	@Override
	protected ArrayList<Item> stockSource(){
		ArrayList<Item> stock = new ArrayList<>();
		stock.add( new ScrollOfRemoveCurse() );
		stock.add( new StewedMeat().quantity( 2 ) );
		stock.add( new PotionOfChilli() );
		stock.add( new ElixirOfWarmth() );
		for (Item it : stock){
			it.cursed = false;
			it.identify( false );
		}
		return stock;
	}

	@Override
	public float priceFactor(){
		return MARK_UP;
	}

	@Override
	public String chatTextFor( Hero h ){
		return Messages.get( Hermit.class, "talk_" + Math.floorMod( WorldClock.day() + (int) siteKey, 5 ) );
	}

	/** What he asks today for a look at an item: nothing once a day, else his share of a scroll. */
	public int idFee( int day, Hero buyer ){
		if (day != freeDay) return 0;
		return Math.max( 1, Math.round( ID_SHARE * Shopkeeper.sellPrice( new ScrollOfIdentify(), buyer, this ) ) );
	}

	//today's rumour: worked out on the actor thread (it resolves the sites round his home), once a day
	private String rumour( int day ){
		if (rumourText == null || rumourDay != day){
			rumourText = Dungeon.level instanceof OverworldLevel && homeX != Integer.MIN_VALUE
					? MountainSites.rumour( ((OverworldLevel) Dungeon.level).worldSeed(), altitude, homeX, homeY, siteKey, day )
					: Messages.get( MountainSites.class, "rumour_none" );
			rumourDay = day;
		}
		return rumourText;
	}

	@Override
	public boolean interact( Char c ){
		//a guest gets the plain shop, and the night its closed door
		if (!(c instanceof Hero) || ((Hero) c).isRemote || c != Dungeon.hero
				|| closedForNight() || tradeBlock() != null){
			return super.interact( c );
		}
		final Hero hero = (Hero) c;
		beginTrade( hero );
		final int day = WorldClock.day();
		final String told = rumour( day );
		final int fee = idFee( day, hero );
		final String talk = chatTextFor( hero );
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ), talk,
				Messages.get( Hermit.class, "opt_trade" ),
				fee == 0 ? Messages.get( Hermit.class, "opt_identify_free" ) : Messages.get( Hermit.class, "opt_identify", fee ),
				Messages.get( Hermit.class, "opt_rumour" ) ){
			@Override
			protected void onSelect( int index ){
				if (index == 0){
					showShopMenu( Messages.titleCase( name() ), description(), talk, buybackItems, Dungeon.gold, false );
				} else if (index == 1){
					GameScene.selectItem( identifier( hero ) );
				} else if (index == 2){
					GameScene.show( new WndQuest( Hermit.this, told ) );
				}
			}
		} ) );
		return true;
	}

	//render thread, as the shop windows' trades are
	private WndBag.ItemSelector identifier( final Hero hero ){
		return new WndBag.ItemSelector(){
			@Override
			public String textPrompt(){
				return Messages.get( Hermit.class, "identify_prompt" );
			}

			@Override
			public boolean itemSelectable( Item item ){
				return !item.isIdentified();
			}

			@Override
			public void onSelect( Item item ){
				if (item == null || !hero.isAlive()) return;
				int day = WorldClock.day();
				int fee = idFee( day, hero );
				if (Dungeon.gold < fee){
					GLog.w( Messages.get( Hermit.class, "no_gold", fee ) );
					return;
				}
				if (fee == 0) freeDay = day;
				else Dungeon.gold -= fee;
				if (hero.sprite != null && hero.sprite.parent != null){
					hero.sprite.parent.add( new Identification( hero.sprite.center().offset( 0, -16 ) ) );
				}
				GLog.p( Messages.get( Hermit.class, "identified" ) );
				ScrollOfIdentify.IDItem( item );
			}
		};
	}

	//driven off: his shelf comes down with him and the hut stays empty - no portal of the
	//dungeon's to close up here (Shopkeeper.flee)
	@Override
	public void flee(){
		destroy();
		Notes.remove( landmark() );
		GLog.newLine();
		GLog.n( Messages.get( this, "flee" ) );
		if (sprite != null){
			sprite.killAndErase();
			CellEmitter.get( pos ).burst( ElmoParticle.FACTORY, 6 );
		}
		if (Dungeon.level instanceof OverworldLevel && siteKey != 0){
			((OverworldLevel) Dungeon.level).markSiteCleared( siteKey );
		}
	}

	private static final String SITE = "hermit_site";
	private static final String HOME_X = "home_x";
	private static final String HOME_Y = "home_y";
	private static final String ALTITUDE = "hermit_alt";
	private static final String FREE_DAY = "free_day";
	private static final String FIRST_STOCK = "first_stock";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SITE, siteKey );
		bundle.put( HOME_X, homeX );
		bundle.put( HOME_Y, homeY );
		bundle.put( ALTITUDE, altitude );
		bundle.put( FREE_DAY, freeDay );
		bundle.put( FIRST_STOCK, firstStock );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		siteKey = bundle.contains( SITE ) ? bundle.getLong( SITE ) : 0;
		homeX = bundle.contains( HOME_X ) ? bundle.getInt( HOME_X ) : Integer.MIN_VALUE;
		homeY = bundle.contains( HOME_Y ) ? bundle.getInt( HOME_Y ) : Integer.MIN_VALUE;
		altitude = bundle.contains( ALTITUDE ) ? bundle.getInt( ALTITUDE ) : 1;
		freeDay = bundle.contains( FREE_DAY ) ? bundle.getInt( FREE_DAY ) : Integer.MIN_VALUE;
		firstStock = bundle.contains( FIRST_STOCK ) && bundle.getBoolean( FIRST_STOCK );
	}
}
