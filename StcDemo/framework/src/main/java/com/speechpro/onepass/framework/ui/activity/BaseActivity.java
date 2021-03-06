package com.speechpro.onepass.framework.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Window;
import android.view.WindowManager;

import com.speechpro.onepass.framework.R;
import com.speechpro.onepass.framework.injection.HasComponent;
import com.speechpro.onepass.framework.injection.components.DaggerUIComponent;
import com.speechpro.onepass.framework.injection.components.UIComponent;
import com.speechpro.onepass.framework.injection.modules.ActivityModule;
import com.speechpro.onepass.framework.injection.modules.UIModule;
import com.speechpro.onepass.framework.model.IModel;
import com.speechpro.onepass.framework.model.Model;
import com.speechpro.onepass.framework.permissions.RequestPermissions;
import com.speechpro.onepass.framework.presenter.BasePresenter;
import com.speechpro.onepass.framework.ui.fragment.BaseFragment;
import com.speechpro.onepass.framework.ui.fragment.ErrorDialogFragment;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base {@link android.app.Activity} class for every Activity in this application.
 *
 * @author volobuev
 * @since 13.01.2016
 */
public abstract class BaseActivity extends AppCompatActivity implements HasComponent<UIComponent> {

    private static final String TAG = BaseActivity.class.getSimpleName();

    static final String INSTANCE_STATE_PARAM_USER_ID = "com.speechpro.onepass.INSTANCE_STATE_PARAM_USER_ID";
    static final String INSTANCE_STATE_PARAM_URL = "com.speechpro.onepass.INSTANCE_STATE_PARAM_URL";

    private static final String INTENT_PARAM_USER_ID = "com.speechpro.onepass.PARAM_USER_ID";
    private static final String INTENT_PARAM_URL = "com.speechpro.onepass.PARAM_URL";

    String userId;
    Pair<Integer, Fragment> currentFragment;

    protected IModel model;
    protected String url;
    protected BasePresenter presenter;

    private UIComponent uiComponent;
    private SensorManager sensorManager;
    private Sensor lightSensor;

    private Queue<Pair<Integer, Fragment>> fragmentsQueue = new LinkedList<>();

    private ExecutorService mService = Executors.newSingleThreadExecutor();

    public static Intent getCallingIntent(Context context, Class clazz, String userId, String url) {
        Intent callingIntent = new Intent(context, clazz);
        callingIntent.putExtra(INTENT_PARAM_USER_ID, userId);
        callingIntent.putExtra(INTENT_PARAM_URL, url);
        return callingIntent;
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    public void nextEpisode() {
        Log.d(TAG, "Checking fragment queue...");
        if (fragmentsQueue.isEmpty()) {
            this.finish();
            return;
        }
        Log.d(TAG, "Running thread executing with nextFragment() method...");
        mService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "nextFragment() method...");
                nextFragment();
                Log.d(TAG, "nextFragment() is finished");
            }
        });
    }

    /**
     * Get the framework component for dependency injection.
     *
     * @return {@link UIComponent}
     */
    public UIComponent getComponent() {
        return uiComponent;
    }

    public BasePresenter getPresenter() {
        return presenter;
    }

    public String getUserId() {
        return userId;
    }

    public void setSensorListener(SensorEventListener sensorListener) {
        if (lightSensor == null) {
            Log.d(TAG, "Light sensor is unavailable.");
        } else {
            sensorManager.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void removeSensorListener(SensorEventListener sensorListener) {
        if (lightSensor == null) {
            Log.d(TAG, "Light sensor is unavailable.");
        } else {
            sensorManager.unregisterListener(sensorListener, lightSensor);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            this.userId = getIntent().getStringExtra(INTENT_PARAM_USER_ID);
            this.url = getIntent().getStringExtra(INTENT_PARAM_URL);
        } else {
            this.userId = savedInstanceState.getString(INSTANCE_STATE_PARAM_USER_ID);
            this.url = savedInstanceState.getString(INSTANCE_STATE_PARAM_URL);
        }

        model = new Model(url);
        initializeInjector();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Fragment fragment = currentFragment.second;
        if (fragment instanceof BaseFragment) {
            ((BaseFragment) fragment).removeCancelHandler();
        }
        if (!isFinishing()) {
            finish();
        }
    }

    public void showErrorFragment(Fragment frsgment, String title, String descriptionError) {
        FragmentManager manager = getFragmentManager();
        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ErrorDialogFragment.TITLE_ERROR, title);
        bundle.putString(ErrorDialogFragment.DESCRIPTION_ERROR, descriptionError);
        errorDialogFragment.setArguments(bundle);
        errorDialogFragment.setTargetFragment(frsgment, 1);
        FragmentTransaction transaction = manager.beginTransaction();
        errorDialogFragment.show(transaction, "dialog");
    }

    protected void addFragmentToQueue(Pair<Integer, Fragment> pair) {
        fragmentsQueue.add(pair);
    }

    protected void nextFragment() {
        Log.d(TAG, "nextFragment: ");
        currentFragment = fragmentsQueue.poll();
        if (currentFragment != null)
            replaceFragment(currentFragment.first, currentFragment.second);
    }

    /**
     * Adds a {@link Fragment} to this activity's layout.
     *
     * @param containerViewId The container view to where add the fragment.
     * @param fragment        The fragment to be added.
     */
    protected void replaceFragment(int containerViewId, Fragment fragment) {
        FragmentTransaction fragmentTransaction = this.getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right);
        fragmentTransaction.replace(containerViewId, fragment);
        fragmentTransaction.addToBackStack(null);
        Log.d(TAG, "Fragment transaction committing...");
        if (!isDestroyed()) {
            Log.d(TAG, "Adding fragment...");
            fragmentTransaction.commit();
        }
        Log.d(TAG, "Commit is finished");
    }

    /**
     * Get an Activity module for dependency injection.
     *
     * @return {@link com.speechpro.onepass.framework.injection.modules.ActivityModule}
     */
    protected ActivityModule getActivityModule() {
        return new ActivityModule(this);
    }

    private void initializeInjector() {
        this.uiComponent = DaggerUIComponent.builder().uIModule(new UIModule(getApplicationContext())).build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RequestPermissions.REQUEST_PERMISSION_ALL) {
            RequestPermissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
