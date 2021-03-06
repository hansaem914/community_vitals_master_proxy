package org.baobabhealthtrust.cvr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import org.baobabhealthtrust.cvr.models.AeSimpleSHA1;
import org.baobabhealthtrust.cvr.models.DdeSettings;
import org.baobabhealthtrust.cvr.models.NationalIdentifiers;
import org.baobabhealthtrust.cvr.models.Outcomes;
import org.baobabhealthtrust.cvr.models.People;
import org.baobabhealthtrust.cvr.models.Relationships;
import org.baobabhealthtrust.cvr.models.User;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
	Context mContext;
	Home mParent;
	DatabaseHandler mDB;
	UserDatabaseHandler mUDB;
	private int mID;
	private int mCategory;
	private static final int KEY_PERSON = 1;
	private static final int KEY_USER = 2;

	SharedPreferences mPrefs;

	private Handler mHandler;
	private int mInterval = 30000;

	/** Instantiate the interface and set the context */
	WebAppInterface(Context c, Home parent) {
		mContext = c;
		mParent = parent;

		mDB = new DatabaseHandler(c);

		mUDB = new UserDatabaseHandler(c);

		mPrefs = c.getSharedPreferences("PrefFile", 0);

		/*
		 * Calendar cal = Calendar.getInstance();
		 * 
		 * Intent intent = new Intent(c, CVRSyncServices.class);
		 * 
		 * Log.i("", "$$$$$$$$$$$$$$$$$$$$$$$$ dde_mode: " +
		 * getPref("dde_mode"));
		 * 
		 * intent.putExtra("mode", getPref("dde_mode"));
		 * 
		 * PendingIntent pintent = PendingIntent.getService(c, 0, intent, 0);
		 * 
		 * AlarmManager alarm = (AlarmManager)
		 * c.getSystemService(Context.ALARM_SERVICE); // Start every 30 seconds
		 * alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 30
		 * * 1000, pintent);
		 */

		mHandler = new Handler();

		startRepeatingTask();
	}

	Runnable mStatusChecker = new Runnable() {
		@Override
		public void run() {
			runService(); // this function can change value of mInterval.
			mHandler.postDelayed(mStatusChecker, mInterval);
		}
	};

	void startRepeatingTask() {
		mStatusChecker.run();
	}

	void stopRepeatingTask() {
		mHandler.removeCallbacks(mStatusChecker);
	}

	public void runService() {
		Intent intent = new Intent(mContext, CVRSyncServices.class);

		Log.i("", "$$$$$$$$$$$$$$$$$$$$$$$$ dde_mode: " + getPref("dde_mode"));

		intent.putExtra("mode", getPref("dde_mode"));
		intent.putExtra("target_username", getPref("target_username"));
		intent.putExtra("target_password", getPref("target_password"));
		intent.putExtra("target_server", getPref("target_server"));
		intent.putExtra("target_port", getPref("target_port"));
		intent.putExtra("site_code", getPref("site_code"));
		intent.putExtra("batch_count", getPref("batch_count"));
		intent.putExtra("gvh", getPref("gvh"));
		intent.putExtra("vh", getPref("vh"));

		mContext.startService(intent);
	}

	/** Show a toast from the web page */
	@JavascriptInterface
	public void showToast(String toast) {
		Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
	}

	@JavascriptInterface
	public void showMsg(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setCancelable(true);
		builder.setTitle(msg);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	@JavascriptInterface
	public void confirmDeletion(String msg, int id, int cat) {
		mID = id;
		mCategory = cat;

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setCancelable(true);
		builder.setTitle(msg);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				switch (mCategory) {
				case KEY_PERSON:
					deletePerson(mID);
					break;
				case KEY_USER:
					deleteUser(mID);
					break;
				}

				dialog.dismiss();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();

	}

	@JavascriptInterface
	public void deletePerson(int id) {
		People person = mDB.getPeople(id);

		mDB.deletePeople(person);

		showMsg("Person deleted!");

		mParent.myWebView.loadUrl("file:///android_asset/reports.html");
	}

	@JavascriptInterface
	public String doLogin(String username, String password) {

		// Log.i("LOGIN PARAMETERS", username + ", " + password);

		String token = mUDB.login(username, password);

		// Log.i("LOGIN STATUS", token);

		if (token.trim().length() > 0) {
			mParent.myWebView.loadUrl("javascript:displayUsers()");
		} else {
			showMsg("User Login Failed!");
		}

		Editor editor = mPrefs.edit();

		editor.putString("token", token);

		editor.commit();

		setPref("user_id", mUDB.mCurrentUserId + "");

		return token;
	}

	@JavascriptInterface
	public void doLogout(String token) {
		mUDB.logout(token);

		Editor editor = mPrefs.edit();

		editor.clear(); // .remove("token");

		editor.commit();

		mParent.myWebView.loadUrl("javascript:displayUsers()");
	}

	@JavascriptInterface
	public String getToken() {
		String token = mPrefs.getString("token", "");

		return token;
	}

	@JavascriptInterface
	public String getPref(String pref) {
		String item = mPrefs.getString(pref, "");

		return item;
	}

	@JavascriptInterface
	public void setPref(String pref, String value) {
		Editor editor = mPrefs.edit();

		editor.putString(pref, value);

		editor.commit();
	}

	@JavascriptInterface
	public void addUser(String username, String password, String date_created) {

		if (mUDB.userExists(username)) {
			showMsg("Username already taken!");
			return;
		}

		User user = new User();

		AeSimpleSHA1 sha = new AeSimpleSHA1();

		try {
			String pass = sha.SHA1(password);

			user.setUsername(username);
			user.setPassword(pass);
			user.setDateCreated(date_created);

			mUDB.addUser(user);

			showMsg("User Created!");

			mParent.myWebView.loadUrl("javascript:displayUsers()");

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@JavascriptInterface
	public void deleteUser(int id) {
		User user = mUDB.getUser(id);

		mUDB.deleteUser(user);

		showMsg("User deleted!");

		mParent.myWebView.loadUrl("javascript:displayUsers()");
	}

	@JavascriptInterface
	public boolean validToken(String token) {
		return mUDB.userLoggedIn(token);
	}

	@JavascriptInterface
	public String ajaxRead(String aUrl) {
		AssetManager am = mContext.getAssets();
		try {
			InputStream is = am.open(aUrl);
			String res = null;

			if (is != null) {
				// prepare the file for reading
				InputStreamReader input = new InputStreamReader(is);
				BufferedReader buffreader = new BufferedReader(input);

				res = "";
				String line;
				while ((line = buffreader.readLine()) != null) {
					res += line + "\n";
				}
				is.close();

				return res.toString();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	@JavascriptInterface
	public void debugPrint(String in) {
		Log.i("JAVASCRIPT DEBUG", in);
	}

	@JavascriptInterface
	public void savePerson(String fname, String mname, String lname,
			String gender, String age, String occupation, String yob,
			String mob, String dob, String success, String failed,
			String relationship, String person_b_id, String relation) {

		People person = new People();

		int identifier = mDB.getBlankNPID();

		if (identifier == 0) {

			showMsg("Sorry, no national identifiers are available!");
		} else {

			String birthdate = "";
			int birthdate_estimated = 0;

			if (yob.toLowerCase().equals("unknown")) {
				int yr = Calendar.getInstance().get(Calendar.YEAR)
						- Integer.parseInt(age);

				birthdate = yr + "-07-15";
				birthdate_estimated = 1;

			} else {

				birthdate = yob;

				if (mob.trim().toLowerCase() != "unknown") {
					birthdate = birthdate + "-"
							+ String.format("%02d", Integer.parseInt(mob));

					if (dob.trim().toLowerCase() != "unknown") {
						birthdate = birthdate + "-"
								+ String.format("%02d", Integer.parseInt(dob));

						birthdate_estimated = 0;
					} else {
						birthdate = birthdate + "-15";

						birthdate_estimated = 1;
					}
				} else {
					birthdate = birthdate + "-07-15";

					birthdate_estimated = 1;
				}
			}

			String vh = getPref("vh");
			String gvh = getPref("gvh");
			String ta = getPref("ta");

			person.setGivenName(fname);
			person.setMiddleName(mname);
			person.setFamilyName(lname);
			person.setGender(gender);
			person.setNationalId(identifier + "");

			person.setBirthdate(birthdate);

			person.setBirthdateEstimated(birthdate_estimated);

			person.setVillage(vh);
			person.setGvh(gvh);
			person.setTa(ta);

			String date = Calendar.getInstance().get(Calendar.YEAR)
					+ "-"
					+ String.format("%02d",
							(Calendar.getInstance().get(Calendar.MONTH) + 1))
					+ "-"
					+ String.format("%02d",
							Calendar.getInstance().get(Calendar.DATE))
					+ " "
					+ String.format("%02d",
							Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
					+ ":"
					+ String.format("%02d",
							Calendar.getInstance().get(Calendar.MINUTE))
					+ ":"
					+ String.format("%02d",
							Calendar.getInstance().get(Calendar.SECOND))
					+ ".000000";
			;

			person.setCreatedAt(date);

			person.setUpdatedAt(date);

			String result[] = mDB.addPeople(person);

			setPref("person id", result[0]);

			setPref("first name", result[1]);

			setPref("last name", result[2]);

			setPref("npid", result[3]);

			setPref("gender", result[4]);

			setPref("dob", result[5]);

			setPref("dobest", result[6]);

			if (relationship.equalsIgnoreCase("yes")) {
				Log.i("", "$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ create relationship");

				savePersonRelationship(result[3], person_b_id, relation);
			}

			showMsg(success);
		}
	}

	@JavascriptInterface
	public String listFirstNames(String filter) {
		JSONObject json = new JSONObject();

		List<String> names = mDB.getFirstNames(filter);

		for (int i = 0; i < names.size(); i++) {
			try {
				json.put(names.get(i), names.get(i));
			} catch (JSONException e) {
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

		}

		return json.toString();
	}

	@JavascriptInterface
	public String listLastNames(String filter) {
		JSONObject json = new JSONObject();

		List<String> names = mDB.getLastNames(filter);

		for (int i = 0; i < names.size(); i++) {
			try {
				json.put(names.get(i), names.get(i));
			} catch (JSONException e) {
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

		}

		return json.toString();
	}

	@JavascriptInterface
	public void updateOutcome(int person_id, String outcome,
			String date_of_event, String explanation) {

		int outcomtype = mDB.getOutcomeByType(outcome);

		Outcomes ocome = new Outcomes();

		String date_created = Calendar.getInstance().get(Calendar.YEAR)
				+ "-"
				+ String.format("%02d",
						(Calendar.getInstance().get(Calendar.MONTH) + 1))
				+ "-"
				+ String.format("%02d",
						Calendar.getInstance().get(Calendar.DATE))
				+ " "
				+ String.format("%02d",
						Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
				+ ":"
				+ String.format("%02d",
						Calendar.getInstance().get(Calendar.MINUTE))
				+ ":"
				+ String.format("%02d",
						Calendar.getInstance().get(Calendar.SECOND))
				+ ".000000";

		ocome.setCreatedAt(date_created);
		ocome.setOutcomeDate(date_of_event);
		ocome.setOutcomeType(outcomtype);
		ocome.setPersonId(person_id);

		mDB.addOutcomes(ocome);

		People person = mDB.getPeople(person_id);

		person.setOutcome(outcome);

		person.setOutcomeDate(date_created);

		mDB.updatePeople(person);
	}

	@JavascriptInterface
	public void savePersonRelationship(String person_a_id, String person_b_id,
			String relation) {

		Log.i("", "$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ create relationship with "
				+ person_a_id + " and " + person_b_id);

		int rtype = mDB.getRelationByType(relation);

		Relationships relationship = new Relationships();

		String date_created = Calendar.getInstance().get(Calendar.YEAR) + "-"
				+ (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-"
				+ Calendar.getInstance().get(Calendar.DATE);

		relationship.setPersonNationalId(person_a_id);
		relationship.setRelationNationalId(person_b_id);
		relationship.setPersonIsToRelation(rtype);
		relationship.setCreatedAt(date_created);

		mDB.addRelationships(relationship);

		debugPrint("Done creating relationship");
	}

	@JavascriptInterface
	public String search(String word) {
		String term = word;

		String locale = getPref("locale");

		if (locale.trim().length() == 0) {
			locale = "en";
		}

		term = mDB.search(word, locale);

		return term;
	}

	@JavascriptInterface
	public String listPeopleNames(String fname, String lname, String gender) {
		JSONObject json = new JSONObject();

		List<People> people = mDB.getPeopleNames(fname, lname, gender);

		for (int i = 0; i < people.size(); i++) {
			JSONObject pjson = new JSONObject();

			People person = people.get(i);

			try {
				String detail = person.getGivenName() + " "
						+ person.getFamilyName() + " ("
						+ person.getNationalId() + " - "
						+ search(person.getGender()) + " - DOB: "
						+ person.getBirthdate() + ")";

				pjson.put("details", detail);

				pjson.put("npid", person.getNationalId());

				json.put(person.getId() + "", pjson);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

		}

		return json.toString();
	}

	@JavascriptInterface
	public boolean searchPerson(int id) {

		String result[] = mDB.getPersonById(id);

		setPref("person id", result[0]);

		setPref("first name", result[1]);

		setPref("last name", result[2]);

		setPref("npid", result[3]);

		setPref("gender", result[4]);

		setPref("dob", result[5]);

		setPref("dobest", result[6]);

		return true;
	}

	@JavascriptInterface
	public int getAvailableIds() {
		int result = 0;
		String mode = getPref("dde_mode");

		result = mDB.getAvailableIds(mode);

		return result;
	}

	@JavascriptInterface
	public int getTakenIds() {
		int result = 0;
		String mode = getPref("dde_mode");

		result = mDB.getTakenIds(mode);

		return result;
	}

	@JavascriptInterface
	public void setSettings(String username, String password, String server,
			String port, String code, String count, String threshold) {
		String mode = getPref("dde_mode");

		DdeSettings settings = mUDB.getDdeSettingsByMode(mode);

		if (username.trim().length() > 0)
			settings.setDdeUsername(username);

		if (password.trim().length() > 0)
			settings.setDdePassword(password);

		if (server.trim().length() > 0)
			settings.setDdeIp(server);

		if (port.trim().length() > 0)
			settings.setDdePort(Integer.parseInt(port));

		if (code.trim().length() > 0)
			settings.setDdeSiteCode(code);

		if (count.trim().length() > 0)
			settings.setDdeBatchSize(count);

		if (threshold.trim().length() > 0)
			settings.setDdeThresholdSize(threshold);

		int result = mUDB.updateDdeSettings(settings);

		// confirmRestart("Will restart app!");

	}

	@JavascriptInterface
	public String getSettings() {
		JSONObject json = new JSONObject();

		String mode = getPref("dde_mode");

		DdeSettings settings = mUDB.getDdeSettingsByMode(mode);

		try {

			json.put("mode", mode);
			json.put("username", settings.getDdeUsername());
			json.put("password", settings.getDdePassword());
			json.put("ip", settings.getDdeIp());
			json.put("port", settings.getDdePort());
			json.put("code", settings.getDdeSiteCode());
			json.put("count", settings.getDdeBatchSize());

			setPref("dde_mode", mode);
			setPref("target_username", settings.getDdeUsername());
			setPref("target_password", settings.getDdePassword());
			setPref("target_server", settings.getDdeIp());
			setPref("target_port", settings.getDdePort() + "");
			setPref("site_code", settings.getDdeSiteCode());
			setPref("batch_count", settings.getDdeBatchSize());

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			showMsg("Sorry, there was an error!");
		}

		return json.toString();
	}

	@JavascriptInterface
	public void getNationalIds() {

		getSettings();

		String mode = getPref("dde_mode");

		String target_username = getPref("target_username");
		String target_password = getPref("target_password");
		String target_server = getPref("target_server");
		String target_port = getPref("target_port");
		String site_code = getPref("site_code");
		String batch_count = getPref("batch_count");
		String gvh = getPref("gvh");
		String vh = getPref("vh");

		String ext = "";

		WebServiceTask wst = new WebServiceTask(WebServiceTask.POST_TASK,
				mParent, "Posting data...");

		if (mode.equalsIgnoreCase("ta")) {
			ext = "npid_requests/get_npids";

			JSONObject json = new JSONObject();

			try {

				json.put("site_code", site_code);

				json.put("count", batch_count);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

			wst.addNameValuePair("site_code", site_code);
			wst.addNameValuePair("count", batch_count);

			// wst.addNameValuePair("npid_request", json.toString());

			wst.targetTaskType = wst.TASK_GET_TA_NPIDS;

		} else if (mode.equalsIgnoreCase("gvh")) {
			ext = "national_identifiers/request_gvh_ids";

			wst.addNameValuePair("site_code", site_code);
			wst.addNameValuePair("count", batch_count);
			wst.addNameValuePair("gvh", gvh);

			wst.targetTaskType = wst.TASK_GET_GVH_NPIDS;

		} else if (mode.equalsIgnoreCase("vh")) {
			ext = "national_identifiers/request_village_ids";

			wst.addNameValuePair("site_code", site_code);
			wst.addNameValuePair("count", batch_count);
			wst.addNameValuePair("gvh", gvh);
			wst.addNameValuePair("vh", vh);

			wst.targetTaskType = wst.TASK_GET_VH_NPIDS;

		}

		String SERVICE_URL = "http://" + target_server + ":" + target_port
				+ "/" + ext;

		wst.mUsername = target_username;
		wst.mPassword = target_password;
		wst.mServer = target_server;
		wst.mPort = Integer.parseInt(target_port);

		wst.mGVH = gvh;
		wst.mVH = vh;

		wst.mDdeMode = mode;
		wst.mCount = batch_count;

		// the passed String is the URL we will POST to
		wst.execute(new String[] { SERVICE_URL });

	}

	@JavascriptInterface
	public void setReportMonth(String months, String month) {
		setPref("report_month", (months+"-31"));
		setPref("display_month", month);
	}

	@JavascriptInterface
	public void setReportDate(String date, String display_date) {
		setPref("query_date", date);
		setPref("report_date", display_date);
	}

	@JavascriptInterface
	public int getGenderCount(String date_min,String date_max, String gender) {
		int result = 0;
		result = mDB.getGenderCount(date_min,date_max, gender);
		return result;
	}

	@JavascriptInterface
	public int getMonthBirths(String duration)
	{
		return mDB.getBirthsInMonth(duration);
	}
	
	@JavascriptInterface
	public int getMonthBirthsGender(String duration, String gender)
	{
		return mDB.getBirthsInMonthGender(duration, gender);
	}
	
	@JavascriptInterface
	public int getMonthBirthsOutcome(String duration, String outcome)
	{
		return mDB.getBirthsInMonthOutcome(duration, outcome);
	}
	
	@JavascriptInterface
	public int getMonthBirthsAlive(String duration)
	{
		return mDB.getBirthsInMonthAlive(duration);
	}
	
	@JavascriptInterface
	public int getOutcomeCount(String date_selected, String outcome) {
		
		return mDB.getOutcomeCount("1900-01-01",date_selected, outcome);	
	}
	
	@JavascriptInterface
	public int getOutcomesOnDate(String date_chosen, String outcome ){
		return mDB.getOutcomesOnDate(date_chosen, outcome);
	}
	
	@JavascriptInterface
	public int getOutcomesByDate(String date_chosen, String outcome ){
		return mDB.getOutcomesByDate(date_chosen, outcome);
	}
	
	@JavascriptInterface
	public int getOutcomesInMonth(String date_chosen, String outcome ){
		return mDB.getOutcomesInMonth(date_chosen, outcome);
	}
	
	@JavascriptInterface
	public int getOutcomesByMonth(String date_chosen, String outcome ){
		return mDB.getOutcomesByMonth(date_chosen, outcome);
	}
	
	@JavascriptInterface
	public int getAlive(String date){
		return mDB.getAlive("1900-01-01", date);
	}

	@JavascriptInterface
	public int getAliveInMonth(String date){
		return mDB.getAliveInMonth("1900-01-01", date);
	}
	@JavascriptInterface
	public int getAgegroupCount(int min, int max, String date_min, String date_max) {
		
		return mDB.getCountInAgeGroup(min, max, date_min, date_max);
	}

	@JavascriptInterface
	public boolean checkConnection(String host, int timeOut) {
		boolean result = false;
		try {
			result = InetAddress.getByName(host).isReachable(timeOut);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// result = mDB.getAgegroupCount(date_selected, age_group);
		return result;
	}

	@JavascriptInterface
	public int getThreshold() {
		int result = 0;
		String mode = getPref("dde_mode");

		result = mUDB.getThreshold(mode);

		return result;
	}

	@JavascriptInterface
	public String getDDESetting(String setting) {
		String result = "";
		String mode = getPref("dde_mode");

		result = mUDB.getDDESetting(mode, setting);

		return result;
	}

	@JavascriptInterface
	public String getDailySummary(String date) {
		
		JSONObject json = new JSONObject();
		
		try
		{
		
			json.put("popln", mDB.getPeopleCountOnDate(date));
			json.put("females", mDB.getGenderCount(date, date, "Female"));
			json.put("males",mDB.getGenderCount(date,date, "Male"));
			json.put("under 1", mDB.getCountInAgeGroup(0, 0, date, date));
			json.put("1-4", mDB.getCountInAgeGroup(1, 4, date, date));
			json.put("5-14", mDB.getCountInAgeGroup(5, 14, date, date));
			json.put("15-24", mDB.getCountInAgeGroup(15, 24, date, date));
			json.put("25-34", mDB.getCountInAgeGroup(25, 34, date, date));
			json.put("35-44", mDB.getCountInAgeGroup(35, 44, date, date));
			json.put("45-54", mDB.getCountInAgeGroup(45, 54, date, date));
			json.put("55-64", mDB.getCountInAgeGroup(55, 64, date, date));
			json.put("65-74", mDB.getCountInAgeGroup(65, 74, date, date));
			json.put("75 and above", mDB.getCountInAgeGroup(75, 200, date, date));
			json.put("dead", mDB.getOutcomesOnDate(date,"dead"));
			json.put("transfer", mDB.getOutcomesOnDate(date,"transfer out"));

		}
		catch(JSONException e)
		{
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        return e.toString();

		}
		
		
		return json.toString();
	}

	@JavascriptInterface
	public String listVillagePeopleNames(String page) {

		List<String> result = new ArrayList<String>();

		JSONObject json = new JSONObject();

		int count = mDB.getPeopleCount();

		int current_page = Integer.parseInt(page);

		int next_page = ((current_page * mDB.PAGE_SIZE) < count ? current_page + 1
				: current_page);

		int previous_page = (current_page > 1 ? current_page - 1 : current_page);

		int last_page = (int) Math.floor((double) (count / mDB.PAGE_SIZE)) + 1;
		//need to investigate
		List<People> people = mDB.getAllPeople(current_page);

		for (int i = 0; i < people.size(); i++) {
			JSONObject pjson = new JSONObject();

			People person = people.get(i);

			try {

				NationalIdentifiers identifier = mDB
						.getNationalIdentifiers(Integer.parseInt(person
								.getNationalId()));

				pjson.put("Name",
						person.getGivenName() + " " + person.getFamilyName());

				pjson.put("First Name", person.getGivenName());

				pjson.put("Middle Name", person.getMiddleName());

				pjson.put("Last Name", person.getFamilyName());

				pjson.put("Birthdate", person.getBirthdate());

				pjson.put("Gender", person.getGender());

				pjson.put("National ID", identifier.getIdentifier());

				pjson.put("Outcome", person.getOutcome());

				pjson.put("synced", identifier.getPostedByVh());

				json.put(person.getId() + "", pjson);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

		}

		int start = ((current_page - 1) * mDB.PAGE_SIZE) + 1;

		result.add(json.toString());

		result.add(previous_page + "");

		result.add(next_page + "");

		result.add(last_page + "");

		result.add(count + "");

		result.add(start + "");

		result.add(people.size() + "");

		return result.toString();
	}

	@JavascriptInterface
	public String listGVHPeopleNames(String page) {

		List<String> result = new ArrayList<String>();

		JSONObject json = new JSONObject();

		int count = mDB.getPeopleCount();

		int current_page = Integer.parseInt(page);

		int next_page = ((current_page * mDB.PAGE_SIZE) < count ? current_page + 1
				: current_page);

		int previous_page = (current_page > 1 ? current_page - 1 : current_page);

		int last_page = (int) Math.floor((double) (count / mDB.PAGE_SIZE)) + 1;

		List<People> people = mDB.getAllPeople(current_page);

		for (int i = 0; i < people.size(); i++) {
			JSONObject pjson = new JSONObject();

			People person = people.get(i);

			try {

				NationalIdentifiers identifier = mDB
						.getNationalIdentifiers(Integer.parseInt(person
								.getNationalId()));

				pjson.put("Name",
						person.getGivenName() + " " + person.getFamilyName());

				pjson.put("First Name", person.getGivenName());

				pjson.put("Middle Name", person.getMiddleName());

				pjson.put("Last Name", person.getFamilyName());

				pjson.put("Village", person.getVillage());

				pjson.put("Birthdate", person.getBirthdate());

				pjson.put("Gender", person.getGender());

				pjson.put("National ID", identifier.getIdentifier());

				pjson.put("Outcome", person.getOutcome());

				pjson.put("synced", identifier.getPostedByGvh());

				pjson.put("flagged", identifier.getPostGvhNotified());

				json.put(person.getId() + "", pjson);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

		}

		int start = ((current_page - 1) * mDB.PAGE_SIZE) + 1;

		result.add(json.toString());

		result.add(previous_page + "");

		result.add(next_page + "");

		result.add(last_page + "");

		result.add(count + "");

		result.add(start + "");

		result.add(people.size() + "");

		return result.toString();
	}

	@JavascriptInterface
	public void gvhFlag(String id, String value) {
		People person = mDB.getPeople(Integer.parseInt(id));

		NationalIdentifiers identifier = mDB.getNationalIdentifiers(Integer
				.parseInt(person.getNationalId()));

		identifier.setPostGvhNotified(Integer.parseInt(value));

		mDB.updateNationalIdentifiers(identifier);
	}

	@JavascriptInterface
	public void updateUser(String fname, String lname, String gender,
			String username, String password) {

		User user = mUDB.getUser(Integer.parseInt(getPref("user_id")));

		if (!fname.trim().equalsIgnoreCase(""))
			user.setFirstName(fname);

		if (!lname.trim().equalsIgnoreCase(""))
			user.setLastName(lname);

		if (!gender.trim().equalsIgnoreCase(""))
			user.setGender(gender);

		if (!username.trim().equalsIgnoreCase(""))
			user.setUsername(username);

		if (!password.trim().equalsIgnoreCase(""))
			user.setPassword(password);

		mUDB.updateUser(user);
	}

	@JavascriptInterface
	public String getUser(String user_id) {
		User user = mUDB.getUser(Integer.parseInt(user_id));

		JSONObject json = new JSONObject();

		try {

			json.put("fname", user.getFirstName());
			json.put("lname", user.getLastName());
			json.put("gender", user.getGender());
			json.put("username", user.getUsername());
			json.put("user_id", user.getUserId());

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			showMsg("Sorry, there was an error!");
		}

		return json.toString();
	}

	@JavascriptInterface
	public String getUsers(String page) {
		JSONObject json = new JSONObject();

		List<String> result = new ArrayList<String>();

		int count = mUDB.getUserCount();

		int current_page = Integer.parseInt(page);

		int next_page = ((current_page * mUDB.PAGE_SIZE) < count ? current_page + 1
				: current_page);

		int previous_page = (current_page > 1 ? current_page - 1 : current_page);

		int last_page = (int) Math.floor((double) (count / mUDB.PAGE_SIZE)) + 1;

		List<User> users = mUDB.getAllUsers(current_page);

		for (int i = 0; i < users.size(); i++) {
			JSONObject pjson = new JSONObject();

			User user = users.get(i);

			try {
				pjson.put("user_id", user.getUserId());
				pjson.put("Username", user.getUsername());
				pjson.put("Name",
						user.getFirstName() + " " + user.getLastName());
				pjson.put("Gender", user.getGender());
				pjson.put("Status", user.getStatus());

				String roles = mUDB.getRoles(user.getUserId());

				pjson.put("Roles", roles);

				json.put(user.getUserId() + "", pjson);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

		}

		int start = ((current_page - 1) * mDB.PAGE_SIZE) + 1;

		result.add(json.toString());

		result.add(previous_page + "");

		result.add(next_page + "");

		result.add(last_page + "");

		result.add(count + "");

		result.add(start + "");

		result.add(users.size() + "");

		return result.toString();
	}

	@JavascriptInterface
	public void addUser(String fname, String lname, String gender,
			String username, String password, String role) {

		User user = new User();

		if (!fname.trim().equalsIgnoreCase(""))
			user.setFirstName(fname);

		if (!lname.trim().equalsIgnoreCase(""))
			user.setLastName(lname);

		if (!gender.trim().equalsIgnoreCase(""))
			user.setGender(gender);

		if (!username.trim().equalsIgnoreCase(""))
			user.setUsername(username);

		if (!password.trim().equalsIgnoreCase(""))
			user.setPassword(password);

		user.setStatus("Suspended");

		mUDB.addUser(user);

		String usernm = (!username.trim().equalsIgnoreCase("") ? username
				.trim() : "");

		if (usernm.trim().length() == 0) {
			usernm = getPref("target_user");
		}

		if (usernm.toString().trim().length() > 0) {
			User result = mUDB.getUserByUsername(usernm.trim());

			mUDB.addUserRole(result, role);
		}
	}

	@JavascriptInterface
	public void confirmRestart(String msg) {

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setCancelable(true);
		builder.setTitle(msg);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				Intent mStartActivity = new Intent(mContext, Home.class);
				int mPendingIntentId = 123456;

				PendingIntent mPendingIntent = PendingIntent.getActivity(
						mContext, mPendingIntentId, mStartActivity,
						PendingIntent.FLAG_CANCEL_CURRENT);
				AlarmManager mgr = (AlarmManager) mContext
						.getSystemService(Context.ALARM_SERVICE);
				mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100,
						mPendingIntent);

				System.exit(0);

				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();

	}

	@JavascriptInterface
	public void updateSelectedUser(String fname, String lname, String gender,
			String username, String password, String role) {

		User user = mUDB.getUser(Integer.parseInt(getPref("target_user_id")));

		if (!fname.trim().equalsIgnoreCase(""))
			user.setFirstName(fname);

		if (!lname.trim().equalsIgnoreCase(""))
			user.setLastName(lname);

		if (!gender.trim().equalsIgnoreCase(""))
			user.setGender(gender);

		if (!username.trim().equalsIgnoreCase(""))
			user.setUsername(username);

		if (!password.trim().equalsIgnoreCase(""))
			user.setPassword(password);

		mUDB.updateUser(user);

		String usernm = (!username.trim().equalsIgnoreCase("") ? username
				.trim() : "");

		if (usernm.trim().length() == 0) {
			usernm = getPref("target_user");
		}

		if (usernm.toString().trim().length() > 0) {
			mUDB.addUserRole(user, role);
		}
	}

	@JavascriptInterface
	public void revokeUserRole(String role) {

		User user = mUDB.getUser(Integer.parseInt(getPref("target_user_id")));

		mUDB.revokeUserRole(user, role);
	}

	@JavascriptInterface
	public void updateUserStatus(String status) {

		User user = mUDB.getUser(Integer.parseInt(getPref("target_user_id")));

		user.setStatus(status);

		mUDB.updateUser(user);
	}

	@JavascriptInterface
	public boolean checkUsername(String user) {

		User suser = mUDB.getUserByUsername(user);

		try {
			if (String.valueOf(suser.getUserId()).length() == 0) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	@JavascriptInterface
	public String getUseRoles() {

		User user = mUDB.getUser(Integer.parseInt(getPref("user_id")));

		String roles = mUDB.getRoles(user.getUserId());

		return roles;

	}

	@JavascriptInterface
	public int getPeople(){
		return mDB.getPeopleCount();
	}
	
	@JavascriptInterface
	public int getPeople(String date){
		return mDB.getPeopleCount(date);
	}
	
	
	@JavascriptInterface
	public int getCulAlive(){
		return mDB.getCulAlive();
	}
	
	@JavascriptInterface
	public int getCulAlive(String date){
		return mDB.getAlive("1900-01-01",date);
	}
	
	@JavascriptInterface
	public int getCulOutcome(String date, String outcome){
		
		return mDB.getOutcomeCount(date,date,outcome);
		
	}
	
	@JavascriptInterface
	public String getSeniorMonthlyBirthReport(String date)
	{
		

		List<String> villages = mDB.getVillages(); 
		JSONArray details = new JSONArray();
		
		try{
			
			
			for (int i = 0; i < villages.size(); i++) {			
			
				String village = villages.get(i);
				JSONObject json = new JSONObject();
				json.put("village", village);
				json.put("male", getMonthBirthsGenderSnr(date, "Male", village));
				json.put("female", getMonthBirthsGenderSnr(date, "Female", village));
				json.put("alive", getMonthBirthsAliveSnr(date, village));
				json.put("dead", getMonthBirthsOutcomeSnr(date, "dead",village));
				json.put("transfer", getMonthBirthsOutcomeSnr(date, "transfer out", village));
				json.put("total", getMonthBirthsSnr(date, village));
				
				details.put(json);
			}
			return details.toString();
		}
		catch (JSONException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        return e.toString();
	    }
		
		
		
	}

	@JavascriptInterface
	public String getSeniorMonthlyOutcomesReport(String date)
	{
		
		List<String> villages = mDB.getVillages(); 
		JSONArray details = new JSONArray();
		
		try{
			
			
			for (int i = 0; i < villages.size(); i++) {			
			
				String village = villages.get(i);
				JSONObject json = new JSONObject();
				json.put("village", village);
				json.put("alive", getAlive(date,village));
				json.put("dead", getOutcomesInMonth(date, "dead",village));
				json.put("trans", getOutcomesInMonth(date, "transfer out",village));
				json.put("cul_alive", getAlive(date,village));
				json.put("cul_dead", getOutcomesByMonth(date,"dead",village));
				json.put("cul_trans", getOutcomesByMonth(date,"transfer out",village));
				json.put("total", getPeople(date, village));
				details.put(json);
			}
			return details.toString();
		}
		catch (JSONException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        return e.toString();
	    }
		
		
		
	}
	
	@JavascriptInterface
	public String cohortReport(String start_date, String end_date)
	{
		
		JSONObject cohort_category = new JSONObject();
		
		try{
		
			cohort_category.put("number alive males", mDB.getGenderCount(start_date, end_date, "Male"));
			cohort_category.put("number alive females", mDB.getGenderCount(start_date, end_date, "Female"));
			cohort_category.put("new births males",  mDB.getBirthsInRange(start_date, end_date, "Male"));
			cohort_category.put("new births females", mDB.getBirthsInRange(start_date, end_date, "Female"));
			cohort_category.put("deaths males", mDB.getOutcomesInRangeByGender(start_date, end_date, "Male", "dead"));
			cohort_category.put("deaths females", mDB.getOutcomesInRangeByGender(start_date, end_date, "Female", "dead"));
			cohort_category.put("transferred males", mDB.getOutcomesInRangeByGender(start_date, end_date, "Male", "transfer out"));
			cohort_category.put("transferred females", mDB.getOutcomesInRangeByGender(start_date, end_date, "Female", "transfer out"));
			cohort_category.put("under 1 males", mDB.getCountInAgeGroup(0, 0, start_date, end_date,"Male"));
			cohort_category.put("under 1 females", mDB.getCountInAgeGroup(0, 0, start_date, end_date,"Female"));
			cohort_category.put("1-4 males", mDB.getCountInAgeGroup(1, 4, start_date, end_date,"Male"));
			cohort_category.put("1-4 females", mDB.getCountInAgeGroup(1, 4, start_date, end_date,"Female"));
			cohort_category.put("5-14 males", mDB.getCountInAgeGroup(5, 14, start_date, end_date,"Male"));
			cohort_category.put("5-14 females", mDB.getCountInAgeGroup(5, 14, start_date, end_date,"Female"));
			cohort_category.put("15-24 males", mDB.getCountInAgeGroup(15, 24, start_date, end_date,"Male"));
			cohort_category.put("15-24 females", mDB.getCountInAgeGroup(15, 24, start_date, end_date,"Female"));
			cohort_category.put("25-34 males", mDB.getCountInAgeGroup(25, 34, start_date, end_date,"Male"));
			cohort_category.put("25-34 females", mDB.getCountInAgeGroup(25, 34, start_date, end_date,"Female"));
			cohort_category.put("35-44 males", mDB.getCountInAgeGroup(35, 44, start_date, end_date,"Male"));
			cohort_category.put("35-44 females", mDB.getCountInAgeGroup(35, 44, start_date, end_date,"Female"));
			cohort_category.put("45-54 males", mDB.getCountInAgeGroup(45, 54, start_date, end_date,"Male"));
			cohort_category.put("45-54 females", mDB.getCountInAgeGroup(45, 54, start_date, end_date,"Female"));
			cohort_category.put("55-64 males", mDB.getCountInAgeGroup(55, 64, start_date, end_date,"Male"));
			cohort_category.put("55-64 females", mDB.getCountInAgeGroup(55, 64, start_date, end_date,"Female"));
			cohort_category.put("65-74 males", mDB.getCountInAgeGroup(65, 74, start_date, end_date,"Male"));
			cohort_category.put("65-74 females", mDB.getCountInAgeGroup(65, 74, start_date, end_date,"Female"));
			cohort_category.put("75 and above males", mDB.getCountInAgeGroup(75, 200, start_date, end_date,"Male"));
			cohort_category.put("75 and above females", mDB.getCountInAgeGroup(75, 200, start_date, end_date,"Female"));
			
			return cohort_category.toString();
		}
		catch (JSONException e){
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        return e.toString();
			
		}
		
	}
	
	public int getMonthBirthsGenderSnr(String duration, String gender, String village)
	{
		return mDB.getBirthsInMonthGender(duration, gender, village);
	}
	
	public int getMonthBirthsSnr(String duration, String village)
	{
		return mDB.getBirthsInMonth(duration, village);
	}
	
	public int getMonthBirthsOutcomeSnr(String duration, String outcome, String village)
	{
		return mDB.getBirthsInMonthOutcome(duration, outcome,village);
	}
	
	public int getMonthBirthsAliveSnr(String duration, String village)
	{
		return mDB.getBirthsInMonthAlive(duration, village);
	}
	
	public int getAlive(String date, String village){
		return mDB.getAlive("1900-01-01", date, village);
	}
	public int getOutcomesInMonth(String date_chosen, String outcome, String village ){
		return mDB.getOutcomesInMonth(date_chosen, outcome, village);
	}
	
	public int getOutcomesByMonth(String date_chosen, String outcome, String village ){
		return mDB.getOutcomesByMonth(date_chosen, outcome, village);
	}
	
	public int getPeople(String date, String village){
		return mDB.getPeopleCount(date, village);
	}
}
