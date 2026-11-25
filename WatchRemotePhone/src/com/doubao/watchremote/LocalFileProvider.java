package com.doubao.watchremote;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** 极简文件 ContentProvider，用于把拍照输出路径交给系统相机 */
public class LocalFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.doubao.watchremote.files";

    public static File dir(Context c) {
        File d = new File(c.getCacheDir(), "ocr");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static Uri uriFor(File f) {
        return Uri.parse("content://" + AUTHORITY + "/" + f.getName());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File resolve(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name == null) return null;
        if (name.contains("/") || name.contains("..")) return null;
        return new File(dir(getContext()), name);
    }

    @Override
    public android.os.ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        File f = resolve(uri);
        if (f == null) throw new FileNotFoundException("bad uri");
        int flags = "w".equals(mode)
                ? android.os.ParcelFileDescriptor.MODE_CREATE
                  | android.os.ParcelFileDescriptor.MODE_READ_WRITE
                : android.os.ParcelFileDescriptor.MODE_READ_ONLY;
        return android.os.ParcelFileDescriptor.open(f, flags);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        File f = resolve(uri);
        String[] cols = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) {
                row[i] = f == null ? "photo.jpg" : f.getName();
            } else if (OpenableColumns.SIZE.equals(cols[i])) {
                row[i] = f == null ? 0L : f.length();
            } else {
                row[i] = null;
            }
        }
        c.addRow(row);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
