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
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A melee kill that leaves no enemy beside you launches you in an arc to land beside
 * the nearest enemy in sight. At mastery the next hit on that enemy is a plunging strike.
 */
public class MomentumMaster extends SubSkill3 {

	public static final float PLUNGE = 1.5f;

	{
		name = "Momentum Master";
		image = 191;
		tier = 3;
	}

	//the enemy the last leap landed beside; the next hit on it plunges (mastery only)
	private int plungeTarget = -1;

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
	public void onEnemyDeath( Mob mob, Object cause ){
		if (mob != null && mob.id() == plungeTarget) plungeTarget = -1;
	}

	/** 2/3/4 tiles of leap */
	public int leapRange(){ return 1 + level; }

	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || ranged || hero == null || !hero.isAlive() || hero.rooted
				|| hero.sprite == null || hero.sprite.parent == null) return;

		Char next = null;
		int land = -1;
		int best = Integer.MAX_VALUE;
		for (Mob other : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (other == mob || !other.isAlive() || other.alignment != Char.Alignment.ENEMY
					|| !Dungeon.level.heroFOV[other.pos]) continue;
			int d = Dungeon.level.distance( hero.pos, other.pos );
			//still in the thick of it: no leap
			if (d <= 1) return;
			if (d > leapRange() + 1 || d >= best) continue;
			int cell = landing( hero, other );
			if (cell < 0 || Dungeon.level.distance( hero.pos, cell ) > leapRange()) continue;
			next = other;
			land = cell;
			best = d;
		}
		if (next == null) return;

		final int from = hero.pos;
		final int dest = land;
		hero.move( dest, false );
		Dungeon.observe();
		GameScene.updateFog();
		CellEmitter.bottom( from ).burst( Speck.factory( Speck.DUST ), 6 );
		//the hero's own leap: heard on him
		SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 1.4f );
		hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "leap" ) );
		hero.sprite.jump( from, dest, () -> {
			hero.sprite.place( hero.pos );
			CellEmitter.bottom( hero.pos ).burst( Speck.factory( Speck.DUST ), 6 );
			SpatialSound.play( Assets.Sounds.TRAMPLE, hero, 1f, 1.1f );
		} );
		if (level >= MAX_LEVEL) plungeTarget = next.id();
	}

	/** the tile in front of the enemy along a clear flight line, or -1 */
	private static int landing( Hero hero, Char enemy ){
		Ballistica line = new Ballistica( hero.pos, enemy.pos, Ballistica.PROJECTILE );
		if (line.collisionPos != enemy.pos || line.dist < 2) return -1;
		int cell = line.path.get( line.dist - 1 );
		if (!Dungeon.level.passable[cell] || Dungeon.level.pit[cell] || Dungeon.level.avoid[cell]
				|| Actor.findChar( cell ) != null
				|| (Char.hasProp( hero, Char.Property.LARGE ) && !Dungeon.level.openSpace[cell])) return -1;
		return cell;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (plungeTarget < 0 || enemy == null || enemy.id() != plungeTarget) return damage;
		plungeTarget = -1;
		if (ranged || level < MAX_LEVEL) return damage;
		Surprise.hit( enemy );
		SpatialSound.play( Assets.Sounds.HIT_STRONG, enemy, 1f, 1.2f );
		Camera.main.shake( 2, 0.2f );
		return Math.round( damage * PLUNGE );
	}
}
