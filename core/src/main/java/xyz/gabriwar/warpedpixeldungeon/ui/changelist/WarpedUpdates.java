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

package xyz.gabriwar.warpedpixeldungeon.ui.changelist;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aplomb;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Banner;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CreepingDread;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DivineWrath;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DreadHowl;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ImpalingThrust;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MassHeal;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MeteorCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.RighteousStrikes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Spark;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Stealth;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SummonCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WarCry;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.sprites.FortuneTellerFolkSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ChaosMageSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DwarfKingTombSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MimicSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PortalGateSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SnakeSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.TowerSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Every Warped update a player sees, newest first: the Changes screen's version tabs (one per
 * major version, a square per version, addMajors), and the rows the patched screen shows (WelcomeScene) for whatever is
 * newer than the version the player last ran. Every change that reaches players gets an entry here
 * (CLAUDE.md): its own icon, a short title, a one-line summary for the patched screen and the
 * fuller text for the Changes screen. Nothing about the debug menu ever goes in.
 *
 * `since` is the first version code the entry ships in (any later code also has it): a version
 * code is the commit count plus 79 (build.gradle), so an entry written now takes the next one.
 */
public final class WarpedUpdates {

	private WarpedUpdates(){}

	public static final class Entry {
		public final int since;
		public final Supplier<Image> icon;
		public final String title, summary, body;

		Entry( int since, Supplier<Image> icon, String title, String summary, String body ){
			this.since = since;
			this.icon = icon;
			this.title = title;
			this.summary = summary;
			this.body = body;
		}
	}

	private static Supplier<Image> item( int sprite ){
		return () -> new ItemSprite( sprite );
	}

	private static Supplier<Image> icon( Icons icon ){
		return () -> Icons.get( icon );
	}

	//the branch stairways' own art, the temple's (custom_tiles/branch_stairs.png, tile 1)
	private static Image branchStairs(){
		Image i = new Image( Assets.Environment.BRANCH_STAIRS );
		i.frame( i.texture.uvRectBySize( 16, 0, 16, 16 ) );
		return i;
	}

	//the portal gate as the town's always stands: lit
	private static Image litGate(){
		PortalGateSprite s = new PortalGateSprite();
		s.onStateChange( PortalGate.State.DEAD, PortalGate.State.ACTIVE );
		return s;
	}

