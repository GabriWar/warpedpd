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

package xyz.gabriwar.warpedpixeldungeon.items.journal;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//A page of the Descent Guide, torn loose and left on the dungeon floor.
//Picking it up permanently unlocks that page in the guide.
public class DescentPage extends Item {

	{
		image = ItemSpriteSheet.GUIDE_PAGE;
	}

	private String page;

	public void page( String page ){
		this.page = page;
	}

	public String page(){
		return page;
	}

	@Override
	public final boolean doPickUp(Hero hero, int pos) {
		GameScene.pickUpJournal(this, pos);
		if (GuideProgress.pageFound(page)){
			GLog.i( Messages.get(this, "already_found") );
		} else {
			GuideProgress.findPage(page);
			GLog.p( Messages.get(this, "unlocked", GuideGraph.titleForKey(page)) );
		}
		Sample.INSTANCE.play( Assets.Sounds.ITEM );
		hero.spendAndNext( pickupDelay() );
		return true;
	}

	@Override
	public String desc() {
		String title = GuideGraph.titleForKey(page);
		return Messages.get(this, "desc", title == null ? "?" : title);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	private static final String PAGE = "page";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( PAGE, page );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		page = bundle.getString( PAGE );
	}
}
