package com.koimp3.blockworld;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {
    private final Paint p = new Paint();
    private final boolean[] keys = new boolean[3]; // left, right, punch
    private final int tile = 64;
    private int playerX = 260;
    private float playerY = 0, vy = 0;
    private boolean grounded = false;
    private long punchUntil = 0;
    private final boolean[][] blocks = new boolean[80][8];
    private long lastTime = System.nanoTime();

    public GameView(Context c) {
        super(c);
        setFocusable(true);
        for (int x=0;x<blocks.length;x++) {
            blocks[x][5] = true;
            blocks[x][6] = true;
            blocks[x][7] = true;
            if (x % 9 == 5) blocks[x][4] = true;
        }
    }

    private void color(Canvas c, int color, float l, float t, float r, float b) {
        p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRect(l,t,r,b,p);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w=getWidth(), h=getHeight();
        float dt=Math.min(0.04f,(System.nanoTime()-lastTime)/1_000_000_000f);
        lastTime=System.nanoTime();
        if (keys[0]) playerX -= (int)(260*dt);
        if (keys[1]) playerX += (int)(260*dt);
        playerX=Math.max(30,Math.min(playerX, (int)(w*2.5f)));
        vy += 1500*dt;
        playerY += vy*dt;
        float groundY=h*0.72f;
        if (playerY >= 0) { playerY=0; vy=0; grounded=true; }

        // Sky and distant hills
        color(c,0xFF83D8F2,0,0,w,h);
        color(c,0xFF61BD78,0,groundY-70,w,groundY+tile*3);
        color(c,0xFF47965B,0,groundY-30,w,groundY+tile*3);

        // Tile world with a simple side-scrolling camera
        int camera= Math.max(0, playerX-w/3);
        int start=Math.max(0,camera/tile);
        int end=Math.min(blocks.length,(camera+w)/tile+2);
        for(int tx=start;tx<end;tx++) for(int ty=0;ty<blocks[tx].length;ty++) {
            if(!blocks[tx][ty]) continue;
            float x=tx*tile-camera, y=groundY+(ty-5)*tile;
            if(ty==5) {
                color(c,0xFF4BAE48,x,y,x+tile,y+tile);
                color(c,0xFF80D64B,x,y,x+tile,y+12);
            } else {
                color(c,0xFF98633C,x,y,x+tile,y+tile);
                color(c,0xFFB7804E,x+4,y+4,x+tile-4,y+tile-4);
                color(c,0xFF86532F,x,y+tile-6,x+tile,y+tile);
            }
            p.setColor(0x553A2B20); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            c.drawRect(x,y,x+tile,y+tile,p); p.setStyle(Paint.Style.FILL);
        }

        // Player, chunky pixel-art style
        float px=w/3f, feet=groundY-8+playerY;
        color(c,0xFF25334B,px-18,feet-58,px+18,feet);
        color(c,0xFFFFC58A,px-13,feet-82,px+13,feet-55);
        color(c,0xFF402B25,px-14,feet-86,px+14,feet-76);
        color(c,0xFF3A8DFF,px-17,feet-55,px+17,feet-28);
        color(c,0xFF263B65,px-16,feet-28,px-2,feet);
        color(c,0xFF263B65,px+2,feet-28,px+16,feet);
        color(c,0xFF18202B,px+5,feet-74,px+9,feet-69);

        if(System.currentTimeMillis()<punchUntil) {
            color(c,0xFFFFC58A,px+15,feet-53,px+43,feet-42);
            color(c,0xFFFFFFD0,px+39,feet-58,px+54,feet-38);
        } else {
            color(c,0xFFFFC58A,px+12,feet-54,px+25,feet-39);
        }

        // Touch controls
        drawButton(c, 24,h-104,116,h-24,"◀",0xCC18324B);
        drawButton(c, 132,h-104,224,h-24,"▶",0xCC18324B);
        drawButton(c, w-224,h-104,w-132,h-24,"JUMP",0xCC247A54);
        drawButton(c, w-116,h-104,w-24,h-24,"PUNCH",0xCCB94B35);
        p.setTextSize(20); p.setColor(0xFF18324B); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("BLOCK WORLD  •  PROTOTYPE",24,32,p);
        p.setTextSize(14); c.drawText("Bergerak • Lompat • Hancurkan blok",24,53,p);
        postInvalidateDelayed(16);
    }

    private void drawButton(Canvas c,float l,float t,float r,float b,String label,int col) {
        p.setColor(col); p.setStyle(Paint.Style.FILL); c.drawRoundRect(new RectF(l,t,r,b),14,14,p);
        p.setColor(0xFFFFFFFF); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(label.length()>4?15:27);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); c.drawText(label,(l+r)/2,(t+b)/2+9,p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(), y=e.getY();
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE) {
            boolean control=y>getHeight()-125;
            keys[0]=control && x<124;
            keys[1]=control && x>=124 && x<236;
            if(control && x>getWidth()-236 && x<getWidth()-124 && action==MotionEvent.ACTION_DOWN) {
                if(grounded) { vy=-600; grounded=false; }
            }
            if(control && x>=getWidth()-124 && action==MotionEvent.ACTION_DOWN) {
                punchUntil=System.currentTimeMillis()+220;
                int camera=Math.max(0,playerX-getWidth()/3);
                int target=(playerX+38+camera)/tile;
                if(target>=0 && target<blocks.length) {
                    for(int ty=3;ty<8;ty++) if(blocks[target][ty]) { blocks[target][ty]=false; break; }
                }
            }
        } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
            keys[0]=false; keys[1]=false;
        }
        return true;
    }
}
