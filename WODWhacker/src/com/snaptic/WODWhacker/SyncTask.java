package com.snaptic.WODWhacker;

import java.net.URL;
import java.util.ArrayList;

import com.snaptic.api.SnapticAPI;
import com.snaptic.api.SnapticNote;

import android.os.AsyncTask;
import android.util.Log;

/*
-Params, the type of the parameters sent to the task upon execution.
-Progress, the type of the progress units published during the background computation.
-Result, the type of the result of the background computation.
*/

public class SyncTask extends AsyncTask<String, Integer, Boolean> {
	//Api object to talk to snaptic, move me to my own async class later -htormey
	private SnapticAPI						  mApi;
	private static final String LOGCATNAME = "WODWhacker";//Debug name
	
    protected Boolean doInBackground(String... params) {
      //  int count = urls.length;
        long totalSize = 0;

        //Test out snaptic API.
        String username 				= "";
        String password					= "";
        mApi							= new SnapticAPI(username, password);
        
        ArrayList<SnapticNote> notes 	= new ArrayList<SnapticNote>();
        int getNotesReturnCode 			= mApi.getNotes(notes);
        
        for(SnapticNote n : notes){
        	Log.d(LOGCATNAME, "Note tag " + n.getTags());
        }
        
        try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        return true;
    }

    protected void onProgressUpdate(Integer... progress) {
       
    }

    protected void onPostExecute(Boolean syncStatus) {

    }
}