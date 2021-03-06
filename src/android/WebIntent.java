package org.apache.cordova.webintent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.DroidGap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.Html;
import org.apache.cordova.PluginResult;
public class WebIntent extends CordovaPlugin {

	private CallbackContext onNewIntentCallback = null;
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		PluginResult pr;
		try {
			if (action.equals("startActivity")) {
				if (args.length() != 1) {
					pr = new PluginResult(PluginResult.Status.INVALID_ACTION);
					callbackContext.sendPluginResult(pr);
					return false;
				}

				// Parse the arguments
				JSONObject obj = args.getJSONObject(0);
				String type = obj.has("type") ? obj.getString("type") : null;
				Uri uri = obj.has("url") ? Uri.parse(obj.getString("url")) : null;
				JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
				Map<String, String> extrasMap = new HashMap<String, String>();

				// Populate the extras if any exist
				if (extras != null) {
					JSONArray extraNames = extras.names();
					for (int i = 0; i < extraNames.length(); i++) {
						String key = extraNames.getString(i);
						String value = extras.getString(key);
						extrasMap.put(key, value);
					}
				}

				startActivity(obj.getString("action"), uri, type, extrasMap);
				pr = new PluginResult(PluginResult.Status.OK);
				callbackContext.sendPluginResult(pr);
				return true;

			} else if (action.equals("hasExtra")) {
				if (args.length() != 1) {
					pr = new PluginResult(PluginResult.Status.INVALID_ACTION);
					callbackContext.sendPluginResult(pr);
					return false;
				}
				Intent i = ((DroidGap)this.cordova.getActivity()).getIntent();
				String extraName = args.getString(0);
				pr =  new PluginResult(PluginResult.Status.OK, i.hasExtra(extraName));
				callbackContext.sendPluginResult(pr);
				return true;
			}
				
			else if(action.equals("openPDF")){
				String filename = args.getString(0);
				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+'/'+filename);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(file), "application/pdf");
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				this.cordova.getActivity().startActivity(intent);
				
			} else if (action.equals("getExtra")) {
				if (args.length() != 1) {
					pr = new PluginResult(PluginResult.Status.INVALID_ACTION);
					callbackContext.sendPluginResult(pr);
					return false;
				}
				Intent i = ((DroidGap)this.cordova.getActivity()).getIntent();
				String extraName = args.getString(0);
				if (i.hasExtra(extraName)) {
					pr =  new PluginResult(PluginResult.Status.OK, i.getStringExtra(extraName));
					callbackContext.sendPluginResult(pr);
					return true;
				} else {
					pr = new PluginResult(PluginResult.Status.ERROR);
					callbackContext.sendPluginResult(pr);
					return false;
				}
			} else if (action.equals("getUri")) {
				if (args.length() != 0) {
					pr = new PluginResult(PluginResult.Status.INVALID_ACTION);
					callbackContext.sendPluginResult(pr);
					return false;
				}

				Intent i = ((DroidGap)this.cordova.getActivity()).getIntent();
				String uri = i.getDataString();
				pr =  new PluginResult(PluginResult.Status.OK, uri);
				callbackContext.sendPluginResult(pr);
				return true;
			} else if (action.equals("onNewIntent")) {
				if (args.length() != 0) {
					pr = new PluginResult(PluginResult.Status.INVALID_ACTION);
					callbackContext.sendPluginResult(pr);
					return false;
				}

				this.onNewIntentCallback = callbackContext;
				pr= new PluginResult(PluginResult.Status.NO_RESULT);
				pr.setKeepCallback(true);
				callbackContext.sendPluginResult(pr);
				return false;
			} else if (action.equals("sendBroadcast")) 
			{
				if (args.length() != 1) {
					pr = new PluginResult(PluginResult.Status.INVALID_ACTION);
					callbackContext.sendPluginResult(pr);
					return false;
				}

				// Parse the arguments
				JSONObject obj = args.getJSONObject(0);

				JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
				Map<String, String> extrasMap = new HashMap<String, String>();

				// Populate the extras if any exist
				if (extras != null) {
					JSONArray extraNames = extras.names();
					for (int i = 0; i < extraNames.length(); i++) {
						String key = extraNames.getString(i);
						String value = extras.getString(key);
						extrasMap.put(key, value);
					}
				}

				sendBroadcast(obj.getString("action"), extrasMap);
				pr = new PluginResult(PluginResult.Status.OK);
				callbackContext.sendPluginResult(pr);
				return true;
			}
			pr = new PluginResult(PluginResult.Status.OK);
			callbackContext.sendPluginResult(pr);
			return false;
		} catch (JSONException e) {
			e.printStackTrace();
			pr = new PluginResult(PluginResult.Status.JSON_EXCEPTION);
			callbackContext.sendPluginResult(pr);
			return false;
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		if (this.onNewIntentCallback != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, intent.getDataString());
			result.setKeepCallback(true);
          	this.onNewIntentCallback.sendPluginResult(result);
		}
	}

	void startActivity(String action, Uri uri, String type, Map<String, String> extras) {
		Intent i = (uri != null ? new Intent(action, uri) : new Intent(action));

		if (type != null && uri != null) {
			i.setDataAndType(uri, type); //Fix the crash problem with android 2.3.6
		} else {
			if (type != null) {
				i.setType(type);
			}
		}

		for (String key : extras.keySet()) {
			String value = extras.get(key);
			// If type is text html, the extra text must sent as HTML
			if (key.equals(Intent.EXTRA_TEXT) && type.equals("text/html")) {
				i.putExtra(key, Html.fromHtml(value));
			} else if (key.equals(Intent.EXTRA_STREAM)) {
				// allowes sharing of images as attachments.
				// value in this case should be a URI of a file
				i.putExtra(key, Uri.parse(value));
			} else if (key.equals(Intent.EXTRA_EMAIL)) {
				// allows to add the email address of the receiver
				i.putExtra(Intent.EXTRA_EMAIL, new String[] { value });
			} else {
				i.putExtra(key, value);
			}
		}
		this.cordova.getActivity().startActivity(i);
	}

	void sendBroadcast(String action, Map<String, String> extras) {
		Intent intent = new Intent();
		intent.setAction(action);
		for (String key : extras.keySet()) {
			String value = extras.get(key);
			intent.putExtra(key, value);
		}

		((DroidGap)this.cordova.getActivity()).sendBroadcast(intent);
	}
}