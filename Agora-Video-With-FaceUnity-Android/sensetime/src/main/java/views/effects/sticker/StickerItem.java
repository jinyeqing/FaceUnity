package views.effects.sticker;

import android.graphics.Bitmap;

public class StickerItem {
    public String name;
    public String path;
    public Bitmap icon;

    public StickerItem(String name, String path, Bitmap icon) {
        this.name = name;
        this.path = path;
        this.icon = icon;
    }
}
