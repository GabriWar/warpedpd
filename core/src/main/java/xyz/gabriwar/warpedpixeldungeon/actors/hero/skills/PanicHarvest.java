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
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowPresenter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.PathFinder;

public class PanicHarvest extends Skill {

	{
		tag = "D4A";
		name = "Panic Harvest";
		image = 117;
		tier = 4;
	}

	/** the turn the last harvest was taken; not bundled, it only gates within a turn */
	private float lastHarvest = -1f;

	/** Motes of fear peeled out of the victim into the mana flying home. */
	public static final int PEEL = 4;

	@Override
	protected boolean upgrade(){ return true; }

	//a hit on a panicked enemy reaps its fear as mana, once per turn
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || enemy == null || hero == null || !isPanicked( enemy ) || lastHarvest == Actor.now()) return damage;
		lastHarvest = Actor.now();

		int gain = Math.min( level, 2 );
		hero.MP = Math.min( hero.MT, hero.MP + gain );
		//the fear harvested rises out of it as wisps of soul
		CellEmitter.get( enemy.pos ).burst( SkillBursts.SOUL_WISP, 5 );
		SpatialSound.play( Assets.Sounds.GHOST, enemy, 0.5f, 1.4f );
		if (hero.sprite != null){
			//the fear is seen peeling out of the victim and flying home into the rogue as a mote of
			//mana that blooms on him (a pickup coming home, never a bolt striking him)
			if (hero.sprite.parent != null && enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]){
				SkillBolt.fly( hero.sprite.parent, enemy.sprite.center(), hero.sprite.center(), SkillBolt.MANA, () -> {
							hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 );
							hero.sprite.showStatusWithIcon( BlowPresenter.MANA_COLOR, Integer.toString( gain ), xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.MANA );
							SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.4f, 1.7f );
						} ).peel( enemy.sprite, PEEL );
			} else {
				hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
				hero.sprite.showStatusWithIcon( BlowPresenter.MANA_COLOR, Integer.toString( gain ), xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.MANA );
			}
		}
		return damage;
	}

	//at mastery a panicked enemy that dies spreads its terror to everyone next to it
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null || !isPanicked( mob )) return;
		boolean spread = false;
		int spreadTo = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( mob.pos + n );
			if (ch instanceof Mob && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY){
				Buff.affect( ch, Terror.class, 3f ).object = hero.id();
				CellEmitter.get( ch.pos ).burst( SkillBursts.DREAD, 4 );
				//its fear crawls over the floor into each neighbour, one after another
				ShoutFX.creep( mob.pos, ch.pos, 0.05f * spreadTo );
				if (ch.sprite != null) ShoutFX.dread( ch.sprite, true, 0.05f * spreadTo + 0.15f );
				spreadTo++;
				spread = true;
			}
		}
		if (!spread) return;
		CellEmitter.get( mob.pos ).burst( SkillBursts.DREAD, 3 );
		if (mob.sprite != null) PixelFlare.show( SkillVfx.cx( mob.pos ), SkillVfx.cy( mob.pos ), ShoutFX.DREAD[2], PixelFlare.M, 0.5f );
		SpatialSound.play( Assets.Sounds.GHOST, mob, 1f, 0.6f );
	}

	private static boolean isPanicked( Char enemy ){
		if (enemy instanceof Mob && ((Mob) enemy).state == ((Mob) enemy).FLEEING) return true;
		return enemy.buff( Terror.class ) != null
				|| enemy.buff( Amok.class ) != null
				|| enemy.buff( Charm.class ) != null;
	}
}
