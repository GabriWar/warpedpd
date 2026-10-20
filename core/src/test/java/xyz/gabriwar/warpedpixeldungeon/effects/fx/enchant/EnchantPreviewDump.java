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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.LightFlash;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The enchantments' preview dump (tools/fxpreview/README.md): with the environment variable
 * FXPREVIEW set (else it returns at once), every entry of the registry is played on a headless
 * stage - stand-in layers, FxRandom seeded - twice, as today's code shows the moment ("before")
 * and as it shows now ("after"), each on dry floor by day, by night and on water, and every live
 * quad (its texture, frame, place, scale, angle, colour, blend, layer) is written at the beats
 * 0, 0.04, 0.1, 0.3, 0.6 and 1.2 s (and every frame at 30 fps for an entry with a GIF) to
 * build/fxpreview/<name>.json; tools/fxpreview/paint.py paints them.
 *
 * Later areas add their moments with one line each in the registry's static block:
 *     entry( "name", gif, s -> before..., s -> after... );
 * where s is a Scene: the attacker's sprite a and the defender's d (on water for the water row).
 */
public class EnchantPreviewDump {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	/** Where an entry plays: the attacker's and the defender's sprites, and whether it is on water. */
	public static final class Scene {
		public CharSprite a, d;
		public boolean water;
		public EnchantFxTest.Rig rig;
	}

	/** One moment, played on a scene (render thread). */
	public interface Play {
		void play( Scene s );
	}

	static final class Entry {
		final boolean gif;
		final Play before, after;

		Entry( boolean gif, Play before, Play after ){
			this.gif = gif;
			this.before = before;
			this.after = after;
		}
	}

	/** The registry: name -> (whether it gets a GIF, today's moment or null for nothing, the new one). */
	static final LinkedHashMap<String, Entry> ENTRIES = new LinkedHashMap<>();

	static void entry( String name, boolean gif, Play before, Play after ){
		ENTRIES.put( name, new Entry( gif, before, after ) );
	}

	static final String[] TIER_NAMES = { "weak", "mid", "strong", "finisher" };

	//today's blow: the star, its crescent, its light
	static void blow( Scene s ){
		HitFx.hit( s.d, s.a.x + s.a.width() / 2f, s.a.y + s.a.height() / 2f, 6, HitFx.SLASH, false );
	}

	//the new blow: the enchantment's beat, then the blow's star stamped by it
	static void proc( Scene s, EnchantLook l, int tier, int flags ){
		EnchantFx.stageSwing( l, s.a, s.d, true, 1, 1, false );
		EnchantFx.stage( l, s.a, s.d, tier, 1, flags );
		if (tier == EnchantFx.FINISHER) EnchantFx.stageEnded( s.a, s.d );
		else blow( s );
	}

