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


import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A moving target is hard to hit: a melee blow that comes while the hero is still on the
 * move (he moved on his last turn) may miss entirely as he rolls one tile away from the
 * attacker. At mastery the roll goes over the attacker instead, landing behind it.
 */
public class Parkour extends SubSkill2 {

	{
		name = "Parkour";
		image = 165;
		tier = 2;
	}

	//game time of the hero's last step; not saved, a fresh load simply starts standing still
	private static float movedAt = -10f;

	/** called from SkillInteractions.onMove whenever the hero changes tiles */
	public static void heroMoved(){ movedAt = Actor.now(); }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** 15/25/35% of the blows taken on the move */
	private float chance(){ return 0.05f + 0.10f * level; }

	//a real dodge: the swing misses outright, no on-hit effects land
	@Override
	public boolean dodgeChance( Char attacker ){
		Hero hero = Dungeon.hero;
		return level > 0 && hero != null && attacker != null && attacker != hero && !hero.rooted
				&& Dungeon.level.adjacent( hero.pos, attacker.pos )
				&& SkillInteractions.heroMovedLastTurn()
				&& Random.Float() < chance();
	}

	//the roll itself waits until the enemy's swing is over
	@Override
	public void onDodge( Char attacker ){
		Hero hero = Dungeon.hero;
		if (hero == null || attacker == null) return;
		SkillInteractions.defer( () -> roll( hero, attacker ) );
	}

	private void roll( Hero hero, Char enemy ){
		if (!hero.isAlive() || hero.rooted) return;
		int land = -1;
		boolean vault = false;
		if (level >= MAX_LEVEL && enemy.isAlive()){
			int over = enemy.pos + (enemy.pos - hero.pos);
			if (safe( hero, over ) && Dungeon.level.adjacent( enemy.pos, over )){ land = over; vault = true; }
		}
		if (land < 0){
			int away = hero.pos + (hero.pos - enemy.pos);
			if (safe( hero, away ) && Dungeon.level.adjacent( hero.pos, away )) land = away;
		}
		if (land < 0){
			if (hero.sprite != null) hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "roll" ) );
			return;
		}

		final int from = hero.pos;
		hero.move( land, false );
		Dungeon.observe();
		GameScene.updateFog();
		if (hero.sprite != null && hero.sprite.parent != null){
			hero.sprite.jump( from, land, 8f, 0.2f, () -> hero.sprite.place( hero.pos ) );
			hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, vault ? "vault" : "roll" ) );
		}
		CellEmitter.bottom( from ).burst( Speck.factory( Speck.DUST ), 8 );
		SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 0.8f );
	}

	private static boolean safe( Hero hero, int cell ){
		return SkillInteractions.valid( cell ) && Dungeon.level.passable[cell] && !Dungeon.level.solid[cell]
				&& !Dungeon.level.pit[cell] && !Dungeon.level.avoid[cell] && Actor.findChar( cell ) == null
				&& (!Char.hasProp( hero, Char.Property.LARGE ) || Dungeon.level.openSpace[cell]);
	}
}
