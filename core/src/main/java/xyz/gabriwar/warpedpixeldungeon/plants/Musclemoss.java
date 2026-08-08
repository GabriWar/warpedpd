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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.MusclemossPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Musclemoss extends Plant {

	{
		image = 35;
		livingPlantImage = 18;
		seedClass = Seed.class;
	}

	//picks an adjacent direction whose throw slams into something solid.
	//returns 0 when the char stands in fully open ground - the old reroll
	//loop hung the actor thread forever in that case
	private static int slamDirection( int from ){
		java.util.ArrayList<Integer> dirs = new java.util.ArrayList<>();
		for (int path : PathFinder.NEIGHBOURS8){
			int opposite = from + path;
			int beyond = opposite + path;
			if (opposite < 0 || opposite >= Dungeon.level.length()
					|| beyond < 0 || beyond >= Dungeon.level.length()) continue;
			if (!Dungeon.level.passable[opposite] || !Dungeon.level.passable[beyond]){
				dirs.add(path);
			}
		}
		return dirs.isEmpty() ? 0 : Random.element(dirs);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		try {
			int path = slamDirection( enemy.pos );
			int opposite = enemy.pos + path;
			if (path != 0 &&
					((enemy instanceof Mob && Dungeon.level.mobs.contains(enemy)) || enemy instanceof Hero)){
				Ballistica trajectory = new Ballistica(enemy.pos, opposite, Ballistica.MAGIC_BOLT);
				//OV's 0.7.x throwChar never closes doors and always deals collision damage
				WandOfBlastWave.throwChar(enemy, trajectory, damage*10, false, true, this);
			}
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			try {
				if (Actor.findChar(pos) != null){
					int path = slamDirection( pos );
					int opposite = pos + path;
					if (path != 0 &&
							((Actor.findChar(pos) instanceof Mob && Dungeon.level.mobs.contains(Actor.findChar(pos))) || Actor.findChar(pos) instanceof Hero)){
						Ballistica trajectory = new Ballistica(pos, opposite, Ballistica.MAGIC_BOLT);
						WandOfBlastWave.throwChar(Actor.findChar(pos), trajectory, 100, false, true, this);
					}
				}
			} catch (Exception e){
				Game.reportException(e);
			}
			return;
		}
		try {
			int path = slamDirection( ch.pos );
			int opposite = ch.pos + path;
			if (path != 0 &&
					((ch instanceof Mob && Dungeon.level.mobs.contains(ch)) || ch instanceof Hero)){
				Ballistica trajectory = new Ballistica(ch.pos, opposite, Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(ch, trajectory, 100, false, true, this);
			}
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new MusclemossPoisonParticle().getColor(), 10);
		try {
			int path = slamDirection( ch.pos );
			int opposite = ch.pos + path;
			if (path != 0 &&
					((ch instanceof Mob && Dungeon.level.mobs.contains(ch)) || ch instanceof Hero)){
				Ballistica trajectory = new Ballistica(ch.pos, opposite, Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(ch, trajectory, 100, false, true, this);
			}
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_MUSCLEMOSS;
			plantClass = Musclemoss.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			new Musclemoss().attackProc(defender, damage);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return MusclemossPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new MusclemossPoisonParticle();
		}
	}
}
