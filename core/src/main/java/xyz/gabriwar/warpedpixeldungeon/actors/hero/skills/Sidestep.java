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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkillDecoy;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Sidestep extends Skill {

	{
		tag = "D1";
		name = "Sidestep";
		image = 145;
		tier = 1;
	}

	private static final float CIRCLED_TURNS = 2f;
	private static final float OPENING = 1.3f;

	@Override
	protected boolean upgrade(){
		return true;
	}

	//stepping from one tile beside an enemy to another beside it leaves it swinging at air.
	//Saves from before this rework carry a SIDESTEP_OPENING key, which is simply never read
	@Override
	public void onCharMoved( xyz.gabriwar.warpedpixeldungeon.actors.Char ch, int from, boolean travelling ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || ch != hero || !travelling) return;
		boolean any = false;
		for (int n : PathFinder.NEIGHBOURS8){
			xyz.gabriwar.warpedpixeldungeon.actors.Char enemy = Actor.findChar( hero.pos + n );
			if (enemy == null || enemy.alignment != xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY || !enemy.isAlive()
					|| !Dungeon.level.adjacent( from, enemy.pos )) continue;
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.prolong( enemy, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze.class, level );
			if (level >= MAX_LEVEL) xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.prolong( enemy, Circled.class, CIRCLED_TURNS );
			if (enemy.sprite != null) enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 2 );
			any = true;
		}
		if (any){
			CellEmitter.bottom( from ).burst( Speck.factory( Speck.DUST ), 4 );
			if (hero.sprite != null) hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "cast" ) );
			Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 1.5f );
		}
	}

	//+3: your next hit on an enemy you circled can't miss and lands harder
	@Override
	public boolean sureHit( xyz.gabriwar.warpedpixeldungeon.actors.Char target ){
		return level >= MAX_LEVEL && target != null && target.buff( Circled.class ) != null;
	}

	@Override
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){
		if (level < MAX_LEVEL || enemy == null) return damage;
		Circled circled = enemy.buff( Circled.class );
		if (circled == null) return damage;
		circled.detach();
		xyz.gabriwar.warpedpixeldungeon.effects.Wound.hit( enemy );
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 1.3f );
		return Math.round( damage * OPENING );
	}

	/** the Duelist slipped around this enemy: its guard is still turned the wrong way */
	public static class Circled extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff {
		{
			type = buffType.NEGATIVE;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
	}
}
