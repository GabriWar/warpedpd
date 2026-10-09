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


import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

public class LastStand extends Skill {

	{
		tag = "PA4";
		name = "Last Stand";
		image = 14;
		tier = 4;
	}

	private static final float COOLDOWN = 40f;
	private static final String READY = "LASTSTAND_READY";

	//game-clock time at which the stand can be taken again
	private float readyAt = 0;

	@Override
	protected boolean upgrade(){
		return true;
	}

	//this runs inside Hero.damage, so the blow has already lost the armour's dr:
	//the barrier answers the damage the hero is really about to take, and soaks it
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || source instanceof Buff.DOTbuff) return 0;
		if (hero.HP - damage > hero.HT / 4) return 0;
		float now = Statistics.duration + Actor.now();
		if (now < readyAt) return 0;
		readyAt = now + COOLDOWN;

		Buff.affect( hero, Barrier.class ).setShield( SkillInteractions.ofHealth( hero.HT, 0.05f * level ) );
		castTextYell();
		if (hero.sprite != null){
			//the stand: a golden flare, a second wider one a beat later, and light streaming off him
			new Flare( 6, 20 ).color( 0xFFCC66, true ).show( hero.sprite, 0.6f );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 5 );
			hero.sprite.emitter().start( Speck.factory( Speck.YELLOW_LIGHT ), 0.05f, 6 );
			xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX.after( 0.2f, () -> {
				if (hero.sprite != null) new Flare( 8, 28 ).color( 0xFFE6A1, true ).show( hero.sprite, 0.5f );
			} );
		}
		SpatialSound.play( Assets.Sounds.STURDY, hero, 1f, 0.8f );

		if (level >= MAX_LEVEL){
			SkillFX.land( hero.pos );
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( hero.pos + n );
				if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
					if (!Char.hasProp( ch, Char.Property.BOSS ) && !Char.hasProp( ch, Char.Property.MINIBOSS ))
						Buff.affect( ch, Paralysis.class, 2f );
					if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
				}
			}
		}
		return 0;
	}

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( READY, readyAt );
	}

	@Override
	public void restoreInBundle( Bundle bundle ){
		super.restoreInBundle( bundle );
		readyAt = bundle.getFloat( READY );
	}
}
