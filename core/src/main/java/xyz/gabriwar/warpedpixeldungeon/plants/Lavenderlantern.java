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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.LavenderlanternPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Lavenderlantern extends Plant {

	//OV reads R.integer.lavenderflash for the flash colour
	private static final int FLASH_COLOR = 0xA470DD;

	{
		image = 43;
		livingPlantImage = 60;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 5f; }

	@Override
	public void activate( Char ch ) {
		//identical for stepped-on and no-char triggers in OV
		GameScene.flash( FLASH_COLOR );
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob != null) Buff.prolong( mob, Blindness.class, Random.Int( 2, 5 ) );
		}
		Buff.prolong( Dungeon.hero, Blindness.class, Random.Int( 2, 5 ) );
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		GameScene.flash( FLASH_COLOR );
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob != null) Buff.prolong( mob, Blindness.class, Random.Int( 2, 5 ) );
		}
		Buff.prolong( Dungeon.hero, Blindness.class, Random.Int( 2, 5 ) );
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new LavenderlanternPoisonParticle().getColor(), 10);
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob != null) Buff.prolong( mob, Blindness.class, Random.Int( 4, 10 ) );
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_LAVENDERLANTERN;
			plantClass = Lavenderlantern.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			GameScene.flash( FLASH_COLOR );
			for (Mob mob : Dungeon.level.mobs){
				if (mob != null) Buff.prolong( mob, Blindness.class, Random.Int( 2, 5 ) );
			}
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return LavenderlanternPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new LavenderlanternPoisonParticle();
		}
	}
}
