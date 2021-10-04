package com.ttv.facedemo.common;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
 */
public class FileChooser implements OnClickListener {
	public enum DialogType {
		SELECT_FILE, SELECT_DIRECTORY, SAVE_AS
	}

	/**
	 * 
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	public interface FileSelectionCallback {
		public void onSelect(File file);
	}

	private FileSelectionCallback callback;

	//private static final String TAG = "FileChooserDialog";

	private static final String PARENT_FILE_NAME = "..";

	private File currentFile;
	private ArrayList<String> fileList;

	/**
	 * 
	 */
	private FilenameFilter filenameFilter = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String filename) {
			File file = new File(dir, filename);
			return !file.isHidden();
		}
	};

	private Context context;
	private String title;

	private Dialog currentDialog;
	private DialogType dialogType = DialogType.SELECT_FILE;

	private Drawable folderDrawable;
	private Drawable fileDrawable;
	private Drawable upFolderDrawable;

	/**
	 * @param context
	 * @param title
	 * @param dialogType
	 * @param startingFile
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	public FileChooser(Context context, String title, DialogType dialogType,
			File startingFile) {
		this.dialogType = dialogType;
		this.title = title;
		this.context = context;
		if (startingFile == null || !startingFile.exists()) {
			startingFile = Environment.getExternalStorageDirectory();
		}
		if (!startingFile.exists()) {
			startingFile = new File("/");
		}
		if (startingFile.exists()) {
			this.currentFile = startingFile;
		}

		ImageLib imgLib = new ImageLib();
		folderDrawable = imgLib.get(ImageLib.FOLDER);
		fileDrawable = imgLib.get(ImageLib.FILE_2);
		upFolderDrawable = imgLib.get(ImageLib.UP_FOLDER_1);

	}

	/**
	 * @param filenameFilter
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	public void setFilelistFilter(FilenameFilter filenameFilter) {
		this.filenameFilter = filenameFilter;
	}

	/**
	 * @param commaSeparatedExtensions
	 * @param hiddenAllowed
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	public void setFilelistFilter(final String commaSeparatedExtensions,
			final boolean hiddenAllowed) {
		this.filenameFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				File file = new File(dir, filename);
				if (file.isHidden()) {
					return hiddenAllowed;
				}
				if (file.isDirectory()) {
					return true;
				}
				String fileExtension = getFileExtension(filename);

				String[] allowedExtensions = commaSeparatedExtensions
						.split(",");
				for (String allowedExtension : allowedExtensions) {
					if (fileExtension.equals(allowedExtension)) {
						return true;
					}
				}
				return false;
			}
		};
	}

	protected String getFileExtension(String filename) {
		int lastIndexOfDot = filename.lastIndexOf(".");
		if (lastIndexOfDot + 1 < filename.length()) {
			return filename.substring(lastIndexOfDot + 1);
		}
		return null;
	}

	/**
	 * @param callback
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	public void show(FileSelectionCallback callback) {
		this.callback = callback;
		loadFilelist();
	}

	/**
	 * 
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	private void loadFilelist() {
		if (currentDialog != null) {
			currentDialog.dismiss();
		}

		Builder builder = new Builder(context);
		builder.setTitle(title);

		addAdapter(context, builder);

		if (dialogType == DialogType.SELECT_DIRECTORY
				|| dialogType == DialogType.SAVE_AS) {
			addPositiveButton(context, builder);
		}
		currentDialog = builder.show();
	}

	/**
	 * @param context
	 * @param builder
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	private void addAdapter(Context context, Builder builder) {
		fileList = new ArrayList<String>();

		if (currentFile.getParentFile() != null) {
			fileList.add(PARENT_FILE_NAME);
		}

		File[] childFiles = currentFile.listFiles(filenameFilter);
		if (childFiles != null) {
			Arrays.sort(childFiles, new Comparator<File>() {

				@Override
				public int compare(File lhs, File rhs) {
					if (lhs.isDirectory() && rhs.isFile()) {
						return -1;
					} else if (rhs.isDirectory() && lhs.isFile()) {
						return 1;
					}
					return lhs.getName().compareToIgnoreCase(rhs.getName());
				}
			});
			for (File childFile : childFiles) {
				fileList.add(childFile.getName());
			}
		}

		ListAdapter adapter;
		adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_list_item_1, fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView view = (TextView) super.getView(position, convertView,
						parent);
				if (fileList.get(position).equals(PARENT_FILE_NAME)) {
					view.setCompoundDrawablesWithIntrinsicBounds(
							upFolderDrawable, null, null, null);
				} else {
					File file = new File(currentFile, fileList.get(position));
					if (file.isDirectory()) {
						view.setCompoundDrawablesWithIntrinsicBounds(
								folderDrawable, null, null, null);
					} else {
						view.setCompoundDrawablesWithIntrinsicBounds(
								fileDrawable, null, null, null);
					}
				}

				return view;
			}
		};
		builder.setAdapter(adapter, this);
	}

	/**
	 * @param context
	 * @param builder
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	private void addPositiveButton(final Context context, Builder builder) {
		String positiveButtonText = "Ok";

		OnClickListener positiveButtonClickListener = new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				if (dialogType == DialogType.SAVE_AS) {
					loadFilenameDialog("");
				} else {
					completeSelection("");
				}

			}
		};
		builder.setPositiveButton(positiveButtonText,
				positiveButtonClickListener);
	}

	/**
	 * @param filename
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	protected void completeSelection(String filename) {
		currentDialog.dismiss();
		if (callback != null) {
			callback.onSelect(new File(currentFile, filename));
			callback = null;
		}
	}

	/**
	 * @param filename
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	protected void loadFilenameDialog(String filename) {
		if (currentDialog != null) {
			currentDialog.dismiss();
		}

		Builder builder = new Builder(context);
		builder.setTitle("Please type a filename");

		final EditText et = new EditText(context);
		et.setHint("filename");
		et.setText(filename);
		builder.setView(et);

		builder.setPositiveButton("Go", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String filename = et.getText().toString();
				if (filename == null || filename.length() <= 0) {
					Toast.makeText(context, "Invalid name", Toast.LENGTH_SHORT)
							.show();
				} else {
					if (new File(currentFile, filename).exists()) {
						loadDeleteExistingFileDialog(filename);
					} else {
						completeSelection(filename);
					}
				}
			}
		});

		builder.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0) {
				loadFilelist();
			}
		});
		currentDialog = builder.show();
	}

	/**
	 * @param newFileName
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	protected void loadDeleteExistingFileDialog(final String newFileName) {
		Builder builder = new Builder(context);
		builder.setTitle("File already exist. Delete existing ?");
		builder.setPositiveButton("Yes", new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				File selectedFile = new File(currentFile, newFileName);
				selectedFile.delete();
				try {
					selectedFile.createNewFile();
					completeSelection(newFileName);
				} catch (IOException e) {
					e.printStackTrace();
					loadFilelist();
				}
			}
		});

		builder.setNegativeButton("No", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				loadFilelist();
			}
		});
		builder.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0) {
				loadFilelist();
			}
		});
		builder.show();

	}

	/*
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )(non-Javadoc)
	 * 
	 * @see
	 * android.content.DialogInterface.OnClickListener#onClick(android.content
	 * .DialogInterface, int)
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (fileList.get(which).equals(PARENT_FILE_NAME)) {
			currentFile = currentFile.getParentFile();
			loadFilelist();
		} else {
			File selectedFile = new File(currentFile, fileList.get(which));
			if (selectedFile.isDirectory()) {
				currentFile = selectedFile;
				loadFilelist();
			} else {
				if (dialogType == DialogType.SAVE_AS) {
					loadFilenameDialog(selectedFile.getName());
				} else {
					completeSelection(selectedFile.getName());
				}
			}
		}
	}

	/**
	 * 
	 * @author MD IMRAN HASAN HIRA ( imranhasanhira@gmail.com )
	 */
	class ImageLib {
		public static final String FOLDER = "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";
		public static final String FILE_1 = "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";
		public static final String FILE_2 = "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";
		public static final String UP_FOLDER_1 = "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";

		BitmapDrawable get(String base64Enc) {
			byte[] decodedString = Base64.decode(base64Enc, Base64.DEFAULT);
			Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0,
					decodedString.length);
			return new BitmapDrawable(context.getResources(), bitmap);
		}
	}

}
