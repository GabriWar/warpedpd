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
package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LockedFloor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.ClimateCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.keys.WornKey;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ConeAOE;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DemonLordSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//boss of the frozen branch (Unleashed PD port, reworked into a proper set-piece).
//Like Goo it telegraphs its big move: it draws in a breath for a turn, the cone it
//will cover frosts over, and then it exhales. Below half health it calls two ice
//demons out of the floor and breathes far more often.
public class DemonLord extends Mob {

	{
		spriteClass = DemonLordSprite.class;

		HP = HT = 300;
		defenseSkill = 35;

		EXP = 30;
		maxLvl = 34;

		properties.add( Property.BOSS );
		properties.add( Property.DEMONIC );
		//at home in the ice: unmoved by cold, and hard to burn
		properties.add( Property.ICY );

		//built for the cold: the deep freeze is home, the thaw is what hurts
		thermal = Thermal.COLD_DWELLER;

		resistances.add( Burning.class );

		immunities.add( Sleep.class );
		immunities.add( Terror.class );
		immunities.add( Vertigo.class );
	}

	private static final int BREATH_RANGE = 4;
	private static final float BREATH_ARC = 70f;
	//turns of warning between drawing in the breath and letting it go
	private static final int BREATH_CHARGE = 3;

	//the cell the breath is aimed at while it is being drawn in, -1 when not
	private int breathCell = -1;
	private int breathCooldown = 3;
	//turns left before the drawn-in breath is released
	private int breathTurns = 0;

	private boolean enraged(){
		return HP * 2 <= HT;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 18, 36 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 45;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 16 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (enemy instanceof Hero && Random.Int( 3 ) == 0) {
			Buff.prolong( enemy, Chill.class, 4f );
		}

		return damage;
	}

	@Override
	public boolean act() {
		if (state != SLEEPING){
			Dungeon.level.seal();
		}

		if (paralysed <= 0) {
			if (breathCell != -1) {
				if (--breathTurns > 0) {
					holdBreath();
				} else {
					breathe();
				}
				return true;
			}
			if (breathCooldown > 0) {
				breathCooldown--;
			} else if (state == HUNTING && enemy != null && enemy.isAlive()
					&& fieldOfView != null && enemy.pos < fieldOfView.length && fieldOfView[enemy.pos]
					&& Dungeon.level.distance( pos, enemy.pos ) <= BREATH_RANGE
					&& Random.Int( enraged() ? 2 : 3 ) == 0) {
				inhale( enemy.pos );
				return true;
			}
		}

		return super.act();
	}

	private ConeAOE cone() {
		Ballistica aim = new Ballistica( pos, breathCell, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID );
		return new ConeAOE( aim, BREATH_RANGE, BREATH_ARC,
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET | Ballistica.IGNORE_SOFT_SOLID );
	}

	//three turns of warning: the whole cone is marked and frosts over, so there is time to get out
	private void inhale( int target ) {
		breathCell = target;
		breathTurns = BREATH_CHARGE;
		if (sprite instanceof DemonLordSprite) {
			((DemonLordSprite) sprite).inhale( cone().cells, breathTurns );
		}
		if (Dungeon.level.heroFOV[pos]) {
			sprite.showStatus( CharSprite.WARNING, Messages.get( this, "!!!" ) );
			GLog.n( Messages.get( this, "inhale" ) );
		}
		spend( TICK );
	}

	//still drawing in: the markers stay up and the count runs down over his head
	private void holdBreath() {
		if (sprite instanceof DemonLordSprite) {
			((DemonLordSprite) sprite).holdBreath( cone().cells, breathTurns );
		}
		if (Dungeon.level.heroFOV[pos]) {
			sprite.showStatus( CharSprite.WARNING, Integer.toString( breathTurns ) );
		}
		spend( TICK );
	}

