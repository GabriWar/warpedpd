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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;


import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff;

public class Bombvoyage extends ActiveSkill3 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Bombvoyage";
		castText = "Bombvoyage";
		image = 91;
		tier = 3;
		mana = 15;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			Sample.INSTANCE.play( Assets.Sounds.PUFF, 1f, 0.8f );
			hero.sprite.emitter().burst( Speck.factory( Speck.SMOKE ), 5 );
			hero.heroSkills.active1.active = false; // Disable Aimed Shot
			hero.heroSkills.active2.active = false; // Disable Double Shot
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Aimed.class, false );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Double.class, false );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Fuse.class, true );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			//the fuse pinched out
			Sample.INSTANCE.play( Assets.Sounds.PUFF, 0.6f, 1.4f );
			if (hero.sprite != null) hero.sprite.emitter().burst( SmokeParticle.FACTORY, 3 );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Fuse.class, false );
		}
	}

	@Override
	public boolean arrowToBomb(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return false;
		else {
			castTextYell();
			Dungeon.hero.MP -= getManaCost();
			//the charge lit as it leaves her hand
			if (Dungeon.hero.sprite != null) Dungeon.hero.sprite.emitter().burst( SmokeParticle.FACTORY, 3 );
			Sample.INSTANCE.play( Assets.Sounds.BURNING, 0.5f, 1.6f );
			return true;
		}
	}

	@Override
	public boolean rangedSource(){ return true; }

	/** Hero.attackProc calls this when a projectile carrying the charge lands on enemyPos */
	public static void blast( Hero hero, int enemyPos ){
		Bombvoyage skill = hero.heroSkills.get( Bombvoyage.class );
		int rank = skill != null ? Math.max( 1, skill.level ) : 1;
		burst( hero, enemyPos, rank >= 2 ? 1.35f : 1f, rank >= MAX_LEVEL, skill );
	}

	private static void burst( Hero hero, int c0, float mult, boolean chain, Object source ){
		Sample.INSTANCE.play( Assets.Sounds.BLAST, mult < 1f ? 0.7f : 1f, mult < 1f ? 1.3f : 1f );
		if (Dungeon.level.heroFOV[c0]){
			xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.center( c0 ).burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle.FACTORY, 30 );
			com.watabou.noosa.Camera.main.shake( 2, 0.3f );
			if (hero.sprite != null && hero.sprite.parent != null){
				new Flare( 6, 20 * mult ).color( 0xFFAA44, true ).show( hero.sprite.parent,
						xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld( c0 ), 0.4f );
			}
		}
		int killedAt = -1;
		int ring = 0;
		for (int n : PathFinder.NEIGHBOURS9){
			int c = c0 + n;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			//the smoke rolls out from the centre, one tile after another
			if (Dungeon.level.heroFOV[c]) xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( c ).startDelayed( SmokeParticle.FACTORY, 0, 3, n == 0 ? 0f : 0.06f + 0.03f * ring++ );
			Char ch = Actor.findChar( c );
			if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) continue;
			int dmg = Math.round( com.watabou.utils.Random.NormalIntRange( 5 + Dungeon.scalingDepth(), 10 + Dungeon.scalingDepth() * 2 ) * mult );
			dmg -= ch.drRoll();
			if (dmg > 0) ch.damage( dmg, source != null ? source : xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb.class );
			if (!ch.isAlive() && killedAt < 0) killedAt = c;
		}
		//+3: a blast that kills sets off a smaller second one where that enemy stood, once per shot
		if (chain && killedAt >= 0){
			final int at = killedAt;
			SkillInteractions.defer( () -> burst( hero, at, mult * 0.5f, false, source ) );
		}
	}

	@Override
	public int getManaCost(){
		return mana - level * 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
