package meteordevelopment.meteorclient.zenith;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiMotionTest {
    @Test void keyFadeIsIndependentOfFrameRate() {
        double a=0,b=0;for(int i=0;i<60;i++)a=UiMotion.approach(a,1,15,1.0/60);
        for(int i=0;i<30;i++)b=UiMotion.approach(b,1,15,1.0/30);
        assertEquals(a,b,1e-9);assertTrue(a>.99);
    }
    @Test void releaseDecaysWithoutOvershootOrTeleport() {
        double value=1;
        for(int i=0;i<40;i++){double next=UiMotion.approach(value,0,15,1.0/60);assertTrue(next>=0&&next<=value);value=next;}
        assertTrue(value<.001);assertEquals(.4,UiMotion.approach(.4,1,15,-1));
    }
    @Test void cameraWrapUsesSmallestTurnRatherThanSpinningDot() {
        assertEquals(2,UiMotion.angleDelta(179,-179),1e-9);
        assertEquals(-2,UiMotion.angleDelta(-179,179),1e-9);
        assertEquals(4,UiMotion.angleDelta(719,3),1e-9);
    }
    @Test void lookingDeflectsDotAndStillnessRecentersIt() {
        var motion=new UiMotion.LookMotion();
        motion.update(0,0,1.0/60,1);
        for(int i=1;i<=10;i++)motion.update(i*3,i,1.0/60,1);
        assertTrue(motion.x()>.25);assertTrue(motion.y()>.08);
        for(int i=0;i<60;i++)motion.update(30,10,1.0/60,1);
        assertEquals(0,motion.x(),1e-6);assertEquals(0,motion.y(),1e-6);
    }
    @Test void directionDotRemainsInsideCircleDuringFastDiagonalTurns() {
        var motion=new UiMotion.LookMotion();
        motion.update(0,0,1.0/120,4);
        for(int i=1;i<=50;i++) {
            motion.update(i*50,i%2==0?90:-90,1.0/120,4);
            assertTrue(Math.hypot(motion.x(),motion.y())<=1.000001);
        }
        motion.update(0,0,2,4);
        assertEquals(0,motion.x());assertEquals(0,motion.y());
    }
    @Test void pressAndReleaseBetweenFramesStillShowsFillAndRing() {
        var motion=new UiMotion.PressAnimation();
        motion.update(false,true,1.0/60);
        assertTrue(motion.amount()>.3);assertEquals(0,motion.rippleProgress(0));
        motion.update(false,false,1.0/60);
        assertTrue(motion.amount()>.6);assertTrue(motion.rippleProgress(0)>0);
        for(int i=0;i<90;i++)motion.update(false,false,1.0/60);
        assertTrue(motion.amount()<1e-6);assertEquals(1,motion.rippleProgress(0));
    }
    @Test void heldKeysDoNotContinuouslyRetriggerPressRings() {
        var motion=new UiMotion.PressAnimation();
        for(int i=0;i<90;i++)motion.update(true,false,1.0/60);
        assertEquals(1,motion.amount(),1e-6);
        for(int i=0;i<UiMotion.PressAnimation.RIPPLE_COUNT;i++)assertEquals(1,motion.rippleProgress(i));
        motion.update(false,false,1.0/60);
        motion.update(true,false,1.0/60);
        assertEquals(0,motion.rippleProgress(1));
    }
    @Test void repeatedClicksKeepBoundedIndependentRingLifetimes() {
        var motion=new UiMotion.PressAnimation();
        for(int i=0;i<100;i++)motion.update(false,true,.025);
        for(int i=0;i<UiMotion.PressAnimation.RIPPLE_COUNT;i++)
            assertTrue(motion.rippleProgress(i)>=0&&motion.rippleProgress(i)<.25);
    }
    @Test void radarAndKeysFollowHotbarAtCommonResolutions() {
        int[][] screens={{854,480,2},{1280,720,2},{1920,1080,3},{2560,1440,4},{3840,2160,4}};
        for(int[] screen:screens) {
            double scale=Math.min(1,Math.max(.6,screen[0]/1280.0));
            for(boolean left:new boolean[]{true,false}) {
                double width=132*scale,height=(left?153:146)*scale;
                UiMotion.Position p=UiMotion.dock(screen[0],screen[1],screen[2],width,height,scale,left);
                assertTrue(p.x()>=0&&p.x()+width<=screen[0]);assertTrue(p.y()>=0&&p.y()+height<=screen[1]);
                double graphEdge=18+235*scale;
                if(left)assertTrue(p.x()>graphEdge);else assertTrue(p.x()+width<screen[0]-graphEdge);
                if(p.raised())assertTrue(p.y()+height<screen[1]-22*screen[2]);
                else if(left)assertTrue(p.x()+width<screen[0]/2.0-91*screen[2]);
                else assertTrue(p.x()>screen[0]/2.0+91*screen[2]);
            }
        }
    }
    @Test void higherGuiScaleRaisesWidgetsWhenSideSpaceDisappears() {
        var a=UiMotion.dock(1280,720,2,132,153,1,true);
        var b=UiMotion.dock(1280,720,4,132,153,1,true);
        assertFalse(a.raised());assertTrue(b.raised());assertTrue(b.y()<a.y());
    }
}
