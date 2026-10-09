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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;

/**
 * Masterwork Core: dropped by bosses, spent at the troll blacksmith in town
 * or the dungeon's quest smith to masterwork an item one step at a time (Destiny 2
 * style) or to reroll its rarity on a wheel. Steps 1-5 cost one core, 6-9 two, the
 * tenth three and only a legendary or exotic base can take it; a reroll stakes one to
 * ten cores, more for better odds (see Rarity.rerollChances).
 */
public class MasterworkCore extends Item {

	{
		image = ItemSpriteSheet.MASTERWORK_CORE;
		stackable = true;
	}

	/** Items the smith can work cores into. */
	public static boolean workable( Item item ){
		return Quality.hasRarity( item );
	}

	//the item's quality, rolled now if it never got one
	private static Quality qualityOf( Item item ){
		Quality q = Quality.of( item );
		if (q == null){
			Quality.roll( item );
			q = Quality.of( item );
		}
		return q;
	}

	/** Cores in the hero's pack. */
	public static int held( Hero hero ){
		MasterworkCore cores = hero.belongings.getItem( MasterworkCore.class );
		return cores == null ? 0 : Math.max( 0, cores.quantity() );
	}

	/**
	 * Takes exactly {@code amount} cores from the hero's pack, or none at all when he
	 * has fewer: the stack shrinks in one step (never one detach per core, which could
	 * run past the stack) and goes when it reaches nothing.
	 */
	public static boolean spend( Hero hero, int amount ){
		MasterworkCore cores = hero.belongings.getItem( MasterworkCore.class );
		if (amount <= 0 || cores == null || cores.quantity() < amount) return false;
		if (cores.quantity() == amount){
			cores.detachAll( hero.belongings.backpack );
		} else {
			cores.quantity( cores.quantity() - amount );
		}
		Item.updateQuickslot();
		return true;
	}

	//the smith's hammer on the item: a turn spent, a flash of light
	private static void worked( Hero hero, Item item ){
		hero.sprite.operate( hero.pos );
		hero.spend( 1f );
		hero.busy();
		SpatialSound.play( Assets.Sounds.EVOKE, hero );
		hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
		Enchanting.show( hero, item );
		item.updateQuickslot();
	}

	/** A smith's work: the hero picks the item, then it is masterworked one step or rerolled. */
	public static void choose( final Hero hero, final boolean masterwork ){
		GameScene.selectItem( new WndBag.ItemSelector() {
			@Override
			public String textPrompt(){
				return Messages.get( MasterworkCore.class, masterwork ? "pick_masterwork" : "pick_reroll" );
			}
			@Override
			public Class<? extends Bag> preferredBag(){
				return xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings.Backpack.class;
			}
			@Override
			public boolean itemSelectable( Item item ){
				return workable( item );
			}
			@Override
			public void onSelect( Item item ){
				if (item == null) return;
				if (masterwork) masterwork( hero, item );
				else GameScene.show( new xyz.gabriwar.warpedpixeldungeon.windows.WndRarityWheel( hero, item ) );
			}
		} );
	}

	/** The smith's menu for a hero carrying cores: masterwork, reroll, and whatever else
	 *  the smith offers after them (the option labels and what they do are the smith's). */
	public static String[] menuOptions( String... more ){
		String[] out = new String[2 + more.length];
		out[0] = Messages.get( MasterworkCore.class, "opt_masterwork" );
		out[1] = Messages.get( MasterworkCore.class, "opt_reroll", Rarity.MAX_STAKE );
		System.arraycopy( more, 0, out, 2, more.length );
		return out;
	}

	/** One masterwork step on the item, paid in cores from the hero's pack. */
	public static void masterwork( Hero hero, Item item ){
		Quality q = qualityOf( item );
		int cost = q.masterworkCost();
		if (cost == 0){
			GLog.w( Messages.get( MasterworkCore.class, q.fullyMasterworked() ? "already" : "needs_legendary", item.name() ) );
			return;
		}
		if (!spend( hero, cost )){
			GLog.w( Messages.get( MasterworkCore.class, "not_enough", cost ) );
			return;
		}
		q.masterworkStep( item );
		worked( hero, item );
		GLog.p( Messages.get( MasterworkCore.class, q.fullyMasterworked() ? "done_full" : "done", item.name(), q.masterwork, Quality.MASTERWORK_MAX ) );
	}

	/** The wheel's outcome worked into the item (WndRarityWheel, once the cores are spent). */
	public static void rerolled( Hero hero, Item item, Rarity to ){
		qualityOf( item ).rerollRarity( item, to );
		worked( hero, item );
		GLog.p( Messages.get( MasterworkCore.class, "rerolled", item.name(), to.title() ) );
	}

	/** The item's quality, rolled now if it never got one (the wheel shows its current tier). */
	public static Quality quality( Item item ){
		return qualityOf( item );
	}

	@Override
	public boolean isUpgradable(){ return false; }

	@Override
	public boolean isIdentified(){ return true; }

	@Override
	public int value(){ return 1000 * quantity; }
}