	/** Newest first. */
	public static final Entry[] ALL = {
		new Entry( 10999, item( ItemSpriteSheet.LOG ), "Wind in the trees",
				"Trees, bushes and tall grass now sway with the wind, the gusts sweeping through the woods.",
				"Out on the surface, trees, bushes and tall grass now move with the wind. A pine's top and an oak's crown sway sideways while their trunks stand still, and bushes and tufts of tall grass sway at the top. Nothing moves in still air, a breeze stirs them a pixel either way, and in a strong wind or a storm the trees lean downwind and bend. The gusts run across the land as a wave, so you can watch them sweep through a forest." ),
		new Entry( 10999, item( ItemSpriteSheet.WAND_LIGHTNING ), "Lightning finds the trees",
				"Storm bolts now strike trees too, and a struck tree burns down from its crown.",
				"In a storm under the open sky, tall trees draw the lightning: when an oak or a pine stands within reach, about half of the bolts strike one of them instead of the open ground. The bolt lands on the tree's crown, which catches fire and burns for several turns in tall flames, embers and a column of smoke before the tree falls to embers and leaves charcoal. A bolt never sets a tree alight in town or beside anyone. Any tree a fire burns now shows its crown alight the same way." ),
		new Entry( 10999, item( ItemSpriteSheet.SEED_SUNGRASS ), "Light and shadow on the land",
				"Cloud shadows drift over the land, sunlit motes hang over meadows and birds cast shadows.",
				"When scattered clouds cross the sky, their shadows now drift over the surface with the wind, across the ground, the trees and everyone on it; there are none under a clear sky, at night, in rain or in fog. On clear or fair days of spring and summer, motes of warm sunlight hang over the grass and drift on the breeze. Crows, gulls and finches cast a small shadow on the ground beneath them that fades as they climb. Falling leaves, dust, ash, fireflies and butterflies now drift the way the wind really blows instead of always to the east." ),
		new Entry( 10999, item( ItemSpriteSheet.TORCH ), "A homestead that breathes",
				"Your lit kiln, forge and hearth smoke, breath shows in the cold, and built pieces drop into place.",
				"A lit kiln, forge or hearth in your homestead, and the coals of a campfire, now send up a gentle column of smoke that leans with the wind. Under the open sky in air below freezing, your hero's breath shows in small puffs, even before the cold sets in, and in co-op you see the other heroes' breath too. Every piece you build now drops into its spot and settles as it lands." ),
		new Entry( 10985, () -> new SkillSprite( new Sanctuary().image() ), "Sanctuary's holy seal",
				"Sanctuary's ground is a calm seal of holy light; Transcendence and Charged Shot look as they used to.",
				"Sanctuary's consecrated ground is now a single seal of holy light on the floor around the cleric: a gold rim, a ring of runes with beads of light between them, an inner ring and a star of light at its heart. It no longer flips or flickers. It breathes gently, and a soft light passes around the ring, brightening each rune in turn. When the circle is consecrated it opens and its runes are written in one after another, walls cut it where they stand, and when it ends it fades away. Raising the ward no longer spins a glyph under your feet, and the runes it raises keep their shapes. Allies healed inside it still show the healing crosses. Transcendence is back to its gold streaks as in version 10924, and Charged Shot's release is again a thin streak of light instead of a thick beam." ),
		new Entry( 10981, () -> new SkillSprite( new RighteousStrikes().image() ), "The holy light leaps again",
				"Righteous Strikes, Retribution and Regeneration look as they did before version 10924.",
				"Righteous Strikes, Retribution and Regeneration are back to the look they had before version 10924. Righteous Strikes' gathered light bursts as a golden star and leaps from enemy to enemy as rays of holy light, coming home to the cleric at mastery. Retribution's great debt bursts on as a ray of light. Regeneration shows the healing crosses as it grows and a green star on a second wind. Gold motes now stream along each ray, light rises off the cleric as the light comes home, and green light rises beside the second wind's crosses." ),
		new Entry( 10981, () -> new SkillSprite( new Spark().image() ), "Colours that mean something",
				"Skill effects take their colours from what they do, and skill bolts fly as dense comets of light.",
				"Every skill effect now takes its colours from what it does: healing is green light with red crosses, blood and rage are crimson, fire is orange, frost is pale cyan, poison is a sickly green, shadow is violet-black, holy light is gold, the arcane is blue-violet, mana is sky blue, storms are white-blue and the huntress's skills are leaf green. The mage's spells no longer share the rogue's lilac. Skill bolts fly as dense comets of light with a bright head, stepping down pixel by pixel as they fade, and pulses start in their own colour instead of a white flash." ),
		new Entry( 10981, () -> new SkillSprite( new ManaShield().image() ), "Wards and spells speak in runes",
				"Spirit Volley throws its rune bolts again, and the mage's wards and spells now show runes.",
				"Spirit Volley throws its purple rune bolts again, and Spirit Armor's ring turns to runes when it is full. Raising a mage's ward locks a ring of mana runes round your feet, and they fade if you step off them. Mana Shield's runes lock in when it is raised and on the side a blow comes from, scatter when it breaks and lock in again when it reforms. Spell Blade writes a rune on the enemy it strikes, Arcane Echo's runes lock round the struck and lift away, Sorcerer's overload spins a ring of runes that bursts outward, and Hex's runes snap shut on the cursed. The cleric's Condemn stamps a holy seal, Sanctuary raises gold runes, Aegis's arc carries two and Litany's verse raises a ring of them; Weapon Bond locks steel runes at the blade. Every cast also shows its own kind: fire for Meteor Call, holy light for Transcendence, steel and earth for the stances, crimson embers for blood and rage, and the other classes' skills glow in their class's own colour." ),
		new Entry( 10981, () -> new SkillSprite( new DoubleStab().image() ), "Links pulled from both ends",
				"Shadow Link draws its bond out of both enemies; drains, rays and spirit arrows carry dense light.",
				"Shadow Link's bond is now drawn out of both enemies: a current of shadow runs from each body into a knot that snaps as it binds. A linked pair stays joined by a thread of shadow with a small cut flickering where it meets, instead of a pink rope, and a lone link shows a violet ring at its feet instead of a target. Blade Bind pulls steel from both blades into a knot the same way. Souls and fear are seen peeling out of the body they leave. Charged Shot looses inside a ray of leaf-green light, Avatar of Light sears and Transcendence's split lance cuts with rays of gold, spirit arrows and holy blades leave a trail of their light, Holy Charge leaves gold dust and True Edge throws sparks. Cinder Trail's orbs trail fire or frost. Madness and terror are crimson, Venom drips from the blade, and Dark Pact is sealed by a ring of blood runes and a ring of mana runes turning the two ways round your feet. Blink crackles with storm light as it casts." ),
		new Entry( 10981, () -> new SkillSprite( new MassHeal().image() ), "Heals look like heals",
				"Heals glow green with red crosses; blood and rage are red, fire orange, mana blue.",
				"Healing is now green light with red crosses. Mass Heal's and Litany's mends flow to each ally as a green ray and land as healing crosses, Healing Prayer's pulse is green, and Skill Sequence's dawn shows the crosses on everyone it mends. Purify sends a short ray of holy light to each ally. Health gained from Soul Drain (once its soul reaches you), Avatar of Light, a shards mark, Sanctuary's ground and Blackout's mended shadows shows the same crosses, only when health really rises. Faith, Meditation, Spirituality and Inner Peace draw blue motes of mana into you when they give mana, Grace's mote of light lands as a bloom of mana, and every mana number is the same blue. Crimson Waltz and Heartseeker are blood red from end to end instead of pink, Ember Arrows burns orange, Frost Arrows glints with frost instead of a mana sparkle, the huntress's stances and Double Shot's mark are leaf green, and Frost Nova throws shards of ice and leaves rime under those it catches." ),
		new Entry( 10981, () -> new SkillSprite( new Aplomb().image() ), "No more falling stars",
				"Skills no longer throw stars off the screen: dazed foes twinkle in place, blades throw sparks.",
				"Skills no longer throw yellow stars that fly off the screen. A foe that is really dazed, dizzy or held now twinkles in place over its head, in the same amber as the game's stun: Aplomb, Dragon Kick, Lunge, Sidestep, Sixth Sense, Piercing Focus, Pirouette, Head Shot, Iron Stance, Last Stand and Scorpion. A boss that Last Stand cannot hold only shudders. Blows that daze no one throw steel sparks, parries, reads and marks glint in steel, and the hero's own successes show his sparkle. The Trumpet's fanfare blesses in holy gold and the foes it holds twinkle like any stunned foe; the Wardrum's frightened foes shudder with fear instead of flashing red alarm marks. Fight Gloves' jabs throw sparks off their metal tips. Accuracy's bullseye flashes in steel, and Second Wind, which restores weapon charges rather than health, now shines in steel instead of a healing green." ),
		new Entry( 10924, () -> new SkillSprite( new ImpalingThrust().image() ), "Impaling Thrust's lance of light",
				"Impaling Thrust no longer slides a spear along the floor: a lance of light thrusts from your hand.",
				"Impaling Thrust used to show an upright spear sliding along the ground, which looked like an item lying there. Now a lance of light thrusts from your hand down the lane at chest height, pierces each enemy in its path with a flash and a few sparks, and pulls back. At +3, the ghostly lances that follow stab once in each tile, one after another, instead of running together into one beam." ),
		new Entry( 10924, () -> new SkillSprite( new WarCry().image() ), "War Cry roars, no alarm",
				"War Cry no longer shows an alarm trap's red marks: rings of force roll out and enemies flinch.",
				"War Cry used the same red brackets as an alarm trap going off, so it looked like a trap had been triggered. Now rings roll out from your feet with lines of force bursting outward, you light up for a moment, and every enemy it reaches shudders and flinches back, shown by a fading double image. At +3 the double images are darker, as the cry saps their strength." ),
		new Entry( 10924, () -> new SkillSprite( new CreepingDread().image() ), "Creeping Dread creeps in",
				"Creeping Dread no longer flashes an alarm and a curse: a violet ring closes in and ink pools at the enemy's feet.",
				"When Creeping Dread frightened an enemy it showed an alarm's red brackets and a curse's dark smoke rising. Now a violet ring closes in on the enemy's feet, ink crawls toward it and pools beneath it, and it shudders. At +3 the fear creeps from that enemy to each one standing beside it, one after another." ),
		new Entry( 10924, () -> new SkillSprite( new DreadHowl().image() ), "The rogue's own shadows",
				"Fear, shadow and soul skills have their own looks instead of curses, chains and wand bolts.",
				"Dread Howl and Demoralize show rings and shudders instead of alarm marks, and maddened enemies get a crimson ring. Hex flies as its own violet bolt instead of a wand's. Shadow Link draws a sagging line of ink instead of ethereal chains, Ambush drives a violet stake of shadow beside the target, and Silent Death and Vanish leave pools of ink. Enemies that lose your trail after Vanish show the usual lost-trail mark. The Stealth decoy is a violet shade of you, and co-op players now see it that way too. Ninja Bomb throws a pellet of ink instead of a bomb, Master Thief's coin flies with a glint, soul skills fly as soft wisps and Summon Skeleton draws its bones together. Aimed Shot pins a shaft of light in the floor, and Double Shot, Bombvoyage and the huntress's stances have their own sparkles." ),
		new Entry( 10924, () -> new SkillSprite( new Banner().image() ), "Warrior and duelist in steel and earth",
				"Warrior and duelist skills no longer borrow stun stars, alarm marks, chains, runestones or a spear on the floor.",
				"Banner plants a standard of light with a gold flag instead of a spear lying on the floor. Firm Hand's stagger is a ring rocking under the enemy's feet instead of the stars of a real stun, so a stagger and a stun no longer look alike. Blade Bind ties you and the enemy with two lines of steel light instead of the Ethereal Chains. Deadfall drops one counterweight on its rope and the rest of the blow comes up from the ground, instead of blast runestones. Undying Will roars in fury instead of flashing alarm marks, and Crimson Waltz is crimson light circling you instead of blood on the floor, since it costs mana, not blood. Rocks became chips of stone thrown up from the ground, forge and electric sparks became steel sparks, and True Edge cuts with a stroke of steel instead of a beam." ),
		new Entry( 10924, () -> new SkillSprite( new SummonCrab().image() ), "Your summons are marked",
				"Your summoned allies now carry two motes in your color, so they never look like hostile monsters.",
				"A summoned rat, crab or skeleton looks just like the monster it copies. Every ally you summon now has two small motes of light in your class color circling over its head, so in a fight you can tell it from the hostile ones. A summon turned against you shows none." ),
		new Entry( 10924, () -> new SkillSprite( new DivineWrath().image() ), "The cleric's holy light",
				"Cleric skills are holy light now: no fire on you, no shield items, no beams between creatures.",
				"Divine Wrath and Fervor no longer look like you or the floor catching fire: they glow with holy motes. Divine Wrath's hallowed ground has its own look, a halo lying on each tile with a crest of light rolling across them, so it no longer matches Sanctuary's circle. Aegis and Shield of the Faithful raise an arc of light on the side the blow comes from, instead of a round shield item and the shielded bubble. Sacred Weapon's swords are blades of holy light standing in the floor instead of the weapon item. Holy Charge, Last Rites, Litany and the other healing and smiting skills use strokes, motes and pillars of light instead of beams from one creature to another, and Grace and Scouring Flame fly holy bolts instead of a wand's. Crusader's Zeal has its own gold cross icon instead of a tinted flame." ),
		new Entry( 10924, () -> new SkillSprite( new ChargedShot().image() ), "The huntress's own arrows",
				"Huntress skills shoot arrows of light and drop the crosshairs, hearts and trap items they borrowed.",
				"Charged Shot marks its lane with a line of dots of light instead of crosshairs like a monster's warning, and releases along the arrow's own path. Arrow Storm looses arrows of light that rain down, Heartseeker beats in crimson instead of showing charm hearts, and Fletching throws feathered darts. Bear Trap throws a steel snare that springs shut, with no stun stars, since nothing is stunned. Wild Call's wolf now lunges and bites in grey light, and Ember and Frost Arrows glow with embers and frost glints instead of looking like you are burning or chilled." ),
		new Entry( 10924, () -> new SkillSprite( new MeteorCall().image() ), "Meteors, storms and arcane",
				"Meteor Call's mark gathers heat instead of red alert specks, and a stone of fire falls instead of a bomb.",
				"Meteor Call's target no longer flashes red alert specks: heat gathers on the tile, with a breathing glow, a ring closing in and embers rising, until the stone falls. It comes down as a burning stone instead of a bomb and strikes with cracks, a scorch, flying chips of stone and a wave of heat; flames appear only where something really burns. The lightning of Spark, Blink and Storm Call is now pale cyan, and Arcane Echo's and Sorcerer's is violet, so it no longer looks like a shock or a wand's bolt. Spark and Wizard's ricochet fly their own bolts, and Mana Shield breaks in a blue flash and ring." ),
		new Entry( 10924, icon( Icons.WARNING ), "A freeze in big fights",
				"Fixed a freeze that could stop the game when something struck in a very busy fight.",
				"When a fight was already full of effects, a strike that tried to throw sparks with no room left for them could get stuck making them forever and freeze the game. It now simply skips the sparks." ),
		new Entry( 10874, () -> { Image i = new Image( Assets.Effects.TEXT_ICONS ); i.frame( FloatingText.iconFilm.get( FloatingText.PHYS_DMG ) ); i.scale.set( 2f ); return i; }, "No black box behind icons",
				"A skill or buff icon on a hit card no longer sits on a black square.",
				"When a skill fired or a buff took hold, its icon on the hit card over the creature sat on a dark square that hid what was behind it. The icons now show on their own, with nothing drawn behind them, on the cards and in the Damage tab's list alike." ),
		new Entry( 10873, () -> { Image i = new Image( Assets.Effects.TEXT_ICONS ); i.frame( FloatingText.iconFilm.get( FloatingText.PHYS_DMG_NO_BLOCK ) ); i.scale.set( 2f ); return i; }, "Falls in the Damage tab",
				"A fall now shows as Fall in the Damage tab, with a line on what it does.",
				"Damage from falling into a chasm showed as \"Something\" in the Damage tab. It now reads Fall, and opening it explains the landing: at least half the health you had left, armor does not stop it, it leaves you crippled and bleeding, and armor with sure footing makes it hurt less. Haunted remains, a falling tree and a cave rift's flare are named and explained the same way. Falls you already took are renamed too." ),
		new Entry( 10872, () -> { Image i = new Image( Assets.Effects.TEXT_ICONS ); i.frame( FloatingText.iconFilm.get( FloatingText.PHYS_DMG ) ); i.scale.set( 2f ); return i; }, "Every hit shows its damage",
				"Hit cards no longer come up blank: every hit shows its damage again.",
				"After the first moments of a fight, most hit cards were drawn empty: a card reused from an earlier hit lost its number, its icon and its small icons, so many hits seemed to show no damage at all. Every card now draws in full. Quick hits that add up on one card flash the number white on each hit, so you see each one land. A number in a busy spot no longer vanishes before you can read it, damage shared through a link that finishes a creature shows its number, a hit on a creature that steps out of sight keeps its card, and damage turned back on an attacker shows once instead of twice." ),
		new Entry( 10872, () -> { Image i = new Image( Assets.Effects.TEXT_ICONS ); i.frame( FloatingText.iconFilm.get( FloatingText.MISS_DODGE ) ); i.scale.set( 2f ); return i; }, "Words became icons",
				"Misses, skill shouts and effects now show as icons on the hit's card instead of floating words.",
				"Fights no longer fill with floating words. A miss shows a card with an icon for a dodge, a block, a parry or a guard, and the reason it missed. A skill that fires puts its own icon on the card of whoever it struck, so a shout like Creeping Dread's becomes its icon, followed by the fear it causes. Effects that land just after a blow join its card, or show as a small card of icons when there is no blow. Immunity, a resisted effect, a monster's rage, a cursed weapon's fuse and a quick reload have their own icons, counts such as fury or stone plates show as an icon with a number, and heals and mana from skills show as a number with their icon. Five effects that had no icon now have one in the buff bar: Gap in the guard, Form read, Pickpocketed, Demoralized and Marked. A small card now shows every icon it carries, not only the first. Every effect is still named in the buff bar and in a monster's window, and the new Damage tab names every icon of your hits." ),
		new Entry( 10872, icon( Icons.DAMAGE ), "Damage tab",
				"The hero window has a Damage tab: your last 100 hits dealt and taken, step by step.",
				"Tap your portrait and open the new Damage tab. It keeps the last 100 hits you dealt and the last 100 you took, misses included, each shown as the card you saw. Open one to see how its number came to be: the roll and every bonus, multiplier, armor and resistance in the order they applied, with the running total ending on the damage done, the chance it had to hit, health and shield before and after, and the buffs both of you had at that moment. A summary on top shows your average, your best or worst hit, your crits, how often you hit or dodged and how much your armor and shields kept off. The history is saved with your game." ),
		new Entry( 10846, () -> { Image i = new Image( Assets.Effects.TEXT_ICONS ); i.frame( FloatingText.iconFilm.get( FloatingText.CRIT ) ); i.scale.set( 2f ); return i; }, "The crit X sits by the damage",
				"A critical hit's red X now lands beside the damage number instead of over the creature.",
				"A critical hit's red X used to strike across the middle of the creature and then sit again beside the damage. It now shows once, right beside the damage number on the hit's card, level with it and at its size, with a short white flash, so the creature stays clear to see. The X itself is simpler too: thin red strokes instead of a boxed X." ),
		new Entry( 10844, item( ItemSpriteSheet.EMPTY_BOTTLE ), "Empty bottles look empty",
				"The empty bottle is now clear glass, with no grey liquid inside.",
				"The empty bottle was drawn with a grey liquid in its bottom, so it looked half full. It is now clear, see-through glass." ),
		new Entry( 10844, icon( Icons.INFO ), "Three descriptions fixed",
				"The slippery, bow fatigue and flail spin descriptions show their numbers again.",
				"The descriptions of being slippery, of bow fatigue and of a spinning flail showed raw codes instead of their numbers. They now read properly." ),
		new Entry( 10844, item( ItemSpriteSheet.BANDOLIER ), "Bottles in the bandolier",
				"The potion bandolier now holds empty bottles too.",
				"Empty bottles now go in the potion bandolier with your potions and bottles of water, so they no longer take up room in your backpack." ),
		new Entry( 10840, item( ItemSpriteSheet.BOTTLE_OF_WATER ), "Pour water",
				"Pour a bottle of water beside you or on yourself: it puts out fire, soaks and leaves a puddle.",
				"A bottle of water can now be poured on a tile beside you, or on yourself, and you keep the bottle. It puts out fire there, soaks whoever stands there and leaves a puddle on bare ground. Thrown, it does the same on the one tile it lands on, instead of flooding everything around. Being soaked now also saves you from the next fire that would set you alight: it only dries you off." ),
		new Entry( 10840, item( ItemSpriteSheet.EMPTY_BOTTLE ), "Potions need bottles",
				"Brewing a potion from seeds takes a bottle, and drinking a potion gives the bottle back.",
				"Three seeds still brew a potion at the alchemy pot, but the pot now takes an empty bottle or a bottle of water from your pack for it. Every potion you drink leaves its empty bottle behind, so the bottles keep coming back to you." ),
		new Entry( 10839, item( ItemSpriteSheet.SWORD ), "Slashes land on their target",
				"A blow's slash, star and other effects now appear on the creature they hit.",
				"A blow's effects were drawn for their first moment half their size down and to the right of where they belong, so a slash often looked like it missed the creature it hit. The slash, the star and every other effect of a hit now appear on their target from the start." ),
		new Entry( 10826, icon( Icons.TARGET ), "One card per hit",
				"A hit now shows one card with its damage and small icons, and its effects one after another.",
				"Every blow now shows one card over whoever it struck: the damage, and above it small icons for what else happened, such as an enchantment that fired (hollow when it was a weapon's vestige), something the weapon's type did, a bonus from a line on your gear, or an effect it left. Quick hits on the same enemy add up on the same card with a count. Each blow plays its main effect first and every other one a moment later and a little higher, so a busy hit no longer piles its effects and words on one spot, and nothing it showed before is left out. Health a blow drains shows on you when it reaches you, and the rival statue's shouts are icons now too." ),
		new Entry( 10826, () -> { Image i = new Image( Assets.Effects.TEXT_ICONS ); i.frame( FloatingText.iconFilm.get( FloatingText.CRIT ) ); i.scale.set( 2f ); return i; }, "A critical hit is a red X",
				"Critical hits show a small red X instead of a word.",
				"A critical hit no longer writes a word over the target: a small red X strikes across it, and the same X sits beside the damage on its card. True Edge's critical hits show it too." ),
		new Entry( 10826, item( ItemSpriteSheet.HAND_AXE ), "Axes, hammers, instruments and whips",
				"Four new weapon types: Woodsman, Quarry, Bard and Lash.",
				"Axes are now Woodsman: each hit on the same enemy splinters its armor, they fell and chop in fewer strokes and sometimes give an extra log, and at alpha every third hit on one enemy knocks it back. Hammers, picks and shovels are Quarry: hits can daze, they break stone and dig in fewer strokes and sometimes bring up one more stone or ore, and at alpha a dazing blow cripples the enemies around. The war hammer, meister hammer and miner's tool now break boulders, and Crom Cruach's axe fells trees. Instruments are Bard: every fourth note plays a chord that blesses you and your allies, then one that frightens nearby enemies, and at alpha it also charms the weakest enemy near you. Whips are Lash: hits from two tiles away deal more and can drag the enemy toward you, and at alpha a hit can knock its grip loose so its next blow is weaker. Flails, ambush blades, fight gloves, the forest dart and the kunai, bolas and small chakram moved to the types that fit them." ),
		new Entry( 10826, item( ItemSpriteSheet.TYPE_SHIFTER ), "Weapon types tuned",
				"Momentum, Reach, Marksman, Gunner and Guard are stronger, and their texts are true.",
				"Momentum now counts from the first hit, peaks at the fourth, and at alpha carries its stacks to the next enemy after a kill. Reach is a flat bonus from two tiles away. Marksman and Gunner reach their full bonus at 5 tiles. Guard softens more of each blow. The Flurry, Wild, Keen, Siphon, Tips and Bulwark descriptions now say exactly what they do. Instruments' songs no longer hurt your allies or bystanders. The greatsword's and royal spork's descriptions are corrected." ),
		new Entry( 10826, item( ItemSpriteSheet.MASTERWORK_CORE ), "Traits",
				"Uncommon gear rolls a trait that fits it; rare and better a trait and a plain bonus.",
				"Lines on weapons and armor are now plain bonuses or traits. A common piece carries a plain bonus, an uncommon piece a trait, and a rare or better piece one of each. Every weapon can roll critical hits or life drain as its trait, and each kind of weapon adds two traits of its own; light and heavy armor each have their own. The catalog in your journal lists the traits each kind can roll. Gear you already own keeps its lines." ),
		new Entry( 10826, item( ItemSpriteSheet.WOODCUTTERS_AXE ), "New weapon traits",
				"Warm grip, slayer, executioner, moonlit, stormborn, defiant, hunter and steady aim.",
				"Warm grip keeps you up to 3 degrees warmer in the cold while you hold it. Slayer deals up to 30% more damage to one family of monsters, named on the weapon: the undead, demons, constructs, elementals, beasts or humanoids. Executioner deals up to 15% more to enemies under half health. Moonlit deals up to 15% more damage and aims truer at night, half that at dusk and dawn, and a little less around noon. Stormborn deals up to 25% more while rain, snow or hail falls on you. Defiant deals up to 30% more while you are under half health. Hunter gives up to a 25% chance that a beast you kill leaves meat. Steady aim takes up to half of the wind's toll off your shots and throws. Masterwork raises them further." ),
		new Entry( 10826, item( ItemSpriteSheet.ARMOR_LEATHER ), "New armor traits",
				"Fleet, hushed, rainproof, cooling, windbreak and sure footing; warmth now fights only the cold.",
				"Light armor can roll fleet (up to 8% quicker steps), hushed (harder to notice, and wet boots make no sound), rainproof (rain takes a few turns to soak you and puddles soak your boots less often) or cooling (up to 5 degrees cooler in the heat). Heavier armor can roll windbreak (up to half the wind's chill), sure footing (falls hurt up to 40% less) or resolve (a better chance to shake off harmful effects). Masterwork raises them further, though fleet stops at 10%. The warmth line on armor now only warms you in the cold, so it no longer adds to the heat in summer." ),
		new Entry( 10730, icon( Icons.INFO ), "Item windows scroll and fold",
				"Long item descriptions now scroll, and their parts fold shut.",
				"An item's window no longer runs off the screen: its text scrolls when it is too long. On gear with a rarity, the description, the rarity and the type each have a header with its icon and name in its colour: the rarity and type open folded, and a tap on a header opens or folds that part. The other rarities and types moved to the journal's catalog: open a weapon, armor, wand, ring or trinket there to see what each rarity and type would give it before you reroll or shift it." ),
		new Entry( 10730, () -> new ItemSprite( ItemSpriteSheet.SWORD, new Shocking().glowing() ), "Enchantments come alive",
				"Every enchantment and glyph now has its own look when it strikes.",
				"Every enchantment, glyph and curse now shows its own effect when it acts: fire licks up the target, frost locks it and cracks, lightning forks to the floor, venom eats into the ground, drains pull health home, wards lock into runes in front of the wearer. The hit's star takes the enchantment's colour, and a blow its enchantment kills ends with a flourish of its own. Stunned creatures show stars circling their heads and charmed ones floating hearts, a curse shows its bite as it turns on you, and co-op partners see all of it too." ),
		new Entry( 10730, () -> new ItemSprite( ItemSpriteSheet.ARMOR_LEATHER, new Brimstone().glowing() ), "Gear that looks enchanted",
				"Enchanted gear shows its magic in your bag, on the floor, in flight and in hand.",
				"An enchanted piece now glows with its enchantment's own colour and motes in your bag and in its item window, and a piece with two enchantments shows them in turn. Enchanted items on the floor glow at night and flare the first time you see them. Thrown weapons and shots trail their magic through the air, and a weapon in hand sheds its magic as you go and lights you up at night." ),
		new Entry( 10730, item( ItemSpriteSheet.STONE_ENCHANT ), "Gaining an enchantment",
				"Enchanting now plays out in the enchantment's own colours and motion.",
				"Scrolls and stones of enchantment, styluses and infusions now show the enchantment being bound into the piece in its own colours and motion, and so does every step at the enchanting pedestal: revealing, weaving, empowering, rerolling and unweaving. Identifying a cursed piece or a vestige shows what it holds, and picking up a piece of enchanted gear for the first time makes it glow." ),
		new Entry( 10730, () -> new ItemSprite( ItemSpriteSheet.SWORD, new Blazing().glowing() ), "Vestigial enchantments",
				"Legendary and exotic weapons and armor carry a faint vestige of another enchantment or glyph.",
				"Every legendary and exotic weapon now carries a vestige: a faint extra enchantment that triggers a fifth to three tenths as often as the real one would, and up to three fifths as often once the weapon is fully masterworked. Legendary and exotic armor carries one too, a vestigial glyph that answers blows at the same rates. A vestige takes no socket at the pedestal and stays silent while the piece carries the same enchantment or glyph for real. At the tempering forge it is the last line to burn out. Legendary and exotic weapons and armor you already own gain one the first time you load your game." ),
		new Entry( 10730, item( ItemSpriteSheet.MASTERWORK_CORE ), "New weapon lines",
				"Weapons can roll critical hits and life drain; guns and bows save ammo and reload.",
				"Five new bonus lines for weapons. Every weapon and thrown weapon can roll a chance to land a critical hit for half again the damage, or a chance to drain a third of a blow back as health. Guns and the bow can roll a chance that a shot spends no ammunition; guns (the flamethrower, laser gun, minigun and technology shotgun) a chance that a reload takes no time; and the Reinforced Flamethrower and Reinforced Laser Gun extra range." ),
		new Entry( 10730, item( ItemSpriteSheet.TYPE_SHIFTER ), "Weapon types reworked",
				"Guns, boomerangs and scythes get perks of their own; heavy throws can be heaved.",
				"Guns get the Gunner perk: shots at enemies three or more tiles away deal more damage and some reloads take no time, and at alpha one shot in five spends no round. Boomerangs get Arc: a throw flies on through more enemies and strikes those in its way back, and at alpha it returns after 2 turns. Sickles and war scythes leave Wild for Reaper: every hit sweeps the other enemies beside you, a carried scythe sometimes doubles the seed of a crop or tuft of tall grass you cut, and at alpha swept enemies bleed; the war scythe now harvests and reaps as the sickle does. Heavy thrown weapons can now be heaved: a two-turn throw for 50% to 150% more damage, with a shockwave at alpha. Heave replaces Impact's bonus against an untouched target. Keen throws of light thrown weapons now fly back to your hand, and alpha ones reach an enemy through walls up to 3 tiles away. Light blades (Ambush) let you sense enemies through walls within 1 to 3 tiles while you hold them." ),
		new Entry( 10730, item( ItemSpriteSheet.FT_T5 ), "Guns and bows use their magic",
				"Gun shots trigger the gun's enchantments; bows fire both of theirs.",
				"Shots from the flamethrower, laser gun, minigun, technology shotgun and arm cannon now trigger the launcher's enchantments, and the bow's arrows trigger both of its enchantments instead of only the first. Bow, flamethrower and laser gun shots now get their weapon's rarity damage and damage lines, and every shot its launcher's accuracy line and armor pierce." ),
		new Entry( 10649, icon( Icons.WARNING ), "Co-op crash fixed",
				"Fixed a crash for a co-op guest arriving out in the world.",
				"Fixed a crash that could close the game for a co-op guest as the world above opened. Crash reports now also say which version crashed, on what device and where in the game, so fixes come faster." ),
		new Entry( 10648, item( ItemSpriteSheet.SCROLL_HOLDER ), "Scholars' shelves",
				"Searched bookshelves give a scroll 8% of the time, and only five useful kinds.",
				"A bookshelf emptied by a search now holds a scroll 8% of the time instead of 10%, but always a scholar's kind: Identify, Remove Curse, Recharging, Transmutation or Magic Mapping." ),
		new Entry( 10646, icon( Icons.LANGS ), "Back to English",
				"This update starts the game in English once; switching language now asks first.",
				"This update starts the game in English once, since Warped is written in English and much of it is not translated yet. You can pick another language again in the settings, and it stays; changing the language now asks you to confirm first." ),
		new Entry( 10646, icon( Icons.MAGNIFY ), "Identifying three times a week",
				"The hermit identifies 3 items free a week; the fortune teller reads 3 a week.",
				"The hermit in his mountain hut now identifies three items for free each week instead of one each day, then charges his usual fee. The town's fortune teller now reads three items a week for her price instead of one a season. Both tell you how many are left this week." ),
		new Entry( 10646, icon( Icons.WARNING ), "A crash fixed",
				"Fixed a crash when a monster picked its target as another one appeared.",
				"Fixed a crash that could close the game while monsters were choosing whom to fight, when a creature appeared or left at the same moment." ),
		new Entry( 10645, item( ItemSpriteSheet.MASTERWORK_CORE ), "Masterwork in five steps",
				"Masterwork is 5 steps of a core each; the last doubles every bonus line.",
				"Masterwork now takes five steps, one core each. Each of the first four lifts every rarity bonus line by 15%, and the fifth, which still needs a legendary or exotic piece, doubles them. A fully masterworked piece stays fully masterworked, and the steps of one partly done keep their place." ),
		new Entry( 10644, item( ItemSpriteSheet.MASTERWORK_CORE ), "Masterwork in three steps",
				"Masterwork is 3 steps of a core each; the last doubles every bonus line.",
				"Masterwork now takes three steps, one core each. The first and the second lift every rarity bonus line by a quarter, and the third, which still needs a legendary or exotic piece, doubles them. Gear masterworked before keeps its place: steps one to five count as the first step, six to nine as the second, and the tenth as the third." ),
		new Entry( 10644, item( ItemSpriteSheet.STYLUS ), "Weaves are not kept",
				"Three weaves left unchosen at the pedestal are lost.",
				"The enchanting pedestal no longer keeps three weaves you paid to see: take one or they are gone, and seeing three more costs 20 again. Closing them unchosen still asks first." ),
		new Entry( 10643, item( ItemSpriteSheet.STYLUS ), "A new enchanting pedestal",
				"The pedestal is remade: socket cards, and 20 energy shows three new weaves.",
				"The enchanting pedestal is remade. Your piece rests in a ring of glyphs in its enchantments' colours, and each of its two sockets is a card: the enchantment or glyph named in its own colour, its level as pips, what it does, and Empower, Reroll and full details at hand. Weaving a new one or rerolling one now costs 20 energy to see three, shown side by side with what each does, and the one you take is free. Leave without taking one and they wait on the piece, free to come back to." ),
		new Entry( 10642, item( ItemSpriteSheet.SWORD ), "Strength lines that work",
				"A strength line takes off 1, or 2 on a good roll, on every weapon and armor.",
				"A bonus line that lowers the strength a piece needs now takes off what it says: 1, or 2 when it rolled at least half. It never reads -0 any more, and the guns, bows, Greataxe and other pieces that ignored the line now get it too." ),
		new Entry( 10642, icon( Icons.ALERT ), "No more stray squares",
				"Fixed the attack button and rarity frames flashing in the wrong place.",
				"Fixed the attack button flashing in the screen's top-left corner when a monster first comes in reach, a rare item's colored frame and sparks flashing at the wrong spot before its slot settled, and stray dots left over the map after watching a Descent Guide demo." ),
		new Entry( 10636, item( ItemSpriteSheet.MASTERY ), "A guide you fill as you play",
				"Descent Guide pages now write themselves as you learn, starting in the dungeon.",
				"The Descent Guide starts empty. Each First Steps page writes itself the first time its lesson comes up: your first level, wand, key, seed, Scroll of Upgrade, gear too heavy for you, or a fight in a doorway. Nothing writes itself until you first go down into the dungeon. Coming back up writes the world above's pages and its word on the road. A day in the dungeon writes Weather & Seasons, unlocking a hero writes Heroes & Classes, and a first building material or builder's satchel writes the homestead's pages. Pages you already read stay." ),
		new Entry( 10636, icon( Icons.JOURNAL ), "A calmer tutorial",
				"The journal button waits until you pick up the guide's first page.",
				"During the tutorial the journal button stays hidden until you pick up the guide's torn first page, and no other page makes it flash first, so pressing it can no longer break the tutorial." ),
		new Entry( 10635, item( ItemSpriteSheet.BOTTLE_OF_WATER ), "Bottles of water",
				"The potion of water is now a bottle of water, always known; fill empty bottles.",
				"The potion of water is now the bottle of water: always known, in a clear bottle of its own. Every shop sells empty bottles five to a stack; fill one at any water you stand in or beside, or in an alchemy pot, for a bottle of water." ),
		new Entry( 10635, item( ItemSpriteSheet.ARMOR_ROGUE ), "Class armor keeps its quality",
				"The crown and the armor kit keep your armor's rarity and type.",
				"Turning your armor into class armor, with the Dwarf King's crown or an armor kit, now keeps its rarity, its type, its bonus lines and its masterwork. Before, class armor came out plain." ),
		new Entry( 10635, item( ItemSpriteSheet.WOODCUTTERS_AXE ), "Walk over stumps",
				"A tap walks onto a stump; hold the tap to chop it out.",
				"Tree stumps no longer get in your way: a tap on one beside you walks onto it. Hold the tap with an axe in your pack to chop it out for one more log, as a tap did before." ),
		new Entry( 10635, item( ItemSpriteSheet.DWARF_HAMMER ), "Builder's tool in the satchel",
				"The builder's satchel now holds the builder's tool.",
				"The builder's tool goes in the builder's satchel now, with the materials and portable chests, instead of your backpack." ),
		new Entry( 10635, item( ItemSpriteSheet.SEED_ICECAP ), "Clearer ice clusters",
				"The Frozen Caves' ice clusters no longer look like open floor.",
				"The ice clusters that block the way in the Frozen Caves are redrawn as dark stone crusted with blue-white ice, outlined and shadowed, so they stand out from the floor around them." ),
		new Entry( 10635, () -> new ChaosMageSprite(), "Chaos mage drops",
				"Chaos mages drop a Potion of Experience 1 time in 50.",
				"A chaos mage used to drop a Potion of Experience every time it died. Now one does 1 time in 50." ),
		new Entry( 10634, icon( Icons.CHANGES ), "Changes by version",
				"The Changes screen lists Warped's updates by version, in tabs.",
				"Warped's updates are no longer one long list. As in Shattered's history, each version has its own square headed by its number, and each major version (v18, v17 and so on) its own tab. The screen opens on the newest; the WPD tab keeps the overview, and SPD leads to Shattered's history." ),
		new Entry( 10633, item( ItemSpriteSheet.SEED_FIREBLOOM ), "No stuck firelight",
				"A fire's glow no longer stays on you after the flames go out.",
				"Sometimes, after burning ended, the fire's light stayed on the hero for good: a still glow on the body and a pool of light at the feet, with no flames. It happened when a burning character moved a long way from one moment to the next, such as a teleport or the open world's view shifting. The glow now stays with its owner wherever it goes and goes out with the fire. The same holds for a torch's light and the other lights a character carries." ),
		new Entry( 10633, WarpedUpdates::litGate, "A portal gate in town",
				"Dolyahaven's square now has a portal gate that is always lit.",
				"A portal gate now stands in Dolyahaven's town square. It is always lit and costs nothing to use. From it you can travel to any lit shop gate you have tapped, and every lit shop gate now lists the town, landing you beside its gate. Each trip costs 150 hunger, as at any gate." ),
		new Entry( 10633, () -> new SkillSprite( new Stealth().image() ), "A double when you spot a foe",
				"The rogue's Stealth double now rises when you spot a new enemy.",
				"Stealth, the rogue's skill: its smoky double no longer pops up whenever some enemy notices you, even one you cannot see. It now rises beside you the moment you spot a new enemy, once per enemy, and never again while that enemy stays in view. An awake enemy hunts the double first; a sleeping one is left asleep." ),
		new Entry( 10633, () -> new TowerSprite(), "One tower for DM-300",
				"DM-300's arena has a single tower now, with half the health.",
				"Only one tower stands beside DM-300 now, instead of two, and it has 130 health instead of about 260. It still prints broken robots and calls the floor's monsters when hit." ),
		new Entry( 10633, item( ItemSpriteSheet.BUILDERS_SATCHEL ), "Tidier bags",
				"Mr Destructo fits the scroll holder; portable chests fit the builder's satchel.",
				"Mr Destructo, switched on or off, now goes in the scroll holder, and portable chests go in the builder's satchel instead of your backpack." ),
		new Entry( 10633, item( ItemSpriteSheet.CHAIN ), "Portals on the ankh chain",
				"The ankh chain now holds every item that takes you somewhere.",
				"Besides ankhs, rings and keys, the ankh chain now holds the books of the dead, the living and the heavens, the ancient coin, the inscribed bone, the conch shell, the forged SanChikarah, the return beacon, Lloyd's beacon and the vault's escape crystal." ),
		new Entry( 10622, () -> new DwarfKingTombSprite(), "Aim at the tomb",
				"The Dwarf King's tomb can now be targeted.",
				"The Dwarf King's tomb now counts as a foe: tapping it attacks it, and wands, thrown weapons and the attack button aim at it. It still never attacks and takes no damage while the king lives." ),
		new Entry( 10621, item( ItemSpriteSheet.TRUMPET ), "Instruments that play",
				"The trumpet, flute and harp play notes as you use them.",
				"The trumpet, the flute and the harp now play a note with every hit, picked at random from a pentatonic scale, so a fight with one plays a little tune. Their special abilities play a run of notes too." ),
		new Entry( 10620, item( ItemSpriteSheet.TENGU_SHOCKER ), "Bigger Tengu toys",
				"The rogue's shocker and fire bomb last longer and end with a blast.",
				"Tengu's Arsenal, the rogue's skill: the shocker now arcs lightning in all eight directions at once, 2 tiles out, for 6 turns instead of 3, then blows up like the smoke bomb. The fire bomb now sits where it lands and keeps setting the ground around it ablaze for 5 turns; at the skill's last level its flames reach 2 tiles out and it blows up at the end." ),
		new Entry( 10619, item( ItemSpriteSheet.CHAIN ), "Keys on the ankh chain",
				"The ankh chain now holds keys of every kind.",
				"Besides ankhs and rings, the ankh chain now holds every kind of key: the ancient keys that open portals to hidden areas and Tengu's hideout, the dungeon's door keys and the skeleton key." ),
		new Entry( 10610, () -> new FortuneTellerFolkSprite(), "One reading a season",
				"The fortune teller now reads one item each season.",
				"The town's fortune teller identifies one item each season, then asks you to come back when the next season begins; the same season a year later counts as a new one. Her price still follows the moon. She no longer reads your whole pack at once." ),
		new Entry( 10609, () -> new MimicSprite(), "No more monster boxes",
				"The monster box is gone; its chests are mimics now.",
				"The monster box, a plain-looking chest that turned into a monster when opened, is gone. The chests that hid one are now mimics, with the mimic's look and tricks. A monster box left unopened in a saved game is a plain chest now, and one already awake is a mimic." ),
		new Entry( 10608, item( ItemSpriteSheet.NORN_GREEN ), "Green stones for everyone",
				"Every hero can now find green Norn Stones.",
				"Gullin used to drop green Norn Stones only for the huntress. Now every hero finds all five colours, so anyone can forge Jupiter's Wraith, the thrown relic, at the temple altar." ),

		new Entry( 10602, () -> new SnakeSprite(), "A living guide",
				"Guide pages show what they name, alive, and act out how things work.",
				"Every Descent Guide page now shows the monsters, items, places, traps, plants, gases and statuses it names, each under the paragraph that names it: monsters idle and strike now and then, items float and glow, fire and gas pour. Tap one to read what the game says about it. Pages that explain how something works act it out in a small looping scene on the game's own tiles and sprites: Goo's charged slam, DM-300's rockfall, Tengu's traps, Yog's beams and fists, the Demon Lord's breath, a gnoll brute's last rage, a spinner's webs, and every lesson of First Steps. Tap a scene to pause it. The page cards on the guide's map show their subject moving too." ),
		new Entry( 10602, item( ItemSpriteSheet.MASTERY ), "The tome is in the guide",
				"The Tome of Dungeon Mastery is now the guide's first chapter.",
				"The Descent Guide opens with a new chapter, First Steps: the tome's 17 pages, rewritten to be true to Warped and open from the start. When the journal button flashes, press it to read the page that fits the moment, right over the game. The tome, its torn pages and its journal tab are gone, and the title screen's journal has one button fewer. Pages you had read stay read. Lore pages no longer wait until you have found every page of the tome: they turn up from the first floor." ),

		new Entry( 10581, item( ItemSpriteSheet.FOLLOWER_ARMOR ), "Redrawn gear",
				"Thirteen items redrawn to match the rest of the game's art.",
				"The ascetic wraps, follower overcoat, performer's jacket, soldier vest and ceramic armor, the arm cannon and its shot, Jupiter's wraith, the bee arrow, the woodcutter's axe, the mason's hammer, the trumpet, the war drum and the cup of coffee have new sprites: proper outlines, light from the top left and real shading, so they sit beside the other items as equals." ),

		new Entry( 10578, item( ItemSpriteSheet.ARTIFACT_BEACON ), "Lloyd's Beacon in the shop",
				"The first shop, on depth 6, now sells a Lloyd's Beacon.",
				"Lloyd's Beacon could not be found in a run, though Tengu, DM-300 and the Dwarf King each give a carried one a level. The shop on depth 6 now sells one: set it somewhere, and come back to that spot whenever you like." ),

		new Entry( 10577, icon( Icons.AUDIO ), "Stronger, adjustable sound",
				"Stereo and room acoustics are much stronger, each with its own slider.",
				"Sounds now pan much harder to the side they come from and fade sooner with distance, and rooms answer back louder: bigger reverb in halls and caverns, stronger echoes, and blows behind a door or the rock come through duller. Settings, Audio has a strength slider (1 to 5) under each switch: 2 is how they sounded before, 4 is the new default." ),

		new Entry( 10576, icon( Icons.DISPLAY_LAND ), "Play on PC",
				"Every release on GitHub now has a jar for Windows, macOS and Linux.",
				"Each release on the game's GitHub page (GabriWar/warpedpd) now carries a desktop build beside the APKs: one jar that runs on Windows, macOS and Linux. Install Java 17 or newer and open it, or run java -jar on it." ),

		new Entry( 10571, icon( Icons.DEPTH ), "Clearer place names",
				"The place name by the stairs icon is crisp and always fits.",
				"The name of the place you are in, under the stairs icon, is drawn in whole pixels so its letters no longer break up, and a long name (a village's, a homestead's) steps down a size or is shortened so it always fits." ),

		new Entry( 10570, () -> new TowerSprite(), "DM-300's towers",
				"The towers in DM-300's arena can be attacked, and are weaker.",
				"The two towers beside DM-300 can now be attacked like any monster. They have less health (about 260) and armor, and print at most two broken robots at a time, less often." ),
		new Entry( 10570, WarpedUpdates::branchStairs, "Branch stairways",
				"The spider nest, temple and frozen halls stairs have their own look.",
				"The stairs into the spider nest are webbed, the temple's are marble and gold, and the frozen halls' are glazed with ice, so none is taken for the floor's stairs down. Coming up out of a branch now puts you back on its own stairs: leaving the smith's mines no longer drops you on the temple's." ),
		new Entry( 10570, icon( Icons.COMPASS ), "A compass for every staircase",
				"Arrows round your portrait point to every staircase, in colour.",
				"The compass round your portrait now has an arrow for every staircase on the floor you have found: gold for stairs down, blue for stairs up, violet into a branch, green back out of one, white to the surface." ),

		new Entry( 10569, item( ItemSpriteSheet.ACORN ), "Planting the land back",
				"Axes clear bushes and stumps; acorns, pine cones and bush seeds grow.",
				"An axe clears a bush, and chops out a stump so it grows back no more. Felling an oak or a pine sometimes drops an acorn or a pine cone, and clearing a bush a bush seed. Plant them in grass or bare earth: a tree grows in 14 days of spring or summer, a bush in 6." ),
		new Entry( 10569, item( ItemSpriteSheet.TORCH ), "Torches in the ground",
				"Set a torch in the ground: it burns for good and keeps animals away.",
				"Out in the world a torch can be placed in the ground beside you. It burns for good, lights the ground round it and keeps wild animals from settling nearby; tap it to take it back. Burnt trees leave charcoal, and the workbench makes torches from charcoal and a plank." ),

		new Entry( 10564, item( ItemSpriteSheet.SEED_ICECAP ), "Frozen water",
				"Ice now frays into water in its own colours.",
				"Frozen pools in the sewers, prison, caves and mines are tinted from their own water, and the frayed edge where ice meets water uses the ice's own colours, with no pale seam." ),

		new Entry( 10563, item( ItemSpriteSheet.BUILDERS_SATCHEL ), "Homesteads",
				"Build anywhere in the open world: walls, roofs, doors and rooms.",
				"Out in the world you can build anywhere: log, plank, stone and brick walls, windows, plain and barred doors, fences, palisades, paths, bridges and five kinds of roof, from a new build menu. Walls and a roof make a room that keeps out wind and snow." ),
		new Entry( 10563, item( ItemSpriteSheet.WOODCUTTERS_AXE ), "Gathering and crafting",
				"Fell trees, quarry stone, dig clay and craft at stations.",
				"Fell trees for logs, break boulders for stone, dig clay and sand, cut thatch, and hunt for hides. Workbench, mason's bench, kiln, hearth, cooking pot, tanning rack, forge and quern turn them into what you build with." ),
		new Entry( 10563, icon( Icons.SLEEP ), "Rooms, hearths and safe sleep",
				"A lit hearth warms a room; a bed in a shut room is never ambushed.",
				"Rooms take a type from what is in them: bedroom, kitchen, workshop, storehouse, greenhouse, shrine. A lit hearth warms a room, and sleeping in a bed in a closed room is safe and leaves you well rested." ),
		new Entry( 10563, icon( Icons.SEED ), "Farms and animals",
				"Farm through the seasons, and rope animals into pens.",
				"Dig farm plots, water them, and grow food through the seasons; a glass roof keeps crops growing in winter. Rope a deer, goat or bunny and lead it into a fenced pen, where it stays." ),
		new Entry( 10563, icon( Icons.TALENT ), "Homes and waystones",
				"Hearthstones make homes; travel between waystones for hunger.",
				"A hearthstone makes a place your home. Homes draw wolves and bandits, so build defences. Touch a settlement's waystone to attune it, and travel between waystones and homes for hunger alone. The safe room is gone: old saves bring its belongings to the town." ),
	};

