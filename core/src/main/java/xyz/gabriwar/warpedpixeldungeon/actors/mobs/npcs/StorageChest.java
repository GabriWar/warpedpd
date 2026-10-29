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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.StorageChestSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

/**
 * A placed personal storage chest out in the open world. Deposit and withdraw
 * items freely; pack it up to move it - the contents travel with it.
 */
public class StorageChest extends Mob implements xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor {

	{
		spriteClass = StorageChestSprite.class;

		HP = HT = 1;
		defenseSkill = 0;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	public int type = PortableChest.WOOD;
	public PortableChest.StorageSpace storage = new PortableChest.StorageSpace();

	/** What it holds where it stands: its own room, and more in a homestead's storehouse
	 *  (homestead/rooms/RoomTypes.chestBonus). Packed up it holds its own again. */
	public int capacity() {
		return PortableChest.capacityFor(type)
				+ xyz.gabriwar.warpedpixeldungeon.homestead.rooms.RoomTypes.chestBonus( Dungeon.level, pos );
	}

	@Override
	public String name() {
		switch (type){
			case PortableChest.GOLDEN:  return Messages.get(PortableChest.class, "name_golden");
			case PortableChest.CRYSTAL: return Messages.get(PortableChest.class, "name_crystal");
			default:                    return Messages.get(PortableChest.class, "name");
		}
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", storage.items.size(), capacity());
	}

	@Override
	protected boolean act() {
		throwItems();
		spend( TICK );
		return true;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//indestructible
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero){
			return true;
		}

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions(
						sprite(),
						Messages.titleCase( name() ),
						Messages.get(StorageChest.class, "wnd_msg", storage.items.size(), capacity()),
						Messages.get(StorageChest.class, "open"),
						Messages.get(StorageChest.class, "deposit"),
						Messages.get(StorageChest.class, "pack"),
						Messages.get(StorageChest.class, "cancel") ){
					@Override
					protected void onSelect( int index ) {
						switch (index){
							case 0: openChest(); break;
							case 1: deposit(); break;
							case 2: packUp(); break;
						}
					}
				} );
			}
		});

		return true;
	}

	private void openChest(){
		if (storage.items.isEmpty()){
			GLog.i( Messages.get(this, "empty") );
			return;
		}
		//the grid fits all it holds: more than it now may, once out of a storehouse
		storage.cap = Math.max( capacity(), storage.items.size() );
		GameScene.show( new WndBag( storage, withdrawSelector ) );
	}

	private void deposit(){
		GameScene.selectItem( depositSelector );
	}

	private void packUp(){
		PortableChest kit = new PortableChest( type );
		storage.cap = Math.max( PortableChest.capacityFor( type ), storage.items.size() );
		kit.storage = storage;
		if (kit.doPickUp( Dungeon.hero )){
			GLog.i( Messages.get(this, "packed") );
		} else {
			Dungeon.level.drop( kit, pos ).sprite.drop();
		}
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	/** Only what lies in the chest is taken out: the window also shows the hero's gear and the
	 *  chest's belly itself, and taking either copied it into the backpack (an equipped item
	 *  twice over; the belly into itself, which no save could then write). */
	boolean holds( Item item ){
		return item != storage && storage.items.contains( item );
	}

	private final WndBag.ItemSelector withdrawSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(StorageChest.class, "withdraw_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return holds( item );
		}

		@Override
		public void onSelect(Item item) {
			if (item != null && holds( item )){
				item.detachAll( storage );
				if (!item.collect( Dungeon.hero.belongings.backpack )){
					Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
				}
			}
		}
	};

	private final WndBag.ItemSelector depositSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(StorageChest.class, "deposit_prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			//no bags, and no storing another chest inside a chest
			return !(item instanceof xyz.gabriwar.warpedpixeldungeon.items.bags.Bag)
					&& !(item instanceof PortableChest);
		}

		@Override
		public void onSelect(Item item) {
			if (item != null){
				int cap = capacity();
				if (storage.items.size() >= cap){
					GLog.w( Messages.get(StorageChest.class, "full") );
					return;
				}
				storage.cap = cap;
				item.detachAll( Dungeon.hero.belongings.backpack );
				if (!item.collect( storage )){
					GLog.w( Messages.get(StorageChest.class, "full") );
					if (!item.collect( Dungeon.hero.belongings.backpack )){
						Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
					}
				}
			}
		}
	};

	private static final String TYPE    = "type";
	private static final String STORAGE = "storage";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TYPE, type );
		bundle.put( STORAGE, storage );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		type = bundle.getInt( TYPE );
		storage = (PortableChest.StorageSpace) bundle.get( STORAGE );
		if (storage == null) storage = new PortableChest.StorageSpace();
	}
}
