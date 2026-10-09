/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;

public class Necrotoxin extends Skill {

	{
		tag = "PB4";
		name = "Necrotoxin";
		castText = "Rot";
		image = 71;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** mana fed back by every tick of poison the rogue has working on an enemy */
	public static final int MANA_PER_TICK = 7;

	/**
	 * Every tick of poison eating an enemy feeds the rogue's own reserves. Called from
	 * {@link xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison}, once per tick, for the
	 * enemy it is ticking on.
	 */
	public static void onPoisonTick( Char enemy ){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || enemy == null || enemy == hero) return;
		if (enemy.alignment != Char.Alignment.ENEMY || hero.heroSkills == null) return;

		Necrotoxin skill = hero.heroSkills.get( Necrotoxin.class );
		if (skill == null || skill.level <= 0) return;

		int cap = hero.MT + xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus( hero );
		if (hero.MP >= cap) return;

		int gain = Math.min( MANA_PER_TICK, cap - hero.MP );
		hero.MP += gain;
		if (hero.sprite != null) hero.sprite.showStatus( 0x44CCFF, "+" + gain + " MP" );
	}

	//a hit can rupture the poison in an enemy: what was left of it lands at once, and half again
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || enemy == null) return damage;
		Poison poison = enemy.buff( Poison.class );
		if (poison == null || Random.Int( 100 ) >= 5 + 15 * level) return damage;

		int burst = Math.round( poison.totalIncomingDMG() * 1.5f );
		poison.detach();

		castTextYell();
		CellEmitter.center( enemy.pos ).burst( PoisonParticle.SPLASH, 10 );
		CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.TOXIC ), 6 );
		if (enemy.sprite != null) new Flare( 6, 20 ).color( 0x66DD44, true ).show( enemy.sprite, 0.6f );
		SpatialSound.play( Assets.Sounds.GAS, enemy, 0.9f, 0.8f );

		//at mastery the rupture sprays onto every enemy next to it
		if (level >= MAX_LEVEL){
			FxTimeline t = FxTimeline.start();
			int order = 0;
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( enemy.pos + n );
				if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY){
					Buff.affect( ch, Poison.class ).set( 5f );
					final int at = ch.pos;
					t.at( 0.08f + 0.05f * order++, () -> CellEmitter.center( at ).burst( PoisonParticle.SPLASH, 4 ) );
				}
			}
		}
		return damage + burst;
	}
}
