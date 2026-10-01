package xyz.gabriwar.warpedpixeldungeon.actors;

import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Smash;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import static org.junit.Assert.*;

public class ThrowdownTest {
    @org.junit.BeforeClass public static void loadAssets(){
        xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
    }
    private Level previousLevel;
    private Hero previousHero;
    private String previousVersion;
    private Char thrown, other;
    private Smash skill;
    private static final int START = 9 * 19 + 5;

    private Char enemy(int pos){
        Char ch = new Char(){
            @Override public void damage(int amount, Object source){ HP -= amount; }
        };
        ch.pos = pos;
        ch.HP = ch.HT = 100;
        ch.alignment = Char.Alignment.ENEMY;
        Actor.add(ch);
        return ch;
    }

    @Before public void setup(){
        previousLevel = Dungeon.level;
        previousHero = Dungeon.hero;
        previousVersion = Game.version;
        Game.version = "test";
        Level floor = new Level(){
            @Override protected boolean build(){ return true; }
            @Override protected void createMobs(){}
            @Override protected void createItems(){}
        };
        floor.setSize(19, 19);
        floor.traps = new com.watabou.utils.SparseArray<>();
        floor.plants = new com.watabou.utils.SparseArray<>();
        floor.heaps = new com.watabou.utils.SparseArray<>();
        floor.blobs = new java.util.HashMap<>();
        Dungeon.level = floor;
        java.util.Arrays.fill(floor.solid, true);
        for (int y=1; y<18; y++) for (int x=1; x<18; x++) Level.set(y*19+x, Terrain.EMPTY, floor);
        java.util.Arrays.fill(floor.heroFOV, true);
        Dungeon.hero = new Hero();
        Dungeon.hero.pos = START - 19;
        thrown = enemy(START);
        other = enemy(START + 3);
        skill = new Smash();
        skill.level = 1;
    }

