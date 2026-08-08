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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Healing;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.VialOfBlood;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class Dewdrop extends Item {
	
	{
		image = ItemSpriteSheet.DEWDROP;
		
		stackable = true;
		dropsDownHeap = true;
	}
	
	@Override
	public boolean doPickUp(Hero hero, int pos) {
		
		Waterskin flask = hero.belongings.getItem( Waterskin.class );
		Catalog.setSeen(getClass());
		Statistics.itemTypesDiscovered.add(getClass());
		
		if (flask != null && !flask.isFull()){

			flask.collectDew( this );
			GameScene.pickUp( this, pos );

		} else {

			int terr = Dungeon.level.map[pos];
			if (!consumeDew(dewValue(), hero, terr == Terrain.ENTRANCE || terr == Terrain.ENTRANCE_SP
					|| terr == Terrain.EXIT || terr == Terrain.UNLOCKED_EXIT)){
				return false;
			} else {
				Catalog.countUse(getClass());
			}
			
		}
		
		Sample.INSTANCE.play( Assets.Sounds.DEWDROP );
		hero.spendAndNext( pickupDelay() );
		
		return true;
	}

	public static boolean consumeDew(int quantity, Hero hero, boolean force){
		//20 drops for a full heal
		int effect = Math.round( hero.HT * 0.05f * quantity );

		int heal = Math.min( hero.HT - hero.HP, effect );

		int shield = 0;
		if (hero.hasTalent(Talent.SHIELDING_DEW)){

			//When vial is present, this allocates exactly as much of the effect as is needed
			// to get to 100% HP, and the rest is then given as shielding (without the vial boost)
			if (quantity > 1 && heal < effect && VialOfBlood.delayBurstHealing()){
				heal = Math.round(heal/VialOfBlood.totalHealMultiplier());
			}

			shield = effect - heal;

			int maxShield = Math.round(hero.HT *0.2f*hero.pointsInTalent(Talent.SHIELDING_DEW));
			int curShield = 0;
			if (hero.buff(Barrier.class) != null) curShield = hero.buff(Barrier.class).shielding();
			shield = Math.min(shield, maxShield-curShield);
		}

		if (heal > 0 || shield > 0) {

			if (heal > 0 && quantity > 1 && VialOfBlood.delayBurstHealing()){
				Healing healing = Buff.affect(hero, Healing.class);
				healing.setHeal(heal, 0, VialOfBlood.maxHealPerTurn());
				healing.applyVialEffect();
			} else {
				hero.HP += heal;
				if (heal > 0){
					hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}

			if (shield > 0) {
				Buff.affect(hero, Barrier.class).incShield(shield);
				hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING );
			}

		} else if (!force) {
			GLog.i( Messages.get(Dewdrop.class, "already_full") );
			return false;
		}

		return true;
	}

	protected int dewValue() {
		return 1;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	//max of one dew in a stack

	@Override
	public Item merge( Item other ){
		if (isSimilar( other )){
			quantity = 1;
			other.quantity = 0;
		}
		return this;
	}

	@Override
	public Item quantity(int value) {
		quantity = Math.min( value, 1);
		return this;
	}

}