	private void breathe() {
		ConeAOE cone = cone();
		breathCell = -1;
		breathCooldown = enraged() ? 3 : 5;

		if (sprite instanceof DemonLordSprite) {
			((DemonLordSprite) sprite).breathe( cone.cells );
		}
		SpatialSound.play( Assets.Sounds.BLAST, pos, 1f, 0.6f );
		SpatialSound.play( Assets.Sounds.SHATTER, pos, 1f, 0.7f );
		if (Dungeon.level.heroFOV[pos]) {
			PixelScene.shake( 5, 0.6f );
			GameScene.flash( 0x3A6EA8 );
		}

		for (int cell : cone.cells) {
			//standing water freezes where the breath passes
			if (Dungeon.level.map[cell] == Terrain.WATER) {
				Level.set( cell, Terrain.FROZEN_WATER );
				GameScene.updateMap( cell );
			}
			Char ch = Actor.findChar( cell );
			if (ch == null || ch == this || ch.alignment == alignment) continue;
			ch.damage( Random.NormalIntRange( 20, 40 ), this );
			if (ch.isAlive()) {
				Buff.prolong( ch, Chill.class, enraged() ? 8f : 5f );
			} else if (ch == Dungeon.hero) {
				Dungeon.fail( this );
				GLog.n( Messages.get( this, "breath_kill" ) );
			}
		}

		spend( attackDelay() );
	}

	//below half health: two ice demons claw their way out of the floor beside him
	private void enrage() {
		BossHealthBar.bleed( true );
		sprite.showStatus( CharSprite.WARNING, Messages.get( this, "enraged" ) );
		yell( Messages.get( this, "rage" ) );
		SpatialSound.play( Assets.Sounds.CHALLENGE, pos );
		breathCooldown = 0;

		int summoned = 0;
		for (int n : PathFinder.NEIGHBOURS8) {
			if (summoned >= 2) break;
			int cell = pos + n;
			if (!Dungeon.level.passable[cell] || Actor.findChar( cell ) != null) continue;
			IceDemon demon = new IceDemon();
			demon.pos = cell;
			demon.state = demon.HUNTING;
			GameScene.add( demon );
			CellEmitter.get( cell ).burst( SnowParticle.FACTORY, 12 );
			summoned++;
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
		}
		boolean wasEnraged = enraged();
		super.damage( dmg, src );
		if (isAlive() && enraged() && !wasEnraged) {
			enrage();
		}
		LockedFloor lock = Dungeon.hero.buff( LockedFloor.class );
		if (lock != null && src != null && !isImmune( src.getClass() ) && !isInvulnerable( src.getClass() )) {
			lock.addTime( dmg * 1.5f );
		}
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );

		Dungeon.level.unseal();
		GameScene.bossSlain();

		//his demons do not outlive him
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
			if (mob instanceof IceDemon && mob.isAlive()) {
				mob.die( this );
			}
		}

		//the worn key is what the locked way out of the arena answers to
		Dungeon.level.drop( new WornKey( Dungeon.depth ), pos ).sprite.drop();
		//the crystal at the heart of his cold
		Dungeon.level.drop( new ClimateCrystal(), pos ).sprite.drop();

		yell( Messages.get( this, "die" ) );
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
			yell( Messages.get( this, "notice" ) );
			for (Char ch : Actor.chars()) {
				if (ch instanceof DriedRose.GhostHero) {
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
		}
	}

	private static final String BREATH_CELL = "breath_cell";
	private static final String BREATH_COOLDOWN = "breath_cooldown";
	private static final String BREATH_TURNS = "breath_turns";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( BREATH_CELL, breathCell );
		bundle.put( BREATH_COOLDOWN, breathCooldown );
		bundle.put( BREATH_TURNS, breathTurns );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		breathCell = bundle.contains( BREATH_CELL ) ? bundle.getInt( BREATH_CELL ) : -1;
		breathCooldown = bundle.getInt( BREATH_COOLDOWN );
		breathTurns = bundle.getInt( BREATH_TURNS );
		if (breathCell != -1 && breathTurns <= 0) breathTurns = 1;
		if (state != SLEEPING) BossHealthBar.assignBoss( this );
		if (enraged()) BossHealthBar.bleed( true );
	}
}
