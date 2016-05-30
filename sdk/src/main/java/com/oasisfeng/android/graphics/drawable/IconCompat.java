package com.oasisfeng.android.graphics.drawable;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;

/**
 * Helper for accessing features of {@link android.graphics.drawable.Icon} in a backwards compatible fashion.
 *
 * Created by Oasis on 2016/3/29.
 */
public class IconCompat implements Parcelable {

	static final int TYPE_BITMAP   = 1;
	static final int TYPE_RESOURCE = 2;
	static final int TYPE_DATA     = 3;
	static final int TYPE_URI      = 4;

	private final int mType;

	private ColorStateList mTintList;
	private static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN;	// Drawable.DEFAULT_TINT_MODE
	private PorterDuff.Mode mTintMode = DEFAULT_TINT_MODE;

	// To avoid adding unnecessary overhead, we have a few basic objects that get repurposed
	// based on the value of mType.

	// TYPE_BITMAP: Bitmap
	// TYPE_RESOURCE: Resources
	// TYPE_DATA: DataBytes
	private Object          mObj1;

	// TYPE_RESOURCE: package name
	// TYPE_URI: uri string
	private String          mString1;

	// TYPE_RESOURCE: resId
	// TYPE_DATA: data length
	private int             mInt1;

	// TYPE_DATA: data offset
	private int             mInt2;

	/**
	 * @return The type of image data held in this Icon. One of
	 * {@link #TYPE_BITMAP},
	 * {@link #TYPE_RESOURCE},
	 * {@link #TYPE_DATA}, or
	 * {@link #TYPE_URI}.
	 */
	int getType() {
		return mType;
	}

	/**
	 * @return The {@link android.graphics.Bitmap} held by this {@link #TYPE_BITMAP} IconCompat.
	 */
	Bitmap getBitmap() {
		if (mType != TYPE_BITMAP) throw new IllegalStateException("called getBitmap() on " + this);
		return (Bitmap) mObj1;
	}

	private void setBitmap(final Bitmap b) { mObj1 = b; }

	/**
	 * @return The length of the compressed bitmap byte array held by this {@link #TYPE_DATA} Icon.
	 */
	int getDataLength() {
		if (mType != TYPE_DATA) throw new IllegalStateException("called getDataLength() on " + this);
		synchronized (this) { return mInt1; }
	}

	/**
	 * @return The offset into the byte array held by this {@link #TYPE_DATA} Icon at which
	 * valid compressed bitmap data is found.
	 */
	int getDataOffset() {
		if (mType != TYPE_DATA) throw new IllegalStateException("called getDataOffset() on " + this);
		synchronized (this) { return mInt2; }
	}

	/**
	 * @return The byte array held by this {@link #TYPE_DATA} Icon ctonaining compressed
	 * bitmap data.
	 */
	byte[] getDataBytes() {
		if (mType != TYPE_DATA) throw new IllegalStateException("called getDataBytes() on " + this);
		synchronized (this) { return (byte[]) mObj1; }
	}

	/**
	 * @return The {@link android.content.res.Resources} for this {@link #TYPE_RESOURCE} Icon.
	 */
	Resources getResources() {
		if (mType != TYPE_RESOURCE) throw new IllegalStateException("called getResources() on " + this);
		return (Resources) mObj1;
	}

	/**
	 * @return The package containing resources for this {@link #TYPE_RESOURCE} Icon.
	 */
	String getResPackage() {
		if (mType != TYPE_RESOURCE) throw new IllegalStateException("called getResPackage() on " + this);
		return mString1;
	}

	/** @return The resource ID for this {@link #TYPE_RESOURCE} Icon. */
	int getResId() {
		if (mType != TYPE_RESOURCE) throw new IllegalStateException("called getResId() on " + this);
		return mInt1;
	}

	/** @return The URI (as a String) for this {@link #TYPE_URI} Icon. */
	String getUriString() {
		if (mType != TYPE_URI) {
			throw new IllegalStateException("called getUriString() on " + this);
		}
		return mString1;
	}

	/** @return The {@link android.net.Uri} for this {@link #TYPE_URI} Icon. */
	Uri getUri() { return Uri.parse(getUriString()); }

