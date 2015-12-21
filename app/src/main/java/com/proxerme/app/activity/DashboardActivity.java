package com.proxerme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.proxerme.app.R;
import com.proxerme.app.dialog.LoginDialog;
import com.proxerme.app.fragment.ConferencesFragment;
import com.proxerme.app.fragment.NewsFragment;
import com.proxerme.app.fragment.SettingsFragment;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.manager.PreferenceManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.ErrorHandler;
import com.proxerme.app.util.MaterialDrawerHelper;
import com.proxerme.app.util.SnackbarManager;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.LoginUser;
import com.rubengees.introduction.IntroductionActivity;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.IntroductionConfiguration;
import com.rubengees.introduction.entity.Option;
import com.rubengees.introduction.entity.Slide;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_DEFAULT;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_DONATE;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_MESSAGES;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_NEWS;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_NONE;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_SETTINGS;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_CHANGE;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_GUEST;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_LOGIN;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_LOGOUT;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_USER;
import static com.proxerme.app.util.MaterialDrawerHelper.MaterialDrawerCallback;

/**
 * This Activity provides the navigation to all different sections through the Drawer.
 *
 * @author Ruben Gees
 */
public class DashboardActivity extends MainActivity {

    private static final String EXTRA_DRAWER_ITEM = "extra_drawer_item";
    private static final String EXTRA_ADDITIONAL_INFO = "extra_additional_info";
    private static final String STATE_TITLE = "dashboard_title";

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private MaterialDrawerHelper drawerHelper;
    private OnActivityListener onActivityListener;

    private String title;

    private UserManager.OnLoginStateListener onLoginStateListener = new UserManager.OnLoginStateListener() {
        @Override
        public void onLogin(@NonNull LoginUser user) {
            if (!isDestroyedCompat()) {
                drawerHelper.refreshHeader();
            }
        }

        @Override
        public void onLogout() {
            if (!isDestroyedCompat()) {
                drawerHelper.refreshHeader();
            }
        }

        @Override
        public void onLogoutFailed(@NonNull ProxerException exception) {
            Toast.makeText(DashboardActivity.this, R.string.error_logout,
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoginFailed(@NonNull ProxerException exception) {
            UserManager.getInstance().removeUser();
            Toast.makeText(DashboardActivity.this,
                    ErrorHandler.getMessageForErrorCode(DashboardActivity.this,
                            exception.getErrorCode()), Toast.LENGTH_LONG).show();
        }
    };

    private MaterialDrawerCallback drawerCallback = new MaterialDrawerCallback() {
        @Override
        public boolean onItemClick(int identifier) {
            return handleOnDrawerItemClick(identifier);
        }

        @Override
        public boolean onAccountClick(int identifier) {
            return handleOnHeaderAccountClick(identifier);
        }

        @Override
        public void onDrawerClosed() {
            if (onActivityListener != null) {
                onActivityListener.showErrorIfNecessary();
            }
        }

        @Override
        public void onDrawerOpened() {
            SnackbarManager.dismiss();
        }
    };

    public static Intent getSectionIntent(@NonNull Context context, int drawerItemId,
                                          @Nullable String additionalInfo) {
        Intent intent = new Intent(context, DashboardActivity.class);

        intent.putExtra(EXTRA_DRAWER_ITEM, drawerItemId);
        if (additionalInfo != null) {
            intent.putExtra(EXTRA_ADDITIONAL_INFO, additionalInfo);
        }
        return intent;
    }

    private void logout() {
        UserManager.getInstance().logout();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (savedInstanceState != null) {
            title = savedInstanceState.getString(STATE_TITLE);

            setTitle(title);
            try {
                onActivityListener = (OnActivityListener) getSupportFragmentManager()
                        .findFragmentById(R.id.activity_main_content_container);
            } catch (ClassCastException e) {
                onActivityListener = null;
            }
        }

        drawerHelper = new MaterialDrawerHelper(this, drawerCallback);
        UserManager userManager = UserManager.getInstance();

        ButterKnife.bind(this);
        initViews();
        drawerHelper.build(toolbar, savedInstanceState);

        userManager.addOnLoginStateListener(onLoginStateListener);

        if (savedInstanceState == null && userManager.getUser() != null) {
            userManager.login(userManager.getUser());
        }

        displayFirstPage(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        UserManager.getInstance().removeOnLoginStateListener(onLoginStateListener);

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_MAIN)) {
            setIntent(intent);

            displayFirstPage(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntroductionBuilder.INTRODUCTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                for (Option option : data.<Option>getParcelableArrayListExtra(IntroductionActivity.
                        OPTION_RESULT)) {
                    switch (option.getPosition()) {
                        case 1:
                            PreferenceManager.setNewsNotificationsEnabled(DashboardActivity.this,
                                    option.isActivated());
                            PreferenceManager.setMessagesNotificationsEnabled(DashboardActivity.this,
                                    option.isActivated());

                            break;
                    }
                }
            }

            StorageManager.setFirstStartOccurred();
            drawerHelper.select(DRAWER_ID_DEFAULT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_TITLE, title);
        drawerHelper.saveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawerHelper.isDrawerOpen()) {
            drawerHelper.handleBackPressed();
        } else if (onActivityListener == null) {
            drawerHelper.handleBackPressed();
        } else {
            if (!onActivityListener.onBackPressed()) {
                if (!drawerHelper.handleBackPressed()) {
                    super.onBackPressed();
                }
            }
        }
    }

    private void displayFirstPage(@Nullable Bundle savedInstanceState) {
        int drawerItemToLoad = getItemToLoad(getIntent());

        if (drawerItemToLoad == DRAWER_ID_NONE) {
            if (savedInstanceState == null) {
                if (StorageManager.isFirstStart()) {
                    initIntroduction();
                } else {
                    drawerHelper.select(DRAWER_ID_DEFAULT);
                }
            }
        } else if (savedInstanceState == null) {
            drawerHelper.select(drawerItemToLoad);
        }
    }

    @MaterialDrawerHelper.DrawerItemId
    private int getItemToLoad(@NonNull Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            String url = intent.getDataString();

            if (url.contains("news")) {
                return MaterialDrawerHelper.DRAWER_ID_NEWS;
            } else if (url.contains("messages")) {
                return MaterialDrawerHelper.DRAWER_ID_MESSAGES;
            } else {
                return MaterialDrawerHelper.DRAWER_ID_NONE;
            }
        } else {
            //noinspection ResourceType
            return intent.getIntExtra(EXTRA_DRAWER_ITEM, MaterialDrawerHelper.DRAWER_ID_NONE);
        }
    }

