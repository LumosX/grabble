package eu.zerovector.grabble;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import eu.zerovector.grabble.Data.Alignment;
import eu.zerovector.grabble.Data.PlayerData;
import eu.zerovector.grabble.Data.XPUtils;
import eu.zerovector.grabble.Utils.GrabbleAPIException;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class InstrumentedTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    // We can keep the default test here too, why not
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("eu.zerovector.grabble", appContext.getPackageName());
    }

    @Test
    public void testXPAmounts() throws Exception {
        // Let's make sure that the experience required for levels is constantly increasing, i.e.
        // that for every rank x, XPRequired(x) < XPRequired(x+1).
        // In other words, this tests whether our circle-using algorithm is working properly.

        // Requires running on device ("Log.d not mocked")
        int curXP = 0, curLevel = 0;
        while (curLevel < XPUtils.MAX_LEVEL) {
            XPUtils.LevelDetails det = XPUtils.getLevelDetailsForXP(curXP);
            Assert.assertTrue("XP required for level " + (curLevel + 1) + " MUST be greater than " +
                    "the XP required for " + curLevel, det.nextLevelXP() > det.thisLevelXP());
            curXP = det.nextLevelXP() + 10; // make sure we're getting the next level
            curLevel++;
        }
    }

    // Let's also test whether the Registration process denies incorrect data properly
    // (At least in the basic cases. I really can't stand writing any more tests.)
    @Test
    public void testRegBadEmail() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();
        PlayerData data;

        // Bad email
        data = new PlayerData("notAnEmail", "John Doe", "pass", "Faction Name", Alignment.Closers);
        exception.expect(GrabbleAPIException.class);
        RegTestHelper(ctx, data, "pass");
    }

    @Test
    public void testRegPassMismatch() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();
        PlayerData data;

        // Password mismatch
        data = new PlayerData("good@email.com", "John Doe", "pass", "Faction Name", Alignment.Closers);
        exception.expect(GrabbleAPIException.class);
        RegTestHelper(ctx, data, "differentPass");
    }

    @Test
    public void testRegShortPass() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();
        PlayerData data;

        // Password too short
        data = new PlayerData("good@email.com", "John Doe", "pass", "Faction Name", Alignment.Closers);
        exception.expect(GrabbleAPIException.class);
        RegTestHelper(ctx, data, "pass");
    }

    @Test
    public void testRegShortName() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();
        PlayerData data;

        // Username too short
        data = new PlayerData("good@email.com", "Mum", "password", "Faction Name", Alignment.Closers);
        exception.expect(GrabbleAPIException.class);
        RegTestHelper(ctx, data, "password");
    }

    @Test
    public void testRegLongName() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();
        PlayerData data;

        // Username too long
        data = new PlayerData("good@email.com", "John Doe, Found Yesterday, of No Memory and Names Too Long",
                "password", "Faction Name", Alignment.Closers);
        exception.expect(GrabbleAPIException.class);
        RegTestHelper(ctx, data, "password");
    }

    @Test
    public void testRegShortFacName() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();
        PlayerData data;

        // Faction name too short
        data = new PlayerData("good@email.com", "John Doe", "password", "Dudes", Alignment.Closers);
        exception.expect(GrabbleAPIException.class);
        RegTestHelper(ctx, data, "password");
    }

    @Test
    public void testRegLongFacName() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();
        PlayerData data;

        // Faction name too long
        data = new PlayerData("good@email.com", "John Doe", "password", "John Doe's Overly Cool Dudes " +
                "Whose Faction Name Somehow Managed To Become Longer Than One Hundered Sodding Characters",
                Alignment.Closers);
        exception.expect(GrabbleAPIException.class);
        RegTestHelper(ctx, data, "password");
    }

    // I simply don't want to have the Net.Reg function pop up 50 times in the usages list
    private void RegTestHelper(Context ctx, PlayerData data, String confirmPass) throws GrabbleAPIException {
        Network.Register(ctx, data, "password");
    }
}
