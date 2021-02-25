/**
 * Copyright 2004-present, Facebook, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.profilo.mmapbuf;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.SoLoader;
import java.io.File;
import java.util.UUID;
import javax.annotation.Nullable;

@DoNotStrip
public class MmapBufferManager {

  private static final String LOG_TAG = "Profilo/MmapBufferMngr";

  static {
    SoLoader.loadLibrary("profilo_mmapbuf");
  }

  @DoNotStrip private final HybridData mHybridData;
  private final MmapBufferFileHelper mFileHelper;
  private final Context mContext;
  private final long mConfigId;

  @DoNotStrip
  private static native HybridData initHybrid();

  public MmapBufferManager(long configId, File folder, Context context) {
    mConfigId = configId;
    mContext = context;
    mFileHelper = new MmapBufferFileHelper(folder);
    mHybridData = initHybrid();
  }

  private int getVersionCode() {
    if (mContext == null) {
      return 0;
    }
    PackageManager pm = mContext.getPackageManager();
    if (pm == null) {
      return 0;
    }
    PackageInfo pi;
    try {
      pi = pm.getPackageInfo(mContext.getPackageName(), 0);
    } catch (PackageManager.NameNotFoundException | RuntimeException e) {
      return 0;
    }
    return pi.versionCode;
  }

  @Nullable
  public Buffer allocateBuffer(int size, boolean filebacked) {
    if (filebacked) {
      String fileName = MmapBufferFileHelper.getBufferFilename(UUID.randomUUID().toString());
      String mmapBufferPath = mFileHelper.ensureFilePath(fileName);
      if (mmapBufferPath == null) {
        return null;
      }
      return nativeAllocateBuffer(size, mmapBufferPath, getVersionCode(), mConfigId);
    } else {
      return nativeAllocateBuffer(size);
    }
  }

  public synchronized boolean deallocateBuffer(Buffer buffer) {
    return nativeDeallocateBuffer(buffer);
  }

  @DoNotStrip
  @Nullable
  private native Buffer nativeAllocateBuffer(int size);

  @DoNotStrip
  @Nullable
  private native Buffer nativeAllocateBuffer(int size, String path, int buildId, long configId);

  @DoNotStrip
  private native boolean nativeDeallocateBuffer(Buffer buffer);
}
