//
//  Copyright 2011 Catch.com, Inc.
//  
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  
//      http://www.apache.org/licenses/LICENSE-2.0
//  
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.example.CatchApiDemo;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.catchnotes.api.CatchAPI;
import com.catchnotes.api.CatchAccount;
import com.catchnotes.api.CatchNote;

public class Dashboard extends Activity {

    public static final String APP_NAME = "CatchApiDemo";
    protected static final int DIALOG_SIGN_IN = 0;
    protected static final int DIALOG_COMPOSE_NOTE = 1;

    /////////////////////////////////////////////////////////
    // You get a token when you sign in.
    // You'll need it each time you create a CatchAPI object.
    /////////////////////////////////////////////////////////
    protected String mAccessToken;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Sign in button
        final Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_SIGN_IN);
            }
        });

        // Fetch Notes button
        final Button fetchButton = (Button) findViewById(R.id.fetch_button);
        fetchButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mAccessToken == null) {
                    Toast.makeText(Dashboard.this, "Not signed in!", Toast.LENGTH_SHORT).show();
                } else {
                    // start Fetch Notes task
                    new FetchNotesTask(getApplicationContext()).execute();
                }
            }
        });

        // New Note button
        final Button createButton = (Button) findViewById(R.id.create_button);
        createButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mAccessToken == null) {
                    Toast.makeText(Dashboard.this, "Not signed in!", Toast.LENGTH_SHORT).show();
                } else {
                    showDialog(DIALOG_COMPOSE_NOTE);
                }
            }
        });
    }

    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder;

        switch (id) {
            case DIALOG_SIGN_IN:
            {
                final View inflated = LayoutInflater.from(this).inflate(R.layout.sign_in, null);
                builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.sign_in_dialog_title)
                    .setCancelable(true)
                    .setView(inflated)
                    .setPositiveButton(getString(R.string.sign_in), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            EditText userEdit = (EditText) inflated.findViewById(R.id.username_edittext);
                            EditText passEdit = (EditText) inflated.findViewById(R.id.password_edittext);
                            // start Sign In task
                            new SignInTask(getApplicationContext())
                                .execute(userEdit.getText().toString(), passEdit.getText().toString());
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
                dialog = builder.create();
                break;
            }
    
            case DIALOG_COMPOSE_NOTE:
            {
                final View inflated = LayoutInflater.from(this).inflate(R.layout.new_note, null);
                builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.new_note_dialog_title)
                    .setCancelable(true)
                    .setView(inflated)
                    .setPositiveButton(getString(R.string.add_note), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            EditText composer = (EditText) inflated.findViewById(R.id.compose_edittext);
                            // start Create Note task
                            new CreateNoteTask(getApplicationContext()).execute(composer.getText().toString());
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
                dialog = builder.create();
                break;
            }
    
            default:
                dialog = null;
                break;
        }

        return dialog;
    }
    
    
    ////////////////////////////////////////////
    // Example sign-in task.  Sets mAccessToken.
    ////////////////////////////////////////////
    private class SignInTask extends AsyncTask<String, Void, String> {
        
        private Context mContext;
        private CatchAccount mAccount; 
        
        public SignInTask(Context context) {
            mContext = context;
            mAccount = new CatchAccount();
        }
        
        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            
            // Create a CatchAPI object.
            CatchAPI api = new CatchAPI(APP_NAME, mContext);
            
            // Sign in.
            // The third parameter (CatchAccount) will be populated with account 
            // information, including the access token that you can use to avoid 
            // signing in each time you need to create a new CatchAPI object.
            int result = api.signIn(username, password, mAccount);
            if (result == CatchAPI.RESULT_OK) {
                Log.d(APP_NAME, "Signed in.");
                Log.d(APP_NAME, "Account ID: "+mAccount.id);
                Log.d(APP_NAME, "Account Name: "+mAccount.username);
                Log.d(APP_NAME, "Account Email: "+mAccount.email);
                return mAccount.auth_token;
                
            } else {
                Log.d(APP_NAME, "Couldn't sign in.  Error code: "+result);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String accessToken) {
            Dashboard.this.mAccessToken = accessToken;
            if (accessToken == null) {
                Toast.makeText(Dashboard.this, "Sign-in Failed!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Dashboard.this, "User "+mAccount.username+" signed in.", 
                        Toast.LENGTH_SHORT).show();
            }
        }
        
    }
    
    
    //////////////////////////////
    // Example note fetching task.
    //////////////////////////////
    private class FetchNotesTask extends AsyncTask<Void, Void, Void> {
        
        private Context mContext;
        private int mResult;
        
        public FetchNotesTask(Context context) {
            mContext = context;
        }
        
        protected String formatDate(long timestamp) {
            return DateUtils.formatDateTime(mContext, timestamp, 
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | 
                    DateUtils.FORMAT_SHOW_TIME);
        }

        @Override
        protected Void doInBackground(Void... params) {
            
            // Create a CatchAPI object.  Use the access token that we got from 
            // a previous sign-in.
            CatchAPI api = new CatchAPI(APP_NAME, mContext);
            api.setAccessToken(Dashboard.this.mAccessToken);
            
            // Call getNotes to fill a list with CatchNote objects.
            ArrayList<CatchNote> notes = new ArrayList<CatchNote>();
            mResult = api.getNotes(notes);
            if (mResult == CatchAPI.RESULT_OK) {
                Log.d(APP_NAME, "Displaying "+notes.size()+" notes...");
                for (CatchNote note : notes) {
                    // Print note info.
                    Log.d(APP_NAME, "----"+
                            "  Note ID: "+note.id+
                            "  Created: "+formatDate(note.creationTime)+
                            "  Modified: "+formatDate(note.modificationTime)+
                            "  ----");
                    // Print note summary.  (Full text is note.text)
                    Log.d(APP_NAME, note.summary.toString());
                }
                Log.d(APP_NAME, "Displayed "+notes.size()+" notes.");
                
            } else {
                Log.d(APP_NAME, "Couldn't fetch notes.  Error code: "+mResult);
                return null;
            }

            return null;
        }
        
        @Override
        protected void onPostExecute(Void unused) {
            if (mResult == CatchAPI.RESULT_OK) {
                Toast.makeText(Dashboard.this, "Notes Fetched", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Dashboard.this, "Note Fetch Failed!", 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


    //////////////////////////////
    // Example note creation task.
    //////////////////////////////
    private class CreateNoteTask extends AsyncTask<String, Void, String> {
        
        private Context mContext;
        
        public CreateNoteTask(Context context) {
            mContext = context;
        }

        @Override
        protected String doInBackground(String... params) {
            String text = params[0];
            
            // Create CatchAPI and set the access token.
            CatchAPI api = new CatchAPI(APP_NAME, mContext);
            api.setAccessToken(Dashboard.this.mAccessToken);
            
            // Create a new note.
            CatchNote note = new CatchNote();
            
            // set the creation and modification timestamps.
            long timestamp = System.currentTimeMillis();
            note.creationTime = timestamp;
            note.modificationTime = timestamp;
            
            // set the note text.
            note.text = text;
            
            // Call addNote to add the note to the account.  The note object  
            // will be updated with extra information, such as the note ID.
            int result = api.addNote(note);
            if (result == CatchAPI.RESULT_OK) {
                Log.d(APP_NAME, "Created note ID: "+note.id);
                return note.id;
                
            } else {
                Log.d(APP_NAME, "Couldn't add note.  Error code: "+result);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String noteId) {
            if (noteId != null) {
                Toast.makeText(Dashboard.this, "Note Added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Dashboard.this, "Note Creation Failed!", 
                        Toast.LENGTH_SHORT).show();                
            }
        }

    }
    
}

