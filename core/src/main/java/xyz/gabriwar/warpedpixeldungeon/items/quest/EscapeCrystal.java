/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.quest;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.VaultLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class EscapeCrystal extends Item {

	{
		image = ItemSpriteSheet.ESCAPE;

		unique = true;

		defaultAction = AC_USE;
	}

	public static final String AC_USE = "USE";

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions( hero );
		actions.add(AC_USE);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {

		super.execute(hero, action);

		if (action.equals( AC_USE )) {

			if (Dungeon.depth > 15 && Dungeon.depth < 20 && Dungeon.branch == 1 && Dungeon.level instanceof VaultLevel){

				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);

				//for full release this will remove any non revive persists buff, but for now just do item buffs
				for (Buff b : hero.buffs()){
					if (b instanceof Wand.Charger
							|| b instanceof Artifact.ArtifactBuff
							|| b instanceof Ring.RingBuff
							//not melee charger, Duelist should retain her charge count
							|| b instanceof ClassArmor.Charger){
						b.detach();
					}
				}

				restoreHeroBelongings(hero);
				hero.updateHT( false );

				Level.beforeTransition();
				InterlevelScene.curTransition = new LevelTransition(Dungeon.level,
						hero.pos,
						LevelTransition.Type.BRANCH_ENTRANCE,
						Dungeon.depth,
						0,
						LevelTransition.Type.BRANCH_EXIT);
				InterlevelScene.mode = InterlevelScene.Mode.ASCEND;
				Game.switchScene( InterlevelScene.class );
				detachAll(hero.belongings.backpack);

			}

		}

	}

	public static String BELONGINGS = "belongings";
	public static String QUICKSLOTS = "quickslots";
	public static String GOLD       = "gold";
	public static String ENERGY     = "energy";

	public void storeHeroBelongings( Hero hero ){
		storedItems = new Bundle();

		Bundle belongings = new Bundle();
		hero.belongings.storeInBundle(belongings);
		storedItems.put(BELONGINGS, belongings);

		Bundle quickslots = new Bundle();
		Dungeon.quickslot.storePlaceholders(quickslots);
		storedItems.put(QUICKSLOTS, quickslots);

		storedItems.put(GOLD, Dungeon.gold);
		storedItems.put(ENERGY, Dungeon.energy);

		Dungeon.quickslot.reset();
		QuickSlotButton.reset();
		Dungeon.gold = Dungeon.energy = 0;
		hero.belongings.clear();
	}

	public void restoreHeroBelongings( Hero hero ){
		hero.belongings.clear();

		Dungeon.quickslot.reset();
		Dungeon.quickslot.restorePlaceholders(storedItems.getBundle(QUICKSLOTS));
		QuickSlotButton.reset();

		Dungeon.hero.belongings.restoreFromBundle(storedItems.getBundle(BELONGINGS));

		Dungeon.gold = storedItems.getInt(GOLD);
		Dungeon.energy = storedItems.getInt(ENERGY);

		storedItems = null;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	public Bundle storedItems;

	public static String STORED_ITEMS = "stored_items";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STORED_ITEMS, storedItems);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		storedItems = bundle.getBundle(STORED_ITEMS);
	}
}
