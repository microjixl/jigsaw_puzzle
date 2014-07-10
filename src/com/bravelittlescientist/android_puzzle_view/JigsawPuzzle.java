package com.bravelittlescientist.android_puzzle_view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

public class JigsawPuzzle {
	/**拼图目标图片的宽度**/
    protected static int puzzleXDimension;
    /**拼图目标图片的高度**/
    protected static int puzzleYDimension;
    
    /**拼图图片块的高度**/
    protected static int puzzlePieceHeight;
    /**拼图图片块的宽度**/
    protected static int puzzlePieceWidth;

    /**图片切割后的列数，用于记录坐标位置**/
    protected static int puzzleGridX;
    /**图片切割后的行数，用于记录坐标位置**/
    protected static int puzzleGridY;

    /****/
    private boolean backgroundTextureOn = true;
    /****/
    private Bitmap backgroundTexture;

    /**画板配置**/
    private Bundle config;

    /**目标拼图图片（原图按照比例缩放后的图）**/
    private Bitmap puzzleResult;
    /**切割后的图片块数组**/
    private Bitmap[] puzzlePiecesArray;
    /**坐标位置点保存变量**/
    private int[][] puzzlePieceTargetPositions;
    /**图片的锁定状态**/
    private boolean[] pieceLocked;

    /**拼图画板上下文**/
    private Context mContext;

    /**
     * JigsawPuzzle constructor: Dynamic Configuration
     * @param res
     * @param resourceId
     *
     * This jigsaw puzzle will be configured dynamically by partitioning
     * the provided source image.
     */
    public JigsawPuzzle(Resources res, Integer resourceId) {
        loadPuzzleResources(res, resourceId, 450, 300);
        buildDynamicPuzzleGrid();
    }

    /**
     * JigsawPuzzle constructor: Bundle configuration
     * @param context
     * @param configuration
     */
    public JigsawPuzzle(Context context, Bundle configuration) {
        config = configuration;
        mContext = context;

        loadPuzzleResources(mContext.getResources(),
                config.getBundle("img").getInt("img_local"), 400, 300);
        loadPuzzleConfiguration();
    }

    public void loadPuzzleResources (Resources res, int resourceId, long targetWidth, long targetHeight) {

        backgroundTexture = decodePuzzleBitmapFromResource(res, R.drawable.background, 800, 800);

        Bitmap decodedPuzzleResource = decodePuzzleBitmapFromResource(
                res, resourceId, targetWidth, targetHeight);

        puzzleResult = decodedPuzzleResource;
        puzzleXDimension = decodedPuzzleResource.getWidth();
        puzzleYDimension = decodedPuzzleResource.getHeight();
    }

