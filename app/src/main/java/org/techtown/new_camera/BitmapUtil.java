package org.techtown.new_camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BitmapUtil {

    // 특정 영역을 블러 처리하는 함수
    public static Bitmap blurRegion(Context context, Bitmap bitmap, Rect region) {
        Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Bitmap regionBitmap = Bitmap.createBitmap(blurredBitmap, region.left, region.top,
                region.width(), region.height());

        // Blur 처리 적용
        Bitmap blurredRegion = blur(context, regionBitmap);

        Canvas canvas = new Canvas(blurredBitmap);
        canvas.drawBitmap(blurredRegion, region.left, region.top, null);

        return blurredBitmap;
    }

    // RenderScript를 사용하여 블러 효과 적용
    public static Bitmap blur(Context context, Bitmap bitmap) {
        RenderScript rs = RenderScript.create(context);
        Allocation input = Allocation.createFromBitmap(rs, bitmap);
        Allocation output = Allocation.createTyped(rs, input.getType());

        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(10);  // 블러 반경 설정
        script.setInput(input);
        script.forEach(output);

        output.copyTo(bitmap);
        rs.destroy();

        return bitmap;
    }
}
