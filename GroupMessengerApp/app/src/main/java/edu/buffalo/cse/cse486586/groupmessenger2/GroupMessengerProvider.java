package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.UserDictionary;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
      //  List<String> cntData = new ArrayList<String>();
        OutputStream f_0utStrm;
        try {

            /*Reference-https://developer.android.com/training/data-storage/files#WriteFileInternal*/

            /*Storing data as a file in the internal storage.
             * openFileOutput() gets a FileOutputStream to write a file in the file directory.
             * Message sequence number is used as the file name
             * message is being written to the file.
             *
             * */
            f_0utStrm = getContext().openFileOutput(values.getAsString("key"), Context.MODE_PRIVATE);
            f_0utStrm.write(values.getAsString("value").getBytes());
           /* cntData.add(values.getAsString("key")+ ":"+values.getAsString("value").getBytes());
            System.out.println(cntData);
            Log.e(TAG, cntData.toString());*/
            f_0utStrm.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */


        InputStream f_inStrm;
        String message="";

        /*
         * once the message is written into the file , each message is stored in each row with its sequence number
         * each row can be queried using "selection" which is a "key".
         *
         * */

        try {
            /*Reference-https://developer.android.com/reference/android/content/Context#openFileInput(java.lang.String)*/
            f_inStrm = getContext().openFileInput(selection);
            /*Reference-https://stackoverflow.com/questions/2864117/read-data-from-a-text-file-using-java*/
            BufferedReader br = new BufferedReader(new InputStreamReader(f_inStrm));
            message = br.readLine();
            f_inStrm.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v("query", selection);
        /*
         * MatrixCursor takes column names as the constructor arguments*/
        /*Reference-https://developer.android.com/reference/android/database/MatrixCursor#MatrixCursor(java.lang.String[])*/
        MatrixCursor cursor = new MatrixCursor(new String[] {"key", "value"});
        /*Reference-https://developer.android.com/reference/android/database/MatrixCursor#addRow(java.lang.Object[])*/
        cursor.addRow(new String[] {selection,message});

        return  cursor;



    }
}
