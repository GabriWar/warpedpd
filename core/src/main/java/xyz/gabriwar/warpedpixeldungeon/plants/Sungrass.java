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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Healing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sprouting;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SungrassPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Bundle;

public class Sungrass extends Plant {

	{
		image = 3;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 10f; }

	@Override
	public void activate( Char ch ) {

		if (ch == Dungeon.hero) {
			if (Dungeon.hero.subClass == HeroSubClass.WARDEN) {
				Buff.affect(ch, Healing.class).setHeal(ch.HT, 0, 1);
			} else {
				Buff.affect(ch, Health.class).boost(ch.HT);
			}
		}

		if (ch != null && Dungeon.level.heroFOV[ch.pos]) {
			CellEmitter.get( ch.pos ).start( ShaftParticle.FACTORY, 0.2f, 3 );
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new SungrassPoisonParticle().getColor(), 10);
		Buff.affect(ch, Health.class).boost(4);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, Sprouting.class, Sprouting.DURATION);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SUNGRASS;

			plantClass = Sungrass.class;

			bones = true;
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return SungrassPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new SungrassPoisonParticle();
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			new Sungrass().attackProc(defender, damage);
		}
	}

	public static class Health extends Buff {

		private static final float STEP = 1f;

		private int pos;
		private float partialHeal;
		private int level;

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public boolean act() {
			if (target.pos != pos) {
				detach();
			}

			//for the hero, full heal takes ~50/93/111/120 turns at levels 1/10/20/30
			partialHeal += (40 + target.HT)/150f;

			if (partialHeal > 1){
				int healThisTurn = (int)partialHeal;
				partialHeal -= healThisTurn;
				level -= healThisTurn;

				if (target.HP < target.HT) {

					target.HP += healThisTurn;
					target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healThisTurn), FloatingText.HEALING);

					if (target.HP >= target.HT) {
						target.HP = target.HT;
						if (target instanceof Hero) {
							((Hero) target).resting = false;
						}
					}
				}
			}

			if (level <= 0) {
				detach();
				if (target instanceof Hero){
					((Hero)target).resting = false;
				}
			}
			spend( STEP );
			return true;
		}

		public void boost( int amount ){
			if (target != null) {
				level += amount;
				pos = target.pos;
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.HERB_HEALING;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (target.HT - level) / (float)target.HT);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(level);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", level);
		}

		private static final String POS	= "pos";
		private static final String PARTIAL = "partial_heal";
		private static final String LEVEL = "level";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( POS, pos );
			bundle.put( PARTIAL, partialHeal);
			bundle.put( LEVEL, level);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			pos = bundle.getInt( POS );
			partialHeal = bundle.getFloat( PARTIAL );
			level = bundle.getInt( LEVEL );

		}
	}
}
