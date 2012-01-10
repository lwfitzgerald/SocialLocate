package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.inflatablegoldfish.sociallocate.foursquare.Venue;
import com.inflatablegoldfish.sociallocate.request.FSVenueRequest;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.SLInitialFetchRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

public class SLRespond extends SLBaseActivity {
    private User friend;
    private ProgressDialog progressDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.respond_map_view);
        
        // Set up the map view
        mapView = (SLRespondMapView) findViewById(R.id.map_view);
        ((SLRespondMapView) mapView).setUp(this, picRunner);
        
        // Get details from the intent
        Intent startIntent = getIntent();
        final int friendID = startIntent.getExtras().getInt("friend_id");
        String venueID = startIntent.getExtras().getString("venue_id");
        
        // Show progress dialog
        progressDialog = ProgressDialog.show(this, "", getText(R.string.loading_details));
        
        requestManager.addRequest(
            new SLInitialFetchRequest(
                requestManager,
                new RequestListener<List<User>>() {
                    @SuppressWarnings("unchecked")
                    public void onComplete(final Object userList) {
                        final List<User> users = (List<User>) userList;
                        
                        // Find friend in users
                        for (User user : users) {
                            if (user.getId() == friendID) {
                                friend = user;
                                break;
                            }
                        }
                        
                        // Calculate distance
                        if (currentLocation != null) {
                            friend.setDistanceFrom(currentLocation);
                        }
                        
                        Util.uiHandler.post(
                            new Runnable() {
                                public void run() {
                                    updateOwnUser(users.get(0));
                                    
                                    mapView.updateUser(friend, true);
                                }
                            }
                        );
                    }

                    public void onError(ResultCode resultCode) {
                        Util.showToast(getText(R.string.load_details_failed), SLRespond.this);
                        
                        Util.uiHandler.post(
                            new Runnable() {
                                public void run() {
                                    progressDialog.cancel();
                                    finish();
                                }
                            }
                        );
                    }
                    
                    public void onCancel() {
                        Util.showToast(getText(R.string.load_details_failed), SLRespond.this);
                        
                        Util.uiHandler.post(
                            new Runnable() {
                                public void run() {
                                    progressDialog.cancel();
                                    finish();
                                }
                            }
                        );
                    }
                },
                facebook,
                socialLocate
            )
        );
        
        requestManager.addRequest(
            new FSVenueRequest(
                venueID,
                foursquare,
                requestManager,
                new RequestListener<Venue>() {
                    public void onComplete(final Object venueObj) {
                        final Venue venue = (Venue) venueObj;
                        
                        // Calculate distance
                        if (currentLocation != null) {
                            venue.setDistanceFrom(currentLocation);
                        }
                        
                        Util.uiHandler.post(
                            new Runnable() {
                                public void run() {
                                    mapView.setVenue(venue);
                                    
                                    progressDialog.dismiss();
                                }
                            }
                        );
                    }

                    public void onError(ResultCode resultCode) {
                        Util.showToast(getText(R.string.load_details_failed), SLRespond.this);
                        
                        Util.uiHandler.post(
                            new Runnable() {
                                public void run() {
                                    progressDialog.cancel();
                                    finish();
                                }
                            }
                        );
                    }
                    
                    public void onCancel() {
                        Util.showToast(getText(R.string.load_details_failed), SLRespond.this);
                        
                        Util.uiHandler.post(
                            new Runnable() {
                                public void run() {
                                    progressDialog.cancel();
                                    finish();
                                }
                            }
                        );
                    }
                }
            )
        );
    }
}
