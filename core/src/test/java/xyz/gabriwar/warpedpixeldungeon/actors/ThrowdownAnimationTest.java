package xyz.gabriwar.warpedpixeldungeon.actors;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import static org.junit.Assert.*;

public class ThrowdownAnimationTest {
    @org.junit.BeforeClass public static void loadAssets(){
        xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
    }
    private float previousElapsed;
    private String previousVersion;
    private Group group;
    private CharSprite sprite;
    private Char enemy;
    private int jumps;
    private int walks;

    @Before public void setup(){
        previousElapsed = Game.elapsed;
        previousVersion = Game.version;
        Game.version = "test";
        Game.elapsed = 0.05f;
        group = new Group();
        enemy = new Char(){
            @Override public void onMotionComplete(){ walks++; }
        };
        sprite = new CharSprite(){
            @Override public void turnTo(int from, int to){}
            @Override public PointF worldToCamera(int cell){ return new PointF(cell, 0); }
            @Override public void update(){} //the group still updates the real movement tweeners
        };
        sprite.ch = enemy;
        enemy.sprite = sprite;
        sprite.visible = false;
        group.add(sprite);
    }

    @After public void cleanup(){
        group.destroy();
        Game.elapsed = previousElapsed;
        Game.version = previousVersion;
    }

    private void frames(int count){ for (int i=0; i<count; i++) group.update(); }

    @Test public void jumpReplacesWalkingTweenAndBlocksNextMoveUntilLanding(){
        enemy.pos = 1;
        sprite.move(0, 1);
        frames(1);
        enemy.pos = 7;
        sprite.jump(1, 7, 2, 0.4f, () -> jumps++);
        assertTrue(sprite.isMoving);
        frames(2);
        assertTrue(sprite.isMoving);
        frames(12);
        assertEquals(7f, sprite.x, 0.001f);
        assertEquals(0f, sprite.y, 0.001f);
        assertFalse(sprite.isMoving);
        assertEquals(1, jumps);
        assertEquals(0, walks);
        frames(20);
        assertEquals(7f, sprite.x, 0.001f);
        assertEquals(1, jumps);
    }

    @Test public void forcedWalkDuringJumpCancelsOldTweenAndCallbackOnlyRunsOnce(){
        enemy.pos = 7;
        sprite.jump(0, 7, 2, 0.4f, () -> jumps++);
        frames(2);
        enemy.pos = 3;
        sprite.move(7, 3);
        frames(20);
        assertEquals(3f, sprite.x, 0.001f);
        assertEquals(0f, sprite.y, 0.001f);
        assertEquals(1, jumps);
        assertEquals(1, walks);
        assertFalse(sprite.isMoving);
    }

    @Test public void twoWalksAfterThrowDoNotLeaveAnOlderPositionTweenAlive(){
        enemy.pos = 7;
        sprite.jump(0, 7, 2, 0.1f, () -> jumps++);
        frames(4);
        enemy.pos = 5;
        sprite.move(7, 5);
        frames(1);
        enemy.pos = 3;
        sprite.move(5, 3);
        frames(20);
        assertEquals(3f, sprite.x, 0.001f);
        assertEquals(1, jumps);
        assertEquals(1, walks);
        assertFalse(sprite.isMoving);
    }

    @Test public void completedJumpDoesNotKeepOrRepeatItsCallback(){
        enemy.pos = 7;
        sprite.jump(0, 7, 2, 0.1f, () -> jumps++);
        frames(4);
        enemy.pos = 4;
        sprite.move(7, 4);
        frames(20);
        assertEquals(1, jumps);
        assertEquals(4f, sprite.x, 0.001f);
        assertFalse(sprite.isMoving);
    }
}
