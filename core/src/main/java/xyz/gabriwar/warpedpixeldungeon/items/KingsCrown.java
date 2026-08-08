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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.Ratmogrify;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndChooseAbility;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class KingsCrown extends Item {
	
	private static final String AC_WEAR = "WEAR";
	
	{
		image = ItemSpriteSheet.CROWN;

		defaultAction = AC_WEAR;

		unique = true;
	}
	
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_WEAR );
		return actions;
	}
	
	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals(AC_WEAR)) {

			curUser = hero;
			if (hero.belongings.armor() != null){
				GameScene.show( new WndChooseAbility(this, hero.belongings.armor(), hero));
			} else {
				GLog.w( Messages.get(this, "naked"));
			}
			
		}
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	public void upgradeArmor(Hero hero, Armor armor, ArmorAbility ability) {

		detach(hero.belongings.backpack);
		Catalog.countUse( getClass() );

		hero.sprite.emitter().burst( Speck.factory( Speck.CROWN), 12 );
		hero.spend(Actor.TICK);
		hero.busy();

		if (armor != null){

			if (ability instanceof Ratmogrify){
				GLog.p(Messages.get(this, "ratgraded"));
			} else {
				GLog.p(Messages.get(this, "upgraded"));
			}

			ClassArmor classArmor = ClassArmor.upgrade(hero, armor);
			if (hero.belongings.armor == armor) {

				hero.belongings.armor = classArmor;
				((HeroSprite) hero.sprite).updateArmor();
				classArmor.activate(hero);

			} else {

				armor.detach(hero.belongings.backpack);
				classArmor.collect(hero.belongings.backpack);

			}
		}

		hero.armorAbility = ability;
		Talent.initArmorTalents(hero);

		hero.sprite.operate( hero.pos );
		Sample.INSTANCE.play( Assets.Sounds.MASTERY );
	}

}
