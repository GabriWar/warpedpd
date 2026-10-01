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
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * Type Shifter: raises an item's type one step, gamma to beta for one shifter, beta to
 * alpha for two. It never lowers a type and alpha cannot be shifted further.
 */
public class TypeShifter extends Item {

	public static final String AC_SHIFT = "SHIFT";

	{
		image = ItemSpriteSheet.TYPE_SHIFTER;
		stackable = true;
		defaultAction = AC_SHIFT;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_SHIFT );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( AC_SHIFT )){
			curUser = hero;
			curItem = this;
			GameScene.selectItem( selector );
		}
	}

	private final WndBag.ItemSelector selector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt(){
			return Messages.get( TypeShifter.class, "prompt" );
		}

		@Override
		public Class<? extends Bag> preferredBag(){
			return xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ){
			return Quality.eligible( item );
		}

		@Override
		public void onSelect( Item item ){
			if (item == null) return;
			Quality q = Quality.of( item );
			if (q == null){
				Quality.roll( item );
				q = Quality.of( item );
			}
			if (q.type.alpha()){
				GLog.w( Messages.get( TypeShifter.class, "already_alpha", item.name() ) );
				return;
			}
			int cost = q.type.shiftCost();
			if (quantity() < cost){
				GLog.w( Messages.get( TypeShifter.class, "not_enough", cost, q.type.next().title() ) );
				return;
			}
			for (int i = 0; i < cost; i++) detach( curUser.belongings.backpack );
			q.shiftType();
			curUser.sprite.operate( curUser.pos );
			curUser.spend( 1f );
			curUser.busy();
			Sample.INSTANCE.play( Assets.Sounds.READ );
			curUser.sprite.burst( q.type.color, 10 );
			Enchanting.show( curUser, item );
			GLog.p( Messages.get( TypeShifter.class, "shifted", item.name(), q.type.title() ) );
			updateQuickslot();
		}
	};

	@Override
	public boolean isUpgradable(){ return false; }

	@Override
	public boolean isIdentified(){ return true; }

	@Override
	public int value(){ return 80 * quantity; }
}
