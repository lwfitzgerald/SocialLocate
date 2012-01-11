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
import android.view.KeyEvent;

public class SLRespond extends SLBaseActivity {
    private User friend;
    private ProgressDialog progressDialog;
    
    private volatile boolean usersLoaded = false;
    private volatile boolean venueLoaded = false;
    
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
                        
                        int newFriendID;

                        // For debugging...
                        if (friendID == users.get(0).getId()) {
                            newFriendID = users.get(1).getId();
                        } else {
                            newFriendID = friendID;
                        }
                        
                        // Find friend in users
                        for (User user : users) {
                            if (user.getId() == newFriendID) {
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
                                    
                                    usersLoaded = true;
                                    
                                    if (venueLoaded) {
                                        progressDialog.dismiss();
                                    }
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
                                    
                                    venueLoaded = true;
                                    
                                    if (usersLoaded) {
                                        progressDialog.dismiss();
                                    }
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
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isTracking()
                && !event.isCanceled()) {
            
            finish();
            
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public void onBackPressed() {
        mapView.onBackPressed();
    }
}
