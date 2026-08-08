/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.ChargrilledMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class FiendBurning extends Burning {

	private float left;

	//for tracking burning of hero items
	private int burnIncrement = 0;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.6f, 0.1f, 0.8f);
	}

	@Override
	public boolean act() {

		if (target.isAlive() && !target.isImmune(getClass())) {

			int damage = Random.NormalIntRange( 1, 3 + Dungeon.depth/4 );
			Buff.detach( target, Chill.class);

			if (target instanceof Hero) {

				Hero hero = (Hero)target;

				if (hero.isImmune(Burning.class)) {
					// Brimstone glyph makes hero immune to fire; no damage taken

				} else {

					hero.damage( damage, this );
					burnIncrement++;

					//at 4+ turns, there is a (turns-3)/3 chance an item burns
					if (Random.Int(3) < (burnIncrement - 3)){
						burnIncrement = 0;

						ArrayList<Item> burnable = new ArrayList<>();
						//does not reach inside of containers
						for (Item i : hero.belongings.backpack.items){
							if ((i instanceof Scroll && !(i instanceof ScrollOfUpgrade))
									|| i instanceof MysteryMeat){
								burnable.add(i);
							}
						}

						if (!burnable.isEmpty()){
							Item toBurn = Random.element(burnable).detach(hero.belongings.backpack);
							GLog.w( Messages.get(this, "burnsup", Messages.capitalize(toBurn.toString())) );
							if (toBurn instanceof MysteryMeat){
								ChargrilledMeat steak = new ChargrilledMeat();
								if (!steak.collect( hero.belongings.backpack )) {
									Dungeon.level.drop( steak, hero.pos ).sprite.drop();
								}
							}
							Heap.burnFX( hero.pos );
						}
					}
				}

			} else {
				target.damage( damage, this );
			}

			if (target instanceof Thief) {

				Item item = ((Thief) target).item;

				if (item instanceof Scroll &&
						!(item instanceof ScrollOfUpgrade)) {
					target.sprite.emitter().burst( ElmoParticle.FACTORY, 6 );
					((Thief)target).item = null;
				} else if (item instanceof MysteryMeat) {
					target.sprite.emitter().burst( ElmoParticle.FACTORY, 6 );
					((Thief)target).item = new ChargrilledMeat();
				}

			}

		} else {

			detach();
		}

		if (Dungeon.level.flamable[target.pos] && Blob.volumeAt(target.pos, Fire.class) == 0) {
			GameScene.add( Blob.seed( target.pos, 4, Fire.class ) );
		}

		spend( TICK );
		left -= TICK;

		if (left <= 0 ) {

			detach();
		}

		return true;
	}

	@Override
	public String heroMessage() {
		return Messages.get(this, "heromsg");
	}

	@Override
	public String toString() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns(left));
	}

	@Override
	public void onDeath() {

		Badges.validateDeathFromFire();

		Dungeon.fail( getClass() );
		GLog.n( Messages.get(this, "ondeath") );
	}
}