	static {
		for (Family f : Family.values()){
			final EnchantLook l = EnchantFxTest.face( f );
			for (int t : new int[]{ EnchantFx.WEAK, EnchantFx.STRONG, EnchantFx.FINISHER }){
				final int tier = t;
				//today a proc that kills shows nothing (Char.attack returned before the blow's pictures)
				entry( "fam-" + f.name().toLowerCase() + "-" + TIER_NAMES[t], false,
						tier == EnchantFx.FINISHER ? null : EnchantPreviewDump::blow, s -> proc( s, l, tier, 0 ) );
			}
		}
		final EnchantLook blazing = EnchantLooks.of( Blazing.class );
		entry( "echo-beside-the-real-thing", true, s -> proc( s, blazing, EnchantFx.MID, 0 ),
				s -> proc( s, blazing, EnchantFx.MID, EnchantFx.ECHO ) );
		entry( "tinted-star", true, EnchantPreviewDump::blow, s -> {
			s.d.blowLook = blazing;
			s.d.blowUntil = Game.timeTotal + EnchantFx.STAMP_FOR;
			blow( s );
		} );
		entry( "plain-swing-of-an-enchanted-weapon", true, EnchantPreviewDump::blow, s -> {
			EnchantFx.stageSwing( blazing, s.a, s.d, true, 2, 1, false );
			blow( s );
		} );
		entry( "killing-proc", true, null, s -> proc( s, blazing, EnchantFx.FINISHER, 0 ) );
		entry( "blazing-proc", true, s -> {
			blow( s );
			s.d.emitter().burst( FlameParticle.FACTORY, 2 );
		}, s -> {
			proc( s, blazing, EnchantFx.MID, 0 );
			s.d.emitter().burst( FlameParticle.FACTORY, 2 );
		} );
		entry( "hex", true, EnchantPreviewDump::blow, s -> proc( s, EnchantLooks.of( Sacrificial.class ), EnchantFx.MID, EnchantFx.CURSE ) );
		entry( "hex-armour", true, null, s -> EnchantFx.stageWard( EnchantLooks.of( Corrosion.class ), s.d, s.a, EnchantFx.MID, 0,
				EnchantFx.CURSE, -1 ) );
		entry( "elements-blazing-ignite", true, s -> { blow( s ); s.d.emitter().burst( FlameParticle.FACTORY, 1 ); }, s -> { EnchantFx.stageSwing( blazing, s.a, s.d, true, 1, 1, false ); EnchantFx.stage( blazing, s.a, s.d, EnchantFx.MID, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.blazing( true, false ) ); blow( s ); s.d.emitter().burst( FlameParticle.FACTORY, 1 ); } );
		entry( "elements-blazing-kill", true, s -> s.d.emitter().burst( FlameParticle.FACTORY, 1 ), s -> { EnchantFx.stageSwing( blazing, s.a, s.d, true, 1, 1, false ); EnchantFx.stage( blazing, s.a, s.d, EnchantFx.FINISHER, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.blazing( false, true ) ); EnchantFx.stageEnded( s.a, s.d ); s.d.emitter().burst( FlameParticle.FACTORY, 1 ); } );
		entry( "elements-chilling", true, s -> { blow( s ); xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( s.d.center(), 0xFFB2D6FF, 5 ); }, s -> { EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling.class ), s.a, s.d, EnchantFx.STRONG, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.chilling( 6f ) ); blow( s ); xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( s.d.center(), 0xFFB2D6FF, 5 ); } );
		entry( "elements-shocking", true, s -> { s.a.parent.addToFront( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning( java.util.Collections.singletonList( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning.Arc( s.a.center(), s.d.center() ) ), null ) ); s.d.centerEmitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle.FACTORY, 3 ); s.d.flash(); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.on( s.d ); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.weapon( s.a ); }, s -> { s.a.parent.addToFront( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning( java.util.Collections.singletonList( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning.Arc( s.a.center(), s.d.center() ) ), null ) ); s.d.centerEmitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle.FACTORY, 3 ); s.d.flash(); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.on( s.d ); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.weapon( s.a ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking.class ), s.a, s.d, EnchantFx.MID, 1, EnchantFx.multi( 0 ), xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.shock( 0, -1 ) ); } );
		entry( "elements-kinetic-release", true, EnchantPreviewDump::blow, s -> { EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic.class ), s.a, s.d, EnchantFx.STRONG, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.kinetic( 0.8f ) ); blow( s ); } );
		entry( "elements-entanglement-ward", true, s -> s.d.bottomEmitter().start( xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle.FACTORY, 0.05f, 8 ), s -> { s.d.bottomEmitter().start( xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle.FACTORY, 0.05f, 8 ); xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.PlantFx.bark( s.d.ch.pos ); EnchantFx.ward( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Entanglement.class ), s.d.ch, s.a.ch, 0.45f, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.entanglement() ); } );
		entry( "elements-curse-corrosion", true, s -> { for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( s.d.ch.pos + i + j * EnchantFxTest.W, 0x000000, 5 ); }, s -> { for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( s.d.ch.pos + i + j * EnchantFxTest.W, 0x000000, 5 ); EnchantFx.ward( EnchantLooks.of( Corrosion.class ), s.d.ch, s.a.ch, 0.45f, 0, EnchantFx.CURSE, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.corrosion( s.d.ch.pos ) ); } );
		entry( "elements-shocking-weak", true, s -> { s.a.parent.addToFront( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning( java.util.Collections.singletonList( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning.Arc( s.a.center(), s.d.center() ) ), null ) ); s.d.centerEmitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle.FACTORY, 3 ); s.d.flash(); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.on( s.d ); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.weapon( s.a ); }, s -> { s.a.parent.addToFront( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning( java.util.Collections.singletonList( new xyz.gabriwar.warpedpixeldungeon.effects.Lightning.Arc( s.a.center(), s.d.center() ) ), null ) ); s.d.centerEmitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle.FACTORY, 3 ); s.d.flash(); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.on( s.d ); xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle.weapon( s.a ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking.class ), s.a, s.d, EnchantFx.WEAK, 0, EnchantFx.multi( 0 ), xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.shock( 0, -1 ) ); } );
		entry( "elements-paralysis", true, EnchantPreviewDump::blow, s -> { EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis.class ), s.a, s.d, EnchantFx.MID, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows.paralysis( 2f ) ); blow( s ); } );
		entry( "shadow-vampiric-drain", true, EnchantPreviewDump::blow, s -> { EnchantFx.stageSwing( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class ), s.a, s.d, true, 1, 1, false ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class ), s.a, s.d, EnchantFx.STRONG, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.VAMPIRIC ); blow( s ); } );
		entry( "shadow-grim-execute", true, s -> { blow( s ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle.UP, 5 ); }, s -> { blow( s ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle.UP, 5 ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class ), s.a, s.d, EnchantFx.FINISHER, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.GRIM ); } );
		entry( "shadow-eldritch", true, s -> { s.rig.rat( s.d.ch.pos - EnchantFxTest.W + 1 ); blow( s ); }, s -> { CharSprite w = s.rig.rat( s.d.ch.pos - EnchantFxTest.W + 1 ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch.class ), s.a, s.d, EnchantFx.MID, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.eldritch( java.util.Collections.singletonList( w.ch ) ) ); blow( s ); } );
		entry( "shadow-vorpal-bleed", true, s -> { blow( s ); xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( s.d.center(), -com.watabou.utils.PointF.PI / 2, com.watabou.utils.PointF.PI / 6, s.d.blood(), 10 ); }, s -> { blow( s ); xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( s.d.center(), -com.watabou.utils.PointF.PI / 2, com.watabou.utils.PointF.PI / 6, s.d.blood(), 10 ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vorpal.class ), s.a, s.d, EnchantFx.MID, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.VORPAL ); } );
		entry( "shadow-hunting-kill", true, null, s -> { EnchantFx.stageSwing( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting.class ), s.a, s.d, true, 1, 1, false ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting.class ), s.a, s.d, EnchantFx.FINISHER, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.HUNTING ); EnchantFx.stageEnded( s.a, s.d ); } );
		entry( "shadow-midas", true, EnchantPreviewDump::blow, s -> { EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class ), s.a, s.d, EnchantFx.MID, 0, 0 ); blow( s ); } );
		entry( "shadow-midas-payout", true, EnchantPreviewDump::blow, s -> { EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas.class ), s.a, s.d, EnchantFx.STRONG, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.MIDAS_KILL ); blow( s ); } );
		entry( "shadow-afterimage-dodge", true, s -> HitFx.dodged( s.d, s.a.x + 8, s.a.y + 8 ), s -> { HitFx.dodged( s.d, s.a.x + 8, s.a.y + 8 ); xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.afterimageDodge( s.d.ch, s.a.ch ); } );
		entry( "shadow-wondrous-hex", true, s -> { blow( s ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.RainbowParticle.BURST, 25 ); }, s -> { blow( s ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.RainbowParticle.BURST, 25 ); EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wondrous.class ), s.a, s.d, EnchantFx.MID, 0, EnchantFx.CURSE, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.WONDROUS ); } );
		entry( "shadow-buzzsaw-cuts", true, EnchantPreviewDump::blow, s -> { for (int k = 0; k <= 3; k++) EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw.class ), s.a, s.d, EnchantFx.STRONG, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.buzz( k ) ); blow( s ); } );
		entry( "shadow-nomnom", true, EnchantPreviewDump::blow, s -> { EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom.class ), s.a, s.d, EnchantFx.FINISHER, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.NOMNOM ); EnchantFx.stageEnded( s.a, s.d ); } );
		entry( "shadow-lucky-mark", true, EnchantPreviewDump::blow, s -> { EnchantFx.stage( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky.class ), s.a, s.d, EnchantFx.MID, 0, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.LUCKY ); blow( s ); } );
		//the item itself (ench-item-looks)
		entry( "item-bag-strip", true, s -> ItemAuraTest.Previews.bag( s, false ), s -> ItemAuraTest.Previews.bag( s, true ) );
		entry( "item-heaps-at-night", true, s -> ItemAuraTest.Previews.heaps( s, false ), s -> ItemAuraTest.Previews.heaps( s, true ) );
		entry( "item-blazing-thrown-knife", true, s -> ItemAuraTest.Previews.knife( s, false ), s -> ItemAuraTest.Previews.knife( s, true ) );
		entry( "item-dual-enchant-crossfade", true, s -> ItemAuraTest.Previews.crossfade( s, false ), s -> ItemAuraTest.Previews.crossfade( s, true ) );
		entry( "item-echo-mote", true, s -> ItemAuraTest.Previews.echo( s, false ), s -> ItemAuraTest.Previews.echo( s, true ) );
		entry( "item-twists", true, s -> ItemAuraTest.Previews.twists( s, false ), s -> ItemAuraTest.Previews.twists( s, true ) );
		//gaining (ench-gain): Enchanting v2 against today's runes in the same glow, from its start and from
		//0.7 s on (the bind and the settle); identifying revealing a vestige and a curse; a first pickup
		for (final Class<?> c : new Class<?>[]{ Blazing.class, xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying.class }){
			final String n = "gain-" + c.getSimpleName().toLowerCase();
			for (final int skip : new int[]{ 0, 21 }){
				entry( skip == 0 ? n : n + "-settle", true,
						s -> { xyz.gabriwar.warpedpixeldungeon.effects.Enchanting.play( s.d, xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet.SWORD,
								null, EnchantLooks.of( c ).glow, 0, null ); s.rig.frames( skip, true ); },
						s -> { xyz.gabriwar.warpedpixeldungeon.effects.Enchanting.play( s.d, xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet.SWORD,
								EnchantLooks.of( c ), EnchantLooks.of( c ).glow, 0, null ); s.rig.frames( skip, true ); } );
			}
		}
		entry( "reveal-vestige", true, null, s -> GainFx.stageVestigeReveal( s.d, blazing ) );
		entry( "reveal-curse", true, null, s -> GainFx.stageCurseReveal( s.d,
				EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying.class ) ) );
		entry( "first-pickup", true, null, s -> GainFx.stagePickup( s.d, blazing ) );
		//the new pieces (PieceFx, SenseFx): today a crit is its star burst, a heave, a sweep and a bite plain blows
		entry( "piece-crit", true, s -> { blow( s ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.STAR ), 4 ); }, s -> { blow( s ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.STAR ), 4 ); PieceFx.runCrit( s.a, s.d, true, false, false ); } );
		entry( "piece-heave-shockwave", true, s -> HitFx.hit( s.d, s.a.x + 8, s.a.y + 8, 30, HitFx.STRONG, false ), s -> { HitFx.hit( s.d, s.a.x + 8, s.a.y + 8, 30, HitFx.STRONG, false ); PieceFx.runHeave( s.a, s.d, null, true, false, false ); PieceFx.runShockwave( s.a, s.d, new CharSprite[]{ s.rig.rat( s.d.ch.pos - EnchantFxTest.W ), s.rig.rat( s.d.ch.pos + 1 ), s.rig.rat( s.d.ch.pos + EnchantFxTest.W + 1 ) }, null, true, false, false ); } );
		entry( "piece-sweep", true, EnchantPreviewDump::blow, s -> { blow( s ); PieceFx.runSweep( s.a, s.d, new CharSprite[]{ s.rig.rat( s.a.ch.pos - EnchantFxTest.W ), s.rig.rat( s.a.ch.pos + EnchantFxTest.W ), s.rig.rat( s.a.ch.pos + EnchantFxTest.W + 1 ) }, EnchantLooks.of( Blazing.class ), true, false ); } );
		entry( "piece-boomerang", true, s -> { blow( s ); HitFx.hit( s.rig.rat( s.d.ch.pos + 1 ), s.a.x + 8, s.a.y + 8, 6, HitFx.HIT, false ); }, s -> { CharSprite n = s.rig.rat( s.d.ch.pos + 1 ); blow( s ); HitFx.hit( n, s.a.x + 8, s.a.y + 8, 6, HitFx.HIT, false ); PieceFx.runBite( s.a, s.d, 0, null, true, false ); PieceFx.runBite( s.a, n, 1, null, true, false ); EnchantFx.later( 0.35f, () -> PieceFx.runTelegraph( s.d.ch.pos + 2, s.a.ch.pos, s.a, null, false ) ); EnchantFx.later( 0.7f, () -> PieceFx.runCatch( s.a, false ) ); } );
		entry( "piece-sense-ping", true, null, s -> SenseFx.stage( s.a, new CharSprite[]{ s.d, s.rig.rat( s.d.ch.pos - EnchantFxTest.W + 1 ) }, 1.2f ) );
		//one blow, one card (rt2-blow): "before" draws every piece of the moment on its own, as the
		//game drew them before the blow's ledger (its words are not in the dump); "after" hands them
		//to the blow and its presenter. The cards' state is dumped beside the quads ("cards")
		entry( "card-busy-blow", true, s -> busy( s, false ), s -> busy( s, true ) );
		entry( "card-minigun", true, s -> minigun( s, false ), s -> minigun( s, true ) );
		entry( "card-shotgun", true, s -> shotgun( s, false ), s -> shotgun( s, true ) );
		entry( "crit-x-lead", true, s -> { blow( s ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.STAR ), 4 ); },
				s -> critBlow( s, null ) );
		entry( "crit-x-accent", true, s -> { proc( s, blazing, EnchantFx.MID, 0 ); s.d.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.STAR ), 4 ); },
				s -> critBlow( s, blazing ) );
		entry( "crit-x-b1", true, null, s -> {
			critBlow( s, null );
			EnchantFx.later( 0.1f, () -> critBlow( s, null ) );
		} );
		//the weapon types (rt2-type-looks): "before" a plain blow (their words are drawn by the previews' composer)
		entry( "woodsman-splinter", true, s -> TypeLooksTest.Previews.blow( s, 6, null ), TypeLooksTest.Previews::splinter );
		entry( "woodsman-fell", true, s -> TypeLooksTest.Previews.fell( s, false, false ), s -> TypeLooksTest.Previews.fell( s, false, true ) );
		entry( "woodsman-fell-wall", true, s -> TypeLooksTest.Previews.fell( s, true, false ), s -> TypeLooksTest.Previews.fell( s, true, true ) );
		entry( "quarry-daze", true, s -> TypeLooksTest.Previews.blow( s, 5, null ), TypeLooksTest.Previews::daze );
		entry( "quarry-crack", true, s -> TypeLooksTest.Previews.blow( s, 5, null ), TypeLooksTest.Previews::crack );
		entry( "bard-note-pips", true, s -> TypeLooksTest.Previews.notes( s, false ), s -> TypeLooksTest.Previews.notes( s, true ) );
		entry( "bard-chord-rally", true, s -> TypeLooksTest.Previews.chord( s, false, false, false ), s -> TypeLooksTest.Previews.chord( s, false, false, true ) );
		entry( "bard-chord-dread", true, s -> TypeLooksTest.Previews.chord( s, true, false, false ), s -> TypeLooksTest.Previews.chord( s, true, false, true ) );
		entry( "bard-charm", true, s -> TypeLooksTest.Previews.chord( s, true, true, false ), s -> TypeLooksTest.Previews.chord( s, true, true, true ) );
		entry( "lash-pull", true, s -> TypeLooksTest.Previews.lashPull( s, false ), s -> TypeLooksTest.Previews.lashPull( s, true ) );
		entry( "lash-unarm", true, s -> TypeLooksTest.Previews.lashUnarm( s, false ), s -> TypeLooksTest.Previews.lashUnarm( s, true ) );
		entry( "perk-accents", true, s -> TypeLooksTest.Previews.perks( s, false ), s -> TypeLooksTest.Previews.perks( s, true ) );
		entry( "perk-accents-reach-parry", true, s -> TypeLooksTest.Previews.morePerks( s, false ), s -> TypeLooksTest.Previews.morePerks( s, true ) );
		entry( "siphon-mote", true, s -> TypeLooksTest.Previews.siphon( s, false ), s -> TypeLooksTest.Previews.siphon( s, true ) );
		entry( "lifesteal-landing", true, s -> { PieceFx.runLifesteal( s.a, s.d, false ); blow( s ); }, s -> inBlow( s, true, () -> {
			s.a.showStatusWithIcon( CharSprite.POSITIVE, "4", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.HEALING );
			PieceFx.lifesteal( s.a.ch, s.d.ch );
			s.d.showStatusWithIcon( CharSprite.NEGATIVE, "12", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
			s.d.hitFx( s.a.x + 8, s.a.y + 8, 12, HitFx.SLASH, false );
		} ) );
		//a drain's heal waits for its beads (rt2-calm): "before" the procs restaged on their own (the
		//heal showed at once), "after" in a blow, the wielder's card when the last bead lands
		final EnchantLook vampiric = EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class );
		entry( "drain-landing-lead", true, s -> { EnchantFx.stage( vampiric, s.a, s.d, EnchantFx.STRONG, 1, 0,
				xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.VAMPIRIC ); blow( s ); },
				s -> drainBlow( s, null, vampiric ) );
		entry( "drain-landing-second", true, s -> { EnchantFx.stage( blazing, s.a, s.d, EnchantFx.MID, 1, 0 );
				EnchantFx.stage( vampiric, s.a, s.d, EnchantFx.STRONG, 1, 0, xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.VAMPIRIC );
				blow( s ); }, s -> drainBlow( s, blazing, vampiric ) );

		//the rarity lines' looks (rt2-line-looks): before this pass none showed
		entry( "line-chips", true, null, s -> LineLooksTest.Previews.chips( s, 21, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.S_UNDEAD,
				xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.EXECUTIONER ) );
		entry( "line-chips-five", true, null, s -> LineLooksTest.Previews.chips( s, 23, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.S_BEAST,
				xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.EXECUTIONER, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.MOON,
				xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.STORM, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.DEFIANT ) );
		final String[] bodyLines = { "warm-grip", "moonlit", "cooling", "rainproof", "windbreak", "fleet", "hushed", "surefoot" };
		final int[] bodyLooks = { LineLooks.WARM_GRIP, LineLooks.MOONLIT, LineLooks.COOLING, LineLooks.RAINPROOF,
				LineLooks.WINDBREAK, LineLooks.FLEET, LineLooks.HUSHED, LineLooks.SUREFOOT };
		for (int i = 0; i < bodyLines.length; i++){
			final int look = bodyLooks[i];
			entry( "line-ambient-" + bodyLines[i], true, null, s -> LineLooksTest.Previews.ambient( s, look ) );
		}
		entry( "extra-yield", true, null, LineLooksTest.Previews::extra );
		entry( "hints", true, null, LineLooksTest.Previews::hints );
		//words that became icons (words to icons): "before" the moment as it was (its words are drawn
		//by the previews' composer), "after" a miss's card, a skill's shout and the fear it causes on
		//the struck one's card, a buff joining a card after its blow, immunity, a count over the hero
		entry( "words-miss", true, s -> s.d.dodgeFx( s.a.x + 8, s.a.y + 8 ), s -> inBlow( s, true, () -> {
			s.d.dodgeFx( s.a.x + 8, s.a.y + 8 );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.miss( s.d.ch, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.MISS_DODGE, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.MISS_EVA );
		} ) );
		entry( "words-shout", true, s -> { blow( s ); number( s, 7, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG ); }, s -> inBlow( s, true, () -> {
			blow( s );
			number( s, 7, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.chip( s.d.ch, xyz.gabriwar.warpedpixeldungeon.effects.HitCard.SKILL_BASE + new xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CreepingDread().image(), 0 );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.announce( s.d.ch, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror() );
		} ) );
		entry( "words-late-chip", true, s -> { blow( s ); number( s, 9, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG ); }, s -> {
			inBlow( s, true, () -> { blow( s ); number( s, 9, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG ); } );
			EnchantFx.later( 0.2f, () -> xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.late( s.d.ch, -(xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.TERROR + 1), 0 ) );
		} );
		entry( "words-immune", true, s -> { }, s -> inBlow( s, true, () -> xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.mark( s.d.ch, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.IMMUNE ) ) );
		entry( "words-counter", true, s -> { }, s -> {
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.counter( s.a.ch, xyz.gabriwar.warpedpixeldungeon.effects.HitCard.SKILL_BASE + new xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.RecklessFury().image(), 7, CharSprite.NEGATIVE );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.late( s.d.ch, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.RAGE, 0 );
		} );
	}

	//a blow healing its attacker by 5 through a strong Vampiric proc (after a lead proc, or leading)
	static void drainBlow( Scene s, EnchantLook lead, EnchantLook drain ){
		inBlow( s, true, () -> {
			if (lead != null) EnchantFx.proc( lead, s.a.ch, s.d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 1, 0 );
			EnchantFx.proc( drain, s.a.ch, s.d.ch, EnchantFx.strengthOf( EnchantFx.STRONG ), 1, 0,
					xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows.VAMPIRIC );
			s.a.showStatusWithIcon( CharSprite.POSITIVE, "5", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.HEALING );
			number( s, 9, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
			s.d.hitFx( s.a.x + 8, s.a.y + 8, 9, HitFx.SLASH, false );
		} );
	}

	// ------------------------------------------------------------------ blows (rt2-blow)

	//a moment inside a blow of the scene's attacker on its defender (or, before the ledger, as it is)
	static void inBlow( Scene s, boolean ledger, Runnable r ){
		if (ledger) xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.begin( s.a.ch, s.d.ch );
		try {
			r.run();
		} finally {
			if (ledger) xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.end();
		}
	}

	//a number on the defender (claimed by the blow's card inside one)
	static void number( Scene s, int n, int icon ){
		s.d.showStatusWithIcon( CharSprite.NEGATIVE, Integer.toString( n ), icon );
	}

	//the worked busy blow (docs/fx/hit-card.md): a Blazing proc, a Shocking vestige's echo, a crit,
	//a lifesteal, the burn's and the blow's numbers, Burning, the blood, two flashes, the swing, the star
	static void busy( Scene s, boolean ledger ){
		final EnchantLook blazing = EnchantLooks.of( Blazing.class );
		inBlow( s, ledger, () -> {
			EnchantFx.proc( blazing, s.a.ch, s.d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
			EnchantFx.proc( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking.class ), s.a.ch, s.d.ch,
					EnchantFx.strengthOf( EnchantFx.MID ), 0, EnchantFx.ECHO );
			PieceFx.crit( s.a.ch, s.d.ch );
			s.a.showStatusWithIcon( CharSprite.POSITIVE, "4", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.HEALING );
			PieceFx.lifesteal( s.a.ch, s.d.ch );
			number( s, 3, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.MAGIC_DMG );
			if (ledger) xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.announce( s.d.ch, new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning() );
			number( s, 27, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
			s.d.bloodBurstA( s.a.center(), 27 );
			s.d.flash();
			s.d.flash();
			EnchantFx.SwingPiece swing = new EnchantFx.SwingPiece( s.a.ch, blazing, s.a, s.d, true, true, 0, false, 1, false );
			if (!xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.offer( swing )) EnchantFx.stageSwing( blazing, s.a, s.d, true, 0, 1, false );
			s.d.hitFx( s.a.x + 8, s.a.y + 8, 27, HitFx.SLASH, false );
		} );
	}

	//a round of a minigun's: its number, a crit on the second, Vulnerable on the first
	static void round( Scene s, boolean ledger, int k ){
		inBlow( s, ledger, () -> {
			if (k == 1) PieceFx.crit( s.a.ch, s.d.ch );
			if (k == 0 && ledger) xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.announce( s.d.ch,
					new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable() );
			number( s, k % 2 == 0 ? 11 : 10, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
			s.d.bloodBurstA( s.a.center(), 11 );
			s.d.flash();
			s.d.hitFx( s.a.x + 8, s.a.y + 8, 11, HitFx.HIT, false );
		} );
	}

	//four rounds on one rat in 0.4 s (B0, B1, B2, B2)
	static void minigun( Scene s, boolean ledger ){
		round( s, ledger, 0 );
		for (int k = 1; k < 4; k++){
			final int r = k;
			EnchantFx.later( 0.1f * k, () -> round( s, ledger, r ) );
		}
	}

	//five pellets in one frame (B0 and four B3)
	static void shotgun( Scene s, boolean ledger ){
		for (int k = 0; k < 5; k++){
			inBlow( s, ledger, () -> {
				number( s, 6, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
				s.d.bloodBurstA( s.a.center(), 6 );
				s.d.flash();
				s.d.hitFx( s.a.x + 8, s.a.y + 8, 6, HitFx.ARROW, false );
			} );
		}
	}

	//a crit blow: the X leading a plain one, or an accent after a proc's lead
	static void critBlow( Scene s, EnchantLook proc ){
		inBlow( s, true, () -> {
			if (proc != null) EnchantFx.proc( proc, s.a.ch, s.d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
			PieceFx.crit( s.a.ch, s.d.ch );
			number( s, 12, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PHYS_DMG );
			s.d.bloodBurstA( s.a.center(), 12 );
			s.d.flash();
			s.d.hitFx( s.a.x + 8, s.a.y + 8, 12, HitFx.SLASH, false );
		} );
	}

	// ------------------------------------------------------------------ the dump

	static final float[] BEATS = { 0f, 0.04f, 0.1f, 0.3f, 0.6f, 1.2f };
	static final float FPS = 30f, LONG = 1.25f;
	/** The view: cells round the defender (the top row, its width and height in cells; two cells
	 *  either side of the defender). */
	static final int VIEW_Y = 3, VIEW_W = 5, VIEW_H = 5;

	@Test
	public void dump() throws Exception {
		if (System.getenv( "FXPREVIEW" ) == null) return;
		File dir = new File( "build/fxpreview" );
		dir.mkdirs();
		for (Map.Entry<String, Entry> e : ENTRIES.entrySet()){
			JSONObject out = new JSONObject();
			out.put( "name", e.getKey() );
			out.put( "fps", FPS );
			out.put( "beats", new JSONArray( BEATS ) );
			out.put( "gif", e.getValue().gif );
			out.put( "cell", DungeonTilemap.SIZE );
			JSONObject variants = new JSONObject();
			variants.put( "before", variant( e.getValue().before, e.getValue().gif ) );
			variants.put( "after", variant( e.getValue().after, e.getValue().gif ) );
			out.put( "variants", variants );
			try (Writer w = new FileWriter( new File( dir, e.getKey() + ".json" ) )){
				w.write( out.toString() );
			}
		}
	}

	//a moment's three rows: dry by day, dry at night, on water by day
	private JSONObject variant( Play play, boolean gif ) throws Exception {
		JSONObject v = new JSONObject();
		v.put( "day", run( play, false, false, gif ) );
		v.put( "night", run( play, false, true, gif ) );
		v.put( "water", run( play, true, false, gif ) );
		return v;
	}

	private JSONObject run( Play play, boolean water, boolean night, boolean gif ) throws Exception {
		EnchantFxTest.Rig rig = new EnchantFxTest.Rig();
		rig.up();
		Sting.out = ( sample, at, volume, pitch ) -> { };
		tint( night ? 0.54f : 0f );
		try {
			FxRandom.seedForTests( 11 );
			Scene s = new Scene();
			s.rig = rig;
			s.water = water;
			int y = water ? 6 : 6, ax = water ? 8 : 5, dx = water ? 9 : 6;
			s.a = rig.rat( EnchantFxTest.at( ax, y ) );
			s.d = rig.rat( EnchantFxTest.at( dx, y ) );
			s.a.flipHorizontal = false;
			s.d.flipHorizontal = true;
			JSONObject run = new JSONObject();
			int vx = dx - 2, vy = VIEW_Y;
			run.put( "view", new JSONArray( new int[]{ vx, vy, VIEW_W, VIEW_H } ) );
			JSONArray wet = new JSONArray();
			for (int cy = vy; cy < vy + VIEW_H; cy++){
				for (int cx = vx; cx < vx + VIEW_W; cx++){
					int c = EnchantFxTest.at( cx, cy );
					if (Dungeon.level.map[c] == Terrain.WATER) wet.put( new JSONArray( new int[]{ cx, cy } ) );
				}
			}
			run.put( "water", wet );
			JSONArray frames = new JSONArray();
			if (play != null) play.play( s );
			int n = Math.round( LONG * FPS );
			int beat = 0;
			for (int f = 0; f <= n; f++){
				float t = f / FPS;
				boolean onBeat = beat < BEATS.length && t + 0.5f / FPS >= BEATS[beat];
				if (onBeat) beat++;
				if (gif || onBeat){
					JSONObject fr = new JSONObject();
					fr.put( "t", t );
					fr.put( "beat", onBeat );
					fr.put( "quads", quads( rig ) );
					fr.put( "cards", cards( rig ) );
					frames.put( fr );
				}
				Game.elapsed = 1f / FPS;
				rig.frames( 1, true );
			}
			run.put( "frames", frames );
			return run;
		} finally {
			tint( 0f );
			rig.down();
		}
	}

	private static void tint( float a ) throws Exception {
		Field f = GameScene.class.getDeclaredField( "curTintA" );
		f.setAccessible( true );
		f.setFloat( null, a );
	}

	// ------------------------------------------------------------------ the quads

	//the layers in the order the game's scene draws them; the stage (the sprites) after the floor
	private static JSONArray quads( EnchantFxTest.Rig rig ) throws Exception {
		JSONArray out = new JSONArray();
		GameScene.Layer[] order = { GameScene.Layer.SUBSURFACE, GameScene.Layer.SHEEN, GameScene.Layer.SURFACE, GameScene.Layer.FLOOR };
		for (GameScene.Layer l : order) walk( rig.layers[l.ordinal()], l.name(), false, out );
		walk( rig.stage, "STAGE", false, out );
		walk( rig.layers[GameScene.Layer.EFFECTS.ordinal()], "EFFECTS", false, out );
		walk( rig.layers[GameScene.Layer.LIGHTS.ordinal()], "LIGHTS", false, out );
		return out;
	}

	//the hit cards alive (their words need fonts, which a headless stage has none of: their state is
	//written for a painter to lay them out): anchor x, number row bottom, hop, alpha, the number
	//(-1 none), its colour, xN, the left icon, the X's stamp, its kind, and its chips [code, tint,
	//landed 0..1] as many as the row shows, and how many it holds
	private static JSONArray cards( EnchantFxTest.Rig rig ) throws Exception {
		JSONArray out = new JSONArray();
		Group statuses = rig.layers[GameScene.Layer.STATUSES.ordinal()];
		for (Gizmo m : statuses.membersView()){
			if (!(m instanceof xyz.gabriwar.warpedpixeldungeon.effects.HitCard) || !m.alive) continue;
			xyz.gabriwar.warpedpixeldungeon.effects.HitCard c = (xyz.gabriwar.warpedpixeldungeon.effects.HitCard) m;
			JSONObject o = new JSONObject();
			o.put( "x", (double) c.left() );
			o.put( "base", (double) c.baseY() );
			o.put( "hop", (double) Math.round( c.hop() ) );
			o.put( "alpha", (double) c.alpha() );
			o.put( "number", c.number() );
			o.put( "color", c.color() );
			o.put( "count", c.count() );
			o.put( "icon", c.leftIcon() );
			o.put( "stamp", c.stamping() );
			o.put( "kind", c.kind() );
			JSONArray chips = new JSONArray();
			for (int i = 0; i < c.shownChips(); i++){
				chips.put( new JSONArray( new double[]{ c.chip( i ), c.chipTint( i ), c.chipLanded( i ) } ) );
			}
			o.put( "chips", chips );
			o.put( "held", c.chipCount() );
			out.put( o );
		}
		return out;
	}

	private static void walk( Group g, String layer, boolean light, JSONArray out ) throws Exception {
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive || !m.visible) continue;
			if (m instanceof Emitter){
				walk( (Group) m, layer, light || lightMode( (Emitter) m ), out );
			} else if (m instanceof Group){
				walk( (Group) m, layer, light, out );
			} else if (m instanceof Image){
				Image i = (Image) m;
				if (i.am <= 0.004f || i.texture == null) continue;
				out.put( quad( i, layer, light || additive( i ) ) );
			}
		}
	}

	private static boolean lightMode( Emitter e ) throws Exception {
		Field f = Emitter.class.getDeclaredField( "lightMode" );
		f.setAccessible( true );
		return f.getBoolean( e );
	}

	private static boolean additive( Image i ) throws Exception {
		if (i instanceof EnchantFx.Flash) return ((EnchantFx.Flash) i).additive();
		if (i instanceof FxRing){
			Field f = FxRing.class.getDeclaredField( "additive" );
			f.setAccessible( true );
			return f.getBoolean( i );
		}
		return i instanceof FxLight || i instanceof LightFlash || i instanceof xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage
				|| i instanceof xyz.gabriwar.warpedpixeldungeon.effects.fx.ItemGhost;
	}

	private static JSONArray quad( Image i, String layer, boolean light ) throws Exception {
		SmartTexture t = i.texture;
		RectF f = i.frame();
		JSONArray q = new JSONArray();
		q.put( layer );
		q.put( key( t ) );
		q.put( Math.round( f.left * t.width ) );
		q.put( Math.round( f.top * t.height ) );
		q.put( Math.round( (f.right - f.left) * t.width ) );
		q.put( Math.round( (f.bottom - f.top) * t.height ) );
		for (float v : new float[]{ i.x, i.y, i.origin.x, i.origin.y, i.scale.x, i.scale.y, i.angle,
				i.rm, i.gm, i.bm, i.am, i.ra, i.ga, i.ba, i.aa }){
			q.put( (double) v );
		}
		q.put( i.flipHorizontal );
		q.put( i.flipVertical );
		q.put( light );
		return q;
	}

	//every texture the game knows by its asset's name, found by the one in use
	private static final IdentityHashMap<SmartTexture, String> KEYS = new IdentityHashMap<>();

	private static String key( SmartTexture t ) throws Exception {
		String k = KEYS.get( t );
		if (k != null) return k;
		for (Class<?> c : new Class<?>[]{ Assets.Effects.class, Assets.Sprites.class, Assets.Environment.class, Assets.Interfaces.class }){
			for (Field f : c.getFields()){
				if (!Modifier.isStatic( f.getModifiers() ) || f.getType() != String.class) continue;
				String asset = (String) f.get( null );
				if (TextureCache.contains( asset ) && TextureCache.get( asset ) == t){
					KEYS.put( t, asset );
					return asset;
				}
			}
		}
		return "?";
	}
}
