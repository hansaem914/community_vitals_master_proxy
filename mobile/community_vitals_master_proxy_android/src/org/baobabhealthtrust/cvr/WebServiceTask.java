package org.baobabhealthtrust.cvr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.baobabhealthtrust.cvr.models.NationalIdentifiers;
import org.baobabhealthtrust.cvr.models.Outcomes;
import org.baobabhealthtrust.cvr.models.People;
import org.baobabhealthtrust.cvr.models.Relationships;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class WebServiceTask extends AsyncTask<String, Integer, String> {

	public static final int POST_TASK = 1;
	public static final int GET_TASK = 2;

	public static final int TASK_GET_VH_NPIDS = 3;
	public static final int TASK_GET_GVH_NPIDS = 4;
	public static final int TASK_GET_TA_NPIDS = 5;
	public static final int TASK_ACK = 6;
	public static final int TASK_POST_VH_DATA = 7;
	public static final int TASK_SYNC_VH_DATA = 8;
	public static final int TASK_POST_GVH_DATA = 9;

	private static final String TAG = "WebServiceTask";

	// connection timeout, in milliseconds (waiting to connect)
	private static final int CONN_TIMEOUT = 0;

	// socket timeout, in milliseconds (waiting for data)
	private static final int SOCKET_TIMEOUT = 0;

	public int targetTaskType = TASK_GET_VH_NPIDS;

	private int taskType = GET_TASK;
	private Context mContext = null;
	private String processMessage = "Processing...";

	private ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

	private ProgressDialog pDlg = null;

	public String mUsername = "";
	public String mPassword = "";
	public String mServer = "";
	public String mSiteCode = "";
	public String mTA = "";
	public String mGVH = "";
	public String mVH = "";
	public int mPort = 80;
	public String mCount = "0";
	public String mDdeMode = "";

	DatabaseHandler mDB;

	public WebServiceTask(int taskType, Context mContext, String processMessage) {

		this.taskType = taskType;
		this.mContext = mContext;
		this.processMessage = processMessage;

		mDB = new DatabaseHandler(mContext);

	}

	public void addNameValuePair(String name, String value) {

		params.add(new BasicNameValuePair(name, value));
	}

	private void showProgressDialog() {

		pDlg = new ProgressDialog(mContext);
		pDlg.setMessage(processMessage);
		pDlg.setProgressDrawable(mContext.getWallpaper());
		pDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDlg.setCancelable(false);
		pDlg.show();

	}

	@Override
	protected void onPreExecute() {

		// hideKeyboard();

		/*
		 * if (processMessage.toString().trim().length() > 0) {
		 * showProgressDialog(); }
		 */

	}

	protected String doInBackground(String... urls) {

		String url = urls[0];
		String result = "";

		HttpResponse response = doResponse(url);

		if (response == null) {
			return result;
		} else {

			try {

				result = inputStreamToString(response.getEntity().getContent());

			} catch (IllegalStateException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);

			} catch (IOException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);
			}

		}

		return result;
	}

	@Override
	protected void onPostExecute(String response) {

		handleResponse(response);

		/*
		 * if (processMessage.toString().trim().length() > 0) { pDlg.dismiss();
		 * }
		 */

	}

	// Establish connection and socket (data retrieval) timeouts
	private HttpParams getHttpParams() {

		HttpParams htpp = new BasicHttpParams();

		HttpConnectionParams.setConnectionTimeout(htpp, CONN_TIMEOUT);
		HttpConnectionParams.setSoTimeout(htpp, SOCKET_TIMEOUT);

		return htpp;
	}

	private HttpResponse doResponse(String url) {

		// Use our connection and data timeouts as parameters for our
		// DefaultHttpClient
		HttpClient httpclient = new DefaultHttpClient(getHttpParams());

		HttpResponse response = null;

		try {
			switch (taskType) {

			case POST_TASK:

				HttpPost httppost = new HttpPost(url);

				// Add parameters
				httppost.setEntity(new UrlEncodedFormEntity(params));

				httppost.setEntity(new UrlEncodedFormEntity(params));

				httppost.setHeader(
						"Authorization",
						"Basic "
								+ Base64.encodeToString(
										(mUsername + ":" + mPassword)
												.getBytes(), Base64.NO_WRAP));

				response = httpclient.execute(httppost);
				break;
			case GET_TASK:
				HttpGet httpget = new HttpGet(url);

				httpget.setHeader(
						"Authorization",
						"Basic "
								+ Base64.encodeToString(
										(mUsername + ":" + mPassword)
												.getBytes(), Base64.NO_WRAP));

				response = httpclient.execute(httpget);
				break;
			}
		} catch (Exception e) {

			Log.e(TAG, e.getLocalizedMessage(), e);

		}

		return response;
	}

	private String inputStreamToString(InputStream is) {

		String line = "";
		StringBuilder total = new StringBuilder();

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));

		try {
			// Read response until the end
			while ((line = rd.readLine()) != null) {
				total.append(line);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}

		// Return full string
		return total.toString();
	}

	private void handleResponse(String response) {

		// showMsg(response);

		switch (targetTaskType) {
		case TASK_GET_VH_NPIDS:
			saveVHNPIDS(response);
			break;
		case TASK_GET_GVH_NPIDS:
			break;
		case TASK_GET_TA_NPIDS:
			break;
		case TASK_POST_VH_DATA:
			savePostVHDataResponse(response);
			break;
		case TASK_POST_GVH_DATA:
			savePostGVHDataResponse(response);
			break;
		case TASK_SYNC_VH_DATA:
			saveSyncVHDataResponse(response);
			break;
		}

	}

	private void saveVHNPIDS(String response) {

		try {

			JSONArray jso = new JSONArray(response);

			for (int i = 0; i < jso.length(); i++) {
				String identifier = (String) jso.get(i);

				Log.i(TAG, identifier);

				String date_created = Calendar.getInstance().get(Calendar.YEAR)
						+ "-"
						+ String.format(
								"%02d",
								(Calendar.getInstance().get(Calendar.MONTH) + 1))
						+ "-"
						+ String.format("%02d",
								Calendar.getInstance().get(Calendar.DATE))
						+ " "
						+ String.format("%02d",
								Calendar.getInstance()
										.get(Calendar.HOUR_OF_DAY))
						+ ":"
						+ String.format("%02d",
								Calendar.getInstance().get(Calendar.MINUTE))
						+ ":"
						+ String.format("%02d",
								Calendar.getInstance().get(Calendar.SECOND))
						+ ".000000";

				NationalIdentifiers obj = new NationalIdentifiers();

				obj.setIdentifier(identifier);

				obj.setSiteId(mSiteCode);

				obj.setAssignedGvh(mGVH);

				obj.setAssignedVh(mVH);

				obj.setRequestedByGvh(0);

				obj.setRequestedByVh(1);

				obj.setRequestGvhNotified(0);

				obj.setRequestVhNotified(1);

				obj.setCreatedAt(date_created);

				obj.setUpdatedAt(date_created);

				mDB.addNationalIdentifiers(obj);
			}

			ackNPIDS(response);

		} catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}

	}

	public void ackNPIDS(String response) {

		String mode = mDdeMode;

		String target_username = mUsername;
		String target_password = mPassword;
		String target_server = mServer;
		int target_port = mPort;
		String site_code = mSiteCode;
		String batch_count = mCount;
		String gvh = mGVH;
		String vh = mVH;

		String extAck = "";

		WebServiceTask wst = new WebServiceTask(WebServiceTask.POST_TASK,
				mContext, "Posting data...");

		if (mode.equalsIgnoreCase("ta")) {

			extAck = "npid_requests/ack/";

			JSONObject json = new JSONObject();

			try {

				json.put("site_code", site_code);

				json.put("count", batch_count);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("FATAL ERROR",
						"Sorry, there was an error! Can't proceed!");
			}

			wst.addNameValuePair("npid_request", json.toString());

		} else if (mode.equalsIgnoreCase("gvh")) {

			extAck = "national_identifiers/request_gvh_ids_ack/";

			wst.addNameValuePair("site_code", site_code);
			wst.addNameValuePair("count", batch_count);
			wst.addNameValuePair("gvh", gvh);

			wst.addNameValuePair("ids", response);

		} else if (mode.equalsIgnoreCase("vh")) {

			extAck = "national_identifiers/request_village_ids_ack/";

			wst.addNameValuePair("site_code", site_code);
			wst.addNameValuePair("count", batch_count);
			wst.addNameValuePair("gvh", gvh);
			wst.addNameValuePair("vh", vh);

			wst.addNameValuePair("ids", response);

		}

		wst.targetTaskType = wst.TASK_ACK;

		String SERVICE_URL = "http://" + target_server + ":" + target_port
				+ "/" + extAck;

		wst.mUsername = target_username;
		wst.mPassword = target_password;
		wst.mServer = target_server;
		wst.mPort = target_port;

		wst.mGVH = gvh;
		wst.mVH = vh;

		// the passed String is the URL we will POST to
		wst.execute(new String[] { SERVICE_URL });
	}

	public void showMessage(String msg) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setCancelable(true);
			builder.setTitle(msg);
			builder.setInverseBackgroundForced(true);
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

			AlertDialog alert = builder.create();
			alert.show();
		} catch (Exception e) {
		}
	}

	private void savePostVHDataResponse(String response) {
		JSONArray arr;

		try {
			arr = new JSONArray(response);

			JSONObject posted = new JSONObject(params.get(0).getValue());

			if (posted.length() == arr.length()) {

				for (int i = 0; i < arr.length(); i++) {
					NationalIdentifiers identifier = mDB
							.getNationalIdentifierByIDentifier(arr.get(i)
									.toString());

					identifier.setPostedByVh(1);

					mDB.updateNationalIdentifiers(identifier);

				}

			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void saveSyncVHDataResponse(String response) {

		try {

			JSONObject records = new JSONObject(response);

			Iterator<?> keys = records.keys();

			while (keys.hasNext()) {
				String key = (String) keys.next();

				if (records.get(key) instanceof JSONObject) {

					NationalIdentifiers identifier = new NationalIdentifiers();

					JSONObject identifier_details = records.getJSONObject(key)
							.getJSONObject("identifier_details");

					Boolean id_exists = mDB.checkNationalIdentifier(identifier_details.get(
							"identifier").toString());

					if (!id_exists)
					{
						identifier.setCreatedAt(identifier_details
								.get("created_at").toString());

						identifier.setVoided(Integer.parseInt(identifier_details
								.get("voided").toString()));

						identifier.setPostedByGvh(Integer
								.parseInt(identifier_details.get("posted_by_gvh")
										.toString()));

						identifier.setRequestGvhNotified(Integer
								.parseInt(identifier_details.get(
										"request_gvh_notified").toString()));

						identifier.setPostedByGvh(Integer
								.parseInt(identifier_details.get("posted_by_vh")
										.toString()));

						identifier.setRequestVhNotified(Integer
								.parseInt(identifier_details.get(
										"request_vh_notified").toString()));

						identifier.setPostGvhNotified(Integer
								.parseInt(identifier_details.get(
										"post_gvh_notified").toString()));

						identifier.setVoidReason(identifier_details.get(
								"void_reason").toString());

						identifier.setRequestedByVh(Integer
								.parseInt(identifier_details.get("requested_by_vh")
										.toString()));

						identifier.setAssignedGvh(identifier_details.get(
								"assigned_gvh").toString());

						identifier.setSiteId(identifier_details.get("site_id")
								.toString());

						identifier.setUpdatedAt(identifier_details
								.get("updated_at").toString());

						identifier.setRequestedByGvh(Integer
								.parseInt(identifier_details
										.get("requested_by_gvh").toString()));

						identifier.setAssignedVh(identifier_details.get(
								"assigned_vh").toString());

						identifier.setIdentifier(identifier_details.get(
								"identifier").toString());

						identifier.setDateVoided(identifier_details.get(
								"date_voided").toString());

						mDB.addNationalIdentifiers(identifier);

					}
					
					NationalIdentifiers nat_id = mDB
							.getNationalIdentifierByIDentifier(key);

					JSONObject person_details = records.getJSONObject(key)
							.getJSONObject("person_details");

					People person = new People();

					person.setGivenName(person_details.get("fname").toString());

					if (person_details.get("middle_name").toString() != "null")
					{
						person.setMiddleName(person_details.get("middle_name")
								.toString());	
					}
					
					person.setFamilyName(person_details.get("lname").toString());

					person.setGender(person_details.get("gender").toString());

					person.setBirthdate(person_details.get("dob").toString());

					person.setBirthdateEstimated(Integer
							.parseInt(person_details.get("dob_estimated")
									.toString()));

					if (person_details.get("outcome").toString() != "null")
					{
						person.setOutcome(person_details.get("outcome").toString());	
					}
					if (person_details.get("outcome_date").toString() != "null")
					{
						person.setOutcomeDate(person_details.get("outcome_date")
								.toString());	
					}

					person.setVillage(person_details.get("village").toString());

					person.setGvh(person_details.get("gvh").toString());

					person.setTa(person_details.get("ta").toString());

					person.setNationalId(nat_id.getId() + "");
					
					People person_id;

					if (nat_id.getPersonId() == 0)
					{
						mDB.addPeople(person);

						person_id = mDB.getPersonByNpid(nat_id.getId());

						NationalIdentifiers n_id = mDB
								.getNationalIdentifierByIDentifier(key);

						n_id.setPersonId(person_id.getId());

						mDB.updateNationalIdentifiers(n_id);
	
					}
					else 
					{
						person_id = mDB.getPersonByNpid(nat_id.getId());
						mDB.updatePeople(person);
					}
					
					JSONArray relationships = records.getJSONObject(key)
							.getJSONArray("relationships");

					for (int i = 0; i < relationships.length(); i++) {
						Relationships relation = new Relationships();

						relation.setPersonNationalId(relationships
								.getJSONObject(i).get("person").toString());

						relation.setRelationNationalId(relationships
								.getJSONObject(i).get("relative").toString());

						relation.setPersonIsToRelation(Integer
								.parseInt(relationships.getJSONObject(i)
										.get("relationship").toString()));

						mDB.addRelationships(relation);

					}

					JSONArray outcomes = records.getJSONObject(key)
							.getJSONArray("outcomes");

					for (int i = 0; i < outcomes.length(); i++) {
						Outcomes outcome = new Outcomes();

						outcome.setPersonId(person_id.getId());

						outcome.setOutcomeType(Integer.parseInt(outcomes
								.getJSONObject(i).get("outcome").toString()));

						outcome.setOutcomeDate(outcomes.getJSONObject(i)
								.get("outcome_date").toString());

						mDB.addOutcomes(outcome);
					}

				}
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	private void savePostGVHDataResponse(String response) {

		try {

			JSONArray records = new JSONArray(response);

			for (int i = 0; i < records.length(); i++) {
				NationalIdentifiers identifier = mDB
						.getNationalIdentifierByIDentifier(records.get(i)
								.toString());
				
				identifier.setPostedByGvh(1);
				
				mDB.updateNationalIdentifiers(identifier);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
