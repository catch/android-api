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
 * Harry Tormey   <harry@snaptic.com>
 */
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
 * Class responsible for editing the properties of a given
 * exercise, i.e 20 push ups, 500m run, 10 195lbs push press.
 * etc.
 *  
 * */
public class ExerciseProperties extends Activity {
	
 	String mExerciseTitle;
	String mExerciseTextArea;
	int mWeightOrDistance;
	int mExerciseUnit;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(WorkoutEditor.LOGCATNAME, "ExerciseProperties onCreate:");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.exercise_properties);
		
		final Intent intent 				= getIntent();
		mExerciseTitle 						= intent.getStringExtra("EXERCISE_TITLE");
		mExerciseTextArea 					= intent.getStringExtra("EXERCISE_TEXT_AREA");
		mWeightOrDistance					= intent.getIntExtra("EXERCISE_DISTANCE_OR_WEIGHT", 0);
		mExerciseUnit						= intent.getIntExtra("EXERCISE_UNIT", 0);
		String [] units						= intent.getStringArrayExtra("EXERCISE_UNIT_STRINGS");
		List<String> listForMetricSpinner 	= Arrays.asList(units);
		TextView exerciseTitle 				= (TextView) findViewById(R.id.EXETITLE);
		EditText exerciseTextArea 			= (EditText) findViewById(R.id.EXENUMEDIT);
		
		//Set the title for the dialog
		if(mExerciseTitle != null)
		{
			exerciseTitle.setText(mExerciseTitle);
		}
		
		//Set the default text for the text area
		if(mExerciseTextArea != null)
		{
			//exerciseTextArea.setText(mExerciseTextArea);
		}
		
		//passed exercise is a weight so turn on secondary edit text area to enter amount.
		if( mWeightOrDistance == Exercise.WEIGHT) 
		{
			EditText exerciseWeightTextArea 	= (EditText) findViewById(R.id.EXENUMWEIGHTEDIT);
			exerciseWeightTextArea.setVisibility(View.VISIBLE);
		}
		
		//Exercise has strings for spinner so make it visible and add the choices.
		if(listForMetricSpinner.size() > 0)
		{
			Spinner metricSpinner 				= (Spinner) findViewById(R.id.EXEUNITM);
			//Create adapter to back spinner		
			ArrayAdapter metricAdapter			= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listForMetricSpinner);
			//Set latout for dropdown in spinner
			metricAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			metricSpinner.setAdapter(metricAdapter);
			//set visible
			metricSpinner.setVisibility(0);
		}

		
		//Wire up ok and cancel buttons with listeners
		Button ok  		= (Button) findViewById(R.id.EXEOK);
		ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//Grab response and add it to intent to return
            	Spinner metricSpinner 				= (Spinner) findViewById(R.id.EXEUNITM);
            	EditText exerciseTextArea 			= (EditText) findViewById(R.id.EXENUMEDIT);
            	EditText exerciseWeightTextArea 	= (EditText) findViewById(R.id.EXENUMWEIGHTEDIT);
            	intent.putExtra("EXERCISE_NUM_ITEM_SELECTED", metricSpinner.getSelectedItemPosition());
            	intent.putExtra("EXERCISE_TEXT_AREA_CONTENTS", exerciseTextArea.getText().toString());
            	intent.putExtra("EXERCISE_WEIGHT_TEXT_AREA_CONTENTS", exerciseWeightTextArea.getText().toString());
                setResult(RESULT_OK, intent);
				finish();
            }
        });
		
		Button cancel  	= (Button) findViewById(R.id.EXECANCEL);
		cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	setResult(RESULT_CANCELED, intent);
            	finish();
            }
        });
		
	}
}
