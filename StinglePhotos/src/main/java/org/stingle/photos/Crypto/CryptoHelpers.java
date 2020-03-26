package org.stingle.photos.Crypto;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;
import org.stingle.photos.Db.Objects.StingleDbAlbum;
import org.stingle.photos.Db.Query.AlbumsDb;
import org.stingle.photos.StinglePhotosApplication;
import org.stingle.photos.Sync.SyncManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class CryptoHelpers {

	public static String encryptParamsForServer(HashMap<String, String> params) throws CryptoException {
		return  encryptParamsForServer(params, null, null);
	}

	public static String encryptParamsForServer(HashMap<String, String> params, byte[] serverPK, byte[] privateKey) throws CryptoException {
		JSONObject json = new JSONObject(params);

		if(serverPK == null){
			serverPK = StinglePhotosApplication.getCrypto().getServerPublicKey();
		}
		if(privateKey == null){
			privateKey = StinglePhotosApplication.getKey();
		}

		if(serverPK == null || privateKey == null){
			return "";
		}

		return Crypto.byteArrayToBase64Default(
				StinglePhotosApplication.getCrypto().encryptCryptoBox(
						json.toString().getBytes(),
						serverPK,
						privateKey
				)
		);
	}


	public static byte[] decryptDbFile(Context context, int folder, int folderId, String headers, boolean isThumb, byte[] bytes) throws IOException, CryptoException {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		return decryptDbFile(context, folder, folderId, headers, isThumb, in, null, null);
	}

	public static byte[] decryptDbFile(Context context, int folder, int folderId, String headers, boolean isThumb, byte[] bytes, AsyncTask<?,?,?> task) throws IOException, CryptoException {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		return decryptDbFile(context, folder, folderId, headers, isThumb, in, null, task);
	}

	public static byte[] decryptDbFile(Context context, int folder, int folderId, String headers, boolean isThumb, InputStream in) throws IOException, CryptoException {
		return decryptDbFile(context, folder, folderId, headers, isThumb, in, null, null);
	}

	public static byte[] decryptDbFile(Context context, int folder, int folderId, String headers, boolean isThumb, InputStream in, CryptoProgress progress, AsyncTask<?,?,?> task) throws IOException, CryptoException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		decryptDbFile(context, folder, folderId, headers, isThumb, in, out, progress, task);

		return out.toByteArray();
	}

	public static void decryptDbFile(Context context, int folder, int folderId, String headers, boolean isThumb, InputStream in, OutputStream out, CryptoProgress progress, AsyncTask<?,?,?> task) throws IOException, CryptoException {
		Crypto crypto = StinglePhotosApplication.getCrypto();
		Crypto.Header header = null;
		if(folder == SyncManager.FOLDER_ALBUM){
			AlbumsDb albumsDb = new AlbumsDb(context);
			StingleDbAlbum album = albumsDb.getAlbumById(folderId);
			Crypto.AlbumData albumData = crypto.parseAlbumData(album.data);

			if(isThumb) {
				header = crypto.getThumbHeaderFromHeadersStr(headers, albumData.privateKey, Crypto.base64ToByteArrayDefault(album.albumPK));
			}
			else{
				header = crypto.getFileHeaderFromHeadersStr(headers, albumData.privateKey, Crypto.base64ToByteArrayDefault(album.albumPK));
			}
		}
		else {
			if(isThumb) {
				header = crypto.getThumbHeaderFromHeadersStr(headers);
			}
			else{
				header = crypto.getFileHeaderFromHeadersStr(headers);
			}
		}

		crypto.decryptFile(in, out, progress, task, header);
	}
}