	/** The entries newer than a version the player last ran, newest first. */
	public static ArrayList<Entry> since( int previousVersion ){
		ArrayList<Entry> out = new ArrayList<>();
		for (Entry e : ALL) if (e.since > previousVersion) out.add( e );
		return out;
	}

	/** The version name a version code shows as: build.gradle's code - 8787, as a.b.c. */
	public static String versionName( int code ){
		int v = code - 8787;
		return v > 0 ? (v / 100) + "." + (v / 10 % 10) + "." + (v % 10) : "3.4.0";
	}

	/** The major version (the a of a.b.c) a version code belongs to. */
	public static int major( int code ){
		return Math.max( 0, code - 8787 ) / 100;
	}

	/** Every major version an update shipped in, newest first. */
	public static ArrayList<Integer> majors(){
		ArrayList<Integer> out = new ArrayList<>();
		for (Entry e : ALL){
			int m = major( e.since );
			if (!out.contains( m )) out.add( m );
		}
		return out;
	}

	/** The updates of the given major versions, newest first, each version in its own square
	 *  headed by its number, as Shattered's own history is laid out. */
	public static void addMajors( ArrayList<ChangeInfo> changeInfos, List<Integer> majors ){
		ChangeInfo version = null;
		int at = -1;
		for (Entry e : ALL){
			if (!majors.contains( major( e.since ) )) continue;
			if (e.since != at){
				at = e.since;
				version = new ChangeInfo( "v" + versionName( at ), true, "" );
				version.hardlight( Window.TITLE_COLOR );
				changeInfos.add( version );
			}
			version.addButton( new ChangeButton( e.icon.get(), e.title, e.body ) );
		}
	}
}
