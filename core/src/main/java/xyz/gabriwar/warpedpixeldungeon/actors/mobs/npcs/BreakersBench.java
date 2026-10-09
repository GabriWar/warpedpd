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
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.ArcaneResin;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.LiquidMetal;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BreakersBenchSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

/**
 * The Breaker's Bench: a vice, a maul and a hopper for what is left. It is the dismantle
 * source the rarity design calls for (docs/rarity-types-draft.md, "Masterwork"):
 *
 *   a legendary or exotic piece   one masterwork core
 *   a rare piece                  one share of scrap; three shares make a core
 *   an uncommon piece             salvage: liquid metal by its tier, or arcane resin from a wand
 *   a common piece                nothing in it worth the swing - the bench will not take it
 *
 * Forgeday is the smiths' day and the bench keeps it: every core that comes off it then
 * comes off as two. The scrap hopper belongs to the bench, so it is saved with the floor.
 */
public class BreakersBench extends NPC {

	{
		spriteClass = BreakersBenchSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	public static final int SCRAP_PER_CORE = 3;

	private int scrap = 0;

	public int scrap(){
		return scrap;
	}

	private static boolean forgeday(){
		return GameCalendar.weekday() == GameCalendar.Weekday.FORGEDAY;
	}

	/** what breaking this item yields, or null when the bench will not take it */
	private static boolean breakable( Item item ){
		if (!Quality.hasRarity( item )) return false;
		if (item.isEquipped( Dungeon.hero ) || item.unique) return false;
		if (item instanceof Armor && ((Armor) item).checkSeal() != null) return false;
		Quality q = Quality.of( item );
		return q != null && q.rarity != Rarity.COMMON;
	}

	@Override
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;
		Game.runOnRenderThread( new Callback(){
			@Override
			public void call(){
				GameScene.selectItem( breaking );
			}
		} );
		return true;
	}

	private final WndBag.ItemSelector breaking = new WndBag.ItemSelector(){
		@Override
		public String textPrompt(){
			return Messages.get( BreakersBench.class, "prompt", scrap, SCRAP_PER_CORE );
		}
		@Override
		public Class<? extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}
		@Override
		public boolean itemSelectable( Item item ){
			return breakable( item );
		}
		@Override
		public void onSelect( final Item item ){
			if (item == null || !breakable( item )) return;
			String body = Messages.get( BreakersBench.class, "confirm", item.name(), yieldText( item ) );
			if (item.level() > 0) body += "\n\n" + Messages.get( BreakersBench.class, "confirm_upgrades", item.level() );
			GameScene.show( new WndOptions( sprite(),
					Messages.titleCase( name() ),
					body,
					Messages.get( BreakersBench.class, "yes" ),
					Messages.get( BreakersBench.class, "no" ) ){
				@Override
				protected void onSelect( int index ){
					if (index == 0) smash( Dungeon.hero, item );
				}
			} );
		}
	};

	private String yieldText( Item item ){
		Rarity rarity = Quality.of( item ).rarity;
		int mult = forgeday() ? 2 : 1;
		if (rarity.ordinal() >= Rarity.LEGENDARY.ordinal()){
			return Messages.get( this, "yield_core", mult );
		} else if (rarity == Rarity.RARE){
			return scrap + 1 >= SCRAP_PER_CORE
					? Messages.get( this, "yield_scrap_core", mult )
					: Messages.get( this, "yield_scrap", scrap + 1, SCRAP_PER_CORE );
		} else if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.wands.Wand){
			return Messages.get( this, "yield_resin" );
		} else {
			return Messages.get( this, "yield_metal", metalFor( item ) );
		}
	}

	//liquid metal is counted in durability: a heavier piece melts down to more of it
	private static int metalFor( Item item ){
		int tier = 1;
		if (item instanceof MeleeWeapon)        tier = ((MeleeWeapon) item).tier;
		else if (item instanceof Armor)         tier = ((Armor) item).tier;
		else if (item instanceof MissileWeapon) tier = ((MissileWeapon) item).tier;
		return 10 * Math.max( 1, tier );
	}

	private void smash( Hero hero, Item item ){
		if (!hero.belongings.contains( item ) || !breakable( item )) return;
		Rarity rarity = Quality.of( item ).rarity;
		boolean wand = item instanceof xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
		int metal = metalFor( item );
		//one piece off a stack of thrown weapons, the whole of anything else
		item.detach( hero.belongings.backpack );

		hero.sprite.operate( pos );
		hero.spendAndNext( 2f );
		SpatialSound.play( Assets.Sounds.HIT_STRONG, pos );
		SpatialSound.play( Assets.Sounds.SHATTER, pos, 0.7f, 0.8f );
		CellEmitter.center( pos ).burst( Speck.factory( Speck.STAR ), 5 );
		if (sprite instanceof BreakersBenchSprite) ((BreakersBenchSprite) sprite).strike();

		int cores = 0;
		if (rarity.ordinal() >= Rarity.LEGENDARY.ordinal()){
			cores = 1;
		} else if (rarity == Rarity.RARE){
			scrap++;
			if (scrap >= SCRAP_PER_CORE){
				scrap -= SCRAP_PER_CORE;
				cores = 1;
			} else {
				GLog.i( Messages.get( this, "got_scrap", scrap, SCRAP_PER_CORE ) );
			}
		} else if (wand){
			give( hero, new ArcaneResin() );
			GLog.i( Messages.get( this, "got_resin" ) );
		} else {
			give( hero, new LiquidMetal().quantity( metal ) );
			GLog.i( Messages.get( this, "got_metal", metal ) );
		}

		if (cores > 0){
			if (forgeday()) cores *= 2;
			give( hero, new MasterworkCore().quantity( cores ) );
			GLog.p( Messages.get( this, forgeday() ? "got_core_forgeday" : "got_core", cores ) );
		}
	}

	private void give( Hero hero, Item item ){
		if (!item.doPickUp( hero )){
			Dungeon.level.drop( item, hero.pos ).sprite.drop();
		}
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
		return Messages.get( this, "desc", scrap, SCRAP_PER_CORE )
				+ (forgeday() ? "\n\n" + Messages.get( this, "desc_forgeday" ) : "");
	}

	private static final String SCRAP = "scrap";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SCRAP, scrap );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		scrap = bundle.getInt( SCRAP );
	}
}
