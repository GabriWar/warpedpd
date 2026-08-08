/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Honeyed;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SugarRush;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bee;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.CombflowerPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.noosa.tweeners.AlphaTweener;

public class Combflower extends Plant {

	{
		image = 52;
		livingPlantImage = 39;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, Honeyed.class, Honeyed.DURATION);
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): spawn a bee
			Bee bee = new Bee();
			bee.spawn( Dungeon.depth );
			bee.setPotInfo( pos, null );
			bee.HP = bee.HT;
			bee.pos = pos;

			GameScene.add( bee );
			Actor.addDelayed( new Pushing( bee, pos, pos ), -1f );

			bee.sprite.alpha( 0 );
			bee.sprite.parent.add( new AlphaTweener( bee.sprite, 1, 0.15f ) );

			Sample.INSTANCE.play( Assets.Sounds.BEE );
			return;
		}

		float duration = SugarRush.DURATION;
		if (ch instanceof Hero){
			duration = (((Hero)ch).subClass == HeroSubClass.WARDEN) ? SugarRush.DURATION * 2 : SugarRush.DURATION;
		}
		Buff.prolong(ch, SugarRush.class, duration);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new CombflowerPoisonParticle().getColor(), 10);
		Buff.prolong(ch, SugarRush.class, 4f);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_COMBFLOWER;
			plantClass = Combflower.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			Buff.prolong(defender, Honeyed.class, Honeyed.DURATION);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return CombflowerPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new CombflowerPoisonParticle();
		}
	}
}
