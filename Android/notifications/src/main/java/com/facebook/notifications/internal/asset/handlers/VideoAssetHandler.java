// Copyright (c) 2016-present, Facebook, Inc. All rights reserved.
//
// You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
// copy, modify, and distribute this software in source code or binary form for use
// in connection with the web services and APIs provided by Facebook.
//
// As with any software that integrates with the Facebook platform, your use of
// this software is subject to the Facebook Developer Principles and Policies
// [http://developers.facebook.com/policy/]. This copyright notice shall be
// included in all copies or substantial portions of the software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.facebook.notifications.internal.asset.handlers;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.facebook.notifications.internal.asset.Asset;
import com.facebook.notifications.internal.asset.AssetManager;
import com.facebook.notifications.internal.utilities.InvalidParcelException;
import com.facebook.notifications.internal.view.VideoAssetView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles assets of the bitmap type
 */
public class VideoAssetHandler implements AssetManager.AssetHandler<VideoAssetHandler.VideoAsset> {
  /**
   * A resource implementation for Bitmaps read from disk
   */
  public static class VideoAsset implements Asset {
    public static final Creator<VideoAsset> CREATOR = new Creator<VideoAsset>() {
      @Override
      public VideoAsset createFromParcel(Parcel source) {
        return new VideoAsset(source);
      }

      @Override
      public VideoAsset[] newArray(int size) {
        return new VideoAsset[size];
      }
    };
    private final @NonNull File createdFrom;
    private final int frameWidth;
    private final int frameHeight;

    private VideoAsset(@NonNull File createdFrom) {
      this.createdFrom = createdFrom;

      MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
      metadataRetriever.setDataSource(createdFrom.getAbsolutePath());

      String width = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
      String height = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

      frameWidth = Integer.valueOf(width);
      frameHeight = Integer.valueOf(height);
    }

    private VideoAsset(@NonNull Parcel parcel) {
      createdFrom = new File(parcel.readString());
      frameWidth = parcel.readInt();
      frameHeight = parcel.readInt();
    }

    @NonNull
    public File getCreatedFrom() {
      return createdFrom;
    }

    public int getFrameWidth() {
      return frameWidth;
    }

    public int getFrameHeight() {
      return frameHeight;
    }

    @NonNull
    @Override
    public String getType() {
      return TYPE;
    }

    @Override
    public void validate() throws InvalidParcelException {
      if (!createdFrom.exists()) {
        throw new InvalidParcelException(
          new FileNotFoundException(
            "Bitmap cache file does not exist: " + createdFrom.getAbsolutePath()
          )
        );
      }
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(createdFrom.getAbsolutePath());
      dest.writeInt(frameWidth);
      dest.writeInt(frameHeight);
    }
  }

  public static final String TYPE = "Video";
  private static final String LOG_TAG = VideoAssetHandler.class.getCanonicalName();

  @Nullable
  @Override
  public Set<URL> getCacheURLs(@NonNull JSONObject payload) {
    try {
      URL url = new URL(payload.getString("url"));
      Set<URL> set = new HashSet<>();
      set.add(url);

      return set;
    } catch (MalformedURLException ex) {
      return null;
    } catch (JSONException ex) {
      return null;
    }
  }

  @Nullable
  @Override
  public VideoAsset createAsset(@NonNull JSONObject payload, @NonNull AssetManager.AssetCache cache) {
    try {
      URL url = new URL(payload.getString("url"));
      File cacheFile = cache.getCachedFile(url);
      if (cacheFile == null) {
        return null;
      }

      return new VideoAsset(cacheFile);
    } catch (MalformedURLException ex) {
      Log.e(LOG_TAG, "JSON key 'url' was not a valid URL", ex);
      return null;
    } catch (JSONException ex) {
      Log.e(LOG_TAG, "JSON exception", ex);
      return null;
    }
  }

  @NonNull
  @Override
  public View createView(@NonNull VideoAsset asset, @NonNull Context context) {
    return new VideoAssetView(context, asset);
  }
}

