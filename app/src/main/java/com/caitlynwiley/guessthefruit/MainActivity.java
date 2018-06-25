package com.caitlynwiley.guessthefruit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> fruitRawInfo;
    ArrayList<Fruit> fruits;
    Random rand;
    String[] choices;
    int[] buttonIds = {R.id.button1, R.id.button2, R.id.button3, R.id.button4};
    ImageView fruitImageView;
    TextView scoreTextView;
    int correctChoice;
    int score;
    int questionsAsked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fruitRawInfo = new ArrayList<String>();
        fruits = new ArrayList<Fruit>();
        rand = new Random();
        choices = new String[4];
        fruitImageView = findViewById(R.id.fruitImageView);
        scoreTextView = findViewById(R.id.scoreTextView);
        score = 0;
        questionsAsked = 0;

        // https://nutrineat.com/fruits-list
        String result = "";
        DownloadWebContent task = new DownloadWebContent();
        try {
            long start = Calendar.getInstance().getTimeInMillis();
            result = task.execute("https://nutrineat.com/fruits-list").get();
            long end = Calendar.getInstance().getTimeInMillis();
            Log.d("Download Time", (end - start) / 1000 + "s");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        generateLists(result);

        nextQuestion();
    }

    public void nextQuestion() {
        if (questionsAsked == fruits.size()) {
            //game over, show score
            scoreTextView.setText(String.format("You got %d/62 correct!", score));
        }

        int index = rand.nextInt(fruits.size());

        // loop until you get a fruit that hasn't been used
        while (fruits.get(index).hasBeenShown()) {
            index = rand.nextInt(fruits.size());
        }
        Fruit current = fruits.get(index);
        current.setShown();
        correctChoice = rand.nextInt(4);
        choices[correctChoice] = current.getName();

        for (int i = 0; i < 4; i++) {
            Log.i("Loop", "i = " + i);
            // only set the other 3 spots, don't overwrite the correct answer
            if (i != correctChoice) {
                choices[i] = fruits.get(rand.nextInt(fruits.size())).getName();
                for (int j = 0; j < i; j++) {
                    Log.i("Loop", "j = " + j);
                    if (choices[i].equals(choices[j])) {
                        // get new fruit name if already in list, then restart loop (j=0)
                        choices[i] = fruits.get(rand.nextInt(fruits.size())).getName();
                        j = 0;
                    }
                }
            }
        }

        // display fruit names on buttons
        Log.i("Buttons", "Setting up buttons");
        for (int i = 0; i < 4; i++) {
            ((Button) findViewById(buttonIds[i])).setText(choices[i]);
        }

        // display picture
        Log.i("Picture", "Displaying picture");
        fruitImageView.setImageBitmap(current.getBitmap());

        questionsAsked++;
    }

    public void checkAnswer(View view) {
        String toastText = "";
        if (view.getId() == buttonIds[correctChoice]) {
            toastText = "Correct!";
            score++;
        } else {
            toastText = "Wrong.";
        }
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
        nextQuestion();
    }

    private void generateLists(String content) {
        if (content == null || content.equals("")) {
            Log.e("Parsing Web Content", "Empty string");
            System.exit(1);
        }

        Pattern p = Pattern.compile("<div class=\"bz-card-body\"><div class=\"bz-card-image\"><img src=\"(.*?)\" width=");
        Matcher m = p.matcher(content);
        while (m.find()) {
            fruitRawInfo.add(m.group(1)); // to get the thing it found
            //Log.i("Fruit Info", m.group(1));
        }

        Log.i("Loop", "Start getting Bitmaps and adding to fruits array");
        for (int i = 0; i < fruitRawInfo.size(); i++) {
            // split each line into the pic url and the alt text
            String[] infoArray = fruitRawInfo.get(i).split("\" alt=\"");
            // download the picture and add it's bitmap to the array list
            DownloadPicture getPic = new DownloadPicture();
            try {
                Bitmap bm = getPic.execute(infoArray[0]).get();
                Fruit f = new Fruit(bm, infoArray[1]);
                fruits.add(f);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public class DownloadWebContent extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    result.append((char) data);
                    data = reader.read();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result.toString();
        }
    }

    public class DownloadPicture extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream in = connection.getInputStream();
                Bitmap bm = BitmapFactory.decodeStream(in);
                return bm;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class Fruit {
        Bitmap bitmap;
        String name;
        boolean hasBeenShown;

        public Fruit(Bitmap bm, String name) {
            bitmap = bm;
            this.name = name;
            hasBeenShown = false;
        }

        public String getName() {
            return name;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public boolean hasBeenShown() {
            return hasBeenShown;
        }

        public void setShown() {
            hasBeenShown = true;
        }
    }
}
