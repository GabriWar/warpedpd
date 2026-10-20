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


import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;

public class BloodDance extends Skill {

	{
		tag = "A5B";
		name = "Crimson Waltz";
		castText = "Stay close";
		image = 63;
		tier = 4;
		mana = 12;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){

			Buff.prolong( hero, Adrenaline.class, 4 + 3 * level );
			//at mastery the dance opens behind a blood-red guard
			if (level >= MAX_LEVEL) Buff.affect( hero, Barrier.class ).setShield( SkillInteractions.ofHealth( hero.HT, 0.03f * level ) );

			hero.MP -= getManaCost();
			castTextYell();
			SpatialSound.play( Assets.Sounds.HIT_STAB, hero, 1f, 0.9f );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 6 );
			PixelFlare.show( SkillVfx.midX( hero.sprite ), SkillVfx.midY( hero.sprite ), CRIMSON, PixelFlare.M, 0.6f );
			//the waltz's ribbons: two crimson arcs wound round him in opposite ways (as Pirouette's),
			//their heads blood's own light, never the crimson paled toward white (pink)
			ArcSpinFX.around( hero.sprite, CRIMSON, RIBBON_INNER, 10, 0.5f, 225, RIBBON_SPIN, RIBBON_FOR );
			ArcSpinFX.around( hero.sprite, RIBBON_INNER, RIBBON_INNER, 7, 0.35f, 45, -RIBBON_SPIN, RIBBON_FOR );
			//the first turn of the waltz: a ring of red sweeping once around him, tile by tile
			int w = Dungeon.level.width();
			int[] around = { -w - 1, -w, -w + 1, 1, w + 1, w, w - 1, -1 };
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < around.length; i++){
				final int c = hero.pos + around[i];
				if (!SkillInteractions.valid( c ) || Dungeon.level.solid[c] || !Dungeon.level.heroFOV[c]) continue;
				final int k = i;
				t.at( 0.05f * i, () -> {
					//the one draw of the game's Random the red splash here always made (its emitter's start)
					if (GameScene.layer( GameScene.Layer.EFFECTS ) != null) Random.Float();
					step( hero.sprite, k );
				} );
			}
			t.at( 0.4f, () -> SpatialSound.play( Assets.Sounds.HIT_SLASH, hero, 0.6f, 1.3f ) );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	/** The waltz's crimson (BLOOD's body, D01818), its inner ribbon's (BLOOD's light, FF5050), the
	 *  ribbons' spin (deg/s) and how long they turn (s); the steps it flicks round him: how far out
	 *  (px), how fast the flick runs on (px/s) and how long it lasts (s). Crimson, never pink. */
	public static final int CRIMSON = SkillPalette.BLOOD.stop( 1 ), RIBBON_INNER = SkillPalette.BLOOD_LIGHT;
	/** The motes lifting off each step: the crimson darkening to BLOOD's dark red (8A0A14). */
	public static final Ramp LIFT = new Ramp( CRIMSON, SkillPalette.BLOOD.stop( 2 ) );
	public static final float RIBBON_SPIN = 1440f, RIBBON_FOR = 0.5f, STEP_OUT = 11f, STEP_SPEED = 45f, STEP_LIFE = 0.3f;

	//the k-th of the waltz's eight steps (north-west first, clockwise) as the sweep passes it: a
	//flick of crimson light running on round him the way the sweep turns, and a dark red mote
	//lifting off where it was. Light, never blood on the floor: the dance costs no blood. Render thread
	//(the waltz's timeline)
	public static void step( final CharSprite s, final int k ){
		if (s == null || s.parent == null || !s.visible) return;
		float a = (float) Math.toRadians( 225 + 45 * k );
		float cos = (float) Math.cos( a ), sin = (float) Math.sin( a );
		float x = SkillVfx.midX( s ) + cos * STEP_OUT;
		float y = s.y + s.height() - 2 + sin * STEP_OUT * 0.5f;
		if (!Fx.seen( x, y )) return;
		//clockwise on the screen is along (-sin, cos), on the ground's 2:1 ellipse
		FxParticle f = SkillVfx.glow( x, y, FxBudget.P2 );
		if (f != null) f.look( FxFrames.SPARK_3 ).paint( CRIMSON ).vel( -sin * STEP_SPEED, cos * STEP_SPEED * 0.5f ).orient()
				.life( STEP_LIFE ).alpha( 1f, 0, STEP_LIFE * 0.6f ).source( x, y );
		FxParticle m = SkillVfx.glow( x, y - 1, FxBudget.P3 );
		if (m != null) m.look( FxFrames.MOTE_2 ).ramp( LIFT, true ).vel( 0, -FxRandom.Float( 8f, 11f ) ).snap()
				.life( 0.45f ).alpha( 0.8f, 0.05f, 0.3f ).source( x, y );
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && !ranged && enemy != null
				&& Dungeon.hero != null && Dungeon.hero.buff( Adrenaline.class ) != null){
			Buff.affect( enemy, Bleeding.class ).set( 1 + level );
			Splash.at( enemy.pos, 0xCC1111, 3 );
			SpatialSound.play( Assets.Sounds.HIT_STAB, enemy, 0.8f, 1.1f );
		}
		return damage;
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
