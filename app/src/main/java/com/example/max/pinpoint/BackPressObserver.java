package com.example.max.pinpoint;

/**
 * Created by Max on 11/27/2016.
 *
 * An interface of sorts, adding this to a fragment will ensure that navigation events can be checked first
 * and vary based on the fragment being referred to
 *
 * For instance, if the user presses the back button, this interface can be used on a fragment
 * to ensure that something takes place instead of just navigating back
 */

public interface BackPressObserver {
    boolean isReadyToInterceptBackPress();
}
