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
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Sniper: now and then a ranged hit cracks the skull. The target takes half again
 * the damage and is knocked off its feet for a turn. Fully trained, the crack rings
 * out and the enemies standing next to it stagger about.
 */
public class HeadShot extends SubSkill2 {

	private static final float DAMAGE = 1.5f;
	private static final float KNOCKDOWN = 1f;
	private static final float STAGGER = 3f;

	{
		name = "Head Shot";
		castText = "Head shot!";
		image = 195;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}


	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || level <= 0 || enemy == null || !enemy.isAlive() || Random.Int( 100 ) >= 5 + 5 * level)
			return damage;

		castTextYell();
		final boolean boss = Char.hasProp( enemy, Char.Property.BOSS ) || Char.hasProp( enemy, Char.Property.MINIBOSS );
		if (enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]){
			Wound.hit( enemy );
			enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			if (!boss) enemy.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( HeadShot.class, "down" ) );
			Camera.main.shake( 1.5f, 0.2f );
		}
		SpatialSound.play( Assets.Sounds.HIT_STRONG, enemy, 1f, 0.8f );

		//the knockdown lands after the blow itself, so the blow cannot shake it off at once
		Actor.add( new Actor(){
			{
				actPriority = VFX_PRIO;
			}
			@Override
			protected boolean act(){
				Actor.remove( this );
				if (!enemy.isAlive()) return true;
				if (!boss) Buff.prolong( enemy, Paralysis.class, KNOCKDOWN );
				if (level >= MAX_LEVEL) ringOut( enemy );
				return true;
			}
		} );
		return Math.round( damage * DAMAGE );
	}

	private static void ringOut( Char struck ){
		for (int n : PathFinder.NEIGHBOURS8){
			int c = struck.pos + n;
			if (!SkillInteractions.valid( c )) continue;
			Char ch = Actor.findChar( c );
			if (ch == null || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			Buff.prolong( ch, Vertigo.class, STAGGER );
			if (ch.sprite != null && Dungeon.level.heroFOV[c]) ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
		}
	}
}
