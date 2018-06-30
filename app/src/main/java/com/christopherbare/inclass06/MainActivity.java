package com.christopherbare.inclass06;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //For reuse throughout program
    String category;
    int numArticles;
    int articleIndex;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the elements on the page
        final Spinner spinner = findViewById(R.id.spinner);
        final ImageView imageView = findViewById(R.id.imageView);
        final ImageButton nextButton = findViewById(R.id.imageButton_next);
        final ImageButton prevButton = findViewById(R.id.imageButton_prev);
        final TextView position = findViewById(R.id.textView_position);
        final ScrollView scrollView = findViewById(R.id.scrollView);

        //Set the progress dialog message for all future uses
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading...");

        //Create the list of categories for the user to choose from and add them to the spinner
        String[] keywords = {"Business", "Entertainment", "General", "Health", "Science", "Sports", "Technology"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, keywords);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //Make sure the no elements on the page are displayed (that shouldn't be) before any news is displayed
        nextButton.setVisibility(View.GONE);
        prevButton.setVisibility(View.GONE);
        position.setVisibility(View.GONE);
        scrollView.setVisibility(View.INVISIBLE);

        //On click listener for GO
        findViewById(R.id.button_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Make sure the user is connected to the internet
                if (isConnected()) {

                    //Show the progress dialog
                    progressDialog.show();

                    //Send the appropriate user request
                    category = spinner.getItemAtPosition(spinner.getSelectedItemPosition()).toString().toLowerCase();
                    new GetDataAsync().execute("https://newsapi.org/v2/top-headlines?country=us&apiKey=9d234a1623c14c1185112443406b0c6d&category=" + category);

                    //Set the article index to 0 since this listener loads a new category each time
                    articleIndex = 0;

                    //Show the article position text view
                    position.setVisibility(View.VISIBLE);

                } else {
                    Toast.makeText(getApplicationContext(), "No Network Connection.", Toast.LENGTH_LONG).show();
                }
            }
        });

        //On click listener for NEXT
        findViewById(R.id.imageButton_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Show the progress dialog
                progressDialog.show();

                //Keep the articles in a loop so that the user can go back and forth between the first and last article
                if (articleIndex == numArticles-1) articleIndex = 0;
                else articleIndex++;

                //Send off the request
                new GetDataAsync().execute("https://newsapi.org/v2/top-headlines?country=us&apiKey=9d234a1623c14c1185112443406b0c6d&category="+category);
            }
        });

        //On click listener for PREV
        findViewById(R.id.imageButton_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Show the progress dialog
                progressDialog.show();

                //Keep the articles in a loop so that the user can go back and forth between the first and last article
                if (articleIndex == 0) articleIndex = numArticles-1;
                else articleIndex--;

                //Send off the request
                new GetDataAsync().execute("https://newsapi.org/v2/top-headlines?country=us&apiKey=9d234a1623c14c1185112443406b0c6d&category=" + category);
            }
        });
    }

    //Method to make sure the user is connected to the internet
    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected() ||
                (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                        && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            return false;
        }
        return true;
    }

    //AsyncTask to get the news articles via JSON
    private class GetDataAsync extends AsyncTask<String, Void, Article> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)

        @Override
        protected Article doInBackground(String... params) {

            HttpURLConnection connection = null;
            Article articleObj = null;

            try {

                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    //The JSON file in string format
                    String json = IOUtils.toString(connection.getInputStream(), "UTF-8");

                    //The entire JSON object
                    JSONObject root = new JSONObject(json);

                    //The array of articles within the JSON object
                    JSONArray articles = root.getJSONArray("articles");
                    numArticles = articles.length();

                    //Initialize the Article object
                    articleObj = new Article();

                    //Get the JSON Article
                    JSONObject JSON_article = articles.getJSONObject(articleIndex);

                    //Fill out the Article object
                    articleObj.setTitle(JSON_article.getString("title"));
                    articleObj.setPublishedAt(JSON_article.getString("publishedAt"));
                    articleObj.setUrlToImage(JSON_article.getString("urlToImage"));
                    articleObj.setDescription(JSON_article.getString("description"));
                    if (articleObj.getDescription()==null || articleObj.getDescription().equals("null")) articleObj.setDescription("No description");

                } else Toast.makeText(getApplicationContext(), "A problem occured", Toast.LENGTH_LONG).show();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return articleObj;
        }

        @Override
        protected void onPostExecute(Article result) {

            //If a news article is returned
            if (result != null) {
                //Get the page elements
                TextView title = findViewById(R.id.textView_title);
                TextView published = findViewById(R.id.textView_published);
                TextView description = findViewById(R.id.textView_desc);
                ImageView imageView = findViewById(R.id.imageView);
                ScrollView scrollView = findViewById(R.id.scrollView);
                TextView position = findViewById(R.id.textView_position);
                ImageButton nextButton = findViewById(R.id.imageButton_next);
                ImageButton prevButton = findViewById(R.id.imageButton_prev);

                //Set the page elements
                title.setText(result.getTitle());
                published.setText(result.getPublishedAt());
                description.setText(result.getDescription());
                Picasso.get().load(result.getUrlToImage()).into(imageView);
                if (scrollView.getVisibility()==View.INVISIBLE) scrollView.setVisibility(View.VISIBLE);
                position.setText((articleIndex+1) + " out of " + (numArticles));
                if (numArticles>1) {
                    nextButton.setVisibility(View.VISIBLE);
                    prevButton.setVisibility(View.VISIBLE);
                } else {
                    nextButton.setVisibility(View.GONE);
                    prevButton.setVisibility(View.GONE);
                }
                if (progressDialog.isShowing()) progressDialog.hide();

            } else {
                Toast.makeText(getApplicationContext(), "No news found", Toast.LENGTH_LONG).show();
                Log.d("demo", "No news found");
            }
        }
    }

}
