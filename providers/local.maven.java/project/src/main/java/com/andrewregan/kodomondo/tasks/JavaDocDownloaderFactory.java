/**
 * 
 */
package com.andrewregan.kodomondo.tasks;

import com.andrewregan.kodomondo.fs.api.IFileObject;

/**
 * TODO
 *
 * @author andrewregan
 *
 */
public interface JavaDocDownloaderFactory {

	JavaDocDownloadTask create( IFileObject artifact);
}
