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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Shadow;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.ShadowbloomPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Shadowbloom extends Plant {

	{
		image = 61;
		livingPlantImage = 63;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -15f; }

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			MirrorImage mob = new MirrorImage();
			mob.duplicate(Dungeon.hero);
			GameScene.add(mob);
			ScrollOfTeleportation.appear( mob, pos );
			GLog.p(Messages.get(Shadow.class, "mirrorimage"));
			return;
		}
		Buff.prolong(ch, Shadow.class, Shadow.DURATION);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, Shadow.class, Shadow.DURATION);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new ShadowbloomPoisonParticle().getColor(), 10);
		Buff.prolong(ch, Shadow.class, 4f);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SHADOWBLOOM;
			plantClass = Shadowbloom.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			Buff.prolong( defender, Blindness.class, Random.Int( damage, damage*2 ) );
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return ShadowbloomPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new ShadowbloomPoisonParticle();
		}
	}
}