    @After public void cleanup(){
        for (Char ch : new Char[]{thrown, other}){
            if (ch == null) continue;
            for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff buff : ch.buffs()) buff.detach();
            Actor.remove(ch);
        }
        Dungeon.level = previousLevel;
        Dungeon.hero = previousHero;
        Game.version = previousVersion;
        if (previousLevel != null) PathFinder.setMapSize(previousLevel.width(), previousLevel.height());
    }

    private static class TrackingSprite extends xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite {
        int drawnCell;
        @Override public void place(int cell){ drawnCell = cell; }
    }

    @Test public void landingIsOccupiedBeforeAnimationAndOldTileCanBeReused(){
        TrackingSprite sprite = new TrackingSprite();
        thrown.sprite = sprite;
        int landing = START + 2;
        assertTrue(skill.beginThrow(thrown, Dungeon.level, START, landing));
        assertSame(thrown, Actor.findChar(landing));
        assertNull(Actor.findChar(START));
        other.pos = START; //another enemy enters the vacated tile during the animation
        assertTrue(skill.finishThrow(thrown, null, Dungeon.level, START, landing, 12));
        assertSame(other, Actor.findChar(START));
        assertSame(thrown, Actor.findChar(landing));
        assertEquals(landing, sprite.drawnCell);
        assertEquals(88, thrown.HP);
    }

    @Test public void displacementDuringJumpDoesNotLeaveGhostAtOldLanding(){
        TrackingSprite sprite = new TrackingSprite();
        thrown.sprite = sprite;
        int landing = START + 2;
        assertTrue(skill.beginThrow(thrown, Dungeon.level, START, landing));
        thrown.move(START + 19, false); //teleport/another forced movement while airborne
        other.pos = landing;
        sprite.drawnCell = landing; //the stale jump tween has just reached its endpoint
        assertFalse(skill.finishThrow(thrown, other, Dungeon.level, START, landing, 12));
        assertEquals(thrown.pos, sprite.drawnCell);
        assertSame(other, Actor.findChar(landing));
        assertEquals(100, thrown.HP);
        assertEquals(100, other.HP);
    }

    @Test public void blockedLandingDoesNotMoveOrOverlapActors(){
        TrackingSprite sprite = new TrackingSprite();
        thrown.sprite = sprite;
        other.pos = START + 2;
        assertFalse(skill.beginThrow(thrown, Dungeon.level, START, START + 2));
        assertEquals(START, thrown.pos);
        assertSame(other, Actor.findChar(START + 2));
    }

    @Test public void deathDuringJumpStillResyncsSpriteWithoutApplyingAnotherHit(){
        TrackingSprite sprite = new TrackingSprite();
        thrown.sprite = sprite;
        int landing = START + 2;
        assertTrue(skill.beginThrow(thrown, Dungeon.level, START, landing));
        thrown.pos = START + 19;
        thrown.HP = 0;
        sprite.drawnCell = landing;
        assertFalse(skill.finishThrow(thrown, null, Dungeon.level, START, landing, 12));
        assertEquals(thrown.pos, sprite.drawnCell);
        assertEquals(0, thrown.HP);
    }

    @Test public void scheduledThrowOwnsTurnAcrossGrabAndJump() throws Exception {
        com.badlogic.gdx.Application oldApp = com.badlogic.gdx.Gdx.app;
        java.lang.reflect.Field current = Actor.class.getDeclaredField("current");
        current.setAccessible(true);
        Object oldCurrent = current.get(null);
        java.util.ArrayDeque<Runnable> renderQueue = new java.util.ArrayDeque<>();
        com.watabou.utils.Callback[] callbacks = new com.watabou.utils.Callback[2];
        com.watabou.noosa.Group layer = new com.watabou.noosa.Group();
        Level floor = Dungeon.level;
        try {
            com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application) java.lang.reflect.Proxy.newProxyInstance(
                    com.badlogic.gdx.Application.class.getClassLoader(), new Class[]{com.badlogic.gdx.Application.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("postRunnable")){ renderQueue.add((Runnable)args[0]); return null; }
                        return method.invoke(oldApp, args);
                    });
            Dungeon.hero.sprite = new TrackingSprite(){
                @Override public synchronized void attack(int cell, com.watabou.utils.Callback callback){ callbacks[0] = callback; }
            };
            thrown.sprite = new TrackingSprite(){
                @Override public void jump(int from, int to, com.watabou.utils.Callback callback){ callbacks[1] = callback; }
            };
            layer.add(Dungeon.hero.sprite);
            layer.add(thrown.sprite);
            Smash.ThrowAction action = skill.new ThrowAction(Dungeon.hero, thrown, null, floor, START, START+2, 12);
            Actor.add(action);
            current.set(null, action); //the scheduler has selected this VFX actor
            assertFalse(action.act());
            assertTrue(Actor.processing());
            renderQueue.remove().run();
            assertNotNull(callbacks[0]);
            Dungeon.hero.next();
            thrown.next();
            assertSame(action, current.get(null));
            assertEquals(START, thrown.pos);
            callbacks[0].call();
            assertEquals(START+2, thrown.pos);
            assertNotNull(callbacks[1]);
            Dungeon.hero.next();
            thrown.next();
            assertSame(action, current.get(null));
            Dungeon.hero.pos = 0; //skip fog rendering in this headless test
            callbacks[1].call();
            assertFalse(Actor.processing());
            assertEquals(88, thrown.HP);
            assertEquals(START+2, ((TrackingSprite)thrown.sprite).drawnCell);
            callbacks[1].call(); //a repeated visual completion must not damage twice
            assertEquals(88, thrown.HP);
        } finally {
            current.set(null, oldCurrent);
            com.badlogic.gdx.Gdx.app = oldApp;
            layer.destroy();
        }
    }

    @Test public void masteryStylePushCancelsTheWalkAnimation(){
        float elapsed = Game.elapsed;
        com.watabou.noosa.Group layer = new com.watabou.noosa.Group();
        try {
            thrown.sprite = new xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite(){
                @Override public void update(){}
                @Override public void turnTo(int from, int to){}
                @Override public com.watabou.utils.PointF worldToCamera(int cell){ return new com.watabou.utils.PointF(cell, 0); }
            };
            thrown.sprite.ch = thrown;
            thrown.sprite.visible = false;
            layer.add(thrown.sprite);
            Game.elapsed = 0.05f;
            thrown.sprite.move(START-19, START);
            layer.update();
            xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.push(thrown, Dungeon.hero.pos, 1, 0);
            assertEquals(START+19, thrown.pos);
            for (int i=0; i<20; i++) layer.update();
            assertEquals(thrown.pos, thrown.sprite.x, 0.001f);
            assertFalse(thrown.sprite.isMoving);
        } finally { layer.destroy(); Game.elapsed = elapsed; }
    }

    @Test public void queuedMasteryReactionIsDiscardedAfterThrowRelocation() throws Exception {
        Dungeon.hero.heroSkills = xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills.forHero(Dungeon.hero);
        java.lang.reflect.Field all = Dungeon.hero.heroSkills.getClass().getDeclaredField("allSkills");
        all.setAccessible(true);
        java.util.List<xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill> skills =
                (java.util.List<xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill>)all.get(Dungeon.hero.heroSkills);
        java.util.List<xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill> saved = new java.util.ArrayList<>(skills);
        java.util.Set<Actor> before = new java.util.HashSet<>(Actor.all());
        int[] reactions = {0};
        try {
            skills.clear();
            xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill spy = new xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill(){
                @Override public void onEnemyStepsAdjacent(Char ch, int from){ reactions[0]++; }
            };
            spy.level = 1;
            skills.add(spy);
            thrown.sprite = new TrackingSprite();
            thrown.pos = START+38;
            thrown.move(START, true);
            java.util.Set<Actor> queued = new java.util.HashSet<>(Actor.all());
            queued.removeAll(before);
            assertEquals(1, queued.size());
            assertTrue(skill.beginThrow(thrown, Dungeon.level, START, START+1));
            for (Actor actor : queued) actor.act();
            assertEquals(0, reactions[0]);
            //An actual later approach still triggers the reaction.
            thrown.move(START+38, false);
            thrown.move(START, true);
            queued = new java.util.HashSet<>(Actor.all());
            queued.removeAll(before);
            for (Actor actor : queued) actor.act();
            assertEquals(1, reactions[0]);
        } finally {
            skills.clear(); skills.addAll(saved);
            for (Actor actor : Actor.all()) if (!before.contains(actor)) Actor.remove(actor);
        }
    }

    @Test public void chasmFallStartsOnlyAfterTheThrowLands(){
        int depth = Dungeon.depth, branch = Dungeon.branch;
        com.watabou.utils.SparseArray<java.util.ArrayList<xyz.gabriwar.warpedpixeldungeon.levels.features.FallenMob>> fallen = Dungeon.fallenMobs;
        xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat rat = null;
        try {
            Dungeon.depth = 1; Dungeon.branch = 0;
            Dungeon.fallenMobs = new com.watabou.utils.SparseArray<>();
            Dungeon.level.mobs = new java.util.HashSet<>();
            rat = new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat();
            rat.pos = START+19;
            rat.HP = rat.HT = 100;
            final int[] falls = {0};
            rat.sprite = new xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite(){
                @Override public void place(int cell){}
                @Override public void fall(){ falls[0]++; }
                @Override public void add(State state){}
            };
            Dungeon.level.mobs.add(rat);
            Actor.add(rat);
            int pit = START+20;
            Level.set(pit, Terrain.CHASM, Dungeon.level);
            assertTrue(skill.beginThrow(rat, Dungeon.level, START+19, pit));
            assertEquals(0, falls[0]);
            assertTrue(Actor.chars().contains(rat));
            assertNull(Dungeon.fallenMobs.get(2));
            assertFalse(skill.finishThrow(rat, null, Dungeon.level, START+19, pit, 12));
            assertEquals(1, falls[0]);
            assertFalse(Actor.chars().contains(rat));
            assertSame(rat, Dungeon.fallenMobs.get(2).get(0).mob);
        } finally {
            if (rat != null){
                for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff b : rat.buffs()) b.detach();
                Actor.remove(rat);
            }
            Dungeon.depth = depth; Dungeon.branch = branch; Dungeon.fallenMobs = fallen;
        }
    }

    @Test public void rangeAndCollisionLandingAtEveryRank(){
        for (int rank=1; rank<=3; rank++){
            skill.level = rank;
            int range = 1 + 2*rank;
            assertEquals(range, skill.range());
            assertEquals(START+2, skill.landingCell(thrown, other.pos));
            assertEquals(START-range*19, skill.landingCell(thrown, START-range*19));
            assertEquals(-1, skill.landingCell(thrown, START-(range+1)*19));
        }
    }

    @Test public void rejectsWallsAndBossesButAllowsChasms(){
        assertTrue(skill.canGrab(Dungeon.hero, thrown));
        Level.set(START+1, Terrain.WALL, Dungeon.level);
        assertEquals(-1, skill.landingCell(thrown, START+2));
        Level.set(START+1, Terrain.EMPTY, Dungeon.level);
        Level.set(START+2, Terrain.CHASM, Dungeon.level);
        assertEquals(START+2, skill.landingCell(thrown, START+2));
        Level.set(START+2, Terrain.EMPTY, Dungeon.level);
        other.properties.add(Char.Property.BOSS);
        assertEquals(-1, skill.landingCell(thrown, other.pos));
        skill.collision(thrown, other, 10);
        assertEquals(100, other.HP);
        assertEquals(100, thrown.HP);
        thrown.properties.add(Char.Property.BOSS);
        assertFalse(skill.canGrab(Dungeon.hero, thrown));
    }

    @Test public void doorAndInvalidWallSelectionDoNotMoveEnemy(){
        TrackingSprite sprite = new TrackingSprite();
        thrown.sprite = sprite;
        sprite.drawnCell = START;
        int hp = thrown.HP;
        Level.set(START+2, Terrain.WALL, Dungeon.level);
        assertEquals(-1, skill.landingCell(thrown, START+2));
        assertEquals(START, thrown.pos);
        assertEquals(START, sprite.drawnCell);
        assertEquals(hp, thrown.HP);
        Level.set(START+2, Terrain.DOOR, Dungeon.level);
        assertEquals(START+2, skill.landingCell(thrown, START+2));
        assertEquals(-1, skill.landingCell(thrown, START+3));
        Level.set(START+2, Terrain.OPEN_DOOR, Dungeon.level);
        assertEquals(START+2, skill.landingCell(thrown, START+2));
        assertEquals(START+2, skill.landingCell(thrown, other.pos));
    }

    @Test public void collisionHurtsAndParalyzesBothAtEveryRank(){
        for (int rank=1; rank<=3; rank++){
            skill.level = rank;
            thrown.HP = other.HP = 100;
            skill.collision(thrown, other, 12);
            assertEquals(88, thrown.HP);
            assertEquals(88, other.HP);
            assertNotNull(thrown.buff(Paralysis.class));
            assertNotNull(other.buff(Paralysis.class));
            assertEquals(1f, thrown.buff(Paralysis.class).cooldown(), 0.001f);
            thrown.buff(Paralysis.class).detach();
            other.buff(Paralysis.class).detach();
        }
    }
}
