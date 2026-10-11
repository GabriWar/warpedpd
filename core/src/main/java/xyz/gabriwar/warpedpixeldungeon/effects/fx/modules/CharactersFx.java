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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.modules;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.effects.TargetedCell;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.CombatScene;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.List;
import java.util.Locale;

/**
 * Characters and combat (the characters-combat area, docs/fx/characters-combat.md): what a
 * character wears while a state lasts (burning, frozen, shielded and the rest: effects/fx/combat,
 * drawn from CharSprite's states), and every blow, its blood and its marks.
 *
 * Its gallery page: every state on the dry rat in turn (a burning rat walking into the pool, a
 * frozen one shattering, a shield breaking under a blow...), a blow of every style on the rat and
 * the skeleton, a heavy one, a blocked one, a miss, blood on the floor and in the pool, bone dust and
 * crystal sparks, a wound, a surprise, a knock into the wall stub, a boss's telegraph and a search.
 * Its scenes: fx-combat and fx-combat-night, a fight to try every kind of blow on.
 */
public class CharactersFx implements FxModule {

	@Override
	public String key(){
		return "characters-combat";
	}

	@Override
	public String title(){
		return "Characters and combat";
	}

	//the states shown on their own, each on the dry rat for three seconds
	private static final CharSprite.State[] STATES = {
			CharSprite.State.BURNING, CharSprite.State.HALOMETHANEBURNING, CharSprite.State.HEATSTROKE,
			CharSprite.State.CHILLED, CharSprite.State.HYPOTHERMIA, CharSprite.State.DARKENED, CharSprite.State.MARKED,
			CharSprite.State.HEALING, CharSprite.State.GLOWING, CharSprite.State.ILLUMINATED, CharSprite.State.TORCHLIT,
			CharSprite.State.LEVITATING, CharSprite.State.ELECTRIC, CharSprite.State.BUTTER, CharSprite.State.POISONED,
			CharSprite.State.CORRODED, CharSprite.State.OOZED, CharSprite.State.ROOTED, CharSprite.State.VERDANT,
			CharSprite.State.HERB, CharSprite.State.HEARTS, CharSprite.State.INVISIBLE };

	private static final String[] STYLES = { "a plain blow (claws, fists)", "a slash", "a stab", "a crushing blow",
			"an arrow", "a magic blow", "a parried blow", "a strong blow" };