    public void setBadge(int drawerItemId, @Nullable String text) {
        drawerHelper.setBadge(drawerItemId, text);
    }

    private void initViews() {
        setSupportActionBar(toolbar);
    }

    private void initIntroduction() {
        new IntroductionBuilder(this).withSlides(generateSlides())
                .withOnSlideListener(new IntroductionConfiguration.OnSlideListener() {
                    @Override
                    protected void onSlideInit(int position, @NonNull TextView title,
                                               @NonNull ImageView image,
                                               @NonNull TextView description) {
                        switch (position) {
                            case 0:
                                Glide.with(image.getContext())
                                        .load(R.drawable.ic_introduction_proxer).into(image);
                                break;
                            case 1:
                                Glide.with(image.getContext())
                                        .load(R.drawable.ic_introduction_notifications).into(image);
                                break;
                        }
                    }
                }).introduceMyself();
    }

    @NonNull
    private List<Slide> generateSlides() {
        List<Slide> slides = new ArrayList<>(2);

        slides.add(new Slide().withTitle(R.string.introduction_welcome_title)
                .withColorResource(R.color.primary)
                .withDescription(R.string.introduction_welcome_description));
        slides.add(new Slide().withTitle(R.string.introduction_notifications_title)
                .withColorResource(R.color.accent)
                .withOption(new Option(getString(R.string.introduction_notifications_description),
                        false)));

        return slides;
    }

    public void setFragment(@NonNull Fragment fragment, @NonNull String title) {
        this.title = title;
        setTitle(title);

        setFragment(fragment);
    }

    public void setFragment(@NonNull final Fragment fragment) {
        if (fragment instanceof OnActivityListener) {
            onActivityListener = (OnActivityListener) fragment;
        } else {
            onActivityListener = null;
        }

        SnackbarManager.dismiss();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.activity_main_content_container, fragment).commit();
            }
        });
    }

    private boolean handleOnHeaderAccountClick(int id) {
        switch (id) {
            case HEADER_ID_GUEST:
                showLoginDialog();
                return false;
            case HEADER_ID_USER:
                //Don't do anything for now
                return false;
            case HEADER_ID_LOGIN:
                showLoginDialog();
                return false;
            case HEADER_ID_CHANGE:
                showLoginDialog();
                return false;
            case HEADER_ID_LOGOUT:
                logout();
                return false;
            default:
                return false;
        }
    }

    private void showLoginDialog() {
        LoginDialog.newInstance().show(getSupportFragmentManager(), "dialog_login");
    }

    private boolean handleOnDrawerItemClick(int id) {
        switch (id) {
            case DRAWER_ID_NEWS:
                setFragment(NewsFragment.newInstance(), getString(R.string.drawer_item_news));
                return false;
            case DRAWER_ID_MESSAGES:
                setFragment(ConferencesFragment.newInstance(),
                        getString(R.string.drawer_item_messages));
                return false;
            case DRAWER_ID_DONATE:
                showPage(UrlHolder.getDonateUrl());
                return true;
            case DRAWER_ID_SETTINGS:
                setFragment(SettingsFragment.newInstance(),
                        getString(R.string.drawer_item_settings));
                return false;
            default:
                return true;
        }
    }
}
