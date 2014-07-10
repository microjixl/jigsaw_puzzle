package com.bravelittlescientist.android_puzzle_view;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.*;
import android.widget.Toast;

import java.util.Random;

public class PuzzleCompactSurface extends SurfaceView implements SurfaceHolder.Callback {

    /** Surface Components **/
    private PuzzleThread gameThread;
    private volatile boolean running = false;
    private int found = -1;

    /** Puzzle and Canvas **/
    private int MAX_PUZZLE_PIECE_SIZE = 100;
    private int LOCK_ZONE_LEFT = 20;
    private int LOCK_ZONE_TOP = 20;

    private JigsawPuzzle puzzle;
    /**保存拼图切割的多块图片的画笔**/
    private BitmapDrawable[] scaledSurfacePuzzlePieces;
    /** 保存锁定的图片位置 **/
    private Rect[] scaledSurfaceTargetBounds;
    /**保存背景图片**/
    private BitmapDrawable backgroundImage;
    /**拼图框架的范围画笔**/
    private Paint framePaint;

    public PuzzleCompactSurface(Context context) {
        super(context);

        getHolder().addCallback(this);

        gameThread = new PuzzleThread(getHolder(), context, this);

        setFocusable(true);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) gameThread.pause();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        gameThread.setSurfaceSize(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameThread.setRunning(true);
        gameThread.start();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
       boolean retry = true;
        gameThread.setRunning(false);
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }


    public void setPuzzle(JigsawPuzzle jigsawPuzzle) {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point outSize = new Point();
        //获取屏幕X、Y坐标刻度
        display.getSize(outSize);

        puzzle = jigsawPuzzle;
        //设置图片的随机坐标的随机变量
        Random r = new Random();

        if (puzzle.isBackgroundTextureOn()) {
        	//将背景图片画满整个屏幕
            backgroundImage = new BitmapDrawable(puzzle.getBackgroundTexture());
            backgroundImage.setBounds(0, 0, outSize.x, outSize.y);
        }
        //初始化画拼图框的画笔
        framePaint = new Paint();
        framePaint.setColor(Color.BLACK);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setTextSize(20);

        /** Initialize drawables from puzzle pieces **/
        //获取拼图的多张图片资源
        Bitmap[] originalPieces = puzzle.getPuzzlePiecesArray();
        //获取拼图资源锁定位置的坐标，二维数组保存多个坐标
        int[][] positions = puzzle.getPuzzlePieceTargetPositions();
        
        int[] dimensions = puzzle.getPuzzleDimensions();
        
        //初始化图片画笔
        scaledSurfacePuzzlePieces = new BitmapDrawable[originalPieces.length];
        //初始化图片坐标
        scaledSurfaceTargetBounds = new Rect[originalPieces.length];

        for (int i = 0; i < originalPieces.length; i++) {
        	//将图片资源载入图片
            scaledSurfacePuzzlePieces[i] = new BitmapDrawable(originalPieces[i]);

            // Top left is (0,0) in Android canvas
            //随机设置图片位置，计算图片显示的安全位置值。
            int topLeftX = r.nextInt(outSize.x - MAX_PUZZLE_PIECE_SIZE);
            int topLeftY = r.nextInt(outSize.y - 2*MAX_PUZZLE_PIECE_SIZE);

            scaledSurfacePuzzlePieces[i].setBounds(topLeftX, topLeftY,
                    topLeftX + MAX_PUZZLE_PIECE_SIZE, topLeftY + MAX_PUZZLE_PIECE_SIZE);
        }

        for (int w = 0; w < dimensions[2]; w++) {
            for (int h = 0; h < dimensions[3]; h++) {
                int targetPiece = positions[w][h];
                //计算每张拼接图的锁定位置
                scaledSurfaceTargetBounds[targetPiece] = new Rect(
                        LOCK_ZONE_LEFT + w*MAX_PUZZLE_PIECE_SIZE,
                        LOCK_ZONE_TOP + h*MAX_PUZZLE_PIECE_SIZE,
                        LOCK_ZONE_LEFT + w*MAX_PUZZLE_PIECE_SIZE + MAX_PUZZLE_PIECE_SIZE,
                        LOCK_ZONE_TOP + h*MAX_PUZZLE_PIECE_SIZE + MAX_PUZZLE_PIECE_SIZE);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);

        if (puzzle.isBackgroundTextureOn()) {
        	//画板画背景
            backgroundImage.draw(canvas);
        }
        //画板话拼图锁定框
        canvas.drawRect(20, 20, 420, 320, framePaint);

        for (int bmd = 0; bmd < scaledSurfacePuzzlePieces.length; bmd++) {
        	//画锁定图片
            if (puzzle.isPieceLocked(bmd)) {
                scaledSurfacePuzzlePieces[bmd].draw(canvas);
            }
        }

        for (int bmd = 0; bmd < scaledSurfacePuzzlePieces.length; bmd++) {
        	//画非锁定图片
            if (!puzzle.isPieceLocked(bmd)) {
                scaledSurfacePuzzlePieces[bmd].draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int xPos =(int) event.getX();
        int yPos =(int) event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                for (int i = 0; i < scaledSurfacePuzzlePieces.length; i++) {
                    Rect place = scaledSurfacePuzzlePieces[i].copyBounds();
                    //判定是否选择了未锁定的拼图图片
                    if (place.contains(xPos, yPos) && !puzzle.isPieceLocked(i)) {
                        found = i;

                        // Trigger puzzle piece picked up
                        puzzle.onJigsawEventPieceGrabbed(found, place.left, place.top);
                    }
                }
                break;


            case MotionEvent.ACTION_MOVE:
            	//如果选定了可拼图未锁定图片
                if (found >= 0 && found < scaledSurfacePuzzlePieces.length && !puzzle.isPieceLocked(found)) {
                    // Lock into position...
                    if (scaledSurfaceTargetBounds[found].contains(xPos, yPos) ) {
                    	//设置为锁定图片状态
                        scaledSurfacePuzzlePieces[found].setBounds(scaledSurfaceTargetBounds[found]);
                        puzzle.setPieceLocked(found, true);

                        // Trigger jigsaw piece events
                        //按照锁定坐标位置进行移动
                        puzzle.onJigsawEventPieceMoved(found,
                                scaledSurfacePuzzlePieces[found].copyBounds().left,
                                scaledSurfacePuzzlePieces[found].copyBounds().top);
                        puzzle.onJigsawEventPieceDropped(found,
                                scaledSurfacePuzzlePieces[found].copyBounds().left,
                                scaledSurfacePuzzlePieces[found].copyBounds().top);
                    } else {
                        Rect rect = scaledSurfacePuzzlePieces[found].copyBounds();

                        rect.left = xPos - MAX_PUZZLE_PIECE_SIZE/2;
                        rect.top = yPos - MAX_PUZZLE_PIECE_SIZE/2;
                        rect.right = xPos + MAX_PUZZLE_PIECE_SIZE/2;
                        rect.bottom = yPos + MAX_PUZZLE_PIECE_SIZE/2;
                        scaledSurfacePuzzlePieces[found].setBounds(rect);

                        // Trigger jigsaw piece event
                        //按照拖动位置进行移动
                        puzzle.onJigsawEventPieceMoved(found, rect.left, rect.top);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                // Trigger jigsaw piece event
                if (found >= 0 && found < scaledSurfacePuzzlePieces.length) {
                    puzzle.onJigsawEventPieceDropped(found, xPos, yPos);
                }
                found = -1;
                break;

        }


        return true;
    }

    public PuzzleThread getThread () {
        return gameThread;
    }
}