	@Override
	public void exhibits( FxGallery.Page page ){
		for (final CharSprite.State state : STATES){
			page.add( state.name().toLowerCase( Locale.ENGLISH ), 3.2f, s -> {
				CharSprite sprite = s.dryRat.sprite;
				if (sprite == null) return;
				sprite.add( state );
				s.after( 3f, () -> sprite.remove( state ) );
			} );
		}
		page.add( "a burning rat walks into the pool", 3.5f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 1, 0 ) );
			if (p == null) return;
			p.sprite.add( CharSprite.State.BURNING );
			s.after( 1.4f, () -> p.move( s.cell( 2, 0 ) ) );
			s.after( 1.6f, () -> p.sprite.remove( CharSprite.State.BURNING ) );
		} );
		page.add( "frozen, then shattering", 3.2f, s -> {
			CharSprite sprite = s.dryRat.sprite;
			if (sprite == null) return;
			sprite.add( CharSprite.State.FROZEN );
			s.after( 2.6f, () -> sprite.remove( CharSprite.State.FROZEN ) );
		} );
		page.add( "a shield, a blow on it, and it breaks", 3.4f, s -> {
			CharSprite sprite = s.dryRat.sprite;
			if (sprite == null) return;
			sprite.add( CharSprite.State.SHIELDED );
			s.after( 1.2f, sprite::flash );
			s.after( 2.6f, () -> sprite.remove( CharSprite.State.SHIELDED ) );
		} );
		page.add( "barkskin, and a blow on it", 3.2f, s -> {
			CharSprite sprite = s.dryRat.sprite;
			if (sprite == null) return;
			sprite.add( CharSprite.State.BARKED );
			s.after( 1.2f, sprite::flash );
			s.after( 2.8f, () -> sprite.remove( CharSprite.State.BARKED ) );
		} );
		page.add( "rooted, and let go", 3.2f, s -> {
			CharSprite sprite = s.dryRat.sprite;
			if (sprite == null) return;
			sprite.add( CharSprite.State.ROOTED );
			s.after( 2.6f, () -> sprite.remove( CharSprite.State.ROOTED ) );
		} );
		page.add( "an aura", 3.2f, s -> {
			CharSprite sprite = s.dryRat.sprite;
			if (sprite == null) return;
			sprite.aura( 0xFFD060, 6 );
			s.after( 3f, sprite::clearAura );
		} );
		for (int i = 0; i < STYLES.length; i++){
			final int style = i;
			page.add( STYLES[i] + " on the rat and the skeleton", 2.4f, s -> {
				blow( s, s.cell( -1, 2 ), s.dryRat, LIGHT, style, false );
				blow( s, s.cell( 0, 3 ), s.skeleton, LIGHT, style, false );
			} );
		}
		page.add( "a heavy blow by the hero (a third of its health)", 2.4f, s -> blow( s, s.cell( -1, 2 ), s.dryRat,
				HEAVY, HitFx.HIT, true ) );
		page.add( "a blow blocked, and one missed", 2.4f, s -> {
			blow( s, s.cell( -1, 2 ), s.dryRat, 0f, HitFx.SLASH, false );
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( 0, 3 ) );
			if (p == null) return;
			p.attack( s.skeleton.pos, () -> {
				if (s.skeleton.sprite != null){
					s.skeleton.sprite.showStatus( CharSprite.NEUTRAL, "dodged" );
					s.skeleton.sprite.dodgeFx( p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f );
				}
			} );
		} );
		page.add( "blood on the floor and in the pool", 3f, s -> {
			Splash.at( FxStage.center( s.cell( 0, 2 ) ), 0xFFBB0000, 9 );
			Splash.at( FxStage.center( s.cell( 3, 0 ) ), 0xFFBB0000, 9 );
			s.after( 0.6f, () -> Splash.at( FxStage.center( s.cell( 1, 2 ) ), -1.2f, 1.6f, 0xFF44AA22, 9 ) );
		} );
		page.add( "bone dust, crystal sparks, ink", 3f, s -> {
			Splash.at( FxStage.center( s.cell( 0, 2 ) ), 0xFFcdcdb7, 9 );
			Splash.at( FxStage.center( s.cell( 1, 1 ) ), 0xFFFFFF, 8 );
			Splash.at( FxStage.center( s.cell( -1, 1 ) ), 0x000000, 8 );
		} );
		page.add( "wounds", 2.4f, s -> {
			Wound.hit( s.dryRat, 0 );
			s.after( 0.5f, () -> Wound.hit( s.skeleton, 40f ) );
			s.after( 1f, () -> Wound.hit( s.dryRat, -90f ) );
		} );
		page.add( "a surprise attack", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( 0, 3 ) );
			if (p == null) return;
			p.attack( s.skeleton.pos, () -> {
				Surprise.hit( s.skeleton );
				blowFx( s.skeleton.sprite, p, LIGHT, HitFx.SLASH, false );
			} );
		} );
		page.add( "knocked into the wall stub", 2.4f, s -> {
			int from = s.cell( -2, 3 ), to = s.cell( -3, 3 );
			FxStage.Puppet p = s.puppet( Rat.class, from );
			if (p == null) return;
			s.after( 0.4f, () -> {
				//thrown from where it stands, as a knockback is
				new Pushing( p.ch, from, to ).new Effect();
				p.ch.pos = to;
			} );
		} );
		page.add( "knocked across the floor", 2.4f, s -> {
			int from = s.cell( 0, 2 ), to = s.cell( 1, 2 );
			FxStage.Puppet p = s.puppet( Rat.class, from );
			if (p == null) return;
			s.after( 0.4f, () -> {
				//thrown from where it stands, as a knockback is
				new Pushing( p.ch, from, to ).new Effect();
				p.ch.pos = to;
			} );
		} );
		page.add( "a boss's telegraph", 3.2f, s -> {
			TargetedCell a = GameScene.targetedCell( s.cell( 0, 2 ), 1 );
			TargetedCell b = GameScene.targetedCell( s.cell( 1, 2 ), 1 );
			s.after( 2.6f, () -> {
				if (a != null) a.time = 0;
				if (b != null) b.time = 0;
			} );
		} );
		page.add( "a search", 1.6f, s -> {
			int c = s.cell( 0, 1 );
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -2; dx <= 2; dx++){
					GameScene.checkedCell( s.cell( dx, 1 + dy ), c );
				}
			}
		} );
	}

	//a blow's damage as a share of the dummy's health (the dummies are hardy: a fixed number would
	//draw no blood): an ordinary blow, and a heavy one (a third or more lands heavy)
	private static final float LIGHT = 0.15f, HEAVY = 0.5f;

	//a gnoll puppet striking a dummy from a cell: blood, the flash, the blow's look
	private static void blow( FxStage s, int from, xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob dummy, float share,
			int style, boolean hero ){
		FxStage.Puppet p = s.puppet( Gnoll.class, from );
		if (p == null || dummy == null) return;
		p.attack( dummy.pos, () -> blowFx( dummy.sprite, p, share, style, hero ) );
	}

	private static void blowFx( CharSprite d, FxStage.Puppet p, float share, int style, boolean hero ){
		if (d == null || d.ch == null) return;
		int damage = share <= 0 ? 0 : Math.max( 1, Math.round( d.ch.HT * share ) );
		d.bloodBurstA( p.sprite.center(), damage );
		d.flash();
		d.hitFx( p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f, damage, style, hero );
	}

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		out.add( new CombatScene( false ) );
		out.add( new CombatScene( true ) );
	}
}
