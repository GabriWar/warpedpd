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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * Plant food. Applied to a growing crop it hurries it along - but with sharp
 * diminishing returns, since each dose only advances a fraction of whatever
 * growth is left, so a nearly-ripe crop barely responds.
 */
public class Fertilizer extends Item {

	public static final String AC_APPLY = "APPLY";

	{
		image = ItemSpriteSheet.FERTILIZER;
		stackable = true;
		defaultAction = AC_APPLY;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_APPLY );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_APPLY )) {
			GameScene.selectCell( selector );
		}
	}

	private final CellSelector.Listener selector = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer cell ) {
			if (cell == null) return;

			Char ch = Actor.findChar( cell );
			if (!(ch instanceof FarmCrop)) {
				GLog.w( Messages.get(Fertilizer.class, "no_crop") );
				return;
			}

			FarmCrop crop = (FarmCrop) ch;
			if (crop.fertilize()) {
				detach( Dungeon.hero.belongings.backpack );
				Sample.INSTANCE.play( Assets.Sounds.PLANT );
				GLog.p( Messages.get(Fertilizer.class, "applied") );
				Dungeon.hero.spendAndNext( 1f );
			} else {
				GLog.i( Messages.get(Fertilizer.class, "no_effect") );
			}
		}

		@Override
		public String prompt() {
			return Messages.get(Fertilizer.class, "prompt");
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
		return 15 * quantity;
	}
}
