package com.youravgjoe.apps.concentration;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private static final String PHOTO_LIST = "PHOTO_LIST";

    private static final String BASE_URL = "https://unsplash.it/800/?random";

    private boolean usePhotos = false;

    List<String> mFilePathList;

    LinearLayout mGameLayout;
//    List<Integer> mCardList; // list of image resource ids to each card image
    List<Bitmap> mBitmapList; // list of bitmaps from downloaded stock images
    List<Drawable> mDrawableList; // list of drawables if they've imported photos

    List<Integer> mMatches = new ArrayList<>();

    ImageView mImageOne;
    ImageView mImageTwo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mGameLayout = (LinearLayout) findViewById(R.id.game_layout);

        if (getIntent().getExtras() != null) {
            mFilePathList = getIntent().getStringArrayListExtra(PHOTO_LIST);
            mDrawableList = new ArrayList<>();
            usePhotos = true;
        }

        setupGame();
    }

    private void setupGame() {
        displayCards();
        randomizeCards();
    }

    private void displayCards() {
        // get screen size (minus margins and header)
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels - convertPixelsToDp(80); // 2 8dp margins + 8 8dp margins: 2(8) + 8(8) = 16 + 64 = 80
        int screenHeight = metrics.heightPixels - convertPixelsToDp(152); // 5dp header plus 2 8dp margins + 8 8dp margins: 1(56) + 2(16) + 8(8) = 56 + 32 + 64 = 152

        for (int i = 0; i < 4; i++) {
            LinearLayout row = new LinearLayout(this);
            for (int j = 0; j < 4; j++) {
                final ImageView image = new ImageView(this);
                image.setMinimumWidth(screenWidth / 4);
                image.setMinimumHeight(screenHeight / 4);
                image.setId((i * 4) + j); // this will number the cards from 0-15.

                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // if we've already matched this one, skip it
                        if (mMatches.contains(image.getId()) || (mImageOne != null && mImageOne.getId() == image.getId())) {
                            return;
                        }
                        if (mImageOne == null) {
                            mImageOne = (ImageView) findViewById(image.getId());
                            mImageOne.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            if (usePhotos) {
                                mImageOne.setImageDrawable(mDrawableList.get(image.getId()));
                            } else {
//                                mImageOne.setImageDrawable(getResources().getDrawable(mCardList.get(image.getId())));
                                mImageOne.setImageBitmap(mBitmapList.get(image.getId()));
                            }
                        } else if (mImageTwo == null) {
                            mImageTwo =  (ImageView) findViewById(image.getId());
                            mImageTwo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            if (usePhotos) {
                                mImageTwo.setImageDrawable(mDrawableList.get(image.getId()));
                            } else {
//                                mImageTwo.setImageDrawable(getResources().getDrawable(mCardList.get(image.getId())));
                                mImageTwo.setImageBitmap(mBitmapList.get(image.getId()));
                            }

                            // grab the two bitmaps so we can compare them
                            Bitmap bitmapOne = ((BitmapDrawable)mImageOne.getDrawable()).getBitmap();
                            Bitmap bitmapTwo = ((BitmapDrawable)mImageTwo.getDrawable()).getBitmap();

                            // we found a match!
                            if (bitmapOne.sameAs(bitmapTwo)) {

                                mImageOne.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                mImageTwo.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                mMatches.add(mImageOne.getId());
                                mMatches.add(mImageTwo.getId());

                                mImageOne = null;
                                mImageTwo = null;

                                if (mMatches.size() == 16) {
                                    gameWon();
                                }
                            } else {
                                // do nothing, just wait for them to click again?
                            }
                        } else {
                            mImageOne.setImageDrawable(null);
                            mImageTwo.setImageDrawable(null);

                            mImageOne = null;
                            mImageTwo = null;
                        }
                    }
                });

                CardView card = new CardView(this);

                // set card margins
                int margin = 16;
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(screenWidth / 4, screenHeight / 4);
                params.setMargins(margin, margin, margin, margin);
                card.setLayoutParams(params);
                card.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

                card.addView(image);

                row.addView(card);
            }
            mGameLayout.addView(row);
        }
    }

    private void gameWon() {
        new AlertDialog.Builder(this)
            .setMessage("You won! Congratulations, you're a pro!")
            .setPositiveButton("Yes, yes I am", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .show();
    }

    private void randomizeCards() {
        // make list of available image resources
        // give two ids to each resource (0-15)

//        mCardList = new ArrayList<>();

        mBitmapList = new ArrayList<>();

        if (mFilePathList == null || mFilePathList.isEmpty()) {

            // todo: maybe find a better api? like pixaby or pexels. One that you can specify what kind of photos you want, or something.

            final ProgressDialog dialog = new ProgressDialog(this);



            AsyncTask<Integer, Void, List<Bitmap>> getPhotosTask = new AsyncTask<Integer, Void, List<Bitmap>>() {
                @Override
                protected void onPreExecute() {
                    dialog.setMessage("Getting images...");
                    dialog.show();
                }

                @Override
                protected List<Bitmap> doInBackground(Integer... params) {
                    int numOfImages = params[0];

                    OkHttpClient client = new OkHttpClient();

                    Request request = new Request.Builder()
                            .url(BASE_URL)
                            .build();

                    Response response;

                    List<Bitmap> bmpList = new ArrayList<>();

                    for (int i = 0; i < numOfImages; i++) {
                        try {
                            response = client.newCall(request).execute();

                            bmpList.add(BitmapFactory.decodeStream(response.body().byteStream()));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return bmpList;
                }

                @Override
                protected void onPostExecute(List<Bitmap> bmpList) {
                    super.onPostExecute(bmpList);

                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    Toast.makeText(getApplicationContext(), "Images Downloaded", Toast.LENGTH_LONG).show();
                    // add each photo twice
                    for (int i = 0; i < 2; i++) {
                        for (Bitmap bmp : bmpList) {
                            mBitmapList.add(bmp);
                        }
                    }
                    long seed = System.nanoTime();
                    Collections.shuffle(mBitmapList, new Random(seed));
                }
            };
            // pass the number of photos you'll need (num of cards / 2)
            getPhotosTask.execute(8);



//            if (mBitmapList.isEmpty()) { // code for android icons
//                // add each drawable id twice
//                for (int i = 0; i < 2; i++) {
//                    mCardList.add(R.drawable.ic_brightness_5_black_24dp);
//                    mCardList.add(R.drawable.ic_insert_emoticon_black_24dp);
//                    mCardList.add(R.drawable.ic_local_airport_black_24dp);
//                    mCardList.add(R.drawable.ic_local_florist_black_24dp);
//                    mCardList.add(R.drawable.ic_local_shipping_black_24dp);
//                    mCardList.add(R.drawable.ic_phone_black_24dp);
//                    mCardList.add(R.drawable.ic_star_black_24dp);
//                    mCardList.add(R.drawable.ic_wifi_black_24dp);
//                }
//                // shuffle the cards
//                long seed = System.nanoTime();
//                Collections.shuffle(mCardList, new Random(seed));
//            } else { // code for downloaded photos
                // shuffle the cards
//                long seed = System.nanoTime();
//                Collections.shuffle(mBitmapList, new Random(seed));
//            }

        } else {
            for (String filepath : mFilePathList) {
                // create a new drawable for each photo, and add each twice
                Drawable d = new BitmapDrawable(getResources(), getBitmapFromFilePath(filepath));
                mDrawableList.add(d);
                mDrawableList.add(d);
            }
            // shuffle the cards
            long seed = System.nanoTime();
            Collections.shuffle(mDrawableList, new Random(seed));
        }
    }

    private Bitmap getBitmapFromFilePath(String filepath) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, options);

        final double heightWidthRatio = (double)options.outHeight / (double)options.outWidth;

        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        final int dpHeight = 100;

        // convert dp to pixels
        float scale = getResources().getDisplayMetrics().density;
        int reqHeight = (int) (dpHeight * scale + 0.5f);
        double reqWidth = (int) ((dpHeight / heightWidthRatio) * scale + 0.5f);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, (int)reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filepath, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private int convertPixelsToDp(int pixels) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (pixels * scale + 0.5f);
    }
}
