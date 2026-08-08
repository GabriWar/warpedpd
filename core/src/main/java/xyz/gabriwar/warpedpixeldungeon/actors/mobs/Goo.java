/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LockedFloor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.keys.WornKey;
import xyz.gabriwar.warpedpixeldungeon.items.quest.GooBlob;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Chainsaw;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GooSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Goo extends Mob {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 120 : 100;
		EXP = 10;
		defenseSkill = 8;
		spriteClass = GooSprite.class;

		properties.add(Property.BOSS);
		properties.add(Property.DEMONIC);
		properties.add(Property.ACIDIC);

		declareExtraLoot(WornKey.class, 1f);
		declareExtraLoot(GooBlob.class, 1f);
		//Sprouted parity: Goo guaranteed a self-enchanting Chainsaw + the Sokoban1 journal page
		declareExtraLoot(Chainsaw.class, 1f);
		declareExtraLoot(xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban1.class, 1f);

		//the earliest and most reliable ActiveMrDestructo; the other two sources are postgame
		loot = new ActiveMrDestructo();
		lootChance = 0.5f;
	}

	private int pumpedUp = 0;
	private int healInc = 1;

	@Override
	public int damageRoll() {
		int min = 1;
		int max = (HP*2 <= HT) ? 12 : 8;
		if (pumpedUp > 0) {
			pumpedUp = 0;
			if (enemy == Dungeon.hero) {
				Statistics.qualifiedForBossChallengeBadge = false;
				Statistics.bossScores[0] -= 100;
			}
			return Random.NormalIntRange( min*3, max*3 );
		} else {
			return Random.NormalIntRange( min, max );
		}
	}

	@Override
	public int attackSkill( Char target ) {
		int attack = 10;
		if (HP*2 <= HT) attack = 15;
		if (pumpedUp > 0) attack *= 2;
		return attack;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return (int)(super.defenseSkill(enemy) * ((HP*2 <= HT)? 1.5 : 1));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}

	@Override
	public boolean act() {

		if (state != HUNTING && pumpedUp > 0){
			pumpedUp = 0;
			sprite.idle();
		}

		if (!flying && Dungeon.level.water[pos] && HP < HT) {
			HP += healInc;
			Statistics.qualifiedForBossChallengeBadge = false;

			LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
			if (lock != null){
				if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.removeTime(healInc);
				else                                                    lock.removeTime(healInc*1.5f);
			}

			if (Dungeon.level.heroFOV[pos] ){
				sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healInc), FloatingText.HEALING );
			}
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) && healInc < 3) {
				healInc++;
			}
			if (HP*2 > HT) {
				BossHealthBar.bleed(false);
				((GooSprite)sprite).spray(false);
				HP = Math.min(HP, HT);
			}
		} else {
			healInc = 1;
		}
		
		if (state != SLEEPING){
			Dungeon.level.seal();
		}

		return super.act();
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (pumpedUp > 0){
			//we check both from and to in this case as projectile logic isn't always symmetrical.
			//this helps trim out BS edge-cases
			return Dungeon.level.distance(enemy.pos, pos) <= 2
						&& new Ballistica( pos, enemy.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == enemy.pos
						&& new Ballistica( enemy.pos, pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == pos;
		} else {
			return super.canAttack(enemy);
		}
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 3 ) == 0) {
			Buff.affect( enemy, Ooze.class ).set( Ooze.DURATION );
			enemy.sprite.burst( 0x000000, 5 );
		}

		if (pumpedUp > 0) {
			PixelScene.shake( 3, 0.2f );
		}

		return damage;
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();

		if (pumpedUp > 0){
			((GooSprite)sprite).pumpUp( pumpedUp );
		}
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (pumpedUp == 1) {
			pumpedUp++;
			((GooSprite)sprite).pumpUp( pumpedUp );

			spend( attackDelay() );

			return true;
		} else if (pumpedUp >= 2 || Random.Int( (HP*2 <= HT) ? 2 : 5 ) > 0) {

			boolean visible = Dungeon.level.heroFOV[pos];

			if (visible) {
				if (pumpedUp >= 2) {
					((GooSprite) sprite).pumpAttack();
				} else {
					sprite.attack(enemy.pos);
				}
			} else {
				if (pumpedUp >= 2){
					((GooSprite)sprite).triggerEmitters();
				}
				attack( enemy );
				Invisibility.dispel(this);
				spend( attackDelay() );
			}

			return !visible;

		} else {

			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
				pumpedUp += 2;
				//don't want to overly punish players with slow move or attack speed
				spend(GameMath.gate(attackDelay(), (int)Math.ceil(enemy.cooldown()), 3*attackDelay()));
			} else {
				pumpedUp++;
				spend( attackDelay() );
			}

			((GooSprite)sprite).pumpUp( pumpedUp );

			if (Dungeon.level.heroFOV[pos]) {
				sprite.showStatus( CharSprite.WARNING, Messages.get(this, "!!!") );
				GLog.n( Messages.get(this, "pumpup") );
			}

			return true;
		}
	}

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		boolean result = super.attack( enemy, dmgMulti, dmgBonus, accMulti );
		if (pumpedUp > 0) {
			pumpedUp = 0;
			if (enemy == Dungeon.hero) {
				Statistics.qualifiedForBossChallengeBadge = false;
				Statistics.bossScores[0] -= 100;
			}
		}
		return result;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (pumpedUp != 0) {
			pumpedUp = 0;
			sprite.idle();
		}
		return super.getCloser( target );
	}

	@Override
	protected boolean getFurther(int target) {
		if (pumpedUp != 0) {
			pumpedUp = 0;
			sprite.idle();
		}
		return super.getFurther( target );
	}

	@Override
	public void damage(int dmg, Object src) {
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
		}
		boolean bleeding = (HP*2 <= HT);
		super.damage(dmg, src);
		if ((HP*2 <= HT) && !bleeding){
			BossHealthBar.bleed(true);
			sprite.showStatus(CharSprite.WARNING, Messages.get(this, "enraged"));
			((GooSprite)sprite).spray(true);
			yell(Messages.get(this, "gluuurp"));
		}
		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null && !isImmune(src.getClass()) && !isInvulnerable(src.getClass())){
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.addTime(dmg);
			else                                                    lock.addTime(dmg*1.5f);
		}
	}

	@Override
	public void die( Object cause ) {

		super.die( cause );

		Dungeon.level.unseal();

		GameScene.bossSlain();

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[0] += 1000;

		yell( Messages.get(this, "defeated") );
	}

	@Override
	protected void dropExtraLoot() {
		trackedDrop(new WornKey( Dungeon.depth ), 0);

		//60% chance of 2 blobs, 30% chance of 3, 10% chance for 4. Average of 2.5
		int blobs = Random.chances(new float[]{0, 0, 6, 3, 1});
		boolean firstBlob = true;
		for (int i = 0; i < blobs; i++){
			int ofs;
			do {
				ofs = PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.passable[pos + ofs]);
			if (firstBlob) {
				trackedDrop(new GooBlob(), pos + ofs, 1);
				firstBlob = false;
			} else {
				Dungeon.level.drop( new GooBlob(), pos + ofs ).sprite.drop( pos );
			}
		}

		//Chainsaw self-enchants BuzzSaw on its first hit (Chainsaw.proc), reproducing
		//Sprouted's guaranteed enchanted drop; the Sokoban1 page unlocks its journal room.
		trackedDrop(new Chainsaw(), 2);
		trackedDrop(new xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban1(), 3);
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			Dungeon.level.seal();
			yell(Messages.get(this, "notice"));
			for (Char ch : Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
		}
	}

	private final String PUMPEDUP = "pumpedup";
	private final String HEALINC = "healinc";

	@Override
	public void storeInBundle( Bundle bundle ) {

		super.storeInBundle( bundle );

		bundle.put( PUMPEDUP , pumpedUp );
		bundle.put( HEALINC, healInc );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {

		super.restoreFromBundle( bundle );

		pumpedUp = bundle.getInt( PUMPEDUP );
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if ((HP*2 <= HT)) BossHealthBar.bleed(true);

		healInc = bundle.getInt(HEALINC);
	}
	
}
