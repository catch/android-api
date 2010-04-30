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
import android.content.Intent;

/*
 * Class representing a description of an exercise for the purpose of translating between both intents and the snaptic backend 
 */
public class Exercise {

	/*
	 * Constants relating to Exercises used throughout the code.
	 */
	public static final int NONE = 0, DISTANCE = 1, WEIGHT = 2;
	public static final int METRIC = 1, IMPERIAL = 2;
	 
	/* 
	 * Constant strings and index enums used to populate Spinners relating to Exercises and identify choices made
	 * */
 	public static final String [] DISTANCE_STRINGS = {"M", "Km", "Ft", "Mi"};
 	public static int 							 METERS = 0, KILOMETERS = 1, FEET = 2, MILES = 3, START_DISTANCE_STRING = METERS, END_DISTANCE_STRING=MILES;	
 	public static final String [] WEIGHT_STRINGS = {"Kg", "Pud", "lb"};
 	public static int 								KG = 0, POOD = 1, POUNDS = 2, START_WEIGHT_STRING = KG, END_WEIGHT_STRING=POUNDS;
 	
 	/*
 	 * Constant strings used for serialization to the snaptic backend
 	 * */
 	public static final String WODTRACKER  = "#WODTracker ", EXERCISE_DESCRIPTION_TAG = "#Exercise_Description";
 	public static final String [] TAGS_DISTANCE_WEIGHT = {"#None", "#Distance", "#Weight"};
	public static final String [] TAGS_UNIT_TYPE = {"#None", "#Metric", "#Imperial"};

	/* 
	 * Member variables that constitute an Exercise.
	 * */
	public String title;// name of exercise, situps, etc
	public int type;//i.e Weight or Distance
	public int unitType;//i.e metric or imperial
	public String [] units;//Strings describing units used to measure exercise i.e Kilo's, pounds or meters miles.
	
	public Exercise(){
		this.title 		= "";
		this.type		= NONE;
		this.unitType	= NONE;
	    this.units 		= new String[]{};//? -htormey	
	}
	
	public Exercise(String title, int type, int unitType){
		
		this.title 		= title;
		this.type		= type;
		this.unitType	= type;
		
		if(this.type > NONE && this.type <= WEIGHT)
		{
			if(this.type == DISTANCE)
			{
				this.units = DISTANCE_STRINGS;
			}
			else if (this.type == WEIGHT)
			{
				this.units = WEIGHT_STRINGS;
			}
		}
		else
		{
			this.type		= NONE;
			this.units 		= new String[]{};
		}
	}
	
	/*
	 * Look up the distance string which corresponds with the passed in unit (i.e meters etc)
	 * returns String 
	 * */
	static String getExerciseDistanceUnitString(int unit){
		String unitString = null;
		
		if(unit >= START_DISTANCE_STRING && unit <= END_DISTANCE_STRING){
			return DISTANCE_STRINGS[unit];
		}
		
		return unitString;
	}
	
	/*
	 * Look up the weight string which corresponds with the passed in unit (i.e kg etc)
	 * returns -1 for error, success returns the unit passed in. 
	 * */
	static String getExerciseWeightUnitString(int unit){
		String unitString = null;
		
		if(unit >= START_WEIGHT_STRING && unit <= END_WEIGHT_STRING){
			return WEIGHT_STRINGS[unit];
		}
		return unitString;
	}
	
	/*
	 * Serialize the important attributes of an exercise into an intent for IPC
	 * or communication with another dialog/activity.
	 * */
	void serializeExerciseForIntent(Intent intent){
		intent.putExtra("EXERCISE_TITLE", this.title);
		intent.putExtra("EXERCISE_DISTANCE_OR_WEIGHT", this.type);//0 = none, 1 = distance , 2 = weight 
		intent.putExtra("EXERCISE_UNIT", this.unitType);//0 = none, 1 = metric, 2 = imperial
		intent.putExtra("EXERCISE_UNIT_STRINGS", this.units);//Strings related to this exercise
	}
	
	/*
	 * Serialize the important attributes of an exercise into a string to be written to
	 * the snaptic backend.
	 * */
	String serializeExerciseForSnaptic(){
		String serializedExercise = new String();
		//All Exercises must be tagged WODTRACKER (name of app) and  EXERCISE_DESCRIPTION_TAG (tag denoting this is an exercise)
		serializedExercise += WODTRACKER + "\n";
		serializedExercise += EXERCISE_DESCRIPTION_TAG + "\n";
		//Check to see if Exercise is a weight or a distance and append appropriate tag
		if(this.type > NONE){
			serializedExercise += TAGS_DISTANCE_WEIGHT[this.type] + "\n";
		}
		//Check to see if Exercise units are of type metric or imperial
		if(this.unitType > NONE){
			serializedExercise += TAGS_UNIT_TYPE[this.type] + "\n";
		}
		//Append title last
		if(this.title != null){
			serializedExercise += this.title + "\n";
		}
		return serializedExercise;
	}
}