    /**
     * decodePuzzleBitmapFromResource
     * 将图片按原图比例缩放
     * @param res
     * @param resId
     * @param targetWidth
     * @param targetHeight
     * @return Bitmap
     *
     *  Bitmap Loading Code from Android Developer lesson: "Loading Large Bitmaps Efficiently"
     */
    public static Bitmap decodePuzzleBitmapFromResource (
            Resources res, int resId, long targetWidth, long targetHeight) {

        // Load only the dimensions of the puzzle image
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate ratio to scale puzzle bitmap
        options.inSampleSize = calculateScaledPuzzleSize(options, targetWidth, targetHeight);

        // Decode puzzle resource image to bitmap from computed ratio
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * calculateScaledPuzzleSize
     * 计算图片缩放比例，按照图片比例宽高比例缩放，缩放按照比例小的部分缩放。
     * Adapted from Android Developer lesson: "Loading Large Bitmaps Efficiently"
     */
    public static int calculateScaledPuzzleSize (
            BitmapFactory.Options options, long targetWidth, long targetHeight) {

        // Source Image Dimensions
        final int height = options.outHeight;
        final int width = options.outWidth;
        int imageScaleRatio = 1;

        if (height > targetHeight || width > targetWidth) {
            // Calculate ratios of height and width to target height and width
            final int heightRatio = Math.round((float) height / (float) targetHeight);
            final int widthRatio = Math.round((float) width / (float) targetWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            imageScaleRatio = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return imageScaleRatio;
    }

    /**
     * buildDynamicPuzzleGrid
     * 动态切割图片初始数据
     * If not already set, computes optimal puzzle piece size using Greatest Common Divisor.
     * Computes Bitmaps for each piece and sets their target positions.
     *
     * TODO: Handle case where GCD is 1, or if piece should be a rectangle?
     */
    public void buildDynamicPuzzleGrid() {
        // Compute optimal piece size:
    	// 按照图片大小计算拼图图片块，结果为正方形的边长
        int optimalPieceSize = greatestCommonDivisor(puzzleXDimension, puzzleYDimension);

        // Update puzzle dimension variables
        puzzlePieceHeight = optimalPieceSize;
        puzzlePieceWidth = optimalPieceSize;
        puzzleGridX = puzzleXDimension / puzzlePieceWidth;
        puzzleGridY = puzzleYDimension / puzzlePieceHeight;

        // Initialize and fill puzzle
        puzzlePieceTargetPositions = new int[puzzleGridX][puzzleGridY];
        puzzlePiecesArray = new Bitmap[puzzleGridX * puzzleGridY];
        pieceLocked = new boolean[puzzleGridX * puzzleGridY];

        // Generate array of bitmaps
        // 初始化每张图片的坐标、图片、序列号等信息
        int counter = 0;
        for (int w = 0; w < puzzleGridX; w++) {
            for (int h = 0; h < puzzleGridY; h++) {
                puzzlePiecesArray[counter] = Bitmap.createBitmap(puzzleResult,
                        w*puzzlePieceWidth, h*puzzlePieceHeight, puzzlePieceWidth, puzzlePieceHeight);

                pieceLocked[counter] = false;

                puzzlePieceTargetPositions[w][h] = counter;


                counter++;
            }
        }
    }

    /**
     * greatestCommonDivisor
     * @param n1
     * @param n2
     * @return gcd of n1, n2
     *
     * Utility class for computing optimal puzzle bitmap scaling.
     */
    public int greatestCommonDivisor (int n1, int n2) {
        if (n2 == 0) return n1;
        return greatestCommonDivisor(n2, n1 % n2);
    }
    
    /**
     * 按照配置参数初始化图片切割
     */
    public void loadPuzzleConfiguration() {
        Bundle grid = config.getBundle("grid");
        Bundle image = config.getBundle("img");
        Bundle pieces = config.getBundle("pieces");

        // Puzzle Grid
        puzzleGridX = grid.getInt("ncols");
        puzzleGridY = grid.getInt("nrows");
        puzzlePieceHeight = puzzleYDimension / puzzleGridY;
        puzzlePieceWidth = puzzleXDimension / puzzleGridX;

        // Fill Puzzle
        puzzlePieceTargetPositions = new int[puzzleGridX][puzzleGridY];
        puzzlePiecesArray = new Bitmap[puzzleGridX * puzzleGridY];
        pieceLocked = new boolean[puzzleGridX * puzzleGridY];

        int counter = 0;
        for (int w = 0; w < puzzleGridX; w++) {
            for (int h = 0; h < puzzleGridY; h++) {
                puzzlePiecesArray[counter] = Bitmap.createBitmap(puzzleResult, w*puzzlePieceWidth, h*puzzlePieceHeight,
                        puzzlePieceWidth, puzzlePieceHeight);

                pieceLocked[counter] = false;

                puzzlePieceTargetPositions[w][h] = counter;

                counter++;
            }
        }

    }

    /** Getters and Setters **/

    public Bitmap[] getPuzzlePiecesArray () {
        return puzzlePiecesArray;
    }

    public int[] getPuzzleDimensions () {
        return new int[] { puzzleXDimension, puzzleYDimension, puzzleGridX, puzzleGridY };
    }

    public int[][] getPuzzlePieceTargetPositions () {
        return puzzlePieceTargetPositions;
    }

    public Bundle getConfig() {
        return config;
    }

    public boolean isBackgroundTextureOn() {
        return backgroundTextureOn;
    }

    public void setBackgroundTextureOn(boolean texture) {
        backgroundTextureOn = texture;
    }

    public Bitmap getBackgroundTexture() {
        return backgroundTexture;
    }

    public void setPieceLocked (int piece, boolean locked) {
        if (piece >= 0 && piece < pieceLocked.length) {
            pieceLocked[piece] = locked;
        }
    }

    public boolean isPieceLocked(int piece) {
        if (piece >= 0 && piece < pieceLocked.length) {
            return pieceLocked[piece];
        } else {
            return false;

        }
    }

    /** Jigsaw Puzzle Message Handlers - Probably Override **/
    public void onJigsawEventPieceGrabbed (int index, int topLeftX, int topLeftY) {

    }

    public void onJigsawEventPieceMoved (int index, int topLeftX, int topLeftY) {

    }

    public void onJigsawEventPieceDropped (int index, int topLeftX, int topLeftY) {

    }
}
