package com.snaptic.WODWhacker;
/*
 * Copyright (c) 2010 Snaptic, Inc
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 *
 * WODWhacker
 * 
 * A simple program to log a workout, a collection of exercises (situps, push ups) 
 * to a Snaptic account. All Exercises descriptions are stored as a series of tags 
 * within the users account.
 * 
 * This example currently does not use asynchronous functions or sqllite. 
 * In order for this example to work you need to input your snaptic.com
 * account username/password in the stateholder object below.
 * Harry Tormey   <harry@snaptic.com>
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.snaptic.api.SnapticAPI;
import com.snaptic.api.SnapticNote;

import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleAdapter.ViewBinder;

/*
 * Class responsible for composing workouts.  
 * */

public class WorkoutEditor extends ListActivity {
	
	private static boolean 					          DEBUG 	= true; //Turn on/off debug messages
	static final String 			  			  LOGCATNAME 	= "WODWhacker";//Debug message log name
	ArrayList<HashMap<String, String>> 		  		  mDisplayedListOfExercises;//List of exercises displayed that constitute this workout.  
	List<String> 							  mDisplayedDialogExercise;//List of exercises which you can select from drop down menu to append to list.
	List<Exercise>							  mExercises;//List of exercises, clean all this up -htormey
	private String 							  mUsername;
	private String 							  mPassword;
	