	private static String typeToString(final int x) {
		switch (x) {
		case TYPE_BITMAP: return "BITMAP";
		case TYPE_DATA: return "DATA";
		case TYPE_RESOURCE: return "RESOURCE";
		case TYPE_URI: return "URI";
		default: return "UNKNOWN";
		}
	}

	private IconCompat(final int type) { this.mType = type; }

	/**
	 * Create an IconCompat pointing to a drawable resource.
	 * @param context The context for the application whose resources should be used to resolve the given resource ID.
	 * @param resId ID of the drawable resource
	 */
	public static IconCompat createWithResource(final Context context, final @DrawableRes int resId) {
		if (context == null) throw new IllegalArgumentException("Context must not be null.");
		final IconCompat rep = new IconCompat(TYPE_RESOURCE);
		rep.mInt1 = resId;
		rep.mString1 = context.getPackageName();
		return rep;
	}

	/**
	 * Create an Icon pointing to a drawable resource.
	 * @param res_pkg Name of the package containing the resource in question
	 * @param res_id ID of the drawable resource
	 */
	public static IconCompat createWithResource(final String res_pkg, final @DrawableRes int res_id) {
		if (res_pkg == null) throw new IllegalArgumentException("Resource package name must not be null.");
		final IconCompat rep = new IconCompat(TYPE_RESOURCE);
		rep.mInt1 = res_id;
		rep.mString1 = res_pkg;
		return rep;
	}

	/**
	 * Create an IconCompat pointing to a bitmap in memory.
	 * @param bits A valid {@link android.graphics.Bitmap} object
	 */
	public static IconCompat createWithBitmap(final Bitmap bits) {
		if (bits == null) throw new IllegalArgumentException("Bitmap must not be null.");
		final IconCompat rep = new IconCompat(TYPE_BITMAP);
		rep.setBitmap(bits);
		return rep;
	}

	/**
	 * Create an Icon pointing to a compressed bitmap stored in a byte array.
	 * @param data Byte array storing compressed bitmap data of a type that
	 *             {@link android.graphics.BitmapFactory}
	 *             can decode (see {@link android.graphics.Bitmap.CompressFormat}).
	 * @param offset Offset into <code>data</code> at which the bitmap data starts
	 * @param length Length of the bitmap data
	 */
	public static IconCompat createWithData(final byte[] data, final int offset, final int length) {
		if (data == null) throw new IllegalArgumentException("Data must not be null.");
		final IconCompat rep = new IconCompat(TYPE_DATA);
		rep.mObj1 = data; rep.mInt1 = length; rep.mInt2 = offset;
		return rep;
	}

	@TargetApi(Build.VERSION_CODES.M) public Icon toIcon() {
		switch (mType) {
		case TYPE_BITMAP: return Icon.createWithBitmap(getBitmap());
		case TYPE_RESOURCE: return Icon.createWithResource(getResPackage(), getResId());
		case TYPE_DATA: return Icon.createWithData(getDataBytes(), getDataOffset(), getDataLength());
		case TYPE_URI: return Icon.createWithContentUri(getUriString());
		}
		return null;
	}

	@Override public String toString() {
		final StringBuilder sb = new StringBuilder("Icon(typ=").append(typeToString(mType));
		switch (mType) {
		case TYPE_BITMAP:
			sb.append(" size=").append(getBitmap().getWidth()).append("x").append(getBitmap().getHeight());
			break;
		case TYPE_RESOURCE:
			sb.append(" pkg=").append(getResPackage()).append(" id=").append(String.format("0x%08x", getResId()));
			break;
		case TYPE_DATA:
			sb.append(" len=").append(getDataLength());
			if (getDataOffset() != 0) sb.append(" off=").append(getDataOffset());
			break;
		case TYPE_URI:
			sb.append(" uri=").append(getUriString());
			break;
		}
		if (mTintList != null) sb.append(" tint=").append(mTintList);
		if (mTintMode != DEFAULT_TINT_MODE) sb.append(" mode=").append(mTintMode);
		sb.append(")");
		return sb.toString();
	}

