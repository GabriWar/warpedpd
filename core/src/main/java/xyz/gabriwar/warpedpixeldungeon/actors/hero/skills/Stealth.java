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
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkillDecoy;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Stealth extends PassiveSkillA2 {

	{
		name = "Stealth";
		image = 50;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//quiet steps: every level of the skill is a level of stealth
	@Override
	public int stealthBonus(){
		return level;
	}

	//an enemy that notices you anyway sees only a smoky double where you stood, and hunts that first
	@Override
	public void onHeroNoticed( Mob mob, boolean wasSleeping ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || mob == null || !mob.isAlive() || mob.alignment != Char.Alignment.ENEMY
				|| mob.properties().contains( Char.Property.BOSS )
				|| mob.buff( Seen.class ) != null) return;
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( mob, Seen.class );

		int cell = decoyCell( hero );
		if (cell == -1) return;
		SkillDecoy decoy = new SkillDecoy();
		decoy.pos = cell;
		decoy.rank = level;
		decoy.left = 1 + level;
		decoy.blinding = level >= Skill.MAX_LEVEL;
		GameScene.add( decoy );
		if (decoy.sprite != null) decoy.sprite.alpha( 0.55f );
		CellEmitter.get( cell ).burst( Speck.factory( Speck.SMOKE ), 8 );
		if (hero.sprite != null) hero.sprite.emitter().burst( ShadowParticle.UP, 6 );
		Sample.INSTANCE.play( Assets.Sounds.MELD, 1f, 1.3f );
		mob.aggro( decoy );
	}

	private static int decoyCell( Hero hero ){
		int start = Random.Int( PathFinder.NEIGHBOURS8.length );
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
			int c = hero.pos + PathFinder.NEIGHBOURS8[(start + i) % PathFinder.NEIGHBOURS8.length];
			if (SkillInteractions.valid( c ) && Dungeon.level.passable[c] && !Dungeon.level.pit[c]
					&& Actor.findChar( c ) == null){
				return c;
			}
		}
		return -1;
	}

	/** this enemy has already been fooled by a double once */
	public static class Seen extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff {
		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
	}
}
