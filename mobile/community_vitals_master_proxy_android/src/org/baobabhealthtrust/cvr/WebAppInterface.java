package org.baobabhealthtrust.cvr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;

import org.baobabhealthtrust.cvr.models.AeSimpleSHA1;
import org.baobabhealthtrust.cvr.models.NationalIdentifiers;
import org.baobabhealthtrust.cvr.models.OutcomeTypes;
import org.baobabhealthtrust.cvr.models.Outcomes;
import org.baobabhealthtrust.cvr.models.People;
import org.baobabhealthtrust.cvr.models.RelationshipTypes;
import org.baobabhealthtrust.cvr.models.Relationships;
import org.baobabhealthtrust.cvr.models.User;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
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

	/** Instantiate the interface and set the context */
	WebAppInterface(Context c, Home parent) {
		mContext = c;
		mParent = parent;

		mDB = new DatabaseHandler(c);

		mUDB = new UserDatabaseHandler(c);

		mPrefs = c.getSharedPreferences("PrefFile", 0);

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
	public void addPerson(String first_name, String middle_name,
			String last_name, String gender, String dob, String occupation,
			String dead, String date_died, String date_created,
			int dob_estimate, String yob, String mob, String dtob) {

		int identifier = mDB.getBlankNPID();

		if (identifier == 0) {
			showMsg("Sorry, no national identifiers are available!");
		} else {
			showMsg("National identifier " + identifier + " available!");
		}

		/*
		 * String vh = getPref("vh"); String gvh = getPref("gvh"); String ta =
		 * getPref("ta");
		 * 
		 * People person = new People();
		 * 
		 * person.setGivenName(first_name); person.setMiddleName(middle_name);
		 * person.setFamilyName(last_name); person.setGender(gender);
		 * person.setBirthdate(dob); person.setOccupation(occupation);
		 * person.setBirthdateEstimated(dob_estimate);
		 * 
		 * mDB.addPeople(person);
		 * 
		 * showMsg("Saved");
		 * 
		 * mParent.myWebView.loadUrl("javascript:displayPeople()");
		 */
	}

	/*
	 * @JavascriptInterface public String getPeople(int gender) { JSONObject
	 * json = new JSONObject();
	 * 
	 * List<People> people;
	 * 
	 * // 1: Male; 2: Female; 3: Adults; 4: Children; 5: Adult Women; 6: Adult
	 * // Men; 7: All Boy Children; 8: All Girl Children; Any other: all switch
	 * (gender) { case 1: // people = mDB.getAllPersons("Male");
	 * debugPrint("Males only"); break; case 2: // people =
	 * mDB.getAllPersons("Female"); debugPrint("Females only"); break; case 3:
	 * // people = mDB.getAllPersons(15, 140); debugPrint("Adults only"); break;
	 * case 4: // people = mDB.getAllPersons(0, 14);
	 * debugPrint("Children only"); break; case 5: // people =
	 * mDB.getAllPersons("Female", 15, 140); debugPrint("Adult Females only");
	 * break; case 6: // people = mDB.getAllPersons("Male", 15, 140);
	 * debugPrint("Adult Males only"); break; case 7: // people =
	 * mDB.getAllPersons("Male", 0, 14); debugPrint("Adult Boy Children only");
	 * break; case 8: // people = mDB.getAllPersons("Female", 0, 14);
	 * debugPrint("Adult Girl Children only"); break; default: // people =
	 * mDB.getAllPersons(); debugPrint("General"); break; }
	 * 
	 * for (int i = 0; i < people.size(); i++) { JSONObject pjson = new
	 * JSONObject();
	 * 
	 * People person = people.get(i);
	 * 
	 * try {
	 * 
	 * List<NationalIdentifiers> identifiers = mDB
	 * .getAllNationalIdentifiers(person.getId());
	 * 
	 * String idList = "";
	 * 
	 * for (int s = 0; s < identifiers.size(); s++) { idList +=
	 * identifiers.get(s).getIdentifier() + "; "; }
	 * 
	 * Outcome outcome = mDB.getLastOutcome(person.getID());
	 * 
	 * OutcomeType otype = null;
	 * 
	 * if (outcome != null) otype =
	 * mDB.getOutcomeType(outcome.getOutcomeTypeId());
	 * 
	 * List<Relationship> relationships = mDB
	 * .getAllRelationships(person.getID());
	 * 
	 * String relations = ""; .trim().length() for (int r = 0; r <
	 * relationships.size(); r++) { Person rel =
	 * mDB.getPerson(relationships.get(r) .getPersonBId());
	 * 
	 * RelationshipType rt = mDB.getRelationshipType(relationships
	 * .get(r).getRelationshipType());
	 * 
	 * relations += rel.getFirstName() + " " + rel.getLastName() + "(" +
	 * rt.getName() + ")" + "<br />"; }
	 * 
	 * pjson.put("id", person.getID()); pjson.put("first_name",
	 * person.getFirstName()); pjson.put("last_name", person.getLastName());
	 * pjson.put("middle_name", person.getMiddleName()); pjson.put("gender",
	 * person.getGender()); pjson.put("dob", person.getDOB());
	 * pjson.put("occupation", person.getOccupation());
	 * pjson.put("year_of_birth", person.getYrOB()); pjson.put("month_of_birth",
	 * person.getMonthOB()); pjson.put("date_of_birth", person.getDateOB());
	 * pjson.put("dob_estimate", person.getDOBEstimate()); pjson.put("outcome",
	 * (outcome != null ? otype.getName() : "")); pjson.put("outcome_date",
	 * (outcome != null ? outcome.getOutcomeDate() : ""));
	 * pjson.put("date_created", person.getDateCreated());
	 * pjson.put("identifiers", idList); pjson.put("relations", relations);
	 * 
	 * json.put(person.getID() + "", pjson);
	 * 
	 * } catch (JSONException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); showMsg("Sorry, there was an error!"); }
	 * 
	 * }
	 * 
	 * return json.toString(); }
	 */

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
	public String getUsers() {
		JSONObject json = new JSONObject();

		List<User> users = mUDB.getAllUsers();

		for (int i = 0; i < users.size(); i++) {
			JSONObject pjson = new JSONObject();

			User user = users.get(i);

			try {
				pjson.put("user_id", user.getUserId());
				pjson.put("username", user.getUsername());
				pjson.put("password", user.getPassword());
				pjson.put("token", user.getToken());
				pjson.put("date_created", user.getDateCreated());

				json.put(user.getUserId() + "", pjson);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showMsg("Sorry, there was an error!");
			}

		}

		return json.toString();
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
			String mob, String dob, String success, String failed) {
		
		People person = new People();

		int identifier = mDB.getBlankNPID();

		if (identifier == 0) {

			showMsg("Sorry, no national identifiers are available!");

		} else {

			String birthdate = "";
			int birthdate_estimated = 0;

			debugPrint("B4 yob: " + yob + "; mob: " + mob + "; dob: " + dob + "; age: " + age);
			
			debugPrint(yob.trim().toLowerCase() + ": " + (yob.toLowerCase() == "unknown") + "");
					
			if (yob.trim().toLowerCase() == "unknown") {
				int yr = Calendar.YEAR - Integer.parseInt(age);

				birthdate = yr + "-07-15";
				birthdate_estimated = 1;

			} else {
				
				birthdate = yob;
				
				if(mob.trim().toLowerCase() != "unknown"){					
					birthdate = birthdate + "-" + String.format("%2d", Integer.parseInt(mob));
					
					if(dob.trim().toLowerCase() != "unknown"){
						birthdate = birthdate + "-" + String.format("%2d", Integer.parseInt(dob));
						
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
					+ " 00:00:00.000000";

			person.setCreatedAt(date);

			person.setUpdatedAt(date);

			debugPrint("After yob: " + yob + "; mob: " + mob + "; dob: " + dob + "; age: " + age);
			
			mDB.addPeople(person);

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

	/*
	 * @JavascriptInterface public String listMatchingNames(String fname, String
	 * lname, String gender) { JSONObject json = new JSONObject();
	 * 
	 * Log.i("JAVASCRIPT DEBUG", "Getting people");
	 * 
	 * List<Person> people = mDB.getPeopleNames(fname, lname, gender);
	 * 
	 * for (int i = 0; i < people.size(); i++) { JSONObject pjson = new
	 * JSONObject();
	 * 
	 * Person person = people.get(i);
	 * 
	 * try { String detail = person.getFirstName() + " " + person.getLastName()
	 * + " (" + person.getGender() + " - DOB: " + person.getDOB() + ")";
	 * 
	 * pjson.put("details", detail);
	 * 
	 * json.put(person.getID() + "", pjson);
	 * 
	 * } catch (JSONException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); showMsg("Sorry, there was an error!"); }
	 * 
	 * }
	 * 
	 * return json.toString(); }
	 * 
	 * 
	 * /*
	 * 
	 * @JavascriptInterface public void addPersonID(int person_id, String
	 * identifier, String type) { PersonIdentifierType idtype =
	 * mDB.getPersonIdentifierType(type);
	 * 
	 * debugPrint("person_id: " + person_id + "");
	 * 
	 * debugPrint("idtype: " + idtype.getPersonIdentifierTypeId() + "");
	 * 
	 * PersonIdentifier personidentifier = new PersonIdentifier();
	 * 
	 * personidentifier.setIdentifier(identifier);
	 * personidentifier.setIdentifierType(idtype.getPersonIdentifierTypeId());
	 * personidentifier.setPersonID(person_id);
	 * 
	 * mDB.addPersonIdentifier(personidentifier); }
	 */

	@JavascriptInterface
	public void updateOutcome(int person_id, int outcome, String date_of_event,
			String explanation) {

		OutcomeTypes outcomtype = mDB.getOutcomeTypes(outcome);

		Outcomes ocome = new Outcomes();

		String date_created = Calendar.getInstance().get(Calendar.YEAR) + "-"
				+ (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-"
				+ Calendar.getInstance().get(Calendar.DATE);

		ocome.setCreatedAt(date_created);
		ocome.setOutcomeDate(date_of_event);
		ocome.setOutcomeType(outcomtype.getId());
		ocome.setPersonId(person_id);

		mDB.addOutcomes(ocome);
	}

	@JavascriptInterface
	public void savePersonRelationship(String person_a_id, String person_b_id,
			int relation) {

		debugPrint("person_a: " + person_a_id + "; person_b_id: " + person_b_id
				+ "; relation: " + relation);

		RelationshipTypes rtype = mDB.getRelationshipTypes(relation);

		debugPrint(rtype.getRelation());

		Relationships relationship = new Relationships();

		String date_created = Calendar.getInstance().get(Calendar.YEAR) + "-"
				+ (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-"
				+ Calendar.getInstance().get(Calendar.DATE);

		relationship.setPersonNationalId(person_a_id);
		relationship.setRelationNationalId(person_b_id);
		relationship.setPersonIsToRelation(rtype.getId());
		relationship.setCreatedAt(date_created);

		mDB.addRelationships(relationship);

		debugPrint("Done creating relationship");
	}
}
