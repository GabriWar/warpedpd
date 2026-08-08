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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FetidRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollArcher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollTrickster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.MailArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ScaleArmor;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ParchmentScrap;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.SewerLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.GhostSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import xyz.gabriwar.warpedpixeldungeon.windows.WndSadGhost;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Ghost extends NPC {

	{
		spriteClass = GhostSprite.class;
		
		flying = true;

		WANDERING = new Wandering();
		state = WANDERING;

		//not actually large of course, but this makes the ghost stick to the exit room
		properties.add(Property.LARGE);
	}

	protected class Wandering extends Mob.Wandering{
		@Override
		protected int randomDestination() {
			int pos = super.randomDestination();
			//cannot wander onto heaps or the level exit
			if (Dungeon.level.heaps.get(pos) != null || pos == Dungeon.level.exit()){
				return -1;
			}
			return pos;
		}
	}

	@Override
	public Notes.Landmark landmark() {
		return Notes.Landmark.GHOST;
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero.buff(AscensionChallenge.class) != null){
			die(null);
			Notes.remove( landmark() );
			return true;
		}
		return super.act();
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}
	
	@Override
	public float speed() {
		return 0.5f;
	}
	
	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//do nothing
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}
	
	@Override
	public boolean reset() {
		return true;
	}
	
	@Override
	public boolean interact(Char c) {
		sprite.turnTo( pos, c.pos );

		Sample.INSTANCE.play( Assets.Sounds.GHOST );

		// Multiplayer: every hero gets their own run of the quest. State, objective mob
		// and reward are tracked per-hero (keyed by hero id). The ghost persists across
		// heroes — it doesn't vanish after one player completes.
		if (!(c instanceof Hero)){
			return super.interact(c);
		}
		Hero h = (Hero) c;
		Quest.HeroProgress hp = Quest.progressFor( h.id() );

		if (hp != null && hp.given) {
			if (hp.completed) {
				// this hero already claimed their reward; nothing left to offer
				return true;
			}
			if (hp.processed) {
				showReward( h, hp );
			} else {
				showReminder( h );
			}
		} else {
			// give the quest to this hero: spawn their own objective mob, roll their reward
			Mob questBoss = Quest.spawnObjective( this, h );
			if (questBoss != null) {
				Quest.give( h );
				showIntro( h );
			}
		}

		return true;
	}

	private void showIntro( final Hero h ) {
		final String txt_quest;
		switch (Quest.type){
			case 1: default:
				txt_quest = Messages.get(this, "rat_1", Messages.titleCase(h.name())); break;
			case 2:
				txt_quest = Messages.get(this, "gnoll_1", Messages.titleCase(h.name())); break;
			case 3:
				txt_quest = Messages.get(this, "crab_1", Messages.titleCase(h.name())); break;
		}
		if (h.isRemote) {
			QuestSupport.sendInfo( this, h, txt_quest );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndQuest( Ghost.this, txt_quest ){
						@Override
						public void hide() {
							super.hide();
							Music.INSTANCE.fadeOut(1f, new Callback() {
								@Override
								public void call() {
									if (Dungeon.level != null) {
										Dungeon.level.playLevelMusic();
									}
								}
							});
						}
					} );
				}
			});
		}
	}

	private void showReminder( final Hero h ) {
		final String txt;
		switch (Quest.type){
			case 1: default: txt = Messages.get(this, "rat_2");   break;
			case 2:          txt = Messages.get(this, "gnoll_2"); break;
			case 3:          txt = Messages.get(this, "crab_2");  break;
		}
		if (h.isRemote) {
			QuestSupport.sendInfo( this, h, txt );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndQuest(Ghost.this, txt));
				}
			});
		}
	}

	private void showReward( final Hero h, final Quest.HeroProgress hp ) {
		if (h.isRemote) {
			Quest.sendReward( h, hp );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndSadGhost(Ghost.this, h, Quest.type));
				}
			});
		}
	}

	public static class Quest {

		// Per-hero progress: each hero who talks to the ghost gets their own copy of
		// the quest (own objective mob, own reward). Keyed by hero.id() (stable across
		// save/load, already networked). spawned/type/depth describe the single physical
		// ghost placement and stay global.
		public static class HeroProgress implements PerHeroProgress {
			public int heroId;
			public boolean given;
			public boolean processed;
			public boolean completed;

			public Weapon weapon;
			public Armor armor;
			public Weapon.Enchantment enchant;
			public Armor.Glyph glyph;

			@Override public int heroId() { return heroId; }

			@Override
			public void storeInBundle( Bundle b ) {
				b.put( HERO_ID, heroId );
				b.put( GIVEN, given );
				b.put( PROCESSED, processed );
				b.put( COMPLETED, completed );
				if (weapon != null)  b.put( WEAPON, weapon );
				if (armor != null)   b.put( ARMOR, armor );
				if (enchant != null) b.put( ENCHANT, enchant );
				if (glyph != null)   b.put( GLYPH, glyph );
			}

			@Override
			public void restoreFromBundle( Bundle b ) {
				heroId    = b.getInt( HERO_ID );
				given     = b.getBoolean( GIVEN );
				processed = b.getBoolean( PROCESSED );
				completed = b.getBoolean( COMPLETED );
				if (b.contains( WEAPON ))  weapon  = (Weapon) b.get( WEAPON );
				if (b.contains( ARMOR ))   armor   = (Armor)  b.get( ARMOR );
				if (b.contains( ENCHANT )) enchant = (Weapon.Enchantment) b.get( ENCHANT );
				if (b.contains( GLYPH ))   glyph   = (Armor.Glyph)        b.get( GLYPH );
			}
		}

		private static boolean spawned;
		private static int type;
		private static int depth;

		private static final PerHeroStore<HeroProgress> progress = new PerHeroStore<>();

		public static void reset() {
			spawned = false;
			progress.clear();
		}

		public static int type() {
			return type;
		}

		public static HeroProgress progressFor( int heroId ) {
			return progress.get( heroId );
		}

		private static final String NODE		= "sadGhost";

		private static final String SPAWNED		= "spawned";
		private static final String TYPE        = "type";
		private static final String DEPTH		= "depth";
		private static final String PROGRESS	= "progress";
		private static final String HERO_ID		= "hero_id";
		private static final String GIVEN		= "given";
		private static final String PROCESSED	= "processed";
		private static final String COMPLETED	= "completed";
		private static final String WEAPON		= "weapon";
		private static final String ARMOR		= "armor";
		private static final String ENCHANT		= "enchant";
		private static final String GLYPH		= "glyph";

		public static void storeInBundle( Bundle bundle ) {

			Bundle node = new Bundle();

			node.put( SPAWNED, spawned );

			if (spawned) {
				node.put( TYPE, type );
				node.put( DEPTH, depth );
				progress.store( node, PROGRESS );
			}

			bundle.put( NODE, node );
		}

		public static void restoreFromBundle( Bundle bundle ) {

			Bundle node = bundle.getBundle( NODE );

			progress.clear();

			if (!node.isNull() && (spawned = node.getBoolean( SPAWNED ))) {
				type  = node.getInt( TYPE );
				depth = node.getInt( DEPTH );
				// Guarded read — pre-per-hero saves had no "progress" array; those legacy
				// in-flight quests simply restart for the current heroes.
				progress.restore( node, PROGRESS );
			} else {
				reset();
			}
		}

		public static void spawn( SewerLevel level, Room room ) {
			if (!spawned && Dungeon.depth > 1 && Random.Int( 5 - Dungeon.depth ) == 0) {

				Ghost ghost = new Ghost();
				do {
					ghost.pos = level.pointToCell(room.random());
				} while (ghost.pos == -1 || level.solid[ghost.pos] || !level.openSpace[ghost.pos] || ghost.pos == level.exit());
				level.mobs.add( ghost );

				//spawn a gnoll archer alongside the ghost quest (from Sprouted)
				GnollArcher archer = new GnollArcher();
				do {
					archer.pos = level.randomRespawnCell( ghost );
				} while (archer.pos == -1);
				level.mobs.add( archer );

				spawned = true;
				//dungeon depth determines type of quest.
				//depth2=fetid rat, 3=gnoll trickster, 4=great crab
				type = Dungeon.depth-1;
				depth = Dungeon.depth;
			}
		}

		/** Spawn a fresh objective mob for the given hero, tagged to their id so its
		 *  death credits the right player. Returns null if no spawn cell was found. */
		public static Mob spawnObjective( Ghost ghost, Hero h ) {
			// Reuse an orphaned objective (e.g. a pre-per-hero save's mob, questOwner==-1)
			// instead of spawning a duplicate that nobody can credit.
			Mob orphan = findUntaggedObjective();
			if (orphan != null) {
				if (orphan instanceof FetidRat)            ((FetidRat) orphan).questOwner = h.id();
				else if (orphan instanceof GnollTrickster) ((GnollTrickster) orphan).questOwner = h.id();
				else if (orphan instanceof GreatCrab)      ((GreatCrab) orphan).questOwner = h.id();
				return orphan;
			}
			int cell = Dungeon.level.randomRespawnCell( ghost );
			if (cell == -1) return null;
			Mob questBoss;
			switch (type){
				case 2: {
					GnollTrickster m = new GnollTrickster(); m.questOwner = h.id(); questBoss = m; break;
				}
				case 3: {
					GreatCrab m = new GreatCrab(); m.questOwner = h.id(); questBoss = m; break;
				}
				case 1: default: {
					FetidRat m = new FetidRat(); m.questOwner = h.id(); questBoss = m; break;
				}
			}
			questBoss.pos = cell;
			GameScene.add( questBoss );
			return questBoss;
		}

		/** Register the quest for a hero and roll their personal reward. */
		public static void give( Hero h ) {
			HeroProgress hp = new HeroProgress();
			hp.heroId = h.id();
			rollReward( hp );
			hp.given = true;
			progress.put( hp );
		}

		private static void rollReward( HeroProgress hp ) {
			//50%:tier2, 30%:tier3, 15%:tier4, 5%:tier5
			switch (Random.chances(new float[]{0, 0, 10, 6, 3, 1})){
				default:
				case 2: hp.armor = new LeatherArmor(); break;
				case 3: hp.armor = new MailArmor();    break;
				case 4: hp.armor = new ScaleArmor();   break;
				case 5: hp.armor = new PlateArmor();   break;
			}
			//50%:tier2, 30%:tier3, 15%:tier4, 5%:tier5
			int wepTier = Random.chances(new float[]{0, 0, 10, 6, 3, 1});
			hp.weapon = (Weapon) Generator.random(Generator.wepTiers[wepTier - 1]);

			//clear weapon's starting properties
			hp.weapon.level(0);
			hp.weapon.enchant(null);
			hp.weapon.cursed = false;

			//50%:+0, 30%:+1, 15%:+2, 5%:+3
			float itemLevelRoll = Random.Float();
			int itemLevel;
			if (itemLevelRoll < 0.5f){
				itemLevel = 0;
			} else if (itemLevelRoll < 0.8f){
				itemLevel = 1;
			} else if (itemLevelRoll < 0.95f){
				itemLevel = 2;
			} else {
				itemLevel = 3;
			}
			hp.weapon.upgrade(itemLevel);
			hp.armor.upgrade(itemLevel);

			// 20% base chance to be enchanted, stored separately so status isn't revealed early
			//we generate first so that the outcome doesn't affect the number of RNG rolls
			hp.enchant = Weapon.Enchantment.random();
			hp.glyph = Armor.Glyph.random();

			float enchantRoll = Random.Float();
			if (enchantRoll > 0.2f * ParchmentScrap.enchantChanceMultiplier()){
				hp.enchant = null;
				hp.glyph = null;
			}
		}

		/** An objective mob died — credit its owning hero. */
		public static void process( int heroId ) {
			HeroProgress hp = progress.get( heroId );
			if (spawned && hp != null && hp.given && !hp.processed && (depth == Dungeon.depth)) {
				Hero owner = (Hero) xyz.gabriwar.warpedpixeldungeon.actors.Actor.findById( heroId );
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( owner, GLog.NEGATIVE + Messages.get(Ghost.class, "find_me") );
				Sample.INSTANCE.play( Assets.Sounds.GHOST );
				hp.processed = true;
				Statistics.questScores[0] += 1000;

				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						Music.INSTANCE.fadeOut(1f, new Callback() {
							@Override
							public void call() {
								if (Dungeon.level != null) {
									Dungeon.level.playLevelMusic();
								}
							}
						});
					}
				});
			}
		}

		/** Grant the chosen reward to a hero who finished the objective. Runs on the
		 *  actor thread (local pick via WndSadGhost, or remote via NetDialogs.resolve). */
		public static void claimReward( Hero hero, boolean chooseWeapon ) {
			HeroProgress hp = progress.get( hero.id() );
			if (hp == null || !hp.processed || hp.completed) return;

			Item reward = chooseWeapon ? hp.weapon : hp.armor;
			if (reward == null) return;

			if (reward instanceof Weapon && hp.enchant != null) {
				((Weapon) reward).enchant( hp.enchant );
			} else if (reward instanceof Armor && hp.glyph != null) {
				((Armor) reward).inscribe( hp.glyph );
			}

			reward.identify(false);
			if (reward.doPickUp( hero )) {
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( hero, Messages.capitalize(Messages.get(hero, "you_now_have", reward.name())) );
			} else {
				Dungeon.level.drop( reward, hero.pos ).sprite.drop();
			}

			hp.completed = true;
			hp.weapon = null;
			hp.armor = null;

			maybeDespawnGhost();
		}

		// Find an objective mob on the level not yet tied to a hero (legacy/orphan).
		private static Mob findUntaggedObjective() {
			if (Dungeon.level == null) return null;
			for (Mob m : Dungeon.level.mobs) {
				if (m instanceof FetidRat && ((FetidRat) m).questOwner == -1) return m;
				if (m instanceof GnollTrickster && ((GnollTrickster) m).questOwner == -1) return m;
				if (m instanceof GreatCrab && ((GreatCrab) m).questOwner == -1) return m;
			}
			return null;
		}

		private static Ghost findGhost() {
			if (Dungeon.level == null) return null;
			for (Mob m : Dungeon.level.mobs) {
				if (m instanceof Ghost) return (Ghost) m;
			}
			return null;
		}

		// SP parity + MP correctness: the ghost only leaves (and its journal landmark
		// clears) once EVERY present hero has completed the quest — so a player who
		// hasn't talked to it yet doesn't lose their chance.
		private static void maybeDespawnGhost() {
			if (!QuestSupport.allPresentDone( progress, hp -> hp.completed )) return;
			final Ghost ghost = findGhost();
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					if (ghost != null) ghost.die( null );
					Notes.remove( Notes.Landmark.GHOST );
				}
			});
		}

		/** True while any hero has the quest in progress at this depth (drives level
		 *  music/water). */
		public static boolean active(){
			if (!spawned || depth != Dungeon.depth) return false;
			for (HeroProgress hp : progress.values()) {
				if (hp.given && !hp.processed) return true;
			}
			return false;
		}

		/** True once any hero has finished the quest (gates Dried Rose availability). */
		public static boolean completed(){
			if (!spawned) return false;
			for (HeroProgress hp : progress.values()) {
				if (hp.completed) return true;
			}
			return false;
		}

		// --- Networked dialog helpers (host side) ---

		static void sendReward( Hero h, HeroProgress hp ) {
			try {
				org.json.JSONObject p = new org.json.JSONObject();
				p.put("text", Messages.get(xyz.gabriwar.warpedpixeldungeon.windows.WndSadGhost.class, "give_item"));
				p.put("type", type);
				if (hp.weapon != null) p.put("weapon", QuestSupport.itemDisplay(hp.weapon));
				if (hp.armor != null)  p.put("armor", QuestSupport.itemDisplay(hp.armor));
				xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.request(
						h, xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_GHOST_REWARD, p, true );
			} catch (Exception e) { /* ignore — host keeps authoritative reward */ }
		}
	}
}
