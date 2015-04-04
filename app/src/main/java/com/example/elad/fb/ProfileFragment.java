package com.example.elad.fb;

/**
 * Created by elad on 4/3/15.
 */
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.gorbin.asne.core.SocialNetwork;
import com.github.gorbin.asne.core.listener.OnPostingCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestSocialPersonCompleteListener;
import com.github.gorbin.asne.core.persons.SocialPerson;
import com.github.gorbin.asne.facebook.FacebookSocialNetwork;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class ProfileFragment extends Fragment implements OnRequestSocialPersonCompleteListener {
    private String message = "Android APP";
    private String link = "http://play.idomoo.com";

    private final int TIMEOUT_CONNECTION = 5000;//5sec
    private final int TIMEOUT_SOCKET = 30000;//30sec

    private static final String NETWORK_ID = "NETWORK_ID";
    private SocialNetwork socialNetwork;
    private int networkId;
    private ImageView photo;
    private TextView name;
    private TextView id;
    private TextView info;
    private Button friends;
    private Button share;
    private Button share2;
    private RelativeLayout frame;

    public static ProfileFragment newInstannce(int id) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt(NETWORK_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        networkId = getArguments().containsKey(NETWORK_ID) ? getArguments().getInt(NETWORK_ID) : 0;
        ((MainActivity)getActivity()).getSupportActionBar().setTitle("Profile");

        View rootView = inflater.inflate(R.layout.profile_fragment, container, false);

        frame = (RelativeLayout) rootView.findViewById(R.id.frame);
        photo = (ImageView) rootView.findViewById(R.id.imageView);
        name = (TextView) rootView.findViewById(R.id.name);
        id = (TextView) rootView.findViewById(R.id.id);
        info = (TextView) rootView.findViewById(R.id.info);
        friends = (Button) rootView.findViewById(R.id.friends);
        friends.setOnClickListener(friendsClick);
        share = (Button) rootView.findViewById(R.id.share);
        share.setOnClickListener(shareClick);

        share2 = (Button) rootView.findViewById(R.id.btnShare);
        share2.setOnClickListener(shareClick2);

        colorProfile(networkId);

        socialNetwork = MainFragment.mSocialNetworkManager.getSocialNetwork(networkId);
        socialNetwork.setOnRequestCurrentPersonCompleteListener(this);
        socialNetwork.requestCurrentPerson();

        MainActivity.showProgress("Loading social person");
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
           /* case R.id.action_logout:
                socialNetwork.logout();
                getActivity().getSupportFragmentManager().popBackStack();
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestSocialPersonSuccess(int i, SocialPerson socialPerson) {
        MainActivity.hideProgress();
        name.setText(socialPerson.name);
        id.setText(socialPerson.id);
        String socialPersonString = socialPerson.toString();
        String infoString = socialPersonString.substring(socialPersonString.indexOf("{")+1, socialPersonString.lastIndexOf("}"));
        info.setText(infoString.replace(", ", "\n"));
        Picasso.with(getActivity())
                .load(socialPerson.avatarURL)
                .into(photo);

        Log.d("Facebook shits: ", socialPerson.toString());
    }

    @Override
    public void onError(int networkId, String requestID, String errorMessage, Object data) {
        MainActivity.hideProgress();
        Toast.makeText(getActivity(), "ERROR: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    private View.OnClickListener friendsClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //FriendsFragment friends = FriendsFragment.newInstannce(networkId);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .addToBackStack("friends");
                    //.replace(R.id.container, friends)
                  //  .commit();
        }
    };

    private View.OnClickListener shareClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder ad = alertDialogInit("Would you like to post Link:", link);
            ad.setPositiveButton("Post link", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Bundle postParams = new Bundle();
                    postParams.putString(SocialNetwork.BUNDLE_NAME, "Shared via idomoo-play App for android");
                    postParams.putString(SocialNetwork.BUNDLE_LINK, link);
                    socialNetwork.requestPostLink(postParams, message, postingComplete);

                }
            });
            ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.cancel();
                }
            });
            ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    dialog.cancel();
                }
            });
            ad.create().show();
        }
    };

    private View.OnClickListener shareClick2 = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String type = "video/mp4";
            String caption = "idomoo-play";
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(type);
            String url = "http://api-eu.idomoo.com/vd/10002/884056/API_INTERVAL_TEST_368079874.mp4";
            try {
                final CountDownLatch latch = new CountDownLatch(1);
                downloadVideo(url, latch);
                latch.await();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Add the URI and the caption to the Intent.
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(Environment.getExternalStorageDirectory() + File.separator + "PlayVideo" + File.separator + "play.mp4") );
            share.putExtra(Intent.EXTRA_TEXT, caption);

            //shareOnlyFB(share, context_main, url);

            // Broadcast the Intent.
            startActivity(Intent.createChooser(share, "Share to"));

        }
    };

    private void downloadVideo(final String movie_url, final CountDownLatch latch) throws IOException {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    String Path = Environment.getExternalStorageDirectory().getPath();
                    String filename = Path + "/idomoo_play.mp4";
                    URL url = null;

                    try {
                        String RootDir = Environment.getExternalStorageDirectory()
                                + File.separator + "PlayVideo";
                        File RootFile = new File(RootDir);
                        RootFile.mkdir();
                        // File root = Environment.getExternalStorageDirectory();
                        URL u = new URL(movie_url);
                        HttpURLConnection c = (HttpURLConnection) u.openConnection();
                        c.setRequestMethod("GET");
                        c.setDoOutput(true);
                        c.connect();
                        FileOutputStream f = new FileOutputStream(new File(RootFile,
                                "play.mp4"));
                        InputStream in = c.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len1 = 0;

                        while ((len1 = in.read(buffer)) > 0) {
                            f.write(buffer, 0, len1);
                        }
                        f.close();
                        Log.d("vvvvvvvv....", "FINISHED!" + Environment.getExternalStorageDirectory()
                                + File.separator + "PlayVideo");
                        latch.countDown();

                    } catch (Exception e) {

                        Log.d("Error....", e.toString());
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }

    private void shareOnlyFB(Intent share, MainActivity context_main, String url) {
        String facebookAppFound = "false";
        List<ResolveInfo> matches = context_main.getPackageManager().queryIntentActivities(share, 0);
        for (ResolveInfo info : matches) {
            if (info.activityInfo.packageName.toLowerCase().startsWith("com.facebook.katana")) {
                share.setPackage(info.activityInfo.packageName);
                facebookAppFound = "true";
                break;
            }
        }
        Toast.makeText(getActivity(), facebookAppFound , Toast.LENGTH_LONG).show();
        share.putExtra(Intent.EXTRA_TEXT, url);
    }

    private OnPostingCompleteListener postingComplete = new OnPostingCompleteListener() {
        @Override
        public void onPostSuccessfully(int socialNetworkID) {
            Toast.makeText(getActivity(), "Sent", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(int socialNetworkID, String requestID, String errorMessage, Object data) {
            Toast.makeText(getActivity(), "Error while sending: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    };

    private void colorProfile(int networkId){
        int color = getResources().getColor(R.color.dark);
        int image = R.drawable.user;
        switch (networkId) {
            case FacebookSocialNetwork.ID:
                color = getResources().getColor(R.color.facebook);
                image = R.drawable.com_facebook_profile_picture_blank_square;
                break;
        }
        frame.setBackgroundColor(color);
        name.setTextColor(color);
        friends.setBackgroundColor(color);
        share.setBackgroundColor(color);
        photo.setBackgroundColor(color);
        photo.setImageResource(image);
    }

    private AlertDialog.Builder alertDialogInit(String title, String message){
        AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
        ad.setTitle(title);
        ad.setMessage(message);
        ad.setCancelable(true);
        return ad;
    }
}