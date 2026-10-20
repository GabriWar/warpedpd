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


import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff;

public class EmberArrows extends ActiveSkill {

	{
		tag = "A5B";
		name = "Ember Arrows";
		castText = "Burn.";
		image = 128;
		tier = 4;
		mana = 2;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			SpatialSound.play( Assets.Sounds.BURNING, hero, 1f, 1.3f );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
			//the pitch catches: embers lift off her (never flames on the hero, a Burning)
			hero.sprite.emitter().burst( SkillBursts.EMBER_MOTE, 6 );
			kindled( hero.sprite, HEAT, FLARE );
			//mutually exclusive with its fork partner
			for (Skill s : hero.heroSkills.toggleGroup()){
				if (s instanceof FrostArrows) s.active = false;
			}
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Frost.class, false );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Ember.class, true );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			//the pitch pinched out
			SpatialSound.play( Assets.Sounds.PUFF, hero, 0.6f, 1.3f );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.SMOKE ), 3 );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Ember.class, false );
		}
	}

	@Override
	public int getManaCost(){
		//one mana per arrow, two once the pitch burns hot enough
		return level >= 3 ? 2 : 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	private static void ignite( Char ch ){
		Buff.affect( ch, Burning.class ).reignite( ch );
		flames( ch );
	}

	/** the catch: a burst of flame, then embers spiralling off the arrow for a beat longer */
	private static void flames( Char ch ){
		if (!Dungeon.level.heroFOV[ch.pos] || ch.sprite == null) return;
		ch.sprite.centerEmitter().burst( FlameParticle.FACTORY, 4 );
		ch.sprite.emitter().start( FlameParticle.FACTORY, 0.05f, 6 );
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || !active || level == 0 || enemy == null
				|| Dungeon.hero.MP < getManaCost())
			return damage;

		Dungeon.hero.MP -= getManaCost();
		ignite( enemy );

		//+2: the flame leaps to one enemy beside the target that is not yet burning
		if (level >= 2){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( enemy.pos + n );
				if (ch != null && ch != Dungeon.hero && ch.alignment == Char.Alignment.ENEMY
						&& ch.isAlive() && ch.buff( Burning.class ) == null){
					Buff.affect( ch, Burning.class ).reignite( ch );
					//the leap is seen: a cinder hops across and catches on arrival (never a firebolt)
					if (enemy.sprite != null && enemy.sprite.parent != null && ch.sprite != null && Dungeon.level.heroFOV[ch.pos]){
						final Char next = ch;
						SkillBolt.fly( enemy.sprite.parent, enemy.sprite.center(), ch.sprite.center(), SkillBolt.EMBER, () -> {
									flames( next );
									SpatialSound.play( Assets.Sounds.BURNING, next, 0.5f, 1.6f );
								} );
					} else {
						flames( ch );
					}
					break;
				}
			}
		}

		SpatialSound.play( Assets.Sounds.BURNING, enemy, 0.6f, 1.3f );
		return damage;
	}

	//+3: a burning enemy your shot kills bursts, setting the enemies around it alight
	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (!ranged || !active || level < 3 || mob.buff( Burning.class ) == null) return;
		//the body goes up: a flash at the centre, then a ring of heat rolling outward and embers
		//thrown off tile by tile (fire only where it truly catches: the burning bodies)
		if (Dungeon.level.heroFOV[mob.pos]){
			CellEmitter.center( mob.pos ).burst( FlameParticle.FACTORY, 10 );
			Fx.shake( 1, 0.2f );
			heatRing( mob.pos );
		}
		int ring = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = mob.pos + n;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get( c ).startDelayed( SkillBursts.EMBER_MOTE, 0, 5, 0.05f + 0.03f * ring++ );
			Char ch = Actor.findChar( c );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				Buff.affect( ch, Burning.class ).reignite( ch );
			}
		}
		SpatialSound.play( Assets.Sounds.BURNING, mob, 1f, 0.9f );
	}

	/** The +3 burst's heat rolling out over the floor from a cell: a flare where the body goes up,
	 *  a pool of its heat (fire's light), and two rings, the fire's orange spreading 13 to 41 px and
	 *  its pale gold 9 to 31 px a beat after. Any thread. */
	public static void heatRing( final int cell ){
		Fx.post( () -> {
			if (!SkillVfx.valid( cell ) || !SkillVfx.seen( cell )) return;
			float x = SkillVfx.cx( cell ), y = SkillVfx.cy( cell ) + 4;
			PixelFlare.show( x, y - 6, FLARE, PixelFlare.M, 0.35f );
			FxLight.pool( x, y, 47, SkillPalette.FIRE_LIGHT, 0.6f ).life( 0, 0.05f, 0.45f );
			Group floor = GameScene.layer( GameScene.Layer.FLOOR );
			FxRing.ground( floor, x, y, 13, 41, 0.45f, HEAT, 0.75f, true );
			FxRing r = FxRing.ground( floor, x, y, 9, 31, 0.4f, FLARE, 0.5f, true );
			if (r != null) r.delay( 0.08f );
		} );
	}

	/** An arrow stance taken up (Ember or Frost Arrows): a ring of its colour spreading at her
	 *  feet and a small flare of it at her hands. Any thread. */
	public static void kindled( final CharSprite s, final int ring, final int flare ){
		PixelFlare.show( SkillVfx.midX( s ), SkillVfx.midY( s ) + 1, flare, PixelFlare.S, 0.3f );
		Fx.post( () -> {
			if (s.parent == null) return;
			FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), SkillVfx.midX( s ), s.y + s.height() - 3,
					9, 23, 0.35f, ring, 0.6f, true );
		} );
	}

	/** The skill's fire on the ground (SkillPalette.FIRE's body, orange, never peach) and its flares
	 *  (FIRE's pale gold). */
	public static final int HEAT = SkillPalette.FIRE.stop( 1 ), FLARE = SkillPalette.FIRE.stop( 0 );
}
