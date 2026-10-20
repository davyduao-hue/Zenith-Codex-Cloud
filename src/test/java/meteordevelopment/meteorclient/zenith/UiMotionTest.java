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
    @Test void lookingSouthPlacesSouthAheadAndEastOnTheLeft() {
        assertArrayEquals(new double[]{0,-10},UiMotion.radar(0,10,0),1e-9);
        assertArrayEquals(new double[]{-10,0},UiMotion.radar(10,0,0),1e-9);
    }
    @Test void turningWestKeepsForwardTargetsCentered() {
        assertArrayEquals(new double[]{0,-10},UiMotion.radar(-10,0,90),1e-9);
        assertArrayEquals(new double[]{10,0},UiMotion.radar(0,-10,90),1e-9);
        assertArrayEquals(UiMotion.radar(7,3,25),UiMotion.radar(7,3,385),1e-9);
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
