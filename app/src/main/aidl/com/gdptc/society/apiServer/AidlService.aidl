// ICoreAidlInterface.aidl
package com.gdptc.society.apiServer;

// Declare any non-default types here with import statements
import com.gdptc.society.apiServer.AccountInfo;

interface AidlService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void bindAccountInfo(in AccountInfo accountInfo);
}
