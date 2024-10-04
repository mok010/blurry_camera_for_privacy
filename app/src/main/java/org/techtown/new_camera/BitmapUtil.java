package org.techtown.new_camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;

public class BitmapUtil {

    // 기존의 원형 영역을 블러 처리하는 함수 (이미 작성된 부분)
    public static Bitmap blurCircularRegion(Context context, Bitmap bitmap, float centerX, float centerY, float radius) {
        Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        for (int y = (int) (centerY - radius); y < (int) (centerY + radius); y++) {
            for (int x = (int) (centerX - radius); x < (int) (centerX + radius); x++) {
                if (Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) <= Math.pow(radius, 2)) {
                    applyBlurEye(bitmap, blurredBitmap, x, y, 10);
                }
            }
        }

        return blurredBitmap;
    }

    /**
     * 사각형 영역을 블러 처리하는 함수
     *
     * @param context  컨텍스트 (필요시 사용할 수 있음)
     * @param bitmap   원본 비트맵
     * @param rect     블러링할 사각형 영역 (Rect 객체)
     * @return 블러 처리된 비트맵
     */
    public static Bitmap blurRectangularRegion(Context context, Bitmap bitmap, Rect rect, PointF index) {
        // 살색 범위 설정 (Hue, Saturation, Value의 범위 설정)
        float hueRange = 10; // ±10도 허용
        float saturationRange = 0.2f; // ±20% 허용
        float valueRange = 0.2f; // ±20% 허용


        // 원본 비트맵을 수정 가능하게 복사
        Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 손목으로 하려고 했는데 소매가 있는 경우가 많아서 index로
        int indexColor = bitmap.getPixel((int) index.x, (int) index.y);

        // 살색 판단 기준을 설정 (예: 손목 색상에서 유사한 범위의 색상)
        float[] wristHsv = new float[3];
        Color.colorToHSV(indexColor, wristHsv);

        // 사각형 영역 내의 픽셀에 블러 처리
        for (int y = rect.top; y <= rect.bottom; y++) {
            for (int x = rect.left; x <= rect.right; x++) {
                int pixelColor = bitmap.getPixel(x, y);
                float[] pixelHsv = new float[3];
                Color.colorToHSV(pixelColor, pixelHsv);
                if (Math.abs(pixelHsv[0] - wristHsv[0]) <= hueRange &&
                        Math.abs(pixelHsv[1] - wristHsv[1]) <= saturationRange &&
                        Math.abs(pixelHsv[2] - wristHsv[2]) <= valueRange){
                    applyBlurHand(bitmap, blurredBitmap, x, y, 10); // 블러 반경 적용
                }

            }
        }

        return blurredBitmap;
    }

    // 주어진 좌표의 픽셀에 블러 처리하는 함수 (눈)
    private static void applyBlurEye(Bitmap original, Bitmap blurred, int x, int y, int radius) {
        int r = 0, g = 0, b = 0;
        int count = 0;

        for (int ky = -radius; ky <= radius; ky++) {
            for (int kx = -radius; kx <= radius; kx++) {
                int newX = x + kx;
                int newY = y + ky;

                if (newX >= 0 && newX < original.getWidth() && newY >= 0 && newY < original.getHeight()) {
                    int pixel = original.getPixel(newX, newY);
                    r += Color.red(pixel);
                    g += Color.green(pixel);
                    b += Color.blue(pixel);
                    count++;
                }
            }
        }

        if (count > 0) {
            r /= count;
            g /=count;
            b /=count;

            //테스트용-빨간색
//            r = 255;
//            g =1;
//            b =1;
            blurred.setPixel(x, y, Color.rgb(r, g, b));
        }
    }
    // 주어진 좌표의 픽셀에 블러 처리하는 함수 (손)
    private static void applyBlurHand(Bitmap original, Bitmap blurred, int x, int y, int radius) {
        int r = 0, g = 0, b = 0;
        int count = 0;

        for (int ky = -radius; ky <= radius; ky++) {
            for (int kx = -radius; kx <= radius; kx++) {
                int newX = x + kx;
                int newY = y + ky;

                if (newX >= 0 && newX < original.getWidth() && newY >= 0 && newY < original.getHeight()) {
                    int pixel = original.getPixel(newX, newY);
                    r += Color.red(pixel);
                    g += Color.green(pixel);
                    b += Color.blue(pixel);
                    count++;
                }
            }
        }

        if (count > 0) {
            r /= count;
            g /=count;
            b /=count;

            //테스트용-빨간색
//            r = 255;
//            g =1;
//            b =1;
            blurred.setPixel(x, y, Color.rgb(r, g, b));
        }
    }
}