	//Replace this with SimpleCursorAdapter or own custom implementation based on this. -htormey
	private SimpleAdapter 					  mWorkOutEditorAdapter;
	private SnapticAPI						  mApi;
	//Singleton pattern user to perserve state, needed because activities can get destroyed at any time.
	private StateHolder 					  mStateHolder;
	   
	
	//Return instances of stateholder object used to preserve selected state between activities recreates.  
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mStateHolder;
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		if(DEBUG) Log.d(LOGCATNAME, "WorkoutEditor onCreate:");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.workout_editor);
        
        if(getLastNonConfigurationInstance() != null){
        	if (DEBUG) Log.d(LOGCATNAME, "Using last non configuration instance.");
            mStateHolder 				= (StateHolder)getLastNonConfigurationInstance();
            mDisplayedListOfExercises	= mStateHolder.mDisplayedListOfExercises;
            mDisplayedDialogExercise	= mStateHolder.mDisplayedDialogExercise;
            mExercises					= mStateHolder.mExercises;
            mUsername					= mStateHolder.mUsername;
            mPassword					= mStateHolder.mPassword;
            mApi						= mStateHolder.mApi;
        } else {
        	mStateHolder				= new StateHolder(); 
        	mDisplayedListOfExercises   = mStateHolder.mDisplayedListOfExercises;  
        	mDisplayedDialogExercise	= mStateHolder.mDisplayedDialogExercise;
        	mExercises 					= mStateHolder.mExercises;
        	mUsername					= mStateHolder.mUsername;
        	mPassword					= mStateHolder.mPassword;
        	mApi						= mStateHolder.mApi;
        }
        
        parseDataFromSnaptic();
        bindDataToList();
        bindDataToExerciseChoicesButton();
	}
	
	
	//Process intent returned from dialog used for setting attributes of a given exercise. (i.e number of situps etc)
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(DEBUG)Log.d(LOGCATNAME, "onActivityResult resultCode: " + resultCode );
		if(resultCode == RESULT_OK){
			

			String exerciseTitle					= data.getStringExtra("EXERCISE_TITLE");
			String exerciseTextArea   				= data.getStringExtra("EXERCISE_TEXT_AREA");
			String exerciseTextAreaContents   		= data.getStringExtra("EXERCISE_TEXT_AREA_CONTENTS");
			String exerciseWeightTextAreaContents   = data.getStringExtra("EXERCISE_WEIGHT_TEXT_AREA_CONTENTS");
			int exerciseNumItemSelected   	    	= data.getIntExtra("EXERCISE_NUM_ITEM_SELECTED", 0);
			int weightOrDistance   	 				= data.getIntExtra("EXERCISE_DISTANCE_OR_WEIGHT", 0);
			int exerciseUnit   						= data.getIntExtra("EXERCISE_UNIT", 0);
			

			if(DEBUG)Log.d(LOGCATNAME, "onActivityResult EXERCISE_TITLE: " + exerciseTitle );
			if(DEBUG)Log.d(LOGCATNAME, "onActivityResult EXERCISE_TEXT_AREA: " + exerciseTextArea );
			if(DEBUG)Log.d(LOGCATNAME, "onActivityResult EXERCISE_TEXT_AREA_CONTENTS: " + exerciseTextAreaContents );
			if(DEBUG)Log.d(LOGCATNAME, "onActivityResult EXERCISE_NUM_ITEM_SELECTED: " + exerciseNumItemSelected );
			if(DEBUG)Log.d(LOGCATNAME, "onActivityResult EXERCISE_DISTANCE_OR_WEIGHT: " + weightOrDistance );
			if(DEBUG)Log.d(LOGCATNAME, "onActivityResult EXERCISE_UNIT: " + exerciseUnit );
			
			if( exerciseTitle != null)
			{
				HashMap<String, String> map = new HashMap<String, String>();
		        map.put("WOD", exerciseTitle);
		        
		        //Check to see what number has been entered in the edit text area
		        if(exerciseTextAreaContents != null){
		        	  map.put("NUMWOD", exerciseTextAreaContents);
		        }
				
				if( weightOrDistance == Exercise.DISTANCE) {
					if (exerciseUnit > Exercise.NONE && exerciseUnit <= Exercise.IMPERIAL) {
						//Replace all of this with enums
						if(exerciseNumItemSelected >= Exercise.START_DISTANCE_STRING && exerciseNumItemSelected <= Exercise.END_DISTANCE_STRING){
							String unitString = Exercise.getExerciseDistanceUnitString(exerciseNumItemSelected);
							if(unitString != null) {
								map.put("WEIGHTDISTANCE",unitString);
							}
						}
					}
				}
				else if( weightOrDistance ==  Exercise.WEIGHT) {
					if (exerciseUnit > Exercise.NONE && exerciseUnit <= Exercise.IMPERIAL) {
						if(exerciseNumItemSelected >= Exercise.START_WEIGHT_STRING && exerciseNumItemSelected <= Exercise.END_WEIGHT_STRING){
							String unitString = Exercise.getExerciseWeightUnitString(exerciseNumItemSelected);
							if(unitString != null) {
								Log.d(LOGCATNAME, "Weight : " + unitString );							
								map.put("WEIGHTDISTANCE",exerciseWeightTextAreaContents + " " + unitString);
							}
						}
					}				
				}

		        map.put("DELWOD", "@android:drawable/ic_delete");
		        mDisplayedListOfExercises.add(map);  
		        mStateHolder.mDisplayedListOfExercises = mDisplayedListOfExercises;  
		        mWorkOutEditorAdapter.notifyDataSetChanged();				
			}

		}

	}
	
	//Load a bunch of pre defined exercises into the snaptic account. Only called if nothing is found.
	private void populateSnapticAccountWithPreDefinedExercises(){
		
		List<Exercise> predefinedExercises = new ArrayList<Exercise>();
		Exercise e 		= new Exercise("Run", Exercise.DISTANCE, Exercise.METRIC);
		predefinedExercises.add(e);
		e 		 		= new Exercise("Push Press", Exercise.WEIGHT, Exercise.METRIC);
		predefinedExercises.add(e);
		e 		 		= new Exercise("Situps", Exercise.NONE, Exercise.NONE);
		predefinedExercises.add(e);
		e 		 		= new Exercise("Double Unders", Exercise.NONE, Exercise.NONE);
		predefinedExercises.add(e);	
		e 		 		= new Exercise("Overhead Squat", Exercise.WEIGHT, Exercise.METRIC);
		predefinedExercises.add(e);			

		int returnCode = 0;
		
		for (Exercise post : predefinedExercises){
			SnapticNote note = new SnapticNote();
			note.text		 = post.serializeExerciseForSnaptic();
			returnCode   = mApi.addNote(note); //Check return code log not done todo -htormey.
		}
	}
	
	//Take list of displayed workouts, parse and write it to snaptic backend. This is a bit ghetto -htormey 
	private void serializeWorkoutAndPostToSnapticAccountStorage(){
		
		TextView title 	= (TextView) findViewById(R.id.EXETITLE);
		String workOutString  	= new String();
		workOutString += "#Workout"+"\n";
		//String workOutTitle ="#"; 
		//workOutTitle += title.getText().toString() + "\n";
		
		Iterator listIterator = mDisplayedListOfExercises.iterator();
			while(listIterator.hasNext()){

				String value 			= null;
				
				//@+id/EXETITLE"
				HashMap map				= ((HashMap)listIterator.next());
				value 					= ((String)map.get("WOD"));
				if(value != null){
					workOutString += value + " ";
				}
				value = null;
				value 					= ((String) map.get("NUMWOD"));
				if(value != null){
					workOutString += value + " ";
				}
				value = null;
				value 					= ((String) map.get("WEIGHTDISTANCE"));
				if(value != null){
					workOutString += value + " ";
				}
				workOutString += "\n";
				value = null;
				if(DEBUG)Log.d(LOGCATNAME, "serializeWorkoutAndPostToSnapticAccountStorage: "+ workOutString);

			
			}
			int returnCode;
			SnapticNote note = new SnapticNote();
			note.text		 = workOutString;
			returnCode   = mApi.addNote(note); 

	}
	//This class will be made async and move out of here, for now just parse stuff. -htormey
	//Also parsing stuff is bit getto hackish, refactor.
	private void parseDataFromSnaptic(){
		
		if(!mStateHolder.synced)
		{
			//First entry is always this
			//Check stateHolder and only add this stuff if it has not been parsed allready?

			//Move all this into parsing function? -htormey
			Pattern tagPattern 					= Pattern.compile("#");
			Pattern tagDistance 				= Pattern.compile("Distance");
			Pattern tagWeight					= Pattern.compile("Weight");
			Pattern tagMetric					= Pattern.compile("Metric");
			Pattern tagImperial					= Pattern.compile("Imperial");
			Pattern tagExerciseDescription  	= Pattern.compile("Exercise_Description");
			boolean exerciseDerscriptionsFound	= false;
        
			ArrayList<SnapticNote> notes 	= new ArrayList<SnapticNote>();
        			
			//Grab all notes from backend
			int getNotesReturnCode 			= mApi.getNotes(notes);
			
			//Search for notes containing descriptions of exercises
			for(SnapticNote n : notes){
				String tags = n.getTags().toString();
				Matcher exerciseDescriptionMatcher = tagExerciseDescription.matcher( tags );
        	
				if(exerciseDescriptionMatcher.find())
				{
					exerciseDerscriptionsFound = true;
	        		String title 		= "";
	        		int type 			= Exercise.NONE;
	        		int unitType		= Exercise.NONE;
	        		
	        		String [] noteLines = n.text.toString().split("\n");
	        		
	        		for(String line : noteLines){
	        			Matcher tagMatcher = tagPattern.matcher(line);
	        			//If line is NOT a tag
	        			if(!tagMatcher.find()){
	        			
	        				title = line;
	        				
	        				Log.d(LOGCATNAME, "Exercise Title:" + title );
	        				
	        				Matcher tagWeightMatcher 		= tagWeight.matcher( tags );
	        				if(tagWeightMatcher.find())
	        				{
	        					Log.d(LOGCATNAME, "Exercise Weight found");
	        					type = Exercise.WEIGHT;
	        				}
	        				Matcher tagDistanceMatcher 		= tagDistance.matcher( tags );
	        				if(tagDistanceMatcher.find())
	        				{
	        					Log.d(LOGCATNAME, "Exercise Distance found");
	        					type = Exercise.DISTANCE;
	        				}
	        				Matcher tagMetricMatcher 		= tagMetric.matcher( tags );
	        				if(tagMetricMatcher.find())
	        				{
	        					Log.d(LOGCATNAME, "Exercise Metric found");
	        					unitType = Exercise.METRIC;
	        				}
	        				Matcher tagImperialMatcher 		= tagImperial.matcher( tags );
	        				if(tagImperialMatcher.find())
	        				{
	        					Log.d(LOGCATNAME, "Exercise Imperial found");
	        					unitType = Exercise.IMPERIAL;
	        				}
	        			}
	        		}
	        		Exercise exercise = new Exercise(title, type, unitType);
	        		mExercises.add(exercise);
	        		mDisplayedDialogExercise.add(exercise.title);
	        		
	        	}   	
			}
			
			//If no Exercises found populate snaptic account with some descriptions.
			if(!exerciseDerscriptionsFound)
			{
				populateSnapticAccountWithPreDefinedExercises();
				//Re sync notes again
				 parseDataFromSnaptic();
			}
			else{
				mStateHolder.mExercises 				= mExercises;
				mStateHolder.mDisplayedDialogExercise	= mDisplayedDialogExercise;
				mStateHolder.synced 					= true;
			}
		}
	}
	
	//change this to take a dialog number. -htormey
	private void showDialog(){
		
		AlertDialog a =	new AlertDialog.Builder(this)
        .setTitle("Test")
        .setItems(mDisplayedDialogExercise.toArray(new CharSequence[]{}), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

				if(which >= 0){
					Intent intent = new Intent(getApplicationContext(), ExerciseProperties.class);
					mExercises.get((which)).serializeExerciseForIntent(intent);
					startActivityForResult(intent, 0);
				}  
            }
        })
        .show();	
	
	}
	//Set up list of individual exercises to append to list. Add string to this or something? -htormey  
	private void bindDataToExerciseChoicesButton(){
		
		Button selectExercise  	= (Button) findViewById(R.id.WODADDEXERCISE);
		selectExercise.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	showDialog();
            }
        });

	}
	
	//Sets up the list of exercises that constitute a workout. For now this takes no arguments, rewrite this in future.
	private void bindDataToList() {
		Log.d(LOGCATNAME, "Enter bindDataToList ");
		//Function will query the data based and return a cursor with information. For now just populate workout with default strings
		//Instantiate Adapter
		mWorkOutEditorAdapter	= new SimpleAdapter(this, mDisplayedListOfExercises,R.layout.wod_row, new String[] {"WOD", "DELWOD", "NUMWOD", "WEIGHTDISTANCE"}, 
				new int[] {R.id.WOD, R.id.DELWOD, R.id.NUM_WOD, R.id.WEIGHT_DISTANCE});

        //Setup event handler for items in row layouts contained within list.
        mWorkOutEditorAdapter.setViewBinder(new ViewBinder() {
        	
        	//This is used to setup values for views within layout used by adapter to populate list.
        	public boolean setViewValue(View view, Object data, String textRepresentation)
        	{
        		boolean retVal = false;
        		
        		//Change this to be a bit more fine grained i.e bound to ids like android:id="@+id/WOD"
        		//view.getId(); android:id="@+id/DELWOD" something like this? -htormey
        		if( view instanceof ImageView )
        		{
        			//What does retVal do?
        			retVal = true;
        			
        			//Convert string path to image to int id.
        			int resourceIdentifier =  getResources().getIdentifier(textRepresentation, null, getPackageName());
        			
        			//set image for entry in row.
        			((ImageView) view).setImageResource(resourceIdentifier);
        			
        			//set listener for this image.
        			view.setOnClickListener(new View.OnClickListener() {
        				 //Delete exercise from list of exercise list when you click this item.
      		             public void onClick(View v) {
       			            int position = getListView().getPositionForView(v);
      		                Log.d(LOGCATNAME, "Image ID when clicked: "+ v.getId());
      		                Toast.makeText(getApplicationContext(), "Image selected @ position: " + position, Toast.LENGTH_SHORT).show(); 
      		   		        Log.d(LOGCATNAME, "Position of image in list: "+ position);
      		   		        mDisplayedListOfExercises.remove(position);
      		   		        //mExercises.remove(position);
      		   		        mStateHolder.mDisplayedListOfExercises = mDisplayedListOfExercises;
      		   		        //mStateHolder.mExercises		   = mExercises; 
      		   		        mWorkOutEditorAdapter.notifyDataSetChanged();
      		             }
        			});
        		}
        		return retVal;
        	}
        	
         });
    
        //Setup listeners for listview
        getListView().setOnItemClickListener(new OnItemClickListener() {
        	
            public void onItemClick(AdapterView<?> parent, View v,
                    int position, long id) {
            		
            		Log.d(LOGCATNAME, "Clicked by listview ");
                }            
            
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
            		Log.d(LOGCATNAME, "Selected by listview ");
            }
              });

        //Add save work out button to footer of listview
		Button saveWorkout  	= new Button(this);

		saveWorkout.setText("Save workout");
		saveWorkout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	serializeWorkoutAndPostToSnapticAccountStorage();
            }
        });
		
		getListView().addFooterView(saveWorkout);
        //Bind adapter to list.
        getListView().setAdapter(mWorkOutEditorAdapter);
	}
	
    private static class StateHolder {
    		//Has workout editor state beem synced with snaptic
    		boolean synced = false;
    		ArrayList<HashMap<String, String>> 		  mDisplayedListOfExercises		= new ArrayList<HashMap<String, String>>();//List of exercises displayed that constitute this workout.  
    		List<String> 							  mDisplayedDialogExercise		= new ArrayList<String>();//List of exercises which yout can select from drop down menu to append to list.
    		List<Exercise>							  mExercises					= new ArrayList<Exercise>();//List of exercises, clean all this up -htormey
    		private String 							  mUsername 					= "";//Add your username here
    		private String 							  mPassword						= "";//Add your password here
    		private SnapticAPI 						  mApi							= new SnapticAPI(mUsername, mPassword);

    }
}
