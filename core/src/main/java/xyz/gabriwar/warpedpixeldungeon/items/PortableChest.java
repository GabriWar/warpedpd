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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A personal storage chest kit. With the builder's tool in hand it can be
 * assembled anywhere in the safe zone, becoming a chest that stores items,
 * can be packed back up (keeping its contents) and re-placed. Three exist
 * per run: wooden, golden and crystal, each roomier than the last.
 */
public class PortableChest extends Item {

	public static final int WOOD    = 0;
	public static final int GOLDEN  = 1;
	public static final int CRYSTAL = 2;

	public static final String AC_PLACE = "PLACE";

	public int type = WOOD;

	//the stored items travel inside the kit when it is packed up
	public StorageSpace storage = new StorageSpace();

	{
		image = ItemSpriteSheet.CHEST;
		unique = true;
		defaultAction = AC_PLACE;
	}

	public PortableChest() {}

	public PortableChest( int type ) {
		this.type = type;
		updateVisuals();
	}

	private void updateVisuals(){
		switch (type){
			case GOLDEN:  image = ItemSpriteSheet.LOCKED_CHEST;  break;
			case CRYSTAL: image = ItemSpriteSheet.CRYSTAL_CHEST; break;
			default:      image = ItemSpriteSheet.CHEST;         break;
		}
		storage.cap = capacityFor(type);
	}

	public static int capacityFor(int type){
		switch (type){
			case GOLDEN:  return 30;
			case CRYSTAL: return 40;
			default:      return 20;
		}
	}

	@Override
	public String name() {
		switch (type){
			case GOLDEN:  return Messages.get(this, "name_golden");
			case CRYSTAL: return Messages.get(this, "name_crystal");
			default:      return Messages.get(this, "name");
		}
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", capacityFor(type), storage.items.size());
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_PLACE );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_PLACE )) {

			if (!(Dungeon.level instanceof SafeLevel)){
				GLog.w( Messages.get(this, "only_safe") );
				return;
			}
			if (hero.belongings.getItem( BuildersTool.class ) == null){
				GLog.w( Messages.get(this, "need_tool") );
				return;
			}

			GameScene.selectCell( placer );
		}
	}

	private final CellSelector.Listener placer = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer cell ) {
			if (cell == null) return;

			if (!Dungeon.level.adjacent( Dungeon.hero.pos, cell )
					|| !Dungeon.level.passable[cell]
					|| Actor.findChar( cell ) != null
					|| Dungeon.level.heaps.get( cell ) != null){
				GLog.w( Messages.get(PortableChest.class, "bad_spot") );
				return;
			}

			StorageChest chest = new StorageChest();
			chest.type = type;
			chest.storage = storage;
			chest.pos = cell;
			GameScene.add( chest );

			detach( Dungeon.hero.belongings.backpack );
			GLog.i( Messages.get(PortableChest.class, "placed") );
			Dungeon.hero.spendAndNext( 1f );
		}

		@Override
		public String prompt() {
			return Messages.get(PortableChest.class, "prompt");
		}
	};

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		switch (type){
			case GOLDEN:  return 600;
			case CRYSTAL: return 900;
			default:      return 300;
		}
	}

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
		storage = (StorageSpace) bundle.get( STORAGE );
		if (storage == null) storage = new StorageSpace();
		updateVisuals();
	}

	//the chest's belly: a bag with a per-type capacity
	public static class StorageSpace extends Bag {

		public int cap = 20;

		{
			image = ItemSpriteSheet.CHEST;
		}

		@Override
		public int capacity() {
			return cap;
		}

		private static final String CAP = "cap";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( CAP, cap );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			cap = bundle.getInt( CAP );
		}
	}
}
