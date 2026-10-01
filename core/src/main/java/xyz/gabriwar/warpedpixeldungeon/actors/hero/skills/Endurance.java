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


import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Warrior: blows taken while the hero holds his ground build up until he digs in and stomps.
 * The floor cracks in a ring, rooting the enemies in it, and a barrier rises around him.
 * Fully trained, the cracked ground leaves them crippled.
 */
public class Endurance extends PassiveSkillA1 {

	{
		name = "Endurance";
		image = 1;
		tier = 1;
	}

	private static final float ROOT_TURNS = 3f;
	private static final float CRIPPLE_TURNS = 6f;
	private static final String STACKS = "ENDURANCE_STACKS";

	//blows taken while holding ground, towards the next stomp
	private int stacks = 0;

	//earlier versions raised HTBoost here; saves that already got it simply keep it
	@Override
	protected boolean upgrade(){
		return true;
	}

	private int needed(){
		return 6 - level;
	}

	//a blow from an enemy while the hero stood his ground builds a stack; moving breaks the stance
	@Override
	public void onDamageTaken( int hpLost, int shieldLost, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || !(source instanceof Char) || source == hero) return;
		if (SkillInteractions.heroMovedLastTurn()){
			stacks = 0;
			return;
		}
		stacks++;
		if (stacks < needed()){
			if (hero.sprite != null){
				hero.sprite.showStatus( CharSprite.NEUTRAL, stacks + "/" + needed() );
				hero.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 2 );
			}
			return;
		}
		stacks = 0;
		stomp( hero );
	}

	private void stomp( Hero hero ){
		int shield = SkillInteractions.ofHealth( hero.HT, 0.01f + 0.03f * level );
		Buff.affect( hero, Barrier.class ).incShield( shield );
		for (int c : SkillInteractions.area( hero.pos, level )){
			if (c == hero.pos) continue;
			Char ch = Actor.findChar( c );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY
					&& !ch.properties().contains( Char.Property.BOSS )){
				Buff.prolong( ch, Roots.class, ROOT_TURNS );
				//+3: the cracked ground leaves them limping long after the roots let go
				if (level >= MAX_LEVEL) Buff.prolong( ch, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple.class, CRIPPLE_TURNS );
			}
		}
		SkillFX.land( hero.pos );
		Sample.INSTANCE.play( Assets.Sounds.ROCKS, 1f, 0.8f );
		Camera.main.shake( 3, 0.3f );
		//the cracks run out from under his boots one ring at a time
		StaggerFX.ring( hero.pos, level, 0.1f, ( c, r ) -> {
			CellEmitter.get( c ).burst( Speck.factory( Speck.ROCK ), 2 );
			CellEmitter.bottom( c ).burst( EarthParticle.FACTORY, 1 );
			Char caught = Actor.findChar( c );
			if (caught != null && caught.alignment == Char.Alignment.ENEMY && caught.sprite != null) caught.sprite.flash();
		} );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 6 );
			hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( shield ), FloatingText.SHIELDING );
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
	}

	//saves from before the rework also carry ENDURANCE_HEART_READY; nothing reads it any more
	@Override
	public void restoreInBundle( Bundle bundle ){
		super.restoreInBundle( bundle );
		stacks = bundle.getInt( STACKS );
	}
}