	private IconCompat(final Parcel in) {
		this(in.readInt());
		switch (mType) {
		case TYPE_BITMAP:
			mObj1 = Bitmap.CREATOR.createFromParcel(in);
			break;
		case TYPE_RESOURCE:
			final String pkg = in.readString();
			final int resId = in.readInt();
			mString1 = pkg;
			mInt1 = resId;
			break;
		case TYPE_DATA:
			final int len = in.readInt();
			final byte[] a = in.createByteArray();//in.readBlob();
			if (len != a.length) {
				throw new RuntimeException("internal unparceling error: blob length ("
						+ a.length + ") != expected length (" + len + ")");
			}
			mInt1 = len;
			mObj1 = a;
			break;
		case TYPE_URI:
			mString1 = in.readString();
			break;
		default:
			throw new RuntimeException("invalid "
					+ this.getClass().getSimpleName() + " type in parcel: " + mType);
		}
		if (in.readInt() == 1) {
			mTintList = ColorStateList.CREATOR.createFromParcel(in);
		}
		mTintMode = /*PorterDuff.*/intToMode(in.readInt());
	}

	private PorterDuff.Mode intToMode(final int i) {
		switch (i) {
		case 0: return PorterDuff.Mode.CLEAR;
		case 1: return PorterDuff.Mode.SRC;
		case 2: return PorterDuff.Mode.DST;
		case 3: return PorterDuff.Mode.SRC_OVER;
		case 4: return PorterDuff.Mode.DST_OVER;
		case 5: return PorterDuff.Mode.SRC_IN;
		case 6: return PorterDuff.Mode.DST_IN;
		case 7: return PorterDuff.Mode.SRC_OUT;
		case 8: return PorterDuff.Mode.DST_OUT;
		case 9: return PorterDuff.Mode.SRC_ATOP;
		case 10: return PorterDuff.Mode.DST_ATOP;
		case 11: return PorterDuff.Mode.XOR;
		case 12: return PorterDuff.Mode.ADD;
		case 13: return PorterDuff.Mode.MULTIPLY;
		case 14: return PorterDuff.Mode.SCREEN;
		case 15: return PorterDuff.Mode.OVERLAY;
		case 16: return PorterDuff.Mode.DARKEN;
		case 17: return PorterDuff.Mode.LIGHTEN;
		}
		return PorterDuff.Mode.CLEAR;
	}

	private int modeToInt(final PorterDuff.Mode mode) {
		switch (mode) {
		case CLEAR: return 0;
		case SRC: return 1;
		case DST: return 2;
		case SRC_OVER: return 3;
		case DST_OVER: return 4;
		case SRC_IN: return 5;
		case DST_IN: return 6;
		case SRC_OUT: return 7;
		case DST_OUT: return 8;
		case SRC_ATOP: return 9;
		case DST_ATOP: return 10;
		case XOR: return 11;
		case DARKEN: return 16;
		case LIGHTEN: return 17;
		case MULTIPLY: return 13;
		case SCREEN: return 14;
		case ADD: return 12;
		case OVERLAY: return 15;
		}
		return 0;
	}

	@Override public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(mType);
		switch (mType) {
		case TYPE_BITMAP:
			getBitmap().writeToParcel(dest, flags);
			break;
		case TYPE_RESOURCE:
			dest.writeString(getResPackage());
			dest.writeInt(getResId());
			break;
		case TYPE_DATA:
			dest.writeInt(getDataLength());
			dest.writeByteArray/* writeBlob */(getDataBytes(), getDataOffset(), getDataLength());
			break;
		case TYPE_URI:
			dest.writeString(getUriString());
			break;
		}
		if (mTintList == null) {
			dest.writeInt(0);
		} else {
			dest.writeInt(1);
			mTintList.writeToParcel(dest, flags);
		}
		dest.writeInt(/*PorterDuff.*/modeToInt(mTintMode));
	}

	@Override public int describeContents() { return 0; }
	public static final Creator<IconCompat> CREATOR = new Creator<IconCompat>() {
		@Override public IconCompat createFromParcel(final Parcel in) { return new IconCompat(in); }
		@Override public IconCompat[] newArray(final int size) { return new IconCompat[size]; }
	};

}
