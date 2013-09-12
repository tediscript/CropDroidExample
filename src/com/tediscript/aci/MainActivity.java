package com.tediscript.aci;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lib.image.CropImage;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

//it use https://github.com/tediscript/CropDroid

public class MainActivity extends Activity {

	private ImageView imageView;
	private static final int VIA_CAMERA = 1;
	private static final int VIA_GALLERY = 2;
	private static final int VIA_CROP = 3;
	private ArrayAdapter<String> adapter;
	private AlertDialog.Builder builder;
	private AlertDialog dialog;
	private static final String[] items = new String[] { "Take Picture",
			"Open Gallery" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initComponents();
	}

	private void initComponents() {
		imageView = (ImageView) findViewById(R.id.imageView);
		imageView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				imageViewOnClickListener(v);
			}
		});

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.select_dialog_item, items);

		builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Source");
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item) {
				dialogOnClickListener(dialog, item);
			}

		});

		dialog = builder.create();
	}

	private void imageViewOnClickListener(View v) {
		dialog.show();
	}

	private void dialogOnClickListener(DialogInterface dialog, int item) {
		if (item == 0) {
			openCamera();
		} else {
			openGallery();
		}
	}

	private void openCamera() {
		File cameraTempFile = getTempFile("camera_temp.jpg");
		if (cameraTempFile != null) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(cameraTempFile));
			try {
				intent.putExtra("return-data", true);
				startActivityForResult(intent, VIA_CAMERA);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			// cannot create cameraTempFile
		}
	}

	private void openGallery() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(
				Intent.createChooser(intent, "Complete action using"),
				VIA_GALLERY);
	}

	private void cameraOnResultListener() {
		File tempFile = getTempFile("camera_temp.jpg");
		Uri uri = Uri.fromFile(tempFile);
		doCrop(uri);
	}

	private void galleryOnResultListener(Uri uri) {
		try {
			File tempFile = getTempFile("gallery_temp.jpg");
			InputStream inputStream = getContentResolver().openInputStream(uri);
			FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
			copyStream(inputStream, fileOutputStream);
			fileOutputStream.close();
			inputStream.close();

			doCrop(Uri.fromFile(tempFile));
		} catch (Exception e) {
			Log.e("", "Error while creating temp file", e);
		}
	}

	public static void copyStream(InputStream input, OutputStream output)
			throws IOException {

		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
	}

	private void doCrop(Uri uri) {
		Intent intent = new Intent(this, CropImage.class);
		intent.putExtra(CropImage.IMAGE_PATH, uri.getPath());
		intent.putExtra(CropImage.SCALE, true);
		intent.putExtra(CropImage.ASPECT_X, 1);
		intent.putExtra(CropImage.ASPECT_Y, 1);
		intent.putExtra(CropImage.OUTPUT_X, 300);
		intent.putExtra(CropImage.OUTPUT_Y, 300);
		startActivityForResult(intent, VIA_CROP);
	}

	private File getTempFile(String fileName) {
		File file = null;
		if (isSDCARDMounted()) {
			file = new File(Environment.getExternalStorageDirectory(), fileName);
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}

	private boolean isSDCARDMounted() {
		String status = Environment.getExternalStorageState();
		return (status.equals(Environment.MEDIA_MOUNTED)) ? true : false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {

			switch (requestCode) {
			case VIA_CAMERA:
				cameraOnResultListener();
				break;

			case VIA_GALLERY:
				galleryOnResultListener(data.getData());
				break;

			case VIA_CROP:
				String path = data.getStringExtra(CropImage.IMAGE_PATH);
				if (path == null) {
					return;
				}

				Bitmap bitmap = BitmapFactory.decodeFile(path);
				imageView.setImageBitmap(bitmap);
				break;
			}
		} else {
			// do nothing
			// result is not OK
		}
	}

}
