package com.robertjscott.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class EarthquakeUpdateService extends Service {

  public static String TAG = "EARTHQUAKE_UPDATE_SERVICE";
  
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  
  private Timer updateTimer;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // Retrieve the shared preferences
    Context context = getApplicationContext();
    SharedPreferences prefs = 
      PreferenceManager.getDefaultSharedPreferences(context);

    int updateFreq = 
      Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));
    boolean autoUpdateChecked = 
      prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);

    updateTimer.cancel();
    if (autoUpdateChecked) {
      updateTimer = new Timer("earthquakeUpdates");
      updateTimer.scheduleAtFixedRate(doRefresh, 0,
        updateFreq*60*1000);
    }
    else {
      Thread t = new Thread(new Runnable() {
        public void run() {
          refreshEarthquakes(); 
        }
      });
      t.start();
    }

    return Service.START_STICKY;
  };

  private TimerTask doRefresh = new TimerTask() {
    public void run() {
      refreshEarthquakes();
    }
  };

  @Override
  public void onCreate() {
    updateTimer = new Timer("earthquakeUpdates");
  }
  
  
  public void refreshEarthquakes() {
    URL url;
    try {
      url = new URL(getString(R.string.quake_feed));
      
      URLConnection connection;
      connection = url.openConnection();
      
      HttpURLConnection httpConnection = (HttpURLConnection)connection;
      int responseCode = httpConnection.getResponseCode();
      
      if (responseCode == HttpURLConnection.HTTP_OK){
        InputStream in = httpConnection.getInputStream();
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        // Parse the earthquake feed
        Document dom = db.parse(in);
        Element docEle = dom.getDocumentElement();
        
        // Get a list of each earthquake entry
        NodeList nl = docEle.getElementsByTagName("entry");
        if (nl != null && nl.getLength() > 0) {
          for (int i = 0; i < nl.getLength(); i++) {
            Element entry = (Element)nl.item(i);
            Element title = (Element)entry.getElementsByTagName("title").item(0);
            Element g = (Element)entry.getElementsByTagName("georss:point").item(0);
            Element when = (Element)entry.getElementsByTagName("updated").item(0);
            Element link = (Element)entry.getElementsByTagName("link").item(0);
            
            String details = title.getFirstChild().getNodeValue();
            String hostname = "http://earthquakes.usgs.gov/";
            String linkString = hostname + link.getAttribute("href");
            
            String point = g.getFirstChild().getNodeValue();
            String dt = when.getFirstChild().getNodeValue();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
            Date qdate = new GregorianCalendar(0,0,0).getTime();
            try {
              qdate = sdf.parse(dt);
            } catch (ParseException e) {
              Log.d(TAG, "Date parsing exception.", e);
            }
            
            String[] location = point.split(" ");
            Location l = new Location("dummyGPS");
            l.setLatitude(Double.parseDouble(location[0]));
            l.setLongitude(Double.parseDouble(location[1]));
            
            String magnitudeString = details.split(" ")[1];
            int end = magnitudeString.length()-1;
            double magnitude = Double.parseDouble(magnitudeString.substring(0, end));
            
            details = details.split(",")[1].trim();
            
            Quake quake = new Quake(qdate, details, l, magnitude, linkString);
            
            addNewQuake(quake);
          }
        }
        
      } 
    } catch (MalformedURLException e) {
      Log.d(TAG, "MalformedURLException");
    } catch (IOException e) {
      Log.d(TAG, "IOException");
    } catch (ParserConfigurationException e) {
      Log.d(TAG, "Parser Configuration Exception");
    } catch (SAXException e) {
      Log.d(TAG, "SAX Exception");
    }
  }

  
  private void addNewQuake(Quake _quake) {
    ContentResolver cr = getContentResolver();
    
    String w = EarthquakeProvider.KEY_DATE + " = " + _quake.getDate().getTime();
    
    Cursor query = cr.query(EarthquakeProvider.CONTENT_URI, null, w, null, null);
    if (query.getCount()==0) {
      ContentValues values = new ContentValues();
      
      values.put(EarthquakeProvider.KEY_DATE, _quake.getDate().getTime());
      values.put(EarthquakeProvider.KEY_DETAILS, _quake.getDetails());
      values.put(EarthquakeProvider.KEY_SUMMARY, _quake.toString());
      values.put(EarthquakeProvider.KEY_LATITUDE, _quake.getLocation().getLatitude());
      values.put(EarthquakeProvider.KEY_LONGITUDE, _quake.getLocation().getLongitude());
      values.put(EarthquakeProvider.KEY_LINK, _quake.getLink());
      values.put(EarthquakeProvider.KEY_MAGNITUDE, _quake.getMagnitude());
      
      cr.insert(EarthquakeProvider.CONTENT_URI, values);
    }
    query.close();
  }
}
