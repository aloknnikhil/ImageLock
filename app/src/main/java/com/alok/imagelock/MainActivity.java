package com.alok.imagelock;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class MainActivity extends ActionBarActivity implements ImageChooserListener {

    TextView saveImage;
    TextView matchImage;
    private ImageChooserManager imageChooserManager;
    private int chooserType;
    String filePath;

    private boolean match = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saveImage = (TextView) findViewById(R.id.save_image);
        matchImage = (TextView) findViewById(R.id.match_image);

        saveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                match = false;
                chooserType = ChooserType.REQUEST_CAPTURE_PICTURE;
                imageChooserManager = new ImageChooserManager(MainActivity.this,
                        ChooserType.REQUEST_CAPTURE_PICTURE, "ImageLock", true);
                imageChooserManager.setImageChooserListener(MainActivity.this);
                try {
                    filePath = imageChooserManager.choose();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        matchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                match = true;
                chooserType = ChooserType.REQUEST_CAPTURE_PICTURE;
                imageChooserManager = new ImageChooserManager(MainActivity.this,
                        ChooserType.REQUEST_CAPTURE_PICTURE, "ImageLock", true);
                imageChooserManager.setImageChooserListener(MainActivity.this);
                try {
                    filePath = imageChooserManager.choose();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onImageChosen(final ChosenImage chosenImage) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                if (chosenImage != null) {
                    Log.d("ImageChoose", chosenImage.getFilePathOriginal());
                    try {

                        if(match) {
                            HttpResponse<JsonNode> response = Unirest.post("https://fashionbase-image-server.p.mashape.com/api/match")
                                    .header("X-Mashape-Key", "bIleDR0ag0mshujqa2ePUnUJsxj8p12NyrVjsnc9r4BArRaPMV")
                                    .field("img", new File(chosenImage.getFilePathOriginal()))
                                    .asJson();

                            Log.d("ImageListener", response.getBody().toString());
                        }
                        else {
                            new UploadImageAsync(chosenImage.getFilePathOriginal()).execute();
                        }

                    } catch (UnirestException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void onError(String s) {

        Log.d("ImageListener", s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK
                && (requestCode == ChooserType.REQUEST_PICK_PICTURE || requestCode == ChooserType.REQUEST_CAPTURE_PICTURE)) {
            if (imageChooserManager == null) {
                reinitializeImageChooser();
            }
            imageChooserManager.submit(requestCode, data);
        }
    }

    private void reinitializeImageChooser() {
        imageChooserManager = new ImageChooserManager(this, chooserType,
                "ImageLock", true);
        imageChooserManager.setImageChooserListener(this);
        imageChooserManager.reinitialize(filePath);
    }

    //***** UPLOAD FILE ****************************

    private class UploadImageAsync extends AsyncTask<Void, Void, Void>  {

        String file;
        String uploadID;

        private UploadImageAsync(String file)   {
            this.file = file;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Log.d("FileUploader", "Uploading file " + file);
                uploadID = uploadFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                HttpResponse<String> response = Unirest.put("https://fashionbase-image-server.p.mashape.com/api/img/{id}")
                        .header("X-Mashape-Key", "bIleDR0ag0mshujqa2ePUnUJsxj8p12NyrVjsnc9r4BArRaPMV")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "text/plain")
                        .field("img", Const.BASE_URL+"/"+ Const.API_FOLDER + Const.FILE_DOWNLOADER_URL + Const.FILE + "=" + uploadID)
                        .asString();
                Log.d("ImageListener", response.getBody().toString());
            } catch (UnirestException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Upload file
     *
     * @param filePath
     * @return file ID
     * @throws IOException
     * @throws JSONException
     * @throws UnsupportedOperationException
     */

    public static String uploadFile(String filePath) throws IOException {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (filePath != null && !filePath.equals("")) {
            params.add(new BasicNameValuePair(Const.FILE, filePath));
            String fileId = getIdFromFileUploader(Const.BASE_URL+"/"+ Const.API_FOLDER +Const.FILE_UPLOADER_URL, params);
            return fileId;
        }
        return null;
    }

    public static String getIdFromFileUploader(String url,
                                               List<NameValuePair> params) throws IOException {
        // Making HTTP request

        // defaultHttpClient
        HttpParams httpParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
        HttpClient httpClient = new DefaultHttpClient(httpParams);
        HttpPost httpPost = new HttpPost(url);

        httpPost.setHeader("database", Const.DATABASE);

        Charset charSet = Charset.forName("UTF-8"); // Setting up the
        // encoding

        MultipartEntity entity = new MultipartEntity(
                HttpMultipartMode.BROWSER_COMPATIBLE);
        for (int index = 0; index < params.size(); index++) {
            if (params.get(index).getName()
                    .equalsIgnoreCase(Const.FILE)) {
                // If the key equals to "file", we use FileBody to
                // transfer the data
                entity.addPart(params.get(index).getName(),
                        new FileBody(new File(params.get(index)
                                .getValue())));
            } else {
                // Normal string data
                entity.addPart(params.get(index).getName(),
                        new StringBody(params.get(index).getValue(),
                                charSet));
            }
        }

        httpPost.setEntity(entity);

        org.apache.http.HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        InputStream is = httpEntity.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                is, Charset.forName("UTF-8")), 8);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        is.close();
        String json = sb.toString();

        return json;
    }

}
