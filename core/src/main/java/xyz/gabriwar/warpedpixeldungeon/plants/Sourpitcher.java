/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2016-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.plants;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Digesting;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.PitcherPlant;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SourPitcherPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.noosa.tweeners.AlphaTweener;

public class Sourpitcher extends Plant {

	{
		image = 40;
		livingPlantImage = 51;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			PitcherPlant pitcherPlant = new PitcherPlant();
			pitcherPlant.pos = pos;
			pitcherPlant.spawn(Dungeon.depth, 0);
			GameScene.add( pitcherPlant );
			Actor.addDelayed( new Pushing( pitcherPlant, pos, pos ), -1f );

			pitcherPlant.sprite.alpha( 0 );
			pitcherPlant.sprite.parent.add( new AlphaTweener( pitcherPlant.sprite, 1, 0.15f ) );
			return;
		}

		Buff.prolong(ch, Digesting.class, Digesting.DURATION);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new SourPitcherPoisonParticle().getColor(), 10);
		Buff.prolong(ch, Digesting.class, 4f);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, Digesting.class, Digesting.DURATION);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SOURPITCHER;
			plantClass = Sourpitcher.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			attacker.HP += Math.round(damage/2);
			if (attacker.HP > attacker.HT) attacker.HP = attacker.HT;
			defender.damage(Math.round(damage/2), attacker);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return SourPitcherPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new SourPitcherPoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}
