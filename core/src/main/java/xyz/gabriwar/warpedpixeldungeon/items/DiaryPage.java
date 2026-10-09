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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//a page torn from a miner's diary, sold by the town librarian. It describes one
//floor of the dungeon in enough detail to map it, but only that floor: read
//anywhere else it is just a sad story about a mine.
public class DiaryPage extends Item {

	public static final String AC_READ = "READ";

	{
		image = ItemSpriteSheet.TORN_PAGE;
		defaultAction = AC_READ;
		stackable = false;
	}

	public int depth = 1;

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_READ );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );
		if (!action.equals( AC_READ )) return;

		if (Dungeon.branch != 0 || Dungeon.depth != depth) {
			GLog.w( Messages.get( this, "wrong_floor", depth ) );
			return;
		}

		detach( hero.belongings.backpack );
		if (ScrollOfMagicMapping.mapCurrentLevel()) {
			SpatialSound.play( Assets.Sounds.SECRET, hero );
		}
		GLog.i( Messages.get( ScrollOfMagicMapping.class, "layout" ) );
		SpellSprite.show( hero, SpellSprite.MAP );
		SpatialSound.play( Assets.Sounds.READ, hero );
		Badges.validateWellRead();
		hero.spendAndNext( 1f );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", depth );
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public int value() {
		return 10;
	}

	private static final String DEPTH = "depth";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( DEPTH, depth );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		depth = bundle.getInt( DEPTH );
	}
}
